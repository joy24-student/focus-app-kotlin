#!/bin/bash
set -e

PROJECT_DIR="/data/data/com.termux/files/home/focus-android"
SDK_JAR="/data/data/com.termux/files/home/android-sdk/android.jar"
KOTLIN_STDLIB="/data/data/com.termux/files/usr/opt/kotlin/lib/kotlin-stdlib.jar"
KEYSTORE="/data/data/com.termux/files/home/debug.keystore"

cd "$PROJECT_DIR"
rm -rf build
mkdir -p build/gen build/compiled build/classes build/dex

echo "==> 1. Compiling Android Resources with AAPT2..."
aapt2 compile --dir app/src/main/res -o build/compiled/res.zip

echo "==> 2. Linking Resources, Assets and generating R.java (Target SDK 34, Min SDK 24)..."
aapt2 link -I "$SDK_JAR" \
    -A app/src/main/assets \
    --manifest app/src/main/AndroidManifest.xml \
    --min-sdk-version 24 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0.0" \
    -o build/unaligned.apk \
    --java build/gen \
    build/compiled/res.zip

echo "==> 3. Compiling Kotlin and Java sources..."
SOURCES=$(find app/src/main/java build/gen -name "*.kt" -o -name "*.java")
kotlinc -cp "$SDK_JAR" $SOURCES -d build/classes

echo "==> 4. Dexing bytecode with D8 (min-api 24)..."
CLASSES=$(find build/classes -name "*.class")
d8 $CLASSES "$KOTLIN_STDLIB" --min-api 24 --output build/dex --lib "$SDK_JAR"

echo "==> 5. Packaging APK..."
cp build/unaligned.apk build/app.apk
(cd build/dex && zip -u ../app.apk classes.dex)

echo "==> 6. Aligning APK with zipalign..."
zipalign -f -p 4 build/app.apk build/app-aligned.apk

echo "==> 7. Signing APK with apksigner (v1, v2, v3)..."
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkey -v -keystore "$KEYSTORE" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=FocusMood,O=FocusMood,C=US"
fi

apksigner sign --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --ks-key-alias androiddebugkey \
    --key-pass pass:android \
    --out FocusMood.apk \
    build/app-aligned.apk

echo "==> 8. Verifying APK signature..."
apksigner verify -v FocusMood.apk

echo "==> 9. Copying APK to output locations..."
cp FocusMood.apk /data/data/com.termux/files/home/FocusMood.apk
if [ -d "/storage/emulated/0/Download" ]; then
    cp FocusMood.apk /storage/emulated/0/Download/FocusMood.apk
    echo "Successfully copied FocusMood.apk to /storage/emulated/0/Download/FocusMood.apk"
fi

echo "============================================================"
echo " BUILD SUCCESSFUL! Direct APK generated at:"
echo " - /data/data/com.termux/files/home/FocusMood.apk"
echo " - /storage/emulated/0/Download/FocusMood.apk"
echo "============================================================"
