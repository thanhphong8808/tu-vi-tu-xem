#!/usr/bin/env bash
# Chấm bộ 40 câu hỏi mẫu với model thật (LiteRT-LM JVM, CPU) — D-57, mục 12.1.
# Dùng: bash tools/ai/eval40.sh <đường-dẫn-model.litertlm> [báo-cáo.md] [số câu]
#   Model: tải từ địa chỉ trong data/kb/src/main/resources/kb/ai/model.json (khoảng 2,6 GB), vd.
#   curl -L -o /tmp/gemma.litertlm "$(python3 -c 'import json;print(json.load(open("data/kb/src/main/resources/kb/ai/model.json"))["url"])')"
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
[ -f "${TUVI_TK:-/opt/tuvi-tk}/env.sh" ] && source "${TUVI_TK:-/opt/tuvi-tk}/env.sh"
: "${KOTLINC:=kotlinc}"; : "${KOTLIN_STDLIB:?}"; : "${SERIALIZATION_JARS:?}"
MODEL=${1:?Thiếu đường dẫn model .litertlm}
REPORT=${2:-$ROOT/docs/tai-ve/ai-cham-40-cau.md}
DEPS=$(bash "$ROOT/tools/apk/fetch_ai_deps.sh" jvm)
JVM_JAR="$DEPS/litertlm-jvm-0.19.1.jar"
OUT=${EVAL_OUT:-$ROOT/app/build-eval}
rm -rf "$OUT"; mkdir -p "$OUT/classes"
javac -nowarn --release 17 -cp "$JVM_JAR:$KOTLIN_STDLIB" -d "$OUT/classes" $(find "$ROOT/app/src/llm/java" -name '*.java')
"$KOTLINC" -nowarn -jvm-target 17 -classpath "$SERIALIZATION_JARS:$OUT/classes" -d "$OUT/classes" \
  $(find "$ROOT/core/calendar/src/main/kotlin" "$ROOT/core/engine/src/main/kotlin" "$ROOT/core/interpret/src/main/kotlin" "$ROOT/app/src/shared/kotlin" -name '*.kt') \
  "$ROOT/tools/ai/Eval40.kt"
cp "$ROOT/core/engine/src/main/resources/brightness.json" "$OUT/classes/"
cp -r "$ROOT/data/kb/src/main/resources/kb" "$OUT/classes/kb"
mkdir -p "$(dirname "$REPORT")"
# Không đưa kotlinx-coroutines vào classpath: chứng minh đường sinh lời đồng bộ của LiteRt.java không cần (như trong APK).
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
  -cp "$OUT/classes:$JVM_JAR:$KOTLIN_STDLIB:$SERIALIZATION_JARS:$DEPS/gson-2.14.0.jar" \
  vn.tuvi.tools.ai.Eval40Kt "$MODEL" "$ROOT/tools/ai/eval40_questions.json" "$REPORT" ${3:-} 2> >(grep -vE "^(INFO|WARNING|W0000|I0000)|Picked up" >&2)
echo "Báo cáo: $REPORT"
