# Báo cáo Giai đoạn 3: Knowledge base và RuleEngine

Ngày 20/11/2026. Sản phẩm: module `:data:kb` và `:core:interpret`, cùng file `TuViTuXem-0.3.0.apk` (1,2 MB). Bản 0.3.0 cài đè lên 0.2.2.

## Cổng giai đoạn (mục 15)

| Yêu cầu | Kết quả |
| --- | --- |
| Knowledge base có JSON schema (mục 9), kể cả `faq/` | **Đạt.** Gồm các thư mục `rules/`, `texts/`, `advice/`, `faq/`, `meta/` và file `index.json`, tổng 604 KB. Bộ nạp đọc chặt: gặp khóa lạ, tên sao, cung hay chi sai, cung neo không khớp tầng vận hạn, hoặc chiều luận ngoài −2..+2 thì báo lỗi kèm `file#luật.when…` |
| ≥ 120 cách cục lấy từ mục 10 và cẩm nang | **Đạt: 189 luật.** Trong đó 33 cát cách, 23 hung cách, 12 phản vi, 35 thế sao khó, 29 dấu hiệu theo cung, 45 vận hạn, 12 chuyên đề |
| Script sinh câu luận nháp (`reviewed: false`) | **Đạt.** `tools/kb/build_kb.py` sinh 189 câu cho luật, 672 câu chính tinh (14 sao × độ sáng × 12 cung), 288 câu cho 24 bộ đồng cung × 12 cung, 100 câu cho 73 phụ tinh, 50 khuyến nghị và 8 câu hỏi mẫu. Mọi mục đều có `reviewed: false` |
| Lint knowledge base | **Đạt.** `KbLint` kiểm tra 21 từ cấm (mục 14.3, so theo ranh giới từ), ô `{…}` ngoài danh sách cho phép, nguồn, textKey, adviceKey, trọng số, luật C phải ở chế độ chuyên sâu, mẫu chặn, và đủ câu cho 24 bộ × 12 cung và mọi phụ tinh. Kết quả: 0 lỗi |
| Mỗi luật có một lá số kích hoạt và một lá số không kích hoạt | **Đạt, 189/189.** 105 luật có lá số “suýt khớp” (điều kiện đầu đúng, cả luật sai). 84 luật còn lại dùng lá số bất kỳ không kích hoạt (D-36) |

## Kiểm thử

| Bộ kiểm thử | Số mục | Kết quả |
| --- | --- | --- |
| `KbAndRulesTest`: lint, khối lượng, lint bắt lỗi cố ý, 189 cặp lá số mẫu, đủ câu luận trên 5 000 lá số (3 trường phái, 2 bảng Cự Môn) | 194 | Đạt |
| `RuleEngineTest`: ngữ nghĩa từng loại predicate trên golden A (gốc và năm 2026), parse báo lỗi, chặn/đảo/làm nhẹ, luật chuyên sâu, luật cụ thể thắng luật chung trên 2 000 lá số, “Vì sao?” luôn có căn cứ, JSON mẫu của prompt, hiệu năng | 11 | Đạt |
| `IndependentRulesTest`: 27 luật viết tay bằng Kotlin thuần, không qua DSL, so với RuleEngine trên 4 000 lá số ngẫu nhiên và mọi lá số mẫu | 1 (≈ 120 000 phép so) | Đạt |
| Lịch và engine giai đoạn 1–2 (không đổi) | 25 + 316 | Đạt (GitHub Actions) |
| Giao diện đầu–cuối (Playwright, mã Kotlin thật). Thêm: thẻ luận giải, “Vì sao?”, luận giải từng cung, quét từ cấm qua 12 năm × 12 cung, chế độ chuyên sâu | 530 | Đạt |
| Máy ảo Android: cài đè lên 0.2.2, mở bản phát hành, tự kiểm tra trong app (4 ca golden và phần luận giải) | 465 / máy | Đạt (bảng dưới) |

Hiệu năng: dò toàn bộ luật cho lá số gốc cộng một năm mất khoảng 2 ms trên JVM. Trên máy ảo, lập lá số kèm luận giải
mất 50–190 ms, tính cả lần nạp knowledge base đầu tiên.

| Máy ảo (GitHub Actions) | WebView | Bản 0.2.2 trước khi cập nhật | Bản 0.3.0 | Tự kiểm tra trong app |
| --- | --- | --- | --- | --- |
| Android 11 (API 30) | 83 | chạy | chạy, không cảnh báo | 465/465 |
| Android 14 (API 34) | 113 | chạy | chạy, không cảnh báo | 465/465 |
| Android 15 (API 35) | 124 | chạy | chạy, không cảnh báo | 465/465 |
| Android 16 (API 36) | 133 | chạy | chạy, không cảnh báo | 465/465 |
| Android 17 (API 37) | 145 | chạy | chạy, không cảnh báo | 465/465 |

Trên máy ảo Android 14 có hai lần hệ thống dừng app giữa chừng. Nhật ký sự kiện ghi
`depends on provider com.google.android.gms/.fonts.provider.FontsProvider in dying proc`: Google Play Services trên
máy ảo tự dừng, và Android dừng luôn mọi app đang dùng dịch vụ font của nó. Đây là lỗi môi trường, không phải lỗi app.
Cùng file APK đó đạt 465/465 ở lần chạy khác. `ci/emulator_test.sh` nay nhận ra trường hợp này và tự chạy lại một lần.
Mọi trường hợp app dừng vì lý do khác vẫn tính là lỗi, kèm chẩn đoán đầy đủ.

## Lỗi tìm ra và đã sửa trong giai đoạn

- Bộ đọc DSL bỏ qua `at` khi đi kèm `chiIn`/`roleIn`. Ví dụ `{"at":"tieuHan","chiIn":[…]}` bị đọc thành điều kiện chi
  của cung neo. Hiện chưa luật nào dùng tổ hợp này, nhưng test predicate đã bắt được.
- Cộng dồn tác động: Cự Môn Thìn Tuất tuổi Tân (đảo) gặp cả “sao hãm gặp Lộc Quyền” (làm nhẹ) cho ra câu Cự Môn hãm
  trung tính. Đã sửa: chỉ luật ưu tiên cao nhất có hiệu lực (D-35).
- “Đảo chiều” đổi luôn câu đã tốt thành xấu: Thiên Hình đắc địa ở Mệnh golden A từ +1 thành −1. Đã sửa: phản vi
  nghĩa là xấu hóa tốt, câu đã tốt giữ nguyên.
- Luật quá rộng, kích hoạt ở phần lớn lá số, làm loãng luận giải. Đã thu hẹp theo cẩm nang:
  - Phúc Đức đẹp: 63 % xuống 36 %;
  - Hạn hỷ đơn lẻ: 72 % xuống 38 %;
  - Phụ Mẫu Nhật Nguyệt sáng: 59 % xuống 26 %;
  - Tử Tức cần chăm sóc: 39 % xuống 9 %;
  - Sinh con theo sao Thai: 35 % xuống 26 %.

  Tỷ lệ đo trên 3 000 lá số ngẫu nhiên.

  “Tuần Triệt giải hung” chuyển thành luật chỉ tác động (chiều 0).
- Phiên bản Gradle: `:data:kb` phải khai đích JVM 17, nếu không `:core:interpret` không phân giải được phụ thuộc.

## Trong app 0.3.0

- **Thẻ “Cách cục & dấu hiệu lá số”** ngay dưới lưới 12 cung, nhóm theo loại: cát cách, hung cách, phản vi, thế sao,
  theo cung, chuyên đề. Mỗi mục có chiều (++ / + / · / − / −−) và cung. Chạm vào mục để mở **“Vì sao?”**: sao, cung,
  quan hệ làm căn cứ (tọa thủ, xung chiếu, tam hợp, giáp…), gợi ý nên/không nên/lưu ý, độ tin cậy và nguồn trong cẩm nang.
- **Dấu hiệu năm** đang xem và **đại hạn đang đi**, đặt trong thẻ vận hạn. Thẻ đổi theo năm khi kéo thanh năm.
- **Bảng chi tiết cung** có thêm câu luận bộ đồng cung, chính tinh theo độ sáng và phụ tinh, cùng các cách cục tại cung đó.
- **Cài đặt → Luận giải → Chế độ chuyên sâu:** bật để hiện cả luật độ tin cậy C.
- Thẻ luôn ghi “bản nháp tự sinh, chưa qua thẩm định”. Nếu nạp knowledge base lỗi, lá số vẫn hiện bình thường.

## Còn lại cho giai đoạn sau

- **Duyệt câu luận.** 1 249 câu và 189 luật đang là bản nháp. Khi anh duyệt mục nào, đổi `reviewed: true` trong
  `tools/kb/*.py` rồi build lại.
- **Giai đoạn 4:**
  - bộ ghép câu (gộp, tránh lặp, chuyển ý);
  - chấm điểm từng chủ đề theo trọng số mục 6 (quan hệ cung × độ sáng × chiều luận);
  - khuyến nghị tổng hợp có điều kiện;
  - trả lời câu hỏi thường gặp từ `faq/`.
- Lá số mẫu dạng “bất kỳ” của 84 luật có thể thay dần bằng lá số suýt khớp, khi viết luật theo dạng điều kiện chính đứng đầu.
