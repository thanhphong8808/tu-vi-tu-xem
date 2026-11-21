package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Chi

/** Phạm vi một bản luận (mục 2.3). */
enum class ReadingScope(val vn: String) { TRON_DOI("Trọn đời"), DAI_VAN("Đại vận"), NAM("Năm"), THANG("Tháng"), NGAY("Ngày") }

/**
 * Loại đoạn: LEAD câu mở chủ đề (điểm + mức), PLAIN câu luận một chiều, WEIGH câu cân nhắc gộp hai chiều
 * (mục 9 bước 7), EVENT câu sự kiện theo đồng thanh tương ứng, NOTE ghi chú trung tính.
 */
enum class ParaKind { LEAD, PLAIN, WEIGH, EVENT, NOTE }

/** Một căn cứ trong ngăn “Vì sao?”: luật/sao/điểm, câu luận gốc, bằng chứng, nguồn. */
data class Why(val id: String, val title: String, val detail: String, val evidence: List<String>, val source: String = "")

/** Một đoạn luận. [ruleIds] luôn có ít nhất một mã (mục 14.4); [polarity] dùng để kiểm câu trái ngược. */
data class Paragraph(
    val text: String,
    val polarity: Int,
    val kind: ParaKind,
    val topic: Topic?,
    val ruleIds: List<String>,
    val why: List<Why>,
    /** Có câu chữ lấy từ KB chưa duyệt → app gắn nhãn “bản nháp”. */
    val draft: Boolean,
)

data class Section(val id: String, val title: String, val topic: Topic?, val score: Int?, val subtitle: String?, val paragraphs: List<Paragraph>)

/** Một khuyến nghị (mục 11) kèm lý do chọn. */
data class AdviceItem(val key: String, val type: AdviceType, val topic: Topic, val text: String, val because: String, val ruleIds: List<String>)

data class MonthScore(val month: Int, val score: Int, val chi: Chi?, val role: String?)

/** Điểm một ngày dương lịch (mục 2.3 tầng 6). */
data class DayScore(
    val date: java.time.LocalDate, val lunarDay: Int, val lunarMonth: Int, val lunarLeap: Boolean, val lunarYear: Int,
    val canChi: String, val score: Int, val nhatHan: Chi?, val role: String?,
)

/** Điểm một giờ (canh) trong ngày theo cung thời hạn. */
data class HourScore(val chi: Chi, val label: String, val score: Int, val palace: Chi, val role: String)

/** Quan hệ khi so hai lá số (cẩm nang P.V.4). */
enum class PairRelation(val vn: String) { VO_CHONG("Vợ chồng"), CHA_ME_CON("Cha mẹ – con") }

/** Kết quả so hai lá số: truyền cung, truyền tinh. */
data class Comparison(val title: String, val relation: PairRelation, val paragraphs: List<Paragraph>, val closing: String)

/** Một giờ ứng viên khi không rõ giờ sinh, xếp theo mức khớp với các sự kiện đã qua. */
data class HourRank(val canh: vn.tuvi.core.engine.CanhGio, val score: Int, val details: List<String>)

/** Thẻ sao (mục 13: chạm giữ một sao). */
data class StarCard(
    val star: vn.tuvi.core.engine.StarId, val kind: String, val chi: Chi, val role: String, val brightness: String?,
    val meta: MainMeta?, val natalHoa: List<String>, val meaning: List<Paragraph>, val patterns: List<Why>,
)

/** Một ô trên dòng thời gian 12 đại vận. */
data class DecadeItem(
    val index: Int, val from: Int, val to: Int, val chi: Chi, val role: String, val canChi: String, val score: Int, val stance: Stance,
    val thienThoi: String, val startYear: Int, val current: Boolean,
)

/** Bản luận hoàn chỉnh cho một phạm vi thời gian. */
data class Reading(
    val scope: ReadingScope,
    val title: String,
    val subtitle: String,
    val score: Int,
    val stance: Stance,
    val stanceText: String,
    val summary: List<Paragraph>,
    val sections: List<Section>,
    val palaces: List<Section>,
    val timeline: List<DecadeItem>,
    val advice: List<AdviceItem>,
    val goodMonths: List<Int>,
    val cautionMonths: List<Int>,
    val months: List<MonthScore>,
    val closing: String,
    val kbVersion: String,
    val hours: List<HourScore> = emptyList(),
) {
    /** Mọi đoạn kèm mã khối chứa nó (để kiểm thử). */
    fun allParagraphs(): List<Pair<String, Paragraph>> =
        summary.map { "summary" to it } + (sections + palaces).flatMap { s -> s.paragraphs.map { s.id to it } }

    /** Toàn văn bản luận (để quét từ cấm, snapshot). */
    fun fullText(): String = buildString {
        appendLine(title); appendLine(subtitle); appendLine(stanceText)
        for (p in summary) appendLine(p.text)
        for (s in sections + palaces) { appendLine(s.title); s.subtitle?.let { appendLine(it) }; s.paragraphs.forEach { appendLine(it.text) } }
        for (a in advice) appendLine(a.text)
        appendLine(closing)
    }
}

/** Bản luận dạng văn bản thuần (snapshot test, xuất văn bản). [why] = in kèm căn cứ của từng đoạn. */
fun Reading.render(why: Boolean = false): String = buildString {
    fun para(p: Paragraph) {
        val tag = when (p.kind) { ParaKind.LEAD -> "■"; ParaKind.WEIGH -> "⚖"; ParaKind.EVENT -> "◆"; ParaKind.NOTE -> "·"; ParaKind.PLAIN -> if (p.polarity > 0) "+" else if (p.polarity < 0) "−" else "○" }
        appendLine("  $tag ${p.text}")
        if (why) {
            appendLine("      [${p.ruleIds.joinToString(", ")}]")
            for (w in p.why) appendLine("      ↳ ${w.title}: ${w.detail}${if (w.evidence.isNotEmpty()) " | " + w.evidence.joinToString("; ") else ""}")
        }
    }
    appendLine("=== ${scope.vn.uppercase()} · $title ===")
    if (subtitle.isNotBlank()) appendLine(subtitle)
    appendLine("Điểm $score/100 · thế ${stance.vn} — $stanceText")
    appendLine()
    appendLine("[Tổng quan]")
    summary.forEach { para(it) }
    for (s in sections + palaces) {
        appendLine()
        appendLine("[${s.title}${s.score?.let { " · $it/100" } ?: ""}${s.subtitle?.let { " · $it" } ?: ""}]")
        s.paragraphs.forEach { para(it) }
    }
    if (timeline.isNotEmpty()) {
        appendLine()
        appendLine("[12 đại vận]")
        for (d in timeline) appendLine("  ${if (d.current) "▶" else " "} ${d.from}–${d.to} tuổi · ${d.chi.vn} (${d.role}) · ${d.canChi} · ${d.score}/100 ${d.stance.vn} · ${d.thienThoi} · từ ${d.startYear}")
    }
    if (hours.isNotEmpty()) {
        appendLine()
        appendLine("[12 giờ] " + hours.joinToString("  ") { "${it.chi.vn}:${it.score}" })
    }
    if (months.isNotEmpty()) {
        appendLine()
        appendLine("[12 tháng âm] " + months.joinToString("  ") { "T${it.month}:${it.score}" })
        if (goodMonths.isNotEmpty()) appendLine("  Tháng thuận: ${goodMonths.joinToString(", ")} · tháng nên thận trọng: ${cautionMonths.joinToString(", ")}")
    }
    if (advice.isNotEmpty()) {
        appendLine()
        appendLine("[Nên / Không nên / Lưu ý]")
        for (a in advice) appendLine("  ${when (a.type) { AdviceType.NEN -> "NÊN"; AdviceType.KHONG_NEN -> "KHÔNG NÊN"; AdviceType.LUU_Y -> "LƯU Ý" }}: ${a.text}${if (why) "  (vì: ${a.because})" else ""}")
    }
    appendLine()
    appendLine(closing)
}
