package vn.tuvi.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import vn.tuvi.app.api.AppApi
import vn.tuvi.app.api.Luan
import vn.tuvi.app.api.KeyValueStore

/**
 * Màn hình duy nhất: WebView hiển thị giao diện trong assets/web, gọi engine qua [Bridge].
 * Mọi tính toán chạy trên máy. Quyền Internet chỉ dùng để tải model Trợ lý AI khi người dùng bấm tải (D-57);
 * WebView không được tải gì từ mạng ([LocalOnlyClient] chặn mọi yêu cầu http/https).
 * Nếu lần chạy trước bị lỗi, màn hình đầu tiên là báo cáo lỗi (CrashLog) để người dùng chụp gửi lại.
 */
class MainActivity : Activity() {

    private var web: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val bg = if (night) Color.rgb(0x14, 0x16, 0x1b) else Color.rgb(0xf6, 0xf2, 0xe9)
        val root = FrameLayout(this)
        root.setBackgroundColor(bg)
        root.setOnApplyWindowInsetsListener(InsetsPadding())

        val report = CrashLog.pending(this)
        if (report != null) {
            showReport(root, report, night)
            applyEdgeToEdge(night, bg)
            return
        }

        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val view = try {
            if (debuggable) WebView.setWebContentsDebuggingEnabled(true)
            WebView(this)
        } catch (e: Throwable) {
            // Thiếu hoặc đang cập nhật “Android System WebView”: báo rõ thay vì văng app.
            showMessage(
                root,
                "Không mở được giao diện vì thiếu Android System WebView.\n\nHãy cập nhật “Android System WebView” và “Chrome” " +
                    "trong Google Play / Galaxy Store rồi mở lại app.\n\n(${e.javaClass.simpleName}: ${e.message})",
                night,
            )
            applyEdgeToEdge(night, bg)
            return
        }
        view.setBackgroundColor(bg)
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        applyEdgeToEdge(night, bg)

        val s = view.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = false // file:///android_asset vẫn đọc được
        s.allowContentAccess = false
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        view.webViewClient = LocalOnlyClient(this)
        view.webChromeClient = ConsoleToLog()
        val prefs = getSharedPreferences("tuvi_tu_xem", Context.MODE_PRIVATE)
        view.addJavascriptInterface(Bridge(this, prefs, view), "TuViNative")
        web = view

        val selfTest = debuggable && intent?.getStringExtra("selftest") != null
        val restored = !selfTest && savedInstanceState != null && view.restoreState(savedInstanceState) != null
        if (!restored) view.loadUrl("file:///android_asset/web/index.html" + if (selfTest) "#selftest" else "")
    }

    /**
     * Gọi SAU setContentView: trước đó cửa sổ chưa có DecorView và `window.insetsController` ném NullPointerException
     * (đây là lỗi làm bản 0.2.0 thoát ngay khi mở trên Android 11+). Lỗi ở đây chỉ ảnh hưởng màu thanh hệ thống.
     */
    private fun applyEdgeToEdge(night: Boolean, bg: Int) {
        try {
            setupEdgeToEdge(night, bg)
        } catch (e: Throwable) {
            Log.w(TAG, "edge-to-edge", e)
        }
    }

    private fun setupEdgeToEdge(night: Boolean, bg: Int) {
        val w = window
        @Suppress("DEPRECATION")
        run {
            w.statusBarColor = Color.TRANSPARENT
            w.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false)
            val c = w.insetsController
            if (c != null) {
                val light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                c.setSystemBarsAppearance(if (night) 0 else light, light)
            }
        } else {
            @Suppress("DEPRECATION")
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            @Suppress("DEPRECATION")
            if (!night) flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = flags
        }
        w.decorView.setBackgroundColor(bg)
    }

    /** Màn báo lỗi lần chạy trước: chữ chọn được, nút Sao chép và Mở app. */
    private fun showReport(root: FrameLayout, report: String, night: Boolean) {
        val fg = if (night) Color.WHITE else Color.BLACK
        val dp = resources.displayMetrics.density
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding((16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        val title = TextView(this)
        title.text = "Lần trước app gặp lỗi"
        title.textSize = 20f
        title.setTypeface(title.typeface, Typeface.BOLD)
        title.setTextColor(fg)
        col.addView(title)
        val hint = TextView(this)
        hint.text = "Bấm “Sao chép” rồi dán gửi cho Claude (hoặc chụp màn hình). Sau đó bấm “Mở app”."
        hint.textSize = 15f
        hint.setTextColor(fg)
        hint.setPadding(0, (6 * dp).toInt(), 0, (10 * dp).toInt())
        col.addView(hint)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val copy = Button(this)
        copy.text = "Sao chép"
        copy.setOnClickListener(CopyReport(this, report))
        val open = Button(this)
        open.text = "Mở app"
        open.setOnClickListener(ReopenApp(this))
        row.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(open, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(row)
        val scroll = ScrollView(this)
        val body = TextView(this)
        body.text = report
        body.textSize = 12f
        body.typeface = Typeface.MONOSPACE
        body.setTextColor(fg)
        body.setTextIsSelectable(true)
        body.setPadding(0, (10 * dp).toInt(), 0, 0)
        scroll.addView(body)
        col.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(col)
        setContentView(root)
    }

    private fun showMessage(root: FrameLayout, text: String, night: Boolean) {
        val msg = TextView(this)
        msg.text = text
        msg.textSize = 17f
        msg.setTextColor(if (night) Color.WHITE else Color.BLACK)
        val pad = (24 * resources.displayMetrics.density).toInt()
        msg.setPadding(pad, pad * 3, pad, pad)
        root.addView(msg)
        setContentView(root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web?.saveState(outState)
    }

    @Deprecated("Giữ cho Android 8–12; Android 13+ vẫn gọi khi chưa bật predictive back")
    override fun onBackPressed() {
        val view = web
        if (view == null) {
            finish()
            return
        }
        view.evaluateJavascript("(window.tvBack && window.tvBack()) ? '1' : '0'", BackResult(this))
    }

    override fun onDestroy() {
        web?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        web = null
        super.onDestroy()
    }

    private class CopyReport(private val activity: Activity, private val text: String) : View.OnClickListener {
        override fun onClick(v: View) {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            cm.setPrimaryClip(ClipData.newPlainText("Báo lỗi Tử Vi Tự Xem", text))
            (v as? Button)?.text = "Đã sao chép"
        }
    }

    private class ReopenApp(private val activity: Activity) : View.OnClickListener {
        override fun onClick(v: View) {
            CrashLog.clear(activity)
            activity.recreate()
        }
    }

    /** Kết quả của nút Back trong giao diện: '1' = giao diện đã tự lùi một màn, ngược lại thoát app. */
    private class BackResult(private val activity: Activity) : android.webkit.ValueCallback<String> {
        override fun onReceiveValue(value: String?) {
            if (value != "\"1\"") activity.finish()
        }
    }

    /** Đệm theo thanh trạng thái, thanh điều hướng, tai thỏ và bàn phím (app vẽ tràn viền từ Android 15). */
    private class InsetsPadding : View.OnApplyWindowInsetsListener {
        override fun onApplyWindowInsets(v: View, insets: WindowInsets): WindowInsets {
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                v.setPadding(b.left, b.top, b.right, b.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            return insets
        }
    }

    /** Chỉ mở trang nội bộ; liên kết ngoài (nếu có) mở bằng trình duyệt. Tiến trình WebView chết thì không kéo app chết theo. */
    private class LocalOnlyClient(private val activity: Activity) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            if (url.scheme == "file") return false
            try {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // Không có ứng dụng mở được liên kết: bỏ qua.
            }
            return true
        }

        /** Lá số không rời máy: trang nội bộ không được gọi ra mạng (kể cả khi app có quyền Internet để tải model). */
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val scheme = request.url.scheme
            if (scheme == "http" || scheme == "https") {
                return WebResourceResponse("text/plain", "utf-8", 403, "Blocked", emptyMap(), java.io.ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            CrashLog.recordText(activity, "Tiến trình hiển thị WebView dừng (didCrash=${detail.didCrash()}).")
            activity.recreate()
            return true
        }
    }

    /** Chuyển console của trang web vào logcat (thẻ TuViWeb) để chẩn đoán và chạy tự kiểm tra. */
    private class ConsoleToLog : WebChromeClient() {
        override fun onConsoleMessage(m: ConsoleMessage): Boolean {
            val line = "${m.messageLevel()} ${m.message()} @${m.sourceId()?.substringAfterLast('/')}:${m.lineNumber()}"
            if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) Log.e(WEB_TAG, line) else Log.i(WEB_TAG, line)
            return true
        }
    }

    private class PrefsStore(private val prefs: SharedPreferences) : KeyValueStore {
        override fun get(key: String): String? = prefs.getString(key, null)
        override fun put(key: String, value: String) {
            prefs.edit().putString(key, value).apply()
        }
    }

    /**
     * Đối tượng `window.TuViNative` trong JavaScript. Chạy trên luồng JavaBridge, không chạm vào View.
     * Engine được tạo khi gọi lần đầu và mọi lỗi (kể cả lỗi nạp lớp) được trả về giao diện thay vì làm app thoát.
     */
    class Bridge(activity: Activity, private val prefs: SharedPreferences, web: WebView) {
        @Volatile private var api: AppApi? = null
        private val activityRef = java.lang.ref.WeakReference(activity)
        private val webRef = java.lang.ref.WeakReference(web)
        private fun appApi(): AppApi = api ?: AppApi(PrefsStore(prefs), llm = AndroidLlm.get(activityRef.get() ?: throw IllegalStateException("màn hình đã đóng"))).also { api = it }

        init {
            // Nạp sẵn knowledge base (luận giải) ở luồng nền để lần mở lá số đầu tiên không phải chờ.
            Thread({
                try { Luan.warmUp() } catch (e: Throwable) { Log.w(TAG, "Nạp knowledge base", e) }
            }, "tuvi-kb").start()
        }

        @JavascriptInterface
        fun call(method: String?, arg: String?): String = try {
            appApi().call(method ?: "", arg)
        } catch (e: Throwable) {
            Log.e(TAG, "Bridge.call($method)", e)
            val trace = Log.getStackTraceString(e).lineSequence().take(8).joinToString(" | ")
            "{\"ok\":false,\"error\":\"" + jsonEscape("Lỗi hệ thống (${e.javaClass.name}: ${e.message}) — $trace") + "\"}"
        }

        @JavascriptInterface
        fun version(): String = APP_VERSION

        /**
         * Gọi bất đồng bộ (Trợ lý AI có thể mất vài chục giây): chạy [method] ở luồng nền riêng, xong thì gọi
         * `window.tvAsyncDone(id, json)` trên luồng giao diện. Các lệnh AI chạy lần lượt trên một luồng.
         */
        @JavascriptInterface
        fun callAsync(id: Int, method: String?, arg: String?) {
            AI_EXEC.execute {
                val out = call(method, arg)
                val view = webRef.get() ?: return@execute
                view.post {
                    try {
                        view.evaluateJavascript("window.tvAsyncDone && window.tvAsyncDone($id, " + org.json.JSONObject.quote(out) + ")", null)
                    } catch (e: Throwable) {
                        Log.i(TAG, "callAsync: WebView đã đóng")
                    }
                }
            }
        }

        /**
         * Xuất tệp: [kind] = "png" (payload = ảnh PNG base64) | "pdf" (payload = mô hình tài liệu JSON);
         * [action] = "save" (Tải xuống / Ảnh) | "share" (bảng chia sẻ). Trả về JSON {ok, path, pages, bytes} hoặc {ok:false, error}.
         */
        @JavascriptInterface
        fun exportFile(kind: String?, action: String?, name: String?, payload: String?): String = try {
            val act = activityRef.get() ?: throw IllegalStateException("màn hình đã đóng")
            val safeName = (name ?: "la-so").replace(Regex("[^A-Za-z0-9._-]"), "-").take(90)
            val mime: String
            val bytes: ByteArray
            var pages = 1
            when (kind) {
                "png" -> { mime = "image/png"; bytes = android.util.Base64.decode(payload ?: "", android.util.Base64.DEFAULT) }
                "pdf" -> {
                    mime = "application/pdf"
                    val pdf = Export.pdf(org.json.JSONObject(payload ?: "{}"))
                    bytes = pdf.bytes; pages = pdf.pages
                }
                else -> throw IllegalArgumentException("không rõ loại tệp $kind")
            }
            if (bytes.isEmpty()) throw IllegalArgumentException("tệp rỗng")
            val path = if (action == "share") { Export.share(act, bytes, safeName, mime); "" } else Export.save(act, bytes, safeName, mime)
            Log.i(TAG, "exportFile $kind $action ${bytes.size} B, $pages trang → $path")
            "{\"ok\":true,\"path\":\"" + jsonEscape(path) + "\",\"pages\":" + pages + ",\"bytes\":" + bytes.size + "}"
        } catch (e: Throwable) {
            Log.e(TAG, "exportFile($kind, $action)", e)
            "{\"ok\":false,\"error\":\"" + jsonEscape("Không xuất được tệp (${e.javaClass.simpleName}: ${e.message})") + "\"}"
        }
    }

    companion object {
        const val APP_VERSION = "0.8.1 (10)"
        const val TAG = "TuViTuXem"
        const val WEB_TAG = "TuViWeb"
        /** Một luồng nền cho các lệnh AI (model không chạy song song hai câu). */
        val AI_EXEC: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor()

        fun jsonEscape(s: String): String {
            val b = StringBuilder(s.length + 16)
            for (ch in s) {
                when {
                    ch == '"' -> b.append("\\\"")
                    ch == '\\' -> b.append("\\\\")
                    ch < ' ' -> b.append(' ')
                    else -> b.append(ch)
                }
            }
            return b.toString()
        }
    }
}
