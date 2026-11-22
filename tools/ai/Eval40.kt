package vn.tuvi.tools.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vn.tuvi.app.api.AppApi
import vn.tuvi.app.api.LlmHost
import vn.tuvi.app.api.MemoryStore
import vn.tuvi.app.llm.LiteRt
import vn.tuvi.core.interpret.AiModelConfig
import java.io.File
import java.time.LocalDate

/**
 * Chấm bộ 40 câu hỏi mẫu (mục 12.1, D-57) với model thật chạy bằng LiteRT-LM trên máy tính (CPU), qua ĐÚNG luồng
 * của app: AppApi “askAi” → AiPrompt → model → AiGuard. Ghi báo cáo Markdown + JSON.
 * Dùng: Eval40 <model.litertlm> <questions.json> <báo-cáo.md> [số câu tối đa]
 */
class JvmLlm(private val model: String) : LlmHost {
    private var rt: LiteRt? = null
    var lastRaw: String = ""
    var loadMs = 0L

    override fun status(): JsonObject = buildJsonObject { put("state", "ready"); put("backend", "CPU (máy tính)") }
    override fun action(name: String): JsonObject = status()
    override fun generate(system: String, user: String, cfg: AiModelConfig): String {
        val r = rt ?: run {
            val t0 = System.currentTimeMillis()
            LiteRt.open(model, false, File(System.getProperty("java.io.tmpdir"), "litertlm-cache").also { it.mkdirs() }.path, cfg.maxTokens)
                .also { rt = it; loadMs = System.currentTimeMillis() - t0 }
        }
        lastRaw = r.generate(system, user, cfg.maxOutputTokens, cfg.topK, cfg.topP, cfg.temperature)
        return lastRaw
    }
}

fun main(args: Array<String>) {
    val model = args[0]
    val spec = Json.parseToJsonElement(File(args[1]).readText()).jsonObject
    val out = File(args[2])
    val limit = args.getOrNull(3)?.toInt() ?: Int.MAX_VALUE
    val today = LocalDate.parse(spec.getValue("today").jsonPrimitive.content)
    val charts = spec.getValue("charts").jsonObject
    // Cấu hình theo file model (Nhanh E2B / Mạnh E4B, D-68).
    vn.tuvi.app.api.AiFlow.models().firstOrNull { it.file == File(model).name }?.let { vn.tuvi.app.api.AiFlow.select(it.id) }
    val llm = JvmLlm(model)
    val api = AppApi(MemoryStore(), { today }, llm)
    val rows = ArrayList<JsonObject>()
    val qs = spec.getValue("questions").jsonArray.take(limit)
    for ((i, e) in qs.withIndex()) {
        val o = e.jsonObject
        val chartId = o.getValue("chart").jsonPrimitive.content
        val q = o.getValue("q").jsonPrimitive.content
        val input = charts.getValue(chartId).jsonObject
        val req = buildJsonObject { put("input", input); put("question", q); put("today", today.toString()) }.toString()
        val ask = Json.parseToJsonElement(api.call("ask", req)).jsonObject.getValue("data").jsonObject
        llm.lastRaw = ""
        val res = Json.parseToJsonElement(api.call("askAi", req)).jsonObject
        val d = res["data"]?.jsonObject ?: buildJsonObject { put("used", false); put("reason", res["error"]?.jsonPrimitive?.contentOrNull ?: "lỗi") }
        val row = buildJsonObject {
            put("n", i + 1); put("chart", chartId); put("q", q)
            put("understood", ask["understood"]?.jsonPrimitive?.contentOrNull)
            put("score", ask["score"] ?: JsonPrimitive(null as String?))
            put("used", d["used"] ?: JsonPrimitive(false)); put("skip", d["skip"] ?: JsonPrimitive(false))
            put("ms", d["ms"] ?: JsonPrimitive(0)); put("words", d["words"] ?: JsonPrimitive(0))
            put("repaired", d["repaired"] ?: JsonPrimitive(0)); put("retried", d["retried"] ?: JsonPrimitive(false))
            put("text", d["text"] ?: JsonPrimitive(null as String?)); put("reason", d["reason"] ?: JsonPrimitive(null as String?))
            put("issues", d["issues"] ?: JsonArray(emptyList())); put("raw", llm.lastRaw)
            put("analyst", (ask["paragraphs"]?.jsonArray?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "").take(400))
        }
        rows += row
        System.err.println("[${i + 1}/${qs.size}] $chartId · $q → used=${row["used"]} ${row["ms"]} ms ${row["issues"]}")
    }
    File(out.path.removeSuffix(".md") + ".json").writeText(JsonArray(rows).toString())
    out.writeText(report(rows, llm.loadMs, model))
}

private fun JsonObject.s(k: String) = this[k]?.let { (it as? JsonPrimitive)?.contentOrNull } ?: ""

private fun report(rows: List<JsonObject>, loadMs: Long, model: String): String = buildString {
    val asked = rows.filter { it.s("skip") != "true" }
    val ok = asked.filter { it.s("used") == "true" }
    val times = asked.mapNotNull { it.s("ms").toLongOrNull() }.filter { it > 0 }.sorted()
    val median = if (times.isEmpty()) 0 else times[times.size / 2]
    appendLine("# Chấm bộ 40 câu — Trợ lý AI trên máy (${vn.tuvi.app.api.AiFlow.config().label.substringBefore(" (")}, LiteRT-LM)")
    appendLine()
    appendLine("Chạy trên máy build Linux, **CPU 4 nhân**, LiteRT-LM 0.19.1 (`litertlm-jvm`), model `${File(model).name}`.")
    appendLine("Cùng luồng với app: `AppApi.askAi` → lời nhắc từ QaAnalyst → model → kiểm tra mục 12.3 (`AiGuard`).")
    appendLine("Câu nào trượt kiểm tra thì app hiện câu của QaAnalyst (không hiện câu AI).")
    appendLine()
    appendLine("| Chỉ số | Kết quả |")
    appendLine("| --- | --- |")
    appendLine("| Câu hỏi | ${rows.size} (bỏ qua không gọi model: ${rows.size - asked.size}) |")
    appendLine("| Qua kiểm tra, hiện câu AI | **${ok.size}/${asked.size}** |")
    appendLine("| Trượt kiểm tra → hiện câu QaAnalyst | ${asked.size - ok.size} |")
    appendLine("| Nạp model lần đầu | ${loadMs / 1000.0} giây |")
    appendLine("| Thời gian một câu (trung vị / lâu nhất) | ${median / 1000.0} / ${(times.lastOrNull() ?: 0) / 1000.0} giây |")
    appendLine()
    val reasons = asked.filter { it.s("used") != "true" }.flatMap { r -> (r["issues"] as? JsonArray)?.map { it.jsonPrimitive.content.substringBefore(" “").substringBefore(" (").substringBefore(" \"") } ?: emptyList() }
        .groupingBy { it.replace(Regex("\\d+"), "N") }.eachCount().entries.sortedByDescending { it.value }
    if (reasons.isNotEmpty()) {
        appendLine("Lý do trượt (đếm theo loại): " + reasons.joinToString("; ") { "${it.key} × ${it.value}" })
        appendLine()
    }
    for (r in rows) {
        appendLine("## ${r.s("n")}. ${r.s("q")} — lá số ${r.s("chart")}")
        appendLine()
        appendLine("App hiểu là: ${r.s("understood")}" + (r.s("score").takeIf { it.isNotEmpty() }?.let { " · điểm $it/100" } ?: ""))
        appendLine()
        when {
            r.s("skip") == "true" -> appendLine("Không gọi model: ${r.s("reason")}")
            r.s("used") == "true" -> {
                appendLine("**Đạt** (${r.s("ms").toLong() / 1000.0} giây, ${r.s("words")} chữ). Câu AI:")
                appendLine()
                r.s("text").lines().forEach { appendLine("> $it") }
            }
            else -> {
                appendLine("**Trượt** (${r.s("ms").toLongOrNull()?.div(1000.0) ?: 0} giây): ${(r["issues"] as? JsonArray)?.joinToString("; ") { it.jsonPrimitive.content } ?: r.s("reason")}")
                appendLine()
                appendLine("Đầu ra thô của model (app không hiện):")
                appendLine()
                r.s("raw").take(1500).lines().forEach { appendLine("> $it") }
            }
        }
        appendLine()
    }
}
