package vn.tuvi.core.interpret

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import java.io.File

/** Thông tin hiển thị phụ tinh: chiều cát/hung mặc định và khi đắc/hãm địa (KB phần c). */
data class MinorMeta(val star: StarId, val polarity: Int, val polarityDac: Int?, val polarityHam: Int?)

/** Bộ đồng cung (KB phần b): hai chính tinh cùng cung. */
data class Combo(val key: String, val name: String, val stars: Set<StarId>)

/**
 * Knowledge base có version (`:data:kb`, mục 3): luật cách cục, câu luận, khuyến nghị, câu trả lời mẫu, siêu dữ liệu
 * sao. Tách khỏi code để cập nhật câu luận không cần sửa engine. Đọc bằng JsonElement (không cần plugin serialization).
 */
class KnowledgeBase(
    val version: String,
    val rules: List<Rule>,
    val texts: Map<String, TextEntry>,
    val advice: Map<String, AdviceEntry>,
    val faq: List<FaqEntry>,
    val combos: List<Combo>,
    val minor: Map<StarId, MinorMeta>,
    /** Mọi file đã đọc, để lint báo đúng chỗ. */
    val files: List<String>,
) {
    private val comboBySet: Map<Set<StarId>, Combo> = combos.associateBy { it.stars }

    fun rule(id: String): Rule? = rules.firstOrNull { it.id == id }
    fun comboKey(a: StarId, b: StarId): String? = comboBySet[setOf(a, b)]?.key

    fun minorKey(star: StarId, role: PalaceRole): String? {
        if (star !in minor) return null
        val specific = "phu.${star.name}.${role.name}"
        if (specific in texts) return specific
        val general = "phu.${star.name}"
        return if (general in texts) general else null
    }

    fun minorPolarity(star: StarId, b: Brightness?): Int {
        val m = minor[star] ?: return 0
        return when (b) {
            Brightness.DAC, Brightness.MIEU, Brightness.VUONG -> m.polarityDac ?: m.polarity
            Brightness.HAM -> m.polarityHam ?: m.polarity
            else -> m.polarity
        }
    }

    companion object {
        const val RESOURCE_ROOT = "/kb/"

        /** Đọc KB đóng gói trong classpath (app Android, test). */
        fun load(): KnowledgeBase = load { path ->
            val stream = KnowledgeBase::class.java.getResourceAsStream(RESOURCE_ROOT + path)
                ?: throw KbException("Không thấy tài nguyên KB: $RESOURCE_ROOT$path")
            stream.use { it.readBytes().toString(Charsets.UTF_8) }
        }

        /** Đọc KB từ thư mục (công cụ dev, lint trước khi đóng gói). */
        fun loadDir(dir: File): KnowledgeBase = load { path -> File(dir, path).readText(Charsets.UTF_8) }

        fun load(read: (String) -> String): KnowledgeBase {
            val index = Json.parseToJsonElement(read("index.json")).jsonObj("index.json")
            val version = index.str("version", "index.json")
            val files = index["files"]?.jsonObj("index.json.files") ?: throw KbException("index.json: thiếu files")
            fun list(k: String): List<String> = (files[k] as? JsonArray)?.map { (it as JsonPrimitive).content } ?: emptyList()
            val all = ArrayList<String>()
            val rules = ArrayList<Rule>()
            val texts = LinkedHashMap<String, TextEntry>()
            val advice = LinkedHashMap<String, AdviceEntry>()
            val faq = ArrayList<FaqEntry>()
            val combos = ArrayList<Combo>()
            val minor = LinkedHashMap<StarId, MinorMeta>()
            val dupTexts = ArrayList<String>()
            for (f in list("rules")) {
                all.add(f)
                val root = Json.parseToJsonElement(read(f)).jsonObj(f)
                (root["rules"] as? JsonArray ?: throw KbException("$f: thiếu mảng rules")).forEachIndexed { i, e ->
                    rules.add(parseRule(e.jsonObj("$f[$i]"), f))
                }
            }
            for (f in list("texts")) {
                all.add(f)
                val root = Json.parseToJsonElement(read(f)).jsonObj(f)
                (root["texts"] as? JsonArray ?: throw KbException("$f: thiếu mảng texts")).forEachIndexed { i, e ->
                    val o = e.jsonObj("$f[$i]")
                    val t = TextEntry(o.str("key", "$f[$i]"), o.str("text", "$f[$i]"), o.str("source", "$f[$i]"), o.bool("reviewed") ?: false, f)
                    if (texts.put(t.key, t) != null) dupTexts.add(t.key)
                }
            }
            for (f in list("advice")) {
                all.add(f)
                val root = Json.parseToJsonElement(read(f)).jsonObj(f)
                (root["advice"] as? JsonArray ?: throw KbException("$f: thiếu mảng advice")).forEachIndexed { i, e ->
                    val o = e.jsonObj("$f[$i]")
                    val p = "$f[$i]"
                    val a = AdviceEntry(
                        key = o.str("key", p),
                        type = AdviceType.entries.firstOrNull { it.name == o.str("type", p) } ?: throw KbException("$p: type"),
                        topic = Topic.entries.firstOrNull { it.name == o.str("topic", p) } ?: throw KbException("$p: topic"),
                        text = o.str("text", p), severity = (o["severity"] as? JsonPrimitive)?.intOrNull ?: 1,
                        source = o.str("source", p), reviewed = o.bool("reviewed") ?: false,
                        stances = o.strList("stances").map { n -> Stance.entries.firstOrNull { it.name == n } ?: throw KbException("$p.stances: $n") }.toSet(),
                        uses = o.strList("uses").map { n -> AdviceUse.entries.firstOrNull { it.name == n } ?: throw KbException("$p.uses: $n") }.toSet(),
                    )
                    if (advice.put(a.key, a) != null) throw KbException("$p: trùng khuyến nghị ${a.key}")
                }
            }
            for (f in list("faq")) {
                all.add(f)
                val root = Json.parseToJsonElement(read(f)).jsonObj(f)
                (root["faq"] as? JsonArray ?: throw KbException("$f: thiếu mảng faq")).forEachIndexed { i, e ->
                    val o = e.jsonObj("$f[$i]")
                    val p = "$f[$i]"
                    faq.add(
                        FaqEntry(
                            id = o.str("id", p), question = o.str("question", p), answer = o.str("answer", p),
                            topics = o.strList("topics").map { n -> Topic.entries.firstOrNull { it.name == n } ?: throw KbException("$p.topics: $n") },
                            relatedRules = o.strList("relatedRules"), source = o.str("source", p), reviewed = o.bool("reviewed") ?: false,
                        ),
                    )
                }
            }
            for (f in list("meta")) {
                all.add(f)
                val root = Json.parseToJsonElement(read(f)).jsonObj(f)
                (root["combos"] as? JsonArray)?.forEachIndexed { i, e ->
                    val o = e.jsonObj("$f.combos[$i]")
                    val stars = o.strList("stars").map { parseStar(it, "$f.combos[$i]") }.toSet()
                    if (stars.size != 2) throw KbException("$f.combos[$i]: cần 2 sao khác nhau")
                    combos.add(Combo(o.str("key", "$f.combos[$i]"), o.str("name", "$f.combos[$i]"), stars))
                }
                (root["minor"] as? JsonArray)?.forEachIndexed { i, e ->
                    val o = e.jsonObj("$f.minor[$i]")
                    val s = parseStar(o.str("star", "$f.minor[$i]"), "$f.minor[$i]")
                    minor[s] = MinorMeta(s, o.int("polarity") ?: 0, o.int("polarityDac"), o.int("polarityHam"))
                }
            }
            if (dupTexts.isNotEmpty()) throw KbException("Trùng khóa câu luận: ${dupTexts.take(10)}")
            return KnowledgeBase(version, rules, texts, advice, faq, combos, minor, all)
        }

        private fun parseRule(o: JsonObject, file: String): Rule {
            val id = o.str("id", file)
            val p = "$file#$id"
            val allowed = setOf("id", "name", "type", "scope", "reliability", "when", "effect", "textKey", "adviceKeys", "source", "reviewed", "expertOnly")
            val extra = o.keys - allowed
            if (extra.isNotEmpty()) throw KbException("$p: khóa không hợp lệ $extra")
            val w = o["when"]?.jsonObj("$p.when") ?: throw KbException("$p: thiếu when")
            val scope = Scope.entries.firstOrNull { it.name == (o.strOrNull("scope") ?: "GOC") } ?: throw KbException("$p.scope")
            val anchor: Anchor = when {
                "layer" in w -> Anchor.Layer(LimitPoint.of(w.str("layer", "$p.when")))
                else -> when (val pal = w["palace"]) {
                    null -> Anchor.Roles(listOf(PalaceRole.MENH))
                    is JsonArray -> Anchor.Roles(pal.map { parseRole((it as JsonPrimitive).content, "$p.when.palace") })
                    is JsonPrimitive -> when (pal.content) {
                        "ANY" -> Anchor.AnyPalace
                        "THAN" -> Anchor.ThanPalace
                        else -> Anchor.Roles(listOf(parseRole(pal.content, "$p.when.palace")))
                    }
                    else -> throw KbException("$p.when.palace")
                }
            }
            if (anchor is Anchor.Layer && scope == Scope.GOC) throw KbException("$p: neo theo tầng vận hạn nhưng scope = GOC")
            val condKeys = w.keys - setOf("palace", "layer")
            if (condKeys.isEmpty()) throw KbException("$p.when: không có điều kiện")
            val condition = Predicate.parse(JsonObject(w.filterKeys { it in condKeys }), "$p.when")
            if (scope == Scope.GOC && condition.needsLimits()) throw KbException("$p: điều kiện cần vận hạn nhưng scope = GOC")
            val e = o["effect"]?.jsonObj("$p.effect") ?: throw KbException("$p: thiếu effect")
            val polarity = e.int("polarity") ?: throw KbException("$p.effect.polarity")
            if (polarity !in -2..2) throw KbException("$p.effect.polarity ngoài −2..2")
            val topicNames = e.strList("topics")
            val effect = Effect(
                topics = topicNames.filter { it != "@PALACE" }.map { n -> Topic.entries.firstOrNull { it.name == n } ?: throw KbException("$p.effect.topics: $n") },
                byPalace = "@PALACE" in topicNames,
                polarity = polarity,
                weight = (e["weight"] as? JsonPrimitive)?.doubleOrNull ?: throw KbException("$p.effect.weight"),
                suppress = e.strList("suppress"), soften = e.strList("soften"), invert = e.strList("invert"),
                event = e.strOrNull("event")?.let { n -> Event.entries.firstOrNull { it.name == n } ?: throw KbException("$p.effect.event: $n") },
            )
            if (effect.topics.isEmpty() && !effect.byPalace) throw KbException("$p.effect.topics rỗng")
            return Rule(
                id = id, name = o.str("name", p),
                type = RuleType.entries.firstOrNull { it.name == o.str("type", p) } ?: throw KbException("$p.type"),
                scope = scope,
                reliability = Reliability.entries.firstOrNull { it.name == (o.strOrNull("reliability") ?: "A") } ?: throw KbException("$p.reliability"),
                anchor = anchor, condition = condition, effect = effect,
                textKey = o.str("textKey", p), adviceKeys = o.strList("adviceKeys"),
                source = o.strOrNull("source") ?: "", reviewed = o.bool("reviewed") ?: false, expertOnly = o.bool("expertOnly") ?: false,
                file = file,
            )
        }

        private fun JsonElement.jsonObj(path: String): JsonObject = this as? JsonObject ?: throw KbException("$path: cần object")
        private fun JsonObject.str(k: String, path: String): String = (this[k] as? JsonPrimitive)?.content ?: throw KbException("$path: thiếu “$k”")
        private fun JsonObject.strOrNull(k: String): String? = (this[k] as? JsonPrimitive)?.content
        private fun JsonObject.bool(k: String): Boolean? = (this[k] as? JsonPrimitive)?.booleanOrNull
        private fun JsonObject.int(k: String): Int? = (this[k] as? JsonPrimitive)?.intOrNull
        private fun JsonObject.strList(k: String): List<String> = (this[k] as? JsonArray)?.map { (it as JsonPrimitive).content } ?: emptyList()
    }
}
