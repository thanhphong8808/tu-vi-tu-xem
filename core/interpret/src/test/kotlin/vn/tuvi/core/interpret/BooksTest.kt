package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.PalaceRole
import java.time.LocalDate

/**
 * D-62 — Kiến thức từ sách: câu chọn ra phải đúng với lá số (sao tọa thủ, độ sáng, sao đồng cung), không lọt từ cấm,
 * có nguồn sách và chương, và đi vào câu trả lời Hỏi đáp (đoạn “Sách bàn về cung …”) cũng như lời nhắc AI.
 */
class BooksTest {
    private val books = Books.load()
    private val charts = listOf("A", "B", "C", "D").map { ChartEngine.build(ReadingTest.GOLDEN.getValue(it)) }

    @Test
    fun allBookTextsAreCleanAndCited() {
        var total = 0
        for (chart in charts) for (role in PalaceRole.entries) {
            for (l in books.forPalace(chart, role, max = 20, perStar = 10) + books.nghiemLy(chart, role, max = 10)) {
                total++
                assertTrue(KbLint.forbiddenIn(l.text).isEmpty(), "từ cấm trong: ${l.text}")
                assertTrue(l.source.startsWith("Tử Vi Đẩu Số Toàn Thư") || l.source.startsWith("Tử Vi Nghiệm Lý"), l.source)
                assertTrue(l.text.endsWith("."), l.text)
            }
        }
        assertTrue(total > 100, "4 lá số × 12 cung phải ra nhiều câu sách, được $total")
    }

    @Test
    fun linesMatchTheChart() {
        for (chart in charts) for (role in PalaceRole.entries) {
            val p = chart.palace(role)
            for (l in books.forPalace(chart, role, max = 20, perStar = 10)) {
                // Câu luôn mở đầu bằng đúng cung và sao tọa thủ trong cung đó.
                val body = l.text.removePrefix("Tuy vậy, c").let { if (it != l.text) "C$it" else it }
                assertTrue(body.startsWith("Cung ${role.vn} có "), l.text)
                val star = p.stars.firstOrNull { body.startsWith("Cung ${role.vn} có ${it.id.vn}") }
                assertNotNull(star, "${role.vn}: sao trong câu không tọa thủ cung: ${l.text}")
                // “sáng” chỉ dùng cho sao miếu/vượng/đắc, “hãm” cho sao hãm.
                val name = star!!.id.vn
                if (body.startsWith("Cung ${role.vn} có $name sáng")) {
                    assertTrue(star.brightness in setOf(Brightness.MIEU, Brightness.VUONG, Brightness.DAC), "${l.text} — ${star.brightness}")
                }
                if (body.startsWith("Cung ${role.vn} có $name hãm")) assertEquals(Brightness.HAM, star.brightness, l.text)
                assertTrue(l.why.startsWith("$name tọa thủ ${role.vn}"), l.why)
            }
        }
    }

    @Test
    fun qaAnswersCarryTheBookParagraphIntoTheAiPrompt() {
        val qa = QaEngine(Fixtures.kb)
        val ctx = QaContext(LocalDate.of(2026, 11, 21))
        var seen = 0
        for (chart in charts.take(2)) {
            for (q in listOf("Năm sau sự nghiệp của tôi thế nào?", "Tài chính năm 2027 ra sao?", "Cung Mệnh của tôi có sao gì?", "Tình duyên đại vận này thế nào?")) {
                val a = qa.answer(chart, q, ctx)
                val book = a.paragraphs.firstOrNull { it.title?.startsWith("Sách bàn về cung") == true } ?: continue
                seen++
                assertTrue(book.why.all { it.source.isNotBlank() && it.detail.contains("Căn cứ trên lá số") }, q)
                val p = AiPrompt.build(a, ctx.lunarYear, AiModelConfig.load())
                assertTrue(book.text.take(30) in p.user, "$q: đoạn sách phải có trong lời nhắc AI")
            }
        }
        assertTrue(seen >= 5, "đoạn Sách bàn xuất hiện $seen lần")
    }
}
