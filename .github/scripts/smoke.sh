#!/usr/bin/env bash
# Cài APK, mở app, đợi 25 giây rồi báo cáo: app còn chạy không, có crash/ANR không.
api="$1"
enc() { awk 'BEGIN{ORS="%0A"} {gsub(/%/,"%25"); gsub(/\r/,""); print}'; }
adb root || true; sleep 3; adb wait-for-device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n app.cyanote/vn.ghichunhanh.app.MainActivity
sleep 25
pid=$(adb shell pidof app.cyanote | tr -d '\r')
focus=$(adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" | head -3)
adb logcat -d > logcat.txt
# Ảnh màn hình + ngăn xếp luồng chính (để biết app đứng ở đâu)
mkdir -p smoke-out
adb exec-out screencap -p > smoke-out/api$api.png
if [ -n "$pid" ]; then
  adb shell debuggerd -j "$pid" > smoke-out/api$api-stacks.txt 2>&1 || true
  main=$(awk '/^"main"/{f=1} f{print} f&&/^$/{exit}' smoke-out/api$api-stacks.txt | head -60 | cut -c1-200)
  echo "::warning title=API $api main thread::$(printf '%s' "$main" | enc)"
fi
cp logcat.txt smoke-out/api$api-logcat.txt
crash=$(grep -E "FATAL EXCEPTION|AndroidRuntime|ANR in|Application Not Responding|Input dispatching timed out" -A 25 logcat.txt | head -80)
applog=""
[ -n "$pid" ] && applog=$(grep " $pid " logcat.txt | grep -vE " [VD] " | tail -60 | cut -c1-230)
echo "::warning title=API $api status::pid=${pid:-NONE}%0A$(printf '%s' "$focus" | enc)"
[ -n "$crash" ] && echo "::error title=API $api crash::$(printf '%s' "$crash" | cut -c1-230 | enc)"
echo "::warning title=API $api app log::$(printf '%s' "$applog" | enc)"
# Thử gõ để bật bàn phím, kiểm tra không crash
adb shell input tap 540 900; sleep 3; adb shell input text "12%2b3"; sleep 3
pid2=$(adb shell pidof app.cyanote | tr -d '\r')
echo "::warning title=API $api after typing::pid=${pid2:-NONE}"
adb exec-out screencap -p > smoke-out/api$api-typing.png
# Màn hình ngang (điện thoại xoay)
adb shell input keyevent 111; sleep 1
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1; sleep 4
adb exec-out screencap -p > smoke-out/api$api-landscape.png
dump_main() {  # in luồng chính nếu app đang treo
  p=$(adb shell pidof app.cyanote | tr -d '\r'); [ -z "$p" ] && return
  adb shell debuggerd -j "$p" > "smoke-out/api$api-$1-stacks.txt" 2>&1 || true
  m=$(awk '/^"main"/{f=1} f{print} f&&/^$/{exit}' "smoke-out/api$api-$1-stacks.txt" | grep -v "^  native:" | head -40 | cut -c1-200)
  echo "::warning title=API $api main thread ($1)::$(printf '%s' "$m" | enc)"
}
dump_main landscape
adb shell input tap 160 200; sleep 3
adb exec-out screencap -p > smoke-out/api$api-landscape-typing.png
adb shell input keyevent 111
adb shell settings put system user_rotation 0; sleep 3
# Máy tính bảng / điện thoại gập mở (giả lập màn hình lớn)
adb shell wm size 1600x2560; adb shell wm density 320; sleep 5
adb exec-out screencap -p > smoke-out/api$api-tablet.png
dump_main tablet
adb shell input tap 500 1200; sleep 3
adb exec-out screencap -p > smoke-out/api$api-tablet-typing.png
adb shell wm size reset; adb shell wm density reset
# Màn hình nhỏ
adb shell input keyevent 111
adb shell wm size 720x1280; adb shell wm density 320; sleep 5
adb shell input tap 360 700; sleep 3
adb exec-out screencap -p > smoke-out/api$api-small-typing.png
adb shell wm size reset; adb shell wm density reset
pid3=$(adb shell pidof app.cyanote | tr -d '\r')
echo "::warning title=API $api after resize::pid=${pid3:-NONE}"
# Lưu kết quả vào nhánh smoke-results (ghi đè mỗi lần) để đọc được từ ngoài
hdr=$(git config --get http.https://github.com/.extraheader)
( cd smoke-out && git init -q && git checkout -q -b smoke-results && git add -A \
  && git -c user.name=cyanote-bot -c user.email=actions@users.noreply.github.com commit -qm "smoke api$api run $GITHUB_RUN_NUMBER" \
  && git -c http.extraheader="$hdr" push -qf "https://github.com/$GITHUB_REPOSITORY" smoke-results ) || echo "::warning::could not push smoke results"
[ -n "$pid" ] && [ -n "$pid2" ] && [ -z "$crash" ]
