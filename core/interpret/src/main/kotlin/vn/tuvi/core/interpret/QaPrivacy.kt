package vn.tuvi.core.interpret

/**
 * Bộ lọc ẩn danh câu hỏi (mục 12.5–12.6): chạy trên máy trước khi lưu vào lịch sử câu hỏi thường gặp hoặc gửi đi.
 * Xóa email, số điện thoại, ngày tháng, giờ, năm sinh, họ tên sau “tôi tên…/tên tôi là…”, số nhà. Câu còn dấu hiệu
 * dữ liệu cá nhân (dãy số dài, @) thì [isSafe] = false: không được gửi đi.
 */
object QaPrivacy {
    private val EMAIL = Regex("[\\p{L}0-9._%+-]+@[\\p{L}0-9-]+(?:\\.[\\p{L}0-9-]+)+")
    private val PHONE = Regex("(?<![0-9])(?:\\+?84|0)(?:[ .-]?[0-9]){8,10}(?![0-9])")
    private val BIRTH = Regex("(?i)(sinh|đẻ|sanh)((?: ra)?(?: vào| lúc| ngày| năm| tháng| giờ| hồi)*)\\s*[0-9][0-9/.:h -]*(?:\\s*(?:giờ|g|h|phút|sáng|chiều|tối|đêm|âm lịch|dương lịch|âm|dương))*")
    private val DATE = Regex("(?<![0-9])[0-9]{1,2}[/.-][0-9]{1,2}(?:[/.-][0-9]{2,4})?(?![0-9])")
    private val TIME = Regex("(?i)(?<![0-9])[0-9]{1,2}\\s?(?::|h|giờ)\\s?[0-9]{2}(?: ?phút)?(?![0-9])")
    private val HOUSE = Regex("(?i)(số nhà|nhà số|ngõ|ngách|hẻm|kiệt)\\s*[0-9][0-9a-z/]*")
    private val NAME_INTRO = Regex("(?i)(tôi tên(?: là)?|em tên(?: là)?|mình tên(?: là)?|con tên(?: là)?|cháu tên(?: là)?|tên tôi là|tên em là|tên mình là|tên con là|tên là|vợ tôi tên(?: là)?|chồng tôi tên(?: là)?|con tôi tên(?: là)?)\\s+")
    private val STOP = setOf(
        "gì", "năm", "thì", "có", "sinh", "muốn", "hỏi", "xem", "và", "là", "nhưng", "nên", "được", "không", "bao", "khi", "đang", "đã", "sẽ",
        "tuổi", "mệnh", "cung", "ơi", "ạ", "nhé", "nha", "này", "đó", "à",
    )

    fun anonymize(question: String): String {
        var s = question.trim().take(500)
        s = EMAIL.replace(s, "[email]")
        s = PHONE.replace(s, "[số điện thoại]")
        s = BIRTH.replace(s) { m -> m.groupValues[1] + m.groupValues[2] + " [đã ẩn]" + (if (m.value.endsWith(" ")) " " else "") }
        s = TIME.replace(s, "[giờ]")
        s = DATE.replace(s, "[ngày]")
        s = HOUSE.replace(s) { m -> m.groupValues[1] + " [đã ẩn]" }
        // Họ tên: tối đa 4 chữ ngay sau lời giới thiệu, dừng ở dấu câu hoặc từ thường gặp trong câu hỏi.
        val sb = StringBuilder()
        var i = 0
        for (m in NAME_INTRO.findAll(s)) {
            if (m.range.first < i) continue
            sb.append(s, i, m.range.last + 1)
            var j = m.range.last + 1
            var words = 0
            while (words < 4 && j < s.length) {
                val w = Regex("^[\\p{L}]+").find(s.substring(j))?.value ?: break
                if (w.lowercase() in STOP) break
                j += w.length; words++
                if (j < s.length && s[j] == ' ') j++ else break
            }
            if (words > 0) {
                sb.append("[tên]")
                if (j <= s.length && j > 0 && s[j - 1] == ' ') sb.append(' ')
            }
            i = j
        }
        sb.append(s.substring(i.coerceAtMost(s.length)))
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /** Câu đã ẩn danh có còn dấu hiệu dữ liệu cá nhân không: dãy từ 7 chữ số, ký tự @. */
    fun isSafe(anonymized: String): Boolean = !Regex("[0-9]{7,}|@").containsMatchIn(anonymized.replace(" ", ""))
}
