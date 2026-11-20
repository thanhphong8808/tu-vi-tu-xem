package vn.tuvi.core.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Giờ Mặt Trời thật (tùy chọn, mặc định tắt):
 * t_solar = t_clock + 4 phút × (kinh độ − 15 × offsetGiờ) + EoT.
 * Ví dụ TP.HCM (106,7°E) lệch +6,8 phút so với kinh tuyến 105° trước khi cộng EoT.
 */
object TrueSolarTime {

    /** Phương trình thời gian (phút), công thức xấp xỉ sai số dưới khoảng 1 phút. */
    fun equationOfTimeMinutes(date: LocalDate): Double {
        val b = 2 * PI * (date.dayOfYear - 81) / 364.0
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }

    /** Độ lệch kinh độ so với kinh tuyến chuẩn của múi giờ (phút). */
    fun longitudeCorrectionMinutes(longitudeDeg: Double, utcOffsetHours: Double): Double =
        4.0 * (longitudeDeg - 15.0 * utcOffsetHours)

    fun correctionMinutes(date: LocalDate, longitudeDeg: Double, utcOffsetHours: Double): Double =
        longitudeCorrectionMinutes(longitudeDeg, utcOffsetHours) + equationOfTimeMinutes(date)

    fun apply(clock: LocalDateTime, longitudeDeg: Double, utcOffsetHours: Double): LocalDateTime {
        val seconds = (correctionMinutes(clock.toLocalDate(), longitudeDeg, utcOffsetHours) * 60).roundToLong()
        return clock.plusSeconds(seconds)
    }
}
