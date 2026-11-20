# Báo cáo Giai đoạn 1 — Lịch + engine an sao

Ngày: 20/11/2026 · Phạm vi: `:core:calendar`, `:core:engine` (docs/PROMPT_TUVI_APP.md mục 15, giai đoạn 1)

## Cổng giai đoạn: PASS

| Cổng (mục 15) | Kết quả |
| --- | --- |
| Test vector lịch mục 4 (10 vector + 2 vector phía lịch TQ + 3 vector hồi quy D-03) | 15/15 pass |
| Phần lá số của 4 ca golden (Mệnh, Thân, Cục, bản mệnh, 14 chính tinh, 28 phụ tinh, Tuần, Triệt, 3 đại hạn, cung tiểu hạn 2026, tuổi âm) | 240/240 kiểm tra pass |
| Cảnh báo thẻ xác nhận của 4 ca golden (B: Tý muộn, C: tháng nhuận, A và D: không) | 4/4 pass |
| Property test mục 14.2, bất biến 1–7 trên 10.000 lá số ngẫu nhiên 1900–2100, mọi tổ hợp SchoolConfig | pass |
| Bất biến 8: lịch khứ hồi mọi ngày 1900–2100, cả UTC+7 và UTC+8 (146.828 ngày) | pass |
| Bất biến 9: đổi từng khóa SchoolConfig chỉ đổi đúng nhóm sao (13 khóa × 3.000 lá số) | 13/13 pass |

**Tổng: 301 test, 301 pass, 0 fail** (25 ở `:core:calendar`, 276 ở `:core:engine`).
Số test là số mục JUnit báo; mỗi property test là một mục nhưng chạy 3.000–10.000 lá số.

| Lớp test | Số mục |
| --- | --- |
| `LunarCalendarTest` | 25 |
| `GoldenChartTest` | 240 |
| `ChartInvariantsTest` | 1 (10.000 lá số) |
| `SchoolConfigDiffTest` | 15 |
| `BirthNormalizerTest` | 13 |
| `TablesTest` | 6 |
| `PerformanceTest` | 1 |

Hiệu năng: lập lá số trung bình **0,02–0,03 ms** trên JVM máy dev (chỉ tiêu < 5 ms trên S22 Ultra; đo trên máy thật thuộc cổng Giai đoạn 5).

## Đối chiếu độc lập (ngoài cổng)

- **Lịch âm ↔ amlich.js** (port JavaScript nguyên bản thuật toán Hồ Ngọc Đức): 146.828 ngày, lệch đúng 7 ngày
  mà bản gốc trả về “ngày 0”. Đây là lỗi của bản gốc, đã vá (D-03). Trong đó 19/04/1939 và 22/03/1947 thuộc thời kỳ
  lịch UTC+8, ảnh hưởng người sinh thật.
- **Engine ↔ lasotuvi**: 9.000 lá số ngẫu nhiên (3 seed), khớp 100% Mệnh, Thân, Cục, đại hạn, tiểu hạn và mọi sao,
  trừ Thai–Dưỡng (đã biết), Phá Toái và Khôi Việt tuổi Đinh/Mậu/Nhâm/Canh. Ở hai chỗ sau, lasotuvi lệch khẩu quyết
  kinh điển, nên engine giữ đúng mục 5.4–5.5 (D-14). Câu “khớp 100% với lasotuvi” trong mục 5 của prompt chưa đúng ở Phá Toái.

Cách chạy lại: `tools/crosscheck/README.md`.

## Ba câu hỏi trường phái

Người dùng xác nhận cả ba mặc định: Nhâm hóa Khoa **Tả Phù**; tháng nhuận **tính như tháng chính**; Khôi Việt tuổi Canh **Ngọ/Dần**.

## Chỗ đã phải tự quyết

Ghi đủ trong `docs/DECISIONS.md` (D-01 … D-18). Những điểm cần biết trước khi sang Giai đoạn 2:
- D-02: dùng `java.time` (`zone: ZoneId`) thay kotlinx-datetime.
- D-04, D-05, D-06: `BirthInput` có thêm `lunarClock`, `canh`, `region`, `calendarTzOverride`; quy ước nhập âm lịch và sinh ở nước ngoài.
- D-07: Tứ Hóa tách hai khóa `tuHoa` (hàng Canh) và `nhamKhoa`.
- D-12: property test viết bằng JUnit 5 + bộ sinh có seed (chưa dùng Kotest). Build được kiểm chứng bằng `kotlinc` 2.0.21 +
  JUnit 5 dựng từ mã nguồn vì môi trường làm việc không vào được Maven Central; **hãy chạy `./gradlew test` một lần trên máy
  bạn để xác nhận build Gradle**.

## File đã tạo

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew, gradlew.bat, gradle/wrapper/*
README.md, .gitignore
docs/PROMPT_TUVI_APP.md          (export Markdown của prompt)
docs/CAMNANG_TUVI_V2.txt         (cẩm nang của bạn)
docs/DECISIONS.md, docs/PHASE1_REPORT.md

core/calendar/build.gradle.kts
core/calendar/src/main/kotlin/vn/tuvi/core/calendar/
    CanChi.kt            Can, Chi, CanChi (năm/tháng/ngày/giờ), hourChiOf
    LunarCalendar.kt     LunarDate, BirthRegion, thuật toán Hồ Ngọc Đức, múi giờ lịch theo thời kỳ
    TrueSolarTime.kt     giờ Mặt Trời thật
core/calendar/src/test/kotlin/…/LunarCalendarTest.kt, tools/DumpLunar.kt

core/engine/build.gradle.kts
core/engine/src/main/kotlin/vn/tuvi/core/engine/
    Types.kt             BirthInput, Chart, Palace, PlacedStar, NapAm, Cuc, cảnh báo…
    Stars.kt             StarId (109 sao, thứ tự hiển thị), StarKind, TuHoa
    SchoolConfig.kt      12 khóa trường phái + 3 preset
    Tables.kt            bảng an sao theo can, tam hợp, Tứ Hóa, Mệnh chủ, Thân chủ
    BrightnessTable.kt   đọc brightness.json
    BirthNormalizer.kt   chuẩn hóa đầu vào + cảnh báo + lá số thay thế
    ChartEngine.kt       lập lá số (hàm thuần)
    ChartCache.kt        cache LRU theo (input, config, kbVersion)
core/engine/src/main/resources/brightness.json
core/engine/src/test/resources/golden.json
core/engine/src/test/kotlin/…/ GoldenChartTest, ChartInvariantsTest, SchoolConfigDiffTest,
    BirthNormalizerTest, TablesTest, PerformanceTest, PropertySupport, tools/DumpCharts.kt

tools/crosscheck/crosscheck.py, calendar_crosscheck.js, README.md
```

## Chưa làm (đúng phạm vi)

LimitEngine và trường `nam2026` của golden (đại hạn năm, lưu niên đại hạn, nguyệt hạn) thuộc Giai đoạn 2;
`SchoolConfig.nguyetHan` và `daiHanTuHoa` đã có sẵn khóa nhưng chưa có tác dụng cho tới khi LimitEngine được viết.
