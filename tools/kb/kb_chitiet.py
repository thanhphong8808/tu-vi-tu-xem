"""Câu luận chi tiết (D-65, D-66, D-67): diễn biến cụ thể theo chủ đề × tác nhân năm, và so hai lá số theo ngũ hành, can chi.

- ``ct.<CHỦ_ĐỀ>.<TÁC_NHÂN>``: điều dễ xảy ra khi tác nhân (Tứ Hóa lưu, sao lưu) chạm cung của chủ đề trong năm.
  Viết ở thể trung tính để dùng được cho năm tới lẫn năm đã qua (“Năm 2015 lá số cho thấy: …”).
- ``ct.<CHỦ_ĐỀ>.<TÁC_NHÂN>.nen``: việc nên làm tương ứng (chỉ dùng cho năm hiện tại, năm tới).
- ``hop.*``: so hai lá số — ngũ hành bản mệnh, thiên can, địa chi năm sinh, vị trí cung Mệnh, Cục.

Mọi câu là mức khả năng, không phán chắc; đi qua KbLint như các câu khác.
"""

SRC_CT = "Cẩm nang V2 P.III–P.IV (Tứ Hóa, sao lưu theo cung); Prompt mục 8–9"
SRC_HOP = "Cẩm nang V2 P.I (ngũ hành, can chi), P.V.4 (so hai lá số)"

# Chủ đề → tác nhân → (điều dễ xảy ra, việc nên làm). Tác nhân: Tứ Hóa lưu; Lộc Tồn, Thiên Mã, Kình Dương, Đà La lưu;
# nhóm Đào Hoa/Hồng Loan/Thiên Hỷ/Hỷ Thần lưu; Tang Môn/Bạch Hổ lưu; Thiên Khốc/Thiên Hư lưu.
CT = {
    "SU_NGHIEP": {
        "HOA_LOC": ("công việc có thêm cơ hội, nguồn thu hoặc dự án mới; dễ được giao phần việc có lợi",
                    "nhận việc mới, mở rộng quan hệ công việc, chốt các thỏa thuận đang chờ"),
        "HOA_QUYEN": ("dễ được trao quyền, thêm trách nhiệm quản lý hoặc có tiếng nói lớn hơn trong tập thể",
                      "nhận vai trò dẫn dắt, giữ cách nói mềm để tránh va chạm"),
        "HOA_KHOA": ("uy tín được chú ý; thuận thi cử, bằng cấp, được người có vị thế nâng đỡ",
                     "học thêm, lấy chứng chỉ, trình bày thành tích với cấp trên"),
        "HOA_KY": ("công việc dễ vướng thị phi, hiểu lầm hoặc trì trệ giấy tờ; kế hoạch có thể phải làm lại",
                   "giữ hồ sơ rõ ràng, nói ít làm nhiều, tránh tranh cãi với cấp trên"),
        "LOC_TON": ("thu nhập từ công việc ổn định hơn, hợp tích lũy hơn là mạo hiểm",
                    "giữ việc chính, để dành phần dư"),
        "THIEN_MA": ("công việc nhiều di chuyển; có thể đổi chỗ làm, đi công tác xa hoặc mở thị trường mới",
                     "chuẩn bị kỹ trước khi đổi việc, ưu tiên cơ hội có lộ trình rõ"),
        "KINH_DUONG": ("cạnh tranh gắt, áp lực chỉ tiêu, dễ va chạm với đồng nghiệp",
                       "giữ bình tĩnh, chọn việc đáng tranh, không quyết định khi đang nóng"),
        "DA_LA": ("việc dễ chậm, kéo dài, vướng thủ tục; phải kiên nhẫn mới xong",
                  "chia việc nhỏ, đặt mốc kiểm tra, không đốt cháy giai đoạn"),
        "DAO_HONG_HY": ("quan hệ với đồng nghiệp, khách hàng vui vẻ; dễ có tin vui trong công việc",
                        "tận dụng các dịp gặp gỡ để mở rộng quan hệ"),
        "TANG_HO": ("công việc dễ có chuyện phiền lòng, mệt mỏi; cần để ý an toàn khi làm việc nặng",
                    "giảm tải, giữ an toàn lao động, không nhận việc quá sức"),
        "KHOC_HU": ("tâm trạng dễ chùng xuống, thấy công sức chưa được ghi nhận",
                    "ghi lại kết quả cụ thể, trao đổi thẳng thắn với người phụ trách"),
    },
    "TAI_CHINH": {
        "HOA_LOC": ("dòng tiền vào khá hơn, có khoản thu thêm hoặc cơ hội làm ăn",
                    "chủ động tìm nguồn thu nhưng vẫn giữ phần dự phòng"),
        "HOA_QUYEN": ("nắm quyền quyết định về tiền bạc; có thể đứng ra quản lý ngân sách hoặc khoản đầu tư",
                      "quyết đoán nhưng có kế hoạch, ghi chép rõ ràng"),
        "HOA_KHOA": ("tài chính ổn nhờ uy tín; dễ được hỗ trợ hoặc vay mượn khi cần",
                     "giữ chữ tín, minh bạch giấy tờ tiền bạc"),
        "HOA_KY": ("tiền bạc dễ vướng: khoản chi bất ngờ, nợ chậm thu hoặc hiểu lầm về tiền",
                   "không cho vay lớn, không đứng ra bảo lãnh, đọc kỹ hợp đồng"),
        "LOC_TON": ("thu nhập đều, hợp tích lũy; có thể nhận lộc từ gia đình hoặc từ công việc ổn định",
                    "lập quỹ tiết kiệm, tránh đầu cơ"),
        "THIEN_MA": ("tiền đến từ đi lại, buôn bán hoặc việc ở xa; tiền vào nhanh thì ra cũng nhanh",
                     "theo dõi dòng tiền hằng tháng"),
        "KINH_DUONG": ("dễ hao tiền vì cạnh tranh, tranh chấp hoặc quyết định vội",
                       "không xuống tiền khi đang nóng, đặt hạn mức chi tiêu"),
        "DA_LA": ("tiền về chậm, khoản thu bị kéo dài, thủ tục rườm rà",
                  "dự phòng tiền mặt, cho vay hay đòi nợ đều có giấy tờ"),
        "DAO_HONG_HY": ("chi tiêu cho tiệc tùng, quà cáp, chuyện vui tăng lên",
                        "đặt ngân sách riêng cho các dịp vui"),
        "TANG_HO": ("có thể phát sinh chi phí cho sức khỏe, việc gia đình hoặc sửa chữa",
                    "giữ quỹ dự phòng, cân nhắc bảo hiểm phù hợp"),
        "KHOC_HU": ("dễ lo lắng về tiền dù thực tế chưa đến mức đó",
                    "lập bảng thu chi để nhìn rõ con số"),
    },
    "TINH_DUYEN": {
        "HOA_LOC": ("tình cảm ngọt hơn; người độc thân dễ gặp người hợp ý, người có đôi gắn bó hơn",
                    "chủ động hẹn hò, bàn chuyện tương lai"),
        "HOA_QUYEN": ("một bên muốn làm chủ trong quan hệ; dễ có quyết định lớn như ra mắt gia đình, cưới hỏi",
                      "lắng nghe nhau, tránh áp đặt"),
        "HOA_KHOA": ("quan hệ được người xung quanh ủng hộ, dễ có người mai mối",
                     "giới thiệu người ấy với gia đình, bạn bè"),
        "HOA_KY": ("dễ hiểu lầm, ghen tuông hoặc lời nói làm tổn thương nhau",
                   "nói rõ suy nghĩ, tránh im lặng kéo dài"),
        "LOC_TON": ("tình cảm ổn định, bền; hợp vun đắp lâu dài",
                    "chăm lo những việc nhỏ hằng ngày cho nhau"),
        "THIEN_MA": ("tình cảm gắn với đi lại: yêu xa, gặp người ở nơi khác hoặc cùng nhau chuyển chỗ ở",
                     "giữ liên lạc đều đặn"),
        "KINH_DUONG": ("dễ cãi vã, nóng nảy, va chạm cái tôi",
                       "chậm lại một nhịp trước khi nói"),
        "DA_LA": ("chuyện tình cảm chậm, dây dưa, khó dứt điểm",
                  "đặt mốc rõ ràng cho mối quan hệ"),
        "DAO_HONG_HY": ("dễ có người để ý, nhiều dịp gặp gỡ; người có đôi dễ có tin vui",
                        "mở lòng nhưng chọn lọc"),
        "TANG_HO": ("trong nhà hoặc trong quan hệ có chuyện buồn phải cùng gánh vác",
                    "ở bên nhau, chia sẻ việc nhà"),
        "KHOC_HU": ("cảm giác cô đơn, hụt hẫng dễ đến",
                    "dành thời gian chất lượng cho nhau"),
    },
    "SUC_KHOE": {
        "HOA_LOC": ("sức khỏe hồi phục tốt, dễ gặp thầy thuốc giỏi hoặc cách chữa hợp",
                    "khám định kỳ, duy trì thói quen tốt"),
        "HOA_QUYEN": ("thể lực khá nhưng dễ làm quá sức",
                      "tập vừa sức, ngủ đủ"),
        "HOA_KHOA": ("dễ tìm được thông tin và người hỗ trợ đúng về sức khỏe",
                     "hỏi ý kiến chuyên môn trước khi dùng thuốc"),
        "HOA_KY": ("dễ mệt mỏi kéo dài, bệnh cũ dễ trở lại hoặc lo âu ảnh hưởng giấc ngủ",
                   "đi khám khi dấu hiệu kéo dài, không tự ý dùng thuốc"),
        "LOC_TON": ("sức khỏe ổn định", "giữ nếp sinh hoạt đều"),
        "THIEN_MA": ("đi lại nhiều dễ mệt; đổi môi trường làm cơ thể khó thích nghi",
                     "nghỉ đủ giữa các chuyến đi"),
        "KINH_DUONG": ("dễ va chạm, trầy xước, hoặc phải can thiệp nhỏ như nhổ răng, tiểu phẫu",
                       "cẩn thận khi lái xe, làm việc với máy móc, dao kéo"),
        "DA_LA": ("bệnh nhẹ nhưng dai dẳng, lâu khỏi", "điều trị đủ liệu trình"),
        "DAO_HONG_HY": ("tinh thần vui, sức khỏe được nâng đỡ", "giữ chừng mực khi ăn uống, tiệc tùng"),
        "TANG_HO": ("cần để ý sức khỏe của bản thân và người lớn tuổi trong nhà, nhất là khi đi lại",
                    "đưa người lớn tuổi đi khám, giữ an toàn khi đi đường"),
        "KHOC_HU": ("dễ buồn bã, mất ngủ, căng thẳng", "vận động nhẹ, trò chuyện với người thân"),
    },
    "GIA_DAO": {
        "HOA_LOC": ("gia đình êm ấm, có thể đón tin vui hoặc thêm tài sản chung",
                    "tổ chức sum họp, cùng lên kế hoạch chung"),
        "HOA_QUYEN": ("có người trong nhà đứng ra quyết việc lớn; dễ bất đồng ý kiến",
                      "bàn bạc cùng nhau trước khi quyết"),
        "HOA_KHOA": ("gia đình có tiếng tốt, con cái học hành tiến bộ", "khích lệ người nhà"),
        "HOA_KY": ("trong nhà dễ hiểu lầm, chuyện cũ được nhắc lại", "nói chuyện thẳng thắn, nhẹ nhàng"),
        "LOC_TON": ("nền nếp ổn định, có của để dành", "chăm lo việc nhà đều đặn"),
        "THIEN_MA": ("người trong nhà đi xa, chuyển chỗ ở hoặc thay đổi nếp sống", "giữ liên lạc thường xuyên"),
        "KINH_DUONG": ("dễ to tiếng, va chạm giữa các thế hệ", "nhường nhịn, tránh lời nặng"),
        "DA_LA": ("việc nhà dây dưa, khó dứt điểm", "phân công việc rõ ràng"),
        "DAO_HONG_HY": ("nhà có chuyện vui: cưới hỏi, đón thành viên mới, tân gia", "chuẩn bị chu đáo cho dịp vui"),
        "TANG_HO": ("có thể phải lo chuyện sức khỏe hoặc chuyện buồn của người lớn trong họ", "thăm hỏi người lớn tuổi"),
        "KHOC_HU": ("không khí gia đình dễ trầm, có người buồn phiền", "dành thời gian bên nhau"),
    },
    "DI_CHUYEN": {
        "HOA_LOC": ("đi xa có lợi: công tác, buôn bán, học tập ở nơi khác thuận", "chủ động nắm cơ hội ở nơi khác"),
        "HOA_QUYEN": ("ra ngoài được nể trọng, có vai trò ở môi trường mới", "chuẩn bị kỹ trước khi nhận việc ở xa"),
        "HOA_KHOA": ("đi xa gặp quý nhân, thủ tục giấy tờ thuận", "nộp hồ sơ, xin thị thực vào thời điểm này"),
        "HOA_KY": ("đi lại dễ trục trặc: lỡ chuyến, vướng giấy tờ, hiểu lầm ở nơi xa", "kiểm tra giấy tờ, lịch trình kỹ"),
        "LOC_TON": ("ra ngoài có lộc nhỏ, ổn định", "giữ quan hệ tốt ở nơi mới"),
        "THIEN_MA": ("nhiều chuyến đi; có thể đổi chỗ ở hoặc chỗ làm", "lên kế hoạch rõ cho việc chuyển"),
        "KINH_DUONG": ("đi đường dễ va quẹt, dễ tranh chấp với người lạ", "lái xe cẩn thận, giữ bình tĩnh"),
        "DA_LA": ("chuyến đi chậm, dời lịch, thủ tục kéo dài", "đặt lịch dư thời gian"),
        "DAO_HONG_HY": ("đi xa có niềm vui, gặp người mới", "mở rộng quan hệ ở nơi mới"),
        "TANG_HO": ("đi xa nên để ý an toàn", "không đi đường dài khi mệt, chuẩn bị bảo hiểm"),
        "KHOC_HU": ("xa nhà dễ nhớ nhà, cô đơn", "giữ liên lạc với người thân"),
    },
    "PHAP_LY": {
        "HOA_LOC": ("giấy tờ, thủ tục suôn sẻ hơn; tranh chấp dễ hòa", "giải quyết dứt điểm hồ sơ tồn đọng"),
        "HOA_QUYEN": ("có lợi thế khi thương lượng", "thương lượng dựa trên chứng cứ"),
        "HOA_KHOA": ("được người có chuyên môn giúp, dễ làm rõ đúng sai", "nhờ luật sư, chuyên gia tư vấn"),
        "HOA_KY": ("dễ vướng thị phi, khiếu nại hoặc sai sót giấy tờ", "đọc kỹ trước khi ký, lưu chứng từ"),
        "LOC_TON": ("giấy tờ ổn định", "lưu giữ hồ sơ cẩn thận"),
        "THIEN_MA": ("có thủ tục liên quan đến xuất nhập cảnh, chuyển hộ khẩu hoặc đăng ký mới", "chuẩn bị đủ hồ sơ"),
        "KINH_DUONG": ("dễ tranh chấp, cãi vã dẫn tới khiếu nại", "hòa giải trước, tránh đối đầu"),
        "DA_LA": ("hồ sơ kéo dài, dễ bị trả lại", "kiên nhẫn, theo dõi tiến độ"),
        "TANG_HO": ("chuyện giấy tờ dễ gây mệt mỏi", "nhờ người tin cậy cùng lo"),
        "KHOC_HU": ("dễ lo lắng về thủ tục", "hỏi rõ quy trình trước khi làm"),
    },
}

# So hai lá số. {a}, {b}: tên hai người; {ha}, {hb}: hành; {x}: chi tiết.
HOP = {
    "hop.menh.sinh": "Bản mệnh {a} ({ha}) sinh bản mệnh {b} ({hb}): {a} có xu hướng nâng đỡ, lo cho {b}; {b} được hưởng sự chăm sóc. "
                     "Quan hệ thuận, chỉ cần {b} biết ghi nhận để {a} không thấy hao sức.",
    "hop.menh.khac": "Bản mệnh {a} ({ha}) khắc bản mệnh {b} ({hb}): {a} dễ lấn át, {b} dễ thấy bị gò bó. Đây không phải điều xấu "
                     "cố định: cần {a} mềm mỏng, tôn trọng khoảng riêng của {b}; khi có người thứ ba hoặc hành trung gian thì dễ hòa.",
    "hop.menh.hoa": "Hai bản mệnh cùng hành {ha}: dễ hiểu nhau, cùng nhịp sống; nhưng cũng dễ cùng nóng hoặc cùng chần chừ, nên bù trừ cho nhau.",
    "hop.menh.trung_gian": "Hành trung gian giúp hai bản mệnh hòa nhau là {x}: những việc, màu sắc, môi trường thuộc hành này "
                           "(theo ngũ hành) giúp hai người dễ dung hòa.",
    "hop.can.hop": "Thiên can năm sinh {x} là cặp ngũ hợp: dễ đồng lòng trong quyết định lớn, chung mục tiêu.",
    "hop.can.sinh": "Thiên can năm sinh {x} tương sinh: người này dễ bổ trợ ý tưởng cho người kia.",
    "hop.can.khac": "Thiên can năm sinh {x} tương khắc: cách nghĩ dễ trái nhau khi bàn việc lớn; nên thống nhất nguyên tắc chung từ đầu.",
    "hop.can.hoa": "Thiên can năm sinh {x} cùng hành: tính toán giống nhau, dễ cùng quan điểm.",
    "hop.chi.tam_hop": "Tuổi {x} nằm trong một nhóm tam hợp: hợp tính, dễ cùng chí hướng, làm việc chung thuận.",
    "hop.chi.luc_hop": "Tuổi {x} lục hợp: dễ gắn bó, biết bù đắp cho nhau.",
    "hop.chi.xung": "Tuổi {x} lục xung: tính cách và nhịp sống khác nhau rõ, dễ bất đồng; cần thống nhất cách sống chung và nhường nhau ở việc nhỏ.",
    "hop.chi.hai": "Tuổi {x} lục hại: dễ phật ý vì chuyện nhỏ, hiểu lầm âm ỉ; nên nói rõ thay vì để trong lòng.",
    "hop.chi.hinh": "Tuổi {x} tương hình: dễ góp ý nặng lời với nhau; giữ lời nói nhẹ nhàng, tôn trọng.",
    "hop.chi.dong": "Hai người cùng tuổi {x}: hiểu nhau nhanh, nhưng dễ cùng cố chấp ở một điểm.",
    "hop.chi.binh": "Tuổi {x} không xung, không hợp: quan hệ bình thường, hợp hay không tùy cách cư xử.",
    "hop.cung.tam_hop": "Cung Mệnh hai người ({x}) tam hợp: cách nhìn đời gần nhau.",
    "hop.cung.luc_hop": "Cung Mệnh hai người ({x}) lục hợp: dễ nương tựa nhau.",
    "hop.cung.xung": "Cung Mệnh hai người ({x}) xung chiếu: mỗi người một hướng, bổ sung cho nhau nếu biết tôn trọng khác biệt.",
    "hop.cung.dong": "Cung Mệnh hai người cùng ở {x}: tính cách nền giống nhau.",
    "hop.cuc.sinh": "Cục của {a} ({x}) sinh bản mệnh {b} ({hb}): hoàn cảnh, cách làm việc của {a} giúp {b} phát huy.",
    "hop.cuc.khac": "Cục của {a} ({x}) khắc bản mệnh {b} ({hb}): nhịp sống của {a} có lúc tạo áp lực cho {b}; nên chia việc theo sở trường.",
    "hop.chinh.sinh": "Chính tinh cung Mệnh của {a} ({x}) sinh chính tinh Mệnh của {b} ({y}): {a} dễ khơi gợi điểm mạnh của {b}.",
    "hop.chinh.khac": "Chính tinh cung Mệnh của {a} ({x}) khắc chính tinh Mệnh của {b} ({y}): cách làm của {a} có lúc lấn át {b}; nên chia vai rõ ràng.",
    "hop.tong.tot": "Nhìn chung hai lá số nhiều điểm tương sinh, hòa hợp ({x}): nền gắn bó tốt; giữ được sự tôn trọng thì quan hệ bền.",
    "hop.tong.trung": "Nhìn chung hai lá số vừa có điểm hợp vừa có điểm khắc ({x}): hợp hay không phụ thuộc nhiều vào cách hai người cư xử.",
    "hop.tong.can": "Nhìn chung hai lá số có nhiều điểm khắc, xung ({x}): cần nhiều nhường nhịn và thỏa thuận rõ; đây là chỗ cần vun đắp, "
                    "không phải lời phán quan hệ không thành.",
    "hop.note": "Tương sinh tương khắc chỉ là một góc nhìn theo ngũ hành và can chi; sự hiểu nhau, tôn trọng và cách sống chung mới là điều quyết định.",
}


def chitiet_texts():
    out = {}

    def put(key, text, source):
        out[key] = {"key": key, "text": text, "source": source, "reviewed": False}

    for topic, d in CT.items():
        for trig, (what, nen) in d.items():
            put(f"ct.{topic}.{trig}", what, SRC_CT)
            put(f"ct.{topic}.{trig}.nen", nen, SRC_CT)
    for k, t in HOP.items():
        put(k, t, SRC_HOP)
    return out
