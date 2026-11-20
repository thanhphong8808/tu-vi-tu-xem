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
import kotlin.math.abs
import kotlin.math.roundToInt

/** Một yếu tố góp vào điểm, để giải thích “Vì sao?”. [value] theo đơn vị thô trước khi quy đổi ra điểm. */
data class Contribution(val label: String, val value: Double)

/** Điểm 5–95 (mục 9 bước 5) kèm tổng thô và các yếu tố nặng nhất. */
data class Score(val score: Int, val sum: Double, val top: List<Contribution>)

/** Lớp phủ năm/tháng chồng lên lá số gốc: sao lưu và Tứ Hóa lưu / đại hạn / tháng. */
data class Overlay(val stars: Map<StarId, Chi> = emptyMap(), val tuHoa: List<Triple<TuHoa, Chi, String>> = emptyList()) {
    val isEmpty: Boolean get() = stars.isEmpty() && tuHoa.isEmpty()
}

/** Điểm một chủ đề ở một tầng: cung đọc chính của tầng đó và điểm. */
data class LayerScore(val layer: Layer, val weight: Double, val palace: Chi, val score: Score)

/** Điểm một chủ đề sau khi tổng hợp tầng (mục 9 bước 6). */
data class TopicScore(val topic: Topic, val score: Int, val layers: List<LayerScore>)

/** Bảng “Chủ đề → cung đọc chính” (mục 9). */
data class TopicSpec(
    val main: List<PalaceRole>,
    val secondary: List<PalaceRole>,
    val withThan: Boolean = false,
    /** false: chủ đề đọc theo sao chỉ báo (Pháp lý), không theo điểm chung của cung. */
    val starBased: Boolean = true,
)

/**
 * Chấm điểm (mục 9 bước 1–6). Tổng thô Σ = Σ(lực sao × quan hệ cung × Tuần/Triệt) + Σ(chiều × trọng số luật) rồi
 * `điểm = clamp(50 + 10 × Σ × đơn vị, 5, 95)`. Đơn vị quy đổi được hiệu chỉnh để điểm cung trải đều quanh 50 (D-38).
 */
class Scorer(private val kb: KnowledgeBase) {

    companion object {
        val TOPICS: Map<Topic, TopicSpec> = mapOf(
            Topic.TINH_CACH to TopicSpec(listOf(PalaceRole.MENH), listOf(PalaceRole.PHUC_DUC, PalaceRole.THIEN_DI), withThan = true),
            Topic.SU_NGHIEP to TopicSpec(listOf(PalaceRole.QUAN_LOC), listOf(PalaceRole.MENH, PalaceRole.THIEN_DI, PalaceRole.NO_BOC)),
            Topic.TAI_CHINH to TopicSpec(listOf(PalaceRole.TAI_BACH), listOf(PalaceRole.DIEN_TRACH, PalaceRole.PHUC_DUC, PalaceRole.MENH)),
            Topic.TINH_DUYEN to TopicSpec(listOf(PalaceRole.PHU_THE), listOf(PalaceRole.MENH, PalaceRole.PHUC_DUC, PalaceRole.NO_BOC), withThan = true),
            Topic.GIA_DAO to TopicSpec(listOf(PalaceRole.PHU_MAU, PalaceRole.HUYNH_DE, PalaceRole.TU_TUC), listOf(PalaceRole.PHUC_DUC, PalaceRole.DIEN_TRACH)),
            Topic.SUC_KHOE to TopicSpec(listOf(PalaceRole.TAT_ACH), listOf(PalaceRole.MENH, PalaceRole.PHUC_DUC)),
            Topic.DI_CHUYEN to TopicSpec(listOf(PalaceRole.THIEN_DI), listOf(PalaceRole.MENH, PalaceRole.QUAN_LOC)),
            Topic.NHA_DAT to TopicSpec(listOf(PalaceRole.DIEN_TRACH), listOf(PalaceRole.TAI_BACH, PalaceRole.TU_TUC)),
            Topic.PHAP_LY to TopicSpec(listOf(PalaceRole.QUAN_LOC, PalaceRole.THIEN_DI), listOf(PalaceRole.NO_BOC), starBased = false),
            Topic.PHUC_DUC to TopicSpec(listOf(PalaceRole.PHUC_DUC), listOf(PalaceRole.MENH)),
        )

        /** Trọng số tầng khi luận năm (mục 9 bước 6). */
        val YEAR_WEIGHTS = mapOf(
            Layer.GOC to 0.10, Layer.DAI_HAN to 0.30, Layer.LUU_NIEN to 0.25, Layer.TIEU_HAN to 0.20, Layer.LUU_THAI_TUE to 0.15,
        )

        /** Đại vận 10 năm: gốc 0,25, đại hạn 0,75 (D-38). */
        val DECADE_WEIGHTS = mapOf(Layer.GOC to 0.25, Layer.DAI_HAN to 0.75)

        /** Quan hệ cung (mục 9 bước 1). */
        const val W_SELF = 1.0
        const val W_XUNG = 0.8
        const val W_TAM_HOP = 0.6
        const val W_GIAP = 0.4
        const val W_NHI_HOP = 0.3
        const val W_BORROWED = 0.7

        /**
         * Hiệu chỉnh (D-38): tổng thô của một lá số trung bình không bằng 0 (phụ tinh tốt nhiều hơn xấu, chính tinh phần
         * lớn sáng), nên trừ mức nền đo trên lá số ngẫu nhiên để 50 điểm = mức trung bình; đơn vị quy đổi chọn để độ lệch
         * chuẩn khoảng 13 điểm. Công thức vẫn là 50 + 10 × Σ, với Σ = (tổng thô − nền) × đơn vị.
         */
        const val PALACE_BASE = 4.4
        const val UNIT_PALACE = 0.34
        const val UNIT_TOPIC = 0.29
        const val UNIT_PHAP_LY = 1.6
        val TOPIC_BASE: Map<Topic, Double> = mapOf(
            Topic.TINH_CACH to 6.7, Topic.SU_NGHIEP to 8.1, Topic.TAI_CHINH to 6.6, Topic.TINH_DUYEN to 5.8, Topic.GIA_DAO to 5.8,
            Topic.SUC_KHOE to 5.5, Topic.DI_CHUYEN to 6.3, Topic.NHA_DAT to 5.7, Topic.PHAP_LY to -0.5, Topic.PHUC_DUC to 6.7,
        )
        /** Một luật chiều p, trọng số w góp p × w × RULE_RAW vào tổng thô. */
        const val RULE_RAW = 0.5

        /** Cặp sao giáp có nghĩa (mục 9 bước 1). */
        val GIAP_PAIRS = listOf(
            StarId.TA_PHU to StarId.HUU_BAT, StarId.VAN_XUONG to StarId.VAN_KHUC, StarId.THIEN_KHOI to StarId.THIEN_VIET,
            StarId.THAI_DUONG to StarId.THAI_AM, StarId.DIA_KHONG to StarId.DIA_KIEP, StarId.KINH_DUONG to StarId.DA_LA,
            StarId.HOA_TINH to StarId.LINH_TINH, StarId.HOA_QUYEN to StarId.HOA_LOC,
        )

        /** Lực phụ tinh (độ lớn; chiều lấy từ meta KB theo đắc/hãm). */
        private val MINOR_MAG: Map<StarId, Double> = buildMap {
            put(StarId.HOA_LOC, 1.2); put(StarId.HOA_QUYEN, 1.0); put(StarId.HOA_KHOA, 1.0); put(StarId.HOA_KY, 1.2); put(StarId.LOC_TON, 1.0)
            for (s in listOf(StarId.TA_PHU, StarId.HUU_BAT, StarId.VAN_XUONG, StarId.VAN_KHUC, StarId.THIEN_KHOI, StarId.THIEN_VIET)) put(s, 0.8)
            for (s in listOf(StarId.KINH_DUONG, StarId.DA_LA, StarId.HOA_TINH, StarId.LINH_TINH, StarId.DIA_KHONG, StarId.DIA_KIEP)) put(s, 1.0)
            for (s in listOf(StarId.THIEN_MA, StarId.DAI_HAO, StarId.TIEU_HAO, StarId.THIEN_HINH, StarId.THIEN_RIEU)) put(s, 0.6)
        }
        private const val MINOR_DEFAULT = 0.3
        private const val LUU_STAR_FACTOR = 0.6
        private const val LUU_HOA_FACTOR = 0.7
        private val TU_HOA_VALUE = mapOf(TuHoa.LOC to 1.2, TuHoa.QUYEN to 0.9, TuHoa.KHOA to 0.8, TuHoa.KY to -1.2)

        /** Sao chỉ báo của chủ đề Pháp lý, thị phi (mục 9). */
        private val PHAP_LY_INDICATORS = mapOf(
            StarId.QUAN_PHU to -1.0, StarId.HOA_KY to -1.0, StarId.THIEN_HINH to -0.6, StarId.KINH_DUONG to -0.4,
            StarId.THIEN_GIAI to 0.4, StarId.DIA_GIAI to 0.4, StarId.GIAI_THAN to 0.4, StarId.HOA_KHOA to 0.5,
        )

        fun toScore(sum: Double, unit: Double): Int = (50 + 10 * sum * unit).roundToInt().coerceIn(5, 95)

        /**
         * Tuần/Triệt tọa thủ (mục 9 bước 3): lực ×(1 − 0,5 × độ mạnh). Triệt mạnh 1,0 trước 30 tuổi, 0,5 sau;
         * Tuần 0,5 trước 30, 1,0 sau. Không có tuổi (luận trọn đời) → độ mạnh 0,75. Ở cung xung chiếu: phần chiếu ×0,5.
         */
        fun tuanTrietFactor(hasTuan: Boolean, hasTriet: Boolean, age: Int?, xung: Boolean): Double {
            if (!hasTuan && !hasTriet) return 1.0
            if (xung) return 0.5
            val triet = if (!hasTriet) 0.0 else if (age == null) 0.75 else if (age < 30) 1.0 else 0.5
            val tuan = if (!hasTuan) 0.0 else if (age == null) 0.75 else if (age < 30) 0.5 else 1.0
            return 1.0 - 0.5 * maxOf(triet, tuan)
        }

        fun frame(anchor: Chi): (PalaceRole) -> Chi = { role -> anchor + role.ordinal }
    }

    private fun roleLabel(chart: Chart, chi: Chi) = "${chart.palace(chi).role.vn} (${chi.vn})"

    /** Lực của một sao gốc tại cung của nó, đã tính phản vi/làm nhẹ do luật (Evaluation.adjust). */
    private fun starValue(chart: Chart, eval: Evaluation, chi: Chi, id: StarId): Double {
        val p = chart.palace(chi)
        val s = p.star(id) ?: return 0.0
        val role = p.role.name
        return if (s.kind == StarKind.CHINH_TINH) {
            val base = (s.brightness ?: Brightness.BINH).scoreWeight
            val main = p.chinhTinh
            val key = if (main.size == 2) kb.comboKey(main[0].id, main[1].id)?.let { "COMBO_$it@$role" } else null
            adjusted(base, eval.adjust[key ?: "STAR_${id.name}_${(s.brightness ?: Brightness.BINH).code}@$role"])
        } else {
            if (id !in kb.minor) return 0.0
            val base = kb.minorPolarity(id, s.brightness) * (MINOR_MAG[id] ?: MINOR_DEFAULT)
            adjusted(base, eval.adjust["PHU_${id.name}@$role"])
        }
    }

    private fun adjusted(v: Double, action: Int?): Double = when (action) {
        2 -> abs(v)
        1 -> v * 0.5
        else -> v
    }

    private fun luuValue(id: StarId): Double {
        if (id == StarId.THAI_TUE || id !in kb.minor) return 0.0
        return kb.minorPolarity(id, null) * (MINOR_MAG[id] ?: MINOR_DEFAULT) * LUU_STAR_FACTOR
    }

    /** Các yếu tố của cung [q] nhìn từ cung đang xét với trọng số quan hệ [rel]. */
    private fun palaceTerms(
        chart: Chart, eval: Evaluation, q: Chi, rel: Double, relName: String, age: Int?, xung: Boolean,
        overlay: Overlay?, mainOnly: Boolean = false, skipMain: Boolean = false,
    ): List<Contribution> {
        val p = chart.palace(q)
        val tt = tuanTrietFactor(p.hasTuan, p.hasTriet, age, xung)
        val out = ArrayList<Contribution>()
        for (s in p.stars) {
            val main = s.kind == StarKind.CHINH_TINH
            if (mainOnly && !main) continue
            if (skipMain && main) continue
            val v = starValue(chart, eval, q, s.id) * rel * tt
            if (v != 0.0) out.add(Contribution("${s.id.vn}${if (main && s.brightness != null) " (${s.brightness!!.vn})" else ""} $relName", v))
        }
        if (overlay != null && !mainOnly) {
            for ((id, at) in overlay.stars) if (at == q) {
                val v = luuValue(id) * rel * tt
                if (v != 0.0) out.add(Contribution("L.${id.vn} $relName", v))
            }
            for ((hoa, at, label) in overlay.tuHoa) if (at == q) {
                val v = (TU_HOA_VALUE[hoa] ?: 0.0) * LUU_HOA_FACTOR * rel * tt
                out.add(Contribution("$label $relName", v))
            }
        }
        return out
    }

    /** Bộ nhớ đệm trong một lượt luận: theo đúng đối tượng Evaluation (mỗi Evaluation thuộc một lá số). */
    private val cache = java.util.IdentityHashMap<Evaluation, HashMap<Triple<Chi, Int?, Overlay?>, List<Contribution>>>()

    /** Các yếu tố sao của cung [target] trong bối cảnh tam phương tứ chính, giáp, nhị hợp (mục 9 bước 1–3). */
    fun starContributions(chart: Chart, eval: Evaluation, target: Chi, age: Int?, overlay: Overlay?): List<Contribution> =
        cache.getOrPut(eval) { HashMap() }.getOrPut(Triple(target, age, overlay?.takeIf { !it.isEmpty })) {
            computeStarContributions(chart, eval, target, age, overlay)
        }

    private fun computeStarContributions(chart: Chart, eval: Evaluation, target: Chi, age: Int?, overlay: Overlay?): List<Contribution> {
        val out = ArrayList<Contribution>()
        val vcd = chart.palace(target).chinhTinh.isEmpty()
        out += palaceTerms(chart, eval, target, W_SELF, "tọa thủ", age, false, overlay)
        val xung = target.opposite
        if (vcd) {
            // Vô chính diệu: mượn chính tinh cung xung chiếu, hệ số 0,7 (mục 9 bước 2).
            out += palaceTerms(chart, eval, xung, W_BORROWED, "(mượn từ ${xung.vn})", age, false, null, mainOnly = true)
            out += palaceTerms(chart, eval, xung, W_XUNG, "xung chiếu", age, true, overlay, skipMain = true)
        } else {
            out += palaceTerms(chart, eval, xung, W_XUNG, "xung chiếu", age, true, overlay)
        }
        out += palaceTerms(chart, eval, target + 4, W_TAM_HOP, "tam hợp", age, false, overlay)
        out += palaceTerms(chart, eval, target + 8, W_TAM_HOP, "tam hợp", age, false, overlay)
        out += palaceTerms(chart, eval, Chi.of(1 - target.ordinal), W_NHI_HOP, "nhị hợp", age, false, overlay)
        // Giáp: chỉ cặp sao có nghĩa, mỗi bên một sao.
        val left = target - 1
        val right = target + 1
        for ((a, b) in GIAP_PAIRS) {
            val pa = chart.starPositions[a]
            val pb = chart.starPositions[b]
            if (pa != null && pb != null && ((pa == left && pb == right) || (pa == right && pb == left))) {
                val v = (starValue(chart, eval, pa, a) + starValue(chart, eval, pb, b)) * W_GIAP
                if (v != 0.0) out.add(Contribution("${a.vn} – ${b.vn} giáp cung", v))
            }
        }
        return out
    }

    private fun ruleTerms(findings: List<Finding>, filter: (Finding) -> Boolean): List<Contribution> =
        findings.filter { it.type != null && filter(it) }.distinctBy { it.ruleId to it.palace }.mapNotNull { f ->
            val r = kb.rule(f.ruleId) ?: return@mapNotNull null
            val v = f.polarity * f.weight * RULE_RAW
            if (v == 0.0) null else Contribution("${r.type.vn} “${r.name}”", v)
        }

    private fun score(terms: List<Contribution>, base: Double, unit: Double): Score {
        val sum = terms.sumOf { it.value }
        return Score(toScore(sum - base, unit), sum, terms.sortedByDescending { abs(it.value) }.take(5))
    }

    /** Điểm một cung: sao trong bối cảnh + luật neo tại cung đó. */
    fun palace(chart: Chart, eval: Evaluation, p: Chi, age: Int? = null, overlay: Overlay? = null, rules: List<Finding> = eval.findings): Score =
        score(starContributions(chart, eval, p, age, overlay) + ruleTerms(rules) { it.palace == p }, PALACE_BASE, UNIT_PALACE)

    /**
     * Điểm một chủ đề trong một khung cung [frame] (vai trò → cung): trung bình cung chính + 0,4 × trung bình cung phụ
     * + sao chỉ báo (Pháp lý) + các luật có chủ đề này trong [rules].
     */
    fun topic(
        chart: Chart, eval: Evaluation, topic: Topic, frame: (PalaceRole) -> Chi, age: Int?, overlay: Overlay?,
        rules: List<Finding>, than: Chi? = null,
    ): Score {
        val spec = TOPICS.getValue(topic)
        val terms = ArrayList<Contribution>()
        val mains = spec.main.map(frame)
        val secs = spec.secondary.map(frame) + (if (spec.withThan && than != null && than !in spec.main.map(frame)) listOf(than) else emptyList())
        if (spec.starBased) {
            for (m in mains) starContributions(chart, eval, m, age, overlay).forEach {
                terms.add(Contribution("${it.label} → ${roleLabel(chart, m)}", it.value / mains.size))
            }
            for (s in secs) starContributions(chart, eval, s, age, overlay).forEach {
                terms.add(Contribution("${it.label} → ${roleLabel(chart, s)}", it.value * 0.4 / secs.size))
            }
        } else {
            for ((list, w) in listOf(mains to 1.0, secs to 0.5)) for (c in list) {
                val p = chart.palace(c)
                val tt = tuanTrietFactor(p.hasTuan, p.hasTriet, age, false)
                for (s in p.stars) {
                    var v = PHAP_LY_INDICATORS[s.id] ?: continue
                    if (s.id == StarId.THIEN_HINH && s.brightness == Brightness.DAC) v = 0.4
                    terms.add(Contribution("${s.id.vn} tại ${roleLabel(chart, c)}", v * w * tt))
                }
                if (p.chinhTinh.any { it.id == StarId.CU_MON && it.brightness == Brightness.HAM }) {
                    terms.add(Contribution("Cự Môn hãm tại ${roleLabel(chart, c)}", -0.6 * w * tt))
                }
                overlay?.tuHoa?.filter { it.second == c && it.first == TuHoa.KY }?.forEach {
                    terms.add(Contribution("${it.third} tại ${roleLabel(chart, c)}", -1.0 * w))
                }
            }
        }
        terms += ruleTerms(rules) { topic == it.topic }
        return score(terms, TOPIC_BASE.getValue(topic), if (spec.starBased) UNIT_TOPIC else UNIT_PHAP_LY)
    }

    /** Khung cung của một tầng: gốc dùng vai trò gốc; tầng khác xoay 12 vai trò từ cung của tầng (mục 8.1, 8.4). */
    fun layerAnchor(chart: Chart, layer: Layer, limits: LimitLayers?): Chi? = when (layer) {
        Layer.GOC -> chart.menh
        Layer.DAI_HAN -> limits?.takeIf { !it.beforeFirstDaiHan }?.daiHan
        Layer.LUU_NIEN -> limits?.takeIf { !it.beforeFirstDaiHan }?.luuNienDaiHan
        Layer.TIEU_HAN -> limits?.tieuHan
        Layer.LUU_THAI_TUE -> limits?.luuThaiTue
    }

    /** Lớp phủ của tầng: Lưu Thái Tuế mang sao lưu + Tứ Hóa lưu; đại hạn mang Tứ Hóa đại hạn (nếu bật). */
    fun layerOverlay(layer: Layer, limits: LimitLayers?): Overlay? = when {
        limits == null -> null
        layer == Layer.LUU_THAI_TUE -> Overlay(limits.luuStars, limits.luuTuHoa.map { (h, c) -> Triple(h, c, "${h.vn} lưu") })
        layer == Layer.DAI_HAN -> limits.daiHanTuHoa?.let { m -> Overlay(tuHoa = m.map { (h, c) -> Triple(h, c, "${h.vn} đại hạn") }) }
        else -> null
    }

    /**
     * Tổng hợp tầng cho một chủ đề (mục 9 bước 6). [weights] = trọng số tầng (năm hoặc đại vận); tầng không có
     * (trước đại hạn đầu) bị bỏ và các trọng số còn lại được chia lại cho đủ 1.
     */
    fun topicLayered(chart: Chart, eval: Evaluation, topic: Topic, limits: LimitLayers?, weights: Map<Layer, Double>): TopicScore {
        val age = limits?.tuoiAm
        val parts = ArrayList<LayerScore>()
        for ((layer, w) in weights) {
            val anchor = layerAnchor(chart, layer, limits) ?: continue
            val rules = eval.findings.filter { f -> kb.rule(f.ruleId)?.scoringLayer() == layer }
            val s = topic(
                chart, eval, topic, frame(anchor), if (layer == Layer.GOC) null else age, layerOverlay(layer, limits), rules,
                than = if (layer == Layer.GOC) chart.than else null,
            )
            parts.add(LayerScore(layer, w, frame(anchor)(TOPICS.getValue(topic).main.first()), s))
        }
        val total = parts.sumOf { it.weight }
        val score = (parts.sumOf { it.weight * it.score.score } / total).roundToInt().coerceIn(5, 95)
        return TopicScore(topic, score, parts.map { it.copy(weight = it.weight / total) })
    }

    /** Điểm “Mệnh của tầng”: cung neo tầng đó, cho thẻ tóm tắt. */
    fun layerMenh(chart: Chart, eval: Evaluation, layer: Layer, limits: LimitLayers?): LayerScore? {
        val anchor = layerAnchor(chart, layer, limits) ?: return null
        val rules = eval.findings.filter { f -> kb.rule(f.ruleId)?.scoringLayer() == layer }
        val s = palace(chart, eval, anchor, if (layer == Layer.GOC) null else limits?.tuoiAm, layerOverlay(layer, limits), rules)
        return LayerScore(layer, 0.0, anchor, s)
    }

    /**
     * Điểm tháng (mục 9 bước 6): năm 0,50 + cung nguyệt hạn 0,35 + Tứ Hóa tháng 0,15.
     * Cung nguyệt hạn chấm như “Mệnh của tháng” với sao lưu của năm.
     */
    fun month(chart: Chart, eval: Evaluation, yearScore: Int, m: LimitLayers): Int {
        val nh = m.nguyetHan ?: return yearScore
        val overlay = Overlay(m.luuStars)
        val pal = palace(chart, eval, nh, m.tuoiAm, overlay, emptyList()).score
        var hoa = 0.0
        for ((h, c) in m.nguyetTuHoa ?: emptyMap()) {
            val d = c.stepsFrom(nh)
            val rel = when (d) { 0 -> W_SELF; 6 -> W_XUNG; 4, 8 -> W_TAM_HOP; else -> 0.0 }
            hoa += (TU_HOA_VALUE[h] ?: 0.0) * rel
        }
        val hoaScore = toScore(hoa, 1.0)
        return (0.5 * yearScore + 0.35 * pal + 0.15 * hoaScore).roundToInt().coerceIn(5, 95)
    }

    /** Điểm ngày (mục 9 bước 6): tháng 0,50 + cung nhật hạn 0,35 + can chi ngày xung/hợp tuổi 0,15. */
    fun day(chart: Chart, eval: Evaluation, monthScore: Int, d: LimitLayers): Int {
        val nh = d.nhatHan ?: return monthScore
        val pal = palace(chart, eval, nh, d.tuoiAm, Overlay(d.luuStars), emptyList()).score
        val dayChi = d.dayCanChi?.chi
        val cc = when {
            dayChi == null -> 50
            dayChi.stepsFrom(chart.yearChi) == 6 -> 25                       // lục xung
            dayChi.tamHopGroup == chart.yearChi.tamHopGroup -> 75             // tam hợp
            (dayChi.ordinal + chart.yearChi.ordinal) % 12 == 1 -> 70          // lục hợp
            else -> 50
        }
        return (0.5 * monthScore + 0.35 * pal + 0.15 * cc).roundToInt().coerceIn(5, 95)
    }

    /** 12 tháng âm của năm (tháng nhuận dùng chung cung với tháng chính). */
    fun months(chart: Chart, eval: Evaluation, yearScore: Int, lunarYear: Int): List<Pair<Int, Int>> =
        (1..12).map { mo -> mo to month(chart, eval, yearScore, LimitEngine.forMonth(chart, lunarYear, mo)) }
}
