package vn.tuvi.core.calendar

/**
 * Địa chi. Chỉ số cố định: Tý = 0, Sửu = 1, Dần = 2 … Hợi = 11.
 * “Thuận” = +1, “nghịch” = −1 (mod 12) — đúng chiều kim đồng hồ trên địa bàn truyền thống.
 */
enum class Chi(val vn: String) {
    TY("Tý"), SUU("Sửu"), DAN("Dần"), MAO("Mão"), THIN("Thìn"), TI("Tỵ"),
    NGO("Ngọ"), MUI("Mùi"), THAN("Thân"), DAU("Dậu"), TUAT("Tuất"), HOI("Hợi");

    val index: Int get() = ordinal

    /** Tý, Dần, Thìn, Ngọ, Thân, Tuất là chi Dương. */
    val isYang: Boolean get() = ordinal % 2 == 0

    operator fun plus(steps: Int): Chi = of(ordinal + steps)
    operator fun minus(steps: Int): Chi = of(ordinal - steps)

    /** Số bước đi thuận từ [from] tới chi này, trong khoảng 0..11. */
    fun stepsFrom(from: Chi): Int = Math.floorMod(ordinal - from.ordinal, 12)

    /** Cung xung chiếu. */
    val opposite: Chi get() = plus(6)

    /** Nhóm tam hợp: 0 = Thân Tý Thìn, 1 = Tỵ Dậu Sửu, 2 = Dần Ngọ Tuất, 3 = Hợi Mão Mùi. */
    val tamHopGroup: Int get() = ordinal % 4

    override fun toString(): String = vn

    companion object {
        private val VALUES = entries.toTypedArray()
        fun of(index: Int): Chi = VALUES[Math.floorMod(index, 12)]
        fun fromVn(name: String): Chi =
            VALUES.firstOrNull { it.vn == name.trim() } ?: throw IllegalArgumentException("Không rõ địa chi: $name")
    }
}

/** Thiên can. Chỉ số cố định: Giáp = 0 … Quý = 9. Can chẵn là can Dương. */
enum class Can(val vn: String) {
    GIAP("Giáp"), AT("Ất"), BINH("Bính"), DINH("Đinh"), MAU("Mậu"),
    KY("Kỷ"), CANH("Canh"), TAN("Tân"), NHAM("Nhâm"), QUY("Quý");

    val index: Int get() = ordinal
    val isYang: Boolean get() = ordinal % 2 == 0

    operator fun plus(steps: Int): Can = of(ordinal + steps)

    override fun toString(): String = vn

    companion object {
        private val VALUES = entries.toTypedArray()
        fun of(index: Int): Can = VALUES[Math.floorMod(index, 10)]
        fun fromVn(name: String): Can =
            VALUES.firstOrNull { it.vn == name.trim() } ?: throw IllegalArgumentException("Không rõ thiên can: $name")
    }
}

/** Một cặp can chi trong lục thập hoa giáp. */
data class CanChi(val can: Can, val chi: Chi) {
    init {
        require(can.ordinal % 2 == chi.ordinal % 2) { "Can chi không hợp lệ: ${can.vn} ${chi.vn}" }
    }

    /** Chỉ số trong lục thập hoa giáp, Giáp Tý = 0 … Quý Hợi = 59. */
    val sexagenaryIndex: Int get() = Math.floorMod(6 * can.ordinal - 5 * chi.ordinal, 60)

    val vn: String get() = "${can.vn} ${chi.vn}"

    override fun toString(): String = vn

    companion object {
        fun fromSexagenary(index: Int): CanChi {
            val i = Math.floorMod(index, 60)
            return CanChi(Can.of(i), Chi.of(i))
        }

        /** Can chi năm âm lịch: can = (năm + 6) mod 10, chi = (năm + 8) mod 12. */
        fun ofYear(lunarYear: Int): CanChi = CanChi(Can.of(lunarYear + 6), Chi.of(lunarYear + 8))

        /** Can chi ngày theo số ngày Julius: can = (jd + 9) mod 10, chi = (jd + 1) mod 12. */
        fun ofDay(jd: Int): CanChi = CanChi(Can.of(jd + 9), Chi.of(jd + 1))

        /**
         * Can chi tháng âm (Ngũ hổ độn): can tháng Giêng = (2·canNăm + 2) mod 10, chi tháng = (tháng + 1) mod 12.
         * Tháng nhuận dùng can chi của tháng chính cùng số.
         */
        fun ofMonth(yearCan: Can, lunarMonth: Int): CanChi {
            require(lunarMonth in 1..12) { "Tháng âm phải trong 1..12: $lunarMonth" }
            val canGieng = 2 * yearCan.ordinal + 2
            return CanChi(Can.of(canGieng + lunarMonth - 1), Chi.of(lunarMonth + 1))
        }

        /** Can chi giờ (Ngũ thử độn): can giờ Tý = (2·canNgày) mod 10. */
        fun ofHour(dayCan: Can, hourChi: Chi): CanChi = CanChi(Can.of(2 * dayCan.ordinal + hourChi.ordinal), hourChi)
    }
}

/** Giờ đồng hồ (0..23) → canh giờ: Tý 23:00–00:59, Sửu 01:00–02:59 … Hợi 21:00–22:59. */
fun hourChiOf(hour: Int): Chi {
    require(hour in 0..23) { "Giờ phải trong 0..23: $hour" }
    return Chi.of((hour + 1) / 2)
}
