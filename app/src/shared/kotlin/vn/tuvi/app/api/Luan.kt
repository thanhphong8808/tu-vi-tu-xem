package vn.tuvi.app.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.StarKind
import vn.tuvi.core.interpret.Anchor
import vn.tuvi.core.interpret.Composer
import vn.tuvi.core.interpret.Paragraph
import vn.tuvi.core.interpret.Section
import vn.tuvi.core.interpret.Finding
import vn.tuvi.core.interpret.KnowledgeBase
import vn.tuvi.core.interpret.LimitPoint
import vn.tuvi.core.interpret.Rule
import vn.tuvi.core.interpret.RuleEngine
import vn.tuvi.core.interpret.RuleType
import vn.tuvi.core.interpret.Scope

/**
 * Cầu nối luận giải cho giao diện:
 * - [json]: cách cục và dấu hiệu RuleEngine dò được trên lá số + năm đang xem (danh mục, giai đoạn 3);
 * - [readingJson]: bản luận đã ghép câu, chấm điểm, Nên/Không nên cho trọn đời / đại vận / năm / tháng (giai đoạn 4).
 * Câu luận trong knowledge base đang là bản nháp (reviewed = false).
 */
object Luan {
    @Volatile private var loaded: Triple<KnowledgeBase, RuleEngine, Composer>? = null

    /** Nạp knowledge base một lần (≈ 0,7 MB JSON trong APK). Gọi sớm ở luồng nền để lần mở lá số đầu không phải chờ. */
    fun engine(): Pair<KnowledgeBase, RuleEngine> = all().let { it.first to it.second }

    private fun all(): Triple<KnowledgeBase, RuleEngine, Composer> = loaded ?: synchronized(this) {
        loaded ?: KnowledgeBase.load().let { Triple(it, RuleEngine(it), Composer(it)) }.also { loaded = it }
    }

    fun warmUp() { all() }

    /** Bản luận một phạm vi: "life" | "decade" | "year" | "month". */
    fun readingJson(chart: Chart, scope: String, lunarYear: Int, month: Int, expert: Boolean, currentLunarYear: Int): JsonObject {
        val c = all().third
        val r = when (scope) {
            "life" -> c.lifetime(chart, expert, currentLunarYear)
            "decade" -> c.decade(chart, lunarYear, expert)
            "month" -> c.month(chart, lunarYear, month, expert)
            else -> c.year(chart, lunarYear, expert)
        }
        return toJson(r)
    }

    private fun para(p: Paragraph) = buildJsonObject {
        put("text", p.text); put("pol", p.polarity); put("kind", p.kind.name); put("topic", p.topic?.name); put("draft", p.draft)
        put("ids", buildJsonArray { p.ruleIds.forEach { add(it) } })
        put("why", buildJsonArray {
            for (w in p.why) add(buildJsonObject {
                put("id", w.id); put("title", w.title); put("detail", w.detail); put("source", w.source)
                put("evidence", buildJsonArray { w.evidence.forEach { add(it) } })
            })
        })
    }

    private fun section(s: Section) = buildJsonObject {
        put("id", s.id); put("title", s.title); put("topic", s.topic?.name); put("score", s.score); put("subtitle", s.subtitle)
        put("paragraphs", JsonArray(s.paragraphs.map { para(it) }))
    }

    fun daysJson(chart: Chart, year: Int, month: Int, expert: Boolean): JsonElement {
        if (year !in 1900..2200 || month !in 1..12) throw IllegalArgumentException("Tháng xem không hợp lệ.")
        return buildJsonArray {
            for (d in all().third.days(chart, year, month, expert)) add(buildJsonObject {
                put("date", d.date.toString()); put("day", d.date.dayOfMonth); put("dow", d.date.dayOfWeek.value)
                put("lunarDay", d.lunarDay); put("lunarMonth", d.lunarMonth); put("lunarLeap", d.lunarLeap); put("lunarYear", d.lunarYear)
                put("canChi", d.canChi); put("score", d.score); put("role", d.role)
            })
        }
    }

    fun dayJson(chart: Chart, date: java.time.LocalDate, expert: Boolean): JsonElement = toJson(all().third.day(chart, date, expert))

    fun starJson(chart: Chart, starName: String): JsonElement {
        val star = vn.tuvi.core.engine.StarId.entries.firstOrNull { it.name == starName } ?: throw IllegalArgumentException("Không có sao $starName.")
        val card = all().third.starCard(chart, star) ?: throw IllegalArgumentException("Sao ${star.vn} không có trong lá số.")
        return buildJsonObject {
            put("star", card.star.name); put("name", card.star.vn); put("kind", card.kind); put("chi", card.chi.vn); put("role", card.role)
            put("brightness", card.brightness); put("element", StarStyle.element(card.star)?.vn)
            put("dau", card.meta?.dau); put("hanh", card.meta?.hanh); put("hoaKhi", card.meta?.hoaKhi); put("dacTinh", card.meta?.dacTinh); put("metaSource", card.meta?.source)
            put("natalHoa", buildJsonArray { card.natalHoa.forEach { add(it) } })
            put("meaning", JsonArray(card.meaning.map { para(it) }))
            put("patterns", buildJsonArray { for (w in card.patterns) add(buildJsonObject { put("id", w.id); put("title", w.title); put("detail", w.detail); put("source", w.source); put("evidence", buildJsonArray { w.evidence.forEach { add(it) } }) }) })
        }
    }

    fun compareJson(a: Chart, b: Chart, rel: vn.tuvi.core.interpret.PairRelation, nameA: String, nameB: String): JsonElement {
        val r = all().third.compare(a, b, rel, nameA, nameB)
        return buildJsonObject {
            put("title", r.title); put("relation", r.relation.vn); put("paragraphs", JsonArray(r.paragraphs.map { para(it) })); put("closing", r.closing)
            put("a", buildJsonObject { put("menh", a.menh.vn); put("phuThe", a.palace(vn.tuvi.core.engine.PalaceRole.PHU_THE).chi.vn); put("tuTuc", a.palace(vn.tuvi.core.engine.PalaceRole.TU_TUC).chi.vn) })
            put("b", buildJsonObject { put("menh", b.menh.vn); put("phuThe", b.palace(vn.tuvi.core.engine.PalaceRole.PHU_THE).chi.vn); put("tuTuc", b.palace(vn.tuvi.core.engine.PalaceRole.TU_TUC).chi.vn) })
        }
    }

    fun rankJson(charts: Map<vn.tuvi.core.engine.CanhGio, Chart>, events: List<Pair<vn.tuvi.core.interpret.Event, Int>>): JsonElement = buildJsonArray {
        for (r in all().third.rankHours(charts, events)) add(buildJsonObject {
            val c = charts.getValue(r.canh)
            put("canh", r.canh.name); put("label", r.canh.vn); put("score", r.score)
            put("menh", c.menh.vn); put("menhStars", c.palace(c.menh).chinhTinh.joinToString(", ") { it.id.vn }.ifEmpty { "Vô chính diệu" })
            put("details", buildJsonArray { r.details.forEach { add(it) } })
        })
    }

    /** Đo hiệu năng ngay trên máy (mục 3): lập lá số, luận một năm, chấm điểm 30 ngày. */
    fun bench(): JsonElement {
        val c = all().third
        val input = vn.tuvi.core.engine.BirthInput(java.time.LocalDateTime.of(1990, 8, 15, 10, 30), null, vn.tuvi.core.engine.Gender.NAM)
        var chart = vn.tuvi.core.engine.ChartEngine.build(input)
        repeat(3) { chart = vn.tuvi.core.engine.ChartEngine.build(input); c.year(chart, 2026); c.days(chart, 2026, 11) }
        val n = 20
        var t = System.nanoTime(); repeat(n) { chart = vn.tuvi.core.engine.ChartEngine.build(input) }
        val chartMs = (System.nanoTime() - t) / 1e6 / n
        t = System.nanoTime(); repeat(5) { c.year(chart, 2026 + it) }
        val yearMs = (System.nanoTime() - t) / 1e6 / 5
        t = System.nanoTime(); repeat(3) { c.days(chart, 2026, 10 + it) }
        val daysMs = (System.nanoTime() - t) / 1e6 / 3
        t = System.nanoTime(); c.lifetime(chart)
        val lifeMs = (System.nanoTime() - t) / 1e6
        return buildJsonObject { put("chartMs", chartMs); put("yearMs", yearMs); put("days30Ms", daysMs); put("lifetimeMs", lifeMs) }
    }

    fun toJson(r: vn.tuvi.core.interpret.Reading): JsonObject = buildJsonObject {
        put("scope", r.scope.name); put("title", r.title); put("subtitle", r.subtitle); put("score", r.score)
        put("stance", r.stance.name); put("stanceVn", r.stance.vn); put("stanceText", r.stanceText)
        put("summary", JsonArray(r.summary.map { para(it) }))
        put("sections", JsonArray(r.sections.map { section(it) }))
        put("palaces", JsonArray(r.palaces.map { section(it) }))
        put("timeline", buildJsonArray {
            for (d in r.timeline) add(buildJsonObject {
                put("index", d.index); put("from", d.from); put("to", d.to); put("chi", d.chi.ordinal); put("chiVn", d.chi.vn); put("role", d.role)
                put("canChi", d.canChi); put("score", d.score); put("stance", d.stance.vn); put("thienThoi", d.thienThoi); put("startYear", d.startYear); put("current", d.current)
            })
        })
        put("advice", buildJsonArray {
            for (a in r.advice) add(buildJsonObject { put("key", a.key); put("type", a.type.name); put("topic", a.topic.vn); put("text", a.text); put("because", a.because) })
        })
        put("goodMonths", buildJsonArray { r.goodMonths.forEach { add(it) } })
        put("cautionMonths", buildJsonArray { r.cautionMonths.forEach { add(it) } })
        put("months", buildJsonArray { for (m in r.months) add(buildJsonObject { put("month", m.month); put("score", m.score); put("chi", m.chi?.vn); put("role", m.role) }) })
        put("hours", buildJsonArray { for (h in r.hours) add(buildJsonObject { put("chi", h.chi.vn); put("label", h.label); put("score", h.score); put("palace", h.palace.vn); put("role", h.role) }) })
        put("closing", r.closing); put("kbVersion", r.kbVersion)
    }

    private val TYPE_ORDER = listOf(
        RuleType.CAT_CACH, RuleType.HUNG_CACH, RuleType.PHAN_VI, RuleType.THE_SAO, RuleType.CUNG, RuleType.CHUYEN_DE, RuleType.VAN_HAN,
    )

    /** Một mục hiển thị: một luật tại một hay nhiều cung (luật neo “mọi cung” được gộp lại cho gọn). */
    private class Item(val rule: Rule, val palaces: List<Chi>, val findings: List<Finding>) {
        val polarity: Int get() = findings.first().polarity
    }

    fun json(chart: Chart, limits: LimitLayers, expert: Boolean): JsonObject {
        val (kb, engine) = engine()
        val ev = engine.evaluate(chart, limits, expert)
        val yearLabel = "${limits.lunarYear} (${limits.yearCanChi.vn})"
        fun roles(ps: List<Chi>) = ps.joinToString(", ") { chart.palace(it).role.vn }
        fun fill(text: String, ps: List<Chi>) = text
            .replace("{cung}", roles(ps))
            .replace("{nam}", yearLabel)
            .replace("{tuoi}", limits.tuoiAm.toString())

        // Một luật có thể cho nhiều finding (mỗi chủ đề một finding) → gộp theo (luật, cung neo); luật neo “mọi cung”
        // cùng chiều thì gộp các cung vào một mục.
        val byPalace = ev.findings.filter { it.type != null }.groupBy { it.ruleId to it.palace }
        val items = ArrayList<Item>()
        for ((key, fs) in byPalace) {
            val rule = kb.rule(key.first) ?: continue
            val idx = if (rule.anchor == Anchor.AnyPalace) items.indexOfFirst { it.rule.id == rule.id && it.polarity == fs.first().polarity } else -1
            if (idx >= 0) {
                val same = items[idx]
                items[idx] = Item(rule, same.palaces + key.second, same.findings + fs)
            } else {
                items.add(Item(rule, listOf(key.second), fs))
            }
        }

        fun itemJson(it: Item) = buildJsonObject {
            val rule = it.rule
            put("id", rule.id)
            put("name", rule.name)
            put("type", rule.type.name)
            put("typeVn", rule.type.vn)
            put("pol", it.polarity)
            put("w", rule.effect.weight)
            put("chi", it.palaces.first().ordinal)
            put("chis", buildJsonArray { it.palaces.forEach { p -> add(p.ordinal) } })
            val role = roles(it.palaces)
            put("role", when (val a = rule.anchor) {
                is Anchor.Layer -> when (a.layer) {
                    LimitPoint.DAI_HAN -> "Đại hạn · $role"
                    LimitPoint.TIEU_HAN -> "Tiểu hạn · $role"
                    LimitPoint.LUU_NIEN_DAI_HAN -> "Lưu niên ĐH · $role"
                    LimitPoint.LUU_THAI_TUE -> "Thái Tuế · $role"
                }
                else -> role
            })
            put("topics", buildJsonArray { it.findings.map { f -> f.topic.vn }.distinct().forEach { t -> add(t) } })
            put("text", fill(kb.texts[rule.textKey]?.text ?: rule.name, it.palaces))
            put("why", buildJsonArray { it.findings.flatMap { f -> f.evidence }.distinct().forEach { e -> add(e.toString()) } })
            put("advice", buildJsonArray {
                for (k in rule.adviceKeys) {
                    val a = kb.advice[k] ?: continue
                    add(buildJsonObject { put("t", a.text); put("type", a.type.name) })
                }
            })
            put("rel", rule.reliability.name)
            put("source", rule.source)
            put("reviewed", rule.reviewed)
        }

        fun section(scope: Scope) = JsonArray(
            items.filter { it.rule.scope == scope }.sortedWith(
                compareBy<Item> { TYPE_ORDER.indexOf(it.rule.type) }
                    .thenByDescending { it.rule.effect.weight * maxOf(1, kotlin.math.abs(it.polarity)) }
                    .thenBy { it.palaces.first().stepsFrom(chart.menh) },
            ).map { itemJson(it) },
        )

        return buildJsonObject {
            put("version", kb.version)
            put("draft", true)
            put("expert", expert)
            put("natal", section(Scope.GOC))
            put("daiHan", section(Scope.DAI_HAN))
            put("year", section(Scope.NAM))
            // Câu luận từng cung: bộ đồng cung / chính tinh theo độ sáng / phụ tinh (KB phần a, b, c).
            put("palaces", buildJsonArray {
                val order = listOf("COMBO_", "STAR_", "PHU_")
                for (chi in Chi.entries) add(buildJsonArray {
                    val here = ev.findings.filter { it.type == null && it.palace == chi }
                    for (f in here.sortedBy { x -> order.indexOfFirst { x.ruleId.startsWith(it) } }) {
                        val stars = f.evidence.mapNotNull { e -> e.star?.let { s -> chart.palace(chi).star(s) } }
                        add(buildJsonObject {
                            put("kind", order.first { f.ruleId.startsWith(it) }.trimEnd('_').lowercase())
                            put("title", stars.joinToString(" – ") { s ->
                                s.id.vn + (if (s.kind == StarKind.CHINH_TINH && s.brightness != null) " (${s.brightness!!.vn})" else "")
                            })
                            put("pol", f.polarity)
                            put("text", fill(kb.texts[f.textKey]?.text ?: "", listOf(chi)))
                        })
                    }
                })
            })
            put("counts", buildJsonObject {
                put("rules", kb.rules.size)
                put("fired", ev.firedRules.size)
                put("suppressed", ev.suppressed.size)
            })
        }
    }
}
