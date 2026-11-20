package vn.tuvi.core.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Đọc golden.json (docs/PROMPT_TUVI_APP.md mục 14.1) — dùng chung cho test lá số và test vận hạn. */
object Golden {
    val cases: List<JsonObject> by lazy {
        val text = Golden::class.java.getResourceAsStream("/golden.json")!!.use { it.readBytes().toString(Charsets.UTF_8) }
        Json.parseToJsonElement(text).jsonArray.map { it.jsonObject }
    }

    fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

    /** "Nam 15/08/1990 10:30 UTC+7" → BirthInput. */
    fun parseInput(s: String): BirthInput {
        val (g, date, time, zone) = s.trim().split(Regex("\\s+"))
        val (dd, mm, yy) = date.split("/").map { it.toInt() }
        val (hh, mi) = time.split(":").map { it.toInt() }
        return BirthInput(
            solar = LocalDateTime.of(yy, mm, dd, hh, mi),
            lunar = null,
            gender = if (g == "Nam") Gender.NAM else Gender.NU,
            zone = ZoneOffset.of(zone.removePrefix("UTC").let { if (it.length == 2) "${it[0]}0${it[1]}" else it }),
        )
    }

    fun chart(case: JsonObject): Chart = ChartEngine.build(parseInput(case.str("input")))
}
