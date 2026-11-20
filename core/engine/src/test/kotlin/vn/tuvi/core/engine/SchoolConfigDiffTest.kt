package vn.tuvi.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi

/**
 * Bất biến 9: đổi một khóa SchoolConfig chỉ làm đổi đúng nhóm sao bị ảnh hưởng (test diff theo từng khóa).
 * So sánh (vị trí, độ sáng) của từng sao giữa cấu hình mặc định và cấu hình đổi một khóa.
 */
class SchoolConfigDiffTest {

    private data class Variation(
        val name: String,
        val config: SchoolConfig,
        val allowed: Set<StarId>,
        /** Điều kiện lá số được phép khác; ngoài điều kiện này phải giống hệt. */
        val appliesTo: (Chart) -> Boolean = { true },
    )

    private val base = SchoolConfig.DEFAULT

    private val variations = listOf(
        Variation("tuHoa = THIEN_LUONG", base.copy(tuHoa = TuHoaVariant.THIEN_LUONG), setOf(StarId.HOA_KHOA, StarId.HOA_KY)) { it.yearCan == Can.CANH },
        Variation("nhamKhoa = THIEN_PHU", base.copy(nhamKhoa = NhamHoaKhoa.THIEN_PHU), setOf(StarId.HOA_KHOA)) { it.yearCan == Can.NHAM },
        Variation("khoiViet = TOAN_THU", base.copy(khoiViet = KhoiVietVariant.TOAN_THU), setOf(StarId.THIEN_KHOI, StarId.THIEN_VIET)) { it.yearCan == Can.CANH },
        Variation("hoaLinh = DAC_LOC", base.copy(hoaLinh = HoaLinhVariant.DAC_LOC), setOf(StarId.HOA_TINH, StarId.LINH_TINH)) { it.yearChi.tamHopGroup == 1 },
        Variation("hoaLinh = TRUNG_CHAU", base.copy(hoaLinh = HoaLinhVariant.TRUNG_CHAU), setOf(StarId.HOA_TINH, StarId.LINH_TINH)),
        Variation("thienGiai = NHAY_CUNG", base.copy(thienGiai = ThienGiaiVariant.NHAY_CUNG), setOf(StarId.THIEN_GIAI)),
        Variation("luuHa = LASOTUVI", base.copy(luuHa = LuuHaVariant.LASOTUVI), setOf(StarId.LUU_HA)) { it.yearCan == Can.DINH || it.yearCan == Can.CANH },
        Variation("cuMonTable = CAM_NANG_V2", base.copy(cuMonTable = CuMonTable.CAM_NANG_V2), setOf(StarId.CU_MON)) {
            it.positionOf(StarId.CU_MON) in setOf(Chi.TY, Chi.NGO, Chi.THAN)
        },
        Variation("thanChuTy = HOA_TINH", base.copy(thanChuTy = ThanChuTy.HOA_TINH), emptySet()),
        Variation("nguyetHan = DAU_QUAN_LUU_THAI_TUE", base.copy(nguyetHan = NguyetHanVariant.DAU_QUAN_LUU_THAI_TUE), emptySet()),
        Variation("daiHanTuHoa = ON", base.copy(daiHanTuHoa = DaiHanTuHoa.ON), emptySet()),
        Variation("leapMonth = SPLIT_15", base.copy(leapMonth = LeapMonthRule.SPLIT_15), StarId.entries.toSet()) {
            it.lunar.isLeap && it.lunar.day >= 16
        },
        Variation("dayBoundary = GIU_NGAY", base.copy(dayBoundary = DayBoundary.GIU_NGAY), StarId.entries.toSet()) { it.birth.lateTy },
    )

    private fun Chart.signature(): Map<StarId, Pair<Chi, Brightness?>> =
        starPositions.mapValues { (star, chi) -> chi to palace(chi).star(star)!!.brightness }

    @TestFactory
    @DisplayName("Bất biến 9: diff theo từng khóa SchoolConfig (3.000 lá số mỗi khóa)")
    fun diffs(): List<DynamicTest> = variations.map { v ->
        DynamicTest.dynamicTest(v.name) {
            var changed = 0
            Prop.forAll(3_000, gen = { r -> RandomBirths.input(r) }) { input ->
                val a = ChartEngine.build(input, base)
                val b = ChartEngine.build(input, v.config)
                val sa = a.signature()
                val sb = b.signature()
                val diff = StarId.entries.filter { sa[it] != sb[it] }.toSet()
                if (!v.appliesTo(a)) {
                    assertEquals(emptySet<StarId>(), diff, "không được khác khi điều kiện không thỏa")
                    assertEquals(a.palaces, b.palaces)
                } else {
                    assertTrue(v.allowed.containsAll(diff), "khác ngoài nhóm cho phép: ${diff - v.allowed}")
                }
                if (diff.isNotEmpty()) changed++
                assertEquals(v.config, b.config)
            }
            if (v.allowed.isNotEmpty()) assertTrue(changed > 0, "biến thể phải có tác dụng trên ít nhất một lá số")
        }
    }

    @Test
    fun `than chu tuoi Ty theo cau hinh`() {
        Prop.forAll(2_000, gen = { r -> RandomBirths.input(r) }) { input ->
            val a = ChartEngine.build(input, base)
            val b = ChartEngine.build(input, base.copy(thanChuTy = ThanChuTy.HOA_TINH))
            if (a.yearChi == Chi.TY) {
                assertEquals(StarId.LINH_TINH, a.thanChu)
                assertEquals(StarId.HOA_TINH, b.thanChu)
            } else {
                assertEquals(a.thanChu, b.thanChu)
            }
        }
    }

    @Test
    fun `preset va nhan truong phai`() {
        assertEquals(SchoolPreset.NAM_PHAI_VN, SchoolConfig.DEFAULT.preset)
        assertEquals("Nam phái VN", SchoolConfig.DEFAULT.label)
        assertEquals("Theo cụ Thiên Lương", SchoolPreset.THIEN_LUONG.config.label)
        assertEquals("Toàn Thư / Trung Châu", SchoolPreset.TOAN_THU_TRUNG_CHAU.config.label)
        assertNull(SchoolConfig(luuHa = LuuHaVariant.LASOTUVI).preset)
        assertEquals("Tùy chỉnh", SchoolConfig(luuHa = LuuHaVariant.LASOTUVI).label)
    }
}
