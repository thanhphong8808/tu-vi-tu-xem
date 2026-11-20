package vn.tuvi.core.engine.tools

import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.HoaLinhVariant
import vn.tuvi.core.engine.LuuHaVariant
import vn.tuvi.core.engine.NhamHoaKhoa
import vn.tuvi.core.engine.ThienGiaiVariant
import vn.tuvi.core.engine.TuHoaVariant
import vn.tuvi.core.engine.SchoolConfig
import java.time.LocalDate
import kotlin.random.Random

/**
 * Công cụ dev cho tools/crosscheck: sinh N lá số ngẫu nhiên (nhập âm lịch, bỏ tháng nhuận) và in JSON Lines.
 * Đối số: [số lá số] [seed] [preset: default | lasotuvi]. Preset `lasotuvi` bật các biến thể mà lasotuvi dùng
 * (Hỏa Linh kiểu Đắc Lộc, Lưu Hà Đinh Thìn/Canh Thân, Thiên Giải nhảy cung, Tứ Hóa Canh/Nhâm theo cụ Thiên Lương)
 * để so sánh phần còn lại.
 */
fun main(args: Array<String>) {
    val n = args.getOrNull(0)?.toInt() ?: 3000
    val seed = args.getOrNull(1)?.toLong() ?: 20261120L
    val config = when (args.getOrNull(2) ?: "lasotuvi") {
        "default" -> SchoolConfig.DEFAULT
        else -> SchoolConfig(
            hoaLinh = HoaLinhVariant.DAC_LOC,
            luuHa = LuuHaVariant.LASOTUVI,
            thienGiai = ThienGiaiVariant.NHAY_CUNG,
            tuHoa = TuHoaVariant.THIEN_LUONG,
            nhamKhoa = NhamHoaKhoa.THIEN_PHU,
        )
    }
    val r = Random(seed)
    val start = LocalDate.of(1900, 2, 1).toEpochDay()
    val end = LocalDate.of(2100, 12, 31).toEpochDay()
    val out = StringBuilder()
    var produced = 0
    while (produced < n) {
        val lunar = LunarCalendar.solarToLunar(LocalDate.ofEpochDay(r.nextLong(start, end)), 7)
        if (lunar.isLeap) continue
        val canh = CanhGio.TWELVE[r.nextInt(12)]
        val gender = if (r.nextBoolean()) Gender.NAM else Gender.NU
        val chart = ChartEngine.build(BirthInput(null, lunar, gender, canh = canh, calendarTzOverride = 7), config)
        out.append("{\"day\":").append(lunar.day).append(",\"month\":").append(lunar.month)
            .append(",\"year\":").append(lunar.year).append(",\"hour\":").append(canh.chi.index)
            .append(",\"gender\":\"").append(gender.name).append("\"")
            .append(",\"menh\":").append(chart.menh.index).append(",\"than\":").append(chart.than.index)
            .append(",\"cuc\":").append(chart.cuc.number)
        out.append(",\"daiHan\":{").append(chart.palaces.values.joinToString(",") { "\"${it.chi.index}\":${it.daiHanStartAge}" }).append("}")
        out.append(",\"tieuHan\":{").append(chart.palaces.values.joinToString(",") { "\"${it.chi.index}\":${it.tieuHanChi.index}" }).append("}")
        out.append(",\"stars\":{").append(chart.starPositions.entries.joinToString(",") { "\"${it.key.name}\":${it.value.index}" }).append("}")
        out.append("}\n")
        produced++
    }
    print(out)
}
