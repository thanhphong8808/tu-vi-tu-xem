package vn.tuvi.core.engine

/**
 * Nhóm sao. Thứ tự khai báo = thứ tự hiển thị trong cung (mục 5.7):
 * chính tinh → Tứ Hóa → lục cát → Lộc Tồn, Thiên Mã → lục sát → trung tinh cát → trung tinh hung → sao vòng.
 */
enum class StarKind(val vn: String) {
    CHINH_TINH("Chính tinh"),
    TU_HOA("Tứ Hóa"),
    LUC_CAT("Lục cát tinh"),
    LOC_MA("Lộc Tồn, Thiên Mã"),
    LUC_SAT("Lục sát tinh"),
    TRUNG_TINH_CAT("Trung tinh cát"),
    TRUNG_TINH_HUNG("Trung tinh hung"),
    VONG_BAC_SI("Vòng Bác Sĩ"),
    VONG_THAI_TUE("Vòng Thái Tuế"),
    VONG_TRANG_SINH("Vòng Tràng Sinh"),
}

/**
 * Danh mục sao an trong lá số gốc (109 sao). Thứ tự khai báo chính là thứ tự hiển thị trong một cung,
 * nên sắp xếp sao trong cung chỉ cần theo `ordinal`.
 *
 * Lưu ý hai sao gần tên: [QUAN_PHU] là **Quan Phù** (vòng Thái Tuế, sao pháp lý — thị phi),
 * [QUAN_PHU_BS] là **Quan Phủ** (sao cuối vòng Bác Sĩ).
 * [PHUC_DUC] là sao Phúc Đức của vòng Thái Tuế, khác với cung Phúc Đức ([PalaceRole.PHUC_DUC]).
 */
enum class StarId(val vn: String, val kind: StarKind) {
    // Chính tinh — nhóm Tử Vi rồi nhóm Thiên Phủ.
    TU_VI("Tử Vi", StarKind.CHINH_TINH),
    THIEN_CO("Thiên Cơ", StarKind.CHINH_TINH),
    THAI_DUONG("Thái Dương", StarKind.CHINH_TINH),
    VU_KHUC("Vũ Khúc", StarKind.CHINH_TINH),
    THIEN_DONG("Thiên Đồng", StarKind.CHINH_TINH),
    LIEM_TRINH("Liêm Trinh", StarKind.CHINH_TINH),
    THIEN_PHU("Thiên Phủ", StarKind.CHINH_TINH),
    THAI_AM("Thái Âm", StarKind.CHINH_TINH),
    THAM_LANG("Tham Lang", StarKind.CHINH_TINH),
    CU_MON("Cự Môn", StarKind.CHINH_TINH),
    THIEN_TUONG("Thiên Tướng", StarKind.CHINH_TINH),
    THIEN_LUONG("Thiên Lương", StarKind.CHINH_TINH),
    THAT_SAT("Thất Sát", StarKind.CHINH_TINH),
    PHA_QUAN("Phá Quân", StarKind.CHINH_TINH),

    // Tứ Hóa (gốc, theo can năm sinh).
    HOA_LOC("Hóa Lộc", StarKind.TU_HOA),
    HOA_QUYEN("Hóa Quyền", StarKind.TU_HOA),
    HOA_KHOA("Hóa Khoa", StarKind.TU_HOA),
    HOA_KY("Hóa Kỵ", StarKind.TU_HOA),

    // Lục cát.
    TA_PHU("Tả Phù", StarKind.LUC_CAT),
    HUU_BAT("Hữu Bật", StarKind.LUC_CAT),
    VAN_XUONG("Văn Xương", StarKind.LUC_CAT),
    VAN_KHUC("Văn Khúc", StarKind.LUC_CAT),
    THIEN_KHOI("Thiên Khôi", StarKind.LUC_CAT),
    THIEN_VIET("Thiên Việt", StarKind.LUC_CAT),

    // Lộc Tồn, Thiên Mã.
    LOC_TON("Lộc Tồn", StarKind.LOC_MA),
    THIEN_MA("Thiên Mã", StarKind.LOC_MA),

    // Lục sát.
    KINH_DUONG("Kình Dương", StarKind.LUC_SAT),
    DA_LA("Đà La", StarKind.LUC_SAT),
    HOA_TINH("Hỏa Tinh", StarKind.LUC_SAT),
    LINH_TINH("Linh Tinh", StarKind.LUC_SAT),
    DIA_KHONG("Địa Không", StarKind.LUC_SAT),
    DIA_KIEP("Địa Kiếp", StarKind.LUC_SAT),

    // Trung tinh cát (phân loại hiển thị tạm, KB giai đoạn 3 có thể tinh chỉnh — DECISIONS D-10).
    THIEN_QUAN("Thiên Quan", StarKind.TRUNG_TINH_CAT),
    THIEN_PHUC("Thiên Phúc", StarKind.TRUNG_TINH_CAT),
    VAN_TINH("Văn Tinh", StarKind.TRUNG_TINH_CAT),
    DUONG_PHU("Đường Phù", StarKind.TRUNG_TINH_CAT),
    QUOC_AN("Quốc Ấn", StarKind.TRUNG_TINH_CAT),
    THAI_PHU("Thai Phụ", StarKind.TRUNG_TINH_CAT),
    PHONG_CAO("Phong Cáo", StarKind.TRUNG_TINH_CAT),
    TAM_THAI("Tam Thai", StarKind.TRUNG_TINH_CAT),
    BAT_TOA("Bát Tọa", StarKind.TRUNG_TINH_CAT),
    AN_QUANG("Ân Quang", StarKind.TRUNG_TINH_CAT),
    THIEN_QUY("Thiên Quý", StarKind.TRUNG_TINH_CAT),
    LONG_TRI("Long Trì", StarKind.TRUNG_TINH_CAT),
    PHUONG_CAC("Phượng Các", StarKind.TRUNG_TINH_CAT),
    GIAI_THAN("Giải Thần", StarKind.TRUNG_TINH_CAT),
    THIEN_GIAI("Thiên Giải", StarKind.TRUNG_TINH_CAT),
    DIA_GIAI("Địa Giải", StarKind.TRUNG_TINH_CAT),
    HONG_LOAN("Hồng Loan", StarKind.TRUNG_TINH_CAT),
    THIEN_HY("Thiên Hỷ", StarKind.TRUNG_TINH_CAT),
    DAO_HOA("Đào Hoa", StarKind.TRUNG_TINH_CAT),
    THIEN_TAI("Thiên Tài", StarKind.TRUNG_TINH_CAT),
    THIEN_THO("Thiên Thọ", StarKind.TRUNG_TINH_CAT),
    THIEN_DUC("Thiên Đức", StarKind.TRUNG_TINH_CAT),
    NGUYET_DUC("Nguyệt Đức", StarKind.TRUNG_TINH_CAT),
    THIEN_Y("Thiên Y", StarKind.TRUNG_TINH_CAT),
    THIEN_TRU("Thiên Trù", StarKind.TRUNG_TINH_CAT),
    HOA_CAI("Hoa Cái", StarKind.TRUNG_TINH_CAT),

    // Trung tinh hung.
    THIEN_HINH("Thiên Hình", StarKind.TRUNG_TINH_HUNG),
    THIEN_RIEU("Thiên Riêu", StarKind.TRUNG_TINH_HUNG),
    THIEN_KHOC("Thiên Khốc", StarKind.TRUNG_TINH_HUNG),
    THIEN_HU("Thiên Hư", StarKind.TRUNG_TINH_HUNG),
    CO_THAN("Cô Thần", StarKind.TRUNG_TINH_HUNG),
    QUA_TU("Quả Tú", StarKind.TRUNG_TINH_HUNG),
    KIEP_SAT("Kiếp Sát", StarKind.TRUNG_TINH_HUNG),
    PHA_TOAI("Phá Toái", StarKind.TRUNG_TINH_HUNG),
    LUU_HA("Lưu Hà", StarKind.TRUNG_TINH_HUNG),
    DAU_QUAN("Đẩu Quân", StarKind.TRUNG_TINH_HUNG),
    THIEN_KHONG("Thiên Không", StarKind.TRUNG_TINH_HUNG),
    THIEN_LA("Thiên La", StarKind.TRUNG_TINH_HUNG),
    DIA_VONG("Địa Võng", StarKind.TRUNG_TINH_HUNG),
    THIEN_THUONG("Thiên Thương", StarKind.TRUNG_TINH_HUNG),
    THIEN_SU("Thiên Sứ", StarKind.TRUNG_TINH_HUNG),

    // Vòng Bác Sĩ (khởi tại Lộc Tồn, đi theo dir).
    BAC_SI("Bác Sĩ", StarKind.VONG_BAC_SI),
    LUC_SI("Lực Sĩ", StarKind.VONG_BAC_SI),
    THANH_LONG("Thanh Long", StarKind.VONG_BAC_SI),
    TIEU_HAO("Tiểu Hao", StarKind.VONG_BAC_SI),
    TUONG_QUAN("Tướng Quân", StarKind.VONG_BAC_SI),
    TAU_THU("Tấu Thư", StarKind.VONG_BAC_SI),
    PHI_LIEM("Phi Liêm", StarKind.VONG_BAC_SI),
    HY_THAN("Hỷ Thần", StarKind.VONG_BAC_SI),
    BENH_PHU("Bệnh Phù", StarKind.VONG_BAC_SI),
    DAI_HAO("Đại Hao", StarKind.VONG_BAC_SI),
    PHUC_BINH("Phục Binh", StarKind.VONG_BAC_SI),
    QUAN_PHU_BS("Quan Phủ", StarKind.VONG_BAC_SI),

    // Vòng Thái Tuế (khởi tại chi năm sinh, luôn thuận).
    THAI_TUE("Thái Tuế", StarKind.VONG_THAI_TUE),
    THIEU_DUONG("Thiếu Dương", StarKind.VONG_THAI_TUE),
    TANG_MON("Tang Môn", StarKind.VONG_THAI_TUE),
    THIEU_AM("Thiếu Âm", StarKind.VONG_THAI_TUE),
    QUAN_PHU("Quan Phù", StarKind.VONG_THAI_TUE),
    TU_PHU("Tử Phù", StarKind.VONG_THAI_TUE),
    TUE_PHA("Tuế Phá", StarKind.VONG_THAI_TUE),
    LONG_DUC("Long Đức", StarKind.VONG_THAI_TUE),
    BACH_HO("Bạch Hổ", StarKind.VONG_THAI_TUE),
    PHUC_DUC("Phúc Đức", StarKind.VONG_THAI_TUE),
    DIEU_KHACH("Điếu Khách", StarKind.VONG_THAI_TUE),
    TRUC_PHU("Trực Phù", StarKind.VONG_THAI_TUE),

    // Vòng Tràng Sinh (khởi theo hành Cục, đi theo dir).
    TRANG_SINH("Tràng Sinh", StarKind.VONG_TRANG_SINH),
    MOC_DUC("Mộc Dục", StarKind.VONG_TRANG_SINH),
    QUAN_DOI("Quan Đới", StarKind.VONG_TRANG_SINH),
    LAM_QUAN("Lâm Quan", StarKind.VONG_TRANG_SINH),
    DE_VUONG("Đế Vượng", StarKind.VONG_TRANG_SINH),
    SUY("Suy", StarKind.VONG_TRANG_SINH),
    BENH("Bệnh", StarKind.VONG_TRANG_SINH),
    TU("Tử", StarKind.VONG_TRANG_SINH),
    MO("Mộ", StarKind.VONG_TRANG_SINH),
    TUYET("Tuyệt", StarKind.VONG_TRANG_SINH),
    THAI("Thai", StarKind.VONG_TRANG_SINH),
    DUONG("Dưỡng", StarKind.VONG_TRANG_SINH);

    override fun toString(): String = vn

    companion object {
        private val BY_VN: Map<String, StarId> = entries.associateBy { java.text.Normalizer.normalize(it.vn, java.text.Normalizer.Form.NFC) }

        /** Tra sao theo tên tiếng Việt có dấu (đã chuẩn hóa NFC). */
        fun fromVn(name: String): StarId =
            BY_VN[java.text.Normalizer.normalize(name.trim(), java.text.Normalizer.Form.NFC)]
                ?: throw IllegalArgumentException("Không rõ sao: $name")

        val CHINH_TINH: List<StarId> = entries.filter { it.kind == StarKind.CHINH_TINH }
        val BAC_SI_RING: List<StarId> = entries.filter { it.kind == StarKind.VONG_BAC_SI }
        val THAI_TUE_RING: List<StarId> = entries.filter { it.kind == StarKind.VONG_THAI_TUE }
        val TRANG_SINH_RING: List<StarId> = entries.filter { it.kind == StarKind.VONG_TRANG_SINH }
    }
}

/** Bốn hóa. */
enum class TuHoa(val vn: String, val star: StarId) {
    LOC("Hóa Lộc", StarId.HOA_LOC),
    QUYEN("Hóa Quyền", StarId.HOA_QUYEN),
    KHOA("Hóa Khoa", StarId.HOA_KHOA),
    KY("Hóa Kỵ", StarId.HOA_KY),
}
