#!/usr/bin/env bash
# Tải (một lần, có kiểm SHA-256) các thư viện cho Trợ lý AI trên máy (D-57) vào thư mục đệm, in đường dẫn thư mục.
#   litertlm-android 0.19.1 (Google Maven) — AAR: classes.jar + liblitertlm_jni.so arm64-v8a / x86_64
#   gson 2.14.0 (Maven Central)             — LiteRT-LM cần
#   r8 8.13.19 (Google Maven)                — D8 chuyển thư viện Java 21 (lambda, nối chuỗi, switch mẫu) sang DEX
#   litertlm-jvm 0.19.1 (Google Maven)       — chỉ khi gọi với đối số "jvm": chạy model trên máy tính (chấm 40 câu)
# Dùng: DIR=$(bash tools/apk/fetch_ai_deps.sh [jvm])
set -euo pipefail
DIR=${TUVI_AI_DEPS:-${TUVI_TK:-$HOME/.cache/tuvi-tk}/ai-deps}
mkdir -p "$DIR"
G=https://dl.google.com/android/maven2
C=https://repo1.maven.org/maven2

get() { # tên_file url sha256
  local f="$DIR/$1"
  if [ -f "$f" ] && echo "$3  $f" | sha256sum -c --status; then return 0; fi
  local i
  for i in 1 2 3 4 5; do
    if curl -sSfL --retry 2 -o "$f.tmp" "$2" && echo "$3  $f.tmp" | sha256sum -c --status; then mv "$f.tmp" "$f"; return 0; fi
    echo "   tải lại $1 (lần $i)…" >&2; sleep $((i * 10))
  done
  rm -f "$f.tmp"; echo "LỖI: không tải được $1 từ $2 (hoặc sai SHA-256)" >&2; exit 5
}

get litertlm-android-0.19.1.aar "$G/com/google/ai/edge/litertlm/litertlm-android/0.19.1/litertlm-android-0.19.1.aar" \
  341f66349ac962e0b6733b8da419b045a4834fca2f7db0bd13e049ce1cfcca72
get gson-2.14.0.jar "$C/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar" \
  2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f
get r8-8.13.19.jar "$G/com/android/tools/r8/8.13.19/r8-8.13.19.jar" \
  7712edbae6a71f35937ffc1bd5f4c202919ef2a5e58d3c3544015a868a3cf457
if [ "${1:-}" = "jvm" ]; then
  get litertlm-jvm-0.19.1.jar "$G/com/google/ai/edge/litertlm/litertlm-jvm/0.19.1/litertlm-jvm-0.19.1.jar" \
    e0ec77de3a2c570af13045d0d514ecea5f5d22463647b1c52584934e16687bbe
fi

# Giải nén AAR một lần.
if [ ! -f "$DIR/litertlm-android/classes.jar" ]; then
  rm -rf "$DIR/litertlm-android"; mkdir -p "$DIR/litertlm-android"
  (cd "$DIR/litertlm-android" && unzip -q "$DIR/litertlm-android-0.19.1.aar")
fi
echo "$DIR"
