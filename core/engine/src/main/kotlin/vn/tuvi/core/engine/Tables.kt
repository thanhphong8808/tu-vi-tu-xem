package vn.tuvi.core.engine

import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.Chi.DAN
import vn.tuvi.core.calendar.Chi.DAU
import vn.tuvi.core.calendar.Chi.HOI
import vn.tuvi.core.calendar.Chi.MAO
import vn.tuvi.core.calendar.Chi.MUI
import vn.tuvi.core.calendar.Chi.NGO
import vn.tuvi.core.calendar.Chi.SUU
import vn.tuvi.core.calendar.Chi.THAN
import vn.tuvi.core.calendar.Chi.THIN
import vn.tuvi.core.calendar.Chi.TI
import vn.tuvi.core.calendar.Chi.TUAT
import vn.tuvi.core.calendar.Chi.TY

/** Bảng tra an sao theo can năm, tam hợp… (mục 5.3–5.7, 7). Chỉ số mảng theo can Giáp = 0 … Quý = 9. */
internal object Tables {

    // —— 5.5 Sao an theo can năm ——
    val LOC_TON: List<Chi> = listOf(DAN, MAO, TI, NGO, TI, NGO, THAN, DAU, HOI, TY)

    private val KHOI_VN: List<Chi> = listOf(SUU, TY, HOI, HOI, SUU, TY, NGO, NGO, MAO, MAO)
    private val VIET_VN: List<Chi> = listOf(MUI, THAN, DAU, DAU, MUI, THAN, DAN, DAN, TI, TI)

    fun khoi(can: Can, v: KhoiVietVariant): Chi =
        if (v == KhoiVietVariant.TOAN_THU && can == Can.CANH) SUU else KHOI_VN[can.index]

    fun viet(can: Can, v: KhoiVietVariant): Chi =
        if (v == KhoiVietVariant.TOAN_THU && can == Can.CANH) MUI else VIET_VN[can.index]

    val THIEN_QUAN: List<Chi> = listOf(MUI, THIN, TI, DAN, MAO, DAU, HOI, DAU, TUAT, NGO)
    val THIEN_PHUC: List<Chi> = listOf(DAU, THAN, TY, HOI, MAO, DAN, NGO, TI, NGO, TI)
    private val LUU_HA_TOAN_THU: List<Chi> = listOf(DAU, TUAT, MUI, THAN, TI, NGO, THIN, MAO, HOI, DAN)
    val THIEN_TRU: List<Chi> = listOf(TI, NGO, TY, TI, NGO, THAN, DAN, NGO, DAU, TUAT)

    fun luuHa(can: Can, v: LuuHaVariant): Chi = when {
        v == LuuHaVariant.LASOTUVI && can == Can.DINH -> THIN
        v == LuuHaVariant.LASOTUVI && can == Can.CANH -> THAN
        else -> LUU_HA_TOAN_THU[can.index]
    }

    /** Triệt: Giáp Kỷ Thân–Dậu, Ất Canh Ngọ–Mùi, Bính Tân Thìn–Tỵ, Đinh Nhâm Dần–Mão, Mậu Quý Tý–Sửu. */
    fun triet(can: Can): Pair<Chi, Chi> {
        val first = Chi.of(8 - 2 * (can.index % 5))
        return first to first + 1
    }

    /** Tuần: s = (chi − can) mod 12; Tuần tại s + 10 và s + 11. */
    fun tuan(can: Can, chi: Chi): Pair<Chi, Chi> {
        val s = Math.floorMod(chi.index - can.index, 12)
        return Chi.of(s + 10) to Chi.of(s + 11)
    }

    // —— 5.4 Theo tam hợp (chỉ số = Chi.tamHopGroup: 0 Thân Tý Thìn, 1 Tỵ Dậu Sửu, 2 Dần Ngọ Tuất, 3 Hợi Mão Mùi) ——
    val THIEN_MA: List<Chi> = listOf(DAN, HOI, THAN, TI)
    val HOA_CAI: List<Chi> = listOf(THIN, SUU, TUAT, MUI)
    val KIEP_SAT: List<Chi> = listOf(TI, DAN, HOI, THAN)
    val DAO_HOA: List<Chi> = listOf(DAU, NGO, MAO, TY)

    /** Cung khởi tiểu hạn theo tam hợp chi năm sinh (mục 8.2). */
    val TIEU_HAN_START: List<Chi> = listOf(TUAT, MUI, THIN, SUU)

    /** Cô Thần: Hợi Tý Sửu → Dần; Dần Mão Thìn → Tỵ; Tỵ Ngọ Mùi → Thân; Thân Dậu Tuất → Hợi. */
    fun coThan(yearChi: Chi): Chi = Chi.of(2 + 3 * (Math.floorMod(yearChi.index + 1, 12) / 3))

    /** Phá Toái: Tý Ngọ Mão Dậu → Tỵ; Thìn Tuất Sửu Mùi → Sửu; Dần Thân Tỵ Hợi → Dậu. */
    fun phaToai(yearChi: Chi): Chi = listOf(TI, SUU, DAU)[yearChi.index % 3]

    // —— 5.6 Hỏa Tinh, Linh Tinh: cung khởi (giờ Tý) theo tam hợp ——
    private val HOA_START_TTL: List<Chi> = listOf(DAN, MAO, SUU, DAU)
    private val LINH_START_TTL: List<Chi> = listOf(TUAT, TUAT, MAO, TUAT)

    fun hoaStart(group: Int, v: HoaLinhVariant): Chi =
        if (v == HoaLinhVariant.DAC_LOC && group == 1) TUAT else HOA_START_TTL[group]

    fun linhStart(group: Int, v: HoaLinhVariant): Chi =
        if (v == HoaLinhVariant.DAC_LOC && group == 1) MAO else LINH_START_TTL[group]

    /** Cung khởi Tràng Sinh theo hành Cục. */
    fun trangSinhStart(cuc: Cuc): Chi = when (cuc.element) {
        NguHanh.KIM -> TI
        NguHanh.MOC -> HOI
        NguHanh.THUY, NguHanh.THO -> THAN
        NguHanh.HOA -> DAN
    }

    // —— 5.7 Mệnh chủ, Thân chủ ——
    val MENH_CHU: List<StarId> = listOf(
        StarId.THAM_LANG, StarId.CU_MON, StarId.LOC_TON, StarId.VAN_KHUC, StarId.LIEM_TRINH, StarId.VU_KHUC,
        StarId.PHA_QUAN, StarId.VU_KHUC, StarId.LIEM_TRINH, StarId.VAN_KHUC, StarId.LOC_TON, StarId.CU_MON,
    )

    fun thanChu(yearChi: Chi, v: ThanChuTy): StarId = when (yearChi) {
        TY -> v.star
        SUU, MUI -> StarId.THIEN_TUONG
        DAN, THAN -> StarId.THIEN_LUONG
        MAO, DAU -> StarId.THIEN_DONG
        THIN, TUAT -> StarId.VAN_XUONG
        TI, HOI -> StarId.THIEN_CO
        NGO -> StarId.HOA_TINH
    }

    // —— 7. Tứ Hóa theo can: Lộc, Quyền, Khoa, Kỵ ——
    fun tuHoa(can: Can, config: SchoolConfig): Map<TuHoa, StarId> {
        val row: List<StarId> = when (can) {
            Can.GIAP -> listOf(StarId.LIEM_TRINH, StarId.PHA_QUAN, StarId.VU_KHUC, StarId.THAI_DUONG)
            Can.AT -> listOf(StarId.THIEN_CO, StarId.THIEN_LUONG, StarId.TU_VI, StarId.THAI_AM)
            Can.BINH -> listOf(StarId.THIEN_DONG, StarId.THIEN_CO, StarId.VAN_XUONG, StarId.LIEM_TRINH)
            Can.DINH -> listOf(StarId.THAI_AM, StarId.THIEN_DONG, StarId.THIEN_CO, StarId.CU_MON)
            Can.MAU -> listOf(StarId.THAM_LANG, StarId.THAI_AM, StarId.HUU_BAT, StarId.THIEN_CO)
            Can.KY -> listOf(StarId.VU_KHUC, StarId.THAM_LANG, StarId.THIEN_LUONG, StarId.VAN_KHUC)
            Can.CANH -> when (config.tuHoa) {
                TuHoaVariant.NAM_PHAI_VN -> listOf(StarId.THAI_DUONG, StarId.VU_KHUC, StarId.THAI_AM, StarId.THIEN_DONG)
                TuHoaVariant.THIEN_LUONG -> listOf(StarId.THAI_DUONG, StarId.VU_KHUC, StarId.THIEN_DONG, StarId.THAI_AM)
            }
            Can.TAN -> listOf(StarId.CU_MON, StarId.THAI_DUONG, StarId.VAN_KHUC, StarId.VAN_XUONG)
            Can.NHAM -> listOf(StarId.THIEN_LUONG, StarId.TU_VI, config.nhamKhoa.star, StarId.VU_KHUC)
            Can.QUY -> listOf(StarId.PHA_QUAN, StarId.CU_MON, StarId.THAI_AM, StarId.THAM_LANG)
        }
        return mapOf(TuHoa.LOC to row[0], TuHoa.QUYEN to row[1], TuHoa.KHOA to row[2], TuHoa.KY to row[3])
    }
}
