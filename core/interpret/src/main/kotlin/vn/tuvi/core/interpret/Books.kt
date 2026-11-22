package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId

/**
 * Kiến thức từ sách (D-62): *Tử Vi Đẩu Số Toàn Thư* (Thập nhị cung luận) và *Tử Vi Nghiệm Lý* (Thiên Lương), diễn giải
 * lại trong `kb/sach/sach.json` (sinh từ `tools/kb/kb_sach.py`). Chọn câu đúng với lá số: sao tọa thủ cung, độ sáng,
 * sao đồng cung, sao hội về tam phương tứ chính, có hay không sát tinh, chi của cung, can năm sinh.
 */
class Books private constructor(private val cung: List<Entry>, private val nl: List<Nl>, val sourceTT: String, val sourceNL: String) {

    class Entry(
        val role: PalaceRole, val star: StarId, val text: String,
        val bright: String?, val dong: List<StarId>, val hoi: List<StarId>, val sat: Boolean?, val chi: List<Chi>,
    ) {
        /** Số điều kiện: câu càng cụ thể càng được ưu tiên. */
        val weight: Int = listOf(bright != null, dong.isNotEmpty(), hoi.isNotEmpty(), sat != null, chi.isNotEmpty()).count { it }
    }

    class Nl(val text: String, val chapter: String, val groups: List<List<StarId>>, val none: List<StarId>, val can: List<Can>, val roles: List<PalaceRole>, val chi: List<Chi>)

    /** Một câu sách đã chọn: [text] là câu hoàn chỉnh, [source] là sách và chương, [why] là điều kiện khớp trên lá số. */
    data class Line(val text: String, val source: String, val why: String)

    private fun tamPhuong(chi: Chi) = listOf(chi, chi.opposite, chi + 4, chi + 8)

    private fun good(b: Brightness?) = b == Brightness.MIEU || b == Brightness.VUONG || b == Brightness.DAC

    /** Các câu Thập nhị cung cho cung [role] trên lá số gốc: mỗi sao tối đa [perStar] câu, tổng tối đa [max] câu. */
    fun forPalace(chart: Chart, role: PalaceRole, max: Int = 4, perStar: Int = 2, only: StarId? = null): List<Line> {
        val p = chart.palace(role)
        val around = tamPhuong(p.chi).flatMap { chart.palace(it).stars.map { s -> s.id } }.toSet()
        val hasSat = around.any { it in SAT }
        val out = ArrayList<Line>()
        // Chính tinh trước, rồi các sao khác theo thứ tự trong cung.
        val order = p.chinhTinh.map { it.id } + p.stars.map { it.id }.filter { s -> p.chinhTinh.none { it.id == s } }
        val usedCombo = HashSet<String>()
        for (sid in order) {
            if (out.size >= max) break
            if (only != null && sid != only) continue
            val ps = p.star(sid) ?: continue
            val cands = cung.filter { e ->
                e.role == role && e.star == sid &&
                    (e.bright == null || (e.bright == "good" && good(ps.brightness)) || (e.bright == "bad" && ps.brightness == Brightness.HAM)) &&
                    (e.dong.isEmpty() || e.dong.any { p.has(it) }) &&
                    (e.hoi.isEmpty() || e.hoi.any { it in around }) &&
                    (e.sat == null || e.sat == hasSat) &&
                    (e.chi.isEmpty() || p.chi in e.chi)
            }.sortedByDescending { it.weight }
            var n = 0
            for (e in cands) {
                if (n >= perStar || out.size >= max) break
                // Hai sao đồng cung cùng có câu “X cùng Y”: chỉ lấy một lần.
                val key = (listOf(e.star) + e.dong.filter { p.has(it) }).sorted().joinToString("+")
                if (e.dong.isNotEmpty() && !usedCombo.add(key)) continue
                // Cùng một sao vừa có câu thuận vừa có câu về sát tinh: nối bằng “Tuy vậy,” để đọc ra thế cân nhau.
                val lead = if (n > 0 && e.sat == true) "Tuy vậy, cung" else "Cung"
                out += Line("$lead ${role.vn} có ${withMatched(e, p, around)}.", "$sourceTT — Thập nhị cung luận, cung ${role.vn}", conditionText(chart, e, p.chi, around, hasSat))
                n++
            }
        }
        return out
    }

    /**
     * Sách nêu điều kiện là một nhóm sao (“hội Xương Khúc, Tả Hữu, Khôi Việt”); lá số thường chỉ có một phần. Ghi rõ
     * sao thực có ngay trước dấu hai chấm để câu không nói quá lá số: “… (lá số có Văn Xương, Thiên Khôi): …”.
     */
    private fun withMatched(e: Entry, p: vn.tuvi.core.engine.Palace, around: Set<StarId>): String {
        val listed = e.dong.size + e.hoi.size
        val got = (e.dong.filter { p.has(it) } + e.hoi.filter { it in around }).distinct()
        if (listed <= 1 || got.size >= listed) return e.text
        val i = e.text.indexOf(':')
        val note = " (lá số có ${got.joinToString(", ") { it.vn }})"
        return if (i > 0) e.text.substring(0, i) + note + e.text.substring(i) else e.text + note
    }

    private fun conditionText(chart: Chart, e: Entry, chi: Chi, around: Set<StarId>, hasSat: Boolean): String {
        val ps = chart.palace(chi).star(e.star)
        val parts = ArrayList<String>()
        parts += "${e.star.vn} tọa thủ ${e.role.vn} (${chi.vn})" + (ps?.brightness?.let { " — ${it.vn.lowercase()}" } ?: "")
        if (e.dong.isNotEmpty()) parts += "đồng cung: " + e.dong.filter { chart.palace(chi).has(it) }.joinToString(", ") { it.vn }
        if (e.hoi.isNotEmpty()) parts += "hội về tam phương: " + e.hoi.filter { it in around }.joinToString(", ") { it.vn }
        if (e.sat != null) parts += if (hasSat) "tam phương có sát tinh" else "tam phương không có sát tinh"
        return parts.joinToString("; ")
    }

    /** Các ý của *Tử Vi Nghiệm Lý* khớp với sao trong cung [role] (tối đa [max]). */
    fun nghiemLy(chart: Chart, role: PalaceRole, max: Int = 2): List<Line> {
        val p = chart.palace(role)
        val ids = p.stars.map { it.id }.toSet()
        return nl.filter { n ->
            n.groups.all { g -> g.any { it in ids } } && n.none.none { it in ids } &&
                (n.can.isEmpty() || chart.yearCan in n.can) && (n.roles.isEmpty() || role in n.roles) && (n.chi.isEmpty() || p.chi in n.chi)
        }.sortedByDescending { it.groups.size + (if (it.can.isNotEmpty()) 1 else 0) + (if (it.roles.isNotEmpty()) 1 else 0) }
            .take(max)
            .map { n -> Line("Ở cung ${role.vn}: ${n.text}.", "$sourceNL — ${n.chapter}",
                n.groups.joinToString("; ") { g -> g.filter { it in ids }.joinToString(", ") { it.vn } } + " trong cung ${role.vn}") }
    }

    companion object {
        private val SAT = setOf(StarId.KINH_DUONG, StarId.DA_LA, StarId.HOA_TINH, StarId.LINH_TINH, StarId.DIA_KHONG, StarId.DIA_KIEP)

        @Volatile private var cache: Books? = null

        fun load(): Books = cache ?: synchronized(this) {
            cache ?: parse(
                (Books::class.java.getResourceAsStream(KnowledgeBase.RESOURCE_ROOT + "sach/sach.json")
                    ?: throw KbException("Không thấy tài nguyên KB: sach/sach.json")).use { it.readBytes().toString(Charsets.UTF_8) },
            ).also { cache = it }
        }

        private fun JsonObject.s(k: String) = (this[k] as? JsonPrimitive)?.contentOrNull
        private fun JsonObject.list(k: String) = (this[k] as? JsonArray)?.map { (it as JsonPrimitive).content }.orEmpty()
        private fun star(n: String) = StarId.entries.firstOrNull { it.name == n } ?: throw KbException("sach.json: không có sao $n")
        private fun chi(n: String) = Chi.entries.firstOrNull { it.name == n } ?: throw KbException("sach.json: không có chi $n")
        private fun role(n: String) = PalaceRole.entries.firstOrNull { it.name == n } ?: throw KbException("sach.json: không có cung $n")

        fun parse(text: String): Books {
            val o = Json.parseToJsonElement(text) as JsonObject
            val src = o["sources"] as JsonObject
            val cung = (o["cung"] as JsonArray).map { e ->
                val x = e as JsonObject
                Entry(role(x.s("role")!!), star(x.s("s")!!), x.s("t")!!, x.s("b"), x.list("d").map(::star), x.list("h").map(::star),
                    (x["x"] as? JsonPrimitive)?.booleanOrNull, x.list("c").map(::chi))
            }
            val nl = (o["nl"] as JsonArray).map { e ->
                val x = e as JsonObject
                Nl(x.s("t")!!, x.s("ch")!!, (x["g"] as JsonArray).map { g -> (g as JsonArray).map { star((it as JsonPrimitive).content) } },
                    x.list("no").map(::star), x.list("can").map { c -> Can.entries.first { it.name == c } }, x.list("role").map(::role), x.list("chi").map(::chi))
            }
            return Books(cung, nl, src.s("TT")!!, src.s("NL")!!)
        }
    }
}
