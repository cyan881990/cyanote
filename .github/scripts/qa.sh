#!/usr/bin/env bash
# Kiểm thử bản phát hành (R8) trên một phiên bản Android: khởi động, thao tác chính, ghi chú dài, xoay màn hình.
api="$1"; PKG=app.cyanote; out=qa-out/api$api; mkdir -p "$out"
enc() { awk 'BEGIN{ORS="%0A"} {gsub(/%/,"%25"); gsub(/\r/,""); print}'; }
ui() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1; }
find_xy() {  # $1 = content-desc / text / "EDIT"
  ui; python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
w = sys.argv[1]
try: root = ET.parse("ui.xml").getroot()
except Exception: sys.exit()
for n in root.iter("node"):
    ok = (n.get("class") == "android.widget.EditText") if w == "EDIT" else (n.get("content-desc") == w or n.get("text") == w)
    if ok:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds"))); print((x1+x2)//2, (y1+y2)//2); break
PY
}
tap() { xy=$(find_xy "$1"); [ -n "$xy" ] && adb shell input tap $xy && return 0; echo "miss: $1"; return 1; }
dismiss_sys() { for i in 1 2; do xy=$(find_xy "Wait"); [ -z "$xy" ] && return; adb shell input tap $xy; sleep 2; done; }
steps=""
log() { steps="$steps%0A$1"; echo "$1"; }

adb wait-for-device
adb install -r app-qa.apk >/dev/null && log "install ok"
[ "$api" -ge 33 ] && adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null
adb logcat -c
start=$(adb shell am start -W -n $PKG/vn.ghichunhanh.app.MainActivity | grep -E "TotalTime|WaitTime" | tr -d '\r' | tr '\n' ' ')
log "cold start: $start"
sleep 15; dismiss_sys
adb exec-out screencap -p > $out/1-launch.png
# Gõ chữ
tap EDIT && sleep 2 && adb shell input text "Hello%scyanote" && sleep 1 && log "typed ok"
adb shell input keyevent 4; sleep 2
# Tạo ghi chú mới 2 lần
tap "New tab" && sleep 2 && log "new note 1"
adb shell input keyevent 4; sleep 1
tap "New tab" && sleep 2 && log "new note 2"
adb shell input keyevent 4; sleep 1
# Mở máy tính
tap "Open calculator" && sleep 2 && log "calculator opened"
for k in 7 "×" 8 "="; do tap "$k" >/dev/null; sleep 0.5; done
adb exec-out screencap -p > $out/2-calculator.png
# Ghi chú dài 1500 dòng (nhận qua "Chia sẻ")
TXT=$(python3 -c "print('\\\\n'.join(f'Line {i}: the quick brown fox jumps over the lazy dog {i*7}' for i in range(1,1501)))")
adb shell "am start -a android.intent.action.SEND -t text/plain -n $PKG/vn.ghichunhanh.app.MainActivity --es android.intent.extra.TEXT \"$(python3 -c "print('\\n'.join(f'Line {i}: the quick brown fox jumps over the lazy dog {i*7}' for i in range(1,1501)))" | sed 's/"/\\"/g')\"" >/dev/null 2>&1
sleep 5; dismiss_sys
adb exec-out screencap -p > $out/3-long-note.png
# Đo độ mượt khi cuộn
adb shell dumpsys gfxinfo $PKG reset >/dev/null
read W H < <(adb shell wm size | tail -1 | sed 's/.*: //; s/x/ /')
for i in $(seq 1 12); do adb shell input swipe $((W/2)) $((H*65/100)) $((W/2)) $((H*35/100)) 120; done
for i in $(seq 1 6); do adb shell input swipe $((W/2)) $((H*35/100)) $((W/2)) $((H*65/100)) 120; done
gfx=$(adb shell dumpsys gfxinfo $PKG | grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|99th percentile" | head -5 | tr -d '\r' | sed 's/^ *//' | tr '\n' ';')
log "scroll: $gfx"
adb exec-out screencap -p > $out/4-scrolled.png
# Xoay ngang rồi xoay lại
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1; sleep 5; dismiss_sys
adb exec-out screencap -p > $out/5-landscape.png
adb shell settings put system user_rotation 0; sleep 4
# Kết quả
pid=$(adb shell pidof $PKG | tr -d '\r')
adb logcat -d > $out/logcat.txt
crash=$(grep -E "FATAL EXCEPTION|ANR in $PKG" -A 20 $out/logcat.txt | grep -B2 -A18 "$PKG" | head -40 | cut -c1-200)
mem=$(adb shell dumpsys meminfo $PKG | grep -E "TOTAL PSS|TOTAL:" | head -1 | tr -s ' ')
log "memory: $mem"
log "alive pid: ${pid:-NONE}"
echo "::warning title=API $api QA::$steps"
if [ -n "$crash" ]; then echo "::error title=API $api crash::$(printf '%s' "$crash" | enc)"; fi
# Đẩy ảnh chụp lên nhánh qa-results
hdr=$(git config --get http.https://github.com/.extraheader)
( mkdir -p /tmp/qa && cd /tmp/qa && rm -rf .git && git init -q && git checkout -q -b qa-api$api && cp -r "$OLDPWD/$out" . \
  && git add -A && git -c user.name=cyanote-bot -c user.email=actions@users.noreply.github.com commit -qm "qa api$api" \
  && git -c http.extraheader="$hdr" push -qf "https://github.com/$GITHUB_REPOSITORY" qa-api$api ) || true
[ -n "$pid" ] && [ -z "$crash" ]
