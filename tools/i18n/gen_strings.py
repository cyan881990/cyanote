"""Sinh res/values*/strings.xml từ translations.py. Dùng: python3 gen_strings.py <thư mục res>"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from translations import KEYS, T  # noqa: E402


def esc(s: str) -> str:
    s = s.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;")
    s = s.replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    if s.startswith("@") or s.startswith("?"):
        s = "\\" + s
    return s


def main(res_dir: str) -> None:
    base_formats = {k: sorted(set(re.findall(r"%\d\$[ds]", T["en"][k]))) for k in KEYS}
    for lang, table in T.items():
        missing = [k for k in KEYS if k not in table]
        if missing:
            raise SystemExit(f"{lang}: thiếu {missing}")
        if len(table["zodiac"]) != 12:
            raise SystemExit(f"{lang}: zodiac phải có 12 mục")
        for k in KEYS:
            got = sorted(set(re.findall(r"%\d\$[ds]", table[k])))
            if got != base_formats[k]:
                raise SystemExit(f"{lang}.{k}: tham số {got} khác bản tiếng Anh {base_formats[k]}")
        folder = "values" if lang == "en" else f"values-{lang}"
        os.makedirs(os.path.join(res_dir, folder), exist_ok=True)
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 "<!-- Tự sinh từ translations.py. Sửa ở đó rồi chạy lại gen_strings.py -->",
                 "<resources>"]
        for k in KEYS:
            lines.append(f'    <string name="{k}">{esc(table[k])}</string>')
        lines.append('    <string-array name="zodiac">')
        for z in table["zodiac"]:
            lines.append(f"        <item>{esc(z)}</item>")
        lines.append("    </string-array>")
        lines.append("</resources>")
        with open(os.path.join(res_dir, folder, "strings.xml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        print("ok", folder)


if __name__ == "__main__":
    main(sys.argv[1])
