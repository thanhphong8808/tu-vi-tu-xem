package vn.tuvi.core.engine

import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import vn.tuvi.core.calendar.LunarDate
import java.util.EnumMap

/**
 * Lập lá số theo mục 5 (SchoolConfig quyết định các điểm biến thể).
 *
 * [build] là hàm thuần, deterministic: không đọc đồng hồ hệ thống, không ghi trạng thái.
 * Mọi vị trí là số học mod 12 với Tý = 0. Ký hiệu trong mã: c can năm, z chi năm, m tháng âm, d ngày âm, h chi giờ.
 */
object ChartEngine {

    fun build(input: BirthInput, config: SchoolConfig = SchoolConfig.DEFAULT): Chart =
        build(input, config, BrightnessTable.default)

    fun build(input: BirthInput, config: SchoolConfig, brightness: BrightnessTable): Chart {
        val normalized = BirthNormalizer.normalize(input, config)
        return place(input, config, normalized.birth, normalized.warnings, brightness)
    }

    /** Lá số thay thế ứng với một cảnh báo trên thẻ xác nhận. */
    fun buildAlternative(chart: Chart, warning: ChartWarning): Chart =
        build(warning.altInput ?: chart.input, warning.altConfig ?: chart.config)

    /** Không rõ giờ sinh: đủ 12 lá số theo 12 canh (Tý tính là Tý sớm). */
    fun buildAllHours(input: BirthInput, config: SchoolConfig = SchoolConfig.DEFAULT): Map<CanhGio, Chart> =
        CanhGio.TWELVE.associateWith { build(input.copy(canh = it), config) }

    /** Tháng dùng để an sao: tháng nhuận tính như tháng chính, hoặc chia đôi ở ngày 16. */
    fun effectiveMonth(lunar: LunarDate, rule: LeapMonthRule): Int =
        if (rule == LeapMonthRule.SPLIT_15 && lunar.isLeap && lunar.day >= 16) lunar.month % 12 + 1 else lunar.month

    private fun place(
        input: BirthInput,
        config: SchoolConfig,
        birth: NormalizedBirth,
        warnings: List<ChartWarning>,
        brightness: BrightnessTable,
    ): Chart {
        val lunar = birth.lunar
        val yearCanChi = CanChi.ofYear(lunar.year)
        val yearCan = yearCanChi.can
        val z = yearCanChi.chi
        val c = yearCan.index
        val m = effectiveMonth(lunar, config.leapMonth)
        val d = lunar.day
        val hourChi = birth.hourChi
        val h = hourChi.index
        val amDuong = AmDuongNamNu.of(yearCan.isYang, input.gender)
        val dir = amDuong.dir

        // —— 5.1 Cung và Cục ——
        val menh = Chi.of(2 + (m - 1) - h)
        val than = Chi.of(2 + (m - 1) + h)
        val canDan = 2 * c + 2
        fun canOf(p: Chi): Can = Can.of(canDan + Math.floorMod(p.index - 2, 12))
        val cuc = Cuc.of(NapAm.elementOf(canOf(menh), menh))
        val banMenh = NapAm.of(yearCan, z)

        val pos = EnumMap<StarId, Chi>(StarId::class.java)
        fun put(star: StarId, chi: Chi) {
            check(pos.put(star, chi) == null) { "An trùng sao $star" }
        }

        // —— 5.2 Mười bốn chính tinh ——
        val n = cuc.number
        val q = (d + n - 1) / n
        val y = q * n - d
        val base = 2 + q - 1
        val tuVi = Chi.of(if (y % 2 == 1) base - y else base + y)
        val thienPhu = Chi.of(4 - tuVi.index)
        put(StarId.TU_VI, tuVi)
        put(StarId.THIEN_CO, tuVi - 1)
        put(StarId.THAI_DUONG, tuVi - 3)
        put(StarId.VU_KHUC, tuVi - 4)
        put(StarId.THIEN_DONG, tuVi - 5)
        put(StarId.LIEM_TRINH, tuVi - 8)
        put(StarId.THIEN_PHU, thienPhu)
        put(StarId.THAI_AM, thienPhu + 1)
        put(StarId.THAM_LANG, thienPhu + 2)
        put(StarId.CU_MON, thienPhu + 3)
        put(StarId.THIEN_TUONG, thienPhu + 4)
        put(StarId.THIEN_LUONG, thienPhu + 5)
        put(StarId.THAT_SAT, thienPhu + 6)
        put(StarId.PHA_QUAN, thienPhu + 10)

        // —— 5.3 Bốn vòng sao ——
        val locTon = Tables.LOC_TON[c]
        put(StarId.LOC_TON, locTon)
        put(StarId.KINH_DUONG, locTon + 1)
        put(StarId.DA_LA, locTon - 1)
        StarId.BAC_SI_RING.forEachIndexed { i, s -> put(s, locTon + i * dir) }
        StarId.THAI_TUE_RING.forEachIndexed { i, s -> put(s, z + i) }
        val trangSinh = Tables.trangSinhStart(cuc)
        StarId.TRANG_SINH_RING.forEachIndexed { i, s -> put(s, trangSinh + i * dir) }
        put(StarId.THIEN_KHONG, z + 1) // đồng cung Thiếu Dương
        put(StarId.NGUYET_DUC, z + 5) // đồng cung Tử Phù
        put(StarId.THIEN_DUC, z + 9) // đồng cung Phúc Đức

        // —— 5.4 Phụ tinh theo tháng ——
        val taPhu = Chi.of(4 + (m - 1))
        val huuBat = Chi.of(10 - (m - 1))
        put(StarId.TA_PHU, taPhu)
        put(StarId.HUU_BAT, huuBat)
        put(StarId.THIEN_HINH, Chi.of(9 + (m - 1)))
        put(StarId.THIEN_RIEU, Chi.of(1 + (m - 1)))
        put(StarId.THIEN_Y, Chi.of(1 + (m - 1)))
        put(StarId.DIA_GIAI, Chi.of(7 + (m - 1)))
        put(
            StarId.THIEN_GIAI,
            when (config.thienGiai) {
                ThienGiaiVariant.THUAN -> Chi.of(8 + (m - 1))
                ThienGiaiVariant.NHAY_CUNG -> Chi.of(8 + 2 * (m - 1))
            },
        )

        // —— Theo giờ ——
        val vanKhuc = Chi.of(4 + h)
        val vanXuong = Chi.of(10 - h)
        put(StarId.VAN_KHUC, vanKhuc)
        put(StarId.VAN_XUONG, vanXuong)
        put(StarId.DIA_KIEP, Chi.of(11 + h))
        put(StarId.DIA_KHONG, Chi.of(11 - h))
        put(StarId.THAI_PHU, vanKhuc + 2)
        put(StarId.PHONG_CAO, vanKhuc - 2)

        // —— Theo ngày ——
        put(StarId.TAM_THAI, taPhu + (d - 1))
        put(StarId.BAT_TOA, huuBat - (d - 1))
        put(StarId.AN_QUANG, vanXuong + (d - 2))
        put(StarId.THIEN_QUY, vanKhuc - (d - 2))

        // —— Theo chi năm ——
        put(StarId.LONG_TRI, Chi.of(4 + z.index))
        put(StarId.PHUONG_CAC, Chi.of(10 - z.index))
        put(StarId.GIAI_THAN, Chi.of(10 - z.index))
        put(StarId.THIEN_KHOC, Chi.of(6 - z.index))
        put(StarId.THIEN_HU, Chi.of(6 + z.index))
        put(StarId.HONG_LOAN, Chi.of(3 - z.index))
        put(StarId.THIEN_HY, Chi.of(9 - z.index))
        put(StarId.THIEN_TAI, menh + z.index)
        put(StarId.THIEN_THO, than + z.index)
        put(StarId.DAU_QUAN, Chi.of(z.index - (m - 1) + h))

        // —— Theo tam hợp ——
        val group = z.tamHopGroup
        put(StarId.THIEN_MA, Tables.THIEN_MA[group])
        put(StarId.HOA_CAI, Tables.HOA_CAI[group])
        put(StarId.KIEP_SAT, Tables.KIEP_SAT[group])
        put(StarId.DAO_HOA, Tables.DAO_HOA[group])
        val coThan = Tables.coThan(z)
        put(StarId.CO_THAN, coThan)
        put(StarId.QUA_TU, coThan - 4)
        put(StarId.PHA_TOAI, Tables.phaToai(z))

        // —— Cố định ——
        put(StarId.THIEN_LA, Chi.THIN)
        put(StarId.DIA_VONG, Chi.TUAT)
        put(StarId.THIEN_THUONG, menh + PalaceRole.NO_BOC.ordinal)
        put(StarId.THIEN_SU, menh + PalaceRole.TAT_ACH.ordinal)

        // —— 5.5 Theo can năm ——
        put(StarId.THIEN_KHOI, Tables.khoi(yearCan, config.khoiViet))
        put(StarId.THIEN_VIET, Tables.viet(yearCan, config.khoiViet))
        put(StarId.THIEN_QUAN, Tables.THIEN_QUAN[c])
        put(StarId.THIEN_PHUC, Tables.THIEN_PHUC[c])
        put(StarId.LUU_HA, Tables.luuHa(yearCan, config.luuHa))
        put(StarId.THIEN_TRU, Tables.THIEN_TRU[c])
        put(StarId.VAN_TINH, locTon + 3)
        put(StarId.DUONG_PHU, locTon + 5)
        put(StarId.QUOC_AN, locTon + 8)

        // —— 5.6 Hỏa Tinh, Linh Tinh ——
        val hoaStart = Tables.hoaStart(group, config.hoaLinh)
        val linhStart = Tables.linhStart(group, config.hoaLinh)
        if (config.hoaLinh == HoaLinhVariant.TRUNG_CHAU) {
            put(StarId.HOA_TINH, hoaStart + h)
            put(StarId.LINH_TINH, linhStart + h)
        } else {
            put(StarId.HOA_TINH, hoaStart + h * dir)
            put(StarId.LINH_TINH, linhStart - h * dir)
        }

        // —— 7. Tứ Hóa gốc: gắn vào cung của sao được hóa ——
        Tables.tuHoa(yearCan, config).forEach { (hoa, target) -> put(hoa.star, pos.getValue(target)) }

        check(pos.size == StarId.entries.size) { "Còn sao chưa an: ${StarId.entries - pos.keys}" }

        // —— Tuần, Triệt ——
        val tuan = Tables.tuan(yearCan, z)
        val triet = Tables.triet(yearCan)

        // —— 12 cung ——
        val byChi = EnumMap<Chi, MutableList<StarId>>(Chi::class.java)
        Chi.entries.forEach { byChi[it] = mutableListOf() }
        pos.forEach { (star, chi) -> byChi.getValue(chi).add(star) } // EnumMap duyệt theo ordinal = thứ tự hiển thị
        val tieuHanStart = Tables.TIEU_HAN_START[group]
        val tieuHanSign = if (input.gender == Gender.NAM) 1 else -1
        val palaces = EnumMap<Chi, Palace>(Chi::class.java)
        for (p in Chi.entries) {
            val roleIndex = p.stepsFrom(menh)
            val daiHanIndex = Math.floorMod(roleIndex * dir, 12)
            palaces[p] = Palace(
                chi = p,
                can = canOf(p),
                role = PalaceRole.entries[roleIndex],
                stars = byChi.getValue(p).map { PlacedStar(it, brightness.brightnessOf(it, p, config), it.kind) },
                daiHanStartAge = n + 10 * daiHanIndex,
                tieuHanChi = Chi.of(z.index + tieuHanSign * (p.index - tieuHanStart.index)),
                hasTuan = p == tuan.first || p == tuan.second,
                hasTriet = p == triet.first || p == triet.second,
                isThan = p == than,
            )
        }

        val dayCanChi = LunarCalendar.dayCanChi(birth.calendarDate)
        val pillars = Pillars(
            year = yearCanChi,
            month = CanChi.ofMonth(yearCan, lunar.month),
            day = dayCanChi,
            hour = CanChi.ofHour(dayCanChi.can, hourChi),
        )

        return Chart(
            input = input,
            lunar = lunar,
            yearCan = yearCan,
            yearChi = z,
            hourChi = hourChi,
            isYangYear = yearCan.isYang,
            banMenh = banMenh,
            cuc = cuc,
            menh = menh,
            than = than,
            palaces = palaces,
            config = config,
            warnings = warnings,
            gender = input.gender,
            amDuongNamNu = amDuong,
            birth = birth,
            pillars = pillars,
            effectiveMonth = m,
            menhChu = Tables.MENH_CHU[menh.index],
            thanChu = Tables.thanChu(z, config.thanChuTy),
            tuan = tuan,
            triet = triet,
            starPositions = pos,
        )
    }
}
