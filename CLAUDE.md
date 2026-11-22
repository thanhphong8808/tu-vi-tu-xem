# Tử Vi Tự Xem — hướng dẫn cho Claude

App Android **offline** lập lá số và luận giải Tử Vi Đẩu Số (Nam phái Việt Nam) của anh **Thanh Phong** (dùng Samsung S22 Ultra).
Repo công khai `thanhphong8808/tu-vi-tu-xem`. Bản hiện tại: **0.9.0 (11)**, giai đoạn 1–9 đã xong (xem `README.md`, mục Trạng thái).

## Cách làm việc với anh Phong

- Trả lời bằng **tiếng Việt**, xưng “mình”, gọi “anh”. Anh muốn làm luôn, không bàn dài; gom các sửa đổi thành một lần;
  kết quả dùng được ngay.
- **Kiểm tra kỹ trước khi bàn giao**: chạy đủ kiểm thử tại chỗ (`bash tools/setup/test_all.sh`), đẩy lên GitHub, rồi chờ
  workflow **máy ảo Android** (API 30/34/35/36/37) xanh hết mới báo xong. Anh không nhận bản chưa chạy thử trên máy ảo.
- **Mọi file giao cho anh đi qua GitHub**: APK trong `dist/`, tài liệu và ảnh mẫu trong `docs/tai-ve/`, link trong `README.md`
  mục “Tải về”. Không gửi file đính kèm trong chat, trừ khóa ký (bí mật). GitHub Releases bị chặn (403): không tạo Release,
  không lách bằng Actions.
- Đặc tả là `docs/PROMPT_TUVI_APP.md`; cẩm nang là `docs/CAMNANG_TUVI_V2.txt`. **Khi hai tài liệu khác nhau, theo PROMPT.**
  Mọi quyết định thiết kế ghi vào `docs/DECISIONS.md` (đang tới D-68). Mỗi giai đoạn có `docs/PHASEn_REPORT.md`.
- Lựa chọn trường phái anh đã chốt: Nhâm hóa Khoa = Tả Phù; sinh tháng nhuận tính như tháng thường; Khôi Việt năm Canh =
  Ngọ/Dần (mặc định Nam phái VN).

## Bảo mật — không được làm sai

- Khóa ký `app/keystore/tuvi-tu-xem.jks` (mật khẩu `tuvituxem`, alias `tuvi`) **không bao giờ đưa lên GitHub**
  (`.gitignore` có `app/keystore/`, không được xóa dòng này). Mất khóa thì điện thoại phải gỡ app, mất lá số đã lưu.
- Ở môi trường mới, khóa đến từ biến môi trường `TUVI_KEYSTORE_B64`. Hook khởi động (`tools/setup/session_start.sh`)
  và `build_apk.sh` tự giải mã rồi kiểm tra dấu vân tay SHA-256 `86:C7:ED…:98:C6` (`tools/setup/restore_keystore.sh`).
  `build_apk.sh` **dừng** khi thiếu khóa thật hoặc khóa sai. Chỉ CI hoặc `ALLOW_NEW_KEY=1` mới ký bằng khóa tạm, và APK
  khóa tạm **không được đưa vào `dist/`**.
- `tools/apk/env.local.sh` và `node_modules/` không đưa lên repo. Không in khóa hay base64 của khóa ra màn hình hoặc vào
  file trong repo.
- Mạng: nếu một tên miền bị proxy chặn (403 “agent proxy”), **báo anh**, không vòng qua đường khác (không nhờ CI tải hộ,
  không mirror).

## Môi trường

Lần đầu trong phiên, chạy `bash tools/setup/doctor.sh`: lệnh này báo mạng, công cụ và khóa ký.

- `tools/setup/cloud_setup.sh` (setup script của môi trường, hoặc tự chạy nền qua hook) cài vào `/opt/tuvi-tk`:
  - Kotlin 2.0.21; android.jar API 26/35 (Sable); kotlinx-serialization 1.6.2;
  - aapt / zipalign / dalvik-exchange / apksigner / dexdump;
  - Android SDK (platform-tools, android-35, build-tools 35.0.0);
  - Playwright + Chromium.

  Script ghi `/opt/tuvi-tk/env.sh`. `build_apk.sh` và `devserver.sh` tự nạp file này. Nhật ký ở `/opt/tuvi-tk/setup.log`.
- Tên miền cần mở thêm ngoài danh sách mặc định: `dl.google.com`, `huggingface.co`, `*.huggingface.co`, `*.hf.co`,
  `cdn.playwright.dev`, `playwright.download.prss.microsoft.com`. Maven, Gradle và GitHub đã có trong danh sách mặc định.
- Không có `/dev/kvm` thì không chạy máy ảo tại chỗ được. Máy ảo chạy trên GitHub Actions (`android-emulator.yml`).

## Cấu trúc

| Phần | Nội dung |
| --- | --- |
| `core/calendar` | Lịch âm VN, can chi, giờ mặt trời thật |
| `core/engine` | An sao (`ChartEngine`, `Tables`, `TuHoaTable`, `SchoolConfig`), vận hạn (`LimitEngine.forYear/forMonth/forDate` → `LimitLayers`) |
| `data/kb` | Knowledge base JSON (`kb/texts`, `rules`, `advice`, `faq`, `meta`): `star.ID.CODE.ROLE`, `combo.KEY.ROLE`, `phu.ID`, `qa.*`; `kb/ai/model.json` (model AI), `kb/ai/system_prompt_vi.md`; `kb/sach/sach.json` (kiến thức sách, sinh từ `tools/kb/kb_sach.py`) |
| `core/interpret` | `RuleEngine`, `Scoring` (`Scorer.topicLayered`), `Composer` (`yearData/decadeData/lifeData`), `Qa.kt` (`QaEngine`), `QaAnalyst.kt` (luận đoán theo tầng), `QaAi.kt` (`AiPrompt`, `AiGuard`, `AiFake`), `Books.kt` (câu sách theo lá số), `QaPrivacy`, `KbLint` |
| `app/src/main` | `MainActivity` (WebView, `APP_VERSION`, `callAsync`), `AndroidLlm` (tải model, LiteRT-LM), `Export` (PNG/PDF), `TuViApp` |
| `app/src/llm` | `LiteRt.java`: lớp bọc LiteRT-LM (Java), dùng chung cho APK và công cụ chấm 40 câu |
| `app/src/shared` | Cầu nối JSON giữa WebView và engine (`AppApi`, `Luan`, `Ai.kt`: `LlmHost`, `AiFlow`, `FakeLlmHost`): dùng chung cho APK và máy chủ thử |
| `app/src/dev` | `DevServer` để thử giao diện trên máy tính |
| `app/assets/web` | Giao diện `index.html`, `app.js`, `app.css`, `icons.js` (bộ icon, cùng nguồn với Figma) |
| `tools/apk` | `build_apk.sh` (APK không cần Android Studio, D-28), `fetch_ai_deps.sh` (LiteRT-LM, gson, R8, ghim SHA-256), `devserver.sh`, `uitest/`, `selftest/`, `check_api.py` |
| `tools/ai` | `eval40.sh` + `Eval40.kt`: chấm 40 câu với model thật trên CPU Linux (D-59) |
| `tools/setup` | `cloud_setup.sh`, `doctor.sh`, `restore_keystore.sh`, `session_start.sh`, `test_all.sh` |
| `ci/` | `emulator_test.sh` (adb trên máy ảo), `regression/` (APK bản trước để thử cài đè) |

Gradle chỉ build các module lõi (JVM). App Android build bằng `tools/apk/build_apk.sh`:
kotlinc → dx → aapt → zipalign → apksigner. Lý do ở D-28. Thư viện AI (LiteRT-LM, bytecode Java 21) đi qua D8 của
R8 8.13.19 thành `classes2.dex`; `.so` arm64-v8a + x86_64 nén trong APK (D-57).

## Lệnh hằng ngày

```bash
bash tools/setup/doctor.sh                       # tình trạng môi trường
./gradlew test --continue --console=plain        # toàn bộ test lõi (có QaTest: luận đoán theo tầng)
bash tools/apk/build_apk.sh                      # → app/build/TuViTuXem-<ver>.apk và -selftest.apk
bash tools/setup/test_all.sh                     # tất cả: Gradle + APK + giao diện + selftest (SKIP_GRADLE=1 … để bỏ bước)
```

- **Giao diện:** `test_all.sh` tự làm. Làm tay thì:
  1. `(cd tools/apk/uitest && npm ci)`
  2. `bash tools/apk/devserver.sh 8765 2026-11-20 > /tmp/devserver.log 2>&1 &`, đợi dòng “DevServer http”
  3. `python3 tools/apk/uitest/test_ui.py /tmp/uishots`

  Khởi động lại devserver giữa các lần chạy. Ảnh chuẩn nằm ở `tools/apk/uitest/baseline`. Cập nhật bằng
  `TUVI_UPDATE_BASELINE=1` (hoặc xóa ảnh rồi chạy hai lần). Chỉ cập nhật khi thay đổi giao diện là cố ý.
- **Selftest tại chỗ:** `bash tools/apk/selftest/run_local.sh`. Lệnh này chạy 4 ca golden và gõ câu hỏi vào mục Hỏi đáp.
- **Mốc hiện tại (0.9.0):** xem `docs/PHASE9_REPORT.md`. Riêng `:core:interpret` có 552 test.
- Gradle cần locale UTF-8 (tên test tiếng Việt): `test_all.sh` tự đặt `LC_ALL=C.UTF-8`.
- Playwright: nếu bản pip không khớp trình duyệt (không tải được từ `cdn.playwright.dev`), `test_ui.py` và
  `run_local.py` tự dùng Chromium có sẵn ở `/opt/pw-browsers`.
- **Chấm model thật:** tải model theo `kb/ai/model.json` (2,6 GB) rồi `bash tools/ai/eval40.sh <model>` (~16 phút CPU).
- **CI:** mỗi lần push chạy hai workflow:
  - `build.yml`: Gradle test + APK khóa tạm;
  - `android-emulator.yml`: cài `ci/regression/*.apk` rồi cài đè `dist/*.apk`, chạy selftest, trợ năng, chạm giữ,
    chia sẻ, gõ câu hỏi, chụp màn hình.

  Đọc log: `gh run list`, `gh api repos/thanhphong8808/tu-vi-tu-xem/actions/jobs/<id>/logs`. Ảnh nằm giữa
  `BEGIN_PNG`/`END_PNG` trong log.

## Phát hành một bản mới

1. Tăng `APP_VERSION` trong `MainActivity.kt` (vd. `"0.6.2 (8)"`; versionCode tính từ x.y.z).
2. `bash tools/setup/test_all.sh` xanh.
3. Chuyển APK bản trước sang `ci/regression/` (thay file cũ), chép APK mới từ `app/build/` vào `dist/` (bản phát hành +
   `-selftest`), xóa APK cũ trong `dist/`.
4. Cập nhật `README.md` (Tải về, Trạng thái), `docs/DECISIONS.md`, báo cáo giai đoạn. Nếu có đầu ra mẫu thì cập nhật
   cả `docs/tai-ve/`.
5. Commit tiếng Việt, tên `Tu Vi Tu Xem`, cuối commit có các dòng ghi công mà phiên cung cấp. Push lên `main`
   (README trỏ `raw/main/dist/...`). Nếu phiên chỉ được đẩy nhánh `claude/*`, mở PR và nhờ anh merge.
6. Chờ máy ảo xanh hết, ghi kết quả vào báo cáo, rồi mới báo anh. Gửi kèm link tải và ảnh máy ảo.

## Quy tắc nội dung luận giải

- Không phán chắc, không gây sợ. `KbLint` cấm các từ: “chắc chắn”, “100%”, “không thể tránh”, “định mệnh”, “tử vong”,
  “chết”, “đoản thọ”, “tuổi thọ”, “sảy thai”, “ly hôn”, “tù tội”, “lao lý”, “tai nạn chết người”, “hạn chí đầu hà”,
  “dị bào”, “giới tính thai”, “con trai”, “con gái”, “giải hạn”, “cúng sao”, “dâng sao”.
- Câu hỏi khủng hoảng: đưa số 115, đường dây Ngày Mai 096 306 1414, Tổng đài 111.
- Hỏi đáp (D-49…D-54): `QaEngine.parse` hiểu câu hỏi (có dấu hoặc không dấu). `QaAnalyst` dựng câu trả lời từ lá số
  qua các tầng: gốc, đại hạn, lưu niên đại hạn, tiểu hạn, Lưu Thái Tuế, Tứ Hóa, sao lưu. Trọng số năm:
  GOC .10, DAI_HAN .30, LUU_NIEN .25, TIEU_HAN .20, LUU_THAI_TUE .15. Điểm phải trùng với bản luận đầy đủ.
- Hỏi mốc năm (D-64, 0.8.1): “đoán năm cưới”, “tuổi kết hôn”… → TIMING; không nêu thời gian thì thêm đoạn “Đối chiếu các
  năm đã qua”. “Đã cưới năm 2015, có khớp không?” → `QaParse.verify`, `verifyYear` (năm đó ± 2). Năm trước năm sinh →
  `qa.truoc_nam_sinh`.
- Luận chi tiết (D-65): `QaAnalyst.starDetailPara` (từng sao trong cung), `triggers`/`concretePara`/`concreteAdvice` (Tứ Hóa
  lưu, sao lưu chạm cung chủ đề → kho `ct.*` trong `tools/kb/kb_chitiet.py`), `topicMonths`, `personalityPara`.
  Lời khuyên chỉ lấy đúng chủ đề. Việc đã qua (D-66): `QaParse.past`, `pastYear`, `milestones`.
- So hai lá số (D-67): `Synastry` (bản mệnh, can, chi, cung Mệnh, Cục, chính tinh; câu `hop.*`).
- KB sinh từ `tools/kb/build_kb.py` (đừng sửa tay file JSON trong `data/kb`).

## Kiến thức sách, thiết kế, liên hệ (0.8.0, D-61…D-63)

- Hai sách anh gửi (Toàn Thư, Nghiệm Lý) còn bản quyền: chỉ **diễn giải lại**, không chép nguyên văn vào repo công khai.
  Sửa/thêm câu trong `tools/kb/kb_sach.py` rồi chạy lại; câu nào cũng phải qua `KbLint`.
- Không fine-tune model (không GPU, không chỗ phát hành model, trái D-58): kiến thức đi qua KB và lời nhắc AI.
- YouTube (youtube.com) bị proxy chặn.
- Giao diện không còn chữ “nháp”/“Prompt”; nguồn hiển thị qua `Luan.src`. Liên hệ: 0378 373 373.
- Figma (tài khoản anh, gói Starter: 1 chế độ mỗi bộ biến): https://www.figma.com/design/kdxoEkGID6PUamNLPzuvyZ.
  Icon: `app/assets/web/icons.js` là nguồn; thêm icon thì thêm cả trên Figma.

## Trợ lý AI trên máy (0.7.0, D-57…D-60)

- Hai model (D-68, `kb/ai/model.json`): **Nhanh** Gemma 4 E2B (`litert-community/gemma-4-E2B-it-litert-lm`, 2,6 GB) và
  **Mạnh** Gemma 4 E4B (`litert-community/gemma-4-E4B-it-litert-lm`, 3,7 GB, RAM ≥ 7 GB); Apache 2.0, không gated,
  LiteRT-LM 0.19.1. Người dùng chọn trong thẻ Trợ lý AI (`aiAction select:<id>`); `eval40.sh` chọn cấu hình theo tên file.
  App tải bằng DownloadManager, chỉ Wi-Fi, kiểm SHA-256; xin quyền INTERNET chỉ cho việc này, WebView vẫn chặn mạng.
- Model **chỉ diễn đạt lại** câu của `QaAnalyst`; `AiGuard` (mục 12.3) kiểm JSON, độ dài, từ cấm, tên sao/cung,
  sao gắn đúng cung, số và điểm đúng tầng. Trượt thì hiện câu của `QaAnalyst`, ghi `aiLog` trên máy.
- Máy chủ thử dùng `FakeLlmHost` (`TUVI_AI=off` để tắt). Máy ảo CI chỉ kiểm thư viện máy nạp được (probe).
- Còn chờ: anh đo tốc độ, chấm chất lượng trên S22 Ultra (GPU/CPU); đồng bộ Firebase (D-53) cần
  `google-services.json` từ dự án Firebase của anh.
