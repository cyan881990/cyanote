"""Ghi thông tin bản mới vào update.json của trang web (app đọc file này để tự cập nhật).
Dùng: python3 tools/publish_update.py <thư mục trang web> <android|ios> <số build>
"""
import json
import os
import sys

DOWNLOAD = "https://cyan881990.github.io/download"


def main(site_dir: str, platform: str, number: str) -> None:
    n = int(number)
    path = os.path.join(site_dir, "update.json")
    data = {}
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    if platform == "android":
        data["android"] = {
            "versionCode": n,
            "versionName": f"1.0.{n}",
            "url": f"{DOWNLOAD}/cyanote-android.apk",
        }
    elif platform == "ios":
        data["ios"] = {
            "build": n,
            "version": f"1.0 ({n})",
            "url": f"{DOWNLOAD}/cyanote-ios-unsigned.ipa",
        }
    else:
        raise SystemExit("platform phải là android hoặc ios")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(json.dumps(data, indent=2))


if __name__ == "__main__":
    main(*sys.argv[1:4])
