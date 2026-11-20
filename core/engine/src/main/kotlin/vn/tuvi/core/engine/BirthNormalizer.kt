package vn.tuvi.core.engine

import vn.tuvi.core.calendar.BirthRegion
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.TrueSolarTime
import vn.tuvi.core.calendar.hourChiOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Đầu vào không hợp lệ (ngày âm không tồn tại, tháng nhuận sai…). Thông điệp hiển thị được cho người dùng. */
class InvalidBirthInputException(message: String) : IllegalArgumentException(message)

/**
 * Chuẩn hóa đầu vào theo mục 4: giờ đồng hồ → giờ tham chiếu → canh giờ → ngày dương dùng đổi lịch → âm lịch.
 * Hàm thuần: cùng (input, config) luôn cho cùng kết quả.
 */
internal object BirthNormalizer {

    data class Result(val birth: NormalizedBirth, val warnings: List<ChartWarning>)

    /** Ngưỡng “sát ranh giới canh giờ” (giây, tính cả hai đầu). */
    private const val NEAR_BOUNDARY_SECONDS = 10 * 60

    private val NOON: LocalTime = LocalTime.NOON

    /** Khoảng năm dương lịch được hỗ trợ (đã kiểm vét cạn; thuật toán lịch còn đúng ngoài khoảng này nhưng chưa kiểm). */
    val SUPPORTED_YEARS: IntRange = 1900..2100

    fun normalize(input: BirthInput, config: SchoolConfig): Result {
        val warnings = mutableListOf<ChartWarning>()

        // 1. Ngày dân lịch (dương) + giờ đồng hồ.
        val lunarInputTz: Int?
        val civilDate: LocalDate
        if (input.solar != null) {
            civilDate = input.solar.toLocalDate()
            lunarInputTz = null
        } else {
            val civil = lunarToCivil(input)
            civilDate = civil.date
            lunarInputTz = civil.tz
            civil.fellBackFrom?.let { periodTz ->
                warnings += ChartWarning(
                    WarningCode.CALENDAR_TZ,
                    "Ngày âm ${input.lunar} không có trong lịch UTC+$periodTz của thời kỳ này; " +
                        "đã lập lá số theo lịch UTC+${civil.tz}. Hãy kiểm tra lại ngày âm trên giấy tờ.",
                )
            }
        }
        if (civilDate.year !in SUPPORTED_YEARS) {
            throw InvalidBirthInputException("Ứng dụng hỗ trợ ngày sinh từ 01/01/1900 đến 31/12/2100.")
        }
        val clock: LocalTime? = if (input.canh != null) null else (input.solar?.toLocalTime() ?: input.lunarClock)

        // 2. Giờ tham chiếu và canh giờ.
        val referenceTime: LocalDateTime?
        val hourChi = if (clock != null) {
            val zoned = ZonedDateTime.ofLocal(LocalDateTime.of(civilDate, clock), input.zone, null)
            val targetOffset: ZoneOffset = if (input.region == BirthRegion.NUOC_NGOAI) {
                input.zone.rules.getStandardOffset(zoned.toInstant())
            } else {
                ZoneOffset.ofHours(7)
            }
            var local = zoned.toInstant().atOffset(targetOffset).toLocalDateTime()

            if (CanhGio.of(local.toLocalTime()) != CanhGio.of(clock)) {
                warnings += ChartWarning(
                    WarningCode.CLOCK_CONVERTED,
                    "Giờ ghi ${clock} được quy về ${local.toLocalTime()} (${offsetLabel(targetOffset)}) nên đổi canh giờ " +
                        "từ ${CanhGio.of(clock).chi.vn} sang ${CanhGio.of(local.toLocalTime()).chi.vn}. " +
                        "Có thể xem lá số theo giờ ghi nguyên văn.",
                    altInput = input.copy(zone = targetOffset),
                )
            }

            if (input.useTrueSolarTime && input.longitudeDeg != null) {
                val corrected = TrueSolarTime.apply(local, input.longitudeDeg, targetOffset.totalSeconds / 3600.0)
                if (CanhGio.of(corrected.toLocalTime()) != CanhGio.of(local.toLocalTime())) {
                    warnings += ChartWarning(
                        WarningCode.TRUE_SOLAR_SHIFT,
                        "Giờ Mặt Trời thật là ${corrected.toLocalTime().withNano(0)}, khác canh giờ đồng hồ. " +
                            "Có thể xem lá số theo giờ đồng hồ.",
                        altInput = input.copy(useTrueSolarTime = false),
                    )
                }
                local = corrected
            }

            nearBoundaryNeighbor(local.toLocalTime())?.let { neighbor ->
                warnings += ChartWarning(
                    WarningCode.NEAR_HOUR_BOUNDARY,
                    "Giờ sinh ${local.toLocalTime().withNano(0)} nằm trong 10 phút sát ranh giới canh giờ. " +
                        "Có thể xem lá số theo giờ ${neighbor.vn}.",
                    altInput = input.copy(
                        solar = LocalDateTime.of(local.toLocalDate(), NOON),
                        lunar = null,
                        lunarClock = null,
                        zone = targetOffset,
                        useTrueSolarTime = false,
                        canh = neighbor,
                    ),
                )
            }
            referenceTime = local
            hourChiOf(local.hour)
        } else {
            referenceTime = null
            input.canh!!.chi
        }
        val lateTy = referenceTime?.let { it.hour == 23 } ?: input.canh!!.isLateTy
        val baseDate = referenceTime?.toLocalDate() ?: civilDate

        // 3. Đổi ngày lúc 23:00 (mặc định) hoặc giữ ngày.
        val calendarDate = if (lateTy && config.dayBoundary == DayBoundary.DOI_NGAY_23H) baseDate.plusDays(1) else baseDate
        if (lateTy) {
            val other = if (config.dayBoundary == DayBoundary.DOI_NGAY_23H) DayBoundary.GIU_NGAY else DayBoundary.DOI_NGAY_23H
            warnings += ChartWarning(
                WarningCode.TY_MUON,
                "Sinh giờ Tý muộn (23:00–23:59): lá số đang ${config.dayBoundary.vn.lowercase()}; " +
                    "có trường phái ${other.vn.lowercase()}.",
                altConfig = config.copy(dayBoundary = other),
            )
        }

        // 4. Múi giờ lịch theo thời kỳ và âm lịch.
        val tz = lunarInputTz ?: input.calendarTzOverride ?: LunarCalendar.calendarTzFor(calendarDate, input.region)
        val lunar = LunarCalendar.solarToLunar(calendarDate, tz)
        if (input.solar != null &&
            (input.calendarTzOverride != null || LunarCalendar.isTzAmbiguousPeriod(calendarDate, input.region))
        ) {
            val otherTz = if (tz == 7) 8 else 7
            val alt = LunarCalendar.solarToLunar(calendarDate, otherTz)
            if (alt != lunar) {
                warnings += ChartWarning(
                    WarningCode.CALENDAR_TZ,
                    "Ngày $calendarDate theo lịch UTC+$tz là $lunar, theo lịch UTC+$otherTz là $alt. " +
                        "Âm lịch Việt Nam trước 1967 (miền Bắc) và trước 1975 (miền Nam) tính theo UTC+8.",
                    altInput = input.copy(calendarTzOverride = otherTz),
                )
            }
        }
        if (lunar.isLeap) {
            val other = if (config.leapMonth == LeapMonthRule.SAME_MONTH) LeapMonthRule.SPLIT_15 else LeapMonthRule.SAME_MONTH
            warnings += ChartWarning(
                WarningCode.LEAP_MONTH,
                "Sinh tháng ${lunar.month} nhuận: lá số đang ${config.leapMonth.vn.lowercase()}; " +
                    "có thể xem theo cách ${other.vn.lowercase()}.",
                altConfig = config.copy(leapMonth = other),
            )
        }

        return Result(NormalizedBirth(referenceTime, calendarDate, tz, lunar, hourChi, lateTy), warnings)
    }

    private data class Civil(val date: LocalDate, val tz: Int, val fellBackFrom: Int?)

    /**
     * Ngày âm (dân lịch) → ngày dương cùng múi giờ lịch đã dùng. Nếu ngày âm không có trong lịch của thời kỳ
     * (vd. 30/7/1950 chỉ có ở lịch UTC+7, trong khi trước 1967 dùng UTC+8) thì dùng lịch còn lại — D-25.
     */
    private fun lunarToCivil(input: BirthInput): Civil {
        val l = input.lunar!!
        val probe = LunarCalendar.lunarToSolar(l, 7) ?: LunarCalendar.lunarToSolar(l, 8)
            ?: throw InvalidBirthInputException(describeInvalid(l.toString(), l.isLeap, l.year))
        val tz = input.calendarTzOverride ?: LunarCalendar.calendarTzFor(probe, input.region)
        LunarCalendar.lunarToSolar(l, tz)?.let { return Civil(it, tz, null) }
        if (input.calendarTzOverride != null) {
            throw InvalidBirthInputException("Ngày âm $l không tồn tại theo lịch UTC+$tz.")
        }
        val other = if (tz == 7) 8 else 7
        return Civil(LunarCalendar.lunarToSolar(l, other)!!, other, tz)
    }

    private fun describeInvalid(label: String, leap: Boolean, year: Int): String {
        if (leap) {
            val leapMonth = LunarCalendar.leapMonthOf(year)
            return if (leapMonth == 0) "Năm âm $year không có tháng nhuận."
            else "Năm âm $year nhuận tháng $leapMonth, ngày $label không tồn tại."
        }
        return "Ngày âm $label không tồn tại (tháng thiếu chỉ có 29 ngày)."
    }

    private fun offsetLabel(offset: ZoneOffset): String = if (offset.totalSeconds == 0) "UTC" else "UTC${offset.id}"

    /**
     * Canh giờ liền kề nếu [t] cách ranh giới hai canh (01:00, 03:00 … 23:00) không quá 10 phút, ngược lại null.
     * Nửa đêm không phải ranh giới canh (Tý muộn → Tý sớm cùng là giờ Tý).
     */
    internal fun nearBoundaryNeighbor(t: LocalTime): CanhGio? {
        val x = Math.floorMod(t.toSecondOfDay() - 3600, 7200)
        val current = CanhGio.of(t)
        return when {
            x <= NEAR_BOUNDARY_SECONDS -> CanhGio.entries[current.ordinal - 1]
            7200 - x <= NEAR_BOUNDARY_SECONDS -> CanhGio.entries[current.ordinal + 1]
            else -> null
        }
    }
}
