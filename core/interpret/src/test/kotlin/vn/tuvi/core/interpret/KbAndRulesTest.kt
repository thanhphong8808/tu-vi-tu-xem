package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.CuMonTable
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.SchoolConfig
import vn.tuvi.core.engine.SchoolPreset
import vn.tuvi.core.engine.StarKind
import java.time.LocalDateTime
import kotlin.random.Random

/** Cổng giai đoạn 3: KB lint pass; mỗi luật có lá số kích hoạt và lá số không kích hoạt. */
class KbAndRulesTest {
    private val kb = Fixtures.kb

    @Test
    fun `KB lint sạch (mục 14_3)`() {
        val issues = KbLint.run(kb)
        assertTrue(issues.isEmpty(), "Lỗi lint:\n" + issues.take(40).joinToString("\n"))
    }

    @Test
    fun `khối lượng KB tối thiểu (mục 9, 15)`() {
        assertTrue(kb.rules.size >= 120, "cần ≥ 120 luật, có ${kb.rules.size}")
        val cachCuc = kb.rules.count { it.type in setOf(RuleType.CAT_CACH, RuleType.HUNG_CACH, RuleType.PHAN_VI, RuleType.THE_SAO) }
        assertTrue(cachCuc >= 90, "cách cục mục 10: $cachCuc")
        assertEquals(24, kb.combos.size, "24 bộ đồng cung")
        assertTrue(kb.minor.size >= 60, "≥ 60 phụ tinh, có ${kb.minor.size}")
        assertTrue(kb.texts.keys.count { it.startsWith("combo.") } == 24 * 12, "24 bộ × 12 cung")
        assertTrue(kb.faq.isNotEmpty(), "có thư mục faq/")
        assertTrue(kb.rules.all { !it.reviewed } && kb.texts.values.all { !it.reviewed }, "mọi mục đang là bản nháp chưa duyệt")
    }

    @Test
    fun `lint bắt được lỗi cố ý`() {
        assertEquals(listOf("chắc chắn"), KbLint.forbiddenIn("Năm nay chắc chắn thành công"))
        assertEquals(listOf("100%"), KbLint.forbiddenIn("đúng 100% luôn"))
        assertTrue(KbLint.forbiddenIn("Thiên Riêu chết").isNotEmpty())
        assertTrue(KbLint.forbiddenIn("chếtt").isEmpty(), "theo ranh giới từ")
        assertEquals(listOf("cung", "foo"), KbLint.slotsIn("{cung} và {foo}"))
    }

    @TestFactory
    fun `mỗi luật có lá số kích hoạt và lá số không kích hoạt`(): List<DynamicTest> {
        val fx = Fixtures.load()
        val missing = kb.rules.map { it.id }.filter { it !in fx }
        val tests = mutableListOf(DynamicTest.dynamicTest("đủ lá số mẫu cho ${kb.rules.size} luật") {
            assertTrue(missing.isEmpty(), "thiếu lá số mẫu: $missing — chạy ./gradlew :core:interpret:findFixtures")
        })
        for (r in kb.rules) {
            val (pos, neg) = fx[r.id] ?: continue
            tests += DynamicTest.dynamicTest("${r.id} — ${r.name}") {
                assertTrue(Fixtures.fires(r, pos), "${r.id} phải kích hoạt với $pos")
                assertFalse(Fixtures.fires(r, neg), "${r.id} không được kích hoạt với $neg")
            }
        }
        return tests
    }

    @Test
    fun `câu đơn sao, bộ đồng cung và phụ tinh luôn có câu trong KB (5000 lá số, cả hai bảng Cự Môn)`() {
        val engine = RuleEngine(kb)
        val rnd = Random(7)
        val cfgs = listOf(SchoolConfig.DEFAULT, SchoolConfig(cuMonTable = CuMonTable.CAM_NANG_V2), SchoolPreset.TOAN_THU_TRUNG_CHAU.config)
        val missing = HashSet<String>()
        val pairs = HashSet<String>()
        repeat(5000) {
            val t = LocalDateTime.of(1900 + rnd.nextInt(201), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), rnd.nextInt(60))
            val chart = ChartEngine.build(BirthInput(t, null, if (rnd.nextBoolean()) Gender.NAM else Gender.NU), cfgs[it % 3])
            for (f in engine.starFindings(chart)) if (f.textKey !in kb.texts) missing.add(f.textKey)
            for (p in chart.palaces.values) {
                val main = p.stars.filter { s -> s.kind == StarKind.CHINH_TINH }
                assertTrue(main.size <= 2)
                if (main.size == 2) {
                    val key = kb.comboKey(main[0].id, main[1].id)
                    if (key == null) pairs.add("${main[0].id}+${main[1].id}")
                }
            }
        }
        assertTrue(pairs.isEmpty(), "cặp chính tinh đồng cung chưa có trong 24 bộ: $pairs")
        assertTrue(missing.isEmpty(), "thiếu câu luận: ${missing.take(20)}")
    }
}
