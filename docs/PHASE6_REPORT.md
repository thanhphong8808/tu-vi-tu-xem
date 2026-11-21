# Báo cáo Giai đoạn 6 — Hỏi đáp

Ngày 21/11/2026. Sản phẩm: `TuViTuXem-0.6.0.apk`. Bản này cài đè được lên 0.5.0 mà không mất lá số đã lưu. Knowledge
base lên bản 0.6.0 (vẫn là bản nháp chưa thẩm định). Quyết định thiết kế ghi ở D-49 → D-53 trong `docs/DECISIONS.md`.

## Có gì mới

**Mục Hỏi đáp.** Người dùng gõ câu hỏi bằng lời của mình, có dấu hay không dấu đều được. App trả lời ngay trên máy từ
chính bản luận đã tính, kèm “Vì sao?” cho từng đoạn.

**Vào từ đâu:**
- thẻ **Hỏi đáp** trên màn lá số, có 3 câu gợi ý và nút “Đặt câu hỏi”;
- tab thứ sáu **Hỏi đáp** trong màn Luận giải, cạnh Trọn đời / Đại vận / Năm / Tháng / Ngày.

**Hỏi được gì:**

| Loại câu hỏi | Ví dụ | App trả lời |
| --- | --- | --- |
| Theo thời gian | “Năm sau tài chính ra sao”, “Tháng 3 âm năm 2027 sức khỏe thế nào”, “Ngày mai thế nào”, “Tuần sau công việc ra sao”, “Mùng 1 Tết thế nào”, “Đại vận hiện tại”, “Hậu vận của tôi” | Điểm 0–100, thế, các đoạn luận đúng chủ đề và đúng kỳ, Nên/Không nên |
| Khi nào thuận | “Khi nào tôi lấy vợ”, “Bao giờ tôi giàu”, “Khi nào mua được nhà”, “Năm nào nên chú ý sức khỏe”, “Tháng nào năm sau tốt để cưới”, “Ngày nào trong tháng này tốt” | Dò 11 năm theo đồng thanh tương ứng: khả năng cao / có thể / mới là dấu hiệu. Hỏi theo hướng thận trọng thì xếp từ năm thấp điểm lên |
| Nên / Không nên | “Tôi nên lưu ý gì năm 2027”, “Lời khuyên về tài chính năm sau” | Khuyến nghị của bản luận kỳ đó, kèm lý do |
| Dữ kiện lá số | “Cung Mệnh của tôi có sao gì”, “Tôi mệnh gì”, “Thân cư ở đâu”, “Đại vận đang ở cung nào”, “Năm 2030 tôi bao nhiêu tuổi”, “Hôm nay bao nhiêu âm” | Câu tính từ lá số |
| Giải nghĩa | “Tuần Triệt là gì”, “Hóa Kỵ là gì”, “Sao Vũ Khúc nghĩa là gì”, “Cung Tật Ách là gì” | Định nghĩa (35 thuật ngữ) kèm vị trí trên lá số của bạn. Hỏi một sao thì ra thẻ sao |
| Người thân, chuyện nhạy cảm | “Con tôi học hành thế nào”, “Vợ tôi có ngoại tình không”, “Năm nay có bị bệnh gì không”, “Tôi và vợ có hợp tuổi không” | Không phán. App nói rõ chỉ đọc gián tiếp qua cung tương ứng, rồi trả lời phần luận phù hợp. Câu hợp tuổi có nút sang So hai lá số |
| Ngoài phạm vi | Tuổi thọ, thời điểm mất, giới tính thai nhi, con số may mắn, cúng sao giải hạn, lá số người khác, thời tiết… | Từ chối nhẹ nhàng, gợi ý câu hỏi khác |
| Khủng hoảng tinh thần | “Tôi muốn chết”… | Không đi qua phần luận. App hiện lời động viên với số cấp cứu 115, đường dây Ngày Mai 096 306 1414, Tổng đài 111 |

**Mỗi câu trả lời có:**
- dòng “App hiểu là …” để người dùng biết app hiểu câu hỏi thế nào;
- “Vì sao?” cho từng đoạn;
- 👍/👎;
- 2–3 câu hỏi tiếp;
- nút mở bản luận đầy đủ của kỳ đó.

**Hội thoại** được giữ khi chuyển qua lại giữa các tab.

**Lịch sử câu hỏi:**
- lưu trên máy sau khi lọc bỏ ngày giờ sinh, tên, số điện thoại, email, số nhà;
- dùng cho chip “Hay hỏi”;
- Cài đặt có nút xóa toàn bộ;
- không gửi đi đâu, app vẫn không xin quyền Internet.

Xem [41 câu hỏi đáp mẫu trên lá số ca A](tai-ve/HoiDapMau_GiaiDoan6.txt).

## Cổng giai đoạn (mục 15)

| Yêu cầu | Kết quả |
| --- | --- |
| Bộ câu hỏi mẫu chấm đạt | **Đạt với bộ hỏi đáp theo luật.** Bộ 134 câu (`qa-questions.json`) kiểm từng câu: ý định, chủ đề, sự kiện, năm/tháng/ngày, đơn vị dò. Câu nào viết được không dấu thì kiểm thêm bản không dấu. Mọi câu trả lời phải có mã luật và “Vì sao?”, không lọt từ cấm, không có hai câu chính ngược chiều. Mọi câu gợi ý và câu hỏi tiếp app đưa ra đều phải hiểu được. Bộ câu chạy trên cả 4 lá số golden. **Chưa chấm model AI trên S22**, vì chưa đóng được model vào app (xem dưới) |
| Chạy ở chế độ máy bay | **Đạt.** Hỏi đáp chạy hoàn toàn trên máy; app không có quyền Internet |
| Đầu ra sai tự về rule-based | Hiện chỉ có lớp rule-based. Lớp này chính là đích quay về khi gắn model |
| Payload không chứa ngày sinh, tên, số điện thoại | **Đạt phần lọc.** `QaPrivacy` có test: ngày sinh, giờ sinh, họ tên, số điện thoại, email, số nhà đều bị bỏ, câu sau khi lọc vẫn hiểu đúng như câu gốc. Chưa gửi gì lên Firebase |
| Tắt đồng ý thì không gửi gì | **Đạt.** Chưa có lớp gửi |

## Kiểm thử

**Test lõi.** 515/515 bài kiểm tra. Trong đó 268 bài mới của Hỏi đáp gồm:
- bộ câu mẫu (134 câu, có dấu + không dấu);
- câu hỏi tiếp và câu gợi ý;
- khóa `qa.*` trong mã phải có trong KB;
- khủng hoảng luôn được xét trước;
- giới hạn cho lá số trẻ em;
- dò năm theo hướng thận trọng;
- câu trả lời bám đúng năm được hỏi;
- đầu vào lạ (rỗng, emoji, 3 000 ký tự, chuỗi ngẫu nhiên);
- hiệu năng trên 4 lá số;
- bộ lọc ẩn danh.

Hiệu năng trên JVM: câu đầu khoảng 200 ms (lập bản luận năm), các câu sau 1–20 ms nhờ bộ nhớ tạm.

**Giao diện trên máy tính** (Chromium + máy chủ chạy đúng mã Kotlin của app): 668/668 kiểm tra.
- Phần mới của giai đoạn 6:
  - thẻ Hỏi đáp trên lá số;
  - câu gợi ý;
  - câu không dấu;
  - “Vì sao?”;
  - câu hỏi tiếp;
  - dò năm cưới;
  - 👍;
  - lời hỗ trợ khi khủng hoảng (không có điểm, không có nút đánh giá);
  - từ chối;
  - nút So hai lá số;
  - thẻ sao;
  - mở bản luận năm 2027 từ câu trả lời rồi quay lại vẫn còn hội thoại;
  - Cài đặt đếm đúng số câu đã lưu (không lưu câu khủng hoảng);
  - nút xóa lịch sử.
- axe-core (WCAG 2.1 A/AA) không còn lỗi serious/critical, kể cả nền tối. Khi kiểm nền tối đã sửa độ tương phản của
  nút đang chọn, bong bóng câu hỏi, huy hiệu điểm, chữ “Vì sao?”.
- Có 3 ảnh chuẩn mới: Hỏi đáp nền sáng, Hỏi đáp nền tối, lời hỗ trợ khi khủng hoảng.

**Tự kiểm tra trong app** (`selftest.js`, chạy trong app thật trên máy ảo): thêm phần giai đoạn 6.
- Hỏi 11 câu qua giao diện thật, gọi engine DEX thật. Mỗi câu phải dưới 3 giây trên máy ảo.
- Kiểm lời hỗ trợ khi khủng hoảng, “Vì sao?”, câu hỏi tiếp, mở bản luận, giữ hội thoại.
- Kiểm lịch sử trong SharedPreferences không có câu khủng hoảng.

**Trên màn hình thật của máy ảo** (bước mới trong `ci/emulator_test.sh`): adb chạm vào ô nhập, gõ
`nam sau tai chinh ra sao` bằng bàn phím, bấm Enter, rồi chụp màn hình câu trả lời.

**Chạy thử trên máy tính** (`run_local.py`): 519 kiểm tra đạt, gõ phím thật vào ô Hỏi đáp đạt.

## Máy ảo Android (CI)

Kết quả lần chạy cuối ghi ở mục dưới, sau khi workflow “Chạy thử trên máy ảo Android” chạy xong trên API 30, 34, 35, 36, 37.

## Chưa làm được và lý do

1. **Model AI trên máy (Gemma 4 E2B qua LiteRT-LM, mục 12.1).**
   - Thư viện LiteRT-LM chỉ có trên Google Maven (`dl.google.com` / `maven.google.com`). Môi trường build của bản này
     chặn các tên miền đó, nên không đóng được thư viện vào APK.
   - Cách mở: cho phép `dl.google.com`, `maven.google.com` trong phần cài đặt mạng của môi trường Claude. Sau đó app
     tải model (khoảng 2,6 GB, từ huggingface.co) ngay trên điện thoại khi bật “Trợ lý AI”, rồi chấm bộ 40 câu trên S22.
   - Phần hỏi đáp hiện tại vẫn giữ nguyên làm lớp kiểm tra và dự phòng (D-49).
2. **Đồng bộ câu hỏi lên Firebase (mục 12.6).** Cần dự án Firebase của anh: file `google-services.json`, bật Firestore
   và App Check. Bộ lọc ẩn danh và dữ liệu đúng trường đã sẵn. Có dự án là thêm được lớp gửi và công tắc đồng ý (D-53).
3. Câu luận và lời giải nghĩa vẫn là bản nháp (`reviewed: false`), chờ anh duyệt.
