#!/usr/bin/env bash
# Biên dịch engine + cầu nối cho máy tính (JVM) và chạy máy chủ thử nghiệm giao diện.
# Dùng: tools/apk/devserver.sh [cổng] [ngày hôm nay yyyy-mm-dd]
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
[ -f "$ROOT/tools/apk/env.local.sh" ] && source "$ROOT/tools/apk/env.local.sh"
: "${KOTLINC:=kotlinc}"; : "${KOTLIN_STDLIB:?}"; : "${SERIALIZATION_JARS:?}"
OUT=${DEV_OUT:-$ROOT/app/build-dev}
if [ ! -f "$OUT/.ok" ] || [ -n "$(find "$ROOT/core" "$ROOT/app/src" -name '*.kt' -newer "$OUT/.ok" | head -1)" ]; then
  rm -rf "$OUT"; mkdir -p "$OUT/classes"
  "$KOTLINC" -nowarn -jvm-target 17 -classpath "$SERIALIZATION_JARS" -d "$OUT/classes" \
    $(find "$ROOT/core/calendar/src/main/kotlin" "$ROOT/core/engine/src/main/kotlin" "$ROOT/core/interpret/src/main/kotlin" "$ROOT/app/src/shared/kotlin" "$ROOT/app/src/dev/kotlin" -name '*.kt')
  cp "$ROOT/core/engine/src/main/resources/brightness.json" "$OUT/classes/"
  touch "$OUT/.ok"
fi
# Knowledge base luôn chép lại (sửa JSON không cần biên dịch lại).
rm -rf "$OUT/classes/kb"; cp -r "$ROOT/data/kb/src/main/resources/kb" "$OUT/classes/kb"
exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$OUT/classes:$KOTLIN_STDLIB:$SERIALIZATION_JARS" vn.tuvi.app.dev.DevServerKt "$ROOT/app/assets/web" "${1:-8765}" ${2:-}
