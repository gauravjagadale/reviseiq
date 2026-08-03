<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/010f60f5-cf34-4ac5-9469-79fa9f8b29fa

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## Cloud Sync & Accounts (Supabase)

ReviseIQ backs up decks, flashcards, streaks, quiz history and preferences to
Supabase (open-source Postgres) with per-user row-level security. Guests can
use the app fully offline; signing in enables sync.

### 1. Create the backend

1. Create a project at [supabase.com](https://supabase.com) (free tier is fine).
2. Open **SQL Editor** and paste the entire contents of
   [`supabase-setup/schema.sql`](supabase-setup/schema.sql), then click **Run**.
   This creates the tables, indexes, RLS policies and the `set_user_id` trigger.
3. (Optional) In **Authentication → Providers** enable **Email** and optionally
   **Google** (configure the OAuth client ID from Google Cloud Console).

### 2. Configure the app

Add these to `.env` (see `.env.example`):

```
SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_JWT_OR_PUBLISHABLE_KEY
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID   # optional; hides Google button if placeholder
```

- The **anon key** is public-safe — row-level security policies isolate each
  user's data, so the key can ship in the app.
- `GOOGLE_WEB_CLIENT_ID` is the **Web client ID** of the Google Cloud OAuth
  client (used by Credential Manager). Leave the placeholder to hide the
  Google button; email/password sign-in still works.

### 3. Sync behavior

- Every local write is stamped with a UUID + timestamp and pushed to the cloud
  on the next sync; edits from other devices are pulled and merged
  (last-write-wins).
- Deletions are soft (tombstones) so they propagate across devices.
- Sync triggers automatically on app foreground, after card reviews/quizzes,
  and immediately after signing in. You can also tap **Sync Now** in
  **Settings → Account & Sync**.

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Supabase sync + Gemini AI features |
| `POST_NOTIFICATIONS` (runtime) | Daily study reminders & Pomodoro completion alerts; requested on first launch and again when reminders are enabled |
| `SCHEDULE_EXACT_ALARM` | Reminders fire on time; if revoked, the app degrades to inexact alarms instead of crashing |
| `RECEIVE_BOOT_COMPLETED` | Re-arms daily reminders after a device reboot |

No location, storage, camera or contact permissions are used. The app works
fully offline as a guest — no account required.

## Release builds

Local release builds are signed with `my-upload-key.jks` (alias `upload`).
Pass `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` as environment
variables, then run:

```
./gradlew assembleRelease
```

For Play Store publishing, Google requires an Android App Bundle (`.aab`):

```
./gradlew bundleRelease
```

