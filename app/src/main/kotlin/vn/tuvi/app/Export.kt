package vn.tuvi.app

import android.app.Activity
import android.content.ClipData
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException

/**
 * Xuất lá số (mục 2.4): PDF khổ A4 dựng bằng [PdfDocument] ngay trên máy (trang 1 lá số, các trang sau luận giải,
 * chân trang mỗi trang ghi trường phái và lời ngỏ), lưu vào Tải xuống / Ảnh qua MediaStore (không cần quyền bộ nhớ),
 * hoặc gửi qua bảng chia sẻ của Android bằng [ShareProvider].
 */
object Export {
    private const val A4_W = 595
    private const val A4_H = 842
    private const val MARGIN = 42f
    private const val FOOT = 54f
    private const val INK = 0xFF1F1B16.toInt()
    private const val MUTED = 0xFF5C554B.toInt()
    private const val ACCENT = 0xFF6E1423.toInt()
    private const val GOOD = 0xFF2E7D32.toInt()
    private const val BAD = 0xFFB71C1C.toInt()

    class Pdf(val bytes: ByteArray, val pages: Int)

    private fun paint(size: Float, color: Int, style: Int = Typeface.NORMAL): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = Typeface.create(Typeface.SANS_SERIF, style)
        return p
    }

    private fun layout(text: CharSequence, p: TextPaint, w: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, p, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.18f).setIncludePad(false).build()

    /** Dàn trang: chữ chảy qua trang, tiêu đề không đứng trơ cuối trang, chân trang vẽ khi đóng mỗi trang. */
    private class Writer(private val doc: PdfDocument, private val footer: String, private val footer2: String) {
        private var page: PdfDocument.Page? = null
        var pages = 0
        var y = MARGIN
        val left = MARGIN
        val width = A4_W - 2 * MARGIN
        val bottom = A4_H - MARGIN - FOOT
        val canvas: Canvas
            get() {
                ensurePage()
                return page!!.canvas
            }
        private val footPaint = paint(7.2f, MUTED)
        private val linePaint = Paint().also { it.color = 0xFF9A9A9A.toInt(); it.strokeWidth = 0.6f }

        fun newPage() {
            finishPage()
            pages++
            page = doc.startPage(PdfDocument.PageInfo.Builder(A4_W, A4_H, pages).create())
            y = MARGIN
        }

        fun finishPage() {
            val p = page ?: return
            val c = p.canvas
            val top = bottom + 12f
            c.drawLine(left, top, left + width, top, linePaint)
            val l = layout("$footer · Trang $pages\n$footer2", footPaint, width.toInt())
            c.save(); c.translate(left, top + 4f); l.draw(c); c.restore()
            doc.finishPage(p)
            page = null
        }

        fun ensurePage() { if (page == null) newPage() }

        fun block(text: String, p: TextPaint, indent: Float = 0f, before: Float = 0f, after: Float = 3f, keep: Float = 0f, bar: Int = 0) {
            if (text.isBlank()) return
            ensurePage()
            val l = layout(text, p, (width - indent).toInt())
            if (y > MARGIN) y += before
            if (keep > 0f && y > MARGIN && y + l.height + keep > bottom) newPage()
            var start = 0
            while (start < l.lineCount) {
                val avail = bottom - y
                var end = start
                while (end < l.lineCount && l.getLineBottom(end) - l.getLineTop(start) <= avail) end++
                if (end == start) { newPage(); continue }
                val h = (l.getLineBottom(end - 1) - l.getLineTop(start)).toFloat()
                val c = canvas
                if (bar != 0) c.drawRect(left, y + 1.5f, left + 2.2f, y + h - 1.5f, Paint().also { it.color = bar })
                c.save()
                c.clipRect(left + indent, y, left + width + 2f, y + h)
                c.translate(left + indent, y - l.getLineTop(start))
                l.draw(c)
                c.restore()
                y += h
                start = end
                if (start < l.lineCount) newPage()
            }
            y += after
        }

        fun close() = finishPage()
    }

    /** [model]: {title, footer, footer2, image (PNG base64), info: [[nhãn, giá trị]], blocks: [{t, x, pol?}]}. */
    fun pdf(model: JSONObject): Pdf {
        val doc = PdfDocument()
        try {
            val w = Writer(doc, model.optString("footer"), model.optString("footer2"))
            val h1 = paint(16f, ACCENT, Typeface.BOLD)
            val h2 = paint(12.5f, INK, Typeface.BOLD)
            val body = paint(10f, INK)
            val note = paint(9.2f, MUTED, Typeface.ITALIC)
            val strong = paint(10.5f, INK, Typeface.BOLD)
            val key = paint(8.8f, MUTED)
            val value = paint(8.8f, INK, Typeface.BOLD)

            // Trang 1: lá số + thông tin.
            w.newPage()
            w.block(model.optString("title"), h1, after = 6f)
            val info = model.optJSONArray("info") ?: JSONArray()
            val infoH = info.length() * 12.6f + 8f
            val img = model.optString("image")
            if (img.isNotEmpty()) {
                val bytes = Base64.decode(img, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    val maxH = w.bottom - w.y - infoH
                    var iw = w.width
                    var ih = iw * bmp.height / bmp.width
                    if (ih > maxH) { ih = maxH; iw = ih * bmp.width / bmp.height }
                    val x = w.left + (w.width - iw) / 2
                    w.canvas.drawBitmap(bmp, null, RectF(x, w.y, x + iw, w.y + ih), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                    w.y += ih + 8f
                    bmp.recycle()
                }
            }
            for (i in 0 until info.length()) {
                val row = info.optJSONArray(i) ?: continue
                val k = layout(row.optString(0), key, 92)
                val v = layout(row.optString(1), value, (w.width - 98).toInt())
                val rh = maxOf(k.height, v.height).toFloat()
                if (w.y + rh > w.bottom) w.newPage()
                val c = w.canvas
                c.save(); c.translate(w.left, w.y); k.draw(c); c.restore()
                c.save(); c.translate(w.left + 98f, w.y); v.draw(c); c.restore()
                w.y += rh + 2.2f
            }

            // Các trang sau: luận giải.
            val blocks = model.optJSONArray("blocks") ?: JSONArray()
            w.newPage()
            for (i in 0 until blocks.length()) {
                val b = blocks.optJSONObject(i) ?: continue
                val x = b.optString("x")
                val pol = b.optInt("pol", 0)
                when (b.optString("t")) {
                    "brk" -> if (w.y > MARGIN) w.newPage()
                    "h1" -> w.block(x, h1, before = 6f, after = 3f, keep = 60f)
                    "h2" -> w.block(x, h2, before = 9f, after = 3f, keep = 40f)
                    "sub" -> w.block(x, note, after = 2f)
                    "score" -> w.block(x, strong, after = 6f)
                    "note" -> w.block(x, note, indent = 6f, after = 4f)
                    "li" -> w.block("•  $x", body, indent = 6f, after = 2.5f)
                    else -> w.block(x, body, indent = 6f, after = 4f, bar = if (pol > 0) GOOD else if (pol < 0) BAD else 0)
                }
            }
            w.close()
            val out = ByteArrayOutputStream()
            doc.writeTo(out)
            return Pdf(out.toByteArray(), w.pages)
        } finally {
            doc.close()
        }
    }

    /** Lưu vào Tải xuống/TuViTuXem (PDF) hoặc Ảnh/TuViTuXem (PNG). Trả về đường dẫn để báo cho người dùng. */
    fun save(ctx: Context, bytes: ByteArray, name: String, mime: String): String {
        val pdf = mime == "application/pdf"
        if (Build.VERSION.SDK_INT >= 29) {
            val collection = if (pdf) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val dir = (if (pdf) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES) + "/TuViTuXem"
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            val r = ctx.contentResolver
            val uri = r.insert(collection, values) ?: throw IllegalStateException("không tạo được tệp trong $dir")
            try {
                val os = r.openOutputStream(uri) ?: throw IllegalStateException("không mở được tệp để ghi")
                os.use { it.write(bytes) }
                val done = ContentValues()
                done.put(MediaStore.MediaColumns.IS_PENDING, 0)
                r.update(uri, done, null, null)
            } catch (e: Exception) {
                r.delete(uri, null, null)
                throw e
            }
            var shown = name
            r.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) shown = c.getString(0) ?: name }
            return (if (pdf) "Tải xuống" else "Ảnh") + "/TuViTuXem/" + shown
        }
        // Android 8–9: thư mục riêng của app trên bộ nhớ ngoài (không cần quyền).
        val dir = File(ctx.getExternalFilesDir(null), "TuViTuXem")
        dir.mkdirs()
        val f = File(dir, name)
        f.writeBytes(bytes)
        return f.absolutePath
    }

    /** Gửi qua bảng chia sẻ (Zalo, Messenger, Gmail, Drive…). Tệp tạm trong bộ nhớ đệm, dọn sau một ngày. */
    fun share(activity: Activity, bytes: ByteArray, name: String, mime: String) {
        val dir = File(activity.cacheDir, ShareProvider.DIR)
        dir.mkdirs()
        val old = System.currentTimeMillis() - 24L * 3600 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < old) it.delete() }
        File(dir, name).writeBytes(bytes)
        val uri = Uri.parse("content://${ShareProvider.AUTHORITY}/${Uri.encode(name)}")
        val send = Intent(Intent.ACTION_SEND)
        send.type = mime
        send.putExtra(Intent.EXTRA_STREAM, uri)
        send.putExtra(Intent.EXTRA_SUBJECT, name)
        send.clipData = ClipData.newRawUri(name, uri)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, "Gửi lá số")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.runOnUiThread(StartActivity(activity, chooser))
    }

    private class StartActivity(private val activity: Activity, private val intent: Intent) : Runnable {
        override fun run() {
            if (!activity.isFinishing) activity.startActivity(intent)
        }
    }
}

/** Cấp tệp tạm (chỉ đọc) cho ứng dụng nhận khi chia sẻ; không xuất (exported = false), chỉ cấp quyền theo từng lần gửi. */
class ShareProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "vn.tuvi.tuxem.share"
        const val DIR = "share"
    }

    private fun file(uri: Uri): File {
        val name = uri.lastPathSegment ?: throw FileNotFoundException("thiếu tên tệp")
        val ctx = context ?: throw FileNotFoundException("chưa sẵn sàng")
        val dir = File(ctx.cacheDir, DIR).canonicalFile
        val f = File(dir, name).canonicalFile
        if (f.parentFile != dir || !f.isFile) throw FileNotFoundException(name)
        return f
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = if (uri.lastPathSegment.orEmpty().endsWith(".pdf")) "application/pdf" else "image/png"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("chỉ cho phép đọc")
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val f = file(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val c = MatrixCursor(cols)
        val row = arrayOfNulls<Any>(cols.size)
        for (i in cols.indices) {
            row[i] = when (cols[i]) {
                OpenableColumns.DISPLAY_NAME -> f.name
                OpenableColumns.SIZE -> f.length()
                else -> null
            }
        }
        c.addRow(row)
        return c
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
