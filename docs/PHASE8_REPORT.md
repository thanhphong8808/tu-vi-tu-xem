# Báo cáo bản 0.8.0 — Kiến thức sách, giao diện mới, liên hệ

Ngày 21/11/2026. Quyết định chi tiết: `docs/DECISIONS.md` D-61 … D-63.

## Anh yêu cầu gì, đã làm gì

| Yêu cầu | Kết quả |
| --- | --- |
| “Đào tạo thêm” Gemma 4 E2B bằng nguồn uy tín, hai sách PDF, video Tử Vi Học Quán | Đưa kiến thức hai sách vào **kho luận giải của app** (D-62); Trợ lý AI nhận đoạn sách trong dữ kiện và dẫn ý sách. **Không fine-tune** model — lý do bên dưới. YouTube bị chặn |
| Figma: giao diện, icon chuyên nghiệp, hợp lĩnh vực Tướng Số | [File Figma](https://www.figma.com/design/kdxoEkGID6PUamNLPzuvyZ): màu sáng/tối, kiểu chữ, 22 icon, nút, thẻ, 2 màn mẫu; áp vào app (D-63) |
| Bỏ hết chữ “bản nháp” và “prompt” | Giao diện, “Vì sao?”, PDF không còn các chữ này (D-61) |
| Thêm số liên hệ 0378 373 373 | Trang chủ (bấm là gọi), thẻ Liên hệ trong Cài đặt, PDF |

## Vì sao không fine-tune model

- Máy build không có GPU. Huấn luyện LoRA cho model 2 tỷ tham số trên 4 nhân CPU mất nhiều ngày.
- Model đã huấn luyện phải chuyển lại sang `.litertlm` bằng bộ công cụ chuyển đổi riêng. Sau đó cần nơi phát hành file
  2,6 GB mới; GitHub Releases đang bị chặn.
- Quan trọng nhất: model đã fine-tune có thể “nhớ” sai mà không ai kiểm được. App giữ nguyên tắc model **chỉ diễn đạt
  lại** dữ kiện của app (D-58). Kiến thức đưa vào kho luận giải thì câu nào cũng có nguồn, có “Vì sao?”, và bộ kiểm tra
  `AiGuard` đối chiếu được.
- Nếu sau này anh muốn fine-tune thật: LiteRT-LM có hỗ trợ LoRA. Cần một máy có GPU (ví dụ Google Colab) và chỉ dùng câu
  đã duyệt (mục 12.7).

## Kiến thức từ hai sách

- *Tử Vi Đẩu Số Toàn Thư* (Hi Di Trần Đoàn, Vũ Tài Lục dịch và bình chú, 1973): phần **Thập nhị cung luận** — từng sao
  ở từng cung (Mệnh, Quan Lộc, Tài Bạch, Thiên Di, Phu Thê, Phúc Đức, Phụ Mẫu, Nô Bộc, Điền Trạch, Tật Ách, Huynh Đệ,
  Tử Tức), kèm điều kiện miếu/hãm, đồng cung, hội sao, gặp sát tinh.
- *Tử Vi Nghiệm Lý Toàn Thư* (Thiên Lương): các bài chuyên đề về Lộc Tồn theo can, Long Phượng Tả Hữu, Khôi Việt với
  Xương Khúc, sát tinh ngộ chế, Hóa Khoa, Thiên La Địa Võng, Cô Quả với Thái Tuế…
- Hai sách còn bản quyền và repo là công khai, nên mình **diễn giải lại bằng lời của app, không chép nguyên văn**. Các
  câu phán nặng trong sách cổ được đổi thành mức khả năng và lời khuyên, theo quy tắc nội dung của app.
- Kết quả: 354 câu “cung × sao × điều kiện” và 18 ý Nghiệm Lý (`tools/kb/kb_sach.py` → `kb/sach/sach.json`).
- App chọn đúng câu theo lá số, đặt vào đoạn **“Sách bàn về cung …”** trong Hỏi đáp và mục **Sách bàn** trong thẻ sao.
  Nếu sách nêu một nhóm sao mà lá số chỉ có một phần, câu ghi rõ “(lá số có …)”. “Vì sao?” ghi tên sách, chương và căn cứ
  trên lá số.

## Giao diện mới (Figma → app)

- Màu: đỏ sơn mài, vàng kim nhũ, nền giấy dó. Nền tối giữ như cũ. Chữ trắng trên nền vàng dùng vàng đậm `#9A6B12` để đạt
  tương phản AA.
- Chữ: tiêu đề Noto Serif (có sẵn trên Android), nội dung theo font hệ thống.
- Icon tự vẽ, nét mảnh: bát quái (logo), lá số, lập lá số, luận giải, hỏi đáp, trợ lý AI, Bắc Đẩu, lịch, la bàn, trăng,
  đại vận, so hai lá số, giờ sinh, cài đặt, chia sẻ, xuất, liên hệ, sách, riêng tư… Cùng một nguồn SVG cho app và Figma.
- Ảnh màn mẫu trên Figma: [docs/tai-ve/ThietKe_Figma_ManMau.png](tai-ve/ThietKe_Figma_ManMau.png).

## Trợ lý AI chấm lại 40 câu (model thật, CPU máy build)

Chi tiết từng câu: [docs/tai-ve/ai-cham-40-cau.md](tai-ve/ai-cham-40-cau.md).

| Lần | Thay đổi | Qua kiểm tra | Trung vị / lâu nhất |
| --- | --- | --- | --- |
| 0.7.0 | (bản trước) | 36/40 | 19,6 / 28,7 giây |
| 0.8.0 – 1 | Thêm đoạn Sách bàn vào dữ kiện | 36/40 | 20,1 giây (câu đầu 171 giây do dựng lại bộ đệm model) |
| 0.8.0 – 2 | Ghi rõ “(lá số có …)” trong câu sách | 35/40 (4 câu quên điểm chung) | 20,6 / 30,0 giây |
| 0.8.0 – 3 | App tự thêm câu điểm chung khi đó là lỗi duy nhất | **39/40** | 20,0 / 28,8 giây |

Câu còn trượt: “Tôi có số giàu không?” — model bịa điểm 38/100; app hiện bản luận gốc. 7/39 câu AI dẫn ý sách.

- Đoạn “Sách bàn” đi vào dữ kiện của Trợ lý AI; câu AI có dẫn ý sách (“Sách cổ bàn rằng…”).
- Model hay quên nêu điểm chung. App biết điểm này, nên nếu đó là lỗi duy nhất thì app tự thêm câu “Điểm chung: N/100.”
  vào đầu rồi kiểm tra lại toàn bộ.

## Kiểm thử tại chỗ (`bash tools/setup/test_all.sh`)

| Bước | Kết quả |
| --- | --- |
| Gradle test | 873/873 (`:core:calendar` 25, `:core:engine` 316, `:core:interpret` 532 — thêm `BooksTest` 3 test) |
| Build APK | Đạt, ký khóa thật (SHA-256 86:C7:ED…98:C6) |
| Giao diện (`test_ui.py`) | 696/696 — thêm: số liên hệ bấm gọi, logo và icon SVG, đoạn Sách bàn có nguồn + căn cứ, không còn chữ “nháp”/“prompt” |
| Selftest trên máy tính | 523/523 + bước gõ câu hỏi |

Ảnh chuẩn giao diện cập nhật có chủ đích (màu, icon, tiêu đề mới).

## Máy ảo Android (CI)

Workflow “Chạy thử trên máy ảo Android”, commit `9cea98b`
([run 44922395019](https://github.com/thanhphong8808/tu-vi-tu-xem/actions/runs/44922395019)) — **đạt cả 5 máy ngay lần đầu**:

| Máy ảo | Cài đè 0.7.0 → 0.8.0 | Selftest | LiteRT-LM (probe) | Trợ năng, chạm giữ, chia sẻ, gõ câu hỏi, PDF, PNG | Luận một năm |
| --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | ✅ | 523/523 | ✅ | ✅ | 32 ms |
| Android 14 (API 34) | ✅ | 523/523 | ✅ | ✅ | 217 ms |
| Android 15 (API 35) | ✅ | 523/523 | ✅ | ✅ | 44 ms |
| Android 16 (API 36) | ✅ | 523/523 | ✅ | ✅ | 81 ms |
| Android 17 (API 37) | ✅ | 523/523 | ✅ | ✅ | 151 ms |

Ảnh màn hình thật: [trang chủ (Android 15)](tai-ve/TrangChu_MayAo_API35.png) — logo bát quái, tiêu đề Noto Serif, số liên
hệ; [Hỏi đáp (Android 15)](tai-ve/HoiDap_MayAo_API35.png).

Lượt “Kiem tra” chạy theo PR có lần đỏ vì Maven Central trả lỗi khi tải kotlinx-serialization (lượt push cùng commit
xanh). Đã sửa `build.yml`: tải bằng `curl -f`, tải lại 5 lần, kiểm SHA-256.

## Việc còn lại

- Anh đo tốc độ Trợ lý AI trên S22 Ultra (vẫn chờ từ 0.7.0).
- Anh duyệt dần câu sách (trong `tools/kb/kb_sach.py`) nếu muốn chỉnh lời.
- YouTube Tử Vi Học Quán: cần mở tên miền youtube.com cho môi trường. Kể cả mở được, video cũng phải chép lời (phụ đề)
  rồi mới diễn giải lại được.

---

# Bản 0.8.1 — Hỏi đoán năm cưới

Quyết định: D-64.

## Anh hỏi gì, đã làm gì

Anh hỏi: *“Làm cách nào để app có thể trả lời tôi khi tôi hỏi là có thể xem từ lá số của tôi và đoán được năm cưới của
tôi không?”*

| Cách hỏi | Bản 0.8.0 | Bản 0.8.1 |
| --- | --- | --- |
| “Năm nào tôi cưới?”, “Khi nào tôi lấy vợ?” | Dò 10 năm tới | Dò 10 năm tới **và** đối chiếu các năm đã qua |
| “Có thể xem từ lá số của tôi và đoán được năm cưới của tôi không?” | Hiểu nhầm thành “tình duyên năm nay” | Dò năm cưới như trên |
| “Tuổi kết hôn của tôi là bao nhiêu?”, “Lá số có đoán được năm tôi có con không?” | Hiểu nhầm | Dò năm như trên |
| “Tôi đã cưới năm 2015, lá số có khớp không?” | Chỉ luận tình duyên năm 2015 | **Đối chiếu năm 2015** và hai năm mỗi bên |

## Câu trả lời mẫu (lá số Ca A: nam, 15/8/1990 10:30)

**“Có thể xem từ lá số của tôi và đoán được năm cưới của tôi không?”** → *Năm thuận cho chuyện tình cảm, cưới hỏi · dò
2026–2036*

- 2026 (Bính Ngọ, 37 tuổi): khả năng cao (3 tầng cùng chỉ: lưu niên, tiểu hạn, Lưu Thái Tuế). Căn cứ: Hạn hỷ: năm thuận
  cưới hỏi (tiểu hạn)…
- 2035 (Ất Mão): có thể (2 tầng). 2033 (Quý Sửu): mới là dấu hiệu (1 tầng).
- Đối chiếu các năm đã qua: lá số cũng từng nổi tín hiệu vào 2008 (Mậu Tý, 19 tuổi) — khả năng cao. Nếu bạn đã trải qua
  việc này, hãy so với năm thật…

**“Tôi đã cưới năm 2015, lá số có khớp không?”** → *Đối chiếu năm 2015 với lá số*

- Kết luận: năm 2015 lá số có tín hiệu cưới hỏi ở mức mới là dấu hiệu (1 tầng) — khớp một phần.
- Từng năm 2013 → 2017: mức khả năng, tầng cùng chỉ, điểm tình duyên, căn cứ (sao lưu, Tứ Hóa lưu vào cung Phu Thê…).

## App đoán năm cưới dựa vào đâu

Phép **đồng thanh tương ứng** (mục 9): mỗi năm app xét bốn tầng vận — đại hạn, lưu niên đại hạn, tiểu hạn, Lưu Thái
Tuế — xem tầng nào vào cung Phu Thê gặp sao tình duyên (Hồng Loan, Thiên Hỷ, Đào Hoa, Long Trì, Phượng Các), hoặc có sao
lưu tình duyên (Đào Hoa, Hồng Loan, Hỷ Thần lưu) chiếu Mệnh, Thân — luật “Hạn hỷ” trong kho luận giải. Ba tầng cùng
chỉ là “khả năng cao”, hai tầng “có thể”, một tầng “mới là dấu hiệu”. Đây là mức khả năng, không phải lời khẳng định.
Giờ sinh lệch thì năm lệch theo; vì vậy đoạn “đối chiếu năm đã qua” giúp kiểm lại giờ sinh.

Ảnh câu trả lời (lá số Ca A, chụp trên máy chủ thử): [docs/tai-ve/HoiDap_DoanNamCuoi.png](tai-ve/HoiDap_DoanNamCuoi.png).

## Máy ảo Android 0.8.1 (CI)

Lượt đầu (commit `9aaf4ca`) đạt cả 5 máy. Lượt chạy lại cùng APK (commit chỉ sửa tài liệu) có **Android 16 trượt**: câu
“Khi nào tôi lấy vợ?” mất 4,4 giây, quá chỉ tiêu 3 giây, vì phần dò thêm các năm đã qua tính đầy đủ từng năm. Mình đã sửa:
chỉ tính sự kiện đồng thanh (`Composer.yearEvents`) và dò các năm song song (D-64), rồi build lại APK.

Bản sửa, commit `4d0f8ab`
([run 44929804114](https://github.com/thanhphong8808/tu-vi-tu-xem/actions/runs/44929804114)) — **đạt cả 5 máy**.
Workflow “Kiem tra” đạt cả lượt push và lượt PR.

| Máy ảo | Cài đè 0.8.0 → 0.8.1 | Selftest | LiteRT-LM (probe) | Gõ câu hỏi, trợ năng, chạm giữ, chia sẻ, PDF, PNG | Luận một năm | “Khi nào tôi lấy vợ?” |
| --- | --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | ✅ | 523/523 | ✅ | ✅ | 32 ms | 290 ms |
| Android 14 (API 34) | ✅ | 523/523 | ✅ | ✅ | 74 ms | 676 ms |
| Android 15 (API 35) | ✅ | 523/523 | ✅ | ✅ | 79 ms | 1.042 ms |
| Android 16 (API 36) | ✅ | 523/523 | ✅ | ✅ | 67 ms | 680 ms (trước khi sửa: 4.364 ms) |
| Android 17 (API 37) | ✅ | 523/523 | ✅ | ✅ | 48 ms | 504 ms |
