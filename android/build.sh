#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SDK=/opt/asdk
BT=$SDK/bt/android-15
PLATFORM=$SDK/pf/android-35/android.jar
OUT=build
PKG=com.le1567.spring
APK_NAME=spring-festival-countdown.apk

rm -rf $OUT gen classes dex
mkdir -p $OUT gen classes dex assets
cp ../spring-festival-countdown.html assets/index.html
cp ../bgm.mp3 assets/bgm.mp3

echo "[1/7] aapt2 compile"
$BT/aapt2 compile --dir res -o $OUT/res.zip

echo "[2/7] aapt2 link"
$BT/aapt2 link -o $OUT/base.apk \
  -I "$PLATFORM" \
  --manifest AndroidManifest.xml \
  -A assets \
  --java gen \
  --min-sdk-version 24 \
  --target-sdk-version 35 \
  --version-code 4 \
  --version-name 1.3.0 \
  $OUT/res.zip

echo "[3/7] javac"
find java gen -name '*.java' > $OUT/sources.txt
javac --release 17 -encoding UTF-8 -nowarn \
  -classpath "$PLATFORM" \
  -d classes @$OUT/sources.txt

echo "[4/7] d8"
find classes -name '*.class' > $OUT/classes.txt
java -cp $BT/lib/d8.jar com.android.tools.r8.D8 \
  --min-api 24 --lib "$PLATFORM" --release \
  --output dex @$OUT/classes.txt

echo "[5/7] package"
cp $OUT/base.apk $OUT/unsigned.apk
(cd dex && zip -q -X ../$OUT/unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $OUT/unsigned.apk $OUT/aligned.apk

echo "[6/7] keystore"
KS=keystore.jks
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v -keystore "$KS" -storepass springfest -keypass springfest \
    -alias spring -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Spring Countdown, OU=App, O=le1567, L=Shanghai, C=CN" >/dev/null 2>&1
fi

echo "[7/7] sign"
$BT/apksigner sign --ks "$KS" --ks-pass pass:springfest --key-pass pass:springfest \
  --ks-key-alias spring --out $OUT/$APK_NAME $OUT/aligned.apk
$BT/apksigner verify --print-certs $OUT/$APK_NAME | head -4

cp $OUT/$APK_NAME ../$APK_NAME
ls -lh ../$APK_NAME
echo "OK -> ../$APK_NAME"
