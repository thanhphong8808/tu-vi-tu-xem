package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind
import vn.tuvi.core.engine.TuHoa
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Bộ ghép câu luận (mục 9 bước 7, mục 11): biến finding + điểm thành bản luận trọn đời / đại vận / năm / tháng.
 *
 * Quy tắc chống câu trái ngược: trong mỗi khối, các câu một chiều (LEAD, PLAIN, EVENT) của cùng một chủ đề luôn cùng
 * chiều với chiều trội của chủ đề; mọi yếu tố ngược chiều được gộp vào đúng một câu cân nhắc (WEIGH):
 * “Có cả … lẫn …, nghiêng về … vì …”. Câu luận gốc của các yếu tố đó vẫn mở được trong “Vì sao?”.
 */
class Composer(private val kb: KnowledgeBase) {
    private val engine = RuleEngine(kb)

    // ———————————————————————————— dữ liệu trung gian ————————————————————————————

    /** Một luật đã kích hoạt, gộp theo (luật, cung neo); luật neo “mọi cung” cùng chiều gộp nhiều cung. */
    private class Item(val rule: Rule, val palaces: List<Chi>, val findings: List<Finding>) {
        val polarity: Int get() = findings.first().polarity
        val weight: Double get() = rule.effect.weight
        val topics: Set<Topic> get() = findings.mapTo(LinkedHashSet()) { it.topic }
        val strength: Double get() = abs(polarity) * weight + weight * 0.1
    }

    private class EventHit(val event: Event, val layers: Set<Layer>, val level: Likelihood, val items: List<Item>)

    /** Một yếu tố đưa vào khối chủ đề: câu luận (đã điền slot), tên ngắn, chiều, căn cứ. */
    private class Piece(val text: String, val name: String, val polarity: Int, val strength: Double, val ruleIds: List<String>, val why: List<Why>, val draft: Boolean)

    private inner class Ctx(val chart: Chart, val limits: LimitLayers?, val eval: Evaluation, val scorer: Scorer) {
        val yearLabel: String? = limits?.let { "${it.lunarYear} (${it.yearCanChi.vn})" }
        fun roles(ps: List<Chi>) = ps.joinToString(", ") { chart.palace(it).role.vn }
        fun fill(text: String, ps: List<Chi>): String = text
            .replace("{cung}", roles(ps))
            .replace("{nam}", yearLabel ?: "này")
            .replace("{tuoi}", limits?.tuoiAm?.toString() ?: "")
        fun place(chi: Chi) = "${chart.palace(chi).role.vn} (${chi.vn})"
    }

    private fun items(ctx: Ctx, scopes: Set<Scope>): List<Item> {
        val byPalace = ctx.eval.findings.filter { it.type != null && it.scope in scopes }.groupBy { it.ruleId to it.palace }
        val out = ArrayList<Item>()
        for ((key, fs) in byPalace) {
            val rule = kb.rule(key.first) ?: continue
            val idx = if (rule.anchor == Anchor.AnyPalace) out.indexOfFirst { it.rule.id == rule.id && it.polarity == fs.first().polarity } else -1
            if (idx >= 0) out[idx] = Item(rule, out[idx].palaces + key.second, out[idx].findings + fs) else out.add(Item(rule, listOf(key.second), fs))
        }
        return out.sortedByDescending { it.strength }
    }

    private fun whyOf(ctx: Ctx, it: Item): Why = Why(
        id = it.rule.id,
        title = "${it.rule.type.vn}: ${it.rule.name}",
        detail = ctx.fill(kb.texts[it.rule.textKey]?.text ?: it.rule.name, it.palaces),
        evidence = it.findings.flatMap { f -> f.evidence }.distinct().map { e -> e.toString() },
        source = it.rule.source,
    )

    private fun pieceOf(ctx: Ctx, it: Item): Piece {
        val t = kb.texts[it.rule.textKey]
        val where = if (it.rule.anchor is Anchor.Roles && (it.rule.anchor as Anchor.Roles).roles.size == 1) "" else " (${ctx.roles(it.palaces)})"
        return Piece(ctx.fill(t?.text ?: it.rule.name, it.palaces), it.rule.name + where, it.polarity, it.strength, listOf(it.rule.id), listOf(whyOf(ctx, it)), t?.reviewed != true)
    }

    /** Câu luận sao chính / bộ đồng cung của một cung (KB phần a, b). */
    private fun mainStarPiece(ctx: Ctx, chi: Chi): Piece? {
        val main = ctx.eval.findings.filter { it.palace == chi && (it.ruleId.startsWith("COMBO_") || it.ruleId.startsWith("STAR_")) }
        val p = ctx.chart.palace(chi)
        val texts = main.mapNotNull { kb.texts[it.textKey] }
        if (texts.isEmpty()) return null
        val name = p.chinhTinh.joinToString(" – ") { s -> s.id.vn + (s.brightness?.let { b -> " ${b.vn.lowercase()}" } ?: "") }
        val pol = main.sumOf { it.polarity }.coerceIn(-2, 2)
        return Piece(
            texts.joinToString(" ") { ctx.fill(it.text, listOf(chi)) }, "$name tại ${p.role.vn}", pol, abs(pol) * 1.2 + 0.1,
            main.map { it.ruleId },
            main.map { m -> Why(m.ruleId, "Sao chính tại cung ${p.role.vn}", ctx.fill(kb.texts[m.textKey]?.text ?: "", listOf(chi)), m.evidence.map { e -> e.toString() }) },
            texts.any { !it.reviewed },
        )
    }

    /**
     * “Lưu cung”: 12 vai trò xoay theo cung của tầng (đại hạn cho đại vận, tiểu hạn cho năm). Câu luận lấy đúng câu
     * sao chính theo vai trò đó trong KB (vd. Tử Vi miếu tại cung Tật Ách), kèm lời dẫn cho biết cung gốc.
     */
    private fun framePiece(ctx: Ctx, layer: Layer, anchor: Chi, role: PalaceRole, period: String): Piece? {
        val chi = anchor + role.ordinal
        val p = ctx.chart.palace(chi)
        val borrowed = p.chinhTinh.isEmpty()
        val stars = if (borrowed) ctx.chart.palace(chi.opposite).chinhTinh else p.chinhTinh
        if (stars.isEmpty()) return null
        val combo = if (stars.size == 2) kb.comboKey(stars[0].id, stars[1].id) else null
        val keys = if (combo != null) listOf("combo.$combo.${role.name}") else stars.map { "star.${it.id.name}.${(it.brightness ?: vn.tuvi.core.engine.Brightness.BINH).code}.${role.name}" }
        val texts = keys.mapNotNull { kb.texts[it] }
        if (texts.isEmpty()) return null
        var pol = stars.sumOf { RuleEngine.brightnessPolarity(it.brightness) }.coerceIn(-2, 2)
        if (borrowed && pol != 0) pol = pol.sign
        val starNames = stars.joinToString(" – ") { s -> s.id.vn + (s.brightness?.let { b -> " ${b.vn.lowercase()}" } ?: "") }
        val lead = "Theo ${layer.vn}, cung ${role.vn} của $period rơi vào ${chi.vn} (cung gốc ${p.role.vn})" +
            (if (borrowed) ", vô chính diệu, mượn chính tinh cung ${chi.opposite.vn}" else "") + ". "
        val id = "LUU_CUNG_${layer.name}_${role.name}"
        return Piece(
            lead + texts.joinToString(" ") { ctx.fill(it.text, listOf(chi)) }, "$starNames tại ${role.vn} của $period", pol, abs(pol) * 1.0 + 0.1,
            listOf(id) + keys,
            listOf(Why(id, "Lưu cung ${role.vn} (${layer.vn})", "Xoay 12 vai trò từ cung ${layer.vn} ${ctx.place(anchor)}: cung ${role.vn} của $period tại ${chi.vn}.",
                stars.map { s -> "${s.id.vn}${s.brightness?.let { b -> " (${b.vn})" } ?: ""} tại ${chi.opposite.takeIf { borrowed }?.vn ?: chi.vn}" }, "Prompt mục 8.1, 8.4")),
            texts.any { !it.reviewed },
        )
    }

    private fun layerLead(l: Layer) = if (l == Layer.LUU_THAI_TUE) "Lưu Thái Tuế và sao lưu" else layerShort(l)
    private fun layerShort(l: Layer) = when (l) { Layer.DAI_HAN -> "đại hạn"; Layer.LUU_NIEN -> "lưu niên đại hạn"; Layer.TIEU_HAN -> "tiểu hạn"; Layer.LUU_THAI_TUE -> "Lưu Thái Tuế"; Layer.GOC -> "lá số gốc" }

    // ———————————————————————————— diễn đạt chung ————————————————————————————

    private fun band(score: Int) = when {
        score >= 70 -> "rất thuận"
        score >= 55 -> "khá thuận"
        score >= 46 -> "cân bằng"
        score >= 35 -> "cần thận trọng"
        else -> "nhiều thử thách"
    }

    /** Chiều của câu mở theo điểm: ≥ 55 thuận, ≤ 45 cần lưu ý, giữa là cân bằng. */
    private fun polOf(score: Int) = when { score >= 55 -> 1; score <= 45 -> -1; else -> 0 }

    private fun names(list: List<String>): String {
        val l = list.distinct()
        val shown = l.take(3)
        val s = if (shown.size <= 1) shown.joinToString("") else shown.dropLast(1).joinToString(", ") + " và " + shown.last()
        return if (l.size > 3) "$s…" else s
    }

    private fun stanceText(st: Stance): String = kb.texts["the.${st.key}"]?.text ?: st.vn

    private fun note(key: String, topic: Topic?, why: List<Why> = emptyList(), id: String = key): Paragraph? =
        kb.texts[key]?.let { Paragraph(it.text, 0, ParaKind.NOTE, topic, listOf(id), why.ifEmpty { listOf(Why(id, "Ghi chú", it.text, emptyList(), it.source)) }, !it.reviewed) }

    /**
     * Ghép một khối chủ đề. [lead] là câu mở (đã có chiều). [extra] là câu mô tả sao chính (luận trọn đời, cung).
     * [dominantOverride] cho khối cung: chiều trội theo sao chính.
     */
    private fun compose(
        topic: Topic?, lead: Paragraph?, extra: List<Piece>, pieces: List<Piece>, events: List<Pair<Piece, Int>>,
        score: Int?, dominantOverride: Int? = null, maxPlain: Int = 4,
    ): List<Paragraph> {
        val all = extra + pieces + events.map { it.first }
        val leadPol = lead?.polarity ?: 0
        val dom = dominantOverride ?: if (leadPol != 0) leadPol else all.sumOf { it.polarity * it.strength }.sign.toInt()
        fun agrees(p: Int) = p == 0 || (dom != 0 && p.sign == dom)
        val out = ArrayList<Paragraph>()
        lead?.let { out.add(it) }
        val opposite = ArrayList<Piece>()
        for (p in extra) if (agrees(p.polarity)) out.add(Paragraph(p.text, p.polarity, ParaKind.PLAIN, topic, p.ruleIds, p.why, p.draft)) else opposite.add(p)
        var shown = 0
        for (p in pieces) {
            if (!agrees(p.polarity)) { opposite.add(p); continue }
            if (shown < maxPlain) out.add(Paragraph(p.text, p.polarity, ParaKind.PLAIN, topic, p.ruleIds, p.why, p.draft))
            shown++
        }
        for ((p, _) in events) {
            if (agrees(p.polarity)) out.add(Paragraph(p.text, p.polarity, ParaKind.EVENT, topic, p.ruleIds, p.why, p.draft)) else opposite.add(p)
        }
        if (opposite.isNotEmpty()) {
            val pos = all.filter { it.polarity > 0 }.map { it.name }
            val neg = all.filter { it.polarity < 0 }.map { it.name }
            val dir = when { dom > 0 -> "thuận"; dom < 0 -> "thận trọng"; else -> "cân bằng" }
            val reason = when {
                dominantOverride != null && extra.isNotEmpty() -> "sao chính tọa thủ (${extra.first().name}) quyết định tính chất của cung"
                score != null && leadPol != 0 -> "điểm tổng hợp ${score}/100 ${if (leadPol > 0) "cao hơn" else "thấp hơn"} mức trung bình"
                dom != 0 -> "tổng lực các yếu tố ${if (dom > 0) "thuận" else "cần lưu ý"} lớn hơn"
                else -> "hai bên gần ngang nhau, nên giữ thế ổn định"
            }
            val text = when {
                pos.isNotEmpty() && neg.isNotEmpty() -> "Có cả điểm thuận (${names(pos)}) lẫn điểm cần lưu ý (${names(neg)}), nghiêng về $dir vì $reason."
                pos.isNotEmpty() && dom == 0 -> "Có điểm thuận (${names(pos)}), nhưng tổng thể vẫn cân bằng vì $reason."
                pos.isNotEmpty() -> "Dù có điểm thuận (${names(pos)}), tổng thể vẫn nghiêng về $dir vì $reason."
                dom == 0 -> "Có điểm cần lưu ý (${names(neg)}), nhưng tổng thể vẫn cân bằng vì $reason."
                else -> "Dù có điểm cần lưu ý (${names(neg)}), tổng thể vẫn nghiêng về $dir vì $reason."
            }
            out.add(Paragraph(text, dom, ParaKind.WEIGH, topic, opposite.flatMap { it.ruleIds }.distinct(), opposite.flatMap { it.why }, opposite.any { it.draft }))
        }
        return out
    }

    // ———————————————————————————— sự kiện (đồng thanh tương ứng) ————————————————————————————

    private fun events(items: List<Item>): List<EventHit> = Event.entries.mapNotNull { e ->
        val its = items.filter { it.rule.effect.event == e && it.rule.scope != Scope.GOC }
        val layers = its.flatMapTo(HashSet()) { it.rule.layers() } - Layer.GOC
        val level = Likelihood.of(layers.size) ?: return@mapNotNull null
        EventHit(e, layers, level, its)
    }

    private fun eventPiece(ctx: Ctx, h: EventHit): Piece {
        val t = kb.texts["ev.${h.event.name.lowercase()}.${h.level.key}"]
        val layerList = h.layers.sortedBy { it.ordinal }.joinToString(", ") { it.vn }
        return Piece(
            ctx.fill(t?.text ?: h.event.vn, emptyList()), "${h.event.short} (${h.level.vn})", h.event.polarity,
            h.layers.size * 1.5, h.items.map { it.rule.id } + "EVENT_${h.event.name}",
            listOf(Why("EVENT_${h.event.name}", "Đồng thanh tương ứng: ${h.layers.size} tầng cùng chỉ", "Các tầng: $layerList.", emptyList(), "Prompt mục 9")) +
                h.items.map { whyOf(ctx, it) },
            t?.reviewed != true,
        )
    }

    // ———————————————————————————— khuyến nghị (mục 11) ————————————————————————————

    private fun advice(ctx: Ctx, items: List<Item>, topicScores: Map<Topic, Int>, stance: Stance, forced: List<String> = emptyList(), child: Boolean = false): List<AdviceItem> {
        val out = ArrayList<AdviceItem>()
        // Trẻ nhỏ: chỉ dùng khuyến nghị dành cho trẻ (D-39); người lớn: không dùng khuyến nghị dành cho trẻ.
        fun ok(a: AdviceEntry) = if (child) AdviceUse.CHILD in a.uses else (a.stances.isEmpty() || stance in a.stances) && AdviceUse.CHILD !in a.uses
        fun count(t: AdviceType) = out.count { it.type == t }
        fun has(key: String) = out.any { it.key == key }
        fun add(a: AdviceEntry, because: String, ids: List<String>, dedupeTopic: Boolean, max: Int): Boolean {
            if (has(a.key) || !ok(a) || count(a.type) >= max) return false
            if (dedupeTopic && out.any { it.type == a.type && it.topic == a.topic }) return false
            out.add(AdviceItem(a.key, a.type, a.topic, a.text, because, ids))
            return true
        }
        for (k in forced) kb.advice[k]?.let { add(it, "Nhiều sao nặng ở Mệnh/Phúc trong giai đoạn này (mục 11 quy tắc 5)", listOf("NOTE_KET_NOI"), false, 5) }
        // 1) Từ finding mạnh nhất: xếp theo |chiều × trọng số|, khử trùng theo chủ đề, tối đa 5 mỗi loại.
        for (it in items.sortedByDescending { x -> abs(x.polarity) * x.weight }) {
            for (k in it.rule.adviceKeys) {
                val a = kb.advice[k] ?: continue
                add(a, it.rule.name, listOf(it.rule.id), true, if (a.type == AdviceType.LUU_Y) 3 else 5)
            }
        }
        // 2) Dự phòng theo chủ đề mạnh/yếu, rồi theo thế giai đoạn, cho đủ 3 NÊN và 3 KHÔNG NÊN.
        val strong = topicScores.entries.filter { it.value >= 55 }.sortedByDescending { it.value }
        val weak = topicScores.entries.filter { it.value < 50 }.sortedBy { it.value }
        fun fallback(type: AdviceType, use: AdviceUse, topics: List<Map.Entry<Topic, Int>>, min: Int, max: Int) {
            for ((t, sc) in topics) {
                if (count(type) >= min) return
                kb.advice.values.firstOrNull { it.topic == t && it.type == type && use in it.uses && ok(it) && !has(it.key) }?.let {
                    add(it, "${t.vn} ${sc}/100 (${if (use == AdviceUse.STRONG) "mạnh" else "yếu"})", listOf("SCORE_${t.name}"), true, max)
                }
            }
        }
        fallback(AdviceType.NEN, AdviceUse.STRONG, strong, 3, 5)
        fallback(AdviceType.NEN, AdviceUse.WEAK, weak, 3, 5)
        fallback(AdviceType.KHONG_NEN, AdviceUse.WEAK, weak, 3, 5)
        fallback(AdviceType.LUU_Y, AdviceUse.WEAK, weak.filter { it.value < 45 }, 2, 3)
        if (child) for (a in kb.advice.values.filter { AdviceUse.CHILD in it.uses }) {
            if (count(a.type) < if (a.type == AdviceType.LUU_Y) 2 else 3) add(a, "Lá số trẻ nhỏ (D-39)", listOf("CHILD"), true, 5)
        }
        for (type in listOf(AdviceType.NEN, AdviceType.KHONG_NEN)) {
            if (child) break
            for (a in kb.advice.values.filter { AdviceUse.STANCE in it.uses && it.type == type && ok(it) }.sortedByDescending { it.stances.size == 1 }) {
                if (count(type) >= 3) break
                add(a, "Thế ${stance.vn} của giai đoạn", listOf("STANCE_${stance.name}"), false, 5)
            }
            // Còn thiếu (ít chủ đề yếu/mạnh): lấy thêm khuyến nghị dự phòng của chủ đề gần mức trung bình.
            for ((t, sc) in topicScores.entries.sortedBy { if (type == AdviceType.NEN) -it.value else it.value }) {
                if (count(type) >= 3) break
                kb.advice.values.firstOrNull { it.topic == t && it.type == type && (AdviceUse.WEAK in it.uses || AdviceUse.STRONG in it.uses) && ok(it) && !has(it.key) }
                    ?.let { add(it, "${t.vn} ${sc}/100", listOf("SCORE_${t.name}"), true, 5) }
            }
        }
        return out.sortedBy { it.type.ordinal }
    }

    // ———————————————————————————— điểm tổng ————————————————————————————

    /** Lá số trẻ nhỏ (dưới 16 tuổi âm, D-39): chỉ luận học tập, sức khỏe, gia đình, đi lại. */
    private val CHILD_TOPICS = setOf(Topic.SU_NGHIEP, Topic.SUC_KHOE, Topic.GIA_DAO, Topic.DI_CHUYEN, Topic.TINH_CACH)
    private val CHILD_EVENTS = setOf(Event.DI_CHUYEN, Event.SUC_KHOE)
    private fun topicTitle(t: Topic, child: Boolean) = if (child && t == Topic.SU_NGHIEP) "Học tập, phát triển" else t.vn

    private val YEAR_TOPICS = listOf(Topic.SU_NGHIEP, Topic.TAI_CHINH, Topic.TINH_DUYEN, Topic.SUC_KHOE, Topic.GIA_DAO, Topic.DI_CHUYEN, Topic.PHAP_LY)
    private val OVERALL_W = mapOf(
        Topic.SU_NGHIEP to 1.0, Topic.TAI_CHINH to 1.0, Topic.TINH_DUYEN to 0.8, Topic.SUC_KHOE to 0.8, Topic.GIA_DAO to 0.6,
        Topic.DI_CHUYEN to 0.4, Topic.PHAP_LY to 0.6,
    )

    /** Điểm tổng của giai đoạn: mỗi tầng = 0,4 × Mệnh của tầng + 0,6 × trung bình có trọng số các chủ đề; rồi gộp tầng. */
    private fun overall(ctx: Ctx, topics: Map<Topic, TopicScore>, weights: Map<Layer, Double>): Pair<Int, List<LayerScore>> {
        val menh = weights.keys.mapNotNull { l -> ctx.scorer.layerMenh(ctx.chart, ctx.eval, l, ctx.limits) }
        var total = 0.0
        var wsum = 0.0
        val parts = ArrayList<LayerScore>()
        for (m in menh) {
            val w = weights.getValue(m.layer)
            var tw = 0.0
            var ts = 0.0
            for ((t, s) in topics) {
                val ls = s.layers.firstOrNull { it.layer == m.layer } ?: continue
                val k = OVERALL_W[t] ?: 0.5
                ts += k * ls.score.score; tw += k
            }
            val layerScore = if (tw > 0) 0.4 * m.score.score + 0.6 * ts / tw else m.score.score.toDouble()
            total += w * layerScore; wsum += w
            parts.add(m.copy(weight = w, score = m.score.copy(score = layerScore.roundToInt())))
        }
        return (total / wsum).roundToInt().coerceIn(5, 95) to parts.map { it.copy(weight = it.weight / wsum) }
    }

    private fun scoreWhy(ctx: Ctx, id: String, title: String, ts: TopicScore?, extra: List<String> = emptyList()): Why {
        val layers = ts?.layers.orEmpty()
        val detail = if (layers.size <= 1) "Điểm ${ts?.score ?: ""}/100 (50 = mức trung bình của lá số)." else
            layers.joinToString("; ") { "${it.layer.vn.replaceFirstChar { c -> c.uppercase() }} ×${"%.2f".format(it.weight)}: ${it.score.score} (cung ${ctx.place(it.palace)})" } + "."
        val ev = layers.flatMap { l -> l.score.top.take(if (layers.size > 1) 2 else 5).map { c -> "${c.label}: ${if (c.value >= 0) "+" else ""}${"%.1f".format(c.value)}" } } + extra
        return Why(id, title, detail, ev, "Prompt mục 9 bước 5–6")
    }

    private fun topicLead(ctx: Ctx, topic: Topic, ts: TopicScore, scope: ReadingScope, child: Boolean): Paragraph {
        val s = ts.score
        val pol = polOf(s)
        val title = topicTitle(topic, child)
        val head = when (scope) {
            ReadingScope.NAM -> "$title năm ${ctx.limits?.lunarYear}"
            ReadingScope.DAI_VAN -> "$title trong đại vận"
            else -> title
        }
        val tail = if (ts.layers.size > 1) {
            val year = ts.layers.filter { it.layer != Layer.GOC }
            val role = Scorer.TOPICS.getValue(topic).main.first()
            fun where(l: LayerScore) = "cung ${role.vn} của ${layerShort(l.layer)} tại ${l.palace.vn}, trên cung gốc ${ctx.chart.palace(l.palace).role.vn}"
            when {
                pol > 0 -> year.maxByOrNull { it.score.score }?.let { " Điểm tựa chính là ${layerLead(it.layer)} (${where(it)})." } ?: ""
                pol < 0 -> year.minByOrNull { it.score.score }?.let { " Áp lực chủ yếu đến từ ${layerLead(it.layer)} (${where(it)})." } ?: ""
                else -> ""
            }
        } else {
            val spec = Scorer.TOPICS.getValue(topic)
            " Cung đọc chính: ${spec.main.joinToString(", ") { r -> ctx.place(ctx.chart.palace(r).chi) }}."
        }
        return Paragraph("$head: ${band(s)} (${s}/100).$tail", pol, ParaKind.LEAD, topic, listOf("SCORE_${topic.name}"), listOf(scoreWhy(ctx, "SCORE_${topic.name}", "Điểm chủ đề ${topic.vn}", ts)), false)
    }

    private fun topicSection(
        ctx: Ctx, topic: Topic, ts: TopicScore, scope: ReadingScope, items: List<Item>, hits: List<EventHit>, extra: List<Piece> = emptyList(),
        child: Boolean = false,
    ): Section {
        val shownEvents = hits.filter { it.event.topic == topic }
        val eventRules = shownEvents.flatMap { h -> h.items.map { it.rule.id } }.toSet()
        val pieces = items.filter { topic in it.topics && it.rule.id !in eventRules }.map { pieceOf(ctx, it) }
        val paras = ArrayList(compose(topic, topicLead(ctx, topic, ts, scope, child), extra, pieces, shownEvents.map { eventPiece(ctx, it) to it.layers.size }, ts.score))
        if (paras.size == 1) note("note.chua_co_luan", topic)?.let { paras.add(it) }
        if (topic == Topic.SUC_KHOE) note("note.suc_khoe", topic)?.let { paras.add(it) }
        if (topic == Topic.TAI_CHINH && scope != ReadingScope.TRON_DOI) note("note.tai_chinh", topic)?.let { paras.add(it) }
        return Section("topic_${topic.name}", topicTitle(topic, child), topic, ts.score, null, paras)
    }

    /** Linh Xương Đà Vũ hoặc ≥ 3 hung tinh nặng ở Mệnh/Phúc trong năm hạn → câu giữ kết nối (mục 11 quy tắc 5). */
    private fun needsWellbeing(ctx: Ctx): Boolean {
        if (ctx.eval.firedRules.any { it.first.id.startsWith("HC_LINH_XUONG_DA_VU") }) return true
        val heavy = setOf(StarId.KINH_DUONG, StarId.DA_LA, StarId.HOA_TINH, StarId.LINH_TINH, StarId.DIA_KHONG, StarId.DIA_KIEP, StarId.HOA_KY)
        val l = ctx.limits ?: return false
        for (role in listOf(PalaceRole.MENH, PalaceRole.PHUC_DUC)) {
            val chi = ctx.chart.palace(role).chi
            var n = ctx.chart.palace(chi).stars.count { it.id in heavy }
            n += l.luuStars.count { (s, c) -> c == chi && (s == StarId.KINH_DUONG || s == StarId.DA_LA) }
            n += l.luuTuHoa.count { (h, c) -> c == chi && h == TuHoa.KY }
            if (n >= 3) return true
        }
        return false
    }

    private fun closing() = kb.texts["note.duc_nang"]?.text ?: "Đức năng thắng số."

    // ———————————————————————————— các bản luận ————————————————————————————

    /** Tử vi trọn đời: tổng quan Mệnh–Thân–Phúc, 10 chủ đề, 12 cung, dòng thời gian 12 đại vận (mục 2.3). */
    fun lifetime(chart: Chart, expert: Boolean = false, currentLunarYear: Int? = null): Reading {
        val eval = engine.evaluate(chart, null, expert)
        val ctx = Ctx(chart, null, eval, Scorer(kb))
        val its = items(ctx, setOf(Scope.GOC))
        val sc = ctx.scorer
        val menhS = sc.palace(chart, eval, chart.menh)
        val thanS = sc.palace(chart, eval, chart.than)
        val phucChi = chart.palace(PalaceRole.PHUC_DUC).chi
        val phucS = sc.palace(chart, eval, phucChi)
        val score = (0.45 * menhS.score + 0.2 * thanS.score + 0.35 * phucS.score).roundToInt().coerceIn(5, 95)
        val stance = Stance.of(score)

        val summary = ArrayList<Paragraph>()
        summary.add(Paragraph(
            "Tổng quan Mệnh – Thân – Phúc: ${band(score)} (${score}/100). Mệnh tại ${chart.menh.vn}, Thân cư ${chart.palace(chart.than).role.vn} (${chart.than.vn}), Phúc Đức tại ${phucChi.vn}.",
            polOf(score), ParaKind.LEAD, null, listOf("SCORE_TONG_QUAN"),
            listOf(Why("SCORE_TONG_QUAN", "Điểm tổng quan", "Mệnh ${menhS.score} ×0,45; Thân ${thanS.score} ×0,2; Phúc Đức ${phucS.score} ×0,35.",
                (menhS.top + phucS.top).map { "${it.label}: ${"%+.1f".format(it.value)}" }, "Prompt mục 2.3, 9")), false,
        ))
        mainStarPiece(ctx, chart.menh)?.let { summary.add(Paragraph(it.text, it.polarity, ParaKind.PLAIN, Topic.TINH_CACH, it.ruleIds, it.why, it.draft)) }
            ?: summary.add(Paragraph("Mệnh vô chính diệu: mượn chính tinh cung Thiên Di (${chart.menh.opposite.vn}) để luận, lực giảm còn khoảng bảy phần.", 0, ParaKind.NOTE, Topic.TINH_CACH, listOf("VCD_MENH"), listOf(Why("VCD_MENH", "Vô chính diệu", "Prompt mục 9 bước 2: mượn chính tinh cung xung chiếu, hệ số 0,7.", emptyList(), "Prompt mục 9")), false))
        its.firstOrNull { it.rule.id.startsWith("TS_THAN_CU_") }?.let { summary.add(Paragraph(pieceOf(ctx, it).text, 0, ParaKind.NOTE, null, listOf(it.rule.id), listOf(whyOf(ctx, it)), true)) }
        mainStarPiece(ctx, phucChi)?.let { summary.add(Paragraph(it.text, it.polarity, ParaKind.PLAIN, Topic.PHUC_DUC, it.ruleIds, it.why, it.draft)) }

        val topicScores = Topic.entries.associateWith { t ->
            val s = sc.topic(chart, eval, t, Scorer.frame(chart.menh), null, null, eval.findings, chart.than)
            TopicScore(t, s.score, listOf(LayerScore(Layer.GOC, 1.0, chart.palace(Scorer.TOPICS.getValue(t).main.first()).chi, s)))
        }
        val sections = Topic.entries.map { t ->
            val mains = Scorer.TOPICS.getValue(t).main.map { r -> chart.palace(r).chi }
            val extra = if (t == Topic.TINH_CACH || t == Topic.PHAP_LY) emptyList() else mains.mapNotNull { mainStarPiece(ctx, it) }
            topicSection(ctx, t, topicScores.getValue(t), ReadingScope.TRON_DOI, its, emptyList(), extra)
        }
        val palaces = PalaceRole.entries.map { r -> palaceSection(ctx, chart.palace(r).chi, its) }

        val periods = LimitEngine.daiHanPeriods(chart)
        val timeline = periods.map { p ->
            val y = chart.lunar.year + p.ages.first - 1
            val d = decadeScore(chart, y, expert)
            DecadeItem(p.index, p.ages.first, p.ages.last, p.palace, chart.palace(p.palace).role.vn, p.canChi.vn, d, Stance.of(d), p.thienThoi.vn, y,
                currentLunarYear != null && chart.tuoiAm(currentLunarYear) in p.ages)
        }
        val adv = advice(ctx, its, topicScores.mapValues { it.value.score }, stance)
        return Reading(
            ReadingScope.TRON_DOI, "Tử vi trọn đời", "${chart.gender.vn} · ${chart.pillars.year.vn} · ${chart.cuc.vn} · ${chart.banMenh.name}",
            score, stance, "Điểm tổng quan ${score}/100 (${band(score)}).", summary, sections, palaces, timeline, adv,
            emptyList(), emptyList(), emptyList(), closing(), kb.version,
        )
    }

    /** Một cung trong “12 cung từng mục”: ý nghĩa cung, sao chính, cách cục tại cung, phụ tinh. */
    private fun palaceSection(ctx: Ctx, chi: Chi, its: List<Item>): Section {
        val p = ctx.chart.palace(chi)
        val topic = Topic.ofRole(p.role)
        val s = ctx.scorer.palace(ctx.chart, ctx.eval, chi)
        val paras = ArrayList<Paragraph>()
        kb.texts["cung.${p.role.name}"]?.let { paras.add(Paragraph(it.text, 0, ParaKind.NOTE, topic, listOf("CUNG_${p.role.name}"), listOf(Why("CUNG_${p.role.name}", "Ý nghĩa cung ${p.role.vn}", it.text, emptyList(), it.source)), !it.reviewed)) }
        val main = mainStarPiece(ctx, chi)
        val extra = ArrayList<Piece>()
        if (main != null) extra.add(main) else paras.add(Paragraph(
            "Cung vô chính diệu: mượn chính tinh cung xung chiếu ${ctx.place(chi.opposite)} để luận, lực giảm còn khoảng bảy phần.",
            0, ParaKind.NOTE, topic, listOf("VCD_${p.role.name}"), listOf(Why("VCD_${p.role.name}", "Vô chính diệu", "Prompt mục 9 bước 2.", emptyList(), "Prompt mục 9")), false,
        ))
        val rulePieces = its.filter { chi in it.palaces && it.rule.anchor !is Anchor.Layer }.map { pieceOf(ctx, it) }
        val minor = ctx.eval.findings.filter { it.palace == chi && it.ruleId.startsWith("PHU_") }.mapNotNull { f ->
            val star = f.evidence.firstOrNull()?.star ?: return@mapNotNull null
            val t = kb.texts[f.textKey] ?: return@mapNotNull null
            Piece(ctx.fill(t.text, listOf(chi)), star.vn, f.polarity, abs(f.polarity) * 0.5 + 0.05, listOf(f.ruleId),
                listOf(Why(f.ruleId, "Phụ tinh ${star.vn}", ctx.fill(t.text, listOf(chi)), f.evidence.map { it.toString() }, t.source)), !t.reviewed)
        }
        // Phụ tinh cùng chiều trội gộp thành một câu liệt kê; ngược chiều vào câu cân nhắc.
        val dom = main?.polarity?.sign ?: polOf(s.score)
        val sameMinor = minor.filter { it.polarity != 0 && it.polarity.sign == dom }
        val minorGroup = if (sameMinor.isEmpty()) emptyList() else listOf(Piece(
            (if (dom > 0) "Cát tinh hỗ trợ: " else "Hung tinh cần lưu ý: ") + sameMinor.joinToString("; ") { it.text.trimEnd('.') } + ".",
            names(sameMinor.map { it.name }), dom, sameMinor.sumOf { it.strength }, sameMinor.flatMap { it.ruleIds }, sameMinor.flatMap { it.why }, sameMinor.any { it.draft },
        ))
        val otherMinor = minor.filter { it.polarity != 0 && it.polarity.sign != dom }
        paras += compose(topic, null, extra, rulePieces + minorGroup + otherMinor, emptyList(), s.score, dominantOverride = if (main != null && main.polarity != 0) main.polarity.sign else null, maxPlain = 6)
        val sub = p.chinhTinh.joinToString(", ") { st -> st.id.vn + (st.brightness?.let { b -> " (${b.code})" } ?: "") }.ifEmpty { "Vô chính diệu" }
        return Section("palace_${p.role.name}", "Cung ${p.role.vn} · ${p.can.vn} ${chi.vn}", topic, s.score, sub, paras)
    }

    private fun decadeScore(chart: Chart, firstYear: Int, expert: Boolean): Int {
        val limits = LimitEngine.forYear(chart, firstYear)
        if (limits.beforeFirstDaiHan) return 50
        val eval = engine.evaluate(chart, limits, expert)
        val ctx = Ctx(chart, limits, eval.copy(findings = eval.findings.filter { it.scope != Scope.NAM }), Scorer(kb))
        val topics = YEAR_TOPICS.associateWith { t -> ctx.scorer.topicLayered(chart, ctx.eval, t, limits, Scorer.DECADE_WEIGHTS) }
        return overall(ctx, topics, Scorer.DECADE_WEIGHTS).first
    }

    /** Đại vận 10 năm chứa năm âm [lunarYear] (mục 2.3 tầng 3). */
    fun decade(chart: Chart, lunarYear: Int, expert: Boolean = false): Reading {
        val viewLimits = LimitEngine.forYear(chart, lunarYear)
        val period = viewLimits.daiHanPeriod
        val firstYear = chart.lunar.year + period.ages.first - 1
        val limits = if (viewLimits.beforeFirstDaiHan) viewLimits else LimitEngine.forYear(chart, firstYear)
        val raw = engine.evaluate(chart, limits, expert)
        val eval = raw.copy(findings = raw.findings.filter { it.scope != Scope.NAM })
        val ctx = Ctx(chart, limits, eval, Scorer(kb))
        val child = period.ages.first < 14
        val its = items(ctx, setOf(Scope.DAI_HAN))
        val topics = (listOf(Topic.TINH_CACH) + YEAR_TOPICS).filter { !child || it in CHILD_TOPICS }.associateWith { t -> ctx.scorer.topicLayered(chart, eval, t, limits, Scorer.DECADE_WEIGHTS) }
        val (score, layers) = if (viewLimits.beforeFirstDaiHan) 50 to emptyList() else overall(ctx, topics, Scorer.DECADE_WEIGHTS)
        val stance = Stance.of(score)
        val role = chart.palace(period.palace).role.vn
        val summary = ArrayList<Paragraph>()
        if (viewLimits.beforeFirstDaiHan) {
            summary.add(Paragraph("Năm $lunarYear chưa vào đại hạn đầu (bắt đầu từ ${period.ages.first} tuổi âm); giai đoạn này đọc theo lá số gốc và tiểu hạn.", 0, ParaKind.NOTE, null,
                listOf("DAI_HAN_CHUA_VAO"), listOf(Why("DAI_HAN_CHUA_VAO", "Trước đại hạn đầu", "Đại hạn đầu bắt đầu ở tuổi âm bằng số Cục (mục 8.1).", emptyList(), "Prompt mục 8.1")), false))
        } else {
            summary.add(Paragraph("Đại vận ${period.ages.first}–${period.ages.last} tuổi tại cung $role (${period.palace.vn}, ${period.canChi.vn}): ${band(score)} (${score}/100). ${stanceText(stance)}",
                polOf(score), ParaKind.LEAD, null, listOf("SCORE_DAI_VAN"),
                listOf(Why("SCORE_DAI_VAN", "Điểm đại vận", layers.joinToString("; ") { "${it.layer.vn} ×${"%.2f".format(it.weight)}: ${it.score.score} (cung ${ctx.place(it.palace)})" } + ".",
                    layers.flatMap { it.score.top.take(3).map { c -> "${c.label}: ${"%+.1f".format(c.value)}" } }, "Prompt mục 9; D-38")), false))
            summary.add(Paragraph("Thiên thời: ${period.thienThoi.vn} — ${period.thienThoi.label.lowercase()} (ngũ hành tam hợp, cụ Thiên Lương).", 0, ParaKind.NOTE, null,
                listOf("VH_THIEN_THOI_${period.thienThoi.name}"), listOf(Why("VH_THIEN_THOI_${period.thienThoi.name}", "Thiên thời đại hạn", "So hành tam hợp của tuổi với hành tam hợp của cung đại hạn.", emptyList(), "Prompt mục 8.1")), false))
        }
        val sections = topics.map { (t, ts) ->
            val extra = if (viewLimits.beforeFirstDaiHan || t == Topic.PHAP_LY) emptyList()
                else listOfNotNull(framePiece(ctx, Layer.DAI_HAN, limits.daiHan, Scorer.TOPICS.getValue(t).main.first(), "đại vận"))
            topicSection(ctx, t, ts, ReadingScope.DAI_VAN, its, emptyList(), extra, child)
        }
        if (child) note("note.tre_nho", null)?.let { summary.add(it) }
        val adv = advice(ctx, its, topics.mapValues { it.value.score }, stance, child = child)
        return Reading(
            ReadingScope.DAI_VAN, "Đại vận ${period.ages.first}–${period.ages.last} tuổi", "Cung $role · ${period.canChi.vn} · từ năm $firstYear",
            score, stance, stanceText(stance), summary, sections, emptyList(), emptyList(), adv, emptyList(), emptyList(), emptyList(), closing(), kb.version,
        )
    }

    /** Tiểu vận / lưu niên một năm: tổng hợp 4 tầng, 7 chuyên đề, sự kiện, Nên/Không nên, tháng thuận (mục 2.3 tầng 4). */
    fun year(chart: Chart, lunarYear: Int, expert: Boolean = false): Reading {
        val limits = LimitEngine.forYear(chart, lunarYear)
        val eval = engine.evaluate(chart, limits, expert)
        val ctx = Ctx(chart, limits, eval, Scorer(kb))
        val child = limits.tuoiAm < 16
        val its = items(ctx, setOf(Scope.NAM, Scope.DAI_HAN))
        val hits = events(its).filter { !child || it.event in CHILD_EVENTS }
        val topics = YEAR_TOPICS.filter { !child || it in CHILD_TOPICS }.associateWith { t -> ctx.scorer.topicLayered(chart, eval, t, limits, Scorer.YEAR_WEIGHTS) }
        val (score, layers) = overall(ctx, topics, Scorer.YEAR_WEIGHTS)
        val stance = Stance.of(score)

        val summary = ArrayList<Paragraph>()
        summary.add(Paragraph("Tổng quan năm ${ctx.yearLabel}: ${band(score)} (${score}/100). ${stanceText(stance)}", polOf(score), ParaKind.LEAD, null, listOf("SCORE_NAM"),
            listOf(Why("SCORE_NAM", "Điểm năm", layers.joinToString("; ") { "${it.layer.vn.replaceFirstChar { c -> c.uppercase() }} ×${"%.2f".format(it.weight)}: ${it.score.score} (cung ${ctx.place(it.palace)})" } + ".",
                layers.flatMap { it.score.top.take(2).map { c -> "${c.label}: ${"%+.1f".format(c.value)}" } }, "Prompt mục 9 bước 6")), false))
        val layerText = buildString {
            if (!limits.beforeFirstDaiHan) append("Đại hạn tại ${ctx.place(limits.daiHan)} là nền 10 năm (${limits.daiHanPeriod.thienThoi.vn.lowercase()}); ")
            else append("Chưa vào đại hạn đầu nên năm này đọc theo lá số gốc, tiểu hạn và Lưu Thái Tuế; ")
            append("tiểu hạn tại ${ctx.place(limits.tieuHan)} cho biết diễn biến; ")
            if (!limits.beforeFirstDaiHan) append("lưu niên đại hạn tại ${ctx.place(limits.luuNienDaiHan)} cho biết kết quả; ")
            append("Lưu Thái Tuế đánh thức cung ${ctx.place(limits.luuThaiTue)}.")
        }
        summary.add(Paragraph(layerText, 0, ParaKind.NOTE, null, listOf("LAYERS_NAM"),
            listOf(Why("LAYERS_NAM", "Vai trò từng tầng", "Tiểu hạn = chuyện gì diễn ra; lưu niên đại hạn = kết cục; Lưu Thái Tuế = lĩnh vực bị đánh thức; đại hạn = nền móng 10 năm (mục 8.5).", emptyList(), "Prompt mục 8")), false))
        val th = layers.firstOrNull { it.layer == Layer.TIEU_HAN }?.score?.score
        val ln = layers.firstOrNull { it.layer == Layer.LUU_NIEN }?.score?.score
        if (th != null && ln != null) {
            val trend = when {
                th <= 45 && ln >= 55 -> "Diễn biến đầu có thể khó, nhưng kết quả cuối năm có xu hướng tốt."
                th >= 55 && ln <= 45 -> "Khởi đầu thuận, nhưng cần giữ thành quả cho đến cuối năm."
                else -> null
            }
            trend?.let { summary.add(Paragraph(it, 0, ParaKind.NOTE, null, listOf("TREND_NAM"), listOf(Why("TREND_NAM", "Tiểu hạn và lưu niên đại hạn", "Tiểu hạn $th/100, lưu niên đại hạn $ln/100 (mục 8.5).", emptyList(), "Prompt mục 8")), false)) }
        }
        val wellbeing = !child && needsWellbeing(ctx)
        if (wellbeing) note("note.ket_noi", null, id = "NOTE_KET_NOI")?.let { summary.add(it) }
        if (child) note("note.tre_nho", null)?.let { summary.add(it) }

        val yearItems = its.filter { it.rule.scope == Scope.NAM }
        val sections = topics.keys.map { t ->
            val extra = if (t == Topic.PHAP_LY) emptyList()
                else listOfNotNull(framePiece(ctx, Layer.TIEU_HAN, limits.tieuHan, Scorer.TOPICS.getValue(t).main.first(), "năm"))
            topicSection(ctx, t, topics.getValue(t), ReadingScope.NAM, yearItems, hits, extra, child)
        }
        val months = ctx.scorer.months(chart, engine.evaluate(chart), score, lunarYear).map { (m, s) ->
            val chi = LimitEngine.forMonth(chart, lunarYear, m).nguyetHan
            MonthScore(m, s, chi, chi?.let { chart.palace(it).role.vn })
        }
        val good = months.sortedWith(compareByDescending<MonthScore> { it.score }.thenBy { it.month }).take(2).map { it.month }.sorted()
        val caution = months.sortedWith(compareBy<MonthScore> { it.score }.thenBy { it.month }).take(2).map { it.month }.sorted()
        val adv = advice(ctx, its, topics.mapValues { it.value.score }, stance, if (wellbeing) listOf("adv.giu_ket_noi", "adv.tim_ho_tro_chuyen_mon") else emptyList(), child)
        return Reading(
            ReadingScope.NAM, "Năm ${ctx.yearLabel}", "${limits.tuoiAm} tuổi âm · ${chart.gender.vn}", score, stance, stanceText(stance),
            summary, sections, emptyList(), emptyList(), adv, good, caution, months, closing(), kb.version,
        )
    }

    /** Một tháng âm: điểm tháng, cung nguyệt hạn, Tứ Hóa tháng, khuyến nghị theo cung tháng (mục 2.3 tầng 5). */
    fun month(chart: Chart, lunarYear: Int, month: Int, expert: Boolean = false): Reading {
        val yearReading = year(chart, lunarYear, expert)
        val m = LimitEngine.forMonth(chart, lunarYear, month)
        val natal = engine.evaluate(chart)
        val ctx = Ctx(chart, m, natal, Scorer(kb))
        val score = yearReading.months.first { it.month == month }.score
        val stance = Stance.of(score)
        val nh = m.nguyetHan
        val summary = ArrayList<Paragraph>()
        summary.add(Paragraph("Tháng $month âm lịch năm ${ctx.yearLabel}: ${band(score)} (${score}/100). ${stanceText(stance)}", polOf(score), ParaKind.LEAD, null, listOf("SCORE_THANG"),
            listOf(Why("SCORE_THANG", "Điểm tháng", "Năm ${yearReading.score} ×0,50 + cung nguyệt hạn ×0,35 + Tứ Hóa tháng ×0,15.", emptyList(), "Prompt mục 9 bước 6")), false))
        if (nh != null) {
            val p = chart.palace(nh)
            val topic = Topic.ofRole(p.role)
            val ps = ctx.scorer.palace(chart, natal, nh, m.tuoiAm, Overlay(m.luuStars), emptyList())
            summary.add(Paragraph("Nguyệt hạn tại ${ctx.place(nh)}: tháng này việc thuộc lĩnh vực ${topic.vn.lowercase()} được chú ý nhiều hơn.", 0, ParaKind.NOTE, topic,
                listOf("NGUYET_HAN"), listOf(Why("NGUYET_HAN", "Cung nguyệt hạn", "Điểm cung nguyệt hạn ${ps.score}/100.", ps.top.map { "${it.label}: ${"%+.1f".format(it.value)}" }, "Prompt mục 8.5")), false))
            val hoa = m.nguyetTuHoa.orEmpty().entries.joinToString("; ") { (h, c) -> "${h.vn} tháng vào ${ctx.place(c)}" }
            if (hoa.isNotEmpty()) summary.add(Paragraph("Tứ Hóa theo can tháng (${m.monthCanChi?.vn}): $hoa.", 0, ParaKind.NOTE, null, listOf("TU_HOA_THANG"),
                listOf(Why("TU_HOA_THANG", "Tứ Hóa tháng", hoa, emptyList(), "Prompt mục 8.5")), false))
        }
        note("note.thang", null)?.let { summary.add(it) }
        val topicScores = if (nh != null) mapOf(Topic.ofRole(chart.palace(nh).role) to score) else emptyMap()
        val child = m.tuoiAm < 16
        val adv = advice(ctx, emptyList(), topicScores.filterKeys { !child || it in CHILD_TOPICS }, stance, child = child)
        return Reading(
            ReadingScope.THANG, "Tháng $month âm lịch · năm $lunarYear", nh?.let { "Nguyệt hạn tại ${ctx.place(it)}" } ?: "", score, stance, stanceText(stance),
            summary, emptyList(), emptyList(), emptyList(), adv, emptyList(), emptyList(), yearReading.months, closing(), kb.version,
        )
    }
}
