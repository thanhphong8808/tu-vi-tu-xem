# Tử Vi Tự Xem

App Android lập lá số và luận giải Tử Vi Đẩu Số (Nam phái Việt Nam), chạy offline.
Đặc tả đầy đủ: [`docs/PROMPT_TUVI_APP.md`](docs/PROMPT_TUVI_APP.md) · Cẩm nang: [`docs/CAMNANG_TUVI_V2.txt`](docs/CAMNANG_TUVI_V2.txt)

## Trạng thái

| Giai đoạn | Nội dung | Trạng thái |
| --- | --- | --- |
| 1 | Lịch âm VN + engine an sao (`:core:calendar`, `:core:engine`) | ✅ Xong — [báo cáo](docs/PHASE1_REPORT.md) |
| 2 | Vận hạn (`LimitEngine`) | ✅ Xong — [báo cáo](docs/PHASE2_REPORT.md) |
| 2B | Bản chạy thử cài lên điện thoại (file APK) | ✅ Xong — [báo cáo](docs/PHASE2B_REPORT.md) |
| 3 | Knowledge base + RuleEngine (`:data:kb`, `:core:interpret`) + luận giải sơ bộ trong app 0.3.0 | ✅ Xong — [báo cáo](docs/PHASE3_REPORT.md) |
| 4 | Luận giải + khuyến nghị | Chưa làm |
| 5 | UI Android đầy đủ | Chưa làm |
| 6 | Trợ lý AI trên máy + câu hỏi thường gặp (Firebase) + phát hành | Chưa làm |

## Cài lên điện thoại Android

1. Chép file `TuViTuXem-0.3.0.apk` (trong `dist/`) vào điện thoại (Zalo, Google Drive, cáp USB…).
2. Mở file trên điện thoại → **Cài đặt**. Lần đầu Android sẽ hỏi quyền cài app từ nguồn này: bật **Cho phép** cho ứng dụng
   đang dùng để mở file (Files, Zalo, Chrome…), quay lại và bấm Cài đặt.
3. Nếu Google Play Protect cảnh báo “ứng dụng không xác định”, chọn **Chi tiết khác → Vẫn cài đặt**. App không xin
   quyền nào, kể cả Internet.

Cần Android 8.0 trở lên. Bản sau cài đè lên bản trước, không mất lá số đã lưu, vì cùng khóa ký trong `app/keystore` (D-31).

Mỗi lần đẩy mã lên GitHub, workflow “Chạy thử trên máy ảo Android” cài file trong `dist/` lên máy ảo Android 11–17
(cài đè lên bản trước như người dùng sẽ làm): mở app, bắt lỗi văng, rồi chạy 4 ca golden và kiểm tra phần luận giải
ngay trong app (`ci/emulator_test.sh`).

Build lại APK (Linux, không cần Android Studio): `bash tools/apk/build_apk.sh`. Lệnh này ra bản phát hành và bản tự kiểm tra. Công cụ cần có ghi ở đầu script.
Kiểm thử giao diện: `tools/apk/devserver.sh 8765 &` rồi chạy `python3 tools/apk/uitest/test_ui.py`.

## Chạy test

Phần lõi (lịch, an sao, vận hạn, knowledge base, RuleEngine) là Kotlin/JVM thuần, nên **không cần Android Studio** để chạy test.

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

Sửa luật hay câu luận: chỉnh `tools/kb/kb_rules.py` / `tools/kb/kb_drafts.py` → `python3 tools/kb/build_kb.py` →
`./gradlew :core:interpret:findFixtures` (tìm lại lá số mẫu) → `./gradlew test` (gồm lint knowledge base, mục 14.3).

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

Cách cục và dấu hiệu (giai đoạn 3):

```kotlin
val kb = KnowledgeBase.load()                         // data/kb: 189 luật, câu luận, khuyến nghị, câu hỏi mẫu
val kq = RuleEngine(kb).evaluate(chart, LimitEngine.forYear(chart, 2026))
kq.findings      // Finding: luật, chủ đề, chiều (−2..+2), trọng số, bằng chứng (sao · cung · quan hệ), textKey
kq.suppressed    // câu bị luật cụ thể hơn chặn, kèm luật chặn
KbLint.run(kb)   // từ cấm, ô {…} lạ, thiếu nguồn, thiếu câu luận
```

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
core/interpret  RuleEngine, DSL predicate (mục 9), KnowledgeBase, KbLint; test: lá số mẫu từng luật, đối chiếu độc lập
data/kb         Knowledge base JSON (rules/, texts/, advice/, faq/, meta/) — sinh từ tools/kb, mọi mục reviewed=false
tools/kb        Nguồn soạn luật và câu luận nháp (Python) → build_kb.py
tools/apk       Build APK (build_apk.sh), vá stdlib (indyfix), máy chủ thử nghiệm, test giao diện
tools/crosscheck  Đối chiếu độc lập với lasotuvi và amlich.js
docs/           Prompt, cẩm nang, DECISIONS.md, báo cáo từng giai đoạn
```
