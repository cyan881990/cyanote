#!/usr/bin/env bash
# Kiểm tra: bấm vào nội dung ghi chú có bật bàn phím không (nhiều tình huống).
PKG=app.cyanote
adb root || true; sleep 3; adb wait-for-device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
pip3 install --quiet pillow >/dev/null 2>&1 || true
python3 tools/demo/seed.py seed en
cp tools/demo/demo.m4a seed/demo.m4a
adb shell wm size 1080x2340; adb shell wm density 420
push() { adb push "$1" /data/local/tmp/x >/dev/null; adb shell "run-as $PKG sh -c 'mkdir -p $(dirname $2) && cat /data/local/tmp/x > $2'"; }
push seed/notes.json files/notes.json
push seed/appointments.json files/appointments.json
push seed/settings.xml shared_prefs/settings.xml
adb shell am start -W -n $PKG/vn.ghichunhanh.app.MainActivity; sleep 12
mkdir -p smoke-out/tap
# Đóng hộp thoại "… isn't responding" của hệ thống (máy ảo chậm) nếu có
dismiss() {
  for i in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
    xy=$(python3 - <<'PY'
import re, xml.etree.ElementTree as ET
for n in ET.parse("ui.xml").getroot().iter("node"):
    if n.get("text") in ("Wait", "Close app") and n.get("text") == "Wait":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds"))); print((x1+x2)//2, (y1+y2)//2); break
PY
)
    [ -z "$xy" ] && return
    adb shell input tap $xy; sleep 3
  done
}
sleep 20; dismiss
ime() { adb shell dumpsys input_method | grep -E "mInputShown|mIsInputViewShown|isInputViewShown|mServedView|mCurFocusedWindowClient" | head -6 | tr '\n' ' '; }
edit_xy() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1
  python3 - <<'PY'
import re, xml.etree.ElementTree as ET
for n in ET.parse("ui.xml").getroot().iter("node"):
    if n.get("class") == "android.widget.EditText":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2, "bounds", x1, y1, x2, y2, "focused", n.get("focused")); break
else:
    print("NONE")
PY
}
dismiss; r1=$(edit_xy); set -- $r1
echo "::warning title=edit bounds::$r1"
adb shell input tap $1 $2; sleep 3
adb exec-out screencap -p > smoke-out/tap/1-after-tap.png
echo "::warning title=1 after first tap::$(ime) || $(edit_xy)"
adb shell input keyevent 4; sleep 2
echo "::warning title=2 after back::$(ime)"
r2=$(edit_xy); set -- $r2
adb shell input tap $1 $2; sleep 3
adb exec-out screencap -p > smoke-out/tap/2-after-second-tap.png
echo "::warning title=3 after second tap::$(ime) || $(edit_xy)"
# Mở máy tính rồi bấm vào ghi chú
adb shell input keyevent 4; sleep 2
dismiss; r3=$(edit_xy); set -- $r3
adb shell input tap $(( $1 + 150 )) $(( $2 - 120 )); sleep 3
adb exec-out screencap -p > smoke-out/tap/3-tap-lower.png
echo "::warning title=4 tap lower area::$(ime) || $(edit_xy)"
# Quảng cáo toàn trang: tạo 6 ghi chú mới (cách nhau 20 giây) → phải hiện 2 lần
adb shell input keyevent 4; sleep 2
addxy=$(adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml >/dev/null 2>&1; python3 - <<'PY'
import re, xml.etree.ElementTree as ET
for n in ET.parse("ui.xml").getroot().iter("node"):
    if n.get("content-desc") == "New tab":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds"))); print((x1+x2)//2, (y1+y2)//2); break
PY
)
shown=0
for i in 1 2 3 4 5 6; do
  adb shell input tap $addxy; sleep 6
  top=$(adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1)
  echo "note $i: $top"; tops="$tops%0A$i: $(echo $top | cut -c1-140)"
  if echo "$top" | grep -q "AdActivity"; then shown=$((shown+1)); adb exec-out screencap -p > smoke-out/tap/ad-$i.png; adb shell input keyevent 4; sleep 3; fi
  sleep 14
done
echo "::warning title=interstitials shown::$shown (expected 2) add=$addxy $tops"
adlog=$(adb logcat -d | grep -iE " Ads |Ads  |interstitial|AdActivity|notes_created" | tail -25 | cut -c1-200 | awk 'BEGIN{ORS="%0A"}{print}')
echo "::warning title=ads log::$adlog"
cnt=$(adb shell "run-as $PKG cat shared_prefs/settings.xml" | grep -E "notes_created|interstitial" | tr '\n' ' ')
echo "::warning title=ad prefs::$cnt"
adb logcat -d | grep -E "FATAL|InputMethod|IME|ImeTracker" | tail -30 | cut -c1-200 > smoke-out/tap/log.txt
echo "::warning title=ime log::$(tail -25 smoke-out/tap/log.txt | awk 'BEGIN{ORS="%0A"}{print}')"
hdr=$(git config --get http.https://github.com/.extraheader)
rm -rf smoke-out/.git
( cd smoke-out && git init -q && git checkout -q -b tap-results && git add -A \
  && git -c user.name=cyanote-bot -c user.email=actions@users.noreply.github.com commit -qm "tap test" \
  && git -c http.extraheader="$hdr" push -qf "https://github.com/$GITHUB_REPOSITORY" tap-results ) || true
exit 0
