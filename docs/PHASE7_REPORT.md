# Báo cáo Giai đoạn 7 — Trợ lý AI sinh lời trên máy (bản 0.7.0)

Ngày 21/11/2026. Quyết định chi tiết: `docs/DECISIONS.md` D-57 … D-60.

## Có gì mới

- Mục **Hỏi đáp** có thẻ **Trợ lý AI trên máy**. Bấm “Tải Trợ lý AI” → app hỏi lại → tải model Gemma 4 E2B
  (~2,6 GB, từ huggingface.co) **chỉ qua Wi-Fi**, có thông báo tiến độ, tắt app vẫn tải, đứt mạng tự tải tiếp. Tải
  xong app kiểm SHA-256 rồi bật Trợ lý AI. Có nút Hủy tải, Xóa model, công tắc bật/tắt (Hỏi đáp và Cài đặt).
- Mỗi câu hỏi: câu trả lời của app (QaAnalyst, luận theo tầng) hiện ngay như trước. Trợ lý AI chạy nền, viết lại
  thành lời trò chuyện: điểm chung, rồi từng tầng (lá số gốc, đại hạn, lưu niên đại hạn, tiểu hạn, Lưu Thái Tuế,
  Tứ Hóa) — sao nào đứng ở cung nào, sáng hay hãm, nghĩa là gì, điểm bao nhiêu; năm đã qua thì nói là dấu hiệu để đối
  chiếu, năm tới thì nói khả năng; kết bằng Nên/Không nên.
- Câu AI **chỉ hiện khi qua kiểm tra dữ kiện** (mục 12.3): đúng tên sao/cung, sao gắn đúng cung, đúng năm, đúng
  điểm của từng tầng, có điểm chung, không từ cấm, tiếng Việt, 60–380 chữ. Trượt thì app hiện bản luận gốc và ghi
  một dòng; nhật ký câu trượt chỉ lưu trên máy (Cài đặt → Nhật ký Trợ lý AI).
- Có câu AI thì bản luận chi tiết của app thu gọn dưới nút “Xem bản luận chi tiết của app (từng tầng, “Vì sao?”)”.
- Câu hỏi khủng hoảng, câu từ chối không bao giờ đi qua model.

## Thay đổi cần anh biết

| Mục | Trước | Từ 0.7.0 |
| --- | --- | --- |
| Quyền | Không xin quyền nào | INTERNET + ACCESS_NETWORK_STATE, chỉ dùng để tải model khi anh bấm tải |
| Mạng của giao diện | Chỉ đọc file trong app | Như cũ: WebView chặn mọi http/https, lá số và câu hỏi không ra mạng |
| Dung lượng APK | 1,5 MB | ~22 MB (thư viện LiteRT-LM cho arm64 và x86_64 của máy ảo CI) |
| Bộ nhớ khi bật AI | — | Model 2,6 GB trong `Android/data/vn.tuvi.tuxem/files/models` (gỡ app là mất); RAM khi chạy ~2–3 GB |
| Sao lưu tự động | Toàn bộ | Bỏ thư mục model (vượt hạn mức 25 MB sẽ làm hỏng bản sao lưu lá số) |

## Cách gắn

- Thư viện: LiteRT-LM 0.19.1 (Google Maven), gson 2.14.0; R8 8.13.19 để chuyển bytecode Java 21 của thư viện sang
  DEX. Tất cả tải một lần qua `tools/apk/fetch_ai_deps.sh`, ghim SHA-256. Vẫn build bằng `build_apk.sh` (D-28), không
  đổi sang Gradle/AGP.
- `app/src/llm/.../LiteRt.java`: lớp bọc bằng Java, cùng mã chạy trên điện thoại và trên máy tính.
- `core/interpret/.../QaAi.kt`: `AiPrompt` (lời nhắc từ câu trả lời QaAnalyst, ≤ 6.500 ký tự), `AiGuard` (kiểm tra),
  `AiFake` (model giả cho máy chủ thử). Cấu hình model: `kb/ai/model.json`; system prompt: `kb/ai/system_prompt_vi.md`.
- `AndroidLlm.kt`: DownloadManager (Wi-Fi), kiểm SHA-256, nạp GPU → lùi CPU (nhớ GPU lỗi để lần sau khỏi thử),
  giải phóng model khi máy thiếu RAM.

## Chấm bộ 40 câu (model thật)

Chạy `tools/ai/eval40.sh` trên máy build Linux, **CPU 4 nhân**, cùng luồng với app (`AppApi.askAi` → model →
`AiGuard`). Chi tiết từng câu: [`docs/tai-ve/ai-cham-40-cau.md`](tai-ve/ai-cham-40-cau.md).

| Lần | Thay đổi | Qua kiểm tra | Trung vị / lâu nhất |
| --- | --- | --- | --- |
| 1 | Lời nhắc đầu tiên; dò cặp sao–cung theo cả câu | 24/40 | 21,6 / 43,6 giây |
| 2 | Lời nhắc: không có “Điểm chung” thì không nói điểm; dò cung **gần sao nhất** | **36/40** | 19,6 / 28,7 giây |

- Lần 1 phần lớn câu trượt là báo động nhầm của bộ kiểm tra (một câu có hai nơi chốn, hoặc dữ kiện tách câu bằng
  dấu chấm phẩy). Cũng bắt được lỗi thật: model bịa “Điểm chung: 90/100”, “60/100” cho câu hỏi dữ kiện không có điểm.
- Lần 2, 4 câu trượt đều là lỗi thật: hai câu quên nêu điểm chung, một câu bịa điểm 38/100, một câu gán Địa Không /
  Địa Kiếp vào cung Hợi. Với 4 câu này app hiện bản luận gốc.
- Đã đọc đối chiếu các câu đạt với dữ kiện lá số (sao, cung, điểm từng tầng): đúng. Văn phong ấm, câu ngắn; đôi khi
  lời khuyên Nên/Không nên lấy hơi rộng từ danh sách khuyến nghị.
- Thời gian trên là CPU máy build. S22 Ultra (Exynos 2200) cần đo thật: app hiện số giây dưới mỗi câu AI.

## Kiểm thử tại chỗ (`bash tools/setup/test_all.sh`)

| Bước | Kết quả |
| --- | --- |
| Gradle test | 870/870 (`:core:calendar` 25, `:core:engine` 316, `:core:interpret` 529 — thêm 7 test `AiTest`) |
| Build APK | Đạt, ký khóa thật (SHA-256 86:C7:ED…98:C6); 0 `invoke-custom` ở cả hai DEX; kiểm API hệ thống 0 lỗi |
| Giao diện (`test_ui.py`) | 689/689 (thêm giai đoạn 7: thẻ AI, tải, câu AI đạt/trượt, khủng hoảng không gọi AI, nhật ký, xóa model) |
| Selftest trên máy tính | 523/523 + bước gõ câu hỏi |

Ảnh chuẩn giao diện được cập nhật có chủ đích: màn Hỏi đáp và Cài đặt có thẻ Trợ lý AI; ảnh xuất PNG lệch 6% chỉ do
Chromium của môi trường mới vẽ chữ khác (không đổi code phần xuất ảnh).

`AiTest` kiểm: cấu hình model; lời nhắc mang đủ tầng và không quá ngân sách; câu khủng hoảng/từ chối không gọi model;
model giả chép đúng dữ kiện thì qua kiểm tra trên 4 lá số golden; các lỗi hay gặp bị chặn (JSON hỏng, bịa năm, bịa
điểm, điểm sai tầng, từ cấm, tiếng Anh, chữ Hán, sao không có, sao gán sai cung, quên điểm chung, quá ngắn); câu
có hai nơi chốn không bị bắt nhầm.

## Máy ảo Android (CI)

Máy ảo x86_64 không chạy được model 2,6 GB, nên selftest trên máy ảo kiểm phần chạy được: `aiStatus` đúng dạng;
nạp `liblitertlm_jni.so` thật và gọi vào LiteRT-LM với file giả phải báo lỗi gọn, không văng app; `askAi` khi chưa có
model trả “chưa dùng AI” và Hỏi đáp vẫn trả lời. Cùng các bước cũ: cài đè từ 0.6.1, selftest 4 ca golden, trợ năng,
chạm giữ, chia sẻ, gõ câu hỏi.

Kết quả workflow “Chạy thử trên máy ảo Android”, commit `551752d`
([run 44904090478](https://github.com/thanhphong8808/tu-vi-tu-xem/actions/runs/44904090478)):

| Máy ảo | Cài đè 0.6.1 → 0.7.0 | Selftest | Thư viện LiteRT-LM (probe) | Màn hình thật (trợ năng, chạm giữ, chia sẻ, gõ câu hỏi, PDF, PNG) | Luận một năm |
| --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | ✅ | 523/523 | ✅ nạp `lib/x86_64`, báo lỗi gọn (10 ms) | ✅ | 32 ms |
| Android 14 (API 34) | ✅ | 523/523 | ✅ (24 ms) | ✅ | 114 ms |
| Android 15 (API 35) | ✅ | 523/523 | ✅ (33 ms) | ✅ | 121 ms |
| Android 16 (API 36) | ✅ | 523/523 | ✅ (29 ms) | ✅ | 61 ms |
| Android 17 (API 37) | ✅ | 523/523 | ✅ (23 ms) | ✅ | 68 ms |

Máy ảo có 2–4 GB RAM nên thẻ Trợ lý AI báo “chưa đủ RAM” (đúng như thiết kế). Lần chạy đầu, máy ảo Android 16 trượt
đúng một mục: ngưỡng hiệu năng “luận một năm < 250 ms” đo được 290 ms (máy ảo GitHub chạy chậm lúc đó; phần này không
dính tới AI, cùng APK trên Android 15 đo 121 ms). Chạy lại job đó: 61 ms, đạt hết.
Ảnh mục Hỏi đáp trên máy ảo Android 15: `docs/tai-ve/HoiDap_MayAo_API35.png`.

## Việc anh cần làm trên S22 Ultra

1. Cài `TuViTuXem-0.7.0.apk` đè lên bản cũ (lá số vẫn còn).
2. Mở một lá số → Hỏi đáp → **Tải Trợ lý AI** (cần Wi-Fi, trống ~2,9 GB). Đợi tải và kiểm tra file xong.
3. Hỏi vài câu (ví dụ các câu trong `docs/tai-ve/ai-cham-40-cau.md`). Ghi lại: số giây dưới mỗi câu AI, câu nào
   hiện “chưa đạt kiểm tra”, Cài đặt → Trợ lý AI ghi đang chạy GPU hay CPU.
4. Gửi lại cho Claude số giây và ảnh màn hình để chốt model (mục 12.1) và chỉnh lời nhắc.

## Chưa làm

- Đo tốc độ, chấm tiếng Việt trên chính S22 Ultra (cần máy thật).
- Đồng bộ Firebase câu hỏi ẩn danh (D-53): cần `google-services.json` từ dự án Firebase của anh.
- Model dự phòng Qwen qua llama.cpp (mục 12.1): chưa cần, Gemma 4 E2B đạt 36/40 trên bộ chấm.
