package vn.tuvi.app.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.tuvi.core.interpret.AiFake
import vn.tuvi.core.interpret.AiGuard
import vn.tuvi.core.interpret.AiModelConfig
import vn.tuvi.core.interpret.AiPrompt
import vn.tuvi.core.interpret.QaAnswer
import vn.tuvi.core.interpret.QaPrivacy

/**
 * Model AI sinh lời do nền tảng cung cấp (D-57): Android dùng LiteRT-LM + model tải về; máy chủ thử dùng bản giả lập.
 * Mọi hàm chạy ở luồng nền ([generate] có thể mất vài chục giây).
 */
interface LlmHost {
    /**
     * Trạng thái cho giao diện: `state` = unsupported | lowram | absent | downloading | verifying | ready | error,
     * kèm `done`/`total` (byte), `wifi`, `waitingWifi`, `freeBytes`, `ramMb`, `backend`, `message`.
     */
    fun status(): JsonObject

    /** `download` | `cancel` | `delete` | `probe` (thử nạp thư viện máy, dùng cho tự kiểm tra). Trả về trạng thái mới. */
    fun action(name: String): JsonObject

    /** Sinh lời (chặn luồng). Ném lỗi nếu model chưa sẵn sàng. */
    fun generate(system: String, user: String, cfg: AiModelConfig): String
}

/** Luồng một câu hỏi qua AI: dựng lời nhắc → model → kiểm tra mục 12.3 → đạt thì trả câu AI, trượt thì báo dùng bản app. */
object AiFlow {
    @Volatile private var cfgCache: AiModelConfig? = null
    fun config(): AiModelConfig = cfgCache ?: AiModelConfig.load().also { cfgCache = it }

    fun modelInfo(): JsonObject {
        val c = config()
        return buildJsonObject {
            put("label", c.label); put("runtime", c.runtime); put("bytes", c.bytes); put("minRamMb", c.minRamMb)
            put("source", c.url.substringAfter("https://").substringBefore("/"))
        }
    }

    fun status(host: LlmHost?): JsonObject {
        val s = host?.status() ?: buildJsonObject { put("state", "unsupported"); put("message", "Bản này chưa có model AI trên máy.") }
        return JsonObject(s + ("model" to modelInfo()))
    }

    fun answer(host: LlmHost?, a: QaAnswer, currentLunarYear: Int, store: KeyValueStore): JsonObject {
        AiPrompt.skipReason(a)?.let { return buildJsonObject { put("used", false); put("skip", true); put("reason", it) } }
        if (host == null) return buildJsonObject { put("used", false); put("reason", "Bản này chưa có model AI trên máy.") }
        val cfg = config()
        val p = AiPrompt.build(a, currentLunarYear, cfg)
        val t0 = System.nanoTime()
        val raw = try {
            host.generate(p.system, p.user, cfg)
        } catch (e: Throwable) {
            return buildJsonObject { put("used", false); put("reason", "Model chưa chạy được: ${e.message ?: e.javaClass.simpleName}") }
        }
        val ms = (System.nanoTime() - t0) / 1_000_000
        val r = AiGuard.check(raw, p.facts, cfg)
        if (!r.ok) log(store, a.question, r.issues, raw)
        return buildJsonObject {
            put("used", r.ok); put("ms", ms); put("words", AiGuard.words(r.text))
            if (r.ok) put("text", r.text) else {
                put("reason", "Câu AI viết chưa qua kiểm tra dữ kiện, app hiện bản luận gốc.")
                put("issues", buildJsonArray { r.issues.take(6).forEach { add(it) } })
            }
        }
    }

    /** Nhật ký câu AI trượt kiểm tra (mục 12.3): chỉ lưu trên máy, tối đa 30 mục, câu hỏi đã ẩn danh. */
    private fun log(store: KeyValueStore, question: String, issues: List<String>, raw: String) {
        val old = try { store.get("aiLog")?.let { Json.parseToJsonElement(it) as? JsonArray } } catch (e: Exception) { null } ?: JsonArray(emptyList())
        val item = buildJsonObject {
            put("t", System.currentTimeMillis()); put("q", QaPrivacy.anonymize(question).take(200))
            put("issues", buildJsonArray { issues.take(8).forEach { add(it) } }); put("raw", raw.take(800))
        }
        store.put("aiLog", JsonArray((old.takeLast(29) + item) as List<JsonElement>).toString())
    }
}

/**
 * Model giả lập cho máy chủ thử giao diện: tải giả trong khoảng 3 giây, rồi “sinh lời” bằng [AiFake].
 * Câu hỏi có chữ “thử lỗi AI” → trả đầu ra hỏng để thử đường rơi về bản QaAnalyst.
 */
class FakeLlmHost(private val delayMs: Long = 400) : LlmHost {
    @Volatile private var state = "absent"
    @Volatile private var done = 0L
    @Volatile private var started = 0L
    @Volatile private var probe: String? = null

    override fun status(): JsonObject {
        val total = AiFlow.config().bytes
        if (state == "downloading") {
            // Tải giả trong khoảng 3 giây.
            done = minOf(total, total * (System.currentTimeMillis() - started) / 3000)
            if (done >= total) state = "ready"
        }
        return buildJsonObject {
            put("state", state); put("done", done); put("total", total); put("wifi", true); put("fake", true)
            put("ramMb", 12000); put("freeBytes", 100_000_000_000L)
            if (state == "ready") put("backend", "giả lập")
            probe?.let { put("probe", it) }
        }
    }

    override fun action(name: String): JsonObject {
        when (name) {
            "download" -> if (state == "absent") { state = "downloading"; done = 0; started = System.currentTimeMillis() }
            "cancel", "delete" -> { state = "absent"; done = 0 }
            "probe" -> probe = "probe-ok: giả lập"

        }
        return status()
    }

    override fun generate(system: String, user: String, cfg: AiModelConfig): String {
        if (state != "ready") throw IllegalStateException("model chưa tải")
        Thread.sleep(delayMs)
        return if ("thử lỗi AI" in user) "{\"answer\": \"Năm 2099 chắc chắn rất tốt.\"}" else AiFake.generate(user)
    }
}
