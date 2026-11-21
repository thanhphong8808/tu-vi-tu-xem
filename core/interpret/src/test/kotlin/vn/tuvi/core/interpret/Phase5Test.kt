package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarKind
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Giai đoạn 5 (mục 2.3 tầng 6, mục 13, cẩm nang P.V.4): lịch ngày, chi tiết ngày + giờ thuận, thẻ sao, so hai lá số,
 * xếp hạng giờ khi không rõ giờ sinh, hiệu năng chấm 30 ngày.
 */
class Phase5Test {
    private val kb = Fixtures.kb
    private val composer = Composer(kb)
    private val a = ChartEngine.build(ReadingTest.GOLDEN.getValue("A"))

    private fun checkReading(r: Reading) {
        assertEquals(emptyList<String>(), ReadingTest.contradictions(r), r.title)
        assertEquals(emptyList<String>(), KbLint.forbiddenIn(r.fullText()), r.title)
        for ((block, p) in r.allParagraphs()) {
            assertTrue(p.ruleIds.isNotEmpty() && p.why.isNotEmpty(), "$block: ${p.text}")
            assertFalse(p.text.contains('{') || p.text.contains('}'), "ô chưa điền: ${p.text}")
        }
        assertTrue(r.score in 5..95)
    }

    @Test
    fun `lịch tháng — đủ ngày, ngày âm khớp lịch, điểm trong 5–95, có ngày tốt và ngày xấu`() {
        for ((y, m) in listOf(2026 to 11, 2026 to 2, 2028 to 2, 2025 to 7)) {
            val days = composer.days(a, y, m)
            assertEquals(LocalDate.of(y, m, 1).lengthOfMonth(), days.size)
            for (d in days) {
                val ld = LunarCalendar.solarToLunar(d.date, 7)
                assertEquals(ld.day, d.lunarDay, "${d.date}"); assertEquals(ld.month, d.lunarMonth); assertEquals(ld.isLeap, d.lunarLeap)
                assertTrue(d.score in 5..95, "${d.date}: ${d.score}")
                assertTrue(d.canChi.isNotBlank() && d.nhatHan != null && d.role != null)
            }
            assertTrue(days.map { it.score }.toSet().size >= 5, "điểm ngày phải phân hóa: ${days.map { it.score }}")
        }
        // Ngày lẻ trong lịch tháng và chi tiết ngày cho cùng một điểm.
        val nov = composer.days(a, 2026, 11)
        for (i in listOf(0, 9, 19, 29)) assertEquals(nov[i].score, composer.day(a, nov[i].date).score)
    }

    @Test
    fun `chi tiết ngày — 12 giờ theo cung thời hạn, giờ thuận, nhãn độ tin cậy, bất biến bản luận`() {
        val r = composer.day(a, LocalDate.of(2026, 11, 20))
        assertEquals(ReadingScope.NGAY, r.scope)
        assertEquals(12, r.hours.size)
        assertEquals(r.hours.map { it.chi }.toSet().size, 12)
        val nh = LimitEngine.forDate(a, LocalDate.of(2026, 11, 20)).nhatHan!!
        for (h in r.hours) assertEquals(nh + h.chi.ordinal, h.palace, "giờ ${h.label}")
        assertTrue(r.summary.any { it.text.startsWith("Giờ thuận: ") })
        assertTrue(r.summary.any { it.text == kb.texts.getValue("note.ngay").text && "độ tin cậy thấp hơn tầng năm" in it.text })
        checkReading(r)
        val rnd = Random(5)
        repeat(60) {
            val t = LocalDateTime.of(1950 + rnd.nextInt(60), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), 0)
            val c = ChartEngine.build(BirthInput(t, null, Gender.entries[rnd.nextInt(2)]))
            val day = LocalDate.of(2020 + rnd.nextInt(10), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28))
            val dr = composer.day(c, day)
            checkReading(dr)
            assertTrue(dr.hours.all { it.score in 5..95 })
            if (c.tuoiAm(LunarCalendar.solarToLunar(day, 7).year) < 16) {
                assertTrue(dr.advice.none { it.text.contains("cưới", true) || it.text.contains("đầu tư", true) })
            }
        }
    }

    @Test
    fun `thẻ sao — đủ 14 chính tinh có chòm, ngũ hành, hóa khí, phụ tinh có ý nghĩa hoặc ghi chú`() {
        val rnd = Random(6)
        val seen = HashSet<StarId>()
        repeat(40) {
            val t = LocalDateTime.of(1950 + rnd.nextInt(60), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), 0)
            val c = ChartEngine.build(BirthInput(t, null, Gender.entries[rnd.nextInt(2)]))
            for (id in StarId.entries.filter { it.kind == StarKind.CHINH_TINH }) {
                val card = composer.starCard(c, id)
                assertNotNull(card, "thiếu thẻ ${id.vn}")
                card!!
                seen.add(id)
                assertEquals(c.starPositions[id], card.chi)
                assertNotNull(card.meta, "thiếu chòm/hành ${id.vn}")
                assertTrue(card.meta!!.dau.isNotBlank() && card.meta!!.hanh.isNotBlank() && card.meta!!.hoaKhi.isNotBlank())
                assertTrue(card.meaning.isNotEmpty() && card.meaning.all { it.why.isNotEmpty() })
                assertTrue(card.brightness != null)
            }
        }
        assertEquals(14, seen.size)
        // Tứ Hóa gốc: ca A (Canh Ngọ, Nam phái) — Vũ Khúc hóa Quyền.
        assertEquals(listOf("Hóa Quyền (can năm Canh)"), composer.starCard(a, StarId.VU_KHUC)!!.natalHoa)
        for (id in a.starPositions.keys.filter { it.kind != StarKind.CHINH_TINH }.take(30)) {
            val card = composer.starCard(a, id)!!
            assertTrue(card.meaning.isNotEmpty(), id.vn)
            assertNull(card.meta)
        }
    }

    @Test
    fun `so hai lá số — truyền cung vợ chồng và cha mẹ con được nhận ra, không câu trái chiều, có lời nhắc hai chiều`() {
        val rnd = Random(7)
        val pool = (0 until 400).map {
            val t = LocalDateTime.of(1960 + rnd.nextInt(50), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), 0)
            ChartEngine.build(BirthInput(t, null, Gender.entries[rnd.nextInt(2)]))
        }
        val b = pool.first { it.menh == a.palace(PalaceRole.PHU_THE).chi }
        val r = composer.compare(a, b, PairRelation.VO_CHONG, "An", "Bình")
        assertTrue(r.paragraphs.any { "CMP_TRUYEN_CUNG_VO_CHONG" in it.ruleIds }, r.paragraphs.joinToString("\n") { it.text })
        assertEquals(kb.texts.getValue("cmp.note").text, r.paragraphs.last().text)
        val child = pool.first { it.menh == a.palace(PalaceRole.TU_TUC).chi }
        val rc = composer.compare(a, child, PairRelation.CHA_ME_CON, "Cha", "Con")
        assertTrue(rc.paragraphs.any { "CMP_TRUYEN_CUNG_CHA_ME_CON" in it.ruleIds })
        assertTrue(rc.paragraphs.all { it.topic == null || it.topic == Topic.GIA_DAO })
        var tinh = 0
        for (i in 0 until 200) {
            val x = pool[i]; val y = pool[399 - i]
            for (rel in PairRelation.entries) {
                val cmp = composer.compare(x, y, rel, "X", "Y")
                assertTrue(cmp.paragraphs.isNotEmpty())
                assertTrue(cmp.paragraphs.all { it.ruleIds.isNotEmpty() && it.why.isNotEmpty() && '{' !in it.text })
                val oneWay = cmp.paragraphs.filter { it.kind != ParaKind.WEIGH && it.kind != ParaKind.NOTE && it.polarity != 0 }
                assertFalse(oneWay.any { it.polarity > 0 } && oneWay.any { it.polarity < 0 }, cmp.paragraphs.joinToString("\n") { it.text })
                assertEquals(emptyList<String>(), KbLint.forbiddenIn(cmp.paragraphs.joinToString(" ") { it.text }))
                if (cmp.paragraphs.any { it.ruleIds.any { id -> id.startsWith("CMP_TRUYEN_TINH") } }) tinh++
            }
        }
        assertTrue(tinh > 0, "phải gặp truyền tinh trong 400 cặp")
    }

    @Test
    fun `không rõ giờ — giờ thật đứng đầu khi sự kiện lấy từ chính lá số đó`() {
        val input = ReadingTest.GOLDEN.getValue("A")
        val canh = CanhGio.of(input.solar!!.toLocalTime())
        // Lấy các sự kiện mạnh nhất (khả năng cao trước, rồi có thể) trong 2000–2040 của chính lá số giờ thật.
        val found = ArrayList<Triple<Event, Int, Int>>()
        for (y in 2000..2040) {
            val limits = LimitEngine.forYear(a, y)
            for (p in composer.year(a, y).sections.flatMap { it.paragraphs }.filter { it.kind == ParaKind.EVENT }) {
                val ev = Event.valueOf(p.ruleIds.first { it.startsWith("EVENT_") }.removePrefix("EVENT_"))
                val lv = if ("khả năng cao" in p.text) 3 else if ("có thể" in p.text) 2 else 1
                found.add(Triple(ev, limits.lunarYear, lv))
            }
        }
        val events = found.sortedByDescending { it.third }.distinctBy { it.first }.take(3).map { it.first to it.second }
        assertTrue(events.size >= 2, "cần ít nhất 2 sự kiện: $found")
        val charts = ChartEngine.buildAllHours(input)
        val t0 = System.nanoTime()
        val ranks = composer.rankHours(charts, events)
        println("Xếp hạng 12 giờ × ${events.size} sự kiện: %.0f ms · ".format((System.nanoTime() - t0) / 1e6) + ranks.joinToString { "${it.canh}=${it.score}" })
        assertEquals(charts.size, ranks.size)
        val own = ranks.first { it.canh == canh }.score
        val expected = 100.0 * events.sumOf { (ev, y) -> found.filter { it.first == ev && it.second == y }.maxOf { it.third } } / (3 * events.size)
        assertEquals(Math.round(expected).toInt(), own, ranks.joinToString { "${it.canh}=${it.score}" })
        assertEquals(ranks.first().score, own, "giờ thật phải đứng đầu (có thể đồng hạng)")
        assertTrue(ranks.any { it.score < own }, "xếp hạng phải phân biệt được các giờ")
        assertTrue(ranks.zipWithNext().all { (x, y) -> x.score >= y.score })
        assertTrue(ranks.all { it.details.size == events.size })
        assertTrue(composer.rankHours(charts, emptyList()).all { it.score == 0 })
    }

    @Test
    fun `hiệu năng — chấm 30 ngày, chi tiết ngày`() {
        val charts = (0 until 10).map { i -> ChartEngine.build(BirthInput(LocalDateTime.of(1960 + i * 3, 1 + i, 2 + i, i * 2, 0), null, Gender.entries[i % 2])) }
        charts.take(3).forEach { composer.days(it, 2026, 10) }
        val t0 = System.nanoTime(); charts.forEach { composer.days(it, 2026, 11) }
        val days = (System.nanoTime() - t0) / 1e6 / charts.size
        val t1 = System.nanoTime(); charts.forEach { composer.day(it, LocalDate.of(2026, 11, 21)) }
        val day = (System.nanoTime() - t1) / 1e6 / charts.size
        println("Chấm 30 ngày %.1f ms, chi tiết ngày %.1f ms (JVM)".format(days, day))
        assertTrue(days < 300 && day < 120, "30 ngày $days ms, chi tiết ngày $day ms")
    }
}
