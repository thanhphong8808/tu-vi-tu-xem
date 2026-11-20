# Prompt xây dựng App Android Tử Vi Đẩu Số — Tự xem lá số & vận hạn

Nov 20, 2026 · @Nguyen Thanh Phong

## 0. Cách dùng prompt này

Mục 1–15 là một prompt liền mạch để giao cho agent lập trình (Claude Code, Cursor, Windsurf…); mục 16 là phụ lục đính chính cẩm nang và nguồn, cũng nên gửi kèm.

1. Export doc này ra Markdown, lưu thành `docs/PROMPT_TUVI_APP.md` trong thư mục dự án.
2. Đặt file cẩm nang của bạn cạnh nó, đổi tên `docs/CAMNANG_TUVI_V2.txt`.
3. Lệnh đầu tiên cho agent: “Đọc `docs/PROMPT_TUVI_APP.md` và `docs/CAMNANG_TUVI_V2.txt`. Thực hiện Giai đoạn 1 theo mục 15. Dừng lại khi toàn bộ test của giai đoạn đó pass và báo cáo.”
4. Mỗi giai đoạn sau dùng câu lệnh tương tự. Không giao nhiều giai đoạn trong một lệnh: engine an sao phải đúng tuyệt đối trước khi viết luận giải và UI.

Khi prompt và cẩm nang mâu thuẫn, prompt thắng: một số quy tắc trong cẩm nang đã được đối chiếu lại với nguồn và sửa (xem mục 16).

## 1. Vai trò & mục tiêu sản phẩm

Bạn là kỹ sư Android cấp cao kiêm chuyên gia Tử Vi Đẩu Số Nam phái Việt Nam. Nhiệm vụ: xây dựng app Android **“Tử Vi Tự Xem”** (tên tạm) chạy offline, cho người dùng nhập giờ–ngày–tháng–năm sinh, lập lá số chính xác theo quy tắc trong prompt này, rồi đọc luận giải chi tiết cho cả đời, đại vận 10 năm, tiểu vận/lưu niên, tháng và ngày, kèm khuyến nghị nên/không nên cho giai đoạn tới.

**Sáu nguyên tắc bất di bất dịch**

1. **Tính toán tách hẳn khỏi luận giải.** Engine an sao là hàm thuần (pure function) có test đầy đủ. Lớp luận giải chỉ đọc kết quả engine, không bao giờ tự suy ra vị trí sao.
2. **Mọi câu luận phải truy được nguồn.** Mỗi câu gắn với một `ruleId` cùng danh sách sao/cung kích hoạt nó; bấm “Vì sao?” là thấy lập luận.
3. **Không bịa.** Tổ hợp chưa có rule thì ghi rõ “chưa có luận giải riêng cho tổ hợp này”, không tự chế.
4. **Trường phái là cấu hình, không phải hằng số.** Các điểm sách vở vênh nhau (Tứ Hóa năm Canh/Nhâm, Khôi Việt, Hỏa Linh, tháng nhuận, nguyệt hạn…) nằm trong `SchoolConfig`; mặc định theo Nam phái VN (Thái Thứ Lang – *Tử Vi Đẩu Số Tân Biên*), khớp cẩm nang V2.
5. **Ngôn ngữ có trách nhiệm** (mục 11): nói xu hướng và điều kiện, không phán định mệnh; không phán chết, bệnh nặng, ly hôn “100%”.
6. **Tiếng Việt có dấu chuẩn** ở mọi nơi, đúng chính tả thuật ngữ Tử Vi.

**Phạm vi v1:** Android 8.0+ (minSdk 26); lập lá số và luận giải rule-based chạy offline 100%; trợ lý AI hỏi đáp chạy ngay trên máy, tải model khi người dùng bật (mục 12). **Ngoài phạm vi v1:** xem tuổi hợp, chọn ngày theo Ngọc Hạp, Bát Tự, phong thủy.

## 2. Tính năng & luồng người dùng

Người dùng nhập thông tin sinh một lần, xác nhận lá số, rồi xem 6 tầng luận giải từ cả đời xuống từng ngày.

### 2.1 Form nhập liệu

| Trường | Bắt buộc | Quy tắc |
| --- | --- | --- |
| Họ tên / biệt danh | Không | Chỉ để hiển thị, lưu cục bộ |
| Giới tính | Có | Nam/Nữ — quyết định Âm Dương Nam Nữ và chiều đại hạn, tiểu hạn |
| Ngày sinh | Có | Chọn Dương lịch hoặc Âm lịch. Âm lịch có ô “tháng nhuận”, chỉ bật khi năm đó thật sự nhuận đúng tháng đó |
| Giờ sinh | Có | Hai chế độ: giờ:phút chính xác, hoặc chọn 1 trong 12 canh giờ (Tý 23:00–00:59…). Có lựa chọn “Không rõ giờ” (xem 2.3) |
| Nơi sinh | Không | Mặc định Việt Nam. Sinh ở nước ngoài: chọn múi giờ IANA; tùy chọn hiệu chỉnh giờ Mặt Trời thật theo kinh độ (mục 4) |
| Thời điểm đang xem | Có | Mặc định là hôm nay; đổi được để xem năm/tháng/ngày khác |

### 2.2 Thẻ “Xác nhận lá số” (bắt buộc trước khi luận)

Hiển thị: dương lịch ↔ âm lịch, can chi năm/tháng/ngày/giờ, Âm Dương Nam Nữ, bản mệnh nạp âm, Cục, cung Mệnh và cung Thân, tuổi âm hiện tại. Bật cảnh báo và cho xem lá số thay thế khi:

- Sinh 23:00–23:59 (giờ Tý muộn — có trường phái đổi ngày, có trường phái không).
- Sinh trong tháng nhuận.
- Sinh trước 08/08/1967 ở miền Bắc hoặc trước 1975 ở miền Nam (âm lịch khi đó tính theo UTC+8).
- Giờ sinh nằm trong 10 phút sát ranh giới hai canh giờ.

### 2.3 Sáu tầng luận giải và các màn hình

1. **Lá số (địa bàn 12 cung)** tương tác: chạm một cung → tô sáng tam phương tứ chính, hai cung giáp, cung nhị hợp.
2. **Tử vi trọn đời:** tổng quan Mệnh–Thân–Phúc; 12 cung từng mục; các cách cục phát hiện được (cát, hung, phản vi kỳ cách); dòng thời gian 12 đại vận.
3. **Đại vận 10 năm:** cung, can chi đại hạn, Thiên thời theo ngũ hành tam hợp (cụ Thiên Lương), điểm mạnh/yếu, chủ đề chính, nên/không nên.
4. **Tiểu vận / lưu niên (1 năm):** tổng hợp 4 tầng — đại hạn, tiểu hạn, lưu niên đại hạn, Lưu Thái Tuế + sao lưu + Tứ Hóa lưu. Chia chuyên đề: công việc, tài chính, tình cảm/hôn nhân, sức khỏe (dạng lưu ý), gia đạo, đi xa, pháp lý/thị phi.
5. **Tháng:** 12 tháng âm của năm đang xem dạng heat-map, chạm vào để xem chi tiết.
6. **Ngày:** lịch tháng có điểm từng ngày; chi tiết ngày gồm nhật hạn, can chi ngày, giờ thuận trong ngày. Gắn nhãn “độ tin cậy thấp hơn tầng năm”.

Bổ sung: ngăn kéo **“Vì sao?”** cho mọi câu luận; **hồ sơ nhiều người** và so truyền cung/truyền tinh vợ chồng, cha con (cẩm nang phần V.4); **xuất PDF/ảnh** lá số và bản luận; **Cài đặt trường phái**.

**Trợ lý hỏi đáp** (mục 12): khung chat để hỏi về lá số và vận hạn như trò chuyện trực tiếp; chip gợi ý câu hỏi thường gặp; mỗi câu trả lời có “Vì sao?” và nút 👍/👎.

**Không rõ giờ sinh:** lập đủ 12 lá số, hiển thị khác biệt chính giữa chúng (cung Mệnh, chính tinh thủ Mệnh, đại hạn), kèm trình hỏi–đáp 5–7 câu về sự kiện đã qua (năm cưới, năm đổi việc lớn…) để xếp hạng giờ khả dĩ nhất. Luôn ghi rõ đây là ước đoán.

**Luồng chính:** Onboarding (lưu ý mục 11) → Nhập → Xác nhận → Lá số → các tab Trọn đời / Đại vận / Năm / Tháng / Ngày / Hỏi đáp.

## 3. Kiến trúc kỹ thuật & stack Android

App gồm bốn lớp tách biệt — lịch, engine an sao, engine luận giải, UI — trao đổi qua data class bất biến; chỉ lớp UI phụ thuộc Android, ba lớp còn lại là Kotlin/JVM thuần để test nhanh trên máy dev.

**Stack:** Kotlin 2.x, Jetpack Compose + Material 3, Navigation Compose, Hilt, Room (hồ sơ), DataStore (cài đặt), kotlinx.serialization (knowledge base JSON), java.time (có sẵn từ Android 8), JUnit5 + property test có seed (Kotest tùy chọn), LiteRT-LM (trợ lý AI trên máy), Firebase Firestore (đồng bộ câu hỏi thường gặp khi người dùng đồng ý).

| Module | Loại | Trách nhiệm |
| --- | --- | --- |
| `:core:calendar` | Kotlin thuần | Dương ↔ âm lịch VN (Hồ Ngọc Đức), can chi năm/tháng/ngày/giờ, giờ Mặt Trời thật |
| `:core:engine` | Kotlin thuần | `ChartEngine` (lập lá số), `LimitEngine` (vận hạn), `SchoolConfig` |
| `:core:interpret` | Kotlin thuần | `RuleEngine`, chấm điểm, dò cách cục, ghép câu luận, khuyến nghị |
| `:data:kb` | JSON resources | Knowledge base có version: sao, cung, cách cục, câu luận, khuyến nghị, câu trả lời mẫu đã duyệt (`faq/`) |
| `:data:store` | Android lib | Room (hồ sơ) + DataStore (cài đặt) |
| `:data:faq` | Android lib | Câu hỏi thường gặp: Room cục bộ, bộ lọc ẩn danh, đồng bộ Firestore khi người dùng đồng ý (mục 12.5–12.6) |
| `:feature:*` | Android | input, chart, lifetime, decade, year, month, day, assistant, settings, export |
| `:ai` | Android lib | Trợ lý AI trên máy: LiteRT-LM + Gemma 4 E2B, tải model, ghép ngữ cảnh, kiểm tra đầu ra (mục 12.1–12.4) |

**Kiểu dữ liệu lõi** (đặt trong `:core:engine`, giữ đúng tên để các mục sau tham chiếu):

```kotlin
// Chỉ số cố định: Tý=0, Sửu=1, Dần=2 … Hợi=11. “Thuận” = +1, “nghịch” = −1 (mod 12).
// Can, Chi, LunarDate, BirthRegion nằm ở :core:calendar (engine xuất lại qua api) — DECISIONS D-01.
enum class Chi(val vn: String) { TY("Tý"), SUU("Sửu"), DAN("Dần"), MAO("Mão"), THIN("Thìn"), TI("Tỵ"),
    NGO("Ngọ"), MUI("Mùi"), THAN("Thân"), DAU("Dậu"), TUAT("Tuất"), HOI("Hợi") }
enum class Can(val vn: String) { GIAP("Giáp"), AT("Ất"), BINH("Bính"), DINH("Đinh"), MAU("Mậu"),
    KY("Kỷ"), CANH("Canh"), TAN("Tân"), NHAM("Nhâm"), QUY("Quý") }
enum class PalaceRole { MENH, PHU_MAU, PHUC_DUC, DIEN_TRACH, QUAN_LOC, NO_BOC,
    THIEN_DI, TAT_ACH, TAI_BACH, TU_TUC, PHU_THE, HUYNH_DE }   // thứ tự = Chỉ số thuận từ Mệnh
enum class Brightness { MIEU, VUONG, DAC, BINH, HAM }
enum class BirthRegion { VN_BAC, VN_NAM, NUOC_NGOAI }
enum class CanhGio { TY_SOM, SUU, DAN, MAO, THIN, TI, NGO, MUI, THAN, DAU, TUAT, HOI, TY_MUON }

data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean)
// java.time thay kotlinx-datetime (D-02); các trường thêm theo D-04.
data class BirthInput(val solar: LocalDateTime?, val lunar: LunarDate?, val gender: Gender,
    val zone: ZoneId, val longitudeDeg: Double?, val useTrueSolarTime: Boolean,
    val lunarClock: LocalTime?, val canh: CanhGio?, val region: BirthRegion, val calendarTzOverride: Int?)
data class PlacedStar(val id: StarId, val brightness: Brightness?, val kind: StarKind)
data class Palace(val chi: Chi, val can: Can, val role: PalaceRole, val stars: List<PlacedStar>,
    val daiHanStartAge: Int, val tieuHanChi: Chi, val hasTuan: Boolean, val hasTriet: Boolean, val isThan: Boolean)
data class Chart(val input: BirthInput, val lunar: LunarDate, val yearCan: Can, val yearChi: Chi,
    val hourChi: Chi, val isYangYear: Boolean, val banMenh: NapAm, val cuc: Cuc, val menh: Chi, val than: Chi,
    val palaces: Map<Chi, Palace>, val config: SchoolConfig, val warnings: List<ChartWarning>
    /* + gender, amDuongNamNu, birth, pillars, effectiveMonth, menhChu, thanChu, tuan, triet, starPositions */)
data class LimitLayers(val daiHan: Chi, val tieuHan: Chi, val luuNienDaiHan: Chi, val luuThaiTue: Chi,
    val luuStars: Map<StarId, Chi>, val luuTuHoa: Map<TuHoa, Chi>, val nguyetHan: Chi?, val nhatHan: Chi?, val thoiHan: Chi?)
data class Finding(val ruleId: String, val scope: Scope, val topic: Topic, val polarity: Int /* -2..+2 */,
    val weight: Double, val evidence: List<Evidence>, val textKey: String, val adviceKeys: List<String>)
```

**Ràng buộc phi chức năng**

- `ChartEngine.build(input, config)` là hàm thuần, deterministic; cache theo hash(input, config, kbVersion).
- Hiệu năng trên Galaxy S22 Ultra: lập lá số < 5 ms, luận một năm < 50 ms, chấm điểm 30 ngày < 300 ms.
- Quyền riêng tư: ngày giờ sinh chỉ lưu trên máy; không analytics chứa dữ liệu sinh; trợ lý AI chạy trên máy nên lá số không rời điện thoại; chỉ câu hỏi đã ẩn danh được gửi lên Firebase khi người dùng bật đồng ý (mục 12.6).
- Knowledge base tách khỏi code để cập nhật câu luận không cần sửa engine.

## 4. Lịch âm Việt Nam & chuẩn hóa đầu vào

Dùng thuật toán Hồ Ngọc Đức (port sang Kotlin) với múi giờ UTC+7; cấm dùng thư viện âm lịch Trung Quốc, vì lịch Việt Nam và Trung Quốc cùng quy tắc nhưng lệch múi giờ nên khác ngày, khác tháng, khác Tết ở một số năm ([VnExpress](https://vnexpress.net/lich-sai-do-dich-sach-trung-quoc-2814087.html)).

1. **Thuật toán:** port nguyên bản `jdFromDate`, `jdToDate`, `NewMoon(k)`, `SunLongitude`, `getSunLongitude(dayNumber, tz) = floor(SunLongitude(dayNumber − 0.5 − tz/24) / π × 6)`, `getNewMoonDay`, `getLunarMonth11`, `getLeapMonthOffset`, `convertSolar2Lunar`, `convertLunar2Solar`. Dùng `Double` và `floor()` — không dùng `toInt()` vì nó cắt về 0 với số âm. Không dùng bản Python `lasotuvi` làm gốc vì hàm `SunLongitude` trong đó đã bị sửa khác bản gốc. Vá một lỗi biên của bản gốc: convertSolar2Lunar chỉ lùi sóc một lần nên trả về “ngày 0” ở 7 ngày trong 1900–2100 (vd. 19/04/1939 và 22/03/1947 theo UTC+8); lùi bằng vòng lặp tới khi mùng 1 không vượt ngày đang đổi (DECISIONS D-03).
2. **Múi giờ lịch theo thời kỳ:** miền Bắc chuyển lịch từ UTC+8 sang UTC+7 ngày 08/08/1967, miền Nam chuyển năm 1975 ([Wikipedia](https://en.wikipedia.org/wiki/Vietnamese_calendar)); vì vậy hai miền ăn Tết Mậu Thân 1968 lệch nhau một ngày ([tư liệu Tết](https://www.namkyluctinh.org/a-vh-vminh/tet-wiki.pdf)). Quy tắc: trước 08/08/1967 dùng tz = 8; từ 08/08/1967 đến 30/04/1975 hỏi nơi sinh (Bắc → 7, Nam → 8); sau đó tz = 7. Nếu tz = 7 và tz = 8 cho ngày âm khác nhau, hiện cảnh báo và cho xem cả hai lá số.
3. **Giờ sinh → canh giờ:** `hourChi = ((giờ + 1) / 2) % 12` (Tý 23:00–00:59, Sửu 01:00–02:59 … Hợi 21:00–22:59).
4. **Đổi ngày lúc 23:00 (mặc định):** sinh 23:00–23:59 thì lấy ngày dương hôm sau để quy đổi âm lịch. Tùy chọn `TY_MUON_GIU_NGAY` giữ nguyên ngày.
5. **Giờ đồng hồ lịch sử:** quy giờ ghi trên giấy khai sinh về UTC+7 bằng tzdb (`Asia/Ho_Chi_Minh` cho miền Nam, nơi giờ chính thức từng là UTC+8 giai đoạn 1960–1975). Luôn hiện giờ đã quy đổi để người dùng kiểm tra; nếu quy đổi làm đổi canh giờ thì cho xem cả hai lá số.
6. **Giờ Mặt Trời thật (tùy chọn, mặc định tắt):** `t_solar = t_clock + 4 phút × (kinh độ − 15 × offsetGiờ) + EoT`. Ví dụ TP.HCM (106,7°E) lệch +6,8 phút so với kinh tuyến 105° trước khi cộng EoT.
7. **Tháng nhuận:** `SchoolConfig.leapMonth` = `SAME_MONTH` (mặc định, tính như tháng chính) hoặc `SPLIT_15` (ngày 1–15 tính tháng chính, ngày 16 trở đi tính tháng sau). Sinh tháng nhuận luôn hiện cảnh báo và nút xem lá số theo cách còn lại.
8. **Can chi:** ngày `can = (jd + 9) % 10`, `chi = (jd + 1) % 12`; tháng: can tháng Giêng `= (2·canNăm + 2) % 10` (Ngũ hổ độn), chi tháng `= (tháng + 1) % 12`; giờ: can giờ Tý `= (2·canNgày) % 10` (Ngũ thử độn). Năm Tử Vi đổi vào mùng 1 Tết, không theo Lập Xuân. Tuổi âm = năm âm đang xem − năm âm sinh + 1.

**Test vector lịch bắt buộc** (đã chạy kiểm tra bằng bản tham chiếu):

| Dương lịch | tz | Âm lịch mong đợi | Kiểm tra điều gì |
| --- | --- | --- | --- |
| 17/02/2026 | 7 | 1/1/2026 (Bính Ngọ) | Tết 2026 |
| 22/03/2023 | 7 | 1/2 nhuận/2023 | Tháng nhuận |
| 23/05/2020 | 7 | 1/4 nhuận/2020 | Tháng nhuận |
| 17/02/2007 | 7 | 1/1/2007 | Tết VN; lịch TQ (tz 8) là 18/02 |
| 15/08/1990 | 7 | 25/6/1990, ngày Nhâm Tý | Can chi ngày |
| 03/03/1985 | 7 và 8 | 12/2/1985 và 12/1/1985 | Cùng ngày dương, lệch cả tháng |
| 21/01/1985 | 7 | 1/1/1985 (Ất Sửu) | Tết VN 1985; TQ ăn Tết 20/02 |
| 30/01/1968 | 8 | 1/1/1968 (Mậu Thân) | Tết miền Nam |
| 29/01/1968 | 7 | 1/1/1968 (Mậu Thân) | Tết miền Bắc |

## 5. Thuật toán an lá số (SchoolConfig mặc định)

Mọi vị trí là số học mod 12 với Tý = 0; đặc tả dưới đây đã được cài thành bản tham chiếu và đối chiếu 3.000 lá số ngẫu nhiên với mã nguồn mở [lasotuvi](https://github.com/doanguyen/lasotuvi): khớp 100% Mệnh, Thân, Cục, 14 chính tinh, đại hạn, tiểu hạn và mọi phụ tinh không thuộc nhóm biến thể trường phái (lasotuvi đảo ngược Thai–Dưỡng, đặt Phá Toái ngược khẩu quyết và đảo Khôi/Việt ở tuổi Đinh, Mậu, Nhâm; đặc tả này theo khẩu quyết kinh điển — đã đối chiếu lại 9.000 lá số ở Giai đoạn 1, DECISIONS D-14).

**Ký hiệu:** `c` can năm (Giáp = 0 … Quý = 9); `z` chi năm; `m` tháng âm 1–12; `D` ngày âm 1–30; `h` chi giờ; năm Dương khi `c` chẵn; `dir = +1` nếu Dương Nam hoặc Âm Nữ, `−1` nếu Âm Nam hoặc Dương Nữ; `T` nhóm tam hợp của `z`: 0 = Thân Tý Thìn, 1 = Tỵ Dậu Sửu, 2 = Dần Ngọ Tuất, 3 = Hợi Mão Mùi.

### 5.1 Cung và Cục

| Bước | Công thức |
| --- | --- |
| Cung Mệnh | `(2 + (m−1) − h) mod 12` — Dần là tháng Giêng, đếm thuận tới tháng sinh, coi là giờ Tý, đếm nghịch tới giờ sinh |
| Cung Thân | `(2 + (m−1) + h) mod 12` |
| 12 cung | Vai trò thứ k đặt ở `Mệnh + k`, k theo thứ tự Mệnh, Phụ Mẫu, Phúc Đức, Điền Trạch, Quan Lộc, Nô Bộc, Thiên Di, Tật Ách, Tài Bạch, Tử Tức, Phu Thê, Huynh Đệ (bằng với cẩm nang đi nghịch Mệnh → Huynh Đệ → Phu Thê…) |
| Can từng cung | `canDần = (2c + 2) mod 10`; `can(p) = (canDần + (p − 2) mod 12) mod 10` |
| Nạp âm | `v = ⌊can/2⌋ + 1 + ⌊(chi mod 6)/2⌋`; nếu `v > 5` thì `v − 5`; 1 Kim, 2 Thủy, 3 Hỏa, 4 Thổ, 5 Mộc |
| Cục | Nạp âm của (can cung Mệnh, chi cung Mệnh): Thủy 2, Mộc 3, Kim 4, Thổ 5, Hỏa 6 |
| Bản mệnh | Nạp âm của (c, z) |

### 5.2 Mười bốn chính tinh

- **Tử Vi:** `q = ⌈D / cục⌉`, `y = q·cục − D`, `base = 2 + q − 1`; Tử Vi = `base − y` nếu y lẻ, `base + y` nếu y chẵn (kể cả y = 0).
- **Thiên Phủ** = `(4 − Tử Vi) mod 12` (đối xứng qua trục Dần–Thân).
- Nhóm Tử Vi (cộng vào vị trí Tử Vi): Thiên Cơ −1, Thái Dương −3, Vũ Khúc −4, Thiên Đồng −5, Liêm Trinh −8.
- Nhóm Thiên Phủ (cộng vào vị trí Thiên Phủ): Thái Âm +1, Tham Lang +2, Cự Môn +3, Thiên Tướng +4, Thiên Lương +5, Thất Sát +6, Phá Quân +10.

### 5.3 Bốn vòng sao

| Vòng | Khởi | Chiều | Thứ tự |
| --- | --- | --- | --- |
| Lộc Tồn | Theo can (bảng 5.5) | — | Kình Dương = Lộc Tồn + 1, Đà La = Lộc Tồn − 1 (không theo dir) |
| Bác Sĩ | Tại Lộc Tồn | dir | Bác Sĩ, Lực Sĩ, Thanh Long, Tiểu Hao, Tướng Quân, Tấu Thư, Phi Liêm, Hỷ Thần, Bệnh Phù, Đại Hao, Phục Binh, Quan Phủ |
| Thái Tuế | Tại z | Luôn thuận | Thái Tuế, Thiếu Dương, Tang Môn, Thiếu Âm, Quan Phù, Tử Phù, Tuế Phá, Long Đức, Bạch Hổ, Phúc Đức, Điếu Khách, Trực Phù |
| Tràng Sinh | Theo Cục: Kim → Tỵ, Mộc → Hợi, Thủy và Thổ → Thân, Hỏa → Dần | dir | Tràng Sinh, Mộc Dục, Quan Đới, Lâm Quan, Đế Vượng, Suy, Bệnh, Tử, Mộ, Tuyệt, Thai, Dưỡng |

Đồng cung cố định: Thiên Không = Thiếu Dương (z+1), Nguyệt Đức = Tử Phù (z+5), Thiên Đức = Phúc Đức (z+9).

### 5.4 Phụ tinh theo tháng, giờ, ngày, chi năm

| Nhóm | Công thức |
| --- | --- |
| Theo tháng | Tả Phù `4+(m−1)`; Hữu Bật `10−(m−1)`; Thiên Hình `9+(m−1)`; Thiên Riêu = Thiên Y `1+(m−1)`; Địa Giải `7+(m−1)`; Thiên Giải `8+(m−1)` (biến thể “nhảy cung”: `8+2(m−1)`) |
| Theo giờ | Văn Khúc `4+h`; Văn Xương `10−h`; Địa Kiếp `11+h`; Địa Không `11−h`; Thai Phụ = Văn Khúc + 2; Phong Cáo = Văn Khúc − 2 |
| Theo ngày | Tam Thai = Tả Phù + (D−1); Bát Tọa = Hữu Bật − (D−1); Ân Quang = Văn Xương + (D−2); Thiên Quý = Văn Khúc − (D−2) |
| Theo chi năm | Long Trì `4+z`; Phượng Các = Giải Thần `10−z`; Thiên Khốc `6−z`; Thiên Hư `6+z`; Hồng Loan `3−z`; Thiên Hỷ `9−z`; Thiên Tài = Mệnh + z; Thiên Thọ = Thân + z; Đẩu Quân `z − (m−1) + h` |
| Theo tam hợp T | Thiên Mã \[Dần, Hợi, Thân, Tỵ\]; Hoa Cái \[Thìn, Sửu, Tuất, Mùi\]; Kiếp Sát \[Tỵ, Dần, Hợi, Thân\]; Đào Hoa \[Dậu, Ngọ, Mão, Tý\] |
| Cô Thần / Quả Tú | Hợi Tý Sửu → Dần; Dần Mão Thìn → Tỵ; Tỵ Ngọ Mùi → Thân; Thân Dậu Tuất → Hợi; Quả Tú = Cô Thần − 4 |
| Phá Toái | Tý Ngọ Mão Dậu → Tỵ; Dần Thân Tỵ Hợi → Dậu; Thìn Tuất Sửu Mùi → Sửu |
| Cố định | Thiên La tại Thìn; Địa Võng tại Tuất; Thiên Thương tại cung Nô Bộc; Thiên Sứ tại cung Tật Ách |
| Tuần | `s = (z − c) mod 12`; Tuần tại `s+10` và `s+11` |

### 5.5 Sao an theo can năm

| Sao | Giáp | Ất | Bính | Đinh | Mậu | Kỷ | Canh | Tân | Nhâm | Quý |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Lộc Tồn | Dần | Mão | Tỵ | Ngọ | Tỵ | Ngọ | Thân | Dậu | Hợi | Tý |
| Thiên Khôi | Sửu | Tý | Hợi | Hợi | Sửu | Tý | Ngọ | Ngọ | Mão | Mão |
| Thiên Việt | Mùi | Thân | Dậu | Dậu | Mùi | Thân | Dần | Dần | Tỵ | Tỵ |
| Thiên Quan | Mùi | Thìn | Tỵ | Dần | Mão | Dậu | Hợi | Dậu | Tuất | Ngọ |
| Thiên Phúc | Dậu | Thân | Tý | Hợi | Mão | Dần | Ngọ | Tỵ | Ngọ | Tỵ |
| Lưu Hà | Dậu | Tuất | Mùi | Thân | Tỵ | Ngọ | Thìn | Mão | Hợi | Dần |
| Thiên Trù | Tỵ | Ngọ | Tý | Tỵ | Ngọ | Thân | Dần | Ngọ | Dậu | Tuất |
| Triệt | Thân–Dậu | Ngọ–Mùi | Thìn–Tỵ | Dần–Mão | Tý–Sửu | Thân–Dậu | Ngọ–Mùi | Thìn–Tỵ | Dần–Mão | Tý–Sửu |

Văn Tinh = Lộc Tồn + 3; Đường Phù = Lộc Tồn + 5; Quốc Ấn = Lộc Tồn + 8. Khôi–Việt ở trên là bản phổ thông VN (Canh Tân → Ngọ/Dần); biến thể Toàn Thư “Giáp Mậu Canh ngưu dương” đưa Canh về Sửu/Mùi (mục 7).

### 5.6 Hỏa Tinh, Linh Tinh

| Tam hợp chi năm | Khởi Hỏa (giờ Tý) | Khởi Linh (giờ Tý) |
| --- | --- | --- |
| Dần Ngọ Tuất | Sửu | Mão |
| Thân Tý Thìn | Dần | Tuất |
| Tỵ Dậu Sửu | Mão | Tuất |
| Hợi Mão Mùi | Dậu | Tuất |

Hỏa = khởiHỏa + h·dir; Linh = khởiLinh − h·dir (Dương Nam, Âm Nữ: Hỏa thuận, Linh nghịch; Âm Nam, Dương Nữ đảo chiều). Theo Thái Thứ Lang ([Học quán Sơn Chu](https://sonchu.vn/nghien-cuu/sai-lam-ve-an-sao-lap-so)); biến thể Đắc Lộc đổi nhóm Tỵ Dậu Sửu thành Hỏa khởi Tuất, Linh khởi Mão — bản lasotuvi dùng biến thể này.

### 5.7 Mệnh chủ, Thân chủ

- **Mệnh chủ** theo chi cung Mệnh: Tý Tham Lang; Sửu, Hợi Cự Môn; Dần, Tuất Lộc Tồn; Mão, Dậu Văn Khúc; Thìn, Thân Liêm Trinh; Tỵ, Mùi Vũ Khúc; Ngọ Phá Quân.
- **Thân chủ** theo chi năm sinh: Tý Linh Tinh (Toàn Thư: Hỏa Tinh); Sửu, Mùi Thiên Tướng; Dần, Thân Thiên Lương; Mão, Dậu Thiên Đồng; Thìn, Tuất Văn Xương; Tỵ, Hợi Thiên Cơ; Ngọ Hỏa Tinh.

**Thứ tự hiển thị sao trong cung:** chính tinh → Tứ Hóa → lục cát (Tả Hữu, Xương Khúc, Khôi Việt) → Lộc Tồn, Thiên Mã → lục sát (Kình Đà, Hỏa Linh, Không Kiếp) → trung tinh cát → trung tinh hung → sao vòng.

## 6. Bảng miếu – vượng – đắc – bình – hãm

Dùng bảng dưới làm `brightness.json` mặc định (theo Thái Thứ Lang, trùng bảng của lasotuvi; Thiên Phủ theo cẩm nang). Ký hiệu: M miếu, V vượng, Đ đắc, B bình hòa, H hãm.

| Chính tinh | Tý | Sửu | Dần | Mão | Thìn | Tỵ | Ngọ | Mùi | Thân | Dậu | Tuất | Hợi |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Tử Vi | B | Đ | M | B | V | M | M | Đ | M | B | V | B |
| Thiên Cơ | Đ | Đ | H | M | M | V | Đ | Đ | V | M | M | H |
| Thái Dương | H | Đ | V | V | V | M | M | Đ | H | H | H | H |
| Vũ Khúc | V | M | V | Đ | M | H | V | M | V | Đ | M | H |
| Thiên Đồng | V | H | M | Đ | H | Đ | H | H | M | H | H | Đ |
| Liêm Trinh | V | Đ | V | H | M | H | V | Đ | V | H | M | H |
| Thiên Phủ | M | B | M | B | V | Đ | M | B | M | B | V | Đ |
| Thái Âm | V | Đ | H | H | H | H | H | Đ | V | M | M | M |
| Tham Lang | H | M | Đ | H | V | H | H | M | Đ | H | V | H |
| Cự Môn | V | H | V | M | H | H | V | H | Đ | M | H | Đ |
| Thiên Tướng | V | Đ | M | H | V | Đ | V | Đ | M | H | V | Đ |
| Thiên Lương | V | Đ | V | V | M | H | M | Đ | V | H | M | H |
| Thất Sát | M | Đ | M | H | H | V | M | Đ | M | H | H | V |
| Phá Quân | M | V | H | H | Đ | H | M | V | H | H | Đ | H |

**Phụ tinh có đắc/hãm** (ô trống = không xét):

| Sao | Đắc địa | Còn lại |
| --- | --- | --- |
| Kình Dương, Đà La | Thìn, Tuất, Sửu, Mùi | Hãm |
| Hỏa Tinh, Linh Tinh | Dần, Mão, Thìn, Tỵ, Ngọ | Hãm |
| Địa Không, Địa Kiếp | Dần, Thân, Tỵ, Hợi | Hãm |
| Văn Xương, Văn Khúc | Sửu, Mão, Tỵ, Mùi, Tuất, Hợi | Hãm (bảng lasotuvi — cần đối chiếu thêm) |
| Hóa Kỵ | Thìn, Tuất, Sửu, Mùi | — |
| Đại Hao, Tiểu Hao | Dần, Mão, Thân, Dậu | — |
| Thiên Khốc, Thiên Hư | Tý, Sửu, Mão, Ngọ, Mùi, Dậu | — |
| Thiên Hình | Dần, Mão, Thân, Dậu | — |
| Thiên Riêu | Dần, Mão, Dậu, Tuất | — |

**Chỗ khác cẩm nang V2 (đã sửa trong bảng):** Thiên Cơ tại Dậu là Miếu, không phải Hãm (cẩm nang tự mâu thuẫn với cách “Cơ Cự Mão Dậu” của chính nó); Vũ Khúc tại Mão, Dậu là Đắc, không phải Hãm; Thiên Đồng tại Tỵ và Liêm Trinh tại Sửu, Mùi là Đắc (cẩm nang bỏ sót). Cự Môn: bảng trên ghi Tý/Ngọ Vượng, Thân Đắc; cẩm nang ghi ngược lại — đặt thành lựa chọn `cuMonTable`.

**Trọng số gợi ý cho chấm điểm (mục 9):** M +2, V +1,5, Đ +1, B 0, H −1,5. Sao hãm không tự động là xấu: luôn chạy bộ luật “phản vi kỳ cách” ở mục 10 trước khi kết luận.

## 7. Tứ Hóa & biến thể trường phái

Tứ Hóa an theo can năm; bảng mặc định “Nam phái VN” khớp cẩm nang (năm Canh: Thiên Đồng hóa Kỵ, nên người Mệnh Thiên Đồng sợ năm Canh).

| Can | Hóa Lộc | Hóa Quyền | Hóa Khoa | Hóa Kỵ |
| --- | --- | --- | --- | --- |
| Giáp | Liêm Trinh | Phá Quân | Vũ Khúc | Thái Dương |
| Ất | Thiên Cơ | Thiên Lương | Tử Vi | Thái Âm |
| Bính | Thiên Đồng | Thiên Cơ | Văn Xương | Liêm Trinh |
| Đinh | Thái Âm | Thiên Đồng | Thiên Cơ | Cự Môn |
| Mậu | Tham Lang | Thái Âm | Hữu Bật | Thiên Cơ |
| Kỷ | Vũ Khúc | Tham Lang | Thiên Lương | Văn Khúc |
| Canh | Thái Dương | Vũ Khúc | Thái Âm | Thiên Đồng |
| Tân | Cự Môn | Thái Dương | Văn Khúc | Văn Xương |
| Nhâm | Thiên Lương | Tử Vi | Tả Phù | Vũ Khúc |
| Quý | Phá Quân | Cự Môn | Thái Âm | Tham Lang |

**Bốn tầng Tứ Hóa:** gốc (can năm sinh); lưu niên (can năm đang xem, luôn bật); đại hạn (can của cung đại hạn theo Ngũ hổ độn — cách của Bắc phái, mặc định tắt); nguyệt (can tháng, chỉ dùng ở tầng tháng). Khi Hóa Kỵ gốc và Hóa Kỵ lưu cùng chiếu một cung (“sóng Kỵ”), tăng trọng số cảnh báo cho cung đó.

### SchoolConfig

| Khóa | Mặc định | Biến thể |
| --- | --- | --- |
| `tuHoa` | `NAM_PHAI_VN` (bảng trên) | `THIEN_LUONG`: Canh = Dương Lộc, Vũ Quyền, Đồng Khoa, Âm Kỵ; Nhâm Khoa = Thiên Phủ |
| `khoiViet` | `VN_PHO_THONG` (Canh Tân: Khôi Ngọ, Việt Dần) | `TOAN_THU` (“Giáp Mậu Canh ngưu dương”: Canh về Sửu/Mùi) |
| `hoaLinh` | `THAI_THU_LANG` (mục 5.6) | `DAC_LOC` (Tỵ Dậu Sửu: Hỏa Tuất, Linh Mão); `TRUNG_CHAU` (cả hai đếm thuận, không xét âm dương nam nữ) |
| `thienGiai` | `THUAN` | `NHAY_CUNG` |
| `luuHa` | `TOAN_THU` | `LASOTUVI` (Đinh Thìn, Canh Thân) |
| `leapMonth` | `SAME_MONTH` | `SPLIT_15` |
| `dayBoundary` | `DOI_NGAY_23H` | `GIU_NGAY` |
| `nguyetHan` | `TIEU_HAN_NGHICH_THANG_THUAN_GIO` | `TIEU_HAN_THUAN_THANG_THUAN_GIO`; `TIEU_HAN_LA_THANG_GIENG`; `DAU_QUAN_LUU_THAI_TUE` |
| `cuMonTable` | `THAI_THU_LANG` | `CAM_NANG_V2` |
| `daiHanTuHoa` | `OFF` | `ON` |
| `thanChuTy` | `LINH_TINH` | `HOA_TINH` |

Cài đặt → “Trường phái” có 3 preset: **Nam phái VN (khuyến nghị)**, **Theo cụ Thiên Lương**, **Toàn Thư / Trung Châu**; từng khóa chỉnh tay được. Mọi lá số và bản xuất PDF ghi rõ preset đã dùng. Golden test chạy trên preset mặc định; test biến thể chỉ kiểm các sao bị ảnh hưởng.

## 8. Hệ vận hạn đa tầng

Một kết luận vận hạn chỉ được đưa ra khi các tầng cùng chỉ một hướng: gốc (lá số + Phúc Đức) → đại hạn → lưu niên đại hạn + tiểu hạn → Lưu Thái Tuế, sao lưu, Tứ Hóa lưu → nguyệt → nhật → thời. Tầng dưới chỉ khuếch đại hoặc làm dịu tầng trên, không bao giờ lật ngược nó.

### 8.1 Đại hạn (10 năm)

Khởi ở cung Mệnh với tuổi âm bằng số Cục; đại hạn thứ i ở `Mệnh + i·dir`, phủ tuổi `[cục + 10i, cục + 10i + 9]`. Can chi đại hạn là can chi của cung đó. Đọc cung đại hạn cùng tam phương tứ chính như “Mệnh của 10 năm”; tùy chọn xoay 12 cung đại hạn (cung đại hạn = ĐH Mệnh, rồi ĐH Phụ Mẫu, ĐH Phúc Đức… theo thứ tự vai trò).

**Thiên thời theo ngũ hành tam hợp (cụ Thiên Lương):** so hành tam hợp của chi năm sinh với hành tam hợp của chi cung đại hạn (Thân Tý Thìn Thủy, Dần Ngọ Tuất Hỏa, Tỵ Dậu Sửu Kim, Hợi Mão Mùi Mộc).

| Quan hệ | Nhãn hiển thị | Điểm nền |
| --- | --- | --- |
| Đại hạn sinh tuổi | Đắc thiên thời, được hoàn cảnh nâng đỡ | +2 |
| Đồng hành | Hòa hợp, giữ vững thành quả | +1 |
| Tuổi khắc đại hạn | Vất vả, phải tự lực nhưng giành được | 0 |
| Tuổi sinh đại hạn | Hao tâm tổn sức, thành quả bị san sẻ | −1 |
| Đại hạn khắc tuổi | Thất thời, nên thủ hơn công | −2 |

### 8.2 Tiểu hạn (1 năm — diễn biến)

Cung khởi theo tam hợp chi năm sinh: Thân Tý Thìn → Tuất; Dần Ngọ Tuất → Thìn; Tỵ Dậu Sửu → Mùi; Hợi Mão Mùi → Sửu. Ghi chi năm sinh vào cung khởi, rồi Nam đi thuận, Nữ đi nghịch ghi lần lượt 12 chi. Tiểu hạn năm Y là cung mang chi của Y.

### 8.3 Lưu niên đại hạn (1 năm — kết quả)

Theo *Tử Vi Đẩu Số Tân Biên*: năm thứ 1 của đại hạn ở cung gốc, năm thứ 2 sang cung xung chiếu; Dương Nam, Âm Nữ lùi lại một cung rồi trở về xung chiếu, sau đó tiến thuận mỗi năm một cung; Âm Nam, Dương Nữ tiến một cung rồi trở về xung chiếu, sau đó lùi nghịch ([trích Tân Biên](https://lyso.vn/xem-tu-vi/xin-hoi-ve-cach-tinh-dai-han-va-tieu-han-t38768/)). Cẩm nang V2 mô tả bước này mơ hồ, nên dùng đúng công thức dưới:

```kotlin
fun luuNienDaiHan(goc: Int, namThu: Int /* 1..10 */, dir: Int): Int {
    val xung = (goc + 6).mod(12)
    return when (namThu) {
        1 -> goc
        2 -> xung
        3 -> (xung - dir).mod(12)          // Dương Nam/Âm Nữ lùi, Âm Nam/Dương Nữ tiến
        4 -> xung
        else -> (xung + (namThu - 4) * dir).mod(12)
    }
}
```

### 8.4 Lưu Thái Tuế, sao lưu, Lưu Mệnh

| Sao lưu (năm Y: can `cy`, chi `zy`) | Vị trí |
| --- | --- |
| Lưu Thái Tuế | `zy` — cung bị kích hoạt mạnh nhất trong năm |
| Lưu Tang Môn / Lưu Bạch Hổ | `zy + 2` / `zy + 8` |
| Lưu Thiên Khốc / Lưu Thiên Hư | `6 − zy` / `6 + zy` |
| Lưu Lộc Tồn, Lưu Kình, Lưu Đà | Bảng Lộc Tồn theo `cy`; Kình +1, Đà −1 |
| Lưu Thiên Mã / Lưu Đào Hoa | Theo tam hợp của `zy`, cùng bảng với sao gốc |
| Lưu Hồng Loan / Lưu Thiên Hỷ | `3 − zy` / `9 − zy` |
| Lưu Hỷ Thần | Lưu Lộc Tồn + 7·dir (sao thứ 8 vòng Bác Sĩ) |
| Lưu Long Đức / Lưu Nguyệt Đức | `zy + 7` / `5 + zy` |
| Lưu Tứ Hóa | Theo `cy`, gắn vào vị trí sao gốc tương ứng |

**Lưu Mệnh:** cung có Lưu Thái Tuế là Lưu Mệnh; 12 lưu cung xếp theo thứ tự vai trò như mục 5.1 và chồng lên cung gốc để đọc nhanh (Lưu Tài rơi vào cung gốc nào, sao gì…).

### 8.5 Nguyệt hạn, nhật hạn, thời hạn

- **Nguyệt hạn (mặc định):** từ cung tiểu hạn coi là tháng Giêng, đếm nghịch tới tháng sinh, coi đó là giờ Tý, đếm thuận tới giờ sinh → cung tháng Giêng của năm; tháng k = tháng Giêng + (k−1) ([Học viện Lý số](https://hocvienlyso.org/chuong-9-luu-nguyet-han.html)). Công thức: `thangGieng = tieuHan − (m−1) + h`. Biến thể Trung Châu lấy Đẩu Quân lưu từ Lưu Thái Tuế thay cho tiểu hạn ([giải thích](https://hocvienlyso.org/luu-nguyet-luu-nhat-luu-thoi.html)). Tháng nhuận dùng chung cung với tháng chính.
- **Nhật hạn:** từ cung tháng coi là mùng 1, đếm thuận tới ngày âm.
- **Thời hạn:** từ cung ngày coi là giờ Tý, đếm thuận tới giờ cần xem.

**Vai trò từng tầng khi luận năm:** tiểu hạn = chuyện gì diễn ra; lưu niên đại hạn = kết cục; Lưu Thái Tuế = lĩnh vực bị đánh thức; đại hạn = nền móng 10 năm; Phúc Đức = lực cứu giải. Ví dụ tiểu hạn nhiều sát tinh nhưng lưu niên đại hạn sáng sủa → “khó lúc đầu, kết quả tốt”.

## 9. Động cơ luận giải

Luận giải là pipeline 7 bước biến lá số + vận hạn thành danh sách `Finding` có trọng số, rồi ghép câu từ knowledge base; không bước nào dùng thông tin nằm ngoài lá số.

1. **Dựng ngữ cảnh cung.** Với mỗi cung p: tọa thủ (p), xung chiếu (p+6), tam hợp (p+4, p−4), giáp (p±1), nhị hợp `(1 − p) mod 12` (Tý–Sửu, Dần–Hợi, Mão–Tuất, Thìn–Dậu, Tỵ–Thân, Ngọ–Mùi). Trọng số quan hệ: tọa thủ 1,0; xung chiếu 0,8; mỗi cung tam hợp 0,6; giáp 0,4 (chỉ với cặp có nghĩa: Tả Hữu, Xương Khúc, Khôi Việt, Nhật Nguyệt, Không Kiếp, Kình Đà, Hỏa Linh, Quyền Lộc…); nhị hợp 0,3.
2. **Vô Chính Diệu.** Cung không có chính tinh mượn chính tinh cung xung chiếu (đánh dấu `borrowed`, hệ số 0,7), rồi chạy bộ luật Vô Chính Diệu ở mục 10.4.
3. **Tuần, Triệt.** Sao tốt miếu/vượng/đắc bị Tuần hoặc Triệt tọa thủ: lực tốt ×0,5. Sao hãm, sát tinh, bại tinh bị Tuần/Triệt: lực xấu ×0,5 và xét “phản vi” (mục 10.3). Hệ số theo tuổi: Triệt 1,0 trước 30 tuổi, 0,5 sau 30; Tuần 0,5 trước 30, 1,0 sau 30. Tuần/Triệt ở cung xung chiếu chỉ chặn phần chiếu sang (×0,5 trên phần đó).
4. **Dò cách cục** bằng rule DSL (dưới). Luật cụ thể hơn thắng luật chung: khi một cách cục kích hoạt, nó `suppress` các câu luận đơn sao cùng cung để không ra hai câu ngược nhau.
5. **Chấm điểm cung:** `score = clamp(50 + 10 × Σ(polarity × weight × brightness × relation × tuanTriet × age), 5, 95)`, tách điểm riêng theo chủ đề.
6. **Tổng hợp theo phạm vi.** Năm: gốc 0,10, đại hạn 0,30, lưu niên đại hạn 0,25, tiểu hạn 0,20, Lưu Thái Tuế + sao lưu + Tứ Hóa lưu 0,15. Tháng: năm 0,50, cung nguyệt hạn 0,35, Tứ Hóa tháng 0,15. Ngày: tháng 0,50, cung nhật hạn 0,35, can chi ngày xung/hợp tuổi 0,15.
7. **Ghép câu.** `textKey` → template trong KB với slot `{cung} {sao} {doSang} {tuoi} {nam}`; mỗi câu giữ `evidence`. Hai finding ngược chiều cùng chủ đề → viết thành một câu cân nhắc (“có cả … lẫn …, nghiêng về … vì …”), không in hai câu trái ngược.

**Quy tắc “đồng thanh tương ứng”** (cẩm nang, quy tắc kết luận 2): một sự kiện cụ thể (cưới, đổi việc, hao tài, kiện tụng, di chuyển xa) chỉ được nói “khả năng cao” khi ≥ 3 tầng cùng chỉ; 2 tầng → “có thể”; 1 tầng → “dấu hiệu, nên lưu ý”. Luôn thẩm định Phúc Đức và đại hạn trước khi kết luận cho một năm (quy tắc kết luận 1).

**Chủ đề → cung đọc chính**

| Chủ đề | Cung chính | Cung phụ | Sao chỉ báo |
| --- | --- | --- | --- |
| Tính cách | Mệnh | Thân, Phúc Đức, Thiên Di | Chính tinh thủ Mệnh, Mệnh chủ |
| Sự nghiệp | Quan Lộc | Mệnh, Thiên Di, Nô Bộc | Hóa Quyền, Khôi Việt, Tướng Ấn, Thiên Mã |
| Tài chính | Tài Bạch | Điền Trạch, Phúc Đức, Mệnh | Lộc Tồn, Hóa Lộc, Vũ Khúc, Song Hao, Không Kiếp |
| Tình duyên, hôn nhân | Phu Thê | Mệnh, Thân, Phúc Đức, Nô Bộc | Đào Hồng Hỷ, Cô Quả, Đẩu Quân, Long Phượng |
| Gia đạo | Phụ Mẫu, Huynh Đệ, Tử Tức | Phúc Đức, Điền Trạch | Nhật Nguyệt (cha mẹ), Thai, Tang Hổ |
| Sức khỏe (lưu ý) | Tật Ách | Mệnh, Phúc Đức | Kình Đà, Hỏa Linh, Thiên Hình, Hóa Kỵ, Bạch Hổ |
| Di chuyển, xuất ngoại | Thiên Di | Mệnh, Quan Lộc | Thiên Mã, Long Trì Phượng Các |
| Nhà đất | Điền Trạch | Tài Bạch, Tử Tức | Thiên Phủ, Thái Âm, Lộc, Song Hao |
| Pháp lý, thị phi | Quan Lộc, Thiên Di | Nô Bộc | Quan Phù, Thiên Hình, Cự Môn, Hóa Kỵ |
| Phúc đức, tâm linh | Phúc Đức | Mệnh | Thiên Lương, Thiên Đồng, Quang Quý, Tứ Đức |

**Rule DSL** (lưu trong `:data:kb/rules/*.json`):

```json
{
  "id": "CC_THACH_TRUNG_AN_NGOC",
  "name": "Thạch Trung Ẩn Ngọc",
  "type": "CAT_CACH",
  "when": {
    "palace": "MENH",
    "allOf": [
      { "star": "CU_MON", "in": "self", "chiIn": ["TY", "NGO"] },
      { "anyOf": [ { "star": "HOA_LOC", "in": "tamPhuongTuChinh" },
                   { "star": "HOA_QUYEN", "in": "tamPhuongTuChinh" },
                   { "star": "HOA_KHOA", "in": "tamPhuongTuChinh" } ] }
    ],
    "noneOf": [ { "star": "KINH_DUONG", "in": "self" }, { "star": "HOA_KY", "in": "self" } ]
  },
  "effect": { "topics": ["SU_NGHIEP", "TAI_CHINH"], "polarity": 2, "weight": 3.0,
              "suppress": ["STAR_CU_MON_*@MENH"] },
  "textKey": "cc.thach_trung_an_ngoc",
  "adviceKeys": ["adv.phat_huy_ngon_ngu", "adv.tich_luy_truoc_khi_bung"],
  "source": "Cẩm nang V2 – P.III.10"
}
```

Predicate hỗ trợ: `star` trong phạm vi `self | xungChieu | tamHop | tamPhuongTuChinh | giap | nhiHop`; `brightnessIn`; `chiIn`; `palace`; `gender`; `yearCanIn`; `yearChiIn`; `amDuongNamNu`; `hasTuan`/`hasTriet`; `isVCD`; `countOf(set) >= n`; `layer` (`goc | daiHan | tieuHan | luuNienDaiHan | luuThaiTue`); `luuStar`; `ageRange`. Độ ưu tiên = số predicate; rule ưu tiên cao hơn thắng khi cùng sao, cùng chủ đề.

**Khối lượng KB tối thiểu cho v1:** (a) 14 chính tinh × 5 độ sáng × 12 vai trò cung = 840 đoạn ngắn; (b) 24 bộ đồng cung (Tử Phủ, Tử Tướng, Tử Sát, Tử Phá, Tử Tham, Cơ Cự, Cơ Âm, Cơ Lương, Nhật Nguyệt, Nhật Cự, Nhật Lương, Vũ Phủ, Vũ Tướng, Vũ Sát, Vũ Phá, Vũ Tham, Đồng Âm, Đồng Cự, Đồng Lương, Liêm Phủ, Liêm Tướng, Liêm Sát, Liêm Phá, Liêm Tham) × 12 vai trò; (c) khoảng 60 phụ tinh, mỗi sao có ý nghĩa chung + ý nghĩa theo cung + đắc/hãm; (d) ≥ 120 cách cục (mục 10). Phần (a)–(c) được phép sinh nháp bằng script từ cẩm nang + mục 10, mỗi mục có trường `source` và `reviewed: false` cho đến khi người dùng duyệt; app hiển thị nhãn nhỏ “bản nháp” với mục chưa duyệt.

## 10. Thư viện cách cục

Bốn nhóm dưới đây là bộ luật khởi điểm bắt buộc (khung sườn để sinh ≥ 120 rule); “ĐK” viết theo ngôn ngữ predicate của mục 9, “TPTC” = tam phương tứ chính của cung Mệnh trừ khi ghi khác. Cột “Tin cậy”: A = cách kinh điển, các sách thống nhất; B = phổ biến ở Nam phái VN; C = quan điểm riêng, chỉ hiện ở chế độ chuyên sâu.

### 10.1 Cát cách

| Tên | ĐK | Hướng luận | Tin cậy |
| --- | --- | --- | --- |
| Tử Phủ đồng cung | Tử Vi + Thiên Phủ tại Dần/Thân thủ Mệnh | Nắm cả quyền lẫn kho; hợp lãnh đạo, làm chủ. Cần Tả Hữu/Lộc mới trọn cách | A |
| Quân thần khánh hội | Tử Vi thủ Mệnh, Tả Phù + Hữu Bật trong TPTC (thêm Xương Khúc/Khôi Việt càng mạnh) | Được phò tá, dễ lên vị trí quản trị | A |
| Cực hướng ly minh | Tử Vi tại Ngọ thủ Mệnh, không Kình Đà Hỏa Linh Không Kiếp tọa thủ | Uy nghi, danh vọng rõ | A |
| Phủ Tướng triều viên | Thiên Phủ và Thiên Tướng cùng trong TPTC, không Không Kiếp trong TPTC | Ổn định, tích lũy, được tin cậy | A |
| Cơ Nguyệt Đồng Lương | Đủ 4 sao Thiên Cơ, Thái Âm, Thiên Đồng, Thiên Lương trong TPTC | Hợp hành chính, giáo dục, y tế, nghiên cứu; ổn định hơn bứt phá | A |
| Sát Phá Tham | Thất Sát/Phá Quân/Tham Lang thủ Mệnh (ba sao luôn tam hợp nhau) | Động, khai phá, thăng trầm; có sát tinh đắc địa thì hợp võ nghiệp, kinh doanh mạo hiểm | A |
| Nhật Nguyệt tịnh minh | Thái Dương và Thái Âm cùng trong TPTC, cả hai ≥ Đắc | Thông minh, danh lợi cân bằng | A |
| Minh Châu xuất hải | Mệnh tại Mùi vô chính diệu, Thái Dương tại Mão, Thái Âm tại Hợi | Phát sáng nhờ học vấn, danh tiếng | A |
| Nhật xuất Phù Tang / Nhật lệ trung thiên | Thái Dương thủ Mệnh tại Mão / tại Ngọ | Quang minh, hướng ngoại, danh lớn hơn lợi | A |
| Nguyệt lãng thiên môn | Thái Âm thủ Mệnh tại Hợi | Tinh tế, phú túc, thuận người sinh đêm | A |
| Nhật chiếu lôi môn | Thái Dương + Thiên Lương đồng cung tại Mão thủ Mệnh | Danh tài rực rỡ | A |
| Thạch trung ẩn ngọc | Cự Môn tại Tý/Ngọ thủ Mệnh + Hóa Lộc/Quyền/Khoa trong TPTC, không Kình Dương hoặc Hóa Kỵ tọa thủ | Tài năng ẩn, phát muộn mà bền | A |
| Cự Nhật đồng cung | Cự Môn + Thái Dương tại Dần thủ Mệnh (tại Thân yếu hơn) | Danh tiếng nhờ ngôn luận, ngoại giao | A |
| Thất Sát triều/ngưỡng đẩu | Thất Sát thủ Mệnh tại Tý, Ngọ, Dần, Thân, không Hóa Kỵ + Không Kiếp | Uy dũng, độc lập, lập nghiệp | A |
| Anh tinh nhập miếu | Phá Quân thủ Mệnh tại Tý/Ngọ; đẹp nhất tuổi Giáp (Phá Quân hóa Quyền), Quý (hóa Lộc) | Tiên phong, dám đổi mới | A |
| Hùng tú triều viên | Liêm Trinh tại Dần/Thân thủ Mệnh, không sát tinh tọa thủ | Liêm chính, có uy tín, hợp ngành luật, kỷ cương | B |
| Tham Hỏa / Tham Linh | Tham Lang + Hỏa Tinh hoặc Linh Tinh đồng cung, đẹp nhất tại Thìn Tuất Sửu Mùi | Bạo phát; bắt buộc kèm lưu ý “bạo phát dễ bạo tàn, giữ phần” | A |
| Vũ Tham đồng hành | Vũ Khúc + Tham Lang đồng cung tại Sửu/Mùi | Tiền bần hậu phú, phát sau khoảng 30 tuổi | A |
| Đồng Lương Dần Thân | Thiên Đồng + Thiên Lương đồng cung tại Dần/Thân | Phúc thọ, gặp nạn có cứu | A |
| Cơ Cự đồng lâm | Thiên Cơ + Cự Môn đồng cung tại Mão/Dậu | Kinh doanh, hoạt ngôn; có Hóa Lộc/Quyền thì giàu; đa nghi | A |
| Văn Quế Văn Hoa | Văn Xương + Văn Khúc đồng cung tại Sửu/Mùi thủ Mệnh | Văn chương, học vấn | A |
| Lộc Mã giao trì | Lộc Tồn hoặc Hóa Lộc cùng Thiên Mã đồng cung/hội chiếu tại Mệnh, Tài, Quan, Di | Tiền đến từ di chuyển, giao thương | A |
| Song Lộc triều viên | Lộc Tồn và Hóa Lộc cùng trong TPTC | Tài lộc dồi dào | A |
| Tam Hóa liên châu | Hóa Lộc, Hóa Quyền, Hóa Khoa đều trong TPTC | Danh, quyền, lộc đủ cả | A |
| Tài Ấm giáp Ấn | Thiên Tướng (luôn bị Cự Môn −1 và Thiên Lương +1 kẹp) + cung Cự Môn có Hóa Lộc hoặc Lộc Tồn | Thiên Tướng hưởng trọn quyền và tiền | A |
| Giáp cát | Hai cung kề Mệnh chứa đủ cặp Tả–Hữu, Xương–Khúc, Khôi–Việt, Nhật–Nguyệt hoặc Quyền–Lộc | Được nâng đỡ từ môi trường xung quanh | A |
| Tọa quý hướng quý | Khôi, Việt một sao ở Mệnh, một sao ở cung xung chiếu hoặc tam hợp | Quý nhân, thi cử thuận | A |

### 10.2 Hung cách

| Tên | ĐK | Hướng luận (đã diễn đạt mềm) | Tin cậy |
| --- | --- | --- | --- |
| Linh Xương Đà Vũ | Linh Tinh, Văn Xương, Đà La, Vũ Khúc cùng hội tại cung hạn (nặng nhất ở Thìn, Tuất) | Giai đoạn áp lực tinh thần rất lớn; nên giảm việc, giữ kết nối với người thân, chủ động tìm hỗ trợ chuyên môn. Không bao giờ viết câu chữ gốc “hạn chí đầu hà” | A |
| Lộ thượng mai thi | Liêm Trinh + Thất Sát đồng cung tại Sửu/Mùi | Cẩn trọng giao thông và đi xa, nhất là năm hạn có sát tinh lưu | A |
| Hình Kỵ giáp Ấn | Thiên Tướng bị kẹp bởi Cự Môn hóa Kỵ và cung Thiên Lương có Kình Dương | Dễ vướng quy định, giấy tờ, trách nhiệm pháp lý; thận trọng ký kết | A |
| Cự Kình | Cự Môn + Kình Dương đồng cung | Hay va chạm lời nói, công việc dễ bế tắc nếu nóng vội | A |
| Phiếm thủy đào hoa | Tham Lang tại Tý/Hợi + Kình Dương hoặc Đà La hoặc Hóa Kỵ đồng cung | Ham vui, dễ rối tình cảm, tài chính cá nhân | A |
| Liêm Tham Tỵ Hợi | Liêm Trinh + Tham Lang đồng cung tại Tỵ/Hợi (thêm Hóa Kỵ càng nặng) | Dễ vướng thị phi, pháp lý do tiền hoặc tình; tránh vùng xám | A |
| Vũ Phá Tỵ Hợi | Vũ Khúc + Phá Quân đồng cung tại Tỵ/Hợi | Tiền bạc tụ tán mạnh, dễ liều | A |
| Không Kiếp giáp Mệnh | Địa Không và Địa Kiếp ở hai cung kề Mệnh | Môi trường hay gây hao hụt, kế hoạch dễ đứt gãy | A |
| Tứ sát hội Mệnh | ≥ 3 trong Kình, Đà, Hỏa, Linh trong TPTC, không có sao giải | Nhiều trở ngại, cần bền bỉ | A |
| Thiên Cơ ngộ Không Kiếp | Thiên Cơ cùng Địa Không hoặc Địa Kiếp tọa thủ | Mưu tính quá đà, dễ hỏng việc vì tính toán | B |
| Lộc phùng lưỡng sát | Lộc Tồn hoặc Hóa Lộc cùng Địa Không + Địa Kiếp hội chiếu | Tiền đến rồi đi, khó giữ | A |
| Mã lạc không vong / Chiết túc mã | Thiên Mã gặp Tuần/Triệt / Thiên Mã đồng cung Đà La | Di chuyển, đổi việc hay trắc trở, chậm | B |
| Nhật Nguyệt phản bối | Thái Dương và Thái Âm cùng hãm (vd. Dương tại Tuất, Âm tại Thìn) hội chiếu Mệnh | Vất vả, danh không tương xứng công sức — trừ khi có điều kiện phản vi ở 10.3 | A |
| Kho thủng / kho lộ | Thiên Phủ gặp Địa Không/Địa Kiếp (thủng) hoặc Tuần/Triệt (lộ) | Tích lũy khó bền; cần kỷ luật tài chính, tránh đầu cơ | B |
| Cô quân / Hôn quân | Tử Vi thủ Mệnh không có Tả Hữu trong TPTC; thêm ≥ 2 sát tinh → hôn quân | Thiếu người phò tá, dễ cô đơn, cố chấp | B |
| Song Kỵ | Hóa Kỵ gốc và Hóa Kỵ lưu cùng một cung trong năm | Vướng mắc kéo dài ở lĩnh vực của cung đó | B |
| Cục khắc Mệnh | Hành Cục khắc hành bản mệnh | Hoàn cảnh ít nâng đỡ, phải tự lực (cẩm nang P.I.4) | B |

### 10.3 Phản vi kỳ cách (hãm hoặc hung hóa tốt)

Chạy nhóm này **trước** khi kết luận một sao hãm là xấu; rule khớp sẽ đảo hoặc giảm polarity của câu đơn sao.

| Tên | ĐK | Hướng luận | Tin cậy |
| --- | --- | --- | --- |
| Cự Môn Thìn Tuất gặp tuổi Tân | Cự Môn tại Thìn/Tuất (hãm) thủ Mệnh, năm sinh can Tân (Cự Môn hóa Lộc) | “Hãm địa mà hóa cát”: khẩu tài thành lợi thế, phát đạt | A |
| Thiên Đồng Tuất gặp tuổi Đinh | Thiên Đồng tại Tuất thủ Mệnh, can Đinh (Thiên Đồng hóa Quyền) | Phản bối hóa cát, có thể đại quý | A |
| Mã đầu đới kiếm | Kình Dương tại Ngọ thủ Mệnh (chỉ xảy ra với can Bính, Mậu), có Thiên Đồng hoặc Tham Lang | Uy trấn một phương, hợp võ nghiệp, quản lý cứng; thiếu điều kiện thì lại là hung | A |
| Nhật/Nguyệt hãm gặp Tuần Triệt | Thái Dương hãm (Thân→Tý) hoặc Thái Âm hãm (Dần→Ngọ) tọa thủ cùng Tuần hoặc Triệt | “Phản vi minh” theo Nam phái: giảm mạnh tính hãm. Ngược lại, Nhật Nguyệt sáng gặp Tuần Triệt bị giảm sáng | B |
| Sao hãm được Hóa Lộc/Quyền | Chính tinh hãm thủ Mệnh nhưng chính nó hóa Lộc hoặc Quyền (vd. Liêm Trinh hãm tuổi Giáp, Phá Quân hãm tuổi Giáp/Quý, Vũ Khúc Tỵ Hợi tuổi Kỷ/Canh) | Giảm hung một bậc; câu luận “vất vả nhưng có lối ra” | B |
| Sát tinh đắc địa phò Sát Phá Tham | Kình/Đà tại Thìn Tuất Sửu Mùi hoặc Hỏa/Linh tại Dần→Ngọ, cùng Thất Sát/Phá Quân/Tham Lang miếu vượng | Võ cách, phẫu thuật, an ninh, kinh doanh cạnh tranh — sát thành quyền | B |
| Không Kiếp đắc địa | Địa Không/Địa Kiếp tại Dần Thân Tỵ Hợi, Mệnh có chính tinh miếu vượng | “Phát như lôi” nhưng dễ tàn nhanh: luôn ghép khuyến nghị chốt lời, phân tán rủi ro | B |
| Hóa Kỵ ở Tứ Mộ | Hóa Kỵ tại Thìn Tuất Sửu Mùi | Giảm hung, thành sự cẩn trọng, chuyên sâu | B |
| Thiên Hình đắc địa | Thiên Hình tại Dần Mão Thân Dậu + chính tinh võ hoặc Thiên Lương | Quyền uy pháp luật, y khoa, kỷ luật | B |
| Tuần Triệt giải hung | Tuần/Triệt tọa thủ cung có sát tinh hoặc chính tinh hãm | Giảm hung của cung đó (hệ số theo tuổi ở mục 9) | B |

### 10.4 Thế sao khó — luật xử lý riêng

1. **Mệnh Vô Chính Diệu.** Mượn chính tinh cung xung chiếu (luôn có, vì hai cung đối nhau không thể cùng trống). Gặp “tam không” (≥ 3 trong Tuần, Triệt, Thiên Không, Địa Không tại hoặc chiếu Mệnh) → cách “vô chính diệu đắc tam không”, phú quý bất ngờ (B). Nhật Nguyệt sáng chiếu → tốt. Sát tinh tọa thủ → gốc yếu, dễ bị ngoại cảnh chi phối, đi xa lập nghiệp thuận hơn.
2. **Tuần và Triệt cùng một cung** (xảy ra ở vài tuổi, vd. Quý Hợi: cả hai tại Tý–Sửu): lực chặn cực mạnh; cung tốt thành bế tắc, cung xấu lại được giải mạnh.
3. **Lộc Tồn thủ Mệnh:** luôn bị Kình Đà giáp → biết giữ của nhưng dễ cô, dè dặt; thêm Hóa Kỵ hoặc Không Kiếp đồng cung → lộc bị phá.
4. **Hóa Lộc + Hóa Kỵ cùng cung:** được rồi vướng, tiền đi kèm rắc rối.
5. **Thiên Tướng:** luôn xét giáp cung trước khi xét sao trong cung (Tài Ấm vs Hình Kỵ).
6. **Âm Dương thuận/nghịch lý:** người tuổi Dương có Mệnh ở cung Dương (Tý, Dần, Thìn, Ngọ, Thân, Tuất), tuổi Âm có Mệnh ở cung Âm → thuận lý (+); ngược lại nghịch lý (−, nhẹ). Hiển thị trên thẻ tổng quan.
7. **Mệnh–Thân:** Thân Mệnh đồng cung → luận Mệnh nặng hơn cả đời; Thân cư cung khác → từ 30 tuổi chuyển trọng số sang cung Thân (Mệnh 0,6 → 0,4; Thân 0,4 → 0,6) và dùng ý nghĩa “Thân cư…” của cẩm nang P.I.3.
8. **Mệnh đẹp, Phúc xấu (hoặc ngược lại):** Phúc Đức quyết định độ bền. Mệnh tốt + Phúc xấu → “có lúc lên nhanh nhưng khó giữ”; Mệnh yếu + Phúc tốt → “chậm nhưng được che chở”.

### 10.5 Chuyên đề từ cẩm nang — cách đưa vào app

- **Hôn nhân** (P.V.1): giữ các dấu hiệu (Hoa Cái–Thiên Mã–Tuần/Triệt tại Phu Thê; Kình Mệnh – Đà Phu Thê + Tang/Cô/Quả; Dương Lương Mão Dậu với Đồng Cự hóa Kỵ; Thiên Tướng với Liêm Tham hóa Kỵ; Sát Phá hãm + Kỵ tại Phu Thê). Mỗi dấu hiệu chỉ nâng mức “nguy cơ trắc trở”; câu chữ thay “chắc chắn đổ vỡ / 90%” bằng “cần vun đắp nhiều hơn, cưới muộn thuận hơn”.
- **“Con dị bào”** (P.V.1.2): không hiển thị nguyên văn. Chỉ ở chế độ chuyên sâu, với câu trung tính “đường con cái có thể không đơn tuyến (con nuôi, con riêng hoặc con của hôn nhân sau)” — luận điểm này có thể gây tổn thương quan hệ gia đình.
- **Hạn hỷ cưới hỏi** (P.V.2): 5 sao lưu tình duyên (Lưu Đào Hoa, Hồng Loan, Hỷ Thần, Long Đức, Nguyệt Đức) vào Mệnh/Thân/Nô + tiểu hạn có Thanh Long/Long Trì/Phượng Các/Thiên Hỷ + lưu niên đại hạn vào Mệnh, Thân, Phu, Quan, Tài → “năm thuận cho chuyện cưới hỏi”. Đủ cả ba = “khả năng cao”, không bao giờ “100%”.
- **Năm Lưu Thái Tuế trùng Cô Thần/Quả Tú gốc:** “theo quan niệm Tử Vi, năm này không thuận cưới hỏi; nếu đã định, hãy dành nhiều thời gian cho giao tiếp vợ chồng”. Bỏ mệnh đề “100% ly hôn”.
- **Sinh con** (P.V.3): giữ điều kiện thuận lợi; dấu hiệu xấu chỉ diễn đạt “nên chăm sóc thai kỳ kỹ, khám định kỳ theo hướng dẫn của bác sĩ”. **Không đưa dự đoán giới tính thai nhi vào app**: quy định về dân số của Việt Nam cấm phổ biến các cách lựa chọn giới tính thai nhi, và tính năng này dễ bị dùng như vậy.
- **Truyền cung, truyền tinh** (P.V.4): tính năng so hai lá số trong hồ sơ gia đình; truyền bộ sao hung chỉ nói “hai người dễ khuếch đại điểm yếu của nhau, cần quy ước rõ ràng”.

## 11. Khuyến nghị nên/không nên & quy tắc diễn đạt

Mỗi bản luận kết thúc bằng 3–5 việc NÊN và 3–5 việc KHÔNG NÊN cho đúng khung thời gian đang xem, lấy từ `adviceKeys` của các finding mạnh nhất; mọi câu là hành động cụ thể, có điều kiện, không gây sợ hãi.

**Cách sinh khuyến nghị**

1. Mục KB dạng `{key, type: NEN | KHONG_NEN | LUU_Y, topic, text, severity}`.
2. Xếp hạng theo |polarity × weight|, khử trùng theo chủ đề, tối đa 5 mỗi loại.
3. Gắn thời điểm: từ điểm nguyệt hạn, lấy 2 tháng âm tốt nhất cho việc NÊN và 2 tháng yếu nhất cho LƯU Ý.
4. Thế của giai đoạn theo điểm tổng: ≥ 70 **Tấn** (chủ động mở rộng); 50–69 **Giữ** (củng cố); 35–49 **Thủ** (tránh rủi ro lớn); < 35 **Lùi** (hoãn quyết định lớn nếu được).

| Tình huống trên lá số | Nên | Không nên |
| --- | --- | --- |
| Lưu Thái Tuế + tiểu hạn vào Quan Lộc có Hóa Quyền, Khôi Việt | Nhận trách nhiệm mới, đề xuất thăng tiến, ra mắt dự án trong tháng điểm cao | Đổi việc vì cảm xúc trong tháng có Kình Đà lưu |
| Tài Bạch có Song Hao + Không Kiếp lưu | Lập quỹ dự phòng, chia nhỏ khoản đầu tư | Cho vay lớn, bảo lãnh, đầu cơ dùng đòn bẩy |
| Tật Ách có Kình Dương/Thiên Hình + Lưu Bạch Hổ | Khám sức khỏe định kỳ, cẩn trọng khi lái xe và vận động mạnh | Bỏ qua dấu hiệu bất thường kéo dài |
| Phu Thê có Hồng Loan/Thiên Hỷ lưu + Long Trì, Phượng Các | Chủ động gặp gỡ, bàn chuyện cưới hỏi | Quyết định lớn khi đang giận |
| Quan Phù, Thiên Hình, Hóa Kỵ ở Quan Lộc/Thiên Di | Đọc kỹ hợp đồng, lưu chứng từ, hỏi luật sư khi cần | Ký giấy tờ chưa đọc kỹ, tham gia việc mập mờ |
| Thiên Di có Thiên Mã + Lộc | Đi xa, công tác, mở thị trường mới | — |

**Quy tắc diễn đạt (bắt buộc, kiểm bằng test)**

1. Nói xác suất, không nói định mệnh: dùng “có xu hướng”, “dễ”, “thuận cho”, “nên lưu ý”. Cấm từ: “chắc chắn”, “100%”, “không thể tránh”, “định mệnh” (test quét toàn bộ KB).
2. Không phán tử vong, tuổi thọ, bệnh cụ thể, sảy thai, ly hôn, tù tội, tai nạn chết người. Chuyển thành “giai đoạn cần chăm sóc sức khỏe/an toàn kỹ hơn”, “dễ va chạm pháp lý — cẩn trọng giấy tờ”.
3. Sức khỏe chỉ “chú ý” + khuyên gặp bác sĩ; không gợi ý thuốc, bài thuốc.
4. Tài chính chỉ nêu khung rủi ro; không khuyên mua/bán tài sản cụ thể.
5. Khi gặp Linh Xương Đà Vũ hoặc ≥ 3 hung tinh nặng ở Mệnh/Phúc trong năm hạn: thêm câu khuyến khích giữ kết nối với người thân và tìm chuyên gia tâm lý; tuyệt đối không nhắc tới tự hại.
6. Không đổ lỗi người khác (“vợ khắc chồng”, “con khắc cha”); diễn đạt thành tương tác hai chiều.
7. Không quảng bá dịch vụ giải hạn, cúng sao có thu phí (hoạt động mê tín thu lợi có thể bị xử phạt theo NĐ 38/2021 — [Luật Minh Khuê](https://luatminhkhue.vn/tu-vi-la-gi.aspx)).
8. Mỗi bản luận có một dòng cuối “Đức năng thắng số”: Tử Vi là bản đồ thời tiết, lựa chọn của bạn quyết định kết quả.

| Không viết | Viết |
| --- | --- |
| Năm nay chắc chắn ly hôn. | Cung Phu Thê năm nay chịu áp lực (Hóa Kỵ lưu, Cô Thần). Dành thời gian lắng nghe; tránh quyết định lớn về hôn nhân khi đang căng thẳng. |
| Tháng 7 gặp tai nạn. | Tháng 7 âm có Lưu Kình Dương chiếu Tật Ách: đi lại và vận động nên cẩn trọng hơn thường lệ. |
| Bạn sẽ mất nhà. | Điền Trạch có Song Hao lưu: chưa phải lúc mua bán bất động sản lớn nếu chưa chuẩn bị kỹ dòng tiền. |

**Lời ngỏ onboarding (dùng nguyên văn):** “Tử Vi Đẩu Số là di sản văn hóa – triết học phương Đông. Nội dung trong ứng dụng mang tính tham khảo và chiêm nghiệm, không thay thế tư vấn y tế, pháp lý hay tài chính. Bạn luôn là người quyết định con đường của mình.”

## 12. Trợ lý AI trên máy & câu hỏi thường gặp

Người xem hỏi đáp như trò chuyện trực tiếp với một trợ lý chạy ngay trên điện thoại; trợ lý chỉ diễn đạt những gì engine đã tính, không tự an sao, không tự luận. Mọi câu hỏi được lưu trên máy làm dữ liệu câu hỏi thường gặp; nếu người xem đồng ý, bản ẩn danh được gửi lên Firebase để chủ app duyệt và bổ sung luận giải thực tế. v1 không gọi AI đám mây.

### 12.1 Model và runtime

| Hạng mục | Lựa chọn |
| --- | --- |
| Model mặc định | Gemma 4 E2B (Apache 2.0), file `.litertlm` khoảng 2,6 GB |
| Runtime | LiteRT-LM cho Android (Kotlin), ưu tiên GPU, tự lùi về CPU |
| Model dự phòng | Qwen3.5-2B hoặc 4B (Apache 2.0), GGUF Q4 chạy qua llama.cpp |
| Phân phối model | Không đóng kèm APK; tải về khi người dùng bật “Trợ lý AI”, kiểm SHA-256, xóa được trong Cài đặt |
| Chốt model | Chạy bộ 40 câu hỏi mẫu trên Galaxy S22 Ultra, chấm tiếng Việt, đúng dữ kiện, tốc độ; ghi kết quả vào `docs/DECISIONS.md` |

Model và runtime là cấu hình (`AssistantModelConfig`), đổi model không sửa code. Đo thời gian ra token đầu tiên và token/giây trên S22 Ultra, ghi vào báo cáo giai đoạn. Máy không đủ RAM hoặc chưa tải model → ẩn khung chat, vẫn đủ luận giải rule-based.

### 12.2 Luồng trả lời một câu hỏi

1. Phân loại câu hỏi ra chủ đề (bảng chủ đề mục 9) và phạm vi thời gian (cả đời, đại vận, năm, tháng, ngày); model trả JSON, lỗi thì dùng bộ từ khóa.
2. Câu đã hỏi với cùng lá số, cùng kỳ và đã có câu trả lời → trả lại ngay từ kho cục bộ (12.5).
3. Ghép ngữ cảnh tối đa 2.000 token: findings và advice của đúng chủ đề, phạm vi (từ engine luận giải); đoạn KB về các sao, cung, cách cục liên quan; câu trả lời mẫu đã duyệt trong `:data:kb/faq`.
4. Model viết câu trả lời theo system prompt 12.4, chế độ `HOI_DAP`.
5. Kiểm tra đầu ra (12.3). Đạt → hiển thị kèm nút “Vì sao?” và 👍/👎; trượt → hiển thị đoạn rule-based của chủ đề đó.
6. Câu hỏi thể hiện khủng hoảng tinh thần hoặc ý định tự hại → không gọi model luận số; hiển thị lời động viên liên hệ người thân, chuyên gia và đường dây hỗ trợ (cấu hình trong KB).

### 12.3 Kiểm tra đầu ra (bắt buộc, có test)

- Parse được JSON đúng schema của chế độ.
- Mọi `ruleId` có trong ngữ cảnh; mọi tên sao, cung, năm, tháng nhắc tới có trong lá số hoặc ngữ cảnh.
- Quét từ cấm mục 11; độ dài trong giới hạn 12.4.
- Sai bất kỳ điều nào → bản rule-based, ghi log cục bộ (không gửi đi) để cải thiện prompt.

### 12.4 System prompt

Đặt trong `:ai/src/main/assets/system_prompt_vi.md`. Chế độ `VIET_LUAN` thay cho lớp viết lại bài luận của bản cũ.

```markdown
Bạn là trợ lý Tử Vi Đẩu Số trong ứng dụng “Tử Vi Tự Xem”, chạy ngay trên điện thoại người dùng.
Bạn KHÔNG an sao và KHÔNG tự luận thêm. Mọi dữ kiện nằm trong <chart_json>, <limits_json>,
<findings_json>, <advice_json>, <kb_snippets>, <faq_examples> do ứng dụng tính sẵn.

CHẾ ĐỘ <mode>:
- HOI_DAP: trả lời câu hỏi <question> trong phạm vi <scope> (CA_DOI | DAI_VAN | NAM | THANG | NGAY).
- VIET_LUAN: viết các finding thành bài luận mạch lạc cho phạm vi <scope>.

QUY TẮC
1. Chỉ dùng sao, cung, cách cục, năm, tháng có trong dữ kiện. Không đủ dữ kiện thì nói rõ
   “chưa đủ dữ kiện để trả lời” và gợi ý câu hỏi gần nhất trả lời được.
2. Giữ nguyên polarity và mức độ của từng finding; không thêm finding mới;
   không bỏ finding có weight ≥ 2 liên quan tới câu hỏi.
3. Hai finding ngược chiều → một câu cân nhắc, nêu vì sao nghiêng về bên nào.
4. Nói xác suất, không nói định mệnh. Cấm: “chắc chắn”, “100%”, “không thể tránh”, phán tử vong,
   tuổi thọ, bệnh cụ thể, sảy thai, ly hôn, tù tội, tai nạn chết người. Sức khỏe chỉ “nên lưu ý” + “khám bác sĩ”.
5. Không đổ lỗi người thân; không khuyên cúng sao, giải hạn có thu phí; không khuyên mua bán tài sản cụ thể.
6. Câu hỏi ngoài Tử Vi, hoặc về người không có lá số trong dữ kiện → từ chối nhẹ nhàng,
   gợi ý một câu trong <faq_examples>.
7. Giọng: ấm, rõ, xưng “bạn”, như đang trò chuyện; câu dưới 30 chữ;
   giải thích ngắn thuật ngữ khi lần đầu xuất hiện.
8. Độ dài: HOI_DAP 60–150 chữ. VIET_LUAN: tổng quan 120–180 chữ, mỗi chủ đề 60–120 chữ;
   Nên/Không nên lấy đúng từ advice_json, được diễn đạt lại.
9. VIET_LUAN kết bằng một câu “Đức năng thắng số” ngắn; HOI_DAP chỉ thêm câu này khi hỏi về vận xấu.

TRẢ VỀ DUY NHẤT JSON:
HOI_DAP:   {"answer": "...", "ruleIds": ["..."], "followUps": ["...", "..."]}
VIET_LUAN: {"summary": "...", "sections": [{"topic": "SU_NGHIEP", "text": "...", "ruleIds": ["..."]}],
            "do": [{"text": "...", "when": "tháng 3, 7 âm", "ruleIds": ["..."]}],
            "dont": [{"text": "...", "ruleIds": ["..."]}], "closing": "..."}
```

### 12.5 Câu hỏi thường gặp trên máy

Bảng Room `faq_local` (module `:data:faq`): câu hỏi đã ẩn danh, dạng chuẩn hóa để so trùng, chủ đề, phạm vi, mã băm lá số, câu trả lời, `ruleIds`, đánh giá (−1, 0, +1), số lần hỏi, thời điểm. Dùng để hiện chip gợi ý câu hay hỏi, trả lời lại tức thì câu đã gặp, cho người xem đọc lại; xóa toàn bộ được trong Cài đặt.

### 12.6 Đồng bộ Firebase (tùy chọn, phải đồng ý)

- Mặc định tắt. Công tắc “Góp câu hỏi ẩn danh để cải thiện app” mở màn giải thích dữ liệu gửi đi; tắt lúc nào cũng được, kèm nút yêu cầu xóa phần đã gửi.
- Collection Firestore `faq_submissions`, mỗi bản ghi chỉ gồm: câu hỏi đã lọc (≤ 500 ký tự), chủ đề, phạm vi, `ruleIds`, đánh giá, `appVersion`, `kbVersion`, thời điểm phía máy chủ. Không gửi ngày giờ sinh, tên, lá số, câu trả lời, mã thiết bị.
- Bộ lọc ẩn danh chạy trên máy trước khi gửi: xóa ngày tháng năm, giờ sinh, số điện thoại, email, họ tên sau “tôi tên/em tên/con tên…”, số nhà; còn nghi ngờ → không gửi.
- Gói Spark miễn phí. Security Rules chỉ cho `create` đúng schema, không cho app đọc; bật App Check (Play Integrity) chống spam; app tự giới hạn 20 câu gửi mỗi ngày.
- Tuân thủ Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15 (hiệu lực 01/01/2026) và Nghị định 356/2025/NĐ-CP: đồng ý rõ ràng, rút lại được, xóa được; khai báo trong chính sách quyền riêng tư và mục Data Safety của Google Play.

### 12.7 Từ câu hỏi thật đến kho luận giải

Chủ app xem câu hỏi trong Firebase console (hoặc script xuất CSV), gom câu trùng, viết câu trả lời mẫu hoặc rule mới, đưa vào `:data:kb/faq/*.json` với `source` và `reviewed: true`, rồi phát hành theo `kbVersion`. Không tự động huấn luyện model từ dữ liệu người dùng; nếu fine-tune sau này, chỉ dùng câu đã duyệt.

## 13. UI/UX

Lá số vẽ theo bố cục truyền thống Việt Nam — lưới 4×4, 12 cung chạy thuận chiều kim đồng hồ quanh ô trung tâm 2×2 — vì người xem tử vi đã quen đọc vị trí cung theo bố cục này.

&#91;embedded content: bố cục lá số · lưới 4×4, chỉ số Tý = 0\]

Đi từ Tý (đáy, giữa phải) sang Sửu rồi lên cột trái là chiều thuận (+1) dùng trong mọi công thức mục 5 và 8; Dần là ô khởi đếm tháng Giêng khi an Mệnh, Thân.

**Nội dung một ô cung (từ trên xuống):** can chi cung + tên vai trò (Mệnh, Tài…) + nhãn “Thân” nếu có; tuổi đại hạn ở góc; chính tinh chữ đậm kèm ký hiệu độ sáng (M/V/Đ/B/H); hai cột phụ tinh — cát bên trái, hung bên phải; Tứ Hóa dạng huy hiệu; sao vòng Tràng Sinh và chi tiểu hạn ở chân ô. Tuần, Triệt vẽ thành dải trên đường ranh giới giữa hai cung bị án ngữ.

**Ô trung tâm:** tên, ngày giờ sinh dương/âm, can chi năm–tháng–ngày–giờ, Âm Dương Nam Nữ (thuận/nghịch lý), bản mệnh, Cục và quan hệ Cục–Mệnh, Mệnh chủ, Thân chủ, năm đang xem và tuổi âm, preset trường phái.

**Màu theo ngũ hành của sao:** Kim xám bạc, Mộc xanh lá, Thủy xanh dương đậm, Hỏa đỏ, Thổ vàng nâu — đủ tương phản ở cả nền sáng và tối (WCAG AA).

**Tương tác**

- Chạm một cung: vẽ tam phương tứ chính, tô hai cung giáp và cung nhị hợp; mở bảng luận cung đó.
- Chạm giữ một sao: thẻ sao (ngũ hành, hóa khí, độ sáng tại cung này, ý nghĩa, cách cục liên quan).
- Thanh thời gian (slider năm) + các công tắc lớp: sao lưu, Tứ Hóa lưu, đại hạn, tiểu hạn, lưu niên đại hạn, nguyệt hạn. Sao lưu hiển thị tiền tố “L.” và màu riêng.
- Pinch-zoom trên lưới; xoay ngang để xem lá số cỡ lớn; tablet chia hai panel (lá số | luận giải).

**Ràng buộc hiển thị:** trên Galaxy S22 Ultra dọc, mỗi ô rộng khoảng 96 dp — cỡ chữ phụ tinh 10–11 sp, chính tinh 12–13 sp; cho phép font scale tới 130% bằng cách cuộn trong ô. TalkBack đọc mỗi ô theo thứ tự: tên cung, chính tinh, phụ tinh. Dark mode đầy đủ.

**Các màn luận giải:** thẻ tóm tắt (điểm 0–100 + thế Tấn/Giữ/Thủ/Lùi) → các chủ đề dạng thẻ mở rộng → Nên / Không nên → nút “Vì sao?” trên mỗi đoạn. Đại vận là dòng thời gian ngang 12 ô; Tháng là heat-map 12 ô; Ngày là lịch tháng tô màu theo điểm.

**Màn Hỏi đáp** (mục 12): khung chat; chip gợi ý câu hỏi thường gặp ở đầu; mỗi câu trả lời có “Vì sao?” mở ra các finding đã dùng và nút 👍/👎; nhãn cố định “Trợ lý chạy trên máy, lá số không rời điện thoại”; tải, xóa model và công tắc đồng ý góp câu hỏi nằm trong Cài đặt.

**Xuất:** PDF A4 (trang 1 lá số, các trang sau luận giải, chân trang ghi preset và lời ngỏ) và ảnh PNG lá số để chia sẻ.

## 14. Kiểm thử & tiêu chí nghiệm thu

Engine chỉ được coi là xong khi pass cả ba lớp test: test vector lịch (mục 4), 4 lá số golden dưới đây, và bộ bất biến chạy trên 10.000 lá số ngẫu nhiên 1900–2100.

### 14.1 Lá số golden (preset Nam phái VN)

Sinh từ bản tham chiếu đã đối chiếu chéo với lasotuvi. Lưu thành `core/engine/src/test/resources/golden.json`. Ca B kiểm tra giờ Tý muộn (23:30 → đổi sang ngày 04/03) và lịch VN khác TQ; ca C kiểm tra tháng nhuận (`SAME_MONTH`); ca D sinh trước 1968 (tz 7 và tz 8 cho cùng ngày âm nên không phát cảnh báo — khoảng 3,8% ngày trước 1968 thì có).

```json
[{"id":"A","input":"Nam 15/08/1990 10:30 UTC+7","lunar":"25/6/1990","nam":"Canh Ngọ","gio":"Tỵ","menh":"Dần","than":"Tý","cuc":"Thổ 5","banMenh":"Thổ",
 "chinhTinh":{"Tử Vi":"Ngọ","Thiên Cơ":"Tỵ","Thái Dương":"Mão","Vũ Khúc":"Dần","Thiên Đồng":"Sửu","Liêm Trinh":"Tuất","Thiên Phủ":"Tuất","Thái Âm":"Hợi","Tham Lang":"Tý","Cự Môn":"Sửu","Thiên Tướng":"Dần","Thiên Lương":"Mão","Thất Sát":"Thìn","Phá Quân":"Thân"},
 "phuTinh":{"Lộc Tồn":"Thân","Kình Dương":"Dậu","Đà La":"Mùi","Tả Phù":"Dậu","Hữu Bật":"Tỵ","Văn Xương":"Tỵ","Văn Khúc":"Dậu","Thiên Khôi":"Ngọ","Thiên Việt":"Dần","Địa Không":"Ngọ","Địa Kiếp":"Thìn","Hỏa Tinh":"Ngọ","Linh Tinh":"Tuất","Thiên Mã":"Thân","Hóa Lộc":"Mão","Hóa Quyền":"Dần","Hóa Khoa":"Hợi","Hóa Kỵ":"Sửu","Tràng Sinh":"Thân","Thai":"Ngọ","Dưỡng":"Mùi","Đào Hoa":"Mão","Hồng Loan":"Dậu","Ân Quang":"Thìn","Thiên Quý":"Tuất","Đẩu Quân":"Ngọ"},
 "tuan":["Tuất","Hợi"],"triet":["Ngọ","Mùi"],"daiHan3":["Dần 5-14","Mão 15-24","Thìn 25-34"],"nam2026":{"tuoiAm":37,"daiHan":"Tỵ","tieuHan":"Thìn","luuNienDaiHan":"Tuất","nguyetHanThang1":"Thìn"}},
{"id":"B","input":"Nữ 03/03/1985 23:30 UTC+7","lunar":"13/2/1985","nam":"Ất Sửu","gio":"Tý","menh":"Mão","than":"Mão","cuc":"Thổ 5","banMenh":"Kim",
 "chinhTinh":{"Tử Vi":"Ngọ","Thiên Cơ":"Tỵ","Thái Dương":"Mão","Vũ Khúc":"Dần","Thiên Đồng":"Sửu","Liêm Trinh":"Tuất","Thiên Phủ":"Tuất","Thái Âm":"Hợi","Tham Lang":"Tý","Cự Môn":"Sửu","Thiên Tướng":"Dần","Thiên Lương":"Mão","Thất Sát":"Thìn","Phá Quân":"Thân"},
 "phuTinh":{"Lộc Tồn":"Mão","Kình Dương":"Thìn","Đà La":"Dần","Tả Phù":"Tỵ","Hữu Bật":"Dậu","Văn Xương":"Tuất","Văn Khúc":"Thìn","Thiên Khôi":"Tý","Thiên Việt":"Thân","Địa Không":"Hợi","Địa Kiếp":"Hợi","Hỏa Tinh":"Mão","Linh Tinh":"Tuất","Thiên Mã":"Hợi","Hóa Lộc":"Tỵ","Hóa Quyền":"Mão","Hóa Khoa":"Ngọ","Hóa Kỵ":"Hợi","Tràng Sinh":"Thân","Thai":"Ngọ","Dưỡng":"Mùi","Đào Hoa":"Ngọ","Hồng Loan":"Dần","Ân Quang":"Dậu","Thiên Quý":"Tỵ","Đẩu Quân":"Tý"},
 "tuan":["Tuất","Hợi"],"triet":["Ngọ","Mùi"],"daiHan3":["Mão 5-14","Thìn 15-24","Tỵ 25-34"],"nam2026":{"tuoiAm":42,"daiHan":"Ngọ","tieuHan":"Dần","luuNienDaiHan":"Thìn","nguyetHanThang1":"Sửu"}},
{"id":"C","input":"Nam 22/03/2023 06:00 UTC+7","lunar":"1/2N/2023","nam":"Quý Mão","gio":"Mão","menh":"Tý","than":"Ngọ","cuc":"Kim 4","banMenh":"Kim",
 "chinhTinh":{"Tử Vi":"Hợi","Thiên Cơ":"Tuất","Thái Dương":"Thân","Vũ Khúc":"Mùi","Thiên Đồng":"Ngọ","Liêm Trinh":"Mão","Thiên Phủ":"Tỵ","Thái Âm":"Ngọ","Tham Lang":"Mùi","Cự Môn":"Thân","Thiên Tướng":"Dậu","Thiên Lương":"Tuất","Thất Sát":"Hợi","Phá Quân":"Mão"},
 "phuTinh":{"Lộc Tồn":"Tý","Kình Dương":"Sửu","Đà La":"Hợi","Tả Phù":"Tỵ","Hữu Bật":"Dậu","Văn Xương":"Mùi","Văn Khúc":"Mùi","Thiên Khôi":"Mão","Thiên Việt":"Tỵ","Địa Không":"Thân","Địa Kiếp":"Dần","Hỏa Tinh":"Ngọ","Linh Tinh":"Sửu","Thiên Mã":"Tỵ","Hóa Lộc":"Mão","Hóa Quyền":"Thân","Hóa Khoa":"Ngọ","Hóa Kỵ":"Mùi","Tràng Sinh":"Tỵ","Thai":"Mùi","Dưỡng":"Ngọ","Đào Hoa":"Tý","Hồng Loan":"Tý","Ân Quang":"Ngọ","Thiên Quý":"Thân","Đẩu Quân":"Tỵ"},
 "tuan":["Thìn","Tỵ"],"triet":["Tý","Sửu"],"daiHan3":["Tý 4-13","Hợi 14-23","Tuất 24-33"],"nam2026":{"tuoiAm":4,"daiHan":"Tý","tieuHan":"Thìn","luuNienDaiHan":"Tý","nguyetHanThang1":"Ngọ"}},
{"id":"D","input":"Nữ 10/02/1964 14:00 UTC+7","lunar":"27/12/1963","nam":"Quý Mão","gio":"Mùi","menh":"Ngọ","than":"Thân","cuc":"Hỏa 6","banMenh":"Kim",
 "chinhTinh":{"Tử Vi":"Mão","Thiên Cơ":"Dần","Thái Dương":"Tý","Vũ Khúc":"Hợi","Thiên Đồng":"Tuất","Liêm Trinh":"Mùi","Thiên Phủ":"Sửu","Thái Âm":"Dần","Tham Lang":"Mão","Cự Môn":"Thìn","Thiên Tướng":"Tỵ","Thiên Lương":"Ngọ","Thất Sát":"Mùi","Phá Quân":"Hợi"},
 "phuTinh":{"Lộc Tồn":"Tý","Kình Dương":"Sửu","Đà La":"Hợi","Tả Phù":"Mão","Hữu Bật":"Hợi","Văn Xương":"Mão","Văn Khúc":"Hợi","Thiên Khôi":"Mão","Thiên Việt":"Tỵ","Địa Không":"Thìn","Địa Kiếp":"Ngọ","Hỏa Tinh":"Thìn","Linh Tinh":"Mão","Thiên Mã":"Tỵ","Hóa Lộc":"Hợi","Hóa Quyền":"Thìn","Hóa Khoa":"Dần","Hóa Kỵ":"Mão","Tràng Sinh":"Dần","Thai":"Tý","Dưỡng":"Sửu","Đào Hoa":"Tý","Hồng Loan":"Tý","Ân Quang":"Thìn","Thiên Quý":"Tuất","Đẩu Quân":"Hợi"},
 "tuan":["Thìn","Tỵ"],"triet":["Tý","Sửu"],"daiHan3":["Ngọ 6-15","Mùi 16-25","Thân 26-35"],"nam2026":{"tuoiAm":64,"daiHan":"Hợi","tieuHan":"Tuất","luuNienDaiHan":"Tuất","nguyetHanThang1":"Ngọ"}}]
```

### 14.2 Bất biến (property test có seed, 10.000 lá số; vét cạn bằng stressCheck)

1. Thiên Phủ = (4 − Tử Vi) mod 12; đúng 14 chính tinh được an.
2. Kình = Lộc Tồn + 1, Đà = Lộc Tồn − 1; Lộc Tồn không bao giờ ở Thìn, Tuất, Sửu, Mùi; Triệt không bao giờ ở Tuất, Hợi.
3. Thiên Tướng luôn nằm giữa Cự Môn (−1) và Thiên Lương (+1); Thất Sát, Phá Quân, Tham Lang luôn tam hợp nhau.
4. Không có cặp cung đối nhau cùng vô chính diệu (đã kiểm bằng vét cạn 12 vị trí Tử Vi).
5. Đối xứng trục Sửu–Mùi (tổng ≡ 2 mod 12): Xương–Khúc, Tả–Hữu, Long Trì–Phượng Các, Tam Thai–Bát Tọa, Ân Quang–Thiên Quý. Không–Kiếp tổng ≡ 10; Khốc–Hư tổng ≡ 0; Hồng Loan–Thiên Hỷ lệch 6.
6. Hoa Cái luôn ở Tứ Mộ, Thiên Mã luôn ở Tứ Sinh; Mệnh có Hoa Cái thì Phu Thê có Thiên Mã; Mệnh có Kình Dương thì Phu Thê có Đà La (cẩm nang P.V.1).
7. (Thân − Mệnh) mod 12 = 2h; 12 đại hạn nối tiếp, bắt đầu đúng số Cục.
8. Lịch khứ hồi: `lunar2Solar(solar2Lunar(d)) == d` cho mọi ngày 1900–2100.
9. Đổi preset chỉ làm đổi đúng nhóm sao bị ảnh hưởng (test diff theo từng khóa `SchoolConfig`).

### 14.3 Lint knowledge base

ID rule duy nhất; mọi rule có `source`; mọi `textKey`/`adviceKey` tồn tại; template chỉ dùng slot hợp lệ; không chứa từ cấm mục 11; mọi sao trong rule đều có trong danh mục `StarId`.

### 14.4 Tiêu chí nghiệm thu

- [ ] Toàn bộ test vector lịch mục 4 pass
- [ ] 4 lá số golden pass 100% trường
- [ ] Property test pass trên 10.000 lá số
- [ ] KB lint pass, không từ cấm
- [ ] Mọi đoạn luận hiển thị có ≥ 1 `ruleId` và mở được “Vì sao?”
- [ ] Chạy đủ tính năng ở chế độ máy bay khi tắt trợ lý AI; đã tải model thì hỏi đáp vẫn chạy ở chế độ máy bay
- [ ] Mọi câu trả lời của trợ lý qua bộ kiểm tra 12.3; đầu ra sai tự về bản rule-based
- [ ] Dữ liệu gửi Firestore chỉ có các trường ở 12.6 và chỉ gửi khi bật đồng ý; test chặn ngày sinh, tên, số điện thoại lẫn trong câu hỏi
- [ ] Đạt chỉ tiêu hiệu năng mục 3 trên Galaxy S22 Ultra
- [ ] TalkBack đọc được toàn bộ lá số

Công cụ dev tùy chọn: `tools/crosscheck/` (Python) so engine với lasotuvi (MIT) trên lá số ngẫu nhiên, bỏ qua nhóm sao biến thể, cặp Thai–Dưỡng, Phá Toái và Khôi Việt (lasotuvi lệch khẩu quyết, D-14); kèm script so lịch với amlich.js; spec\_reference.py là bản cài đặt tham chiếu độc lập viết lại từ prompt (so lá số + vận hạn); ./gradlew :core:engine:stressCheck vét cạn mọi ngày giờ sinh 1900–2100 qua bộ bất biến; SpecConformanceTest đọc thẳng các bảng trong prompt này để so với engine.

## 15. Lộ trình build & định dạng bàn giao

Làm lần lượt các giai đoạn 1, 2, 2B, 3, 4, 5, 6, dừng sau mỗi giai đoạn để báo cáo; không sang giai đoạn sau khi “cổng” của giai đoạn trước chưa pass.

- **Giai đoạn 1 — Lịch + engine an sao** (`:core:calendar`, `:core:engine`): port Hồ Ngọc Đức, `ChartEngine`, `SchoolConfig`, bảng độ sáng. *Cổng:* test vector lịch, phần lá số của 4 ca golden, property test mục 14.2.
- **Giai đoạn 2 — Vận hạn** (`LimitEngine`): đại hạn + thiên thời, tiểu hạn, lưu niên đại hạn, sao lưu, Lưu Mệnh, Tứ Hóa lưu/đại hạn/nguyệt, nguyệt/nhật/thời hạn. *Cổng:* trường `nam2026` của golden + ví dụ Tân Biên (Dương nam, đại hạn 23–32: 23 gốc, 24 xung chiếu, 25 lùi một cung, 26 xung chiếu, 27 tiến một cung).
- **Giai đoạn 2B — Bản chạy thử giao diện** (`:app` tối thiểu, Compose): màn nhập ngày giờ sinh, thẻ xác nhận lá số, lưới 12 cung theo mục 13, chọn năm xem để hiện đại hạn, tiểu hạn, Lưu Thái Tuế và sao lưu. Cài lên điện thoại thật bằng Android Studio (Run qua cáp USB). *Cổng:* chạy trên Galaxy S22 Ultra; 4 ca golden hiển thị đúng; chưa cần luận giải.
- **Giai đoạn 3 — Knowledge base + RuleEngine:** schema JSON (kể cả `faq/` cho câu trả lời mẫu), lint, ≥ 120 cách cục từ mục 10 và cẩm nang, script sinh nháp câu luận (`reviewed: false`). *Cổng:* mỗi rule có một lá số fixture kích hoạt và một fixture không kích hoạt; KB lint pass.
- **Giai đoạn 4 — Luận giải + khuyến nghị:** chấm điểm, tổng hợp tầng, ghép câu, xử lý mâu thuẫn, Nên/Không nên. *Cổng:* snapshot bản luận của 4 ca golden; không có cặp câu trái ngược cùng chủ đề; quét từ cấm sạch.
- **Giai đoạn 5 — UI đầy đủ:** nâng bản chạy thử lên đủ nhập liệu, thẻ xác nhận, lưới lá số, 5 màn luận giải, cài đặt trường phái, hồ sơ, xuất PDF/PNG. *Cổng:* screenshot test, TalkBack, chỉ tiêu hiệu năng.
- **Giai đoạn 6 — Trợ lý AI trên máy + câu hỏi thường gặp + phát hành:** `:ai` (LiteRT-LM, tải model, ghép ngữ cảnh, kiểm tra đầu ra, fallback), `:data:faq` (Room, bộ lọc ẩn danh, Firestore + Security Rules + App Check), màn Hỏi đáp, chính sách quyền riêng tư và Data Safety. *Cổng:* bộ 40 câu hỏi mẫu chấm đạt trên S22 Ultra (ghi kết quả chọn model); hỏi đáp chạy ở chế độ máy bay khi đã tải model; đầu ra sai tự về rule-based; test payload Firestore không chứa ngày sinh, tên, số điện thoại; tắt đồng ý thì không gửi gì.

**Báo cáo cuối mỗi giai đoạn** gồm: danh sách file đã tạo/sửa; kết quả test (số pass/fail); chỗ nào đã phải tự quyết (ghi vào `docs/DECISIONS.md`); ảnh chụp màn hình với giai đoạn UI.

**Ba câu phải hỏi người dùng trước khi chốt Giai đoạn 1** (không hỏi được thì dùng mặc định và ghi vào `DECISIONS.md`): (1) năm Nhâm hóa Khoa là Tả Phù (mặc định) hay Thiên Phủ; (2) sinh tháng nhuận tính như tháng chính (mặc định) hay chia đôi tháng; (3) Khôi Việt tuổi Canh ở Ngọ/Dần (mặc định) hay Sửu/Mùi. Người dùng đã chốt ngày 20/11/2026: cả ba theo mặc định.

## 16. Phụ lục: đính chính cẩm nang V2 & nguồn tham khảo

Cẩm nang V2 đúng ở phần lớn quy tắc an sao (an Mệnh–Thân, Cục, 14 chính tinh, vòng sao, Tuần Triệt, tiểu hạn, sao lưu đều khớp bản tham chiếu); 10 điểm dưới đây đã được sửa hoặc chuyển thành cấu hình.

| # | Vị trí trong cẩm nang | Cẩm nang ghi | Prompt dùng |
| --- | --- | --- | --- |
| 1 | P.I ch.1 — Tứ Mộ | Tứ Mộ “độc quyền” Hoa Cái, Cô Thần, Quả Tú | Hoa Cái, Quả Tú luôn ở Tứ Mộ; Cô Thần luôn ở Tứ Sinh (Dần, Tỵ, Thân, Hợi) |
| 2 | P.III.2 — Thiên Cơ | Hãm tại Dậu | Miếu tại Dậu (khớp cách Cơ Cự Mão Dậu của chính cẩm nang) |
| 3 | P.III.4 — Vũ Khúc | Hãm tại Mão, Dậu | Đắc tại Mão, Dậu; chỉ hãm tại Tỵ, Hợi |
| 4 | P.III.5, P.III.6 | Thiên Đồng thiếu Tỵ; Liêm Trinh thiếu Sửu, Mùi | Đắc địa |
| 5 | P.III.10 — Cự Môn | Tý, Ngọ Đắc; Thân Vượng | Tý, Ngọ Vượng; Thân Đắc (cấu hình `cuMonTable`) |
| 6 | Bảng thuộc tính 14 sao | Thái Dương, Thái Âm thuộc Bắc Đẩu | Thường xếp Trung Thiên tinh; chỉ ảnh hưởng mục dự đoán giới tính (đã bỏ) |
| 7 | P.III.14 — Phá Quân | “Rất ưa Lục Sát” nói chung | Chỉ khi Phá Quân miếu vượng và sát tinh đắc địa (mục 10.3) |
| 8 | P.IV ch.2 — Lưu niên đại hạn | “Lùi 1 cung theo chiều thuận” (mơ hồ) | Công thức Tân Biên ở mục 8.3 |
| 9 | P.IV ch.4 — Tứ Hóa năm Canh | Thiên Đồng hóa Kỵ | Giữ làm mặc định; thêm biến thể Thái Âm hóa Kỵ (cụ Thiên Lương) |
| 10 | P.V — chuyên đề | “chắc chắn”, “90%”, “100%”, dự đoán giới tính con | Mức khả năng theo quy tắc đồng thanh tương ứng; bỏ dự đoán giới tính thai nhi |

### Nguồn tham khảo

- [lasotuvi — mã nguồn mở an sao Tử Vi (MIT)](https://github.com/doanguyen/lasotuvi): dùng để đối chiếu 3.000 lá số; có bản Python thuật toán lịch Hồ Ngọc Đức.
- [Vietnamese calendar — Wikipedia](https://en.wikipedia.org/wiki/Vietnamese_calendar): mốc chuyển lịch UTC+8 → UTC+7.
- [Tết — tư liệu Nam Kỳ Lục Tỉnh (PDF)](https://www.namkyluctinh.org/a-vh-vminh/tet-wiki.pdf): Tết Mậu Thân 1968 lệch ngày hai miền.
- [VnExpress — Lịch sai do dịch sách Trung Quốc](https://vnexpress.net/lich-sai-do-dich-sach-trung-quoc-2814087.html): lịch VN khác TQ do múi giờ.
- [Học quán Sơn Chu — Sai lầm về an sao lập số](https://sonchu.vn/nghien-cuu/sai-lam-ve-an-sao-lap-so): cách an Hỏa Linh và Khôi Việt.
- [Học viện Lý số — Lưu nguyệt hạn](https://hocvienlyso.org/chuong-9-luu-nguyet-han.html) và [Lưu Nguyệt, lưu Nhật, lưu Thời](https://hocvienlyso.org/luu-nguyet-luu-nhat-luu-thoi.html).
- [Lý số Việt Nam — trích Tân Biên về lưu đại hạn](https://lyso.vn/xem-tu-vi/xin-hoi-ve-cach-tinh-dai-han-va-tieu-han-t38768/) (đọc qua trích đoạn tìm kiếm; trang chặn truy cập tự động).
- [Luật Minh Khuê — Tử vi là gì](https://luatminhkhue.vn/tu-vi-la-gi.aspx): xử phạt hoạt động mê tín thu lợi theo NĐ 38/2021 (đọc qua trích đoạn tìm kiếm).
- Sách: Vân Đằng Thái Thứ Lang, *Tử Vi Đẩu Số Tân Biên*; *Cẩm nang thực hành Tử Vi Đẩu Số toàn thư V2* (file của bạn).
- [amlich.js](https://www.npmjs.com/package/amlich.js) — port JavaScript nguyên bản thuật toán Hồ Ngọc Đức, dùng đối chiếu 146.828 ngày ở Giai đoạn 1.
- [Gemma 4 trên LiteRT-LM](https://ai.google.dev/edge/litert-lm/models/gemma-4) — bản E2B/E4B cho điện thoại, API Kotlin cho Android.
- [Qwen3.5 Small](https://huggingface.co/Qwen/Qwen3.5-0.8B) — dòng 0.8B–9B, Apache 2.0, bản nhỏ cho thiết bị.
- [Firebase — các gói giá](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans) — gói Spark miễn phí, không cần thẻ thanh toán.
- [Luật Bảo vệ dữ liệu cá nhân 91/2025/QH15](https://thuvienphapluat.vn/van-ban/Cong-nghe-thong-tin/Luat-Bao-ve-du-lieu-ca-nhan-2025-91-2025-QH15-625628.aspx) và [tóm tắt Nghị định 356/2025/NĐ-CP (EY)](https://www.ey.com/content/dam/ey-unified-site/ey-com/vi-vn/technical/tax/documents/ey-vietnam-legal-alert-march-2026-decree-no356-2025-nd-cp-providing-detailed-guidance-for-implementation-of-personal-data-protection-law-viet.pdf) — đồng ý, rút lại, xóa dữ liệu.
