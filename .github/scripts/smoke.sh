#!/usr/bin/env bash
# Cài APK, mở app, đợi 25 giây rồi báo cáo: app còn chạy không, có crash/ANR không.
api="$1"
enc() { awk 'BEGIN{ORS="%0A"} {gsub(/%/,"%25"); gsub(/\r/,""); print}'; }
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n app.cyanote/vn.ghichunhanh.app.MainActivity
sleep 25
pid=$(adb shell pidof app.cyanote | tr -d '\r')
focus=$(adb shell dumpsys window | grep -E "mCurrentFocus|mFocusedApp" | head -3)
adb logcat -d > logcat.txt
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
[ -n "$pid" ] && [ -n "$pid2" ] && [ -z "$crash" ]
