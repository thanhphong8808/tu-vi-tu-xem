package vn.tuvi.app.dev

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import vn.tuvi.app.api.AppApi
import vn.tuvi.app.api.FakeLlmHost
import vn.tuvi.app.api.MemoryStore
import java.io.File
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Máy chủ thử nghiệm trên máy tính: phục vụ giao diện assets/web và chuyển /api/<hàm> tới [AppApi] —
 * cùng mã Kotlin chạy trong app Android. Dùng để kiểm thử giao diện bằng trình duyệt (Playwright).
 * Dùng: DevServer <thư mục web> [cổng] [ngày hôm nay yyyy-mm-dd]
 */
fun main(args: Array<String>) {
    val root = File(args[0]).canonicalFile
    val port = args.getOrNull(1)?.toInt() ?: 8765
    val fixed = args.getOrNull(2)?.let { LocalDate.parse(it) }
    // Trợ lý AI giả lập (D-57): thử luồng tải model, sinh lời, kiểm tra và rơi về bản QaAnalyst mà không cần model thật.
    // TUVI_AI=off → như máy không có AI.
    val llm = if (System.getenv("TUVI_AI") == "off") null else FakeLlmHost()
    val api = AppApi(MemoryStore(), { fixed ?: LocalDate.now(ZoneOffset.ofHours(7)) }, llm)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    server.createContext("/api/") { ex ->
        val method = ex.requestURI.path.removePrefix("/api/")
        val body = ex.requestBody.readBytes().toString(Charsets.UTF_8)
        send(ex, 200, "application/json; charset=utf-8", api.call(method, body).toByteArray(Charsets.UTF_8))
    }
    server.createContext("/") { ex ->
        val path = ex.requestURI.path.let { if (it == "/") "/index.html" else it }
        val file = File(root, path).canonicalFile
        if (!file.path.startsWith(root.path) || !file.isFile) {
            send(ex, 404, "text/plain", "not found".toByteArray())
        } else {
            val type = when (file.extension) {
                "html" -> "text/html; charset=utf-8"; "css" -> "text/css; charset=utf-8"; "js" -> "text/javascript; charset=utf-8"
                else -> "application/octet-stream"
            }
            send(ex, 200, type, file.readBytes())
        }
    }
    server.executor = java.util.concurrent.Executors.newFixedThreadPool(4)
    server.start()
    println("DevServer http://127.0.0.1:$port/ (web: $root)")
}

private fun send(ex: HttpExchange, code: Int, type: String, bytes: ByteArray) {
    ex.responseHeaders.add("Content-Type", type)
    ex.responseHeaders.add("Cache-Control", "no-store")
    ex.sendResponseHeaders(code, bytes.size.toLong())
    ex.responseBody.use { it.write(bytes) }
}
