package vn.tuvi.core.engine

/** Hàng Canh của bảng Tứ Hóa. */
enum class TuHoaVariant(val vn: String) {
    /** Canh: Dương Lộc, Vũ Quyền, Âm Khoa, Đồng Kỵ (khớp cẩm nang). */
    NAM_PHAI_VN("Nam phái VN"),
    /** Canh: Dương Lộc, Vũ Quyền, Đồng Khoa, Âm Kỵ. */
    THIEN_LUONG("Theo cụ Thiên Lương"),
}

/** Sao hóa Khoa của năm Nhâm (tách khỏi [TuHoaVariant] để chỉnh độc lập — DECISIONS D-07). */
enum class NhamHoaKhoa(val vn: String, val star: StarId) {
    TA_PHU("Tả Phù", StarId.TA_PHU),
    THIEN_PHU("Thiên Phủ", StarId.THIEN_PHU),
}

enum class KhoiVietVariant(val vn: String) {
    /** Canh Tân: Khôi Ngọ, Việt Dần. */
    VN_PHO_THONG("Phổ thông VN"),
    /** “Giáp Mậu Canh ngưu dương”: Canh về Sửu/Mùi. */
    TOAN_THU("Toàn Thư"),
}

enum class HoaLinhVariant(val vn: String) {
    /** Bảng khởi mục 5.6, Hỏa đi theo dir, Linh đi ngược dir. */
    THAI_THU_LANG("Thái Thứ Lang"),
    /** Như Thái Thứ Lang nhưng nhóm Tỵ Dậu Sửu: Hỏa khởi Tuất, Linh khởi Mão. */
    DAC_LOC("Đắc Lộc"),
    /** Bảng khởi Thái Thứ Lang, cả hai đếm thuận, không xét Âm Dương Nam Nữ. */
    TRUNG_CHAU("Trung Châu"),
}

enum class ThienGiaiVariant(val vn: String) {
    /** Thiên Giải = Thân + (tháng − 1). */
    THUAN("Đếm thuận"),
    /** Thiên Giải = Thân + 2·(tháng − 1). */
    NHAY_CUNG("Nhảy cung"),
}

enum class LuuHaVariant(val vn: String) {
    TOAN_THU("Toàn Thư"),
    /** Như Toàn Thư nhưng Đinh → Thìn, Canh → Thân (theo mã nguồn lasotuvi). */
    LASOTUVI("lasotuvi"),
}

enum class LeapMonthRule(val vn: String) {
    /** Tháng nhuận tính như tháng chính. */
    SAME_MONTH("Tính như tháng chính"),
    /** Ngày 1–15 tính tháng chính, từ ngày 16 tính tháng sau. */
    SPLIT_15("Chia đôi tháng nhuận"),
}

enum class DayBoundary(val vn: String) {
    /** Sinh 23:00–23:59 tính sang ngày hôm sau. */
    DOI_NGAY_23H("Đổi ngày lúc 23:00"),
    /** Giữ nguyên ngày (đổi ngày lúc 00:00). */
    GIU_NGAY("Giữ nguyên ngày"),
}

/** Cách an nguyệt hạn — dùng ở LimitEngine (Giai đoạn 2), không ảnh hưởng lá số gốc. */
enum class NguyetHanVariant(val vn: String) {
    TIEU_HAN_NGHICH_THANG_THUAN_GIO("Từ tiểu hạn: nghịch tháng, thuận giờ"),
    TIEU_HAN_THUAN_THANG_THUAN_GIO("Từ tiểu hạn: thuận tháng, thuận giờ"),
    TIEU_HAN_LA_THANG_GIENG("Cung tiểu hạn là tháng Giêng"),
    DAU_QUAN_LUU_THAI_TUE("Đẩu Quân lưu từ Lưu Thái Tuế"),
}

enum class CuMonTable(val vn: String) {
    /** Tý/Ngọ Vượng, Thân Đắc. */
    THAI_THU_LANG("Thái Thứ Lang"),
    /** Tý/Ngọ Đắc, Thân Vượng. */
    CAM_NANG_V2("Cẩm nang V2"),
}

/** Tứ Hóa theo can đại hạn — dùng ở LimitEngine (Giai đoạn 2). */
enum class DaiHanTuHoa(val vn: String) { OFF("Tắt"), ON("Bật") }

enum class ThanChuTy(val vn: String, val star: StarId) {
    LINH_TINH("Linh Tinh", StarId.LINH_TINH),
    HOA_TINH("Hỏa Tinh", StarId.HOA_TINH),
}

/**
 * Các điểm sách vở vênh nhau là cấu hình, không phải hằng số (nguyên tắc 4).
 * Mặc định = Nam phái VN (Thái Thứ Lang – Tử Vi Đẩu Số Tân Biên), khớp cẩm nang V2.
 */
data class SchoolConfig(
    val tuHoa: TuHoaVariant = TuHoaVariant.NAM_PHAI_VN,
    val nhamKhoa: NhamHoaKhoa = NhamHoaKhoa.TA_PHU,
    val khoiViet: KhoiVietVariant = KhoiVietVariant.VN_PHO_THONG,
    val hoaLinh: HoaLinhVariant = HoaLinhVariant.THAI_THU_LANG,
    val thienGiai: ThienGiaiVariant = ThienGiaiVariant.THUAN,
    val luuHa: LuuHaVariant = LuuHaVariant.TOAN_THU,
    val leapMonth: LeapMonthRule = LeapMonthRule.SAME_MONTH,
    val dayBoundary: DayBoundary = DayBoundary.DOI_NGAY_23H,
    val nguyetHan: NguyetHanVariant = NguyetHanVariant.TIEU_HAN_NGHICH_THANG_THUAN_GIO,
    val cuMonTable: CuMonTable = CuMonTable.THAI_THU_LANG,
    val daiHanTuHoa: DaiHanTuHoa = DaiHanTuHoa.OFF,
    val thanChuTy: ThanChuTy = ThanChuTy.LINH_TINH,
) {
    /** Preset khớp hoàn toàn với cấu hình này, hoặc null nếu đã chỉnh tay. */
    val preset: SchoolPreset? get() = SchoolPreset.entries.firstOrNull { it.config == this }

    /** Nhãn ghi trên lá số và bản xuất PDF. */
    val label: String get() = preset?.vn ?: "Tùy chỉnh"

    companion object {
        val DEFAULT: SchoolConfig = SchoolConfig()
    }
}

/** Ba preset trong Cài đặt → “Trường phái” (mục 7). */
enum class SchoolPreset(val vn: String, val config: SchoolConfig) {
    NAM_PHAI_VN("Nam phái VN", SchoolConfig()),
    THIEN_LUONG(
        "Theo cụ Thiên Lương",
        SchoolConfig(tuHoa = TuHoaVariant.THIEN_LUONG, nhamKhoa = NhamHoaKhoa.THIEN_PHU),
    ),
    TOAN_THU_TRUNG_CHAU(
        "Toàn Thư / Trung Châu",
        SchoolConfig(
            khoiViet = KhoiVietVariant.TOAN_THU,
            hoaLinh = HoaLinhVariant.TRUNG_CHAU,
            nguyetHan = NguyetHanVariant.DAU_QUAN_LUU_THAI_TUE,
            thanChuTy = ThanChuTy.HOA_TINH,
        ),
    ),
}
