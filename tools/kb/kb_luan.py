"""Bổ sung knowledge base cho giai đoạn 4 (luận giải + khuyến nghị).

- Luật năm theo từng tầng cho quy tắc “đồng thanh tương ứng” (mục 9), gắn sự kiện (`effect.event`).
- Sáu tình huống của bảng khuyến nghị mục 11.
- Khuyến nghị KHÔNG NÊN / LƯU Ý theo chủ đề, khuyến nghị dự phòng khi chủ đề mạnh/yếu và theo thế Tấn/Giữ/Thủ/Lùi.
- Câu sự kiện theo mức khả năng, câu thế giai đoạn, ghi chú bắt buộc (mục 11 quy tắc 5, 8; nguyên tắc 3).

Mọi mục `reviewed: false` cho tới khi người dùng duyệt. Chạy tools/kb/build_kb.py để xuất JSON.
"""
from kb_rules import ADVICE, RULES, adv, rule, S, L, ALL, ANY, CNT, AT, any_self, SAT6, CN_IV, CN_V, P10_5

P11 = "Prompt mục 11"
P9 = "Prompt mục 9 (đồng thanh tương ứng)"

# ——————————————————————————— khuyến nghị bổ sung ———————————————————————————
adv("adv.nhan_trach_nhiem_moi", "NEN", "SU_NGHIEP", "Nhận trách nhiệm mới, đề xuất thăng tiến hoặc ra mắt dự án vào những tháng điểm cao.", stances=["TAN", "GIU"])
adv("adv.khong_doi_viec_cam_tinh", "KHONG_NEN", "SU_NGHIEP", "Đổi việc vì cảm xúc nhất thời, nhất là trong tháng có Kình Dương, Đà La lưu.", 2, uses=["WEAK"])
adv("adv.khong_om_viec", "KHONG_NEN", "SU_NGHIEP", "Ôm đồm quá nhiều việc cùng lúc rồi không việc nào trọn vẹn.", uses=["WEAK"])
adv("adv.khong_chi_phong_tay", "KHONG_NEN", "TAI_CHINH", "Chi tiêu phô trương hoặc mua sắm lớn theo cảm hứng.", uses=["WEAK"])
adv("adv.khong_dem_chuyen_rieng_ra_ngoai", "KHONG_NEN", "TINH_DUYEN", "Đem chuyện riêng của hai người ra bàn với người ngoài khi đang có mâu thuẫn.", uses=["WEAK"])
adv("adv.khong_mo_ho_tai_san_gia_dinh", "KHONG_NEN", "GIA_DAO", "Để tiền bạc, tài sản chung với người thân mập mờ, không có giấy tờ.", uses=["WEAK"])
adv("adv.khong_qua_suc", "KHONG_NEN", "SUC_KHOE", "Làm việc quá sức, thức khuya kéo dài.", uses=["WEAK"])
adv("adv.khong_voi_vang_di_xa", "KHONG_NEN", "DI_CHUYEN", "Lên đường vội vàng khi chưa chuẩn bị giấy tờ, lịch trình, bảo hiểm.", uses=["WEAK"])
adv("adv.khong_the_chap_nha", "KHONG_NEN", "NHA_DAT", "Dùng nhà đất làm tài sản bảo đảm cho khoản vay nhiều rủi ro.", 2, uses=["WEAK"])
adv("adv.khong_dung_ten_viec_mo_ho", "KHONG_NEN", "PHAP_LY", "Đứng tên, bảo lãnh hay tham gia việc chưa rõ ràng về pháp lý.", 2, uses=["WEAK"])
adv("adv.khong_hua_qua_suc", "KHONG_NEN", "TINH_CACH", "Hứa hẹn vượt khả năng chỉ để giữ thể diện.", uses=["WEAK"])
adv("adv.khong_ton_tien_doi_van", "KHONG_NEN", "PHUC_DUC", "Tốn tiền cho các dịch vụ tâm linh thu phí hứa hẹn đổi vận.", 2, source=P11 + " quy tắc 7", uses=["WEAK"])
adv("adv.chuan_bi_chuyen_di", "NEN", "DI_CHUYEN", "Chuẩn bị kỹ giấy tờ, lịch trình và phương án dự phòng trước mỗi chuyến đi xa.", uses=["WEAK"])
adv("adv.giu_nguyen_tai_san", "NEN", "NHA_DAT", "Giữ nguyên hiện trạng tài sản lớn; chưa vội mua bán nếu dòng tiền chưa vững.", uses=["WEAK"])
adv("adv.ly_cong_viec_cham", "LUU_Y", "SU_NGHIEP", "Công việc có thể chậm hơn kỳ vọng: chia mục tiêu lớn thành từng bước và theo dõi tiến độ.", uses=["WEAK"])
adv("adv.ly_nguoi_than", "LUU_Y", "GIA_DAO", "Người thân có thể cần bạn nhiều hơn; sắp xếp thời gian thăm hỏi, chăm sóc.", uses=["WEAK"])
adv("adv.ly_tam_ly", "LUU_Y", "TINH_CACH", "Tâm lý dễ dao động: giữ nhịp sinh hoạt đều đặn và chia sẻ khi thấy áp lực.", uses=["WEAK"])
adv("adv.ly_suc_khoe", "LUU_Y", "SUC_KHOE", "Để ý giấc ngủ, ăn uống, vận động; đi khám khi có dấu hiệu bất thường kéo dài.", 2, uses=["WEAK"])
adv("adv.ly_binh_an", "LUU_Y", "PHUC_DUC", "Giữ sự bình an trong lòng; đừng để lo lắng chi phối các quyết định lớn.", uses=["WEAK"])
adv("adv.ly_dong_tien", "LUU_Y", "TAI_CHINH", "Dòng tiền có thể căng vào một số thời điểm: theo dõi thu chi hằng tháng.", uses=["WEAK"])
adv("adv.ly_tinh_cam", "LUU_Y", "TINH_DUYEN", "Chuyện tình cảm cần kiên nhẫn: nói rõ mong muốn, tránh suy đoán ý nhau.", uses=["WEAK"])
adv("adv.ly_giay_to", "LUU_Y", "PHAP_LY", "Lưu giữ chứng từ, tin nhắn thỏa thuận quan trọng để tránh hiểu lầm về sau.", uses=["WEAK"])
adv("adv.ly_di_lai", "LUU_Y", "DI_CHUYEN", "Kiểm tra phương tiện trước chuyến đi xa; tránh đi đêm khi mệt.", uses=["WEAK"])
adv("adv.ly_nha_dat", "LUU_Y", "NHA_DAT", "Rà soát giấy tờ nhà đất, sửa chữa hư hỏng nhỏ trước khi thành lớn.", uses=["WEAK"])

# Khuyến nghị theo thế giai đoạn (mục 11 bước 4).
adv("adv.the_tan_nen", "NEN", "SU_NGHIEP", "Chủ động mở rộng: đề xuất dự án, nhận vai trò lớn hơn, tìm thị trường mới trong các tháng điểm cao.", stances=["TAN"], uses=["STANCE"])
adv("adv.the_tan_khong", "KHONG_NEN", "TAI_CHINH", "Chủ quan vì đang thuận mà bỏ qua kế hoạch dự phòng.", stances=["TAN"], uses=["STANCE"])
adv("adv.the_giu_nen", "NEN", "SU_NGHIEP", "Củng cố những gì đang có: hoàn thiện kỹ năng, chăm sóc khách hàng và các mối quan hệ.", stances=["GIU"], uses=["STANCE"])
adv("adv.the_giu_khong", "KHONG_NEN", "SU_NGHIEP", "Đổi hướng lớn khi chưa có lý do rõ ràng và phương án dự phòng.", stances=["GIU"], uses=["STANCE"])
adv("adv.the_thu_nen", "NEN", "TAI_CHINH", "Ưu tiên an toàn: giữ dòng tiền, hoàn tất việc dang dở trước khi mở việc mới.", stances=["THU"], uses=["STANCE"])
adv("adv.the_thu_khong", "KHONG_NEN", "TAI_CHINH", "Nhận cam kết dài hạn mới hoặc dồn khoản tiền lớn vào một chỗ.", 2, stances=["THU"], uses=["STANCE"])
adv("adv.the_lui_nen", "NEN", "TINH_CACH", "Hoãn quyết định lớn nếu được; dồn sức cho sức khỏe, gia đình và nền tảng.", 2, stances=["LUI"], uses=["STANCE"])
adv("adv.the_lui_khong", "KHONG_NEN", "SU_NGHIEP", "Ép tiến độ, chạy theo mục tiêu lớn khi hoàn cảnh chưa thuận.", 2, stances=["LUI"], uses=["STANCE"])
adv("adv.chung_ra_soat", "NEN", "TINH_CACH", "Ghi lại mục tiêu và rà soát mỗi tháng để điều chỉnh kịp thời.", uses=["STANCE"])
adv("adv.chung_khong_quyet_voi", "KHONG_NEN", "TINH_CACH", "Quyết việc lớn khi chưa đủ thông tin hoặc đang mệt mỏi, nóng giận.", uses=["STANCE"])

# Lá số trẻ nhỏ (dưới 16 tuổi âm, D-39): chỉ khuyến nghị về học tập, sức khỏe, gia đình, an toàn.
adv("adv.tre_tro_chuyen", "NEN", "GIA_DAO", "Dành thời gian cùng con trò chuyện, đọc sách, vui chơi mỗi ngày.", uses=["CHILD"])
adv("adv.tre_ngu_du", "NEN", "SUC_KHOE", "Cho trẻ ngủ đủ giấc, ăn uống điều độ và vận động ngoài trời thường xuyên.", uses=["CHILD"])
adv("adv.tre_kham_pha", "NEN", "SU_NGHIEP", "Khuyến khích trẻ khám phá năng khiếu qua nhiều hoạt động khác nhau.", uses=["CHILD"])
adv("adv.tre_khong_ep_hoc", "KHONG_NEN", "SU_NGHIEP", "Ép trẻ học quá sức hay so sánh trẻ với bạn bè.", uses=["CHILD"])
adv("adv.tre_khong_man_hinh", "KHONG_NEN", "SUC_KHOE", "Để trẻ dùng điện thoại, máy tính bảng quá lâu mỗi ngày.", uses=["CHILD"])
adv("adv.tre_khong_cai_nhau", "KHONG_NEN", "GIA_DAO", "Tranh cãi gay gắt trước mặt trẻ.", uses=["CHILD"])
adv("adv.tre_an_toan", "LUU_Y", "DI_CHUYEN", "Chú ý an toàn cho trẻ khi đi lại, vui chơi gần nước và xe cộ.", 2, uses=["CHILD"])

# Gắn công dụng dự phòng cho khuyến nghị sẵn có (chủ đề mạnh → STRONG, yếu → WEAK).
FALLBACK = {
    "adv.tan_dung_thoi_co": ["STRONG"], "adv.chot_loi_phan_tan": ["STRONG"], "adv.chu_dong_gap_go": ["STRONG"],
    "adv.giu_hoa_khi_gia_dinh": ["STRONG"], "adv.dieu_do_sinh_hoat": ["STRONG"], "adv.di_xa_mo_rong": ["STRONG"],
    "adv.giu_tai_san_co_dinh": ["STRONG"], "adv.doc_ky_hop_dong": ["STRONG", "WEAK"], "adv.giu_tron_chu_tin": ["STRONG"],
    "adv.tu_duong_tinh_than": ["STRONG", "WEAK"], "adv.giam_tai_cong_viec": ["WEAK"], "adv.lap_quy_du_phong": ["WEAK"],
    "adv.vun_dap_hon_nhan": ["WEAK"], "adv.quan_tam_cha_me": ["WEAK"], "adv.kham_dinh_ky": ["WEAK"], "adv.lang_nghe_gop_y": ["WEAK"],
    "adv.khong_dau_co": ["WEAK"], "adv.khong_quyet_khi_gian": ["WEAK"], "adv.khong_bo_qua_dau_hieu": ["WEAK"],
    "adv.khong_ky_khi_chua_ro": ["WEAK"], "adv.khong_doc_doan": ["WEAK"],
}
for k, uses in FALLBACK.items():
    ADVICE[k]["uses"] = sorted(set(ADVICE[k].get("uses", [])) | set(uses))
STANCES = {"adv.tan_dung_thoi_co": ["TAN", "GIU"], "adv.thu_hon_cong": ["THU", "LUI"], "adv.giam_tai_cong_viec": ["GIU", "THU", "LUI"],
           "adv.chot_loi_phan_tan": ["TAN", "GIU"]}
for k, st in STANCES.items():
    ADVICE[k]["stances"] = st

# ——————————————————————————— luật năm theo tầng, gắn sự kiện ———————————————————————————
CAT_QUAN = ["HOA_QUYEN", "THIEN_KHOI", "THIEN_VIET"]
LOC_ALL = lambda inn="self": ANY(S("LOC_TON", inn), S("HOA_LOC", inn), L("LOC_TON", inn), {"luuHoa": "LOC", "in": inn})
CAT_DH = ["HOA_LOC", "HOA_QUYEN", "HOA_KHOA", "TA_PHU", "HUU_BAT", "THIEN_KHOI", "THIEN_VIET", "LOC_TON"]
HAO_KK = ["DAI_HAO", "TIEU_HAO", "DIA_KHONG", "DIA_KIEP"]
TINH_DUYEN_SAO = ["HONG_LOAN", "THIEN_HY", "DAO_HOA", "LONG_TRI", "PHUONG_CAC"]

# Thăng tiến (mục 11 bảng, dòng 1: Lưu Thái Tuế + tiểu hạn vào Quan Lộc có Hóa Quyền, Khôi Việt).
rule("VH_LTT_QUAN_QUYEN", "Lưu Thái Tuế vào Quan Lộc có Quyền, Khôi Việt", "VAN_HAN",
     {"layer": "luuThaiTue", "allOf": [{"role": ["QUAN_LOC"]}, ANY(*[S(s) for s in CAT_QUAN], {"luuHoa": "QUYEN", "in": "self"})]},
     ["SU_NGHIEP"], 2, 1.5,
     "Năm {nam}, Lưu Thái Tuế đánh thức cung Quan Lộc có Hóa Quyền hoặc Khôi Việt: thuận để nhận trách nhiệm mới, đề xuất thăng tiến.",
     ["adv.nhan_trach_nhiem_moi", "adv.khong_doi_viec_cam_tinh"], P11 + "; " + CN_IV, scope="NAM", event="THANG_TIEN")
rule("VH_TIEU_HAN_QUAN_QUYEN", "Tiểu hạn vào Quan Lộc có Quyền, Khôi Việt", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["QUAN_LOC"]}, ANY(*[S(s) for s in CAT_QUAN], {"luuHoa": "QUYEN", "in": "self"})]},
     ["SU_NGHIEP"], 1, 1.5,
     "Năm {nam}, tiểu hạn đi vào cung Quan Lộc có Hóa Quyền hoặc Khôi Việt: diễn biến công việc trong năm có nhiều cơ hội thể hiện năng lực.",
     ["adv.nhan_trach_nhiem_moi"], P11 + "; " + CN_IV, scope="NAM", event="THANG_TIEN")
rule("VH_LNDH_CAT_HOI", "Lưu niên đại hạn hội cát tinh", "VAN_HAN",
     {"layer": "luuNienDaiHan", "allOf": [CNT(CAT_DH, 3, "tamPhuongTuChinh")]},
     ["SU_NGHIEP", "TAI_CHINH"], 1, 1.5,
     "Năm {nam}, lưu niên đại hạn tại cung {cung} hội nhiều cát tinh (Lộc, Quyền, Khoa, Tả Hữu, Khôi Việt): kết quả cuối năm có xu hướng tốt.",
     ["adv.tan_dung_thoi_co"], CN_IV + "; Prompt mục 8.3", scope="NAM", event="THANG_TIEN")
rule("VH_DAI_HAN_CAT_HOI", "Đại hạn hội nhiều cát tinh", "VAN_HAN",
     {"layer": "daiHan", "allOf": [CNT(CAT_DH, 4, "tamPhuongTuChinh")]},
     ["SU_NGHIEP", "TAI_CHINH"], 1, 2.0,
     "Đại hạn 10 năm tại cung {cung} hội nhiều cát tinh trong tam phương tứ chính: nền vận thuận để xây dựng sự nghiệp và tài sản.",
     ["adv.tan_dung_thoi_co", "adv.tich_luy_truoc_khi_bung"], CN_IV + "; Prompt mục 8.1", scope="DAI_HAN", event="THANG_TIEN")
rule("VH_DAI_HAN_SAT_HOI", "Đại hạn hội nhiều sát tinh", "VAN_HAN",
     {"layer": "daiHan", "allOf": [CNT(SAT6 + ["HOA_KY"], 4, "tamPhuongTuChinh")]},
     ["SU_NGHIEP", "TAI_CHINH"], -1, 2.0,
     "Đại hạn 10 năm tại cung {cung} hội nhiều sát tinh trong tam phương tứ chính: giai đoạn nhiều thử thách, nên đi chắc từng bước và giữ nền tảng.",
     ["adv.thu_hon_cong", "adv.lap_quy_du_phong"], CN_IV + "; Prompt mục 8.1", scope="DAI_HAN")

# Thay đổi công việc, đi xa.
rule("VH_TIEU_HAN_PHA_MA", "Tiểu hạn gặp Phá Quân hoặc Thiên Mã", "VAN_HAN",
     {"layer": "tieuHan", "anyOf": [S("PHA_QUAN"), S("THIEN_MA"), L("THIEN_MA")]},
     ["SU_NGHIEP"], 0, 1.0,
     "Năm {nam}, tiểu hạn gặp Phá Quân hoặc Thiên Mã: dễ có thay đổi công việc, chỗ làm hay cách làm việc.",
     ["adv.dam_doi_moi_co_ke_hoach", "adv.khong_doi_viec_cam_tinh"], CN_IV, scope="NAM", event="DOI_VIEC")
rule("VH_LTT_QUAN_DONG", "Lưu Thái Tuế vào Quan Lộc gặp sao động", "VAN_HAN",
     {"layer": "luuThaiTue", "allOf": [{"role": ["QUAN_LOC"]}, ANY(S("PHA_QUAN"), S("THIEN_MA"), L("THIEN_MA"), S("THAT_SAT"))]},
     ["SU_NGHIEP"], 0, 1.0,
     "Năm {nam}, Lưu Thái Tuế vào cung Quan Lộc gặp sao động (Phá Quân, Thất Sát, Thiên Mã): công việc có xu hướng chuyển động, đổi mới.",
     ["adv.dam_doi_moi_co_ke_hoach"], CN_IV, scope="NAM", event="DOI_VIEC")
rule("VH_TIEU_HAN_THIEN_DI", "Tiểu hạn vào Thiên Di", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["THIEN_DI"]}]},
     ["DI_CHUYEN"], 0, 1.0,
     "Năm {nam}, tiểu hạn đi vào cung Thiên Di: năm nhiều việc bên ngoài, dễ đi xa hoặc thay đổi môi trường.",
     ["adv.chuan_bi_chuyen_di"], CN_IV, scope="NAM", event="DI_CHUYEN")
rule("VH_LNDH_THIEN_DI", "Lưu niên đại hạn vào Thiên Di", "VAN_HAN",
     {"layer": "luuNienDaiHan", "allOf": [{"role": ["THIEN_DI"]}]},
     ["DI_CHUYEN"], 0, 1.0,
     "Năm {nam}, lưu niên đại hạn vào cung Thiên Di: kết quả của năm gắn với việc đi xa, đối ngoại.",
     ["adv.di_xa_mo_rong"], CN_IV + "; Prompt mục 8.3", scope="NAM", event="DI_CHUYEN")
rule("VH_DI_MA_LOC_LUU", "Thiên Di có Lưu Thiên Mã gặp Lộc", "VAN_HAN",
     {"palace": "THIEN_DI", "allOf": [L("THIEN_MA"), LOC_ALL()]},
     ["DI_CHUYEN", "TAI_CHINH"], 1, 1.5,
     "Năm {nam}, cung Thiên Di có Lưu Thiên Mã gặp Lộc: thuận đi xa, công tác, mở thị trường mới.",
     ["adv.di_xa_mo_rong"], P11, scope="NAM", event="DI_CHUYEN")

# Sức khỏe (mục 11 bảng, dòng 3).
rule("VH_TAT_KINH_HINH_BACH_HO", "Tật Ách có Kình Dương/Thiên Hình gặp Lưu Bạch Hổ", "VAN_HAN",
     {"palace": "TAT_ACH", "allOf": [ANY(S("KINH_DUONG"), S("THIEN_HINH")), L("BACH_HO")]},
     ["SUC_KHOE"], -1, 1.5,
     "Năm {nam}, cung Tật Ách có Kình Dương hoặc Thiên Hình gặp Lưu Bạch Hổ: nên khám sức khỏe định kỳ, cẩn trọng khi lái xe và vận động mạnh.",
     ["adv.kham_dinh_ky", "adv.khong_bo_qua_dau_hieu"], P11, scope="NAM", event="SUC_KHOE", suppress=["VH_LUU_BACH_HO_TAT"])
rule("VH_TIEU_HAN_TAT_SAT", "Tiểu hạn vào Tật Ách gặp sát tinh", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["TAT_ACH"]}, CNT(SAT6, 2)]},
     ["SUC_KHOE"], -1, 1.5,
     "Năm {nam}, tiểu hạn vào cung Tật Ách gặp sát tinh: giai đoạn cần chăm sóc sức khỏe và an toàn kỹ hơn.",
     ["adv.dieu_do_sinh_hoat", "adv.khong_qua_suc"], CN_IV, scope="NAM", event="SUC_KHOE")

# Hao tài, được tài (mục 11 bảng, dòng 2).
rule("VH_TAI_HAO_LUU_SAT", "Tài Bạch có Song Hao/Không Kiếp gặp sát lưu", "VAN_HAN",
     {"palace": "TAI_BACH", "allOf": [any_self(HAO_KK), ANY(L("KINH_DUONG"), L("DA_LA"), {"luuHoa": "KY", "in": "self"})]},
     ["TAI_CHINH"], -1, 1.5,
     "Năm {nam}, cung Tài Bạch có Song Hao hoặc Không Kiếp lại gặp Kình Đà lưu hay Hóa Kỵ lưu: nên lập quỹ dự phòng, chia nhỏ khoản đầu tư.",
     ["adv.lap_quy_du_phong", "adv.khong_dau_co"], P11, scope="NAM", event="HAO_TAI")
rule("VH_TIEU_HAN_TAI_HAO", "Tiểu hạn vào Tài Bạch gặp Hao, Không Kiếp", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["TAI_BACH"]}, any_self(HAO_KK)]},
     ["TAI_CHINH"], -1, 1.0,
     "Năm {nam}, tiểu hạn vào cung Tài Bạch gặp Song Hao hoặc Không Kiếp: dễ phát sinh khoản chi ngoài dự tính.",
     ["adv.ky_luat_tai_chinh", "adv.khong_chi_phong_tay"], CN_IV, scope="NAM", event="HAO_TAI")
rule("VH_LNDH_TAI_KY", "Lưu niên đại hạn vào Tài Bạch gặp Kỵ, Không Kiếp", "VAN_HAN",
     {"layer": "luuNienDaiHan", "allOf": [{"role": ["TAI_BACH"]}, ANY(S("HOA_KY"), {"luuHoa": "KY", "in": "self"}, S("DIA_KHONG"), S("DIA_KIEP"))]},
     ["TAI_CHINH"], -1, 1.5,
     "Năm {nam}, lưu niên đại hạn vào cung Tài Bạch gặp Hóa Kỵ hoặc Không Kiếp: kết quả tài chính cuối năm cần giữ chặt, tránh mạo hiểm.",
     ["adv.lap_quy_du_phong", "adv.khong_mo_rong_nong"], CN_IV + "; Prompt mục 8.3", scope="NAM", event="HAO_TAI")
rule("VH_TIEU_HAN_LOC", "Tiểu hạn gặp Lộc", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [LOC_ALL()]},
     ["TAI_CHINH"], 1, 1.0,
     "Năm {nam}, tiểu hạn gặp Lộc Tồn hoặc Hóa Lộc: diễn biến trong năm có thêm cơ hội về tiền bạc.",
     ["adv.ky_luat_tai_chinh"], CN_IV, scope="NAM", event="DUOC_TAI")
rule("VH_LNDH_LOC", "Lưu niên đại hạn hội Lộc", "VAN_HAN",
     {"layer": "luuNienDaiHan", "allOf": [LOC_ALL("tamPhuongTuChinh"), {"role": ["MENH", "TAI_BACH", "QUAN_LOC", "DIEN_TRACH", "THIEN_DI"]}]},
     ["TAI_CHINH"], 1, 1.0,
     "Năm {nam}, lưu niên đại hạn tại cung {cung} hội Lộc: kết quả tài chính trong năm có xu hướng cải thiện.",
     ["adv.chot_loi_phan_tan"], CN_IV + "; Prompt mục 8.3", scope="NAM", event="DUOC_TAI")

# Pháp lý, thị phi (mục 11 bảng, dòng 5).
QP_HINH_KY = ["QUAN_PHU", "THIEN_HINH", "HOA_KY"]
rule("VH_TIEU_HAN_PHAP_LY", "Tiểu hạn vào Quan Lộc/Thiên Di gặp Quan Phù, Hình, Kỵ", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["QUAN_LOC", "THIEN_DI"]}, any_self(QP_HINH_KY)]},
     ["PHAP_LY"], -1, 1.5,
     "Năm {nam}, tiểu hạn vào cung {cung} gặp Quan Phù, Thiên Hình hoặc Hóa Kỵ: đọc kỹ hợp đồng, lưu chứng từ, hỏi người có chuyên môn khi cần.",
     ["adv.doc_ky_hop_dong", "adv.khong_ky_khi_chua_ro"], P11, scope="NAM", event="PHAP_LY")
rule("VH_LTT_PHAP_LY", "Lưu Thái Tuế vào Quan Lộc/Thiên Di gặp Quan Phù, Hình, Kỵ", "VAN_HAN",
     {"layer": "luuThaiTue", "allOf": [{"role": ["QUAN_LOC", "THIEN_DI"]}, any_self(QP_HINH_KY)]},
     ["PHAP_LY"], -1, 1.5,
     "Năm {nam}, Lưu Thái Tuế vào cung {cung} gặp Quan Phù, Thiên Hình hoặc Hóa Kỵ: dễ va chạm giấy tờ, thị phi; tránh tham gia việc mập mờ.",
     ["adv.doc_ky_hop_dong", "adv.khong_dung_ten_viec_mo_ho"], P11, scope="NAM", event="PHAP_LY")
rule("VH_KY_LUU_PHAP_LY", "Hóa Kỵ lưu gặp Quan Phù/Thiên Hình ở Quan Lộc, Thiên Di", "VAN_HAN",
     {"palace": ["QUAN_LOC", "THIEN_DI"], "allOf": [{"luuHoa": "KY", "in": "self"}, ANY(S("QUAN_PHU"), S("THIEN_HINH"))]},
     ["PHAP_LY"], -1, 1.5,
     "Năm {nam}, Hóa Kỵ lưu vào cung {cung} gặp Quan Phù hoặc Thiên Hình: ký kết, giấy tờ cần thận trọng gấp đôi.",
     ["adv.doc_ky_hop_dong", "adv.khong_ky_khi_chua_ro"], P11, scope="NAM", event="PHAP_LY")

# Tình duyên (mục 11 bảng, dòng 4) và con cái.
rule("VH_PHU_THE_HONG_HY_LONG_PHUONG", "Phu Thê có Hồng Loan/Thiên Hỷ lưu gặp Long Trì, Phượng Các", "VAN_HAN",
     {"palace": "PHU_THE", "allOf": [ANY(L("HONG_LOAN"), L("THIEN_HY")), ANY(S("LONG_TRI"), S("PHUONG_CAC"))]},
     ["TINH_DUYEN"], 1, 1.5,
     "Năm {nam}, cung Phu Thê có Hồng Loan hoặc Thiên Hỷ lưu gặp Long Trì, Phượng Các: thuận để chủ động gặp gỡ, bàn chuyện cưới hỏi.",
     ["adv.chu_dong_gap_go", "adv.khong_quyet_khi_gian"], P11, scope="NAM", event="CUOI_HOI")
rule("VH_TIEU_HAN_PHU_THE", "Tiểu hạn vào Phu Thê gặp sao tình duyên", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["PHU_THE"]}, ANY(*[S(s) for s in TINH_DUYEN_SAO], L("HONG_LOAN"), L("THIEN_HY"), L("DAO_HOA"))]},
     ["TINH_DUYEN"], 1, 1.0,
     "Năm {nam}, tiểu hạn vào cung Phu Thê gặp sao tình duyên: chuyện tình cảm được khơi gợi trong năm.",
     ["adv.chu_dong_gap_go"], CN_V, scope="NAM", event="CUOI_HOI")
rule("VH_LNDH_PHU_THE", "Lưu niên đại hạn vào Phu Thê gặp sao tình duyên", "VAN_HAN",
     {"layer": "luuNienDaiHan", "allOf": [{"role": ["PHU_THE"]}, ANY(*[S(s) for s in TINH_DUYEN_SAO], L("HONG_LOAN"), L("THIEN_HY"))]},
     ["TINH_DUYEN"], 1, 1.0,
     "Năm {nam}, lưu niên đại hạn vào cung Phu Thê gặp sao tình duyên: kết quả năm có xu hướng gắn với chuyện gia đình riêng.",
     ["adv.vun_dap_hon_nhan"], CN_V + "; Prompt mục 8.3", scope="NAM", event="CUOI_HOI")
rule("VH_TIEU_HAN_TU_TUC", "Tiểu hạn vào Tử Tức gặp sao hỷ", "VAN_HAN",
     {"layer": "tieuHan", "allOf": [{"role": ["TU_TUC"]}, ANY(S("THAI"), S("THANH_LONG"), S("HY_THAN"), S("THIEN_HY"), L("THIEN_HY"), L("HY_THAN"))]},
     ["GIA_DAO"], 1, 1.0,
     "Năm {nam}, tiểu hạn vào cung Tử Tức gặp sao hỷ: thuận cho chuyện con cái nếu gia đình đang mong.",
     ["adv.cham_soc_thai_ky"], CN_V, scope="NAM", event="SINH_CON")

# Gắn sự kiện cho luật sẵn có.
EVENT_TAGS = {
    "CD_HAN_HY_CUOI_HOI": "CUOI_HOI", "CD_HAN_HY_TINH_DUYEN": "CUOI_HOI",
    "CD_SINH_CON_LUU_LOC": "SINH_CON", "CD_SINH_CON_THAI": "SINH_CON",
    "VH_LUU_KINH_TAT": "SUC_KHOE", "VH_LUU_BACH_HO_TAT": "SUC_KHOE",
    "VH_LUU_LOC_TON_TAI": "DUOC_TAI", "VH_LOC_LUU_TAI": "DUOC_TAI", "VH_LOC_LUU_MENH": "DUOC_TAI",
    "VH_LUU_MA_DI_CHUYEN": "DI_CHUYEN",
}
for s in ["THAI_DUONG", "THIEN_CO", "VU_KHUC", "LIEM_TRINH", "THAI_AM", "CU_MON", "THIEN_DONG", "THAM_LANG"]:
    EVENT_TAGS[f"VH_MENH_{s}_LOC_LUU"] = "DUOC_TAI"
_by_id = {r["id"]: r for r in RULES}
for rid, ev in EVENT_TAGS.items():
    _by_id[rid]["effect"]["event"] = ev

# ——————————————————————————— câu luận dùng khi ghép (texts/luan.json) ———————————————————————————
EVENTS = {
    "CUOI_HOI": ("chuyện tình cảm, cưới hỏi", 1), "SINH_CON": ("chuyện con cái, đón thành viên mới", 1),
    "THANG_TIEN": ("thăng tiến, nhận trách nhiệm mới", 1), "DOI_VIEC": ("thay đổi công việc, môi trường làm việc", 0),
    "DUOC_TAI": ("tài lộc, dòng tiền cải thiện", 1), "HAO_TAI": ("hao tài, chi phí ngoài dự tính", -1),
    "PHAP_LY": ("vướng giấy tờ, thị phi, va chạm pháp lý", -1), "DI_CHUYEN": ("đi xa, di chuyển, đổi chỗ ở hoặc chỗ làm", 0),
    "SUC_KHOE": ("cần chăm sóc sức khỏe, an toàn kỹ hơn", -1),
}
LEVEL_TAIL = {
    ("cao", 1): "khả năng cao đây là điểm sáng của năm, nên chủ động đón lấy.",
    ("cao", 0): "khả năng cao năm có chuyển động theo hướng này; chuẩn bị trước sẽ chủ động hơn.",
    ("cao", -1): "khả năng cao đây là việc cần phòng bị trong năm; chuẩn bị sớm sẽ giảm nhẹ ảnh hưởng.",
    ("co_the", 1): "có thể thành hiện thực nếu bạn chủ động.",
    ("co_the", 0): "có thể xảy ra, nên tính trước phương án.",
    ("co_the", -1): "có thể gặp, nên phòng bị từ sớm.",
    ("dau_hieu", 1): "mới là dấu hiệu, nên lưu ý để không bỏ lỡ.",
    ("dau_hieu", 0): "mới là dấu hiệu, nên lưu ý.",
    ("dau_hieu", -1): "mới là dấu hiệu, nên lưu ý và cẩn trọng hơn thường lệ.",
}
LEVEL_HEAD = {"cao": "từ ba tầng vận hạn trở lên cùng chỉ về", "co_the": "hai tầng vận hạn cùng chỉ về", "dau_hieu": "một tầng vận hạn chỉ về"}


def luan_texts():
    out = {}
    def put(key, text, source):
        out[key] = {"key": key, "text": text, "source": source, "reviewed": False}
    for ev, (phrase, pol) in EVENTS.items():
        for lv in ("cao", "co_the", "dau_hieu"):
            put(f"ev.{ev.lower()}.{lv}", f"Năm {{nam}}, {LEVEL_HEAD[lv]} {phrase}: {LEVEL_TAIL[(lv, pol)]}", P9)
    put("the.tan", "Thế Tấn: giai đoạn thuận, nên chủ động mở rộng.", P11 + " bước 4")
    put("the.giu", "Thế Giữ: nên củng cố, phát triển vững những gì đang có.", P11 + " bước 4")
    put("the.thu", "Thế Thủ: nên tránh rủi ro lớn, ưu tiên an toàn.", P11 + " bước 4")
    put("the.lui", "Thế Lùi: nên hoãn quyết định lớn nếu được, dồn sức cho nền tảng.", P11 + " bước 4")
    put("note.duc_nang", "Đức năng thắng số: Tử Vi là bản đồ thời tiết, lựa chọn của bạn quyết định kết quả.", P11 + " quy tắc 8")
    put("note.ket_noi", "Giai đoạn này có thể nhiều áp lực tinh thần. Hãy giữ kết nối thường xuyên với người thân, bạn bè tin cậy, và tìm gặp chuyên gia tâm lý khi thấy căng thẳng kéo dài.", P11 + " quy tắc 5")
    put("note.chua_co_luan", "Chưa có luận giải riêng cho tổ hợp này; điểm số dựa trên sao tại các cung đọc chính.", "Prompt mục 1, nguyên tắc 3")
    put("note.suc_khoe", "Phần sức khỏe chỉ là gợi ý để chú ý chăm sóc bản thân, không thay cho thăm khám của bác sĩ.", P11 + " quy tắc 3")
    put("note.tai_chinh", "Phần tài chính chỉ nêu khung rủi ro, không phải lời khuyên mua bán tài sản cụ thể.", P11 + " quy tắc 4")
    put("note.tre_nho", "Lá số trẻ nhỏ (dưới 16 tuổi âm): phần luận vận hạn chỉ tập trung vào học tập, sức khỏe, gia đình và an toàn đi lại.", "D-39")
    put("note.thang", "Điểm tháng tính từ điểm năm, cung nguyệt hạn và Tứ Hóa tháng; độ tin cậy thấp hơn tầng năm.", "Prompt mục 9 bước 6")
    return out
