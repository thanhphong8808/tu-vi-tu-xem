package vn.tuvi.core.interpret

import vn.tuvi.core.engine.StarId

/**
 * Lint knowledge base (mục 14.3): ID luật duy nhất; mọi luật có `source`; mọi `textKey`/`adviceKey` tồn tại;
 * template chỉ dùng slot hợp lệ; không có từ cấm (mục 11); mọi sao trong luật thuộc danh mục StarId (kiểm khi đọc).
 * Thêm: câu luận và khuyến nghị đều có `source`; câu trả lời mẫu tham chiếu luật có thật.
 */
object KbLint {

    val ALLOWED_SLOTS = setOf("cung", "sao", "doSang", "tuoi", "nam")

    /** Câu so hai lá số (`hop.*`, D-67): tên hai người, hành, chi tiết. */
    val HOP_SLOTS = setOf("a", "b", "ha", "hb", "x", "y")

    /**
     * Từ/cụm cấm (mục 11 quy tắc 1, 2, 7 và mục 10.5): nói định mệnh, phán tử vong/bệnh/ly hôn/tù tội,
     * quảng bá giải hạn, “con dị bào”, dự đoán giới tính thai nhi. So khớp không phân biệt hoa thường, theo ranh giới từ.
     */
    val FORBIDDEN = listOf(
        "chắc chắn", "100%", "không thể tránh", "định mệnh",
        "tử vong", "chết", "đoản thọ", "tuổi thọ", "sảy thai", "ly hôn", "tù tội", "lao lý", "tai nạn chết người",
        "hạn chí đầu hà", "dị bào", "giới tính thai", "con trai", "con gái",
        "giải hạn", "cúng sao", "dâng sao",
    )

    data class Issue(val where: String, val message: String) {
        override fun toString() = "$where: $message"
    }

    fun forbiddenIn(text: String): List<String> {
        val t = text.lowercase()
        return FORBIDDEN.filter { w ->
            var i = t.indexOf(w)
            var found = false
            while (i >= 0 && !found) {
                val before = if (i == 0) ' ' else t[i - 1]
                val afterIdx = i + w.length
                val after = if (afterIdx >= t.length) ' ' else t[afterIdx]
                if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) found = true
                i = t.indexOf(w, i + 1)
            }
            found
        }
    }

    fun slotsIn(text: String): List<String> = Regex("\\{([^}]*)\\}").findAll(text).map { it.groupValues[1] }.toList()

    fun run(kb: KnowledgeBase): List<Issue> {
        val issues = ArrayList<Issue>()
        val ids = HashSet<String>()
        for (r in kb.rules) {
            val w = "${r.file}#${r.id}"
            if (!ids.add(r.id)) issues.add(Issue(w, "trùng ID luật"))
            if (!Regex("[A-Z0-9_]+").matches(r.id)) issues.add(Issue(w, "ID chỉ dùng A–Z, 0–9, _"))
            if (r.source.isBlank()) issues.add(Issue(w, "thiếu source"))
            if (r.name.isBlank()) issues.add(Issue(w, "thiếu tên"))
            if (r.textKey !in kb.texts) issues.add(Issue(w, "textKey không tồn tại: ${r.textKey}"))
            for (a in r.adviceKeys) if (a !in kb.advice) issues.add(Issue(w, "adviceKey không tồn tại: $a"))
            if (r.effect.weight <= 0.0 || r.effect.weight > 5.0) issues.add(Issue(w, "weight ngoài (0, 5]"))
            if (r.reliability == Reliability.C && !r.expertOnly) issues.add(Issue(w, "độ tin cậy C phải đặt expertOnly"))
            for (pat in r.effect.suppress + r.effect.soften + r.effect.invert) {
                if (!Regex("[A-Z0-9_*]+(@([A-Z_]+|SELF|\\*))?").matches(pat)) issues.add(Issue(w, "mẫu chặn không hợp lệ: $pat"))
            }
            val bad = forbiddenIn(r.name)
            if (bad.isNotEmpty()) issues.add(Issue(w, "tên có từ cấm $bad"))
        }
        for (t in kb.texts.values) {
            val w = "${t.file}#${t.key}"
            if (t.source.isBlank()) issues.add(Issue(w, "câu luận thiếu source"))
            if (t.text.isBlank()) issues.add(Issue(w, "câu luận rỗng"))
            val slots = if (t.key.startsWith("hop.")) HOP_SLOTS else ALLOWED_SLOTS
            for (s in slotsIn(t.text)) if (s !in slots) issues.add(Issue(w, "slot không hợp lệ {$s}"))
            val bad = forbiddenIn(t.text)
            if (bad.isNotEmpty()) issues.add(Issue(w, "có từ cấm $bad"))
        }
        for (a in kb.advice.values) {
            val w = "advice#${a.key}"
            if (a.source.isBlank()) issues.add(Issue(w, "khuyến nghị thiếu source"))
            val bad = forbiddenIn(a.text)
            if (bad.isNotEmpty()) issues.add(Issue(w, "có từ cấm $bad"))
            if (a.severity !in 1..3) issues.add(Issue(w, "severity ngoài 1..3"))
        }
        val faqIds = HashSet<String>()
        for (f in kb.faq) {
            val w = "faq#${f.id}"
            if (!faqIds.add(f.id)) issues.add(Issue(w, "trùng ID"))
            if (f.source.isBlank()) issues.add(Issue(w, "thiếu source"))
            for (r in f.relatedRules) if (kb.rule(r) == null) issues.add(Issue(w, "luật không tồn tại: $r"))
            val bad = forbiddenIn(f.question + " " + f.answer)
            if (bad.isNotEmpty()) issues.add(Issue(w, "có từ cấm $bad"))
            for (s in slotsIn(f.answer)) if (s !in ALLOWED_SLOTS) issues.add(Issue(w, "slot không hợp lệ {$s}"))
        }
        for (c in kb.combos) {
            if (kb.texts.keys.none { it.startsWith("combo.${c.key}.") }) issues.add(Issue("combo#${c.key}", "chưa có câu luận"))
        }
        // Giai đoạn 4: câu sự kiện đủ 3 mức, câu thế giai đoạn, ghi chú bắt buộc, khuyến nghị dự phòng đủ cho mọi chủ đề và mọi thế.
        for (e in Event.entries) for (l in Likelihood.entries) {
            if ("ev.${e.name.lowercase()}.${l.key}" !in kb.texts) issues.add(Issue("event#${e.name}", "thiếu câu mức ${l.key}"))
        }
        for (st in Stance.entries) if ("the.${st.key}" !in kb.texts) issues.add(Issue("stance#${st.name}", "thiếu câu thế giai đoạn"))
        for (n in listOf("note.duc_nang", "note.ket_noi", "note.chua_co_luan")) if (n !in kb.texts) issues.add(Issue("note", "thiếu $n"))
        for (t in Topic.entries) {
            fun has(type: AdviceType, use: AdviceUse) = kb.advice.values.any { it.topic == t && it.type == type && use in it.uses }
            if (!has(AdviceType.NEN, AdviceUse.STRONG)) issues.add(Issue("advice#${t.name}", "thiếu NÊN dự phòng khi chủ đề mạnh"))
            if (!has(AdviceType.NEN, AdviceUse.WEAK)) issues.add(Issue("advice#${t.name}", "thiếu NÊN dự phòng khi chủ đề yếu"))
            if (!has(AdviceType.KHONG_NEN, AdviceUse.WEAK)) issues.add(Issue("advice#${t.name}", "thiếu KHÔNG NÊN dự phòng khi chủ đề yếu"))
        }
        for (type in listOf(AdviceType.NEN, AdviceType.KHONG_NEN)) {
            if (kb.advice.values.count { AdviceUse.CHILD in it.uses && it.type == type } < 3) issues.add(Issue("advice#CHILD", "cần ≥ 3 $type cho trẻ nhỏ"))
        }
        if ("note.tre_nho" !in kb.texts) issues.add(Issue("note", "thiếu note.tre_nho"))
        // Giai đoạn 5: thẻ sao đủ 14 chính tinh; câu so hai lá số; ghi chú ngày, giờ.
        for (st in StarId.entries.filter { it.kind == vn.tuvi.core.engine.StarKind.CHINH_TINH }) if (st !in kb.mainMeta) issues.add(Issue("chinh#${st.name}", "thiếu thuộc tính thẻ sao"))
        for (k in listOf("cmp.truyen_cung.vo_chong", "cmp.truyen_cung.cha_me_con", "cmp.truyen_tinh.dep", "cmp.truyen_tinh.trung", "cmp.truyen_tinh.hung",
            "cmp.truyen_tinh.mot_phan", "cmp.khong", "cmp.note", "note.ngay", "note.gio")) if (k !in kb.texts) issues.add(Issue("text", "thiếu $k"))
        for (st in Stance.entries) for (type in listOf(AdviceType.NEN, AdviceType.KHONG_NEN)) {
            if (kb.advice.values.none { AdviceUse.STANCE in it.uses && it.type == type && (it.stances.isEmpty() || st in it.stances) }) {
                issues.add(Issue("advice#${st.name}", "thiếu $type theo thế giai đoạn"))
            }
        }
        for (m in kb.minor.keys) {
            if (kb.texts.keys.none { it == "phu.${m.name}" || it.startsWith("phu.${m.name}.") }) issues.add(Issue("minor#${m.name}", "chưa có câu luận"))
        }
        return issues
    }
}
