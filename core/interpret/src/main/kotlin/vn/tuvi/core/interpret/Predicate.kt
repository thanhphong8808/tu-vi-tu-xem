package vn.tuvi.core.interpret

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import vn.tuvi.core.calendar.Can
import vn.tuvi.core.calendar.Chi
import vn.tuvi.core.engine.AmDuongNamNu
import vn.tuvi.core.engine.Brightness
import vn.tuvi.core.engine.CucMenhRelation
import vn.tuvi.core.engine.Gender
import vn.tuvi.core.engine.PalaceRole
import vn.tuvi.core.engine.StarId
import vn.tuvi.core.engine.ThienThoi
import vn.tuvi.core.engine.TuHoa

/** Phạm vi tìm sao, tính từ cung neo p (mục 9 bước 1). */
enum class Area(val key: String) {
    SELF("self"),
    XUNG_CHIEU("xungChieu"),
    TAM_HOP("tamHop"),
    /** Xung chiếu + hai cung tam hợp (các cung chiếu về, không gồm chính cung). */
    TAM_PHUONG("tamPhuong"),
    TAM_PHUONG_TU_CHINH("tamPhuongTuChinh"),
    GIAP("giap"),
    NHI_HOP("nhiHop"),
    CHART("chart"),
    ;

    fun contains(anchor: Chi, at: Chi): Boolean {
        val d = Math.floorMod(at.ordinal - anchor.ordinal, 12)
        return when (this) {
            SELF -> d == 0
            XUNG_CHIEU -> d == 6
            TAM_HOP -> d == 4 || d == 8
            TAM_PHUONG -> d == 4 || d == 8 || d == 6
            TAM_PHUONG_TU_CHINH -> d == 0 || d == 4 || d == 8 || d == 6
            GIAP -> d == 1 || d == 11
            NHI_HOP -> Math.floorMod(1 - anchor.ordinal, 12) == at.ordinal
            CHART -> true
        }
    }

    companion object {
        fun of(key: String, path: String): Area = entries.firstOrNull { it.key == key } ?: throw KbException("$path: phạm vi không hợp lệ “$key”")
    }
}

/** Điểm trên lá số dùng trong predicate `at`. */
sealed class Locator {
    data object Self : Locator()
    data object Menh : Locator()
    data object Than : Locator()
    data class Role(val role: PalaceRole) : Locator()
    data class StarAt(val star: StarId) : Locator()
    data class LuuStarAt(val star: StarId) : Locator()
    data class Point(val point: LimitPoint) : Locator()

    val needsLimits: Boolean get() = this is LuuStarAt || this is Point

    companion object {
        fun parse(s: String, path: String): Locator = when {
            s == "self" -> Self
            s == "menh" -> Menh
            s == "than" -> Than
            s.startsWith("role:") -> Role(parseRole(s.removePrefix("role:"), path))
            s.startsWith("star:") -> StarAt(parseStar(s.removePrefix("star:"), path))
            s.startsWith("luuStar:") -> LuuStarAt(parseStar(s.removePrefix("luuStar:"), path))
            else -> Point(LimitPoint.entries.firstOrNull { it.key == s } ?: throw KbException("$path: điểm không hợp lệ “$s”"))
        }
    }
}

/** Mục đếm trong `countOf`: một sao, hoặc dấu Tuần/Triệt ở cung trong phạm vi. */
sealed class CountItem {
    data class OfStar(val star: StarId) : CountItem()
    data object TuanMark : CountItem()
    data object TrietMark : CountItem()
}

/** Predicate của rule DSL (mục 9). Đánh giá trên một cung neo; bằng chứng ghi vào [out] chỉ khi predicate đúng. */
sealed class Predicate {
    abstract fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean
    open fun leafCount(): Int = 1
    open fun needsLimits(): Boolean = false
    /** Mọi sao được nhắc tới (cho lint). */
    open fun stars(): List<StarId> = emptyList()

    data class AllOf(val items: List<Predicate>) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val tmp = ArrayList<Evidence>()
            for (p in items) if (!p.eval(ctx, anchor, tmp)) return false
            out.addAll(tmp)
            return true
        }
        override fun leafCount() = items.sumOf { it.leafCount() }
        override fun needsLimits() = items.any { it.needsLimits() }
        override fun stars() = items.flatMap { it.stars() }
    }

    data class AnyOf(val items: List<Predicate>) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            for (p in items) {
                val tmp = ArrayList<Evidence>()
                if (p.eval(ctx, anchor, tmp)) {
                    out.addAll(tmp)
                    return true
                }
            }
            return false
        }
        override fun leafCount() = items.minOfOrNull { it.leafCount() } ?: 0
        override fun needsLimits() = items.any { it.needsLimits() }
        override fun stars() = items.flatMap { it.stars() }
    }

    data class NoneOf(val items: List<Predicate>) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean =
            items.none { it.eval(ctx, anchor, ArrayList()) }
        override fun leafCount() = items.sumOf { it.leafCount() }
        override fun needsLimits() = items.any { it.needsLimits() }
        override fun stars() = items.flatMap { it.stars() }
    }

    /** Sao (gốc hoặc lưu) nằm trong phạm vi, có thể kèm điều kiện độ sáng và chi của cung chứa sao. */
    data class StarIn(
        val star: StarId,
        val area: Area,
        val brightnessIn: Set<Brightness>?,
        val chiIn: Set<Chi>?,
        val luu: Boolean,
    ) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val at = (if (luu) ctx.luuPos(star) else ctx.pos(star)) ?: return false
            if (!area.contains(anchor, at)) return false
            if (chiIn != null && at !in chiIn) return false
            if (brightnessIn != null) {
                val b = ctx.brightness(star) ?: return false
                if (b !in brightnessIn) return false
            }
            out.add(ctx.evidence(star, at, anchor, luu))
            return true
        }
        override fun needsLimits() = luu
        override fun stars() = listOf(star)
    }

    /** Tứ Hóa lưu (theo can năm xem) nằm trong phạm vi. */
    data class LuuHoaIn(val hoa: TuHoa, val area: Area) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val at = ctx.limits?.luuTuHoa?.get(hoa) ?: return false
            if (!area.contains(anchor, at)) return false
            out.add(ctx.evidence(hoa.star, at, anchor, luu = true))
            return true
        }
        override fun needsLimits() = true
    }

    data class CountOf(val items: List<CountItem>, val area: Area, val atLeast: Int, val luu: Boolean) : Predicate() {
        /** “Ít nhất n trong tập” tương đương n điều kiện. */
        override fun leafCount() = atLeast
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val tmp = ArrayList<Evidence>()
            var n = 0
            for (item in items) {
                when (item) {
                    is CountItem.OfStar -> {
                        val at = (if (luu) ctx.luuPos(item.star) else ctx.pos(item.star)) ?: continue
                        if (area.contains(anchor, at)) {
                            n++
                            tmp.add(ctx.evidence(item.star, at, anchor, luu))
                        }
                    }
                    CountItem.TuanMark, CountItem.TrietMark -> {
                        val hit = Chi.entries.firstOrNull { c ->
                            area.contains(anchor, c) && (if (item == CountItem.TuanMark) ctx.chart.palace(c).hasTuan else ctx.chart.palace(c).hasTriet)
                        }
                        if (hit != null) {
                            n++
                            tmp.add(ctx.evidence(null, hit, anchor, false, if (item == CountItem.TuanMark) "Tuần" else "Triệt"))
                        }
                    }
                }
            }
            if (n < atLeast) return false
            out.addAll(tmp)
            return true
        }
        override fun needsLimits() = luu
        override fun stars() = items.filterIsInstance<CountItem.OfStar>().map { it.star }
    }

    /** Hai sao kẹp cung neo: một sao ở p−1, sao kia ở p+1 (mục 10.1 “Giáp cát”, 10.2 “Không Kiếp giáp Mệnh”). */
    data class PairGiap(val a: StarId, val b: StarId) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val pa = ctx.pos(a) ?: return false
            val pb = ctx.pos(b) ?: return false
            val ok = (pa == anchor - 1 && pb == anchor + 1) || (pa == anchor + 1 && pb == anchor - 1)
            if (ok) {
                out.add(ctx.evidence(a, pa, anchor, false))
                out.add(ctx.evidence(b, pb, anchor, false))
            }
            return ok
        }
        override fun stars() = listOf(a, b)
    }

    /** Chính sao [star] được hóa [hoas] theo can năm sinh (gốc) hoặc can năm xem (lưu). */
    data class HoaOf(val star: StarId, val hoas: Set<TuHoa>, val luu: Boolean) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val table = (if (luu) ctx.luuTuHoaStars() else ctx.natalTuHoaStars()) ?: return false
            val hit = hoas.firstOrNull { table[it] == star } ?: return false
            val at = ctx.pos(star) ?: return false
            out.add(ctx.evidence(star, at, anchor, false, (if (luu) "hóa ${hit.vn.removePrefix("Hóa ")} lưu" else hit.vn)))
            return true
        }
        override fun needsLimits() = luu
        override fun stars() = listOf(star)
    }

    data class ChiIn(val chis: Set<Chi>) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>) = anchor in chis
    }

    data class RoleIn(val roles: Set<PalaceRole>) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>) = ctx.chart.palace(anchor).role in roles
    }

    enum class Flag { TUAN, TRIET, VCD, THAN }

    data class PalaceFlag(val flag: Flag, val value: Boolean) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val p = ctx.chart.palace(anchor)
            val actual = when (flag) {
                Flag.TUAN -> p.hasTuan
                Flag.TRIET -> p.hasTriet
                Flag.VCD -> p.isVoChinhDieu
                Flag.THAN -> p.isThan
            }
            if (actual && value) {
                val label = when (flag) { Flag.TUAN -> "Tuần"; Flag.TRIET -> "Triệt"; Flag.VCD -> "Vô chính diệu"; Flag.THAN -> "Thân cư" }
                out.add(ctx.evidence(null, anchor, anchor, false, label))
            }
            return actual == value
        }
    }

    /** Điều kiện trên người và năm (không phụ thuộc cung neo). */
    data class Person(
        val gender: Gender? = null,
        val yearCanIn: Set<Can>? = null,
        val yearChiIn: Set<vn.tuvi.core.calendar.Chi>? = null,
        val amDuongNamNu: Set<AmDuongNamNu>? = null,
        val thuanLy: Boolean? = null,
        val cucMenh: Set<CucMenhRelation>? = null,
        val hourChiIn: Set<Chi>? = null,
    ) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val c = ctx.chart
            if (gender != null && c.gender != gender) return false
            if (yearCanIn != null && c.yearCan !in yearCanIn) return false
            if (yearChiIn != null && c.yearChi !in yearChiIn) return false
            if (amDuongNamNu != null && c.amDuongNamNu !in amDuongNamNu) return false
            if (thuanLy != null && c.amDuongThuanLy != thuanLy) return false
            if (cucMenh != null && c.cucMenhRelation !in cucMenh) return false
            if (hourChiIn != null && c.hourChi !in hourChiIn) return false
            return true
        }
    }

    /** Điều kiện của năm xem: Thiên thời đại hạn, tuổi âm, can năm xem. */
    data class YearCond(val thienThoi: Set<ThienThoi>? = null, val ageMin: Int? = null, val ageMax: Int? = null, val viewCanIn: Set<Can>? = null) :
        Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val l = ctx.limits ?: return false
            if (thienThoi != null && (l.beforeFirstDaiHan || l.daiHanPeriod.thienThoi !in thienThoi)) return false
            if (ageMin != null && l.tuoiAm < ageMin) return false
            if (ageMax != null && l.tuoiAm > ageMax) return false
            if (viewCanIn != null && l.yearCanChi.can !in viewCanIn) return false
            return true
        }
        override fun needsLimits() = true
    }

    /** Điều kiện tại một điểm của lá số: có sao gốc/lưu, vai trò cung, trùng cung với điểm khác. */
    data class At(
        val locator: Locator,
        val hasStar: List<StarId>?,
        val hasLuuStar: List<StarId>?,
        val roleIn: Set<PalaceRole>?,
        val same: Locator?,
        val chiIn: Set<Chi>?,
    ) : Predicate() {
        override fun eval(ctx: EvalContext, anchor: Chi, out: MutableList<Evidence>): Boolean {
            val at = ctx.locate(locator, anchor) ?: return false
            val tmp = ArrayList<Evidence>()
            if (hasStar != null) {
                val s = hasStar.firstOrNull { ctx.pos(it) == at } ?: return false
                tmp.add(ctx.evidence(s, at, anchor, false))
            }
            if (hasLuuStar != null) {
                val s = hasLuuStar.firstOrNull { ctx.luuPos(it) == at } ?: return false
                tmp.add(ctx.evidence(s, at, anchor, true))
            }
            if (roleIn != null && ctx.chart.palace(at).role !in roleIn) return false
            if (chiIn != null && at !in chiIn) return false
            if (same != null && ctx.locate(same, anchor) != at) return false
            if (tmp.isEmpty()) tmp.add(ctx.evidence(null, at, anchor, false, describe(locator)))
            out.addAll(tmp)
            return true
        }
        override fun needsLimits() = locator.needsLimits || same?.needsLimits == true || hasLuuStar != null
        override fun stars() = (hasStar ?: emptyList()) + (hasLuuStar ?: emptyList())

        private fun describe(l: Locator): String = when (l) {
            Locator.Self -> "cung đang xét"
            Locator.Menh -> "cung Mệnh"
            Locator.Than -> "cung Thân"
            is Locator.Role -> "cung ${l.role.vn}"
            is Locator.StarAt -> "cung có ${l.star.vn}"
            is Locator.LuuStarAt -> "cung có L.${l.star.vn}"
            is Locator.Point -> when (l.point) {
                LimitPoint.DAI_HAN -> "cung đại hạn"
                LimitPoint.TIEU_HAN -> "cung tiểu hạn"
                LimitPoint.LUU_NIEN_DAI_HAN -> "cung lưu niên đại hạn"
                LimitPoint.LUU_THAI_TUE -> "cung Lưu Thái Tuế"
            }
        }
    }

    companion object {
        private val LOGIC = setOf("allOf", "anyOf", "noneOf")

        /** Đọc một predicate từ JSON; [path] để báo lỗi rõ ràng (file#rule.when…). */
        fun parse(e: JsonElement, path: String): Predicate {
            val o = e as? JsonObject ?: throw KbException("$path: predicate phải là object")
            val keys = o.keys
            fun arr(k: String): JsonArray = o[k] as? JsonArray ?: throw KbException("$path.$k: cần mảng")
            fun strs(k: String): List<String> = arr(k).map { (it as? JsonPrimitive)?.content ?: throw KbException("$path.$k: cần chuỗi") }
            fun str(k: String): String = (o[k] as? JsonPrimitive)?.content ?: throw KbException("$path.$k: cần chuỗi")
            fun bool(k: String): Boolean = (o[k] as? JsonPrimitive)?.booleanOrNull ?: throw KbException("$path.$k: cần true/false")
            fun area(): Area = if ("in" in o) Area.of(str("in"), "$path.in") else Area.SELF
            fun allow(vararg allowed: String) {
                val extra = keys - allowed.toSet()
                if (extra.isNotEmpty()) throw KbException("$path: khóa không hợp lệ $extra")
            }
            fun bright(): Set<Brightness>? = if ("brightnessIn" in o) strs("brightnessIn").map { parseBrightness(it, "$path.brightnessIn") }.toSet() else null
            fun chis(k: String): Set<Chi>? = if (k in o) strs(k).map { parseChi(it, "$path.$k") }.toSet() else null
            return when {
                keys.isEmpty() -> throw KbException("$path: predicate rỗng")
                keys.all { it in LOGIC } -> {
                    val parts = ArrayList<Predicate>()
                    if ("allOf" in o) parts.add(AllOf(arr("allOf").mapIndexed { i, x -> parse(x, "$path.allOf[$i]") }))
                    if ("anyOf" in o) parts.add(AnyOf(arr("anyOf").mapIndexed { i, x -> parse(x, "$path.anyOf[$i]") }))
                    if ("noneOf" in o) parts.add(NoneOf(arr("noneOf").mapIndexed { i, x -> parse(x, "$path.noneOf[$i]") }))
                    if (parts.size == 1) parts[0] else AllOf(parts)
                }
                "at" in o -> {
                    allow("at", "hasStar", "hasLuuStar", "roleIn", "same", "chiIn")
                    fun starList(k: String): List<StarId>? = if (k in o) (o[k] as? JsonArray)?.map { parseStar((it as JsonPrimitive).content, "$path.$k") }
                        ?: listOf(parseStar(str(k), "$path.$k")) else null
                    At(
                        locator = Locator.parse(str("at"), "$path.at"),
                        hasStar = starList("hasStar"),
                        hasLuuStar = starList("hasLuuStar"),
                        roleIn = if ("roleIn" in o) strs("roleIn").map { parseRole(it, "$path.roleIn") }.toSet() else null,
                        same = if ("same" in o) Locator.parse(str("same"), "$path.same") else null,
                        chiIn = chis("chiIn"),
                    )
                }
                "star" in o -> {
                    allow("star", "in", "brightnessIn", "chiIn", "layer")
                    val luu = when (val l = if ("layer" in o) str("layer") else "goc") {
                        "goc" -> false; "luu" -> true; else -> throw KbException("$path.layer: “$l” (cần goc|luu)")
                    }
                    StarIn(parseStar(str("star"), "$path.star"), area(), bright(), chis("chiIn"), luu)
                }
                "luuStar" in o -> {
                    allow("luuStar", "in", "chiIn")
                    StarIn(parseStar(str("luuStar"), "$path.luuStar"), area(), null, chis("chiIn"), true)
                }
                "luuHoa" in o -> {
                    allow("luuHoa", "in")
                    LuuHoaIn(parseHoa(str("luuHoa"), "$path.luuHoa"), area())
                }
                "countOf" in o -> {
                    allow("countOf", "in", "atLeast", "layer")
                    val n = (o["atLeast"] as? JsonPrimitive)?.intOrNull ?: throw KbException("$path.atLeast: cần số")
                    val items = strs("countOf").map {
                        when (it) {
                            "@TUAN" -> CountItem.TuanMark
                            "@TRIET" -> CountItem.TrietMark
                            else -> CountItem.OfStar(parseStar(it, "$path.countOf"))
                        }
                    }
                    if (n < 1 || n > items.size) throw KbException("$path.atLeast: $n ngoài 1..${items.size}")
                    CountOf(items, area(), n, "layer" in o && str("layer") == "luu")
                }
                "pairGiap" in o -> {
                    allow("pairGiap")
                    val s = strs("pairGiap")
                    if (s.size != 2) throw KbException("$path.pairGiap: cần đúng 2 sao")
                    PairGiap(parseStar(s[0], path), parseStar(s[1], path))
                }
                "hoaOf" in o || "luuHoaOf" in o -> {
                    allow("hoaOf", "luuHoaOf", "hoa")
                    val luu = "luuHoaOf" in o
                    HoaOf(parseStar(str(if (luu) "luuHoaOf" else "hoaOf"), path), strs("hoa").map { parseHoa(it, "$path.hoa") }.toSet(), luu)
                }
                "chiIn" in o -> { allow("chiIn"); ChiIn(chis("chiIn")!!) }
                "role" in o -> { allow("role"); RoleIn(strs("role").map { parseRole(it, "$path.role") }.toSet()) }
                "hasTuan" in o -> { allow("hasTuan"); PalaceFlag(Flag.TUAN, bool("hasTuan")) }
                "hasTriet" in o -> { allow("hasTriet"); PalaceFlag(Flag.TRIET, bool("hasTriet")) }
                "isVCD" in o -> { allow("isVCD"); PalaceFlag(Flag.VCD, bool("isVCD")) }
                "isThan" in o -> { allow("isThan"); PalaceFlag(Flag.THAN, bool("isThan")) }
                keys.any { it in setOf("gender", "yearCanIn", "yearChiIn", "amDuongNamNu", "thuanLy", "cucMenh", "hourChiIn") } -> {
                    allow("gender", "yearCanIn", "yearChiIn", "amDuongNamNu", "thuanLy", "cucMenh", "hourChiIn")
                    Person(
                        gender = if ("gender" in o) Gender.entries.firstOrNull { it.name == str("gender") } ?: throw KbException("$path.gender") else null,
                        yearCanIn = if ("yearCanIn" in o) strs("yearCanIn").map { n -> Can.entries.firstOrNull { it.name == n } ?: throw KbException("$path.yearCanIn: $n") }.toSet() else null,
                        yearChiIn = chis("yearChiIn"),
                        amDuongNamNu = if ("amDuongNamNu" in o) strs("amDuongNamNu").map { n -> AmDuongNamNu.entries.firstOrNull { it.name == n } ?: throw KbException("$path.amDuongNamNu: $n") }.toSet() else null,
                        thuanLy = if ("thuanLy" in o) bool("thuanLy") else null,
                        cucMenh = if ("cucMenh" in o) strs("cucMenh").map { n -> CucMenhRelation.entries.firstOrNull { it.name == n } ?: throw KbException("$path.cucMenh: $n") }.toSet() else null,
                        hourChiIn = chis("hourChiIn"),
                    )
                }
                keys.any { it in setOf("thienThoiIn", "ageRange", "viewCanIn") } -> {
                    allow("thienThoiIn", "ageRange", "viewCanIn")
                    val range = if ("ageRange" in o) arr("ageRange").map { (it as? JsonPrimitive)?.intOrNull ?: throw KbException("$path.ageRange") } else null
                    if (range != null && range.size != 2) throw KbException("$path.ageRange: cần [min, max]")
                    YearCond(
                        thienThoi = if ("thienThoiIn" in o) strs("thienThoiIn").map { n -> ThienThoi.entries.firstOrNull { it.name == n } ?: throw KbException("$path.thienThoiIn: $n") }.toSet() else null,
                        ageMin = range?.get(0), ageMax = range?.get(1),
                        viewCanIn = if ("viewCanIn" in o) strs("viewCanIn").map { n -> Can.entries.firstOrNull { it.name == n } ?: throw KbException("$path.viewCanIn: $n") }.toSet() else null,
                    )
                }
                else -> throw KbException("$path: không nhận ra predicate với khóa $keys")
            }
        }
    }
}

internal fun parseStar(s: String, path: String): StarId = StarId.entries.firstOrNull { it.name == s } ?: throw KbException("$path: không có sao “$s” trong StarId")
internal fun parseRole(s: String, path: String): PalaceRole = PalaceRole.entries.firstOrNull { it.name == s } ?: throw KbException("$path: không có cung “$s”")
internal fun parseChi(s: String, path: String): Chi = Chi.entries.firstOrNull { it.name == s } ?: throw KbException("$path: không có chi “$s”")
internal fun parseHoa(s: String, path: String): TuHoa = TuHoa.entries.firstOrNull { it.name == s } ?: throw KbException("$path: không có hóa “$s”")
internal fun parseBrightness(s: String, path: String): Brightness =
    Brightness.entries.firstOrNull { it.code == s || it.name == s } ?: throw KbException("$path: độ sáng không hợp lệ “$s”")
