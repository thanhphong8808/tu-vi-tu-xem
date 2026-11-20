package vn.tuvi.core.engine

/**
 * Bộ nhớ đệm LRU cho lá số, khóa theo (input, config, kbVersion) — mục 3, ràng buộc phi chức năng.
 * An toàn đa luồng; engine bên dưới là hàm thuần nên cache không đổi kết quả.
 */
class ChartCache(private val maxSize: Int = 64, private val kbVersion: String = "") {

    private data class Key(val input: BirthInput, val config: SchoolConfig, val kbVersion: String)

    private val map = object : LinkedHashMap<Key, Chart>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Chart>?): Boolean = size > maxSize
    }

    @Synchronized
    fun get(input: BirthInput, config: SchoolConfig = SchoolConfig.DEFAULT): Chart =
        map.getOrPut(Key(input, config, kbVersion)) { ChartEngine.build(input, config) }

    @Synchronized
    fun clear() = map.clear()

    @get:Synchronized
    val size: Int get() = map.size
}
