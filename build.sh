#!/usr/bin/env bash
# 老友桌面 OldFriend Launcher - build script (no Gradle)
# aapt2 compile -> aapt2 link -> javac -> d8 -> zipalign -> apksigner
# usage: bash build.sh
set -e

cd "$(cd "$(dirname "$0")" && pwd)"

SDK="${ANDROID_SDK:-E:/AndroidSDK}"
BT="$SDK/build-tools/34.0.0"
P19="$SDK/platforms/android-19/android.jar"
P34="$SDK/platforms/android-34/android.jar"
JAVA_DIR="${JAVA_HOME:-C:/Java/jdk-26.0.2+10}"
JAVAC="$JAVA_DIR/bin/javac"
KEYTOOL="$JAVA_DIR/bin/keytool"
JAVA="$JAVA_DIR/bin/java"
if [ -f "$SDK/cmdline-tools/latest/lib/r8.jar" ]; then
  D8JAR="$SDK/cmdline-tools/latest/lib/r8.jar"
else
  D8JAR="$BT/lib/d8.jar"
fi
PY="C:/Users/LCJ/.workbuddy/binaries/python/versions/3.13.12/python.exe"

NAME="OldFriendLauncher"
OUT="build"
KS="keystore/oldfriend.jks"
KSPASS="oldfriend2026"
ALIAS="oldfriend"
VERCODE=5
VERNAME=2.2.0

echo "==> 0/7 clean"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

echo "==> 1/7 aapt2 compile (res)"
"$BT/aapt2.exe" compile --dir res -o "$OUT/res.zip"

echo "==> 2/7 aapt2 link"
"$BT/aapt2.exe" link \
  -o "$OUT/base.apk" \
  -I "$P34" \
  --manifest AndroidManifest.xml \
  -R "$OUT/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 19 \
  --target-sdk-version 28 \
  --version-code $VERCODE \
  --version-name $VERNAME \
  --auto-add-overlay

echo "==> 3/7 javac"
find src -name '*.java' > "$OUT/sources.txt"
"$JAVAC" --release 8 -encoding UTF-8 -Xlint:-options \
  -classpath "$P34" -d "$OUT/classes" "@$OUT/sources.txt"

echo "==> 4/7 d8 (min-api 19)"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$JAVA" -cp "$D8JAR" com.android.tools.r8.D8 \
  --min-api 19 --lib "$P34" --output "$OUT/dex" "@$OUT/classes.txt"

echo "==> 5/7 add classes.dex into apk"
"$PY" - "$OUT/base.apk" "$OUT/dex/classes.dex" <<'PY'
import sys, zipfile, shutil, os
apk, dex = sys.argv[1], sys.argv[2]
tmp = apk + ".tmp"
shutil.copyfile(apk, tmp)
with zipfile.ZipFile(tmp, "a", zipfile.ZIP_DEFLATED) as z:
    z.write(dex, "classes.dex")
os.replace(tmp, apk)
print("    classes.dex =", os.path.getsize(dex), "bytes")
PY

echo "==> 6/7 zipalign"
"$BT/zipalign.exe" -f -p 4 "$OUT/base.apk" "$OUT/aligned.apk"

echo "==> 7/7 apksigner"
if [ ! -f "$KS" ]; then
  mkdir -p "$(dirname "$KS")"
  "$KEYTOOL" -genkeypair -v -keystore "$KS" -alias "$ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 -storetype PKCS12 \
    -storepass "$KSPASS" -keypass "$KSPASS" \
    -dname "CN=OldFriend Launcher,O=LCJ,C=CN" >/dev/null 2>&1
  echo "    keystore created: $KS"
fi
"$JAVA" -jar "$BT/lib/apksigner.jar" sign \
  --ks "$KS" --ks-pass "pass:$KSPASS" --key-pass "pass:$KSPASS" \
  --min-sdk-version 19 --v1-signing-enabled true --v2-signing-enabled true \
  --out "$NAME-v$VERNAME.apk" "$OUT/aligned.apk"

echo "==> verify"
"$JAVA" -jar "$BT/lib/apksigner.jar" verify --min-sdk-version 19 --print-certs "$NAME-v$VERNAME.apk" | head -6
"$BT/aapt2.exe" dump badging "$NAME-v$VERNAME.apk" | head -5
ls -l "$NAME-v$VERNAME.apk"
echo "==> DONE"
