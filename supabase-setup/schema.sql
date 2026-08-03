-- ============================================================
-- ReviseIQ sync schema (Supabase)
-- Open in the SQL Editor, paste everything, click Run.
-- ============================================================
-- NOTE: This schema uses UUID-based foreign keys (folder_uuid,
-- deck_uuid, card_uuid) instead of numeric ids. Numeric auto-increment
-- ids are per-device and collide across devices, which corrupts
-- cross-device sync. If you previously ran an older version of this
-- file, the DROP TABLE statements below will recreate the affected
-- tables (destructive) so every device gets the corrected shape.
--
-- IMPORTANT: camelCase columns (colorHex, boxLevel, ...) are wrapped in
-- double quotes. Postgres folds unquoted identifiers to lowercase, which
-- would make PostgREST reject the app's exact-case column names. Keep the
-- quotes when editing. Existing deployments that were created from the
-- unquoted version must run fix-deployed-schema.sql first.

drop table if exists public.decks cascade;
drop table if exists public.flashcards cascade;
drop table if exists public.study_logs cascade;
drop table if exists public.quiz_results cascade;
drop table if exists public.folders cascade;
drop table if exists public.daily_streaks cascade;
drop table if exists public.user_settings cascade;

-- Helper: stamp every row with the logged-in user id
create or replace function public.set_user_id()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  new.user_id := auth.uid();
  return new;
end $$;

-- ---------- folders ----------
create table if not exists public.folders (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  name text not null,
  "colorHex" text not null default '#6366F1',
  "createdAt" bigint not null default 0,
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, uuid)
);
create index if not exists folders_updated_idx on public.folders (user_id, updated_at);
create trigger trg_folders before insert on public.folders
  for each row execute function public.set_user_id();
alter table public.folders enable row level security;
create policy "folders_all" on public.folders for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- decks ----------
create table if not exists public.decks (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  title text not null,
  description text not null default '',
  category text not null default '',
  "colorHex" text not null default '#6366F1',
  "createdAt" bigint not null default 0,
  folder_uuid text not null default '',
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, uuid)
);
create index if not exists decks_updated_idx on public.decks (user_id, updated_at);
create trigger trg_decks before insert on public.decks
  for each row execute function public.set_user_id();
alter table public.decks enable row level security;
create policy "decks_all" on public.decks for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- flashcards ----------
create table if not exists public.flashcards (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  deck_uuid text not null default '',
  front text not null,
  back text not null,
  hint text not null default '',
  "boxLevel" integer not null default 1,
  "intervalDays" integer not null default 1,
  repetitions integer not null default 0,
  "easeFactor" double precision not null default 2.5,
  "lastReviewed" bigint,
  "nextReviewDate" bigint not null default 0,
  "lastRating" text not null default '',
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, uuid)
);
create index if not exists flashcards_updated_idx on public.flashcards (user_id, updated_at);
create trigger trg_flashcards before insert on public.flashcards
  for each row execute function public.set_user_id();
alter table public.flashcards enable row level security;
create policy "flashcards_all" on public.flashcards for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- study_logs ----------
create table if not exists public.study_logs (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  card_uuid text not null default '',
  deck_uuid text not null default '',
  timestamp bigint not null default 0,
  rating text not null default '',
  "reviewDurationSeconds" integer not null default 5,
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, uuid)
);
create index if not exists study_logs_updated_idx on public.study_logs (user_id, updated_at);
create trigger trg_study_logs before insert on public.study_logs
  for each row execute function public.set_user_id();
alter table public.study_logs enable row level security;
create policy "study_logs_all" on public.study_logs for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- daily_streaks ----------
create table if not exists public.daily_streaks (
  "dateString" text not null,
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  "cardsReviewed" integer not null default 0,
  "quizzesCompleted" integer not null default 0,
  "studyDurationMinutes" integer not null default 0,
  "goalTargetCards" integer not null default 20,
  "targetMet" boolean not null default false,
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, "dateString")
);
create unique index if not exists daily_streaks_uuid_uidx on public.daily_streaks (user_id, uuid);
create index if not exists daily_streaks_updated_idx on public.daily_streaks (user_id, updated_at);
create trigger trg_daily_streaks before insert on public.daily_streaks
  for each row execute function public.set_user_id();
alter table public.daily_streaks enable row level security;
create policy "daily_streaks_all" on public.daily_streaks for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- quiz_results ----------
create table if not exists public.quiz_results (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  uuid text not null,
  deck_uuid text not null default '',
  "deckTitle" text not null default '',
  "totalQuestions" integer not null default 0,
  "correctAnswers" integer not null default 0,
  "scorePercentage" integer not null default 0,
  "durationSeconds" integer not null default 0,
  timestamp bigint not null default 0,
  updated_at bigint not null default 0,
  is_deleted boolean not null default false,
  primary key (user_id, uuid)
);
create index if not exists quiz_results_updated_idx on public.quiz_results (user_id, updated_at);
create trigger trg_quiz_results before insert on public.quiz_results
  for each row execute function public.set_user_id();
alter table public.quiz_results enable row level security;
create policy "quiz_results_all" on public.quiz_results for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- user_settings ----------
create table if not exists public.user_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  payload jsonb not null default '{}'::jsonb,
  updated_at bigint not null default 0
);
create trigger trg_user_settings before insert on public.user_settings
  for each row execute function public.set_user_id();
alter table public.user_settings enable row level security;
create policy "settings_all" on public.user_settings for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);
