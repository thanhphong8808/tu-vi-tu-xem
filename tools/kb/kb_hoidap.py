"""Câu chữ cho màn Hỏi đáp (mục 12): giải nghĩa thuật ngữ, từ chối nhẹ nhàng, chuyển hướng, khủng hoảng, giới thiệu.

Mọi câu đều là bản nháp (reviewed = false). Không dùng từ cấm mục 11 (KbLint quét). Câu có số liệu cụ thể của lá số
do QaEngine ghép, không nằm ở đây.
"""

P1 = "Prompt mục 1"
P8 = "Prompt mục 8"
P9 = "Prompt mục 9"
P11 = "Prompt mục 11"
P12 = "Prompt mục 12"
CN2 = "Cẩm nang V2 – P.II"
CN3 = "Cẩm nang V2 – P.III"
CN4 = "Cẩm nang V2 – P.IV"
CN5 = "Cẩm nang V2 – P.V"

# Giải nghĩa thuật ngữ: khóa → (câu, nguồn). Bí danh nhận diện nằm ở QaEngine (không dấu).
DEFS = {
    "tu_vi": ("Tử Vi Đẩu Số là môn lý số phương Đông: từ giờ, ngày, tháng, năm sinh âm lịch, an hơn trăm sao vào 12 cung "
              "để xem xu hướng tính cách và vận trình. App dùng Tử Vi như bản đồ thời tiết để tham khảo, chiêm nghiệm, "
              "không phải lời phán định.", P1 + ", " + P11),
    "menh": ("Cung Mệnh là cung gốc của lá số: bản chất, tư chất và xu hướng hành động, ảnh hưởng mạnh nhất ở nửa đầu đời. "
             "Luôn đọc cùng tam phương tứ chính: Thiên Di (xung chiếu), Quan Lộc và Tài Bạch (tam hợp).", CN2),
    "than": ("Cung Thân cho biết nơi bạn dồn sức khi trưởng thành và hậu vận. Thân luôn đóng ở một trong sáu cung: Mệnh, "
             "Phúc Đức, Quan Lộc, Thiên Di, Tài Bạch, Phu Thê (gọi là Thân cư …); ảnh hưởng của Thân rõ hơn từ khoảng 30 tuổi.", CN2),
    "cuc": ("Cục (Thủy nhị, Mộc tam, Kim tứ, Thổ ngũ, Hỏa lục) tính từ can năm sinh và cung Mệnh. Số Cục là tuổi bắt đầu đại "
            "hạn đầu tiên và dùng để an sao Tử Vi. Quan hệ ngũ hành giữa Cục và bản mệnh (sinh, khắc, đồng hành) cho biết môi "
            "trường có nâng đỡ bạn hay không.", "Prompt mục 5, " + CN2),
    "ban_menh": ("Bản mệnh là nạp âm của can chi năm sinh (ví dụ Lộ Bàng Thổ, Hải Trung Kim), thuộc một trong năm hành. "
                 "Tử Vi dùng bản mệnh để xét quan hệ với Cục và với hành của cung, sao.", CN2),
    "chinh_tinh": ("Chính tinh là 14 sao chính: Tử Vi, Thiên Cơ, Thái Dương, Vũ Khúc, Thiên Đồng, Liêm Trinh, Thiên Phủ, Thái "
                   "Âm, Tham Lang, Cự Môn, Thiên Tướng, Thiên Lương, Thất Sát, Phá Quân. Chính tinh quyết định tính chất chính "
                   "của cung; sao càng sáng càng phát huy mặt tốt.", CN3),
    "phu_tinh": ("Phụ tinh là các sao còn lại (lục cát, lục sát, Lộc Tồn, Thiên Mã, Đào Hoa, vòng Tràng Sinh…). Phụ tinh làm "
                 "cung tốt thêm hoặc kém đi so với tính chất của chính tinh.", CN3),
    "luc_cat": ("Lục cát tinh gồm Tả Phù, Hữu Bật, Văn Xương, Văn Khúc, Thiên Khôi, Thiên Việt: chủ được trợ giúp, học hành, "
                "quý nhân nâng đỡ.", CN3),
    "luc_sat": ("Lục sát tinh gồm Kình Dương, Đà La, Hỏa Tinh, Linh Tinh, Địa Không, Địa Kiếp: chủ trở ngại, áp lực, hao hụt. "
                "Khi đắc địa hoặc gặp chính tinh hợp cách, sát tinh lại thành động lực mạnh.", CN3 + "; Prompt mục 10.3"),
    "tu_hoa": ("Tứ Hóa (Hóa Lộc, Hóa Quyền, Hóa Khoa, Hóa Kỵ) an theo can năm sinh, làm biến đổi tính chất của sao mang hóa: "
               "Lộc là lợi lộc, Quyền là quyền hành, Khoa là danh tiếng và học vấn, Kỵ là vướng mắc. Khi xem vận còn có Tứ "
               "Hóa lưu theo can năm, can tháng.", CN4),
    "hoa_loc": ("Hóa Lộc chủ lợi lộc, cơ hội tài chính và sự suôn sẻ của việc mà sao mang hóa đại diện.", CN4),
    "hoa_quyen": ("Hóa Quyền chủ quyền hành, năng lực quyết đoán, được giao trách nhiệm.", CN4),
    "hoa_khoa": ("Hóa Khoa chủ danh tiếng, học vấn, thi cử và quý nhân giải nguy.", CN4),
    "hoa_ky": ("Hóa Kỵ chủ vướng mắc, trắc trở, thị phi ở việc mà sao mang hóa đại diện. Gặp Hóa Kỵ chỉ là lời nhắc nên làm "
               "kỹ, chậm, rõ ràng hơn ở lĩnh vực đó.", CN4 + "; " + P11),
    "tuan": ("Tuần (Tuần Không) án ngữ hai cung liền nhau, tính theo can chi năm sinh. Tuần làm giảm lực của sao trong hai "
             "cung: cung tốt bớt tốt, cung xấu bớt xấu. Theo quan niệm phổ biến, Tuần tác động rõ hơn ở nửa sau cuộc đời.", "Prompt mục 5, D-38"),
    "triet": ("Triệt (Triệt Lộ) án ngữ hai cung liền nhau, tính theo can năm sinh. Triệt chặn mạnh: cung tốt khó phát, cung "
              "xấu lại được giải. Theo quan niệm phổ biến, Triệt tác động mạnh ở nửa đầu cuộc đời.", "Prompt mục 5, D-38"),
    "dai_han": ("Đại hạn (đại vận) là vận 10 năm. Đại hạn đầu ở cung Mệnh, bắt đầu từ tuổi bằng số Cục, rồi mỗi 10 năm sang "
                "cung kế (thuận hay nghịch tùy âm dương nam nữ). Cung đại hạn cùng tam phương tứ chính được đọc như “cung "
                "Mệnh của 10 năm đó”.", P8 + ".1"),
    "tieu_han": ("Tiểu hạn là vận từng năm, an theo chi năm sinh và giới tính; cho biết chuyện gì diễn ra trong năm.", P8 + ".2"),
    "luu_nien": ("Lưu niên đại hạn là cung của năm trong đại hạn: năm đầu ở cung đại hạn, năm sau ở cung xung chiếu, rồi lùi "
                 "hay tiến theo công thức Tân Biên; cho biết kết cục của năm.", P8 + ".3"),
    "thai_tue": ("Lưu Thái Tuế là cung mang chi của năm đang xem (năm Ngọ ở cung Ngọ). Cung này cho biết lĩnh vực được “đánh "
                 "thức” trong năm và là Lưu Mệnh để xếp 12 lưu cung.", P8 + ".4"),
    "nguyet_han": ("Nguyệt hạn là cung của từng tháng âm; nhật hạn và thời hạn là cung của ngày, giờ. Tầng càng nhỏ thì độ tin "
                   "cậy càng thấp, nên chỉ dùng để sắp xếp việc.", P8 + ".5"),
    "tam_phuong": ("Tam phương tứ chính của một cung gồm chính cung đó, cung xung chiếu (đối diện) và hai cung tam hợp. Luận một "
                   "cung luôn xét cả bốn cung này; cung xung chiếu ảnh hưởng mạnh nhất sau chính cung.", CN2),
    "xung_chieu": ("Cung xung chiếu là cung đối diện, cách sáu cung (Mệnh đối Thiên Di, Tài Bạch đối Phúc Đức…). Sao ở cung "
                   "xung chiếu chiếu sang và ảnh hưởng mạnh tới cung đang xét.", CN2),
    "giap": ("Giáp cung là hai cung kề hai bên. Hai cung kề có cặp sao tốt (Tả Hữu, Xương Khúc, Khôi Việt, Nhật Nguyệt, Quyền "
             "Lộc) là giáp cát, được nâng đỡ; có cặp sát tinh (Kình Đà, Không Kiếp, Hỏa Linh) là giáp hung, dễ bị chèn ép.", "Prompt mục 10"),
    "nhi_hop": ("Nhị hợp là cặp cung có chi lục hợp: Tý–Sửu, Dần–Hợi, Mão–Tuất, Thìn–Dậu, Tỵ–Thân, Ngọ–Mùi. Ảnh hưởng nhẹ "
                "hơn tam phương tứ chính.", CN2),
    "vo_chinh_dieu": ("Vô chính diệu là cung không có chính tinh. Khi đó mượn chính tinh cung xung chiếu để luận, lực giảm còn "
                      "khoảng bảy phần. Mệnh vô chính diệu gặp đủ Tuần, Triệt, Thiên Không, Địa Không là cách “vô chính diệu "
                      "đắc tam không”.", "Prompt mục 10.4"),
    "do_sang": ("Độ sáng của sao theo từng cung: Miếu (M), Vượng (V), Đắc (Đ), Bình hòa (B), Hãm (H). Sao miếu vượng phát huy "
                "mặt tốt; sao hãm dễ lộ mặt hạn chế, cần sao tốt hội về để đỡ.", "Prompt mục 6"),
    "dong_thanh": ("Đồng thanh tương ứng: một sự kiện chỉ được nói mạnh khi nhiều tầng vận (đại hạn, lưu niên đại hạn, tiểu "
                   "hạn, Lưu Thái Tuế) cùng chỉ về một hướng. App ghi: từ 3 tầng là khả năng cao, 2 tầng là có thể, 1 tầng "
                   "mới là dấu hiệu.", P9),
    "the": ("Thế của giai đoạn theo điểm: từ 70 là Tấn (chủ động mở rộng), 50–69 là Giữ (củng cố), 35–49 là Thủ (tránh rủi "
            "ro lớn), dưới 35 là Lùi (hoãn quyết định lớn nếu được).", P11),
    "diem": ("Điểm 0–100 tổng hợp sao tốt, xấu ở các cung đọc chính, có trọng số theo từng tầng vận. 50 là mức trung bình, từ "
             "55 là thuận, từ 45 trở xuống nên thận trọng. Điểm dùng để so sánh tương đối, không phải lời phán.", P9),
    "thien_thoi": ("Thiên thời của đại hạn so hành tam hợp của chi năm sinh với hành tam hợp của chi cung đại hạn: đại hạn sinh "
                   "tuổi là đắc thiên thời; đồng hành là hòa hợp; tuổi khắc đại hạn thì vất vả nhưng giành được; tuổi sinh đại "
                   "hạn thì hao tâm tổn sức; đại hạn khắc tuổi là thất thời, nên thủ hơn công.", CN4),
    "sao_luu": ("Sao lưu là sao an theo năm (hoặc tháng) đang xem, như Lưu Thái Tuế, Lưu Lộc Tồn, Lưu Kình Dương, Lưu Đào "
                "Hoa; chồng lên lá số gốc để xem năm đó. Trên lưới, sao lưu có tiền tố “L.”.", P8 + ".4"),
    "cach_cuc": ("Cách cục là tổ hợp sao và cung có tên riêng, như Tử Phủ Vũ Tướng, Sát Phá Tham, Nhật Nguyệt tịnh minh. Khi "
                 "một cách cục kích hoạt, app ưu tiên luận theo cách cục thay cho câu luận từng sao.", "Prompt mục 9, 10"),
    "truyen_cung": ("Truyền cung là khi cung Phu Thê (vợ chồng) hoặc Tử Tức (cha mẹ – con) của người này trùng cung Mệnh của "
                    "người kia; truyền tinh là khi hai cung đó có cùng bộ chính tinh. Xem trong mục So hai lá số.", CN5 + ".4"),
    "menh_chu": ("Mệnh chủ an theo chi của cung Mệnh, Thân chủ an theo chi năm sinh; là hai sao đại diện, dùng tham khảo thêm "
                 "về tính cách và cách hành xử.", CN2),
    "han": ("“Hạn” là vận trình theo thời gian: đại hạn 10 năm, tiểu hạn và lưu niên từng năm, nguyệt hạn từng tháng. Năm có "
            "nhiều sao xấu chỉ là giai đoạn nên thận trọng hơn, không phải điều nhất định xảy ra.", P8 + "; " + P11),
}

# Từ chối nhẹ nhàng (mục 11, 12.4 quy tắc 4–6).
REFUSE = {
    "qa.tu_choi.em_be": ("App không đoán bé là trai hay gái. Pháp luật Việt Nam cấm phổ biến các cách chọn lựa giới tính em bé, "
                         "nên tính năng này không có trong app. Mình có thể xem chuyện con cái, gia đạo trong lá số của bạn.", "Prompt mục 10.5"),
    "qa.tu_choi.song_lau": ("App không bàn chuyện sống được bao lâu hay thời điểm mất của ai. Mình có thể chỉ ra những giai đoạn "
                            "nên chăm sóc sức khỏe kỹ hơn để bạn chủ động.", P11 + " quy tắc 2"),
    "qa.tu_choi.so": ("Tử Vi không dùng để đoán con số, xổ số hay kết quả cá cược, nên mình không trả lời phần này. Bạn có thể hỏi "
                      "về tài chính, cách giữ tiền trong năm.", P11 + " quy tắc 4"),
    "qa.tu_choi.nguoi_khac": ("Mình chỉ luận được lá số đang mở. Muốn xem cho người khác, hãy thêm ngày giờ sinh của người đó vào "
                              "hồ sơ, rồi mở lá số ấy hoặc dùng mục So hai lá số.", P12 + ".4 quy tắc 6"),
    "qa.tu_choi.ngoai": ("Câu này nằm ngoài Tử Vi nên mình không trả lời được. Bạn có thể hỏi về tính cách, công việc, tài chính, "
                         "tình duyên, gia đạo, sức khỏe, đi xa, nhà đất… theo năm, tháng, ngày hoặc trọn đời.", P12 + ".4 quy tắc 6"),
    "qa.tu_choi.dich_vu": ("App không khuyên các dịch vụ hóa giải vận hạn có thu phí. Theo tinh thần “đức năng thắng số”, cách "
                           "chủ động nhất là sống điều độ, cẩn trọng ở giai đoạn khó và làm việc thiện.", P11 + " quy tắc 4"),
}

# Chuyển hướng: không phán, nhưng vẫn trả lời phần luận phù hợp (mục 11).
REFRAME = {
    "qa.chuyen.hon_nhan": ("App không phán chuyện tan vỡ hay chia lìa. Dưới đây là phần luận tình duyên, hôn nhân để bạn biết lúc "
                           "nào nên vun đắp, lắng nghe nhiều hơn.", P11 + " quy tắc 2, 6"),
    "qa.chuyen.benh": ("App không chẩn đoán bệnh. Phần sức khỏe chỉ là gợi ý giai đoạn nên chăm sóc bản thân kỹ hơn; khi có triệu "
                       "chứng, hãy đi khám bác sĩ.", P11 + " quy tắc 3"),
    "qa.chuyen.phap_ly": ("App không phán chuyện kiện tụng hay giam giữ. Dưới đây là phần pháp lý, thị phi: những lúc nên cẩn trọng "
                          "giấy tờ, hợp đồng và hỏi luật sư khi cần.", P11 + " quy tắc 2"),
    "qa.chuyen.hop_tuoi": ("Xem tuổi hợp, tam tai, kim lâu nằm ngoài phạm vi bản này. App có mục So hai lá số theo cẩm nang (truyền "
                           "cung, truyền tinh) khi bạn đã lưu lá số của cả hai người.", "Prompt mục 1 (phạm vi v1); " + CN5 + ".4"),
    "qa.chuyen.chon_ngay": ("Điểm ngày dưới đây tính theo lá số của bạn (tháng, nhật hạn, can chi ngày so với tuổi), không phải "
                            "chọn ngày theo Ngọc Hạp; độ tin cậy thấp hơn tầng năm.", "Prompt mục 1, 2.3"),
    "qa.chuyen.nguoi_than": ("Lá số đang mở là của bạn, nên chuyện của người thân chỉ đọc gián tiếp qua cung tương ứng (Phu Thê cho "
                             "vợ chồng, Tử Tức cho con cái, Phụ Mẫu cho cha mẹ, Huynh Đệ cho anh chị em), tức là mối quan hệ của bạn "
                             "với họ. Muốn xem vận riêng của người đó, hãy thêm ngày giờ sinh của họ vào hồ sơ.", P9 + "; " + P12 + ".4 quy tắc 6"),
    "qa.chuyen.tai_nan": ("App không phán tai họa. Dưới đây là các giai đoạn nên cẩn trọng đi lại, vận động hơn thường lệ.", P11 + " quy tắc 2"),
}

OTHER = {
    "qa.khung_hoang": ("Mình rất tiếc vì bạn đang phải trải qua lúc khó khăn như vậy. Lúc này điều quan trọng nhất là bạn được an "
                       "toàn và có người ở bên. Hãy gọi ngay cho một người thân hoặc bạn bè tin cậy. Nếu bạn đang gặp nguy hiểm, "
                       "gọi cấp cứu 115. Bạn có thể gọi Đường dây nóng Ngày Mai 096 306 1414 (hỗ trợ tâm lý miễn phí, thường "
                       "13:00–20:30 từ thứ Tư đến Chủ Nhật). Trẻ em và người dưới 16 tuổi có thể gọi Tổng đài 111 (miễn phí, cả "
                       "ngày đêm). Bạn không phải một mình đối mặt với chuyện này.", P12 + ".2 bước 6"),
    "qa.gioi_thieu": ("Mình là phần hỏi đáp của Tử Vi Tự Xem, trả lời dựa trên lá số đang mở và bản luận app đã tính, ngay trên máy "
                      "của bạn. Bạn có thể hỏi về tính cách, công việc, tài chính, tình duyên, gia đạo, sức khỏe, đi xa, nhà đất; "
                      "theo năm, tháng, ngày, đại vận hoặc trọn đời; hỏi khi nào thuận cho một việc; hoặc hỏi nghĩa của một sao, "
                      "một cung, một thuật ngữ.", P12),
    "qa.chua_hieu": ("Mình chưa hiểu rõ câu hỏi này. Bạn thử hỏi cụ thể hơn, ví dụ nêu chủ đề (công việc, tiền bạc, tình duyên…) "
                     "và thời gian (năm nay, năm 2027, tháng 3 âm, ngày mai…), hoặc chọn một câu gợi ý bên dưới.", P12 + ".4 quy tắc 1"),
    "qa.chua_du": ("Lá số chưa đủ dữ kiện để trả lời riêng câu này; dưới đây là phần gần nhất app tính được.", P12 + ".4 quy tắc 1"),
    "qa.truoc_nam_sinh": ("Năm bạn hỏi đứng trước năm sinh của lá số đang mở nên chưa có vận hạn để xem. Bạn thử hỏi một năm từ năm "
                          "sinh trở đi, hoặc mở đúng lá số của người cần xem.", "Mục 8 (vận hạn tính từ năm sinh)"),
    "qa.note.doi_chieu": ("Đây là các tín hiệu lá số cho những năm đã qua; bạn đối chiếu với việc đã thật sự xảy ra. Nhiều năm khớp "
                          "thì giờ sinh khá đúng; lệch nhiều thì nên xem lại giờ sinh.", P9),
    "qa.khong_su_kien": ("Trong khoảng năm đã dò, chưa thấy năm nào có dấu hiệu đồng thanh rõ cho việc này; dưới đây là những năm "
                         "chủ đề liên quan có điểm cao nhất.", P9),
    "qa.note.kha_nang": ("Đây là mức khả năng theo số tầng vận cùng chỉ, không phải lời khẳng định; quyết định vẫn là của bạn.", P9 + "; " + P11),
    "qa.note.tre_nho": ("Lá số này dưới 16 tuổi âm: phần hỏi đáp chỉ trả lời về học tập, sức khỏe, gia đình và an toàn đi lại.", "D-39"),
    "qa.note.nha_dat": ("Nhà đất không có điểm riêng theo năm: app xem kèm điểm tài chính cùng giai đoạn (Điền Trạch là cung phụ "
                        "của tài chính), nên đây là gợi ý gián tiếp.", P9),
    "qa.note.khong_diem_nam": ("Chủ đề này luận ở lá số gốc, không có điểm riêng theo năm; dưới đây xếp theo điểm cả năm.", P9),
    "qa.note.thang_tong": ("Điểm tháng là điểm chung của tháng (năm, nguyệt hạn, Tứ Hóa tháng); tháng có nguyệt hạn vào cung của "
                           "chủ đề thì chủ đề đó được chú ý nhiều hơn.", P9 + " bước 6"),
}


def hoidap_texts():
    out = {}

    def put(key, text, source):
        out[key] = {"key": key, "text": text, "source": source, "reviewed": False}

    for k, (t, s) in DEFS.items():
        put(f"qa.def.{k}", t, s)
    for group in (REFUSE, REFRAME, OTHER):
        for k, (t, s) in group.items():
            put(k, t, s)
    return out
