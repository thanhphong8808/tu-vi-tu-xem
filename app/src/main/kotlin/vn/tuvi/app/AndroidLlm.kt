package vn.tuvi.app

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.tuvi.app.api.AiFlow
import vn.tuvi.app.api.LlmHost
import vn.tuvi.app.llm.LiteRt
import vn.tuvi.core.interpret.AiModelConfig
import java.io.File
import java.security.MessageDigest

/**
 * Trợ lý AI trên điện thoại (D-57): tải model bằng DownloadManager của hệ thống (chỉ Wi-Fi, tự tải tiếp khi đứt mạng,
 * có thông báo tiến độ), lưu trong thư mục riêng của app (Android/data/vn.tuvi.tuxem/files/models, gỡ app là mất),
 * kiểm SHA-256 rồi mới dùng; nạp bằng LiteRT-LM, thử GPU trước, lỗi thì CPU.
 *
 * Một đối tượng cho cả tiến trình. Mọi hàm chạy ở luồng nền (luồng JavaBridge hoặc luồng ai của MainActivity).
 */
class AndroidLlm private constructor(private val ctx: Context) : LlmHost {
    private val prefs = ctx.getSharedPreferences("tuvi_ai", Context.MODE_PRIVATE)
    private val cfg: AiModelConfig get() = AiFlow.config()
    private val dir: File get() = (ctx.getExternalFilesDir("models") ?: File(ctx.filesDir, "models")).also { it.mkdirs() }
    private val modelFile: File get() = File(dir, cfg.file)
    private val partFile: File get() = File(dir, cfg.file + ".part")
    private val okFile: File get() = File(dir, cfg.file + ".ok")

    @Volatile private var engine: LiteRt? = null
    @Volatile private var verifying = false
    @Volatile private var verifyPct = 0
    @Volatile private var lastError: String? = null
    @Volatile private var probeResult: String? = null
    private val lock = Object()

    init {
        // Lần trước tiến trình chết giữa lúc nạp GPU (lỗi trong thư viện máy) → không thử GPU nữa.
        if (prefs.getString(GPU_KEY, null) == "trying") prefs.edit().putString(GPU_KEY, "bad").commit()
        // Model người dùng đã chọn (Nhanh E2B / Mạnh E4B, D-68).
        AiFlow.select(prefs.getString(MODEL_KEY, null))
    }

    /** Id các model đã tải và kiểm SHA-256 xong (có thể có cả hai). */
    private fun have(): List<String> = AiFlow.models().filter { m ->
        val ok = File(dir, m.file + ".ok")
        val f = File(dir, m.file)
        ok.isFile && f.isFile && f.length() == m.bytes && ok.readText().trim() == m.sha256
    }.map { it.id }

    private fun ramMb(): Long {
        val mi = ActivityManager.MemoryInfo()
        (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        return mi.totalMem / (1024 * 1024)
    }

    private fun abiOk(): Boolean = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }

    private fun wifi(): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    } catch (e: Exception) {
        false
    }

    private fun verified(): Boolean = okFile.isFile && modelFile.isFile && modelFile.length() == cfg.bytes &&
        okFile.readText().trim() == cfg.sha256

    private fun dm() = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    override fun status(): JsonObject {
        synchronized(lock) { return statusLocked() }
    }

    private fun statusLocked(): JsonObject {
        val ram = ramMb()
        val base = buildJsonObject {
            put("ramMb", ram); put("wifi", wifi()); put("freeBytes", dir.usableSpace); put("total", cfg.bytes)
            put("gpu", prefs.getString(GPU_KEY, null) ?: "chưa thử")
            put("have", kotlinx.serialization.json.JsonArray(have().map { kotlinx.serialization.json.JsonPrimitive(it) }))
            probeResult?.let { put("probe", it) }
        }
        fun with(state: String, extra: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()) =
            JsonObject(base + ("state" to kotlinx.serialization.json.JsonPrimitive(state)) + extra)
        fun p(v: Any?) = when (v) {
            is Number -> kotlinx.serialization.json.JsonPrimitive(v)
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
            else -> kotlinx.serialization.json.JsonPrimitive(v?.toString())
        }
        if (!abiOk()) return with("unsupported", mapOf("message" to p("Bộ xử lý của máy chưa được LiteRT-LM hỗ trợ.")))
        if (ram < cfg.minRamMb) return with("lowram", mapOf("message" to p("Máy có ${ram / 1024} GB RAM, model cần khoảng ${cfg.minRamMb / 1024 + 1} GB.")))
        if (verified()) {
            val e = engine
            return with("ready", mapOf("loaded" to p(e != null), "backend" to p(e?.backend()), "message" to p(lastError)))
        }
        if (verifying) return with("verifying", mapOf("pct" to p(verifyPct)))
        val id = prefs.getLong(DL_KEY, -1L)
        if (id >= 0) {
            dm().query(DownloadManager.Query().setFilterById(id))?.use { c ->
                if (c.moveToFirst()) {
                    val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    when (st) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            startVerify(id)
                            return with("verifying", mapOf("pct" to p(0)))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            prefs.edit().remove(DL_KEY).apply()
                            dm().remove(id)
                            lastError = "Tải model lỗi (mã $reason). Bấm tải lại."
                        }
                        else -> return with("downloading", mapOf(
                            "done" to p(done), "paused" to p(st == DownloadManager.STATUS_PAUSED),
                            "waitingWifi" to p(st == DownloadManager.STATUS_PAUSED &&
                                (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI || reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK)),
                        ))
                    }
                } else {
                    prefs.edit().remove(DL_KEY).apply() // người dùng xóa khỏi danh sách tải của hệ thống
                }
            }
        }
        return with(if (lastError != null) "error" else "absent", mapOf("message" to p(lastError)))
    }

    override fun action(name: String): JsonObject {
        synchronized(lock) {
            when (name) {
                "download" -> download()
                "cancel" -> cancel()
                "delete" -> { cancel(); closeEngine(); modelFile.delete(); okFile.delete(); partFile.delete() }
                "probe" -> probe()
                // Người dùng chọn phần cứng chạy model (D-69): GPU (thử GPU trước) hoặc CPU.
                "backend:cpu" -> { closeEngine(); prefs.edit().putString(GPU_KEY, "off").commit() }
                "backend:gpu" -> { closeEngine(); prefs.edit().remove(GPU_KEY).commit() }
                else -> if (name.startsWith("select:")) select(name.removePrefix("select:"))
            }
        }
        return status()
    }

    private fun download() {
        if (verified() || prefs.getLong(DL_KEY, -1L) >= 0) return
        lastError = null
        val need = cfg.bytes + 300L * 1024 * 1024
        if (dir.usableSpace < need) {
            lastError = "Máy còn trống ${dir.usableSpace / (1024 * 1024)} MB, cần khoảng ${need / (1024 * 1024)} MB."
            return
        }
        partFile.delete(); modelFile.delete(); okFile.delete()
        val req = DownloadManager.Request(Uri.parse(cfg.url))
            .setTitle("Trợ lý AI — Tử Vi Tự Xem")
            .setDescription("${cfg.label}, khoảng ${cfg.bytes / 1_000_000} MB, chỉ tải qua Wi-Fi")
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(partFile))
        val id = dm().enqueue(req)
        prefs.edit().putLong(DL_KEY, id).apply()
        Log.i(TAG, "Bắt đầu tải model ${cfg.id} (id $id)")
    }

    private fun select(id: String) {
        if (id == cfg.id) return
        if (prefs.getLong(DL_KEY, -1L) >= 0 || verifying) {
            lastError = "Đang tải model: hủy tải trước khi đổi model."
            return
        }
        closeEngine()
        AiFlow.select(id)
        prefs.edit().putString(MODEL_KEY, cfg.id).apply()
        lastError = null
        Log.i(TAG, "Chọn model ${cfg.id}")
    }

    private fun cancel() {
        val id = prefs.getLong(DL_KEY, -1L)
        if (id >= 0) dm().remove(id)
        prefs.edit().remove(DL_KEY).apply()
        partFile.delete()
        lastError = null
    }

    /** Kiểm SHA-256 ở luồng riêng (2,6 GB mất khoảng 10–30 giây), đúng thì đổi tên thành file model. */
    private fun startVerify(id: Long) {
        if (verifying) return
        verifying = true
        verifyPct = 0
        Thread({
            try {
                val md = MessageDigest.getInstance("SHA-256")
                val buf = ByteArray(1 shl 20)
                var read = 0L
                val total = partFile.length().coerceAtLeast(1)
                partFile.inputStream().use { input ->
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        md.update(buf, 0, n)
                        read += n
                        verifyPct = (read * 100 / total).toInt()
                    }
                }
                val hex = md.digest().joinToString("") { b -> String.format("%02x", b) }
                synchronized(lock) {
                    prefs.edit().remove(DL_KEY).apply()
                    if (hex == cfg.sha256 && partFile.length() == cfg.bytes && partFile.renameTo(modelFile)) {
                        okFile.writeText(hex)
                        Log.i(TAG, "Model đã kiểm SHA-256, sẵn sàng")
                    } else {
                        partFile.delete()
                        lastError = "File tải về không đúng (SHA-256 khác). Bấm tải lại."
                    }
                    try { dm().remove(id) } catch (e: Exception) { }
                }
            } catch (e: Throwable) {
                lastError = "Không kiểm được file model: ${e.message}"
            } finally {
                verifying = false
            }
        }, "tuvi-ai-verify").start()
    }

    /** Tự kiểm tra: nạp thư viện máy và gọi LiteRT-LM với một file không phải model (phải báo lỗi, không văng app). */
    private fun probe() {
        probeResult = try {
            System.loadLibrary("litertlm_jni")
            val bogus = File(ctx.cacheDir, "probe.litertlm").also { it.writeText("not a model") }
            try {
                LiteRt.open(bogus.path, false, ctx.cacheDir.path, 256).close()
                "probe: nạp được file giả (không mong đợi)"
            } catch (e: Throwable) {
                "probe-ok: ${e.javaClass.simpleName}"
            } finally {
                bogus.delete()
            }
        } catch (e: Throwable) {
            "probe-fail: ${e.javaClass.simpleName}: ${e.message}"
        }
        Log.i(TAG, "AI probe → $probeResult")
    }

    private fun closeEngine() {
        val e = engine
        engine = null
        try { e?.close() } catch (t: Throwable) { }
    }

    /** Giải phóng model khỏi RAM (máy thiếu bộ nhớ); lần hỏi sau tự nạp lại. */
    fun release() {
        synchronized(genLock) { closeEngine() }
    }

    private val genLock = Object()

    private fun ensureEngine(): LiteRt {
        engine?.let { return it }
        if (!verified()) throw IllegalStateException("chưa tải model")
        val cache = File(ctx.cacheDir, "litertlm").also { it.mkdirs() }.path
        val gpu = prefs.getString(GPU_KEY, null)
        if (gpu != "bad" && gpu != "off") {
            prefs.edit().putString(GPU_KEY, "trying").commit()
            try {
                val e = LiteRt.open(modelFile.path, true, cache, cfg.maxTokens)
                prefs.edit().putString(GPU_KEY, "ok").apply()
                engine = e
                Log.i(TAG, "Đã nạp model trên GPU")
                return e
            } catch (t: Throwable) {
                prefs.edit().putString(GPU_KEY, "bad").commit()
                Log.i(TAG, "GPU không dùng được (${t.javaClass.simpleName}: ${t.message}), chuyển CPU")
            }
        }
        val e = LiteRt.open(modelFile.path, false, cache, cfg.maxTokens)
        engine = e
        Log.i(TAG, "Đã nạp model trên CPU")
        return e
    }

    /** Đầu ra hỏng khi chạy GPU (D-69): đóng model, ghi nhớ không dùng GPU nữa, lần sinh sau nạp lại trên CPU. */
    override fun fallbackToCpu(): Boolean = synchronized(genLock) {
        val e = engine ?: return@synchronized false
        if (e.backend() != "GPU") return@synchronized false
        closeEngine()
        prefs.edit().putString(GPU_KEY, "bad").commit()
        Log.i(TAG, "Đầu ra GPU có dấu hiệu hỏng, chuyển sang CPU")
        true
    }

    override fun generate(system: String, user: String, cfg: AiModelConfig): String {
        synchronized(genLock) {
            val e = ensureEngine()
            try {
                return e.generate(system, user, cfg.maxOutputTokens, cfg.topK, cfg.topP, cfg.temperature)
            } catch (t: Throwable) {
                lastError = "Model lỗi khi sinh lời: ${t.message}"
                throw t
            }
        }
    }

    companion object {
        private const val TAG = "TuViAI"
        private const val DL_KEY = "download_id"
        private const val GPU_KEY = "gpu"
        private const val MODEL_KEY = "model"
        @Volatile private var instance: AndroidLlm? = null

        fun get(ctx: Context): AndroidLlm = instance ?: synchronized(this) {
            instance ?: AndroidLlm(ctx.applicationContext).also { instance = it }
        }

        fun peek(): AndroidLlm? = instance
    }
}
