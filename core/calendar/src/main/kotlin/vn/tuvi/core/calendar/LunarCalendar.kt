package vn.tuvi.core.calendar

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Ngày âm lịch. [year] là năm âm (vd. sinh 10/02/1964 dương → năm âm 1963). */
data class LunarDate(val day: Int, val month: Int, val year: Int, val isLeap: Boolean) {
    init {
        require(day in 1..30) { "Ngày âm phải trong 1..30: $day" }
        require(month in 1..12) { "Tháng âm phải trong 1..12: $month" }
    }

    /** Dạng “25/6/1990”, tháng nhuận ghi “1/2N/2023”. */
    override fun toString(): String = "$day/$month${if (isLeap) "N" else ""}/$year"
}

/** Nơi sinh — quyết định múi giờ lịch giai đoạn 1967–1975 và cách quy đổi giờ đồng hồ. */
enum class BirthRegion(val vn: String) {
    VN_BAC("Miền Bắc"),
    VN_NAM("Miền Nam"),
    NUOC_NGOAI("Nước ngoài"),
}

/**
 * Lịch âm Việt Nam theo thuật toán Hồ Ngọc Đức (bản gốc amlich.js), port nguyên văn.
 *
 * Mọi phép làm tròn dùng floor() đúng như INT() của bản gốc — không dùng toInt() vì nó cắt về 0 với số âm.
 * Múi giờ [tz] tính bằng giờ: Việt Nam dùng 7 từ 08/08/1967 (miền Bắc) và từ 1975 (miền Nam), trước đó là 8.
 */
object LunarCalendar {

    private const val NEW_MOON_EPOCH = 2415021.076998695
    private const val SYNODIC_MONTH = 29.530588853

    /** Mốc miền Bắc chuyển lịch từ UTC+8 sang UTC+7. */
    val NORTH_SWITCH_DATE: LocalDate = LocalDate.of(1967, 8, 8)

    /** Ngày cuối miền Nam còn dùng lịch UTC+8. */
    val SOUTH_LAST_UTC8_DATE: LocalDate = LocalDate.of(1975, 4, 30)

    private fun int(d: Double): Int = floor(d).toInt()

    fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = int((14 - mm) / 12.0)
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + int((153 * m + 2) / 5.0) + 365 * y + int(y / 4.0) - int(y / 100.0) + int(y / 400.0) - 32045
        if (jd < 2299161) {
            jd = dd + int((153 * m + 2) / 5.0) + 365 * y + int(y / 4.0) - 32083
        }
        return jd
    }

    fun jdFromDate(date: LocalDate): Int = jdFromDate(date.dayOfMonth, date.monthValue, date.year)

    fun jdToDate(jd: Int): LocalDate {
        val b: Int
        val c: Int
        if (jd > 2299160) {
            val a = jd + 32044
            b = int((4 * a + 3) / 146097.0)
            c = a - int((b * 146097) / 4.0)
        } else {
            b = 0
            c = jd + 32082
        }
        val d = int((4 * c + 3) / 1461.0)
        val e = c - int((1461 * d) / 4.0)
        val m = int((5 * e + 2) / 153.0)
        val day = e - int((153 * m + 2) / 5.0) + 1
        val month = m + 3 - 12 * int(m / 10.0)
        val year = b * 100 + d - 4800 + int(m / 10.0)
        return LocalDate.of(year, month, day)
    }

    /** Thời điểm sóc (Julius, giờ UTC) thứ k tính từ sóc 1900-01-01. */
    internal fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltat
    }

    /** Hoàng kinh Mặt Trời (radian, 0..2π) tại thời điểm Julius [jdn]. */
    internal fun sunLongitude(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * int(l / (PI * 2))
        return l
    }

    /** Cung hoàng đạo (0..11, mỗi cung 30°) của Mặt Trời lúc nửa đêm đầu ngày [dayNumber] theo giờ địa phương. */
    internal fun getSunLongitude(dayNumber: Int, tz: Int): Int =
        int(sunLongitude(dayNumber - 0.5 - tz / 24.0) / PI * 6)

    internal fun getNewMoonDay(k: Int, tz: Int): Int = int(newMoon(k) + 0.5 + tz / 24.0)

    internal fun getLunarMonth11(yy: Int, tz: Int): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = int(off / SYNODIC_MONTH)
        var nm = getNewMoonDay(k, tz)
        val sunLong = getSunLongitude(nm, tz)
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, tz)
        }
        return nm
    }

    internal fun getLeapMonthOffset(a11: Int, tz: Int): Int {
        val k = int((a11 - NEW_MOON_EPOCH) / SYNODIC_MONTH + 0.5)
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, tz), tz)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, tz), tz)
        } while (arc != last && i < 14)
        return i - 1
    }

    /** Dương lịch → âm lịch Việt Nam. */
    fun solarToLunar(date: LocalDate, tz: Int = 7): LunarDate {
        val dayNumber = jdFromDate(date)
        var k = int((dayNumber - NEW_MOON_EPOCH) / SYNODIC_MONTH)
        var monthStart = getNewMoonDay(k + 1, tz)
        // Bản gốc chỉ lùi một lần (`if`). Khi sóc thật đến muộn hơn sóc trung bình, getNewMoonDay(k) vẫn có thể
        // rơi sau dayNumber và bản gốc trả về “ngày 0” (vd. 07/05/2054 tz7, 19/04/1939 tz8 — đã đối chiếu amlich.js).
        // Lùi tiếp cho tới khi mùng 1 không vượt quá ngày đang đổi. Xem docs/DECISIONS.md, D-03.
        while (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, tz)
            k--
        }
        val yy = date.year
        var a11 = getLunarMonth11(yy, tz)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1, tz)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1, tz)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = int((monthStart - a11) / 29.0)
        var lunarLeap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, tz)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = true
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }
        return LunarDate(lunarDay, lunarMonth, lunarYear, lunarLeap)
    }

    /** Ngày Julius của mùng 1 tháng âm [lunarMonth] (nhuận nếu [leap]) năm [lunarYear]; null nếu tháng không tồn tại. */
    private fun monthStartJd(lunarMonth: Int, lunarYear: Int, leap: Boolean, tz: Int): Int? {
        val a11: Int
        val b11: Int
        if (lunarMonth < 11) {
            a11 = getLunarMonth11(lunarYear - 1, tz)
            b11 = getLunarMonth11(lunarYear, tz)
        } else {
            a11 = getLunarMonth11(lunarYear, tz)
            b11 = getLunarMonth11(lunarYear + 1, tz)
        }
        val k = int(0.5 + (a11 - NEW_MOON_EPOCH) / SYNODIC_MONTH)
        var off = lunarMonth - 11
        if (off < 0) off += 12
        if (b11 - a11 > 365) {
            val leapOff = getLeapMonthOffset(a11, tz)
            var leapMonth = leapOff - 2
            if (leapMonth < 0) leapMonth += 12
            if (leap && lunarMonth != leapMonth) {
                return null
            } else if (leap || off >= leapOff) {
                off += 1
            }
        } else if (leap) {
            return null
        }
        return getNewMoonDay(k + off, tz)
    }

    /** Số ngày (29 hoặc 30) của tháng âm; null nếu tháng không tồn tại. */
    fun daysInLunarMonth(lunarMonth: Int, lunarYear: Int, leap: Boolean, tz: Int = 7): Int? {
        val start = monthStartJd(lunarMonth, lunarYear, leap, tz) ?: return null
        // Ngày thứ 30 kể từ mùng 1: hoặc vẫn là ngày 30 của tháng này (tháng đủ), hoặc đã sang mùng 1 tháng sau.
        return if (solarToLunar(jdToDate(start + 29), tz).day == 30) 30 else 29
    }

    /** Âm lịch → dương lịch; null nếu ngày âm không tồn tại (tháng nhuận sai, ngày 30 của tháng thiếu…). */
    fun lunarToSolar(lunar: LunarDate, tz: Int = 7): LocalDate? {
        val start = monthStartJd(lunar.month, lunar.year, lunar.isLeap, tz) ?: return null
        val date = jdToDate(start + lunar.day - 1)
        // Chặn ngày 30 của tháng thiếu: quy đổi ngược phải ra đúng ngày đã nhập.
        return if (solarToLunar(date, tz) == lunar) date else null
    }

    /** Tháng nhuận của năm âm [lunarYear] (1..12), hoặc 0 nếu năm không nhuận. */
    fun leapMonthOf(lunarYear: Int, tz: Int = 7): Int =
        (1..12).firstOrNull { monthStartJd(it, lunarYear, true, tz) != null } ?: 0

    /**
     * Múi giờ lịch theo thời kỳ: trước 08/08/1967 dùng 8; từ 08/08/1967 đến 30/04/1975 miền Bắc dùng 7,
     * miền Nam dùng 8; sau đó dùng 7. Sinh ở nước ngoài trong 1967–1975 mặc định dùng 7 (xem docs/DECISIONS.md).
     */
    fun calendarTzFor(date: LocalDate, region: BirthRegion): Int = when {
        date.isBefore(NORTH_SWITCH_DATE) -> 8
        !date.isAfter(SOUTH_LAST_UTC8_DATE) -> if (region == BirthRegion.VN_NAM) 8 else 7
        else -> 7
    }

    /** Ngày [date] thuộc giai đoạn lịch có thể tính theo UTC+7 hoặc UTC+8 tùy nơi/nguồn. */
    fun isTzAmbiguousPeriod(date: LocalDate, region: BirthRegion): Boolean = when {
        date.isBefore(NORTH_SWITCH_DATE) -> true
        !date.isAfter(SOUTH_LAST_UTC8_DATE) -> region != BirthRegion.VN_BAC
        else -> false
    }

    /** Can chi của một ngày dương lịch. */
    fun dayCanChi(date: LocalDate): CanChi = CanChi.ofDay(jdFromDate(date))
}
