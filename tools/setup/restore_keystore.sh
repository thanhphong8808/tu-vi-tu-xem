#!/usr/bin/env bash
# Khôi phục và kiểm tra khóa ký APK thật (D-31). Khóa KHÔNG nằm trên GitHub (repo công khai).
#
#   bash tools/setup/restore_keystore.sh [đường_dẫn]
#       Chưa có file → giải mã từ biến TUVI_KEYSTORE_B64 (base64 của file .jks) hoặc chép từ TUVI_KEYSTORE_FILE,
#       kiểm tra đúng khóa thật rồi mới đặt vào đường dẫn (mặc định app/keystore/tuvi-tu-xem.jks).
#   bash tools/setup/restore_keystore.sh --verify [đường_dẫn]
#       So dấu vân tay SHA-256 của chứng chỉ với khóa thật. Thoát 1 nếu khác.
#
# Dấu vân tay chứng chỉ là thông tin công khai (ai cài APK cũng đọc được), ghi ở đây để không ký nhầm khóa khác.
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
EXPECTED="86:C7:ED:F7:BC:40:A8:AD:FA:2C:B9:C5:41:C1:E7:85:05:81:38:AF:85:E0:7D:53:45:19:64:1F:55:BF:98:C6"
KS_PASS=${KS_PASS:-tuvituxem}

mode=restore
if [ "${1:-}" = "--verify" ]; then mode=verify; shift; fi
KS=${1:-$ROOT/app/keystore/tuvi-tu-xem.jks}

fingerprint() {
  keytool -list -v -keystore "$1" -storepass "$KS_PASS" -alias tuvi 2>/dev/null \
    | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -1
}
verify() { # file [tên hiển thị]
  local got; got=$(fingerprint "$1")
  if [ "$got" = "$EXPECTED" ]; then
    echo "   khóa ký: đúng khóa thật (SHA-256 86:C7:ED…98:C6)"
    return 0
  fi
  echo "LỖI: ${2:-$1} không phải khóa ký thật (SHA-256 đọc được: '${got:-không đọc được}')"
  return 1
}

if [ "$mode" = verify ]; then verify "$KS"; exit $?; fi
if [ -f "$KS" ]; then verify "$KS"; exit $?; fi

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT
if [ -n "${TUVI_KEYSTORE_B64:-}" ]; then
  if ! printf '%s' "$TUVI_KEYSTORE_B64" | tr -d ' \r\n\t' | base64 -d > "$tmp" 2>/dev/null; then
    echo "LỖI: TUVI_KEYSTORE_B64 không giải mã base64 được (dán thiếu ký tự?)"
    exit 1
  fi
elif [ -n "${TUVI_KEYSTORE_FILE:-}" ] && [ -f "$TUVI_KEYSTORE_FILE" ]; then
  cp "$TUVI_KEYSTORE_FILE" "$tmp"
else
  echo "   chưa có khóa ký thật: biến TUVI_KEYSTORE_B64 trống (docs/BAN_GIAO_MOI_TRUONG_MOI.md, mục 3)"
  exit 1
fi

src=TUVI_KEYSTORE_FILE; [ -n "${TUVI_KEYSTORE_B64:-}" ] && src=TUVI_KEYSTORE_B64
if verify "$tmp" "khóa lấy từ $src"; then
  mkdir -p "$(dirname "$KS")"
  cp "$tmp" "$KS" && chmod 600 "$KS"
  echo "   đã khôi phục khóa ký vào $KS"
  exit 0
fi
exit 1
