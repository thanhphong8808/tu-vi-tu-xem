#!/usr/bin/env bash
# Setup script cho môi trường đám mây của Claude Code (claude.ai/code) — dự án Tử Vi Tự Xem.
# Dán toàn bộ file này vào ô “Setup script” của môi trường (hoặc chạy tay: sudo bash tools/setup/cloud_setup.sh).
# Chạy với quyền root trên Ubuntu 24.04, trước khi Claude khởi động; xong trong khoảng 2–4 phút, sau đó được lưu đệm.
#
# Cài vào /opt/tuvi-tk (đổi bằng TUVI_TK):
#   - aapt, zipalign, dalvik-exchange, apksigner, dexdump (kho Ubuntu)         → build APK bằng tools/apk/build_apk.sh
#   - Kotlin 2.0.21 (github.com), android.jar API 26/35 (raw.githubusercontent.com)
#   - kotlinx-serialization 1.6.2 (repo1.maven.org)
#   - Android SDK: cmdline-tools, platform-tools, platforms;android-35, build-tools;35.0.0 (dl.google.com)
#   - Playwright + Chromium + Pillow cho kiểm thử giao diện (pypi.org, cdn.playwright.dev)
# và ghi /opt/tuvi-tk/env.sh, file mà build_apk.sh / devserver.sh tự nạp.
#
# Luôn thoát 0 (quy định của setup script): bước nào lỗi chỉ ghi lại, tools/setup/doctor.sh sẽ báo thiếu gì.
set -uo pipefail
TK=${TUVI_TK:-/opt/tuvi-tk}
LOG=$TK/setup.log
mkdir -p "$TK"
log() { echo "[tuvi-setup $(date +%H:%M:%S)] $*"; }
fetch() { # url → file
  curl -fsSL --retry 3 --retry-delay 2 -o "$2.part" "$1" && mv "$2.part" "$2" && return 0
  rm -f "$2.part"; log "LỖI tải $1"; return 1
}

apt_tools() {
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq >/dev/null 2>&1
  apt-get install -y -qq aapt zipalign dalvik-exchange apksigner dexdump unzip >/dev/null 2>&1 \
    && log "apt: aapt zipalign dalvik-exchange apksigner dexdump — xong" || log "LỖI apt-get"
}

kotlin_and_jars() {
  if [ ! -x "$TK/kotlinc/bin/kotlinc" ]; then
    fetch https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip kotlin.zip \
      && unzip -q -o kotlin.zip && rm -f kotlin.zip && log "Kotlin 2.0.21 — xong"
  fi
  for a in 26 35; do
    [ -s "android-$a.jar" ] || fetch "https://raw.githubusercontent.com/Sable/android-platforms/master/android-$a/android.jar" "android-$a.jar"
  done
  local M=https://repo1.maven.org/maven2/org/jetbrains/kotlinx
  for n in core json; do
    local f="kotlinx-serialization-$n-jvm-1.6.2.jar"
    [ -s "$f" ] || fetch "$M/kotlinx-serialization-$n-jvm/1.6.2/$f" "$f"
  done
  log "android.jar + kotlinx-serialization — xong"
}

android_sdk() {
  local SDK=$TK/android-sdk SM
  SM=$SDK/cmdline-tools/latest/bin/sdkmanager
  if [ ! -x "$SM" ]; then
    local zip=commandlinetools-linux-13114758_latest.zip
    if ! fetch "https://dl.google.com/android/repository/$zip" clt.zip; then
      # Tên file đổi theo phiên bản: đọc tên mới nhất trong danh mục của Google.
      zip=$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml 2>/dev/null \
        | grep -o 'commandlinetools-linux-[0-9]*_latest.zip' | sort -t- -k3 -n | tail -1)
      [ -n "$zip" ] && fetch "https://dl.google.com/android/repository/$zip" clt.zip
    fi
    if [ -s clt.zip ]; then
      rm -rf "$SDK/cmdline-tools"; mkdir -p "$SDK/cmdline-tools"
      unzip -q -o clt.zip -d "$SDK/cmdline-tools" && mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
      rm -f clt.zip
    fi
  fi
  if [ ! -x "$SM" ]; then log "LỖI: không cài được Android cmdline-tools (dl.google.com bị chặn?)"; return 0; fi
  # sdkmanager là Java: tự truyền proxy nếu môi trường dùng HTTPS_PROXY mà JAVA_TOOL_OPTIONS chưa có.
  local P=() hp=${HTTPS_PROXY:-${https_proxy:-}}
  if [ -n "$hp" ] && [[ "${JAVA_TOOL_OPTIONS:-}" != *proxyHost* ]]; then
    hp=${hp#*://}; hp=${hp#*@}; hp=${hp%%/*}
    P=(--proxy=http "--proxy_host=${hp%%:*}" "--proxy_port=${hp##*:}")
  fi
  yes 2>/dev/null | "$SM" --sdk_root="$SDK" "${P[@]}" --licenses >/dev/null 2>&1
  "$SM" --sdk_root="$SDK" "${P[@]}" "platform-tools" "platforms;android-35" "build-tools;35.0.0" >/dev/null 2>&1 \
    && log "Android SDK (platform-tools, android-35, build-tools 35.0.0) — xong" || log "LỖI sdkmanager"
}

ui_test_tools() {
  python3 -m pip install -q --break-system-packages playwright pillow >/dev/null 2>&1 \
    || python3 -m pip install -q playwright pillow >/dev/null 2>&1 || { log "LỖI pip playwright"; return 0; }
  python3 -m playwright install --with-deps chromium >/dev/null 2>&1 \
    && log "Playwright + Chromium — xong" || log "LỖI playwright install (thiếu cdn.playwright.dev?)"
}

# TUVI_SETUP_SKIP="apt kotlin sdk ui" để bỏ bớt bước (dùng khi thử script).
skip() { [[ " ${TUVI_SETUP_SKIP:-} " == *" $1 "* ]]; }

main() {
  log "bắt đầu, thư mục $TK"
  cd "$TK" || return 0
  skip apt    || apt_tools &
  skip kotlin || kotlin_and_jars &
  skip sdk    || android_sdk &
  skip ui     || ui_test_tools &
  wait
  write_env
  log "đã ghi $TK/env.sh — kết thúc (xem đủ ở $LOG)"
}

write_env() {

cat > "$TK/env.sh" <<EOF
# Ghi bởi tools/setup/cloud_setup.sh — build_apk.sh và devserver.sh tự nạp file này.
export TUVI_TK=$TK
export KOTLINC=$TK/kotlinc/bin/kotlinc
export KOTLIN_STDLIB=$TK/kotlinc/lib/kotlin-stdlib.jar
export ANDROID_JAR_26=$TK/android-26.jar
export ANDROID_JAR_35=$TK/android-35.jar
export SERIALIZATION_JARS=$TK/kotlinx-serialization-core-jvm-1.6.2.jar:$TK/kotlinx-serialization-json-jvm-1.6.2.jar
if [ -d $TK/android-sdk/platform-tools ]; then
  export ANDROID_HOME=$TK/android-sdk ANDROID_SDK_ROOT=$TK/android-sdk
  case ":\$PATH:" in *":$TK/android-sdk/platform-tools:"*) ;; *) export PATH="\$PATH:$TK/android-sdk/platform-tools:$TK/android-sdk/cmdline-tools/latest/bin:$TK/android-sdk/build-tools/35.0.0";; esac
fi
EOF
}

# Không dùng exec > >(tee …): “wait” sẽ chờ cả tiến trình tee và treo mãi.
main 2>&1 | tee -a "$LOG"
exit 0
