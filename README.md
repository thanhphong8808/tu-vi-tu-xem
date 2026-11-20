# Tử Vi Tự Xem

App Android lập lá số và luận giải Tử Vi Đẩu Số (Nam phái Việt Nam), chạy offline.
Đặc tả đầy đủ: [`docs/PROMPT_TUVI_APP.md`](docs/PROMPT_TUVI_APP.md) · Cẩm nang: [`docs/CAMNANG_TUVI_V2.txt`](docs/CAMNANG_TUVI_V2.txt)

## Trạng thái

| Giai đoạn | Nội dung | Trạng thái |
| --- | --- | --- |
| 1 | Lịch âm VN + engine an sao (`:core:calendar`, `:core:engine`) | ✅ Xong — [báo cáo](docs/PHASE1_REPORT.md) |
| 2 | Vận hạn (`LimitEngine`) | ✅ Xong — [báo cáo](docs/PHASE2_REPORT.md) |
| 2B | Bản chạy thử cài lên điện thoại (file APK) | ✅ Xong — [báo cáo](docs/PHASE2B_REPORT.md) |
| 3 | Knowledge base + RuleEngine | Chưa làm |
| 4 | Luận giải + khuyến nghị | Chưa làm |
| 5 | UI Android đầy đủ | Chưa làm |
| 6 | Trợ lý AI trên máy + câu hỏi thường gặp (Firebase) + phát hành | Chưa làm |

## Cài lên điện thoại Android

1. Chép file `TuViTuXem-0.2.0.apk` vào điện thoại (Zalo, Google Drive, cáp USB…).
2. Mở file trên điện thoại → **Cài đặt**. Lần đầu Android sẽ hỏi quyền cài app từ nguồn này: bật **Cho phép** cho ứng dụng
   đang dùng để mở file (Files, Zalo, Chrome…), quay lại và bấm Cài đặt.
3. Nếu Google Play Protect cảnh báo “ứng dụng không xác định”, chọn **Chi tiết khác → Vẫn cài đặt**. App không xin
   quyền nào, kể cả Internet.

Cần Android 8.0 trở lên. Bản sau cài đè lên bản trước, không mất lá số đã lưu, vì cùng khóa ký trong `app/keystore` (D-31).

Build lại APK (Linux, không cần Android Studio): `bash tools/apk/build_apk.sh`. Công cụ cần có ghi ở đầu script.
Kiểm thử giao diện: `tools/apk/devserver.sh 8765 &` rồi chạy `python3 tools/apk/uitest/test_ui.py`.

## Chạy test

Hiện dự án mới có phần lõi (lịch + an sao + vận hạn), chưa có app để cài lên điện thoại. Vì vậy **chưa cần Android Studio**.

**Trên Windows (cách dễ nhất):** bấm đúp `KIEM_TRA.bat`. Script sẽ:

1. Tìm Java 17 trở lên. Nếu máy chưa có, nó tự cài Java 21 (Eclipse Temurin) qua `winget`.
2. Chạy toàn bộ bài kiểm tra. Lần đầu máy phải tải Gradle và thư viện, khoảng 200 MB.
3. Hiện **BUILD SUCCESSFUL** và mở báo cáo HTML.
4. Hỏi có chạy thêm phần vét cạn 2,2 triệu lá số hay không.

Nếu bị lỗi, script ghi `kiem_tra_log.txt` để gửi lại.

**Không cần cài gì trên máy:** đưa thư mục lên một repo GitHub. Workflow `.github/workflows/build.yml` sẽ tự build và test trên máy chủ GitHub (tab *Actions*). Từ Giai đoạn 2B, workflow này sẽ xuất file APK để cài lên điện thoại.

**Dòng lệnh** (cần JDK 17 trở lên; JBR đi kèm Android Studio cũng dùng được):

```bash
./gradlew test            # Windows: gradlew.bat test
./gradlew :core:engine:stressCheck   # tùy chọn: vét cạn mọi ngày giờ sinh 1900–2100 (khoảng 1–3 phút)
```

Kết quả kiểm tra toàn diện lần gần nhất: [docs/QA_REPORT.md](docs/QA_REPORT.md). Các công cụ đối chiếu độc lập: [tools/crosscheck](tools/crosscheck/README.md).

## Dùng engine

```kotlin
val chart = ChartEngine.build(
    BirthInput(solar = LocalDateTime.of(1990, 8, 15, 10, 30), lunar = null, gender = Gender.NAM),
)
chart.menh                                   // Dần
chart.palace(PalaceRole.MENH).stars          // [Vũ Khúc (V), Thiên Tướng (M), Hóa Quyền, …]
chart.warnings                               // cảnh báo cho thẻ “Xác nhận lá số”
chart.warnings.firstOrNull()?.let { ChartEngine.buildAlternative(chart, it) }  // lá số thay thế
```

Trường phái là cấu hình: `ChartEngine.build(input, SchoolPreset.THIEN_LUONG.config)`.

Vận hạn:

```kotlin
val nam = LimitEngine.forYear(chart, 2026)
nam.daiHan; nam.tieuHan; nam.luuNienDaiHan; nam.luuThaiTue   // Tỵ, Thìn, Tuất, Ngọ với ca mẫu trên
LimitEngine.forMonth(chart, 2026, 1).nguyetHan              // Thìn
LimitEngine.forDate(chart, LocalDate.of(2026, 11, 20)).nhatHan
```

## Cấu trúc

```
app/            App Android: Activity + WebView (src/main), cầu nối AppApi (src/shared), giao diện (assets/web)
core/calendar   Lịch âm Hồ Ngọc Đức (UTC+7/UTC+8 theo thời kỳ), can chi, giờ Mặt Trời thật
core/engine     ChartEngine, LimitEngine, SchoolConfig, bảng độ sáng (brightness.json), chuẩn hóa đầu vào, cảnh báo
tools/apk       Build APK (build_apk.sh), vá stdlib (indyfix), máy chủ thử nghiệm, test giao diện
tools/crosscheck  Đối chiếu độc lập với lasotuvi và amlich.js
docs/           Prompt, cẩm nang, DECISIONS.md, báo cáo từng giai đoạn
```
