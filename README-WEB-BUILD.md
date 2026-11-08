# Hisaboo — Web se APK banane ka tareeqa

Ye zip me poora Android source code hai (Kotlin + Jetpack Compose).
Package: com.mianzahid.expenseentry | Version 1.2 | Name: Hisaboo

## Tareeqa 1 — Codemagic (sab se aasan, web pe, free tier hai)
1. Is zip ko GitHub pe upload karo (naya repository banao, zip extract kar ke files daalo).
2. https://codemagic.io pe jao, GitHub se sign in karo, repository select karo.
3. Workflow: Android, Build type: Debug APK. Build dabao.
4. Build complete hone pe APK download kar lo.

## Tareeqa 2 — GitHub Actions (free)
1. GitHub repository banao, ye files daalo.
2. GitHub pe Actions > New workflow > "Android CI" select karo.
3. Workflow run karo — APK Artifacts me milegi.

## Tareeqa 3 — Android Studio (computer pe)
1. https://developer.android.com/studio se Android Studio download karo.
2. Is zip ko extract kar ke Android Studio me Open karo.
3. Build > Build App Bundle(s) / APK(s) > Build APK(s). APK app/build/outputs/apk/debug/ me milegi.

## Zaroori baat
- Pehle `gradle.properties` me koi SDK path nahi hai — web builder khud SDK download karega.
- Agar Room/KSP error aaye to: app/build.gradle.kts me room wali 3 lines (room-runtime, room-ktx, ksp room-compiler) hata do — app Room use nahi karta, SQLite use karta hai.
- Debug APK hi banao pehle — Play Store ke liye release signing alag se chahiye.
