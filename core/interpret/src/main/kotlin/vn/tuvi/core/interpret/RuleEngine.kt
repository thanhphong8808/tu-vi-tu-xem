package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind
import vn.tuvi.core.engine.TuHoa
import vn.tuvi.core.engine.TuHoaTable

/** Ngữ cảnh đánh giá: lá số gốc và (nếu có) các tầng vận hạn của năm xem. */
class EvalContext(val chart: Chart, val limits: LimitLayers?) {
    private val natalTuHoa: Map<TuHoa, StarId> by lazy { TuHoaTable.of(chart.yearCan, chart.config) }
    private val luuTuHoa: Map<TuHoa, StarId>? by lazy { limits?.let { TuHoaTable.of(it.yearCanChi.can, chart.config) } }

    fun pos(star: StarId): Chi? = chart.starPositions[star]
    fun luuPos(star: StarId): Chi? = limits?.luuStars?.get(star)
    fun brightness(star: StarId): Brightness? = pos(star)?.let { chart.palace(it).star(star)?.brightness }
    fun natalTuHoaStars(): Map<TuHoa, StarId> = natalTuHoa
    fun luuTuHoaStars(): Map<TuHoa, StarId>? = luuTuHoa

    fun locate(l: Locator, anchor: Chi): Chi? = when (l) {
        Locator.Self -> anchor
        Locator.Menh -> chart.menh
        Locator.Than -> chart.than
        is Locator.Role -> chart.palace(l.role).chi
        is Locator.StarAt -> pos(l.star)
        is Locator.LuuStarAt -> luuPos(l.star)
        is Locator.Point -> limits?.let {
            when (l.point) {
                LimitPoint.DAI_HAN -> if (it.beforeFirstDaiHan) null else it.daiHan
                LimitPoint.TIEU_HAN -> it.tieuHan
                LimitPoint.LUU_NIEN_DAI_HAN -> if (it.beforeFirstDaiHan) null else it.luuNienDaiHan
                LimitPoint.LUU_THAI_TUE -> it.luuThaiTue
            }
        }
    }

    fun evidence(star: StarId?, at: Chi, anchor: Chi, luu: Boolean, note: String? = null): Evidence =
        Evidence(star, at, chart.palace(at).role, Relation.between(anchor, at), luu, note)
}

/** Kết quả một lượt luận: các finding còn hiệu lực, finding bị luật cụ thể hơn chặn, và luật đã kích hoạt. */
data class Evaluation(
    val findings: List<Finding>,
    val suppressed: List<Pair<Finding, String>>,
    val firedRules: List<Pair<Rule, Chi>>,
    /** Tác động lên câu sao/bộ đồng cung/phụ tinh (ruleId → 1 làm nhẹ, 2 phản vi, 3 chặn): bộ chấm điểm dùng để chỉnh lực sao. */
    val adjust: Map<String, Int> = emptyMap(),
)

/**
 * RuleEngine (mục 9 bước 1–4): dò cách cục theo rule DSL, sinh câu đơn sao và bộ đồng cung từ KB,
 * rồi áp `suppress` / `soften` / `invert` (luật cụ thể hơn thắng luật chung). Chấm điểm và ghép câu là giai đoạn 4.
 */
class RuleEngine(private val kb: KnowledgeBase) {

    /** Các cung neo của một luật trên lá số (đã có ngữ cảnh vận hạn nếu là luật năm). */
    fun anchors(rule: Rule, ctx: EvalContext): List<Chi> = when (val a = rule.anchor) {
        is Anchor.Roles -> a.roles.map { ctx.chart.palace(it).chi }
        Anchor.AnyPalace -> Chi.entries
        Anchor.ThanPalace -> listOf(ctx.chart.than)
        is Anchor.Layer -> listOfNotNull(ctx.locate(Locator.Point(a.layer), ctx.chart.menh))
    }

    /** Luật [rule] có kích hoạt không, ở cung nào, với bằng chứng gì. */
    fun fire(rule: Rule, ctx: EvalContext): List<Pair<Chi, List<Evidence>>> {
        if (rule.scope != Scope.GOC && ctx.limits == null) return emptyList()
        val out = ArrayList<Pair<Chi, List<Evidence>>>()
        for (p in anchors(rule, ctx)) {
            val ev = ArrayList<Evidence>()
            if (rule.condition.eval(ctx, p, ev)) out.add(p to ev.distinct())
        }
        return out
    }

    /**
     * Luận lá số gốc (limits = null) hoặc một năm (limits ≠ null: chạy cả luật gốc lẫn luật năm).
     * [includeExpert] = bật các luật độ tin cậy C (chế độ chuyên sâu).
     */
    fun evaluate(chart: Chart, limits: LimitLayers? = null, includeExpert: Boolean = false): Evaluation {
        val ctx = EvalContext(chart, limits)
        val fired = ArrayList<Pair<Rule, Chi>>()
        val ruleFindings = ArrayList<Finding>()
        val modifiers = ArrayList<Triple<Rule, Chi, Effect>>()
        for (rule in kb.rules) {
            if (rule.expertOnly && !includeExpert) continue
            for ((palace, ev) in fire(rule, ctx)) {
                fired.add(rule to palace)
                modifiers.add(Triple(rule, palace, rule.effect))
                val topics = rule.effect.topics + (if (rule.effect.byPalace) listOf(Topic.ofRole(chart.palace(palace).role)) else emptyList())
                for (t in topics.distinct()) {
                    ruleFindings.add(
                        Finding(
                            ruleId = rule.id, scope = rule.scope, topic = t, polarity = rule.effect.polarity, weight = rule.effect.weight,
                            evidence = ev, textKey = rule.textKey, adviceKeys = rule.adviceKeys, palace = palace,
                            priority = rule.priority, type = rule.type, reliability = rule.reliability, reviewed = rule.reviewed,
                        ),
                    )
                }
            }
        }
        val all = ArrayList<Finding>()
        all.addAll(starFindings(chart))
        all.addAll(ruleFindings)

        // Áp tác động của luật lên finding khác (không tự tác động lên chính mình). Nhiều luật cùng nhắm một finding
        // → chỉ luật ưu tiên cao nhất (nhiều predicate nhất) có hiệu lực; bằng nhau thì chặn > đảo > làm nhẹ (mục 9).
        val suppressed = ArrayList<Pair<Finding, String>>()
        val modified = ArrayList<Finding>()
        val adjust = HashMap<String, Int>()
        for (f in all) {
            var best: Triple<Rule, Int, Int>? = null // luật, hành động (3 chặn, 2 đảo, 1 nhẹ), ưu tiên
            for ((rule, palace, eff) in modifiers) {
                if (f.ruleId == rule.id) continue
                val role = chart.palace(palace).role.name
                fun hit(patterns: List<String>) = patterns.any { matches(it.replace("@SELF", "@$role"), f.ruleId) }
                val action = when { hit(eff.suppress) -> 3; hit(eff.invert) -> 2; hit(eff.soften) -> 1; else -> continue }
                val b = best
                if (b == null || rule.priority > b.third || (rule.priority == b.third && action > b.second)) best = Triple(rule, action, rule.priority)
            }
            val b = best
            if (b != null && f.type == null) adjust[f.ruleId] = b.second
            when (b?.second) {
                null -> modified.add(f)
                3 -> suppressed.add(f to b.first.id)
                // Phản vi = xấu hóa tốt: câu đang xấu thành tốt; câu đã tốt (vd. sát tinh đắc địa) giữ nguyên.
                2 -> modified.add(f.copy(polarity = kotlin.math.abs(f.polarity)))
                else -> modified.add(f.copy(polarity = when { f.polarity > 0 -> f.polarity - 1; f.polarity < 0 -> f.polarity + 1; else -> 0 }))
            }
        }
        // Luật cụ thể hơn thắng luật chung (mục 5 bước 4, mục 9): hai luật ngược chiều, cùng cung neo, cùng chủ đề, cùng tầng,
        // mà bằng chứng của luật này chứa trọn bằng chứng của luật kia (cùng sao, thêm yếu tố) và nhiều predicate hơn
        // → luật kia bị chặn. Luật phản vi (PHAN_VI) không tham gia: chúng tác động qua chặn / đảo / làm nhẹ.
        val contenders = modified.filter { it.type != null && it.type != RuleType.PHAN_VI && it.polarity != 0 }
        val keys = contenders.associateWith { evidenceKeys(it) }
        val result = ArrayList<Finding>()
        for (f in modified) {
            val fk = keys[f]
            val winner = if (fk == null || fk.none { it.startsWith("*") }) null else contenders.firstOrNull { g ->
                g.ruleId != f.ruleId && g.palace == f.palace && g.scope == f.scope && g.topic == f.topic && g.priority > f.priority &&
                    g.polarity * f.polarity < 0 && keys.getValue(g).containsAll(fk)
            }
            if (winner != null) suppressed.add(f to winner.ruleId) else result.add(f)
        }
        return Evaluation(result, suppressed, fired, adjust)
    }

    /**
     * Câu đơn sao (KB phần a) và bộ đồng cung (phần b) cho từng cung. Bộ đồng cung chặn câu đơn sao của hai sao đó
     * trong cùng cung. ID: `STAR_<SAO>_<ĐỘ SÁNG>@<CUNG>`, `COMBO_<SAO1>_<SAO2>@<CUNG>`.
     */
    fun starFindings(chart: Chart): List<Finding> {
        val out = ArrayList<Finding>()
        for (chi in Chi.entries) {
            val p = chart.palace(chi)
            val main = p.chinhTinh
            val topic = Topic.ofRole(p.role)
            val combo = if (main.size == 2) kb.comboKey(main[0].id, main[1].id) else null
            if (combo != null) {
                val key = "combo.$combo.${p.role.name}"
                out.add(
                    Finding(
                        ruleId = "COMBO_${combo}@${p.role.name}", scope = Scope.GOC, topic = topic,
                        polarity = (main.sumOf { brightnessPolarity(it.brightness) }).coerceIn(-2, 2), weight = 1.5,
                        evidence = main.map { Evidence(it.id, chi, p.role, Relation.SELF) }, textKey = key, adviceKeys = emptyList(),
                        palace = chi, priority = 2, type = null, reliability = Reliability.A, reviewed = kb.texts[key]?.reviewed ?: false,
                    ),
                )
            }
            for (s in main) {
                if (combo != null) continue
                val b = s.brightness ?: Brightness.BINH
                val key = "star.${s.id.name}.${b.code}.${p.role.name}"
                out.add(
                    Finding(
                        ruleId = "STAR_${s.id.name}_${b.code}@${p.role.name}", scope = Scope.GOC, topic = topic,
                        polarity = brightnessPolarity(s.brightness), weight = 1.0,
                        evidence = listOf(Evidence(s.id, chi, p.role, Relation.SELF)), textKey = key, adviceKeys = emptyList(),
                        palace = chi, priority = 1, type = null, reliability = Reliability.A, reviewed = kb.texts[key]?.reviewed ?: false,
                    ),
                )
            }
            for (s in p.stars) {
                if (s.kind == StarKind.CHINH_TINH) continue
                val key = kb.minorKey(s.id, p.role) ?: continue
                out.add(
                    Finding(
                        ruleId = "PHU_${s.id.name}@${p.role.name}", scope = Scope.GOC, topic = topic,
                        polarity = kb.minorPolarity(s.id, s.brightness), weight = 0.5,
                        evidence = listOf(Evidence(s.id, chi, p.role, Relation.SELF)), textKey = key, adviceKeys = emptyList(),
                        palace = chi, priority = 1, type = null, reliability = Reliability.B, reviewed = kb.texts[key]?.reviewed ?: false,
                    ),
                )
            }
        }
        return out
    }

    companion object {
        /** Các yếu tố làm bằng chứng của một finding: sao (tiền tố `*`) và dấu Tuần / Triệt / Vô chính diệu tại cung nào. */
        fun evidenceKeys(f: Finding): Set<String> =
            f.evidence.mapTo(HashSet()) { e -> e.star?.let { "*" + (if (e.luu) "L." else "") + it.name } ?: "${e.note}@${e.chi.name}" }

        /** M +2, V/Đ +1, B 0, H −1 (thang polarity −2..+2; trọng số chi tiết theo mục 6 dùng ở giai đoạn 4). */
        fun brightnessPolarity(b: Brightness?): Int = when (b) {
            Brightness.MIEU -> 2
            Brightness.VUONG, Brightness.DAC -> 1
            Brightness.BINH, null -> 0
            Brightness.HAM -> -1
        }

        /** So khớp mẫu có `*` (khớp mọi chuỗi) với ruleId. */
        fun matches(pattern: String, id: String): Boolean {
            if (!pattern.contains('*')) return pattern == id
            val parts = pattern.split('*')
            var i = 0
            for ((n, part) in parts.withIndex()) {
                if (part.isEmpty()) continue
                val j = id.indexOf(part, i)
                if (j < 0 || (n == 0 && j != 0)) return false
                i = j + part.length
            }
            return parts.last().isEmpty() || id.endsWith(parts.last())
        }
    }
}
