-- ============================================================================
-- ReviseIQ: align the DEPLOYED Supabase tables with the app's sync models
-- ----------------------------------------------------------------------------
-- Run this ONCE in Supabase Dashboard -> SQL Editor. It is idempotent for the
-- renames (they fail loudly if the source column is gone) and safe to run even
-- if the local app already created rows, because all cloud tables are empty.
--
-- Why: Postgres folds unquoted identifiers to lowercase, so the columns
-- created by the original schema.sql are stored as `colorhex`, `createdat`,
-- etc. The app reads/writes exact camelCase keys (colorHex, createdAt...),
-- which made PostgREST fail with "column does not exist". These statements
-- rename the stored columns to the exact case the app expects.
-- ============================================================================

-- ---------- decks ----------
alter table public.decks rename column colorhex to "colorHex";
alter table public.decks rename column createdat to "createdAt";
alter table public.decks add column if not exists folder_uuid text not null default '';

-- ---------- flashcards ----------
alter table public.flashcards rename column boxlevel to "boxLevel";
alter table public.flashcards rename column intervaldays to "intervalDays";
alter table public.flashcards rename column easefactor to "easeFactor";
alter table public.flashcards rename column lastreviewed to "lastReviewed";
alter table public.flashcards rename column nextreviewdate to "nextReviewDate";
alter table public.flashcards rename column lastrating to "lastRating";
alter table public.flashcards add column if not exists deck_uuid text not null default '';

-- ---------- study_logs ----------
alter table public.study_logs rename column reviewdurationseconds to "reviewDurationSeconds";
alter table public.study_logs add column if not exists card_uuid text not null default '';

-- ---------- daily_streaks ----------
alter table public.daily_streaks rename column datestring to "dateString";
alter table public.daily_streaks rename column cardsreviewed to "cardsReviewed";
alter table public.daily_streaks rename column quizzescompleted to "quizzesCompleted";
alter table public.daily_streaks rename column studydurationminutes to "studyDurationMinutes";
alter table public.daily_streaks rename column goaltargetcards to "goalTargetCards";
alter table public.daily_streaks rename column targetmet to "targetMet";

-- ---------- quiz_results ----------
alter table public.quiz_results rename column decktitle to "deckTitle";
alter table public.quiz_results rename column totalquestions to "totalQuestions";
alter table public.quiz_results rename column correctanswers to "correctAnswers";
alter table public.quiz_results rename column scorepercentage to "scorePercentage";
alter table public.quiz_results rename column durationseconds to "durationSeconds";
alter table public.quiz_results add column if not exists deck_uuid text not null default '';

-- ---------- folders ----------
alter table public.folders rename column colorhex to "colorHex";
alter table public.folders rename column createdat to "createdAt";