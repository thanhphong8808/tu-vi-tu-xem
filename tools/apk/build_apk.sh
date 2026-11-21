#!/usr/bin/env bash
# Build file APK cài lên Android mà KHÔNG cần Android Studio / Gradle / Android SDK của Google:
#   kotlinc → dx (dalvik-exchange) → aapt → zipalign → apksigner.
#
# Cần (Ubuntu 24.04):  sudo apt-get install aapt zipalign dalvik-exchange apksigner
#                      JDK 17+, kotlinc 2.0.x, Python 3
#   ANDROID_JAR_26, ANDROID_JAR_35: android.jar API 26 và 35 (vd. từ github.com/Sable/android-platforms)
#   SERIALIZATION_JARS: kotlinx-serialization-core-jvm + json-jvm (1.6.x), phân tách bằng “:”
# Biến tùy chọn: KOTLINC, KOTLIN_STDLIB, OUT, KEYSTORE, KS_PASS.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/../.." && pwd)
# Công cụ do tools/setup/cloud_setup.sh cài (môi trường mới), rồi tới cấu hình riêng của máy (nếu có).
[ -f "${TUVI_TK:-/opt/tuvi-tk}/env.sh" ] && source "${TUVI_TK:-/opt/tuvi-tk}/env.sh"
ENV_FILE="$ROOT/tools/apk/env.local.sh"
[ -f "$ENV_FILE" ] && source "$ENV_FILE"

: "${KOTLINC:=kotlinc}"
: "${ANDROID_JAR_26:?Thiếu ANDROID_JAR_26}"
: "${ANDROID_JAR_35:?Thiếu ANDROID_JAR_35}"
: "${SERIALIZATION_JARS:?Thiếu SERIALIZATION_JARS}"
: "${KOTLIN_STDLIB:?Thiếu KOTLIN_STDLIB}"
: "${OUT:=$ROOT/app/build}"
: "${KEYSTORE:=$ROOT/app/keystore/tuvi-tu-xem.jks}"
: "${KS_PASS:=tuvituxem}"

VERSION_NAME=$(sed -n 's/.*APP_VERSION = "\([^ "]*\).*/\1/p' "$ROOT/app/src/main/kotlin/vn/tuvi/app/MainActivity.kt")
VERSION_CODE=$(echo "$VERSION_NAME" | awk -F. '{ printf "%d", $1*10000 + $2*100 + $3 }')
APK_NAME="TuViTuXem-${VERSION_NAME}.apk"

KFLAGS=(-no-reflect -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class -Xstring-concat=inline -nowarn)
step() { printf '\n== %s\n' "$*"; }

rm -rf "$OUT"
mkdir -p "$OUT/classes/core" "$OUT/classes/app" "$OUT/libs" "$OUT/dex" "$OUT/javares"

step "1/7 Biên dịch engine + luận giải + cầu nối (:core:calendar, :core:engine, :core:interpret, app/src/shared) với android.jar API 26 (minSdk)"
"$KOTLINC" "${KFLAGS[@]}" -no-jdk -classpath "$ANDROID_JAR_26:$SERIALIZATION_JARS" -d "$OUT/classes/core" \
  $(find "$ROOT/core/calendar/src/main/kotlin" "$ROOT/core/engine/src/main/kotlin" "$ROOT/core/interpret/src/main/kotlin" "$ROOT/app/src/shared/kotlin" -name '*.kt')
cp "$ROOT/core/engine/src/main/resources/brightness.json" "$OUT/javares/"
# Knowledge base (giai đoạn 3): đọc bằng Class.getResourceAsStream("/kb/…") như trên JVM.
cp -r "$ROOT/data/kb/src/main/resources/kb" "$OUT/javares/kb"

step "2/7 Biên dịch Activity với android.jar API 35 (targetSdk; API mới đều có kiểm tra phiên bản)"
"$KOTLINC" "${KFLAGS[@]}" -no-jdk -classpath "$ANDROID_JAR_35:$OUT/classes/core:$SERIALIZATION_JARS" -d "$OUT/classes/app" \
  $(find "$ROOT/app/src/main/kotlin" -name '*.kt')

step "3/7 Chuẩn bị thư viện cho Android (bỏ module-info; thay 5 hàm dùng invokedynamic của kotlin-stdlib)"
KLIB_DIR=$(dirname "$KOTLINC")/../lib
mkdir -p "$OUT/tools" "$OUT/classes/indyfix"
javac -nowarn --release 8 -cp "$KOTLIN_STDLIB" -d "$OUT/classes/indyfix" "$ROOT/tools/apk/indyfix/src/vn/tuvi/indyfix/Comparators.java"
javac -nowarn -cp "$KLIB_DIR/kotlin-compiler.jar" -d "$OUT/tools" "$ROOT/tools/apk/indyfix/src/vn/tuvi/indyfix/StripIndy.java"
for jar in "$KOTLIN_STDLIB" ${SERIALIZATION_JARS//:/ }; do
  java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "$KLIB_DIR/kotlin-compiler.jar:$OUT/tools" vn.tuvi.indyfix.StripIndy "$jar" "$OUT/libs/$(basename "$jar")"
done

step "4/7 Chuyển bytecode sang DEX (dx, minSdk 26)"
dalvik-exchange --dex --min-sdk-version=26 --output="$OUT/dex/classes.dex" \
  "$OUT/classes/core" "$OUT/classes/app" "$OUT/classes/indyfix" "$OUT"/libs/*.jar
ls -l "$OUT/dex/classes.dex"
if command -v dexdump >/dev/null; then
  BAD=$(dexdump -d "$OUT/dex/classes.dex" 2>/dev/null | grep -cE "invoke-(custom|polymorphic)" || true)
  [ "$BAD" = "0" ] || { echo "LỖI: DEX còn $BAD lệnh invoke-custom/polymorphic (Android không chạy được)"; exit 3; }
  echo "   kiểm tra DEX: 0 invoke-custom/polymorphic"
fi

# aapt 1 không đọc được resources.arsc của API 35 → liên kết tài nguyên với API 26 (ID tài nguyên framework không đổi).
step "5/7 Đóng gói manifest, tài nguyên, giao diện web (aapt): bản phát hành + bản tự kiểm tra"
# Bản tự kiểm tra = bản phát hành + android:debuggable + assets/web/selftest.js (4 ca golden nhúng sẵn).
rm -rf "$OUT/assets-selftest"; cp -r "$ROOT/app/assets" "$OUT/assets-selftest"
python3 - "$ROOT/tools/apk/selftest/selftest.js" "$ROOT/core/engine/src/test/resources/golden.json" "$OUT/assets-selftest/web/selftest.js" <<'PY2'
import sys, json
tpl, golden, dst = sys.argv[1:]
g = json.dumps(json.load(open(golden, encoding='utf-8')), ensure_ascii=False)
open(dst, 'w', encoding='utf-8').write(open(tpl, encoding='utf-8').read().replace('/*__GOLDEN__*/[]', g))
PY2
package() { # $1 = tên, $2 = thư mục assets, $3.. = cờ aapt thêm
  local name=$1 assets=$2; shift 2
  aapt package -f -M "$ROOT/app/AndroidManifest.xml" -S "$ROOT/app/res" -A "$assets" -I "$ANDROID_JAR_26" \
    --min-sdk-version 26 --target-sdk-version 35 --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" "$@" \
    -F "$OUT/$name-unsigned.apk"
  (cd "$OUT/dex" && aapt add -k "$OUT/$name-unsigned.apk" classes.dex >/dev/null)
  (cd "$OUT/javares" && aapt add "$OUT/$name-unsigned.apk" brightness.json $(find kb -type f -name '*.json' | sort) >/dev/null)
  zipalign -p -f 4 "$OUT/$name-unsigned.apk" "$OUT/$name-aligned.apk"
}
package release "$ROOT/app/assets"
package selftest "$OUT/assets-selftest" --debug-mode

step "6/7 Kiểm tra lời gọi API hệ thống trong DEX (android.jar API 35)"
python3 "$ROOT/tools/apk/check_api.py" "$OUT/dex/classes.dex" "$ANDROID_JAR_35"

step "7/7 Ký (apksigner v1 + v2 + v3)"
# Khóa ký thật (D-31) không có trên GitHub. Ở môi trường mới, khóa lấy từ biến TUVI_KEYSTORE_B64 (xem
# docs/BAN_GIAO_MOI_TRUONG_MOI.md). Thiếu khóa thật thì DỪNG: APK ký bằng khóa khác không cài đè được lên app
# đã cài trên điện thoại. Chỉ CI (bản tạm, không phát hành) hoặc ALLOW_NEW_KEY=1 mới được ký bằng khóa tạm.
if [ ! -f "$KEYSTORE" ]; then
  bash "$ROOT/tools/setup/restore_keystore.sh" "$KEYSTORE" || true
fi
if [ ! -f "$KEYSTORE" ]; then
  if [ "${CI:-}" = "true" ] || [ "${ALLOW_NEW_KEY:-}" = "1" ]; then
    mkdir -p "$(dirname "$KEYSTORE")"
    keytool -genkeypair -keystore "$KEYSTORE" -alias tuvi -keyalg RSA -keysize 2048 -validity 10950 \
      -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=Tu Vi Tu Xem, O=Thanh Phong, C=VN" >/dev/null 2>&1
    echo "   CẢNH BÁO: ký bằng khóa TẠM ($KEYSTORE) — APK này không cài đè được lên app thật, không đưa vào dist/"
  else
    echo "LỖI: thiếu khóa ký thật $KEYSTORE. Đặt biến TUVI_KEYSTORE_B64 hoặc chép file khóa vào đó"
    echo "     (docs/BAN_GIAO_MOI_TRUONG_MOI.md, mục 3). Chỉ để thử: ALLOW_NEW_KEY=1 bash tools/apk/build_apk.sh"
    exit 4
  fi
else
  bash "$ROOT/tools/setup/restore_keystore.sh" --verify "$KEYSTORE" || {
    [ "${CI:-}" = "true" ] || [ "${ALLOW_NEW_KEY:-}" = "1" ] || exit 4; }
fi
sign() { # $1 = tên trung gian, $2 = file ra
  apksigner sign --ks "$KEYSTORE" --ks-key-alias tuvi --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$OUT/$2" "$OUT/$1-aligned.apk"
  apksigner verify "$OUT/$2"
  rm -f "$OUT/$2.idsig"
}
sign release "$APK_NAME"
sign selftest "${APK_NAME%.apk}-selftest.apk"
ls -l "$OUT"/*.apk | grep -v -- '-unsigned\|-aligned'
echo "APK: $OUT/$APK_NAME"
