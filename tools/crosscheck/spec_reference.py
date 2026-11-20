#!/usr/bin/env python3
"""Bản cài đặt tham chiếu độc lập: viết lại lá số + vận hạn trực tiếp từ docs/PROMPT_TUVI_APP.md rồi so với engine.

Không dùng chung mã với engine Kotlin. Các bảng dữ liệu (mục 5.5, 5.6, 6, 7) đọc thẳng từ prompt; công thức (mục 5.1–5.4,
5.7, 8.1–8.5) gõ lại từ văn bản prompt; thứ tự 12 cung theo cẩm nang (Mệnh → Huynh Đệ → Phu Thê … đi nghịch).
Đầu vào là ngày âm + chi giờ đã chuẩn hóa (lịch và chuẩn hóa đầu vào được kiểm riêng).

Cách dùng (từ thư mục gốc dự án):
    ./gradlew :core:engine:dumpFull -q --args="20000 7" > /tmp/full.jsonl
    python3 tools/crosscheck/spec_reference.py /tmp/full.jsonl docs/PROMPT_TUVI_APP.md
"""
import json
import re
import sys
import unicodedata
from collections import Counter

CHI = ["Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"]
CAN = ["Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"]


def m12(x):
    return x % 12


def star_id(vn_name):
    """'Lộc Tồn' → 'LOC_TON' (quy ước đặt tên enum; 'Quan Phủ' của vòng Bác Sĩ là QUAN_PHU_BS)."""
    if vn_name == "Quan Phủ":
        return "QUAN_PHU_BS"
    s = unicodedata.normalize("NFD", vn_name.replace("Đ", "D").replace("đ", "d"))
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Mn")
    return s.upper().replace(" ", "_")


def chi_index(name):
    return CHI.index(name.strip().strip("\\[]"))


# ---------------------------------------------------------------- đọc bảng từ prompt
def load_tables(path):
    lines = [unicodedata.normalize("NFC", ln.rstrip("\n")) for ln in open(path, encoding="utf-8")]

    def table_after(prefix):
        start = next(i for i, ln in enumerate(lines) if ln.startswith(prefix))
        first = next(i for i in range(start + 1, len(lines)) if lines[i].startswith("|"))
        rows = []
        for ln in lines[first:]:
            if not ln.startswith("|"):
                break
            cells = [c.strip() for c in ln.strip().strip("|").split("|")]
            if all(re.fullmatch(r"-{3,}", c) for c in cells):
                continue
            rows.append(cells)
        return rows

    t = {}
    can_rows = table_after("### 5.5")
    t["can"] = {}
    for row in can_rows[1:]:
        if row[0] == "Triệt":
            t["triet"] = [tuple(chi_index(x) for x in cell.split("–")) for cell in row[1:]]
        else:
            t["can"][star_id(row[0])] = [chi_index(x) for x in row[1:]]
    t["hoaLinh"] = {}
    for row in table_after("### 5.6")[1:]:
        for z in row[0].split(" "):
            t["hoaLinh"][chi_index(z)] = (chi_index(row[1]), chi_index(row[2]))
    code = {"M": "M", "V": "V", "Đ": "Đ", "B": "B", "H": "H"}
    t["bright"] = {star_id(row[0]): [code[x] for x in row[1:]] for row in table_after("## 6.")[1:]}
    phu_start = next(i for i, ln in enumerate(lines) if ln.startswith("**Phụ tinh có đắc/hãm**"))
    t["phu"] = {}
    for ln in lines[phu_start + 1:]:
        if ln.startswith("|") and not ln.startswith("| Sao") and not ln.startswith("| ---"):
            cells = [c.strip() for c in ln.strip().strip("|").split("|")]
            dac = {chi_index(x) for x in cells[1].split(",")}
            other = "H" if cells[2].startswith("Hãm") else None
            for name in cells[0].split(","):
                t["phu"][star_id(name.strip())] = (dac, other)
        elif t["phu"] and not ln.startswith("|"):
            break
    t["tuHoa"] = {}
    for row in table_after("## 7.")[1:]:
        t["tuHoa"][CAN.index(row[0])] = [star_id(x) for x in row[1:]]
    return t


# ---------------------------------------------------------------- công thức gõ lại từ prompt
ELEM = {1: "KIM", 2: "THUY", 3: "HOA", 4: "THO", 5: "MOC"}
CUC_NUMBER = {"THUY": 2, "MOC": 3, "KIM": 4, "THO": 5, "HOA": 6}
GEN = {"KIM": "THUY", "THUY": "MOC", "MOC": "HOA", "HOA": "THO", "THO": "KIM"}       # tương sinh
KHAC = {"KIM": "MOC", "MOC": "THO", "THO": "THUY", "THUY": "HOA", "HOA": "KIM"}      # tương khắc
BAC_SI = ["Bác Sĩ", "Lực Sĩ", "Thanh Long", "Tiểu Hao", "Tướng Quân", "Tấu Thư", "Phi Liêm", "Hỷ Thần", "Bệnh Phù",
          "Đại Hao", "Phục Binh", "Quan Phủ"]
THAI_TUE = ["Thái Tuế", "Thiếu Dương", "Tang Môn", "Thiếu Âm", "Quan Phù", "Tử Phù", "Tuế Phá", "Long Đức", "Bạch Hổ",
            "Phúc Đức", "Điếu Khách", "Trực Phù"]
TRANG_SINH = ["Tràng Sinh", "Mộc Dục", "Quan Đới", "Lâm Quan", "Đế Vượng", "Suy", "Bệnh", "Tử", "Mộ", "Tuyệt", "Thai", "Dưỡng"]
# Cẩm nang P.I ch.3: an 12 cung đi nghịch từ Mệnh.
ROLES_CCW = ["MENH", "HUYNH_DE", "PHU_THE", "TU_TUC", "TAI_BACH", "TAT_ACH", "THIEN_DI", "NO_BOC", "QUAN_LOC",
             "DIEN_TRACH", "PHUC_DUC", "PHU_MAU"]
MENH_CHU = {"Tý": "Tham Lang", "Sửu": "Cự Môn", "Hợi": "Cự Môn", "Dần": "Lộc Tồn", "Tuất": "Lộc Tồn", "Mão": "Văn Khúc",
            "Dậu": "Văn Khúc", "Thìn": "Liêm Trinh", "Thân": "Liêm Trinh", "Tỵ": "Vũ Khúc", "Mùi": "Vũ Khúc", "Ngọ": "Phá Quân"}
THAN_CHU = {"Sửu": "Thiên Tướng", "Mùi": "Thiên Tướng", "Dần": "Thiên Lương", "Thân": "Thiên Lương", "Mão": "Thiên Đồng",
            "Dậu": "Thiên Đồng", "Thìn": "Văn Xương", "Tuất": "Văn Xương", "Tỵ": "Thiên Cơ", "Hợi": "Thiên Cơ", "Ngọ": "Hỏa Tinh"}


def nap_am(can, chi):
    v = can // 2 + 1 + (chi % 6) // 2
    if v > 5:
        v -= 5
    return ELEM[v]


def tam_hop_group(z):  # 0 Thân Tý Thìn, 1 Tỵ Dậu Sửu, 2 Dần Ngọ Tuất, 3 Hợi Mão Mùi
    for g, members in enumerate([("Thân", "Tý", "Thìn"), ("Tỵ", "Dậu", "Sửu"), ("Dần", "Ngọ", "Tuất"), ("Hợi", "Mão", "Mùi")]):
        if CHI[z] in members:
            return g


TAM_HOP_ELEM = ["THUY", "KIM", "HOA", "MOC"]


def tu_hoa(t, can, cfg):
    row = list(t["tuHoa"][can])
    if can == 6 and cfg["tuHoa"] == "THIEN_LUONG":  # Canh: Dương Lộc, Vũ Quyền, Đồng Khoa, Âm Kỵ
        row = ["THAI_DUONG", "VU_KHUC", "THIEN_DONG", "THAI_AM"]
    if can == 8 and cfg["nhamKhoa"] == "THIEN_PHU":
        row[2] = "THIEN_PHU"
    return dict(zip(["LOC", "QUYEN", "KHOA", "KY"], row))


def reference(rec, t):
    cfg = rec["config"]
    D, mo, Y, leap, h = rec["lunarDay"], rec["lunarMonth"], rec["lunarYear"], rec["leap"], rec["hour"]
    nam = rec["gender"] == "NAM"
    c, z = (Y + 6) % 10, (Y + 8) % 12
    m = mo % 12 + 1 if (cfg["leapMonth"] == "SPLIT_15" and leap and D >= 16) else mo
    dirn = 1 if ((c % 2 == 0) == nam) else -1
    menh, than = m12(2 + (m - 1) - h), m12(2 + (m - 1) + h)
    can_dan = (2 * c + 2) % 10

    def can_of(p):
        return (can_dan + m12(p - 2)) % 10

    cuc_el = nap_am(can_of(menh), menh)
    cuc = CUC_NUMBER[cuc_el]
    S = {}
    q = -(-D // cuc)
    y = q * cuc - D
    base = 2 + q - 1
    tv = m12(base - y if y % 2 == 1 else base + y)
    tp = m12(4 - tv)
    for name, off in [("TU_VI", 0), ("THIEN_CO", -1), ("THAI_DUONG", -3), ("VU_KHUC", -4), ("THIEN_DONG", -5), ("LIEM_TRINH", -8)]:
        S[name] = m12(tv + off)
    for name, off in [("THIEN_PHU", 0), ("THAI_AM", 1), ("THAM_LANG", 2), ("CU_MON", 3), ("THIEN_TUONG", 4),
                      ("THIEN_LUONG", 5), ("THAT_SAT", 6), ("PHA_QUAN", 10)]:
        S[name] = m12(tp + off)
    lt = t["can"]["LOC_TON"][c]
    S["LOC_TON"], S["KINH_DUONG"], S["DA_LA"] = lt, m12(lt + 1), m12(lt - 1)
    for i, n in enumerate(BAC_SI):
        S[star_id(n)] = m12(lt + i * dirn)
    for i, n in enumerate(THAI_TUE):
        S[star_id(n)] = m12(z + i)
    ts = {"KIM": 5, "MOC": 11, "THUY": 8, "THO": 8, "HOA": 2}[cuc_el]
    for i, n in enumerate(TRANG_SINH):
        S[star_id(n)] = m12(ts + i * dirn)
    S["THIEN_KHONG"], S["NGUYET_DUC"], S["THIEN_DUC"] = m12(z + 1), m12(z + 5), m12(z + 9)
    S["TA_PHU"], S["HUU_BAT"] = m12(4 + (m - 1)), m12(10 - (m - 1))
    S["THIEN_HINH"], S["THIEN_RIEU"], S["THIEN_Y"] = m12(9 + (m - 1)), m12(1 + (m - 1)), m12(1 + (m - 1))
    S["DIA_GIAI"] = m12(7 + (m - 1))
    S["THIEN_GIAI"] = m12(8 + (m - 1)) if cfg["thienGiai"] == "THUAN" else m12(8 + 2 * (m - 1))
    S["VAN_KHUC"], S["VAN_XUONG"], S["DIA_KIEP"], S["DIA_KHONG"] = m12(4 + h), m12(10 - h), m12(11 + h), m12(11 - h)
    S["THAI_PHU"], S["PHONG_CAO"] = m12(S["VAN_KHUC"] + 2), m12(S["VAN_KHUC"] - 2)
    S["TAM_THAI"], S["BAT_TOA"] = m12(S["TA_PHU"] + (D - 1)), m12(S["HUU_BAT"] - (D - 1))
    S["AN_QUANG"], S["THIEN_QUY"] = m12(S["VAN_XUONG"] + (D - 2)), m12(S["VAN_KHUC"] - (D - 2))
    S["LONG_TRI"], S["PHUONG_CAC"], S["GIAI_THAN"] = m12(4 + z), m12(10 - z), m12(10 - z)
    S["THIEN_KHOC"], S["THIEN_HU"], S["HONG_LOAN"], S["THIEN_HY"] = m12(6 - z), m12(6 + z), m12(3 - z), m12(9 - z)
    S["THIEN_TAI"], S["THIEN_THO"], S["DAU_QUAN"] = m12(menh + z), m12(than + z), m12(z - (m - 1) + h)
    g = tam_hop_group(z)
    S["THIEN_MA"] = [CHI.index(x) for x in ["Dần", "Hợi", "Thân", "Tỵ"]][g]
    S["HOA_CAI"] = [CHI.index(x) for x in ["Thìn", "Sửu", "Tuất", "Mùi"]][g]
    S["KIEP_SAT"] = [CHI.index(x) for x in ["Tỵ", "Dần", "Hợi", "Thân"]][g]
    S["DAO_HOA"] = [CHI.index(x) for x in ["Dậu", "Ngọ", "Mão", "Tý"]][g]
    co = {"Hợi": "Dần", "Tý": "Dần", "Sửu": "Dần", "Dần": "Tỵ", "Mão": "Tỵ", "Thìn": "Tỵ", "Tỵ": "Thân", "Ngọ": "Thân",
          "Mùi": "Thân", "Thân": "Hợi", "Dậu": "Hợi", "Tuất": "Hợi"}[CHI[z]]
    S["CO_THAN"] = CHI.index(co)
    S["QUA_TU"] = m12(S["CO_THAN"] - 4)
    S["PHA_TOAI"] = CHI.index({"Tý": "Tỵ", "Ngọ": "Tỵ", "Mão": "Tỵ", "Dậu": "Tỵ", "Dần": "Dậu", "Thân": "Dậu", "Tỵ": "Dậu",
                               "Hợi": "Dậu"}.get(CHI[z], "Sửu"))
    S["THIEN_LA"], S["DIA_VONG"] = CHI.index("Thìn"), CHI.index("Tuất")
    role_at = {m12(menh - k): ROLES_CCW[k] for k in range(12)}
    pos_of_role = {v: k for k, v in role_at.items()}
    S["THIEN_THUONG"], S["THIEN_SU"] = pos_of_role["NO_BOC"], pos_of_role["TAT_ACH"]
    for name in ["THIEN_KHOI", "THIEN_VIET", "THIEN_QUAN", "THIEN_PHUC", "LUU_HA", "THIEN_TRU"]:
        S[name] = t["can"][name][c]
    if cfg["khoiViet"] == "TOAN_THU" and c == 6:
        S["THIEN_KHOI"], S["THIEN_VIET"] = CHI.index("Sửu"), CHI.index("Mùi")
    if cfg["luuHa"] == "LASOTUVI" and c == 3:
        S["LUU_HA"] = CHI.index("Thìn")
    if cfg["luuHa"] == "LASOTUVI" and c == 6:
        S["LUU_HA"] = CHI.index("Thân")
    S["VAN_TINH"], S["DUONG_PHU"], S["QUOC_AN"] = m12(lt + 3), m12(lt + 5), m12(lt + 8)
    hs, ls = t["hoaLinh"][z]
    if cfg["hoaLinh"] == "DAC_LOC" and g == 1:
        hs, ls = CHI.index("Tuất"), CHI.index("Mão")
    if cfg["hoaLinh"] == "TRUNG_CHAU":
        S["HOA_TINH"], S["LINH_TINH"] = m12(hs + h), m12(ls + h)
    else:
        S["HOA_TINH"], S["LINH_TINH"] = m12(hs + h * dirn), m12(ls - h * dirn)
    for hoa, target in tu_hoa(t, c, cfg).items():
        S["HOA_" + hoa] = S[target]

    def brightness(star, p):
        if star == "CU_MON" and cfg["cuMonTable"] == "CAM_NANG_V2":
            return {0: "Đ", 6: "Đ", 8: "V"}.get(p, t["bright"]["CU_MON"][p])
        if star in t["bright"]:
            return t["bright"][star][p]
        if star in t["phu"]:
            dac, other = t["phu"][star]
            return "Đ" if p in dac else other
        return None

    s_tuan = m12(z - c)
    tieu_start = CHI.index({0: "Tuất", 1: "Mùi", 2: "Thìn", 3: "Sửu"}[g])
    gs = 1 if nam else -1
    tieu_chi = {m12(tieu_start + k * gs): m12(z + k) for k in range(12)}
    palaces = []
    for p in range(12):
        i = next(i for i in range(12) if m12(menh + i * dirn) == p)
        palaces.append({"can": can_of(p), "role": role_at[p], "daiHan": cuc + 10 * i, "tieuHanChi": tieu_chi[p]})
    thanchu = ("Linh Tinh" if cfg["thanChuTy"] == "LINH_TINH" else "Hỏa Tinh") if z == 0 else THAN_CHU[CHI[z]]
    chart = {
        "menh": menh, "than": than, "cuc": cuc, "banMenh": nap_am(c, z),
        "menhChu": star_id(MENH_CHU[CHI[menh]]), "thanChu": star_id(thanchu),
        "tuan": [m12(s_tuan + 10), m12(s_tuan + 11)], "triet": list(t["triet"][c]),
        "palaces": palaces, "stars": S, "brightness": {k: brightness(k, v) for k, v in S.items()},
    }

    # ---------------- vận hạn (mục 8)
    v = rec["view"]
    VY, VM, VD, VH = v["year"], v["month"], v["day"], v["hour"]
    cy, zy = (VY + 6) % 10, (VY + 8) % 12
    tuoi = VY - Y + 1
    if tuoi < cuc:
        dai, nam_thu = menh, None
        lnd = menh
    else:
        i = ((tuoi - cuc) // 10) % 12
        dai = m12(menh + i * dirn)
        nam_thu = (tuoi - cuc) % 10 + 1
        xung = m12(dai + 6)
        lnd = {1: dai, 2: xung, 3: m12(xung - dirn), 4: xung}.get(nam_thu, m12(xung + (nam_thu - 4) * dirn))
    tuoi_el, dai_el = TAM_HOP_ELEM[tam_hop_group(z)], TAM_HOP_ELEM[tam_hop_group(dai)]
    if tuoi_el == dai_el:
        tt = "DONG_HANH"
    elif GEN[dai_el] == tuoi_el:
        tt = "DAI_HAN_SINH_TUOI"
    elif KHAC[tuoi_el] == dai_el:
        tt = "TUOI_KHAC_DAI_HAN"
    elif GEN[tuoi_el] == dai_el:
        tt = "TUOI_SINH_DAI_HAN"
    else:
        tt = "DAI_HAN_KHAC_TUOI"
    tieu = next(p for p, ch in tieu_chi.items() if ch == zy)
    llt = t["can"]["LOC_TON"][cy]
    gy = tam_hop_group(zy)
    luu = {
        "THAI_TUE": zy, "TANG_MON": m12(zy + 2), "BACH_HO": m12(zy + 8), "THIEN_KHOC": m12(6 - zy), "THIEN_HU": m12(6 + zy),
        "LOC_TON": llt, "KINH_DUONG": m12(llt + 1), "DA_LA": m12(llt - 1),
        "THIEN_MA": [CHI.index(x) for x in ["Dần", "Hợi", "Thân", "Tỵ"]][gy],
        "DAO_HOA": [CHI.index(x) for x in ["Dậu", "Ngọ", "Mão", "Tý"]][gy],
        "HONG_LOAN": m12(3 - zy), "THIEN_HY": m12(9 - zy), "HY_THAN": m12(llt + 7 * dirn),
        "LONG_DUC": m12(zy + 7), "NGUYET_DUC": m12(5 + zy),
    }

    def positions(can):
        return {k: S[v] for k, v in tu_hoa(t, can, cfg).items()}

    month_can = (2 * cy + 2 + VM - 1) % 10
    gieng = {
        "TIEU_HAN_NGHICH_THANG_THUAN_GIO": m12(tieu - (m - 1) + h),
        "TIEU_HAN_THUAN_THANG_THUAN_GIO": m12(tieu + (m - 1) + h),
        "TIEU_HAN_LA_THANG_GIENG": tieu,
        "DAU_QUAN_LUU_THAI_TUE": m12(zy - (m - 1) + h),
    }[cfg["nguyetHan"]]
    nguyet = m12(gieng + VM - 1)
    nhat = m12(nguyet + VD - 1)
    limits = {
        "tuoiAm": tuoi, "daiHan": dai, "tieuHan": tieu, "luuNienDaiHan": lnd, "namThu": nam_thu, "thienThoi": tt,
        "luuStars": luu, "luuTuHoa": positions(cy),
        "daiHanTuHoa": positions(can_of(dai)) if cfg["daiHanTuHoa"] == "ON" else None,
        "nguyetTuHoa": positions(month_can), "nguyetHan": nguyet, "nhatHan": nhat, "thoiHan": m12(nhat + VH), "luuMenh": zy,
    }
    return chart, limits


def main(dump_path, prompt_path):
    t = load_tables(prompt_path)
    total, diffs, example = 0, Counter(), {}
    for line in open(dump_path, encoding="utf-8"):
        if not line.startswith("{"):
            continue
        rec = json.loads(line)
        total += 1
        chart, limits = reference(rec, t)

        def cmp(key, ours, theirs):
            if ours != theirs:
                diffs[key] += 1
                example.setdefault(key, (rec, ours, theirs))

        for k in ["menh", "than", "cuc", "banMenh", "menhChu", "thanChu", "tuan", "triet", "palaces"]:
            cmp(k, chart[k], rec[k])
        for s, p in rec["stars"].items():
            cmp("sao " + s, chart["stars"].get(s), p)
        for s, b in rec["brightness"].items():
            cmp("độ sáng " + s, chart["brightness"].get(s), b)
        for k, val in limits.items():
            cmp("vận hạn " + k, val, rec["limits"][k])
    print(f"Đã so {total} lá số (kèm một thời điểm xem mỗi lá số) với bản tham chiếu độc lập.")
    if not diffs:
        print("KHỚP 100%: Mệnh, Thân, Cục, bản mệnh, Mệnh/Thân chủ, Tuần, Triệt, 12 cung, 109 sao, độ sáng, vận hạn năm/tháng/ngày/giờ.")
        return 0
    for key, n in diffs.most_common(40):
        rec, ref, eng = example[key]
        print(f"  LỆCH {key}: {n}/{total} — vd. {rec['lunarDay']}/{rec['lunarMonth']}/{rec['lunarYear']} giờ {rec['hour']} "
              f"{rec['gender']} {rec['config']}: tham chiếu {ref}, engine {eng}")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "docs/PROMPT_TUVI_APP.md"))
