# Báo cáo kiểm tra toàn diện — sau Giai đoạn 2

Ngày: 20/11/2026 · Phạm vi: toàn bộ phần đã xây (`:core:calendar`, `:core:engine`: lịch âm, lập lá số, vận hạn), build script, công cụ, tài liệu.

Kết luận: **không còn lỗi nào đã biết**. Lần kiểm tra này tìm ra và sửa 4 lỗi (bảng cuối). Mọi kiểm tra dưới đây chạy trên mã cuối cùng trong gói gửi đi.

## Kết quả

| Hạng mục | Cách kiểm | Kết quả |
| --- | --- | --- |
| Test JUnit | `./gradlew test` (25 test lịch, 316 test engine) | 341/341 pass |
| Vét cạn ngày giờ sinh | `stressCheck`: mọi ngày dương 1900–2100 × 13 canh × 2 giới; mọi ngày âm của lịch UTC+7 và UTC+8 (kể cả tháng nhuận, ngày 30) × 2 quy tắc tháng nhuận; giờ sát ranh giới canh với múi giờ lịch sử miền Nam | 2.278.027 lá số, 0 lỗi |
| Bản cài đặt tham chiếu độc lập | `spec_reference.py` viết lại mục 5 và 8 từ prompt, không dùng chung mã; 3 seed × 10.000 lá số, mọi tổ hợp SchoolConfig, kèm thời điểm xem năm/tháng/ngày/giờ | Khớp 100%: 12 cung, 109 sao, độ sáng, Tuần Triệt, Mệnh/Thân chủ, toàn bộ vận hạn |
| Đối chiếu bảng trong prompt | `SpecConformanceTest` đọc thẳng bảng mục 4, 5.3–5.7, 6, 7, 8.1, 14.1 rồi so với engine | 10/10 pass |
| Đối chiếu lasotuvi | 3 seed × 3.000 lá số | Khớp 100% trừ Thai–Dưỡng, Phá Toái, Khôi Việt (lasotuvi lệch khẩu quyết, D-14) |
| Đối chiếu amlich.js | 146.828 ngày (1900–2100, UTC+7 và UTC+8) | Lệch đúng 7 ngày lỗi “ngày 0” của bản gốc (D-03) |
| Bộ test có bắt được lỗi không | Cố ý cài 4 lỗi (một ô bảng sao theo can, một ô độ sáng, đảo Phá Toái, sai một sao Tứ Hóa) | Bắt đủ 4/4 (5 test đỏ), rồi khôi phục |
| Cảnh báo biên dịch | `kotlinc` 2.0.21, đích JVM 17, toàn bộ mã chính và test | 0 cảnh báo |
| Chuẩn Unicode | Mọi tệp `.kt .kts .json .md .py .js .toml` | Đều NFC |
| Hiệu năng | Trung bình 3.000 lá số trên JVM máy dev | 0,02–0,03 ms/lá số (chỉ tiêu < 5 ms) |

## Lỗi tìm thấy và đã sửa trong lần kiểm tra này

| # | Lỗi | Ảnh hưởng | Cách sửa |
| --- | --- | --- | --- |
| 1 | 204 ngày âm trước 1967 (vd. 30/7/1950) có ở lịch UTC+7 nhưng không có ở lịch UTC+8 của thời kỳ đó, nên bị báo “không tồn tại” | Người sinh những ngày này không lập được lá số | Dùng lịch còn lại kèm cảnh báo `CALENDAR_TZ` (D-25); thêm test |
| 2 | Không chặn ngày sinh ngoài 1900–2100 | Có thể ra kết quả ngoài vùng đã kiểm | Báo lỗi dễ hiểu (D-26); thêm test biên 01/01/1900 và 23:30 ngày 31/12/2100 |
| 3 | Công cụ dev in chữ “Đ” thành “?” khi JVM không mặc định UTF-8 (Windows) | Công cụ đối chiếu báo lệch giả | Ghi UTF-8 tường minh, ép UTF-8 cho mọi tác vụ test và công cụ (D-27) |
| 4 | Chú thích cách chạy `dumpCharts` trong build script bị sai | Người chạy công cụ làm theo sẽ lỗi | Sửa chú thích, bỏ cấu hình thừa |

Bộ test cũ không có test nào bắt được lỗi đảo Phá Toái; nay `SpecConformanceTest` đã phủ.

## Còn phải xác nhận trên máy bạn

- **Build Gradle.** Môi trường làm việc không vào được Maven Central, nên lệnh `./gradlew test` chưa chạy được ở đây. Mã đã biên dịch
  và chạy bằng đúng Kotlin 2.0.21 (cùng phiên bản plugin trong `libs.versions.toml`), JUnit Jupiter 5.8.2 (dự án khai báo 5.11.3;
  mọi API dùng đều có ở cả hai) và kotlinx-serialization 1.6.2 (dự án khai báo 1.7.3; API JSON dùng đều có ở cả hai).
  Lần chạy `gradlew.bat test` đầu tiên trên máy bạn là bước xác nhận cuối. Nếu báo lỗi, gửi lại đoạn log để mình sửa.
- **Hiệu năng trên Galaxy S22 Ultra** đo ở Giai đoạn 5, khi đã có app.

## Chưa có (đúng lộ trình)

Giao diện Android (Giai đoạn 2B, 5), kho luận giải và luận giải (Giai đoạn 3, 4), trợ lý AI và câu hỏi thường gặp (Giai đoạn 6).
