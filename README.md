# Hisaboo v3.0 — Hybrid Offline + Gmail Cloud Auto-Sync & Dark Obsidian FinTech UI

Complete Kotlin + Jetpack Compose (Material 3) Android app for `com.mianzahid.expenseentry`.

## How Hybrid Offline + Gmail Sync Works
1. **Offline Auto-Save (No Login Needed):** Every entry, category, budget, loan, bill, reminder, and PIN automatically saves to local device storage (`SharedPreferences`). Closing the app or restarting the phone never resets your data.
2. **Gmail / Google Account Auto-Backup & Auto-Restore:**
   - Open **Hisaboo Hub -> Backup / Export -> Sign in with Gmail / Google**.
   - Once signed in, every change auto-syncs to your Google account's hidden `appDataFolder`.
   - When you install Hisaboo on a new phone and sign in with the same Gmail ID, your ledger **auto-restores** immediately.
3. **One-Time Google Cloud Setup (For Google Sign-In in APK):**
   - Go to [Google Cloud Console](https://console.cloud.google.com/) -> Create a project -> **APIs & Services -> Enable APIs** -> Enable **Google Drive API**.
   - Go to **OAuth consent screen** -> External -> Add your Gmail as a Test User.
   - Go to **Credentials -> Create Credentials -> OAuth client ID -> Android**:
     - **Package name:** `com.mianzahid.expenseentry`
     - **SHA-1 certificate fingerprint:** Copy the `SHA1:` printed in your GitHub Actions build log under the `Show Debug SHA-1 for Google Sign-In` step.
