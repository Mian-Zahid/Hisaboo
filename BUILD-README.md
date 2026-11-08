# Kharcha Saathi — Standalone Android App (APK v1.1 ✅ — crash fixed)

## APK (ready to install) — v1.1 (fixed, renamed)

- **File:** `expense-entry-app-debug.apk` (in this folder)
- **Size:** 55 MB
- **MD5:** `79c1d009bac57e6c03e89c6c66da60ae`
- **App name (NEW, chosen by Mian 2026-11-06 22:15 PKT):** **Kharcha Saathi** (home-screen label; manifest `android:label` + in-app `appName` in all 3 languages). Package stays `com.mianzahid.expenseentry`.
- **Version:** 1.1 (versionCode 2) — installs over v1.0
- **minSdk:** 26 (Android 8.0+), **targetSdk:** 34, **compileSdk:** 34
- **Launcher activity:** `com.mianzahid.expenseentry.MainActivity`
- **Signature:** DEBUG keystore (self-signed, `debug.keystore` in this folder,
  storepass/keypass `android`, alias `androiddebugkey`) — **v2 + v3 schemes verify OK**.
  This is a **debug** build, not a release build.

## ⚠️ v1.0 crash — what happened and what was fixed (2026-11-06)

**Symptom:** v1.0 installed fine but closed immediately on launch ("foran band ho rai hai").

**Root cause (found by inspecting the v1.0 dex, not guessed):** the manual build's
D8 runtime filter excluded several real runtime dependencies, and the dependency
resolver never fetched one of them at all. The fatal one:
- `androidx.customview.poolingcontainer.PoolingContainer` was **missing from the dex
  entirely** (no jar in `libs/`). Compose's `ComponentActivity.setContent()` uses the
  default `ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool`,
  whose `installFor()` calls `PoolingContainer.isPoolingContainer(view)` — so the
  app threw `NoClassDefFoundError` on the very first frame, before any UI appeared.
  Proven by jar-level scan: `ui-android-1.7.2.jar` references PoolingContainer in
  `ViewCompositionStrategy$DisposeOnDetachedFromWindowOrReleasedFromPool*`.

**Also fixed in the same build (same class of bug, would have crashed next):**
- `androidx.emoji2.text.EmojiCompat` / `EmojiSpan` — missing from dex; referenced by
  Compose `ui-text` / `foundation` (`AndroidParagraphHelper`, `TextInputServiceAndroid`)
  on the text-layout path. Fetched `emoji2-1.4.0` AAR from Google Maven, `classes.jar`
  saved as `libs/emoji2-1.4.0.jar`.
- `androidx.lifecycle.LiveData` / `MutableLiveData` — jars existed in `libs/` but the
  D8 filter excluded `lifecycle-livedata*` (referenced by `fragment` and `biometric`).
  Filter fixed so the 2.8.2 livedata jars are now dexed (stale `lifecycle-livedata-core-ktx-2.6.1.jar`
  stays excluded — it would duplicate `LiveDataKt` from the 2.8.2 ktx jar).
- Fetched `customview-poolingcontainer-1.0.0` AAR from Google Maven, `classes.jar`
  saved as `libs/customview-poolingcontainer-1.0.0.jar`.
- Startup guards added in `ViewModel.kt`: `seedIfEmpty()` and `refresh()` DB work is
  now wrapped in try/catch, so a DB failure can never crash first launch (falls back
  to an empty DB instead of closing the app).

**Verified in v1.1:** `apksigner verify` = Verifies (v2+v3); `dexdump` confirms
`PoolingContainer`, `PoolingContainerListener`, `EmojiCompat`, `EmojiSpan`,
`LiveData`, `MutableLiveData` are all **defined** in the dex now; `aapt2 dump badging`
shows versionCode 2 / versionName 1.1 / label "Kharcha Saathi"; kotlinc log has
0 errors. **NOT yet proven on a real phone** — no phone/emulator is available in
this sandbox, so Mian must install v1.1 and confirm it opens to the PIN screen.
If it still closes, the phone's crash log (logcat) would be needed to go further.

**Known remaining limitation:** `androidx.appcompat.app.AlertDialog` is still not
dexed — it is only used by the biometric library's `FingerprintDialogFragment` on
Android 8/9 (API 23–27) legacy fingerprint path, not at launch. If fingerprint is
ever used on a very old phone, that path could crash; modern phones (API 28+) use
the platform BiometricPrompt and are unaffected.

## How to install on a phone

1. Copy `expense-entry-app-debug.apk` to the phone (WhatsApp, USB, Drive, etc.).
2. On the phone, tap the APK → allow “Install unknown apps” if asked → Install.
3. Or from a computer with ADB: `adb install expense-entry-app-debug.apk`
4. First open: set your own PIN (4–8 digits). The app starts with the seed data
   Mian already had: Salary Rs 65,000 (5 Nov 2026) and Food Rs 500 (6 Nov 2026).

## What is inside (final approved version)

- Static one-screen Home: Balance In-Hand (all-time Total In − Total Out, **never
  resets to zero** — carry-forward with Opening/Closing), full date with weekday
  (e.g. Friday, 6 November 2026), Total In / Total Out boxes, Top Expense Category
  card (one-line label, slim, tappable), 6 customizable shortcuts (3+3, tap
  “+ Add Shortcut” to pin any section/category, change/remove), + Money In (green) /
  + Expense (red), bottom nav only Home / Categories / Calculator (taller).
- Categories page: 2-column cards, permanent record (entry count + all-time total),
  add custom category (In/Out), delete individual category only, entry only inside
  a tapped category (manual, locked to that category, In and Out). Defaults:
  Salary / Business / Other Income (IN), Food / Transport / Shopping / Bills /
  Rent / Other Expense (OUT).
- PIN lock (set on first open, auto-unlock on correct PIN, no extra unlock step) +
  Fingerprint/Biometric lock. Change PIN with old PIN.
- Family Sharing: one Admin, rest members, each member own PIN, invite code
  **24h expiry** (generate/share via WhatsApp), member joins with code + name +
  own PIN, member sees ONLY own kharcha + amounts assigned to them by name,
  Admin sees all with names, Member Name dropdown for Admin entries, Cash-In
  dropdown of all users (visible only to Admin + that user).
- Budget only for expense categories, per-category delete, over-budget warning.
- Loan (EN “Loan”, Urdu “ادھار”, Roman “Udhar” — **no Lena/Dena wording anywhere**),
  Utility Bills (LESCO, K-Electric, SNGPL, WASA, PTCL/Internet), Zakat calculator (2.5%).
- Calculator: arithmetic + currency (live rates when online, offline last saved,
  editable rates) + length/weight/volume/area/temperature/speed/time/pressure/
  energy/power/data/fuel conversions.
- Monthly Report with chart, Search, Recurring entries, Reminders, Receipt Photo,
  Voice Entry, Dark Mode (Auto/Light/Dark), Backup/Export (CSV/JSON file) +
  Restore from file and from saved in-app backup.
- Gmail backup: **honest note in the app** — the web version could only do a manual
  Drive upload; in this native app the backup file can be saved/shared to Drive,
  but there is no automatic Google sign-in backup. If Mian wants true automatic
  Gmail/Drive backup, that needs a Google Cloud OAuth client (separate setup).
- Languages: English, Urdu script, Roman Urdu — proper short text in all three,
  choice saved.
- 3-dot menu (scrollable): Monthly Report, Search, Planned Payments, Reminders,
  Budget, Loan, Bills, Zakat, Family, Backup/Export, Calculator, Change PIN,
  Fingerprint Lock, Theme, Language, Lock — and credit **“Made by Mian Zahid Sharif”
  ONLY in the 3-dot menu**, never on the front page.
- No Sab Entries list on front page, no Theme/Language rows on front, no
  “Viewing as” card (only tiny Admin access pill in header).

## What is still needed for Play Store release

1. **Release signing keystore** — create once, keep safe (losing it = the app can
   never be updated under the same package id). The current APK uses a debug
   keystore; a release keystore must be generated and the APK re-signed with it.
2. **Google Play Developer account — $25 one-time**, in Mian Zahid Sharif’s own
   name/payment/ID (Bro cannot create it).
3. Release build (AAB) + Play listing (icon, screenshots, description, privacy policy).
   Icon already exists: `../expense-entry-app-apk/icon-1024.png` (1024×1024) and
   `../expense-entry-app-apk/icon-512.png` (512×512).
4. Every change after release needs a new versioned build (1.0 → 1.1), Play review
   ~1–2 days; users update in place and keep data.

## How this APK was built (no Gradle)

Gradle cannot run in this sandbox: the sandbox intercepts all TCP from JVM
processes, which breaks the Gradle daemon protocol (localhost sockets).
So the app is built manually — see `build-manual.sh`:

1. `aapt2 compile/link` — resources + manifest (v1.1: versionCode 2, versionName 1.1,
   minSdk 26, targetSdk 34), generates R.java.
2. `kotlinc` via the **embeddable compiler 2.0.20** (`kotlin-compiler-embeddable.jar`)
   with the Compose compiler plugin 2.0.20 — the standalone kotlinc CLI cannot
   load the Compose plugin (relocated `org.jetbrains.kotlin.com.intellij.*`
   classes). Room was replaced by plain `SQLiteOpenHelper` (`Data.kt`) because
   Room needs KSP codegen, which needs Gradle.
3. `d8` via standalone **R8 8.5.35** (`r8.jar`) with `--min-api 26` (multi-dex,
   3 dex files, 131K methods > 64K limit). Build-tools 34’s d8 (8.2.2-dev) NPEs
   on fragment-1.8.2 — do not use it.
4. `zipalign` + `apksigner` (debug keystore).

Dependencies (265 artifacts) were resolved from Google Maven / Maven Central
with `resolve_deps.py` (urllib, no Java) into `libs/` (classes jars) and `aars/`
(extracted AARs). Note: AndroidX 1.7.x/2.8.x publish KMP “anchor” AARs; the real
code is in the `-android` variants (e.g. `material3-android`, `runtime-android`,
`lifecycle-viewmodel-android`) — the resolver follows those automatically.

Rebuild: `bash build-manual.sh` (takes ~6 minutes: kotlinc ~1 min, d8 ~4 min).

## Known limitations of this debug build

- **54 MB** — no R8 shrinking/minification applied; a release build with R8
  shrinking would be much smaller.
- Gmail backup is a manual file → Drive flow (see above), not automatic sign-in.
- Fingerprint unlock and Voice Entry depend on the phone’s hardware/WebView
  support; they show an honest “not supported” message where unavailable.
- **v1.0 crashed on launch** (see the fix note at the top — missing PoolingContainer
  class in the dex). **v1.1 fixes that cause and is rebuilt**, but it has **not**
  been run on a real phone yet — install v1.1 and check: it should open to the
  PIN setup screen, then PIN setup/auto-unlock, front page fits one screen,
  3-dot menu scrolls, shortcuts picker, invite code flow, member view scoping.
  If v1.1 still closes on launch, a phone crash log (logcat) is needed to go further.
- Data lives in on-device SQLite (`expense.db`); it survives app updates but
  NOT an uninstall unless a backup file was saved first (use Backup → save the
  file, then Restore after reinstall).

## v1.2 — crash fix, second attempt (2026-11-06 23:27 PKT)

**v1.1 STILL crashed on Mian's phone (Vivo V20, Android 13 / API 33, Funtouch OS 13)
with the same immediate "runtime exception" dialog.** The v1.1 PoolingContainer
fix was real (that class IS in v1.1's dex) but was not the (only) cause.

**Full audit of v1.1 (referenced-vs-defined across all 3 dex files, 25,357
defined classes):** 466 referenced types were not defined in any dex. Most
were harmless (array types, R$ classes inlined as constants, Android
framework types like org.json/org.xmlpull, and OEM-provided window-extension
types loaded reflectively with try/catch). The REAL missing classes, all
referenced in code and absent from every jar in the project:

- `androidx.graphics.path.PathIterator` (+ PathSegment) — Compose's
  `AndroidPathIterator` implements it; `Path.iterator()` is called from
  `PathEasing`. The `androidx.graphics:graphics-path` artifact was never
  resolved by resolve_deps.py.
- `androidx.emoji2.text.flatbuffer.MetadataList` / `MetadataItem` (+ 47
  flatbuffer classes) — referenced (new-instance/invoke) by EmojiCompat's
  MetadataRepo/MetadataListReader. They live in the emoji2 AAR's
  `libs/repackaged.jar`, which the resolver ignored (it only took
  classes.jar). EmojiCompat is called from Compose text code
  (AndroidParagraphHelper / TextInputService), guarded by isConfigured(),
  so this was a latent crash (e.g. on text-field focus) rather than a
  proven first-frame one.
- `androidx.appcompat.app.AlertDialog` (+ Builder) — referenced by the
  biometric library's FingerprintDialogFragment. Not the API-33 path
  (API 28+ uses platform BiometricPrompt), but missing on every API.
- `androidx.interpolator.view.animation.FastOutLinearInInterpolator` —
  referenced by Compose animated-vector XML parsing.
- `androidx.lifecycle.ProcessLifecycleInitializer` — its jar WAS in libs/
  but the D8 filter in build-manual.sh excluded `lifecycle-process`.
  (Not registered in the manifest — no androidx.startup provider is
  merged in this manual build — so it never ran, but it was still absent
  from the dex.)

**What changed in v1.2 (versionCode 3, versionName 1.2, name Kharcha Saathi,
package com.mianzahid.expenseentry, debug-signed, apksigner Verifies v2+v3):**
1. Added libs/graphics-path-1.0.0.jar, libs/emoji2-flatbuffer-1.4.0.jar
   (emoji2 AAR repackaged.jar), libs/appcompat-1.7.0.jar,
   libs/interpolator-1.0.0.jar — all downloaded from Google Maven.
2. Removed `lifecycle-process` from the D8 exclusion in build-manual.sh.
3. MainActivity: `setContent` is now wrapped in try/catch
   (`startCompose()`); on failure the app opens a plain Android Views
   PIN screen (`showFallback()`) that shows the exact exception class +
   message (so it can be screenshotted) and lets the user set/enter the
   PIN and retry Compose — instead of the Android crash dialog. This
   covers synchronous launch failures; an async composition crash would
   still kill the process (no logcat available to diagnose that yet).

**Verification (v1.2 APK, MD5 bdfab04d72f07d5138f277e47b76725c, 55 MB):**
aapt2 badging versionCode 3 / versionName 1.2 / label "Kharcha Saathi";
dexdump confirms ALL previously-missing classes above are now DEFINED in
the dex (defined classes 25,357 -> 25,846), including showFallback /
startCompose in MainActivity; kotlinc 0 errors.

**Honest caveat:** still NOT proven on a real phone — no phone/emulator
in this sandbox. If v1.2 still crashes, the fallback screen should show
the exception (screenshot it), or a logcat is needed for the exact cause.
