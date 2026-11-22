package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.engine.ChartEngine
import java.time.LocalDate

/**
 * D-57 — Trợ lý AI sinh lời: lời nhắc dựng từ câu trả lời QaAnalyst, kiểm tra đầu ra mục 12.3 bắt được các lỗi
 * hay gặp của model nhỏ (gán sao sai cung, bịa năm/điểm, từ cấm, tiếng Anh, JSON hỏng, quên điểm chung),
 * còn câu diễn đạt đúng dữ kiện thì cho qua.
 */
class AiTest {
    private val qa = QaEngine(Fixtures.kb)
    private val cfg = AiModelConfig.load()
    private val ctx = QaContext(LocalDate.of(2026, 11, 21))
    private val a = ChartEngine.build(ReadingTest.GOLDEN.getValue("A"))

    private fun prompt(q: String, chart: vn.tuvi.core.engine.Chart = a): Pair<QaAnswer, AiPrompt> {
        val ans = qa.answer(chart, q, ctx)
        return ans to AiPrompt.build(ans, ctx.lunarYear, cfg)
    }

    private fun json(s: String) = "{\"answer\": \"" + s.replace("\"", "\\\"") + "\"}"

    @Test
    fun configAndPrompt() {
        assertEquals(64, cfg.sha256.length)
        assertTrue(cfg.url.startsWith("https://huggingface.co/"), cfg.url)
        assertTrue(cfg.bytes > 1_000_000_000L)
        assertTrue(cfg.minWords < cfg.maxWords)
        val sys = AiModelConfig.systemPrompt()
        assertTrue("<du_kien>" in sys && "{\"answer\"" in sys, sys)
        // D-68: hai model — Nhanh (E2B, mặc định) và Mạnh (E4B), cấu hình riêng; system prompt điền độ dài theo model.
        val all = AiModelConfig.all()
        assertEquals(listOf("Nhanh", "Mạnh"), all.map { it.tier })
        val e4 = AiModelConfig.load("gemma-4-E4B-it")
        assertEquals("gemma-4-E2B-it", cfg.id)
        assertTrue(e4.bytes > cfg.bytes && e4.contextChars > cfg.contextChars && e4.maxTokens > cfg.maxTokens && e4.maxWords > cfg.maxWords)
        assertTrue(e4.url.contains("gemma-4-E4B-it-litert-lm/resolve/") && e4.sha256.length == 64)
        assertEquals(cfg.id, AiModelConfig.load("không có").id)
        val s4 = AiModelConfig.systemPrompt(e4)
        assertTrue("${e4.minWords + 40}–${e4.maxWords - 100} chữ" in s4 && "{" !in s4.substringBefore("TRẢ VỀ"), s4)
    }

    @Test
    fun promptKeepsConcreteParagraphsWithinBudget() {
        // Khối dữ kiện nhỏ: vẫn giữ Kết luận và đoạn “Cụ thể trong năm” trước các đoạn từng sao, sách.
        val (ans, _) = prompt("Năm sau sự nghiệp của tôi thế nào?")
        val small = AiPrompt.build(ans, ctx.lunarYear, cfg.copy(contextChars = 1800))
        val block = small.user.substringAfter("<du_kien>")
        assertTrue("[Cụ thể trong năm]" in block, block)
        assertTrue("[Sách bàn" !in block)
        // Thứ tự gốc được giữ.
        val iKl = block.indexOf("[Kết luận]"); val iCt = block.indexOf("[Cụ thể trong năm]")
        assertTrue(iKl in 0 until iCt)
    }

    @Test
    fun promptCarriesLayersAndFitsBudget() {
        for (q in listOf("Năm sau sự nghiệp của tôi thế nào?", "Tình duyên năm 2027 ra sao?", "Đại vận này tài chính thế nào?",
            "Năm nào tôi dễ có tiền nhất?", "Cung Mệnh của tôi có sao gì?", "Tháng 3 năm sau sức khỏe cần lưu ý gì?",
            "Năm 2020 công việc của tôi thế nào?", "Tôi có số giàu không?")) {
            val (ans, p) = prompt(q)
            assertNull(AiPrompt.skipReason(ans), "$q: ${ans.parse.intent}")
            val block = p.user.substringAfter("<du_kien>").substringBefore("</du_kien>")
            assertTrue(block.length <= cfg.contextChars + 200, "$q: dữ kiện ${block.length} ký tự")
            assertTrue(p.user.startsWith("<cau_hoi>$q</cau_hoi>"), p.user.take(80))
            assertTrue("Năm hiện tại: ${ctx.lunarYear}" in block)
            ans.score?.let { assertTrue("Điểm chung: $it/100" in block, "$q: thiếu điểm chung") }
            // Đoạn Kết luận luôn có mặt.
            assertTrue(ans.paragraphs.first().text.take(40) in block, q)
        }
        val (y, p) = prompt("Năm sau sự nghiệp của tôi thế nào?")
        for (t in listOf("① Lá số gốc", "② Đại hạn", "④ Tiểu hạn", "⑤ Lưu Thái Tuế")) assertTrue(t in p.user, "thiếu tầng $t")
        assertNotNull(y.score)
    }

    @Test
    fun crisisAndRefusalNeverGoToModel() {
        val (c, _) = prompt("Tôi muốn chết, chán sống quá")
        assertEquals(QaIntent.CRISIS, c.parse.intent)
        assertNotNull(AiPrompt.skipReason(c))
        val (r, _) = prompt("Giá vàng tuần sau thế nào?")
        assertTrue(r.parse.intent == QaIntent.REFUSE || r.parse.intent == QaIntent.UNKNOWN, r.parse.intent.name)
        assertNotNull(AiPrompt.skipReason(r))
    }

    @Test
    fun fakeModelPassesGuardOnGoldenCharts() {
        var passed = 0
        var total = 0
        for (id in listOf("A", "B", "C", "D")) {
            val chart = ChartEngine.build(ReadingTest.GOLDEN.getValue(id))
            for (q in listOf("Năm sau sự nghiệp của tôi thế nào?", "Tình duyên năm 2027 ra sao?", "Tài chính năm nay thế nào?",
                "Đại vận này công việc ra sao?", "Sức khỏe năm sau cần lưu ý gì?")) {
                val (ans, p) = prompt(q, chart)
                if (AiPrompt.skipReason(ans) != null) continue
                total++
                val r = AiGuard.check(AiFake.generate(p.user), p.facts, cfg)
                if (r.ok) passed++ else println("$id · $q: ${r.issues}")
            }
        }
        assertTrue(total >= 16, "số câu $total")
        assertEquals(total, passed, "bản giả lập chép đúng dữ kiện phải qua kiểm tra")
    }

    private fun goodAnswer(): Pair<AiPrompt, String> {
        val (ans, p) = prompt("Năm sau sự nghiệp của tôi thế nào?")
        val r = AiGuard.check(AiFake.generate(p.user), p.facts, cfg)
        assertTrue(r.ok, r.issues.toString())
        assertNotNull(ans.score)
        return p to r.text
    }

    @Test
    fun guardCatchesTypicalModelErrors() {
        val (p, good) = goodAnswer()
        fun issues(s: String) = AiGuard.check(json(s), p.facts, cfg).issues
        assertTrue(issues(good).isEmpty())
        // JSON hỏng hoặc thiếu khóa answer.
        assertFalse(AiGuard.check(good, p.facts, cfg).ok)
        assertFalse(AiGuard.check("{\"text\": \"$good\"}", p.facts, cfg).ok)
        // Model quên đóng chuỗi / thừa ký tự cuối: vẫn đọc được, nội dung vẫn kiểm đủ (D-68).
        val esc = good.replace("\"", "\\\"")
        assertTrue(AiGuard.check("{\"answer\": \"$esc\n", p.facts, cfg).ok)
        assertTrue(AiGuard.check("{\"answer\": \"$esc\", \"}}", p.facts, cfg).ok)
        assertFalse(AiGuard.check("{\"answer\": \"$esc Năm 2031 sẽ rất thuận.", p.facts, cfg).ok)
        // Chấp nhận ```json … ``` và markdown đơn giản (được làm sạch).
        assertTrue(AiGuard.check("```json\n" + json("**" + good + "**") + "\n```", p.facts, cfg).ok)
        // Bịa năm, bịa điểm.
        assertTrue(issues("$good Năm 2031 sẽ rất thuận.").any { "2031" in it })
        assertTrue(issues("$good Riêng đại hạn được 93/100.").any { "93" in it })
        // Từ cấm, tiếng Anh, chữ Hán.
        assertTrue(issues("$good Điều này chắc chắn xảy ra.").any { "chắc chắn" in it })
        assertTrue(issues("$good This is the best year for your career and you will win.").any { "tiếng Anh" in it })
        assertTrue(issues("$good 事业").any { "tiếng Việt" in it })
        // Sao không có trong dữ kiện; sao gán sai cung.
        assertTrue(issues("$good Sao Phá Quân cũng giúp bạn.").any { "Phá Quân" in it })
        // Chọn một sao và một cung đều có trong dữ kiện nhưng không bao giờ đứng cùng câu → gán sai.
        val near = AiGuard.starPlaces(p.facts.lines)
        val stars = vn.tuvi.core.engine.StarId.entries.map { it.vn }.filter { ' ' in it && it in near }
        val roles = vn.tuvi.core.engine.PalaceRole.entries.map { it.vn }.filter { it in p.facts.text && it != "Phúc Đức" }
        val (st, ro) = stars.flatMap { x -> roles.map { x to it } }.first { (x, r) -> r !in near.getValue(x) }
        val wrong = issues("$good $st đóng tại cung $ro nên công việc rất vững.")
        assertTrue(wrong.any { st in it && ro in it }, "$st/$ro: $wrong")
        // Quên điểm chung.
        val noScore = good.replace(Regex("\\d+/100"), "").replace(Regex("\\b\\d+\\b"), "")
        // Chỉ quên điểm chung → app tự thêm câu điểm chung vào đầu, câu vẫn được dùng.
        val fixed = AiGuard.check(json(noScore), p.facts, cfg)
        assertTrue(fixed.ok && fixed.text.startsWith("Điểm chung: ${p.facts.score}/100."), fixed.issues.toString())
        // Quên điểm chung cùng một lỗi khác → vẫn trượt.
        assertFalse(AiGuard.check(json("$noScore Năm 2031 sẽ rất thuận."), p.facts, cfg).ok)
        // Điểm đúng tầng: lấy điểm của tầng khác gán cho đại hạn → trượt.
        val dh = Regex("→ Đại hạn: (\\d+)/100").find(p.facts.text)!!.groupValues[1]
        val other = Regex("→ (Tiểu hạn|Nền gốc|Lưu Thái Tuế|Lưu niên đại hạn): (\\d+)/100").findAll(p.facts.text)
            .map { it.groupValues[2] }.first { it != dh }
        assertTrue(issues("$good Riêng đại hạn được $other/100.").any { "gán cho Đại hạn" in it })
        assertTrue(issues("$good Riêng đại hạn được $dh/100.").isEmpty())
        // Quá ngắn.
        assertTrue(issues("Năm sau ổn.").any { "ngắn" in it })
    }

    @Test
    fun nearestPlaceDecidesStarPalacePair() {
        val facts = AiFacts(listOf(
            "[② Đại hạn] Đại hạn tại Ngọ. Xoay 12 cung từ đây, cung Phụ Mẫu của đại hạn rơi vào Mùi (cung gốc Huynh Đệ): Thiên Đồng hãm – Cự Môn hãm.",
            "[① Lá số gốc] Cung Tài Bạch an tại Tuất: Thiên Phủ vượng. Xung chiếu từ Phúc Đức (Thìn): Thất Sát hãm; tam hợp Mệnh (Dần) và Quan Lộc (Ngọ).",
            "Cung Mệnh của bạn ở Dần (Mậu Dần). Chính tinh: Vũ Khúc (Vượng), Thiên Tướng (Miếu).",
        ), null, "")
        fun bad(s: String) = AiGuard.check(json(s.repeat(1)), facts, cfg.copy(minWords = 1)).issues.filter { "gán" in it }
        // Câu có hai nơi chốn: sao đi với nơi gần nhất (Mùi), không bắt lỗi cặp xa (Ngọ).
        assertTrue(bad("Đại hạn tại Ngọ, cung Phụ Mẫu rơi vào Mùi, có Thiên Đồng hãm và Cự Môn hãm.").isEmpty())
        assertTrue(bad("Xung chiếu từ Phúc Đức là Thất Sát hãm, và có tam hợp Mệnh và Quan Lộc.").isEmpty())
        assertTrue(bad("Cung Mệnh của bạn có Vũ Khúc vượng và Thiên Tướng miếu.").isEmpty())
        // Gán sai thật.
        assertTrue(bad("Thiên Phủ đóng ở cung Mệnh.").isNotEmpty())
        assertTrue(bad("Cung Tài Bạch có Vũ Khúc vượng.").isNotEmpty())
    }

    @Test
    fun mentionsUseWordBoundariesAndLongestMatch() {
        assertEquals(setOf("Thiên Lương"), AiGuard.mentions("Thiên Lương hãm", listOf("Thiên Lương", "Lương")))
        assertTrue(AiGuard.mentions("Tử Vi Đẩu Số", listOf("Tử Vi")).contains("Tử Vi"))
        assertEquals(setOf("Thân"), AiGuard.placedChi("Liêm Trinh vượng tại Thân, năm Đinh Mùi"))
    }
}
