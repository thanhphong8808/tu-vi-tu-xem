package vn.tuvi.core.interpret
import org.junit.jupiter.api.Test
import vn.tuvi.core.engine.ChartEngine
import java.time.LocalDate
class TmpPrint {
    @Test fun p() {
        val qa = QaEngine(Fixtures.kb); val ctx = QaContext(LocalDate.of(2026, 11, 22))
        val a = ChartEngine.build(ReadingTest.GOLDEN.getValue("A"))
        val sb = StringBuilder()
        val qs = (System.getenv("QS") ?: java.io.File("/tmp/claude-0/sc/qs.txt").readText()).lines().filter { it.isNotBlank() }
        for (q in qs) {
            val r = qa.answer(a, q, ctx)
            sb.append("=== $q\n[${r.parse.intent}] ${r.understood} | điểm ${r.score}\n")
            for (p in r.paragraphs) sb.append("- (${p.title}|${p.kind}) ${p.text}\n")
            for (ad in r.advice) sb.append("  * ${ad.text}\n")
            sb.append("tiếp: ${r.followUps}\n")
        }
        java.io.File("/tmp/claude-0/sc/out.txt").writeText(sb.toString())
    }
}
