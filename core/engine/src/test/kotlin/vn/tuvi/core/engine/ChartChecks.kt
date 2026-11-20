package vn.tuvi.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.hourChiOf

/** Bộ bất biến mục 14.2 dùng chung cho property test (ngẫu nhiên) và StressCheck (vét cạn). */
object ChartChecks {
    private val tuSinh = setOf(Chi.DAN, Chi.THAN, Chi.TI, Chi.HOI)
    private val tuMo = setOf(Chi.THIN, Chi.TUAT, Chi.SUU, Chi.MUI)

    private fun Chart.at(star: StarId): Int = positionOf(star).index
    private fun sum(a: Int, b: Int): Int = Math.floorMod(a + b, 12)

    fun assertInvariants(chart: Chart, config: SchoolConfig) {
        val p = chart.palaces

        // Toàn vẹn: 12 cung, mỗi sao đúng một cung, vai trò không trùng, đúng một cung Thân.
        assertEquals(12, p.size)
        assertEquals(StarId.entries.size, p.values.sumOf { it.stars.size }, "tổng số sao")
        assertEquals(StarId.entries.toSet(), chart.starPositions.keys)
        chart.starPositions.forEach { (star, chi) -> assertTrue(p.getValue(chi).has(star), "$star phải ở $chi") }
        assertEquals(PalaceRole.entries.toSet(), p.values.map { it.role }.toSet())
        assertEquals(PalaceRole.MENH, p.getValue(chart.menh).role)
        assertEquals(listOf(chart.than), p.values.filter { it.isThan }.map { it.chi })
        p.values.forEach { palace ->
            assertEquals(palace.stars.sortedBy { it.id.ordinal }, palace.stars, "thứ tự hiển thị ở ${palace.chi}")
            palace.chinhTinh.forEach { assertNotNull(it.brightness, "${it.id} phải có độ sáng") }
            assertEquals(Chi.of(palace.role.ordinal + chart.menh.index), palace.chi)
        }
        assertEquals(p.getValue(Chi.DAN).can, p.getValue(Chi.TY).can, "Tý trùng can Dần")
        assertEquals(p.getValue(Chi.MAO).can, p.getValue(Chi.SUU).can, "Sửu trùng can Mão")
        assertEquals(Chi.entries.toSet(), p.values.map { it.tieuHanChi }.toSet(), "chi tiểu hạn phủ đủ 12 chi")
        chart.birth.referenceTime?.let { assertEquals(hourChiOf(it.hour), chart.hourChi) }

        // 1. Thiên Phủ = (4 − Tử Vi) mod 12; đúng 14 chính tinh.
        assertEquals(Math.floorMod(4 - chart.at(StarId.TU_VI), 12), chart.at(StarId.THIEN_PHU))
        assertEquals(14, p.values.sumOf { it.chinhTinh.size })

        // 2. Kình = Lộc Tồn + 1, Đà = Lộc Tồn − 1; Lộc Tồn không ở Tứ Mộ; Triệt không ở Tuất, Hợi.
        val lt = chart.at(StarId.LOC_TON)
        assertEquals(sum(lt, 1), chart.at(StarId.KINH_DUONG))
        assertEquals(sum(lt, -1), chart.at(StarId.DA_LA))
        assertFalse(chart.positionOf(StarId.LOC_TON) in tuMo, "Lộc Tồn ở Tứ Mộ")
        assertFalse(chart.triet.first in setOf(Chi.TUAT, Chi.HOI) || chart.triet.second in setOf(Chi.TUAT, Chi.HOI))

        // 3. Thiên Tướng giữa Cự Môn (−1) và Thiên Lương (+1); Sát Phá Tham tam hợp.
        val tt = chart.at(StarId.THIEN_TUONG)
        assertEquals(sum(tt, -1), chart.at(StarId.CU_MON))
        assertEquals(sum(tt, 1), chart.at(StarId.THIEN_LUONG))
        val spt = listOf(StarId.THAT_SAT, StarId.PHA_QUAN, StarId.THAM_LANG).map { chart.at(it) }
        assertEquals(setOf(0, 4, 8), spt.map { Math.floorMod(it - spt[0], 12) }.toSet(), "Sát Phá Tham tam hợp")

        // 4. Không có cặp cung đối nhau cùng vô chính diệu.
        Chi.entries.forEach { c ->
            assertFalse(p.getValue(c).isVoChinhDieu && p.getValue(c.opposite).isVoChinhDieu, "$c và ${c.opposite} cùng VCD")
        }

        // 5. Đối xứng trục Sửu–Mùi (tổng ≡ 2), Không–Kiếp ≡ 10, Khốc–Hư ≡ 0, Hồng Loan–Thiên Hỷ lệch 6.
        listOf(
            StarId.VAN_XUONG to StarId.VAN_KHUC, StarId.TA_PHU to StarId.HUU_BAT,
            StarId.LONG_TRI to StarId.PHUONG_CAC, StarId.TAM_THAI to StarId.BAT_TOA,
            StarId.AN_QUANG to StarId.THIEN_QUY,
        ).forEach { (a, b) -> assertEquals(2, sum(chart.at(a), chart.at(b)), "$a + $b") }
        assertEquals(10, sum(chart.at(StarId.DIA_KHONG), chart.at(StarId.DIA_KIEP)))
        assertEquals(0, sum(chart.at(StarId.THIEN_KHOC), chart.at(StarId.THIEN_HU)))
        assertEquals(6, Math.floorMod(chart.at(StarId.THIEN_HY) - chart.at(StarId.HONG_LOAN), 12))

        // 6. Hoa Cái ở Tứ Mộ, Thiên Mã ở Tứ Sinh; Mệnh có Hoa Cái ⇒ Phu Thê có Thiên Mã; Mệnh có Kình ⇒ Phu Thê có Đà.
        assertTrue(chart.positionOf(StarId.HOA_CAI) in tuMo)
        assertTrue(chart.positionOf(StarId.THIEN_MA) in tuSinh)
        val menh = chart.palace(PalaceRole.MENH)
        val phuThe = chart.palace(PalaceRole.PHU_THE)
        if (menh.has(StarId.HOA_CAI)) assertTrue(phuThe.has(StarId.THIEN_MA), "Hoa Cái Mệnh ⇒ Mã Phu Thê")
        if (menh.has(StarId.KINH_DUONG)) assertTrue(phuThe.has(StarId.DA_LA), "Kình Mệnh ⇒ Đà Phu Thê")
        // Đính chính #1 cẩm nang: Quả Tú ở Tứ Mộ, Cô Thần ở Tứ Sinh.
        assertTrue(chart.positionOf(StarId.QUA_TU) in tuMo)
        assertTrue(chart.positionOf(StarId.CO_THAN) in tuSinh)

        // 7. (Thân − Mệnh) mod 12 = 2h; 12 đại hạn nối tiếp, bắt đầu đúng số Cục.
        assertEquals(Math.floorMod(2 * chart.hourChi.index, 12), chart.than.stepsFrom(chart.menh))
        val byAge = p.values.sortedBy { it.daiHanStartAge }
        assertEquals((0 until 12).map { chart.cuc.number + 10 * it }, byAge.map { it.daiHanStartAge })
        assertEquals(chart.menh, byAge.first().chi)
        byAge.zipWithNext().forEach { (a, b) -> assertEquals(a.chi + chart.dir, b.chi, "đại hạn nối tiếp") }

        // Đồng cung cố định và Tứ Hóa bám sao gốc.
        assertEquals(chart.at(StarId.THIEU_DUONG), chart.at(StarId.THIEN_KHONG))
        assertEquals(chart.at(StarId.TU_PHU), chart.at(StarId.NGUYET_DUC))
        assertEquals(chart.at(StarId.PHUC_DUC), chart.at(StarId.THIEN_DUC))
        assertEquals(chart.yearChi, chart.positionOf(StarId.THAI_TUE))
        Tables.tuHoa(chart.yearCan, config).forEach { (hoa, target) ->
            assertEquals(chart.positionOf(target), chart.positionOf(hoa.star), "$hoa phải đồng cung $target")
        }
    }

    /** Kiểm nhanh các tầng vận hạn năm âm [lunarYear] của lá số. */
    fun assertYearLimits(chart: Chart, lunarYear: Int) {
        val y = LimitEngine.forYear(chart, lunarYear)
        val zy = CanChi.ofYear(lunarYear).chi
        assertEquals(zy, y.luuThaiTue)
        assertEquals(zy, chart.palace(y.tieuHan).tieuHanChi)
        if (!y.beforeFirstDaiHan) {
            assertTrue(y.tuoiAm in chart.palace(y.daiHan).daiHanAges || y.tuoiAm >= chart.cuc.number + 120)
            assertEquals(LimitEngine.luuNienDaiHan(y.daiHan, y.namThuDaiHan!!, chart.dir), y.luuNienDaiHan)
        }
        assertEquals(15, y.luuStars.size)
    }
}
