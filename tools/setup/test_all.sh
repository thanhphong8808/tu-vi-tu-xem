#!/usr/bin/env bash
# Chạy toàn bộ kiểm thử tại chỗ trước khi bàn giao (máy ảo Android chạy trên GitHub Actions sau khi đẩy lên).
#   1. ./gradlew test        — lịch, an sao, vận hạn, knowledge base, luận giải, Hỏi đáp (bỏ qua: SKIP_GRADLE=1)
#   2. build_apk.sh          — bản phát hành + bản tự kiểm tra, ký bằng khóa thật     (bỏ qua: SKIP_APK=1)
#   3. test_ui.py            — giao diện trên Chromium, so ảnh chuẩn, axe-core         (bỏ qua: SKIP_UI=1)
#   4. selftest/run_local.sh — 4 ca golden + gõ câu hỏi vào mục Hỏi đáp               (bỏ qua: SKIP_SELFTEST=1)
# Dùng: bash tools/setup/test_all.sh
set -uo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT" || exit 1
declare -a RESULT=()
run() { # tên, lệnh...
  local name=$1; shift
  printf '\n######## %s\n' "$name"
  local t0=$SECONDS
  if "$@"; then RESULT+=("✅ $name ($((SECONDS - t0)) s)"); else RESULT+=("❌ $name ($((SECONDS - t0)) s)"); fi
}

if [ "${SKIP_GRADLE:-}" != 1 ]; then run "Gradle test" ./gradlew test --continue --console=plain; fi
if [ "${SKIP_APK:-}" != 1 ]; then run "Build APK" bash tools/apk/build_apk.sh; fi

ui() {
  [ -f tools/apk/uitest/node_modules/axe-core/axe.min.js ] || (cd tools/apk/uitest && npm ci --silent) || return 1
  pkill -f DevServerKt 2>/dev/null; sleep 1
  bash tools/apk/devserver.sh 8765 2026-11-20 > /tmp/devserver.log 2>&1 &
  local pid=$!
  for _ in $(seq 1 600); do grep -q "DevServer http" /tmp/devserver.log 2>/dev/null && break; sleep 0.5; done
  python3 tools/apk/uitest/test_ui.py /tmp/uishots; local rc=$?
  kill "$pid" 2>/dev/null; pkill -f DevServerKt 2>/dev/null
  return $rc
}
if [ "${SKIP_UI:-}" != 1 ]; then run "Giao diện (test_ui.py)" ui; fi
if [ "${SKIP_SELFTEST:-}" != 1 ]; then run "Tự kiểm tra (selftest)" bash tools/apk/selftest/run_local.sh 8767; fi

printf '\n======== TÓM TẮT\n'
printf '%s\n' "${RESULT[@]}"
for r in "${RESULT[@]}"; do [[ $r == ❌* ]] && exit 1; done
exit 0
