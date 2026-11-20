package vn.tuvi.core.calendar.tools

import vn.tuvi.core.calendar.LunarCalendar
import java.time.LocalDate

/**
 * Công cụ dev cho tools/crosscheck/calendar_crosscheck.js: in mọi ngày 1900–2100 với tz 7 và 8,
 * mỗi dòng “d m y tz ngàyÂm thángÂm nămÂm nhuận”.
 */
fun main() {
    val out = StringBuilder()
    for (tz in listOf(7, 8)) {
        var d = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        while (!d.isAfter(end)) {
            val l = LunarCalendar.solarToLunar(d, tz)
            out.append("${d.dayOfMonth} ${d.monthValue} ${d.year} $tz ${l.day} ${l.month} ${l.year} ${if (l.isLeap) 1 else 0}\n")
            d = d.plusDays(1)
        }
    }
    print(out)
}
