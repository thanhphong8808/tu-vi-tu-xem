package vn.tuvi.core.engine.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.RandomBirths
import vn.tuvi.core.engine.StarId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

/**
 * Công cụ dev cho tools/crosscheck/spec_reference.py: sinh N lá số ngẫu nhiên (mọi tổ hợp SchoolConfig) kèm một thời điểm
 * xem ngẫu nhiên, in JSON Lines gồm đầu vào đã chuẩn hóa (ngày âm, chi giờ), toàn bộ lá số và các tầng vận hạn.
 * Đối số: [số lá số] [seed].
 */
fun main(args: Array<String>) {
    val n = args.getOrNull(0)?.toInt() ?: 5000
    val r = Random(args.getOrNull(1)?.toLong() ?: 20261120L)
    val start = LocalDate.of(1900, 1, 1).toEpochDay()
    val end = LocalDate.of(2090, 12, 31).toEpochDay()
    val out = StringBuilder()
    repeat(n) {
        val birth = LocalDateTime.of(LocalDate.ofEpochDay(r.nextLong(start, end)), LocalTime.ofSecondOfDay(r.nextLong(0, 86_400)))
        val config = RandomBirths.config(r)
        val chart = ChartEngine.build(BirthInput(birth, null, if (r.nextBoolean()) Gender.NAM else Gender.NU), config)

        val viewYear = r.nextInt(chart.lunar.year, minOf(chart.lunar.year + 100, 2100) + 1)
        val viewMonth = r.nextInt(1, 13)
        val leap = LunarCalendar.leapMonthOf(viewYear) == viewMonth && r.nextBoolean()
        val viewDate = LunarCalendar.lunarToSolar(LunarDate(r.nextInt(1, 29), viewMonth, viewYear, leap))!!
        val hour = Chi.of(r.nextInt(12))
        val lim = LimitEngine.forHour(chart, viewDate, hour)

        fun chis(m: Map<*, Chi>?) = m?.let { map -> buildJsonObject { map.forEach { (k, v) -> put(k.toString(), v.index) } } } ?: JsonNull
        val record = buildJsonObject {
            put("lunarDay", chart.lunar.day); put("lunarMonth", chart.lunar.month); put("lunarYear", chart.lunar.year)
            put("leap", chart.lunar.isLeap); put("hour", chart.hourChi.index); put("gender", chart.gender.name)
            put("config", buildJsonObject {
                put("tuHoa", config.tuHoa.name); put("nhamKhoa", config.nhamKhoa.name); put("khoiViet", config.khoiViet.name)
                put("hoaLinh", config.hoaLinh.name); put("thienGiai", config.thienGiai.name); put("luuHa", config.luuHa.name)
                put("leapMonth", config.leapMonth.name); put("nguyetHan", config.nguyetHan.name); put("cuMonTable", config.cuMonTable.name)
                put("daiHanTuHoa", config.daiHanTuHoa.name); put("thanChuTy", config.thanChuTy.name)
            })
            put("menh", chart.menh.index); put("than", chart.than.index); put("cuc", chart.cuc.number)
            put("banMenh", chart.banMenh.element.name)
            put("menhChu", chart.menhChu.name); put("thanChu", chart.thanChu.name)
            put("tuan", JsonArray(listOf(JsonPrimitive(chart.tuan.first.index), JsonPrimitive(chart.tuan.second.index))))
            put("triet", JsonArray(listOf(JsonPrimitive(chart.triet.first.index), JsonPrimitive(chart.triet.second.index))))
            put("palaces", JsonArray(Chi.entries.map { c ->
                val p = chart.palace(c)
                buildJsonObject {
                    put("can", p.can.index); put("role", p.role.name); put("daiHan", p.daiHanStartAge); put("tieuHanChi", p.tieuHanChi.index)
                }
            }))
            put("stars", buildJsonObject { StarId.entries.forEach { put(it.name, chart.positionOf(it).index) } })
            put("brightness", buildJsonObject {
                StarId.entries.forEach { s -> put(s.name, chart.palaceOf(s).star(s)!!.brightness?.code?.let(::JsonPrimitive) ?: JsonNull) }
            })
            put("view", buildJsonObject {
                put("year", lim.lunarYear); put("month", lim.lunarMonth!!); put("leap", lim.isLeapMonth)
                put("day", lim.lunarDate!!.day); put("hour", hour.index)
            })
            put("limits", buildJsonObject {
                put("tuoiAm", lim.tuoiAm); put("daiHan", lim.daiHan.index); put("tieuHan", lim.tieuHan.index)
                put("luuNienDaiHan", lim.luuNienDaiHan.index); put("namThu", lim.namThuDaiHan?.let(::JsonPrimitive) ?: JsonNull)
                put("thienThoi", lim.daiHanPeriod.thienThoi.name)
                put("luuStars", chis(lim.luuStars.mapKeys { it.key.name }))
                put("luuTuHoa", chis(lim.luuTuHoa.mapKeys { it.key.name }))
                put("daiHanTuHoa", chis(lim.daiHanTuHoa?.mapKeys { it.key.name }))
                put("nguyetTuHoa", chis(lim.nguyetTuHoa?.mapKeys { it.key.name }))
                put("nguyetHan", lim.nguyetHan!!.index); put("nhatHan", lim.nhatHan!!.index); put("thoiHan", lim.thoiHan!!.index)
                put("luuMenh", lim.luuRoles.entries.first { it.value.name == "MENH" }.key.index)
            })
        }
        out.append(record.toString()).append('\n')
    }
    // Ghi UTF-8 tường minh: mặc định của JVM (nhất là trên Windows) có thể biến “Đ” thành “?”.
    java.io.PrintStream(java.io.FileOutputStream(java.io.FileDescriptor.out), true, "UTF-8").use { it.print(out) }
}
