# Tử Vi Tự Xem

App Android lập lá số và luận giải Tử Vi Đẩu Số (Nam phái Việt Nam), chạy offline.
Đặc tả đầy đủ: [`docs/PROMPT_TUVI_APP.md`](docs/PROMPT_TUVI_APP.md) · Cẩm nang: [`docs/CAMNANG_TUVI_V2.txt`](docs/CAMNANG_TUVI_V2.txt)

## Tải về

Bấm vào tên file để tải (GitHub mở trang file, bấm nút **Download raw file** ⤓ ở góc phải nếu không tự tải):

| File | Nội dung |
| --- | --- |
| [TuViTuXem-0.8.0.apk](https://github.com/thanhphong8808/tu-vi-tu-xem/raw/main/dist/TuViTuXem-0.8.0.apk) | File cài app Android (bản mới nhất, ~22 MB). Giao diện mới (icon tướng số, sơn mài – kim nhũ); Hỏi đáp có đoạn **Sách bàn** theo *Tử Vi Đẩu Số Toàn Thư* và *Tử Vi Nghiệm Lý*; **Trợ lý AI chạy trên máy** (Gemma 4 E2B, tải một lần ~2,6 GB qua Wi-Fi); số liên hệ 0378 373 373. Cài đè lên bản cũ không mất lá số |
| [Thiết kế trên Figma](https://www.figma.com/design/kdxoEkGID6PUamNLPzuvyZ) · [ảnh màn mẫu](docs/tai-ve/ThietKe_Figma_ManMau.png) | Hệ thiết kế: màu sáng/tối, kiểu chữ, 22 icon, nút, thẻ, màn Trang chủ và Hỏi đáp |
| [Chấm 40 câu Trợ lý AI](docs/tai-ve/ai-cham-40-cau.md) | 40 câu hỏi trên 4 lá số, câu AI viết (model thật), câu nào qua/trượt kiểm tra dữ kiện và vì sao, thời gian mỗi câu |
| [Ảnh thẻ Trợ lý AI](docs/tai-ve/TroLyAI_TheTai.png) | Thẻ tải Trợ lý AI trong mục Hỏi đáp: dung lượng, nguồn tải, chỉ qua Wi-Fi |
| [Báo cáo bản 0.8.0](docs/PHASE8_REPORT.md) | Kiến thức sách, giao diện mới, kết quả kiểm thử, 5 máy ảo |
| [Báo cáo giai đoạn 7](docs/PHASE7_REPORT.md) | Trợ lý AI trên máy: cách gắn, kiểm tra đầu ra, kết quả kiểm thử, 5 máy ảo, việc anh cần thử trên S22 Ultra |
| [Ảnh mục Hỏi đáp trên máy ảo](docs/tai-ve/HoiDap_MayAo_API35.png) | Màn hình thật (Android 15, bản 0.7.0) sau khi gõ “nam sau tai chinh ra sao” |
| [Hỏi đáp mẫu (0.6.1)](docs/tai-ve/HoiDapMau_GiaiDoan6.txt) | 41 câu hỏi và câu trả lời app luận trên lá số mẫu, đủ các tầng: gốc, đại hạn, lưu niên, tiểu hạn, Lưu Thái Tuế, tổng hợp |
| [Lá số mẫu (PDF A4)](https://github.com/thanhphong8808/tu-vi-tu-xem/raw/main/docs/tai-ve/LaSoMau_CaA.pdf) | Đúng file app xuất ra trên máy Android: trang 1 lá số, sau đó luận trọn đời và năm 2026 |
| [Lá số mẫu (ảnh PNG)](docs/tai-ve/LaSoMau_CaA.png) | Ảnh lưới 12 cung app xuất ra |
| [Bản luận mẫu giai đoạn 5](docs/tai-ve/BanLuanMau_GiaiDoan5.txt) | Lịch ngày, chi tiết ngày + 12 giờ, thẻ sao, so hai lá số, xếp hạng giờ khi không rõ giờ sinh |
| [Bản luận mẫu 4 ca golden](docs/tai-ve/BanLuanMau_4CaGolden.txt) | Bản luận trọn đời / đại vận / năm 2026 / tháng của 4 lá số mẫu, để đọc góp ý văn phong |
| [Làm tiếp ở môi trường mới](docs/BAN_GIAO_MOI_TRUONG_MOI.md) | Hướng dẫn từng bước dựng môi trường Claude Code (claude.ai/code) có mở `dl.google.com` và `huggingface.co`; khóa ký đi kèm gói riêng gửi trong chat |
| [Báo cáo giai đoạn 6](docs/PHASE6_REPORT.md) | Mục Hỏi đáp, kết quả kiểm thử, 5 máy ảo, phần còn chờ (model AI, Firebase) |
| [Báo cáo giai đoạn 5](docs/PHASE5_REPORT.md) | Kết quả kiểm thử, 5 máy ảo, hiệu năng, các điểm phải tự quyết |
| [Mã nguồn (ZIP)](https://github.com/thanhphong8808/tu-vi-tu-xem/archive/refs/heads/main.zip) | Toàn bộ mã nguồn, trừ khóa ký app (xem D-31) |

## Trạng thái

| Giai đoạn | Nội dung | Trạng thái |
| --- | --- | --- |
| 1 | Lịch âm VN + engine an sao (`:core:calendar`, `:core:engine`) | ✅ Xong — [báo cáo](docs/PHASE1_REPORT.md) |
| 2 | Vận hạn (`LimitEngine`) | ✅ Xong — [báo cáo](docs/PHASE2_REPORT.md) |
| 2B | Bản chạy thử cài lên điện thoại (file APK) | ✅ Xong — [báo cáo](docs/PHASE2B_REPORT.md) |
| 3 | Knowledge base + RuleEngine (`:data:kb`, `:core:interpret`) + luận giải sơ bộ trong app 0.3.0 | ✅ Xong — [báo cáo](docs/PHASE3_REPORT.md) |
| 4 | Luận giải + khuyến nghị (chấm điểm, tổng hợp tầng, ghép câu, Nên/Không nên) + màn Luận giải trong app 0.4.0 | ✅ Xong — [báo cáo](docs/PHASE4_REPORT.md) |
| 5 | UI đầy đủ: tab Ngày, thẻ sao, so hai lá số, ước đoán giờ, trường phái tùy chỉnh, xuất PNG/PDF, hai cột, trợ năng — app 0.5.0 | ✅ Xong — [báo cáo](docs/PHASE5_REPORT.md) |
| 6 | Hỏi đáp trên máy: hỏi bằng lời (có dấu/không dấu), luận đoán theo lá số qua từng tầng vận hạn kèm “Vì sao?”, lịch sử câu hỏi ẩn danh — app 0.6.1 | ✅ Xong phần hỏi đáp — [báo cáo](docs/PHASE6_REPORT.md). Đồng bộ Firebase: chờ (D-53) |
| 7 | Trợ lý AI sinh lời trên máy (Gemma 4 E2B qua LiteRT-LM): tải model qua Wi-Fi, diễn đạt lại câu trả lời Hỏi đáp, kiểm tra dữ kiện trước khi hiện — app 0.7.0 | ✅ Xong — [báo cáo](docs/PHASE7_REPORT.md). Chờ anh đo tốc độ trên S22 Ultra |
| 8 | Kiến thức từ sách (Toàn Thư, Nghiệm Lý) vào luận giải và Trợ lý AI; giao diện mới thiết kế trên Figma; bỏ nhãn nháp; số liên hệ — app 0.8.0 | ✅ Xong — [báo cáo](docs/PHASE8_REPORT.md) |

## Cài lên điện thoại Android

1. Tải `TuViTuXem-0.8.0.apk` ở mục **Tải về** ngay trên điện thoại (hoặc chép vào máy qua Zalo, Google Drive, cáp USB…).
2. Mở file trên điện thoại → **Cài đặt**. Lần đầu Android sẽ hỏi quyền cài app từ nguồn này: bật **Cho phép** cho ứng dụng
   đang dùng để mở file (Files, Zalo, Chrome…), quay lại và bấm Cài đặt.
3. Nếu Google Play Protect cảnh báo “ứng dụng không xác định”, chọn **Chi tiết khác → Vẫn cài đặt**. Từ bản 0.7.0
   app có quyền Internet **chỉ để tải model Trợ lý AI** khi bạn bấm tải (qua Wi-Fi); lá số và câu hỏi không gửi đi đâu.

Cần Android 8.0 trở lên. Bản sau cài đè lên bản trước, không mất lá số đã lưu, vì cùng một khóa ký (D-31). Khóa ký không
đưa lên GitHub công khai; bản sao nằm trong gói riêng `GoiRieng_KhoaKy_TuViTuXem.zip` gửi trong chat.

Mỗi lần đẩy mã lên GitHub, workflow “Chạy thử trên máy ảo Android” cài file trong `dist/` lên máy ảo Android 11–17
(cài đè lên bản trước như người dùng sẽ làm): mở app, bắt lỗi văng, chạy 4 ca golden và kiểm tra phần luận giải
ngay trong app (cả mục Hỏi đáp), rồi thử trên màn hình thật: cây trợ năng TalkBack, chạm giữ một sao, bảng chia sẻ,
PDF/PNG đã lưu, gõ câu hỏi bằng bàn phím vào mục Hỏi đáp (`ci/emulator_test.sh`).

Build lại APK (Linux, không cần Android Studio): `bash tools/apk/build_apk.sh`. Lệnh này ra bản phát hành và bản tự kiểm tra. Công cụ cần có ghi ở đầu script; ở môi trường mới, `tools/setup/cloud_setup.sh` cài hết. Script dừng nếu thiếu khóa ký thật hoặc khóa sai, để không ra APK không cài đè được.
Chạy mọi kiểm thử tại chỗ bằng một lệnh: `bash tools/setup/test_all.sh`. Kiểm tra môi trường: `bash tools/setup/doctor.sh`.
Kiểm thử giao diện: `(cd tools/apk/uitest && npm install)` (axe-core) một lần, rồi `tools/apk/devserver.sh 8765 2026-11-20 &`
và `python3 tools/apk/uitest/test_ui.py` (so ảnh với `tools/apk/uitest/baseline`, cập nhật bằng `TUVI_UPDATE_BASELINE=1`).

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

Bản luận (giai đoạn 4):

```kotlin
val composer = Composer(kb)
val nam = composer.year(chart, 2026)          // điểm 0–100, thế Tấn/Giữ/Thủ/Lùi, 7 chủ đề, sự kiện, Nên/Không nên, 12 tháng
composer.lifetime(chart); composer.decade(chart, 2026); composer.month(chart, 2026, 3)
println(nam.render(why = true))               // văn bản, kèm căn cứ “Vì sao?” của từng đoạn
```

Bản luận mẫu của 4 ca golden: [`core/interpret/src/test/resources/snapshots/`](core/interpret/src/test/resources/snapshots/).

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
