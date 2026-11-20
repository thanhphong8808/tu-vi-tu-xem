package vn.tuvi.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.BirthRegion
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Chuẩn hóa đầu vào (mục 4) và thẻ “Xác nhận lá số” (mục 2.2). */
class BirthNormalizerTest {

    private fun solar(y: Int, mo: Int, d: Int, h: Int, mi: Int, g: Gender = Gender.NAM, region: BirthRegion = BirthRegion.VN_BAC, zone: ZoneId = VN_STANDARD_ZONE) =
        BirthInput(LocalDateTime.of(y, mo, d, h, mi), null, g, zone = zone, region = region)

    private fun Chart.codes() = warnings.map { it.code }
    private fun Chart.warning(code: WarningCode) = warnings.single { it.code == code }

    @Test
    fun `gio Ty muon doi ngay va la so thay the giu ngay`() {
        val chart = ChartEngine.build(solar(1985, 3, 3, 23, 30, Gender.NU))
        assertEquals(LocalDate.of(1985, 3, 4), chart.birth.calendarDate)
        assertEquals("13/2/1985", chart.lunar.toString())
        assertTrue(chart.birth.lateTy)
        val alt = ChartEngine.buildAlternative(chart, chart.warning(WarningCode.TY_MUON))
        assertEquals(DayBoundary.GIU_NGAY, alt.config.dayBoundary)
        assertEquals("12/2/1985", alt.lunar.toString())
        assertEquals(Chi.TY, alt.hourChi)
        // Lá số thay thế cũng cảnh báo và trỏ ngược về cách đổi ngày.
        assertEquals(DayBoundary.DOI_NGAY_23H, alt.warning(WarningCode.TY_MUON).altConfig!!.dayBoundary)
    }

    @Test
    fun `can chi tu tru cua ca golden A`() {
        val chart = ChartEngine.build(solar(1990, 8, 15, 10, 30))
        assertEquals("Canh Ngọ", chart.pillars.year.vn)
        assertEquals("Quý Mùi", chart.pillars.month.vn)
        assertEquals("Nhâm Tý", chart.pillars.day.vn)
        assertEquals("Ất Tỵ", chart.pillars.hour.vn)
        assertEquals(AmDuongNamNu.DUONG_NAM, chart.amDuongNamNu)
        assertEquals("Lộ Bàng Thổ", chart.banMenh.name)
        assertEquals(CucMenhRelation.DONG_HANH, chart.cucMenhRelation)
        assertEquals(StarId.LOC_TON, chart.menhChu) // Mệnh tại Dần
        assertEquals(StarId.HOA_TINH, chart.thanChu) // tuổi Ngọ
        assertTrue(chart.amDuongThuanLy) // tuổi Dương, Mệnh cung Dương (Dần)
    }

    @Test
    fun `thang nhuan SAME_MONTH va SPLIT_15`() {
        val date = LunarCalendar.lunarToSolar(LunarDate(16, 2, 2023, true))!!
        val input = BirthInput(LocalDateTime.of(date, LocalTime.of(6, 0)), null, Gender.NAM)
        val same = ChartEngine.build(input)
        assertEquals("16/2N/2023", same.lunar.toString())
        assertEquals(2, same.effectiveMonth)
        val split = ChartEngine.buildAlternative(same, same.warning(WarningCode.LEAP_MONTH))
        assertEquals(LeapMonthRule.SPLIT_15, split.config.leapMonth)
        assertEquals(3, split.effectiveMonth)
        assertEquals(same.menh + 1, split.menh)
        // Ngày 1–15 của tháng nhuận: hai cách cho cùng lá số.
        val early = ChartEngine.build(input.copy(solar = LocalDateTime.of(date.minusDays(1), LocalTime.of(6, 0))))
        val earlySplit = ChartEngine.build(early.input, early.config.copy(leapMonth = LeapMonthRule.SPLIT_15))
        assertEquals(early.starPositions, earlySplit.starPositions)
    }

    /** Một ngày trước 1967 mà lịch UTC+7 và UTC+8 cho âm lịch khác nhau (dò tất định từ 1960). */
    private fun firstPre1967DisagreeingDate(): LocalDate {
        var d = LocalDate.of(1960, 1, 1)
        while (LunarCalendar.solarToLunar(d, 7) == LunarCalendar.solarToLunar(d, 8)) d = d.plusDays(1)
        return d
    }

    @Test
    fun `canh bao lich UTC+7 va UTC+8 truoc 1967`() {
        val d = firstPre1967DisagreeingDate()
        val chart = ChartEngine.build(BirthInput(LocalDateTime.of(d, LocalTime.NOON), null, Gender.NU))
        assertEquals(8, chart.birth.calendarTz)
        assertEquals(LunarCalendar.solarToLunar(d, 8), chart.lunar)
        val w = chart.warning(WarningCode.CALENDAR_TZ)
        val alt = ChartEngine.buildAlternative(chart, w)
        assertEquals(7, alt.birth.calendarTz)
        assertEquals(LunarCalendar.solarToLunar(d, 7), alt.lunar)
        assertNotEquals(chart.lunar, alt.lunar)
    }

    @Test
    fun `Tet Mau Than 1968 lech ngay hai mien`() {
        val north = ChartEngine.build(solar(1968, 1, 29, 12, 0, region = BirthRegion.VN_BAC))
        val south = ChartEngine.build(solar(1968, 1, 29, 12, 0, region = BirthRegion.VN_NAM, zone = ZoneId.of("Asia/Ho_Chi_Minh")))
        assertEquals("1/1/1968", north.lunar.toString())
        assertEquals("Mậu Thân", north.pillars.year.vn)
        assertEquals(1967, south.lunar.year) // miền Nam còn ở tháng Chạp Đinh Mùi
        assertTrue(WarningCode.CALENDAR_TZ !in north.codes(), "miền Bắc sau 08/08/1967 không mơ hồ")
        assertTrue(WarningCode.CALENDAR_TZ in south.codes())
    }

    @Test
    fun `gio sat ranh gioi canh gio`() {
        assertEquals(CanhGio.NGO, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(10, 55)))
        assertEquals(CanhGio.TI, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(11, 5)))
        assertEquals(CanhGio.TI, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(11, 10)))
        assertNull(BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(11, 11)))
        assertEquals(CanhGio.TY_MUON, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(22, 55)))
        assertEquals(CanhGio.HOI, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(23, 5)))
        assertEquals(CanhGio.SUU, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(0, 55)))
        assertEquals(CanhGio.TY_SOM, BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(1, 0)))
        assertNull(BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(0, 5)), "nửa đêm không phải ranh giới canh")
        assertNull(BirthNormalizer.nearBoundaryNeighbor(LocalTime.of(23, 55)))

        // 22:55 → lá số thay thế giờ Tý muộn phải sang ngày hôm sau.
        val chart = ChartEngine.build(solar(2001, 5, 10, 22, 55))
        assertEquals(Chi.HOI, chart.hourChi)
        val alt = ChartEngine.buildAlternative(chart, chart.warning(WarningCode.NEAR_HOUR_BOUNDARY))
        assertEquals(Chi.TY, alt.hourChi)
        assertEquals(LocalDate.of(2001, 5, 11), alt.birth.calendarDate)
    }

    @Test
    fun `quy doi gio dong ho mien Nam giai doan UTC+8`() {
        // 1965 miền Nam dùng giờ UTC+8: 07:30 ghi trên giấy = 06:30 UTC+7 → đổi từ Thìn sang Mão.
        val input = solar(1965, 6, 15, 7, 30, region = BirthRegion.VN_NAM, zone = ZoneId.of("Asia/Ho_Chi_Minh"))
        val chart = ChartEngine.build(input)
        assertEquals(LocalTime.of(6, 30), chart.birth.referenceTime!!.toLocalTime())
        assertEquals(Chi.MAO, chart.hourChi)
        val alt = ChartEngine.buildAlternative(chart, chart.warning(WarningCode.CLOCK_CONVERTED))
        assertEquals(Chi.THIN, alt.hourChi)
    }

    @Test
    fun `sinh o nuoc ngoai dung gio chuan dia phuong bo DST`() {
        // 11:30 giờ mùa hè New York (EDT) = 10:30 giờ chuẩn EST → Tỵ thay vì Ngọ.
        val input = solar(1995, 7, 1, 11, 30, region = BirthRegion.NUOC_NGOAI, zone = ZoneId.of("America/New_York"))
        val chart = ChartEngine.build(input)
        assertEquals(LocalDateTime.of(1995, 7, 1, 10, 30), chart.birth.referenceTime)
        assertEquals(Chi.TI, chart.hourChi)
        assertTrue(WarningCode.CLOCK_CONVERTED in chart.codes())
        // Mùa đông không có DST: không đổi giờ, không cảnh báo.
        val winter = ChartEngine.build(input.copy(solar = LocalDateTime.of(1995, 1, 10, 11, 30)))
        assertEquals(Chi.NGO, winter.hourChi)
        assertTrue(winter.warnings.isEmpty())
    }

    @Test
    fun `gio Mat Troi that`() {
        // TP.HCM 106,7°E, đầu tháng 11 (EoT ≈ +16 phút): 10:50 → khoảng 11:13 → sang giờ Ngọ.
        val input = solar(2000, 11, 3, 10, 50, region = BirthRegion.VN_NAM, zone = ZoneId.of("Asia/Ho_Chi_Minh"))
            .copy(longitudeDeg = 106.7, useTrueSolarTime = true)
        val chart = ChartEngine.build(input)
        assertEquals(Chi.NGO, chart.hourChi)
        val alt = ChartEngine.buildAlternative(chart, chart.warning(WarningCode.TRUE_SOLAR_SHIFT))
        assertEquals(Chi.TI, alt.hourChi)
    }

    @Test
    fun `nhap am lich cho cung la so voi nhap duong lich`() = Prop.forAll(1_000, gen = { r -> RandomBirths.solarDate(r) to r.nextLong(0, 86_400) }) { (date, sec) ->
        val time = LocalTime.ofSecondOfDay(sec)
        val tz = LunarCalendar.calendarTzFor(date, BirthRegion.VN_BAC)
        val bySolar = ChartEngine.build(BirthInput(LocalDateTime.of(date, time), null, Gender.NU))
        val byLunar = ChartEngine.build(BirthInput(null, LunarCalendar.solarToLunar(date, tz), Gender.NU, lunarClock = time))
        assertEquals(bySolar.lunar, byLunar.lunar)
        assertEquals(bySolar.starPositions, byLunar.starPositions)
    }

    @Test
    fun `ngay am chi co o lich con lai thi dung lich do va canh bao`() {
        // 30/7/1950 có trong lịch UTC+7 nhưng tháng 7 năm đó chỉ có 29 ngày ở lịch UTC+8 (lịch của thời kỳ trước 1967).
        val lunar = LunarDate(30, 7, 1950, false)
        assertNull(LunarCalendar.lunarToSolar(lunar, 8))
        val chart = ChartEngine.build(BirthInput(null, lunar, Gender.NU, canh = CanhGio.TY_SOM))
        assertEquals(lunar, chart.lunar)
        assertEquals(7, chart.birth.calendarTz)
        assertTrue(WarningCode.CALENDAR_TZ in chart.codes())
        // Ép lịch UTC+8 thì báo lỗi rõ ràng.
        assertThrows(InvalidBirthInputException::class.java) {
            ChartEngine.build(BirthInput(null, lunar, Gender.NU, canh = CanhGio.TY_SOM, calendarTzOverride = 8))
        }
    }

    @Test
    fun `che do chon canh gio`() {
        val exact = ChartEngine.build(solar(1985, 3, 3, 23, 30, Gender.NU))
        val byCanh = ChartEngine.build(BirthInput(LocalDateTime.of(1985, 3, 3, 12, 0), null, Gender.NU, canh = CanhGio.TY_MUON))
        assertEquals(exact.starPositions, byCanh.starPositions)
        assertNull(byCanh.birth.referenceTime)
        val all = ChartEngine.buildAllHours(byCanh.input)
        assertEquals(12, all.size)
        assertEquals(Chi.entries.toSet(), all.values.map { it.hourChi }.toSet())
    }

    @Test
    fun `ngay am khong ton tai bao loi de hieu`() {
        val e1 = assertThrows(InvalidBirthInputException::class.java) {
            ChartEngine.build(BirthInput(null, LunarDate(1, 3, 2026, true), Gender.NAM, lunarClock = LocalTime.NOON))
        }
        assertTrue(e1.message!!.contains("không có tháng nhuận"), e1.message)
        val shortMonth = (1..12).first { LunarCalendar.daysInLunarMonth(it, 2026, false) == 29 }
        assertThrows(InvalidBirthInputException::class.java) {
            ChartEngine.build(BirthInput(null, LunarDate(30, shortMonth, 2026, false), Gender.NAM, lunarClock = LocalTime.NOON))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BirthInput(null, null, Gender.NAM)
        }
        // Ngoài khoảng 1900–2100.
        val outOfRange = assertThrows(InvalidBirthInputException::class.java) { ChartEngine.build(solar(1899, 12, 31, 12, 0)) }
        assertTrue(outOfRange.message!!.contains("1900"))
        assertThrows(InvalidBirthInputException::class.java) { ChartEngine.build(solar(2101, 1, 1, 12, 0)) }
        // Biên trong khoảng vẫn lập được, kể cả giờ Tý muộn ngày cuối cùng.
        ChartEngine.build(solar(1900, 1, 1, 0, 30))
        ChartEngine.build(solar(2100, 12, 31, 23, 30))
    }

    @Test
    fun `cache tra ve cung lo so`() {
        val cache = ChartCache(maxSize = 2)
        val a = solar(1990, 8, 15, 10, 30)
        val first = cache.get(a)
        assertTrue(first === cache.get(a))
        cache.get(solar(1991, 1, 1, 1, 0))
        cache.get(solar(1992, 1, 1, 1, 0))
        assertEquals(2, cache.size)
        assertEquals(first, cache.get(a)) // bị đẩy ra rồi lập lại: vẫn bằng nhau vì engine thuần
    }
}
