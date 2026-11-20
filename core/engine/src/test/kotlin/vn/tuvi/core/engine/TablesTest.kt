package vn.tuvi.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi

class TablesTest {

    private val table = BrightnessTable.default
    private val cfg = SchoolConfig.DEFAULT

    private fun b(star: StarId, chi: Chi, config: SchoolConfig = cfg) = table.brightnessOf(star, chi, config)

    @Test
    fun `bang do sang khop muc 6 va cac dinh chinh cam nang`() {
        assertEquals(Brightness.MIEU, b(StarId.THIEN_CO, Chi.DAU)) // cẩm nang ghi Hãm
        assertEquals(Brightness.DAC, b(StarId.VU_KHUC, Chi.MAO))
        assertEquals(Brightness.DAC, b(StarId.VU_KHUC, Chi.DAU))
        assertEquals(Brightness.HAM, b(StarId.VU_KHUC, Chi.TI))
        assertEquals(Brightness.HAM, b(StarId.VU_KHUC, Chi.HOI))
        assertEquals(Brightness.DAC, b(StarId.THIEN_DONG, Chi.TI))
        assertEquals(Brightness.DAC, b(StarId.LIEM_TRINH, Chi.SUU))
        assertEquals(Brightness.DAC, b(StarId.LIEM_TRINH, Chi.MUI))
        // Cự Môn theo hai bảng.
        assertEquals(Brightness.VUONG, b(StarId.CU_MON, Chi.TY))
        assertEquals(Brightness.DAC, b(StarId.CU_MON, Chi.THAN))
        val v2 = cfg.copy(cuMonTable = CuMonTable.CAM_NANG_V2)
        assertEquals(Brightness.DAC, b(StarId.CU_MON, Chi.TY, v2))
        assertEquals(Brightness.DAC, b(StarId.CU_MON, Chi.NGO, v2))
        assertEquals(Brightness.VUONG, b(StarId.CU_MON, Chi.THAN, v2))
        // Tử Vi, Thiên Phủ không có vị trí hãm; Thái Dương hãm từ Thân đến Tý.
        Chi.entries.forEach {
            assertTrue(b(StarId.TU_VI, it) != Brightness.HAM)
            assertTrue(b(StarId.THIEN_PHU, it) != Brightness.HAM)
        }
        listOf(Chi.THAN, Chi.DAU, Chi.TUAT, Chi.HOI, Chi.TY).forEach { assertEquals(Brightness.HAM, b(StarId.THAI_DUONG, it)) }
        listOf(Chi.DAN, Chi.MAO, Chi.THIN, Chi.TI, Chi.NGO).forEach { assertEquals(Brightness.HAM, b(StarId.THAI_AM, it)) }
    }

    @Test
    fun `phu tinh dac ham va o khong xet`() {
        assertEquals(Brightness.DAC, b(StarId.KINH_DUONG, Chi.THIN))
        assertEquals(Brightness.HAM, b(StarId.KINH_DUONG, Chi.TY))
        assertEquals(Brightness.DAC, b(StarId.DIA_KIEP, Chi.HOI))
        assertEquals(Brightness.HAM, b(StarId.DIA_KIEP, Chi.NGO))
        assertEquals(Brightness.DAC, b(StarId.HOA_KY, Chi.SUU))
        assertNull(b(StarId.HOA_KY, Chi.TY))
        assertNull(b(StarId.THIEN_RIEU, Chi.TY))
        assertNull(b(StarId.TA_PHU, Chi.TY))
        assertNull(b(StarId.BAC_SI, Chi.TY))
    }

    @Test
    fun `nap am 60 hoa giap khop cong thuc`() {
        val suffix = mapOf("Kim" to NguHanh.KIM, "Thủy" to NguHanh.THUY, "Hỏa" to NguHanh.HOA, "Thổ" to NguHanh.THO, "Mộc" to NguHanh.MOC)
        for (i in 0 until 60) {
            val cc = CanChi.fromSexagenary(i)
            val napAm = NapAm.of(cc.can, cc.chi)
            assertEquals(suffix.getValue(napAm.name.substringAfterLast(' ')), napAm.element, "${cc.vn} – ${napAm.name}")
        }
        assertEquals("Hải Trung Kim", NapAm.of(Can.GIAP, Chi.TY).name)
        assertEquals("Kim Bạch Kim", NapAm.of(Can.QUY, Chi.MAO).name)
        assertEquals("Đại Hải Thủy", NapAm.of(Can.QUY, Chi.HOI).name)
    }

    @Test
    fun `tuan triet theo can chi`() {
        assertEquals(Chi.TUAT to Chi.HOI, Tables.tuan(Can.GIAP, Chi.TY))
        assertEquals(Chi.TY to Chi.SUU, Tables.tuan(Can.QUY, Chi.HOI)) // Quý Hợi: Tuần và Triệt cùng Tý–Sửu (10.4.2)
        assertEquals(Chi.TY to Chi.SUU, Tables.triet(Can.QUY))
        assertEquals(Chi.THAN to Chi.DAU, Tables.triet(Can.GIAP))
        assertEquals(Chi.THAN to Chi.DAU, Tables.triet(Can.KY))
        assertEquals(Chi.DAN to Chi.MAO, Tables.triet(Can.NHAM))
    }

    @Test
    fun `tu hoa theo bien the`() {
        val canh = Tables.tuHoa(Can.CANH, cfg)
        assertEquals(StarId.THIEN_DONG, canh[TuHoa.KY])
        assertEquals(StarId.THAI_AM, canh[TuHoa.KHOA])
        val canhTL = Tables.tuHoa(Can.CANH, SchoolPreset.THIEN_LUONG.config)
        assertEquals(StarId.THAI_AM, canhTL[TuHoa.KY])
        assertEquals(StarId.THIEN_DONG, canhTL[TuHoa.KHOA])
        assertEquals(StarId.TA_PHU, Tables.tuHoa(Can.NHAM, cfg)[TuHoa.KHOA])
        assertEquals(StarId.THIEN_PHU, Tables.tuHoa(Can.NHAM, SchoolPreset.THIEN_LUONG.config)[TuHoa.KHOA])
        // Mỗi can đủ bốn hóa, bốn sao khác nhau.
        Can.entries.forEach { assertEquals(4, Tables.tuHoa(it, cfg).values.toSet().size, it.vn) }
    }

    @Test
    fun `menh chu than chu va co than`() {
        assertEquals(StarId.THAM_LANG, Tables.MENH_CHU[Chi.TY.index])
        assertEquals(StarId.PHA_QUAN, Tables.MENH_CHU[Chi.NGO.index])
        assertEquals(StarId.LINH_TINH, Tables.thanChu(Chi.TY, ThanChuTy.LINH_TINH))
        assertEquals(StarId.HOA_TINH, Tables.thanChu(Chi.NGO, ThanChuTy.LINH_TINH))
        assertEquals(Chi.DAN, Tables.coThan(Chi.HOI))
        assertEquals(Chi.TI, Tables.coThan(Chi.THIN))
        assertEquals(Chi.THAN, Tables.coThan(Chi.MUI))
        assertEquals(Chi.HOI, Tables.coThan(Chi.TUAT))
    }
}
