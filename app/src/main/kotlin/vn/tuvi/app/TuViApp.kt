package vn.tuvi.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Cài bộ ghi lỗi ngay khi tiến trình khởi động, trước cả Activity. */
class TuViApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}

/**
 * Ghi lỗi làm app dừng vào file trong bộ nhớ riêng của app; lần mở sau MainActivity hiện nguyên văn để người dùng
 * sao chép gửi lại. Không gửi đi đâu cả.
 */
object CrashLog {
    private const val FILE = "bao-loi.txt"

    fun install(ctx: Context) {
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(Handler(app, previous))
    }

    private class Handler(private val ctx: Context, private val previous: Thread.UncaughtExceptionHandler?) :
        Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            try {
                recordText(ctx, "Luồng: ${t.name}\n\n" + Log.getStackTraceString(e))
            } catch (ignored: Throwable) {
                // Không để bộ ghi lỗi tự gây lỗi.
            }
            if (previous != null) {
                previous.uncaughtException(t, e)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(10)
            }
        }
    }

    fun recordText(ctx: Context, detail: String) {
        val text = StringBuilder()
            .append("Tử Vi Tự Xem ").append(MainActivity.APP_VERSION).append('\n')
            .append("Thời điểm: ").append(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ROOT).format(Date())).append('\n')
            .append("Máy: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            .append("WebView: ").append(webViewVersion()).append("\n\n")
            .append(detail)
            .toString()
        Log.e(MainActivity.TAG, text)
        File(ctx.filesDir, FILE).writeText(text)
    }

    fun pending(ctx: Context): String? = try {
        val f = File(ctx.filesDir, FILE)
        if (f.exists()) f.readText() else null
    } catch (e: Throwable) {
        null
    }

    fun clear(ctx: Context) {
        File(ctx.filesDir, FILE).delete()
    }

    private fun webViewVersion(): String = try {
        val p = WebView.getCurrentWebViewPackage()
        if (p == null) "không có" else "${p.packageName} ${p.versionName}"
    } catch (e: Throwable) {
        "không rõ"
    }
}
