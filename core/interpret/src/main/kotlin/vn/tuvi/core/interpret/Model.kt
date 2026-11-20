package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId

/** Chủ đề luận (mục 9, bảng “Chủ đề → cung đọc chính”). */
enum class Topic(val vn: String) {
    TINH_CACH("Tính cách"),
    SU_NGHIEP("Sự nghiệp"),
    TAI_CHINH("Tài chính"),
    TINH_DUYEN("Tình duyên, hôn nhân"),
    GIA_DAO("Gia đạo"),
    SUC_KHOE("Sức khỏe (lưu ý)"),
    DI_CHUYEN("Di chuyển, xuất ngoại"),
    NHA_DAT("Nhà đất"),
    PHAP_LY("Pháp lý, thị phi"),
    PHUC_DUC("Phúc đức, tâm linh"),
    ;

    companion object {
        /** Chủ đề chính của mỗi cung khi luận câu đơn sao. */
        fun ofRole(role: PalaceRole): Topic = when (role) {
            PalaceRole.MENH -> TINH_CACH
            PalaceRole.QUAN_LOC, PalaceRole.NO_BOC -> SU_NGHIEP
            PalaceRole.TAI_BACH -> TAI_CHINH
            PalaceRole.PHU_THE -> TINH_DUYEN
            PalaceRole.PHU_MAU, PalaceRole.HUYNH_DE, PalaceRole.TU_TUC -> GIA_DAO
            PalaceRole.TAT_ACH -> SUC_KHOE
            PalaceRole.THIEN_DI -> DI_CHUYEN
            PalaceRole.DIEN_TRACH -> NHA_DAT
            PalaceRole.PHUC_DUC -> PHUC_DUC
        }
    }
}

/** Phạm vi thời gian của một finding (mục 2.3: trọn đời → đại vận → năm → tháng → ngày). */
enum class Scope { GOC, DAI_HAN, NAM, THANG, NGAY }

/** Loại luật trong KB. */
enum class RuleType(val vn: String) {
    CAT_CACH("Cát cách"),
    HUNG_CACH("Hung cách"),
    PHAN_VI("Phản vi kỳ cách"),
    THE_SAO("Thế sao khó"),
    CUNG("Dấu hiệu theo cung"),
    VAN_HAN("Vận hạn"),
    CHUYEN_DE("Chuyên đề"),
}

/** Độ tin cậy (mục 10): A kinh điển, B phổ biến Nam phái VN, C quan điểm riêng (chỉ ở chế độ chuyên sâu). */
enum class Reliability { A, B, C }

/** Quan hệ giữa cung đang xét và cung chứa sao làm bằng chứng (mục 9 bước 1). */
enum class Relation(val vn: String, val weight: Double) {
    SELF("tọa thủ", 1.0),
    XUNG_CHIEU("xung chiếu", 0.8),
    TAM_HOP("tam hợp", 0.6),
    GIAP("giáp", 0.4),
    NHI_HOP("nhị hợp", 0.3),
    KHAC("vị trí khác", 0.0),
    ;

    companion object {
        fun between(anchor: Chi, at: Chi): Relation = when (Math.floorMod(at.ordinal - anchor.ordinal, 12)) {
            0 -> SELF
            6 -> XUNG_CHIEU
            4, 8 -> TAM_HOP
            1, 11 -> GIAP
            else -> if (Math.floorMod(1 - anchor.ordinal, 12) == at.ordinal) NHI_HOP else KHAC
        }
    }
}

/** Một bằng chứng của finding: sao (gốc hoặc lưu) ở cung nào, quan hệ gì với cung đang luận. */
data class Evidence(
    val star: StarId?,
    val chi: Chi,
    val role: PalaceRole,
    val relation: Relation,
    val luu: Boolean = false,
    val note: String? = null,
) {
    override fun toString(): String =
        (if (star != null) (if (luu) "L." else "") + star.vn + " " else "") + "tại ${chi.vn} (${role.vn}, ${relation.vn})" +
            (note?.let { " — $it" } ?: "")
}

/**
 * Kết quả luận của một luật trên một cung (mục 3). [palace] là cung neo của luật; [priority] = số predicate lá,
 * luật cụ thể hơn thắng khi cùng sao, cùng chủ đề (mục 9).
 */
data class Finding(
    val ruleId: String,
    val scope: Scope,
    val topic: Topic,
    val polarity: Int,
    val weight: Double,
    val evidence: List<Evidence>,
    val textKey: String,
    val adviceKeys: List<String>,
    val palace: Chi,
    val priority: Int,
    val type: RuleType?,
    val reliability: Reliability,
    val reviewed: Boolean,
)

/** Tác động của luật khi kích hoạt. Mẫu `suppress`/`soften`/`invert` khớp ruleId của finding khác (có `*` và `@SELF`). */
data class Effect(
    val topics: List<Topic>,
    /** `"@PALACE"` trong topics: chủ đề lấy theo vai trò của cung neo (vd. Song Kỵ ở cung Tài → tài chính). */
    val byPalace: Boolean = false,
    val polarity: Int,
    val weight: Double,
    val suppress: List<String> = emptyList(),
    val soften: List<String> = emptyList(),
    /** Phản vi (xấu hóa tốt): finding đang âm thành dương cùng độ lớn; finding đã dương giữ nguyên. */
    val invert: List<String> = emptyList(),
    /** Sự kiện cụ thể mà luật chỉ báo (quy tắc đồng thanh tương ứng, mục 9). */
    val event: Event? = null,
)

/**
 * Sự kiện cụ thể dùng cho quy tắc “đồng thanh tương ứng” (mục 9): ≥ 3 tầng cùng chỉ → “khả năng cao”,
 * 2 tầng → “có thể”, 1 tầng → “dấu hiệu, nên lưu ý”. [polarity]: chiều của chính sự kiện.
 */
enum class Event(val vn: String, val short: String, val topic: Topic, val polarity: Int) {
    CUOI_HOI("chuyện tình cảm, cưới hỏi", "tình cảm, cưới hỏi", Topic.TINH_DUYEN, 1),
    SINH_CON("chuyện con cái, đón thành viên mới", "con cái", Topic.GIA_DAO, 1),
    THANG_TIEN("thăng tiến, nhận trách nhiệm mới", "thăng tiến", Topic.SU_NGHIEP, 1),
    DOI_VIEC("thay đổi công việc, môi trường làm việc", "đổi việc", Topic.SU_NGHIEP, 0),
    DUOC_TAI("tài lộc, dòng tiền cải thiện", "tài lộc", Topic.TAI_CHINH, 1),
    HAO_TAI("hao tài, chi phí ngoài dự tính", "hao tài", Topic.TAI_CHINH, -1),
    PHAP_LY("vướng giấy tờ, thị phi, va chạm pháp lý", "giấy tờ, thị phi", Topic.PHAP_LY, -1),
    DI_CHUYEN("đi xa, di chuyển, đổi chỗ ở hoặc chỗ làm", "đi xa", Topic.DI_CHUYEN, 0),
    SUC_KHOE("cần chăm sóc sức khỏe, an toàn kỹ hơn", "sức khỏe", Topic.SUC_KHOE, -1),
}

/** Mức khả năng theo số tầng cùng chỉ (mục 9). */
enum class Likelihood(val key: String, val vn: String) {
    CAO("cao", "khả năng cao"),
    CO_THE("co_the", "có thể"),
    DAU_HIEU("dau_hieu", "mới là dấu hiệu");

    companion object {
        fun of(layers: Int): Likelihood? = when {
            layers >= 3 -> CAO
            layers == 2 -> CO_THE
            layers == 1 -> DAU_HIEU
            else -> null
        }
    }
}

/** Các tầng vận hạn (mục 8, mục 9 bước 6). */
enum class Layer(val vn: String) {
    GOC("lá số gốc"),
    DAI_HAN("đại hạn"),
    LUU_NIEN("lưu niên đại hạn"),
    TIEU_HAN("tiểu hạn"),
    LUU_THAI_TUE("Lưu Thái Tuế, sao lưu, Tứ Hóa lưu"),
}

/** Thế của giai đoạn theo điểm tổng (mục 11 bước 4). */
enum class Stance(val vn: String, val key: String) {
    TAN("Tấn", "tan"), GIU("Giữ", "giu"), THU("Thủ", "thu"), LUI("Lùi", "lui");

    companion object {
        fun of(score: Int): Stance = when {
            score >= 70 -> TAN
            score >= 50 -> GIU
            score >= 35 -> THU
            else -> LUI
        }
    }
}

/**
 * Khuyến nghị dự phòng theo chủ đề: dùng khi chủ đề mạnh (STRONG) hay yếu (WEAK), theo thế giai đoạn (STANCE),
 * hoặc cho lá số trẻ nhỏ dưới 16 tuổi âm (CHILD, D-39).
 */
enum class AdviceUse { STRONG, WEAK, STANCE, CHILD }

/** Cung neo của luật. */
sealed class Anchor {
    data class Roles(val roles: List<PalaceRole>) : Anchor()
    data object AnyPalace : Anchor()
    data object ThanPalace : Anchor()
    /** Tầng vận hạn (chỉ luật phạm vi năm): cung đại hạn, tiểu hạn, lưu niên đại hạn, Lưu Thái Tuế. */
    data class Layer(val layer: LimitPoint) : Anchor()
}

/** Các điểm vận hạn dùng làm neo hoặc trong predicate `at`. */
enum class LimitPoint(val key: String) {
    DAI_HAN("daiHan"), TIEU_HAN("tieuHan"), LUU_NIEN_DAI_HAN("luuNienDaiHan"), LUU_THAI_TUE("luuThaiTue");

    companion object {
        fun of(key: String): LimitPoint = entries.firstOrNull { it.key == key } ?: throw KbException("Không rõ tầng vận hạn: $key")
    }
}

data class Rule(
    val id: String,
    val name: String,
    val type: RuleType,
    val scope: Scope,
    val reliability: Reliability,
    val anchor: Anchor,
    val condition: Predicate,
    val effect: Effect,
    val textKey: String,
    val adviceKeys: List<String>,
    val source: String,
    val reviewed: Boolean,
    val expertOnly: Boolean,
    val file: String,
) {
    /** Độ ưu tiên = số predicate lá (mục 9). */
    val priority: Int get() = condition.leafCount()
}

/** Câu luận mẫu. Slot hợp lệ: {cung} {sao} {doSang} {tuoi} {nam} (mục 9 bước 7). */
data class TextEntry(val key: String, val text: String, val source: String, val reviewed: Boolean, val file: String)

enum class AdviceType { NEN, KHONG_NEN, LUU_Y }

/** Khuyến nghị (mục 11): hành động cụ thể, có điều kiện, không gây sợ hãi. */
data class AdviceEntry(
    val key: String,
    val type: AdviceType,
    val topic: Topic,
    val text: String,
    val severity: Int,
    val source: String,
    val reviewed: Boolean,
    /** Chỉ dùng khi thế giai đoạn thuộc tập này (rỗng = mọi thế). */
    val stances: Set<Stance> = emptySet(),
    /** Dùng làm khuyến nghị dự phòng khi các finding không đủ 3 mục (mục 11). */
    val uses: Set<AdviceUse> = emptySet(),
)

/** Câu trả lời mẫu cho câu hỏi thường gặp (mục 12.5, thư mục `faq/`). */
data class FaqEntry(
    val id: String,
    val question: String,
    val answer: String,
    val topics: List<Topic>,
    val relatedRules: List<String>,
    val source: String,
    val reviewed: Boolean,
)

class KbException(message: String) : RuntimeException(message)
