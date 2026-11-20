package vn.tuvi.core.engine

import vn.tuvi.core.calendar.BirthRegion
import vn.tuvi.core.calendar.LunarCalendar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Bộ chạy property test tối giản: sinh mẫu ngẫu nhiên có seed cố định, báo seed + mẫu hỏng khi thất bại.
 * (Kotest property chưa dùng ở Giai đoạn 1 — xem docs/DECISIONS.md D-12; đổi sang `checkAll` chỉ cần thay hàm này.)
 */
object Prop {
    const val DEFAULT_SEED = 20261120L

    fun <T> forAll(iterations: Int, seed: Long = DEFAULT_SEED, gen: (Random) -> T, property: (T) -> Unit) {
        val random = Random(seed)
        repeat(iterations) { i ->
            val sample = gen(random)
            try {
                property(sample)
            } catch (e: Throwable) {
                throw AssertionError("Hỏng ở mẫu #$i (seed=$seed): $sample\n→ ${e.message}", e)
            }
        }
    }
}

/** Sinh đầu vào ngẫu nhiên phủ 1900–2100, cả hai giới, ba vùng, nhập dương/âm, chế độ canh giờ và giờ Mặt Trời. */
object RandomBirths {
    private val START = LocalDate.of(1900, 1, 1).toEpochDay()
    private val END = LocalDate.of(2100, 12, 31).toEpochDay()
    private val FOREIGN_ZONES = listOf("America/Los_Angeles", "America/New_York", "Europe/Paris", "Asia/Tokyo", "Australia/Sydney")
        .map { ZoneId.of(it) }

    fun solarDate(r: Random): LocalDate = LocalDate.ofEpochDay(r.nextLong(START, END + 1))

    fun input(r: Random): BirthInput {
        val date = solarDate(r)
        val time = LocalTime.ofSecondOfDay(r.nextLong(0, 86_400))
        val gender = if (r.nextBoolean()) Gender.NAM else Gender.NU
        val (region, zone) = when (r.nextInt(10)) {
            in 0..5 -> BirthRegion.VN_BAC to VN_STANDARD_ZONE
            in 6..7 -> BirthRegion.VN_NAM to ZoneId.of("Asia/Ho_Chi_Minh")
            else -> BirthRegion.NUOC_NGOAI to FOREIGN_ZONES[r.nextInt(FOREIGN_ZONES.size)]
        }
        val canh = if (r.nextInt(8) == 0) CanhGio.entries[r.nextInt(CanhGio.entries.size)] else null
        val trueSolar = r.nextInt(10) == 0
        val longitude = if (trueSolar) r.nextDouble(102.0, 110.0) else null
        return if (r.nextInt(6) == 0) {
            val tz = LunarCalendar.calendarTzFor(date, region)
            BirthInput(
                solar = null,
                lunar = LunarCalendar.solarToLunar(date, tz),
                gender = gender,
                zone = zone,
                lunarClock = if (canh == null) time else null,
                canh = canh,
                region = region,
                longitudeDeg = longitude,
                useTrueSolarTime = trueSolar,
            )
        } else {
            BirthInput(
                solar = LocalDateTime.of(date, time),
                lunar = null,
                gender = gender,
                zone = zone,
                canh = canh,
                region = region,
                longitudeDeg = longitude,
                useTrueSolarTime = trueSolar,
            )
        }
    }

    fun config(r: Random): SchoolConfig = SchoolConfig(
        tuHoa = TuHoaVariant.entries.random(r),
        nhamKhoa = NhamHoaKhoa.entries.random(r),
        khoiViet = KhoiVietVariant.entries.random(r),
        hoaLinh = HoaLinhVariant.entries.random(r),
        thienGiai = ThienGiaiVariant.entries.random(r),
        luuHa = LuuHaVariant.entries.random(r),
        leapMonth = LeapMonthRule.entries.random(r),
        dayBoundary = DayBoundary.entries.random(r),
        nguyetHan = NguyetHanVariant.entries.random(r),
        cuMonTable = CuMonTable.entries.random(r),
        daiHanTuHoa = DaiHanTuHoa.entries.random(r),
        thanChuTy = ThanChuTy.entries.random(r),
    )
}
