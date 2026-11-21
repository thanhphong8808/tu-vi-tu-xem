package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Giai đoạn 6 — Hỏi đáp (mục 12, 13): bộ câu hỏi mẫu (có dấu và không dấu) với ý định, chủ đề, thời gian mong đợi;
 * bất biến của câu trả lời (ruleIds + “Vì sao?”, không từ cấm, không câu chính ngược chiều, khủng hoảng không đi qua
 * phần luận, từ chối không chấm điểm, giới hạn lá số trẻ em), gợi ý và câu hỏi tiếp đều hiểu được, hiệu năng.
 */
class QaTest {
    private val kb = Fixtures.kb
    private val qa = QaEngine(kb)
    private val a: Chart = ChartEngine.build(ReadingTest.GOLDEN.getValue("A"))
    private val ctx = QaContext(LocalDate.of(2026, 11, 21))

    private data class Case(val q: String, val intent: QaIntent, val o: JsonObject)

    private val cases: List<Case> by lazy {
        val text = javaClass.getResourceAsStream("/qa-questions.json")!!.readBytes().toString(Charsets.UTF_8)
        Json.parseToJsonElement(text).jsonArray.map { e ->
            val o = e.jsonObject
            Case(o.getValue("q").jsonPrimitive.content, QaIntent.valueOf(o.getValue("intent").jsonPrimitive.content), o)
        }
    }

    private fun str(o: JsonObject, k: String): String? = o[k]?.let { if (it is JsonNull) null else it.jsonPrimitive.content }

    private fun checkParse(c: Case, p: QaParse, label: String) {
        val o = c.o
        assertEquals(c.intent, p.intent, "$label: ý định")
        str(o, "topic")?.let { assertEquals(it, p.topics.firstOrNull()?.name, "$label: chủ đề ${p.topics}") }
        if (o.containsKey("event")) assertEquals(str(o, "event"), p.event?.name, "$label: sự kiện")
        str(o, "scope")?.let { s ->
            if (s == "none") assertNull(p.time, "$label: không có thời gian") else assertEquals(s, p.time?.scope?.name, "$label: phạm vi ${p.time}")
        }
        o["year"]?.let { assertEquals(it.jsonPrimitive.int, p.time?.lunarYear, "$label: năm ${p.time}") }
        o["month"]?.let { assertEquals(it.jsonPrimitive.int, p.time?.month, "$label: tháng ${p.time}") }
        str(o, "date")?.let { assertEquals(LocalDate.parse(it), p.time?.date, "$label: ngày ${p.time}") }
        o["days"]?.let { assertEquals(it.jsonPrimitive.int, p.time?.days, "$label: số ngày ${p.time}") }
        str(o, "unit")?.let { assertEquals(it, p.timingUnit?.name, "$label: đơn vị dò") }
        str(o, "refuse")?.let { assertEquals(it, p.refuse, "$label: từ chối") }
        str(o, "reframe")?.let { assertEquals(it, p.reframe, "$label: chuyển hướng") }
        str(o, "fact")?.let { assertEquals(it, p.fact, "$label: dữ kiện") }
        str(o, "star")?.let { assertEquals(it, p.star?.name, "$label: sao") }
        str(o, "role")?.let { assertEquals(it, p.role?.name, "$label: cung") }
        o["terms"]?.let { t -> for (x in t.jsonArray) assertTrue(x.jsonPrimitive.content in p.terms, "$label: thuật ngữ ${p.terms}") }
        o["caution"]?.let { assertEquals(it.jsonPrimitive.boolean, p.caution, "$label: hướng thận trọng") }
        if (o.containsKey("relative")) assertEquals(str(o, "relative"), p.relative, "$label: người thân")
    }

    /** Bất biến của mọi câu trả lời. */
    private fun checkAnswer(ans: QaAnswer, label: String) {
        assertTrue(ans.understood.isNotBlank(), "$label: thiếu dòng “app hiểu là”")
        assertTrue(ans.paragraphs.isNotEmpty(), "$label: câu trả lời rỗng")
        assertTrue(ans.paragraphs.size <= 12, "$label: quá dài (${ans.paragraphs.size} đoạn)")
        for (p in ans.paragraphs) {
            assertTrue(p.ruleIds.isNotEmpty() && p.why.isNotEmpty(), "$label: đoạn thiếu căn cứ: ${p.text}")
            assertFalse(p.text.contains('{') || p.text.contains('}'), "$label: ô chưa điền: ${p.text}")
        }
        for (ad in ans.advice) assertTrue(ad.ruleIds.isNotEmpty() || ad.because.isNotBlank(), "$label: khuyến nghị thiếu căn cứ")
        assertEquals(emptyList<String>(), KbLint.forbiddenIn(ans.fullText()), "$label: từ cấm")
        assertEquals(ans.paragraphs.size, ans.paragraphs.map { it.text }.toSet().size, "$label: đoạn trùng")
        // Không có hai câu chính ngược chiều cho cùng chủ đề trong cùng một tầng (gốc / năm / tháng…).
        val leads = ans.paragraphs.filter { it.kind == ParaKind.LEAD && it.polarity != 0 }
        for ((key, g) in leads.groupBy { it.topic to it.text.substringBefore(" — ", "") }) {
            assertFalse(g.any { it.polarity > 0 } && g.any { it.polarity < 0 }, "$label: câu chính ngược chiều $key")
        }
        when (ans.parse.intent) {
            QaIntent.CRISIS -> {
                assertEquals(listOf("QA_KHUNG_HOANG"), ans.paragraphs.flatMap { it.ruleIds }, "$label: khủng hoảng chỉ có lời hỗ trợ")
                assertNull(ans.score); assertTrue(ans.advice.isEmpty() && ans.followUps.isEmpty() && ans.link == null)
                val t = ans.paragraphs.single().text
                assertTrue("115" in t && "096 306 1414" in t && "111" in t, "$label: thiếu số điện thoại hỗ trợ")
            }
            QaIntent.REFUSE -> { assertNull(ans.score, label); assertTrue(ans.advice.isEmpty(), label) }
            QaIntent.FORECAST, QaIntent.ADVICE -> assertTrue(ans.score != null && ans.score in 5..95 && ans.link != null, "$label: thiếu điểm hoặc liên kết bản luận")
            QaIntent.TIMING -> assertTrue(ans.link != null || ans.parse.timingUnit == ReadingScope.NGAY || ans.paragraphs.any { "QA_NOTE_TRE_NHO" in it.ruleIds }, "$label: thiếu liên kết")
            else -> {}
        }
    }

    @TestFactory
    fun `bộ câu hỏi mẫu — ý định, chủ đề, thời gian (có dấu và không dấu)`(): List<DynamicTest> {
        assertTrue(cases.size >= 100, "cần ≥ 100 câu hỏi mẫu, có ${cases.size}")
        return cases.flatMap { c ->
            val base = DynamicTest.dynamicTest(c.q) {
                checkParse(c, qa.parse(c.q, a, ctx), c.q)
                checkAnswer(qa.answer(a, c.q, ctx), c.q)
            }
            val plain = QaEngine.normalize(c.q)
            val accent = c.o["accent"]?.jsonPrimitive?.boolean == true
            if (accent || plain == c.q.lowercase()) listOf(base) else listOf(base, DynamicTest.dynamicTest("[không dấu] $plain") {
                checkParse(c, qa.parse(plain, a, ctx), plain)
            })
        }
    }

    @Test
    fun `câu hỏi tiếp và gợi ý đều hiểu được, trả lời không lỗi`() {
        val follow = LinkedHashSet<String>()
        for (c in cases) follow += qa.answer(a, c.q, ctx).followUps
        follow += qa.suggestions(a, ctx)
        assertTrue(follow.size >= 30, "ít câu hỏi tiếp: ${follow.size}")
        for (f in follow) {
            val ans = qa.answer(a, f, ctx)
            assertNotEquals(QaIntent.UNKNOWN, ans.parse.intent, "không hiểu câu gợi ý: $f")
            assertNotEquals(QaIntent.REFUSE, ans.parse.intent, "gợi ý bị từ chối: $f")
            checkAnswer(ans, "[tiếp] $f")
        }
    }

    @Test
    fun `mọi khóa qa trong mã đều có trong knowledge base`() {
        val src = listOf("src/main/kotlin", "core/interpret/src/main/kotlin", System.getProperty("tuvi.src") ?: "")
            .map { File(it, "vn/tuvi/core/interpret/Qa.kt") }.firstOrNull { it.isFile }
        assertTrue(src != null, "không tìm thấy Qa.kt (chạy test từ thư mục module)")
        val keys = Regex("\"((?:qa|note)\\.[a-z_.]*[a-z_])\"").findAll(src!!.readText()).map { it.groupValues[1] }.toSet()
        assertTrue(keys.size >= 25, "ít khóa: $keys")
        val missing = keys.filter { it !in kb.texts }
        assertEquals(emptyList<String>(), missing, "khóa qa.* chưa có trong KB")
        // Khóa giải nghĩa dùng qua tên thuật ngữ.
        for (t in listOf("tu_vi", "tuan", "triet", "hoa_ky", "dong_thanh", "vo_chinh_dieu", "dai_han", "the", "diem", "menh", "than", "cuc", "han"))
            assertTrue("qa.def.$t" in kb.texts, "thiếu qa.def.$t")
        for (t in kb.texts.values.filter { it.key.startsWith("qa.") }) assertTrue(t.source.isNotBlank() && t.text.isNotBlank(), t.key)
    }

    @Test
    fun `khủng hoảng luôn ưu tiên, kể cả khi câu có chủ đề hoặc thời gian`() {
        for (q in listOf("Năm nay công việc tệ quá tôi muốn chết", "tu tu co phai la cach giai thoat khong", "Tôi muốn tự tử vì nợ nần năm nay",
            "khong con ly do de song", "Tôi chán sống rồi")) {
            val ans = qa.answer(a, q, ctx)
            assertEquals(QaIntent.CRISIS, ans.parse.intent, q)
            checkAnswer(ans, q)
        }
        // Không nhầm sang khủng hoảng.
        for (q in listOf("Sao Tử Vi ở cung nào", "Năm nay sống ở đâu thì thuận", "Tử Tức là gì")) assertNotEquals(QaIntent.CRISIS, qa.parse(q, a, ctx).intent, q)
    }

    @Test
    fun `lá số dưới 16 tuổi chỉ trả lời học tập, sức khỏe, gia đình, đi lại`() {
        val child = ChartEngine.build(BirthInput(LocalDateTime.of(2018, 3, 10, 8, 0), null, Gender.NU))
        assertTrue(child.tuoiAm(ctx.lunarYear) < 16)
        val love = qa.answer(child, "Năm nay tình duyên thế nào?", ctx)
        assertTrue(love.paragraphs.any { "QA_NOTE_TRE_NHO" in it.ruleIds }, "thiếu ghi chú trẻ nhỏ")
        assertTrue(love.paragraphs.none { it.topic == Topic.TINH_DUYEN && "QA_NOTE_TRE_NHO" !in it.ruleIds }, "không luận tình duyên cho trẻ")
        val wed = qa.answer(child, "Khi nào lấy chồng?", ctx)
        assertEquals(listOf("QA_NOTE_TRE_NHO"), wed.paragraphs.flatMap { it.ruleIds })
        val money = qa.answer(child, "Năm nào phát tài?", ctx)
        assertEquals(listOf("QA_NOTE_TRE_NHO"), money.paragraphs.flatMap { it.ruleIds })
        val study = qa.answer(child, "Năm nay học hành thế nào?", ctx)
        assertTrue(study.paragraphs.any { it.topic == Topic.SU_NGHIEP }, "phải luận học tập")
        for (s in qa.suggestions(child, ctx)) {
            val ans = qa.answer(child, s, ctx)
            assertNotEquals(QaIntent.UNKNOWN, ans.parse.intent, s)
            checkAnswer(ans, "[bé] $s")
            assertTrue(ans.paragraphs.none { it.topic in setOf(Topic.TINH_DUYEN, Topic.TAI_CHINH, Topic.PHAP_LY) }, "[bé] $s")
        }
    }

    @Test
    fun `dò năm — mức khả năng theo số tầng, câu hỏi thận trọng xếp từ thấp lên`() {
        val wed = qa.answer(a, "Năm nào thuận cho chuyện cưới hỏi?", ctx)
        assertTrue(wed.paragraphs.any { "QA_NOTE_KHA_NANG" in it.ruleIds })
        val ev = wed.paragraphs.filter { it.kind == ParaKind.EVENT }
        assertTrue(ev.isNotEmpty())
        for (p in ev) assertFalse(p.text.contains("Tứ Hóa lưu") || p.text.contains("sao lưu,"), "nhãn tầng phải gọn: ${p.text}")
        val careful = qa.answer(a, "Năm nào nên chú ý sức khỏe?", ctx)
        val scores = careful.paragraphs.filter { it.ruleIds.any { id -> id.startsWith("QA_YEAR_") } }
            .map { Regex("điểm sức khỏe (\\d+)").find(it.text)!!.groupValues[1].toInt() }
        assertTrue(scores.size >= 2 && scores == scores.sorted(), "phải xếp từ thấp lên: $scores")
        assertTrue(careful.understood.startsWith("Những năm nên thận trọng"))
        val good = qa.answer(a, "Năm nào sức khỏe tốt nhất?", ctx)
        val gs = good.paragraphs.filter { it.ruleIds.any { id -> id.startsWith("QA_YEAR_") } }
            .map { Regex("điểm sức khỏe (\\d+)").find(it.text)!!.groupValues[1].toInt() }
        assertTrue(gs == gs.sortedDescending(), "phải xếp từ cao xuống: $gs")
        val house = qa.answer(a, "Khi nào tôi mua được nhà?", ctx)
        val hy = house.paragraphs.filter { it.ruleIds.any { id -> id.startsWith("QA_YEAR_") } }
        assertTrue(hy.size == 3 && hy.all { "điểm nhà đất" in it.text && "Căn cứ:" in it.text }, "nhà đất dò theo điểm Điền Trạch, có căn cứ từng năm")
    }

    @Test
    fun `câu trả lời bám đúng năm được hỏi, câu hỏi tiếp cùng năm`() {
        val ans = qa.answer(a, "Lời khuyên về tài chính năm 2028", ctx)
        assertEquals(2028, ans.link?.lunarYear)
        assertTrue(ans.followUps.any { "2028" in it } && ans.followUps.none { "2026" in it }, "${ans.followUps}")
        val y = qa.answer(a, "Năm 2029 công việc thế nào?", ctx)
        assertEquals(ReadingScope.NAM, y.link?.scope); assertEquals(2029, y.link?.lunarYear)
        assertEquals(Composer(kb).year(a, 2029).sections.first { it.topic == Topic.SU_NGHIEP }.score, y.score, "điểm hỏi đáp = điểm sự nghiệp trong bản luận năm")
        val life = qa.answer(a, "Tính cách của tôi thế nào?", ctx)
        assertEquals(ReadingScope.TRON_DOI, life.link?.scope)
        assertTrue(life.paragraphs.all { it.topic == null || it.topic == Topic.TINH_CACH }, "chỉ luận tính cách")
    }

    @Test
    fun `chịu được đầu vào lạ, nhiều lá số, đủ nhanh`() {
        val rnd = Random(6)
        val weird = listOf("", "   ", "???", "😀😀", "a".repeat(3000), "năm năm năm năm", "1990", "15/08/1990", "tháng 13", "ngày 32 tháng 2",
            "năm 1800 thế nào", "năm 2300 thế nào", "mùng 30 tháng chạp", "thứ 8 thế nào", "SAO TỬ VI Ở CUNG NÀO", "tuoi 200 the nao") +
            List(40) { String(CharArray(rnd.nextInt(1, 60)) { "aăâbcdđeêghiklmnoôơpqrstuưvxyáàảãạ 0123456789/?".random(rnd) }) }
        for (q in weird) checkWeird(qa.answer(a, q, ctx), q)
        var maxMs = 0L
        val t0 = System.nanoTime()
        for ((id, input) in ReadingTest.GOLDEN) {
            val ch = ChartEngine.build(input)
            for (c in cases) {
                val s = System.nanoTime()
                val ans = qa.answer(ch, c.q, ctx)
                maxMs = maxOf(maxMs, (System.nanoTime() - s) / 1_000_000)
                checkAnswer(ans, "ca $id · ${c.q}")
            }
        }
        val total = (System.nanoTime() - t0) / 1_000_000
        assertTrue(maxMs < 3000, "một câu chậm: $maxMs ms")
        assertTrue(total < 60_000, "tổng chậm: $total ms")
        // Lần hai dùng bộ nhớ tạm: nhanh.
        val t1 = System.nanoTime()
        for (c in cases) qa.answer(a, c.q, ctx)
        val warm = (System.nanoTime() - t1) / 1_000_000 / cases.size
        assertTrue(warm < 150, "trung bình mỗi câu (đã có bộ nhớ tạm) $warm ms")
    }

    @Test
    fun `bộ lọc ẩn danh — bỏ ngày giờ sinh, tên, số điện thoại, email, số nhà`() {
        val cases = mapOf(
            "Tôi tên Nguyễn Văn An, sinh ngày 15/08/1990 lúc 10:30, năm sau công việc thế nào?" to listOf("Nguyễn", "An,", "15/08", "1990", "10:30"),
            "em sinh năm 1995 gọi em số 0912 345 678 nhé" to listOf("1995", "0912", "345", "678"),
            "mail tôi là lan.tran@gmail.com, tháng sau thế nào" to listOf("lan.tran", "gmail"),
            "nhà số 12 ngõ 34 có nên bán không" to listOf("12", "34"),
            "tên con là Bảo Anh năm nay học hành ra sao" to listOf("Bảo", "Anh"),
            "15/12 có nên ký hợp đồng không" to listOf("15/12"),
            "Tôi sinh 1990 10h30 thì năm 2027 ra sao" to listOf("1990", "10h30"),
        )
        for ((q, gone) in cases) {
            val a = QaPrivacy.anonymize(q)
            for (g in gone) assertFalse(a.contains(g), "“$q” → “$a” còn “$g”")
            assertTrue(QaPrivacy.isSafe(a), "“$a” phải an toàn sau khi lọc")
        }
        // Giữ nguyên phần hỏi: năm được hỏi, chủ đề.
        assertEquals("năm sau công việc thế nào?", QaPrivacy.anonymize("Tôi tên Nguyễn Văn An, sinh ngày 15/08/1990 lúc 10:30, năm sau công việc thế nào?").substringAfterLast(", "))
        assertTrue(QaPrivacy.anonymize("Tôi sinh 1990 10h30 thì năm 2027 ra sao").contains("năm 2027"))
        assertEquals("Tôi tên gì nhỉ", QaPrivacy.anonymize("Tôi tên gì nhỉ"))
        assertEquals("Năm 2027 tài chính thế nào?", QaPrivacy.anonymize("Năm 2027 tài chính thế nào?"))
        assertFalse(QaPrivacy.isSafe("gọi 0912345678"))
        // Câu ẩn danh vẫn hiểu được như câu gốc.
        val q = "Tôi tên Nguyễn Văn An, sinh ngày 15/08/1990 lúc 10:30, năm sau công việc thế nào?"
        val p0 = qa.parse(q, a, ctx); val p1 = qa.parse(QaPrivacy.anonymize(q), a, ctx)
        assertEquals(p0.intent, p1.intent); assertEquals(p0.topics.firstOrNull(), p1.topics.firstOrNull()); assertEquals(2027, p1.time?.lunarYear)
    }

    // ———————————————— luận đoán theo tầng (D-54) ————————————————

    private fun para(ans: QaAnswer, titlePart: String) = ans.paragraphs.firstOrNull { it.title?.contains(titlePart) == true }

    @Test
    fun `luận đoán năm đi qua đủ các tầng và khớp dữ kiện lá số`() {
        val y = 2027
        val ans = qa.answer(a, "Năm 2027 tài chính của tôi thế nào?", ctx)
        val l = vn.tuvi.core.engine.LimitEngine.forYear(a, y)
        val role = vn.tuvi.core.engine.PalaceRole.TAI_BACH
        for (t in listOf("Kết luận", "Lá số gốc", "Đại hạn", "Lưu niên đại hạn", "Tiểu hạn", "Lưu Thái Tuế", "Tổng hợp"))
            assertTrue(para(ans, t) != null, "thiếu tầng “$t”: ${ans.paragraphs.map { it.title }}")
        val natal = a.palace(role)
        val g = para(ans, "Lá số gốc")!!.text
        assertTrue(g.startsWith("Cung Tài Bạch an tại ${natal.chi.vn}"), g)
        for (s0 in natal.chinhTinh) assertTrue(s0.id.vn in g, "gốc phải nêu ${s0.id.vn}: $g")
        val dh = para(ans, "Đại hạn")!!.text
        assertTrue("tại ${l.daiHan.vn}," in dh && "rơi vào ${(l.daiHan + role.ordinal).vn} " in dh, dh)
        val th = para(ans, "Tiểu hạn")!!.text
        assertTrue("tại ${l.tieuHan.vn} " in th && "rơi vào ${(l.tieuHan + role.ordinal).vn} " in th, th)
        val ln = para(ans, "Lưu niên đại hạn")!!.text
        assertTrue("tại ${l.luuNienDaiHan.vn} " in ln, ln)
        val tt = para(ans, "Lưu Thái Tuế")!!.text
        assertTrue("Thái Tuế đóng tại ${l.luuThaiTue.vn} " in tt, tt)
        for ((h, star) in vn.tuvi.core.engine.TuHoaTable.of(l.yearCanChi.can, a.config)) assertTrue("${h.vn} → ${star.vn} tại" in tt, "Tứ Hóa năm: $tt")
        // Điểm kết luận = điểm chủ đề của engine; dòng tổng hợp cộng đúng.
        val ts = Composer(kb).yearData(a, y).topics.getValue(Topic.TAI_CHINH)
        assertEquals(ts.score, ans.score)
        assertTrue(ans.paragraphs.first().text.contains("${ts.score}/100"))
        val sum = ts.layers.sumOf { it.score.score * it.weight }
        assertTrue(kotlin.math.abs(sum - ts.score) <= 1.0, "tổng hợp: $sum vs ${ts.score}")
        for (ls in ts.layers) assertTrue("${ls.score.score}×" in para(ans, "Tổng hợp")!!.text)
    }

    @Test
    fun `câu trả lời dựng từ lá số, không soạn sẵn — khác lá số, khác năm thì khác lời`() {
        val b = ChartEngine.build(ReadingTest.GOLDEN.getValue("B"))
        val q = "Năm sau công việc của tôi thế nào?"
        val ta = qa.answer(a, q, ctx).paragraphs.map { it.text }
        val tb = qa.answer(b, q, ctx).paragraphs.map { it.text }
        assertTrue(ta.zip(tb).count { (x, y) -> x != y } >= ta.size - 1, "hai lá số phải cho lời luận khác nhau")
        // Cùng đại vận (2024–2033) thì lá số gốc và đại hạn giống nhau; các tầng của năm phải khác.
        fun yearly(q: String) = qa.answer(a, q, ctx).paragraphs.filter { p -> listOf("Lá số gốc", "Đại hạn", "Sách bàn").none { p.title?.contains(it) == true } }.map { it.text }
        val t27 = yearly("Năm 2027 công việc thế nào?")
        val t30 = yearly("Năm 2030 công việc thế nào?")
        assertTrue(t27.intersect(t30.toSet()).isEmpty(), "hai năm phải khác phần vận hạn: ${t27.intersect(t30.toSet())}")
        // Câu hỏi có sự kiện: trả lời thẳng khả năng.
        val wed = qa.answer(a, "Năm nay tôi có cưới được không?", ctx)
        assertTrue(wed.paragraphs.first().text.contains("Riêng chuyện tình cảm, cưới hỏi"), wed.paragraphs.first().text)
    }

    @Test
    fun `luận đoán chạy trên mọi lá số, mọi chủ đề, nhiều năm`() {
        val topicsQ = listOf("công việc", "tài chính", "tình duyên", "sức khỏe", "gia đạo", "đi xa", "nhà đất", "kiện tụng", "tính cách", "phúc đức")
        for ((id, input) in ReadingTest.GOLDEN) {
            val ch = ChartEngine.build(input)
            for (y in listOf(2026, 2028, 2031)) for (t in topicsQ) {
                val q = "Năm $y $t thế nào?"
                val ans = qa.answer(ch, q, ctx)
                checkAnswer(ans, "ca $id · $q")
            }
            for (q in listOf("Tháng 3 âm năm 2027 tài chính thế nào?", "10 năm tới tình duyên ra sao", "Cả đời tôi tiền bạc thế nào?", "Khi nào tôi giàu?", "Năm nào nên chú ý sức khỏe?"))
                checkAnswer(qa.answer(ch, q, ctx), "ca $id · $q")
        }
        // Câu hỏi có sự kiện (cả sự kiện xấu) qua nhiều năm: mọi nhánh lời kết luận đều sạch từ cấm.
        for (id in listOf("A", "B")) {
            val ch = ChartEngine.build(ReadingTest.GOLDEN.getValue(id))
            for (y in 2026..2036) for (q in listOf("Năm $y có hao tài không", "Năm $y có kiện tụng gì không", "Năm $y có cưới được không", "Năm $y có thăng chức không", "Năm $y có đi xa không"))
                checkAnswer(qa.answer(ch, q, ctx), "ca $id · $q")
        }
        val m = qa.answer(a, "Tháng 3 âm năm 2027 tài chính thế nào?", ctx)
        val nh = vn.tuvi.core.engine.LimitEngine.forMonth(a, 2027, 3).nguyetHan!!
        assertTrue(para(m, "Nguyệt hạn")!!.text.startsWith("Nguyệt hạn tại ${nh.vn} "), para(m, "Nguyệt hạn")!!.text)
        val life = qa.answer(a, "Cả đời tôi tiền bạc thế nào?", ctx)
        assertTrue(para(life, "Dòng đại vận") != null && para(life, "Lá số gốc") != null)
        val dec = qa.answer(a, "10 năm tới tình duyên ra sao", ctx)
        assertTrue(para(dec, "Đại hạn") != null && dec.paragraphs.first().text.contains("năm thuận nhất là"))
        val wed = qa.answer(a, "Khi nào tôi lấy vợ?", ctx)
        assertTrue(wed.paragraphs.filter { it.kind == ParaKind.EVENT }.all { "Căn cứ:" in it.text })
    }

    private fun checkWeird(ans: QaAnswer, q: String) {
        assertTrue(ans.understood.isNotBlank(), q)
        assertTrue(ans.paragraphs.isNotEmpty(), q)
        assertEquals(emptyList<String>(), KbLint.forbiddenIn(ans.fullText()), q)
        for (p in ans.paragraphs) assertTrue(p.ruleIds.isNotEmpty() && p.why.isNotEmpty(), "$q: ${p.text}")
    }
}
