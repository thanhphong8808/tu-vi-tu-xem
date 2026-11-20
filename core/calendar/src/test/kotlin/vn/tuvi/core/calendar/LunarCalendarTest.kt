package vn.tuvi.core.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.LocalDate

class LunarCalendarTest {

    private data class Vector(val solar: String, val tz: Int, val lunar: String, val note: String)

    private fun date(s: String): LocalDate {
        val (d, m, y) = s.split("/").map { it.toInt() }
        return LocalDate.of(y, m, d)
    }

    /** Test vector bắt buộc — docs/PROMPT_TUVI_APP.md mục 4. */
    private val vectors = listOf(
        Vector("17/02/2026", 7, "1/1/2026", "Tết 2026"),
        Vector("22/03/2023", 7, "1/2N/2023", "Tháng nhuận"),
        Vector("23/05/2020", 7, "1/4N/2020", "Tháng nhuận"),
        Vector("17/02/2007", 7, "1/1/2007", "Tết VN; lịch TQ (tz 8) là 18/02"),
        Vector("15/08/1990", 7, "25/6/1990", "Can chi ngày"),
        Vector("03/03/1985", 7, "12/2/1985", "Cùng ngày dương, lệch cả tháng (tz 7)"),
        Vector("03/03/1985", 8, "12/1/1985", "Cùng ngày dương, lệch cả tháng (tz 8)"),
        Vector("21/01/1985", 7, "1/1/1985", "Tết VN 1985; TQ ăn Tết 20/02"),
        Vector("30/01/1968", 8, "1/1/1968", "Tết miền Nam"),
        Vector("29/01/1968", 7, "1/1/1968", "Tết miền Bắc"),
        // Bổ sung: phía lịch Trung Quốc của hai năm lệch Tết.
        Vector("18/02/2007", 8, "1/1/2007", "Tết TQ 2007"),
        Vector("20/02/1985", 8, "1/1/1985", "Tết TQ 1985"),
        // Hồi quy D-03: bản gốc amlich.js trả về “ngày 0” cho các ngày này.
        Vector("07/05/2054", 7, "30/3/2054", "Vá lỗi ngày 0 của bản gốc"),
        Vector("09/04/2062", 7, "30/2/2062", "Vá lỗi ngày 0 của bản gốc"),
        Vector("26/03/2009", 8, "30/2/2009", "Vá lỗi ngày 0 của bản gốc"),
    )

    @TestFactory
    @DisplayName("Test vector lịch mục 4")
    fun vectors(): List<DynamicTest> = vectors.map { v ->
        DynamicTest.dynamicTest("${v.solar} tz${v.tz} → ${v.lunar} (${v.note})") {
            val lunar = LunarCalendar.solarToLunar(date(v.solar), v.tz)
            assertEquals(v.lunar, lunar.toString())
            assertEquals(date(v.solar), LunarCalendar.lunarToSolar(lunar, v.tz))
        }
    }

    @Test
    fun `can chi nam cua cac test vector`() {
        assertEquals("Bính Ngọ", CanChi.ofYear(2026).vn)
        assertEquals("Canh Ngọ", CanChi.ofYear(1990).vn)
        assertEquals("Ất Sửu", CanChi.ofYear(1985).vn)
        assertEquals("Mậu Thân", CanChi.ofYear(1968).vn)
        assertEquals("Quý Mão", CanChi.ofYear(2023).vn)
        assertEquals("Quý Mão", CanChi.ofYear(1963).vn)
    }

    @Test
    fun `ngay 15-08-1990 la ngay Nham Ty`() {
        assertEquals("Nhâm Tý", LunarCalendar.dayCanChi(LocalDate.of(1990, 8, 15)).vn)
    }

    @Test
    fun `can chi thang theo Ngu ho don va gio theo Ngu thu don`() {
        assertEquals("Bính Dần", CanChi.ofMonth(Can.GIAP, 1).vn)
        assertEquals("Mậu Dần", CanChi.ofMonth(Can.AT, 1).vn)
        assertEquals("Canh Dần", CanChi.ofMonth(Can.BINH, 1).vn)
        assertEquals("Nhâm Dần", CanChi.ofMonth(Can.DINH, 1).vn)
        assertEquals("Giáp Dần", CanChi.ofMonth(Can.MAU, 1).vn)
        assertEquals("Đinh Sửu", CanChi.ofMonth(Can.GIAP, 12).vn)
        assertEquals("Giáp Tý", CanChi.ofHour(Can.GIAP, Chi.TY).vn)
        assertEquals("Canh Tý", CanChi.ofHour(Can.NHAM, Chi.TY).vn)
        assertEquals("Ất Tỵ", CanChi.ofHour(Can.NHAM, Chi.TI).vn)
    }

    @Test
    fun `gio dong ho sang canh gio`() {
        val expected = mapOf(
            23 to Chi.TY, 0 to Chi.TY, 1 to Chi.SUU, 2 to Chi.SUU, 3 to Chi.DAN, 6 to Chi.MAO,
            10 to Chi.TI, 13 to Chi.MUI, 14 to Chi.MUI, 21 to Chi.HOI, 22 to Chi.HOI,
        )
        expected.forEach { (h, chi) -> assertEquals(chi, hourChiOf(h), "giờ $h") }
    }

    @Test
    fun `thang nhuan cua cac nam`() {
        assertEquals(2, LunarCalendar.leapMonthOf(2023))
        assertEquals(4, LunarCalendar.leapMonthOf(2020))
        assertEquals(6, LunarCalendar.leapMonthOf(2025))
        assertEquals(0, LunarCalendar.leapMonthOf(2026))
        assertEquals(0, LunarCalendar.leapMonthOf(2024))
    }

    @Test
    fun `ngay am khong ton tai tra ve null`() {
        assertNull(LunarCalendar.lunarToSolar(LunarDate(1, 3, 2026, true)), "2026 không nhuận")
        assertNull(LunarCalendar.lunarToSolar(LunarDate(1, 3, 2023, true)), "2023 nhuận tháng 2, không phải 3")
        // Tìm một tháng thiếu (29 ngày) rồi kiểm tra ngày 30 bị từ chối.
        val shortMonth = (1..12).first { LunarCalendar.daysInLunarMonth(it, 2026, false) == 29 }
        assertNull(LunarCalendar.lunarToSolar(LunarDate(30, shortMonth, 2026, false)))
    }

    @Test
    fun `tong so ngay cac thang am bang khoang cach hai Tet`() {
        for (year in listOf(1984, 1985, 2020, 2023, 2026)) {
            val leap = LunarCalendar.leapMonthOf(year)
            var total = 0
            for (m in 1..12) {
                total += LunarCalendar.daysInLunarMonth(m, year, false)!!
                if (m == leap) total += LunarCalendar.daysInLunarMonth(m, year, true)!!
            }
            val tet = LunarCalendar.lunarToSolar(LunarDate(1, 1, year, false))!!
            val nextTet = LunarCalendar.lunarToSolar(LunarDate(1, 1, year + 1, false))!!
            assertEquals(nextTet.toEpochDay() - tet.toEpochDay(), total.toLong(), "năm âm $year")
        }
    }

    @Test
    @DisplayName("Bất biến 8: lịch khứ hồi lunar2Solar(solar2Lunar(d)) == d cho mọi ngày 1900–2100, tz 7 và 8")
    fun `lich khu hoi 1900-2100`() {
        for (tz in listOf(7, 8)) {
            var d = LocalDate.of(1900, 1, 1)
            val end = LocalDate.of(2100, 12, 31)
            var prev: LunarDate? = null
            while (!d.isAfter(end)) {
                val lunar = LunarCalendar.solarToLunar(d, tz)
                val back = LunarCalendar.lunarToSolar(lunar, tz)
                if (back != d) throw AssertionError("tz$tz: $d → $lunar → $back")
                if (lunar.isLeap) {
                    assertEquals(lunar.month, LunarCalendar.leapMonthOf(lunar.year, tz), "tháng nhuận $lunar")
                }
                // Ngày âm tăng liên tục: hôm sau là ngày kế tiếp hoặc mùng 1 tháng mới.
                if (prev != null && lunar.day != prev.day + 1) {
                    assertEquals(1, lunar.day, "nhảy ngày bất thường tại $d")
                    assertTrue(prev.day == 29 || prev.day == 30, "tháng có ${prev.day} ngày tại $d")
                }
                prev = lunar
                d = d.plusDays(1)
            }
        }
    }

    @Test
    fun `mui gio lich theo thoi ky`() {
        assertEquals(8, LunarCalendar.calendarTzFor(LocalDate.of(1967, 8, 7), BirthRegion.VN_BAC))
        assertEquals(7, LunarCalendar.calendarTzFor(LocalDate.of(1967, 8, 8), BirthRegion.VN_BAC))
        assertEquals(8, LunarCalendar.calendarTzFor(LocalDate.of(1968, 1, 30), BirthRegion.VN_NAM))
        assertEquals(7, LunarCalendar.calendarTzFor(LocalDate.of(1968, 1, 29), BirthRegion.VN_BAC))
        assertEquals(8, LunarCalendar.calendarTzFor(LocalDate.of(1975, 4, 30), BirthRegion.VN_NAM))
        assertEquals(7, LunarCalendar.calendarTzFor(LocalDate.of(1975, 5, 1), BirthRegion.VN_NAM))
        assertEquals(8, LunarCalendar.calendarTzFor(LocalDate.of(1964, 2, 10), BirthRegion.VN_BAC))
    }

    @Test
    fun `gio Mat Troi that`() {
        assertEquals(6.8, TrueSolarTime.longitudeCorrectionMinutes(106.7, 7.0), 1e-9)
        // EoT nằm trong khoảng ±17 phút quanh năm.
        var d = LocalDate.of(2026, 1, 1)
        repeat(365) {
            val eot = TrueSolarTime.equationOfTimeMinutes(d)
            assertTrue(eot in -17.0..17.0, "EoT $d = $eot")
            d = d.plusDays(1)
        }
    }
}
