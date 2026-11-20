package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.BirthInput
import vn.tuvi.core.engine.Chart
import vn.tuvi.core.engine.ChartEngine
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.LimitEngine
import java.time.LocalDateTime

/** Ngữ nghĩa từng loại predicate trên ca golden A (Nam 15/08/1990 10:30) và các tác động suppress / invert / soften. */
class RuleEngineTest {
    private val a: Chart = ChartEngine.build(BirthInput(LocalDateTime.of(1990, 8, 15, 10, 30), null, Gender.NAM))
    private val a2026 = LimitEngine.forYear(a, 2026)

    private fun p(json: String): Predicate = Predicate.parse(Json.parseToJsonElement(json), "test")
    private fun at(json: String, anchor: Chi = Chi.DAN, year: Boolean = false) =
        p(json).eval(EvalContext(a, if (year) a2026 else null), anchor, ArrayList())

    @Test
    fun `sao trong các phạm vi — Mệnh Dần (Vũ Tướng), golden A`() {
        assertTrue(at("""{"star":"VU_KHUC"}"""))
        assertTrue(at("""{"star":"VU_KHUC","brightnessIn":["V"]}"""))
        assertFalse(at("""{"star":"VU_KHUC","brightnessIn":["M"]}"""))
        assertTrue(at("""{"star":"THIEN_TUONG","brightnessIn":["M"]}"""))
        assertTrue(at("""{"star":"PHA_QUAN","in":"xungChieu"}"""), "Phá Quân ở Thân xung Dần")
        assertTrue(at("""{"star":"TU_VI","in":"tamHop"}"""), "Tử Vi Ngọ tam hợp Dần")
        assertTrue(at("""{"star":"LIEM_TRINH","in":"tamHop"}"""), "Liêm Trinh Tuất tam hợp Dần")
        assertFalse(at("""{"star":"TU_VI","in":"xungChieu"}"""))
        assertTrue(at("""{"star":"LOC_TON","in":"tamPhuongTuChinh"}"""))
        assertTrue(at("""{"star":"LOC_TON","in":"tamPhuong"}"""))
        assertFalse(at("""{"star":"VU_KHUC","in":"tamPhuong"}"""), "tamPhuong không gồm chính cung")
        assertTrue(at("""{"star":"CU_MON","in":"giap"}"""), "Cự Môn Sửu giáp Dần")
        assertTrue(at("""{"star":"THAI_DUONG","in":"giap"}"""), "Thái Dương Mão giáp Dần")
        assertTrue(at("""{"star":"THAI_AM","in":"nhiHop"}"""), "Dần nhị hợp Hợi")
        assertTrue(at("""{"star":"THAI_AM","in":"chart","chiIn":["HOI"]}"""))
        assertFalse(at("""{"star":"THAI_AM","in":"chart","chiIn":["TY"]}"""))
    }

    @Test
    fun `countOf, pairGiap, hoaOf, Tuần Triệt, người, cung`() {
        assertTrue(at("""{"countOf":["KINH_DUONG","DA_LA","HOA_TINH","LINH_TINH"],"in":"tamPhuongTuChinh","atLeast":2}"""), "Hỏa Ngọ + Linh Tuất")
        assertFalse(at("""{"countOf":["KINH_DUONG","DA_LA","HOA_TINH","LINH_TINH"],"in":"tamPhuongTuChinh","atLeast":3}"""))
        assertTrue(at("""{"countOf":["@TUAN","@TRIET"],"in":"tamPhuongTuChinh","atLeast":2}"""), "Tuần Tuất, Triệt Ngọ")
        assertTrue(at("""{"pairGiap":["THIEN_DONG","THIEN_LUONG"]}"""), "Đồng Sửu – Lương Mão kẹp Dần")
        assertFalse(at("""{"pairGiap":["TA_PHU","HUU_BAT"]}"""))
        assertTrue(at("""{"hoaOf":"VU_KHUC","hoa":["QUYEN"]}"""), "tuổi Canh: Vũ Khúc hóa Quyền")
        assertTrue(at("""{"hoaOf":"THIEN_DONG","hoa":["KY"]}"""))
        assertFalse(at("""{"hoaOf":"VU_KHUC","hoa":["LOC"]}"""))
        assertTrue(at("""{"hasTuan":true}""", Chi.TUAT)); assertFalse(at("""{"hasTuan":true}"""))
        assertTrue(at("""{"hasTriet":true}""", Chi.MUI))
        assertFalse(at("""{"isVCD":true}"""))
        assertTrue(at("""{"isThan":true}""", Chi.TY), "Thân tại Tý")
        assertTrue(at("""{"gender":"NAM","yearCanIn":["CANH"],"amDuongNamNu":["DUONG_NAM"],"thuanLy":true}"""))
        assertTrue(at("""{"chiIn":["DAN"]}""")); assertTrue(at("""{"role":["QUAN_LOC"]}""", Chi.NGO))
        assertTrue(at("""{"at":"role:PHU_THE","hasStar":"THAM_LANG"}"""), "Phu Thê tại Tý có Tham Lang")
        assertTrue(at("""{"at":"star:CU_MON","hasStar":"HOA_KY"}"""))
        assertTrue(at("""{"allOf":[{"star":"VU_KHUC"}],"noneOf":[{"star":"KINH_DUONG"}]}"""))
        assertFalse(at("""{"anyOf":[{"star":"KINH_DUONG"},{"star":"DA_LA"}]}"""))
    }

    @Test
    fun `predicate năm 2026 (Bính Ngọ) — golden A`() {
        assertTrue(at("""{"at":"luuThaiTue","roleIn":["QUAN_LOC"]}""", year = true), "Lưu Thái Tuế Ngọ = Quan Lộc")
        assertTrue(at("""{"at":"tieuHan","chiIn":["THIN"]}""", year = true))
        assertTrue(at("""{"at":"luuNienDaiHan","chiIn":["TUAT"]}""", year = true))
        assertTrue(at("""{"at":"daiHan","chiIn":["TI"]}""", year = true))
        assertTrue(at("""{"luuHoaOf":"LIEM_TRINH","hoa":["KY"]}""", year = true), "năm Bính: Liêm Trinh hóa Kỵ")
        assertTrue(at("""{"thienThoiIn":["TUOI_KHAC_DAI_HAN"]}""", year = true))
        assertTrue(at("""{"ageRange":[37,37]}""", year = true))
        assertTrue(at("""{"luuStar":"THAI_TUE","in":"chart","chiIn":["NGO"]}""", year = true))
        assertFalse(at("""{"luuStar":"THAI_TUE","in":"chart","chiIn":["NGO"]}"""), "không có vận hạn → sai")
    }

    @Test
    fun `parse báo lỗi rõ ràng`() {
        assertThrows(KbException::class.java) { p("""{"star":"SAO_LA"}""") }
        assertThrows(KbException::class.java) { p("""{"star":"TU_VI","in":"dauDo"}""") }
        assertThrows(KbException::class.java) { p("""{"star":"TU_VI","foo":1}""") }
        assertThrows(KbException::class.java) { p("""{"countOf":["TU_VI"],"atLeast":3}""") }
        assertThrows(KbException::class.java) { p("""{}""") }
    }

    @Test
    fun `luật lá số gốc trên golden A — các luật chắc chắn đúng và chắc chắn sai`() {
        val e = RuleEngine(Fixtures.kb).evaluate(a)
        val ids = e.firedRules.map { it.first.id }.toSet()
        assertTrue("TS_THIEN_TUONG_XET_GIAP" in ids, "Thiên Tướng thủ Mệnh")
        assertTrue("TS_AM_DUONG_THUAN_LY" in ids)
        assertFalse("CC_TAI_AM_GIAP_AN" in ids, "Cự Môn có Hóa Kỵ, không có Lộc")
        assertFalse("CC_PHU_TUONG_TRIEU_VIEN" in ids, "Địa Không ở Ngọ trong tam phương")
        assertFalse("TS_VCD_MUON_XUNG" in ids)
        assertTrue(ids.none { r -> Fixtures.kb.rule(r)!!.scope != Scope.GOC }, "không có vận hạn thì không chạy luật năm")
        val y = RuleEngine(Fixtures.kb).evaluate(a, a2026)
        val yids = y.firedRules.map { it.first.id }.toSet()
        assertTrue("VH_LUU_THAI_TUE_QUAN_LOC" in yids)
        assertTrue("VH_THIEN_THOI_TUOI_KHAC_DAI_HAN" in yids)
        // Finding năm có chủ đề theo cung (@PALACE): Lưu Thái Tuế vào Quan Lộc → Sự nghiệp.
        assertTrue(y.findings.any { it.ruleId == "VH_LUU_THAI_TUE_QUAN_LOC" && it.topic == Topic.SU_NGHIEP && it.palace == Chi.NGO })
    }

    @Test
    fun `suppress, invert, soften và luật chuyên sâu`() {
        val fx = Fixtures.load()
        val kb = Fixtures.kb
        val engine = RuleEngine(kb)
        fun findingsOf(rule: String): Evaluation {
            val spec = fx.getValue(rule).first
            val c = spec.chart()
            return engine.evaluate(c, spec.limits(c), includeExpert = true)
        }
        // Thạch trung ẩn ngọc chặn câu đơn sao Cự Môn ở Mệnh.
        val t = findingsOf("CC_THACH_TRUNG_AN_NGOC")
        assertTrue(t.findings.none { it.ruleId.startsWith("STAR_CU_MON_") && it.ruleId.endsWith("@MENH") })
        assertTrue(t.suppressed.any { it.first.ruleId.startsWith("STAR_CU_MON_") && it.second == "CC_THACH_TRUNG_AN_NGOC" })
        // Cự Môn Thìn Tuất tuổi Tân: câu Cự Môn hãm ở Mệnh bị đảo thành tích cực.
        val pv = findingsOf("PV_CU_MON_THIN_TUAT_TAN")
        val cm = pv.findings.first { it.ruleId == "STAR_CU_MON_H@MENH" }
        assertTrue(cm.polarity > 0, "đảo chiều câu Cự Môn hãm: ${cm.polarity}")
        // Bộ đồng cung chặn câu đơn sao trong cùng cung.
        val combo = t.findings.filter { it.ruleId.startsWith("COMBO_") }
        for (f in combo) {
            val role = f.ruleId.substringAfter("@")
            assertTrue(t.findings.none { it.ruleId.startsWith("STAR_") && it.ruleId.endsWith("@$role") })
        }
        // Luật độ tin cậy C chỉ chạy ở chế độ chuyên sâu.
        val spec = fx.getValue("CD_CON_CAI_KHONG_DON_TUYEN").first
        val c = spec.chart()
        assertFalse(engine.evaluate(c).firedRules.any { it.first.id == "CD_CON_CAI_KHONG_DON_TUYEN" })
        assertTrue(engine.evaluate(c, includeExpert = true).firedRules.any { it.first.id == "CD_CON_CAI_KHONG_DON_TUYEN" })
    }

    @Test
    fun `khớp mẫu chặn`() {
        assertTrue(RuleEngine.matches("STAR_CU_MON_*@MENH", "STAR_CU_MON_H@MENH"))
        assertFalse(RuleEngine.matches("STAR_CU_MON_*@MENH", "STAR_CU_MON_H@TAI_BACH"))
        assertTrue(RuleEngine.matches("STAR_*_H@MENH", "STAR_THAI_AM_H@MENH"))
        assertFalse(RuleEngine.matches("STAR_*_H@MENH", "STAR_THAI_AM_M@MENH"))
        assertTrue(RuleEngine.matches("CC_CO_QUAN", "CC_CO_QUAN"))
        assertFalse(RuleEngine.matches("CC_CO_QUAN", "CC_CO_QUAN_X"))
    }

    @Test
    fun `mọi finding đều có bằng chứng và luật có trong KB (mục 14_4 — Vì sao)`() {
        val e = RuleEngine(Fixtures.kb).evaluate(a, a2026)
        for (f in e.findings) {
            assertTrue(f.ruleId.isNotBlank())
            if (!f.ruleId.startsWith("STAR_") && !f.ruleId.startsWith("COMBO_") && !f.ruleId.startsWith("PHU_")) {
                assertTrue(Fixtures.kb.rule(f.ruleId) != null, f.ruleId)
            }
            assertTrue(f.textKey in Fixtures.kb.texts, "thiếu câu ${f.textKey}")
        }
        assertTrue(e.findings.filter { it.type == RuleType.CAT_CACH || it.type == RuleType.HUNG_CACH }.all { it.evidence.isNotEmpty() })
    }

    @Test
    fun `hiệu năng — luận lá số + một năm`() {
        val engine = RuleEngine(Fixtures.kb)
        val charts = (0 until 300).map { i -> ChartEngine.build(BirthInput(LocalDateTime.of(1950 + i % 60, 1 + i % 12, 1 + i % 28, i % 24, 0), null, Gender.entries[i % 2])) }
        repeat(50) { engine.evaluate(charts[it], LimitEngine.forYear(charts[it], 2026)) }
        val t0 = System.nanoTime()
        for (c in charts) engine.evaluate(c, LimitEngine.forYear(c, 2026))
        val ms = (System.nanoTime() - t0) / 1e6 / charts.size
        println("RuleEngine: %.3f ms/lá số (gốc + năm, %d luật)".format(ms, Fixtures.kb.rules.size))
        assertTrue(ms < 20.0, "quá chậm: $ms ms")
    }

    @Test
    fun `đọc JSON luật mẫu của prompt (mục 9)`() {
        val json = """{"rules":[{"id":"CC_THACH_TRUNG_AN_NGOC","name":"Thạch Trung Ẩn Ngọc","type":"CAT_CACH",
          "when":{"palace":"MENH","allOf":[{"star":"CU_MON","in":"self","chiIn":["TY","NGO"]},
            {"anyOf":[{"star":"HOA_LOC","in":"tamPhuongTuChinh"},{"star":"HOA_QUYEN","in":"tamPhuongTuChinh"},{"star":"HOA_KHOA","in":"tamPhuongTuChinh"}]}],
            "noneOf":[{"star":"KINH_DUONG","in":"self"},{"star":"HOA_KY","in":"self"}]},
          "effect":{"topics":["SU_NGHIEP","TAI_CHINH"],"polarity":2,"weight":3.0,"suppress":["STAR_CU_MON_*@MENH"]},
          "textKey":"cc.thach_trung_an_ngoc","adviceKeys":["adv.phat_huy_ngon_ngu","adv.tich_luy_truoc_khi_bung"],"source":"Cẩm nang V2 – P.III.10"}]}"""
        val files = mapOf(
            "index.json" to """{"version":"t","files":{"rules":["r.json"]}}""",
            "r.json" to json,
        )
        val kb = KnowledgeBase.load { files.getValue(it) }
        val r = kb.rules.single()
        assertEquals(4, r.priority, "độ ưu tiên = số predicate lá: Cự Môn + anyOf (tính 1) + 2 noneOf")
        val ref = Fixtures.kb.rule("CC_THACH_TRUNG_AN_NGOC")!!
        // Luật trong KB phải tương đương luật mẫu của prompt trên lá số mẫu.
        val fx = Fixtures.load().getValue("CC_THACH_TRUNG_AN_NGOC")
        for (spec in listOf(fx.first, fx.second)) {
            val c = spec.chart()
            val ctx = EvalContext(c, null)
            assertEquals(RuleEngine(kb).fire(r, ctx).isNotEmpty(), RuleEngine(Fixtures.kb).fire(ref, ctx).isNotEmpty())
        }
        assertTrue(Json.parseToJsonElement(json).jsonObject.isNotEmpty())
    }

    @Test
    fun `không còn hai câu ngược chiều cùng sao cùng chủ đề khác độ ưu tiên (mục 9) — 2000 lá số`() {
        val engine = RuleEngine(Fixtures.kb)
        var resolved = 0
        for (i in 0 until 2000) {
            val c = ChartEngine.build(BirthInput(LocalDateTime.of(1940 + i % 80, 1 + i % 12, 1 + (i * 7) % 28, (i * 5) % 24, 0), null, Gender.entries[i % 2]))
            val e = engine.evaluate(c, LimitEngine.forYear(c, 2026), includeExpert = true)
            val rf = e.findings.filter { it.type != null && it.type != RuleType.PHAN_VI && it.polarity != 0 }
            for (f in rf) for (g in rf) {
                if (f.palace == g.palace && f.scope == g.scope && f.topic == g.topic && f.polarity * g.polarity < 0 && g.priority > f.priority) {
                    val fk = RuleEngine.evidenceKeys(f)
                    assertFalse(fk.any { it.startsWith("*") } && RuleEngine.evidenceKeys(g).containsAll(fk), "${f.ruleId} phải bị ${g.ruleId} chặn")
                }
            }
            resolved += e.suppressed.count { (f, by) -> f.type != null && Fixtures.kb.rule(by)?.effect?.suppress?.isEmpty() != false }
        }
        println("Xung đột luật được giải theo độ ưu tiên: $resolved lần / 2000 lá số")
    }
}
