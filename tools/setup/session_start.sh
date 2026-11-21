#!/usr/bin/env bash
# Chạy khi phiên Claude Code trên đám mây bắt đầu (.claude/settings.json → SessionStart). Nhanh, không làm hỏng phiên:
#   - khôi phục khóa ký APK từ TUVI_KEYSTORE_B64 nếu chưa có;
#   - nếu môi trường chưa có setup script (thiếu /opt/tuvi-tk/env.sh) → chạy cloud_setup.sh ở chế độ nền;
#   - cài axe-core cho kiểm thử giao diện nếu thiếu;
#   - in tình trạng ngắn để Claude biết ngay.
[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
ROOT=${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}
cd "$ROOT" || exit 0
TK=${TUVI_TK:-/opt/tuvi-tk}
echo "[Tử Vi Tự Xem] đọc CLAUDE.md trước khi làm."
if [ ! -f app/keystore/tuvi-tu-xem.jks ]; then
  bash tools/setup/restore_keystore.sh 2>&1 | sed 's/^/[khóa ký] /'
else
  bash tools/setup/restore_keystore.sh --verify 2>&1 | sed 's/^/[khóa ký] /'
fi
if [ ! -f "$TK/env.sh" ]; then
  if [ -w /opt ] || [ "$(id -u)" = 0 ]; then
    nohup bash tools/setup/cloud_setup.sh >/tmp/tuvi-setup.out 2>&1 &
    echo "[công cụ] chưa cài: đang chạy tools/setup/cloud_setup.sh ở nền (2–4 phút, nhật ký /tmp/tuvi-setup.out)."
  else
    echo "[công cụ] chưa cài: chạy sudo bash tools/setup/cloud_setup.sh"
  fi
else
  echo "[công cụ] đã có $TK/env.sh"
fi
if [ ! -f tools/apk/uitest/node_modules/axe-core/axe.min.js ]; then
  (cd tools/apk/uitest && timeout 120 npm ci --silent >/dev/null 2>&1) && echo "[axe-core] đã cài" || echo "[axe-core] chưa cài được (npm ci)"
fi
exit 0
