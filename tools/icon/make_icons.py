"""Vẽ logo cyanote++ (bản cao cấp) và xuất mọi kích thước cho Android, iOS và cửa hàng.
Dùng: python3 tools/icon/make_icons.py   (chạy từ thư mục gốc của kho)

Ý tưởng: chữ "c" cách điệu màu ngọc trai + "++" vàng ánh kim trên nền xanh cyan đậm → "c++".
Hình học dùng hệ toạ độ 108×108 của adaptive icon Android (khớp với ic_launcher_foreground.xml).
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SS = 4

BG_TOP = (16, 104, 118)     # #106876
BG_BOTTOM = (3, 30, 36)     # #031E24
PEARL_A = (255, 255, 255)
PEARL_B = (196, 232, 238)   # #C4E8EE
GOLD_A = (248, 220, 150)    # #F8DC96
GOLD_B = (201, 149, 46)     # #C9952E

# Hình học (hệ 108)
PAGE = (31.0, 39.0, 67.0, 83.0)   # trái, trên, phải, dưới
PAGE_R = 5.0
FOLD = 10.0
LINES = [(38.0, 54.0, 20.0), (38.0, 62.0, 22.0), (38.0, 70.0, 13.0)]  # x, y, dài
LINE_H = 3.4
LINE_COLOR = (11, 114, 133)
PLUS = [(66.5, 31.0), (75.0, 31.0)]
PLUS_ARM = 2.6
PLUS_W = 2.2


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def linear_gradient(size, c1, c2, angle_deg=45):
    """Gradient chéo từ góc trên-trái (c1) đến dưới-phải (c2)."""
    w, h = size
    small = Image.new("RGB", (64, 64))
    px = small.load()
    a = math.radians(angle_deg)
    dx, dy = math.cos(a), math.sin(a)
    for y in range(64):
        for x in range(64):
            t = ((x / 63) * dx + (y / 63) * dy) / (dx + dy)
            px[x, y] = lerp(c1, c2, max(0.0, min(1.0, t)))
    return small.resize((w, h), Image.BICUBIC)


def mark_masks(size, sx, sy, scale):
    """Mặt nạ: trang giấy, góc gấp, dòng chữ, dấu ++."""
    P = lambda x, y: ((x - sx) * scale, (y - sy) * scale)  # noqa: E731
    l, t, r, b = PAGE
    page = Image.new("L", size, 0)
    d = ImageDraw.Draw(page)
    d.rounded_rectangle([*P(l, t), *P(r, b)], radius=PAGE_R * scale, fill=255)
    d.polygon([P(r - FOLD, t - 0.5), P(r + 0.5, t - 0.5), P(r + 0.5, t + FOLD)], fill=0)  # cắt góc
    fold = Image.new("L", size, 0)
    ImageDraw.Draw(fold).polygon([P(r - FOLD, t - 0.5), P(r - FOLD, t + FOLD), P(r + 0.5, t + FOLD)], fill=255)
    lines = Image.new("L", size, 0)
    dl = ImageDraw.Draw(lines)
    for (x, y, w) in LINES:
        dl.rounded_rectangle([*P(x, y), *P(x + w, y + LINE_H)], radius=LINE_H / 2 * scale, fill=255)
    plus = Image.new("L", size, 0)
    d2 = ImageDraw.Draw(plus)
    for (px_, py_) in PLUS:
        x, y = P(px_, py_)
        arm, th = PLUS_ARM * scale, PLUS_W * scale / 2
        d2.rounded_rectangle([x - arm - th, y - th, x + arm + th, y + th], radius=th, fill=255)
        d2.rounded_rectangle([x - th, y - arm - th, x + th, y + arm + th], radius=th, fill=255)
    return page, fold, lines, plus


def compose(size, sx, sy, scale, background=True, rounded=False):
    big = (size * SS, size * SS)
    sc = scale * SS
    if background:
        img = linear_gradient(big, BG_TOP, BG_BOTTOM).convert("RGBA")
        # Ánh sáng nhẹ ở góc trên-trái cho cảm giác có chiều sâu
        glow = Image.new("L", big, 0)
        ImageDraw.Draw(glow).ellipse([-big[0] * 0.35, -big[1] * 0.45, big[0] * 0.75, big[1] * 0.55], fill=70)
        glow = glow.filter(ImageFilter.GaussianBlur(big[0] * 0.12))
        img = Image.composite(Image.new("RGBA", big, (120, 210, 222, 255)), img, glow.point(lambda v: v // 3))
    else:
        img = Image.new("RGBA", big, (0, 0, 0, 0))
    page, fold, lines, plus = mark_masks(big, sx, sy, sc)
    # Bóng đổ mềm dưới trang giấy
    body = Image.composite(Image.new("L", big, 255), page, fold)
    shadow = body.filter(ImageFilter.GaussianBlur(sc * 2.2)).point(lambda v: int(v * 0.45))
    shadow = Image.composite(Image.new("RGBA", big, (0, 8, 12, 255)), Image.new("RGBA", big, (0, 0, 0, 0)), shadow)
    shadow = shadow.transform(big, Image.AFFINE, (1, 0, 0, 0, 1, -sc * 1.8))
    img = Image.alpha_composite(img, shadow)
    pearl = linear_gradient(big, PEARL_A, PEARL_B).convert("RGBA")
    gold = linear_gradient(big, GOLD_A, GOLD_B, angle_deg=60).convert("RGBA")
    img.paste(pearl, (0, 0), page)
    img.paste(gold, (0, 0), fold)
    img.paste(Image.new("RGBA", big, LINE_COLOR + (255,)), (0, 0), lines)
    img.paste(gold, (0, 0), plus)
    img = img.resize((size, size), Image.LANCZOS)
    if rounded:
        mask = Image.new("L", big, 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, big[0] - 1, big[1] - 1], radius=int(big[0] * 0.225), fill=255)
        img.putalpha(mask.resize((size, size), Image.LANCZOS))
    return img


def full_icon(size, rounded=False):
    span = 72  # phóng vùng 76×76 quanh tâm biểu tượng
    cx, cy = 54.0, 56.0
    return compose(size, cx - span / 2, cy - span / 2, size / span, rounded=rounded)


def font(path_candidates, size):
    for p in path_candidates:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


GF = "/usr/share/fonts/truetype/google-fonts/"
WORD_FONT = [GF + "Poppins-Medium.ttf", "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf"]
SUB_FONT = [GF + "Poppins-Light.ttf", "/usr/share/fonts/opentype/inter/Inter-Regular.otf"]


def feature_graphic():
    """Ảnh bìa Google Play 1024×500."""
    W, H = 1024 * SS, 500 * SS
    img = linear_gradient((W, H), (10, 84, 96), BG_BOTTOM, angle_deg=20).convert("RGBA")
    icon = full_icon(250, rounded=True).resize((250 * SS, 250 * SS), Image.LANCZOS)
    # viền vàng mảnh quanh biểu tượng
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([86 * SS, 121 * SS, 344 * SS, 379 * SS], radius=60 * SS, outline=(201, 149, 46, 255), width=2 * SS)
    img.alpha_composite(icon, (90 * SS, 125 * SS))
    f1, f2 = font(WORD_FONT, 104 * SS), font(SUB_FONT, 34 * SS)
    x = 400 * SS
    d.text((x, 138 * SS), "cyanote", font=f1, fill=(255, 255, 255, 255))
    w = d.textlength("cyanote", font=f1)
    d.text((x + w + 4 * SS, 138 * SS), "++", font=f1, fill=GOLD_A + (255,))
    d.line([(x + 4 * SS, 292 * SS), (x + 120 * SS, 292 * SS)], fill=GOLD_B + (255,), width=2 * SS)
    d.text((x + 4 * SS, 312 * SS), "Notes · Calculator · Lunar calendar", font=f2, fill=(205, 232, 237, 255))
    return img.convert("RGB").resize((1024, 500), Image.LANCZOS)


def save(img, *parts):
    path = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("ok", os.path.relpath(path, ROOT), img.size)


def main():
    appicon_dir = os.path.join(ROOT, "ios/QuickNotes/Resources/Assets.xcassets/AppIcon.appiconset")
    save(full_icon(1024).convert("RGB"), "ios", "QuickNotes", "Resources", "Assets.xcassets", "AppIcon.appiconset", "icon-1024.png")
    with open(os.path.join(appicon_dir, "Contents.json"), "w") as f:
        json.dump({"images": [{"filename": "icon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
                   "info": {"author": "xcode", "version": 1}}, f, indent=2)
    with open(os.path.join(ROOT, "ios/QuickNotes/Resources/Assets.xcassets/Contents.json"), "w") as f:
        json.dump({"info": {"author": "xcode", "version": 1}}, f, indent=2)
    save(full_icon(512).convert("RGB"), "store", "play-icon-512.png")
    save(feature_graphic(), "store", "play-feature-graphic-1024x500.png")
    save(full_icon(256, rounded=True), "docs", "logo.png")
    save(full_icon(1024, rounded=True), "store", "logo-1024-rounded.png")


if __name__ == "__main__":
    main()
