#!/usr/bin/env bash
# Chạy bộ tự kiểm tra selftest.js (4 ca golden + mục Hỏi đáp) trên máy tính, giống bản -selftest.apk trên máy ảo.
# Dùng: bash tools/apk/selftest/run_local.sh [cổng, mặc định 8767]
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
PORT=${1:-8767}
TMP=$(mktemp -d)
cp -r "$ROOT/app/assets/web" "$TMP/web"
python3 - "$ROOT/tools/apk/selftest/selftest.js" "$ROOT/core/engine/src/test/resources/golden.json" "$TMP/web/selftest.js" <<'PY'
import sys, json
tpl, golden, dst = sys.argv[1:]
g = json.dumps(json.load(open(golden, encoding='utf-8')), ensure_ascii=False)
open(dst, 'w', encoding='utf-8').write(open(tpl, encoding='utf-8').read().replace('/*__GOLDEN__*/[]', g))
PY
WEB_DIR="$TMP/web" bash "$ROOT/tools/apk/devserver.sh" "$PORT" > "$TMP/server.log" 2>&1 &
SERVER=$!
cleanup() { kill "$SERVER" 2>/dev/null; pkill -f "DevServerKt $TMP/web" 2>/dev/null; rm -rf "$TMP"; }
trap cleanup EXIT
for _ in $(seq 1 600); do
  grep -q "DevServer http" "$TMP/server.log" 2>/dev/null && break
  kill -0 "$SERVER" 2>/dev/null || { cat "$TMP/server.log"; echo "LỖI: máy chủ thử nghiệm không chạy"; exit 1; }
  sleep 0.5
done
python3 "$ROOT/tools/apk/selftest/run_local.py" "http://127.0.0.1:$PORT/"
