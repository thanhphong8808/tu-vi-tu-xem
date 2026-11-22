# Nhật ký quyết định — Tử Vi Tự Xem

Ghi lại mọi chỗ agent phải tự quyết khi prompt (`docs/PROMPT_TUVI_APP.md`) chưa nói rõ hoặc không làm đúng nguyên văn được.
Mỗi mục: quyết định, lý do, ảnh hưởng. Mã `D-xx` được tham chiếu trong code.

## Giai đoạn 1 — Lịch + engine an sao (20/11/2026)

### D-01 · Can, Chi, LunarDate, BirthRegion đặt ở `:core:calendar`
Prompt đặt kiểu dữ liệu lõi trong `:core:engine`, nhưng lớp lịch cũng cần chúng (can chi ngày/tháng/giờ, ngày âm).
Đặt ở `:core:calendar`, `:core:engine` khai báo `api(project(":core:calendar"))` nên mọi module dùng engine vẫn thấy đủ.
Tên enum và giá trị giữ đúng như mục 3.

### D-02 · Dùng `java.time` thay `kotlinx-datetime`
`BirthInput.zone` có kiểu `java.time.ZoneId` thay vì `TimeZone`; `solar` là `java.time.LocalDateTime`.
Lý do: hai module lõi là Kotlin/JVM, Android minSdk 26 có sẵn `java.time` (kể cả tzdb lịch sử của `Asia/Ho_Chi_Minh`),
bớt một thư viện. Nếu sau này cần Kotlin Multiplatform thì đổi sang kotlinx-datetime ở ranh giới API.

### D-03 · Vá lỗi “ngày âm 0” của thuật toán Hồ Ngọc Đức bản gốc
Bản gốc (và mọi bản port nguyên văn, kể cả `amlich.js` trên npm) chỉ lùi điểm sóc một lần trong `convertSolar2Lunar`.
Khi sóc thật đến muộn hơn sóc trung bình, kết quả là ngày âm = 0. Quét 1900–2100 thấy đúng 7 trường hợp:

| Dương lịch | tz | Bản gốc | Đã vá |
| --- | --- | --- | --- |
| 19/04/1939 | 8 | 0/3/1939 | 30/2/1939 |
| 22/03/1947 | 8 | 0/2N/1947 | 30/2/1947 |
| 26/03/2009 | 8 | 0/3/2009 | 30/2/2009 |
| 07/05/2054 | 7 và 8 | 0/4/2054 | 30/3/2054 |
| 09/04/2062 | 7 và 8 | 0/3/2062 | 30/2/2062 |

Hai ngày 1939 và 1947 rơi vào thời kỳ lịch UTC+8 nên ảnh hưởng người thật. Sửa `if` thành vòng `while`;
mọi ngày khác giữ nguyên kết quả bản gốc (đã so 146.828 ngày với `amlich.js`, xem `tools/crosscheck/calendar_crosscheck.js`).

### D-04 · `BirthInput` có thêm trường
Giữ nguyên các trường của prompt, bổ sung:
- `lunarClock: LocalTime?` — giờ đồng hồ khi nhập âm lịch.
- `canh: CanhGio?` — chế độ chọn canh giờ. `CanhGio` tách giờ Tý thành `TY_SOM` (00:00–00:59) và `TY_MUON` (23:00–23:59)
  vì hai trường hợp khác nhau về đổi ngày.
- `region: BirthRegion` (Miền Bắc / Miền Nam / Nước ngoài) — múi giờ lịch 1967–1975 và cách quy đổi giờ.
- `calendarTzOverride: Int?` — ép lịch UTC+7 hoặc UTC+8, dùng cho “xem lá số thay thế”.

Không rõ giờ sinh: `ChartEngine.buildAllHours()` trả về 12 lá số (Tý tính là Tý sớm). Phần hỏi–đáp xếp hạng giờ thuộc Giai đoạn 4–5.

### D-05 · Nhập âm lịch = ngày âm dân lịch
Ngày âm người dùng nhập được đổi sang ngày dương theo múi giờ lịch của thời kỳ, rồi đi chung đường với nhập dương lịch.
Hệ quả: nhập âm lịch kèm giờ 23:xx cũng được đổi sang ngày âm hôm sau theo `dayBoundary`, nhất quán với nhập dương lịch
(test `nhap am lich cho cung la so voi nhap duong lich` kiểm 1.000 ngày ngẫu nhiên).
Không phát cảnh báo lệch lịch UTC+7/UTC+8 với nhập âm lịch, vì người dùng đã cho chính ngày âm.

### D-06 · Sinh ở nước ngoài
Canh giờ lấy theo **giờ chuẩn địa phương** (bỏ giờ mùa hè DST qua tzdb), ngày theo địa phương; đổi âm lịch bằng lịch Việt Nam
(UTC+7 sau 1975, UTC+8 trước 08/08/1967). Giai đoạn 08/08/1967–30/04/1975 dùng UTC+7 và cảnh báo nếu UTC+8 cho ngày âm khác.
Nếu bỏ DST làm đổi canh giờ, thẻ xác nhận báo `CLOCK_CONVERTED` và cho xem lá số theo giờ ghi nguyên văn.

### D-07 · Tách Tứ Hóa năm Nhâm khỏi khóa `tuHoa`
Câu hỏi (1) ở mục 15 hỏi riêng về Nhâm hóa Khoa, nên `SchoolConfig` có hai khóa độc lập:
`tuHoa` (hàng Canh: Nam phái VN hay theo cụ Thiên Lương) và `nhamKhoa` (Tả Phù hay Thiên Phủ).
Preset “Theo cụ Thiên Lương” đặt cả hai theo cụ Thiên Lương.

### D-08 · Nội dung preset “Toàn Thư / Trung Châu”
Prompt chỉ nêu tên preset. Đã đặt: Khôi Việt `TOAN_THU`, Hỏa Linh `TRUNG_CHAU`, nguyệt hạn `DAU_QUAN_LUU_THAI_TUE`,
Thân chủ tuổi Tý `HOA_TINH`; các khóa còn lại giữ mặc định (Tứ Hóa Canh Đồng Kỵ, Nhâm Tả Phù hóa Khoa).
Nếu bạn theo một bản Toàn Thư cụ thể khác, chỉ cần sửa `SchoolPreset.TOAN_THU_TRUNG_CHAU`.

### D-09 · Quy tắc cảnh báo trên thẻ “Xác nhận lá số”
- `CALENDAR_TZ`: chỉ trong thời kỳ mơ hồ (trước 08/08/1967 với mọi nơi sinh; đến 30/04/1975 với miền Nam và nước ngoài)
  **và** chỉ khi UTC+7, UTC+8 cho ngày âm khác nhau — đúng ghi chú ca golden D.
- `NEAR_HOUR_BOUNDARY`: cách ranh giới canh giờ (01:00, 03:00 … 23:00) không quá 10 phút, tính cả hai đầu.
  Nửa đêm không phải ranh giới (Tý muộn và Tý sớm cùng là giờ Tý).
- Thêm hai cảnh báo prompt chưa liệt kê nhưng suy ra từ mục 4: `CLOCK_CONVERTED` (quy đổi giờ lịch sử/DST làm đổi canh)
  và `TRUE_SOLAR_SHIFT` (hiệu chỉnh giờ Mặt Trời làm đổi canh).
- Mọi cảnh báo mang sẵn `altInput`/`altConfig`; `ChartEngine.buildAlternative(chart, warning)` dựng lá số thay thế.

### D-10 · Phân loại sao và tên định danh
- Trung tinh cát/hung (quyết định cột trái/phải và thứ tự hiển thị) là phân loại tạm, xem `Stars.kt`;
  KB Giai đoạn 3 có thể tinh chỉnh mà không đụng engine.
- `QUAN_PHU` = **Quan Phù** (vòng Thái Tuế, sao pháp lý trong mục 9), `QUAN_PHU_BS` = **Quan Phủ** (cuối vòng Bác Sĩ).
- `StarId.PHUC_DUC` là sao Phúc Đức vòng Thái Tuế; cung Phúc Đức là `PalaceRole.PHUC_DUC`.

### D-11 · `SPLIT_15` với tháng nhuận cuối năm
Sinh từ ngày 16 tháng nhuận được tính sang tháng sau; tháng nhuận 12 (nếu có) tính sang tháng 1 cùng năm âm.
1900–2100 không có tháng 12 nhuận; có tháng 11 nhuận năm 2033 (tính sang tháng 12).

### D-12 · Bộ test: JUnit 5, property test bằng bộ sinh có seed
Prompt ghi “JUnit5 + Kotest property testing”. Giai đoạn 1 được kiểm chứng trong môi trường không truy cập được Maven Central,
nên không biên dịch thử được mã Kotest. Để không giao mã chưa chạy, property test viết bằng JUnit 5 + helper `Prop.forAll`
(seed cố định, báo seed và mẫu hỏng). Đổi sang Kotest `checkAll` chỉ là thay helper này.

Cách đã kiểm chứng: biên dịch bằng `kotlinc` 2.0.21 (cùng phiên bản với Gradle plugin trong `libs.versions.toml`),
JUnit Jupiter 5.8.2 dựng từ mã nguồn, chạy qua JUnit Platform Launcher. Lệnh `./gradlew test` chưa chạy được trong môi trường
đó (không tải được plugin Kotlin); lần chạy `./gradlew test` đầu tiên trên máy bạn là bước xác nhận build Gradle.

### D-13 · Chưa tạo module Android ở Giai đoạn 1
Chỉ có `:core:calendar`, `:core:engine` (Kotlin/JVM, đích JVM 17) — chạy test không cần Android SDK.
Module Android (`:data:store`, `:feature:*`, `:app`) thêm ở các giai đoạn sau theo mục 15.

### D-14 · Đối chiếu lasotuvi: ba chỗ lasotuvi lệch khẩu quyết, engine giữ đặc tả
Mục 5 viết bản tham chiếu “khớp 100% … mọi phụ tinh không thuộc nhóm biến thể” với lasotuvi. Đối chiếu lại 9.000 lá số
(nhập âm lịch trực tiếp, bật các biến thể lasotuvi dùng: Hỏa Linh Đắc Lộc, Lưu Hà, Thiên Giải nhảy cung, Tứ Hóa Canh/Nhâm
theo cụ Thiên Lương) cho kết quả khớp 100% Mệnh, Thân, Cục, đại hạn, tiểu hạn và mọi sao, **trừ**:
- **Thai, Dưỡng**: lasotuvi đảo ngược — prompt đã biết.
- **Phá Toái**: lasotuvi đặt Tý Ngọ Mão Dậu → Dậu, Dần Thân Tỵ Hợi → Tỵ, ngược khẩu quyết “Tý Ngọ Mão Dậu cư Tỵ,
  Dần Thân Tỵ Hợi cư Dậu, Thìn Tuất Sửu Mùi cư Sửu” mà mục 5.4 ghi. Có vẻ do lasotuvi đánh chỉ số chi từ 1.
  Engine theo mục 5.4. Đây là điểm câu “khớp 100%” trong prompt chưa đúng.
- **Thiên Khôi, Thiên Việt**: lasotuvi đảo Khôi/Việt với tuổi Đinh, Mậu, Nhâm và đặt Canh ở Mùi/Sửu, khác cả khẩu quyết
  “Giáp Mậu Canh ngưu dương, Bính Đinh trư kê, Nhâm Quý thố xà” lẫn hai biến thể của mục 7. Engine theo mục 5.5.

### D-15 · Ô “không xét” độ sáng
Sao không có trong bảng mục 6 có `brightness = null`. Hóa Kỵ, Song Hao, Khốc Hư, Thiên Hình, Thiên Riêu ngoài cung đắc địa
cũng là null (prompt ghi “—”), không phải Hãm.

### D-16 · `brightness.json` nằm trong `:core:engine`
Bảng độ sáng là dữ liệu của engine an sao (ghi lên lá số), không phải câu luận, nên để ở tài nguyên của `:core:engine`
thay vì `:data:kb`. Vẫn là JSON, sửa không cần đụng code; trình đọc kiểm đủ 14 chính tinh × 12 cung và hai biến thể Cự Môn.

### D-17 · Tuổi trước đại hạn đầu
`Palace.daiHanStartAge` ghi tuổi khởi từng cung. Tuổi nhỏ hơn số Cục (vd. 1–4 tuổi với Thổ ngũ cục) chưa vào đại hạn nào;
LimitEngine (Giai đoạn 2) quyết định cách trình bày giai đoạn này.

### D-18 · Ba câu hỏi trường phái (mục 15)
Đã cài đủ cả hai lựa chọn cho mỗi câu; mặc định đang là:
1. Năm Nhâm hóa Khoa: **Tả Phù** (`nhamKhoa = TA_PHU`).
2. Sinh tháng nhuận: **tính như tháng chính** (`leapMonth = SAME_MONTH`).
3. Khôi Việt tuổi Canh: **Ngọ/Dần** (`khoiViet = VN_PHO_THONG`).

Trạng thái: **người dùng đã xác nhận cả ba mặc định ngày 20/11/2026.** Lựa chọn còn lại vẫn chỉnh được trong Cài đặt → Trường phái.

## Cập nhật prompt (20/11/2026)

### D-19 · Trợ lý AI trên máy và câu hỏi thường gặp
Theo yêu cầu người dùng, mục 12 của prompt được viết lại: bỏ lớp gọi API đám mây, thay bằng trợ lý chạy trên máy
(Gemma 4 E2B qua LiteRT-LM, dự phòng Qwen3.5-2B/4B qua llama.cpp; chốt model bằng bộ 40 câu hỏi mẫu trên S22 Ultra).
Câu hỏi thường gặp lưu Room trên máy; bản ẩn danh đồng bộ lên Firebase Firestore (gói Spark miễn phí) chỉ khi người dùng
bật đồng ý. Lộ trình thêm Giai đoạn 2B (bản chạy thử giao diện) và Giai đoạn 6 thành “Trợ lý AI + câu hỏi thường gặp + phát hành”.
Prompt cũng được sửa theo D-02, D-03, D-12, D-14 để agent sau đọc đúng.

## Giai đoạn 2 — Vận hạn (20/11/2026)

### D-20 · Tuổi trước đại hạn đầu
Tuổi âm nhỏ hơn số Cục: `daiHan` và `luuNienDaiHan` lấy cung Mệnh (cung của đại hạn đầu), `namThuDaiHan = null`,
`beforeFirstDaiHan = true`. Giai đoạn 4 nên hạ độ tin cậy kết luận năm cho các tuổi này và dựa chủ yếu vào tiểu hạn.
Tuổi vượt đại hạn thứ 12 (≥ Cục + 120) thì vòng đại hạn lặp lại.

### D-21 · Khóa của `luuStars`
Giữ đúng kiểu `Map<StarId, Chi>` của prompt: khóa là sao gốc tương ứng (`THAI_TUE` → Lưu Thái Tuế, `LOC_TON` → Lưu Lộc Tồn…),
đủ 15 sao lưu của mục 8.4. UI hiển thị với tiền tố “L.”.

### D-22 · Lưu Hỷ Thần đi theo chiều của đương số
Lưu Hỷ Thần = Lưu Lộc Tồn + 7·dir (sao thứ 8 vòng Bác Sĩ lưu), dir là Âm Dương Nam Nữ của lá số.

### D-23 · Nguyệt hạn và Tứ Hóa tháng
Công thức nguyệt hạn dùng tháng sinh đã áp quy tắc tháng nhuận (`effectiveMonth`) và chi giờ sinh.
Tháng đang xem là tháng nhuận thì dùng chung cung với tháng chính. Tứ Hóa nguyệt theo can tháng (Ngũ hổ độn từ can năm đang xem).

### D-24 · Ngày đang xem đổi âm lịch theo UTC+7
`LimitEngine.forDate`/`forHour` mặc định đổi ngày dương sang âm bằng lịch UTC+7 (ngày đang xem luôn sau 1975 trong thực tế);
có tham số `tz` nếu cần.

## Kiểm tra toàn diện (20/11/2026)

### D-25 · Ngày âm chỉ có ở lịch còn lại
Chạy vét cạn phát hiện 204 ngày âm trước 1967 (vd. 30/7/1950) có trong lịch UTC+7 nhưng không có trong lịch UTC+8
mà thời kỳ đó dùng. Trước đây app báo “ngày không tồn tại”, nên người sinh những ngày này không lập được lá số.
Nay nhập âm lịch mà ngày không có trong lịch của thời kỳ thì engine dùng lịch còn lại, kèm cảnh báo `CALENDAR_TZ`
nhắc kiểm tra giấy tờ. Ép lịch bằng `calendarTzOverride` thì vẫn báo lỗi rõ ràng.

### D-26 · Khoảng năm sinh hỗ trợ: 1900–2100
Ngày sinh (dân lịch) ngoài 01/01/1900–31/12/2100 bị từ chối bằng thông báo dễ hiểu. Đây là khoảng đã kiểm vét cạn;
thuật toán lịch còn đúng ngoài khoảng này nhưng chưa được kiểm.

### D-27 · Công cụ dev in UTF-8
Các công cụ in tiếng Việt (dumpFull, stressCheck) ghi UTF-8 tường minh và mọi tác vụ JavaExec/Test được đặt
`-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`, tránh chữ “Đ” thành “?” trên Windows.

### D-28 · Bản 2B: file APK build không cần Android Studio, giao diện WebView thay cho Compose
Người dùng cần một file cài thẳng lên điện thoại, không qua Android Studio. Môi trường build không tải được
Android Gradle Plugin, thư viện AndroidX hay Compose (maven.google.com bị chặn), nên bản 2B:
- build bằng `tools/apk/build_apk.sh`: kotlinc → dx (dalvik-exchange) → aapt → zipalign → apksigner. Công cụ lấy
  từ kho Ubuntu, android.jar API 26/35 lấy từ Sable/android-platforms;
- giao diện là HTML/CSS/JS trong `app/assets/web`, chạy trong một WebView. Engine Kotlin được gọi qua
  `@JavascriptInterface` (`AppApi`, đầu vào và đầu ra đều là JSON);
- minSdk 26 (cần java.time), targetSdk 35, vẽ tràn viền có đệm theo insets, không xin quyền nào (kể cả Internet).

Lớp `AppApi` không phụ thuộc Android, nên cùng một mã được chạy trên máy tính (`DevServer`) để kiểm thử giao diện
bằng Playwright, và được kiểm cả trên đúng bytecode bản Android. Phần UI Compose đầy đủ (giai đoạn 5) vẫn theo
mục 3 khi có môi trường Gradle/Android Studio; khi đó `AppApi` và `StarStyle` dùng lại được.

### D-29 · Màu ngũ hành và cột cát/hung của sao chỉ để hiển thị
`StarStyle` quy định vị trí mỗi sao trong ô cung (chính tinh, Tứ Hóa, cột cát, cột hung, vòng Tràng Sinh ở chân ô)
và ngũ hành để tô màu (mục 13). Ngũ hành 14 chính tinh lấy theo bảng cẩm nang V2 (Tham Lang “Thủy đới Mộc” → Thủy).
Phụ tinh theo cách ghi phổ biến của sách Nam phái. Vòng Bác Sĩ và vòng Thái Tuế chia cát/hung theo tính chất
từng sao. Bảng này không tham gia an sao hay luận giải; giai đoạn 3 có thể tinh chỉnh.

### D-30 · Vá 5 hàm dùng invokedynamic của kotlin-stdlib cho Android
Năm hàm `compareBy(vararg)`, `then`, `thenDescending`, `nullsFirst(cmp)`, `nullsLast(cmp)` trong
`kotlin.comparisons` tạo Comparator bằng LambdaMetafactory, mà Android không có lớp này. Không có R8 để khử đường
(desugar), nên `StripIndy` dùng ASM có sẵn trong kotlin-compiler.jar để chuyển thân 5 hàm sang
`vn.tuvi.indyfix.Comparators`, và bỏ hai gói không dùng là `kotlin.uuid` và `kotlin.streams`. Build dừng nếu còn
invokedynamic hoặc nếu file DEX còn `invoke-custom`/`invoke-polymorphic`. Kết quả sắp xếp của bản vá đã được đối
chiếu và khớp với stdlib gốc. Mã app được biên dịch với `-Xlambdas=class -Xsam-conversions=class -Xstring-concat=inline`.

### D-31 · Khóa ký APK nằm trong dự án
`app/keystore/tuvi-tu-xem.jks` (mật khẩu `tuvituxem`) dùng để ký mọi bản cập nhật. Android chỉ cho cài đè khi
chữ ký trùng, nên phải giữ file này; mất khóa thì phải gỡ app cũ (mất dữ liệu đã lưu) rồi mới cài được bản mới.
Đây là app cá nhân cài ngoài cửa hàng. Nếu sau này đưa lên Google Play, cần tạo khóa riêng và không để trong mã nguồn.

### D-32 · Lỗi 0.2.0 thoát ngay khi mở: gọi `window.insetsController` trước `setContentView`
Trên Android 11+ (API 30+), `PhoneWindow.getInsetsController()` dùng DecorView. Trước `setContentView` chưa có
DecorView, nên lệnh này ném NullPointerException trong `onCreate`, và app thoát trên mọi máy Android 11 trở lên.
Kiểm thử trên trình duyệt không thấy lỗi này, vì nó nằm ở phần Activity chỉ chạy trên Android.
Bản 0.2.2 thiết lập tràn viền sau `setContentView`, và có bọc try/catch vì phần này chỉ ảnh hưởng màu thanh hệ thống.
Máy ảo đã chạy lại đúng file 0.2.0 và tái hiện đúng lỗi này (`OLD_APK_DEAD`) trước khi xác nhận bản sửa.

### D-33 · Không gửi APK khi chưa chạy trên máy ảo Android
Mỗi lần đẩy mã, workflow `android-emulator.yml` cài đúng file trong `dist/` lên máy ảo API 30/34/35/36
(và 37.0 nếu GitHub có ảnh máy ảo). Các bước kiểm tra:
- mở bản phát hành; app phải còn sống sau 20 giây, logcat không có FATAL, lỗi JavaScript hay cảnh báo của app;
- chạy bản tự kiểm tra: cùng mã, thêm `debuggable` và `selftest.js`. Bản này nhập 4 ca golden qua giao diện thật
  và gọi engine thật dạng DEX (430 mục);
- cài đè lên bản cũ để kiểm tra đường cập nhật.
App có thêm bộ ghi lỗi (`TuViApp`/`CrashLog`): nếu app dừng, lần mở sau hiện nguyên văn lỗi kèm nút Sao chép.
Lỗi trong cầu nối JS được trả về giao diện, không làm app thoát. `onRenderProcessGone` cũng được xử lý.
Bước build có thêm `check_api.py`: mọi lời gọi từ DEX sang Android phải có trong android.jar API 35.

### D-34 · Luật viết bằng Python, app chỉ đọc JSON
Knowledge base được soạn trong `tools/kb/kb_rules.py` (189 luật, 50 khuyến nghị) và `tools/kb/kb_drafts.py`
(câu luận chính tinh theo độ sáng × cung, 24 bộ đồng cung, 73 phụ tinh, câu hỏi mẫu). `tools/kb/build_kb.py` sinh
ra `data/kb/src/main/resources/kb/**.json` đúng schema mục 9. App và test chỉ đọc JSON; không có Python lúc chạy.
Viết bằng Python để dùng lại điều kiện chung, ví dụ nhóm sát tinh `SAT6`, `TINH_DUYEN_O` hay hàm `rule(...)`. Viết
tay 189 khối JSON dễ sai lệch giữa các luật cùng họ. Mọi mục đều có `reviewed: false` (bản nháp do script sinh,
mục 15) cho tới khi người dùng duyệt. Sửa luật → `python3 tools/kb/build_kb.py` → `./gradlew :core:interpret:findFixtures`
(tìm lại lá số mẫu) → `./gradlew test`.

### D-35 · Độ ưu tiên và “luật cụ thể hơn thắng luật chung”
Mục 9 định nghĩa độ ưu tiên là số predicate. Cách đếm cụ thể:
- mỗi điều kiện lá tính 1;
- `anyOf` tính bằng nhánh ngắn nhất;
- `countOf … atLeast n` tính n, vì “ít nhất 4 trong 4 sao” cụ thể như 4 điều kiện riêng;
- cung neo không tính.

Áp dụng ở hai chỗ:
1. **Chặn / đảo / làm nhẹ.** Khi nhiều luật cùng nhắm vào một finding, chỉ luật ưu tiên cao nhất có hiệu lực.
   Nếu bằng nhau, thứ tự là chặn > đảo > làm nhẹ. Ví dụ: Cự Môn Thìn Tuất tuổi Tân (đảo, 2 predicate) thắng
   “sao hãm gặp Lộc Quyền” (làm nhẹ). Nếu cộng dồn, câu Cự Môn hãm sẽ thành trung tính, sai với cách phản vi.
2. **Hai luật ngược chiều.** Điều kiện để luật chung bị chặn:
   - hai luật cùng cung neo, cùng chủ đề, cùng tầng;
   - bằng chứng của luật cụ thể chứa trọn bằng chứng của luật chung (cùng sao, có thêm yếu tố);
   - luật cụ thể có nhiều predicate hơn.

   Ví dụ: “Điền Trạch có tài tinh” (Thiên Phủ) bị “Kho lộ” (Thiên Phủ + Tuần) chặn. Ngược lại, “Kho lộ” không bị
   “Tử Phủ đồng cung” chặn, vì Tử Phủ không nói gì về Tuần/Triệt. Hai câu cùng hiện, giai đoạn 4 cân trọng số.
   Luật phản vi không tham gia bước này, vì chúng đã tác động qua chặn/đảo/làm nhẹ. Đã thử cách so khớp theo
   “có chung một sao”: cách này chặn nhầm hàng trăm câu, nên đã bỏ.

### D-36 · Lá số mẫu cho từng luật được tìm tự động
Cổng mục 15 yêu cầu mỗi luật có một lá số kích hoạt và một lá số không kích hoạt.
`core/interpret/src/test/kotlin/…/tools/FindFixtures.kt` tìm hai lá số này cho từng luật:
- dò 200 000 lá số ngẫu nhiên (hạt giống cố định);
- sau đó vét cạn năm 1984–2043 × tháng × ngày × 12 canh × 2 giới.

Lá số “không kích hoạt” ưu tiên loại *suýt khớp*: điều kiện lá đầu tiên đúng nhưng cả luật sai.
105/189 luật có lá số suýt khớp; 84 luật còn lại dùng lá số bất kỳ không kích hoạt.
Kết quả lưu ở `rule-fixtures.json` (âm lịch + canh + giới + năm xem), nên test không phụ thuộc engine sinh ra nó.
Vì lá số mẫu do chính engine tìm, có thêm `IndependentRulesTest`: 27 luật được viết tay lại bằng Kotlin thuần trên
mô hình lá số, không qua DSL, rồi so với RuleEngine trên 4 000 lá số ngẫu nhiên cộng mọi lá số mẫu.

### D-37 · Bản 0.3.0 hiện luận giải sơ bộ, ghi rõ là bản nháp
App 0.3.0 nạp knowledge base từ APK (`/kb/…`, nạp sẵn ở luồng nền) và gọi RuleEngine cùng lúc lập lá số.
Giao diện có ba phần:
- thẻ “Cách cục & dấu hiệu lá số”, nhóm theo loại;
- mục “Dấu hiệu năm …” trong thẻ vận hạn;
- câu luận từng cung trong bảng chi tiết cung.

Mỗi mục có “Vì sao?” (sao, cung, quan hệ làm căn cứ), gợi ý và nguồn. Thẻ luôn ghi “bản nháp tự sinh, chưa qua
thẩm định”. Luật độ tin cậy C chỉ hiện khi bật “Chế độ chuyên sâu” trong Cài đặt. Nếu nạp knowledge base lỗi, lá số
vẫn hiện bình thường, kèm một dòng báo lỗi. Bộ ghép câu, chấm điểm chủ đề và khuyến nghị tổng hợp là giai đoạn 4.

### D-38 · Chấm điểm: hiệu chỉnh mức nền, khung cung theo tầng, trọng số đại vận
Điểm vẫn tính theo công thức mục 9: `clamp(50 + 10 × Σ, 5, 95)`. Σ gồm hai phần:
- lực sao × quan hệ cung (tọa thủ 1,0; xung chiếu 0,8; tam hợp 0,6; giáp 0,4 chỉ với cặp có nghĩa; nhị hợp 0,3)
  × Tuần/Triệt theo tuổi;
- chiều × trọng số của các luật đã kích hoạt.

Có ba điểm phải tự quyết:
1. **Mức nền.** Trên lá số ngẫu nhiên, tổng thô trung bình không bằng 0: phụ tinh tốt nhiều hơn phụ tinh xấu, chính
   tinh phần lớn sáng. Vì vậy Σ = (tổng thô − mức nền) × đơn vị. Mức nền và đơn vị đo trên 3 000 lá số ngẫu nhiên
   (36 000 cung), sao cho 50 điểm = mức trung bình và độ lệch chuẩn khoảng 13 điểm. Đo lại: điểm cung trung bình 50,2,
   độ lệch chuẩn 12,6. Lực chính tinh lấy theo bảng mục 6 (M +2, V +1,5, Đ +1, B 0, H −1,5). Phản vi và làm nhẹ từ
   luật chỉnh lực sao tương ứng.
2. **Khung cung theo tầng.** Mỗi tầng xoay 12 vai trò từ cung của tầng đó: đại hạn = “ĐH Mệnh”, tiểu hạn = “Mệnh của
   năm”, lưu niên đại hạn, Lưu Thái Tuế = Lưu Mệnh (mục 8.1, 8.4). Chủ đề Tài chính của năm vì vậy đọc ở cung Tài Bạch
   của từng tầng. Sao lưu và Tứ Hóa lưu chỉ cộng vào tầng Lưu Thái Tuế (đúng nhóm 0,15 của mục 9 bước 6). Tứ Hóa đại
   hạn (nếu bật) cộng vào tầng đại hạn. Luật năm góp vào tầng của điểm neo; luật neo theo cung gốc có sao lưu góp
   vào tầng Lưu Thái Tuế. Trước đại hạn đầu không có đại hạn và lưu niên đại hạn, nên trọng số còn lại được chia lại
   cho đủ 1.
3. **Đại vận và điểm tổng.** Mục 9 không cho trọng số đại vận, nên dùng gốc 0,25 + đại hạn 0,75. Điểm tổng mỗi tầng
   = 0,4 × điểm cung Mệnh của tầng + 0,6 × trung bình có trọng số các chủ đề (Sự nghiệp, Tài chính 1; Tình duyên,
   Sức khỏe 0,8; Gia đạo, Pháp lý 0,6; Di chuyển 0,4). Tổng quan trọn đời = Mệnh 0,45 + Thân 0,2 + Phúc Đức 0,35.
   Tháng và ngày theo đúng trọng số mục 9 bước 6. Can chi ngày: lục xung 25, tam hợp 75, lục hợp 70, còn lại 50.

Chủ đề Pháp lý không có cung riêng, nên đọc theo sao chỉ báo (Quan Phù, Thiên Hình, Hóa Kỵ, Cự Môn hãm, Kình
Dương; Giải Thần, Thiên Giải, Địa Giải, Hóa Khoa làm nhẹ) ở Quan Lộc, Thiên Di (Nô Bộc ×0,5).

### D-39 · Lá số trẻ nhỏ (dưới 16 tuổi âm)
Luận năm và đại vận cho trẻ dưới 16 tuổi âm chỉ giữ bốn chủ đề: Học tập, phát triển (thay tên Sự nghiệp), Sức khỏe,
Gia đạo, Di chuyển. Bỏ Tình duyên, Tài chính, Pháp lý. Bỏ các sự kiện cưới hỏi, con cái, thăng tiến, đổi việc, tài lộc.
Chỉ dùng 7 khuyến nghị dành cho trẻ (giấc ngủ, vận động, trò chuyện, không ép học, an toàn đi lại…). Không bật câu
áp lực tinh thần. Bản luận ghi rõ “Lá số trẻ nhỏ”. Phần trọn đời giữ nguyên, vì đó là mô tả tính chất lá số.
Ca golden C (sinh 2023) là trường hợp kiểm thử.

### D-40 · Ghép câu và chống câu trái ngược
Mỗi khối (một chủ đề, một cung, phần tổng quan) có:
- câu mở: điểm và mức “rất thuận / khá thuận / cân bằng / cần thận trọng / nhiều thử thách”, kèm tầng đỡ nhất hoặc
  tầng kéo xuống nhất, ghi rõ “cung Tài Bạch của tiểu hạn tại Tý, trên cung gốc Phu Thê”;
- các câu luận một chiều;
- câu sự kiện;
- **nhiều nhất một câu cân nhắc**.

Chiều trội lấy theo điểm (≥ 55 thuận, ≤ 45 cần lưu ý). Ở mức cân bằng, chiều trội lấy theo tổng lực các yếu tố.
Riêng khối cung lấy theo sao chính tọa thủ, vì đó là phần mô tả cốt lõi của cung. Yếu tố ngược chiều trội không in
thành câu riêng. Tên của chúng đi vào câu cân nhắc “Có cả … lẫn …, nghiêng về … vì …”, và câu gốc vẫn mở được trong
“Vì sao?”. Test kiểm bất biến này trên 4 ca golden và 300 lá số ngẫu nhiên (24 000 đoạn).

Mục “lưu cung” lấy đúng câu sao chính theo vai trò xoay trong KB. Ví dụ “Theo tiểu hạn, cung Tài Bạch của năm rơi
vào Tý…” dùng câu Tham Lang hãm tại cung Tài Bạch. Nhờ vậy năm và đại vận có nội dung cả khi chưa có luật riêng.
Chỉ khi không có gì mới in “Chưa có luận giải riêng cho tổ hợp này” (nguyên tắc 3).

**Đồng thanh tương ứng.** Luật mang `effect.event` (cưới hỏi, con cái, thăng tiến, đổi việc, tài lộc, hao tài,
giấy tờ/thị phi, đi xa, sức khỏe). Số tầng tính từ điều kiện của luật: tầng neo, cộng tầng mà điều kiện bắt buộc xét
(`allOf` lấy hợp, `anyOf` lấy giao). Không tính lá số gốc, vì gốc luôn đúng mọi năm. Ví dụ “Hạn hỷ” = tiểu hạn +
Lưu Thái Tuế + lưu niên đại hạn = 3 tầng → “khả năng cao”. Test đếm lại số tầng độc lập cho mọi câu sự kiện.

**Khuyến nghị.** Bước 1 lấy từ adviceKeys của các finding mạnh nhất (xếp theo |chiều × trọng số|), mỗi loại một mục
cho một chủ đề, tối đa 5. Nếu còn thiếu thì thêm theo thứ tự cho đủ 3 Nên và 3 Không nên:
1. khuyến nghị dự phòng của chủ đề mạnh/yếu;
2. khuyến nghị theo thế Tấn/Giữ/Thủ/Lùi;
3. khuyến nghị của chủ đề gần mức trung bình.

Khuyến nghị gắn thế (“thủ hơn công”, “tận dụng thời cơ”) chỉ dùng khi đúng thế. Mỗi khuyến nghị ghi “vì: …”. Hai
tháng điểm cao nhất là tháng thuận cho việc NÊN, hai tháng thấp nhất là tháng nên thận trọng. Thêm 24 luật năm (có
6 tình huống của bảng mục 11) và 41 khuyến nghị: Không nên/Lưu ý cho mọi chủ đề, theo thế giai đoạn, cho trẻ nhỏ.
Nâng KB lên 0.4.0 (213 luật, 91 khuyến nghị).

### D-41 · App 0.4.0: màn Luận giải
Màn Luận giải có các tab Trọn đời / Đại vận / Năm / Tháng, gọi `AppApi.reading` (Composer chạy trong DEX).
- **Mọi tab:** thẻ tóm tắt (điểm 0–100, thế Tấn/Giữ/Thủ/Lùi), các chủ đề, mỗi đoạn có “Vì sao?” (căn cứ, bằng chứng,
  nguồn, mã luật), nhãn “nháp” cho câu chưa duyệt.
- **Tab Năm:** thêm heat-map 12 tháng (chạm vào để xem tháng) và Nên/Không nên/Lưu ý.
- **Tab Trọn đời:** thêm 12 cung (mở từng cung) và dòng thời gian 12 đại vận (chạm vào để xem đại vận).

Bản luận chỉ tính khi mở tab và được lưu tạm theo (lá số, năm, tháng, trường phái, chế độ chuyên sâu), nên kéo thanh
năm ở màn lá số không chậm đi. Danh mục “Cách cục & dấu hiệu” của giai đoạn 3 vẫn giữ ở màn lá số.

### D-42 · Tầng Ngày: lịch tháng có điểm, chi tiết ngày, 12 giờ
Mục 2.3 tầng 6 và mục 9 bước 6, theo công thức đã chốt ở D-38:
- **Điểm ngày** = điểm tháng ×0,50 + cung nhật hạn ×0,35 + can chi ngày so với tuổi ×0,15. Can chi ngày: lục xung 25,
  tam hợp 75, lục hợp 70, còn lại 50. Nhật hạn: từ cung nguyệt hạn coi là mùng 1, đếm thuận tới ngày âm (mục 8.5).
- **Lịch tháng** đi theo tháng dương lịch (người dùng xếp việc theo lịch dương). Mỗi ô ghi ngày dương, ngày âm (mùng 1
  ghi kèm tháng âm, “n” là tháng nhuận) và điểm. Màu theo đúng ngưỡng toàn app: từ 55 thuận, từ 45 trở xuống nên thận
  trọng. Một tháng dương có thể vắt qua hai tháng âm, nên mỗi ngày tính lại từ đúng tháng âm của nó. Điểm năm và điểm
  tháng được lưu tạm trong một lần chấm, nên 30 ngày chỉ tốn khoảng 1–2 lần luận năm (≈ 5 ms trên JVM, 12 ms trên máy ảo).
- **Giờ trong ngày**: giờ Tý ở cung nhật hạn, đếm thuận. Điểm giờ = điểm ngày ×0,5 + điểm cung thời hạn ×0,5. Ba giờ
  cao nhất là giờ thuận, hai giờ thấp nhất là giờ nên thận trọng.
- **Nhãn độ tin cậy**: thẻ lịch luôn ghi “Độ tin cậy thấp hơn tầng năm” (mục 2.3). Bản luận ngày kết bằng `note.ngay`,
  “Vì sao?” của giờ thuận kèm `note.gio` (độ tin cậy thấp hơn tầng ngày).
- Trên 50 tháng ngẫu nhiên: trung bình 50, độ lệch 6,7; 30% số ngày thuận, 23% nên thận trọng. Trong một tháng, điểm
  dao động khoảng 15.

### D-43 · Thẻ sao
Chạm giữ một sao trên lưới 0,55 giây (rê ngón quá 12 px thì huỷ) hoặc chạm tên sao trong bảng cung để mở thẻ sao:
- **Thông tin chung**: chòm (Bắc/Nam Đẩu/Trung Thiên), ngũ hành âm dương, hóa khí, đặc tính. Lấy từ bảng thuộc tính
  14 chính tinh của cẩm nang P.III, ghi trong `meta/sao.json` mục `chinh`. KbLint bắt đủ 14 sao.
- **Tại cung này**: độ sáng, câu ý nghĩa đúng sao – độ sáng – cung, Tứ Hóa gốc kèm can năm, các cách cục gốc có sao
  này làm bằng chứng.
- **Phụ tinh** không có dòng chòm/hóa khí và dùng câu phụ tinh theo cung. Chưa có câu thì in `note.chua_co_luan`.
- Chạm nhanh vẫn mở bảng cung như cũ. Trên lưới, chạm giữ không kích hoạt chọn chữ hay menu ngữ cảnh của WebView.

### D-44 · So hai lá số (cẩm nang P.V.4)
- **Truyền cung**:
  - vợ chồng: cung Phu Thê của người này trùng cung Mệnh của người kia, xét cả hai chiều;
  - cha mẹ – con: cung Tử Tức của cha/mẹ trùng cung Mệnh của con.
- **Truyền tinh**: bộ chính tinh của cung đó giống hệt bộ chính tinh cung Mệnh người kia.
  - Đều sáng → thuận.
  - Là bộ sao nặng → truyền tinh hung. Gồm: Vũ Phá tại Tỵ/Hợi, Liêm Tham có sao hãm, hoặc Không/Kiếp ở cả hai cung.
  - Còn lại → trung tính.
  - Chung một phần → “truyền tinh một phần”.
- Cùng chủ đề không để hai câu trái chiều. Có cả dấu hiệu thuận lẫn nặng thì phần thuận gộp vào một câu cân nhắc, như
  bản luận. Không có dấu hiệu nào thì in `cmp.khong`. Luôn kết bằng `cmp.note`: quan hệ là tương tác hai chiều, không
  đổ lỗi cho ai (mục 11).
- Màn so sánh lấy hai lá số trong hồ sơ, bỏ qua hồ sơ không rõ giờ.

### D-45 · Không rõ giờ sinh: xếp hạng 12 giờ theo sự kiện đã qua
Bảy câu hỏi (mục 2.3: 5–7 câu): năm kết hôn, sinh con đầu, thăng tiến, đổi việc, khoản thu lớn, hao tài lớn, chuyển nhà
hay đi xa. Cần ít nhất hai câu trả lời, năm phải từ năm sinh tới năm nay.

Với mỗi giờ, mỗi sự kiện được chấm theo mức đồng thanh tương ứng (D-40) ở đúng năm đó: khả năng cao 3, có thể 2, mới
là dấu hiệu 1, không thấy 0. Mức khớp = tổng / (3 × số sự kiện), thang 100. Không ghi phần trăm để khỏi giống lời khẳng
định chắc chắn. Kết quả luôn gắn nhãn “ước đoán”, kèm chi tiết từng sự kiện, và bảng 12 lá số để tự đọc vẫn ở ngay dưới.

Test: lấy 3 sự kiện mạnh nhất của chính lá số giờ thật (ca A, giờ Tỵ) thì Tỵ phải đứng đầu với đúng số điểm tính lại
độc lập. Ca A đạt Tỵ 78–89, các giờ khác từ 44 trở xuống.

### D-46 · Trường phái tùy chỉnh từng điểm
Ngoài 3 preset, Cài đặt có “Tùy chỉnh từng điểm” cho 12 điểm của `SchoolConfig`: Tứ Hóa năm Canh, năm Nhâm hóa Khoa,
Khôi Việt, Hỏa Linh, Thiên Giải, Lưu Hà, tháng nhuận, giờ Tý muộn, nguyệt hạn, độ sáng Cự Môn, Tứ Hóa theo can đại hạn,
Thân chủ tuổi Tý.
- Danh sách lấy từ `AppApi.configOptions`, nên engine thêm biến thể thì giao diện tự có.
- Cấu hình gửi theo tên enum. Tên lạ thì dùng giá trị mặc định, không lỗi.
- Lá số và PDF ghi “Tùy chỉnh”. Bộ nhớ tạm của bản luận có tính cả cấu hình.

### D-47 · Xuất PNG, PDF A4
- **PNG**: vẽ bằng canvas ở phía giao diện, 1256×1504, luôn nền sáng để in, không phụ thuộc màn hình. Gồm lưới 12 cung
  (chính tinh có độ sáng, Tứ Hóa, phụ tinh cát/hung, vòng Tràng Sinh, tuổi đại hạn, dấu ĐH/TH/LN của năm xem), ô trung
  tâm, Tuần/Triệt trên ranh giới, chân ảnh “tham khảo, chiêm nghiệm — đức năng thắng số”.
- **PDF**: dựng bằng `android.graphics.pdf.PdfDocument` ngay trên máy, khổ A4 (595×842 pt). Không dùng hộp thoại in của
  WebView, vì chân trang lặp lại mỗi trang trong CSS in không ổn định giữa các bản WebView.
  - Trang 1: lá số và 11 dòng thông tin.
  - Từ trang 2: luận trọn đời (tóm tắt, chủ đề, 12 đại vận, 12 cung, Nên/Không nên); sang trang mới là luận năm đang
    xem (thêm 12 tháng).
  - Chữ chảy qua trang theo dòng; tiêu đề không đứng trơ cuối trang.
  - Mỗi trang có chân trang “Tử Vi Tự Xem · Trường phái … · lập ngày … · Trang n” và lời ngỏ (mục 11).
- **Lưu và gửi**:
  - Android 10+: lưu qua MediaStore vào Tải xuống/TuViTuXem và Ảnh/TuViTuXem, không cần quyền bộ nhớ.
  - Android 8–9: lưu vào thư mục riêng của app.
  - “Gửi” dùng `ShareProvider` (exported = false, chỉ đọc, cấp quyền theo từng lần gửi) và bảng chia sẻ của Android.
- Máy chủ thử nghiệm trên máy tính dùng hộp thoại in của trình duyệt thay cho PDF.

### D-48 · Màn hình lớn, phóng to, trợ năng
- **Hai cột**: từ 900 px CSS trở lên (máy tính bảng, điện thoại xoay ngang), lưới 12 cung bên trái (dính khi cuộn), các
  thẻ bên phải. Bảng cung, thẻ sao trượt ra thành cột phải rộng 460 px. Mục chủ đề của bản luận chia hai cột.
- **Phóng to lưới**: chụm hai ngón (1–3×, giữ điểm giữa hai ngón đứng yên) hoặc nút −/＋/⟲. Dùng `transform: scale` trên
  lưới bên trong một khung cuộn, nên toạ độ Tuần/Triệt và đường tam hợp không phải tính lại. Chạm ô vẫn đúng cung.
- **TalkBack**: mỗi ô là một nút có nhãn gộp đọc theo thứ tự tên cung, can chi, chính tinh (độ sáng), Tứ Hóa, phụ tinh,
  tuổi đại hạn (mục 13). Nội dung chi tiết của ô ẩn với trình đọc màn hình. Mọi phần tử chạm được có role="button",
  tabindex và nhận Enter/phím cách. Bảng trượt khi đóng có `visibility: hidden` nên trình đọc không đọc nội dung cũ.
- **Cổng kiểm tra**:
  - axe-core (WCAG 2.1 A/AA) trên 9 màn: không lỗi mức serious/critical. Đã sửa độ tương phản điểm đại hạn, ô lịch,
    nhãn độ tin cậy; sửa nút lồng nút ở danh sách hồ sơ.
  - Máy ảo: `uiautomator dump` phải thấy nhãn ô Mệnh của ca A.
- **Cổng ảnh chụp**: 10 ảnh chuẩn trong `tools/apk/uitest/baseline`, so từng điểm ảnh, lệch tối đa 0,5%.
- **Hiệu năng ngay trên máy**: `AppApi.bench`, selftest ghi dòng BENCH. Ngưỡng máy ảo: lá số < 25 ms, năm < 250 ms,
  30 ngày < 1 500 ms. Chỉ tiêu S22 ở mục 3 là 5 / 50 / 300 ms.

## Giai đoạn 6 — Hỏi đáp

### D-49 · Hỏi đáp chạy bằng luật trên máy; model AI để sau
Mục 12.1 chọn Gemma 4 E2B chạy qua LiteRT-LM. Bản này chưa đóng được phần đó:
- Thư viện `com.google.ai.edge.litertlm:litertlm-android` chỉ có trên Google Maven (`dl.google.com`). Môi trường build
  của bản này chặn tên miền đó và cả `repo1.maven.org`, nên không lấy được AAR để đóng vào APK.
- Model khoảng 2,6 GB (tải từ huggingface.co), phải chấm bộ 40 câu trên chính S22 Ultra (mục 12.1) trước khi chốt.

Vì vậy giai đoạn 6 làm trước con đường dự phòng mà mục 12.2 đã định: phân loại câu hỏi bằng bộ từ khóa, trả lời bằng
chính các đoạn luận của engine (bước 5: “trượt → hiển thị đoạn rule-based”). Lớp này giữ lại khi gắn model:
- model chỉ thay bước diễn đạt;
- câu nào model viết sai kiểm tra 12.3 thì quay về đúng câu trả lời này.

Ưu điểm hiện tại:
- APK vẫn khoảng 1,5 MB, không xin quyền Internet;
- trả lời trong khoảng 0,1–0,2 giây;
- câu nào cũng có mã luật và “Vì sao?”.

### D-50 · Hiểu câu hỏi (`QaEngine.parse`)
- **Chuẩn hóa.** Chuyển chữ thường, bỏ dấu, đ → d. Câu có dấu và không dấu hiểu như nhau. Có 4 chỗ dấu quyết định nghĩa
  và được giữ riêng:
  - Tý / Tỵ;
  - “tháng sáu”;
  - “còn tôi”, “chỉ tôi”, “bỏ tôi” (không phải “con tôi”, “chị tôi”, “bố tôi”);
  - “cưới” (không phải “cuối”).
- **Thứ tự xét.** Câu trống → khủng hoảng → từ chối → ngoài phạm vi → dữ kiện lá số → giải nghĩa → hợp tuổi → khi nào
  (dò năm/tháng/ngày) → chọn ngày → nên/không nên → luận theo chủ đề và thời gian → chào hỏi.
- **Chủ đề và sự kiện.** Theo bảng mục 9: 10 chủ đề và 9 sự kiện đồng thanh. Chọn cái xuất hiện trước trong câu.
- **Thời gian (âm lịch là chính).**
  - Năm: số, năm nay/sau/ngoái, N năm nữa, can chi, tuổi âm.
  - Tháng: tháng N, tháng này/sau/trước, giêng, chạp, dương lịch.
  - Ngày: dd/mm, mùng N, rằm, Tết, hôm nay/mai/kia, thứ N (tuần sau).
  - Tuần này, tuần sau, cuối tuần.
  - Đại vận: này/tới/trước, “10 năm tới”; tiền/trung/hậu vận.
  - Năm sinh, ngày sinh nhắc trong câu không bị hiểu là thời gian hỏi.
  - Không nêu năm mà tháng hoặc ngày đã qua thì hiểu là lần tới. Ví dụ hỏi “tháng 7” vào tháng 10 âm là tháng 7 năm sau.
- **Chủ đề không có điểm theo năm.**
  - Tính cách, phúc đức chỉ đọc ở lá số gốc.
  - Nhà đất xem kèm điểm tài chính cùng giai đoạn (Điền Trạch là cung phụ của tài chính) và luôn ghi rõ là gián tiếp
    (`qa.note.nha_dat`).
- **Hỏi theo hướng cần thận trọng** (“năm nào nên chú ý sức khỏe”): xếp từ điểm thấp lên.
- **Hỏi về người thân** (“con tôi”, “mẹ tôi”): lá số đang mở là của người hỏi, nên chỉ đọc qua cung tương ứng và nói
  rõ điều đó (`qa.chuyen.nguoi_than`). Muốn xem lá số riêng của người khác thì app từ chối và hướng sang thêm hồ sơ.

### D-51 · Trả lời (`QaEngine.answer`)
Không tự viết câu luận mới. Mỗi câu trả lời chỉ lấy:
- đoạn của `Composer` (trọn đời, đại vận, năm, tháng, ngày), giữ nguyên ruleIds và “Vì sao?”;
- điểm, thế, khuyến nghị của đúng bản luận đó;
- lời của app trong `texts/hoidap.json` (khóa `qa.*`): giải nghĩa 35 thuật ngữ, từ chối, chuyển hướng, ghi chú.

Các dạng câu trả lời:
- **Luận:** dòng “App hiểu là …”, điểm, thế, các đoạn của chủ đề (tối đa 12 đoạn), Nên/Không nên, nút “Xem luận …”
  mở bản luận đầy đủ, 2–3 câu hỏi tiếp.
- **Khi nào thuận:** dò 11 năm.
  - Có sự kiện: xếp theo mức đồng thanh (khả năng cao / có thể / mới là dấu hiệu), sau đó theo điểm chủ đề. Nhãn tầng
    rút gọn.
  - Không có sự kiện, hoặc không năm nào có đồng thanh: xếp theo điểm chủ đề.
  - Luôn kèm `qa.note.kha_nang`: đây là khả năng, không phải lời khẳng định.
  - Dò tháng: điểm 12 tháng và tháng nguyệt hạn vào cung của chủ đề. Dò ngày: điểm ngày trong tháng hoặc tuần.
- **Dữ kiện:** câu tính từ lá số, ví dụ cung Mệnh có sao gì, Thân cư đâu, mệnh/cục, mệnh chủ, tuổi âm, đại vận, tiểu
  hạn, ngày âm hôm nay.
- **Giải nghĩa:** định nghĩa, kèm vị trí trên lá số của người hỏi nếu có. Hỏi một sao thì trả về thẻ sao (D-43).
- **Lá số dưới 16 tuổi âm** (D-39): chỉ trả lời học tập, sức khỏe, gia đình, tính cách, đi lại. Không dò năm cho cưới
  hỏi, tiền bạc, pháp lý.
- **Ngoài phạm vi / từ chối:**
  - không dùng cho: tuổi thọ, thời điểm mất, giới tính thai nhi, xổ số và cá cược, dịch vụ giải hạn có thu phí, lá số
    của người khác, câu ngoài Tử Vi;
  - chuyển hướng, vẫn trả lời phần luận phù hợp: ly hôn, ngoại tình, bệnh, tù tội, tai nạn, chọn ngày theo Ngọc Hạp,
    hợp tuổi (hướng sang So hai lá số).

### D-52 · Khủng hoảng tinh thần
Câu có dấu hiệu ý định tự hại được xét trước mọi thứ khác, kể cả khi câu có chủ đề hay năm. App không chạy phần luận,
không chấm điểm, không có nút đánh giá, không lưu câu hỏi. App chỉ hiện lời động viên với:
- cấp cứu 115;
- đường dây nóng Ngày Mai 096 306 1414, hỗ trợ tâm lý miễn phí, thường 13:00–20:30 từ thứ Tư đến Chủ Nhật;
- Tổng đài 111 cho trẻ em và người dưới 16 tuổi, miễn phí, 24/7.

Số đã đối chiếu với nguồn chính thức tháng 11/2026. Lời này nằm trong KB (`qa.khung_hoang`), sửa không cần build lại mã.

### D-53 · Lịch sử câu hỏi trên máy; đồng bộ Firebase chưa bật
- Mỗi câu hỏi đi qua `QaPrivacy.anonymize` trước khi lưu. Bộ lọc bỏ email, số điện thoại, ngày tháng, giờ, phần sau
  “sinh …”, họ tên sau “tôi tên / tên tôi là …”, số nhà. Câu còn dãy số dài hoặc @ thì `isSafe = false`.
- Lịch sử nằm trong SharedPreferences (khóa `qaLog`, tối đa 200 câu). Mỗi bản ghi gồm câu đã ẩn danh, dạng chuẩn hóa để
  so trùng, ý định, chủ đề, phạm vi, số lần hỏi, 👍/👎, thời điểm.
- Lịch sử dùng cho chip “Hay hỏi” (câu hỏi từ hai lần, không có phần bị ẩn). Cài đặt có nút xóa toàn bộ.
- Đồng bộ Firestore (mục 12.6) cần dự án Firebase của chủ app (`google-services.json`, Security Rules, App Check). Bản
  này chưa có nên chưa gửi gì đi, app vẫn không xin quyền Internet. Dữ liệu đã sẵn đúng các trường mục 12.6, nên khi có
  dự án chỉ cần thêm lớp gửi và công tắc đồng ý.

### D-54 · Bản 0.6.1: câu trả lời Hỏi đáp là phần luận đoán dựng từ lá số
Người dùng nhận xét bản 0.6.0 trả lời như “cài sẵn”. Lý do là bản đó chủ yếu chọn lại các đoạn của bản luận năm.

Bản 0.6.1 thêm `QaAnalyst`. Với mỗi câu hỏi, app đi qua lá số như một người xem tử vi, theo đúng chủ đề và đúng người
được hỏi.

**Cung của câu hỏi:**
- Tài chính → Tài Bạch, Sự nghiệp → Quan Lộc, Tình duyên → Phu Thê…
- Hỏi về con thì đọc Tử Tức, hỏi về cha mẹ thì đọc Phụ Mẫu.

**Câu hỏi theo năm đi qua các tầng:**

| # | Tầng | Nội dung |
| --- | --- | --- |
| ① | Lá số gốc | Cung an ở đâu; chính tinh và độ sáng; nghĩa của sao tại đúng cung đó theo độ sáng (KB); cát tinh, sát tinh, Tứ Hóa gốc (ghi sao được hóa); Tuần/Triệt; cung Thân; vô chính diệu (mượn sao xung chiếu); xung chiếu, tam hợp; cách cục gốc liên quan |
| ② | Đại hạn | Cung đại hạn, các năm, thiên thời; cung của chủ đề khi xoay 12 vai trò từ đại hạn và sao tại đó; Tứ Hóa đại hạn nếu trường phái bật |
| ③ | Lưu niên đại hạn | Năm thứ mấy của đại hạn, cung của chủ đề trong khung lưu niên — cho biết kết quả cuối năm |
| ④ | Tiểu hạn | Cung tiểu hạn, cung của chủ đề trong khung tiểu hạn — cho biết diễn biến trong năm |
| ⑤ | Lưu Thái Tuế | Tứ Hóa theo can năm (sao nào hóa gì, rơi cung nào); sao lưu theo chủ đề (Lộc Tồn, Kình, Đà, Mã, Hồng Loan, Thiên Hỷ, Đào Hoa, Tang Môn, Bạch Hổ…) tọa thủ, xung chiếu hay tam hợp cung của chủ đề; Tứ Hóa lưu nhập thẳng cung gốc |
| ⑥ | Đồng thanh tương ứng | Mức khả năng theo số tầng; các cách vận hạn kích hoạt kèm tầng và cung |
| ⑦ | Tổng hợp | Điểm từng tầng × trọng số mục 9 = điểm chủ đề; nhận xét các tầng đồng thuận hay không |

**Kết luận đặt lên đầu:**
- điểm và mức;
- điểm tựa và áp lực, nêu tầng nào, cung nào, sao gì; các tầng cùng rơi một cung thì gộp lại;
- xu hướng đầu năm và cuối năm;
- câu hỏi có sự kiện (“có cưới được không”) thì trả lời thẳng mức khả năng, kèm điểm chung.

**Các phạm vi khác:**
- Tháng: nguyệt hạn (cung, sao), Tứ Hóa tháng chạm cung chủ đề, nền của năm.
- Đại vận: gốc, đại hạn, năm thuận nhất và năm cần giữ trong 10 năm.
- Trọn đời: gốc, Mệnh và Thân, điểm chủ đề của từng đại vận, giai đoạn thuận nhất và cần giữ. Câu “có số giàu không”,
  “số tôi có làm quan không” giờ hiểu là trọn đời.
- Dò “năm nào…”: mỗi năm kèm **căn cứ** (cách vận hạn, Tứ Hóa lưu, sao lưu chạm cung chủ đề) và phần **cần lưu ý**.
  Câu hỏi theo hướng thận trọng thì đảo lại: nêu yếu tố xấu là căn cứ, yếu tố tốt là mặt đỡ.
- Nhà đất có điểm riêng theo cung Điền Trạch trên mọi tầng, không xem kèm tài chính nữa (bỏ D-50 phần nhà đất).

**Điểm số.** Mọi điểm lấy từ `Scorer` qua `Composer.yearData` / `decadeData` / `lifeData`, nên trùng với điểm chủ đề
trong bản luận đầy đủ. Test kiểm điều này.

**Mỗi đoạn có tiêu đề** (`Paragraph.title`) và “Vì sao?”: công thức, các yếu tố cộng trừ điểm lớn nhất của tầng, Tứ Hóa,
sao lưu.

**Test (`QaTest`):**
- các tầng khớp dữ kiện lá số: cung gốc, cung đại hạn, tiểu hạn, lưu niên, Thái Tuế, Tứ Hóa năm theo can;
- dòng tổng hợp cộng đúng;
- hai lá số khác nhau, hoặc hai năm khác nhau, phải cho lời khác nhau;
- chạy trên 4 lá số golden × 3 năm × 10 chủ đề, cộng tháng, đại vận, trọn đời, dò năm.

### D-55 · Model AI sinh lời (LLM) vẫn chưa gắn được
Đã kiểm tra lại mạng ngày 21/11/2026: `dl.google.com`, `maven.google.com`, `repo1.maven.org`, `huggingface.co` vẫn
bị chặn từ môi trường build. Vì vậy không lấy được thư viện LiteRT-LM, không lấy được model, không chạy thử được.

Không gắn một model chưa chạy thử vào app. Lớp luận đoán D-54 làm phần nội dung. Khi có mạng, model chỉ làm việc diễn
đạt lại kết quả của lớp này, và mọi câu model viết phải qua kiểm tra mục 12.3. Câu nào trượt kiểm tra thì app hiện lại
câu của D-54.

### D-56 · Bàn giao sang môi trường mới (claude.ai/code)
Môi trường cũ không mở được `dl.google.com` và `huggingface.co` (D-55), nên dự án chuyển sang một phiên Claude Code có
môi trường mạng Custom. Hướng dẫn cho anh Phong: `docs/BAN_GIAO_MOI_TRUONG_MOI.md`. Bối cảnh cho Claude: `CLAUDE.md`.
- `tools/setup/cloud_setup.sh` dựng lại công cụ build vào `/opt/tuvi-tk`, ghi `env.sh`. `build_apk.sh` và `devserver.sh`
  tự nạp file này, nên không cần `env.local.sh`.
- Khóa ký vẫn không lên GitHub. Ở môi trường mới, khóa đi qua biến `TUVI_KEYSTORE_B64` của môi trường cá nhân.
  `restore_keystore.sh` giải mã và so dấu vân tay SHA-256 với khóa thật.
- `build_apk.sh` không còn tự tạo khóa mới khi thiếu khóa: thiếu hoặc sai khóa thì dừng (mã 4). Chỉ CI hoặc
  `ALLOW_NEW_KEY=1` mới ký bằng khóa tạm. Lý do: APK ký bằng khóa khác không cài đè được lên app đang dùng.
- `tools/setup/test_all.sh` chạy một lần đủ Gradle, APK, giao diện và selftest. `doctor.sh` báo mạng, công cụ, khóa.
- Hook SessionStart (`.claude/settings.json`) chỉ chạy trên đám mây: khôi phục khóa, cài nền công cụ nếu môi trường
  chưa có setup script, cài axe-core.

### D-57 · Trợ lý AI sinh lời trên máy: Gemma 4 E2B qua LiteRT-LM (bản 0.7.0)
Mạng đã mở (`dl.google.com`, `maven.google.com`, `huggingface.co`), nên D-55 được gỡ. Đã kiểm tra trước khi gắn:
- **Model** `litert-community/gemma-4-E2B-it-litert-lm`, file `gemma-4-E2B-it.litertlm` (2.588.147.712 byte,
  SHA-256 `181938…139a63c`). Giấy phép **Apache 2.0, không gated**: app tải ẩn danh được, không cần tài khoản.
  Ghim theo commit `2e12bfd…` của kho trên Hugging Face, nên file không đổi dưới chân app.
- **Runtime** LiteRT-LM 0.19.1 (`com.google.ai.edge.litertlm:litertlm-android`, Google Maven, Apache 2.0). AAR gồm
  `classes.jar` (Kotlin 2.4, bytecode Java 21) và `liblitertlm_jni.so` cho arm64-v8a và x86_64. Phụ thuộc: gson
  (bắt buộc), kotlinx-coroutines và kotlin-reflect (chỉ dùng cho API Flow và công cụ, app không gọi → không đóng gói).
- Đã chạy thật trên máy build (CPU 4 nhân, `litertlm-jvm`): nạp model ~8 giây, mỗi câu ~20–30 giây, tiếng Việt tốt.

**Cách gắn (giữ hệ thống build D-28, không chuyển sang Gradle/AGP):**
- `tools/apk/fetch_ai_deps.sh` tải AAR và R8 8.13.19 (Google Maven) một lần, kiểm SHA-256. gson 2.14.0 (313 KB) nằm sẵn
  trong `tools/apk/vendor/` vì Maven Central có lúc trả 404/429 cho máy chủ CI.
- Lớp bọc `app/src/llm/java/vn/tuvi/app/llm/LiteRt.java` viết bằng Java: kotlinc 2.0 của dự án không phải đọc
  metadata Kotlin 2.4 của thư viện. Cùng file này chạy trong APK và trên máy tính (công cụ chấm 40 câu).
- Code của app vẫn qua `dx` (classes.dex). Thư viện AI (bytecode Java 21 có lambda, nối chuỗi `invokedynamic`,
  `switch` theo mẫu) đi qua **D8 của R8 8.13.19** thành `classes2.dex`. D8 trong build-tools 35 (8.6) còn sót một
  `invoke-custom` (`SwitchBootstraps.typeSwitch`); R8 8.13 chuyển hết. `build_apk.sh` kiểm 0 `invoke-custom` ở cả hai DEX.
- `lib/arm64-v8a` (điện thoại) và `lib/x86_64` (máy ảo CI) nén trong APK, `extractNativeLibs="true"`. APK tăng từ
  1,5 MB lên khoảng 22 MB (riêng x86_64 khoảng 11 MB, giữ để máy ảo CI cài và nạp được thư viện thật).
- Manifest: `uses-native-library` libOpenCL.so / libvndksupport.so với `required="false"` (GPU nếu máy có).

**Thay đổi sản phẩm (anh cần biết):**
- App xin quyền **INTERNET** và **ACCESS_NETWORK_STATE**. Chỉ dùng khi người dùng bấm “Tải Trợ lý AI”. WebView vẫn
  bị chặn mọi yêu cầu http/https (`LocalOnlyClient.shouldInterceptRequest`), nên lá số và câu hỏi không ra mạng.
- Tải bằng `DownloadManager` của hệ thống: **chỉ Wi-Fi** (`NETWORK_WIFI`, không mạng trả tiền, không chuyển vùng),
  có thông báo tiến độ, tắt app vẫn tải, đứt mạng tự tải tiếp. File vào `Android/data/vn.tuvi.tuxem/files/models`
  (bộ nhớ riêng của app, gỡ app là mất). Trước khi tải kiểm chỗ trống (≥ dung lượng + 300 MB); tải xong kiểm SHA-256
  rồi mới đổi tên thành file model. Có nút Hủy tải và Xóa model.
- Máy dưới 5,5 GB RAM thì không cho bật (S22 Ultra có 12 GB). Sao lưu tự động bỏ thư mục model (`backup_rules.xml`).
- Nạp model: thử **GPU** trước; lỗi thì **CPU**. Ghi cờ `trying` trước khi nạp GPU: nếu tiến trình chết giữa chừng
  (lỗi trong thư viện máy), lần sau bỏ qua GPU. Thiếu RAM (`onTrimMemory`) thì giải phóng model.

### D-58 · Model chỉ diễn đạt lại; kiểm tra đầu ra mục 12.3 (`AiGuard`)
- `AiPrompt.build` dựng lời nhắc từ **chính câu trả lời của QaAnalyst** (D-54): câu hỏi, “app hiểu là”, năm hiện
  tại, điểm chung và thế, các đoạn ① gốc → ⑦ tổng hợp, Nên/Không nên. Khối dữ kiện tối đa 6.500 ký tự (~2.000 token).
  System prompt ở `kb/ai/system_prompt_vi.md`, cấu hình model ở `kb/ai/model.json` (đổi model không sửa code).
- Mỗi câu hỏi một hội thoại mới, nhiệt độ 0,3, tối đa 700 token ra. Model trả JSON `{"answer": "…"}`.
  Không yêu cầu model trả `ruleIds`: “Vì sao?” vẫn lấy từ các đoạn của QaAnalyst (vẫn mở được dưới câu AI).
- `AiGuard.check` — trượt bất kỳ điều nào thì app hiện bản QaAnalyst, ghi nhật ký trên máy (`aiLog`, 30 mục):
  1. đọc được JSON đúng dạng;
  2. 60–380 chữ; chỉ tiếng Việt (không chữ Hán/Kirin/Thái, không câu tiếng Anh);
  3. không từ cấm (`KbLint.FORBIDDEN`);
  4. mọi tên sao, tên cung có trong dữ kiện;
  5. **sao gắn đúng cung**: cặp (sao, cung hoặc “tại + chi”) trong một câu phải cùng có trong một câu dữ kiện;
  6. mọi con số có trong dữ kiện hoặc câu hỏi; “N/100” phải là điểm có trong dữ kiện; **điểm đúng tầng** (điểm nói
     là của đại hạn phải bằng “→ Đại hạn: N/100”…); câu trả lời phải nêu điểm chung.
  Độ dài nới từ 60–150 chữ (mục 12.4) lên 60–380 vì anh muốn giải thích chi tiết từng tầng, sao nào ở cung nào.
- Không gọi model với câu khủng hoảng (mục 12.2 bước 6), câu từ chối, chào hỏi, chưa hiểu.
- Giao diện: câu của QaAnalyst hiện ngay; khung “✨ Trợ lý AI đang viết…” chạy nền (`TuViNative.callAsync`, một luồng
  riêng); đạt kiểm tra thì hiện lời AI, bản luận chi tiết thu gọn dưới nút; trượt thì ghi một dòng và mở bản luận.

### D-59 · Chấm bộ 40 câu trên Linux; máy ảo CI kiểm thư viện máy
- `tools/ai/eval40.sh <model>` chạy 40 câu (`tools/ai/eval40_questions.json`: 4 lá số, đủ chủ đề, năm đã qua và năm
  tới, câu có dấu/không dấu) qua đúng `AppApi.askAi` với model thật trên CPU. Báo cáo: `docs/tai-ve/ai-cham-40-cau.md`.
- Máy ảo CI (x86_64, 2 GB RAM) không chạy được model 2,6 GB và không đánh giá được tốc độ. Selftest trên máy ảo kiểm:
  `aiStatus` đúng dạng; nạp `liblitertlm_jni.so` thật và gọi `Engine.initialize` với một file giả phải ném lỗi gọn
  (không văng app); `askAi` khi chưa có model trả “chưa dùng AI”.
- Thời gian ra token đầu tiên và token/giây trên S22 Ultra (GPU/CPU) cần anh chạy trên máy thật; app hiện số giây
  của từng câu AI.

### D-60 · Máy chủ thử giao diện có model giả lập
`FakeLlmHost` (DevServer): tải giả ~3 giây, “sinh lời” bằng `AiFake` (ghép lại đúng các câu dữ kiện, luôn qua kiểm
tra); câu có “thử lỗi AI” trả đầu ra sai để thử đường rơi về bản QaAnalyst. `TUVI_AI=off` để thử như máy không có AI.
Giao diện thử: thẻ Trợ lý AI, tải, câu AI đạt/trượt, câu khủng hoảng không gọi AI, nhật ký, xóa model.

### D-61 · Bỏ nhãn “bản nháp” và chữ “Prompt” khỏi giao diện; thêm số liên hệ (bản 0.8.0)
Anh Phong yêu cầu. Giao diện, PDF xuất ra và “Vì sao?” không còn chữ “nháp”, “bản nháp”, “Prompt mục …”, “D-xx”.
- Nguồn hiển thị đi qua `Luan.src`: “Prompt mục N” → “Hệ quy tắc luận giải Tử Vi Tự Xem”; “Sinh nháp tự động từ cẩm nang
  V2 …” → “Cẩm nang Tử Vi Đẩu Số V2 …”; bỏ “(bản nháp, cần duyệt)” và mã quyết định. Dữ liệu KB giữ nguyên trường
  `reviewed` để còn biết mục nào anh đã duyệt (chỉ không hiện ra nữa). Đặc tả mục 9 yêu cầu nhãn nháp; theo lời anh, bỏ.
- Liên hệ **0378 373 373**: dòng cuối trang chủ, thẻ “Liên hệ” trong Cài đặt (bấm là mở ứng dụng Điện thoại qua `tel:`),
  ghi chú trong PDF.

### D-62 · Kiến thức từ hai sách vào app, không fine-tune model
Anh gửi *Tử Vi Đẩu Số Toàn Thư* (Hi Di Trần Đoàn, Vũ Tài Lục dịch, 1973) và *Tử Vi Nghiệm Lý Toàn Thư* (Thiên Lương),
và muốn “đào tạo thêm” Gemma 4 E2B, tham khảo cả kênh YouTube Tử Vi Học Quán.
- **Không fine-tune.** Máy build không có GPU (huấn luyện LoRA model 2 tỷ tham số trên 4 nhân CPU mất nhiều ngày); đổi
  sang `.litertlm` cần bộ công cụ chuyển đổi riêng; model mới 2,6 GB phải có chỗ phát hành (GitHub Releases bị chặn).
  Quan trọng hơn: model đã fine-tune vẫn có thể nói sai mà không ai kiểm được, trái nguyên tắc “model chỉ diễn đạt lại”
  (D-58). Đưa kiến thức vào KB thì mỗi câu đều có nguồn, có “Vì sao?”, và `AiGuard` kiểm được.
- **YouTube bị proxy chặn** (403 cho youtube.com): không xem được video, không vòng đường khác. Cẩm nang V2 hiện có đã
  hệ thống hóa từ bài giảng của kênh này.
- **Bản quyền:** hai sách còn bản quyền, repo công khai → **không chép nguyên văn**. Mình đọc phần Thập nhị cung luận
  (Toàn Thư) và các bài chuyên đề (Nghiệm Lý), diễn giải lại bằng lời của app, ghi nguồn sách + chương. Lời theo quy tắc
  nội dung của app: bỏ các câu phán nặng (chết, tù, “làm đĩ”, khắc vợ ba lần…), đổi thành mức khả năng và lời khuyên.
- Dữ liệu: `tools/kb/kb_sach.py` → `kb/sach/sach.json`: 354 câu “cung × sao × điều kiện” (độ sáng, sao đồng cung, sao hội
  tam phương, có/không sát tinh, chi của cung) cho 12 cung, và 18 ý Nghiệm Lý (Lộc Tồn theo can, Long Phượng Tả Hữu,
  Khôi Việt với Xương Khúc, Không Kiếp, Hóa Khoa, La Võng, Cô Quả…).
- `Books.forPalace` chọn câu khớp lá số (câu nhiều điều kiện hơn được ưu tiên; tối đa 2 câu mỗi sao, 4 câu mỗi cung;
  cùng sao vừa có câu thuận vừa có câu về sát tinh thì nối bằng “Tuy vậy,”). `Books.nghiemLy` thêm tối đa 2 ý.
- Hiện ở: đoạn **“Sách bàn về cung …”** trong câu trả lời Hỏi đáp (năm, đại vận, trọn đời, câu hỏi về một cung) ngay sau
  tầng lá số gốc, “Vì sao?” ghi sách, chương và căn cứ trên lá số; mục **Sách bàn** trong thẻ sao. Trợ lý AI nhận đoạn
  này trong dữ kiện (system prompt: “dẫn thêm ý sách”), khối dữ kiện nới lên 7.500 ký tự.
- Test `BooksTest`: không từ cấm, có nguồn; câu đúng sao tọa thủ, “sáng”/“hãm” đúng độ sáng; đoạn sách có trong lời nhắc AI.

### D-63 · Hệ thiết kế trên Figma và bộ icon tướng số
- File Figma “Tử Vi Tự Xem — Hệ thiết kế” (tài khoản của anh): hai bộ biến màu “Màu sáng”/“Màu tối” (gói Starter chỉ
  cho 1 chế độ mỗi bộ), 5 kiểu chữ (Noto Serif cho tiêu đề, Roboto cho nội dung — đều có sẵn trên Android, app không
  tải font), 22 icon component, component `nut/chinh`, `nut/phu`, `the/la-so`, hai màn mẫu (Trang chủ, Hỏi đáp có
  Trợ lý AI và đoạn Sách bàn).
- Bảng màu: đỏ sơn mài `#7A1726`, vàng kim nhũ `#A87A1E` (trong app dùng `#9A6B12` cho chữ và nền có chữ trắng để đạt tương phản AA), nền giấy `#F6F1E6`; nền tối giữ như cũ, đỏ `#C94F60`, kim
  `#E0B45A`.
- Icon tự vẽ (nét 1,7 px, lưới 24 px): bát quái (logo), lá số, lập lá số, luận giải (cuộn), hỏi đáp, trợ lý AI, Bắc Đẩu
  (sao), lịch, la bàn (năm), trăng (tháng), đại vận, so hai lá số, giờ sinh, cài đặt, chia sẻ, xuất, liên hệ, sách,
  riêng tư, quay lại, thêm, đóng. Cùng một nguồn SVG cho app (`app/assets/web/icons.js`) và Figma.
- App: icon SVG thay emoji trên nút và tiêu đề thẻ, logo bát quái ở trang chủ, đường kẻ kim nhũ, tiêu đề chữ có chân,
  đoạn “Sách bàn” nền vàng nhạt. Ảnh chuẩn giao diện cập nhật có chủ đích.

### D-64 · Hỏi đoán năm cưới, đối chiếu năm đã xảy ra (0.8.1)
- Anh hỏi: “Làm sao để app trả lời khi tôi hỏi có thể xem từ lá số và đoán được năm cưới của tôi không?”. Trước đây câu
  “Năm nào tôi cưới?” đã chạy (dò năm theo đồng thanh tương ứng, mục 9), nhưng câu đúng lời anh bị hiểu thành “tình duyên
  năm nay”, vì bộ hiểu câu hỏi chỉ nhận “năm nào / khi nào / bao giờ…”.
- **Hiểu câu hỏi:** thêm các cách hỏi mốc năm: “năm cưới”, “tuổi kết hôn”, “năm lấy vợ/chồng”, “năm lập gia đình”, “năm có
  con”, “đoán (được) năm …”, “biết năm …”, “báo trước năm …”. “cưới” đã được đổi thành “cưới hỏi” trước khi bỏ dấu, nên
  “năm cưới” không lẫn với “năm cuối”. Không dấu: “đã cưới”, “vừa cưới” cũng nhận là cưới hỏi.
- **Dò cả các năm đã qua:** câu hỏi mốc năm của một việc (không nêu khoảng thời gian) vẫn dò 10 năm tới như cũ, thêm đoạn
  **“Đối chiếu các năm đã qua”**: các năm từ 18 tuổi âm (tối đa 20 năm) có từ hai tầng vận cùng chỉ về việc đó. Người đã
  cưới so với năm thật: trùng hoặc lệch một năm là lá số, giờ sinh khá khớp; lệch xa thì nên xem lại giờ sinh.
- **Đối chiếu năm đã kể:** “Tôi đã cưới năm 2015, lá số có khớp không?” (năm đã qua + việc + “đã/vừa/có khớp/đúng không/
  đối chiếu…”) → câu trả lời “Đối chiếu năm 2015 với lá số”: câu kết luận (khớp / khớp một phần / tín hiệu gần nhất là năm
  nào), rồi năm đó và hai năm mỗi bên, mỗi năm có mức khả năng, tầng cùng chỉ, điểm chủ đề và căn cứ. Năm tương lai hoặc
  câu không có ý đối chiếu vẫn là luận năm như cũ.
- Sửa kèm: hỏi một năm trước năm sinh của lá số đang mở (vd. lá số sinh 2023 hỏi “năm 2015”) trước đây làm app báo lỗi;
  nay trả lời bằng lời nhắc `qa.truoc_nam_sinh`.
- Lời vẫn là mức khả năng (“khả năng cao / có thể / mới là dấu hiệu”), không khẳng định, kèm ghi chú `qa.note.kha_nang`.
- Hiệu năng: dò năm đã qua dùng `Composer.yearEvents` (chỉ tính sự kiện đồng thanh, bỏ phần chấm điểm chủ đề và ghép
  câu) và chạy song song các năm. Lần đầu dùng `yearData` cho từng năm, máy ảo Android 16 có lượt mất 4,4 giây cho
  “Khi nào tôi lấy vợ?” (chỉ tiêu dưới 3 giây); sau khi sửa, trên máy build từ 415 ms còn khoảng 200 ms.

### D-65 · Luận giải chi tiết hơn từng câu hỏi (0.9.0)
- Anh nhận xét phần luận còn ít và chung chung. Mình đọc lại câu trả lời mẫu và thấy bốn chỗ yếu: lời khuyên bị độn từ
  chủ đề khác (hỏi sức khỏe lại khuyên cưới hỏi, đọc hợp đồng); câu hỏi sức khỏe chỉ có hai đoạn; chưa giải thích từng
  sao trong cung; chưa nói “cụ thể trong năm dễ gặp gì”.
- **Từng sao trong cung …** (`QaAnalyst.starDetailPara`): chính tinh kèm hành, hóa khí, đặc tính; từng phụ tinh trong
  cung với nghĩa tại cung đó (bỏ vế câu nói về cung khác); sao lớn chiếu về từ tam phương tứ chính. Cung Tật Ách nêu
  thêm phần cơ thể nên để ý theo ngũ hành của chính tinh, kèm “đi khám khi có dấu hiệu kéo dài”.
- **Cụ thể trong năm** (`QaAnalyst.triggers`, `concretePara`): Tứ Hóa lưu và sao lưu (Lộc Tồn, Thiên Mã, Kình Dương,
  Đà La, Đào Hoa/Hồng Loan/Thiên Hỷ/Hỷ Thần, Tang Môn/Bạch Hổ, Thiên Khốc/Thiên Hư) chạm cung của chủ đề — trên lá số
  gốc, theo Thái Tuế, theo tiểu hạn, theo lưu niên đại hạn; tọa thủ nặng hơn xung chiếu. Mỗi tác nhân một câu “dễ xảy
  ra gì” và một câu “nên làm gì” theo đúng chủ đề (kho mới `texts/chi_tiet.json`, sinh từ `tools/kb/kb_chitiet.py`,
  7 chủ đề × 11 tác nhân). Hỏi tổng quan thì lấy các tác nhân mạnh nhất qua mọi chủ đề.
- **Theo tháng**: tháng nên chủ động / nên giữ cho chủ đề, tháng nguyệt hạn đi vào cung chính của chủ đề.
- **Lời khuyên**: chỉ lấy đúng chủ đề được hỏi; thêm lời khuyên theo tác nhân của năm (không thêm cho năm đã qua).
- **Tính cách**: thêm đoạn bản mệnh – Cục (tương quan), Mệnh chủ – Thân chủ, cung Thân, cung Phúc Đức.
- Câu hỏi “nên / lưu ý” (vd. sức khỏe) nay có: kết luận, từng sao, cụ thể trong năm, theo tháng, tổng hợp.
- Giới hạn độ dài câu trả lời trong test nâng từ 12 lên 16 đoạn.

### D-66 · Hỏi việc đã xảy ra (0.9.0)
- “Năm 2015 tôi gặp chuyện gì?”, “Năm 2019 có biến động gì không?” (năm đã qua hoặc năm nay, chưa nêu việc cụ thể) →
  `QaParse.past`, câu trả lời `pastYear`: các mặt thuận / vướng nổi bật nhất (điểm chủ đề), việc được các tầng vận
  chỉ tới, Tứ Hóa lưu và sao lưu chạm vào đâu (giọng kể lại, không kèm lời khuyên), Tứ Hóa năm vào lĩnh vực nào trên
  lá số gốc, căn cứ của các mặt nổi bật, tháng nổi bật, lời nhắc đối chiếu (`qa.note.doi_chieu`).
- “Những mốc quan trọng đã qua của tôi?” (có thể kèm chủ đề, vd. tình duyên) → `milestones`: dò từ 15 tuổi âm (tối
  đa 30 năm) các năm có việc được từ hai tầng vận cùng chỉ, chọn 8 năm nổi rõ nhất, xếp theo thời gian. Dùng chung
  phần tính sự kiện song song của D-64.
- Hỏi theo chủ đề cho năm đã qua (“Năm 2020 công việc thế nào?”) vẫn là luận năm, nhưng đoạn cụ thể đổi thành “Năm
  2020 lá số cho thấy…” và không kèm lời khuyên theo tác nhân.

### D-67 · So hai lá số: tương sinh tương khắc (0.9.0)
- Trước đây chỉ có truyền cung, truyền tinh. Thêm `Synastry`: ngũ hành bản mệnh (sinh / khắc / cùng hành, kèm hành
  trung gian khi khắc), thiên can năm sinh (ngũ hợp, sinh, khắc, cùng hành), địa chi năm sinh (tam hợp, lục hợp, lục
  xung, lục hại, tương hình, cùng tuổi), vị trí cung Mệnh hai người, Cục người này với bản mệnh người kia (hai chiều),
  hành chính tinh cung Mệnh.
- Mỗi quan hệ một câu (`hop.*`) và một mức điểm nhỏ; câu tổng nêu số điểm hợp, số điểm khắc xung và mức hòa hợp
  (50 + 6 × tổng, từ 10 đến 95), chia ba bậc: nhiều điểm hòa hợp / vừa hợp vừa khắc / cần vun đắp. Lời luôn là góc nhìn
  tham khảo, không phán quan hệ thành hay không; câu khắc đều kèm cách dung hòa.
- Các câu từng yếu tố là loại NOTE (mặt để cân nhắc), câu tổng là WEIGH, nên không phạm quy tắc “không câu một chiều
  trái nhau”.

### D-68 · Trợ lý AI: chọn model Nhanh / Mạnh, lời nhắc ưu tiên phần cụ thể (0.9.0)
- Anh muốn AI mạnh nhất có thể. Trong giới hạn hiện có (chạy trên máy, không fine-tune — D-62, không chỗ phát hành
  model riêng), cách mạnh nhất là cho chọn model lớn hơn cùng họ: **Gemma 4 E4B** (`litert-community/gemma-4-E4B-it-litert-lm`,
  Apache 2.0, không gated, 3,66 GB, ghim commit và SHA-256), bên cạnh **Gemma 4 E2B** (2,6 GB) hiện có.
- `kb/ai/model.json` nay có danh sách `models` và `default`. E4B: cần RAM khoảng 7 GB (S22 Ultra đủ), cửa sổ 6.144
  token, khối dữ kiện 10.500 ký tự, tối đa 1.000 token đầu ra, câu trả lời 80–500 chữ. E2B giữ nguyên.
- Thẻ Trợ lý AI có hai lựa chọn (Nhanh / Mạnh), ghi dung lượng, RAM cần, đã tải hay chưa; máy thiếu RAM cho E4B vẫn
  chọn lại được E2B. Hai model có thể cùng nằm trên máy; đổi model khi đang tải thì app báo hủy tải trước.
  `AndroidLlm` nhớ lựa chọn, đóng model cũ khỏi RAM khi đổi.
- Lời nhắc: khi dữ kiện vượt ngân sách, chọn đoạn theo độ ưu tiên (kết luận → cụ thể trong năm → đồng thanh → các
  tầng → tháng → từng sao → sách → tổng hợp), rồi giữ thứ tự gốc. System prompt yêu cầu nói rõ việc cụ thể dễ xảy ra,
  tháng nên chủ động, nghĩa các sao chính, kể lại khi là năm đã qua; độ dài lấy theo model.
- `AiGuard` giữ nguyên: câu nào sai sao, sai cung, bịa số, có từ cấm thì app hiện bản luận của app.
