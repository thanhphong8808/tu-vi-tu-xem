package vn.tuvi.core.interpret

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import vn.tuvi.core.engine.LimitLayers
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.StarId.*
import vn.tuvi.core.engine.TuHoa
import vn.tuvi.core.engine.TuHoaTable
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Đối chiếu độc lập: 27 luật được viết tay lại bằng Kotlin thuần trên mô hình lá số (không qua DSL / Predicate),
 * rồi so với RuleEngine trên 4 000 lá số ngẫu nhiên + toàn bộ lá số mẫu. Bắt lỗi ở bộ đọc DSL, phạm vi cung,
 * neo cung và tầng vận hạn mà test lá số mẫu (dùng chính engine để sinh) không bắt được.
 */
class IndependentRulesTest {
    private class C(val c: Chart, val l: LimitLayers) {
        fun has(chi: Chi, s: StarId) = c.palace(chi).star(s) != null
        fun pos(s: StarId): Chi? = c.palaces.values.firstOrNull { it.star(s) != null }?.chi
        fun role(r: PalaceRole): Chi = c.palaces.values.first { it.role == r }.chi
        fun tpc(a: Chi) = listOf(a, a + 4, a + 8, a + 6)
        fun inTpc(a: Chi, s: StarId) = tpc(a).any { has(it, s) }
        fun bright(chi: Chi, s: StarId): Brightness? = c.palace(chi).star(s)?.brightness
        val m get() = c.menh
    }

    /** Mỗi luật → tập cung neo nó kích hoạt (viết tay). */
    private val hand: Map<String, (C) -> Set<Chi>> = mapOf(
        "CC_TU_PHU_DONG_CUNG" to { x -> x.m.takeIf { x.has(it, TU_VI) && x.has(it, THIEN_PHU) && it in setOf(Chi.DAN, Chi.THAN) }.set() },
        "CC_QUAN_THAN_KHANH_HOI" to { x -> x.m.takeIf { x.has(it, TU_VI) && x.inTpc(it, TA_PHU) && x.inTpc(it, HUU_BAT) }.set() },
        "CC_PHU_TUONG_TRIEU_VIEN" to { x ->
            x.m.takeIf { x.inTpc(it, THIEN_PHU) && x.inTpc(it, THIEN_TUONG) && !x.inTpc(it, DIA_KHONG) && !x.inTpc(it, DIA_KIEP) }.set()
        },
        "CC_CO_NGUYET_DONG_LUONG" to { x -> x.m.takeIf { a -> listOf(THIEN_CO, THAI_AM, THIEN_DONG, THIEN_LUONG).all { x.inTpc(a, it) } }.set() },
        "CC_SAT_PHA_THAM" to { x -> x.m.takeIf { a -> listOf(THAT_SAT, PHA_QUAN, THAM_LANG).any { x.has(a, it) } }.set() },
        "CC_LOC_MA_GIAO_TRI" to { x -> x.m.takeIf { x.inTpc(it, THIEN_MA) && (x.inTpc(it, LOC_TON) || x.inTpc(it, HOA_LOC)) }.set() },
        "CC_THACH_TRUNG_AN_NGOC" to { x ->
            x.m.takeIf {
                x.has(it, CU_MON) && it in setOf(Chi.TY, Chi.NGO) && (x.inTpc(it, HOA_LOC) || x.inTpc(it, HOA_QUYEN) || x.inTpc(it, HOA_KHOA)) &&
                    !x.has(it, KINH_DUONG) && !x.has(it, HOA_KY)
            }.set()
        },
        "CC_TAI_AM_GIAP_AN" to { x ->
            val cm = x.pos(CU_MON)
            x.m.takeIf { x.has(it, THIEN_TUONG) && cm != null && (x.has(cm, HOA_LOC) || x.has(cm, LOC_TON)) }.set()
        },
        "HC_LINH_XUONG_DA_VU" to { x -> x.m.takeIf { a -> listOf(LINH_TINH, VAN_XUONG, DA_LA, VU_KHUC).all { x.inTpc(a, it) } }.set() },
        "HC_LO_THUONG_MAI_THI" to { x ->
            listOf(PalaceRole.MENH, PalaceRole.THIEN_DI, PalaceRole.TAT_ACH).map(x::role)
                .filter { x.has(it, LIEM_TRINH) && x.has(it, THAT_SAT) && it in setOf(Chi.SUU, Chi.MUI) }.toSet()
        },
        "HC_CU_KINH" to { x -> listOf(PalaceRole.MENH, PalaceRole.QUAN_LOC).map(x::role).filter { x.has(it, CU_MON) && x.has(it, KINH_DUONG) }.toSet() },
        "HC_KHONG_KIEP_GIAP_MENH" to { x ->
            val n = setOf(x.m - 1, x.m + 1)
            x.m.takeIf { x.pos(DIA_KHONG) in n && x.pos(DIA_KIEP) in n && x.pos(DIA_KHONG) != x.pos(DIA_KIEP) }.set()
        },
        "HC_HINH_KY_GIAP_AN" to { x ->
            val cm = x.pos(CU_MON); val tl = x.pos(THIEN_LUONG)
            x.m.takeIf { x.has(it, THIEN_TUONG) && cm != null && x.has(cm, HOA_KY) && tl != null && x.has(tl, KINH_DUONG) }.set()
        },
        "PV_CU_MON_THIN_TUAT_TAN" to { x -> x.m.takeIf { x.has(it, CU_MON) && it in setOf(Chi.THIN, Chi.TUAT) && x.c.yearCan == Can.TAN }.set() },
        "PV_NHAT_HAM_TUAN_TRIET" to { x ->
            x.m.takeIf { x.bright(it, THAI_DUONG) == Brightness.HAM && (x.c.palace(it).hasTuan || x.c.palace(it).hasTriet) }.set()
        },
        "TS_VCD_MUON_XUNG" to { x -> x.m.takeIf { x.c.palace(it).isVoChinhDieu }.set() },
        "TS_VCD_TAM_KHONG" to { x ->
            x.m.takeIf { a ->
                val t = x.tpc(a)
                x.c.palace(a).isVoChinhDieu &&
                    listOf(t.any { x.c.palace(it).hasTuan }, t.any { x.c.palace(it).hasTriet }, x.inTpc(a, THIEN_KHONG), x.inTpc(a, DIA_KHONG)).count { it } >= 3
            }.set()
        },
        "TS_TUAN_TRIET_DONG_CUNG" to { x -> Chi.entries.filter { x.c.palace(it).hasTuan && x.c.palace(it).hasTriet }.toSet() },
        "TS_LOC_TON_THU_MENH" to { x -> x.m.takeIf { x.has(it, LOC_TON) }.set() },
        "CG_PHU_MAU_THUA_HUONG" to { x -> x.role(PalaceRole.PHU_MAU).takeIf { a -> listOf(LOC_TON, THIEN_PHU, TUONG_QUAN, QUOC_AN).any { x.has(a, it) } }.set() },
        "CG_PHUC_DUC_DEP" to { x ->
            x.role(PalaceRole.PHUC_DUC).takeIf { a ->
                listOf(THIEN_LUONG, THIEN_DONG, TU_VI, THIEN_PHU, HOA_KHOA, HOA_QUYEN, HOA_LOC, AN_QUANG, THIEN_QUY).count { x.inTpc(a, it) } >= 4
            }.set()
        },
        "CD_HON_NHAN_SAT_PHA_HAM_KY" to { x ->
            x.role(PalaceRole.PHU_THE).takeIf {
                (x.bright(it, THAT_SAT) == Brightness.HAM || x.bright(it, PHA_QUAN) == Brightness.HAM) && x.has(it, HOA_KY)
            }.set()
        },
        // —— Luật năm (tầng vận hạn) ——
        "VH_MENH_VU_KHUC_KY_LUU" to { x -> x.m.takeIf { x.has(it, VU_KHUC) && TuHoaTable.of(x.l.yearCanChi.can, x.c.config)[TuHoa.KY] == VU_KHUC }.set() },
        "VH_LUU_THAI_TUE_QUAN_LOC" to { x -> x.l.luuThaiTue.takeIf { x.c.palace(it).role == PalaceRole.QUAN_LOC }.set() },
        // Trước đại hạn đầu tiên không có đại hạn / lưu niên đại hạn (D-19 bổ sung của LimitEngine).
        "VH_TIEU_HAN_TRUNG_LUU_NIEN" to { x -> x.l.tieuHan.takeIf { !x.l.beforeFirstDaiHan && it == x.l.luuNienDaiHan }.set() },
        "VH_LUU_LOC_TON_MENH" to { x -> x.m.takeIf { x.l.luuStars[LOC_TON] == it }.set() },
        "HC_LINH_XUONG_DA_VU_DAI_HAN" to { x -> x.l.daiHan.takeIf { a -> !x.l.beforeFirstDaiHan && listOf(LINH_TINH, VAN_XUONG, DA_LA, VU_KHUC).all { x.inTpc(a, it) } }.set() },
    )

    private fun Chi?.set(): Set<Chi> = if (this == null) emptySet() else setOf(this)

    @Test
    fun `27 luật viết tay khớp RuleEngine trên 4000 lá số ngẫu nhiên và mọi lá số mẫu`() {
        val kb = Fixtures.kb
        val engine = RuleEngine(kb)
        val rnd = Random(20261120)
        val charts = ArrayList<Pair<Chart, LimitLayers>>()
        repeat(4000) {
            val t = LocalDateTime.of(1930 + rnd.nextInt(100), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28), rnd.nextInt(24), rnd.nextInt(60))
            val c = ChartEngine.build(BirthInput(t, null, if (rnd.nextBoolean()) Gender.NAM else Gender.NU))
            val year = t.year + 1 + rnd.nextInt(80)
            charts += c to LimitEngine.forYear(c, year)
        }
        for ((pos, neg) in Fixtures.load().values) for (s in listOf(pos, neg)) {
            val c = s.chart(); charts += c to (s.limits(c) ?: LimitEngine.forYear(c, s.viewYear ?: (c.lunar.year + 30)))
        }
        val positives = HashMap<String, Int>()
        for ((id, f) in hand) {
            val rule = kb.rule(id) ?: error("không có luật $id trong KB")
            var hits = 0
            for ((c, l) in charts) {
                val want = f(C(c, l))
                val got = engine.fire(rule, EvalContext(c, l)).map { it.first }.toSet()
                assertEquals(want, got, "$id lệch trên lá số ${c.input.solar ?: c.lunar} (năm xem ${l.lunarYear})")
                if (got.isNotEmpty()) hits++
            }
            positives[id] = hits
        }
        println("Đối chiếu độc lập: " + positives.entries.joinToString { "${it.key}=${it.value}" })
        assertTrue(positives.values.all { it > 0 }, "luật nào cũng phải có ít nhất một lá số kích hoạt trong mẫu")
        assertTrue(hand.size >= 27)
    }
}
