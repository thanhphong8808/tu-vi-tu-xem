# tools/crosscheck — kiểm chứng độc lập

Công cụ dev, không đóng gói vào app. Kết quả lần chạy gần nhất ở `docs/QA_REPORT.md`.

## 1. Bản cài đặt tham chiếu độc lập (lá số + vận hạn) ↔ engine

`spec_reference.py` viết lại toàn bộ công thức mục 5 và mục 8 trực tiếp từ `docs/PROMPT_TUVI_APP.md` (các bảng đọc thẳng
từ prompt), không dùng chung mã với engine, rồi so từng trường: Mệnh, Thân, Cục, bản mệnh, Mệnh/Thân chủ, Tuần, Triệt,
12 cung, 109 sao, độ sáng, vận hạn năm/tháng/ngày/giờ, với mọi tổ hợp SchoolConfig.

```bash
./gradlew :core:engine:dumpFull -q --args="10000 7" > /tmp/full.jsonl
python3 tools/crosscheck/spec_reference.py /tmp/full.jsonl docs/PROMPT_TUVI_APP.md
```

## 2. Engine an sao ↔ lasotuvi

```bash
git clone --depth 1 https://github.com/doanguyen/lasotuvi.git /tmp/lasotuvi
./gradlew :core:engine:dumpCharts -q --args="3000 20261120 lasotuvi" > /tmp/charts.jsonl
python3 -W ignore tools/crosscheck/crosscheck.py /tmp/charts.jsonl /tmp/lasotuvi
```

Bỏ qua có chủ đích Thai–Dưỡng, Phá Toái, Thiên Khôi, Thiên Việt (lasotuvi lệch khẩu quyết — `docs/DECISIONS.md` D-14).

## 3. Lịch âm ↔ amlich.js (port JavaScript thuật toán Hồ Ngọc Đức)

```bash
npm install --prefix /tmp/amlich amlich.js
./gradlew :core:calendar:dumpLunar -q > /tmp/lunar.txt
NODE_PATH=/tmp/amlich/node_modules node tools/crosscheck/calendar_crosscheck.js /tmp/lunar.txt
```

Kỳ vọng: lệch đúng 7 ngày mà bản gốc trả về “ngày 0” (D-03).

## 4. Vét cạn mọi ngày giờ sinh

```bash
./gradlew :core:engine:stressCheck
```

Mọi ngày dương 1900–2100 × 13 canh × 2 giới, mọi ngày âm của cả lịch UTC+7 và UTC+8 (kể cả tháng nhuận, ngày 30) với cả hai
quy tắc tháng nhuận, và các giờ sát ranh giới canh với múi giờ lịch sử miền Nam — tất cả qua bộ bất biến mục 14.2 và kiểm vận hạn.
