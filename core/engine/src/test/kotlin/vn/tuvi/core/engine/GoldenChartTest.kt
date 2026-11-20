package vn.tuvi.core.engine

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi

/**
 * 4 lá số golden (preset Nam phái VN) — docs/PROMPT_TUVI_APP.md mục 14.1, lưu ở golden.json.
 * Test này kiểm phần lá số; trường `nam2026` (vận hạn) kiểm ở LimitEngineTest. Cung tiểu hạn và tuổi âm được kiểm
 * thêm ở đây vì là dữ liệu của lá số gốc.
 */
class GoldenChartTest {

    private val cases get() = Golden.cases
    private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content
    private fun parseInput(s: String): BirthInput = Golden.parseInput(s)

    @TestFactory
    fun golden(): List<DynamicNode> = cases.map { case ->
        val id = case.str("id")
        val chart = ChartEngine.build(parseInput(case.str("input")))
        val tests = mutableListOf<DynamicTest>()
        fun check(name: String, expected: String, actual: String) {
            tests += DynamicTest.dynamicTest("$name = $expected") { assertEquals(expected, actual, "Ca $id – $name") }
        }

        check("âm lịch", case.str("lunar"), chart.lunar.toString())
        check("năm", case.str("nam"), CanChi.ofYear(chart.lunar.year).vn)
        check("giờ", case.str("gio"), chart.hourChi.vn)
        check("Mệnh", case.str("menh"), chart.menh.vn)
        check("Thân", case.str("than"), chart.than.vn)
        check("Cục", case.str("cuc"), chart.cuc.short)
        check("bản mệnh", case.str("banMenh"), chart.banMenh.element.vn)

        case.getValue("chinhTinh").jsonObject.forEach { (star, chi) ->
            check(star, chi.jsonPrimitive.content, chart.positionOf(StarId.fromVn(star)).vn)
        }
        case.getValue("phuTinh").jsonObject.forEach { (star, chi) ->
            check(star, chi.jsonPrimitive.content, chart.positionOf(StarId.fromVn(star)).vn)
        }

        val tuan = case.getValue("tuan").jsonArray.map { it.jsonPrimitive.content }
        val triet = case.getValue("triet").jsonArray.map { it.jsonPrimitive.content }
        check("Tuần", tuan.joinToString("–"), "${chart.tuan.first.vn}–${chart.tuan.second.vn}")
        check("Triệt", triet.joinToString("–"), "${chart.triet.first.vn}–${chart.triet.second.vn}")
        check("cờ Tuần trên cung", tuan.joinToString(), chart.palaces.values.filter { it.hasTuan }.joinToString { it.chi.vn })
        check("cờ Triệt trên cung", triet.joinToString(), chart.palaces.values.filter { it.hasTriet }.joinToString { it.chi.vn })

        val daiHan = case.getValue("daiHan3").jsonArray.map { it.jsonPrimitive.content }
        daiHan.forEachIndexed { i, expected ->
            val (chiName, ages) = expected.split(" ")
            val palace = chart.palace(Chi.fromVn(chiName))
            check("đại hạn ${i + 1}", expected, "${palace.chi.vn} ${palace.daiHanAges.first}-${palace.daiHanAges.last}")
            check("đại hạn ${i + 1} tuổi khởi", ages.substringBefore("-"), palace.daiHanStartAge.toString())
        }

        val nam2026 = case.getValue("nam2026").jsonObject
        val tieuHan = chart.palaces.values.single { it.tieuHanChi == CanChi.ofYear(2026).chi }
        check("tiểu hạn 2026 (chi tiểu hạn trên cung)", nam2026.str("tieuHan"), tieuHan.chi.vn)
        check("tuổi âm 2026", nam2026.str("tuoiAm"), chart.tuoiAm(2026).toString())

        DynamicContainer.dynamicContainer("Ca $id: ${case.str("input")}", tests)
    }

    @TestFactory
    fun `canh bao tren the xac nhan`(): List<DynamicTest> {
        val expected = mapOf(
            "A" to emptyList(),
            "B" to listOf(WarningCode.TY_MUON),
            "C" to listOf(WarningCode.LEAP_MONTH),
            "D" to emptyList<WarningCode>(), // tz 7 và tz 8 cho cùng ngày âm → không cảnh báo
        )
        return cases.map { case ->
            val id = case.str("id")
            DynamicTest.dynamicTest("Ca $id: ${expected.getValue(id)}") {
                val chart = ChartEngine.build(parseInput(case.str("input")))
                assertEquals(expected.getValue(id), chart.warnings.map { it.code })
            }
        }
    }
}
