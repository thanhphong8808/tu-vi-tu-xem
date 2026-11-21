# Báo cáo Giai đoạn 5 — Giao diện đầy đủ

Ngày 21/11/2026. Sản phẩm: `TuViTuXem-0.5.0.apk`, cài đè được lên 0.4.0 mà không mất lá số đã lưu. Knowledge base lên
bản 0.5.0 (vẫn là bản nháp chưa thẩm định). Quyết định thiết kế ghi ở D-42 → D-48 trong `docs/DECISIONS.md`.

## Cổng giai đoạn (mục 15)

| Yêu cầu | Kết quả |
| --- | --- |
| Screenshot test | **Đạt.** 10 ảnh chuẩn trong `tools/apk/uitest/baseline`: lá số, thẻ sao, tab Ngày, so hai lá số, xếp hạng giờ, cài đặt tùy chỉnh, trang chủ, ảnh PNG xuất ra, máy tính bảng (lá số + bảng cung bên phải). Mỗi lần chạy so từng điểm ảnh, lệch quá 0,5% là trượt và lưu ảnh khác biệt. Ngoài ra, trên 5 máy ảo, mỗi bước đều chụp màn hình thật: lá số, thẻ sao sau khi chạm giữ, bảng chia sẻ, 3 trang đầu của file PDF |
| TalkBack | **Đạt.** Mỗi ô lá số là một nút có nhãn đọc theo thứ tự tên cung → can chi → chính tinh (kèm độ sáng) → Tứ Hóa → phụ tinh → tuổi đại hạn (mục 13). axe-core (WCAG 2.1 A/AA) chạy trên 9 màn, không còn lỗi mức serious/critical. Trên cả 5 máy ảo, `uiautomator dump` đọc được nhãn của đủ 12 ô, ví dụ ô Mệnh ca A: “Cung Mệnh, Mậu Dần. Chính tinh: Vũ Khúc Vượng, Thiên Tướng Miếu. Tứ Hóa: Hóa Quyền. Phụ tinh: …” |
| Chỉ tiêu hiệu năng | **Đạt trên JVM và máy ảo nhanh; S22 cần anh đo bằng nút trong app.** JVM: lá số 0,2 ms, luận năm 9–12 ms, chấm 30 ngày 5 ms. Máy ảo: xem bảng dưới. Cài đặt có nút **Đo hiệu năng** để đo ngay trên S22 và so với chỉ tiêu 5 / 50 / 300 ms |

## Đã làm

- **Tab Ngày (mục 2.3 tầng 6, D-42).**
  - Lịch tháng dương lịch, mỗi ô có ngày âm, điểm 0–100 và màu thuận/thận trọng. Lùi, tiến từng tháng.
  - Chạm một ngày để xem chi tiết: điểm, thế, nhật hạn, can chi ngày so với tuổi, giờ thuận / giờ nên thận trọng, bảng
    12 giờ theo cung thời hạn, Nên/Không nên.
  - Thẻ lịch luôn ghi “Độ tin cậy thấp hơn tầng năm”.
  - Có đủ 5 màn luận giải: Trọn đời, Đại vận, Năm, Tháng, Ngày.
- **Thẻ sao (mục 13, D-43).** Chạm giữ một sao trên lưới, hoặc chạm tên sao trong bảng cung. Thẻ có chòm, ngũ hành,
  hóa khí, đặc tính (14 chính tinh, theo bảng thuộc tính P.III), độ sáng tại cung, Tứ Hóa gốc, ý nghĩa tại cung đó và
  cách cục liên quan. Phụ tinh cũng có thẻ.
- **Hồ sơ nhiều người, so hai lá số (P.V.4, D-44).** Chọn hai lá số đã lưu, quan hệ vợ chồng hoặc cha mẹ – con. App
  nhận ra truyền cung và truyền tinh (đẹp / trung tính / bộ sao nặng / một phần). Không có câu trái chiều. Luôn kết
  bằng lời nhắc “quan hệ là tương tác hai chiều”.
- **Không rõ giờ: hỏi–đáp 7 câu (mục 2.3, D-45).** Nhập năm của những việc đã qua (kết hôn, sinh con, thăng tiến, đổi
  việc, thu lớn, hao tài, chuyển nhà), tối thiểu 2 câu. App chấm cả 12 giờ theo mức đồng thanh của từng sự kiện rồi
  xếp hạng, ghi rõ “ước đoán”. Bảng 12 lá số để tự chọn vẫn ở ngay dưới.
- **Trường phái tùy chỉnh từng điểm (D-46).** Ngoài 3 preset có thêm 12 điểm chọn riêng. Lá số và PDF ghi “Tùy chỉnh”.
- **Xuất PNG và PDF A4 (mục 2.4, D-47).**
  - Ảnh PNG lưới 12 cung, 1256×1504.
  - PDF A4 dựng ngay trên máy: trang 1 là lá số và thông tin; các trang sau là luận trọn đời (chủ đề, 12 đại vận,
    12 cung, Nên/Không nên) rồi luận năm đang xem. Chân trang mỗi trang ghi trường phái, ngày lập, số trang và lời ngỏ.
    Ca A ra 8 trang, khoảng 550 KB.
  - **Lưu** vào Tải xuống/TuViTuXem (PDF) hoặc Ảnh/TuViTuXem (PNG), không xin quyền bộ nhớ.
  - **Gửi** qua bảng chia sẻ của Android (Zalo, Drive, Gmail, In…).
- **Màn hình lớn, phóng to (D-48).** Từ 760 px CSS trở lên (S22 xoay ngang, máy tính bảng): lưới bên trái, thẻ bên
  phải; bảng cung/thẻ sao trượt ra thành cột phải. Phóng to lưới bằng chụm hai ngón (1–3×) hoặc nút −/＋/⟲.
- **Trợ năng.** Mọi phần tử chạm được là nút với TalkBack và bàn phím. Đã tăng độ tương phản các ô điểm. Bảng trượt
  khi đóng thì TalkBack không đọc nội dung cũ.
- **Cầu nối mới:**
  - `AppApi`: `days`, `day`, `star`, `compare`, `rankHours`, `configOptions`, `bench`.
  - Android: `exportFile` (PdfDocument, MediaStore, `ShareProvider`).
  - Manifest có thêm provider, không thêm quyền nào: app vẫn không xin quyền, kể cả Internet.

## Kiểm thử

| Bộ kiểm thử | Kết quả |
| --- | --- |
| `:core:interpret` (Gradle, GitHub Actions) | 247/247 test đạt. Thêm `Phase5Test`: lịch tháng (đủ ngày, ngày âm khớp lịch, điểm 5–95, phân hóa), chi tiết ngày (12 giờ đúng cung thời hạn, nhãn độ tin cậy, bất biến bản luận trên 60 ngày ngẫu nhiên), thẻ sao (đủ 14 chính tinh trên 40 lá số), so hai lá số (truyền cung dựng sẵn, 400 cặp không câu trái chiều), xếp hạng giờ (giờ thật đứng đầu, đúng điểm tính độc lập), hiệu năng |
| `:core:calendar`, `:core:engine` | Không đổi, vẫn đạt |
| Giao diện đầu–cuối (Playwright, mã Kotlin thật) | 638/638 mục: giai đoạn 2B–4, cộng toàn bộ phần mới. Gồm: chạm giữ bằng chuột, chụm hai ngón bằng sự kiện chạm thật (CDP), bàn phím Enter, tải ảnh PNG, mô hình PDF, axe-core, so ảnh chuẩn, máy tính bảng 1280 px, màn ngang 860 px |
| Tự kiểm tra trong app (selftest, chạy trên máy ảo) | 499/499 mục: 4 ca golden, luận giải, cộng tab Ngày, thẻ sao, chạm giữ, phóng to, lưu PNG và PDF thật, đo hiệu năng, so hai lá số, xếp hạng giờ, trường phái tùy chỉnh |
| Trên màn hình máy ảo (adb, sau selftest) | Cây trợ năng có nhãn 12 ô; chạm giữ thật 0,9 giây đúng vị trí sao Thiên Tướng mở thẻ sao; bảng chia sẻ PDF mở; PDF kéo về có `%PDF-`, đủ trang, kết thúc `%%EOF`; PNG 1256×1504 |

| Máy ảo (GitHub Actions) | WebView | Cài đè lên 0.4.0 | Tự kiểm tra | Trợ năng · chạm giữ · chia sẻ · PDF/PNG | Lá số / năm / 30 ngày | Xuất PDF |
| --- | --- | --- | --- | --- | --- | --- |
| Android 11 (API 30) | 83 | chạy, không cảnh báo | 499/499 | đạt cả 4 | 0,09 / 31 / 26 ms | 8 trang, 0,3 s |
| Android 14 (API 34) | 113 | chạy, không cảnh báo | 499/499 | đạt cả 4 | 0,94 / 175 / 179 ms | 8 trang, 0,7 s |
| Android 15 (API 35) | 124 | chạy, không cảnh báo | 499/499 | đạt cả 4 | 0,14 / 53 / 50 ms | 8 trang, 0,3 s |
| Android 16 (API 36) | 133 | chạy, không cảnh báo | 499/499 | đạt cả 4 | 1,56 / 125 / 132 ms | 8 trang, 1,3 s |
| Android 17 (API 37) | 145 | chạy, không cảnh báo | 499/499 | đạt cả 4 | 0,22 / 54 / 47 ms | 8 trang, 0,2 s |

Các lần chạy liên tiếp trên GitHub cho 0.5.0 đều đạt cả 5 máy ngay lần đầu, không phải chạy lại lần nào. Số trong bảng lấy từ lần chạy 58d4440.

“Lá số” trong bảng là thời gian lập một lá số. “Năm” là thời gian luận một năm. “30 ngày” là chấm điểm cả tháng. Số
đo bằng `AppApi.bench` ngay trong app, sau khi đã chạy nóng. Máy ảo trên GitHub không có GPU, chạy chung máy chủ, nên
số dao động nhiều giữa các lần và giữa các máy. Android 11 nhanh nhất vì ART cũ biên dịch sẵn nhiều hơn. Chỉ tiêu mục 3
là trên S22: 5 / 50 / 300 ms. Mở **Cài đặt → Hiệu năng trên máy này → Đo hiệu năng** để xem số thật và dấu ✓ cạnh
từng chỉ tiêu.

## Điểm cần anh quyết hoặc lưu ý

1. **Câu luận vẫn là bản nháp.** Câu ngày/giờ, câu so sánh và thuộc tính 14 chính tinh mới thêm cũng chưa thẩm định
   (reviewed = false). Bản luận mẫu để đọc góp ý: `docs/tai-ve/BanLuanMau_GiaiDoan5.txt`.
2. **Ngưỡng màu lịch ngày** dùng chung ngưỡng 55/45 của toàn app. Trên 50 tháng ngẫu nhiên, 30% số ngày thuận và 23%
   số ngày nên thận trọng. Nếu muốn lịch “xanh” hơn hay “đỏ” hơn, chỉ cần đổi ngưỡng.
3. **Xếp hạng giờ** dựa trên năm âm lịch gần đúng của sự kiện. Việc xảy ra tháng 1–2 dương lịch có thể thuộc năm âm
   trước; màn hỏi–đáp có ghi chú điều này.
4. **Android 8–9** lưu PDF/PNG vào thư mục riêng của app (Android/data/vn.tuvi.tuxem/files/TuViTuXem), vì không xin
   quyền bộ nhớ. Nút “Gửi” vẫn dùng bình thường. Máy ảo kiểm thử là Android 11–17.
5. **Khóa ký** vẫn không đưa lên GitHub công khai (D-31). File APK trong `dist/` đã ký đúng khóa cũ, nên cài đè lên
   0.4.0 được (máy ảo đã thử đúng đường cập nhật này).
