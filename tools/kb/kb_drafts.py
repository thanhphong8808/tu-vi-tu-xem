"""Script sinh nháp câu luận (mục 9: KB phần a, b, c) từ cẩm nang V2 phần II–III, mục 6 và mục 10.

Mỗi câu ghép từ: tính chất sao (cẩm nang P.III), lăng kính của cung (P.II) và mức độ sáng (mục 6). Mọi câu là bản nháp
(`reviewed: false`) cho tới khi người dùng duyệt; app hiện nhãn “bản nháp”. Câu chữ theo quy tắc mục 11.
"""

CN_II = "Cẩm nang V2 – P.II"
CN_III = "Cẩm nang V2 – P.III"
GEN = "Sinh nháp tự động từ cẩm nang V2 (P.II, P.III) + mục 6"
PHO_THONG = "Tổng hợp phổ thông Nam phái (bản nháp, cần duyệt)"

ROLES = ["MENH", "PHU_MAU", "PHUC_DUC", "DIEN_TRACH", "QUAN_LOC", "NO_BOC", "THIEN_DI", "TAT_ACH", "TAI_BACH", "TU_TUC", "PHU_THE", "HUYNH_DE"]
ROLE_VN = {"MENH": "Mệnh", "PHU_MAU": "Phụ Mẫu", "PHUC_DUC": "Phúc Đức", "DIEN_TRACH": "Điền Trạch", "QUAN_LOC": "Quan Lộc",
           "NO_BOC": "Nô Bộc", "THIEN_DI": "Thiên Di", "TAT_ACH": "Tật Ách", "TAI_BACH": "Tài Bạch", "TU_TUC": "Tử Tức",
           "PHU_THE": "Phu Thê", "HUYNH_DE": "Huynh Đệ"}
# Cung nói về người khác (cha mẹ, bạn đời, con, anh em, bạn bè): câu tả người/quan hệ đó.
PERSON_ROLES = {"PHU_MAU": "cha mẹ", "PHU_THE": "người bạn đời", "TU_TUC": "con cái", "HUYNH_DE": "anh chị em", "NO_BOC": "bạn bè, cộng sự"}
LENS = {
    "MENH": "về tính cách và xu hướng hành động",
    "PHUC_DUC": "về đời sống tinh thần và phúc ấm gia đình",
    "DIEN_TRACH": "về nhà cửa, đất đai và tài sản cố định",
    "QUAN_LOC": "trong công việc, sự nghiệp",
    "THIEN_DI": "khi ra ngoài xã hội, đi xa",
    "TAT_ACH": "về sức khỏe (chỉ là lưu ý)",
    "TAI_BACH": "về tiền bạc và cách xoay xở tài chính",
}

# —— Cung (cẩm nang P.II, viết lại mềm) ——
PALACE_TEXT = {
    "MENH": "Cung Mệnh là gốc của lá số: bản chất, tư chất, năng khiếu và xu hướng hành động, nặng nhất trước khoảng 30 tuổi. Luôn đọc cùng Thiên Di (xung chiếu), Quan Lộc và Tài Bạch (tam hợp).",
    "PHU_MAU": "Cung Phụ Mẫu nói về cha mẹ (cả cha mẹ nuôi, gia đình bên vợ/chồng): điều kiện, sự nâng đỡ và mức gắn bó giữa mình với cha mẹ. Đọc thêm độ sáng của Thái Dương (cha) và Thái Âm (mẹ).",
    "PHUC_DUC": "Cung Phúc Đức được xem là “linh hồn” của lá số: phúc ấm dòng họ, đời sống nội tâm, sự thanh thản và khả năng được che chở khi gặp hạn khó.",
    "DIEN_TRACH": "Cung Điền Trạch nói về nhà cửa, đất đai, nơi ở và khả năng tích lũy tài sản cố định.",
    "QUAN_LOC": "Cung Quan Lộc nói về con đường sự nghiệp, ngành nghề phù hợp, vị trí và tinh thần trách nhiệm trong công việc.",
    "NO_BOC": "Cung Nô Bộc nói về bạn bè, đồng nghiệp, cấp dưới, đối tác và quan hệ với cộng đồng.",
    "THIEN_DI": "Cung Thiên Di đối diện cung Mệnh: hoàn cảnh bên ngoài xã hội, cơ hội đi xa, khả năng thích ứng với môi trường mới.",
    "TAT_ACH": "Cung Tật Ách cho biết những điểm nên lưu ý về sức khỏe và an toàn. Đây chỉ là gợi ý để chăm sóc bản thân, không thay cho thăm khám của bác sĩ.",
    "TAI_BACH": "Cung Tài Bạch nói về dòng tiền: cách kiếm tiền, chi tiêu và quản lý tài chính (khác Điền Trạch là tài sản cố định).",
    "TU_TUC": "Cung Tử Tức nói về đường con cái, sự gắn bó giữa cha mẹ và con, và cũng là một trong ba cung tài sản.",
    "PHU_THE": "Cung Phu Thê nói về người bạn đời: tính cách, hoàn cảnh và mức hòa hợp của hôn nhân.",
    "HUYNH_DE": "Cung Huynh Đệ nói về anh chị em: tình cảm, sự tương trợ và khả năng hợp tác giữa anh em.",
}

# —— 14 chính tinh (cẩm nang P.III): nét tích cực khi sáng, mặt trái khi hãm; nét người (cho cung lục thân);
#    gợi ý nghề, tiền, lưu ý sức khỏe chung (không nêu bệnh cụ thể). ——
STAR = {
    "TU_VI": dict(vn="Tử Vi", pos="điềm đạm, đôn hậu, có tài tổ chức và lãnh đạo, giàu lòng vị tha",
                  neg="dễ tự tôn, thích người khác phục tùng, đôi khi cô độc nơi vị trí cao",
                  ppos="đĩnh đạc, có uy, biết lo toan", pneg="tự tôn, khó chiều",
                  job="hợp vai trò quản trị, điều phối", money="biết dùng tiền để làm việc lớn", health="nên giữ nhịp làm việc vừa phải, tránh ôm việc"),
    "THIEN_CO": dict(vn="Thiên Cơ", pos="thông minh, giỏi phân tích, ứng biến và hoạch định",
                     neg="dễ tính toán quá nhiều, thiếu quyết đoán, hay lo nghĩ",
                     ppos="thông minh, khéo léo, chu đáo", pneg="hay lo nghĩ, khó đoán",
                     job="hợp tham mưu, nghiên cứu, giáo dục, lập trình", money="giỏi tính toán, xoay xở", health="nên giảm căng thẳng, ngủ đủ"),
    "THAI_DUONG": dict(vn="Thái Dương", pos="chính trực, hào sảng, quang minh, dám nghĩ dám làm",
                       neg="bộc trực quá mức, dễ hao tổn vì người khác, danh chưa tương xứng công sức",
                       ppos="thẳng thắn, rộng lượng, có trách nhiệm", pneg="nóng tính, hay lo việc người",
                       job="hợp công việc cần uy tín, giao tiếp, công quyền", money="danh thường đi trước lợi", health="nên giữ thị lực, nghỉ ngơi điều độ"),
    "VU_KHUC": dict(vn="Vũ Khúc", pos="quả cảm, cương nghị, thực tế, kỷ luật cao, giỏi tài chính",
                    neg="cứng nhắc, cô độc, hay quyết liệt quá mức",
                    ppos="thực tế, có kỷ luật, giỏi kiếm tiền", pneg="cứng rắn, ít biểu lộ tình cảm",
                    job="hợp tài chính, ngân hàng, kinh doanh", money="giỏi kiếm và quản lý tiền", health="nên điều độ, tránh làm việc quá sức"),
    "THIEN_DONG": dict(vn="Thiên Đồng", pos="nhân hậu, yêu đời, ôn hòa, có khả năng hóa giải khó khăn",
                       neg="dễ ham an nhàn, thiếu quyết tâm, hay đổi ý",
                       ppos="hiền hòa, dễ chịu, biết hưởng thụ", pneg="ham vui, thiếu kiên định",
                       job="hợp dịch vụ, chăm sóc, văn hóa", money="tiền đến nhẹ nhàng nhưng ít tích lũy lớn", health="nên vận động đều, tránh lười"),
    "LIEM_TRINH": dict(vn="Liêm Trinh", pos="liêm khiết, nghiêm minh, có gu thẩm mỹ",
                       neg="nóng nảy, cố chấp, dễ vướng thị phi",
                       ppos="nghiêm túc, có nguyên tắc, thu hút", pneg="nóng nảy, khó tính",
                       job="hợp tư pháp, kỷ cương hoặc nghệ thuật, thiết kế", money="kiếm tiền bằng chuyên môn, cần minh bạch", health="nên giữ tinh thần thoải mái, tránh nóng giận"),
    "THIEN_PHU": dict(vn="Thiên Phủ", pos="cẩn trọng, trầm tĩnh, chu đáo, giỏi tích lũy, bao dung",
                      neg="dè dặt quá mức, khi kho bị phá thì dễ toan tính",
                      ppos="chu đáo, biết vun vén, rộng lượng", pneg="bảo thủ, hay giữ ý",
                      job="hợp quản lý tài sản, hành chính, tài chính", money="giỏi giữ và tích lũy", health="nên ăn uống điều độ"),
    "THAI_AM": dict(vn="Thái Âm", pos="nhu thuận, thanh lịch, tinh tế, có duyên với tài sản",
                    neg="đa sầu đa cảm, dễ thiếu quyết đoán",
                    ppos="dịu dàng, tinh tế, biết thu vén", pneg="nhạy cảm, hay buồn",
                    job="hợp bất động sản, tài chính, nghệ thuật, chăm sóc", money="có duyên tích lũy, nhất là tài sản", health="nên giữ nhịp sinh hoạt đều đặn"),
    "THAM_LANG": dict(vn="Tham Lang", pos="linh hoạt, giỏi ngoại giao, nhiều tham vọng, đa tài",
                      neg="dễ ham vui, nhiều ham muốn vật chất và tình cảm",
                      ppos="lôi cuốn, linh hoạt, giỏi giao tiếp", pneg="ham vui, khó nắm bắt",
                      job="hợp kinh doanh, ngoại giao, giải trí", money="kiếm tiền nhanh, cần kỷ luật chi tiêu", health="nên điều độ ăn uống, nghỉ ngơi"),
    "CU_MON": dict(vn="Cự Môn", pos="hùng biện, tư duy phản biện sắc bén",
                   neg="đa nghi, dễ gây hiểu lầm, khẩu thiệt",
                   ppos="hoạt ngôn, sắc sảo", pneg="hay tranh luận, đa nghi",
                   job="hợp luật, ngoại giao, giảng dạy, truyền thông", money="kiếm tiền bằng lời nói, chuyên môn", health="nên giữ giọng nói, ăn uống điều độ"),
    "THIEN_TUONG": dict(vn="Thiên Tướng", pos="cẩn trọng, điềm đạm, trọng danh dự, có tinh thần gánh vác",
                        neg="dễ bị hoàn cảnh chi phối, ngại tranh đấu",
                        ppos="đoan chính, chung thủy, trọng nghĩa", pneg="cả nể, dễ bị chi phối",
                        job="hợp vai trò phó tướng, thừa hành, quản lý theo nguyên tắc", money="trọng nghĩa hơn tiền", health="nên chú ý chăm sóc vẻ ngoài, da"),
    "THIEN_LUONG": dict(vn="Thiên Lương", pos="lương thiện, bác ái, khoan dung, hay giúp người",
                        neg="hay lo chuyện người khác, đôi khi cô độc, phải qua nguy rồi mới yên",
                        ppos="hiền lành, đáng tin, hay giúp đỡ", pneg="hay can thiệp, khó tính",
                        job="hợp y tế, giáo dục, tư vấn, công tác xã hội", money="tiền đến từ uy tín và chuyên môn", health="gặp khó thường có người giúp; vẫn nên khám định kỳ"),
    "THAT_SAT": dict(vn="Thất Sát", pos="quyết đoán, dũng cảm, dám đương đầu",
                     neg="nóng nảy, cô độc, dễ hành động vội",
                     ppos="mạnh mẽ, độc lập", pneg="nóng tính, xa cách",
                     job="hợp quân sự, an ninh, khai phá, kinh doanh cạnh tranh", money="tiền đến theo từng đợt", health="nên cẩn trọng khi vận động mạnh"),
    "PHA_QUAN": dict(vn="Phá Quân", pos="tiên phong, dám đổi mới, phóng khoáng",
                     neg="liều lĩnh, ngang tàng, tiêu tiền phóng tay",
                     ppos="phóng khoáng, dám nghĩ dám làm", pneg="thất thường, khó giữ ổn định",
                     job="hợp đổi mới, khởi nghiệp, kỹ thuật, quân sự", money="tiền vào ra mạnh, cần kỷ luật", health="nên cẩn trọng khi vận động mạnh, đi lại"),
}
BRIGHT = {
    "M": ("miếu địa", "phát huy trọn vẹn"),
    "V": ("vượng địa", "phát huy mạnh"),
    "Đ": ("đắc địa", "phát huy khá tốt"),
    "B": ("bình hòa", "ở mức trung bình, tốt xấu tùy sao hội chiếu"),
    "H": ("hãm địa", "bị hạn chế"),
}
# Gợi ý riêng theo cẩm nang P.III cho một số sao ở một số cung.
SPECIAL = {
    ("VU_KHUC", "TAI_BACH"): "Vũ Khúc rất hợp cung tài sản.",
    ("VU_KHUC", "DIEN_TRACH"): "Vũ Khúc rất hợp cung tài sản.",
    ("THIEN_PHU", "TAI_BACH"): "Thiên Phủ là kho, rất hợp cung tài sản.",
    ("THIEN_PHU", "DIEN_TRACH"): "Thiên Phủ là kho, rất hợp cung tài sản.",
    ("THIEN_LUONG", "PHUC_DUC"): "Thiên Lương ở đây thường phát phúc.",
    ("THIEN_LUONG", "DIEN_TRACH"): "Thiên Lương ở đây dễ được hưởng nền tảng gia đình.",
    ("THAI_AM", "DIEN_TRACH"): "Thái Âm có duyên với nhà đất.",
    ("PHA_QUAN", "PHU_THE"): "Phá Quân ở cung Phu Thê khiến duyên phận nhiều thay đổi, cần vun đắp.",
    ("THAT_SAT", "PHU_THE"): "Thất Sát ở cung lục thân dễ tạo khoảng cách, cần chủ động gần gũi.",
    ("THAT_SAT", "HUYNH_DE"): "Thất Sát ở cung lục thân dễ tạo khoảng cách, cần chủ động gần gũi.",
    ("THAT_SAT", "TU_TUC"): "Thất Sát ở cung lục thân dễ tạo khoảng cách, cần chủ động gần gũi.",
    ("VU_KHUC", "PHU_THE"): "Vũ Khúc ở cung lục thân cần nhiều vun đắp tình cảm.",
    ("THIEN_DONG", "PHUC_DUC"): "Thiên Đồng là phúc tinh, rất hợp cung Phúc Đức.",
    ("THAI_DUONG", "PHU_MAU"): "Thái Dương cũng tượng trưng cho người cha.",
    ("THAI_AM", "PHU_MAU"): "Thái Âm cũng tượng trưng cho người mẹ.",
}


def main_star_text(sid, b, role):
    s = STAR[sid]
    bvn, tone = BRIGHT[b]
    good = b in ("M", "V", "Đ")
    head = f"{s['vn']} {bvn} tại cung {ROLE_VN[role]}"
    if role in PERSON_ROLES:
        who = PERSON_ROLES[role]
        body = (f"{who} thường {s['ppos']}; quan hệ có xu hướng thuận" if good else
                f"{who} có thể {s['pneg']}; quan hệ cần thêm sự thấu hiểu" if b == "H" else
                f"{who} mang nét {s['ppos']}, đôi khi {s['pneg']}")
    elif role == "TAT_ACH":
        body = f"{s['health']}; có dấu hiệu bất thường thì nên đi khám" + ("" if good else ", đừng chủ quan")
    elif role == "QUAN_LOC":
        body = (f"{tone} điểm mạnh — {s['pos']}; {s['job']}" if good or b == "B" else
                f"dễ lộ mặt trái là {s['neg']}; nên chọn môi trường ổn định, {s['job']}")
    elif role == "TAI_BACH":
        body = (f"{tone}: {s['money']}" if good or b == "B" else f"tiền bạc dễ thăng trầm vì {s['neg']}; cần kỷ luật chi tiêu")
    else:
        lens = LENS[role]
        body = (f"{lens}, sao {tone}: {s['pos']}" if good else
                f"{lens}, sao {tone}: tốt xấu tùy sao hội chiếu; nét chính là {s['pos']}" if b == "B" else
                f"{lens}, sao {tone}: dễ lộ mặt trái là {s['neg']}")
    extra = SPECIAL.get((sid, role))
    return head + ": " + body + "." + (" " + extra if extra else "")


# —— Bảng độ sáng (mục 6) để chỉ sinh tổ hợp xảy ra được (gồm cả hai bảng Cự Môn). ——
BRIGHTNESS = {
    "TU_VI": "B Đ M B V M M Đ M B V B", "THIEN_CO": "Đ Đ H M M V Đ Đ V M M H", "THAI_DUONG": "H Đ V V V M M Đ H H H H",
    "VU_KHUC": "V M V Đ M H V M V Đ M H", "THIEN_DONG": "V H M Đ H Đ H H M H H Đ", "LIEM_TRINH": "V Đ V H M H V Đ V H M H",
    "THIEN_PHU": "M B M B V Đ M B M B V Đ", "THAI_AM": "V Đ H H H H H Đ V M M M", "THAM_LANG": "H M Đ H V H H M Đ H V H",
    "CU_MON": "V H V M H H V H Đ M H Đ", "THIEN_TUONG": "V Đ M H V Đ V Đ M H V Đ", "THIEN_LUONG": "V Đ V V M H M Đ V H M H",
    "THAT_SAT": "M Đ M H H V M Đ M H H V", "PHA_QUAN": "M V H H Đ H M V H H Đ H",
}
CU_MON_CAM_NANG = {"Đ"}  # bảng Cẩm nang V2 đổi V/Đ ở Tý, Ngọ, Thân — đều đã có trong tập {V, Đ}


def reachable(sid):
    return sorted(set(BRIGHTNESS[sid].split()) | (CU_MON_CAM_NANG if sid == "CU_MON" else set()), key="MVĐBH".index)


# —— 24 bộ đồng cung (mục 9 phần b) ——
COMBOS = [
    ("TU_VI", "THIEN_PHU", "Tử Phủ", "vừa có quyền vừa có kho, ổn định và có tầm", CN_III),
    ("TU_VI", "THIEN_TUONG", "Tử Tướng", "tài năng dễ bị kìm hãm (thế La Võng), hợp vị trí “dưới một người, trên muôn người”", CN_III),
    ("TU_VI", "THAT_SAT", "Tử Sát", "quyết đoán, biến áp lực thành động lực", CN_III),
    ("TU_VI", "PHA_QUAN", "Tử Phá", "tiên phong mở lối, dám nghĩ dám làm, nhiều thăng trầm", CN_III),
    ("TU_VI", "THAM_LANG", "Tử Tham", "giằng co giữa ham muốn vật chất và hướng về tâm linh", CN_III),
    ("THIEN_CO", "CU_MON", "Cơ Cự", "hoạt ngôn, giỏi kinh doanh nhưng hay đa nghi", CN_III),
    ("THIEN_CO", "THAI_AM", "Cơ Âm", "khéo léo, tinh tế, thích thay đổi môi trường", PHO_THONG),
    ("THIEN_CO", "THIEN_LUONG", "Cơ Lương", "mưu trí, giỏi tư vấn, hợp giáo dục, tham mưu", PHO_THONG),
    ("THAI_DUONG", "THAI_AM", "Nhật Nguyệt", "cân bằng giữa hướng ngoại và nội tâm, đôi khi do dự", PHO_THONG),
    ("THAI_DUONG", "CU_MON", "Nhật Cự", "có danh nhờ ngôn luận, ngoại giao", CN_III),
    ("THAI_DUONG", "THIEN_LUONG", "Nhật Lương", "danh và tài dễ rực rỡ, hợp học thuật, y tế", CN_III),
    ("VU_KHUC", "THIEN_PHU", "Vũ Phủ", "giỏi kiếm và giữ tiền, tài chính vững", PHO_THONG),
    ("VU_KHUC", "THIEN_TUONG", "Vũ Tướng", "thực tế, có trách nhiệm, hợp quản lý tài chính", PHO_THONG),
    ("VU_KHUC", "THAT_SAT", "Vũ Sát", "cương quyết, dám làm, tiền bạc đến qua nỗ lực lớn", PHO_THONG),
    ("VU_KHUC", "PHA_QUAN", "Vũ Phá", "tiền bạc tụ tán mạnh, dễ liều", "Prompt mục 10.2"),
    ("VU_KHUC", "THAM_LANG", "Vũ Tham", "tiền bần hậu phú, khá lên rõ sau khoảng 30 tuổi", "Prompt mục 10.1"),
    ("THIEN_DONG", "THAI_AM", "Đồng Âm", "ôn hòa, tinh tế, coi trọng gia đình", PHO_THONG),
    ("THIEN_DONG", "CU_MON", "Đồng Cự", "dễ bất đồng trong lời nói, cần học cách diễn đạt mềm", CN_III),
    ("THIEN_DONG", "THIEN_LUONG", "Đồng Lương", "phúc hậu, gặp khó thường có người giúp", CN_III),
    ("LIEM_TRINH", "THIEN_PHU", "Liêm Phủ", "nguyên tắc nhưng biết giữ của, ổn định", PHO_THONG),
    ("LIEM_TRINH", "THIEN_TUONG", "Liêm Tướng", "nghiêm túc, trọng danh dự, hợp kỷ cương", PHO_THONG),
    ("LIEM_TRINH", "THAT_SAT", "Liêm Sát", "mạnh mẽ nhưng cần cẩn trọng khi đi lại, vận động", CN_III),
    ("LIEM_TRINH", "PHA_QUAN", "Liêm Phá", "nhiều biến động, dám thay đổi, cần kiểm soát cảm xúc", PHO_THONG),
    ("LIEM_TRINH", "THAM_LANG", "Liêm Tham", "dễ vướng thị phi vì tiền hoặc tình, nên tránh vùng xám", CN_III),
]


def combo_text(a, b, name, essence, role):
    if role in PERSON_ROLES:
        return f"Bộ {name} đồng cung tại cung {ROLE_VN[role]}: {PERSON_ROLES[role]} mang nét {essence}."
    if role == "TAT_ACH":
        return f"Bộ {name} đồng cung tại cung Tật Ách: nên giữ nhịp sống điều độ, khám định kỳ; có dấu hiệu bất thường thì đi khám."
    return f"Bộ {name} đồng cung tại cung {ROLE_VN[role]}: {LENS[role]}, {essence}."


# —— Phụ tinh (mục 9 phần c): ý nghĩa chung, chiều cát/hung, thêm vài cung riêng theo cẩm nang P.II ——
MINOR = [
    # sao, tên, ý nghĩa chung, polarity, polarityDac, polarityHam, nguồn, {cung: câu riêng}
    ("TA_PHU", "Tả Phù", "sao phò tá: được người giúp, có cộng sự", 1, None, None, CN_III, {"NO_BOC": "bạn bè, cộng sự đắc lực"}),
    ("HUU_BAT", "Hữu Bật", "sao phò tá: được người giúp, khéo hợp tác", 1, None, None, CN_III, {"NO_BOC": "bạn bè, cộng sự đắc lực"}),
    ("VAN_XUONG", "Văn Xương", "văn tinh: học vấn, chữ nghĩa, giấy tờ", 1, 1, 0, CN_III, {}),
    ("VAN_KHUC", "Văn Khúc", "văn tinh: tài ăn nói, nghệ thuật, năng khiếu", 1, 1, 0, CN_III, {}),
    ("THIEN_KHOI", "Thiên Khôi", "quý nhân: được người có vị thế giúp đỡ, thuận thi cử", 1, None, None, CN_III, {"NO_BOC": "dễ gặp quý nhân"}),
    ("THIEN_VIET", "Thiên Việt", "quý nhân: được giúp đỡ, thuận thi cử", 1, None, None, CN_III, {}),
    ("LOC_TON", "Lộc Tồn", "tài lộc, biết giữ của; luôn bị Kình Đà kẹp nên dễ dè dặt", 1, None, None, CN_II, {"TAI_BACH": "dòng tiền ổn định, biết giữ", "PHU_MAU": "dễ được cha mẹ để lại tài sản"}),
    ("THIEN_MA", "Thiên Mã", "sao động: di chuyển, thay đổi, cơ hội ở xa", 0, None, None, CN_II, {"THIEN_DI": "dễ đi xa, công tác, xuất ngoại"}),
    ("HOA_LOC", "Hóa Lộc", "tài lộc, sự thuận lợi, duyên tiền bạc", 1, None, None, "Prompt mục 7", {"TAI_BACH": "dòng tiền thuận"}),
    ("HOA_QUYEN", "Hóa Quyền", "quyền hành, sức ảnh hưởng, năng lực quản lý", 1, None, None, "Prompt mục 7", {"QUAN_LOC": "dễ nắm vai trò chủ chốt"}),
    ("HOA_KHOA", "Hóa Khoa", "danh tiếng, học vấn, quý nhân giải nguy", 1, None, None, "Prompt mục 7", {}),
    ("HOA_KY", "Hóa Kỵ", "vướng mắc, hiểu lầm, chậm trễ; ở Tứ Mộ thì giảm hung", -1, 0, -1, "Prompt mục 7, 10.3", {"TAT_ACH": "nên khám định kỳ"}),
    ("KINH_DUONG", "Kình Dương", "sát tinh: cạnh tranh, va chạm, quyết liệt; đắc địa thì thành sức mạnh", -1, 1, -1, CN_II, {"TAT_ACH": "cẩn trọng va chạm khi vận động"}),
    ("DA_LA", "Đà La", "sát tinh: trì trệ, vướng mắc ngấm ngầm; đắc địa thì bền bỉ", -1, 1, -1, CN_II, {}),
    ("HOA_TINH", "Hỏa Tinh", "sát tinh: nóng vội, bất ngờ; đắc địa thì nhanh nhạy, quyết đoán", -1, 1, -1, CN_II, {"TAT_ACH": "nên giữ sức, tránh làm việc quá sức"}),
    ("LINH_TINH", "Linh Tinh", "sát tinh: âm thầm, bất ngờ; đắc địa thì sắc sảo", -1, 1, -1, CN_II, {}),
    ("DIA_KHONG", "Địa Không", "sát tinh: hao hụt, ý tưởng khác thường; đắc địa thì phát nhanh nhưng dễ tàn", -1, 0, -1, CN_II, {"TAI_BACH": "kiếm tiền táo bạo nhưng dễ hụt"}),
    ("DIA_KIEP", "Địa Kiếp", "sát tinh: hao tổn, biến động; đắc địa thì phát nhanh nhưng dễ tàn", -1, 0, -1, CN_II, {"TAI_BACH": "kiếm tiền táo bạo nhưng dễ hụt"}),
    ("THIEN_QUAN", "Thiên Quan", "quý nhân, được che chở, thuận việc công", 1, None, None, PHO_THONG, {}),
    ("THIEN_PHUC", "Thiên Phúc", "phúc đức, được che chở", 1, None, None, PHO_THONG, {}),
    ("VAN_TINH", "Văn Tinh", "học vấn, văn chương", 1, None, None, PHO_THONG, {}),
    ("DUONG_PHU", "Đường Phù", "nhà cửa, địa vị, sự vững chãi", 1, None, None, PHO_THONG, {"DIEN_TRACH": "thuận có nhà cửa"}),
    ("QUOC_AN", "Quốc Ấn", "ấn tín, quyền hành, chức vị", 1, None, None, CN_II, {"QUAN_LOC": "thuận chức vị"}),
    ("THAI_PHU", "Thai Phụ", "khoa danh, chữ nghĩa", 1, None, None, PHO_THONG, {}),
    ("PHONG_CAO", "Phong Cáo", "khen thưởng, danh dự", 1, None, None, PHO_THONG, {}),
    ("TAM_THAI", "Tam Thai", "địa vị, sự nâng đỡ", 1, None, None, PHO_THONG, {}),
    ("BAT_TOA", "Bát Tọa", "địa vị, sự nâng đỡ", 1, None, None, PHO_THONG, {}),
    ("AN_QUANG", "Ân Quang", "ân huệ, quý nhân, phúc", 1, None, None, CN_II, {}),
    ("THIEN_QUY", "Thiên Quý", "ân huệ, quý nhân, phúc", 1, None, None, CN_II, {}),
    ("LONG_TRI", "Long Trì", "thanh lịch, khoa danh; sao hỷ sự", 1, None, None, CN_II, {"THIEN_DI": "dễ đi xa"}),
    ("PHUONG_CAC", "Phượng Các", "thanh lịch, khoa danh; sao hỷ sự", 1, None, None, CN_II, {"THIEN_DI": "dễ đi xa"}),
    ("GIAI_THAN", "Giải Thần", "giải trừ khó khăn", 1, None, None, "Prompt mục 10.2", {}),
    ("THIEN_GIAI", "Thiên Giải", "giải trừ khó khăn", 1, None, None, "Prompt mục 10.2", {}),
    ("DIA_GIAI", "Địa Giải", "giải trừ khó khăn", 1, None, None, "Prompt mục 10.2", {}),
    ("HONG_LOAN", "Hồng Loan", "tình duyên, hỷ sự, duyên dáng", 1, None, None, CN_II, {}),
    ("THIEN_HY", "Thiên Hỷ", "niềm vui, hỷ sự", 1, None, None, CN_II, {}),
    ("DAO_HOA", "Đào Hoa", "duyên dáng, thu hút, tình cảm", 0, None, None, CN_II, {}),
    ("THIEN_TAI", "Thiên Tài", "tài năng, khéo léo", 1, None, None, PHO_THONG, {}),
    ("THIEN_THO", "Thiên Thọ", "hiền hòa, bền bỉ", 1, None, None, PHO_THONG, {}),
    ("THIEN_DUC", "Thiên Đức", "tứ đức: hiền lành, gặp khó có người giúp", 1, None, None, PHO_THONG, {}),
    ("NGUYET_DUC", "Nguyệt Đức", "tứ đức: hiền lành, gặp khó có người giúp", 1, None, None, PHO_THONG, {}),
    ("THIEN_Y", "Thiên Y", "y dược, chăm sóc, chữa lành", 1, None, None, PHO_THONG, {}),
    ("THIEN_TRU", "Thiên Trù", "ăn uống, bếp núc, đủ đầy", 1, None, None, PHO_THONG, {}),
    ("HOA_CAI", "Hoa Cái", "đẹp đẽ, thích tôn giáo, nghệ thuật; đôi khi cô độc", 0, None, None, CN_II, {}),
    ("THIEN_HINH", "Thiên Hình", "kỷ luật, pháp lý; đắc địa thì có uy", -1, 1, -1, CN_II, {"TAT_ACH": "cẩn trọng va chạm, thương tích"}),
    ("THIEN_RIEU", "Thiên Riêu", "phong lưu, đa cảm; dễ vướng chuyện ngấm ngầm", -1, 0, -1, CN_II, {"TAT_ACH": "nên khám định kỳ"}),
    ("THIEN_KHOC", "Thiên Khốc", "buồn phiền; đắc địa thì có chí, có tiếng nói", -1, 0, -1, CN_II, {}),
    ("THIEN_HU", "Thiên Hư", "hư hao, lo âu; đắc địa thì giảm bớt", -1, 0, -1, CN_II, {}),
    ("CO_THAN", "Cô Thần", "cô độc, thích tự lập", -1, None, None, CN_II, {"PHU_THE": "hôn nhân cần vun đắp"}),
    ("QUA_TU", "Quả Tú", "cô độc, sống nội tâm", -1, None, None, CN_II, {"PHU_THE": "hôn nhân cần vun đắp"}),
    ("KIEP_SAT", "Kiếp Sát", "biến cố bất ngờ, cần cẩn trọng", -1, None, None, PHO_THONG, {}),
    ("PHA_TOAI", "Phá Toái", "hao hụt, đổ vỡ nhỏ", -1, None, None, PHO_THONG, {}),
    ("LUU_HA", "Lưu Hà", "cẩn trọng nơi sông nước, đi lại", -1, None, None, PHO_THONG, {}),
    ("DAU_QUAN", "Đẩu Quân", "nghiêm khắc, cô độc; ở Phu Thê cản trở hôn nhân", -1, None, None, CN_II, {"PHU_THE": "hôn nhân cần vun đắp"}),
    ("THIEN_KHONG", "Thiên Không", "hư không, ý tưởng khác thường", -1, None, None, PHO_THONG, {}),
    ("THIEN_LA", "Thiên La", "bị gò bó, ràng buộc", -1, None, None, PHO_THONG, {}),
    ("DIA_VONG", "Địa Võng", "bị gò bó, ràng buộc", -1, None, None, PHO_THONG, {}),
    ("THIEN_THUONG", "Thiên Thương", "hao tổn, buồn phiền khi gặp hạn", -1, None, None, PHO_THONG, {}),
    ("THIEN_SU", "Thiên Sứ", "hao tổn, trắc trở khi gặp hạn", -1, None, None, PHO_THONG, {}),
    ("DAI_HAO", "Đại Hao", "hao tán tiền bạc; đắc địa thì tiêu tiền có mục đích", -1, 0, -1, CN_II, {"TAI_BACH": "tiền tụ tán thất thường"}),
    ("TIEU_HAO", "Tiểu Hao", "hao hụt nhỏ, chi tiêu vụn vặt", -1, 0, -1, CN_II, {"TAI_BACH": "tiền tụ tán thất thường"}),
    ("TANG_MON", "Tang Môn", "buồn phiền, thay đổi", -1, None, None, CN_II, {}),
    ("BACH_HO", "Bạch Hổ", "va chạm, thị phi; đi cùng Cự Môn thì thành tài hùng biện", -1, None, None, CN_II, {}),
    ("THAI_TUE", "Thái Tuế", "lời nói, thị phi, sự chính trực", 0, None, None, CN_II, {}),
    ("QUAN_PHU", "Quan Phù", "giấy tờ, pháp lý, thị phi", -1, None, None, "Prompt mục 9", {"QUAN_LOC": "cẩn trọng giấy tờ"}),
    ("THANH_LONG", "Thanh Long", "hỷ sự, may mắn, thuận lợi", 1, None, None, CN_II, {}),
    ("TAU_THU", "Tấu Thư", "văn thư, tài ăn nói", 1, None, None, CN_III, {}),
    ("PHUC_BINH", "Phục Binh", "tiểu nhân ngấm ngầm, cần đề phòng", -1, None, None, CN_II, {"NO_BOC": "cẩn trọng khi hợp tác"}),
    ("TUONG_QUAN", "Tướng Quân", "uy quyền, mạnh mẽ", 1, None, None, CN_II, {}),
    ("HY_THAN", "Hỷ Thần", "niềm vui, hỷ sự", 1, None, None, CN_II, {}),
    ("LONG_DUC", "Long Đức", "tứ đức: nhân hậu, được giúp đỡ", 1, None, None, PHO_THONG, {}),
    ("PHUC_DUC", "Phúc Đức (sao)", "tứ đức: phúc hậu, được che chở", 1, None, None, PHO_THONG, {}),
    ("THAI", "Thai", "khởi đầu mới, chuyện con cái", 0, None, None, CN_II, {"TU_TUC": "đường con cái thuận"}),
]


def minor_texts():
    out = {}
    for sid, vn, meaning, pol, pd, ph, src, roles in MINOR:
        out[f"phu.{sid}"] = {"key": f"phu.{sid}", "text": f"{vn}: {meaning}.", "source": src, "reviewed": False}
        for role, txt in roles.items():
            k = f"phu.{sid}.{role}"
            out[k] = {"key": k, "text": f"{vn} tại cung {ROLE_VN[role]}: {txt}.", "source": src, "reviewed": False}
    return out


def minor_meta():
    res = []
    for sid, vn, meaning, pol, pd, ph, src, roles in MINOR:
        m = {"star": sid, "polarity": pol}
        if pd is not None: m["polarityDac"] = pd
        if ph is not None: m["polarityHam"] = ph
        res.append(m)
    return res


def main_texts():
    out = {}
    for sid in STAR:
        for b in reachable(sid):
            for role in ROLES:
                k = f"star.{sid}.{b}.{role}"
                out[k] = {"key": k, "text": main_star_text(sid, b, role), "source": GEN, "reviewed": False}
    return out


def combo_texts():
    out = {}
    for a, b, name, essence, src in COMBOS:
        key = f"{a}_{b}"
        for role in ROLES:
            k = f"combo.{key}.{role}"
            out[k] = {"key": k, "text": combo_text(a, b, name, essence, role), "source": src, "reviewed": False}
    return out


def combo_meta():
    return [{"key": f"{a}_{b}", "name": n, "stars": [a, b]} for a, b, n, e, s in COMBOS]


def palace_texts():
    return {f"cung.{r}": {"key": f"cung.{r}", "text": t, "source": CN_II, "reviewed": False} for r, t in PALACE_TEXT.items()}


# —— Câu trả lời mẫu cho câu hỏi thường gặp (mục 12.5): bản nháp, chưa duyệt ——
FAQ = [
    dict(id="faq.nam_nay_cong_viec", question="Năm nay công việc của tôi thế nào?",
         answer="Năm {nam}, hãy xem cung tiểu hạn (diễn biến), lưu niên đại hạn (kết quả) và Lưu Thái Tuế (lĩnh vực được kích hoạt). "
                "Nếu Lưu Thái Tuế vào Quan Lộc thì công việc là trọng tâm năm; có Hóa Quyền, Khôi Việt hội về thì thuận để nhận trách nhiệm mới.",
         topics=["SU_NGHIEP"], relatedRules=["VH_LUU_THAI_TUE_QUAN_LOC", "VH_KY_LUU_TAI_QUAN"]),
    dict(id="faq.co_nen_doi_viec", question="Tôi có nên đổi việc năm nay không?",
         answer="Ứng dụng không quyết định thay bạn. Hãy xem Thiên thời của đại hạn và tiểu hạn năm {nam}: giai đoạn thuận thì thuận để chủ động, "
                "giai đoạn thất thời thì nên chuẩn bị kỹ và có phương án dự phòng trước khi đổi.",
         topics=["SU_NGHIEP"], relatedRules=["VH_THIEN_THOI_DAI_HAN_SINH_TUOI", "VH_THIEN_THOI_DAI_HAN_KHAC_TUOI"]),
    dict(id="faq.nam_nao_cuoi", question="Năm nào thuận cho chuyện cưới hỏi?",
         answer="Theo cẩm nang, năm thuận cưới hỏi khi hội đủ ba dấu hiệu: sao lưu tình duyên vào Mệnh, Thân hoặc Nô Bộc; tiểu hạn có sao hôn lễ; "
                "lưu niên đại hạn vào Mệnh, Thân, Phu Thê, Quan Lộc hoặc Tài Bạch. Đây là xu hướng tham khảo, quyết định vẫn là của hai bạn.",
         topics=["TINH_DUYEN"], relatedRules=["CD_HAN_HY_CUOI_HOI", "CD_LUU_THAI_TUE_CO_QUA"]),
    dict(id="faq.menh_vo_chinh_dieu", question="Mệnh vô chính diệu có xấu không?",
         answer="Không hẳn. Mệnh vô chính diệu mượn chính tinh cung Thiên Di để luận; nếu được Nhật Nguyệt sáng chiếu hoặc gặp “tam không” "
                "thì lại có cách riêng. Điều quan trọng là sự tự lực và môi trường bạn chọn.",
         topics=["TINH_CACH"], relatedRules=["TS_VCD_MUON_XUNG", "TS_VCD_TAM_KHONG", "TS_VCD_NHAT_NGUYET_CHIEU"]),
    dict(id="faq.sao_ham_co_xau", question="Sao hãm địa có phải là xấu không?",
         answer="Sao hãm không tự động là xấu. Ứng dụng luôn xét các cách “phản vi” trước khi kết luận — ví dụ Cự Môn hãm ở Thìn Tuất mà tuổi Tân "
                "thì lại hóa cát.",
         topics=["TINH_CACH"], relatedRules=["PV_CU_MON_THIN_TUAT_TAN", "PV_SAO_HAM_HOA_LOC_QUYEN"]),
    dict(id="faq.tai_chinh_nam", question="Tài chính năm nay có ổn không?",
         answer="Năm {nam}, hãy xem Hóa Lộc lưu, Lưu Lộc Tồn có vào Mệnh hay Tài Bạch không, và cung Tài Bạch có gặp Không Kiếp, Song Hao không. "
                "Dù xu hướng thế nào, quỹ dự phòng và kỷ luật chi tiêu luôn là lựa chọn an toàn.",
         topics=["TAI_CHINH"], relatedRules=["VH_LOC_LUU_TAI", "VH_LUU_LOC_TON_TAI", "CG_TAI_KHONG_KIEP"]),
    dict(id="faq.suc_khoe", question="Lá số nói gì về sức khỏe của tôi?",
         answer="Tử Vi chỉ gợi ý những điểm nên lưu ý qua cung Tật Ách, không chẩn đoán bệnh. Hãy khám sức khỏe định kỳ và hỏi ý kiến bác sĩ "
                "khi có dấu hiệu bất thường.",
         topics=["SUC_KHOE"], relatedRules=["CG_TAT_RIEU_KY", "CG_TAT_KINH_HINH"]),
    dict(id="faq.giai_han", question="Tôi có cần làm lễ để tránh hạn xấu không?",
         answer="Ứng dụng không khuyến nghị dịch vụ có thu phí nào. Tử Vi là bản đồ thời tiết: hiểu giai đoạn khó để chủ động thu xếp công việc, "
                "giữ sức khỏe và gắn kết với người thân — đức năng thắng số.",
         topics=["PHUC_DUC"], relatedRules=[]),
]


def faq_entries():
    src = "Bản nháp từ prompt mục 9–12 và cẩm nang V2"
    return [dict(f, source=src, reviewed=False) for f in FAQ]
