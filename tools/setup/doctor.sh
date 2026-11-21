#!/usr/bin/env bash
# Kiểm tra nhanh môi trường làm việc: mạng tới các tên miền cần dùng, công cụ build/test, khóa ký.
# Dùng: bash tools/setup/doctor.sh        (không sửa gì, chỉ báo; thoát 1 nếu thiếu thứ bắt buộc)
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
[ -f "${TUVI_TK:-/opt/tuvi-tk}/env.sh" ] && source "${TUVI_TK:-/opt/tuvi-tk}/env.sh"
[ -f "$ROOT/tools/apk/env.local.sh" ] && source "$ROOT/tools/apk/env.local.sh"
bad=0
ok()   { printf '  ✅ %s\n' "$*"; }
miss() { printf '  ❌ %s\n' "$*"; bad=1; }
warn() { printf '  ⚠️  %s\n' "$*"; }

echo "== Mạng (❌ = proxy của môi trường chặn tên miền này)"
reach() { # host [bắt buộc?]
  local code
  code=$(curl -sS -o /dev/null --max-time 15 -w '%{http_code}' "https://$1/" 2>/dev/null)
  if [ -n "$code" ] && [ "$code" != "000" ]; then ok "$1 (HTTP $code)"
  elif [ "${2:-}" = need ]; then miss "$1 bị chặn"
  else warn "$1 bị chặn"; fi
}
for h in github.com raw.githubusercontent.com repo1.maven.org services.gradle.org pypi.org registry.npmjs.org; do reach "$h" need; done
echo "  — cho model AI trên máy (D-55) và Android SDK:"
for h in dl.google.com maven.google.com huggingface.co cdn-lfs.hf.co cdn.playwright.dev; do reach "$h"; done

echo "== Công cụ"
have() { if command -v "$1" >/dev/null 2>&1; then ok "$1"; else miss "$1 (chạy: sudo bash tools/setup/cloud_setup.sh)"; fi; }
for t in java git gh python3 node curl unzip aapt zipalign dalvik-exchange apksigner keytool; do have "$t"; done
command -v dexdump >/dev/null 2>&1 && ok dexdump || warn "dexdump (không bắt buộc, dùng để soát DEX)"
[ -x "${KOTLINC:-}" ] && ok "kotlinc: $("$KOTLINC" -version 2>&1 | grep -o 'kotlinc-jvm [0-9.]*')" || miss "KOTLINC chưa có (cloud_setup.sh)"
for v in KOTLIN_STDLIB ANDROID_JAR_26 ANDROID_JAR_35; do
  [ -s "${!v:-}" ] && ok "$v" || miss "$v chưa có"
done
SJ=${SERIALIZATION_JARS:-}
for j in ${SJ//:/ }; do [ -s "$j" ] && ok "$(basename "$j")" || miss "thiếu $j"; done
[ -n "${SERIALIZATION_JARS:-}" ] || miss "SERIALIZATION_JARS chưa đặt"
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/platforms/android-35" ]; then ok "Android SDK: $ANDROID_HOME"
else warn "Android SDK chưa có (chỉ cần khi gắn thư viện từ Google Maven / LiteRT-LM)"; fi
D=${TUVI_AI_DEPS:-${TUVI_TK:-$HOME/.cache/tuvi-tk}/ai-deps}
[ -f "$D/litertlm-android/classes.jar" ] && ok "thư viện Trợ lý AI (LiteRT-LM, gson, R8) đã tải: $D" || warn "thư viện Trợ lý AI chưa tải (build_apk.sh tự tải từ Google Maven / Maven Central)"
python3 -c 'import playwright, PIL' 2>/dev/null && ok "playwright + Pillow (Python)" || miss "playwright/Pillow (pip install playwright pillow)"
[ -f "$ROOT/tools/apk/uitest/node_modules/axe-core/axe.min.js" ] && ok "axe-core" || warn "axe-core chưa cài: (cd tools/apk/uitest && npm ci)"
[ -e /dev/kvm ] && ok "/dev/kvm (chạy được máy ảo Android tại chỗ)" || warn "không có /dev/kvm: máy ảo Android chạy trên GitHub Actions (android-emulator.yml)"

echo "== Khóa ký APK (D-31)"
KS=$ROOT/app/keystore/tuvi-tu-xem.jks
if [ -f "$KS" ]; then bash "$ROOT/tools/setup/restore_keystore.sh" --verify "$KS" >/dev/null && ok "đúng khóa thật" || miss "khóa sai (xem restore_keystore.sh)"
elif [ -n "${TUVI_KEYSTORE_B64:-}" ]; then warn "chưa giải mã — chạy: bash tools/setup/restore_keystore.sh"
else miss "chưa có khóa: đặt TUVI_KEYSTORE_B64 (docs/BAN_GIAO_MOI_TRUONG_MOI.md mục 3)"; fi

echo "== GitHub"
if git -C "$ROOT" remote get-url origin >/dev/null 2>&1; then ok "origin: $(git -C "$ROOT" remote get-url origin | sed 's#https://[^@]*@#https://#')"
else warn "repo chưa có remote origin"; fi
git -C "$ROOT" ls-remote --heads origin main >/dev/null 2>&1 && ok "đọc được nhánh main" || warn "không đọc được origin/main"

echo
[ "$bad" = 0 ] && echo "KẾT QUẢ: đủ để build và kiểm thử" || echo "KẾT QUẢ: còn thiếu (các dòng ❌)"
exit "$bad"
