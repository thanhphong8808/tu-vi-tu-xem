# Báo cáo Giai đoạn 2 — Vận hạn (LimitEngine)

Ngày: 20/11/2026 · Phạm vi: `LimitEngine` trong `:core:engine` (docs/PROMPT_TUVI_APP.md mục 8, mục 15 giai đoạn 2)

## Cổng giai đoạn: PASS

| Cổng (mục 15) | Kết quả |
| --- | --- |
| Trường `nam2026` của 4 ca golden: tuổi âm, đại hạn, tiểu hạn, lưu niên đại hạn, nguyệt hạn tháng Giêng | 20/20 pass |
| Ví dụ Tân Biên (Dương nam, đại hạn 23–32: 23 gốc, 24 xung chiếu, 25 lùi một cung, 26 xung chiếu, 27 tiến một cung), cả công thức lẫn trên lá số thật | pass |

Thêm các test bất biến (tất cả pass):
- 3.000 lá số × năm xem ngẫu nhiên, mọi tổ hợp SchoolConfig: Lưu Thái Tuế, Lưu Mệnh, tiểu hạn, đại hạn chứa đúng tuổi,
  lưu niên đại hạn, 15 sao lưu (Tang Môn = Thái Tuế + 2, Bạch Hổ xung Tang Môn, Khốc + Hư ≡ 0, Hồng Loan – Thiên Hỷ lệch 6,
  Kình/Đà kẹp Lưu Lộc Tồn, Lưu Lộc Tồn không ở Tứ Mộ…), Tứ Hóa lưu bám sao gốc, 12 tháng nguyệt hạn nối tiếp, tháng nhuận chung cung.
- 500 lá số × 12 năm liên tiếp: tiểu hạn và Lưu Thái Tuế đi đủ 12 cung; tiểu hạn Nam thuận, Nữ nghịch.
- 1.000 ngày ngẫu nhiên: nhật hạn = nguyệt hạn + (ngày âm − 1), thời hạn giờ Tý = nhật hạn.
- Thiên thời theo ngũ hành tam hợp (5 quan hệ, điểm +2 … −2); ca A năm 2026 ra “Tuổi khắc đại hạn”.
- Bốn biến thể nguyệt hạn, Tứ Hóa đại hạn bật/tắt, tuổi trước đại hạn đầu, năm xem trước năm sinh bị từ chối.

**Tổng toàn dự án: 330 test, 330 pass, 0 fail** (Giai đoạn 1: 301, Giai đoạn 2: 29).

| Lớp test | Số mục |
| --- | --- |
| `LunarCalendarTest` | 25 |
| `GoldenChartTest` | 240 |
| `LimitEngineTest` | 29 |
| `ChartInvariantsTest` | 1 (10.000 lá số) |
| `SchoolConfigDiffTest` | 15 |
| `BirthNormalizerTest` | 13 |
| `TablesTest` | 6 |
| `PerformanceTest` | 1 |

Kiểm chứng độc lập: 4 giá trị `nam2026` của golden đến từ bản tham chiếu lúc viết prompt, không từ mã Giai đoạn 2;
mình cũng đã tính tay lại cả 4 ca theo công thức mục 8 và khớp.

## API

```kotlin
val chart = ChartEngine.build(input)
LimitEngine.daiHanPeriods(chart)               // 12 đại hạn: cung, can chi, tuổi, thiên thời
LimitEngine.forYear(chart, 2026)               // đại hạn, tiểu hạn, lưu niên đại hạn, Lưu Thái Tuế, 15 sao lưu, Tứ Hóa lưu, Lưu Mệnh
LimitEngine.forMonth(chart, 2026, 3)           // + nguyệt hạn, can chi tháng, Tứ Hóa nguyệt
LimitEngine.forDate(chart, LocalDate.now())    // + nhật hạn, can chi ngày
LimitEngine.forHour(chart, date, Chi.NGO)      // + thời hạn
```

## Chỗ đã phải tự quyết

`docs/DECISIONS.md` D-20 … D-24: tuổi trước đại hạn đầu, khóa của `luuStars`, chiều Lưu Hỷ Thần, nguyệt hạn với tháng nhuận,
ngày đang xem đổi âm lịch theo UTC+7. D-19 ghi lần cập nhật prompt (trợ lý AI trên máy, câu hỏi thường gặp, Firebase, Giai đoạn 2B).

## File đã tạo/sửa

```
core/engine/src/main/kotlin/vn/tuvi/core/engine/LimitEngine.kt      (mới) LimitEngine, LimitLayers, DaiHanPeriod, ThienThoi
core/engine/src/test/kotlin/vn/tuvi/core/engine/LimitEngineTest.kt  (mới)
core/engine/src/test/kotlin/vn/tuvi/core/engine/GoldenSupport.kt    (mới) đọc golden.json dùng chung
core/engine/src/test/kotlin/vn/tuvi/core/engine/GoldenChartTest.kt  (sửa) dùng GoldenSupport
docs/PROMPT_TUVI_APP.md   (cập nhật theo doc: mục 12 mới, lộ trình 2B, đính chính)
docs/DECISIONS.md, docs/PHASE2_REPORT.md, README.md
```

## Bước tiếp theo

Giai đoạn 2B — bản chạy thử giao diện (nhập ngày giờ sinh, thẻ xác nhận, lưới 12 cung, chọn năm xem để hiện vận hạn) để cài lên
Galaxy S22 Ultra bằng Android Studio.
