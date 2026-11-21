package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind
import vn.tuvi.core.engine.TuHoa
import vn.tuvi.core.engine.TuHoaTable
import vn.tuvi.core.engine.DaiHanTuHoa
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Luận đoán theo lá số cho Hỏi đáp (D-54). Không trả lời bằng câu soạn sẵn cho từng câu hỏi: mỗi câu trả lời được dựng
 * từ chính lá số đang mở, đi qua từng tầng như người xem tử vi:
 *
 * 1. **Lá số gốc**: cung của chủ đề an ở đâu, chính tinh nào, độ sáng, nghĩa của sao tại đúng cung đó (KB), cát tinh,
 *    sát tinh, Tứ Hóa gốc, Tuần/Triệt, vô chính diệu, tam phương tứ chính, cách cục gốc liên quan;
 * 2. **Đại hạn**: cung đại hạn, thiên thời, cung của chủ đề khi xoay 12 vai trò từ đại hạn, Tứ Hóa đại hạn (nếu trường
 *    phái dùng) chạm vào đâu;
 * 3. **Lưu niên đại hạn** (kết quả của năm), **tiểu hạn** (diễn biến trong năm);
 * 4. **Lưu Thái Tuế**: Tứ Hóa theo can năm, sao lưu (Lộc Tồn, Kình, Đà, Mã, Đào Hồng Hỷ, Tang Hổ…) rơi vào cung của
 *    chủ đề, xung chiếu hay tam hợp;
 * 5. **Đồng thanh tương ứng** và cách vận hạn kích hoạt; **tổng hợp** theo trọng số mục 9.
 *
 * Điểm số lấy đúng từ [Scorer] (cùng điểm với bản luận đầy đủ), câu chữ ghép từ dữ kiện lá số và nghĩa sao trong KB.
 */
class QaAnalyst(private val kb: KnowledgeBase) {

    companion object {
        private val CORE_CAT = setOf(
            StarId.TA_PHU, StarId.HUU_BAT, StarId.VAN_XUONG, StarId.VAN_KHUC, StarId.THIEN_KHOI, StarId.THIEN_VIET, StarId.LOC_TON, StarId.THIEN_MA,
        )
        private val CORE_SAT = setOf(StarId.KINH_DUONG, StarId.DA_LA, StarId.HOA_TINH, StarId.LINH_TINH, StarId.DIA_KHONG, StarId.DIA_KIEP)
        private val HOA_STARS = setOf(StarId.HOA_LOC, StarId.HOA_QUYEN, StarId.HOA_KHOA, StarId.HOA_KY)

        /** Trung tinh đáng nêu theo chủ đề (ngoài lục cát, lục sát, Lộc Mã, Tứ Hóa). */
        val TOPIC_STARS: Map<Topic, Set<StarId>> = mapOf(
            Topic.TAI_CHINH to setOf(StarId.DAI_HAO, StarId.TIEU_HAO, StarId.THIEN_TAI, StarId.THIEN_PHUC, StarId.KIEP_SAT),
            Topic.SU_NGHIEP to setOf(StarId.QUOC_AN, StarId.THIEN_QUAN, StarId.TAM_THAI, StarId.BAT_TOA, StarId.QUAN_PHU, StarId.THIEN_HINH),
            Topic.TINH_DUYEN to setOf(StarId.HONG_LOAN, StarId.THIEN_HY, StarId.DAO_HOA, StarId.THIEN_RIEU, StarId.CO_THAN, StarId.QUA_TU, StarId.LONG_TRI, StarId.PHUONG_CAC),
            Topic.GIA_DAO to setOf(StarId.THIEN_DUC, StarId.NGUYET_DUC, StarId.THAI, StarId.CO_THAN, StarId.QUA_TU, StarId.THIEN_KHOC, StarId.THIEN_HU),
            Topic.SUC_KHOE to setOf(StarId.THIEN_HINH, StarId.THIEN_RIEU, StarId.BENH_PHU, StarId.THIEN_Y, StarId.THIEN_GIAI, StarId.DIA_GIAI, StarId.GIAI_THAN, StarId.THIEN_THO),
            Topic.DI_CHUYEN to setOf(StarId.LONG_TRI, StarId.PHUONG_CAC, StarId.THIEN_THUONG, StarId.THIEN_SU),
            Topic.NHA_DAT to setOf(StarId.DUONG_PHU, StarId.DAI_HAO, StarId.TIEU_HAO, StarId.THIEN_TAI),
            Topic.PHAP_LY to setOf(StarId.THIEN_HINH, StarId.QUAN_PHU, StarId.QUAN_PHU_BS, StarId.THIEN_RIEU, StarId.PHUC_BINH),
            Topic.TINH_CACH to setOf(StarId.THIEN_QUAN, StarId.THIEN_PHUC, StarId.HOA_CAI, StarId.THIEN_HINH, StarId.THIEN_RIEU),
            Topic.PHUC_DUC to setOf(StarId.THIEN_DUC, StarId.NGUYET_DUC, StarId.HOA_CAI, StarId.THIEN_PHUC, StarId.THIEN_KHOC, StarId.THIEN_HU),
        )

        /** Sao lưu đáng nêu theo chủ đề. */
        val TOPIC_LUU: Map<Topic, Set<StarId>> = mapOf(
            Topic.TAI_CHINH to setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA, StarId.THIEN_MA),
            Topic.SU_NGHIEP to setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA, StarId.THIEN_MA),
            Topic.TINH_DUYEN to setOf(StarId.HONG_LOAN, StarId.THIEN_HY, StarId.DAO_HOA, StarId.KINH_DUONG, StarId.DA_LA),
            Topic.GIA_DAO to setOf(StarId.TANG_MON, StarId.BACH_HO, StarId.THIEN_KHOC, StarId.THIEN_HU, StarId.HY_THAN),
            Topic.SUC_KHOE to setOf(StarId.TANG_MON, StarId.BACH_HO, StarId.KINH_DUONG, StarId.DA_LA, StarId.THIEN_KHOC, StarId.THIEN_HU),
            Topic.DI_CHUYEN to setOf(StarId.THIEN_MA, StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA),
            Topic.NHA_DAT to setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA, StarId.BACH_HO),
            Topic.PHAP_LY to setOf(StarId.KINH_DUONG, StarId.DA_LA, StarId.BACH_HO, StarId.TANG_MON),
            Topic.TINH_CACH to setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA),
            Topic.PHUC_DUC to setOf(StarId.THIEN_KHOC, StarId.THIEN_HU, StarId.LONG_DUC, StarId.NGUYET_DUC),
        )

        private val LAYER_TITLE = mapOf(
            Layer.GOC to "Lá số gốc", Layer.DAI_HAN to "Đại hạn", Layer.LUU_NIEN to "Lưu niên đại hạn", Layer.TIEU_HAN to "Tiểu hạn", Layer.LUU_THAI_TUE to "Lưu Thái Tuế, Tứ Hóa năm",
        )
        private val LAYER_SHORT = mapOf(
            Layer.GOC to "gốc", Layer.DAI_HAN to "đại hạn", Layer.LUU_NIEN to "lưu niên đại hạn", Layer.TIEU_HAN to "tiểu hạn", Layer.LUU_THAI_TUE to "Lưu Thái Tuế",
        )
        private val LAYER_ROLE = mapOf(
            Layer.DAI_HAN to "nền 10 năm", Layer.LUU_NIEN to "kết quả của năm", Layer.TIEU_HAN to "diễn biến trong năm", Layer.LUU_THAI_TUE to "việc bị đánh thức trong năm",
        )
        const val SOURCE = "Prompt mục 6–9; cẩm nang V2 P.II–P.IV"

        fun band(s: Int): String = when {
            s >= 70 -> "rất thuận"
            s >= 55 -> "thuận"
            s >= 46 -> "trung bình, có lên có xuống"
            s >= 35 -> "cần thận trọng"
            else -> "khó khăn, nên giữ"
        }
        fun pol(s: Int) = when { s >= 55 -> 1; s <= 45 -> -1; else -> 0 }
        private fun fmt(v: Double) = (if (v >= 0) "+" else "") + "%.1f".format(v)
    }

    /** Một đoạn luận đoán + có dùng câu chữ KB chưa duyệt không. */
    private class Txt(val text: String, val draft: Boolean)

    // ———————————————————————————— dữ kiện lá số → lời ————————————————————————————

    private fun place(chart: Chart, chi: Chi) = "${chart.palace(chi).role.vn} (${chi.vn})"

    private fun bright(b: Brightness?) = b?.vn?.lowercase()?.let { if (it == "bình hòa") "bình hòa" else it }

    /** “Thiên Đồng vượng – Cự Môn hãm”; vô chính diệu: “vô chính diệu, mượn … từ cung xung chiếu”. */
    fun mainStars(chart: Chart, chi: Chi): String {
        val p = chart.palace(chi)
        fun list(cs: List<vn.tuvi.core.engine.PlacedStar>) = cs.joinToString(" – ") { s -> s.id.vn + (bright(s.brightness)?.let { " $it" } ?: "") }
        return if (p.chinhTinh.isNotEmpty()) list(p.chinhTinh)
        else {
            val o = chart.palace(chi.opposite).chinhTinh
            if (o.isEmpty()) "vô chính diệu" else "vô chính diệu, mượn ${list(o)} từ ${chi.opposite.vn}"
        }
    }

    /** Như [mainStars] nhưng gọn: “vô chính diệu (mượn Thái Dương – Thiên Lương)”. */
    fun shortStars(chart: Chart, chi: Chi): String = mainStars(chart, chi).replace(Regex(", mượn (.+) từ \\S+$"), " (mượn $1)")

    /** Cát tinh, sát tinh đáng nêu tại cung [chi] cho chủ đề [topic]; Tứ Hóa gốc ghi kèm sao được hóa. */
    private fun aux(chart: Chart, chi: Chi, topic: Topic?): Pair<List<String>, List<String>> {
        val p = chart.palace(chi)
        val extra = topic?.let { TOPIC_STARS[it] }.orEmpty()
        val hoaOf = TuHoaTable.of(chart.yearCan, chart.config)
        val cat = ArrayList<String>()
        val hung = ArrayList<String>()
        for (s in p.stars) {
            when {
                s.id in HOA_STARS -> {
                    val h = TuHoa.entries.first { it.star == s.id }
                    val name = "${h.vn} (${hoaOf[h]?.vn ?: ""})".replace(" ()", "")
                    if (h == TuHoa.KY) hung.add(name) else cat.add(name)
                }
                s.id in CORE_CAT -> cat.add(s.id.vn)
                s.id in CORE_SAT -> hung.add(s.id.vn + (bright(s.brightness)?.takeIf { s.brightness == Brightness.DAC || s.brightness == Brightness.HAM }?.let { " $it" } ?: ""))
                s.id in extra -> {
                    val pl = kb.minorPolarity(s.id, s.brightness)
                    if (pl > 0) cat.add(s.id.vn) else if (pl < 0) hung.add(s.id.vn)
                }
            }
        }
        return cat to hung
    }

    /** Nghĩa chính tinh (hoặc bộ đồng cung) tại cung [chi] theo vai trò [role] — câu trong KB, bỏ phần lặp tên sao. */
    private fun meaning(chart: Chart, chi: Chi, role: PalaceRole): Txt? {
        val p = chart.palace(chi)
        val stars = p.chinhTinh.ifEmpty { chart.palace(chi.opposite).chinhTinh }
        if (stars.isEmpty()) return null
        val combo = if (stars.size == 2) kb.comboKey(stars[0].id, stars[1].id) else null
        val keys = if (combo != null) listOf("combo.$combo.${role.name}") else stars.map { "star.${it.id.name}.${(it.brightness ?: Brightness.BINH).code}.${role.name}" }
        val ts = keys.mapNotNull { kb.texts[it] }
        if (ts.isEmpty()) return null
        val parts = ts.map { t -> clean(t.text) }.filter { it.isNotBlank() }.distinct()
        return Txt(parts.joinToString("; ").trimEnd('.', ';'), ts.any { !it.reviewed })
    }

    private val leadRx = Regex("^(về [^,]+, |khi ra ngoài xã hội, đi xa, |trong công việc, sự nghiệp, )")
    private val powerRx = Regex("^(sao )?(phát huy (trọn vẹn|mạnh|khá tốt)|bị hạn chế)( điểm mạnh)?\\s*[:—-]\\s*")

    private fun clean(text: String): String {
        var s = text.substringAfter(": ", text).trim()
        s = leadRx.replace(s, "")
        s = powerRx.replace(s, "")
        s = s.replace(Regex("^sao bị hạn chế: "), "")
        return s.replaceFirstChar { it.lowercase() }.trimEnd('.')
    }

    private fun phuMeaning(chart: Chart, chi: Chi, role: PalaceRole, topic: Topic?): Txt? {
        val p = chart.palace(chi)
        val ids = p.stars.map { it.id }.filter { it in HOA_STARS || it in CORE_CAT || it in CORE_SAT || it in topic?.let { t -> TOPIC_STARS[t] }.orEmpty() }
        val ts = ids.mapNotNull { kb.texts["phu.${it.name}.${role.name}"] }.take(2)
        if (ts.isEmpty()) return null
        return Txt(ts.joinToString("; ") { it.text.trimEnd('.') }, ts.any { !it.reviewed })
    }

    /** Các vị trí quanh cung [target]: tọa thủ, xung chiếu, tam hợp. */
    private fun relation(target: Chi, at: Chi): String? = when (at) {
        target -> "tọa thủ"
        target.opposite -> "xung chiếu"
        target + 4, target + 8 -> "tam hợp"
        else -> null
    }

    /** Tứ Hóa theo can: dòng liệt kê và các hóa chạm cung [target] (dạng “Hóa Lộc{hậu tố} (tam hợp)”). */
    private fun tuHoaLine(chart: Chart, m: Map<TuHoa, Chi>, can: vn.tuvi.core.calendar.Can, target: Chi?, suffix: String = ""): Pair<String, List<String>> {
        val stars = TuHoaTable.of(can, chart.config)
        val all = TuHoa.entries.mapNotNull { h -> m[h]?.let { c -> "${h.vn} → ${stars[h]?.vn} tại ${place(chart, c)}" } }.joinToString("; ")
        val hits = if (target == null) emptyList() else TuHoa.entries.mapNotNull { h ->
            val c = m[h] ?: return@mapNotNull null
            relation(target, c)?.let { r -> "${h.vn}$suffix ($r)" }
        }
        return all to hits
    }

    /** Mô tả gọn một cung: chính tinh + sát/hung tinh đầu tiên (nếu có). */
    private fun brief(chart: Chart, chi: Chi, topic: Topic?): String {
        val (_, hung) = aux(chart, chi, topic)
        return shortStars(chart, chi) + (hung.firstOrNull()?.let { ", gặp $it" } ?: "")
    }

    // ———————————————————————————— đoạn theo tầng ————————————————————————————

    private fun para(text: String, polarity: Int, kind: ParaKind, topic: Topic?, id: String, title: String, detail: String, evidence: List<String>, draft: Boolean) =
        Paragraph(text, polarity, kind, topic, listOf(id), listOf(Why(id, title, detail, evidence, SOURCE)), draft, title)

    private fun evidence(ls: LayerScore?): List<String> = ls?.score?.top?.map { "${it.label}: ${fmt(it.value)}" }.orEmpty()

    /** Tầng gốc: cung của chủ đề trên lá số gốc. */
    private fun natalPara(chart: Chart, data: Composer.PeriodData, topic: Topic?, role: PalaceRole, n: Int): Paragraph {
        val chi = chart.palace(role).chi
        val p = chart.palace(chi)
        val ts = topic?.let { data.topics[it] }
        val ls = (ts?.layers ?: data.layers).firstOrNull { it.layer == Layer.GOC }
        val score = ls?.score?.score ?: ts?.score
        val (cat, hung) = aux(chart, chi, topic)
        val mean = meaning(chart, chi, role)
        val phu = phuMeaning(chart, chi, role, topic)
        val sb = StringBuilder()
        sb.append("Cung ${role.vn} an tại ${chi.vn}")
        val marks = listOfNotNull("là cung Thân".takeIf { p.isThan }, "có Tuần".takeIf { p.hasTuan }, "có Triệt".takeIf { p.hasTriet })
        if (marks.isNotEmpty()) sb.append(" (${marks.joinToString(", ")})")
        sb.append(": ${mainStars(chart, chi)}")
        mean?.let { sb.append(" — ${it.text}") }
        sb.append(". ")
        if (cat.isNotEmpty()) sb.append("Cát tinh: ${cat.joinToString(", ")}. ")
        if (hung.isNotEmpty()) sb.append("Sát/hung tinh: ${hung.joinToString(", ")}. ")
        phu?.let { sb.append(it.text.replaceFirstChar { c -> c.uppercase() }).append(". ") }
        val opp = chi.opposite
        sb.append("Xung chiếu từ ${place(chart, opp)}: ${shortStars(chart, opp)}; tam hợp ${place(chart, chi + 4)} và ${place(chart, chi + 8)}. ")
        val natalHits = data.hits.filter { it.layer == Layer.GOC && (topic == null || topic in it.topics) && it.polarity != 0 }.sortedByDescending { it.strength }.take(2)
        if (natalHits.isNotEmpty()) sb.append("Cách cục gốc liên quan: ${natalHits.joinToString("; ") { hitLabel(it) }}. ")
        if (score != null) sb.append("→ Nền gốc: $score/100 (${band(score)}).")
        return para(sb.toString().trim(), score?.let(::pol) ?: 0, ParaKind.WEIGH, topic, "AN_GOC_${role.name}", "${CIRC[n]} Lá số gốc",
            "Cung ${role.vn} gốc tại ${chi.vn}. Lực sao tính theo tọa thủ ×1, xung chiếu ×0,8, tam hợp ×0,6, nhị hợp ×0,3, giáp ×0,4; vô chính diệu mượn chính tinh xung chiếu ×0,7 (mục 9 bước 1–3).",
            evidence(ls) + natalHits.map { "${it.kind}: ${it.name}" }, (mean?.draft ?: false) || (phu?.draft ?: false))
    }

    private fun hitLabel(h: Composer.RuleHit): String {
        val k = h.kind.lowercase()
        return if ("cách" in k) "${h.name} ($k)" else "${h.name} (${if (h.polarity > 0) "cát" else "hung"})"
    }

    private val CIRC = listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧")

    /** Một tầng vận hạn: cung tầng, cung của chủ đề khi xoay vai trò từ tầng đó, sao tại đó, lớp phủ (Tứ Hóa, sao lưu). */
    private fun layerPara(chart: Chart, data: Composer.PeriodData, layer: Layer, topic: Topic?, role: PalaceRole, n: Int, ofLabel: String): Paragraph? {
        val l = data.limits ?: return null
        val anchor = when (layer) {
            Layer.DAI_HAN -> if (l.beforeFirstDaiHan) null else l.daiHan
            Layer.LUU_NIEN -> if (l.beforeFirstDaiHan) null else l.luuNienDaiHan
            Layer.TIEU_HAN -> l.tieuHan
            Layer.LUU_THAI_TUE -> l.luuThaiTue
            Layer.GOC -> chart.menh
        } ?: return null
        val target = anchor + role.ordinal
        val ts = topic?.let { data.topics[it] }
        val ls = (ts?.layers ?: data.layers).firstOrNull { it.layer == layer }
        val score = ls?.score?.score
        val sb = StringBuilder()
        var draft = false
        val ev = ArrayList<String>()
        when (layer) {
            Layer.DAI_HAN -> {
                val pd = l.daiHanPeriod
                val y1 = chart.lunar.year + pd.ages.first - 1
                sb.append("Giai đoạn ${y1}–${y1 + 9}, đại hạn tại ${chi(anchor)}, cung gốc ${chart.palace(anchor).role.vn} (${shortStars(chart, anchor)}), can chi ${pd.canChi.vn}, thiên thời ${pd.thienThoi.vn.lowercase()}. ")
            }
            Layer.LUU_NIEN -> sb.append("Năm thứ ${l.namThuDaiHan} của đại hạn: lưu niên đại hạn tại ${chi(anchor)} (cung gốc ${chart.palace(anchor).role.vn}). ")
            Layer.TIEU_HAN -> sb.append("Tiểu hạn năm ${l.lunarYear} tại ${chi(anchor)} (cung gốc ${chart.palace(anchor).role.vn}: ${shortStars(chart, anchor)}). ")
            Layer.LUU_THAI_TUE -> sb.append("Năm ${l.yearCanChi.vn}: Thái Tuế đóng tại ${chi(anchor)} (cung gốc ${chart.palace(anchor).role.vn}). ")
            Layer.GOC -> {}
        }
        if (role != PalaceRole.MENH || layer == Layer.LUU_THAI_TUE) {
            sb.append("Xoay 12 cung từ đây, cung ${role.vn} $ofLabel rơi vào ${chi(target)} (cung gốc ${chart.palace(target).role.vn}): ${mainStars(chart, target)}")
        } else sb.append("Sao tại cung này: ${mainStars(chart, target)}")
        val mean = if (layer == Layer.DAI_HAN || layer == Layer.TIEU_HAN) meaning(chart, target, role) else null
        mean?.let { sb.append(" — ${it.text}"); draft = draft || it.draft }
        sb.append(". ")
        val (cat, hung) = aux(chart, target, topic)
        if (layer != Layer.LUU_NIEN) {
            if (cat.isNotEmpty()) sb.append("Cát tinh tại đó: ${cat.joinToString(", ")}. ")
            if (hung.isNotEmpty()) sb.append("Sát/hung tinh: ${hung.joinToString(", ")}. ")
        }
        if (layer == Layer.DAI_HAN && chart.config.daiHanTuHoa == DaiHanTuHoa.ON && l.daiHanTuHoa != null) {
            val (all, hits) = tuHoaLine(chart, l.daiHanTuHoa!!, l.daiHanPeriod.canChi.can, target)
            sb.append("Tứ Hóa đại hạn (can ${l.daiHanPeriod.canChi.can.vn}): $all. ")
            sb.append(if (hits.isEmpty()) "Không hóa nào chạm thẳng cung ${role.vn} của đại hạn. " else "Chạm cung ${role.vn} của đại hạn: ${hits.joinToString(", ")}. ")
            ev += "Tứ Hóa đại hạn: $all"
        }
        if (layer == Layer.LUU_THAI_TUE) {
            val (all, hits) = tuHoaLine(chart, l.luuTuHoa, l.yearCanChi.can, target, " lưu")
            sb.append("Tứ Hóa năm ${l.yearCanChi.can.vn}: $all. ")
            val natalTarget = chart.palace(role).chi
            val natalHits = TuHoa.entries.mapNotNull { h -> l.luuTuHoa[h]?.takeIf { it == natalTarget }?.let { h.vn } }
            val wanted = topic?.let { TOPIC_LUU[it] } ?: setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA, StarId.THIEN_MA)
            val luuHits = l.luuStars.filterKeys { it in wanted }.mapNotNull { (id, c) -> relation(target, c)?.let { r -> "${id.vn} lưu ($r)" } }
            val allHits = hits + luuHits
            sb.append(if (allHits.isEmpty()) "Không có Tứ Hóa lưu hay sao lưu lớn chạm cung ${role.vn} năm. " else "Chạm cung ${role.vn} năm và tam phương: ${allHits.joinToString(", ")}. ")
            if (natalHits.isNotEmpty() && natalTarget != target) sb.append("${natalHits.joinToString(", ")} lưu còn nhập thẳng cung ${role.vn} gốc (${natalTarget.vn}). ")
            ev += "Tứ Hóa năm: $all"
            ev += "Sao lưu: " + l.luuStars.entries.filter { it.key != StarId.THAI_TUE }.joinToString(", ") { "${it.key.vn} ${it.value.vn}" }
        }
        if (score != null) sb.append("→ ${LAYER_SHORT[layer]!!.replaceFirstChar { it.uppercase() }}: $score/100 (${band(score)})${LAYER_ROLE[layer]?.let { ", $it" } ?: ""}.")
        val title = when (layer) {
            Layer.DAI_HAN -> "${CIRC[n]} Đại hạn ${l.daiHanPeriod.ages.first}–${l.daiHanPeriod.ages.last} tuổi"
            Layer.TIEU_HAN -> "${CIRC[n]} Tiểu hạn ${l.lunarYear}"
            else -> "${CIRC[n]} ${LAYER_TITLE[layer]}"
        }
        return para(sb.toString().trim(), score?.let(::pol) ?: 0, ParaKind.WEIGH, topic, "AN_${layer.name}_${role.name}", title,
            "Khung 12 cung của ${LAYER_SHORT[layer]} lấy cung ${chi(anchor)} làm Mệnh; cung ${role.vn} ở ${chi(target)} (mục 8.1, 8.4). " +
                (if (layer == Layer.LUU_THAI_TUE) "Lớp phủ: sao lưu ×0,6, Tứ Hóa lưu ×0,7." else "") ,
            evidence(ls) + ev, draft)
    }

    private fun chi(c: Chi) = c.vn

    // ———————————————————————————— luận đoán theo phạm vi ————————————————————————————

    class Result(val paragraphs: List<Paragraph>, val score: Int, val conclusion: Paragraph)

    /** Vai trò cung chính của chủ đề (người thân: cung của người đó). */
    fun roleFor(topic: Topic?, relative: String? = null): PalaceRole = when {
        relative != null -> when (relative) {
            "con" -> PalaceRole.TU_TUC
            "vo", "chong", "nguoi yeu", "ban trai", "ban gai" -> PalaceRole.PHU_THE
            "bo", "me", "cha", "ba" -> PalaceRole.PHU_MAU
            "anh", "chi", "em" -> PalaceRole.HUYNH_DE
            else -> topic?.let { Scorer.TOPICS.getValue(it).main.first() } ?: PalaceRole.MENH
        }
        topic == null -> PalaceRole.MENH
        else -> Scorer.TOPICS.getValue(topic).main.first()
    }

    private fun topicName(topic: Topic?, role: PalaceRole? = null): String {
        val base = when (topic) {
            null -> "Vận chung"
            Topic.SUC_KHOE -> "Sức khỏe"
            Topic.TINH_DUYEN -> "Tình duyên, hôn nhân"
            else -> topic.vn
        }
        // Hỏi về một người thân: nói rõ đang đọc cung nào.
        return if (topic != null && role != null && role != Scorer.TOPICS.getValue(topic).main.first()) "$base (qua cung ${role.vn})" else base
    }

    /** Năm âm: kết luận + 5 tầng + đồng thanh/cách vận hạn + tổng hợp. */
    fun year(chart: Chart, data: Composer.PeriodData, topic: Topic?, event: Event?, relative: String?, compact: Boolean = false): Result {
        val l = data.limits!!
        val role = roleFor(topic, relative)
        val ts = topic?.let { data.topics[it] }
        val score = ts?.score ?: data.score
        val layers = ts?.layers ?: data.layers
        val out = ArrayList<Paragraph>()
        val conclusion = yearConclusion(chart, data, topic, event, role, score, layers)
        out.add(conclusion)
        if (compact) {
            out.add(summaryPara(chart, data, topic, role, score, layers, 1))
            return Result(out, score, conclusion)
        }
        var n = 0
        out.add(natalPara(chart, data, topic, role, n++))
        for (layer in listOf(Layer.DAI_HAN, Layer.LUU_NIEN, Layer.TIEU_HAN, Layer.LUU_THAI_TUE)) {
            val lbl = when (layer) { Layer.DAI_HAN -> "của đại hạn"; Layer.LUU_NIEN -> "của lưu niên đại hạn"; Layer.TIEU_HAN -> "của tiểu hạn"; else -> "theo Thái Tuế" }
            layerPara(chart, data, layer, topic, role, n, lbl)?.let { out.add(it); n++ }
        }
        // Đồng thanh tương ứng + cách vận hạn của năm cho chủ đề.
        val evs = data.events.filter { topic == null || it.event.topic == topic || it.event == event }
        val yearHits = data.hits.filter { it.layer != Layer.GOC && (topic == null || topic in it.topics) && it.polarity != 0 }.sortedByDescending { it.strength }.take(3)
        if (evs.isNotEmpty() || yearHits.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("Năm ${l.lunarYear} — ")
            if (evs.isNotEmpty()) sb.append("đồng thanh tương ứng: " + evs.joinToString("; ") { e -> "${e.event.short} ${e.level.vn} (${e.layers.size} tầng: ${e.layers.joinToString(", ") { LAYER_SHORT.getValue(it) }})" } + ". ")
            if (yearHits.isNotEmpty()) sb.append((if (evs.isEmpty()) "c" else "C") + "ách vận hạn kích hoạt: " + yearHits.joinToString("; ") { h ->
                "${h.name} (${LAYER_SHORT.getValue(h.layer)}${h.palaces.firstOrNull()?.let { c -> " tại ${c.vn}" } ?: ""}, ${if (h.polarity > 0) "cát" else "hung"})" } + ".")
            out.add(Paragraph(sb.toString().trim(), 0, ParaKind.NOTE, topic, listOf("AN_DONG_THANH") + yearHits.map { it.ruleId },
                listOf(Why("AN_DONG_THANH", "Đồng thanh tương ứng", "Từ ba tầng cùng chỉ một việc trở lên: khả năng cao; hai tầng: có thể; một tầng: mới là dấu hiệu (mục 9).",
                    evs.map { "${it.event.vn}: ${it.layers.joinToString(", ") { l2 -> l2.vn }}" }, "Prompt mục 9")) + yearHits.map { it.why },
                yearHits.isNotEmpty(), "${CIRC[n++]} Đồng thanh tương ứng"))
        }
        out.add(summaryPara(chart, data, topic, role, score, layers, n))
        return Result(out, score, conclusion)
    }

    private fun layerScore(layers: List<LayerScore>, layer: Layer) = layers.firstOrNull { it.layer == layer }?.score?.score

    private fun yearConclusion(chart: Chart, data: Composer.PeriodData, topic: Topic?, event: Event?, role: PalaceRole, score: Int, layers: List<LayerScore>): Paragraph {
        val l = data.limits!!
        val sb = StringBuilder()
        sb.append("${topicName(topic, role)} năm ${l.lunarYear} (${l.yearCanChi.vn}, ${l.tuoiAm} tuổi âm): $score/100 — ${band(score)}. ")
        val named = layers.filter { it.layer != Layer.GOC }
        val good = named.filter { it.score.score >= 55 }.sortedByDescending { it.score.score }
        val bad = named.filter { it.score.score <= 45 }.sortedBy { it.score.score }
        // Gộp các tầng có cung của chủ đề rơi cùng một chỗ.
        fun at(xs: List<LayerScore>): String = xs.groupBy { it.palace }.entries.take(2).joinToString("; ") { (c, g) ->
            val who = g.joinToString(" và ") { LAYER_SHORT.getValue(it.layer) }
            "$who (cung ${role.vn}${if (g.size > 1) " cùng rơi vào" else " tại"} ${c.vn}: ${brief(chart, c, topic)})"
        }
        if (good.isNotEmpty()) sb.append("Điểm tựa: ${at(good)}. ")
        if (bad.isNotEmpty()) sb.append("Áp lực: ${at(bad)}. ")
        val th = layerScore(layers, Layer.TIEU_HAN)
        val ln = layerScore(layers, Layer.LUU_NIEN)
        if (th != null && ln != null) when {
            th <= 45 && ln >= 55 -> sb.append("Diễn biến đầu có thể vất vả nhưng kết quả cuối năm nghiêng về tốt. ")
            th >= 55 && ln <= 45 -> sb.append("Khởi đầu thuận, cần giữ thành quả đến cuối năm. ")
        }
        if (event != null) {
            val e = data.events.firstOrNull { it.event == event }
            val strong = e != null && e.level != Likelihood.DAU_HIEU
            sb.append(
                if (e != null) "Riêng chuyện ${event.short}: ${e.level.vn} (${e.layers.size} tầng cùng chỉ: ${e.layers.joinToString(", ") { LAYER_SHORT.getValue(it) }})" +
                    when {
                        !strong -> ". "
                        event.polarity < 0 -> "; nên chuẩn bị trước — đây là khả năng cần phòng, không phải lời khẳng định. "
                        score >= 55 -> ", điểm chung cũng thuận nên đây là thời điểm đáng chủ động. "
                        else -> " dù điểm chung chỉ ${band(score)}: cơ hội có, nhưng cần vun đắp và chọn thời điểm. "
                    }
                else "Riêng chuyện ${event.short}: năm này chưa thấy tầng vận hạn nào chỉ rõ. "
            )
        }
        return Paragraph(sb.toString().trim(), pol(score), ParaKind.LEAD, topic, listOf("AN_KET_LUAN"),
            listOf(Why("AN_KET_LUAN", "Kết luận", "Điểm chủ đề = gộp 5 tầng theo trọng số mục 9 bước 6: gốc 0,10; đại hạn 0,30; lưu niên đại hạn 0,25; tiểu hạn 0,20; Lưu Thái Tuế 0,15.",
                layers.map { "${it.layer.vn}: ${it.score.score} (cung ${it.palace.vn})" }, "Prompt mục 9")), false, "Kết luận")
    }

    private fun summaryPara(chart: Chart, data: Composer.PeriodData, topic: Topic?, role: PalaceRole, score: Int, layers: List<LayerScore>, n: Int): Paragraph {
        val parts = layers.joinToString(" + ") { "${LAYER_SHORT.getValue(it.layer)} ${it.score.score}×${"%.2f".format(it.weight).replace('.', ',')}" }
        val lows = layers.count { it.score.score <= 45 }
        val highs = layers.count { it.score.score >= 55 }
        val names = { pred: (Int) -> Boolean -> layers.filter { pred(it.score.score) }.joinToString(", ") { LAYER_SHORT.getValue(it.layer) } }
        val agree = when {
            highs >= 3 && lows == 0 -> "Phần lớn các tầng cùng thuận nên xu hướng tốt khá rõ."
            lows >= 3 && highs == 0 -> "Phần lớn các tầng cùng yếu: nên đi chậm, giữ an toàn."
            highs > 0 && lows > 0 -> "Các tầng không đồng thuận — thuận ở ${names { it >= 55 }}, yếu ở ${names { it <= 45 }} — nên kết quả phụ thuộc nhiều vào cách bạn chọn thời điểm."
            highs > 0 -> "Có tầng thuận (${names { it >= 55 }}), còn lại quanh trung bình: nghiêng về tốt nếu chủ động."
            lows > 0 -> "Có tầng yếu (${names { it <= 45 }}), còn lại quanh trung bình: nên phòng trước ở mặt này."
            else -> "Các tầng ở quanh mức trung bình: không có biến động lớn ở mặt này."
        }
        return Paragraph("$parts = $score/100. $agree", pol(score), ParaKind.NOTE, topic, listOf("AN_TONG_HOP"),
            listOf(Why("AN_TONG_HOP", "Cách gộp tầng", "Điểm mỗi tầng tính trên khung 12 cung của tầng đó; trọng số tầng theo mục 9 bước 6 (đại vận: gốc 0,25, đại hạn 0,75).",
                layers.flatMap { evidence(it).take(2) }, "Prompt mục 9; D-38")), false, "${CIRC[n.coerceAtMost(CIRC.size - 1)]} Tổng hợp")
    }

    /** Đại vận: gốc + đại hạn, năm thuận/khó trong 10 năm của chủ đề. */
    fun decade(chart: Chart, data: Composer.PeriodData, topic: Topic?, relative: String?, years: List<Pair<Int, Int>>): Result {
        val l = data.limits!!
        val role = roleFor(topic, relative)
        val ts = topic?.let { data.topics[it] }
        val score = ts?.score ?: data.score
        val layers = ts?.layers ?: data.layers
        val pd = l.daiHanPeriod
        val out = ArrayList<Paragraph>()
        val sb = StringBuilder("${topicName(topic)} trong đại vận ${pd.ages.first}–${pd.ages.last} tuổi: $score/100 — ${band(score)}. ")
        if (l.beforeFirstDaiHan) sb.append("Năm hỏi chưa vào đại hạn đầu nên đọc theo lá số gốc. ")
        if (years.size >= 3) {
            val best = years.maxBy { it.second }
            val worst = years.minBy { it.second }
            sb.append("Trong 10 năm này, năm thuận nhất là ${best.first} (${best.second}/100), năm cần giữ nhất là ${worst.first} (${worst.second}/100).")
        }
        val concl = Paragraph(sb.toString().trim(), pol(score), ParaKind.LEAD, topic, listOf("AN_KET_LUAN"),
            listOf(Why("AN_KET_LUAN", "Kết luận đại vận", "Đại vận: gốc 0,25 + đại hạn 0,75 (D-38); điểm từng năm theo luận năm.", years.map { "${it.first}: ${it.second}" }, "Prompt mục 9")), false, "Kết luận")
        out.add(concl)
        out.add(natalPara(chart, data, topic, role, 0))
        layerPara(chart, data, Layer.DAI_HAN, topic, role, 1, "của đại hạn")?.let { out.add(it) }
        out.add(summaryPara(chart, data, topic, role, score, layers, 2))
        return Result(out, score, concl)
    }

    /** Trọn đời: gốc sâu + dòng đại vận của chủ đề. */
    fun life(chart: Chart, data: Composer.PeriodData, topic: Topic?, relative: String?, decades: List<Triple<IntRange, Chi, Int>>, currentAge: Int?): Result {
        val role = roleFor(topic, relative)
        val ts = topic?.let { data.topics[it] }
        val score = ts?.score ?: data.score
        val out = ArrayList<Paragraph>()
        val sb = StringBuilder("${topicName(topic)} theo lá số gốc: $score/100 — ${band(score)}. ")
        val menh = chart.palace(chart.menh)
        sb.append("Mệnh tại ${chart.menh.vn} (${shortStars(chart, chart.menh)}), Thân cư ${chart.palace(chart.than).role.vn}, ${chart.banMenh.name} — ${chart.cuc.vn}${if (menh.isVoChinhDieu) ", Mệnh vô chính diệu" else ""}. ")
        val adult = decades.filter { it.first.first >= 14 }
        if (adult.size >= 2) {
            val best = adult.maxBy { it.third }
            val worst = adult.minBy { it.third }
            sb.append("Giai đoạn thuận nhất cho mặt này: ${best.first.first}–${best.first.last} tuổi (đại hạn tại ${best.second.vn}, ${best.third}/100); giai đoạn cần giữ: ${worst.first.first}–${worst.first.last} tuổi (${worst.third}/100).")
        }
        val concl = Paragraph(sb.toString().trim(), pol(score), ParaKind.LEAD, topic, listOf("AN_KET_LUAN"),
            listOf(Why("AN_KET_LUAN", "Kết luận trọn đời", "Điểm chủ đề trên lá số gốc; dòng đại vận: điểm chủ đề của từng đại vận (gốc 0,25 + đại hạn 0,75).",
                decades.map { "${it.first.first}–${it.first.last} (${it.second.vn}): ${it.third}" }, "Prompt mục 9")), false, "Kết luận")
        out.add(concl)
        out.add(natalPara(chart, data, topic, role, 0))
        if (role != PalaceRole.MENH) {
            // Mệnh và Thân: cách người này xử lý chuyện của chủ đề.
            val (cat, hung) = aux(chart, chart.menh, null)
            val mm = meaning(chart, chart.menh, PalaceRole.MENH)
            val t = "Mệnh tại ${chart.menh.vn}: ${mainStars(chart, chart.menh)}${mm?.let { " — ${it.text}" } ?: ""}." +
                (if (cat.isNotEmpty()) " Cát tinh: ${cat.joinToString(", ")}." else "") + (if (hung.isNotEmpty()) " Sát/hung tinh: ${hung.joinToString(", ")}." else "") +
                chart.palace(chart.than).role.let { tr -> " Thân cư $tr (${chart.than.vn}): sau khoảng 30 tuổi, đời sống dồn nhiều về việc của cung $tr" +
                    (if (tr == role) " — đúng cung của câu hỏi, nên mặt này càng quan trọng với bạn." else ".") }
            val ms = data.topics[Topic.TINH_CACH]?.score
            out.add(para(t, ms?.let(::pol) ?: 0, ParaKind.WEIGH, Topic.TINH_CACH, "AN_GOC_MENH", "② Mệnh và Thân", "Mệnh là gốc tính cách, Thân là nơi đời sống dồn về sau tuổi 30 (mục 2.3).",
                emptyList(), mm?.draft ?: false))
        }
        if (decades.isNotEmpty()) {
            val adultTopic = topic != null && topic !in setOf(Topic.SU_NGHIEP, Topic.SUC_KHOE, Topic.GIA_DAO, Topic.DI_CHUYEN, Topic.TINH_CACH)
            val line = decades.filter { it.first.first <= 84 && (!adultTopic || it.first.first >= 14) }.joinToString("; ") { (ages, c, s) ->
                "${ages.first}–${ages.last}: $s${if (currentAge != null && currentAge in ages) " (hiện tại)" else ""}"
            }
            out.add(Paragraph("Điểm ${topic?.let { topicName(it).lowercase() } ?: "chung"} theo từng đại vận — $line.", 0, ParaKind.NOTE, topic, listOf("AN_DONG_DAI_VAN"),
                listOf(Why("AN_DONG_DAI_VAN", "Dòng đại vận", "Mỗi đại vận: điểm chủ đề = gốc 0,25 + đại hạn 0,75.", decades.map { "${it.first.first}–${it.first.last} tại ${it.second.vn}: ${it.third}" }, "Prompt mục 8.1, 9")),
                false, "${CIRC[if (role != PalaceRole.MENH) 2 else 1]} Dòng đại vận"))
        }
        return Result(out, score, concl)
    }

    /** Tháng âm: nguyệt hạn, Tứ Hóa tháng chạm cung chủ đề, nền của năm. */
    fun month(chart: Chart, yearData: Composer.PeriodData, m: LimitLayers, monthScore: Int, topic: Topic?, relative: String?): Result {
        val role = roleFor(topic, relative)
        val out = ArrayList<Paragraph>()
        val nh = m.nguyetHan
        val natalTarget = chart.palace(role).chi
        val ty = topic?.let { yearData.topics[it] }
        val sb = StringBuilder("Tháng ${m.lunarMonth} âm năm ${m.lunarYear} (${m.monthCanChi?.vn ?: ""}): $monthScore/100 — ${band(monthScore)}. ")
        if (ty != null) sb.append("Nền ${topicName(topic).lowercase()} của cả năm: ${ty.score}/100. ")
        if (nh != null) {
            val r = relation(natalTarget, nh)
            sb.append(when {
                nh == natalTarget -> "Nguyệt hạn đi đúng vào cung ${role.vn} nên tháng này chuyện ${topicName(topic).lowercase()} nổi lên rõ."
                r != null -> "Nguyệt hạn ở thế $r với cung ${role.vn} gốc nên tháng này có tác động gián tiếp."
                else -> "Nguyệt hạn không chạm cung ${role.vn} gốc."
            })
        }
        val concl = Paragraph(sb.toString().trim(), pol(monthScore), ParaKind.LEAD, topic, listOf("AN_KET_LUAN"),
            listOf(Why("AN_KET_LUAN", "Kết luận tháng", "Điểm tháng = năm ×0,50 + cung nguyệt hạn ×0,35 + Tứ Hóa tháng ×0,15 (mục 9 bước 6).", emptyList(), "Prompt mục 9")), false, "Kết luận")
        out.add(concl)
        if (nh != null) {
            val p = chart.palace(nh)
            val (cat, hung) = aux(chart, nh, topic)
            val mean = meaning(chart, nh, p.role)
            val t = "Nguyệt hạn tại ${nh.vn} (cung gốc ${p.role.vn}): ${mainStars(chart, nh)}${mean?.let { " — ${it.text}" } ?: ""}." +
                (if (cat.isNotEmpty()) " Cát tinh: ${cat.joinToString(", ")}." else "") + (if (hung.isNotEmpty()) " Sát/hung tinh: ${hung.joinToString(", ")}." else "") +
                " Tháng này việc thuộc cung ${p.role.vn} được chú ý nhiều hơn."
            out.add(para(t, 0, ParaKind.WEIGH, Topic.ofRole(p.role), "AN_NGUYET_HAN", "① Nguyệt hạn", "Nguyệt hạn tháng Giêng tính từ tiểu hạn và Thái Tuế, rồi đếm thuận theo tháng (mục 8.5).",
                emptyList(), mean?.draft ?: false))
        }
        m.nguyetTuHoa?.let { hoa ->
            val can = m.monthCanChi!!.can
            val (all, hits) = tuHoaLine(chart, hoa, can, natalTarget)
            val t = "Tứ Hóa tháng (can ${can.vn}): $all. " + (if (hits.isEmpty()) "Không hóa nào chạm cung ${role.vn} gốc." else "Chạm cung ${role.vn} gốc (${natalTarget.vn}): ${hits.joinToString(", ")}.")
            val p = hits.sumOf { h -> if (h.startsWith("Hóa Kỵ")) -1L else 1L }.toInt().coerceIn(-1, 1)
            out.add(para(t, p, ParaKind.WEIGH, topic, "AN_TU_HOA_THANG", "② Tứ Hóa tháng", "Tứ Hóa theo can tháng (mục 8.5).", listOf(all), false))
        }
        out.add(year(chart, yearData, topic, null, relative, compact = true).paragraphs.last().copy(title = "③ Nền của năm ${m.lunarYear}"))
        return Result(out, monthScore, concl)
    }

    /**
     * Lý do cho một năm khi dò “năm nào…”: cách vận hạn kích hoạt, Tứ Hóa lưu và sao lưu chạm cung chủ đề (gốc và theo
     * Thái Tuế), tách phần hỗ trợ và phần cần lưu ý. [negative] = câu hỏi theo hướng thận trọng.
     */
    private val LUU_GOOD = setOf(StarId.LOC_TON, StarId.THIEN_MA, StarId.HONG_LOAN, StarId.THIEN_HY, StarId.HY_THAN, StarId.LONG_DUC, StarId.NGUYET_DUC)

    fun yearReason(chart: Chart, data: Composer.PeriodData, topic: Topic?, relative: String? = null, negative: Boolean = false): String {
        val l = data.limits ?: return ""
        val role = roleFor(topic, relative)
        val pos = ArrayList<String>()
        val neg = ArrayList<String>()
        data.hits.filter { it.layer != Layer.GOC && (topic == null || topic in it.topics) && it.polarity != 0 }.sortedByDescending { it.strength }.take(3)
            .forEach { h -> (if (h.polarity > 0) pos else neg).add("${h.name} (${LAYER_SHORT.getValue(h.layer)})") }
        val luuTarget = l.luuThaiTue + role.ordinal
        val natalTarget = chart.palace(role).chi
        for (h in TuHoa.entries) {
            val c = l.luuTuHoa[h] ?: continue
            val r = when {
                c == natalTarget -> "nhập cung ${role.vn} gốc"
                c == luuTarget -> "nhập cung ${role.vn} năm"
                c == luuTarget.opposite || c == natalTarget.opposite -> "xung chiếu cung ${role.vn}"
                else -> null
            } ?: continue
            (if (h == TuHoa.KY) neg else pos).add("${h.vn} lưu $r")
        }
        val wanted = topic?.let { TOPIC_LUU[it] } ?: setOf(StarId.LOC_TON, StarId.KINH_DUONG, StarId.DA_LA, StarId.THIEN_MA)
        for ((id, c) in l.luuStars) if (id in wanted && (c == natalTarget || c == luuTarget)) {
            val good = id in LUU_GOOD || (id == StarId.DAO_HOA && topic == Topic.TINH_DUYEN)
            (if (good) pos else neg).add("${id.vn} lưu tại cung ${role.vn}")
        }
        val main = if (negative) neg else pos
        val other = if (negative) pos else neg
        if (main.isEmpty()) {
            // Không có sao lưu/cách nào nổi bật: nêu tầng mạnh nhất (hoặc yếu nhất với câu hỏi thận trọng).
            val ls = (topic?.let { data.topics[it]?.layers } ?: data.layers).filter { it.layer != Layer.GOC }
            val pick = if (negative) ls.minByOrNull { it.score.score } else ls.maxByOrNull { it.score.score }
            pick?.let { main.add("${LAYER_SHORT.getValue(it.layer)} ${it.score.score}/100 (cung ${role.vn} tại ${it.palace.vn}: ${brief(chart, it.palace, topic)})") }
        }
        return "Căn cứ: " + main.distinct().take(3).joinToString("; ") +
            (if (other.isNotEmpty()) ". ${if (negative) "Mặt đỡ" else "Cần lưu ý"}: " + other.distinct().take(2).joinToString("; ") else "")
    }
}
