package vn.tuvi.core.engine

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.CanChi
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.calendar.LunarCalendar
import java.io.File
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Đối chiếu thẳng với docs/PROMPT_TUVI_APP.md: đọc các bảng dữ liệu trong prompt rồi so với engine.
 * Prompt là nguồn gốc (“prompt thắng”); nếu sửa bảng trong prompt mà quên sửa engine, test này đỏ.
 */
class SpecConformanceTest {

    private val lines: List<String> by lazy {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "docs/PROMPT_TUVI_APP.md").exists()) dir = dir.parentFile
        requireNotNull(dir) { "Không tìm thấy docs/PROMPT_TUVI_APP.md từ ${System.getProperty("user.dir")}" }
        File(dir, "docs/PROMPT_TUVI_APP.md").readLines(Charsets.UTF_8).map { Normalizer.normalize(it, Normalizer.Form.NFC) }
    }

    /** Bảng Markdown đầu tiên sau dòng bắt đầu bằng [heading]; bỏ hàng phân cách. */
    private fun tableAfter(heading: String): List<List<String>> {
        val start = lines.indexOfFirst { it.startsWith(heading) }
        require(start >= 0) { "Không thấy mục “$heading” trong prompt" }
        val first = (start + 1 until lines.size).first { lines[it].startsWith("|") }
        return lines.drop(first).takeWhile { it.startsWith("|") }
            .map { row -> row.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() } }
            .filterNot { row -> row.all { cell -> cell.matches(Regex("-{3,}")) } }
    }

    private fun lineAfter(heading: String, startsWith: String): String {
        val start = lines.indexOfFirst { it.startsWith(heading) }
        return lines.drop(start).first { it.startsWith(startsWith) }
    }

    private fun star(name: String) = StarId.fromVn(name)
    private fun chi(name: String) = Chi.fromVn(name.trim().trim('\\', '[', ']'))

    /** Lá số mẫu có can năm [can] (1984 là Giáp Tý; ngày 1/7 luôn sau Tết). */
    private fun chartWithCan(can: Can, hour: Int = 12): Chart =
        ChartEngine.build(BirthInput(LocalDateTime.of(1984 + can.index, 7, 1, hour, 30), null, Gender.NAM))

    private fun chartWithChi(chi: Chi, hour: Int): Chart {
        val year = (1984..1995).first { CanChi.ofYear(it).chi == chi }
        return ChartEngine.build(BirthInput(LocalDateTime.of(year, 7, 1, hour, 30), null, Gender.NAM))
    }

    @Test
    @DisplayName("Mục 5.5: sao an theo can năm")
    fun `bang sao theo can`() {
        val table = tableAfter("### 5.5")
        val header = table.first()
        assertEquals(Can.entries.map { it.vn }, header.drop(1))
        for (can in Can.entries) {
            val chart = chartWithCan(can)
            assertEquals(can, chart.yearCan)
            for (row in table.drop(1)) {
                val cell = row[can.index + 1]
                if (row[0] == "Triệt") {
                    val (a, b) = cell.split("–").map(::chi)
                    assertEquals(a to b, chart.triet, "Triệt ${can.vn}")
                } else {
                    assertEquals(chi(cell), chart.positionOf(star(row[0])), "${row[0]} tuổi ${can.vn}")
                }
            }
            val lt = chart.positionOf(StarId.LOC_TON)
            assertEquals(lt + 3, chart.positionOf(StarId.VAN_TINH))
            assertEquals(lt + 5, chart.positionOf(StarId.DUONG_PHU))
            assertEquals(lt + 8, chart.positionOf(StarId.QUOC_AN))
        }
    }

    @Test
    @DisplayName("Mục 5.6: cung khởi Hỏa Tinh, Linh Tinh")
    fun `bang Hoa Linh`() {
        for (row in tableAfter("### 5.6").drop(1)) {
            val group = row[0].split(" ").map(::chi)
            for (c in group) {
                val chart = chartWithChi(c, hour = 0) // giờ Tý sớm: Hỏa, Linh nằm đúng cung khởi
                assertEquals(chi(row[1]), chart.positionOf(StarId.HOA_TINH), "Hỏa tuổi ${c.vn}")
                assertEquals(chi(row[2]), chart.positionOf(StarId.LINH_TINH), "Linh tuổi ${c.vn}")
            }
        }
    }

    @Test
    @DisplayName("Mục 5.3: thứ tự bốn vòng sao và cung khởi Tràng Sinh")
    fun `vong sao`() {
        val table = tableAfter("### 5.3").associateBy { it[0] }
        fun names(row: String) = table.getValue(row)[3].split(",").map { it.trim() }
        assertEquals(names("Bác Sĩ"), StarId.BAC_SI_RING.map { it.vn })
        assertEquals(names("Thái Tuế"), StarId.THAI_TUE_RING.map { it.vn })
        assertEquals(names("Tràng Sinh"), StarId.TRANG_SINH_RING.map { it.vn })
        val starts = table.getValue("Tràng Sinh")[1]
        assertTrue(starts.contains("Kim → Tỵ") && starts.contains("Mộc → Hợi") && starts.contains("Thủy và Thổ → Thân") && starts.contains("Hỏa → Dần"))
        assertEquals(Chi.TI, Tables.trangSinhStart(Cuc.KIM_TU))
        assertEquals(Chi.HOI, Tables.trangSinhStart(Cuc.MOC_TAM))
        assertEquals(Chi.THAN, Tables.trangSinhStart(Cuc.THUY_NHI))
        assertEquals(Chi.THAN, Tables.trangSinhStart(Cuc.THO_NGU))
        assertEquals(Chi.DAN, Tables.trangSinhStart(Cuc.HOA_LUC))
    }

    @Test
    @DisplayName("Mục 5.4: bảng theo tam hợp, Cô Thần, Phá Toái")
    fun `bang tam hop`() {
        val row = tableAfter("### 5.4").first { it[0] == "Theo tam hợp T" }[1]
        val lists = Regex("""([^;\\]+?) \\\[([^\]\\]+)\\]""").findAll(row).associate { m ->
            m.groupValues[1].trim() to m.groupValues[2].split(",").map(::chi)
        }
        assertEquals(lists.getValue("Thiên Mã"), Tables.THIEN_MA)
        assertEquals(lists.getValue("Hoa Cái"), Tables.HOA_CAI)
        assertEquals(lists.getValue("Kiếp Sát"), Tables.KIEP_SAT)
        assertEquals(lists.getValue("Đào Hoa"), Tables.DAO_HOA)

        fun mapping(rowName: String): Map<Chi, Chi> = tableAfter("### 5.4").first { it[0] == rowName }[1]
            .split(";").map { it.trim() }.filter { it.contains("→") }
            .flatMap { part ->
                val (from, to) = part.split("→").map { it.trim() }
                from.split(" ").map { chi(it) to chi(to) }
            }.toMap()
        mapping("Cô Thần / Quả Tú").forEach { (z, coThan) -> assertEquals(coThan, Tables.coThan(z), "Cô Thần tuổi ${z.vn}") }
        val phaToai = mapping("Phá Toái")
        assertEquals(12, phaToai.size)
        phaToai.forEach { (z, pos) -> assertEquals(pos, Tables.phaToai(z), "Phá Toái tuổi ${z.vn}") }
    }

    @Test
    @DisplayName("Mục 5.7: Mệnh chủ, Thân chủ")
    fun `menh chu than chu`() {
        fun parse(prefix: String): Map<Chi, String> = lineAfter("### 5.7", prefix).substringAfter(": ").trimEnd('.')
            .split(";").flatMap { part ->
                val words = part.trim().replace(",", "").split(" ")
                val chis = words.takeWhile { w -> Chi.entries.any { it.vn == w } }.map(::chi)
                val starName = words.drop(chis.size).joinToString(" ").substringBefore(" (").trim()
                chis.map { it to starName }
            }.toMap()
        parse("- **Mệnh chủ**").forEach { (c, s) -> assertEquals(star(s), Tables.MENH_CHU[c.index], "Mệnh chủ ${c.vn}") }
        val than = parse("- **Thân chủ**")
        assertEquals(12, than.size)
        than.forEach { (c, s) -> assertEquals(star(s), Tables.thanChu(c, ThanChuTy.LINH_TINH), "Thân chủ ${c.vn}") }
    }

    @Test
    @DisplayName("Mục 6: bảng độ sáng 14 chính tinh và phụ tinh")
    fun `bang do sang`() {
        val table = BrightnessTable.default
        val cfg = SchoolConfig.DEFAULT
        val main = tableAfter("## 6.")
        assertEquals(Chi.entries.map { it.vn }, main.first().drop(1))
        assertEquals(14, main.size - 1)
        for (row in main.drop(1)) {
            Chi.entries.forEach { c ->
                assertEquals(Brightness.fromCode(row[c.index + 1]), table.brightnessOf(star(row[0]), c, cfg), "${row[0]} tại ${c.vn}")
            }
        }
        val phuStart = lines.indexOfFirst { it.startsWith("**Phụ tinh có đắc/hãm**") }
        val phu = lines.drop(phuStart + 1).dropWhile { !it.startsWith("|") }.takeWhile { it.startsWith("|") }
            .map { r -> r.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() } }.drop(2)
        assertEquals(9, phu.size)
        for (row in phu) {
            val dac = row[1].split(",").map(::chi).toSet()
            val other = if (row[2].startsWith("Hãm")) Brightness.HAM else null
            for (name in row[0].split(",").map { it.trim() }) {
                Chi.entries.forEach { c ->
                    val expected = if (c in dac) Brightness.DAC else other
                    assertEquals(expected, table.brightnessOf(star(name), c, cfg), "$name tại ${c.vn}")
                }
            }
        }
    }

    @Test
    @DisplayName("Mục 7: bảng Tứ Hóa mặc định")
    fun `bang Tu Hoa`() {
        val table = tableAfter("## 7.")
        assertEquals(listOf("Can", "Hóa Lộc", "Hóa Quyền", "Hóa Khoa", "Hóa Kỵ"), table.first())
        for (row in table.drop(1)) {
            val actual = Tables.tuHoa(Can.fromVn(row[0]), SchoolConfig.DEFAULT)
            assertEquals(row.drop(1).map(::star), listOf(TuHoa.LOC, TuHoa.QUYEN, TuHoa.KHOA, TuHoa.KY).map { actual.getValue(it) }, row[0])
        }
    }

    @Test
    @DisplayName("Mục 8.1: thiên thời theo ngũ hành tam hợp")
    fun `bang thien thoi`() {
        val rows = tableAfter("**Thiên thời").drop(1)
        assertEquals(ThienThoi.entries.size, rows.size)
        rows.zip(ThienThoi.entries).forEach { (row, tt) ->
            assertEquals(row[0], tt.vn)
            assertEquals(row[1], tt.label)
            assertEquals(row[2].replace("−", "-").replace("+", "").toInt(), tt.score)
        }
    }

    @Test
    @DisplayName("Mục 4: bảng test vector lịch")
    fun `bang test vector lich`() {
        val rows = tableAfter("**Test vector lịch").drop(1)
        assertEquals(9, rows.size)
        for (row in rows) {
            val (d, m, y) = row[0].split("/").map { it.toInt() }
            val tzs = row[1].split(" và ").map { it.trim().toInt() }
            val expected = row[2].split(" và ").map { cell ->
                cell.substringBefore(" (").substringBefore(",").replace(" nhuận", "N").trim()
            }
            tzs.zip(expected).forEach { (tz, exp) ->
                assertEquals(exp, LunarCalendar.solarToLunar(LocalDate.of(y, m, d), tz).toString(), "${row[0]} tz$tz")
            }
            if (row[2].contains("ngày ")) {
                assertEquals(row[2].substringAfter("ngày ").trim(), LunarCalendar.dayCanChi(LocalDate.of(y, m, d)).vn)
            }
            Regex("""\(([^)]+)\)""").find(row[2])?.let { year ->
                assertEquals(year.groupValues[1], CanChi.ofYear(LunarCalendar.solarToLunar(LocalDate.of(y, m, d), tzs[0]).year).vn)
            }
        }
    }

    @Test
    @DisplayName("Mục 14.1: golden.json trùng khối JSON trong prompt")
    fun `golden trung prompt`() {
        val start = lines.indexOfFirst { it.startsWith("### 14.1") }
        val open = (start until lines.size).first { lines[it].startsWith("```json") }
        val close = (open + 1 until lines.size).first { lines[it].startsWith("```") }
        val fromPrompt = Json.parseToJsonElement(lines.subList(open + 1, close).joinToString("\n"))
        val resource = Json.parseToJsonElement(
            javaClass.getResourceAsStream("/golden.json")!!.use { Normalizer.normalize(it.readBytes().toString(Charsets.UTF_8), Normalizer.Form.NFC) },
        )
        assertEquals(fromPrompt, resource)
    }
}
