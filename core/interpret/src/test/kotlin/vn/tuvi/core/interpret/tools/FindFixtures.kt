package vn.tuvi.core.interpret.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.interpret.Anchor
import vn.tuvi.core.interpret.EvalContext
import vn.tuvi.core.interpret.FixtureSpec
import vn.tuvi.core.interpret.Fixtures
import vn.tuvi.core.interpret.Predicate
import vn.tuvi.core.interpret.Rule
import vn.tuvi.core.interpret.Scope
import java.io.File
import kotlin.random.Random

/**
 * Tìm lá số mẫu cho mọi luật: một lá số kích hoạt (pos) và một lá số “suýt khớp” không kích hoạt (neg: predicate đầu
 * tiên của luật đúng ở một cung neo nhưng cả luật không đúng). Bước 1 ngẫu nhiên có seed; bước 2 vét cạn mọi cấu trúc
 * lá số (60 can chi năm × 12 tháng × 30 ngày × 12 giờ × 2 giới) cho các luật còn thiếu. Luật vẫn không tìm được lá số
 * kích hoạt là luật không thể xảy ra → báo lỗi.
 */
fun main(args: Array<String>) {
    val out = args.getOrNull(0) ?: "core/interpret/src/test/resources/rule-fixtures.json"
    val kb = Fixtures.kb
    val engine = Fixtures.engine
    val pos = LinkedHashMap<String, FixtureSpec>()
    val neg = LinkedHashMap<String, FixtureSpec>()
    val negFallback = LinkedHashMap<String, FixtureSpec>()
    val firstLeaf = kb.rules.associate { it.id to firstLeaf(it.condition) }
    fun done() = kb.rules.all { it.id in pos && it.id in neg }

    fun visit(spec: FixtureSpec) {
        val chart = try { spec.chart() } catch (e: IllegalArgumentException) { return }
        val ctx = EvalContext(chart, spec.limits(chart))
        for (rule in kb.rules) {
            if (rule.id in pos && rule.id in neg) continue
            val fired = engine.fire(rule, ctx).isNotEmpty()
            if (fired) { pos.putIfAbsent(rule.id, spec); continue }
            if (rule.id in neg) continue
            val leaf = firstLeaf[rule.id]
            val near = leaf != null && engine.anchors(rule, ctx).any { leaf.eval(ctx, it, ArrayList()) }
            if (near) neg[rule.id] = spec else negFallback.putIfAbsent(rule.id, spec)
        }
    }

    val rnd = Random(20261120)
    val t0 = System.currentTimeMillis()
    var n = 0
    while (n < 200_000 && !done()) {
        val y = 1930 + rnd.nextInt(80)
        val m = 1 + rnd.nextInt(12)
        val d = 1 + rnd.nextInt(29)
        val canh = CanhGio.TWELVE[rnd.nextInt(12)]
        val g = if (rnd.nextBoolean()) Gender.NAM else Gender.NU
        visit(FixtureSpec(d, m, y, false, canh, g, y + 5 + rnd.nextInt(70)))
        n++
    }
    println("Ngẫu nhiên: $n lá số, ${(System.currentTimeMillis() - t0) / 1000}s; còn thiếu pos=${kb.rules.count { it.id !in pos }}, neg=${kb.rules.count { it.id !in neg }}")

    if (!done()) {
        // Vét cạn cấu trúc: năm 1984–2043 phủ đủ 60 can chi.
        loop@ for (year in 1984..2043) for (month in 1..12) for (day in 1..30) {
            if (LunarCalendar.lunarToSolar(vn.tuvi.core.calendar.LunarDate(day, month, year, false)) == null) continue
            for (canh in CanhGio.TWELVE) for (g in Gender.entries) {
                visit(FixtureSpec(day, month, year, false, canh, g, year + 20 + (day + month) % 50))
                if (done()) break@loop
            }
        }
        println("Sau vét cạn: còn thiếu pos=${kb.rules.count { it.id !in pos }}, neg=${kb.rules.count { it.id !in neg }}")
    }
    val missingPos = kb.rules.filter { it.id !in pos }.map { it.id }
    for (r in kb.rules) if (r.id !in neg) negFallback[r.id]?.let { neg[r.id] = it }
    val json: JsonObject = buildJsonObject {
        put("note", "Sinh bởi FindFixtures (./gradlew :core:interpret:findFixtures). pos = kích hoạt, neg = suýt khớp nhưng không kích hoạt.")
        put("kbVersion", kb.version)
        putJsonObject("fixtures") {
            for (r in kb.rules) {
                val p = pos[r.id] ?: continue
                val q = neg[r.id] ?: continue
                putJsonObject(r.id) { put("pos", p.toJson()); put("neg", q.toJson()) }
            }
        }
    }
    File(out).writeText(json.toString().replace("},\"", "},\n\"") + "\n")
    println("Đã ghi ${pos.size} luật vào $out")
    if (missingPos.isNotEmpty()) {
        System.err.println("KHÔNG THỂ KÍCH HOẠT: $missingPos")
        kotlin.system.exitProcess(2)
    }
}

private fun firstLeaf(p: Predicate): Predicate? = when (p) {
    is Predicate.AllOf -> p.items.firstOrNull()?.let { firstLeaf(it) }
    is Predicate.AnyOf -> p.items.firstOrNull()?.let { firstLeaf(it) }
    is Predicate.NoneOf -> null
    else -> p
}

@Suppress("unused")
private fun describe(r: Rule) = "${r.id} (${r.scope}${if (r.anchor is Anchor.Layer) ", tầng" else ""})" + if (r.scope == Scope.GOC) "" else " · cần năm xem"
