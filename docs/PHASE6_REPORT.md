# Báo cáo Giai đoạn 6 — Hỏi đáp

## Bản 0.6.1 (21/11/2026): câu trả lời luận đoán theo lá số

Bản 0.6.0 trả lời bằng cách chọn lại các đoạn của bản luận năm nên đọc như câu cài sẵn. Từ 0.6.1, mỗi câu hỏi được
**luận ngay trên lá số đang mở**, theo trình tự người xem tử vi vẫn làm (D-54):

| Tầng | App xét gì |
| --- | --- |
| Kết luận | Điểm và mức; điểm tựa và áp lực (tầng nào, cung nào, sao gì); xu hướng đầu năm và cuối năm; câu hỏi “có… không” thì trả lời thẳng mức khả năng |
| ① Lá số gốc | Cung của câu hỏi an ở đâu; chính tinh và độ sáng; nghĩa của sao tại đúng cung đó; cát tinh, sát tinh, Tứ Hóa gốc; Tuần/Triệt; cung Thân; vô chính diệu; xung chiếu, tam hợp; cách cục gốc |
| ② Đại hạn | Cung đại hạn, 10 năm, thiên thời; cung của câu hỏi khi xoay 12 cung từ đại hạn và sao tại đó |
| ③ Lưu niên đại hạn | Năm thứ mấy của đại hạn, cung của câu hỏi trong khung lưu niên (kết quả cuối năm) |
| ④ Tiểu hạn | Cung tiểu hạn, cung của câu hỏi trong khung tiểu hạn (diễn biến trong năm) |
| ⑤ Lưu Thái Tuế | Tứ Hóa năm (sao nào hóa gì, rơi cung nào), sao lưu (Lộc Tồn, Kình, Đà, Mã, Hồng Loan, Đào Hoa, Tang Môn, Bạch Hổ…) chạm cung của câu hỏi |
| ⑥ Đồng thanh tương ứng | Mức khả năng theo số tầng; cách vận hạn kích hoạt kèm cung |
| ⑦ Tổng hợp | Điểm từng tầng × trọng số = điểm của câu trả lời; các tầng đồng thuận hay không |

Phạm vi khác:
- Tháng: nguyệt hạn và sao ở đó, Tứ Hóa tháng chạm cung chủ đề, nền của năm.
- Đại vận: gốc, đại hạn, năm thuận nhất và năm cần giữ.
- Trọn đời (cả câu “có số giàu không”): gốc, Mệnh và Thân, điểm từng đại vận.
- “Năm nào…”: mỗi năm kèm căn cứ và phần cần lưu ý.
- Hỏi về người thân: đọc đúng cung của người đó (con → Tử Tức, cha mẹ → Phụ Mẫu…).

Mỗi đoạn có tiêu đề và “Vì sao?”: công thức, các yếu tố cộng trừ điểm lớn nhất, Tứ Hóa, sao lưu. Điểm trùng với điểm
chủ đề trong bản luận đầy đủ. Xem [41 câu hỏi đáp mẫu](tai-ve/HoiDapMau_GiaiDoan6.txt).

**Kiểm thử 0.6.1:**
- Test lõi: 522/522. Bài mới kiểm:
  - từng tầng khớp dữ kiện lá số (cung gốc, đại hạn, lưu niên, tiểu hạn, Thái Tuế, Tứ Hóa theo can năm);
  - dòng tổng hợp cộng đúng;
  - hai lá số hoặc hai năm khác nhau phải cho lời khác nhau;
  - 4 lá số × 3 năm × 10 chủ đề, cộng tháng, đại vận, trọn đời, dò năm;
  - 2 lá số × 11 năm × 5 câu hỏi có sự kiện, không lọt từ cấm.
- Giao diện: 670/670, gồm kiểm tra đủ 7 tiêu đề tầng và căn cứ khi dò năm; axe-core sáng và tối không lỗi nặng.
- Tự kiểm tra trong app (chạy thử trên máy tính): 520/520, mỗi câu 0,1–0,25 giây; gõ phím thật vào ô Hỏi đáp đạt.
- Máy ảo: xem mục “Máy ảo Android (CI) — 0.6.1” bên dưới.

**Model AI sinh lời (LLM).** Đã kiểm tra lại: các máy chủ tải thư viện và model (`dl.google.com`, `maven.google.com`,
`huggingface.co`) vẫn bị chặn từ môi trường build, nên chưa gắn và chưa thử được model (D-55). Phần luận đoán ở trên chạy
hoàn toàn bằng luật cẩm nang trên máy. Khi mở được mạng, model chỉ diễn đạt lại kết quả này, và câu nào sai kiểm tra
thì app quay về câu luận đoán.

---

## Bản 0.6.0

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

Workflow “Chạy thử trên máy ảo Android”, lần chạy 44879531386 (commit `1d1a348`): **đạt cả 5 máy ảo**. Mỗi máy:
- cài 0.5.0 trước;
- cài đè 0.6.0 như người dùng sẽ làm;
- mở app, bắt lỗi văng;
- chạy tự kiểm tra trong app;
- thử trên màn hình thật.

| Máy ảo | Tự kiểm tra trong app | Gõ câu hỏi bằng bàn phím | Câu trả lời chậm nhất | Câu hỏi đầu | Trợ năng, chạm giữ, chia sẻ |
| --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | 519/519 | ✓ “nam sau tai chinh ra sao” → Tài chính · năm 2027 | 480 ms | 98 ms | ✓ ✓ ✓ |
| Android 14 (API 34) | 519/519 | ✓ | 750 ms | 162 ms | ✓ ✓ ✓ |
| Android 15 (API 35) | 519/519 | ✓ | 842 ms | 134 ms | ✓ ✓ ✓ |
| Android 16 (API 36) | 519/519 | ✓ | 442 ms | 90 ms | ✓ ✓ ✓ |
| Android 17 (API 37) | 519/519 | ✓ | 393 ms | 62 ms | ✓ ✓ ✓ |

“Câu trả lời chậm nhất” tính trọn một câu: gọi engine, ghép câu trả lời và vẽ lại màn hình. Câu chậm nhất thường là câu
dò năm hoặc câu cần luận trọn đời lần đầu. Máy ảo trên CI chậm hơn S22 Ultra nhiều.

[Ảnh chụp màn hình máy ảo Android 15](tai-ve/HoiDap_MayAo_API35.png): ngay sau khi adb gõ câu hỏi và bấm Enter.

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
