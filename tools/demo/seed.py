"""Tạo dữ liệu mẫu (tiếng Việt) để chụp ảnh giới thiệu tính năng trên máy ảo.
Dùng: python3 seed.py <thư mục ra>  → notes.json, appointments.json, settings.xml, demo.jpg
"""
import json, os, sys, time, datetime as dt

out = sys.argv[1]
lang = sys.argv[2] if len(sys.argv) > 2 else "vi"
os.makedirs(out, exist_ok=True)
now = dt.datetime.now().replace(second=0, microsecond=0)
ms = lambda d: int(d.timestamp() * 1000)

def tag(d):
    return f"[⏰ {d.day}/{d.month} {d:%H:%M}]"

t_call = (now + dt.timedelta(hours=2)).replace(minute=0)
t_meet = (now + dt.timedelta(hours=3)).replace(minute=30)
t_tomorrow = (now + dt.timedelta(days=1)).replace(hour=9, minute=0)
t_soon = now + dt.timedelta(seconds=100)          # thông báo nổ trong lúc chụp
t_done = now - dt.timedelta(hours=3)

n1 = 1001
content1 = "\n".join([
    "Việc hôm nay",
    f"• Gọi anh Nam báo giá 0909 123 456 {tag(t_call)}",
    f"• Họp nhóm thiết kế bản 1.1 {tag(t_meet)}",
    "• Chuyển tiền nhà: 4.500.000 + 350.000 = 4.850.000",
    "• Chụp hoá đơn điện nước, gửi chủ nhà",
    "",
    "Biên bản họp nhanh:",
    "- Chốt giao diện tablet 2 cột",
    "- Thêm ghi âm và ảnh vào ghi chú",
    "- Nhắc lịch có chuông, bấm Gọi ngay trên thông báo",
    "- Kiểm tra số dòng & chế độ không tự xuống dòng: một dòng rất dài để thấy thanh cuộn ngang khi tắt tự xuống dòng ......",
    "",
    "Gọi chị Hoa đặt bánh sinh nhật 0987 654 321",
])
if lang == "en":
    content1 = "\n".join([
        "Today",
        f"• Call Nam about the quote 0909 123 456 {tag(t_call)}",
        f"• Design team meeting for v1.1 {tag(t_meet)}",
        "• Rent transfer: 4,500,000 + 350,000 = 4,850,000",
        "• Photo of the power bill, send to landlord",
        "",
        "Quick meeting notes:",
        "- Lock the two-column tablet layout",
        "- Add voice notes and photos to notes",
        "- Reminders with sound, Call right from the notification",
        "- Check line numbers & no-wrap mode: a very long line to show sideways scrolling when word wrap is off ......",
        "",
        "Call Hoa to order the birthday cake 0987 654 321",
    ])
notes = {
    "version": 1, "selectedId": n1, "monospace": False,
    "calc": {"expr": "4500000+350000", "justEvaluated": False, "lastFull": None},
    "notes": [
        {"id": n1, "title": "", "content": content1, "updatedAt": ms(now)},
        {"id": 1002, "title": "Clients" if lang == "en" else "Khách hàng", "content": "Lan – 0912 345 678\n" + ("House viewing on Saturday\n" if lang == "en" else "Hẹn xem nhà thứ Bảy\n"), "updatedAt": ms(now)},
        {"id": 1003, "title": "Shopping" if lang == "en" else "Mua sắm", "content": ("Milk, eggs, coffee\n" if lang == "en" else "Sữa, trứng, cà phê\n"), "updatedAt": ms(now)},
    ],
}
TT = {
    "en": ["Call Nam about the quote", "Design team meeting v1.1", "Show Lan the house", "Call the bank back", "Pay the power bill"],
    "vi": ["Gọi anh Nam báo giá", "Họp nhóm thiết kế bản 1.1", "Dẫn chị Lan xem nhà", "Gọi lại cho ngân hàng", "Nộp tiền điện"],
}[lang]
appts = [
    {"id": 2001, "title": TT[0], "time": ms(t_call), "phone": "0909123456", "noteId": n1, "remindBefore": 15, "sound": True, "done": False},
    {"id": 2002, "title": TT[1], "time": ms(t_meet), "phone": "", "noteId": n1, "remindBefore": 5, "sound": True, "done": False},
    {"id": 2003, "title": TT[2], "time": ms(t_tomorrow), "phone": "0912345678", "noteId": 1002, "remindBefore": 30, "sound": False, "done": False},
    {"id": 2004, "title": TT[3], "time": ms(t_soon), "phone": "1900545413", "noteId": 0, "remindBefore": 0, "sound": True, "done": False},
    {"id": 2005, "title": TT[4], "time": ms(t_done), "phone": "", "noteId": 0, "remindBefore": 0, "sound": True, "done": True},
]
json.dump(notes, open(os.path.join(out, "notes.json"), "w"), ensure_ascii=False)
json.dump(appts, open(os.path.join(out, "appointments.json"), "w"), ensure_ascii=False)
open(os.path.join(out, "settings.xml"), "w").write(f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="language">{lang}</string>
    <long name="update_last_check" value="{int(time.time()*1000)}" />
    <boolean name="word_wrap" value="true" />
    <boolean name="line_numbers" value="true" />
    <boolean name="dock_visible" value="true" />
    <string name="dock_mode">CALC</string>
</map>
""")

# Ảnh mẫu: "hoá đơn" vẽ bằng PIL
from PIL import Image, ImageDraw
img = Image.new("RGB", (900, 1200), (250, 248, 240))
d = ImageDraw.Draw(img)
d.rectangle([60, 60, 840, 1140], outline=(30, 30, 30), width=6)
d.rectangle([60, 60, 840, 220], fill=(11, 114, 133))
for i, y in enumerate(range(300, 1000, 90)):
    d.rectangle([110, y, 600 - (i % 3) * 90, y + 26], fill=(70, 70, 70))
    d.rectangle([680, y, 790, y + 26], fill=(201, 149, 46))
img.save(os.path.join(out, "demo.jpg"), quality=88)
img2 = Image.new("RGB", (1200, 900), (180, 220, 228))
d2 = ImageDraw.Draw(img2)
d2.polygon([(0, 900), (420, 380), (760, 720), (980, 520), (1200, 900)], fill=(16, 104, 118))
d2.ellipse([880, 120, 1040, 280], fill=(248, 220, 150))
img2.save(os.path.join(out, "demo2.jpg"), quality=88)
print("seed ok", now)
