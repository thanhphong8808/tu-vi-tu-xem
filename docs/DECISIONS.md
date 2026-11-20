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
