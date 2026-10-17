"""Sinh Localizable.strings cho iOS từ cùng nguồn translations.py.
Dùng: python3 tools/i18n/gen_ios_strings.py ios/QuickNotes/Resources
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from translations import KEYS, T  # noqa: E402

# Tên thư mục Android → tên .lproj của iOS
LPROJ = {"en": "en", "vi": "vi", "es": "es", "pt": "pt", "fr": "fr", "de": "de", "in": "id",
         "th": "th", "ja": "ja", "ko": "ko", "zh-rCN": "zh-Hans", "hi": "hi", "ru": "ru"}


def esc(s: str) -> str:
    s = s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")
    return s.replace("%1$s", "%1$@").replace("%2$s", "%2$@")


def main(res_dir: str) -> None:
    for lang, table in T.items():
        folder = os.path.join(res_dir, LPROJ[lang] + ".lproj")
        os.makedirs(folder, exist_ok=True)
        lines = ["/* Tự sinh từ tools/i18n/translations.py — sửa ở đó rồi chạy lại gen_ios_strings.py */"]
        for k in KEYS:
            lines.append(f'"{k}" = "{esc(table[k])}";')
        for i, z in enumerate(table["zodiac"]):
            lines.append(f'"zodiac_{i}" = "{esc(z)}";')
        with open(os.path.join(folder, "Localizable.strings"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        # Tên app hiển thị dưới biểu tượng
        with open(os.path.join(folder, "InfoPlist.strings"), "w", encoding="utf-8") as f:
            f.write(f'"CFBundleDisplayName" = "{esc(table["app_name"])}";\n')
        print("ok", folder)


if __name__ == "__main__":
    main(sys.argv[1])
