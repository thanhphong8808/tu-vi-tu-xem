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

Chi tiết từng câu: [docs/tai-ve/ai-cham-40-cau.md](tai-ve/ai-cham-40-cau.md). Kết quả mới nhất: _(điền sau lần chấm cuối)_.

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

_(ghi sau khi workflow chạy xong)_

## Việc còn lại

- Anh đo tốc độ Trợ lý AI trên S22 Ultra (vẫn chờ từ 0.7.0).
- Anh duyệt dần câu sách (trong `tools/kb/kb_sach.py`) nếu muốn chỉnh lời.
- YouTube Tử Vi Học Quán: cần mở tên miền youtube.com cho môi trường. Kể cả mở được, video cũng phải chép lời (phụ đề)
  rồi mới diễn giải lại được.
