package vn.tuvi.core.engine

import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import java.time.LocalDate
import java.util.EnumMap

/** Hành của nhóm tam hợp: Thân Tý Thìn Thủy, Tỵ Dậu Sửu Kim, Dần Ngọ Tuất Hỏa, Hợi Mão Mùi Mộc. */
fun tamHopElement(chi: Chi): NguHanh = when (chi.tamHopGroup) {
    0 -> NguHanh.THUY
    1 -> NguHanh.KIM
    2 -> NguHanh.HOA
    else -> NguHanh.MOC
}

/** Thiên thời theo ngũ hành tam hợp (cụ Thiên Lương) — mục 8.1. */
enum class ThienThoi(val vn: String, val label: String, val score: Int) {
    DAI_HAN_SINH_TUOI("Đại hạn sinh tuổi", "Đắc thiên thời, được hoàn cảnh nâng đỡ", 2),
    DONG_HANH("Đồng hành", "Hòa hợp, giữ vững thành quả", 1),
    TUOI_KHAC_DAI_HAN("Tuổi khắc đại hạn", "Vất vả, phải tự lực nhưng giành được", 0),
    TUOI_SINH_DAI_HAN("Tuổi sinh đại hạn", "Hao tâm tổn sức, thành quả bị san sẻ", -1),
    DAI_HAN_KHAC_TUOI("Đại hạn khắc tuổi", "Thất thời, nên thủ hơn công", -2);

    companion object {
        /** [tuoi] = hành tam hợp của chi năm sinh, [daiHan] = hành tam hợp của chi cung đại hạn. */
        fun of(tuoi: NguHanh, daiHan: NguHanh): ThienThoi = when {
            daiHan == tuoi -> DONG_HANH
            daiHan.generates == tuoi -> DAI_HAN_SINH_TUOI
            tuoi.overcomes == daiHan -> TUOI_KHAC_DAI_HAN
            tuoi.generates == daiHan -> TUOI_SINH_DAI_HAN
            else -> DAI_HAN_KHAC_TUOI
        }
    }
}

/** Một đại hạn 10 năm. */
data class DaiHanPeriod(
    /** 0 = đại hạn đầu (tại cung Mệnh). */
    val index: Int,
    val palace: Chi,
    /** Can chi đại hạn = can chi của cung. */
    val canChi: CanChi,
    val ages: IntRange,
    val thienThoi: ThienThoi,
)

/**
 * Các tầng vận hạn tại một thời điểm xem (mục 8). Chín trường đầu giữ đúng kiểu dữ liệu của prompt (mục 3).
 *
 * [luuStars] khóa theo sao gốc tương ứng: `THAI_TUE` → vị trí Lưu Thái Tuế, `LOC_TON` → Lưu Lộc Tồn, …
 */
data class LimitLayers(
    val daiHan: Chi,
    val tieuHan: Chi,
    val luuNienDaiHan: Chi,
    val luuThaiTue: Chi,
    val luuStars: Map<StarId, Chi>,
    val luuTuHoa: Map<TuHoa, Chi>,
    val nguyetHan: Chi?,
    val nhatHan: Chi?,
    val thoiHan: Chi?,
    // —— Bổ sung ——
    val lunarYear: Int,
    val yearCanChi: CanChi,
    val tuoiAm: Int,
    val daiHanPeriod: DaiHanPeriod,
    /** Tuổi âm chưa tới số Cục: chưa vào đại hạn đầu (D-19). */
    val beforeFirstDaiHan: Boolean,
    /** Năm thứ mấy (1..10) của đại hạn; null khi [beforeFirstDaiHan]. */
    val namThuDaiHan: Int?,
    /** 12 lưu cung: cung Lưu Thái Tuế là Lưu Mệnh, các vai trò xếp như mục 5.1. */
    val luuRoles: Map<Chi, PalaceRole>,
    /** 12 cung đại hạn xoay: cung đại hạn là “ĐH Mệnh”. */
    val daiHanRoles: Map<Chi, PalaceRole>,
    /** Tứ Hóa theo can cung đại hạn — chỉ khi SchoolConfig.daiHanTuHoa = ON. */
    val daiHanTuHoa: Map<TuHoa, Chi>?,
    val lunarMonth: Int?,
    val isLeapMonth: Boolean,
    val monthCanChi: CanChi?,
    /** Tứ Hóa theo can tháng — chỉ có ở tầng tháng trở xuống. */
    val nguyetTuHoa: Map<TuHoa, Chi>?,
    val lunarDate: LunarDate?,
    val dayCanChi: CanChi?,
    val hourChi: Chi?,
)

/**
 * Hệ vận hạn đa tầng (mục 8): đại hạn + thiên thời, tiểu hạn, lưu niên đại hạn, sao lưu, Lưu Mệnh,
 * Tứ Hóa lưu/đại hạn/nguyệt, nguyệt – nhật – thời hạn. Hàm thuần, chỉ đọc [Chart].
 */
object LimitEngine {

    /** 12 đại hạn: thứ i ở Mệnh + i·dir, phủ tuổi [cục + 10i, cục + 10i + 9]. */
    fun daiHanPeriods(chart: Chart): List<DaiHanPeriod> {
        val tuoi = tamHopElement(chart.yearChi)
        return (0 until 12).map { i ->
            val palace = chart.menh + i * chart.dir
            val start = chart.cuc.number + 10 * i
            DaiHanPeriod(
                index = i,
                palace = palace,
                canChi = CanChi(chart.palace(palace).can, palace),
                ages = start..start + 9,
                thienThoi = ThienThoi.of(tuoi, tamHopElement(palace)),
            )
        }
    }

    /**
     * Lưu niên đại hạn (Tân Biên, mục 8.3): năm 1 ở cung gốc, năm 2 sang xung chiếu; Dương Nam/Âm Nữ lùi một cung
     * rồi về xung chiếu, sau đó tiến thuận; Âm Nam/Dương Nữ tiến một cung rồi về xung chiếu, sau đó lùi nghịch.
     */
    fun luuNienDaiHan(goc: Chi, namThu: Int, dir: Int): Chi {
        require(namThu in 1..10) { "Năm thứ của đại hạn phải trong 1..10: $namThu" }
        val xung = goc.opposite
        return when (namThu) {
            1 -> goc
            2 -> xung
            3 -> xung - dir
            4 -> xung
            else -> xung + (namThu - 4) * dir
        }
    }

    /** Vận hạn năm âm [lunarYear]. */
    fun forYear(chart: Chart, lunarYear: Int): LimitLayers = build(chart, lunarYear, null, false, null, null, null)

    /** Vận hạn tháng âm [lunarMonth] (tháng nhuận dùng chung cung nguyệt hạn với tháng chính). */
    fun forMonth(chart: Chart, lunarYear: Int, lunarMonth: Int, isLeap: Boolean = false): LimitLayers {
        require(lunarMonth in 1..12) { "Tháng âm phải trong 1..12: $lunarMonth" }
        return build(chart, lunarYear, lunarMonth, isLeap, null, null, null)
    }

    /** Vận hạn một ngày dương lịch (đổi sang âm lịch theo múi giờ lịch [tz], mặc định UTC+7). */
    fun forDate(chart: Chart, date: LocalDate, tz: Int = 7): LimitLayers {
        val lunar = LunarCalendar.solarToLunar(date, tz)
        return build(chart, lunar.year, lunar.month, lunar.isLeap, lunar, LunarCalendar.dayCanChi(date), null)
    }

    /** Vận hạn một canh giờ trong ngày. */
    fun forHour(chart: Chart, date: LocalDate, hour: Chi, tz: Int = 7): LimitLayers {
        val lunar = LunarCalendar.solarToLunar(date, tz)
        return build(chart, lunar.year, lunar.month, lunar.isLeap, lunar, LunarCalendar.dayCanChi(date), hour)
    }

    /** Cung tháng Giêng của năm theo biến thể nguyệt hạn. */
    fun nguyetHanThangGieng(chart: Chart, tieuHan: Chi, luuThaiTue: Chi): Chi {
        val m = chart.effectiveMonth
        val h = chart.hourChi.index
        return when (chart.config.nguyetHan) {
            NguyetHanVariant.TIEU_HAN_NGHICH_THANG_THUAN_GIO -> tieuHan - (m - 1) + h
            NguyetHanVariant.TIEU_HAN_THUAN_THANG_THUAN_GIO -> tieuHan + (m - 1) + h
            NguyetHanVariant.TIEU_HAN_LA_THANG_GIENG -> tieuHan
            NguyetHanVariant.DAU_QUAN_LUU_THAI_TUE -> luuThaiTue - (m - 1) + h
        }
    }

    private fun build(
        chart: Chart,
        lunarYear: Int,
        lunarMonth: Int?,
        isLeap: Boolean,
        lunarDate: LunarDate?,
        dayCanChi: CanChi?,
        hour: Chi?,
    ): LimitLayers {
        val tuoiAm = chart.tuoiAm(lunarYear)
        require(tuoiAm >= 1) { "Năm xem ($lunarYear) phải từ năm sinh âm lịch (${chart.lunar.year}) trở đi" }
        val yearCanChi = CanChi.ofYear(lunarYear)
        val zy = yearCanChi.chi
        val config = chart.config

        // —— Đại hạn ——
        val periods = daiHanPeriods(chart)
        val beforeFirst = tuoiAm < chart.cuc.number
        val period = if (beforeFirst) periods[0] else periods[((tuoiAm - chart.cuc.number) / 10) % 12]
        val namThu = if (beforeFirst) null else (tuoiAm - chart.cuc.number) % 10 + 1

        // —— Tiểu hạn: cung mang chi của năm xem ——
        val tieuHan = chart.palaces.values.single { it.tieuHanChi == zy }.chi

        // —— Lưu niên đại hạn ——
        val luuNien = if (namThu == null) period.palace else luuNienDaiHan(period.palace, namThu, chart.dir)

        // —— Sao lưu (mục 8.4) ——
        val luuLocTon = Tables.LOC_TON[yearCanChi.can.index]
        val luu = EnumMap<StarId, Chi>(StarId::class.java)
        luu[StarId.THAI_TUE] = zy
        luu[StarId.TANG_MON] = zy + 2
        luu[StarId.BACH_HO] = zy + 8
        luu[StarId.THIEN_KHOC] = Chi.of(6 - zy.index)
        luu[StarId.THIEN_HU] = Chi.of(6 + zy.index)
        luu[StarId.LOC_TON] = luuLocTon
        luu[StarId.KINH_DUONG] = luuLocTon + 1
        luu[StarId.DA_LA] = luuLocTon - 1
        luu[StarId.THIEN_MA] = Tables.THIEN_MA[zy.tamHopGroup]
        luu[StarId.DAO_HOA] = Tables.DAO_HOA[zy.tamHopGroup]
        luu[StarId.HONG_LOAN] = Chi.of(3 - zy.index)
        luu[StarId.THIEN_HY] = Chi.of(9 - zy.index)
        luu[StarId.HY_THAN] = luuLocTon + 7 * chart.dir
        luu[StarId.LONG_DUC] = zy + 7
        luu[StarId.NGUYET_DUC] = zy + 5

        // —— Tứ Hóa: lưu (can năm xem), đại hạn (can cung đại hạn, tùy chọn), nguyệt (can tháng) ——
        fun tuHoaPositions(can: vn.tuvi.core.calendar.Can): Map<TuHoa, Chi> =
            Tables.tuHoa(can, config).mapValues { (_, star) -> chart.positionOf(star) }
        val luuTuHoa = tuHoaPositions(yearCanChi.can)
        val daiHanTuHoa = if (config.daiHanTuHoa == DaiHanTuHoa.ON) tuHoaPositions(period.canChi.can) else null

        // —— Lưu Mệnh và 12 cung đại hạn xoay ——
        fun rolesFrom(start: Chi): Map<Chi, PalaceRole> =
            EnumMap<Chi, PalaceRole>(Chi::class.java).apply { PalaceRole.entries.forEach { put(start + it.ordinal, it) } }

        // —— Nguyệt, nhật, thời hạn ——
        val monthCanChi = lunarMonth?.let { CanChi.ofMonth(yearCanChi.can, it) }
        val nguyetHan = lunarMonth?.let { nguyetHanThangGieng(chart, tieuHan, zy) + (it - 1) }
        val nhatHan = if (nguyetHan != null && lunarDate != null) nguyetHan + (lunarDate.day - 1) else null
        val thoiHan = if (nhatHan != null && hour != null) nhatHan + hour.index else null

        return LimitLayers(
            daiHan = period.palace,
            tieuHan = tieuHan,
            luuNienDaiHan = luuNien,
            luuThaiTue = zy,
            luuStars = luu,
            luuTuHoa = luuTuHoa,
            nguyetHan = nguyetHan,
            nhatHan = nhatHan,
            thoiHan = thoiHan,
            lunarYear = lunarYear,
            yearCanChi = yearCanChi,
            tuoiAm = tuoiAm,
            daiHanPeriod = period,
            beforeFirstDaiHan = beforeFirst,
            namThuDaiHan = namThu,
            luuRoles = rolesFrom(zy),
            daiHanRoles = rolesFrom(period.palace),
            daiHanTuHoa = daiHanTuHoa,
            lunarMonth = lunarMonth,
            isLeapMonth = isLeap,
            monthCanChi = monthCanChi,
            nguyetTuHoa = monthCanChi?.let { tuHoaPositions(it.can) },
            lunarDate = lunarDate,
            dayCanChi = dayCanChi,
            hourChi = hour,
        )
    }
}
