# Hisaboo — Khud APK banane ka asan tareeqa (GitHub Actions)

Ye zip me Hisaboo v1.2 ka poora source + GitHub Actions workflow hai.
Tumhe apne computer pe kuch install nahi karna — APK GitHub ke free server pe banega.

## Kya theek kiya gaya hai (taake error na aaye)
- **Room database hata diya:** app Room use nahi karta (SQLite use karta hai), lekin
  Room ki lines build me KSP error deti thin — wo 3 lines + KSP plugin nikaal diye.
- **Gradle wrapper ki zaroorat nahi:** workflow khud Gradle 8.7 download karta hai.
- **SDK khud ready hota hai:** workflow Android API 34 + build-tools khud install karta hai.
- Workflow me har step ka naam Roman Urdu/English me likha hai taake samajh aaye.

## Steps (sirf pehli dafa)
1. GitHub pe jao (github.com), sign in karo.
2. **New repository** banao — naam kuch bhi, jaise `hisaboo`. Public rakhna.
3. Is zip ko apne computer pe **extract** karo (folder milega `hisaboo`).
4. GitHub repo page pe **"uploading an existing file"** (ya Add file > Upload files) dabao,
   aur extract kiye hue folder ki **saari files + folders** (`.github` folder samet) drag-drop karo.
   (`.github` chhupa hua folder hai — agar nazar na aaye to "show hidden files" on karo.
   Ye folder sab se zaroori hai, is ke baghair APK nahi banega.)
5. Neeche **Commit changes** dabao.

## APK banana (har dafa)
1. Repo page pe upar **Actions** tab kholo.
2. Left side **"Build Hisaboo APK"** select karo.
3. Right side **"Run workflow"** dabao, phir green **"Run workflow"** button dabao.
4. 3–6 minute wait karo — build chalne lagega (peeli dot, phir green tick).
5. Build complete hone pe page neeche scroll karo — **Artifacts** me
   **"Hisaboo-v1.2-debug"** milega. Us pe click kar ke zip download karo.
6. Zip extract karo — andar `app-debug.apk` milega. Phone pe bhejo aur install karo.

## Agar build fail ho jaye
- Actions wale run pe click karo, jo step laal (red) hai usko kholo, aur error ka
  screenshot bhej do — main theek kar dunga. Ghabrana nahi, ye normal hai.

## Phone pe install
1. APK phone pe bhejo (WhatsApp / USB / Drive).
2. APK pe tap karo → "Install unknown apps" allow karo agar pooche → Install.
3. Pehli dafa kholo: apna PIN set karo (4–8 digits).

**Note:** ye debug APK hai (Play Store wala release nahi). Version 1.2, package
`com.mianzahid.expenseentry`, Android 8.0+ chahiye.
