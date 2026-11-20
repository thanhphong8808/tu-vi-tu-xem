package vn.tuvi.app.api

import vn.tuvi.core.engine.NguHanh
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind

/**
 * Thông tin hiển thị của sao (chỉ dùng cho giao diện, không tham gia an sao hay luận giải):
 * vị trí trong ô cung (mục 13: chính tinh, Tứ Hóa, cột cát trái, cột hung phải, vòng Tràng Sinh ở chân ô)
 * và ngũ hành để tô màu. Ngũ hành 14 chính tinh theo bảng cẩm nang V2 (Tham Lang “Thủy đới Mộc” → Thủy);
 * phụ tinh theo cách ghi phổ biến của sách Nam phái — giai đoạn 3 (knowledge base) có thể tinh chỉnh (DECISIONS D-29).
 */
object StarStyle {

    enum class Side(val key: String) { MAIN("main"), HOA("hoa"), GOOD("good"), BAD("bad"), RING("ring") }

    private val GOOD_BAC_SI = setOf(
        StarId.BAC_SI, StarId.LUC_SI, StarId.THANH_LONG, StarId.TUONG_QUAN, StarId.TAU_THU, StarId.HY_THAN,
    )
    private val GOOD_THAI_TUE = setOf(StarId.THIEU_DUONG, StarId.THIEU_AM, StarId.LONG_DUC, StarId.PHUC_DUC)

    fun side(star: StarId): Side = when (star.kind) {
        StarKind.CHINH_TINH -> Side.MAIN
        StarKind.TU_HOA -> Side.HOA
        StarKind.LUC_CAT, StarKind.LOC_MA, StarKind.TRUNG_TINH_CAT -> Side.GOOD
        StarKind.LUC_SAT, StarKind.TRUNG_TINH_HUNG -> Side.BAD
        StarKind.VONG_BAC_SI -> if (star in GOOD_BAC_SI) Side.GOOD else Side.BAD
        StarKind.VONG_THAI_TUE -> if (star in GOOD_THAI_TUE) Side.GOOD else Side.BAD
        StarKind.VONG_TRANG_SINH -> Side.RING
    }

    private val K = NguHanh.KIM
    private val M = NguHanh.MOC
    private val T = NguHanh.THUY
    private val H = NguHanh.HOA
    private val O = NguHanh.THO

    private val ELEMENT: Map<StarId, NguHanh> = mapOf(
        StarId.TU_VI to O, StarId.THIEN_CO to M, StarId.THAI_DUONG to H, StarId.VU_KHUC to K,
        StarId.THIEN_DONG to T, StarId.LIEM_TRINH to H, StarId.THIEN_PHU to O, StarId.THAI_AM to T,
        StarId.THAM_LANG to T, StarId.CU_MON to T, StarId.THIEN_TUONG to T, StarId.THIEN_LUONG to M,
        StarId.THAT_SAT to K, StarId.PHA_QUAN to T,
        StarId.HOA_LOC to M, StarId.HOA_QUYEN to M, StarId.HOA_KHOA to T, StarId.HOA_KY to T,
        StarId.TA_PHU to O, StarId.HUU_BAT to O, StarId.VAN_XUONG to K, StarId.VAN_KHUC to T,
        StarId.THIEN_KHOI to H, StarId.THIEN_VIET to H, StarId.LOC_TON to O, StarId.THIEN_MA to H,
        StarId.KINH_DUONG to K, StarId.DA_LA to K, StarId.HOA_TINH to H, StarId.LINH_TINH to H,
        StarId.DIA_KHONG to H, StarId.DIA_KIEP to H,
        StarId.THIEN_QUAN to H, StarId.THIEN_PHUC to O, StarId.VAN_TINH to H, StarId.DUONG_PHU to M,
        StarId.QUOC_AN to O, StarId.THAI_PHU to K, StarId.PHONG_CAO to O, StarId.TAM_THAI to T,
        StarId.BAT_TOA to M, StarId.AN_QUANG to M, StarId.THIEN_QUY to O, StarId.LONG_TRI to T,
        StarId.PHUONG_CAC to M, StarId.GIAI_THAN to M, StarId.THIEN_GIAI to H, StarId.DIA_GIAI to O,
        StarId.HONG_LOAN to T, StarId.THIEN_HY to T, StarId.DAO_HOA to M, StarId.THIEN_TAI to O,
        StarId.THIEN_THO to O, StarId.THIEN_DUC to H, StarId.NGUYET_DUC to H, StarId.THIEN_Y to T,
        StarId.THIEN_TRU to O, StarId.HOA_CAI to K,
        StarId.THIEN_HINH to H, StarId.THIEN_RIEU to T, StarId.THIEN_KHOC to K, StarId.THIEN_HU to T,
        StarId.CO_THAN to O, StarId.QUA_TU to O, StarId.KIEP_SAT to H, StarId.PHA_TOAI to H,
        StarId.LUU_HA to T, StarId.DAU_QUAN to H, StarId.THIEN_KHONG to H, StarId.THIEN_LA to O,
        StarId.DIA_VONG to O, StarId.THIEN_THUONG to O, StarId.THIEN_SU to T,
        StarId.BAC_SI to T, StarId.LUC_SI to H, StarId.THANH_LONG to T, StarId.TIEU_HAO to H,
        StarId.TUONG_QUAN to M, StarId.TAU_THU to K, StarId.PHI_LIEM to H, StarId.HY_THAN to H,
        StarId.BENH_PHU to O, StarId.DAI_HAO to H, StarId.PHUC_BINH to H, StarId.QUAN_PHU_BS to H,
        StarId.THAI_TUE to H, StarId.THIEU_DUONG to H, StarId.TANG_MON to M, StarId.THIEU_AM to T,
        StarId.QUAN_PHU to H, StarId.TU_PHU to K, StarId.TUE_PHA to H, StarId.LONG_DUC to T,
        StarId.BACH_HO to K, StarId.PHUC_DUC to O, StarId.DIEU_KHACH to H, StarId.TRUC_PHU to K,
    )

    /** Ngũ hành để tô màu; null với vòng Tràng Sinh (hành đi theo Cục). */
    fun element(star: StarId): NguHanh? = ELEMENT[star]

    fun elementKey(e: NguHanh?): String? = when (e) {
        NguHanh.KIM -> "kim"; NguHanh.MOC -> "moc"; NguHanh.THUY -> "thuy"; NguHanh.HOA -> "hoa"; NguHanh.THO -> "tho"
        null -> null
    }
}
