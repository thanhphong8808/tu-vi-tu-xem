"""Nguồn soạn bộ luật cách cục, câu luận của luật và khuyến nghị (giai đoạn 3).

Chạy tools/kb/build_kb.py để xuất JSON vào data/kb/src/main/resources/kb/. App chỉ đọc JSON; muốn sửa luật hay câu
luận thì sửa ở đây rồi chạy lại (DECISIONS D-34). Mọi mục đều `reviewed: false` cho tới khi người dùng duyệt.
Câu chữ theo quy tắc diễn đạt mục 11: nói xu hướng, không phán định mệnh, không phán bệnh, ly hôn, tù tội.
"""

P10_1 = "Prompt mục 10.1"
P10_2 = "Prompt mục 10.2"
P10_3 = "Prompt mục 10.3"
P10_4 = "Prompt mục 10.4"
P10_5 = "Prompt mục 10.5"
CN_II = "Cẩm nang V2 – P.II"
CN_III = "Cẩm nang V2 – P.III"
CN_IV = "Cẩm nang V2 – P.IV"
CN_V = "Cẩm nang V2 – P.V"
CN_I3 = "Cẩm nang V2 – P.I.3"

CHINH = ["TU_VI", "THIEN_CO", "THAI_DUONG", "VU_KHUC", "THIEN_DONG", "LIEM_TRINH", "THIEN_PHU",
         "THAI_AM", "THAM_LANG", "CU_MON", "THIEN_TUONG", "THIEN_LUONG", "THAT_SAT", "PHA_QUAN"]
SAT6 = ["KINH_DUONG", "DA_LA", "HOA_TINH", "LINH_TINH", "DIA_KHONG", "DIA_KIEP"]
TU_SAT = SAT6[:4]
TU_MO = ["THIN", "TUAT", "SUU", "MUI"]
TU_SINH = ["DAN", "THAN", "TI", "HOI"]
SANG = ["M", "V", "Đ"]
MIEU_VUONG = ["M", "V"]
LUU_TINH_DUYEN = ["DAO_HOA", "HONG_LOAN", "HY_THAN", "LONG_DUC", "NGUYET_DUC"]


# ——————————————————————————— trợ giúp viết predicate ———————————————————————————
def S(star, inn="self", **kw):
    d = {"star": star, "in": inn}
    d.update(kw)
    return d

def L(star, inn="self", **kw):
    d = {"luuStar": star, "in": inn}
    d.update(kw)
    return d

def ALL(*p): return {"allOf": list(p)}
def ANY(*p): return {"anyOf": list(p)}
def NONE(*p): return {"noneOf": list(p)}
def CNT(stars, n, inn="self"): return {"countOf": list(stars), "in": inn, "atLeast": n}
def CHI(*c): return {"chiIn": list(c)}
def AT(loc, **kw):
    d = {"at": loc}
    d.update(kw)
    return d

def any_self(stars, inn="self", **kw): return ANY(*[S(s, inn, **kw) for s in stars])
def none_of(stars, inn="self"): return NONE(*[S(s, inn) for s in stars])


ADVICE = {}

def adv(key, type_, topic, text, severity=1, source="Prompt mục 11", stances=(), uses=()):
    ADVICE[key] = {"key": key, "type": type_, "topic": topic, "text": text, "severity": severity, "source": source, "reviewed": False}
    if stances: ADVICE[key]["stances"] = list(stances)
    if uses: ADVICE[key]["uses"] = list(uses)

# ——————————————————————————— khuyến nghị (mục 11) ———————————————————————————
adv("adv.phat_huy_lanh_dao", "NEN", "SU_NGHIEP", "Nhận vai trò điều phối, dẫn dắt nhóm khi có cơ hội; xây đội ngũ tin cậy quanh mình.")
adv("adv.tim_nguoi_pho_ta", "NEN", "SU_NGHIEP", "Chủ động tìm cộng sự, người cố vấn bù chỗ còn thiếu thay vì ôm việc một mình.")
adv("adv.lang_nghe_gop_y", "NEN", "TINH_CACH", "Dành thời gian lắng nghe góp ý trước khi quyết việc lớn.")
adv("adv.khong_doc_doan", "KHONG_NEN", "TINH_CACH", "Quyết việc lớn theo cảm tính hoặc gạt bỏ ý kiến người cùng làm.")
adv("adv.tich_luy_truoc_khi_bung", "NEN", "TAI_CHINH", "Tích lũy năng lực và vốn đủ lâu trước khi mở rộng.")
adv("adv.phat_huy_ngon_ngu", "NEN", "SU_NGHIEP", "Phát huy khả năng nói, viết, thuyết phục trong công việc.")
adv("adv.can_than_loi_noi", "LUU_Y", "PHAP_LY", "Cân nhắc lời nói nơi đông người; ghi lại thỏa thuận quan trọng bằng văn bản.", 2)
adv("adv.doc_ky_hop_dong", "NEN", "PHAP_LY", "Đọc kỹ hợp đồng, lưu chứng từ, hỏi người có chuyên môn pháp lý khi cần.", 2)
adv("adv.khong_ky_khi_chua_ro", "KHONG_NEN", "PHAP_LY", "Ký giấy tờ chưa đọc kỹ hoặc tham gia việc mập mờ.", 2)
adv("adv.lap_quy_du_phong", "NEN", "TAI_CHINH", "Lập quỹ dự phòng, chia nhỏ khoản đầu tư.", 2)
adv("adv.khong_dau_co", "KHONG_NEN", "TAI_CHINH", "Cho vay lớn, bảo lãnh hay đầu cơ dùng đòn bẩy.", 2)
adv("adv.chot_loi_phan_tan", "NEN", "TAI_CHINH", "Khi được lợi nhanh, chốt một phần và phân tán rủi ro.", 2)
adv("adv.bao_phat_giu_phan", "LUU_Y", "TAI_CHINH", "Thành quả đến nhanh thì dễ đi nhanh: giữ lại một phần, đừng dồn hết vào một lần.", 2)
adv("adv.ky_luat_tai_chinh", "NEN", "TAI_CHINH", "Giữ kỷ luật chi tiêu, ghi chép dòng tiền đều đặn.")
adv("adv.on_dinh_hon_but_pha", "NEN", "SU_NGHIEP", "Chọn môi trường ổn định, đi đường dài thay vì đổi hướng liên tục.")
adv("adv.hop_hanh_chinh_giao_duc", "NEN", "SU_NGHIEP", "Cân nhắc các hướng hành chính, giáo dục, y tế, nghiên cứu.")
adv("adv.dam_doi_moi_co_ke_hoach", "NEN", "SU_NGHIEP", "Dám đổi mới nhưng có kế hoạch dự phòng cho từng bước.")
adv("adv.tranh_lieu_linh", "KHONG_NEN", "SU_NGHIEP", "Dồn toàn lực vào một canh bạc lớn khi chưa có phương án lùi.", 2)
adv("adv.hoc_tap_thi_cu", "NEN", "SU_NGHIEP", "Đầu tư cho học tập, bằng cấp, chứng chỉ chuyên môn.")
adv("adv.di_xa_mo_rong", "NEN", "DI_CHUYEN", "Tận dụng cơ hội đi xa, công tác, mở thị trường mới.")
adv("adv.can_trong_di_lai", "LUU_Y", "DI_CHUYEN", "Đi lại và vận động nên cẩn trọng hơn thường lệ; tuân thủ luật giao thông.", 2)
adv("adv.kham_dinh_ky", "NEN", "SUC_KHOE", "Khám sức khỏe định kỳ; có dấu hiệu bất thường kéo dài thì gặp bác sĩ.", 2)
adv("adv.khong_bo_qua_dau_hieu", "KHONG_NEN", "SUC_KHOE", "Bỏ qua dấu hiệu bất thường kéo dài hoặc tự ý dùng thuốc.", 2)
adv("adv.dieu_do_sinh_hoat", "NEN", "SUC_KHOE", "Giữ nhịp ngủ nghỉ điều độ, tránh làm việc quá sức kéo dài.")
adv("adv.giu_ket_noi", "NEN", "TINH_CACH", "Giữ kết nối thường xuyên với người thân, bạn bè tin cậy; chia sẻ khi thấy áp lực.", 3)
adv("adv.tim_ho_tro_chuyen_mon", "NEN", "SUC_KHOE", "Khi áp lực tinh thần kéo dài, chủ động tìm chuyên gia tâm lý hoặc bác sĩ để được hỗ trợ.", 3)
adv("adv.giam_tai_cong_viec", "NEN", "SU_NGHIEP", "Giảm bớt việc, ưu tiên những gì thật cần trong giai đoạn này.", 2)
adv("adv.vun_dap_hon_nhan", "NEN", "TINH_DUYEN", "Dành nhiều thời gian lắng nghe, trò chuyện với người bạn đời; cùng thống nhất việc lớn.")
adv("adv.khong_quyet_khi_gian", "KHONG_NEN", "TINH_DUYEN", "Đưa ra quyết định lớn về hôn nhân khi đang giận hoặc căng thẳng.", 2)
adv("adv.cuoi_muon_thuan", "LUU_Y", "TINH_DUYEN", "Tìm hiểu đủ lâu trước khi kết hôn; theo quan niệm Tử Vi, lập gia đình muộn thường thuận hơn.")
adv("adv.chu_dong_gap_go", "NEN", "TINH_DUYEN", "Chủ động gặp gỡ, mở rộng quan hệ; thuận để bàn chuyện cưới hỏi nếu hai bên đã sẵn sàng.")
adv("adv.giu_hoa_khi_gia_dinh", "NEN", "GIA_DAO", "Giữ hòa khí trong nhà; khác ý kiến thì bàn bạc thẳng thắn, tôn trọng nhau.")
adv("adv.cham_soc_thai_ky", "NEN", "GIA_DAO", "Nếu có kế hoạch sinh con: chăm sóc thai kỳ kỹ, khám định kỳ theo hướng dẫn của bác sĩ.", 2)
adv("adv.quan_tam_cha_me", "NEN", "GIA_DAO", "Dành thời gian thăm hỏi, quan tâm sức khỏe cha mẹ.")
adv("adv.ro_rang_tien_bac_nguoi_than", "NEN", "GIA_DAO", "Rõ ràng chuyện tiền bạc, tài sản chung với người thân bằng giấy tờ.")
adv("adv.chon_ban_lam_an", "NEN", "SU_NGHIEP", "Chọn kỹ cộng sự, đối tác; phân định quyền lợi rõ ràng từ đầu.", 2)
adv("adv.khong_tin_nguoi_de_dang", "KHONG_NEN", "SU_NGHIEP", "Giao tiền bạc, quyền quyết định cho người chưa đủ tin cậy.", 2)
adv("adv.giu_tai_san_co_dinh", "NEN", "NHA_DAT", "Ưu tiên giữ tài sản cố định; chuẩn bị kỹ dòng tiền trước khi mua bán nhà đất lớn.")
adv("adv.can_trong_giay_to_nha_dat", "LUU_Y", "NHA_DAT", "Kiểm tra kỹ pháp lý, ranh giới khi giao dịch nhà đất.", 2)
adv("adv.tu_duong_tinh_than", "NEN", "PHUC_DUC", "Dành thời gian cho đời sống tinh thần: thiện nguyện, đọc sách, thiền, gắn bó gia đình.")
adv("adv.tu_luc", "NEN", "TINH_CACH", "Chủ động tự lực, xây nền tảng vững từng bước thay vì trông chờ hoàn cảnh.")
adv("adv.lap_nghiep_xa", "NEN", "DI_CHUYEN", "Cân nhắc lập nghiệp ở môi trường mới, xa nơi quen thuộc.")
adv("adv.giu_tron_chu_tin", "NEN", "TINH_CACH", "Giữ chữ tín và sự minh bạch — đó là vốn quý nhất trong giai đoạn này.")
adv("adv.tan_dung_thoi_co", "NEN", "SU_NGHIEP", "Thời điểm thuận: chủ động đề xuất, nhận trách nhiệm mới, triển khai việc đã chuẩn bị.")
adv("adv.thu_hon_cong", "NEN", "SU_NGHIEP", "Giai đoạn nên thủ hơn công: củng cố nền tảng, hoãn quyết định lớn nếu được.", 2)
adv("adv.khong_mo_rong_nong", "KHONG_NEN", "TAI_CHINH", "Mở rộng nóng, vay mượn lớn để tăng quy mô.", 2)
adv("adv.uu_tien_cong_viec_nam", "NEN", "SU_NGHIEP", "Đặt trọng tâm năm vào lĩnh vực của cung được kích hoạt; lên kế hoạch cụ thể cho nó.")
adv("adv.nghien_cuu_chuyen_sau", "NEN", "SU_NGHIEP", "Phát triển chuyên môn sâu; hợp nghiên cứu, y học, triết học, tâm linh.")
adv("adv.tham_my_nghe_thuat", "NEN", "SU_NGHIEP", "Thử sức ở lĩnh vực cần óc thẩm mỹ: thiết kế, kiến trúc, nghệ thuật.")
adv("adv.ky_cuong_phap_luat", "NEN", "SU_NGHIEP", "Hợp môi trường kỷ cương: luật, thanh tra, an ninh, quản lý chất lượng.")


RULES = []
TEXTS = {}

def rule(id, name, type_, when, topics, polarity, weight, text, advice=(), source="", rel="A", scope="GOC",
         suppress=(), soften=(), invert=(), expert=False, text_source=None, event=None):
    key = ("cc." if type_ in ("CAT_CACH", "HUNG_CACH", "PHAN_VI", "THE_SAO") else
           "cg." if type_ == "CUNG" else "vh." if type_ == "VAN_HAN" else "cd.") + id.lower()
    r = {"id": id, "name": name, "type": type_, "scope": scope, "reliability": rel, "when": when,
         "effect": {"topics": list(topics), "polarity": polarity, "weight": weight},
         "textKey": key, "adviceKeys": list(advice), "source": source, "reviewed": False}
    if suppress: r["effect"]["suppress"] = list(suppress)
    if soften: r["effect"]["soften"] = list(soften)
    if invert: r["effect"]["invert"] = list(invert)
    if event: r["effect"]["event"] = event
    if expert: r["expertOnly"] = True
    RULES.append(r)
    TEXTS[key] = {"key": key, "text": text, "source": text_source or source, "reviewed": False}


# ═══════════════════════════ 10.1 CÁT CÁCH ═══════════════════════════
rule("CC_TU_PHU_DONG_CUNG", "Tử Phủ đồng cung", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("TU_VI"), S("THIEN_PHU"), CHI("DAN", "THAN")]},
     ["SU_NGHIEP", "TAI_CHINH"], 2, 2.5,
     "Mệnh có Tử Vi – Thiên Phủ đồng cung: có xu hướng nắm cả quyền lẫn kho, thuận cho vai trò lãnh đạo, làm chủ. Gặp thêm Tả Hữu hoặc Lộc thì cách mới trọn.",
     ["adv.phat_huy_lanh_dao", "adv.tim_nguoi_pho_ta"], P10_1 + "; " + CN_III, suppress=["COMBO_TU_VI_THIEN_PHU@SELF"])
rule("CC_QUAN_THAN_KHANH_HOI", "Quân thần khánh hội", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("TU_VI"), S("TA_PHU", "tamPhuongTuChinh"), S("HUU_BAT", "tamPhuongTuChinh")]},
     ["SU_NGHIEP"], 2, 2.5,
     "Tử Vi thủ Mệnh có Tả Phù, Hữu Bật hội về: dễ được người phò tá, thuận lên vị trí quản trị. Có thêm Xương Khúc hay Khôi Việt thì càng mạnh.",
     ["adv.phat_huy_lanh_dao", "adv.lang_nghe_gop_y"], P10_1 + "; " + CN_III, suppress=["HC_CO_QUAN", "HC_HON_QUAN"])
rule("CC_CUC_HUONG_LY_MINH", "Cực hướng ly minh", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("TU_VI", chiIn=["NGO"]), none_of(SAT6)]},
     ["TINH_CACH", "SU_NGHIEP"], 2, 2.0,
     "Tử Vi tại Ngọ thủ Mệnh, không bị sát tinh tọa thủ: phong thái uy nghi, danh vọng dễ rõ ràng.",
     ["adv.phat_huy_lanh_dao"], P10_1 + "; " + CN_III)
rule("CC_PHU_TUONG_TRIEU_VIEN", "Phủ Tướng triều viên", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_PHU", "tamPhuongTuChinh"), S("THIEN_TUONG", "tamPhuongTuChinh"),
                                   NONE(S("DIA_KHONG", "tamPhuongTuChinh"), S("DIA_KIEP", "tamPhuongTuChinh"))]},
     ["TAI_CHINH", "SU_NGHIEP"], 1, 2.0,
     "Thiên Phủ và Thiên Tướng cùng hội về Mệnh, không gặp Không Kiếp: thiên về ổn định, biết tích lũy, dễ được tin cậy.",
     ["adv.tich_luy_truoc_khi_bung", "adv.giu_tron_chu_tin"], P10_1)
rule("CC_CO_NGUYET_DONG_LUONG", "Cơ Nguyệt Đồng Lương", "CAT_CACH",
     {"palace": "MENH", "allOf": [S(s, "tamPhuongTuChinh") for s in ["THIEN_CO", "THAI_AM", "THIEN_DONG", "THIEN_LUONG"]]},
     ["SU_NGHIEP"], 1, 2.0,
     "Đủ Thiên Cơ, Thái Âm, Thiên Đồng, Thiên Lương hội về Mệnh: hợp hành chính, giáo dục, y tế, nghiên cứu; con đường ổn định hơn là bứt phá.",
     ["adv.hop_hanh_chinh_giao_duc", "adv.on_dinh_hon_but_pha"], P10_1 + "; " + CN_II)
rule("CC_SAT_PHA_THAM", "Sát Phá Tham", "CAT_CACH",
     {"palace": "MENH", "anyOf": [S("THAT_SAT"), S("PHA_QUAN"), S("THAM_LANG")]},
     ["TINH_CACH", "SU_NGHIEP"], 1, 1.5,
     "Mệnh thuộc bộ Sát Phá Tham: tính động, thích khai phá, đời có nhiều thăng trầm. Gặp sát tinh đắc địa thì hợp võ nghiệp hoặc kinh doanh cạnh tranh.",
     ["adv.dam_doi_moi_co_ke_hoach", "adv.tranh_lieu_linh"], P10_1)
rule("CC_NHAT_NGUYET_TINH_MINH", "Nhật Nguyệt tịnh minh", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", "tamPhuongTuChinh", brightnessIn=SANG), S("THAI_AM", "tamPhuongTuChinh", brightnessIn=SANG)]},
     ["TINH_CACH", "SU_NGHIEP"], 2, 2.0,
     "Thái Dương và Thái Âm đều sáng, cùng hội về Mệnh: thông minh, danh và lợi có xu hướng cân bằng.",
     ["adv.hoc_tap_thi_cu"], P10_1)
rule("CC_MINH_CHAU_XUAT_HAI", "Minh Châu xuất hải", "CAT_CACH",
     {"palace": "MENH", "allOf": [CHI("MUI"), {"isVCD": True}, S("THAI_DUONG", "chart", chiIn=["MAO"]), S("THAI_AM", "chart", chiIn=["HOI"])]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.5,
     "Mệnh tại Mùi vô chính diệu, Thái Dương ở Mão và Thái Âm ở Hợi chiếu về: dễ tỏa sáng nhờ học vấn và danh tiếng.",
     ["adv.hoc_tap_thi_cu"], P10_1, suppress=["TS_VCD_MUON_XUNG", "TS_VCD_NHAT_NGUYET_CHIEU"])
rule("CC_NHAT_XUAT_PHU_TANG", "Nhật xuất Phù Tang", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", chiIn=["MAO"])]},
     ["TINH_CACH", "SU_NGHIEP"], 2, 2.0,
     "Thái Dương thủ Mệnh tại Mão (mặt trời mới mọc): quang minh, hướng ngoại; danh thường lớn hơn lợi.",
     ["adv.phat_huy_lanh_dao"], P10_1, suppress=["STAR_THAI_DUONG_*@SELF"])
rule("CC_NHAT_LE_TRUNG_THIEN", "Nhật lệ trung thiên", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", chiIn=["NGO"])]},
     ["TINH_CACH", "SU_NGHIEP"], 2, 2.0,
     "Thái Dương thủ Mệnh tại Ngọ (mặt trời giữa trưa): khí chất rực rỡ, hướng ngoại, danh lớn; nên giữ sự khiêm nhường.",
     ["adv.phat_huy_lanh_dao", "adv.lang_nghe_gop_y"], P10_1, suppress=["STAR_THAI_DUONG_*@SELF"])
rule("CC_NGUYET_LANG_THIEN_MON", "Nguyệt lãng thiên môn", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAI_AM", chiIn=["HOI"])]},
     ["TINH_CACH", "TAI_CHINH"], 2, 2.0,
     "Thái Âm thủ Mệnh tại Hợi: tinh tế, dễ phú túc; càng thuận với người sinh ban đêm.",
     ["adv.tich_luy_truoc_khi_bung"], P10_1, suppress=["STAR_THAI_AM_*@SELF"])
rule("CC_NHAT_CHIEU_LOI_MON", "Nhật chiếu lôi môn", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", chiIn=["MAO"]), S("THIEN_LUONG")]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.5,
     "Thái Dương cùng Thiên Lương tại Mão thủ Mệnh: danh và tài dễ rực rỡ, được nhiều người biết đến.",
     ["adv.hoc_tap_thi_cu", "adv.phat_huy_ngon_ngu"], P10_1 + "; " + CN_III, suppress=["CC_NHAT_XUAT_PHU_TANG", "COMBO_THAI_DUONG_THIEN_LUONG@SELF"])
rule("CC_THACH_TRUNG_AN_NGOC", "Thạch trung ẩn ngọc", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("CU_MON", chiIn=["TY", "NGO"]),
                                   ANY(S("HOA_LOC", "tamPhuongTuChinh"), S("HOA_QUYEN", "tamPhuongTuChinh"), S("HOA_KHOA", "tamPhuongTuChinh"))],
      "noneOf": [S("KINH_DUONG"), S("HOA_KY")]},
     ["SU_NGHIEP", "TAI_CHINH"], 2, 3.0,
     "Cự Môn tại {cung} có Hóa Lộc, Quyền hoặc Khoa hội về: tài năng như ngọc trong đá, có xu hướng phát muộn mà bền.",
     ["adv.phat_huy_ngon_ngu", "adv.tich_luy_truoc_khi_bung"], P10_1 + "; " + CN_III, suppress=["STAR_CU_MON_*@MENH"])
rule("CC_CU_NHAT_DONG_CUNG_DAN", "Cự Nhật đồng cung (Dần)", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("CU_MON"), S("THAI_DUONG"), CHI("DAN")]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.0,
     "Cự Môn cùng Thái Dương tại Dần thủ Mệnh: dễ có danh nhờ ngôn luận, ngoại giao.",
     ["adv.phat_huy_ngon_ngu"], P10_1, suppress=["COMBO_THAI_DUONG_CU_MON@SELF"])
rule("CC_CU_NHAT_DONG_CUNG_THAN", "Cự Nhật đồng cung (Thân)", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("CU_MON"), S("THAI_DUONG"), CHI("THAN")]},
     ["SU_NGHIEP"], 1, 1.5,
     "Cự Môn cùng Thái Dương tại Thân thủ Mệnh: vẫn có khiếu ngôn luận, ngoại giao nhưng yếu hơn ở Dần, cần bền bỉ hơn.",
     ["adv.phat_huy_ngon_ngu", "adv.can_than_loi_noi"], P10_1, suppress=["COMBO_THAI_DUONG_CU_MON@SELF"])
rule("CC_THAT_SAT_TRIEU_DAU", "Thất Sát triều đẩu / ngưỡng đẩu", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAT_SAT", chiIn=["TY", "NGO", "DAN", "THAN"])],
      "noneOf": [ALL(S("HOA_KY", "tamPhuongTuChinh"), ANY(S("DIA_KHONG", "tamPhuongTuChinh"), S("DIA_KIEP", "tamPhuongTuChinh")))]},
     ["TINH_CACH", "SU_NGHIEP"], 2, 2.0,
     "Thất Sát thủ Mệnh tại {cung}: uy dũng, độc lập, có chí lập nghiệp.",
     ["adv.dam_doi_moi_co_ke_hoach"], P10_1 + "; " + CN_III, suppress=["STAR_THAT_SAT_*@SELF"])
rule("CC_ANH_TINH_NHAP_MIEU", "Anh tinh nhập miếu", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("PHA_QUAN", chiIn=["TY", "NGO"])]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.0,
     "Phá Quân thủ Mệnh tại {cung}: tinh thần tiên phong, dám đổi mới.",
     ["adv.dam_doi_moi_co_ke_hoach"], P10_1, suppress=["STAR_PHA_QUAN_*@SELF"])
rule("CC_ANH_TINH_NHAP_MIEU_HOA", "Anh tinh nhập miếu (Phá Quân hóa Quyền/Lộc)", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("PHA_QUAN", chiIn=["TY", "NGO"]), {"hoaOf": "PHA_QUAN", "hoa": ["QUYEN", "LOC"]}]},
     ["SU_NGHIEP", "TAI_CHINH"], 2, 3.0,
     "Phá Quân miếu địa thủ Mệnh lại được hóa Quyền hoặc hóa Lộc: cách Anh tinh nhập miếu ở dạng đẹp nhất, sức khai phá đi kèm thành quả.",
     ["adv.dam_doi_moi_co_ke_hoach", "adv.phat_huy_lanh_dao"], P10_1, suppress=["CC_ANH_TINH_NHAP_MIEU", "STAR_PHA_QUAN_*@SELF"])
rule("CC_HUNG_TU_TRIEU_VIEN", "Hùng tú triều viên", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH", chiIn=["DAN", "THAN"]), none_of(SAT6)]},
     ["SU_NGHIEP", "TINH_CACH"], 1, 2.0,
     "Liêm Trinh tại {cung} thủ Mệnh, không sát tinh tọa thủ: liêm chính, có uy tín; hợp ngành luật, kỷ cương.",
     ["adv.ky_cuong_phap_luat", "adv.giu_tron_chu_tin"], P10_1, rel="B", suppress=["STAR_LIEM_TRINH_*@SELF"])
rule("CC_THAM_HOA_TUONG_PHUNG", "Tham Hỏa tương phùng", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAM_LANG"), S("HOA_TINH")], "anyOf": [CHI(*TU_MO)]},
     ["TAI_CHINH", "SU_NGHIEP"], 2, 2.5,
     "Tham Lang gặp Hỏa Tinh tại {cung} thủ Mệnh: dễ có những đợt bạo phát. Bạo phát thì dễ bạo tàn — nhớ giữ lại phần thành quả.",
     ["adv.bao_phat_giu_phan", "adv.chot_loi_phan_tan"], P10_1 + "; " + CN_III)
rule("CC_THAM_LINH_TINH_THU", "Tham Linh tịnh thủ", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THAM_LANG"), S("LINH_TINH")], "anyOf": [CHI(*TU_MO)]},
     ["TAI_CHINH", "SU_NGHIEP"], 2, 2.5,
     "Tham Lang gặp Linh Tinh tại {cung} thủ Mệnh: có xu hướng phát nhanh. Bạo phát thì dễ bạo tàn — nhớ giữ lại phần thành quả.",
     ["adv.bao_phat_giu_phan", "adv.chot_loi_phan_tan"], P10_1 + "; " + CN_III)
rule("CC_THAM_HOA_TAI_QUAN", "Tham Hỏa / Tham Linh tại Tài, Quan", "CAT_CACH",
     {"palace": ["TAI_BACH", "QUAN_LOC"], "allOf": [S("THAM_LANG"), ANY(S("HOA_TINH"), S("LINH_TINH")), CHI(*TU_MO)]},
     ["@PALACE"], 1, 1.5,
     "Tham Lang gặp Hỏa hoặc Linh tại cung {cung}: lĩnh vực của cung này dễ có những bước tiến nhanh, cần biết dừng đúng lúc.",
     ["adv.bao_phat_giu_phan"], P10_1 + "; " + CN_III, rel="B")
rule("CC_VU_THAM_DONG_HANH", "Vũ Tham đồng hành", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("VU_KHUC"), S("THAM_LANG"), CHI("SUU", "MUI")]},
     ["TAI_CHINH"], 1, 2.0,
     "Vũ Khúc cùng Tham Lang tại {cung} thủ Mệnh: tiền bần hậu phú, thường khá lên rõ sau khoảng 30 tuổi.",
     ["adv.tich_luy_truoc_khi_bung", "adv.ky_luat_tai_chinh"], P10_1, suppress=["COMBO_VU_KHUC_THAM_LANG@SELF"])
rule("CC_DONG_LUONG_DAN_THAN", "Đồng Lương Dần Thân", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_DONG"), S("THIEN_LUONG"), CHI("DAN", "THAN")]},
     ["PHUC_DUC", "TINH_CACH"], 2, 2.0,
     "Thiên Đồng cùng Thiên Lương tại {cung} thủ Mệnh: phúc hậu, gặp khó thường có người giúp.",
     ["adv.tu_duong_tinh_than"], P10_1 + "; " + CN_III, suppress=["COMBO_THIEN_DONG_THIEN_LUONG@SELF"])
rule("CC_CO_CU_DONG_LAM", "Cơ Cự đồng lâm", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_CO"), S("CU_MON"), CHI("MAO", "DAU")]},
     ["TAI_CHINH", "SU_NGHIEP"], 1, 2.0,
     "Thiên Cơ cùng Cự Môn tại {cung} thủ Mệnh: hoạt ngôn, hợp kinh doanh, buôn bán; nhược điểm là hay đa nghi.",
     ["adv.phat_huy_ngon_ngu", "adv.lang_nghe_gop_y"], P10_1 + "; " + CN_III, suppress=["COMBO_THIEN_CO_CU_MON@SELF"])
rule("CC_CO_CU_HOA_LOC_QUYEN", "Cơ Cự đồng lâm có Hóa Lộc/Quyền", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_CO"), S("CU_MON"), CHI("MAO", "DAU"), ANY(S("HOA_LOC"), S("HOA_QUYEN"))]},
     ["TAI_CHINH"], 2, 2.5,
     "Cơ Cự đồng lâm lại có Hóa Lộc hoặc Hóa Quyền tọa thủ: thuận cho việc làm giàu bằng kinh doanh.",
     ["adv.phat_huy_ngon_ngu", "adv.ky_luat_tai_chinh"], P10_1, suppress=["CC_CO_CU_DONG_LAM", "COMBO_THIEN_CO_CU_MON@SELF"])
rule("CC_VAN_QUE_VAN_HOA", "Văn Quế Văn Hoa", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("VAN_XUONG"), S("VAN_KHUC"), CHI("SUU", "MUI")]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.0,
     "Văn Xương và Văn Khúc cùng tại {cung} thủ Mệnh: thiên về văn chương, học vấn.",
     ["adv.hoc_tap_thi_cu"], P10_1)
rule("CC_LOC_MA_GIAO_TRI", "Lộc Mã giao trì", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_MA", "tamPhuongTuChinh"), ANY(S("LOC_TON", "tamPhuongTuChinh"), S("HOA_LOC", "tamPhuongTuChinh"))]},
     ["TAI_CHINH", "DI_CHUYEN"], 2, 2.0,
     "Lộc và Thiên Mã hội về Mệnh, Tài, Quan, Di: tiền dễ đến từ di chuyển, giao thương.",
     ["adv.di_xa_mo_rong"], P10_1)
rule("CC_SONG_LOC_TRIEU_VIEN", "Song Lộc triều viên", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("LOC_TON", "tamPhuongTuChinh"), S("HOA_LOC", "tamPhuongTuChinh")]},
     ["TAI_CHINH"], 2, 2.5,
     "Lộc Tồn và Hóa Lộc cùng hội về Mệnh: tài lộc có xu hướng dồi dào.",
     ["adv.ky_luat_tai_chinh", "adv.lap_quy_du_phong"], P10_1)
rule("CC_TAM_HOA_LIEN_CHAU", "Tam Hóa liên châu", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("HOA_LOC", "tamPhuongTuChinh"), S("HOA_QUYEN", "tamPhuongTuChinh"), S("HOA_KHOA", "tamPhuongTuChinh")]},
     ["SU_NGHIEP", "TAI_CHINH"], 2, 3.0,
     "Hóa Lộc, Hóa Quyền, Hóa Khoa đều hội về Mệnh: danh, quyền, lộc có đủ điều kiện cùng phát triển.",
     ["adv.tan_dung_thoi_co", "adv.giu_tron_chu_tin"], P10_1)
rule("CC_TAI_AM_GIAP_AN", "Tài Ấm giáp Ấn", "CAT_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_TUONG"), ANY(AT("star:CU_MON", hasStar="HOA_LOC"), AT("star:CU_MON", hasStar="LOC_TON"))]},
     ["TAI_CHINH", "SU_NGHIEP"], 2, 2.5,
     "Thiên Tướng thủ Mệnh được kẹp bởi Thiên Lương và Cự Môn có Lộc: hưởng trọn cả quyền lẫn tiền.",
     ["adv.phat_huy_lanh_dao", "adv.giu_tron_chu_tin"], P10_1 + "; " + CN_III, suppress=["TS_THIEN_TUONG_XET_GIAP"])
rule("CC_GIAP_CAT", "Giáp cát", "CAT_CACH",
     {"palace": "MENH", "anyOf": [{"pairGiap": ["TA_PHU", "HUU_BAT"]}, {"pairGiap": ["VAN_XUONG", "VAN_KHUC"]},
                                   {"pairGiap": ["THIEN_KHOI", "THIEN_VIET"]}, {"pairGiap": ["THAI_DUONG", "THAI_AM"]},
                                   {"pairGiap": ["HOA_QUYEN", "HOA_LOC"]}]},
     ["SU_NGHIEP"], 1, 1.5,
     "Hai cung kề Mệnh có cặp sao tốt kẹp hai bên: dễ được môi trường xung quanh nâng đỡ.",
     ["adv.tim_nguoi_pho_ta"], P10_1)
rule("CC_TOA_QUY_HUONG_QUY", "Tọa quý hướng quý", "CAT_CACH",
     {"palace": "MENH", "anyOf": [ALL(S("THIEN_KHOI"), S("THIEN_VIET", "tamPhuong")), ALL(S("THIEN_VIET"), S("THIEN_KHOI", "tamPhuong"))]},
     ["SU_NGHIEP"], 1, 1.5,
     "Thiên Khôi, Thiên Việt một sao ở Mệnh, một sao chiếu về: dễ gặp quý nhân, thuận thi cử.",
     ["adv.hoc_tap_thi_cu"], P10_1)

# ═══════════════════════════ 10.2 HUNG CÁCH ═══════════════════════════
LXDV = ["LINH_TINH", "VAN_XUONG", "DA_LA", "VU_KHUC"]
LXDV_TEXT = ("Linh Tinh, Văn Xương, Đà La, Vũ Khúc cùng hội ở {cung}: giai đoạn dễ chịu áp lực tinh thần rất lớn. Nên giảm việc, "
             "giữ kết nối với người thân và chủ động tìm hỗ trợ chuyên môn khi thấy quá tải.")
rule("HC_LINH_XUONG_DA_VU", "Linh Xương Đà Vũ (gốc)", "HUNG_CACH",
     {"palace": "MENH", "allOf": [CNT(LXDV, 4, "tamPhuongTuChinh")]},
     ["TINH_CACH", "SUC_KHOE"], -2, 2.5, LXDV_TEXT,
     ["adv.giu_ket_noi", "adv.tim_ho_tro_chuyen_mon", "adv.giam_tai_cong_viec"], P10_2)
rule("HC_LINH_XUONG_DA_VU_DAI_HAN", "Linh Xương Đà Vũ (đại hạn)", "HUNG_CACH",
     {"layer": "daiHan", "allOf": [CNT(LXDV, 4, "tamPhuongTuChinh")]},
     ["TINH_CACH", "SUC_KHOE"], -2, 2.5, LXDV_TEXT,
     ["adv.giu_ket_noi", "adv.tim_ho_tro_chuyen_mon", "adv.giam_tai_cong_viec"], P10_2, scope="DAI_HAN")
rule("HC_LINH_XUONG_DA_VU_TIEU_HAN", "Linh Xương Đà Vũ (tiểu hạn)", "HUNG_CACH",
     {"layer": "tieuHan", "allOf": [CNT(LXDV, 4, "tamPhuongTuChinh")]},
     ["TINH_CACH", "SUC_KHOE"], -2, 2.5, LXDV_TEXT,
     ["adv.giu_ket_noi", "adv.tim_ho_tro_chuyen_mon", "adv.giam_tai_cong_viec"], P10_2, scope="NAM")
rule("HC_LO_THUONG_MAI_THI", "Lộ thượng mai thi", "HUNG_CACH",
     {"palace": ["MENH", "THIEN_DI", "TAT_ACH"], "allOf": [S("LIEM_TRINH"), S("THAT_SAT"), CHI("SUU", "MUI")]},
     ["DI_CHUYEN", "SUC_KHOE"], -1, 2.0,
     "Liêm Trinh cùng Thất Sát tại {cung}: nên cẩn trọng khi tham gia giao thông và đi xa, nhất là năm hạn có sát tinh lưu.",
     ["adv.can_trong_di_lai"], P10_2 + "; " + CN_III)
rule("HC_HINH_KY_GIAP_AN", "Hình Kỵ giáp Ấn", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_TUONG"), AT("star:CU_MON", hasStar="HOA_KY"), AT("star:THIEN_LUONG", hasStar="KINH_DUONG")]},
     ["PHAP_LY", "SU_NGHIEP"], -2, 2.5,
     "Thiên Tướng thủ Mệnh bị kẹp giữa Cự Môn hóa Kỵ và Thiên Lương gặp Kình Dương: dễ vướng quy định, giấy tờ, trách nhiệm pháp lý; thận trọng khi ký kết.",
     ["adv.doc_ky_hop_dong", "adv.khong_ky_khi_chua_ro"], P10_2 + "; " + CN_III, suppress=["TS_THIEN_TUONG_XET_GIAP"])
rule("HC_CU_KINH", "Cự Kình", "HUNG_CACH",
     {"palace": ["MENH", "QUAN_LOC"], "allOf": [S("CU_MON"), S("KINH_DUONG")]},
     ["PHAP_LY", "SU_NGHIEP"], -1, 2.0,
     "Cự Môn gặp Kình Dương tại cung {cung}: hay va chạm lời nói, việc dễ bế tắc nếu nóng vội.",
     ["adv.can_than_loi_noi", "adv.lang_nghe_gop_y"], P10_2 + "; " + CN_III)
rule("HC_PHIEM_THUY_DAO_HOA", "Phiếm thủy đào hoa", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("THAM_LANG", chiIn=["TY", "HOI"]), ANY(S("KINH_DUONG"), S("DA_LA"), S("HOA_KY"))]},
     ["TINH_DUYEN", "TAI_CHINH"], -1, 2.0,
     "Tham Lang tại {cung} gặp Kình, Đà hoặc Hóa Kỵ: ham vui, dễ rối chuyện tình cảm và tài chính cá nhân.",
     ["adv.ky_luat_tai_chinh", "adv.vun_dap_hon_nhan"], P10_2 + "; " + CN_III)
rule("HC_LIEM_THAM_TI_HOI", "Liêm Tham Tỵ Hợi", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH"), S("THAM_LANG"), CHI("TI", "HOI")]},
     ["PHAP_LY", "TINH_DUYEN"], -1, 2.0,
     "Liêm Trinh cùng Tham Lang tại {cung} thủ Mệnh: dễ vướng thị phi vì tiền hoặc tình; nên tránh xa vùng xám.",
     ["adv.khong_ky_khi_chua_ro", "adv.giu_tron_chu_tin"], P10_2 + "; " + CN_III, suppress=["COMBO_LIEM_TRINH_THAM_LANG@SELF"])
rule("HC_LIEM_THAM_TI_HOI_KY", "Liêm Tham Tỵ Hợi gặp Hóa Kỵ", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH"), S("THAM_LANG"), CHI("TI", "HOI"), S("HOA_KY")]},
     ["PHAP_LY", "TINH_DUYEN"], -2, 2.5,
     "Liêm Tham tại {cung} lại gặp Hóa Kỵ: nguy cơ vướng pháp lý vì tiền hoặc tình cao hơn; tuyệt đối minh bạch giấy tờ, tránh việc mập mờ.",
     ["adv.doc_ky_hop_dong", "adv.khong_ky_khi_chua_ro"], P10_2, suppress=["HC_LIEM_THAM_TI_HOI", "COMBO_LIEM_TRINH_THAM_LANG@SELF"])
rule("HC_VU_PHA_TI_HOI", "Vũ Phá Tỵ Hợi", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("VU_KHUC"), S("PHA_QUAN"), CHI("TI", "HOI")]},
     ["TAI_CHINH"], -1, 2.0,
     "Vũ Khúc cùng Phá Quân tại {cung} thủ Mệnh: tiền bạc tụ tán mạnh, dễ liều.",
     ["adv.lap_quy_du_phong", "adv.khong_dau_co"], P10_2, suppress=["COMBO_VU_KHUC_PHA_QUAN@SELF"])
rule("HC_KHONG_KIEP_GIAP_MENH", "Không Kiếp giáp Mệnh", "HUNG_CACH",
     {"palace": "MENH", "allOf": [{"pairGiap": ["DIA_KHONG", "DIA_KIEP"]}]},
     ["TAI_CHINH", "SU_NGHIEP"], -1, 2.0,
     "Địa Không và Địa Kiếp kẹp hai bên Mệnh: môi trường hay gây hao hụt, kế hoạch dễ đứt gãy; nên có phương án dự phòng.",
     ["adv.lap_quy_du_phong", "adv.dam_doi_moi_co_ke_hoach"], P10_2)
rule("HC_TU_SAT_HOI_MENH", "Tứ sát hội Mệnh", "HUNG_CACH",
     {"palace": "MENH", "allOf": [CNT(TU_SAT, 3, "tamPhuongTuChinh")],
      "noneOf": [S("THIEN_GIAI", "tamPhuongTuChinh"), S("DIA_GIAI", "tamPhuongTuChinh"), S("GIAI_THAN", "tamPhuongTuChinh")]},
     ["TINH_CACH", "SU_NGHIEP"], -1, 2.0,
     "Từ ba sao trở lên trong Kình, Đà, Hỏa, Linh hội về Mệnh mà không có sao giải: đường đi nhiều trở ngại, cần sự bền bỉ.",
     ["adv.tu_luc", "adv.dieu_do_sinh_hoat"], P10_2)
rule("HC_THIEN_CO_KHONG_KIEP", "Thiên Cơ ngộ Không Kiếp", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("THIEN_CO"), ANY(S("DIA_KHONG"), S("DIA_KIEP"))]},
     ["SU_NGHIEP", "TAI_CHINH"], -1, 1.5,
     "Thiên Cơ gặp Địa Không hoặc Địa Kiếp tọa thủ: dễ mưu tính quá đà, hỏng việc vì tính toán quá nhiều.",
     ["adv.lang_nghe_gop_y", "adv.tranh_lieu_linh"], P10_2 + "; " + CN_III, rel="B")
rule("HC_LOC_PHUNG_LUONG_SAT", "Lộc phùng lưỡng sát", "HUNG_CACH",
     {"palace": "MENH", "allOf": [ANY(S("LOC_TON", "tamPhuongTuChinh"), S("HOA_LOC", "tamPhuongTuChinh")),
                                   S("DIA_KHONG", "tamPhuongTuChinh"), S("DIA_KIEP", "tamPhuongTuChinh")]},
     ["TAI_CHINH"], -1, 2.0,
     "Lộc gặp cả Địa Không lẫn Địa Kiếp hội về Mệnh: tiền đến rồi đi, khó giữ.",
     ["adv.lap_quy_du_phong", "adv.ky_luat_tai_chinh"], P10_2)
rule("HC_MA_LAC_KHONG_VONG", "Mã lạc không vong", "HUNG_CACH",
     {"palace": ["MENH", "THIEN_DI"], "allOf": [S("THIEN_MA"), ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["DI_CHUYEN"], -1, 1.5,
     "Thiên Mã tại cung {cung} gặp Tuần hoặc Triệt: di chuyển, đổi việc hay trắc trở, chậm hơn dự tính.",
     ["adv.dam_doi_moi_co_ke_hoach"], P10_2, rel="B")
rule("HC_CHIET_TUC_MA", "Chiết túc mã", "HUNG_CACH",
     {"palace": ["MENH", "THIEN_DI"], "allOf": [S("THIEN_MA"), S("DA_LA")]},
     ["DI_CHUYEN"], -1, 1.5,
     "Thiên Mã đồng cung Đà La tại cung {cung}: việc đi lại, thay đổi dễ bị trì trệ; nên chuẩn bị kỹ trước khi đi.",
     ["adv.can_trong_di_lai"], P10_2, rel="B")
rule("HC_NHAT_NGUYET_PHAN_BOI", "Nhật Nguyệt phản bối", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", "tamPhuongTuChinh", brightnessIn=["H"]), S("THAI_AM", "tamPhuongTuChinh", brightnessIn=["H"])]},
     ["SU_NGHIEP", "TINH_CACH"], -1, 2.0,
     "Thái Dương và Thái Âm cùng hãm, hội về Mệnh: dễ vất vả, danh chưa tương xứng công sức — trừ khi có điều kiện phản vi.",
     ["adv.tu_luc", "adv.hoc_tap_thi_cu"], P10_2)
rule("HC_KHO_THUNG", "Kho thủng", "HUNG_CACH",
     {"palace": ["MENH", "TAI_BACH", "DIEN_TRACH"], "allOf": [S("THIEN_PHU"), ANY(S("DIA_KHONG"), S("DIA_KIEP"))]},
     ["TAI_CHINH", "NHA_DAT"], -1, 2.0,
     "Thiên Phủ gặp Địa Không/Địa Kiếp tại cung {cung} (kho thủng): tích lũy khó bền; cần kỷ luật tài chính, tránh đầu cơ.",
     ["adv.ky_luat_tai_chinh", "adv.khong_dau_co"], P10_2 + "; " + CN_III, rel="B")
rule("HC_KHO_LO", "Kho lộ", "HUNG_CACH",
     {"palace": ["MENH", "TAI_BACH", "DIEN_TRACH"], "allOf": [S("THIEN_PHU"), ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["TAI_CHINH", "NHA_DAT"], -1, 1.5,
     "Thiên Phủ gặp Tuần hoặc Triệt tại cung {cung} (kho lộ): khó giữ của bền; nên giữ kỷ luật chi tiêu.",
     ["adv.ky_luat_tai_chinh"], P10_2 + "; " + CN_III, rel="B")
rule("HC_CO_QUAN", "Cô quân", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("TU_VI")], "noneOf": [S("TA_PHU", "tamPhuongTuChinh"), S("HUU_BAT", "tamPhuongTuChinh")]},
     ["TINH_CACH"], -1, 1.5,
     "Tử Vi thủ Mệnh mà không có Tả Hữu hội về (cô quân): dễ thiếu người phò tá, đôi khi cô đơn, cố chấp.",
     ["adv.tim_nguoi_pho_ta", "adv.lang_nghe_gop_y"], P10_2 + "; " + CN_III, rel="B")
rule("HC_HON_QUAN", "Hôn quân", "HUNG_CACH",
     {"palace": "MENH", "allOf": [S("TU_VI"), CNT(SAT6, 2, "tamPhuongTuChinh")],
      "noneOf": [S("TA_PHU", "tamPhuongTuChinh"), S("HUU_BAT", "tamPhuongTuChinh")]},
     ["TINH_CACH"], -2, 2.0,
     "Tử Vi không Tả Hữu lại gặp từ hai sát tinh: dễ bị người xấu lợi dụng, tính ngang bướng; rất cần người góp ý thẳng thắn.",
     ["adv.lang_nghe_gop_y", "adv.khong_doc_doan"], P10_2 + "; " + CN_III, rel="B", suppress=["HC_CO_QUAN"])
rule("HC_SONG_KY", "Song Kỵ", "HUNG_CACH",
     {"palace": "ANY", "allOf": [S("HOA_KY"), {"luuHoa": "KY", "in": "self"}]},
     ["@PALACE"], -1, 2.0,
     "Hóa Kỵ gốc và Hóa Kỵ lưu cùng ở cung {cung} trong năm {nam}: lĩnh vực của cung này dễ có vướng mắc kéo dài; giải quyết từng việc nhỏ, đừng để dồn.",
     ["adv.doc_ky_hop_dong"], P10_2 + "; Prompt mục 7", rel="B", scope="NAM")
rule("HC_CUC_KHAC_MENH", "Cục khắc Mệnh", "HUNG_CACH",
     {"palace": "MENH", "allOf": [{"cucMenh": ["CUC_KHAC_MENH"]}]},
     ["TINH_CACH"], -1, 1.0,
     "Hành của Cục khắc hành bản mệnh: hoàn cảnh ít nâng đỡ, thành quả chủ yếu do tự lực.",
     ["adv.tu_luc"], P10_2 + "; Cẩm nang V2 – P.I.4", rel="B")

# ═══════════════════════════ 10.3 PHẢN VI KỲ CÁCH ═══════════════════════════
rule("PV_CU_MON_THIN_TUAT_TAN", "Cự Môn Thìn Tuất gặp tuổi Tân", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("CU_MON", chiIn=["THIN", "TUAT"]), {"yearCanIn": ["TAN"]}]},
     ["SU_NGHIEP", "TAI_CHINH"], 2, 2.5,
     "Cự Môn hãm tại {cung} nhưng tuổi Tân được Cự Môn hóa Lộc: “hãm địa mà hóa cát”, khẩu tài trở thành lợi thế.",
     ["adv.phat_huy_ngon_ngu"], P10_3, invert=["STAR_CU_MON_H@MENH"])
rule("PV_THIEN_DONG_TUAT_DINH", "Thiên Đồng Tuất gặp tuổi Đinh", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("THIEN_DONG", chiIn=["TUAT"]), {"yearCanIn": ["DINH"]}]},
     ["SU_NGHIEP", "TINH_CACH"], 2, 2.5,
     "Thiên Đồng tại Tuất thủ Mệnh, tuổi Đinh được Thiên Đồng hóa Quyền: phản bối hóa cát, có thể vươn rất cao.",
     ["adv.tan_dung_thoi_co"], P10_3, invert=["STAR_THIEN_DONG_H@MENH"])
rule("PV_MA_DAU_DOI_KIEM", "Mã đầu đới kiếm", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("KINH_DUONG", chiIn=["NGO"]), ANY(S("THIEN_DONG"), S("THAM_LANG"))]},
     ["SU_NGHIEP"], 1, 2.0,
     "Kình Dương tại Ngọ thủ Mệnh có Thiên Đồng hoặc Tham Lang: uy trấn một phương, hợp võ nghiệp và quản lý cứng rắn.",
     ["adv.ky_cuong_phap_luat"], P10_3, invert=["PHU_KINH_DUONG@MENH"])
rule("PV_NHAT_HAM_TUAN_TRIET", "Nhật hãm gặp Tuần Triệt", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("THAI_DUONG", brightnessIn=["H"]), ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["TINH_CACH"], 1, 1.5,
     "Thái Dương hãm thủ Mệnh lại gặp Tuần hoặc Triệt: theo Nam phái là “phản vi minh”, tính hãm giảm mạnh.",
     [], P10_3, rel="B", invert=["STAR_THAI_DUONG_H@MENH"])
rule("PV_NGUYET_HAM_TUAN_TRIET", "Nguyệt hãm gặp Tuần Triệt", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("THAI_AM", brightnessIn=["H"]), ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["TINH_CACH"], 1, 1.5,
     "Thái Âm hãm thủ Mệnh lại gặp Tuần hoặc Triệt: theo Nam phái là “phản vi minh”, tính hãm giảm mạnh.",
     [], P10_3, rel="B", invert=["STAR_THAI_AM_H@MENH"])
rule("PV_NHAT_NGUYET_SANG_TUAN_TRIET", "Nhật Nguyệt sáng gặp Tuần Triệt", "PHAN_VI",
     {"palace": "MENH", "allOf": [ANY(S("THAI_DUONG", brightnessIn=MIEU_VUONG), S("THAI_AM", brightnessIn=MIEU_VUONG)),
                                   ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["TINH_CACH"], -1, 1.0,
     "Nhật hoặc Nguyệt đang sáng thủ Mệnh lại bị Tuần/Triệt án ngữ: độ sáng bị giảm, cần nỗ lực hơn để tỏa sáng.",
     ["adv.tu_luc"], P10_3, rel="B", soften=["STAR_THAI_DUONG_*@MENH", "STAR_THAI_AM_*@MENH"])
rule("PV_SAO_HAM_HOA_LOC_QUYEN", "Sao hãm được hóa Lộc/Quyền", "PHAN_VI",
     {"palace": "MENH", "anyOf": [ALL(S(s, brightnessIn=["H"]), {"hoaOf": s, "hoa": ["LOC", "QUYEN"]}) for s in CHINH]},
     ["TINH_CACH", "SU_NGHIEP"], 1, 1.5,
     "Chính tinh hãm thủ Mệnh nhưng chính nó được hóa Lộc hoặc Quyền: giảm hung một bậc — vất vả nhưng có lối ra.",
     ["adv.tu_luc"], P10_3, rel="B", soften=["STAR_*_H@MENH"])
rule("PV_SAT_TINH_DAC_DIA_SPT", "Sát tinh đắc địa phò Sát Phá Tham", "PHAN_VI",
     {"palace": "MENH", "allOf": [ANY(S("THAT_SAT", brightnessIn=MIEU_VUONG), S("PHA_QUAN", brightnessIn=MIEU_VUONG), S("THAM_LANG", brightnessIn=MIEU_VUONG)),
                                   ANY(S("KINH_DUONG", chiIn=TU_MO), S("DA_LA", chiIn=TU_MO),
                                       S("HOA_TINH", chiIn=["DAN", "MAO", "THIN", "TI", "NGO"]), S("LINH_TINH", chiIn=["DAN", "MAO", "THIN", "TI", "NGO"]))]},
     ["SU_NGHIEP"], 2, 2.0,
     "Sát tinh đắc địa đi cùng Sát Phá Tham miếu vượng ở Mệnh: sát hóa thành quyền — hợp võ nghiệp, phẫu thuật, an ninh, kinh doanh cạnh tranh.",
     ["adv.ky_cuong_phap_luat", "adv.dam_doi_moi_co_ke_hoach"], P10_3, rel="B",
     invert=["PHU_KINH_DUONG@MENH", "PHU_DA_LA@MENH", "PHU_HOA_TINH@MENH", "PHU_LINH_TINH@MENH"])
rule("PV_KHONG_KIEP_DAC_DIA", "Không Kiếp đắc địa", "PHAN_VI",
     {"palace": "MENH", "allOf": [ANY(S("DIA_KHONG", chiIn=TU_SINH), S("DIA_KIEP", chiIn=TU_SINH)),
                                   ANY(*[S(s, brightnessIn=MIEU_VUONG) for s in CHINH])]},
     ["TAI_CHINH", "SU_NGHIEP"], 1, 1.5,
     "Không Kiếp đắc địa ở Mệnh có chính tinh miếu vượng: có thể “phát như sấm” nhưng cũng dễ tàn nhanh — luôn chốt lời, phân tán rủi ro.",
     ["adv.chot_loi_phan_tan", "adv.bao_phat_giu_phan"], P10_3, rel="B", soften=["PHU_DIA_KHONG@MENH", "PHU_DIA_KIEP@MENH"])
rule("PV_HOA_KY_TU_MO", "Hóa Kỵ ở Tứ Mộ", "PHAN_VI",
     {"palace": "ANY", "allOf": [S("HOA_KY", chiIn=TU_MO)]},
     ["@PALACE"], 1, 1.0,
     "Hóa Kỵ tại {cung} thuộc Tứ Mộ: giảm hung, chuyển thành sự cẩn trọng, đi sâu vào chuyên môn.",
     ["adv.nghien_cuu_chuyen_sau"], P10_3, rel="B", soften=["PHU_HOA_KY@SELF"])
rule("PV_THIEN_HINH_DAC_DIA", "Thiên Hình đắc địa", "PHAN_VI",
     {"palace": "MENH", "allOf": [S("THIEN_HINH", chiIn=["DAN", "MAO", "THAN", "DAU"]),
                                   any_self(["THIEN_LUONG", "THAT_SAT", "PHA_QUAN", "VU_KHUC", "LIEM_TRINH", "THAM_LANG"])]},
     ["SU_NGHIEP", "PHAP_LY"], 1, 1.5,
     "Thiên Hình đắc địa thủ Mệnh cùng chính tinh võ hoặc Thiên Lương: quyền uy về pháp luật, y khoa, kỷ luật.",
     ["adv.ky_cuong_phap_luat"], P10_3, rel="B", invert=["PHU_THIEN_HINH@MENH"])
rule("PV_TUAN_TRIET_GIAI_HUNG", "Tuần Triệt giải hung", "PHAN_VI",
     {"palace": "ANY", "allOf": [ANY({"hasTuan": True}, {"hasTriet": True}),
                                 ANY(CNT(SAT6, 1), *[S(s, brightnessIn=["H"]) for s in CHINH])]},
     ["@PALACE"], 0, 1.0,
     "Tuần/Triệt án ngữ cung {cung} có sát tinh hoặc chính tinh hãm: phần xấu ở đó được giảm bớt.",
     [], P10_3, rel="B", soften=["STAR_*_H@SELF", "PHU_KINH_DUONG@SELF", "PHU_DA_LA@SELF", "PHU_HOA_TINH@SELF",
                                  "PHU_LINH_TINH@SELF", "PHU_DIA_KHONG@SELF", "PHU_DIA_KIEP@SELF"])

# ═══════════════════════════ 10.4 THẾ SAO KHÓ ═══════════════════════════
rule("TS_VCD_MUON_XUNG", "Mệnh vô chính diệu", "THE_SAO",
     {"palace": "MENH", "allOf": [{"isVCD": True}]},
     ["TINH_CACH"], 0, 1.0,
     "Mệnh vô chính diệu: mượn chính tinh cung xung chiếu (Thiên Di) để luận, lực giảm còn khoảng bảy phần; tính cách dễ chịu ảnh hưởng của môi trường.",
     ["adv.tu_luc"], P10_4)
rule("TS_VCD_TAM_KHONG", "Vô chính diệu đắc tam không", "THE_SAO",
     {"palace": "MENH", "allOf": [{"isVCD": True}, CNT(["@TUAN", "@TRIET", "THIEN_KHONG", "DIA_KHONG"], 3, "tamPhuongTuChinh")]},
     ["TINH_CACH", "TAI_CHINH"], 2, 2.0,
     "Mệnh vô chính diệu gặp đủ “tam không”: theo Nam phái là cách có thể phú quý bất ngờ.",
     ["adv.chot_loi_phan_tan"], P10_4, rel="B")
rule("TS_VCD_NHAT_NGUYET_CHIEU", "Vô chính diệu có Nhật Nguyệt sáng chiếu", "THE_SAO",
     {"palace": "MENH", "allOf": [{"isVCD": True}, ANY(S("THAI_DUONG", "tamPhuong", brightnessIn=SANG), S("THAI_AM", "tamPhuong", brightnessIn=SANG))]},
     ["TINH_CACH"], 1, 1.5,
     "Mệnh vô chính diệu được Nhật hoặc Nguyệt sáng chiếu về: điểm tựa tốt, giảm bớt sự chông chênh.",
     [], P10_4)
rule("TS_VCD_SAT_TOA", "Vô chính diệu có sát tinh tọa thủ", "THE_SAO",
     {"palace": "MENH", "allOf": [{"isVCD": True}, CNT(SAT6, 1)]},
     ["TINH_CACH", "DI_CHUYEN"], -1, 1.5,
     "Mệnh vô chính diệu có sát tinh tọa thủ: gốc yếu, dễ bị ngoại cảnh chi phối; đi xa lập nghiệp thường thuận hơn.",
     ["adv.lap_nghiep_xa", "adv.tu_luc"], P10_4)
rule("TS_TUAN_TRIET_DONG_CUNG", "Tuần Triệt cùng một cung", "THE_SAO",
     {"palace": "ANY", "allOf": [{"hasTuan": True}, {"hasTriet": True}]},
     ["@PALACE"], 0, 1.5,
     "Tuần và Triệt cùng án ngữ cung {cung}: lực chặn rất mạnh — điều tốt của cung dễ bế tắc, điều xấu lại được giải mạnh.",
     [], P10_4)
rule("TS_LOC_TON_THU_MENH", "Lộc Tồn thủ Mệnh", "THE_SAO",
     {"palace": "MENH", "allOf": [S("LOC_TON")]},
     ["TAI_CHINH", "TINH_CACH"], 0, 1.0,
     "Lộc Tồn thủ Mệnh (luôn bị Kình Đà kẹp hai bên): biết giữ của nhưng dễ dè dặt, đôi khi cô độc.",
     ["adv.tim_nguoi_pho_ta"], P10_4)
rule("TS_LOC_TON_BI_PHA", "Lộc Tồn thủ Mệnh bị phá", "THE_SAO",
     {"palace": "MENH", "allOf": [S("LOC_TON"), ANY(S("HOA_KY"), S("DIA_KHONG"), S("DIA_KIEP"))]},
     ["TAI_CHINH"], -1, 1.5,
     "Lộc Tồn thủ Mệnh gặp Hóa Kỵ hoặc Không Kiếp đồng cung: lộc dễ bị phá, nên thận trọng với tiền bạc.",
     ["adv.lap_quy_du_phong"], P10_4, suppress=["TS_LOC_TON_THU_MENH"])
rule("TS_LOC_KY_DONG_CUNG", "Hóa Lộc + Hóa Kỵ cùng cung", "THE_SAO",
     {"palace": "ANY", "allOf": [S("HOA_LOC"), S("HOA_KY")]},
     ["@PALACE"], 0, 1.5,
     "Hóa Lộc và Hóa Kỵ cùng ở cung {cung}: được rồi vướng — cái lợi thường đi kèm rắc rối.",
     ["adv.doc_ky_hop_dong"], P10_4)
rule("TS_THIEN_TUONG_XET_GIAP", "Thiên Tướng thủ Mệnh — xét giáp cung", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THIEN_TUONG")]},
     ["TINH_CACH"], 0, 0.5,
     "Thiên Tướng thủ Mệnh: tốt xấu phụ thuộc nhiều vào hai cung kẹp bên (Thiên Lương và Cự Môn); trọng danh dự hơn tiền bạc.",
     ["adv.giu_tron_chu_tin"], P10_4 + "; " + CN_III)
rule("TS_AM_DUONG_THUAN_LY", "Âm Dương thuận lý", "THE_SAO",
     {"palace": "MENH", "allOf": [{"thuanLy": True}]},
     ["TINH_CACH"], 1, 1.0,
     "Âm Dương thuận lý (tuổi và cung Mệnh cùng tính Âm/Dương): nền tảng hài hòa, làm việc thuận chiều.",
     [], P10_4)
rule("TS_AM_DUONG_NGHICH_LY", "Âm Dương nghịch lý", "THE_SAO",
     {"palace": "MENH", "allOf": [{"thuanLy": False}]},
     ["TINH_CACH"], -1, 0.5,
     "Âm Dương nghịch lý: hay phải đi đường vòng hơn người khác một chút; bù lại bằng sự kiên trì.",
     ["adv.tu_luc"], P10_4)
rule("TS_THAN_MENH_DONG_CUNG", "Thân Mệnh đồng cung", "THE_SAO",
     {"palace": "MENH", "allOf": [{"isThan": True}]},
     ["TINH_CACH"], 0, 1.0,
     "Thân Mệnh đồng cung (sinh giờ Tý hoặc Ngọ): tính cách nhất quán, tự làm tự chịu, cung Mệnh có trọng lượng cả đời.",
     [], P10_4 + "; " + CN_I3)
THAN_CU = [
    ("THIEN_DI", "Thân cư Thiên Di", "hướng ngoại, hay di chuyển; thành bại gắn liền với hoạt động xã hội", "DI_CHUYEN"),
    ("QUAN_LOC", "Thân cư Quan Lộc", "nặng lòng vì sự nghiệp, lấy công việc làm lẽ sống", "SU_NGHIEP"),
    ("TAI_BACH", "Thân cư Tài Bạch", "thực tế, coi trọng giá trị đồng tiền; chuyện mưu sinh chi phối nhiều quyết định", "TAI_CHINH"),
    ("PHU_THE", "Thân cư Phu Thê", "chịu ảnh hưởng sâu từ người bạn đời; buồn vui gắn liền với gia đạo", "TINH_DUYEN"),
    ("PHUC_DUC", "Thân cư Phúc Đức", "hưởng phúc ấm tổ tiên hoặc gánh vác việc dòng họ", "PHUC_DUC"),
]
for role, name, meaning, topic in THAN_CU:
    rule("TS_THAN_CU_" + role, name, "THE_SAO",
         {"palace": "THAN", "allOf": [{"role": [role]}]},
         [topic, "TINH_CACH"], 0, 1.0,
         f"{name}: {meaning}. Từ khoảng 30 tuổi, cung Thân dần có trọng lượng lớn hơn cung Mệnh.",
         [], P10_4 + "; " + CN_I3)

# ═══════════════════════════ DẤU HIỆU THEO CUNG (Cẩm nang P.II) ═══════════════════════════
rule("CG_PHU_MAU_AM_DUONG_SANG", "Phụ Mẫu: Nhật Nguyệt sáng", "CUNG",
     {"palace": "PHU_MAU", "allOf": [S("THAI_DUONG", "chart", brightnessIn=SANG), S("THAI_AM", "chart", brightnessIn=SANG),
                                     ANY(S("THAI_DUONG", "tamPhuongTuChinh"), S("THAI_AM", "tamPhuongTuChinh"))]},
     ["GIA_DAO"], 1, 1.0,
     "Thái Dương (cha) và Thái Âm (mẹ) đều sáng, lại chiếu về cung Phụ Mẫu: thường được cha mẹ nâng đỡ, gia đình gốc vững.",
     ["adv.quan_tam_cha_me"], CN_II)
rule("CG_PHU_MAU_AM_DUONG_HAM_SAT", "Phụ Mẫu: Nhật Nguyệt hãm gặp sát", "CUNG",
     {"palace": "PHU_MAU", "allOf": [S("THAI_DUONG", "chart", brightnessIn=["H"]), S("THAI_AM", "chart", brightnessIn=["H"]), CNT(SAT6, 2, "tamPhuongTuChinh")]},
     ["GIA_DAO"], -1, 1.5,
     "Nhật Nguyệt cùng hãm, cung Phụ Mẫu lại gặp nhiều sát tinh: nên dành nhiều sự quan tâm, chăm sóc sức khỏe cha mẹ.",
     ["adv.quan_tam_cha_me"], CN_II)
rule("CG_PHU_MAU_THUA_HUONG", "Phụ Mẫu: được thừa hưởng", "CUNG",
     {"palace": "PHU_MAU", "anyOf": [S("LOC_TON"), S("THIEN_PHU"), S("TUONG_QUAN"), S("QUOC_AN")]},
     ["GIA_DAO", "TAI_CHINH"], 1, 1.0,
     "Cung Phụ Mẫu có Lộc Tồn, Thiên Phủ, Tướng Quân hoặc Quốc Ấn: dễ được thừa hưởng tài sản, nền tảng từ cha mẹ.",
     ["adv.ro_rang_tien_bac_nguoi_than"], CN_II)
rule("CG_PHU_MAU_BAT_HOA", "Phụ Mẫu: dễ khác quan điểm", "CUNG",
     {"palace": "PHU_MAU", "anyOf": [S("THIEN_RIEU"), S("HOA_KY"), CNT(SAT6, 2)]},
     ["GIA_DAO"], -1, 1.0,
     "Cung Phụ Mẫu có Thiên Riêu, Hóa Kỵ hoặc sát tinh: cha mẹ dễ khác quan điểm với nhau hoặc với mình; cần nhiều sự kiên nhẫn.",
     ["adv.giu_hoa_khi_gia_dinh"], CN_II)
rule("CG_PHUC_DUC_DEP", "Phúc Đức đẹp", "CUNG",
     {"palace": "PHUC_DUC", "allOf": [CNT(["THIEN_LUONG", "THIEN_DONG", "TU_VI", "THIEN_PHU", "HOA_KHOA", "HOA_QUYEN", "HOA_LOC", "AN_QUANG", "THIEN_QUY"], 4, "tamPhuongTuChinh")]},
     ["PHUC_DUC"], 2, 2.0,
     "Cung Phúc Đức hội nhiều phúc tinh (Lương, Đồng, Tử Phủ, Khoa Quyền Lộc, Quang Quý): phúc ấm dày, gặp khó dễ được che chở; đây là chỗ dựa cho cả lá số.",
     ["adv.tu_duong_tinh_than"], CN_II)
rule("CG_PHUC_DUC_KY_SAT", "Phúc Đức gặp sát tinh nặng", "CUNG",
     {"palace": "PHUC_DUC", "anyOf": [ALL(S("DIA_KHONG"), S("DIA_KIEP")),
                                      ALL(ANY(S("HOA_TINH"), S("LINH_TINH")), CHI("THAN", "DAU", "TUAT", "HOI", "TY", "SUU"))]},
     ["PHUC_DUC"], -1, 1.5,
     "Cung Phúc Đức gặp Không Kiếp, hoặc Hỏa Linh ở cung ban đêm: tâm tính dễ bất an, nên chăm lo đời sống tinh thần và sự gắn kết dòng họ.",
     ["adv.tu_duong_tinh_than", "adv.giu_ket_noi"], CN_II)
rule("CG_DIEN_TAI_TINH", "Điền Trạch có tài tinh", "CUNG",
     {"palace": "DIEN_TRACH", "anyOf": [S("LOC_TON"), S("HOA_LOC"), S("THIEN_PHU"), S("VU_KHUC", brightnessIn=SANG), S("THAI_AM", brightnessIn=MIEU_VUONG)]},
     ["NHA_DAT"], 1, 1.5,
     "Cung Điền Trạch có tài tinh (Lộc, Thiên Phủ, Vũ Khúc hay Thái Âm sáng): thuận tích lũy nhà đất, dễ có tài sản cố định.",
     ["adv.giu_tai_san_co_dinh"], CN_II)
rule("CG_DIEN_HAO_KHONG_KIEP", "Điền Trạch gặp Hao, Không Kiếp", "CUNG",
     {"palace": "DIEN_TRACH", "anyOf": [S("DIA_KHONG"), S("DIA_KIEP"), S("DAI_HAO"), S("TIEU_HAO")]},
     ["NHA_DAT", "TAI_CHINH"], -1, 1.5,
     "Cung Điền Trạch gặp Không Kiếp hoặc Song Hao: tài sản cố định dễ hao hụt; tránh dùng nhà đất làm tài sản bảo đảm cho việc rủi ro.",
     ["adv.giu_tai_san_co_dinh", "adv.khong_dau_co"], CN_II)
rule("CG_DIEN_TRANH_CHAP", "Điền Trạch dễ tranh chấp", "CUNG",
     {"palace": "DIEN_TRACH", "anyOf": [S("KINH_DUONG"), S("DA_LA"), S("THIEN_HINH"), S("HOA_KY")]},
     ["NHA_DAT", "PHAP_LY"], -1, 1.5,
     "Cung Điền Trạch có Kình Đà, Thiên Hình hoặc Hóa Kỵ: dễ phát sinh tranh chấp về nhà đất; giấy tờ cần chặt chẽ.",
     ["adv.can_trong_giay_to_nha_dat"], CN_II)
rule("CG_QUAN_TU_PHU_VU_TUONG_LIEM", "Quan Lộc: bộ Tử Phủ Vũ Tướng Liêm hội cát", "CUNG",
     {"palace": "QUAN_LOC", "allOf": [any_self(["TU_VI", "THIEN_PHU", "VU_KHUC", "THIEN_TUONG", "LIEM_TRINH"]),
                                      CNT(["TA_PHU", "HUU_BAT", "THIEN_KHOI", "THIEN_VIET", "QUOC_AN", "LOC_TON", "HOA_LOC"], 2, "tamPhuongTuChinh")]},
     ["SU_NGHIEP"], 2, 2.0,
     "Cung Quan Lộc có bộ Tử Phủ Vũ Tướng Liêm hội Lộc, Ấn, Tả Hữu, Khôi Việt: thuận cho quản trị, lãnh đạo tổ chức lớn.",
     ["adv.phat_huy_lanh_dao"], CN_II)
rule("CG_QUAN_SAT_PHA_THAM_SAT", "Quan Lộc: Sát Phá Tham hội sát", "CUNG",
     {"palace": "QUAN_LOC", "allOf": [any_self(["THAT_SAT", "PHA_QUAN", "THAM_LANG"]), CNT(TU_SAT, 2, "tamPhuongTuChinh")]},
     ["SU_NGHIEP"], 1, 1.5,
     "Cung Quan Lộc có Sát Phá Tham hội sát tinh: hợp quân sự, an ninh, thể thao hoặc kinh doanh nhiều cạnh tranh.",
     ["adv.ky_cuong_phap_luat", "adv.tranh_lieu_linh"], CN_II)
rule("CG_QUAN_CO_NGUYET_DONG_LUONG", "Quan Lộc: Cơ Nguyệt Đồng Lương", "CUNG",
     {"palace": "QUAN_LOC", "allOf": [any_self(["THIEN_CO", "THAI_AM", "THIEN_DONG", "THIEN_LUONG"]),
                                      CNT(["THIEN_CO", "THAI_AM", "THIEN_DONG", "THIEN_LUONG"], 3, "tamPhuongTuChinh")]},
     ["SU_NGHIEP"], 1, 1.5,
     "Cung Quan Lộc thuộc bộ Cơ Nguyệt Đồng Lương: hợp giáo dục, hành chính công, y tế, văn hóa, nghiên cứu.",
     ["adv.hop_hanh_chinh_giao_duc"], CN_II)
rule("CG_NO_CAT", "Nô Bộc có cát tinh", "CUNG",
     {"palace": "NO_BOC", "allOf": [CNT(["THIEN_KHOI", "THIEN_VIET", "TA_PHU", "HUU_BAT", "HOA_KHOA", "LOC_TON"], 1)], "noneOf": [CNT(SAT6, 1)]},
     ["SU_NGHIEP"], 1, 1.0,
     "Cung Nô Bộc có cát tinh trợ lực, không sát tinh: bạn bè, cộng sự đắc lực, dễ có người nâng đỡ.",
     ["adv.tim_nguoi_pho_ta"], CN_II)
rule("CG_NO_HAM_SAT", "Nô Bộc gặp Không Kiếp, Kỵ, Phục Binh", "CUNG",
     {"palace": "NO_BOC", "anyOf": [S("DIA_KHONG"), S("DIA_KIEP"), S("HOA_KY"), S("PHUC_BINH")]},
     ["SU_NGHIEP", "TAI_CHINH"], -1, 1.5,
     "Cung Nô Bộc gặp Không Kiếp, Hóa Kỵ hoặc Phục Binh: cẩn trọng khi hợp tác, cho vay hoặc giao quyền cho người khác.",
     ["adv.chon_ban_lam_an", "adv.khong_tin_nguoi_de_dang"], CN_II)
rule("CG_DI_SAO_DONG", "Thiên Di có sao động", "CUNG",
     {"palace": "THIEN_DI", "anyOf": [S("THIEN_MA"), S("THIEN_DONG"), S("LONG_TRI"), S("PHUONG_CAC")]},
     ["DI_CHUYEN"], 1, 1.0,
     "Cung Thiên Di có Thiên Mã, Thiên Đồng, Long Trì hoặc Phượng Các: dễ đi xa, công tác, xuất ngoại.",
     ["adv.di_xa_mo_rong"], CN_II)
rule("CG_DI_SAT_TINH", "Thiên Di gặp nhiều sát tinh", "CUNG",
     {"palace": "THIEN_DI", "allOf": [CNT(SAT6, 2)]},
     ["DI_CHUYEN"], -1, 1.5,
     "Cung Thiên Di có từ hai sát tinh: ra ngoài nên cẩn trọng giao thông, tránh va chạm và thị phi.",
     ["adv.can_trong_di_lai", "adv.can_than_loi_noi"], CN_II)
rule("CG_TAT_HOA_LINH", "Tật Ách có Hỏa Linh", "CUNG",
     {"palace": "TAT_ACH", "anyOf": [S("HOA_TINH"), S("LINH_TINH")]},
     ["SUC_KHOE"], -1, 1.0,
     "Cung Tật Ách có Hỏa Tinh hoặc Linh Tinh: nên chú ý giữ sức, tránh làm việc quá sức; có dấu hiệu bất thường thì đi khám.",
     ["adv.dieu_do_sinh_hoat", "adv.kham_dinh_ky"], CN_II)
rule("CG_TAT_KINH_HINH", "Tật Ách có Kình Dương, Thiên Hình", "CUNG",
     {"palace": "TAT_ACH", "anyOf": [S("KINH_DUONG"), S("THIEN_HINH")]},
     ["SUC_KHOE"], -1, 1.0,
     "Cung Tật Ách có Kình Dương hoặc Thiên Hình: cẩn trọng va chạm khi vận động mạnh, lao động hay tham gia giao thông.",
     ["adv.can_trong_di_lai", "adv.kham_dinh_ky"], CN_II)
rule("CG_TAT_RIEU_KY", "Tật Ách có Thiên Riêu, Hóa Kỵ", "CUNG",
     {"palace": "TAT_ACH", "anyOf": [S("THIEN_RIEU"), S("HOA_KY")]},
     ["SUC_KHOE"], -1, 1.0,
     "Cung Tật Ách có Thiên Riêu hoặc Hóa Kỵ: nên khám sức khỏe định kỳ để phát hiện sớm những vấn đề âm thầm.",
     ["adv.kham_dinh_ky", "adv.khong_bo_qua_dau_hieu"], CN_II)
rule("CG_TAT_KY_XUNG", "Tật Ách bị Hóa Kỵ xung chiếu", "CUNG",
     {"palace": "TAT_ACH", "allOf": [S("HOA_KY", "xungChieu")]},
     ["SUC_KHOE"], -1, 1.0,
     "Cung Tật Ách bị Hóa Kỵ xung chiếu: nên duy trì khám định kỳ, không chủ quan với dấu hiệu nhỏ.",
     ["adv.kham_dinh_ky"], CN_II)
rule("CG_TAI_LOC", "Tài Bạch có tài tinh", "CUNG",
     {"palace": "TAI_BACH", "anyOf": [S("LOC_TON"), S("HOA_LOC"), S("VU_KHUC"), S("THIEN_PHU")]},
     ["TAI_CHINH"], 1, 1.5,
     "Cung Tài Bạch có Lộc, Vũ Khúc hoặc Thiên Phủ: dòng tiền có xu hướng thuận, giỏi xoay xở tài chính.",
     ["adv.ky_luat_tai_chinh"], CN_II)
rule("CG_TAI_SONG_HAO", "Tài Bạch gặp Song Hao", "CUNG",
     {"palace": "TAI_BACH", "anyOf": [S("DAI_HAO"), S("TIEU_HAO")]},
     ["TAI_CHINH"], -1, 1.0,
     "Cung Tài Bạch có Song Hao: tiền bạc tụ tán thất thường, có tiền là có việc phải chi — nên lập ngân sách.",
     ["adv.ky_luat_tai_chinh", "adv.lap_quy_du_phong"], CN_II)
rule("CG_TAI_KHONG_KIEP", "Tài Bạch gặp Không Kiếp", "CUNG",
     {"palace": "TAI_BACH", "anyOf": [S("DIA_KHONG"), S("DIA_KIEP")]},
     ["TAI_CHINH"], -1, 1.5,
     "Cung Tài Bạch gặp Địa Không/Địa Kiếp: kiếm tiền táo bạo nhưng dễ hụt hẫng; nên phân tán rủi ro.",
     ["adv.khong_dau_co", "adv.chot_loi_phan_tan"], CN_II)
rule("CG_TU_TUC_CAT", "Tử Tức có sao tốt", "CUNG",
     {"palace": "TU_TUC", "anyOf": [S("THAI"), ALL(S("LONG_TRI"), S("PHUONG_CAC")), S("THIEN_KHOI"), S("THIEN_VIET")]},
     ["GIA_DAO"], 1, 1.0,
     "Cung Tử Tức có Thai, Long Phượng hoặc Khôi Việt: đường con cái thuận, con dễ thành đạt.",
     [], CN_II)
rule("CG_TU_TUC_LUU_Y", "Tử Tức cần chăm sóc", "CUNG",
     {"palace": "TU_TUC", "allOf": [CNT(["BACH_HO", "TANG_MON", "THIEN_KHOC", "THIEN_HU", "DIA_KHONG", "DIA_KIEP"], 2)]},
     ["GIA_DAO"], -1, 1.0,
     "Cung Tử Tức có từ hai sao trong nhóm Tang Hổ, Khốc Hư, Không Kiếp: nên chăm sóc con nhỏ kỹ hơn; nếu có kế hoạch sinh con thì khám thai định kỳ.",
     ["adv.cham_soc_thai_ky"], CN_II)
rule("CG_PHU_THE_DOAN_CHINH", "Phu Thê: người phối ngẫu đoan chính", "CUNG",
     {"palace": "PHU_THE", "anyOf": [S("THIEN_TUONG"), S("THIEN_PHU"), ALL(S("THAI_AM"), {"gender": "NAM"})]},
     ["TINH_DUYEN"], 1, 1.0,
     "Cung Phu Thê có Thiên Tướng, Thiên Phủ (hoặc Thái Âm với nam): người phối ngẫu đoan chính, biết vun vén, giúp ích cho nhau.",
     ["adv.vun_dap_hon_nhan"], CN_II)
rule("CG_PHU_THE_TRAC_TRO", "Phu Thê nhiều sao cô, sát", "CUNG",
     {"palace": "PHU_THE", "allOf": [CNT(["THAT_SAT", "PHA_QUAN", "CO_THAN", "QUA_TU", "HOA_KY", "KINH_DUONG", "DA_LA"], 2)]},
     ["TINH_DUYEN"], -1, 1.5,
     "Cung Phu Thê có từ hai sao cô độc, sát phạt: hôn nhân cần vun đắp nhiều hơn; dễ bất đồng nếu thiếu lắng nghe.",
     ["adv.vun_dap_hon_nhan", "adv.khong_quyet_khi_gian"], CN_II)
rule("CG_HUYNH_DE_HOA_THUAN", "Huynh Đệ hòa thuận", "CUNG",
     {"palace": "HUYNH_DE", "allOf": [CNT(["TA_PHU", "HUU_BAT", "THIEN_KHOI", "THIEN_VIET", "HOA_KHOA", "HOA_LOC", "LOC_TON"], 1)], "noneOf": [CNT(SAT6, 1)]},
     ["GIA_DAO"], 1, 1.0,
     "Cung Huynh Đệ có cát tinh, không sát tinh: anh chị em hòa thuận, dễ tương trợ.",
     [], CN_II)
rule("CG_HUYNH_DE_BAT_HOA", "Huynh Đệ nhiều sát tinh", "CUNG",
     {"palace": "HUYNH_DE", "allOf": [CNT(SAT6, 2)]},
     ["GIA_DAO"], -1, 1.0,
     "Cung Huynh Đệ có từ hai sát tinh: anh chị em dễ bất đồng quyền lợi; nên rõ ràng chuyện tài sản chung.",
     ["adv.ro_rang_tien_bac_nguoi_than"], CN_II)

# ═══════════════════════════ THẾ SAO RIÊNG (Cẩm nang P.III) ═══════════════════════════
rule("ST_TU_VI_TY", "Tử Vi độc thủ tại Tý", "THE_SAO",
     {"palace": "MENH", "allOf": [S("TU_VI", chiIn=["TY"])]},
     ["TINH_CACH"], 0, 1.0,
     "Tử Vi độc thủ tại Tý: thế quân vương nhưng hơi trầm mặc, dễ thấy cô đơn khi ở vị trí cao.",
     ["adv.giu_ket_noi", "adv.tim_nguoi_pho_ta"], CN_III)
rule("ST_THIEN_CO_BAT_HUNG", "Thiên Cơ gặp hung tinh", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THIEN_CO"), CNT(["DIA_KHONG", "DIA_KIEP", "KINH_DUONG", "DA_LA", "HOA_TINH", "LINH_TINH", "HOA_KY", "THIEN_HINH"], 2)]},
     ["TINH_CACH", "SU_NGHIEP"], -1, 1.5,
     "Thiên Cơ thủ Mệnh gặp từ hai trong tám hung tinh: tâm trí dễ bất an, mưu tính dễ sai; nên làm việc có kế hoạch rõ và người góp ý.",
     ["adv.lang_nghe_gop_y"], CN_III, rel="B")
rule("ST_VU_KHUC_LUC_THAN", "Vũ Khúc ở cung lục thân", "THE_SAO",
     {"palace": ["PHU_THE", "HUYNH_DE", "TU_TUC"], "allOf": [S("VU_KHUC")]},
     ["@PALACE"], -1, 1.0,
     "Vũ Khúc tại cung {cung}: tính cương của tài tinh khiến quan hệ trong cung này cần được vun đắp nhiều hơn.",
     ["adv.giu_hoa_khi_gia_dinh"], CN_III, rel="B")
rule("ST_VU_KHUC_HOA_LINH", "Vũ Khúc gặp Hỏa Linh", "THE_SAO",
     {"palace": ["MENH", "TAI_BACH"], "allOf": [S("VU_KHUC"), ANY(S("HOA_TINH"), S("LINH_TINH"))]},
     ["TAI_CHINH"], -1, 1.5,
     "Vũ Khúc gặp Hỏa hoặc Linh tại cung {cung}: vì tiền dễ rước phiền phức; giữ kín đáo, tránh phô trương tài sản.",
     ["adv.ky_luat_tai_chinh"], CN_III)
rule("ST_LIEM_TRINH_KY", "Liêm Trinh gặp Hóa Kỵ", "THE_SAO",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH", chiIn=["TI", "HOI", "TY", "NGO"]), S("HOA_KY")]},
     ["PHAP_LY"], -2, 2.0,
     "Liêm Trinh gặp Hóa Kỵ tại {cung}: dễ va chạm pháp lý, kiện tụng; tuân thủ quy định và minh bạch giấy tờ là ưu tiên số một.",
     ["adv.doc_ky_hop_dong", "adv.khong_ky_khi_chua_ro"], CN_III)
rule("ST_LIEM_TRINH_NGHE_THUAT", "Liêm Trinh hội Xương Khúc, Đào Hồng", "THE_SAO",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH"), ANY(S("VAN_XUONG", "tamPhuongTuChinh"), S("VAN_KHUC", "tamPhuongTuChinh"), S("DAO_HOA"), S("HONG_LOAN"))]},
     ["SU_NGHIEP"], 1, 1.0,
     "Liêm Trinh hội Xương Khúc hoặc Đào Hồng: có gu thẩm mỹ tốt, hợp nghệ thuật, thiết kế, kiến trúc.",
     ["adv.tham_my_nghe_thuat"], CN_III, rel="B")
rule("ST_LIEM_TRINH_SANG", "Liêm Trinh sáng sủa", "THE_SAO",
     {"palace": "MENH", "allOf": [S("LIEM_TRINH", brightnessIn=MIEU_VUONG, chiIn=["THIN", "TUAT", "TY", "NGO"])]},
     ["SU_NGHIEP"], 1, 1.0,
     "Liêm Trinh sáng sủa thủ Mệnh: làm việc nghiêm minh; hợp tư pháp, thanh tra, công an, quân đội.",
     ["adv.ky_cuong_phap_luat"], CN_III)
rule("ST_THIEN_PHU_TAI_DIEN", "Thiên Phủ ở Tài, Điền", "THE_SAO",
     {"palace": ["TAI_BACH", "DIEN_TRACH"], "allOf": [S("THIEN_PHU")],
      "noneOf": [S("DIA_KHONG"), S("DIA_KIEP"), {"hasTuan": True}, {"hasTriet": True}]},
     ["@PALACE"], 1, 1.5,
     "Thiên Phủ tại cung {cung}, kho không bị thủng hay lộ: giỏi tích lũy, giữ của.",
     ["adv.giu_tai_san_co_dinh"], CN_III)
rule("ST_THAI_AM_SINH_DEM", "Thái Âm sáng, sinh ban đêm", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THAI_AM", brightnessIn=SANG), {"hourChiIn": ["TUAT", "HOI", "TY", "SUU", "DAN"]}]},
     ["TINH_CACH"], 1, 1.0,
     "Thái Âm sáng thủ Mệnh và sinh ban đêm: hợp cách, tính tinh tế, dễ được việc.",
     [], CN_III, rel="B")
rule("ST_THAI_AM_HAM_KY", "Thái Âm hãm gặp Hóa Kỵ", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THAI_AM", brightnessIn=["H"]), S("HOA_KY")]},
     ["GIA_DAO", "TINH_DUYEN"], -1, 1.5,
     "Thái Âm hãm thủ Mệnh lại gặp Hóa Kỵ: nên dành nhiều quan tâm cho mẹ và người bạn đời; chuyện gia đạo cần kiên nhẫn.",
     ["adv.quan_tam_cha_me", "adv.vun_dap_hon_nhan"], CN_III)
rule("ST_CU_MON_NHAT_CHIEU", "Cự Môn có Thái Dương sáng chiếu", "THE_SAO",
     {"palace": "MENH", "allOf": [S("CU_MON"), S("THAI_DUONG", "tamPhuongTuChinh", brightnessIn=MIEU_VUONG)]},
     ["TINH_CACH"], 1, 1.0,
     "Cự Môn thủ Mệnh được Thái Dương sáng chiếu: tính “ám” được giải, lời nói có sức nặng hơn.",
     ["adv.phat_huy_ngon_ngu"], CN_III, rel="B")
rule("ST_CU_MON_HUNG_BIEN", "Cự Môn hội Xương Khúc, Lộc Quyền", "THE_SAO",
     {"palace": "MENH", "allOf": [S("CU_MON"), CNT(["VAN_XUONG", "VAN_KHUC", "HOA_LOC", "HOA_QUYEN", "TAU_THU", "BACH_HO"], 2, "tamPhuongTuChinh")]},
     ["SU_NGHIEP"], 1, 1.5,
     "Cự Môn thủ Mệnh hội Xương Khúc, Lộc, Quyền, Tấu Thư hoặc Bạch Hổ: hợp luật, ngoại giao, truyền thông.",
     ["adv.phat_huy_ngon_ngu"], CN_III)
rule("ST_THIEN_LUONG_PHUC_DIEN", "Thiên Lương ở Phúc, Điền", "THE_SAO",
     {"palace": ["PHUC_DUC", "DIEN_TRACH"], "allOf": [S("THIEN_LUONG")]},
     ["@PALACE"], 1, 1.5,
     "Thiên Lương tại cung {cung}: phát phúc, dễ được hưởng nền tảng của gia đình.",
     ["adv.tu_duong_tinh_than"], CN_III)
rule("ST_THAT_SAT_VO_CONG", "Thất Sát hội Kình Đà đắc địa hoặc Lộc Ấn", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THAT_SAT"), ANY(S("KINH_DUONG", "tamPhuongTuChinh", chiIn=TU_MO), S("DA_LA", "tamPhuongTuChinh", chiIn=TU_MO),
                                                     ALL(ANY(S("LOC_TON", "tamPhuongTuChinh"), S("HOA_LOC", "tamPhuongTuChinh")), S("QUOC_AN", "tamPhuongTuChinh")))]},
     ["SU_NGHIEP"], 2, 2.0,
     "Thất Sát thủ Mệnh hội Kình Đà đắc địa hoặc Lộc Ấn: thăng tiến nhanh trong quân sự, an ninh, y khoa phẫu thuật.",
     ["adv.ky_cuong_phap_luat"], CN_III)
rule("ST_THAT_SAT_HAM_KY_KK", "Thất Sát hãm gặp Kỵ, Không Kiếp", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THAT_SAT", brightnessIn=["H"]), S("HOA_KY", "tamPhuongTuChinh"), ANY(S("DIA_KHONG", "tamPhuongTuChinh"), S("DIA_KIEP", "tamPhuongTuChinh"))]},
     ["SU_NGHIEP", "SUC_KHOE"], -2, 2.0,
     "Thất Sát hãm thủ Mệnh gặp Hóa Kỵ và Không Kiếp: đường đi nhiều trắc trở; nên chọn việc an toàn, cẩn trọng trong vận động.",
     ["adv.thu_hon_cong", "adv.can_trong_di_lai"], CN_III)
rule("ST_PHA_QUAN_PHU_THE", "Phá Quân ở Phu Thê", "THE_SAO",
     {"palace": "PHU_THE", "allOf": [S("PHA_QUAN")]},
     ["TINH_DUYEN"], -1, 1.0,
     "Phá Quân tại cung Phu Thê: duyên phận nhiều thay đổi; hôn nhân cần sự kiên nhẫn và vun đắp.",
     ["adv.vun_dap_hon_nhan", "adv.cuoi_muon_thuan"], CN_III)
rule("ST_PHA_QUAN_GAP_SAT", "Phá Quân gặp lục sát", "THE_SAO",
     {"palace": "MENH", "allOf": [S("PHA_QUAN"), ANY(S("DIA_KHONG"), S("DIA_KIEP"), S("KINH_DUONG"), S("DA_LA"))]},
     ["SU_NGHIEP"], 1, 1.5,
     "Phá Quân thủ Mệnh gặp Không Kiếp hoặc Kình Đà: Phá Quân ưa sát tinh — như mãnh thú có nanh vuốt, hợp lập nghiệp lớn trong môi trường cạnh tranh.",
     ["adv.dam_doi_moi_co_ke_hoach", "adv.chot_loi_phan_tan"], CN_III, rel="B")
rule("ST_THIEN_DONG_SAT", "Thiên Đồng hội nhiều sát tinh", "THE_SAO",
     {"palace": "MENH", "allOf": [S("THIEN_DONG"), CNT(SAT6, 2, "tamPhuongTuChinh")]},
     ["PHUC_DUC", "SU_NGHIEP"], 0, 1.0,
     "Thiên Đồng thủ Mệnh hội nhiều sát tinh: có xu hướng chuyển sang nghiên cứu huyền học, tâm linh, y học.",
     ["adv.nghien_cuu_chuyen_sau"], CN_III, rel="B")

# ═══════════════════════════ VẬN HẠN (Cẩm nang P.IV) ═══════════════════════════
MENH_HAN = [("THAI_DUONG", "Thái Dương"), ("THIEN_CO", "Thiên Cơ"), ("VU_KHUC", "Vũ Khúc"), ("LIEM_TRINH", "Liêm Trinh"),
            ("THAI_AM", "Thái Âm"), ("CU_MON", "Cự Môn"), ("THIEN_DONG", "Thiên Đồng"), ("THAM_LANG", "Tham Lang")]
for sid, vn in MENH_HAN:
    rule(f"VH_MENH_{sid}_KY_LUU", f"Mệnh {vn} gặp năm {vn} hóa Kỵ", "VAN_HAN",
         {"palace": "MENH", "allOf": [S(sid), {"luuHoaOf": sid, "hoa": ["KY"]}]},
         ["TINH_CACH", "SU_NGHIEP"], -1, 1.5,
         f"Mệnh có {vn}, năm {{nam}} can năm làm {vn} hóa Kỵ: năm dễ vướng mắc với chính điểm mạnh của mình; làm chậm mà chắc.",
         ["adv.thu_hon_cong", "adv.doc_ky_hop_dong"], CN_IV, scope="NAM")
    rule(f"VH_MENH_{sid}_LOC_LUU", f"Mệnh {vn} gặp năm {vn} hóa Lộc", "VAN_HAN",
         {"palace": "MENH", "allOf": [S(sid), {"luuHoaOf": sid, "hoa": ["LOC"]}]},
         ["TAI_CHINH", "SU_NGHIEP"], 1, 1.5,
         f"Mệnh có {vn}, năm {{nam}} can năm làm {vn} hóa Lộc: năm thuận để phát huy sở trường, tài lộc có xu hướng tăng.",
         ["adv.tan_dung_thoi_co"], CN_IV, scope="NAM")
for role in ["MENH", "PHU_MAU", "PHUC_DUC", "DIEN_TRACH", "QUAN_LOC", "NO_BOC", "THIEN_DI", "TAT_ACH", "TAI_BACH", "TU_TUC", "PHU_THE", "HUYNH_DE"]:
    rule(f"VH_LUU_THAI_TUE_{role}", f"Lưu Thái Tuế vào cung {role}", "VAN_HAN",
         {"layer": "luuThaiTue", "allOf": [{"role": [role]}]},
         ["@PALACE"], 0, 1.0,
         "Năm {nam}, Lưu Thái Tuế vào cung {cung}: các việc trong năm có xu hướng xoay quanh lĩnh vực của cung này.",
         ["adv.uu_tien_cong_viec_nam"], CN_IV, scope="NAM")
rule("VH_TIEU_HAN_TRUNG_LUU_NIEN", "Tiểu hạn trùng lưu niên đại hạn", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [AT("tieuHan", same="luuNienDaiHan")]},
     ["@PALACE"], 0, 1.5,
     "Năm {nam}, tiểu hạn và lưu niên đại hạn cùng ở cung {cung}: diễn biến và kết quả trong năm cùng xoay quanh một lĩnh vực.",
     ["adv.uu_tien_cong_viec_nam"], CN_IV, scope="NAM")
rule("VH_LUU_KINH_TAT", "Lưu Kình Dương vào Tật Ách", "VAN_HAN",
     {"palace": "TAT_ACH", "allOf": [L("KINH_DUONG")]},
     ["SUC_KHOE"], -1, 1.0,
     "Năm {nam}, Lưu Kình Dương vào cung Tật Ách: cẩn trọng va chạm, vận động và đi lại.",
     ["adv.can_trong_di_lai", "adv.kham_dinh_ky"], CN_IV, scope="NAM")
rule("VH_LUU_BACH_HO_TAT", "Lưu Bạch Hổ vào Tật Ách", "VAN_HAN",
     {"palace": "TAT_ACH", "allOf": [L("BACH_HO")]},
     ["SUC_KHOE"], -1, 1.0,
     "Năm {nam}, Lưu Bạch Hổ vào cung Tật Ách: nên khám sức khỏe định kỳ, đi lại cẩn trọng hơn.",
     ["adv.kham_dinh_ky", "adv.can_trong_di_lai"], CN_IV, scope="NAM")
rule("VH_LUU_LOC_TON_TAI", "Lưu Lộc Tồn vào Tài Bạch", "VAN_HAN",
     {"palace": "TAI_BACH", "allOf": [L("LOC_TON")]},
     ["TAI_CHINH"], 1, 1.0,
     "Năm {nam}, Lưu Lộc Tồn vào cung Tài Bạch: có thêm cơ hội tài lộc.",
     ["adv.ky_luat_tai_chinh"], CN_IV, scope="NAM")
rule("VH_LUU_LOC_TON_MENH", "Lưu Lộc Tồn vào Mệnh", "VAN_HAN",
     {"palace": "MENH", "allOf": [L("LOC_TON")]},
     ["TAI_CHINH", "TINH_CACH"], 1, 1.0,
     "Năm {nam}, Lưu Lộc Tồn vào cung Mệnh: năm có cơ hội và người giúp; giữ tâm thế chủ động.",
     ["adv.tan_dung_thoi_co"], CN_IV, scope="NAM")
rule("VH_LUU_MA_DI_CHUYEN", "Lưu Thiên Mã vào Mệnh/Di", "VAN_HAN",
     {"palace": ["MENH", "THIEN_DI"], "allOf": [L("THIEN_MA")]},
     ["DI_CHUYEN"], 0, 1.0,
     "Năm {nam}, Lưu Thiên Mã vào cung {cung}: dễ có di chuyển, đổi việc hoặc mua sắm phương tiện.",
     ["adv.di_xa_mo_rong"], CN_IV, scope="NAM")
rule("VH_LUU_KHOC_HU_MENH", "Lưu Khốc Hư vào Mệnh", "VAN_HAN",
     {"palace": "MENH", "anyOf": [L("THIEN_KHOC"), L("THIEN_HU")]},
     ["TINH_CACH"], -1, 1.0,
     "Năm {nam}, Lưu Thiên Khốc hoặc Thiên Hư vào cung Mệnh: tâm trạng dễ trùng xuống; giữ nhịp sống điều độ và chia sẻ với người thân.",
     ["adv.giu_ket_noi", "adv.dieu_do_sinh_hoat"], CN_IV, scope="NAM")
rule("VH_LUU_TANG_MON_MENH", "Lưu Tang Môn vào Mệnh", "VAN_HAN",
     {"palace": "MENH", "allOf": [L("TANG_MON")]},
     ["GIA_DAO", "SU_NGHIEP"], -1, 1.0,
     "Năm {nam}, Lưu Tang Môn vào cung Mệnh: dễ có chuyện phiền lòng hoặc thay đổi chỗ làm; quan tâm người thân nhiều hơn.",
     ["adv.quan_tam_cha_me"], CN_IV, scope="NAM")
rule("VH_KY_LUU_MENH", "Hóa Kỵ lưu vào Mệnh", "VAN_HAN",
     {"palace": "MENH", "allOf": [{"luuHoa": "KY", "in": "self"}]},
     ["TINH_CACH"], -1, 1.5,
     "Năm {nam}, Hóa Kỵ lưu vào cung Mệnh: dễ vướng hiểu lầm, việc chậm; giữ bình tĩnh và minh bạch.",
     ["adv.can_than_loi_noi", "adv.thu_hon_cong"], CN_IV + "; Prompt mục 7", scope="NAM")
rule("VH_LOC_LUU_MENH", "Hóa Lộc lưu vào Mệnh", "VAN_HAN",
     {"palace": "MENH", "allOf": [{"luuHoa": "LOC", "in": "self"}]},
     ["TAI_CHINH", "TINH_CACH"], 1, 1.5,
     "Năm {nam}, Hóa Lộc lưu vào cung Mệnh: năm thuận, dễ có cơ hội mới.",
     ["adv.tan_dung_thoi_co"], CN_IV + "; Prompt mục 7", scope="NAM")
rule("VH_LOC_LUU_TAI", "Hóa Lộc lưu vào Tài Bạch", "VAN_HAN",
     {"palace": "TAI_BACH", "allOf": [{"luuHoa": "LOC", "in": "self"}]},
     ["TAI_CHINH"], 1, 1.5,
     "Năm {nam}, Hóa Lộc lưu vào cung Tài Bạch: dòng tiền có xu hướng cải thiện.",
     ["adv.ky_luat_tai_chinh"], CN_IV + "; Prompt mục 7", scope="NAM")
rule("VH_KY_LUU_TAI_QUAN", "Hóa Kỵ lưu vào Tài/Quan", "VAN_HAN",
     {"palace": ["TAI_BACH", "QUAN_LOC"], "allOf": [{"luuHoa": "KY", "in": "self"}]},
     ["@PALACE"], -1, 1.5,
     "Năm {nam}, Hóa Kỵ lưu vào cung {cung}: lĩnh vực này dễ chậm trễ, vướng giấy tờ; nên thận trọng và giữ chứng từ.",
     ["adv.doc_ky_hop_dong", "adv.thu_hon_cong"], CN_IV + "; Prompt mục 7", scope="NAM")
THIEN_THOI = [
    ("DAI_HAN_SINH_TUOI", "Đại hạn sinh tuổi", 2, "đắc thiên thời, được hoàn cảnh nâng đỡ; thuận để chủ động mở rộng", ["adv.tan_dung_thoi_co"]),
    ("DONG_HANH", "Đại hạn đồng hành", 1, "hòa hợp, thuận để giữ vững và phát triển thành quả", ["adv.on_dinh_hon_but_pha"]),
    ("TUOI_KHAC_DAI_HAN", "Tuổi khắc đại hạn", 0, "vất vả, phải tự lực nhưng có thể giành được thành quả", ["adv.tu_luc"]),
    ("TUOI_SINH_DAI_HAN", "Tuổi sinh đại hạn", -1, "dễ hao tâm tổn sức, thành quả bị san sẻ; nên chọn việc trọng tâm", ["adv.giam_tai_cong_viec"]),
    ("DAI_HAN_KHAC_TUOI", "Đại hạn khắc tuổi", -2, "thất thời — nên thủ hơn công, hoãn quyết định lớn nếu được", ["adv.thu_hon_cong", "adv.khong_mo_rong_nong"]),
]
for key, name, pol, meaning, advs in THIEN_THOI:
    rule(f"VH_THIEN_THOI_{key}", f"Thiên thời: {name}", "VAN_HAN",
         {"layer": "daiHan", "allOf": [{"thienThoiIn": [key]}]},
         ["SU_NGHIEP", "TAI_CHINH"], pol, 2.0,
         f"Đại hạn 10 năm tại cung {{cung}} — {name.lower()} theo ngũ hành tam hợp (cụ Thiên Lương): {meaning}.",
         advs, CN_IV + "; Prompt mục 8.1", scope="DAI_HAN")

# ═══════════════════════════ CHUYÊN ĐỀ (Prompt 10.5, Cẩm nang P.V) ═══════════════════════════
HON_NHAN_TEXT = "Dấu hiệu tại cung Phu Thê cho thấy hôn nhân cần vun đắp nhiều hơn; lập gia đình muộn và tìm hiểu kỹ thường thuận hơn."
rule("CD_HON_NHAN_HOA_CAI_MA", "Hôn nhân: Hoa Cái – Thiên Mã – Tuần/Triệt", "CHUYEN_DE",
     {"palace": "PHU_THE", "allOf": [AT("menh", hasStar="HOA_CAI"), S("THIEN_MA"), ANY({"hasTuan": True}, {"hasTriet": True})]},
     ["TINH_DUYEN"], -1, 1.5, "Mệnh có Hoa Cái, cung Phu Thê có Thiên Mã gặp Tuần/Triệt. " + HON_NHAN_TEXT,
     ["adv.cuoi_muon_thuan", "adv.vun_dap_hon_nhan"], P10_5 + "; " + CN_V)
rule("CD_HON_NHAN_KINH_DA_CO_QUA", "Hôn nhân: Kình Mệnh – Đà Phu Thê gặp Tang/Cô/Quả", "CHUYEN_DE",
     {"palace": "PHU_THE", "allOf": [AT("menh", hasStar="KINH_DUONG"), S("DA_LA"), ANY(S("TANG_MON"), S("CO_THAN"), S("QUA_TU"))]},
     ["TINH_DUYEN"], -1, 1.5, "Mệnh có Kình Dương, cung Phu Thê có Đà La gặp Tang Môn, Cô Thần hoặc Quả Tú. " + HON_NHAN_TEXT,
     ["adv.cuoi_muon_thuan", "adv.vun_dap_hon_nhan"], P10_5 + "; " + CN_V)
rule("CD_HON_NHAN_DUONG_LUONG", "Hôn nhân: Dương Lương Mão Dậu – Đồng Cự hóa Kỵ", "CHUYEN_DE",
     {"palace": "MENH", "allOf": [S("THAI_DUONG"), S("THIEN_LUONG"), CHI("MAO", "DAU"),
                                   AT("role:PHU_THE", hasStar="THIEN_DONG"), AT("role:PHU_THE", hasStar="CU_MON"), AT("role:PHU_THE", hasStar="HOA_KY")]},
     ["TINH_DUYEN"], -1, 1.5, "Mệnh Dương Lương tại {cung}, cung Phu Thê có Đồng Cự gặp Hóa Kỵ: dễ bất đồng trong lời nói. " + HON_NHAN_TEXT,
     ["adv.vun_dap_hon_nhan", "adv.khong_quyet_khi_gian"], P10_5 + "; " + CN_V)
rule("CD_HON_NHAN_TUONG_LIEM_THAM", "Hôn nhân: Thiên Tướng – Phu Thê Liêm Tham hóa Kỵ", "CHUYEN_DE",
     {"palace": "MENH", "allOf": [S("THIEN_TUONG"), AT("role:PHU_THE", hasStar="LIEM_TRINH"), AT("role:PHU_THE", hasStar="THAM_LANG"),
                                   AT("role:PHU_THE", hasStar="HOA_KY")]},
     ["TINH_DUYEN"], -1, 1.5, "Mệnh có Thiên Tướng, cung Phu Thê có Liêm Tham gặp Hóa Kỵ. " + HON_NHAN_TEXT,
     ["adv.vun_dap_hon_nhan", "adv.khong_quyet_khi_gian"], P10_5 + "; " + CN_V)
rule("CD_HON_NHAN_SAT_PHA_HAM_KY", "Hôn nhân: Sát Phá hãm gặp Hóa Kỵ ở Phu Thê", "CHUYEN_DE",
     {"palace": "PHU_THE", "allOf": [ANY(S("THAT_SAT", brightnessIn=["H"]), S("PHA_QUAN", brightnessIn=["H"])), S("HOA_KY")]},
     ["TINH_DUYEN"], -1, 1.5, "Cung Phu Thê có Thất Sát hoặc Phá Quân hãm gặp Hóa Kỵ. " + HON_NHAN_TEXT,
     ["adv.vun_dap_hon_nhan", "adv.cuoi_muon_thuan"], P10_5 + "; " + CN_V)
TINH_DUYEN_O = ANY(AT("menh", hasLuuStar=LUU_TINH_DUYEN), AT("than", hasLuuStar=LUU_TINH_DUYEN), AT("role:NO_BOC", hasLuuStar=LUU_TINH_DUYEN))
rule("CD_HAN_HY_CUOI_HOI", "Hạn hỷ: năm thuận cưới hỏi", "CHUYEN_DE",
     {"layer": "tieuHan", "allOf": [TINH_DUYEN_O, any_self(["THANH_LONG", "LONG_TRI", "PHUONG_CAC", "THIEN_HY"]),
                                     ANY(AT("luuNienDaiHan", roleIn=["MENH", "PHU_THE", "QUAN_LOC", "TAI_BACH"]), AT("luuNienDaiHan", same="than"))]},
     ["TINH_DUYEN"], 2, 2.0,
     "Năm {nam} hội đủ ba dấu hiệu hạn hỷ (sao lưu tình duyên vào Mệnh/Thân/Nô, tiểu hạn có sao hôn lễ, lưu niên đại hạn vào cung hợp): khả năng cao là năm thuận cho chuyện cưới hỏi.",
     ["adv.chu_dong_gap_go"], P10_5 + "; " + CN_V, scope="NAM", suppress=["CD_HAN_HY_TINH_DUYEN"])
rule("CD_HAN_HY_TINH_DUYEN", "Hạn hỷ: sao lưu tình duyên kích hoạt", "CHUYEN_DE",
     {"palace": "MENH", "allOf": [ANY(AT("menh", hasLuuStar=["DAO_HOA", "HONG_LOAN", "HY_THAN"]), AT("than", hasLuuStar=["DAO_HOA", "HONG_LOAN", "HY_THAN"]))]},
     ["TINH_DUYEN"], 1, 1.0,
     "Năm {nam} có Lưu Đào Hoa, Hồng Loan hoặc Hỷ Thần vào Mệnh hay Thân: chuyện tình cảm dễ được khơi gợi; đây mới là một dấu hiệu, nên lưu ý.",
     ["adv.chu_dong_gap_go"], P10_5 + "; " + CN_V, scope="NAM")
rule("CD_LUU_THAI_TUE_CO_QUA", "Lưu Thái Tuế trùng Cô Thần/Quả Tú", "CHUYEN_DE",
     {"layer": "luuThaiTue", "anyOf": [S("CO_THAN"), S("QUA_TU")]},
     ["TINH_DUYEN"], -1, 1.5,
     "Năm {nam}, Lưu Thái Tuế gặp Cô Thần/Quả Tú gốc: theo quan niệm Tử Vi, năm này không thuận cưới hỏi; nếu đã định, hãy dành nhiều thời gian cho giao tiếp vợ chồng.",
     ["adv.vun_dap_hon_nhan"], P10_5 + "; " + CN_V, scope="NAM")
rule("CD_SINH_CON_LUU_LOC", "Sinh con: Lộc lưu vào Tử Tức", "CHUYEN_DE",
     {"palace": "TU_TUC", "anyOf": [L("LOC_TON"), {"luuHoa": "LOC", "in": "self"}]},
     ["GIA_DAO"], 1, 1.0,
     "Năm {nam}, Lưu Lộc Tồn hoặc Hóa Lộc lưu vào cung Tử Tức: thuận cho chuyện con cái.",
     ["adv.cham_soc_thai_ky"], P10_5 + "; " + CN_V, scope="NAM")
rule("CD_SINH_CON_THAI", "Sinh con: tiểu hạn hội sao Thai", "CHUYEN_DE",
     {"layer": "tieuHan", "anyOf": [S("THAI"), S("THAI", "tamHop")]},
     ["GIA_DAO"], 1, 1.0,
     "Năm {nam}, tiểu hạn ở tam hợp của sao Thai: theo cẩm nang là năm thuận cho việc có con, nếu gia đình đang mong.",
     ["adv.cham_soc_thai_ky"], P10_5 + "; " + CN_V, scope="NAM")
rule("CD_SINH_CON_LUU_Y", "Sinh con: dấu hiệu cần chăm sóc thai kỳ", "CHUYEN_DE",
     {"palace": "TU_TUC", "anyOf": [ALL(L("BACH_HO"), L("THIEN_KHOC")), ALL(L("THIEN_HU"), any_self(["KINH_DUONG", "DA_LA", "DIA_KHONG", "DIA_KIEP"]))]},
     ["GIA_DAO"], -1, 1.5,
     "Năm {nam}, cung Tử Tức có sao lưu cần lưu ý: nếu có kế hoạch sinh con, nên chăm sóc thai kỳ kỹ và khám định kỳ theo hướng dẫn của bác sĩ.",
     ["adv.cham_soc_thai_ky"], P10_5 + "; " + CN_V, scope="NAM")
rule("CD_CON_CAI_KHONG_DON_TUYEN", "Đường con cái không đơn tuyến (chuyên sâu)", "CHUYEN_DE",
     {"palace": "TU_TUC", "anyOf": [
         ALL(S("THIEN_CO"), S("CU_MON"), CHI("MAO", "DAU")),
         ALL(S("THIEN_TUONG"), any_self(["TUYET", "THAI", "TA_PHU", "HUU_BAT"])),
         ALL(S("THAI"), S("PHUC_BINH"), S("DE_VUONG"), S("TUONG_QUAN")),
         ANY(ALL(S("TUONG_QUAN"), S("PHUC_BINH", "xungChieu")), ALL(S("PHUC_BINH"), S("TUONG_QUAN", "xungChieu"))),
         ALL(S("THAI_AM"), S("THIEN_PHUC"))]},
     ["GIA_DAO"], 0, 0.5,
     "Đường con cái có thể không đơn tuyến (con nuôi, con riêng hoặc con của hôn nhân sau). Đây là quan điểm riêng của một số sách, chỉ để tham khảo.",
     [], P10_5 + "; " + CN_V, rel="C", expert=True)
