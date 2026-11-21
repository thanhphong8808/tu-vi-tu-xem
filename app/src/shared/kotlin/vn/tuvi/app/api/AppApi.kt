package vn.tuvi.app.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vn.tuvi.core.calendar.BirthRegion
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.CanhGio
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.InvalidBirthInputException
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.CuMonTable
import vn.tuvi.core.engine.DaiHanTuHoa
import vn.tuvi.core.engine.DayBoundary
import vn.tuvi.core.engine.HoaLinhVariant
import vn.tuvi.core.engine.KhoiVietVariant
import vn.tuvi.core.engine.LeapMonthRule
import vn.tuvi.core.engine.LuuHaVariant
import vn.tuvi.core.engine.NguyetHanVariant
import vn.tuvi.core.engine.NhamHoaKhoa
import vn.tuvi.core.engine.SchoolConfig
import vn.tuvi.core.engine.ThanChuTy
import vn.tuvi.core.engine.ThienGiaiVariant
import vn.tuvi.core.engine.TuHoaVariant
import vn.tuvi.core.interpret.Event
import vn.tuvi.core.interpret.PairRelation
import vn.tuvi.core.engine.SchoolPreset
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.TuHoa
import vn.tuvi.core.engine.VN_STANDARD_ZONE
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Kho khóa–giá trị nhỏ để lưu hồ sơ lá số (Android: SharedPreferences; máy tính: bộ nhớ hoặc file). */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

class MemoryStore : KeyValueStore {
    private val map = HashMap<String, String>()
    @Synchronized override fun get(key: String): String? = map[key]
    @Synchronized override fun put(key: String, value: String) { map[key] = value }
}

/**
 * Cầu nối giữa giao diện web (WebView) và engine. Mọi hàm nhận và trả chuỗi JSON để dùng chung cho
 * `@JavascriptInterface` trên Android và máy chủ thử nghiệm trên máy tính. Không bao giờ ném lỗi ra ngoài:
 * lỗi trả về dạng `{"ok":false,"error":"…"}` để giao diện hiện cho người dùng.
 */
class AppApi(private val store: KeyValueStore, private val today: () -> LocalDate = { LocalDate.now(ZoneOffset.ofHours(7)) }) {

    fun call(method: String, arg: String?): String = try {
        val req: JsonElement = if (arg.isNullOrBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(arg)
        val data: JsonElement = when (method) {
            "chart" -> chart(req.jsonObject)
            "reading" -> reading(req.jsonObject)
            "days" -> Luan.daysJson(buildChart(req.jsonObject), req.jsonObject.need("year", "năm"), req.jsonObject.need("month", "tháng"), expertOf(req.jsonObject))
            "day" -> Luan.dayJson(buildChart(req.jsonObject), parseDate(req.jsonObject.str("date")), expertOf(req.jsonObject))
            "star" -> Luan.starJson(buildChart(req.jsonObject), req.jsonObject.str("star") ?: throw IllegalArgumentException("Thiếu tên sao."))
            "compare" -> compare(req.jsonObject)
            "rankHours" -> rankHours(req.jsonObject)
            "configOptions" -> configOptions()
            "bench" -> Luan.bench()
            "ask" -> ask(req.jsonObject)
            "qaSuggest" -> Luan.qaSuggestJson(buildChart(req.jsonObject), req.jsonObject.str("today")?.let { parseDate(it) } ?: today())
            "allHours" -> allHours(req.jsonObject)
            "lunarYear" -> lunarYear(req.jsonObject)
            "convert" -> convert(req.jsonObject)
            "today" -> todayInfo()
            "storeGet" -> storeGet(req.jsonObject)
            "storeSet" -> storeSet(req.jsonObject)
            else -> throw IllegalArgumentException("Không có chức năng “$method”.")
        }
        buildJsonObject { put("ok", true); put("data", data) }.toString()
    } catch (e: InvalidBirthInputException) {
        fail(e.message ?: "Thông tin sinh không hợp lệ.")
    } catch (e: IllegalArgumentException) {
        fail(e.message ?: "Thông tin không hợp lệ.")
    } catch (e: Throwable) {
        fail("Lỗi nội bộ (${e.javaClass.simpleName}): ${e.message ?: ""}")
    }

    private fun fail(message: String): String = buildJsonObject { put("ok", false); put("error", message) }.toString()

    // ——————————————————————————————— đọc đầu vào ———————————————————————————————

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.contentOrNull?.trim()?.toIntOrNull() }
    private fun JsonObject.dbl(key: String): Double? =
        (this[key] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.trim()?.replace(',', '.')?.toDoubleOrNull() }
    private fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false
    private fun JsonObject.need(key: String, label: String): Int = int(key) ?: throw IllegalArgumentException("Chưa nhập $label.")

    /**
     * Trường phái: `preset` (NAM_PHAI_VN, THIEN_LUONG, TOAN_THU_TRUNG_CHAU) hoặc `preset = CUSTOM` kèm `config`
     * (từng điểm của SchoolConfig, theo tên enum — Cài đặt → Tùy chỉnh từng điểm).
     */
    private fun configOf(o: JsonObject): SchoolConfig {
        val key = o.str("preset") ?: return SchoolConfig.DEFAULT
        if (key == "CUSTOM") {
            val c = o["config"] as? JsonObject ?: return SchoolConfig.DEFAULT
            fun <E : Enum<E>> pick(name: String, values: Array<E>, def: E): E = values.firstOrNull { it.name == c.str(name) } ?: def
            val d = SchoolConfig.DEFAULT
            return SchoolConfig(
                tuHoa = pick("tuHoa", TuHoaVariant.entries.toTypedArray(), d.tuHoa),
                nhamKhoa = pick("nhamKhoa", NhamHoaKhoa.entries.toTypedArray(), d.nhamKhoa),
                khoiViet = pick("khoiViet", KhoiVietVariant.entries.toTypedArray(), d.khoiViet),
                hoaLinh = pick("hoaLinh", HoaLinhVariant.entries.toTypedArray(), d.hoaLinh),
                thienGiai = pick("thienGiai", ThienGiaiVariant.entries.toTypedArray(), d.thienGiai),
                luuHa = pick("luuHa", LuuHaVariant.entries.toTypedArray(), d.luuHa),
                leapMonth = pick("leapMonth", LeapMonthRule.entries.toTypedArray(), d.leapMonth),
                dayBoundary = pick("dayBoundary", DayBoundary.entries.toTypedArray(), d.dayBoundary),
                nguyetHan = pick("nguyetHan", NguyetHanVariant.entries.toTypedArray(), d.nguyetHan),
                cuMonTable = pick("cuMonTable", CuMonTable.entries.toTypedArray(), d.cuMonTable),
                daiHanTuHoa = pick("daiHanTuHoa", DaiHanTuHoa.entries.toTypedArray(), d.daiHanTuHoa),
                thanChuTy = pick("thanChuTy", ThanChuTy.entries.toTypedArray(), d.thanChuTy),
            )
        }
        return SchoolPreset.entries.firstOrNull { it.name == key }?.config ?: SchoolConfig.DEFAULT
    }

    /** Danh sách điểm tùy chỉnh của trường phái cho màn Cài đặt. */
    private fun configOptions(): JsonElement {
        fun field(key: String, label: String, values: List<Pair<String, String>>, def: String) = buildJsonObject {
            put("key", key); put("label", label); put("default", def)
            put("options", buildJsonArray { for ((n, v) in values) add(buildJsonObject { put("value", n); put("label", v) }) })
        }
        val d = SchoolConfig.DEFAULT
        return buildJsonArray {
            add(field("tuHoa", "Tứ Hóa năm Canh", TuHoaVariant.entries.map { it.name to it.vn }, d.tuHoa.name))
            add(field("nhamKhoa", "Năm Nhâm hóa Khoa", NhamHoaKhoa.entries.map { it.name to it.vn }, d.nhamKhoa.name))
            add(field("khoiViet", "Khôi Việt tuổi Canh, Tân", KhoiVietVariant.entries.map { it.name to it.vn }, d.khoiViet.name))
            add(field("hoaLinh", "An Hỏa Tinh, Linh Tinh", HoaLinhVariant.entries.map { it.name to it.vn }, d.hoaLinh.name))
            add(field("thienGiai", "An Thiên Giải", ThienGiaiVariant.entries.map { it.name to it.vn }, d.thienGiai.name))
            add(field("luuHa", "An Lưu Hà", LuuHaVariant.entries.map { it.name to it.vn }, d.luuHa.name))
            add(field("leapMonth", "Sinh tháng nhuận", LeapMonthRule.entries.map { it.name to it.vn }, d.leapMonth.name))
            add(field("dayBoundary", "Giờ Tý muộn (23:00–23:59)", DayBoundary.entries.map { it.name to it.vn }, d.dayBoundary.name))
            add(field("nguyetHan", "An nguyệt hạn", NguyetHanVariant.entries.map { it.name to it.vn }, d.nguyetHan.name))
            add(field("cuMonTable", "Độ sáng Cự Môn", CuMonTable.entries.map { it.name to it.vn }, d.cuMonTable.name))
            add(field("daiHanTuHoa", "Tứ Hóa theo can đại hạn", DaiHanTuHoa.entries.map { it.name to it.vn }, d.daiHanTuHoa.name))
            add(field("thanChuTy", "Thân chủ tuổi Tý", ThanChuTy.entries.map { it.name to it.vn }, d.thanChuTy.name))
        }
    }

    private fun expertOf(o: JsonObject): Boolean = ((o["input"] as? JsonObject) ?: o).bool("expert") || o.bool("expert")

    private fun parseDate(s: String?): LocalDate = try { LocalDate.parse(s ?: "") } catch (e: Exception) {
        throw IllegalArgumentException("Ngày không hợp lệ: $s")
    }

    /** Lá số từ đầu vào form (kể cả lá số thay thế `alt`). */
    private fun buildChart(o: JsonObject): Chart {
        val inputObj = (o["input"] as? JsonObject) ?: o
        val base = ChartEngine.build(parseInput(inputObj), configOf(inputObj))
        val altIndex = o.int("alt") ?: return base
        val w = base.warnings.getOrNull(altIndex) ?: throw IllegalArgumentException("Không có lá số thay thế này.")
        return ChartEngine.buildAlternative(base, w)
    }

    /** So hai lá số trong hồ sơ: `a`, `b` là đầu vào form; `relation` = VO_CHONG | CHA_ME_CON. */
    /** Hỏi đáp: `question` (≤ 500 ký tự), `today` (ISO, mặc định hôm nay giờ VN) trên lá số `input` (+ `alt`). */
    private fun ask(o: JsonObject): JsonElement {
        val q = (o["question"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        if (q.length > 500) throw IllegalArgumentException("Câu hỏi dài quá (tối đa 500 ký tự).")
        val day = o.str("today")?.let { parseDate(it) } ?: today()
        return Luan.askJson(buildChart(o), q, day, expertOf(o))
    }

    private fun compare(o: JsonObject): JsonElement {
        val a = o["a"] as? JsonObject ?: throw IllegalArgumentException("Thiếu lá số thứ nhất.")
        val b = o["b"] as? JsonObject ?: throw IllegalArgumentException("Thiếu lá số thứ hai.")
        val rel = PairRelation.entries.firstOrNull { it.name == o.str("relation") } ?: PairRelation.VO_CHONG
        val ca = ChartEngine.build(parseInput(a), configOf(a))
        val cb = ChartEngine.build(parseInput(b), configOf(b))
        return Luan.compareJson(ca, cb, rel, a.str("name") ?: "Người thứ nhất", b.str("name") ?: "Người thứ hai")
    }

    /** Không rõ giờ sinh: xếp 12 giờ theo các sự kiện đã qua (`events` = [{event, year}]). */
    private fun rankHours(o: JsonObject): JsonElement {
        val inputObj = (o["input"] as? JsonObject) ?: o
        val charts = ChartEngine.buildAllHours(parseInput(inputObj), configOf(inputObj))
        val events = (o["events"] as? JsonArray).orEmpty().mapNotNull { e ->
            val eo = e as? JsonObject ?: return@mapNotNull null
            val ev = Event.entries.firstOrNull { it.name == eo.str("event") } ?: return@mapNotNull null
            val y = eo.int("year") ?: return@mapNotNull null
            if (y !in 1900..2200) throw IllegalArgumentException("Năm sự kiện phải trong 1900–2200.")
            ev to y
        }
        return Luan.rankJson(charts, events)
    }

    private fun regionOf(o: JsonObject): BirthRegion =
        BirthRegion.entries.firstOrNull { it.name == o.str("region") } ?: BirthRegion.VN_BAC

    /** Đọc form nhập liệu (mục 2.1) thành [BirthInput]. Với “Không rõ giờ”, canh tạm là Tý sớm. */
    internal fun parseInput(o: JsonObject): BirthInput {
        val gender = when (o.str("gender")) {
            "NAM" -> Gender.NAM
            "NU" -> Gender.NU
            else -> throw IllegalArgumentException("Chưa chọn giới tính.")
        }
        val region = regionOf(o)
        val zone: ZoneId = when (region) {
            BirthRegion.VN_BAC -> VN_STANDARD_ZONE
            BirthRegion.VN_NAM -> ZoneId.of("Asia/Ho_Chi_Minh")
            BirthRegion.NUOC_NGOAI -> {
                val id = o.str("zone") ?: throw IllegalArgumentException("Chưa chọn múi giờ nơi sinh.")
                try { ZoneId.of(id) } catch (e: DateTimeException) { throw IllegalArgumentException("Múi giờ không hợp lệ: $id") }
            }
        }
        val day = o.need("day", "ngày sinh")
        val month = o.need("month", "tháng sinh")
        val year = o.need("year", "năm sinh")
        val canh: CanhGio?
        val clock: LocalTime?
        when (o.str("timeMode") ?: "clock") {
            "canh" -> {
                canh = CanhGio.entries.firstOrNull { it.name == o.str("canh") } ?: throw IllegalArgumentException("Chưa chọn canh giờ.")
                clock = null
            }
            "unknown" -> { canh = CanhGio.TY_SOM; clock = null }
            else -> {
                val h = o.need("hour", "giờ sinh")
                val m = o.int("minute") ?: 0
                if (h !in 0..23 || m !in 0..59) throw IllegalArgumentException("Giờ sinh phải trong 00:00–23:59.")
                canh = null
                clock = LocalTime.of(h, m)
            }
        }
        val lon = o.dbl("longitude")
        if (lon != null && (lon < -180.0 || lon > 180.0)) throw IllegalArgumentException("Kinh độ phải trong −180…180.")
        val trueSolar = o.bool("trueSolar") && lon != null && clock != null
        return if (o.str("calendar") == "lunar") {
            if (month !in 1..12 || day !in 1..30) throw IllegalArgumentException("Ngày âm lịch không hợp lệ.")
            BirthInput(
                solar = null, lunar = LunarDate(day, month, year, o.bool("leap")), gender = gender, zone = zone,
                longitudeDeg = lon, useTrueSolarTime = trueSolar, lunarClock = clock, canh = canh, region = region,
            )
        } else {
            val date = try { LocalDate.of(year, month, day) } catch (e: DateTimeException) {
                throw IllegalArgumentException("Ngày $day/$month/$year không có trong dương lịch.")
            }
            BirthInput(
                solar = LocalDateTime.of(date, clock ?: LocalTime.NOON), lunar = null, gender = gender, zone = zone,
                longitudeDeg = lon, useTrueSolarTime = trueSolar, canh = canh, region = region,
            )
        }
    }

    // ——————————————————————————————— chức năng ———————————————————————————————

    private fun currentLunarYear(): Int = LunarCalendar.solarToLunar(today(), 7).year

    /** Lá số + vận hạn năm đang xem. `alt` = chỉ số cảnh báo để lập lá số thay thế. */
    private fun chart(o: JsonObject): JsonElement {
        val inputObj = (o["input"] as? JsonObject) ?: o
        val input = parseInput(inputObj)
        val config = configOf(inputObj)
        val base = ChartEngine.build(input, config)
        val altIndex = o.int("alt")
        val chart = if (altIndex != null) {
            val w = base.warnings.getOrNull(altIndex) ?: throw IllegalArgumentException("Không có lá số thay thế này.")
            ChartEngine.buildAlternative(base, w)
        } else base
        val viewYear = o.int("viewYear") ?: currentLunarYear()
        if (viewYear !in 1900..2200) throw IllegalArgumentException("Năm xem phải trong 1900–2200.")
        val name = inputObj.str("name") ?: ""
        return buildJsonObject {
            put("info", infoJson(chart, inputObj, name, viewYear))
            put("palaces", buildJsonArray { for (chi in Chi.entries) add(palaceJson(chart, chi)) })
            put("tuan", buildJsonArray { add(chart.tuan.first.ordinal); add(chart.tuan.second.ordinal) })
            put("triet", buildJsonArray { add(chart.triet.first.ordinal); add(chart.triet.second.ordinal) })
            put("warnings", buildJsonArray {
                if (altIndex == null) for (w in chart.warnings) add(buildJsonObject {
                    put("code", w.code.name); put("title", w.code.vn); put("message", w.message)
                    put("alt", w.altInput != null || w.altConfig != null)
                })
            })
            put("alt", if (altIndex != null) JsonPrimitive(base.warnings[altIndex].code.vn) else JsonNull)
            put("daiHan", buildJsonArray {
                for (p in LimitEngine.daiHanPeriods(chart)) add(buildJsonObject {
                    put("i", p.index); put("chi", p.palace.ordinal); put("canChi", p.canChi.vn)
                    put("from", p.ages.first); put("to", p.ages.last)
                    put("thienThoi", p.thienThoi.vn); put("thienThoiLabel", p.thienThoi.label); put("score", p.thienThoi.score)
                    put("year", chart.lunar.year + p.ages.first - 1)
                })
            })
            val limits = LimitEngine.forYear(chart, viewYear)
            put("year", yearJson(chart, limits))
            // Luận giải sơ bộ (giai đoạn 3). Lỗi ở đây không được làm hỏng lá số.
            put("reading", try { Luan.json(chart, limits, inputObj.bool("expert") || o.bool("expert")) } catch (e: Throwable) {
                buildJsonObject { put("error", "${e.javaClass.simpleName}: ${e.message ?: ""}") }
            })
            put("months", buildJsonArray {
                val leap = LunarCalendar.leapMonthOf(viewYear)
                for (m in 1..12) {
                    val l = LimitEngine.forMonth(chart, viewYear, m)
                    add(buildJsonObject {
                        put("month", m); put("chi", l.nguyetHan?.ordinal); put("canChi", l.monthCanChi?.vn)
                        put("leapAfter", leap == m)
                        put("tuHoa", tuHoaJson(l.nguyetTuHoa, "Ng."))
                    })
                }
            })
        }
    }

    /**
     * Bản luận (giai đoạn 4): `scope` = life | decade | year | month, `viewYear`, `month`. Cùng đầu vào với [chart]
     * (kể cả lá số thay thế `alt`, chế độ chuyên sâu `expert`).
     */
    private fun reading(o: JsonObject): JsonElement {
        val inputObj = (o["input"] as? JsonObject) ?: o
        val chart = buildChart(o)
        val viewYear = o.int("viewYear") ?: currentLunarYear()
        if (viewYear !in 1900..2200) throw IllegalArgumentException("Năm xem phải trong 1900–2200.")
        val month = (o.int("month") ?: 1).coerceIn(1, 12)
        return Luan.readingJson(chart, o.str("scope") ?: "year", viewYear, month, inputObj.bool("expert") || o.bool("expert"), currentLunarYear())
    }

    private fun canhLabel(input: BirthInput): String? = input.canh?.vn

    private fun infoJson(chart: Chart, o: JsonObject, name: String, viewYear: Int): JsonObject = buildJsonObject {
        val input = chart.input
        val b = chart.birth
        put("name", name)
        put("gender", chart.gender.vn)
        val solarDate = input.solar?.toLocalDate() ?: b.calendarDate
        put("solar", fmtDate(solarDate))
        put("clock", when {
            input.canh != null -> canhLabel(input)
            input.solar != null -> fmtTime(input.solar!!.toLocalTime())
            input.lunarClock != null -> fmtTime(input.lunarClock!!)
            else -> null
        })
        put("timeMode", o.str("timeMode") ?: "clock")
        put("referenceTime", b.referenceTime?.let { "${fmtTime(it.toLocalTime())} ngày ${fmtDate(it.toLocalDate())}" })
        put("lunar", "${chart.lunar.day}/${chart.lunar.month}${if (chart.lunar.isLeap) " nhuận" else ""}/${chart.lunar.year}")
        put("lunarLeap", chart.lunar.isLeap)
        put("calendarTz", b.calendarTz)
        put("lateTy", b.lateTy)
        put("yearCanChi", chart.pillars.year.vn)
        put("monthCanChi", chart.pillars.month.vn)
        put("dayCanChi", chart.pillars.day.vn)
        put("hourCanChi", chart.pillars.hour.vn)
        put("hourChi", chart.hourChi.vn)
        put("amDuong", chart.amDuongNamNu.vn)
        put("thuanLy", chart.amDuongThuanLy)
        put("banMenh", chart.banMenh.name)
        put("banMenhHanh", chart.banMenh.element.vn)
        put("cuc", chart.cuc.vn)
        put("cucShort", chart.cuc.short)
        put("cucMenh", chart.cucMenhRelation.vn)
        put("menh", chart.menh.vn)
        put("menhIdx", chart.menh.ordinal)
        put("than", chart.than.vn)
        put("thanIdx", chart.than.ordinal)
        put("thanRole", chart.palace(chart.than).role.vn)
        put("menhChu", chart.menhChu.vn)
        put("thanChu", chart.thanChu.vn)
        put("preset", chart.config.label)
        put("presetKey", chart.config.preset?.name ?: "CUSTOM")
        put("viewYear", viewYear)
        put("viewYearCanChi", CanChi.ofYear(viewYear).vn)
        put("tuoiAm", chart.tuoiAm(viewYear))
        put("tuoiAmNow", chart.tuoiAm(currentLunarYear()))
        put("region", when (input.region) {
            BirthRegion.VN_BAC -> "Miền Bắc Việt Nam"
            BirthRegion.VN_NAM -> "Miền Nam Việt Nam"
            BirthRegion.NUOC_NGOAI -> "Nước ngoài (${input.zone.id})"
        })
        put("dir", chart.dir)
    }

    private fun starJson(id: StarId, label: String, brightness: String?): JsonObject = buildJsonObject {
        put("k", id.name); put("n", label); put("s", StarStyle.side(id).key)
        put("e", StarStyle.elementKey(StarStyle.element(id))); put("b", brightness)
    }

    private fun palaceJson(chart: Chart, chi: Chi): JsonObject {
        val p = chart.palace(chi)
        return buildJsonObject {
            put("chi", chi.ordinal); put("chiVn", chi.vn); put("can", p.can.vn)
            put("role", p.role.vn); put("roleKey", p.role.name); put("isThan", p.isThan)
            put("daiHan", p.daiHanStartAge); put("tieuHan", p.tieuHanChi.vn)
            put("tuan", p.hasTuan); put("triet", p.hasTriet); put("voChinhDieu", p.isVoChinhDieu)
            put("stars", buildJsonArray {
                for (s in p.stars.sortedBy { it.id.ordinal }) add(starJson(s.id, s.id.vn, s.brightness?.code))
            })
        }
    }

    private fun tuHoaJson(map: Map<TuHoa, Chi>?, prefix: String): JsonElement {
        if (map == null) return JsonNull
        return buildJsonArray {
            for (h in TuHoa.entries) {
                val chi = map[h] ?: continue
                add(buildJsonObject {
                    put("k", h.name); put("n", prefix + h.vn.removePrefix("Hóa ")); put("full", h.vn); put("chi", chi.ordinal)
                })
            }
        }
    }

    private fun rolesJson(map: Map<Chi, PalaceRole>): JsonArray = buildJsonArray { for (c in Chi.entries) add(map[c]?.vn ?: "") }

    private fun yearJson(chart: Chart, l: LimitLayers): JsonObject = buildJsonObject {
        put("lunarYear", l.lunarYear)
        put("canChi", l.yearCanChi.vn)
        put("tuoiAm", l.tuoiAm)
        put("daiHan", l.daiHan.ordinal)
        put("daiHanIndex", l.daiHanPeriod.index)
        put("daiHanFrom", l.daiHanPeriod.ages.first)
        put("daiHanTo", l.daiHanPeriod.ages.last)
        put("daiHanCanChi", l.daiHanPeriod.canChi.vn)
        put("thienThoi", l.daiHanPeriod.thienThoi.vn)
        put("thienThoiLabel", l.daiHanPeriod.thienThoi.label)
        put("beforeFirstDaiHan", l.beforeFirstDaiHan)
        put("namThu", l.namThuDaiHan)
        put("tieuHan", l.tieuHan.ordinal)
        put("luuNienDaiHan", l.luuNienDaiHan.ordinal)
        put("luuThaiTue", l.luuThaiTue.ordinal)
        put("luuStars", buildJsonArray {
            for ((id, chi) in l.luuStars.entries.sortedBy { it.key.ordinal }) {
                add(buildJsonObject {
                    put("k", id.name); put("n", "L." + id.vn); put("chi", chi.ordinal)
                    put("s", StarStyle.side(id).key); put("e", StarStyle.elementKey(StarStyle.element(id)))
                })
            }
        })
        put("luuTuHoa", tuHoaJson(l.luuTuHoa, "L."))
        put("daiHanTuHoa", tuHoaJson(l.daiHanTuHoa, "ĐH."))
        put("luuRoles", rolesJson(l.luuRoles))
        put("daiHanRoles", rolesJson(l.daiHanRoles))
        put("daiHanRoleGoc", chart.palace(l.daiHan).role.vn)
    }

    /** Không rõ giờ sinh (mục 2.3): 12 lá số theo 12 canh, tóm tắt điểm khác nhau. */
    private fun allHours(o: JsonObject): JsonElement {
        val inputObj = (o["input"] as? JsonObject) ?: o
        val input = parseInput(inputObj)
        val charts = ChartEngine.buildAllHours(input, configOf(inputObj))
        return buildJsonArray {
            for ((canh, c) in charts) add(buildJsonObject {
                put("canh", canh.name); put("label", canh.vn)
                put("menh", c.menh.vn); put("than", c.than.vn); put("thanRole", c.palace(c.than).role.vn)
                put("cuc", c.cuc.short)
                val main = c.palace(c.menh).chinhTinh
                put("menhStars", if (main.isEmpty()) "Vô chính diệu" else main.joinToString(", ") { s ->
                    s.brightness?.let { "${s.id.vn} (${it.code})" } ?: s.id.vn
                })
                val first = LimitEngine.daiHanPeriods(c).first()
                put("daiHanDau", "${first.ages.first}–${first.ages.last} tại ${first.palace.vn}")
            })
        }
    }

    /** Thông tin năm âm cho form âm lịch: tháng nhuận theo lịch UTC+7 và UTC+8. */
    private fun lunarYear(o: JsonObject): JsonElement {
        val y = o.need("year", "năm")
        if (y !in 1899..2101) throw IllegalArgumentException("Năm phải trong 1900–2100.")
        return buildJsonObject {
            put("year", y); put("canChi", CanChi.ofYear(y).vn)
            put("leap7", LunarCalendar.leapMonthOf(y, 7)); put("leap8", LunarCalendar.leapMonthOf(y, 8))
        }
    }

    /** Đổi nhanh dương ↔ âm để hiện gợi ý ngay dưới ô ngày sinh. */
    private fun convert(o: JsonObject): JsonElement {
        val d = o.need("day", "ngày"); val m = o.need("month", "tháng"); val y = o.need("year", "năm")
        if (y !in 1900..2100) throw IllegalArgumentException("Năm phải trong 1900–2100.")
        val region = regionOf(o)
        return if (o.str("calendar") == "lunar") {
            val leap = o.bool("leap")
            val l = LunarDate(d, m, y, leap)
            // Ưu tiên ngày dương nằm đúng thời kỳ dùng múi giờ lịch đó (giống chuẩn hóa của engine).
            val candidates = listOf(7, 8).mapNotNull { tz -> LunarCalendar.lunarToSolar(l, tz)?.let { tz to it } }
            val solar = (candidates.firstOrNull { (tz, s) -> LunarCalendar.calendarTzFor(s, region) == tz } ?: candidates.firstOrNull())?.second
                ?: throw IllegalArgumentException("Ngày âm $d/$m${if (leap) " nhuận" else ""}/$y không tồn tại.")
            buildJsonObject {
                put("text", "Dương lịch: ${weekday(solar)} ${fmtDate(solar)}")
                put("yearCanChi", CanChi.ofYear(y).vn)
            }
        } else {
            val date = try { LocalDate.of(y, m, d) } catch (e: DateTimeException) {
                throw IllegalArgumentException("Ngày $d/$m/$y không có trong dương lịch.")
            }
            val tz = LunarCalendar.calendarTzFor(date, region)
            val l = LunarCalendar.solarToLunar(date, tz)
            buildJsonObject {
                put("text", "Âm lịch: ${l.day}/${l.month}${if (l.isLeap) " nhuận" else ""} năm ${CanChi.ofYear(l.year).vn} (${l.year})")
                put("yearCanChi", CanChi.ofYear(l.year).vn)
            }
        }
    }

    private fun todayInfo(): JsonElement {
        val t = today()
        val l = LunarCalendar.solarToLunar(t, 7)
        return buildJsonObject {
            put("solar", fmtDate(t)); put("year", t.year); put("iso", t.toString()); put("month", t.monthValue)
            put("lunarYear", l.year); put("lunarMonth", l.month); put("lunar", "${l.day}/${l.month}${if (l.isLeap) " nhuận" else ""}/${l.year}")
            put("lunarYearCanChi", CanChi.ofYear(l.year).vn)
        }
    }

    private fun storeKey(o: JsonObject): String {
        val k = o.str("key") ?: throw IllegalArgumentException("Thiếu khóa lưu trữ.")
        if (k !in setOf("profiles", "settings", "qaLog")) throw IllegalArgumentException("Khóa lưu trữ không hợp lệ.")
        return k
    }

    private fun storeGet(o: JsonObject): JsonElement {
        val raw = store.get(storeKey(o)) ?: return JsonNull
        return try { Json.parseToJsonElement(raw) } catch (e: Exception) { JsonNull }
    }

    private fun storeSet(o: JsonObject): JsonElement {
        val value = o["value"] ?: JsonNull
        store.put(storeKey(o), value.toString())
        return JsonPrimitive(true)
    }

    private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()
    private fun fmtDate(d: LocalDate): String = "${two(d.dayOfMonth)}/${two(d.monthValue)}/${d.year}"
    private fun fmtTime(t: LocalTime): String = "${two(t.hour)}:${two(t.minute)}"
    private fun weekday(d: LocalDate): String =
        listOf("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật")[d.dayOfWeek.value - 1] + ","
}
