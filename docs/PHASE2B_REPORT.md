# Báo cáo Giai đoạn 2B — Bản chạy thử cài lên điện thoại

Ngày: 20/11/2026 · Sản phẩm: `TuViTuXem-0.2.2.apk` (1,1 MB) · package `vn.tuvi.tuxem` · Android 8.0+ (minSdk 26, targetSdk 35)

## Cổng giai đoạn

| Cổng (mục 15) | Kết quả |
| --- | --- |
| 4 ca golden hiển thị đúng trên lưới 12 cung | **PASS.** Nhập qua form như người dùng; đối chiếu mọi chính tinh và phụ tinh có trong golden, Tứ Hóa, Tuần/Triệt, Mệnh, Thân, Cục, âm lịch, tuổi đại hạn, đại hạn/tiểu hạn/lưu niên đại hạn/nguyệt hạn tháng Giêng năm 2026 |
| Chạy trên điện thoại | **PASS trên máy ảo Android 11, 14, 15, 16** (GitHub Actions): app mở không văng, tự kiểm tra 4 ca golden trong app thật đạt 430/430 ở mọi máy. Còn chờ anh cài thử trên máy thật |
| Chưa cần luận giải | Đúng phạm vi: lá số + vận hạn, chưa có luận giải |

## Sự cố bản 0.2.0 (đã sửa ở 0.2.2)

Bản 0.2.0 thoát ngay khi mở trên mọi máy Android 11 trở lên. Nguyên nhân: gọi `window.insetsController` trước
`setContentView` → NullPointerException trong `onCreate` (D-32). Kiểm thử trên trình duyệt không bắt được lỗi này,
vì nó nằm ở phần Activity chỉ chạy trên Android. Từ nay mọi APK phải qua máy ảo Android trước khi gửi (D-33).

| Máy ảo (GitHub Actions) | WebView | Bản 0.2.0 | Bản 0.2.2 mở app | Tự kiểm tra trong app |
| --- | --- | --- | --- | --- |
| Android 11 (API 30) | 83 | văng (NPE) | chạy, không cảnh báo | 430/430 |
| Android 14 (API 34) | 113 | văng (NPE) | chạy, không cảnh báo | 430/430 |
| Android 15 (API 35) | 124 | văng (NPE) | chạy, không cảnh báo | 430/430 |
| Android 16 (API 36) | 133 | văng (NPE) | chạy, không cảnh báo | 430/430 |

Cũng nhờ máy ảo mà tìm ra thêm: trên WebView cũ (Android 11), khung “Lời ngỏ” lệch xuống đáy màn hình vì CSS `inset`.
Đã sửa. Bản 0.2.2 cài đè lên 0.2.0 được, không cần gỡ app cũ.

## Tính năng

- Màn nhập (mục 2.1): họ tên; giới tính; dương hoặc âm lịch (ô tháng nhuận chỉ hiện khi năm đó nhuận đúng tháng
  đang chọn); giờ:phút, 13 canh giờ hoặc “Không rõ”; miền Bắc/Nam/nước ngoài (múi giờ IANA); giờ Mặt Trời thật;
  năm xem. Ngay dưới ô ngày sinh có gợi ý đổi dương ↔ âm.
- Thẻ xác nhận (mục 2.2): dương ↔ âm, tứ trụ, Âm Dương Nam Nữ, bản mệnh, Cục và quan hệ Cục–Mệnh, Mệnh/Thân,
  tuổi âm. Có cảnh báo kèm nút “Xem lá số thay thế” cho các trường hợp: Tý muộn, tháng nhuận, lịch UTC+7/+8,
  giờ sát ranh giới canh.
- Lưới 12 cung (mục 13):
  - bố cục 4×4 truyền thống;
  - mỗi ô có can chi, vai trò cung, nhãn Thân, tuổi đại hạn;
  - chính tinh chữ đậm kèm độ sáng, huy hiệu Tứ Hóa, cát tinh/hung tinh, vòng Tràng Sinh và chi tiểu hạn ở chân ô;
  - Tuần, Triệt vẽ trên đường ranh giới; màu theo ngũ hành;
  - màn hình ngang tự chia hai cột cát | hung;
  - chế độ tối; phóng to bằng hai ngón.
- Chạm một cung: tô tam phương tứ chính (có đường nối), hai cung giáp, cung nhị hợp; mở bảng chi tiết. Bảng có
  sao theo nhóm kèm độ sáng và ngũ hành, đại hạn, tiểu hạn, lưu niên, các cung liên hệ.
- Vận hạn:
  - thanh chọn năm (nút ◀ ▶ và thanh trượt); bật/tắt lớp đại/tiểu hạn, sao lưu (tiền tố “L.”), Tứ Hóa lưu, nguyệt hạn từng tháng;
  - thẻ năm: đại hạn, Thiên thời, tiểu hạn, lưu niên đại hạn, Lưu Thái Tuế;
  - dòng 12 đại hạn (chạm để nhảy tới năm đầu vận); bảng nguyệt hạn 12 tháng.
- Không rõ giờ: lập 12 lá số và so sánh cung Mệnh, chính tinh, Thân, Cục, đại hạn đầu; chạm để chọn.
- Lưu nhiều hồ sơ, mở lại, sửa, xóa. Cài đặt có 3 preset trường phái. Lời ngỏ lần đầu dùng nguyên văn mục 11.
- Chạy offline hoàn toàn, không xin quyền nào.

## Kiểm thử

| Lớp | Kết quả |
| --- | --- |
| Engine (`:core:calendar`, `:core:engine`) | 341/341 test pass (không đổi) |
| Giao diện đầu-cuối (`tools/apk/uitest/test_ui.py`, Playwright + DevServer) | **508/508 kiểm tra đạt**, 0 lỗi JavaScript |
| Cùng bộ test trên đúng bytecode bản Android (biên dịch kiểu Android, stdlib đã vá) | 508/508 đạt |
| Bản vá Comparators so với kotlin-stdlib gốc | 5 phép sắp xếp khớp hoàn toàn |
| DEX | 0 `invoke-custom`/`invoke-polymorphic`; 15.810 method (< 65.536); `@JavascriptInterface` giữ nguyên |
| APK | `apksigner verify`: chữ ký v2 + v3 hợp lệ; `aapt dump badging` đúng tên app, package, SDK; 0 quyền |

Ngoài 4 ca golden, bộ test giao diện còn kiểm:
- nhập âm lịch tháng nhuận (ca C), chọn canh giờ (ca D), không rõ giờ rồi chọn giờ (ca A);
- lá số thay thế ca B (giữ ngày → 12/2/1985), sau đó quay lại lá số chính;
- lưu, mở lại, xóa hồ sơ; preset Thiên Lương (năm Canh: Đồng Khoa, Âm Kỵ); đổi năm và nhảy theo đại hạn;
- tô sáng tam phương tứ chính, nút Back theo đúng thứ tự (bảng chi tiết → màn trước → trang chủ);
- báo lỗi khi chưa chọn giới tính hoặc nhập ngày 31/2.

## Chưa kiểm được

- Chạy trên máy thật. Nếu app dừng, lần mở sau sẽ hiện báo cáo lỗi kèm nút Sao chép; lỗi JavaScript hiện ngay
  trên màn hình.
- Android 17: tùy GitHub đã có ảnh máy ảo `android-37.0` hay chưa (job được đánh dấu tùy chọn).

Trên GitHub: build Gradle lần đầu chạy thật đã đạt, và job `apk` đã build lại APK từ mã nguồn thành công.

## Quyết định mới

D-28 (APK không cần Android Studio, giao diện WebView), D-29 (màu và cột sao chỉ để hiển thị),
D-30 (vá invokedynamic của stdlib), D-31 (khóa ký nằm trong dự án),
D-32 (nguyên nhân lỗi 0.2.0), D-33 (bắt buộc chạy máy ảo trước khi gửi) — xem `docs/DECISIONS.md`.
