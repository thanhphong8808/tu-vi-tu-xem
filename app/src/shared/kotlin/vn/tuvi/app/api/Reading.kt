package vn.tuvi.app.api

import kotlinx.serialization.json.JsonArray
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
import vn.tuvi.core.interpret.Finding
import vn.tuvi.core.interpret.KnowledgeBase
import vn.tuvi.core.interpret.LimitPoint
import vn.tuvi.core.interpret.Rule
import vn.tuvi.core.interpret.RuleEngine
import vn.tuvi.core.interpret.RuleType
import vn.tuvi.core.interpret.Scope

/**
 * Luận giải sơ bộ (giai đoạn 3): cách cục và dấu hiệu do RuleEngine dò trên lá số + năm đang xem, kèm câu luận
 * nháp trong knowledge base (chưa duyệt). Bộ ghép câu và chấm điểm theo chủ đề thuộc giai đoạn 4.
 */
object Reading {
    @Volatile private var loaded: Pair<KnowledgeBase, RuleEngine>? = null

    /** Nạp knowledge base một lần (≈ 0,6 MB JSON trong APK). Gọi sớm ở luồng nền để lần mở lá số đầu không phải chờ. */
    fun engine(): Pair<KnowledgeBase, RuleEngine> = loaded ?: synchronized(this) {
        loaded ?: KnowledgeBase.load().let { it to RuleEngine(it) }.also { loaded = it }
    }

    fun warmUp() { engine() }

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
