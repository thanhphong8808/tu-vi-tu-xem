# Báo cáo Giai đoạn 4 — Luận giải và khuyến nghị

Ngày 21/11/2026. Mã mới nằm trong `:core:interpret`: `Scorer`, `Composer`, `Layers`, `ReadingModel`. Knowledge base lên
bản 0.4.0. Sản phẩm là `TuViTuXem-0.4.0.apk`, cài đè được lên 0.3.0.

## Cổng giai đoạn (mục 15)

| Yêu cầu | Kết quả |
| --- | --- |
| Snapshot bản luận của 4 ca golden | **Đạt.** Mỗi ca có 4 bản luận: trọn đời, đại vận chứa năm 2026, năm 2026, tháng Giêng 2026. Lưu ở `core/interpret/src/test/resources/snapshots/A–D.txt`. Test so từng dòng và báo đúng dòng khác. Muốn cập nhật thì chạy `./gradlew :core:interpret:test -PupdateSnapshots` |
| Không có cặp câu trái ngược cùng chủ đề | **Đạt.** Trong mỗi khối, các câu một chiều của một chủ đề luôn cùng chiều, và có nhiều nhất một câu cân nhắc “Có cả … lẫn …, nghiêng về … vì …”. Đã kiểm trên 4 ca golden và 300 lá số ngẫu nhiên: 24 008 đoạn, trong đó 2 664 câu cân nhắc, không vi phạm |
| Quét từ cấm sạch | **Đạt.** Toàn văn mọi bản luận (golden và ngẫu nhiên) không chứa từ nào trong 21 từ cấm của mục 11. Cũng không có “tự hại / tự tử / tự sát”, và không còn ô `{…}` chưa điền. Lint của knowledge base vẫn 0 lỗi |

## Đã làm

- **Chấm điểm (mục 9 bước 1–5).** Mỗi cung tính sao tọa thủ, xung chiếu, tam hợp, giáp (chỉ các cặp có nghĩa) và nhị hợp.
  - Cung vô chính diệu mượn chính tinh cung xung chiếu, hệ số 0,7.
  - Tuần/Triệt tính theo tuổi.
  - Phản vi và làm nhẹ từ luật chỉnh lực của sao.

  Điểm theo công thức `clamp(50 + 10 × Σ, 5, 95)`, có trừ mức nền để 50 điểm là mức trung bình (D-38). Đo lại trên 1 000
  lá số: trung bình 50,2, độ lệch chuẩn 12,6. Có 10 chủ đề theo bảng “Chủ đề → cung đọc chính”. Riêng Pháp lý đọc theo
  sao chỉ báo.
- **Tổng hợp tầng (mục 9 bước 6).**
  - Năm: gốc 0,10, đại hạn 0,30, lưu niên đại hạn 0,25, tiểu hạn 0,20, Lưu Thái Tuế + sao lưu + Tứ Hóa lưu 0,15. Mỗi
    tầng xoay 12 vai trò từ cung của tầng đó.
  - Đại vận: gốc 0,25 + đại hạn 0,75.
  - Tháng: năm 0,50, nguyệt hạn 0,35, Tứ Hóa tháng 0,15.
  - Ngày: tháng 0,50, nhật hạn 0,35, can chi ngày xung/hợp tuổi 0,15.
- **Ghép câu (mục 9 bước 7).** Mỗi chủ đề mở bằng câu điểm. Câu này nêu tầng đỡ nhất hoặc kéo xuống nhất, ví dụ “cung
  Tài Bạch của tiểu hạn tại Tý, trên cung gốc Phu Thê”. Sau đó là:
  - câu “lưu cung” lấy đúng câu sao chính theo vai trò xoay trong KB;
  - các câu luật cùng chiều;
  - câu sự kiện;
  - câu cân nhắc gộp các yếu tố ngược chiều.

  Đoạn nào cũng có mã luật và “Vì sao?”: căn cứ, bằng chứng (sao · cung · quan hệ), điểm từng tầng, nguồn.
- **Đồng thanh tương ứng.** Có 41 luật gắn sự kiện: cưới hỏi, con cái, thăng tiến, đổi việc, tài lộc, hao tài, giấy
  tờ/thị phi, đi xa, sức khỏe. Số tầng cùng chỉ lấy từ điều kiện của luật. 3 tầng thì viết “khả năng cao”, 2 tầng “có
  thể”, 1 tầng “mới là dấu hiệu”. Test đếm lại số tầng độc lập cho mọi câu sự kiện, và trong mẫu đã gặp đủ ba mức.
  Ví dụ ca A năm 2026: hạn hỷ đủ ba tầng (tiểu hạn, Lưu Thái Tuế, lưu niên đại hạn), nên “khả năng cao” là năm thuận
  chuyện tình cảm, cưới hỏi.
- **Nên / Không nên (mục 11).**
  - Lấy từ adviceKeys của các finding mạnh nhất, mỗi loại một mục cho một chủ đề. Nếu thiếu, bổ sung theo chủ đề
    mạnh/yếu, rồi theo thế giai đoạn.
  - Năm và đại vận luôn có 3–5 Nên và 3–5 Không nên (kiểm trên 300 lá số).
  - Mỗi mục ghi “vì: …”.
  - Có 2 tháng thuận và 2 tháng nên thận trọng.
  - Thế Tấn/Giữ/Thủ/Lùi theo ngưỡng 70/50/35.
- **Quy tắc diễn đạt.**
  - Có câu giữ kết nối và tìm chuyên gia tâm lý khi gặp Linh Xương Đà Vũ hoặc từ 3 hung tinh nặng ở Mệnh/Phúc
    (quy tắc 5).
  - Có ghi chú cho phần sức khỏe (quy tắc 3) và tài chính (quy tắc 4).
  - Bản luận kết thúc bằng câu “Đức năng thắng số” (quy tắc 8).
  - Khi không có gì để luận thì ghi “Chưa có luận giải riêng cho tổ hợp này” (nguyên tắc 3).
- **Lá số trẻ nhỏ dưới 16 tuổi âm (D-39).** Chỉ luận học tập, sức khỏe, gia đình, đi lại. Khuyến nghị dành riêng cho
  trẻ. Không nói tới cưới hỏi, đầu tư, đổi việc. Ca golden C (sinh 2023) dùng làm kiểm thử.
- **Knowledge base 0.4.0.**
  - 213 luật: thêm 24 luật năm theo từng tầng, trong đó có đủ 6 tình huống của bảng mục 11.
  - 91 khuyến nghị: 48 Nên, 27 Không nên, 16 Lưu ý.
  - 38 câu sự kiện, thế giai đoạn và ghi chú.
  - Lint kiểm thêm: đủ câu sự kiện ba mức; đủ khuyến nghị dự phòng cho mọi chủ đề, mọi thế và cho trẻ nhỏ.
- **App 0.4.0 (D-41).** Màn Luận giải có các tab Trọn đời / Đại vận / Năm / Tháng:
  - thẻ điểm và thế;
  - các chủ đề, mỗi đoạn có “Vì sao?” và nhãn “nháp”;
  - heat-map 12 tháng (chạm vào để xem tháng);
  - 12 cung và 12 đại vận (chạm vào để xem đại vận);
  - Nên/Không nên/Lưu ý.

## Kiểm thử

| Bộ kiểm thử | Kết quả |
| --- | --- |
| `:core:interpret` (Gradle, GitHub Actions) | 241/241 test đạt. Gồm: lint KB, 213 cặp lá số mẫu cho từng luật, đối chiếu độc lập 27 luật, RuleEngine, ReadingTest (snapshot, bất biến trên 300 lá số, chấm điểm, trẻ nhỏ, hiệu năng) |
| `:core:calendar`, `:core:engine` | Không đổi, vẫn đạt (25 + 316) |
| Giao diện đầu–cuối (Playwright, mã Kotlin thật) | 548/548 mục. Thêm phần màn Luận giải: 7 chủ đề, heat-map, Nên/Không nên 3–5, mọi đoạn có “Vì sao?”, chuyển tab, 12 cung, 12 đại vận, từ cấm |
| Máy ảo Android, cài đè lên 0.3.0 | 475/475 trên cả 5 máy (bảng dưới) |

| Máy ảo (GitHub Actions) | WebView | Bản 0.3.0 trước khi cập nhật | Bản 0.4.0 | Tự kiểm tra | Luận năm / trọn đời |
| --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | 83 | chạy | chạy, không cảnh báo | 475/475 | 68 / 159 ms |
| Android 14 (API 34) | 113 | chạy | chạy, không cảnh báo | 475/475 * | 333 / 932 ms |
| Android 15 (API 35) | 124 | chạy | chạy, không cảnh báo | 475/475 | 92 / 316 ms |
| Android 16 (API 36) | 133 | chạy | chạy, không cảnh báo | 475/475 | 250 / 1 006 ms |
| Android 17 (API 37) | 145 | chạy | chạy, không cảnh báo | 475/475 | 106 / 300 ms |

\* Lần đầu, Google Play Services trên máy ảo tự dừng và kéo app dừng theo. Đây là lỗi môi trường đã ghi ở báo cáo giai
đoạn 3. Script tự chạy lại và đạt.

Thời gian trong bảng tính từ lúc chạm tab đến khi vẽ xong màn hình, gồm cả dựng giao diện, trên máy ảo không có GPU.
Trên JVM, phần tính toán mất 10 ms cho một năm và 36 ms cho trọn đời. Chỉ tiêu “luận một năm < 50 ms trên S22 Ultra”
còn chờ đo trên máy thật.

## Còn lại

- **Duyệt câu luận.** Hiện có 1 273 câu luận và 213 luật ở dạng bản nháp. Bản luận mẫu A–D nên đọc trước để góp ý văn phong.
- **Hiệu chỉnh điểm.** Mức nền và đơn vị đang đo trên lá số ngẫu nhiên (D-38). Khi có phản hồi thực tế, có thể chỉnh
  `PALACE_BASE`, `TOPIC_BASE`, `UNIT_*` trong `Scoring.kt` mà không phải sửa công thức.
- **Giai đoạn 5 (UI đầy đủ):**
  - lịch ngày tô màu theo điểm (đã có `Scorer.day`);
  - chạm giữ một sao để mở thẻ sao;
  - xuất PDF/PNG;
  - hồ sơ gia đình;
  - TalkBack;
  - đo hiệu năng trên S22 Ultra.
