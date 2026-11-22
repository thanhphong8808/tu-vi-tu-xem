package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId

/**
 * Trợ lý AI sinh lời trên máy (mục 12, D-57): model chỉ DIỄN ĐẠT LẠI câu trả lời đã dựng bởi [QaEngine] / QaAnalyst.
 * Lớp này không gọi model: nó dựng lời nhắc ([AiPrompt]) và kiểm tra đầu ra ([AiGuard], mục 12.3). Nền tảng (Android:
 * LiteRT-LM; máy tính: LiteRT-LM JVM hoặc bản giả lập) chỉ cần một hàm `generate(system, user) → String`.
 */
data class AiModelConfig(
    val id: String,
    val label: String,
    /** “Nhanh” (E2B) hoặc “Mạnh” (E4B): người dùng chọn trong thẻ Trợ lý AI (D-68). */
    val tier: String,
    val runtime: String,
    val file: String,
    val url: String,
    val bytes: Long,
    val sha256: String,
    val minRamMb: Int,
    val maxTokens: Int,
    val maxOutputTokens: Int,
    val contextChars: Int,
    val temperature: Double,
    val topK: Int,
    val topP: Double,
    val minWords: Int,
    val maxWords: Int,
) {
    companion object {
        private fun res(path: String): String =
            (AiModelConfig::class.java.getResourceAsStream(KnowledgeBase.RESOURCE_ROOT + path)
                ?: throw KbException("Không thấy tài nguyên KB: ${KnowledgeBase.RESOURCE_ROOT}$path")).use { it.readBytes().toString(Charsets.UTF_8) }

        /** Mọi model trong `kb/ai/model.json` (D-68: Nhanh E2B, Mạnh E4B); model đầu tiên khi không có `default`. */
        fun all(): List<AiModelConfig> = parseAll(res("ai/model.json")).second

        /** Cấu hình model [id] (null hoặc không có → model mặc định). Đổi model không sửa code (mục 12.1). */
        fun load(id: String? = null): AiModelConfig {
            val (def, list) = parseAll(res("ai/model.json"))
            return list.firstOrNull { it.id == id } ?: list.firstOrNull { it.id == def } ?: list.first()
        }

        fun parseAll(text: String): Pair<String?, List<AiModelConfig>> {
            val o = Json.parseToJsonElement(text) as JsonObject
            val arr = o["models"] as? kotlinx.serialization.json.JsonArray ?: return null to listOf(parse(text))
            return (o["default"] as? JsonPrimitive)?.contentOrNull to arr.map { parse(it.toString()) }.also {
                if (it.isEmpty()) throw KbException("ai/model.json: chưa có model nào")
            }
        }

        /** System prompt chế độ HOI_DAP trong `kb/ai/system_prompt_vi.md` (mục 12.4). */
        fun systemPrompt(cfg: AiModelConfig? = null): String = res("ai/system_prompt_vi.md").trim().let { s ->
            if (cfg == null) s else s.replace("{minWords}", (cfg.minWords + 40).toString()).replace("{maxWords}", (cfg.maxWords - 100).toString())
        }

        fun parse(text: String): AiModelConfig {
            val o = Json.parseToJsonElement(text) as JsonObject
            fun s(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull ?: throw KbException("ai/model.json: thiếu $k")
            fun i(k: String) = (o[k] as? JsonPrimitive)?.intOrNull ?: throw KbException("ai/model.json: thiếu $k")
            fun d(k: String) = (o[k] as? JsonPrimitive)?.doubleOrNull ?: throw KbException("ai/model.json: thiếu $k")
            return AiModelConfig(
                id = s("id"), label = s("label"), tier = (o["tier"] as? JsonPrimitive)?.contentOrNull ?: "Nhanh", runtime = s("runtime"), file = s("file"), url = s("url"),
                bytes = (o["bytes"] as? JsonPrimitive)?.longOrNull ?: throw KbException("ai/model.json: thiếu bytes"),
                sha256 = s("sha256").lowercase(), minRamMb = i("minRamMb"), maxTokens = i("maxTokens"),
                maxOutputTokens = i("maxOutputTokens"), contextChars = i("contextChars"), temperature = d("temperature"),
                topK = i("topK"), topP = d("topP"), minWords = i("minWords"), maxWords = i("maxWords"),
            )
        }
    }
}

/** Lời nhắc cho một câu hỏi: [user] chứa câu hỏi và khối dữ kiện; [facts] là phần dữ kiện dùng để kiểm tra đầu ra. */
data class AiPrompt(val system: String, val user: String, val facts: AiFacts) {
    companion object {
        /** Ý định không đi qua model: khủng hoảng (mục 12.2 bước 6), từ chối, chào hỏi, chưa hiểu. */
        private val SKIP = setOf(QaIntent.CRISIS, QaIntent.REFUSE, QaIntent.GREETING, QaIntent.UNKNOWN)

        /** Câu trả lời này có nên nhờ model diễn đạt lại không. Trả về lý do không, hoặc null nếu nên. */
        fun skipReason(a: QaAnswer): String? = when {
            a.parse.intent == QaIntent.CRISIS -> "câu hỏi cần hỗ trợ, không luận số"
            a.parse.intent in SKIP -> "câu hỏi ngoài phần luận lá số"
            a.paragraphs.isEmpty() -> "chưa có đoạn luận để diễn đạt"
            else -> null
        }

        private fun label(t: AdviceType) = when (t) {
            AdviceType.NEN -> "Nên"
            AdviceType.KHONG_NEN -> "Không nên"
            AdviceType.LUU_Y -> "Lưu ý"
        }

        /**
         * Dựng lời nhắc từ câu trả lời của QaAnalyst. Khối dữ kiện không quá [AiModelConfig.contextChars] ký tự
         * (khoảng 2.000 token, mục 12.2 bước 3): đoạn nào vượt thì cắt, nhưng luôn giữ đoạn Kết luận và khuyến nghị.
         */
        fun build(a: QaAnswer, currentLunarYear: Int, cfg: AiModelConfig, system: String = AiModelConfig.systemPrompt(cfg)): AiPrompt {
            val head = buildList {
                add("Câu hỏi được hiểu là: ${a.understood}.")
                add("Năm hiện tại: $currentLunarYear (âm lịch).")
                if (a.score != null) add("Điểm chung: ${a.score}/100" + (a.stance?.let { ", thế ${it.vn}." } ?: "."))
            }
            val adv = a.advice.map { "${label(it.type)}: ${it.text}" } + listOfNotNull(a.closing?.let { "Lời kết: $it" })
            val budget = cfg.contextChars - (head + adv).sumOf { it.length + 1 }
            // Vượt ngân sách thì giữ đoạn quan trọng trước (D-68): kết luận, việc cụ thể trong năm, đồng thanh, các tầng,
            // tháng, từng sao, sách, tổng hợp; đoạn được chọn vẫn xếp theo thứ tự gốc.
            val lines = a.paragraphs.map { p -> (p.title?.let { "[$it] " } ?: "") + p.text }
            val chosen = arrayOfNulls<String>(lines.size)
            var used = 0
            for (i in lines.indices.sortedBy { priority(a.paragraphs[it], it) }) {
                val room = budget - used
                if (i > 0 && room < 120) continue
                val cut = if (lines[i].length > room) cutAtSentence(lines[i], room) else lines[i]
                chosen[i] = cut
                used += cut.length + 1
            }
            val paras = chosen.filterNotNull()
            val facts = AiFacts(
                lines = head + paras + adv,
                score = a.score,
                question = a.question,
            )
            val user = buildString {
                append("<cau_hoi>").append(a.question.trim()).append("</cau_hoi>\n<du_kien>\n")
                facts.lines.forEach { append(it).append('\n') }
                append("</du_kien>")
            }
            return AiPrompt(system, user, facts)
        }

        private fun priority(p: Paragraph, i: Int): Int {
            if (i == 0) return 0
            val t = p.title ?: return 4
            return when {
                "Cụ thể" in t || "đã có gì" in t || "chạm vào đâu" in t -> 1
                "Đồng thanh" in t || "Việc nổi lên" in t || t.startsWith("Năm ") -> 2
                "Lá số gốc" in t || "Đại hạn" in t || "Tiểu hạn" in t || "Lưu niên" in t || "Lưu Thái Tuế" in t || "Tứ Hóa" in t -> 3
                "Theo tháng" in t || "Vì sao" in t -> 4
                "Từng sao" in t || "Bản mệnh" in t -> 5
                "Sách bàn" in t -> 6
                else -> 7
            }
        }

        private fun cutAtSentence(s: String, max: Int): String {
            if (max <= 0) return ""
            val part = s.take(max)
            val end = maxOf(part.lastIndexOf(". "), part.lastIndexOf("; "))
            return if (end > max / 3) part.substring(0, end + 1) else part.trimEnd() + "…"
        }
    }
}

/** Dữ kiện đưa cho model, dạng từng dòng (mỗi dòng một đoạn luận). Kiểm tra đầu ra chỉ chấp nhận những gì có ở đây. */
data class AiFacts(val lines: List<String>, val score: Int?, val question: String) {
    val text: String by lazy { lines.joinToString("\n") }
}

/** Kết quả kiểm tra: [text] là câu trả lời đã làm sạch (khi đạt); [issues] rỗng = đạt. */
data class AiCheck(val text: String, val issues: List<String>, val removed: List<String> = emptyList()) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Kiểm tra đầu ra của model (mục 12.3) trước khi hiện cho người xem. Trượt bất kỳ điều nào → app hiện lại đúng câu
 * của QaAnalyst. Các bước:
 * 1. Đọc được JSON `{"answer": "..."}` (chấp nhận khi model bọc trong ```json … ```).
 * 2. Độ dài trong [AiModelConfig.minWords]…[AiModelConfig.maxWords] chữ; chỉ tiếng Việt (không chữ Hán/Kirin/Thái, không câu tiếng Anh).
 * 3. Không từ cấm ([KbLint.FORBIDDEN]).
 * 4. Mọi tên sao, tên cung nhắc tới đều có trong dữ kiện.
 * 5. Sao nào gắn với cung/chi nào trong một câu thì cặp đó phải cùng xuất hiện trong một đoạn dữ kiện (không gán sao sai cung).
 * 6. Mọi con số có trong dữ kiện hoặc câu hỏi; điểm “N/100” phải là điểm có trong dữ kiện; nêu đúng điểm chung.
 */
object AiGuard {
    /** Tên sao để dò: bỏ tên một chữ của vòng Tràng Sinh (Suy, Bệnh, Tử…: dễ trùng lời thường) và “Phúc Đức” (trùng tên cung). */
    private val STAR_NAMES: List<String> = StarId.entries.map { it.vn }.distinct()
        .filter { ' ' in it && it != PalaceRole.PHUC_DUC.vn }.sortedByDescending { it.length }
    private val ROLE_NAMES: List<String> = PalaceRole.entries.map { it.vn }.sortedByDescending { it.length }
    private val CHI_NAMES: List<String> = Chi.entries.map { it.vn }
    /** Cụm có chữ “Tử Vi” nhưng không phải sao Tử Vi. */
    private val NOT_STAR = listOf("Tử Vi Đẩu Số", "Tử Vi Tự Xem", "lá số Tử Vi", "môn Tử Vi", "xem Tử Vi", "theo Tử Vi")
    private val ENGLISH = Regex("\\b(the|and|is|are|your|you|with|this|that|of|for)\\b", RegexOption.IGNORE_CASE)
    private val FOREIGN_SCRIPT = Regex("[\\u0400-\\u04FF\\u0E00-\\u0E7F\\u3040-\\u30FF\\u3400-\\u9FFF\\uAC00-\\uD7AF]")

    /** Lấy chuỗi answer từ đầu ra thô của model; null nếu không đọc được JSON đúng dạng. */
    fun extract(raw: String): String? {
        var s = raw.trim()
        if (s.startsWith("```")) s = s.substringAfter('\n', "").substringBeforeLast("```").trim()
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        val strict = if (start < 0 || end <= start) null else try {
            val o = Json.parseToJsonElement(s.substring(start, end + 1)) as? JsonObject
            (o?.get("answer") as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
        return strict ?: lenient(s)
    }

    /**
     * Model nhỏ đôi khi viết đúng `{"answer": "…` nhưng quên đóng chuỗi hoặc thừa ký tự cuối (D-68). Lấy phần chuỗi sau
     * `"answer": "` tới dấu nháy kép cuối cùng chưa thoát (hoặc hết đầu ra), rồi giải mã \n, \", \\. Nội dung vẫn đi qua
     * toàn bộ kiểm tra như thường.
     */
    private fun lenient(s: String): String? {
        val m = Regex("\"answer\"\\s*:\\s*\"").find(s) ?: return null
        val body = s.substring(m.range.last + 1)
        var endQ = -1
        var i = 0
        while (i < body.length) {
            when (body[i]) {
                '\\' -> i++
                '"' -> endQ = i
            }
            i++
        }
        val raw = if (endQ >= 0) body.substring(0, endQ) else body
        val out = StringBuilder()
        var k = 0
        while (k < raw.length) {
            val c = raw[k]
            if (c == '\\' && k + 1 < raw.length) {
                when (raw[k + 1]) { 'n' -> out.append('\n'); 't' -> out.append(' '); '"' -> out.append('"'); '\\' -> out.append('\\'); '/' -> out.append('/'); else -> out.append(raw[k + 1]) }
                k += 2
            } else { out.append(c); k++ }
        }
        return out.toString().trim().trimEnd('}', ',', ' ', '"').trim().takeIf { it.length >= 20 }
    }

    /** Làm sạch: bỏ ký hiệu markdown, gộp khoảng trắng; giữ xuống đoạn. */
    fun clean(s: String): String = s
        .replace("**", "").replace("__", "")
        .lines().joinToString("\n") { it.trim().removePrefix("#").removePrefix("#").removePrefix("- ").removePrefix("* ").trim() }
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    fun words(s: String): Int = s.split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }

    /** Các tên trong [names] xuất hiện trong [text] (so khớp dài trước, theo ranh giới từ, phân biệt hoa thường). */
    internal fun mentions(text: String, names: List<String>): Set<String> {
        val found = LinkedHashSet<String>()
        val taken = BooleanArray(text.length)
        for (n in names) {
            var i = text.indexOf(n)
            while (i >= 0) {
                val e = i + n.length
                val okB = i == 0 || !text[i - 1].isLetterOrDigit()
                val okA = e >= text.length || !text[e].isLetterOrDigit()
                if (okB && okA && (i until e).none { taken[it] }) {
                    found += n
                    for (k in i until e) taken[k] = true
                }
                i = text.indexOf(n, i + 1)
            }
        }
        return found
    }

    /** Chi đứng sau “tại / ở / cung / đóng” (vd. “tại Thân”), tránh nhầm với tên năm “Đinh Mùi”. */
    internal fun placedChi(text: String): Set<String> {
        val out = LinkedHashSet<String>()
        for (c in CHI_NAMES) {
            if (Regex("(?<![\\p{L}])(tại|ở|cung|đóng|đóng tại|nằm ở|rơi vào)\\s+$c(?![\\p{L}])").containsMatchIn(text)) out += c
        }
        return out
    }

    private fun stripNotStar(s: String): String = NOT_STAR.fold(s) { acc, p -> acc.replace(p, " ").replace(p.lowercase(), " ") }

    /** Câu của dữ kiện: tách theo dấu chấm (giữ “;” và “:” trong câu, vì QaAnalyst viết “cung X tại Y: sao…; tam hợp…”). */
    internal fun factSentences(s: String): List<String> = s.split(Regex("(?<=[.!?…])\\s+|\\n+")).filter { it.isNotBlank() }

    /** Mệnh đề của câu AI: tách thêm theo “;”. */
    internal fun clauses(s: String): List<String> = s.split(Regex("(?<=[.!?…;])\\s+|\\n+")).filter { it.isNotBlank() }

    /** Vị trí các tên trong [names] (dài trước, không chồng nhau). */
    internal fun positions(text: String, names: List<String>): List<Pair<String, Int>> {
        val out = ArrayList<Pair<String, Int>>()
        val taken = BooleanArray(text.length)
        for (n in names) {
            var i = text.indexOf(n)
            while (i >= 0) {
                val e = i + n.length
                if ((i == 0 || !text[i - 1].isLetterOrDigit()) && (e >= text.length || !text[e].isLetterOrDigit()) && (i until e).none { taken[it] }) {
                    out += n to i
                    for (k in i until e) taken[k] = true
                }
                i = text.indexOf(n, i + 1)
            }
        }
        return out
    }

    /** Cung (tên cung) và chi đứng sau “tại / ở / cung / rơi vào…” trong một câu, kèm vị trí. */
    private fun placePositions(sen: String): List<Pair<String, Int>> {
        val out = ArrayList(positions(sen, ROLE_NAMES))
        for (c in CHI_NAMES) {
            Regex("(?<![\\p{L}])(?:tại|ở|cung|đóng|rơi vào|nằm ở)\\s+($c)(?![\\p{L}])").findAll(sen).forEach { out += c to it.groups[1]!!.range.first }
        }
        return out
    }

    /** Với mỗi sao trong dữ kiện: các cung, chi cùng câu hoặc ở câu liền trước. */
    internal fun starPlaces(lines: List<String>): Map<String, Set<String>> {
        val m = HashMap<String, MutableSet<String>>()
        for (line in lines) {
            val sens = factSentences(stripNotStar(line))
            // Đoạn liệt kê sao của MỘT cung (D-68): “[Từng sao trong cung X (chi)] …” và “[① Lá số gốc] Cung X an tại Y: …”.
            // Câu không tự nêu nơi chốn thì thuộc cung đó.
            val title = Regex("^\\[([^\\]]+)]").find(line)?.groupValues?.get(1).orEmpty()
            val ctx: Set<String> = when {
                "Từng sao trong cung" in title -> mentions(title, ROLE_NAMES) + mentions(title, CHI_NAMES)
                "Lá số gốc" in title && sens.isNotEmpty() -> sens[0].substringBefore(":").let { mentions(it, ROLE_NAMES) + mentions(it, CHI_NAMES) }
                else -> emptySet()
            }
            for ((i, sen) in sens.withIndex()) {
                val stars = mentions(sen, STAR_NAMES)
                if (stars.isEmpty()) continue
                val win = if (i > 0) sens[i - 1] + " " + sen else sen
                val own = mentions(sen, ROLE_NAMES) + mentions(sen, CHI_NAMES)
                val pl = mentions(win, ROLE_NAMES) + mentions(win, CHI_NAMES) + (if (own.isEmpty() || i == 0) ctx else emptySet())
                for (st in stars) m.getOrPut(st) { HashSet() } += pl
            }
        }
        return m
    }

    internal fun sentences(s: String): List<String> = s.split(Regex("(?<=[.!?…;])\\s+|\\n+")).filter { it.isNotBlank() }

    private fun numbers(s: String): Set<String> = Regex("\\d+").findAll(s).map { it.value.trimStart('0').ifEmpty { "0" } }.toSet()

    /** Tầng vận hạn và cách gọi trong lời (dài trước); khóa trùng tên dòng “→ Tầng: N/100” của QaAnalyst. */
    private val LAYERS = listOf(
        "Lưu niên đại hạn" to listOf("lưu niên đại hạn", "lưu niên"),
        "Lưu Thái Tuế" to listOf("lưu thái tuế", "thái tuế"),
        "Tiểu hạn" to listOf("tiểu hạn"),
        "Đại hạn" to listOf("đại hạn", "đại vận"),
        "Nền gốc" to listOf("lá số gốc", "nền gốc", "gốc"),
    )

    /** Điểm gán cho tầng nào thì phải đúng điểm của tầng đó trong dữ kiện (vd. không lấy điểm tiểu hạn nói là đại hạn). */
    internal fun layerScoreIssues(text: String, facts: AiFacts): List<String> {
        val known = HashMap<String, MutableSet<String>>()
        for ((k, _) in LAYERS) {
            Regex("→ ${Regex.escape(k)}: (\\d+)/100").findAll(facts.text).forEach { known.getOrPut(k) { HashSet() } += it.groupValues[1] }
        }
        if (known.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        for (sen in sentences(text)) {
            val vals = Regex("(\\d+)\\s*/\\s*100").findAll(sen).map { it.groupValues[1] }.toList()
            if (vals.isEmpty()) continue
            var low = sen.lowercase()
            val hit = ArrayList<String>()
            for ((k, names) in LAYERS) for (n in names) {
                val m = Regex("(?<![\\p{L}])${Regex.escape(n)}(?![\\p{L}])").find(low) ?: continue
                hit += k
                low = low.replace(Regex("(?<![\\p{L}])${Regex.escape(n)}(?![\\p{L}])"), " ")
                if (m.value.isNotEmpty()) break
            }
            if (hit.isEmpty() || hit.any { it !in known }) continue
            val allowed = hit.flatMap { known.getValue(it) }.toSet() + listOfNotNull(facts.score?.toString())
            vals.filter { it !in allowed }.forEach { out += "điểm $it/100 gán cho ${hit.joinToString("/")} không đúng dữ kiện" }
        }
        return out
    }

    /**
     * Kiểm tra; nếu lỗi duy nhất là quên nêu điểm chung thì app tự thêm câu “Điểm chung: N/100.” (dữ kiện của chính app,
     * không phải lời model) vào đầu rồi kiểm tra lại toàn bộ.
     */
    fun check(raw: String, facts: AiFacts, cfg: AiModelConfig): AiCheck {
        val r = checkOnce(raw, facts, cfg)
        val sc = facts.score ?: return r
        if (r.issues.size != 1 || !r.issues[0].startsWith("chưa nêu điểm chung")) return r
        val fixed = "Điểm chung: $sc/100. " + r.text
        val again = checkOnce(Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject { put("answer", kotlinx.serialization.json.JsonPrimitive(fixed)) }), facts, cfg)
        return if (again.ok) again else r
    }

    private fun checkOnce(raw: String, facts: AiFacts, cfg: AiModelConfig): AiCheck {
        val answer = extract(raw) ?: return AiCheck("", listOf("không đọc được JSON {\"answer\": …}"))
        val text = clean(answer)
        val issues = ArrayList<String>()
        val n = words(text)
        if (n < cfg.minWords) issues += "quá ngắn ($n chữ, tối thiểu ${cfg.minWords})"
        if (n > cfg.maxWords) issues += "quá dài ($n chữ, tối đa ${cfg.maxWords})"
        issues += contentIssues(text, facts)
        facts.score?.let { sc ->
            if (!Regex("(?<!\\d)$sc\\s*(/\\s*100|điểm)|điểm\\s*(chung\\s*)?(là\\s*)?$sc(?!\\d)").containsMatchIn(text)) issues += "chưa nêu điểm chung $sc/100"
        }
        return AiCheck(text, issues.distinct())
    }

    /**
     * Tự sửa (D-69): model nhỏ thường viết đúng phần lớn nhưng sai vài câu (bịa số, gán sao sai cung). Bỏ đúng những câu
     * có lỗi nội dung, giữ nguyên các câu còn lại; nếu phần giữ lại còn đủ dài (≥ [AiModelConfig.minWords] chữ và ≥ 60%
     * số câu) thì kiểm tra lại TOÀN BỘ như câu mới. Câu đã bỏ ghi trong [AiCheck.removed].
     */
    fun checkAndRepair(raw: String, facts: AiFacts, cfg: AiModelConfig): AiCheck {
        val first = check(raw, facts, cfg)
        if (first.ok) return first
        val answer = extract(raw) ?: return first
        val sens = clean(answer).split(Regex("(?<=[.!?…])\\s+")).filter { it.isNotBlank() }
        if (sens.size < 3) return first
        val bad = sens.filter { contentIssues(it, facts).isNotEmpty() }
        val kept = sens - bad.toSet()
        if (bad.isEmpty() || kept.size < (sens.size * 0.6).toInt().coerceAtLeast(2) || words(kept.joinToString(" ")) < cfg.minWords) return first
        var text = kept.joinToString(" ")
        val sc = facts.score
        if (sc != null && !Regex("(?<!\\d)$sc\\s*(/\\s*100|điểm)").containsMatchIn(text)) text = "Điểm chung: $sc/100. $text"
        val again = checkOnce(Json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject { put("answer", kotlinx.serialization.json.JsonPrimitive(text)) }), facts, cfg)
        return if (again.ok) again.copy(removed = bad.map { s -> s.take(160) + " ← " + contentIssues(s, facts).joinToString("; ") }) else first
    }

    /** Đầu ra có dấu hiệu hỏng (nhiều số dài vô nghĩa, chữ nước ngoài): thường do phần cứng sinh sai, nên thử lại trên CPU. */
    fun looksGarbled(c: AiCheck): Boolean =
        c.issues.count { Regex("^số \\d{3,} không có").containsMatchIn(it) && !Regex("^số (19|20)\\d\\d ").containsMatchIn(it) } >= 2 ||
            c.issues.any { "không phải tiếng Việt" in it || "tiếng Anh" in it }

    /** Lỗi nội dung của một đoạn (dùng cho cả câu trả lời lẫn từng câu): chữ nước ngoài, từ cấm, sao/cung lạ, gán sai, số lạ, điểm sai tầng. */
    private fun contentIssues(text: String, facts: AiFacts): List<String> {
        val issues = ArrayList<String>()
        if (FOREIGN_SCRIPT.containsMatchIn(text)) issues += "có chữ không phải tiếng Việt"
        val en = ENGLISH.findAll(text).count()
        if (en >= 3) issues += "có câu tiếng Anh ($en từ)"
        KbLint.forbiddenIn(text).forEach { issues += "từ cấm “$it”" }

        val ctx = facts.text
        val ctxStars = mentions(stripNotStar(ctx), STAR_NAMES)
        val ctxRoles = mentions(ctx, ROLE_NAMES)
        val body = stripNotStar(text)
        mentions(body, STAR_NAMES).filter { it !in ctxStars }.forEach { issues += "sao “$it” không có trong dữ kiện" }
        mentions(body, ROLE_NAMES).filter { it !in ctxRoles }.forEach { issues += "cung “$it” không có trong dữ kiện" }

        // Sao gắn đúng cung: trong mỗi câu, cung/chi đứng GẦN sao nhất phải đi cùng sao đó trong dữ kiện
        // (cùng câu hoặc câu liền trước của một đoạn dữ kiện).
        val near = starPlaces(facts.lines)
        for (sen in clauses(body)) {
            val places = placePositions(sen)
            if (places.isEmpty()) continue
            for ((st, at) in positions(sen, STAR_NAMES)) {
                val pl = places.minByOrNull { (_, i) -> kotlin.math.abs(i - at) }!!.first
                if (pl !in (near[st] ?: emptySet<String>())) issues += "gán “$st” với “$pl” không khớp dữ kiện"
            }
        }

        // Con số: không bịa năm, tháng, tuổi, điểm.
        val known = numbers(ctx) + numbers(facts.question) + setOf("12")
        numbers(text).filter { it !in known }.forEach { issues += "số $it không có trong dữ kiện" }
        val ctxScores = Regex("(\\d+)/100").findAll(ctx).map { it.groupValues[1] }.toSet()
        Regex("(\\d+)\\s*/\\s*100").findAll(text).map { it.groupValues[1] }.filter { it !in ctxScores }.forEach { issues += "điểm $it/100 không có trong dữ kiện" }
        layerScoreIssues(text, facts).forEach { issues += it }
        return issues.distinct()
    }
}

/**
 * Model giả lập cho máy chủ thử giao diện và test (không phải AI): ghép lại đúng các câu của dữ kiện thành một đoạn
 * trả lời dạng JSON như model thật. Dùng để thử luồng hiển thị, kiểm tra, rơi về bản QaAnalyst mà không cần model 2,6 GB.
 */
object AiFake {
    fun generate(user: String): String {
        val block = user.substringAfter("<du_kien>", "").substringBefore("</du_kien>")
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val score = lines.firstOrNull { it.startsWith("Điểm chung:") }?.removePrefix("Điểm chung:")?.trim()
        val out = StringBuilder()
        if (score != null) out.append("Điểm chung của câu hỏi này là ").append(score.trimEnd('.')).append(". ")
        var words = 0
        for (l in lines) {
            if (l.startsWith("Câu hỏi được hiểu") || l.startsWith("Năm hiện tại") || l.startsWith("Điểm chung")) continue
            val body = l.replace(Regex("^\\[[^\\]]*]\\s*"), "")
            for (s in body.split(Regex("(?<=[.;])\\s+"))) {
                val w = AiGuard.words(s)
                if (words + w > 220) break
                out.append(s.trim()).append(' ')
                words += w
            }
            if (words >= 160) break
        }
        val text = out.toString().trim().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
        return "{\"answer\": \"$text\"}"
    }
}
