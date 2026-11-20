package vn.tuvi.core.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import vn.tuvi.core.calendar.Chi
import java.util.EnumMap

/**
 * Bảng miếu – vượng – đắc – bình – hãm (mục 6), đọc từ `brightness.json` để chỉnh dữ liệu không cần sửa code.
 * Ô “không xét” trả về null.
 */
class BrightnessTable private constructor(
    val version: Int,
    private val chinhTinh: Map<StarId, List<Brightness>>,
    private val cuMonVariants: Map<CuMonTable, List<Brightness>>,
    private val phuTinh: Map<StarId, Pair<Set<Chi>, Brightness?>>,
) {
    fun brightnessOf(star: StarId, chi: Chi, config: SchoolConfig): Brightness? {
        if (star == StarId.CU_MON) return cuMonVariants.getValue(config.cuMonTable)[chi.index]
        chinhTinh[star]?.let { return it[chi.index] }
        val rule = phuTinh[star] ?: return null
        return if (chi in rule.first) Brightness.DAC else rule.second
    }

    /** Các sao có xét độ sáng. */
    val coveredStars: Set<StarId> get() = chinhTinh.keys + StarId.CU_MON + phuTinh.keys

    companion object {
        private const val RESOURCE = "/brightness.json"

        /** Bảng mặc định, nạp một lần từ tài nguyên của module. */
        val default: BrightnessTable by lazy {
            val text = BrightnessTable::class.java.getResourceAsStream(RESOURCE)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("Thiếu tài nguyên $RESOURCE")
            parse(text)
        }

        fun parse(json: String): BrightnessTable {
            val root = Json.parseToJsonElement(json).jsonObject
            fun row(arr: JsonArray, label: String): List<Brightness> {
                require(arr.size == 12) { "$label phải có đủ 12 cung" }
                return arr.map { Brightness.fromCode(it.jsonPrimitive.content) }
            }

            val chinh = EnumMap<StarId, List<Brightness>>(StarId::class.java)
            root.getValue("chinhTinh").jsonObject.forEach { (key, value) ->
                val star = StarId.valueOf(key)
                require(star.kind == StarKind.CHINH_TINH) { "$key không phải chính tinh" }
                chinh[star] = row(value.jsonArray, key)
            }
            require(chinh.keys == StarId.CHINH_TINH.toSet()) { "Bảng chính tinh phải đủ 14 sao" }

            val cuMon = EnumMap<CuMonTable, List<Brightness>>(CuMonTable::class.java)
            root.getValue("variants").jsonObject.getValue("cuMonTable").jsonObject.forEach { (key, value) ->
                cuMon[CuMonTable.valueOf(key)] = row(value.jsonObject.getValue("CU_MON").jsonArray, "Cự Môn $key")
            }
            require(cuMon.keys == CuMonTable.entries.toSet()) { "Thiếu biến thể bảng Cự Môn" }
            // Bảng chính tinh mặc định phải trùng biến thể Thái Thứ Lang của Cự Môn.
            require(chinh[StarId.CU_MON] == cuMon[CuMonTable.THAI_THU_LANG]) { "CU_MON trong chinhTinh lệch biến thể THAI_THU_LANG" }

            val phu = EnumMap<StarId, Pair<Set<Chi>, Brightness?>>(StarId::class.java)
            root.getValue("phuTinh").jsonObject.forEach { (key, value) ->
                val obj: JsonObject = value.jsonObject
                val dac = obj.getValue("dac").jsonArray.map { Chi.fromVn(it.jsonPrimitive.content) }.toSet()
                val conLai = obj["conLai"]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content?.let(Brightness::fromCode)
                phu[StarId.valueOf(key)] = dac to conLai
            }

            return BrightnessTable(
                version = root.getValue("version").jsonPrimitive.content.toInt(),
                chinhTinh = chinh,
                cuMonVariants = cuMon,
                phuTinh = phu,
            )
        }
    }
}
