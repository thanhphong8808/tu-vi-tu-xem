#!/usr/bin/env bash
# Chạy thử APK trên máy ảo Android (gọi từ .github/workflows/android-emulator.yml).
# 1) Bản phát hành (đúng file gửi người dùng): cài, mở, chờ 20 giây, kiểm tra tiến trình còn sống, không có FATAL.
# 2) Bản tự kiểm tra: mở với extra "selftest" → selftest.js nhập 4 ca golden qua giao diện thật, gọi engine thật.
set -u
API=${1:-?}
PKG=vn.tuvi.tuxem
ACT=vn.tuvi.app.MainActivity
REL=$(ls dist/TuViTuXem-*.apk | grep -v selftest | head -1)
ST=$(ls dist/TuViTuXem-*-selftest.apk | head -1)
OUT=out
mkdir -p "$OUT"
FAIL=0
emit_png() { echo "BEGIN_PNG $1"; base64 -w 0 "$2" | fold -w 3000; echo; echo "END_PNG"; }

echo "== Máy ảo: Android $(adb shell getprop ro.build.version.release | tr -d '\r') · API $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
adb shell dumpsys webviewupdate 2>/dev/null | grep -m2 -iE "Current WebView package|versionName" || true
echo "== APK: $REL · $ST"

OLD=$(ls ci/regression/TuViTuXem-*.apk 2>/dev/null | head -1)
if [ -n "$OLD" ]; then
  echo "== 0) Bản cũ người dùng đã cài ($OLD) — chỉ để xác nhận nguyên nhân lỗi, không tính vào kết luận"
  adb install -r "$OLD" >/dev/null && adb logcat -c && adb shell am start -W -n "$PKG/$ACT" >/dev/null; sleep 12
  if [ -n "$(adb shell pidof "$PKG" | tr -d '\r')" ]; then echo "OLD_APK_ALIVE"; else echo "OLD_APK_DEAD"; fi
  adb logcat -d | grep -E "FATAL EXCEPTION|NullPointerException|at vn\.tuvi" | head -8
  adb shell am force-stop "$PKG"
fi

echo "== 1) Bản phát hành (cài đè lên bản cũ như người dùng sẽ làm)"
adb install -r "$REL" || { echo "CAI_DAT_THAT_BAI"; exit 1; }
adb logcat -c
adb shell am start -W -n "$PKG/$ACT"
sleep 20
PID=$(adb shell pidof "$PKG" | tr -d '\r')
adb exec-out screencap -p > "$OUT/1-release-api$API.png"
adb logcat -d > "$OUT/logcat-release-api$API.txt"
grep -nE "FATAL EXCEPTION|AndroidRuntime|TuViTuXem|TuViWeb|Process: $PKG|VerifyError|NoClassDefFound|ClassNotFound" "$OUT/logcat-release-api$API.txt" | head -150
if [ -n "$PID" ]; then echo "RELEASE_ALIVE pid=$PID"; else echo "RELEASE_DEAD"; FAIL=1; fi
if grep -q "FATAL EXCEPTION" "$OUT/logcat-release-api$API.txt"; then echo "RELEASE_FATAL"; FAIL=1; fi
if grep -q "TuViWeb.*ERROR" "$OUT/logcat-release-api$API.txt"; then echo "RELEASE_JS_ERROR"; FAIL=1; fi
if grep -qE " [WE] TuViTuXem" "$OUT/logcat-release-api$API.txt"; then echo "RELEASE_APP_WARNING"; FAIL=1; fi
emit_png "release-api$API" "$OUT/1-release-api$API.png"

echo "== 2) Bản tự kiểm tra (4 ca golden trong app thật)"
adb shell am force-stop "$PKG"
adb install -r "$ST" || { echo "CAI_SELFTEST_THAT_BAI"; exit 1; }
adb shell pm clear "$PKG" >/dev/null
adb logcat -c
adb shell am start -W -n "$PKG/$ACT" --es selftest 1
for i in $(seq 1 150); do
  adb logcat -d -s TuViWeb:V 2>/dev/null | grep -q "TUVI_SELFTEST RESULT" && break
  [ -z "$(adb shell pidof "$PKG" | tr -d '\r')" ] && { echo "SELFTEST_PROCESS_DEAD"; break; }
  sleep 2
done
adb exec-out screencap -p > "$OUT/2-selftest-api$API.png"
adb logcat -d > "$OUT/logcat-selftest-api$API.txt"
grep -nE "TUVI_SELFTEST|FATAL EXCEPTION|AndroidRuntime|TuViTuXem|VerifyError|NoClassDefFound" "$OUT/logcat-selftest-api$API.txt" | head -250
RES=$(grep -o "TUVI_SELFTEST RESULT pass=[0-9]* fail=[0-9]*" "$OUT/logcat-selftest-api$API.txt" | tail -1)
echo "SELFTEST: ${RES:-KHONG_CO_KET_QUA}"
case "$RES" in *" fail=0") ;; *) FAIL=1 ;; esac
emit_png "selftest-api$API" "$OUT/2-selftest-api$API.png"
echo "== KẾT LUẬN API $API: $([ $FAIL = 0 ] && echo ĐẠT || echo LỖI)"
exit $FAIL
