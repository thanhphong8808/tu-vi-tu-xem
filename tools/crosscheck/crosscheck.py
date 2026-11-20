#!/usr/bin/env python3
"""So engine Kotlin với lasotuvi (MIT, https://github.com/doanguyen/lasotuvi) trên lá số ngẫu nhiên.

Cách dùng (từ thư mục gốc dự án):
    git clone --depth 1 https://github.com/doanguyen/lasotuvi.git /tmp/lasotuvi
    ./gradlew :core:engine:dumpCharts -q --args="3000 20261120 lasotuvi" > /tmp/charts.jsonl
    python3 tools/crosscheck/crosscheck.py /tmp/charts.jsonl /tmp/lasotuvi

Nhập âm lịch trực tiếp cho cả hai bên để loại khác biệt lịch. Preset `lasotuvi` của dumpCharts bật các biến thể
lasotuvi dùng (Hỏa Linh Đắc Lộc, Lưu Hà, Thiên Giải nhảy cung, Tứ Hóa Canh/Nhâm theo cụ Thiên Lương).

Bỏ qua có chủ đích (lasotuvi lệch khẩu quyết kinh điển — xem docs/DECISIONS.md D-14):
- Thai, Dưỡng: lasotuvi đảo ngược thứ tự cuối vòng Tràng Sinh.
- Phá Toái: lasotuvi đặt Tý Ngọ Mão Dậu → Dậu, Dần Thân Tỵ Hợi → Tỵ (ngược khẩu quyết “Tý Ngọ Mão Dậu cư Tỵ…”).
- Thiên Khôi, Thiên Việt: lasotuvi đảo Khôi/Việt với tuổi Đinh, Mậu, Nhâm và đặt Canh ở Mùi/Sửu.
"""
import json
import re
import sys
from collections import Counter

IGNORED = {"THAI", "DUONG", "PHA_TOAI", "THIEN_KHOI", "THIEN_VIET"}
SPECIAL = {"saoBacSy": "BAC_SI", "saoQuanPhu3": "QUAN_PHU", "saoQuanPhu2": "QUAN_PHU_BS"}


def to_star_id(var_name):
    if var_name in SPECIAL:
        return SPECIAL[var_name]
    return re.sub(r"(?<!^)(?=[A-Z])", "_", var_name[3:]).upper()


def main(charts_path, lasotuvi_path):
    sys.path.insert(0, lasotuvi_path)
    from lasotuvi import App, Sao  # noqa: E402
    from lasotuvi.DiaBan import diaBan  # noqa: E402

    from lasotuvi.AmDuong import diaChi  # noqa: E402
    chi_names = [diaChi[i]["tenChi"] for i in range(1, 13)]
    id_to_star = {v.saoID: to_star_id(k) for k, v in vars(Sao).items() if isinstance(v, Sao.Sao)}
    total = 0
    diffs = Counter()
    examples = {}
    for line in open(charts_path, encoding="utf-8"):
        line = line.strip()
        if not line.startswith("{"):
            continue
        c = json.loads(line)
        total += 1
        gioi_tinh = 1 if c["gender"] == "NAM" else -1
        db = App.lapDiaBan(diaBan, c["day"], c["month"], c["year"], c["hour"] + 1, gioi_tinh, False, 7)
        theirs = {}
        for cung in db.thapNhiCung[1:]:
            for sao in cung.cungSao:
                theirs[id_to_star[sao["saoID"]]] = cung.cungSo - 1

        def record(key, ours, their):
            if ours != their:
                diffs[key] += 1
                examples.setdefault(key, (c, ours, their))

        record("Mệnh", c["menh"], db.cungMenh - 1)
        record("Thân", c["than"], db.cungThan - 1)
        for cung in db.thapNhiCung[1:]:
            record("đại hạn", c["daiHan"][str(cung.cungSo - 1)], cung.cungDaiHan)
            record("tiểu hạn", c["tieuHan"][str(cung.cungSo - 1)], chi_names.index(cung.cungTieuHan))
        for star, pos in c["stars"].items():
            if star in IGNORED:
                continue
            if star not in theirs:
                diffs["thiếu trong lasotuvi: " + star] += 1
                continue
            record(star, pos, theirs[star])

    print(f"Đã so {total} lá số.")
    if not diffs:
        print("KHỚP 100% Mệnh, Thân, đại hạn, tiểu hạn và mọi sao còn lại (bỏ qua: " + ", ".join(sorted(IGNORED)) + ").")
        return 0
    for key, count in diffs.most_common():
        c, ours, their = examples.get(key, (None, None, None))
        sample = f" — ví dụ {c['day']}/{c['month']}/{c['year']} giờ {c['hour']} {c['gender']}: engine {ours}, lasotuvi {their}" if c else ""
        print(f"  {key}: lệch {count}/{total}{sample}")
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "lasotuvi"))
