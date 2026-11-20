package vn.tuvi.core.engine

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.Golden.str
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

/** Cổng Giai đoạn 2 (mục 15): trường `nam2026` của golden, ví dụ Tân Biên, và bất biến của hệ vận hạn. */
class LimitEngineTest {

    @TestFactory
    @DisplayName("Trường nam2026 của 4 ca golden")
    fun golden2026(): List<DynamicNode> = Golden.cases.map { case ->
        val id = case.str("id")
        val chart = Golden.chart(case)
        val expected = case.getValue("nam2026").jsonObject
        val year = LimitEngine.forYear(chart, 2026)
        val month1 = LimitEngine.forMonth(chart, 2026, 1)
        val checks = listOf(
            Triple("tuổi âm", expected.getValue("tuoiAm").jsonPrimitive.content, year.tuoiAm.toString()),
            Triple("đại hạn", expected.str("daiHan"), year.daiHan.vn),
            Triple("tiểu hạn", expected.str("tieuHan"), year.tieuHan.vn),
            Triple("lưu niên đại hạn", expected.str("luuNienDaiHan"), year.luuNienDaiHan.vn),
            Triple("nguyệt hạn tháng Giêng", expected.str("nguyetHanThang1"), month1.nguyetHan!!.vn),
        )
        DynamicContainer.dynamicContainer(
            "Ca $id: ${case.str("input")}",
            checks.map { (name, exp, act) -> DynamicTest.dynamicTest("$name = $exp") { assertEquals(exp, act, "Ca $id – $name") } },
        )
    }

    @Test
    @DisplayName("Ví dụ Tân Biên: Dương nam, đại hạn 23–32")
    fun `vi du Tan Bien`() {
        // Công thức thuần: 23 gốc, 24 xung chiếu, 25 lùi một cung, 26 xung chiếu, 27 tiến một cung.
        val goc = Chi.DAN
        val xung = Chi.THAN
        assertEquals(listOf(goc, xung, xung - 1, xung, xung + 1), (1..5).map { LimitEngine.luuNienDaiHan(goc, it, +1) })
        // Âm Nam, Dương Nữ: tiến một cung rồi về xung chiếu, sau đó lùi.
        assertEquals(listOf(goc, xung, xung + 1, xung, xung - 1), (1..5).map { LimitEngine.luuNienDaiHan(goc, it, -1) })
        assertEquals(xung + 6, LimitEngine.luuNienDaiHan(goc, 10, +1))

        // Trên một lá số thật: Dương Nam, Mộc tam cục → đại hạn thứ ba phủ tuổi 23–32.
        val chart = findChart { it.amDuongNamNu == AmDuongNamNu.DUONG_NAM && it.cuc == Cuc.MOC_TAM }
        val third = LimitEngine.daiHanPeriods(chart)[2]
        assertEquals(23..32, third.ages)
        val byAge = (23..27).associateWith { age -> LimitEngine.forYear(chart, chart.lunar.year + age - 1) }
        byAge.values.forEach { assertEquals(third.palace, it.daiHan) }
        assertEquals(third.palace, byAge.getValue(23).luuNienDaiHan)
        assertEquals(third.palace.opposite, byAge.getValue(24).luuNienDaiHan)
        assertEquals(third.palace.opposite - 1, byAge.getValue(25).luuNienDaiHan)
        assertEquals(third.palace.opposite, byAge.getValue(26).luuNienDaiHan)
        assertEquals(third.palace.opposite + 1, byAge.getValue(27).luuNienDaiHan)
        assertEquals(listOf(1, 2, 3, 4, 5), byAge.values.map { it.namThuDaiHan })
    }

    @Test
    fun `thien thoi theo ngu hanh tam hop`() {
        // Ca golden A (tuổi Ngọ – Hỏa), năm 2026 đại hạn ở Tỵ (Kim): Hỏa khắc Kim → tuổi khắc đại hạn.
        val a = Golden.chart(Golden.cases.first { it.str("id") == "A" })
        assertEquals(ThienThoi.TUOI_KHAC_DAI_HAN, LimitEngine.forYear(a, 2026).daiHanPeriod.thienThoi)
        assertEquals(ThienThoi.DAI_HAN_SINH_TUOI, ThienThoi.of(NguHanh.HOA, NguHanh.MOC))
        assertEquals(ThienThoi.DONG_HANH, ThienThoi.of(NguHanh.THUY, NguHanh.THUY))
        assertEquals(ThienThoi.TUOI_SINH_DAI_HAN, ThienThoi.of(NguHanh.KIM, NguHanh.THUY))
        assertEquals(ThienThoi.DAI_HAN_KHAC_TUOI, ThienThoi.of(NguHanh.MOC, NguHanh.KIM))
        assertEquals(listOf(2, 1, 0, -1, -2), ThienThoi.entries.map { it.score })
        assertEquals(NguHanh.THUY, tamHopElement(Chi.THIN))
        assertEquals(NguHanh.KIM, tamHopElement(Chi.SUU))
        assertEquals(NguHanh.HOA, tamHopElement(Chi.TUAT))
        assertEquals(NguHanh.MOC, tamHopElement(Chi.MUI))
    }

    @Test
    @DisplayName("Bất biến vận hạn trên 3.000 lá số × năm xem ngẫu nhiên")
    fun invariants() = Prop.forAll(
        iterations = 3_000,
        gen = { r -> Triple(ChartEngine.build(RandomBirths.input(r), RandomBirths.config(r)), r.nextInt(0, 90), r.nextInt(1, 13)) },
    ) { (chart, offset, month) ->
        val year = chart.lunar.year + offset
        val y = LimitEngine.forYear(chart, year)
        val zy = CanChi.ofYear(year).chi
        assertEquals(offset + 1, y.tuoiAm)

        // Lưu Thái Tuế, Lưu Mệnh, tiểu hạn.
        assertEquals(zy, y.luuThaiTue)
        assertEquals(PalaceRole.MENH, y.luuRoles.getValue(zy))
        assertEquals(PalaceRole.entries.toSet(), y.luuRoles.values.toSet())
        assertEquals(zy, chart.palace(y.tieuHan).tieuHanChi)
        assertEquals(PalaceRole.MENH, y.daiHanRoles.getValue(y.daiHan))

        // Đại hạn và lưu niên đại hạn.
        if (y.beforeFirstDaiHan) {
            assertTrue(y.tuoiAm < chart.cuc.number)
            assertEquals(chart.menh, y.daiHan)
            assertNull(y.namThuDaiHan)
        } else {
            assertTrue(y.tuoiAm in chart.palace(y.daiHan).daiHanAges, "tuổi ${y.tuoiAm} phải nằm trong đại hạn ${y.daiHan}")
            assertEquals(y.daiHanPeriod.ages.first + y.namThuDaiHan!! - 1, y.tuoiAm)
            assertEquals(LimitEngine.luuNienDaiHan(y.daiHan, y.namThuDaiHan!!, chart.dir), y.luuNienDaiHan)
            when (y.namThuDaiHan) {
                1 -> assertEquals(y.daiHan, y.luuNienDaiHan)
                2, 4 -> assertEquals(y.daiHan.opposite, y.luuNienDaiHan)
            }
        }
        assertEquals(CanChi(chart.palace(y.daiHan).can, y.daiHan), y.daiHanPeriod.canChi)

        // Sao lưu.
        val l = y.luuStars
        fun at(s: StarId) = l.getValue(s).index
        assertEquals(15, l.size)
        assertEquals(zy, l.getValue(StarId.THAI_TUE))
        assertEquals(2, Math.floorMod(at(StarId.TANG_MON) - zy.index, 12))
        assertEquals(l.getValue(StarId.TANG_MON).opposite, l.getValue(StarId.BACH_HO))
        assertEquals(0, Math.floorMod(at(StarId.THIEN_KHOC) + at(StarId.THIEN_HU), 12))
        assertEquals(6, Math.floorMod(at(StarId.THIEN_HY) - at(StarId.HONG_LOAN), 12))
        assertEquals(l.getValue(StarId.LOC_TON) + 1, l.getValue(StarId.KINH_DUONG))
        assertEquals(l.getValue(StarId.LOC_TON) - 1, l.getValue(StarId.DA_LA))
        assertFalse(l.getValue(StarId.LOC_TON) in setOf(Chi.THIN, Chi.TUAT, Chi.SUU, Chi.MUI))
        assertEquals(l.getValue(StarId.LOC_TON) + 7 * chart.dir, l.getValue(StarId.HY_THAN))
        assertTrue(l.getValue(StarId.THIEN_MA) in setOf(Chi.DAN, Chi.THAN, Chi.TI, Chi.HOI))

        // Tứ Hóa lưu bám vị trí sao gốc của năm xem.
        Tables.tuHoa(CanChi.ofYear(year).can, chart.config).forEach { (hoa, star) ->
            assertEquals(chart.positionOf(star), y.luuTuHoa.getValue(hoa))
        }
        if (chart.config.daiHanTuHoa == DaiHanTuHoa.ON) assertNotNull(y.daiHanTuHoa) else assertNull(y.daiHanTuHoa)

        // Nguyệt hạn: 12 tháng nối tiếp; tháng nhuận chung cung với tháng chính.
        val m1 = LimitEngine.forMonth(chart, year, 1).nguyetHan!!
        val mk = LimitEngine.forMonth(chart, year, month)
        assertEquals(m1 + (month - 1), mk.nguyetHan)
        assertEquals(mk.nguyetHan, LimitEngine.forMonth(chart, year, month, isLeap = true).nguyetHan)
        assertEquals(CanChi.ofMonth(CanChi.ofYear(year).can, month), mk.monthCanChi)
        assertNotNull(mk.nguyetTuHoa)
        assertNull(y.nguyetHan)
    }

    @Test
    fun `12 nam lien tiep tieu han va Luu Thai Tue di du 12 cung`() = Prop.forAll(500, gen = { r -> ChartEngine.build(RandomBirths.input(r)) }) { chart ->
        val years = (0 until 12).map { LimitEngine.forYear(chart, chart.lunar.year + 20 + it) }
        assertEquals(12, years.map { it.tieuHan }.toSet().size)
        assertEquals(12, years.map { it.luuThaiTue }.toSet().size)
        // Tiểu hạn Nam đi thuận, Nữ đi nghịch theo năm.
        val step = if (chart.gender == Gender.NAM) 1 else -1
        years.zipWithNext().forEach { (a, b) -> assertEquals(a.tieuHan + step, b.tieuHan) }
    }

    @Test
    fun `dai han 12 cung noi tiep`() {
        val chart = Golden.chart(Golden.cases.first())
        val periods = LimitEngine.daiHanPeriods(chart)
        assertEquals(12, periods.size)
        assertEquals(Chi.entries.toSet(), periods.map { it.palace }.toSet())
        periods.zipWithNext().forEach { (a, b) ->
            assertEquals(a.ages.last + 1, b.ages.first)
            assertEquals(a.palace + chart.dir, b.palace)
        }
        assertEquals("Mậu Dần", periods[0].canChi.vn) // ca A: Mệnh Mậu Dần, Thổ ngũ cục
        assertEquals(5..14, periods[0].ages)
    }

    @Test
    fun `nhat han va thoi han`() = Prop.forAll(1_000, gen = { r ->
        val chart = ChartEngine.build(RandomBirths.input(r))
        val date = LunarCalendar.lunarToSolar(LunarDate(1, 1, chart.lunar.year + r.nextInt(1, 80), false))!!
            .plusDays(r.nextLong(0, 365))
        Triple(chart, date, Chi.of(r.nextInt(12)))
    }) { (chart, date, hour) ->
        val lunar = LunarCalendar.solarToLunar(date, 7)
        val d = LimitEngine.forDate(chart, date)
        assertEquals(lunar, d.lunarDate)
        assertEquals(LimitEngine.forMonth(chart, lunar.year, lunar.month).nguyetHan!! + (lunar.day - 1), d.nhatHan)
        assertEquals(LunarCalendar.dayCanChi(date), d.dayCanChi)
        val h = LimitEngine.forHour(chart, date, hour)
        assertEquals(d.nhatHan!! + hour.index, h.thoiHan)
        assertEquals(d.nhatHan, LimitEngine.forHour(chart, date, Chi.TY).thoiHan)
        assertNull(d.thoiHan)
    }

    @Test
    fun `bien the nguyet han`() {
        val base = Golden.chart(Golden.cases.first()) // ca A: tháng 6, giờ Tỵ
        val m = base.effectiveMonth
        val h = base.hourChi.index
        fun month1(v: NguyetHanVariant): Pair<LimitLayers, Chi> {
            val chart = ChartEngine.build(base.input, base.config.copy(nguyetHan = v))
            return LimitEngine.forYear(chart, 2026) to LimitEngine.forMonth(chart, 2026, 1).nguyetHan!!
        }
        month1(NguyetHanVariant.TIEU_HAN_NGHICH_THANG_THUAN_GIO).let { (y, g) -> assertEquals(y.tieuHan - (m - 1) + h, g) }
        month1(NguyetHanVariant.TIEU_HAN_THUAN_THANG_THUAN_GIO).let { (y, g) -> assertEquals(y.tieuHan + (m - 1) + h, g) }
        month1(NguyetHanVariant.TIEU_HAN_LA_THANG_GIENG).let { (y, g) -> assertEquals(y.tieuHan, g) }
        month1(NguyetHanVariant.DAU_QUAN_LUU_THAI_TUE).let { (y, g) -> assertEquals(y.luuThaiTue - (m - 1) + h, g) }
        // Đổi biến thể nguyệt hạn không đổi các tầng năm.
        val a = LimitEngine.forYear(base, 2026)
        val b = month1(NguyetHanVariant.DAU_QUAN_LUU_THAI_TUE).first
        assertEquals(listOf(a.daiHan, a.tieuHan, a.luuNienDaiHan, a.luuThaiTue), listOf(b.daiHan, b.tieuHan, b.luuNienDaiHan, b.luuThaiTue))
    }

    @Test
    fun `Tu Hoa dai han khi bat`() {
        val base = Golden.chart(Golden.cases.first())
        val on = ChartEngine.build(base.input, base.config.copy(daiHanTuHoa = DaiHanTuHoa.ON))
        val y = LimitEngine.forYear(on, 2026)
        val expected = Tables.tuHoa(on.palace(y.daiHan).can, on.config).mapValues { (_, star) -> on.positionOf(star) }
        assertEquals(expected, y.daiHanTuHoa)
        assertNull(LimitEngine.forYear(base, 2026).daiHanTuHoa)
    }

    @Test
    fun `truoc dai han dau va nam xem truoc nam sinh`() {
        val chart = Golden.chart(Golden.cases.first { it.str("id") == "C" }) // sinh 2023, Kim tứ cục
        val y = LimitEngine.forYear(chart, 2024) // 2 tuổi âm
        assertTrue(y.beforeFirstDaiHan)
        assertEquals(chart.menh, y.daiHan)
        assertEquals(chart.menh, y.luuNienDaiHan)
        assertNull(y.namThuDaiHan)
        assertFalse(LimitEngine.forYear(chart, 2026).beforeFirstDaiHan) // 4 tuổi = số Cục
        assertThrows(IllegalArgumentException::class.java) { LimitEngine.forYear(chart, 2022) }
    }

    private fun findChart(predicate: (Chart) -> Boolean): Chart {
        val r = Random(11)
        repeat(10_000) {
            val date = RandomBirths.solarDate(r)
            val chart = ChartEngine.build(
                BirthInput(LocalDateTime.of(date, LocalTime.ofSecondOfDay(r.nextLong(0, 86_400))), null, Gender.NAM),
            )
            if (predicate(chart)) return chart
        }
        error("Không tìm được lá số thỏa điều kiện")
    }
}
