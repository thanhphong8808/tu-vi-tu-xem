# Làm tiếp dự án ở môi trường mới (claude.ai/code)

Môi trường cũ chặn `dl.google.com`, `maven.google.com` và `huggingface.co`, nên chưa gắn được AI sinh lời (D-55).
Tài liệu này hướng dẫn chuyển sang một phiên Claude Code mới, nơi anh tự đặt được danh sách tên miền.

Mọi thứ cần dùng đã nằm trên GitHub, trừ **khóa ký APK**: khóa nằm trong gói riêng `GoiRieng_KhoaKy_TuViTuXem.zip`
mình gửi trong chat. Không đưa gói đó lên GitHub hay gửi cho ai.

## 1. Mở claude.ai/code

Vào <https://claude.ai/code>. Lần đầu, trang sẽ hướng dẫn kết nối GitHub. Cho phép truy cập repo
`thanhphong8808/tu-vi-tu-xem` (cần quyền ghi để Claude đẩy được bản mới).

## 2. Tạo môi trường riêng cho dự án

Ở màn hình bắt đầu phiên mới, bấm **bộ chọn môi trường** ngay dưới ô nhập tin nhắn, rồi chọn thêm môi trường mới.
Đặt tên, ví dụ `Tu Vi Tu Xem`, rồi điền ba mục sau.

**Network access** → chọn **Custom**. Trong ô **Allowed domains**, dán đủ 6 dòng:

```
dl.google.com
huggingface.co
*.huggingface.co
*.hf.co
cdn.playwright.dev
playwright.download.prss.microsoft.com
```

Tích ô **Also include default list of common package managers**. Danh sách mặc định đã có Maven, Gradle, GitHub,
npm và PyPI.

**Setup script** → bấm **Choose file** và chọn `cloud_setup.sh` trong gói riêng. Cũng có thể mở
[tools/setup/cloud_setup.sh](../tools/setup/cloud_setup.sh) trên GitHub, bấm Raw, chép hết rồi dán vào. Script tự cài
Kotlin, công cụ build APK, Android SDK và Chromium để kiểm thử, mất khoảng 2–4 phút. Môi trường lưu đệm sau lần đầu,
nên các phiên sau mở nhanh.

**Environment variables** → mở file `TUVI_KEYSTORE_B64.txt` trong gói riêng, chép **cả dòng** (bắt đầu bằng
`TUVI_KEYSTORE_B64=`) rồi dán vào.

Bấm **Add environment** để lưu.

> Đây là môi trường cá nhân: chỉ anh dùng được, trừ khi anh tự chia sẻ ở mục **Who can use it**. Đừng chia sẻ môi trường
> này, vì ai dùng được môi trường thì đọc được biến chứa khóa ký.

## 3. Bắt đầu phiên làm việc

1. Chọn repo `thanhphong8808/tu-vi-tu-xem` và môi trường `Tu Vi Tu Xem` vừa tạo.
2. Dán tin nhắn đầu tiên này:

```
Đây là dự án Tử Vi Tự Xem chuyển từ môi trường cũ sang. Đọc CLAUDE.md, chạy bash tools/setup/doctor.sh và báo kết quả.
Nếu dl.google.com, maven.google.com và huggingface.co đã mở thì làm tiếp mục "Việc tiếp theo" trong CLAUDE.md:
gắn AI sinh lời chạy trên máy cho mục Hỏi đáp. Kiểm tra kỹ, chạy tools/setup/test_all.sh, chờ máy ảo Android
trên GitHub xanh hết rồi mới bàn giao. Mọi file đưa lên GitHub.
```

Khi phiên mở, Claude tự đọc `CLAUDE.md`, file chứa toàn bộ bối cảnh: cách làm việc với anh, cấu trúc mã, lệnh
build và test, quy tắc bảo mật, kế hoạch gắn AI. Hook khởi động tự đặt khóa ký vào `app/keystore/` và kiểm tra đúng
khóa thật.

## 4. Nếu có trục trặc

| Hiện tượng | Cách xử lý |
| --- | --- |
| `doctor.sh` báo ❌ ở `dl.google.com` / `huggingface.co` | Mở lại môi trường: bộ chọn môi trường, biểu tượng bánh răng ở dòng `Tu Vi Tu Xem`. Kiểm tra Network access đang là **Custom**, đủ 6 dòng, đã bấm lưu. Với phiên đang chạy: bấm mũi tên sau tên phiên → **Edit cloud environment** |
| `doctor.sh` báo “chưa có khóa” | Biến `TUVI_KEYSTORE_B64` chưa vào. Thêm lại ở mục Environment variables, mở phiên mới. Cách tạm thời: gửi file `tuvi-tu-xem.jks` trong gói riêng vào phiên và bảo Claude chép vào `app/keystore/` (không commit) |
| `build_apk.sh` báo “không phải khóa ký thật” | Dòng base64 bị dán thiếu. Chép lại nguyên dòng từ `TUVI_KEYSTORE_B64.txt` |
| Thiếu công cụ (kotlinc, aapt…) | Bảo Claude chạy `sudo bash tools/setup/cloud_setup.sh`, xem nhật ký ở `/opt/tuvi-tk/setup.log` |

## Trong gói riêng có gì

| File | Dùng để |
| --- | --- |
| `tuvi-tu-xem.jks` | Khóa ký APK thật (mật khẩu `tuvituxem`, alias `tuvi`). Giữ bản sao ở nơi an toàn: mất khóa thì không cài đè được bản mới lên app đang dùng |
| `TUVI_KEYSTORE_B64.txt` | Cùng khóa đó dưới dạng một dòng chữ, để dán vào Environment variables |
| `cloud_setup.sh` | Setup script, để chọn bằng nút Choose file |
| `DOC_TRUOC.txt` | Tóm tắt các bước trên |

Dấu vân tay chứng chỉ khóa thật (SHA-256):
`86:C7:ED:F7:BC:40:A8:AD:FA:2C:B9:C5:41:C1:E7:85:05:81:38:AF:85:E0:7D:53:45:19:64:1F:55:BF:98:C6`.
