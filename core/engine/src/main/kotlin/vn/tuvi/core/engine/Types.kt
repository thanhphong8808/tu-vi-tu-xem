package vn.tuvi.core.engine

import vn.tuvi.core.calendar.BirthRegion
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

enum class Gender(val vn: String) { NAM("Nam"), NU("Nữ") }

/** Thứ tự = số bước thuận tính từ cung Mệnh (vai trò thứ k đặt ở Mệnh + k). */
enum class PalaceRole(val vn: String) {
    MENH("Mệnh"), PHU_MAU("Phụ Mẫu"), PHUC_DUC("Phúc Đức"), DIEN_TRACH("Điền Trạch"),
    QUAN_LOC("Quan Lộc"), NO_BOC("Nô Bộc"), THIEN_DI("Thiên Di"), TAT_ACH("Tật Ách"),
    TAI_BACH("Tài Bạch"), TU_TUC("Tử Tức"), PHU_THE("Phu Thê"), HUYNH_DE("Huynh Đệ");

    override fun toString(): String = vn
}

/** Độ sáng. [scoreWeight] là trọng số gợi ý cho chấm điểm (mục 6): M +2, V +1,5, Đ +1, B 0, H −1,5. */
enum class Brightness(val vn: String, val code: String, val scoreWeight: Double) {
    MIEU("Miếu", "M", 2.0),
    VUONG("Vượng", "V", 1.5),
    DAC("Đắc", "Đ", 1.0),
    BINH("Bình hòa", "B", 0.0),
    HAM("Hãm", "H", -1.5);

    companion object {
        fun fromCode(code: String): Brightness =
            entries.firstOrNull { it.code == code.trim() } ?: throw IllegalArgumentException("Không rõ độ sáng: $code")
    }
}

enum class NguHanh(val vn: String) {
    KIM("Kim"), THUY("Thủy"), HOA("Hỏa"), THO("Thổ"), MOC("Mộc");

    /** Vòng tương sinh: Kim → Thủy → Mộc → Hỏa → Thổ → Kim. */
    val generates: NguHanh
        get() = when (this) { KIM -> THUY; THUY -> MOC; MOC -> HOA; HOA -> THO; THO -> KIM }

    /** Vòng tương khắc: Kim → Mộc → Thổ → Thủy → Hỏa → Kim. */
    val overcomes: NguHanh
        get() = when (this) { KIM -> MOC; MOC -> THO; THO -> THUY; THUY -> HOA; HOA -> KIM }
}

/** Nạp âm của một cặp can chi (tên đủ, vd. “Lộ Bàng Thổ”). */
data class NapAm(val name: String, val element: NguHanh) {
    override fun toString(): String = name

    companion object {
        private val NAMES = listOf(
            "Hải Trung Kim", "Lô Trung Hỏa", "Đại Lâm Mộc", "Lộ Bàng Thổ", "Kiếm Phong Kim",
            "Sơn Đầu Hỏa", "Giản Hạ Thủy", "Thành Đầu Thổ", "Bạch Lạp Kim", "Dương Liễu Mộc",
            "Tuyền Trung Thủy", "Ốc Thượng Thổ", "Tích Lịch Hỏa", "Tùng Bách Mộc", "Trường Lưu Thủy",
            "Sa Trung Kim", "Sơn Hạ Hỏa", "Bình Địa Mộc", "Bích Thượng Thổ", "Kim Bạch Kim",
            "Phú Đăng Hỏa", "Thiên Hà Thủy", "Đại Trạch Thổ", "Thoa Xuyến Kim", "Tang Đố Mộc",
            "Đại Khê Thủy", "Sa Trung Thổ", "Thiên Thượng Hỏa", "Thạch Lựu Mộc", "Đại Hải Thủy",
        )

        /**
         * Hành nạp âm theo công thức mục 5.1:
         * v = ⌊can/2⌋ + 1 + ⌊(chi mod 6)/2⌋; nếu v > 5 thì v − 5; 1 Kim, 2 Thủy, 3 Hỏa, 4 Thổ, 5 Mộc.
         */
        fun elementOf(can: Can, chi: Chi): NguHanh {
            var v = can.index / 2 + 1 + (chi.index % 6) / 2
            if (v > 5) v -= 5
            return when (v) {
                1 -> NguHanh.KIM; 2 -> NguHanh.THUY; 3 -> NguHanh.HOA; 4 -> NguHanh.THO; else -> NguHanh.MOC
            }
        }

        fun of(can: Can, chi: Chi): NapAm =
            NapAm(NAMES[CanChi(can, chi).sexagenaryIndex / 2], elementOf(can, chi))

        internal fun nameFor(sexagenaryIndex: Int): String = NAMES[sexagenaryIndex / 2]
    }
}

enum class Cuc(val number: Int, val element: NguHanh, val vn: String) {
    THUY_NHI(2, NguHanh.THUY, "Thủy nhị cục"),
    MOC_TAM(3, NguHanh.MOC, "Mộc tam cục"),
    KIM_TU(4, NguHanh.KIM, "Kim tứ cục"),
    THO_NGU(5, NguHanh.THO, "Thổ ngũ cục"),
    HOA_LUC(6, NguHanh.HOA, "Hỏa lục cục");

    /** Dạng ngắn dùng trong golden test: “Thổ 5”. */
    val short: String get() = "${element.vn} $number"

    companion object {
        fun of(element: NguHanh): Cuc = entries.first { it.element == element }
    }
}

/** Tương quan Cục – bản Mệnh (cẩm nang P.I.4). */
enum class CucMenhRelation(val vn: String) {
    CUC_SINH_MENH("Cục sinh Mệnh — được hoàn cảnh nâng đỡ"),
    MENH_SINH_CUC("Mệnh sinh Cục — hao tổn tâm lực"),
    DONG_HANH("Cục Mệnh đồng hành — bình ổn"),
    CUC_KHAC_MENH("Cục khắc Mệnh — nghịch cảnh, phải tự lực"),
    MENH_KHAC_CUC("Mệnh khắc Cục — vượt khó bằng ý chí");

    companion object {
        fun of(cuc: NguHanh, menh: NguHanh): CucMenhRelation = when {
            cuc == menh -> DONG_HANH
            cuc.generates == menh -> CUC_SINH_MENH
            menh.generates == cuc -> MENH_SINH_CUC
            cuc.overcomes == menh -> CUC_KHAC_MENH
            else -> MENH_KHAC_CUC
        }
    }
}

/** Âm Dương Nam Nữ. [dir] = +1 với Dương Nam, Âm Nữ; −1 với Âm Nam, Dương Nữ. */
enum class AmDuongNamNu(val vn: String, val dir: Int) {
    DUONG_NAM("Dương Nam", +1),
    AM_NU("Âm Nữ", +1),
    AM_NAM("Âm Nam", -1),
    DUONG_NU("Dương Nữ", -1);

    companion object {
        fun of(yangYear: Boolean, gender: Gender): AmDuongNamNu = when {
            yangYear && gender == Gender.NAM -> DUONG_NAM
            !yangYear && gender == Gender.NAM -> AM_NAM
            yangYear -> DUONG_NU
            else -> AM_NU
        }
    }
}

/**
 * Chọn canh giờ thay cho giờ phút. Giờ Tý tách hai: [TY_SOM] (00:00–00:59, cùng ngày) và
 * [TY_MUON] (23:00–23:59, áp quy tắc đổi ngày của SchoolConfig.dayBoundary).
 */
enum class CanhGio(val chi: Chi, val vn: String) {
    TY_SOM(Chi.TY, "Tý sớm (00:00–00:59)"),
    SUU(Chi.SUU, "Sửu (01:00–02:59)"),
    DAN(Chi.DAN, "Dần (03:00–04:59)"),
    MAO(Chi.MAO, "Mão (05:00–06:59)"),
    THIN(Chi.THIN, "Thìn (07:00–08:59)"),
    TI(Chi.TI, "Tỵ (09:00–10:59)"),
    NGO(Chi.NGO, "Ngọ (11:00–12:59)"),
    MUI(Chi.MUI, "Mùi (13:00–14:59)"),
    THAN(Chi.THAN, "Thân (15:00–16:59)"),
    DAU(Chi.DAU, "Dậu (17:00–18:59)"),
    TUAT(Chi.TUAT, "Tuất (19:00–20:59)"),
    HOI(Chi.HOI, "Hợi (21:00–22:59)"),
    TY_MUON(Chi.TY, "Tý muộn (23:00–23:59)");

    val isLateTy: Boolean get() = this == TY_MUON

    companion object {
        /** 12 canh dùng khi không rõ giờ sinh (Tý tính là Tý sớm). */
        val TWELVE: List<CanhGio> = entries.filter { it != TY_MUON }

        /** Canh chứa giờ đồng hồ [time]. */
        fun of(time: LocalTime): CanhGio = when (val h = time.hour) {
            23 -> TY_MUON
            0 -> TY_SOM
            else -> entries[(h + 1) / 2]
        }
    }
}

val VN_STANDARD_ZONE: ZoneId = ZoneOffset.ofHours(7)

/**
 * Thông tin sinh. Đúng một trong [solar] hoặc [lunar] khác null.
 *
 * - [solar]: ngày dương + giờ đồng hồ ghi trên giấy tờ, theo múi giờ [zone]. Ở chế độ [canh] phần giờ bị bỏ qua.
 * - [lunar]: ngày âm (dân lịch) khi người dùng nhập âm lịch; giờ lấy từ [lunarClock] hoặc [canh].
 * - [zone]: múi giờ của giờ đồng hồ. Miền Nam nên dùng `Asia/Ho_Chi_Minh` để tzdb quy đổi giờ UTC+8 giai đoạn 1960–1975.
 * - [region]: quyết định múi giờ lịch 1967–1975 và cách quy đổi giờ (nước ngoài dùng giờ chuẩn địa phương, bỏ DST).
 * - [calendarTzOverride]: ép múi giờ lịch 7 hoặc 8 (dùng cho “xem lá số thay thế”).
 */
data class BirthInput(
    val solar: LocalDateTime?,
    val lunar: LunarDate?,
    val gender: Gender,
    val zone: ZoneId = VN_STANDARD_ZONE,
    val longitudeDeg: Double? = null,
    val useTrueSolarTime: Boolean = false,
    val lunarClock: LocalTime? = null,
    val canh: CanhGio? = null,
    val region: BirthRegion = BirthRegion.VN_BAC,
    val calendarTzOverride: Int? = null,
) {
    init {
        require((solar == null) != (lunar == null)) { "Nhập đúng một trong ngày dương lịch hoặc âm lịch" }
        require(lunar == null || lunarClock != null || canh != null) { "Nhập âm lịch cần kèm giờ sinh hoặc canh giờ" }
        require(calendarTzOverride == null || calendarTzOverride == 7 || calendarTzOverride == 8) {
            "Múi giờ lịch chỉ nhận 7 hoặc 8"
        }
    }
}

/** Kết quả chuẩn hóa đầu vào — mọi con số engine dùng để an sao. */
data class NormalizedBirth(
    /** Giờ đã quy đổi (UTC+7 với sinh ở VN; giờ chuẩn địa phương với nước ngoài; sau hiệu chỉnh Mặt Trời nếu bật). Null khi nhập canh giờ. */
    val referenceTime: LocalDateTime?,
    /** Ngày dương dùng đổi sang âm lịch (đã áp đổi ngày lúc 23:00 nếu có). */
    val calendarDate: LocalDate,
    /** Múi giờ lịch đã dùng (7 hoặc 8). */
    val calendarTz: Int,
    val lunar: LunarDate,
    val hourChi: Chi,
    /** Sinh trong giờ Tý muộn (23:00–23:59). */
    val lateTy: Boolean,
)

/** Tứ trụ can chi. */
data class Pillars(val year: CanChi, val month: CanChi, val day: CanChi, val hour: CanChi)

data class PlacedStar(val id: StarId, val brightness: Brightness?, val kind: StarKind) {
    override fun toString(): String = brightness?.let { "${id.vn} (${it.code})" } ?: id.vn
}

data class Palace(
    val chi: Chi,
    val can: Can,
    val role: PalaceRole,
    val stars: List<PlacedStar>,
    val daiHanStartAge: Int,
    val tieuHanChi: Chi,
    val hasTuan: Boolean,
    val hasTriet: Boolean,
    val isThan: Boolean,
) {
    val isVoChinhDieu: Boolean get() = stars.none { it.kind == StarKind.CHINH_TINH }
    val daiHanAges: IntRange get() = daiHanStartAge..daiHanStartAge + 9
    fun has(star: StarId): Boolean = stars.any { it.id == star }
    fun star(id: StarId): PlacedStar? = stars.firstOrNull { it.id == id }
    val chinhTinh: List<PlacedStar> get() = stars.filter { it.kind == StarKind.CHINH_TINH }
}

enum class WarningCode(val vn: String) {
    TY_MUON("Sinh giờ Tý muộn (23:00–23:59)"),
    LEAP_MONTH("Sinh trong tháng nhuận"),
    CALENDAR_TZ("Âm lịch khác nhau giữa lịch UTC+7 và UTC+8"),
    NEAR_HOUR_BOUNDARY("Giờ sinh sát ranh giới hai canh giờ"),
    CLOCK_CONVERTED("Quy đổi giờ đồng hồ lịch sử làm đổi canh giờ"),
    TRUE_SOLAR_SHIFT("Hiệu chỉnh giờ Mặt Trời thật làm đổi canh giờ"),
}

/**
 * Cảnh báo trên thẻ “Xác nhận lá số”. Lá số thay thế = `ChartEngine.build(altInput ?: input, altConfig ?: config)`,
 * hoặc gọi [ChartEngine.buildAlternative].
 */
data class ChartWarning(
    val code: WarningCode,
    val message: String,
    val altInput: BirthInput? = null,
    val altConfig: SchoolConfig? = null,
)

data class Chart(
    val input: BirthInput,
    val lunar: LunarDate,
    val yearCan: Can,
    val yearChi: Chi,
    val hourChi: Chi,
    val isYangYear: Boolean,
    val banMenh: NapAm,
    val cuc: Cuc,
    val menh: Chi,
    val than: Chi,
    val palaces: Map<Chi, Palace>,
    val config: SchoolConfig,
    val warnings: List<ChartWarning>,
    // —— Bổ sung ngoài kiểu dữ liệu tối thiểu của prompt ——
    val gender: Gender,
    val amDuongNamNu: AmDuongNamNu,
    val birth: NormalizedBirth,
    val pillars: Pillars,
    /** Tháng dùng để an sao (khác lunar.month khi leapMonth = SPLIT_15 và sinh từ ngày 16 tháng nhuận). */
    val effectiveMonth: Int,
    val menhChu: StarId,
    val thanChu: StarId,
    val tuan: Pair<Chi, Chi>,
    val triet: Pair<Chi, Chi>,
    /** Vị trí từng sao — mỗi sao đúng một cung. */
    val starPositions: Map<StarId, Chi>,
) {
    /** +1 với Dương Nam, Âm Nữ; −1 với Âm Nam, Dương Nữ. */
    val dir: Int get() = amDuongNamNu.dir

    fun palace(chi: Chi): Palace = palaces.getValue(chi)
    fun palace(role: PalaceRole): Palace = palaces.values.first { it.role == role }
    fun positionOf(star: StarId): Chi = starPositions.getValue(star)
    fun palaceOf(star: StarId): Palace = palace(positionOf(star))

    val cucMenhRelation: CucMenhRelation get() = CucMenhRelation.of(cuc.element, banMenh.element)

    /** Âm Dương thuận lý: tuổi Dương có Mệnh ở cung Dương, tuổi Âm có Mệnh ở cung Âm (mục 10.4.6). */
    val amDuongThuanLy: Boolean get() = isYangYear == menh.isYang

    /** Tuổi âm tại năm âm [lunarYear]. */
    fun tuoiAm(lunarYear: Int): Int = lunarYear - lunar.year + 1
}
