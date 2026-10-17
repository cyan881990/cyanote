#!/usr/bin/env bash
# Chụp ảnh giới thiệu tính năng (lịch hẹn, thông báo, ảnh & ghi âm, số dòng, tự xuống dòng) bằng dữ liệu mẫu.
api="$1"
set -x
PKG=app.cyanote
out=smoke-out/features; mkdir -p "$out"
shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$out/$1.png"; }
ui() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1; }
# Bấm vào phần tử có content-desc hoặc text = $1
tap() {
  ui
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
root = ET.parse("ui.xml").getroot()
for n in root.iter("node"):
    if n.get("content-desc") == want or n.get("text") == want:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
)
  [ -n "$xy" ] && adb shell input tap $xy || echo "::warning::not found: $1"
}

pip3 install --quiet pillow >/dev/null 2>&1 || true
python3 tools/demo/seed.py seed
cp tools/demo/demo.m4a seed/demo.m4a

adb shell wm size 1080x2340; adb shell wm density 420
adb shell am force-stop $PKG
push() { adb push "$1" /data/local/tmp/x >/dev/null; adb shell "run-as $PKG sh -c 'mkdir -p $(dirname $2) && cat /data/local/tmp/x > $2'"; }
push seed/notes.json files/notes.json
push seed/appointments.json files/appointments.json
push seed/settings.xml shared_prefs/settings.xml
push seed/demo.jpg files/attachments/1001/1700000000001.jpg
push seed/demo2.jpg files/attachments/1001/1700000000002.jpg
[ -f seed/demo.m4a ] && push seed/demo.m4a files/attachments/1001/1700000000003.m4a
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow || true
adb shell settings put system user_rotation 0
adb shell am start -W -n $PKG/vn.ghichunhanh.app.MainActivity
shot 01-main 12
# Bảng lịch hẹn
adb shell am start -n $PKG/vn.ghichunhanh.app.MainActivity --ez open_appointments true
shot 02-appointments 5
# Máy tính mở rộng
tap "Máy tính"; shot 03-calculator 3
tap "Thu gọn" ; sleep 1
# Gắn lịch hẹn từ dòng đang gõ (con trỏ đang ở dòng cuối: "Gọi chị Hoa … 0987 654 321")
tap "Thêm lịch hẹn"; shot 04-tag-appointment 3
adb shell input keyevent 4; sleep 1; adb shell input keyevent 4; sleep 1
# Thông báo lịch hẹn (đã hẹn sẵn ~100 giây sau khi tạo dữ liệu)
for i in $(seq 1 30); do adb shell dumpsys notification --noredact | grep -q "Gọi lại cho ngân hàng" && break; sleep 5; done
adb shell cmd statusbar expand-notifications; shot 05-notification 3
tap "Xong"; sleep 2
adb shell cmd statusbar collapse; sleep 2
# Ảnh & ghi âm
tap "Tùy chọn"; sleep 1; tap "Ảnh & ghi âm (3)"; shot 06-attachments 4
adb shell input keyevent 4; sleep 1
# Tắt tự xuống dòng
tap "Tự xuống dòng"; shot 07-no-wrap 3
tap "Tự xuống dòng"; sleep 1
# Ẩn bảng dưới → ghi chú rộng hơn
tap "Ẩn bảng"; shot 08-panel-hidden 3
tap "Hiện máy tính & lịch hẹn"; sleep 1
# Máy tính bảng: 2 cột, lịch hẹn bên phải
adb shell wm size 1600x2560; adb shell wm density 320; sleep 3
adb shell am start -n $PKG/vn.ghichunhanh.app.MainActivity --ez open_appointments true
shot 09-tablet 6
# Chế độ tối
adb shell wm size 1080x2340; adb shell wm density 420; sleep 2
tap "Chuyển sang nền tối"; shot 10-dark 4
adb shell wm size reset; adb shell wm density reset
pid=$(adb shell pidof $PKG | tr -d '\r'); echo "::warning title=features pid::${pid:-NONE}"
adb logcat -d | grep -E "FATAL EXCEPTION" -A 30 | head -60 > "$out/crash.txt" || true
[ -s "$out/crash.txt" ] && echo "::error title=feature crash::$(head -40 $out/crash.txt | cut -c1-200 | awk 'BEGIN{ORS="%0A"}{print}')"
# Đẩy kết quả
hdr=$(git config --get http.https://github.com/.extraheader)
rm -rf smoke-out/.git
( cd smoke-out && git init -q && git checkout -q -b smoke-results && git add -A \
  && git -c user.name=cyanote-bot -c user.email=actions@users.noreply.github.com commit -qm "features api$api" \
  && git -c http.extraheader="$hdr" push -qf "https://github.com/$GITHUB_REPOSITORY" smoke-results ) || echo "::warning::push failed"
exit 0
