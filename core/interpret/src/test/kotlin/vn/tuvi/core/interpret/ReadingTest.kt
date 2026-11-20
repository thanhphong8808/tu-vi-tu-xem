package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import java.io.File
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Cổng giai đoạn 4 (mục 15): snapshot bản luận của 4 ca golden; không có cặp câu trái ngược cùng chủ đề;
 * quét từ cấm sạch. Thêm: mọi đoạn có ruleId và “Vì sao?” (mục 14.4), đồng thanh tương ứng đúng mức, khuyến nghị
 * 3–5 Nên / 3–5 Không nên, câu cuối “Đức năng thắng số”, lá số trẻ nhỏ, hiệu năng.
 */
class ReadingTest {
    private val kb = Fixtures.kb
    private val composer = Composer(kb)

    companion object {
        val GOLDEN = linkedMapOf(
            "A" to BirthInput(LocalDateTime.of(1990, 8, 15, 10, 30), null, Gender.NAM),
            "B" to BirthInput(LocalDateTime.of(1985, 3, 3, 23, 30), null, Gender.NU),
            "C" to BirthInput(LocalDateTime.of(2023, 3, 22, 6, 0), null, Gender.NAM),
            "D" to BirthInput(LocalDateTime.of(1964, 2, 10, 14, 0), null, Gender.NU),
        )

        /** Mọi cặp câu một chiều trái ngược cùng chủ đề trong cùng khối, và khối có hơn một câu cân nhắc. */
        fun contradictions(r: Reading): List<String> {
            val out = ArrayList<String>()
            val blocks = listOf("summary" to r.summary) + (r.sections + r.palaces).map { it.id to it.paragraphs }
            for ((id, ps) in blocks) for ((topic, group) in ps.groupBy { it.topic }) {
                val oneWay = group.filter { it.kind in setOf(ParaKind.LEAD, ParaKind.PLAIN, ParaKind.EVENT) && it.polarity != 0 }
                val pos = oneWay.filter { it.polarity > 0 }
                val neg = oneWay.filter { it.polarity < 0 }
                if (pos.isNotEmpty() && neg.isNotEmpty()) out.add("${r.title} · $id/$topic: «${pos.first().text.take(70)}» ↔ «${neg.first().text.take(70)}»")
                val weigh = group.filter { it.kind == ParaKind.WEIGH }
                if (weigh.size > 1) out.add("${r.title} · $id/$topic: ${weigh.size} câu cân nhắc")
                for (w in weigh) if (w.polarity != 0 && oneWay.any { it.polarity * w.polarity < 0 }) out.add("${r.title} · $id/$topic: câu cân nhắc ngược chiều câu chính")
            }
            return out
        }
    }

    private fun readings(chart: Chart, year: Int): List<Reading> = listOf(
        composer.lifetime(chart, false, year), composer.decade(chart, year), composer.year(chart, year), composer.month(chart, year, 1),
    )

    // ———————————————————————————— snapshot 4 ca golden ————————————————————————————

    @TestFactory
    fun `snapshot bản luận 4 ca golden (trọn đời, đại vận, năm 2026, tháng Giêng 2026)`(): List<DynamicTest> = GOLDEN.map { (id, input) ->
        DynamicTest.dynamicTest("ca $id") {
            val chart = ChartEngine.build(input)
            val text = readings(chart, 2026).joinToString("\n\n") { it.render() }
            val dir = System.getProperty("tuvi.snapshotDir")
            if (System.getProperty("tuvi.updateSnapshots") == "true" && dir != null) {
                File(dir).mkdirs(); File(dir, "$id.txt").writeText(text)
                return@dynamicTest
            }
            val expected = javaClass.getResourceAsStream("/snapshots/$id.txt")?.readBytes()?.toString(Charsets.UTF_8)
                ?: (dir?.let { File(it, "$id.txt").takeIf { f -> f.exists() }?.readText() })
            assertTrue(expected != null, "chưa có snapshot $id — chạy ./gradlew :core:interpret:test -PupdateSnapshots")
            if (expected != text) {
                val e = expected!!.lines()
                val a = text.lines()
                val i = (0 until minOf(e.size, a.size)).firstOrNull { e[it] != a[it] } ?: minOf(e.size, a.size)
                throw AssertionError("snapshot $id khác ở dòng ${i + 1}:\n  cũ: ${e.getOrNull(i)}\n  mới: ${a.getOrNull(i)}\n(chạy -PupdateSnapshots nếu thay đổi là chủ ý)")
            }
        }
    }

    @Test
    fun `ca golden — bất biến chung`() {
        for ((id, input) in GOLDEN) {
            val chart = ChartEngine.build(input)
            for (r in readings(chart, 2026)) {
                assertEquals(emptyList<String>(), contradictions(r), "ca $id")
                assertEquals(emptyList<String>(), KbLint.forbiddenIn(r.fullText()), "ca $id ${r.title}")
                assertEquals(kb.texts.getValue("note.duc_nang").text, r.closing)
                for ((block, p) in r.allParagraphs()) {
                    assertTrue(p.ruleIds.isNotEmpty(), "ca $id $block: đoạn không có ruleId: ${p.text}")
                    assertTrue(p.why.isNotEmpty(), "ca $id $block: đoạn không mở được “Vì sao?”: ${p.text}")
                    assertFalse(p.text.contains('{') || p.text.contains('}'), "ô chưa điền: ${p.text}")
                }
            }
        }
    }

    @Test
    fun `ca A năm 2026 — hạn hỷ đủ ba tầng thành “khả năng cao”, khuyến nghị đủ 3–5`() {
        val chart = ChartEngine.build(GOLDEN.getValue("A"))
        val y = composer.year(chart, 2026)
        val td = y.sections.first { it.topic == Topic.TINH_DUYEN }
        assertTrue(td.paragraphs.any { it.kind == ParaKind.EVENT && "từ ba tầng vận hạn trở lên" in it.text && "khả năng cao" in it.text }, td.paragraphs.joinToString("\n") { it.text })
        assertEquals(12, y.months.size)
        assertEquals(2, y.goodMonths.size); assertEquals(2, y.cautionMonths.size)
        assertTrue(y.goodMonths.none { it in y.cautionMonths })
        val nen = y.advice.count { it.type == AdviceType.NEN }
        val khong = y.advice.count { it.type == AdviceType.KHONG_NEN }
        assertTrue(nen in 3..5 && khong in 3..5, "NÊN $nen, KHÔNG NÊN $khong")
        assertEquals(Stance.of(y.score), y.stance)
    }

    @Test
    fun `ca C (4 tuổi âm) — không luận tình duyên, cưới hỏi, sự nghiệp cho trẻ nhỏ`() {
        val chart = ChartEngine.build(GOLDEN.getValue("C"))
        val y = composer.year(chart, 2026)
        assertTrue(y.sections.none { it.topic == Topic.TINH_DUYEN || it.topic == Topic.TAI_CHINH || it.topic == Topic.PHAP_LY })
        assertTrue(y.sections.any { it.title == "Học tập, phát triển" })
        val text = y.fullText().lowercase()
        for (w in listOf("cưới", "hôn nhân", "thăng tiến", "đổi việc", "đầu tư", "vay")) assertFalse(w in text, "trẻ nhỏ không được có “$w”")
        assertTrue(y.advice.count { it.type == AdviceType.NEN } >= 3 && y.advice.count { it.type == AdviceType.KHONG_NEN } >= 3)
        assertTrue(y.summary.any { it.text == kb.texts.getValue("note.tre_nho").text })
    }

    // ———————————————————————————— bất biến trên lá số ngẫu nhiên ————————————————————————————

    @Test
    fun `300 lá số ngẫu nhiên — không câu trái ngược, không từ cấm, đủ căn cứ, đồng thanh đúng mức`() {
        val rnd = Random(4)
        var paragraphs = 0
        var weigh = 0
        val levelSeen = HashSet<Likelihood>()
        repeat(300) { i ->
            val t = LocalDateTime.of(1940 + rnd.nextInt(80), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), rnd.nextInt(60))
            val chart = ChartEngine.build(BirthInput(t, null, if (rnd.nextBoolean()) Gender.NAM else Gender.NU))
            val year = t.year + 16 + rnd.nextInt(60)
            val rs = if (i % 3 == 0) readings(chart, year) else listOf(composer.year(chart, year), composer.decade(chart, year))
            for (r in rs) {
                assertEquals(emptyList<String>(), contradictions(r))
                assertEquals(emptyList<String>(), KbLint.forbiddenIn(r.fullText()), r.title)
                for (w in listOf("tự hại", "tự tử", "tự sát")) assertFalse(w in r.fullText().lowercase())
                for ((_, p) in r.allParagraphs()) {
                    paragraphs++
                    if (p.kind == ParaKind.WEIGH) weigh++
                    assertTrue(p.ruleIds.isNotEmpty() && p.why.isNotEmpty(), p.text)
                    if (p.kind == ParaKind.EVENT) {
                        // Đồng thanh tương ứng: mức trong câu phải khớp số tầng của các luật gắn sự kiện (đếm độc lập lại từ KB).
                        val ev = Event.valueOf(p.ruleIds.first { it.startsWith("EVENT_") }.removePrefix("EVENT_"))
                        val layers = p.ruleIds.mapNotNull { kb.rule(it) }.filter { it.effect.event == ev }.flatMap { it.layers() }.toSet() - Layer.GOC
                        val level = Likelihood.of(layers.size)!!
                        levelSeen.add(level)
                        assertEquals(kb.texts.getValue("ev.${ev.name.lowercase()}.${level.key}").text.substringAfter("{nam}, "), p.text.substringAfter("), "), "${r.title}: ${p.text}")
                    }
                }
                if (r.scope == ReadingScope.NAM || r.scope == ReadingScope.DAI_VAN) {
                    val nen = r.advice.count { it.type == AdviceType.NEN }
                    val khong = r.advice.count { it.type == AdviceType.KHONG_NEN }
                    assertTrue(nen in 3..5 && khong in 3..5, "${r.title}: NÊN $nen, KHÔNG NÊN $khong")
                    assertTrue(r.advice.all { it.because.isNotBlank() && it.ruleIds.isNotEmpty() })
                    assertEquals(r.advice.size, r.advice.map { it.key }.toSet().size, "khuyến nghị trùng")
                }
                assertTrue(r.score in 5..95)
            }
        }
        println("Bản luận: $paragraphs đoạn, $weigh câu cân nhắc; mức đồng thanh gặp: $levelSeen")
        assertTrue(weigh > 0 && levelSeen.size == 3, "phải gặp đủ ba mức đồng thanh: $levelSeen")
    }

    // ———————————————————————————— chấm điểm ————————————————————————————

    @Test
    fun `chấm điểm — công thức, Tuần Triệt theo tuổi, trọng số tầng, thế giai đoạn`() {
        assertEquals(50, Scorer.toScore(0.0, 1.0))
        assertEquals(95, Scorer.toScore(100.0, 1.0)); assertEquals(5, Scorer.toScore(-100.0, 1.0))
        assertEquals(0.5, Scorer.tuanTrietFactor(false, true, 20, false), 1e-9)   // Triệt trước 30: mạnh 1,0
        assertEquals(0.75, Scorer.tuanTrietFactor(false, true, 40, false), 1e-9)  // Triệt sau 30: mạnh 0,5
        assertEquals(0.75, Scorer.tuanTrietFactor(true, false, 20, false), 1e-9)  // Tuần trước 30: mạnh 0,5
        assertEquals(0.5, Scorer.tuanTrietFactor(true, false, 40, false), 1e-9)   // Tuần sau 30: mạnh 1,0
        assertEquals(0.5, Scorer.tuanTrietFactor(true, false, 20, true), 1e-9)    // xung chiếu: chặn phần chiếu ×0,5
        assertEquals(1.0, Scorer.YEAR_WEIGHTS.values.sum(), 1e-9)
        assertEquals(1.0, Scorer.DECADE_WEIGHTS.values.sum(), 1e-9)
        assertEquals(Stance.TAN, Stance.of(70)); assertEquals(Stance.GIU, Stance.of(69)); assertEquals(Stance.GIU, Stance.of(50))
        assertEquals(Stance.THU, Stance.of(49)); assertEquals(Stance.THU, Stance.of(35)); assertEquals(Stance.LUI, Stance.of(34))
        assertEquals(Likelihood.CAO, Likelihood.of(3)); assertEquals(Likelihood.CO_THE, Likelihood.of(2)); assertEquals(Likelihood.DAU_HIEU, Likelihood.of(1))
        assertEquals(setOf(Layer.TIEU_HAN, Layer.LUU_THAI_TUE, Layer.LUU_NIEN), kb.rule("CD_HAN_HY_CUOI_HOI")!!.layers())
        assertEquals(setOf(Layer.LUU_THAI_TUE), kb.rule("VH_TAI_HAO_LUU_SAT")!!.layers())
        assertEquals(setOf(Layer.GOC), kb.rule("CC_LOC_MA_GIAO_TRI")!!.layers())

        // Trước đại hạn đầu: bỏ tầng đại hạn và lưu niên đại hạn, chia lại trọng số cho đủ 1.
        val c = ChartEngine.build(GOLDEN.getValue("C"))
        val l = LimitEngine.forYear(c, 2024)
        assertTrue(l.beforeFirstDaiHan)
        val ts = Scorer(kb).topicLayered(c, RuleEngine(kb).evaluate(c, l), Topic.SUC_KHOE, l, Scorer.YEAR_WEIGHTS)
        assertEquals(setOf(Layer.GOC, Layer.TIEU_HAN, Layer.LUU_THAI_TUE), ts.layers.map { it.layer }.toSet())
        assertEquals(1.0, ts.layers.sumOf { it.weight }, 1e-9)
    }

    @Test
    fun `chấm điểm — điểm cung trải quanh 50 trên 1000 lá số`() {
        val rnd = Random(9)
        val engine = RuleEngine(kb)
        val scores = ArrayList<Int>()
        repeat(1000) {
            val c = ChartEngine.build(BirthInput(LocalDateTime.of(1930 + rnd.nextInt(90), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), 0), null, Gender.entries[it % 2]))
            val sc = Scorer(kb)
            val e = engine.evaluate(c)
            for (chi in vn.tuvi.core.calendar.Chi.entries) scores.add(sc.palace(c, e, chi).score)
        }
        val mean = scores.average()
        val sd = Math.sqrt(scores.map { (it - mean) * (it - mean) }.average())
        println("Điểm cung: trung bình %.1f, độ lệch chuẩn %.1f".format(mean, sd))
        assertTrue(mean in 45.0..55.0 && sd in 9.0..18.0, "mean $mean sd $sd")
    }

    @Test
    fun `hiệu năng — luận một năm, trọn đời`() {
        val charts = (0 until 30).map { i -> ChartEngine.build(BirthInput(LocalDateTime.of(1950 + i, 1 + i % 12, 1 + i % 28, i % 24, 0), null, Gender.entries[i % 2])) }
        charts.take(5).forEach { composer.year(it, 2026); composer.lifetime(it) }
        val t0 = System.nanoTime(); charts.forEach { composer.year(it, 2026) }
        val year = (System.nanoTime() - t0) / 1e6 / charts.size
        val t1 = System.nanoTime(); charts.forEach { composer.lifetime(it) }
        val life = (System.nanoTime() - t1) / 1e6 / charts.size
        println("Luận năm %.1f ms, trọn đời %.1f ms (JVM)".format(year, life))
        assertTrue(year < 50 && life < 200, "năm $year ms, trọn đời $life ms")
    }
}
