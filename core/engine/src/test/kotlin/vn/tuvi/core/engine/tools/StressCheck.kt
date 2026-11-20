package vn.tuvi.core.engine.tools

import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.ChartChecks
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LeapMonthRule
import vn.tuvi.core.engine.SchoolConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.LongStream
import kotlin.system.exitProcess

/**
 * Kiểm vét cạn (công cụ dev, chạy vài phút): mọi ngày dương 1900–2100 × 13 canh giờ × 2 giới, và mọi ngày âm
 * 1900–2099 (kể cả tháng nhuận, ngày 30) với cả hai quy tắc tháng nhuận — đều qua bộ bất biến mục 14.2 và kiểm vận hạn.
 * Dùng: ./gradlew :core:engine:stressCheck
 */
fun main() {
    val charts = AtomicLong()
    val failures = AtomicLong()
    val firstFailure = java.util.concurrent.atomic.AtomicReference<String?>(null)
    val config = SchoolConfig.DEFAULT
    val started = System.nanoTime()

    fun check(input: BirthInput, cfg: SchoolConfig) {
        try {
            val chart = ChartEngine.build(input, cfg)
            ChartChecks.assertInvariants(chart, cfg)
            ChartChecks.assertYearLimits(chart, chart.lunar.year + 29)
            charts.incrementAndGet()
        } catch (e: Throwable) {
            failures.incrementAndGet()
            firstFailure.compareAndSet(null, "$input / $cfg → ${e.message}")
        }
    }

    // 1. Mọi ngày dương × 13 canh × 2 giới.
    val first = LocalDate.of(1900, 1, 1).toEpochDay()
    val last = LocalDate.of(2100, 12, 31).toEpochDay()
    LongStream.rangeClosed(first, last).parallel().forEach { day ->
        val date = LocalDate.ofEpochDay(day)
        for (canh in CanhGio.entries) for (g in Gender.entries) {
            check(BirthInput(LocalDateTime.of(date, LocalTime.NOON), null, g, canh = canh), config)
        }
    }
    val solarCharts = charts.get()
    println("Ngày dương 1900–2100 × 13 canh × 2 giới: $solarCharts lá số")

    // 2. Mọi ngày âm của cả lịch UTC+7 lẫn UTC+8 (kể cả tháng nhuận, ngày 30) × hai quy tắc tháng nhuận.
    val split = config.copy(leapMonth = LeapMonthRule.SPLIT_15)
    LongStream.rangeClosed(1900, 2099).parallel().forEach { y ->
        val year = y.toInt()
        val seen = HashSet<LunarDate>()
        for (tz in listOf(7, 8)) {
            val leap = LunarCalendar.leapMonthOf(year, tz)
            for (month in 1..12) for (isLeap in listOf(false, true)) {
                if (isLeap && month != leap) continue
                val days = LunarCalendar.daysInLunarMonth(month, year, isLeap, tz) ?: error("Thiếu tháng $month/$year tz$tz")
                for (d in 1..days) {
                    val lunar = LunarDate(d, month, year, isLeap)
                    if (!seen.add(lunar)) continue
                    val input = BirthInput(null, lunar, Gender.NU, canh = CanhGio.TY_SOM)
                    check(input, config)
                    if (isLeap) check(input, split)
                }
            }
        }
    }
    val lunarCharts = charts.get() - solarCharts
    println("Ngày âm 1900–2099 (lịch UTC+7 và UTC+8, kể cả tháng nhuận): $lunarCharts lá số")

    // 3. Giờ chính xác sát ranh giới canh, giờ Tý muộn, múi giờ lịch sử miền Nam (Asia/Ho_Chi_Minh) cho mọi ngày.
    val saigon = java.time.ZoneId.of("Asia/Ho_Chi_Minh")
    val times = listOf(LocalTime.of(0, 55), LocalTime.of(1, 5), LocalTime.of(22, 55), LocalTime.of(23, 30))
    LongStream.rangeClosed(first, last).parallel().forEach { day ->
        val date = LocalDate.ofEpochDay(day)
        for (t in times) {
            check(BirthInput(LocalDateTime.of(date, t), null, Gender.NAM, zone = saigon, region = vn.tuvi.core.calendar.BirthRegion.VN_NAM), config)
        }
    }
    println("Giờ sát ranh giới, múi giờ miền Nam: ${charts.get() - solarCharts - lunarCharts} lá số")
    println("Tổng: ${charts.get()} lá số đạt, ${failures.get()} lỗi, %.1f giây".format((System.nanoTime() - started) / 1e9))
    firstFailure.get()?.let { println("Lỗi đầu tiên: $it") }
    if (failures.get() > 0) exitProcess(1)
}
