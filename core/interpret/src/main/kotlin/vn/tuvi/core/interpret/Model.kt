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
    val invert: List<String> = emptyList(),
)

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
