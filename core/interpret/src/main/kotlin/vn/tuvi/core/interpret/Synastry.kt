package vn.tuvi.core.interpret

import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.NguHanh
import kotlin.math.roundToInt

/**
 * Tương sinh tương khắc giữa hai lá số (D-67): ngũ hành bản mệnh (nạp âm), thiên can và địa chi năm sinh, vị trí cung
 * Mệnh, Cục của người này với bản mệnh người kia, hành chính tinh cung Mệnh. Mỗi quan hệ một câu (kho `hop.*`) và một
 * mức điểm nhỏ; cộng lại thành mức hòa hợp. Luôn là góc nhìn tham khảo, không phán quan hệ thành hay không.
 */
class Synastry(private val kb: KnowledgeBase) {

    /** Một quan hệ đã xét: [points] dương là hòa hợp, âm là khắc xung. */
    data class Link(val key: String, val text: String, val points: Int, val evidence: String)

    class Result(val links: List<Link>, val score: Int, val paragraphs: List<Paragraph>)

    companion object {
        fun canElement(c: Can): NguHanh = when (c.index / 2) { 0 -> NguHanh.MOC; 1 -> NguHanh.HOA; 2 -> NguHanh.THO; 3 -> NguHanh.KIM; else -> NguHanh.THUY }

        /** Quan hệ hai địa chi: tam hợp, lục hợp, lục xung, lục hại, tương hình, cùng chi, bình thường. */
        fun chiRelation(a: Chi, b: Chi): String = when {
            a == b -> "dong"
            a.tamHopGroup == b.tamHopGroup -> "tam_hop"
            (a.index + b.index) % 12 == 1 -> "luc_hop"
            a.opposite == b -> "xung"
            (a.index + b.index) % 12 == 7 -> "hai"
            HINH.any { a in it && b in it } -> "hinh"
            else -> "binh"
        }

        private val HINH = listOf(setOf(Chi.DAN, Chi.TI, Chi.THAN), setOf(Chi.SUU, Chi.TUAT, Chi.MUI), setOf(Chi.TY, Chi.MAO))

        private val CHI_POINTS = mapOf("tam_hop" to 2, "luc_hop" to 2, "xung" to -2, "hai" to -1, "hinh" to -1, "dong" to 0, "binh" to 0)

        /** Hành của chuỗi “Âm Thổ”, “Thủy đới Mộc” → hành chính (Thổ, Thủy). */
        fun elementOf(hanh: String): NguHanh? {
            val w = hanh.removePrefix("Âm ").removePrefix("Dương ").substringBefore(' ')
            return NguHanh.entries.firstOrNull { it.vn == w }
        }
    }

    private fun t(key: String, vararg kv: Pair<String, String>): String? {
        var s = kb.texts[key]?.text ?: return null
        for ((k, v) in kv) s = s.replace("{$k}", v)
        return s
    }

    /** Xét đủ các quan hệ giữa [a] (tên [na]) và [b] (tên [nb]). */
    fun analyze(a: Chart, b: Chart, na: String, nb: String, topic: Topic): Result {
        val links = ArrayList<Link>()
        fun add(key: String, points: Int, evidence: String, vararg kv: Pair<String, String>) {
            t(key, *kv)?.let { links += Link(key, it, points, evidence) }
        }
        // 1. Bản mệnh (nạp âm).
        val ha = a.banMenh.element
        val hb = b.banMenh.element
        val ev1 = "${na}: ${a.banMenh.name} (${ha.vn}); ${nb}: ${b.banMenh.name} (${hb.vn})"
        when {
            ha == hb -> add("hop.menh.hoa", 1, ev1, "ha" to ha.vn)
            ha.generates == hb -> add("hop.menh.sinh", 2, ev1, "a" to na, "b" to nb, "ha" to ha.vn, "hb" to hb.vn)
            hb.generates == ha -> add("hop.menh.sinh", 2, ev1, "a" to nb, "b" to na, "ha" to hb.vn, "hb" to ha.vn)
            ha.overcomes == hb -> {
                add("hop.menh.khac", -2, ev1, "a" to na, "b" to nb, "ha" to ha.vn, "hb" to hb.vn)
                add("hop.menh.trung_gian", 0, ev1, "x" to ha.generates.vn)
            }
            else -> {
                add("hop.menh.khac", -2, ev1, "a" to nb, "b" to na, "ha" to hb.vn, "hb" to ha.vn)
                add("hop.menh.trung_gian", 0, ev1, "x" to hb.generates.vn)
            }
        }
        // 2. Thiên can năm sinh.
        val ca = a.yearCan
        val cb = b.yearCan
        val ea = canElement(ca)
        val eb = canElement(cb)
        val pairCan = "${ca.vn} (${na}) – ${cb.vn} (${nb})"
        val ev2 = "Can năm: $pairCan; hành ${ea.vn} – ${eb.vn}"
        when {
            Math.floorMod(ca.index - cb.index, 10) == 5 -> add("hop.can.hop", 2, ev2, "x" to pairCan)
            ea == eb -> add("hop.can.hoa", 1, ev2, "x" to pairCan)
            ea.generates == eb || eb.generates == ea -> add("hop.can.sinh", 1, ev2, "x" to pairCan)
            else -> add("hop.can.khac", -1, ev2, "x" to pairCan)
        }
        // 3. Địa chi năm sinh (tuổi).
        val rel = chiRelation(a.yearChi, b.yearChi)
        val pairChi = if (rel == "dong") a.yearChi.vn else "${a.yearChi.vn} (${na}) – ${b.yearChi.vn} (${nb})"
        add("hop.chi.$rel", CHI_POINTS.getValue(rel), "Tuổi: ${a.yearChi.vn} – ${b.yearChi.vn}", "x" to pairChi)
        // 4. Vị trí cung Mệnh.
        val mr = chiRelation(a.menh, b.menh)
        val pairMenh = "${a.menh.vn} – ${b.menh.vn}"
        when (mr) {
            "tam_hop" -> add("hop.cung.tam_hop", 1, "Cung Mệnh: $pairMenh", "x" to pairMenh)
            "luc_hop" -> add("hop.cung.luc_hop", 1, "Cung Mệnh: $pairMenh", "x" to pairMenh)
            "xung" -> add("hop.cung.xung", -1, "Cung Mệnh: $pairMenh", "x" to pairMenh)
            "dong" -> add("hop.cung.dong", 0, "Cung Mệnh: $pairMenh", "x" to a.menh.vn)
        }
        // 5. Cục người này với bản mệnh người kia (hai chiều).
        for ((x, y, nx, ny) in listOf(Quad(a, b, na, nb), Quad(b, a, nb, na))) {
            val ce = x.cuc.element
            val me = y.banMenh.element
            val ev = "Cục của $nx: ${x.cuc.vn}; bản mệnh $ny: ${me.vn}"
            if (ce.generates == me) add("hop.cuc.sinh", 1, ev, "a" to nx, "b" to ny, "x" to x.cuc.vn, "hb" to me.vn)
            else if (ce.overcomes == me) add("hop.cuc.khac", -1, ev, "a" to nx, "b" to ny, "x" to x.cuc.vn, "hb" to me.vn)
        }
        // 6. Hành chính tinh cung Mệnh (chính tinh đầu; vô chính diệu thì mượn cung xung chiếu).
        fun mainEl(c: Chart): Pair<String, NguHanh>? {
            val s = (c.palace(c.menh).chinhTinh.ifEmpty { c.palace(c.menh.opposite).chinhTinh }).firstOrNull() ?: return null
            val h = kb.mainMeta[s.id]?.hanh ?: return null
            return elementOf(h)?.let { "${s.id.vn}, ${it.vn}" to it }
        }
        val sa = mainEl(a)
        val sb = mainEl(b)
        if (sa != null && sb != null && sa.second != sb.second) {
            val ev = "Chính tinh Mệnh: ${na} ${sa.first}; ${nb} ${sb.first}"
            when {
                sa.second.generates == sb.second -> add("hop.chinh.sinh", 1, ev, "a" to na, "b" to nb, "x" to sa.first, "y" to sb.first)
                sb.second.generates == sa.second -> add("hop.chinh.sinh", 1, ev, "a" to nb, "b" to na, "x" to sb.first, "y" to sa.first)
                sa.second.overcomes == sb.second -> add("hop.chinh.khac", -1, ev, "a" to na, "b" to nb, "x" to sa.first, "y" to sb.first)
                sb.second.overcomes == sa.second -> add("hop.chinh.khac", -1, ev, "a" to nb, "b" to na, "x" to sb.first, "y" to sa.first)
            }
        }
        val sum = links.sumOf { it.points }
        val score = (50 + 6.0 * sum).roundToInt().coerceIn(10, 95)
        val good = links.count { it.points > 0 }
        val bad = links.count { it.points < 0 }
        val tally = "$good điểm hợp, $bad điểm khắc xung; mức hòa hợp $score/100"
        val tongKey = when { score >= 62 -> "hop.tong.tot"; score <= 42 -> "hop.tong.can"; else -> "hop.tong.trung" }
        val paras = ArrayList<Paragraph>()
        val src = kb.texts["hop.note"]?.source ?: ""
        t(tongKey, "x" to tally)?.let { s ->
            paras += Paragraph(s, if (score >= 62) 1 else if (score <= 42) -1 else 0, ParaKind.WEIGH, topic, listOf("HOP_TONG"),
                listOf(Why("HOP_TONG", "Mức hòa hợp", "Mỗi quan hệ một mức: bản mệnh sinh ±2, can ngũ hợp +2, tuổi tam hợp/lục hợp +2, lục xung −2, hại/hình −1, cung Mệnh, Cục, chính tinh ±1; mức = 50 + 6 × tổng.",
                    links.map { "${it.evidence}: ${if (it.points > 0) "+" else ""}${it.points}" }, src)), false, "Tương sinh tương khắc")
        }
        // Từng yếu tố là một mặt để cân nhắc (có hợp có khắc), câu tổng ở trên mới là kết luận → loại NOTE.
        for (l in links) paras += Paragraph(l.text, if (l.points > 0) 1 else if (l.points < 0) -1 else 0, ParaKind.NOTE, topic,
            listOf(l.key.uppercase().replace('.', '_')), listOf(Why(l.key.uppercase().replace('.', '_'), "Tương sinh tương khắc", l.text, listOf(l.evidence), src)), false)
        t("hop.note")?.let { paras += Paragraph(it, 0, ParaKind.NOTE, topic, listOf("HOP_NOTE"), listOf(Why("HOP_NOTE", "Ghi chú", it, emptyList(), src)), false) }
        return Result(links, score, paras)
    }

    private data class Quad(val x: Chart, val y: Chart, val nx: String, val ny: String)
}
