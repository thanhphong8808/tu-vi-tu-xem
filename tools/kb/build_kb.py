"""Xuất knowledge base ra JSON (data/kb/src/main/resources/kb/). Chạy: python3 tools/kb/build_kb.py"""
import json, os, sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import kb_rules, kb_drafts  # noqa: E402

VERSION = "0.3.0-nhap"
ROOT = os.path.dirname(os.path.dirname(HERE))
OUT = os.path.join(ROOT, "data", "kb", "src", "main", "resources", "kb")

ROLE_VN = kb_drafts.ROLE_VN


def write(rel, obj):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)
        f.write("\n")
    return rel


def main():
    # Tên tiếng Việt cho luật Lưu Thái Tuế theo cung.
    for r in kb_rules.RULES:
        if r["id"].startswith("VH_LUU_THAI_TUE_"):
            role = r["id"].removeprefix("VH_LUU_THAI_TUE_")
            r["name"] = f"Lưu Thái Tuế vào cung {ROLE_VN[role]}"
    ids = [r["id"] for r in kb_rules.RULES]
    dup = [k for k, v in Counter(ids).items() if v > 1]
    if dup:
        sys.exit(f"Trùng ID luật: {dup}")

    files = {"rules": [], "texts": [], "advice": [], "faq": [], "meta": []}
    groups = {
        "cat_cach": ["CAT_CACH"], "hung_cach": ["HUNG_CACH"], "phan_vi": ["PHAN_VI"], "the_sao": ["THE_SAO"],
        "cung": ["CUNG"], "van_han": ["VAN_HAN"], "chuyen_de": ["CHUYEN_DE"],
    }
    for name, types in groups.items():
        rules = [r for r in kb_rules.RULES if r["type"] in types]
        files["rules"].append(write(f"rules/{name}.json", {"rules": rules}))

    files["texts"].append(write("texts/rules.json", {"texts": list(kb_rules.TEXTS.values())}))
    files["texts"].append(write("texts/cung.json", {"texts": list(kb_drafts.palace_texts().values())}))
    files["texts"].append(write("texts/chinh_tinh.json", {"texts": list(kb_drafts.main_texts().values())}))
    files["texts"].append(write("texts/dong_cung.json", {"texts": list(kb_drafts.combo_texts().values())}))
    files["texts"].append(write("texts/phu_tinh.json", {"texts": list(kb_drafts.minor_texts().values())}))
    files["advice"].append(write("advice/advice.json", {"advice": list(kb_rules.ADVICE.values())}))
    files["faq"].append(write("faq/faq.json", {"faq": kb_drafts.faq_entries()}))
    files["meta"].append(write("meta/sao.json", {"combos": kb_drafts.combo_meta(), "minor": kb_drafts.minor_meta()}))

    counts = {
        "rules": len(kb_rules.RULES),
        "rulesByType": dict(Counter(r["type"] for r in kb_rules.RULES)),
        "textsRules": len(kb_rules.TEXTS),
        "textsChinhTinh": len(kb_drafts.main_texts()),
        "textsDongCung": len(kb_drafts.combo_texts()),
        "textsPhuTinh": len(kb_drafts.minor_texts()),
        "phuTinh": len(kb_drafts.MINOR),
        "advice": len(kb_rules.ADVICE),
        "faq": len(kb_drafts.FAQ),
    }
    write("index.json", {"version": VERSION, "note": "Bản nháp: mọi mục reviewed=false cho tới khi người dùng duyệt (mục 9).",
                         "counts": counts, "files": files})
    print(json.dumps(counts, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
