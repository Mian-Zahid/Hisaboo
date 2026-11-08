#!/usr/bin/env bash
# Kharcha Saathi v1.2 — manual APK build (no Gradle: JVM networking + Gradle
# daemon are blocked in this sandbox). Uses aapt2 + kotlinc embeddable +
# R8 D8 + zipalign + apksigner, all local.
set -e
cd "$(dirname "$0")"

JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
SDK=~/workspace/android-sdk
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
DL=~/workspace/downloads
KOTLINC_JAR=$DL/kotlin-compiler-embeddable-2.0.20.jar
COMPOSE_PLUGIN=$DL/kotlin-compose-compiler-plugin-embeddable-2.0.20.jar
R8_JAR=$DL/r8-8.5.35.jar

AAPT2=$BT/aapt2
ZIPALIGN=$BT/zipalign
APKSIGNER=$BT/apksigner
DEXDUMP=$BT/dexdump

BUILD=build
rm -rf $BUILD
mkdir -p $BUILD/compiled $BUILD/gen $BUILD/classes $BUILD/dex

echo "== 1. aapt2 compile resources =="
$AAPT2 compile --dir app/src/main/res -o $BUILD/compiled.zip

echo "== 2. aapt2 link =="
$AAPT2 link -o $BUILD/app-unsigned.apk \
  --manifest app/src/main/AndroidManifest.xml \
  -I "$ANDROID_JAR" \
  -R $BUILD/compiled.zip \
  --java $BUILD/gen \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 3 --version-name 1.2

echo "== 3. kotlinc (embeddable 2.0.20 + compose plugin) =="
CP=$(ls libs/*.jar | tr '\n' ':')$ANDROID_JAR
# shellcheck disable=SC2086
"$JAVA_HOME/bin/java" -Xmx1g -cp "$KOTLINC_JAR" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -jvm-target 17 \
  -cp "$CP" \
  -d $BUILD/classes \
  -Xplugin="$COMPOSE_PLUGIN" \
  app/src/main/java/com/mianzahid/expenseentry/*.kt \
  $BUILD/gen/com/mianzahid/expenseentry/R.java

echo "== 4. d8 (R8 8.5.35) =="
"$JAVA_HOME/bin/java" -Xmx1g -cp "$R8_JAR" \
  com.android.tools.r8.D8 \
  --min-api 26 \
  --output $BUILD/dex \
  --lib "$ANDROID_JAR" \
  $BUILD/classes \
  libs/*.jar

echo "== 5. add dex to apk =="
cd $BUILD/dex
zip -q ../app-unsigned.apk classes*.dex
cd ../..

echo "== 6. zipalign =="
$ZIPALIGN -f 4 $BUILD/app-unsigned.apk $BUILD/app-aligned.apk

echo "== 7. keystore (fresh debug) =="
if [ ! -f debug.keystore ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore debug.keystore \
    -alias androiddebugkey -storepass android -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug, O=Android, C=US" -noprompt
fi

echo "== 8. apksigner =="
$APKSIGNER sign --ks debug.keystore --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey \
  --out $BUILD/expense-entry-app-debug.apk $BUILD/app-aligned.apk

echo "== 9. verify =="
$APKSIGNER verify --verbose $BUILD/expense-entry-app-debug.apk
$AAPT2 dump badging $BUILD/expense-entry-app-debug.apk | head -8
ls -la $BUILD/expense-entry-app-debug.apk
md5sum $BUILD/expense-entry-app-debug.apk
