#!/usr/bin/env bash
# Chạy thử APK trên máy ảo Android (gọi từ .github/workflows/android-emulator.yml).
# 1) Bản phát hành (đúng file gửi người dùng): cài, mở, chờ 20 giây, kiểm tra tiến trình còn sống, không có FATAL.
# 2) Bản tự kiểm tra: mở với extra "selftest" → selftest.js nhập 4 ca golden qua giao diện thật, gọi engine thật.
# 3) Giai đoạn 5 trên màn hình thật (adb): cây trợ năng (TalkBack đọc ô lá số), chạm giữ một sao bằng ngón tay ảo,
#    bảng chia sẻ PDF, tệp PDF/PNG đã lưu trong Tải xuống/Ảnh (kéo về, đếm trang, vẽ 3 trang đầu thành ảnh).
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
emit_file() { echo "BEGIN_FILE $1"; base64 -w 0 "$2" | fold -w 3000; echo; echo "END_FILE"; }

echo "== Máy ảo: Android $(adb shell getprop ro.build.version.release | tr -d '\r') · API $(adb shell getprop ro.build.version.sdk | tr -d '\r')"
adb shell dumpsys webviewupdate 2>/dev/null | grep -m2 -iE "Current WebView package|versionName" || true
echo "== APK: $REL · $ST"

OLD=$(ls ci/regression/TuViTuXem-*.apk 2>/dev/null | head -1)
if [ -n "$OLD" ]; then
  echo "== 0) Bản trước đang có trên máy người dùng ($OLD) — để bước 1 kiểm tra đường cập nhật (cài đè)"
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

echo "== 2) Bản tự kiểm tra (4 ca golden + luận giải trong app thật)"
adb shell am force-stop "$PKG"
adb install -r "$ST" || { echo "CAI_SELFTEST_THAT_BAI"; exit 1; }
run_selftest() { # $1 = lần chạy
  adb shell am force-stop "$PKG"
  adb shell pm clear "$PKG" >/dev/null
  adb logcat -c; adb logcat -b events -c 2>/dev/null
  adb shell am start -W -n "$PKG/$ACT" --es selftest 1
  for i in $(seq 1 150); do
    adb logcat -d -s TuViWeb:V 2>/dev/null | grep -q "TUVI_SELFTEST RESULT" && break
    [ -z "$(adb shell pidof "$PKG" | tr -d '\r')" ] && { echo "SELFTEST_PROCESS_DEAD (lần $1)"; break; }
    sleep 2
  done
  adb exec-out screencap -p > "$OUT/2-selftest-api$API.png"
  adb logcat -d > "$OUT/logcat-selftest-api$API.txt"
  RES=$(grep -o "TUVI_SELFTEST RESULT pass=[0-9]* fail=[0-9]*" "$OUT/logcat-selftest-api$API.txt" | tail -1)
}
run_selftest 1
# Máy ảo có Google Play Services đôi khi tự dừng gms.persistent; Android khi đó dừng luôn mọi app đang dùng
# FontsProvider của nó (“depends on provider … in dying proc”). Đây là lỗi môi trường, không phải lỗi app → chạy lại một lần.
if [ -z "$RES" ] && adb logcat -d -b events 2>/dev/null | grep -q "am_kill.*vn.tuvi.tuxem.*in dying proc"; then
  echo "SELFTEST_RETRY: hệ thống dừng app vì tiến trình Google Play Services trên máy ảo bị dừng"
  adb logcat -d -b events 2>/dev/null | grep "am_kill.*vn.tuvi.tuxem" | tail -3
  sleep 10
  run_selftest 2
fi
grep -nE "TUVI_SELFTEST|FATAL EXCEPTION|AndroidRuntime|TuViTuXem|VerifyError|NoClassDefFound" "$OUT/logcat-selftest-api$API.txt" | head -250
echo "SELFTEST: ${RES:-KHONG_CO_KET_QUA}"
case "$RES" in *" fail=0") ;; *) FAIL=1 ;; esac
emit_png "selftest-api$API" "$OUT/2-selftest-api$API.png"

if [ -n "$RES" ]; then
  echo "== 3) Giai đoạn 5 trên màn hình thật"
  wait_log() { # $1 = mẫu, $2 = số giây
    for i in $(seq 1 "$2"); do adb logcat -d -s TuViWeb:V 2>/dev/null | grep -q "$1" && return 0; sleep 1; done; return 1
  }
  wait_log "LONGPRESS_TARGET" 20 || echo "KHONG_CO_LONGPRESS_TARGET"
  # 3a) Cây trợ năng: TalkBack đọc mỗi ô theo thứ tự tên cung → chính tinh → phụ tinh (nhãn gộp của ô).
  A11Y=0
  for k in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/ui-api$API.xml" >/dev/null 2>&1
    if grep -q "Cung Mệnh, Mậu Dần. Chính tinh: Vũ Khúc Vượng, Thiên Tướng Miếu" "$OUT/ui-api$API.xml" 2>/dev/null; then A11Y=1; break; fi
    sleep 3
  done
  if [ $A11Y = 1 ]; then
    echo "A11Y_OK: $(grep -o 'Cung [^"]*Chính tinh:[^"]*' "$OUT/ui-api$API.xml" | wc -l) ô lá số có nhãn TalkBack"
    grep -o 'Cung Mệnh, Mậu Dần[^"]*' "$OUT/ui-api$API.xml" | head -1
  else
    echo "A11Y_FAIL: cây trợ năng không có nhãn ô Mệnh"; FAIL=1
    grep -o 'class="android.webkit.WebView"[^>]*' "$OUT/ui-api$API.xml" 2>/dev/null | head -2
    grep -c "<node" "$OUT/ui-api$API.xml" 2>/dev/null
  fi
  # 3b) Chạm giữ thật (0,9 giây) đúng vị trí sao Thiên Tướng: toạ độ CSS × devicePixelRatio + mép trên WebView.
  T=$(adb logcat -d -s TuViWeb:V | grep -o "LONGPRESS_TARGET x=[0-9.]* y=[0-9.]* dpr=[0-9.]*" | tail -1)
  WV=$(grep -o 'class="android.webkit.WebView"[^>]*bounds="\[[0-9]*,[0-9]*\]' "$OUT/ui-api$API.xml" 2>/dev/null | head -1 | grep -o '\[[0-9]*,[0-9]*\]' | tr -d '[]')
  if [ -n "$T" ] && [ -n "$WV" ]; then
    read -r PX PY <<< "$(python3 -c "
import re,sys
t='$T'; wx,wy=map(int,'$WV'.split(','))
x,y,d=[float(v) for v in re.findall(r'=([0-9.]+)',t)]
print(int(wx+x*d), int(wy+y*d))")"
    echo "LONGPRESS tại ($PX,$PY) — WebView từ ($WV), $T"
    adb shell input swipe "$PX" "$PY" "$PX" "$PY" 900
    if wait_log "LONGPRESS_SEEN" 12; then
      echo "LONGPRESS_OK: $(adb logcat -d -s TuViWeb:V | grep -o 'LONGPRESS_SEEN [A-Z_]*' | tail -1)"
      sleep 1; adb exec-out screencap -p > "$OUT/3-starcard-api$API.png"; emit_png "starcard-api$API" "$OUT/3-starcard-api$API.png"
    else
      echo "LONGPRESS_FAIL: chạm giữ thật không mở thẻ sao"; FAIL=1
    fi
  else
    echo "LONGPRESS_FAIL: thiếu toạ độ (T='$T', WebView='$WV')"; FAIL=1
  fi
  # 3c) Bảng chia sẻ PDF (ShareProvider + ACTION_SEND).
  if wait_log "SHARE_STARTED" 40; then
    sleep 2
    FOCUS=$(adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | head -3 | tr -d '\r')
    echo "$FOCUS"
    if echo "$FOCUS" | grep -qiE "chooser|resolver|intentresolver"; then echo "SHARE_OK: bảng chia sẻ đã mở"; else echo "SHARE_FAIL: không thấy bảng chia sẻ"; FAIL=1; fi
    adb exec-out screencap -p > "$OUT/4-share-api$API.png"; emit_png "share-api$API" "$OUT/4-share-api$API.png"
    adb shell input keyevent KEYCODE_BACK
  else
    echo "SHARE_FAIL: selftest không mở bảng chia sẻ"; FAIL=1
  fi
  grep -E "exportFile|ShareProvider" "$OUT/logcat-selftest-api$API.txt" | head -5
  adb logcat -d | grep -E "FATAL EXCEPTION|E TuViTuXem" | head -10
  if adb logcat -d | grep -q "FATAL EXCEPTION"; then echo "PHASE5_FATAL"; FAIL=1; fi
  # 3d) Tệp đã lưu: PDF trong Tải xuống/TuViTuXem, PNG trong Ảnh/TuViTuXem.
  PDF=$(adb shell ls -t /sdcard/Download/TuViTuXem/ 2>/dev/null | tr -d '\r' | grep -m1 '\.pdf$')
  PNGF=$(adb shell ls -t /sdcard/Pictures/TuViTuXem/ 2>/dev/null | tr -d '\r' | grep -m1 '\.png$')
  echo "Tệp đã lưu: PDF='$PDF' PNG='$PNGF'"
  if [ -n "$PDF" ] && adb pull "/sdcard/Download/TuViTuXem/$PDF" "$OUT/laso-api$API.pdf" >/dev/null 2>&1; then
    python3 - "$OUT/laso-api$API.pdf" <<'PY' || FAIL=1
import re, sys
b = open(sys.argv[1], 'rb').read()
pages = len(re.findall(rb'/Type\s*/Page[^s]', b))
print(f'PDF {len(b)} byte, {pages} trang, mở đầu {b[:5]!r}')
ok = b[:5] == b'%PDF-' and pages >= 5 and b.rstrip().endswith(b'%%EOF')
print('PDF_OK' if ok else 'PDF_FAIL')
sys.exit(0 if ok else 1)
PY
    if command -v pdftoppm >/dev/null; then
      pdftoppm -r 60 -f 1 -l 3 -png "$OUT/laso-api$API.pdf" "$OUT/pdf-api$API" && for f in "$OUT"/pdf-api$API-*.png; do emit_png "$(basename "$f" .png)" "$f"; done
      pdftotext -l 3 "$OUT/laso-api$API.pdf" - 2>/dev/null | head -40
    fi
    [ "$API" = "35" ] && emit_file "laso-api$API.pdf" "$OUT/laso-api$API.pdf"  # bản PDF mẫu (kho artifact không tải được từ mọi nơi)
  else
    echo "PDF_FAIL: không thấy PDF trong Tải xuống/TuViTuXem"; FAIL=1
  fi
  if [ -n "$PNGF" ] && adb pull "/sdcard/Pictures/TuViTuXem/$PNGF" "$OUT/laso-api$API.png" >/dev/null 2>&1; then
    python3 -c "
import struct,sys
b=open('$OUT/laso-api$API.png','rb').read(); w,h=struct.unpack('>II',b[16:24])
print('PNG', len(b), 'byte', w, 'x', h); sys.exit(0 if b[:8]==b'\x89PNG\r\n\x1a\n' and (w,h)==(1256,1504) else 1)" && echo "PNG_OK" || { echo "PNG_FAIL"; FAIL=1; }
  else
    echo "PNG_FAIL: không thấy ảnh trong Ảnh/TuViTuXem"; FAIL=1
  fi
fi
if [ "$FAIL" != 0 ]; then
  echo "== Chẩn đoán: tiến trình, bộ nhớ, lỗi native, WebView"
  adb logcat -d -b crash 2>/dev/null | tail -80
  adb logcat -d -b events 2>/dev/null | grep -E "am_proc_died|am_kill|am_low_memory|am_crash|am_anr|am_proc_start" | grep -iE "tuvi|webview|sandboxed|low_memory" | tail -40
  grep -nE "Fatal signal|SIGSEGV|SIGABRT|SIGKILL|lowmemorykiller|lmkd|Killing|has died|ANR in|Force finishing|backtrace|crash_dump|DEBUG +:|libc +:|chromium|cr_|WebView|vn\.tuvi|TuVi" "$OUT/logcat-selftest-api$API.txt" \
    | grep -vE "SliceDataConverter|AppsFilter|TUVI_SELFTEST (OK|TIME|READING)" | tail -150
  adb shell dumpsys meminfo vn.tuvi.tuxem 2>/dev/null | head -30 || true
  adb shell cat /proc/meminfo | head -5
fi
echo "== KẾT LUẬN API $API: $([ $FAIL = 0 ] && echo ĐẠT || echo LỖI)"
exit $FAIL
