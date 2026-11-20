package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.LimitLayers
import java.io.File

/** Một lá số mẫu: ngày âm + canh giờ + giới tính (+ năm xem cho luật vận hạn). Dạng JSON gọn để đọc được bằng mắt. */
data class FixtureSpec(val day: Int, val month: Int, val year: Int, val leap: Boolean, val canh: CanhGio, val gender: Gender, val viewYear: Int?) {
    fun input(): BirthInput = BirthInput(solar = null, lunar = LunarDate(day, month, year, leap), gender = gender, canh = canh)
    fun chart(): Chart = ChartEngine.build(input())
    fun limits(chart: Chart): LimitLayers? = viewYear?.let { LimitEngine.forYear(chart, it) }

    fun toJson(): JsonObject = buildJsonObject {
        put("lunar", "$day/$month${if (leap) "N" else ""}/$year"); put("canh", canh.name); put("gender", gender.name)
        if (viewYear != null) put("viewYear", viewYear)
    }

    override fun toString() = "${gender.vn} ${day}/${month}${if (leap) "N" else ""}/${year} giờ ${canh.name}" + (viewYear?.let { " · năm xem $it" } ?: "")

    companion object {
        fun fromJson(o: JsonObject): FixtureSpec {
            val l = o["lunar"]!!.jsonPrimitive.content
            val (d, m, y) = l.split("/")
            return FixtureSpec(
                d.toInt(), m.removeSuffix("N").toInt(), y.toInt(), m.endsWith("N"),
                CanhGio.valueOf(o["canh"]!!.jsonPrimitive.content), Gender.valueOf(o["gender"]!!.jsonPrimitive.content),
                (o["viewYear"] as? JsonPrimitive)?.int,
            )
        }
    }
}

object Fixtures {
    val kb: KnowledgeBase by lazy { KnowledgeBase.load() }
    val engine: RuleEngine by lazy { RuleEngine(kb) }

    /** rule id → (lá số kích hoạt, lá số không kích hoạt). */
    fun load(): Map<String, Pair<FixtureSpec, FixtureSpec>> {
        val text = Fixtures::class.java.getResourceAsStream("/rule-fixtures.json")!!.readBytes().toString(Charsets.UTF_8)
        val root = Json.parseToJsonElement(text).jsonObject["fixtures"]!!.jsonObject
        return root.mapValues { (_, v) ->
            val o = v.jsonObject
            FixtureSpec.fromJson(o["pos"]!!.jsonObject) to FixtureSpec.fromJson(o["neg"]!!.jsonObject)
        }
    }

    fun fires(rule: Rule, spec: FixtureSpec): Boolean {
        val c = spec.chart()
        return engine.fire(rule, EvalContext(c, spec.limits(c))).isNotEmpty()
    }

    fun writeFile(path: String, content: String) = File(path).writeText(content)
}
