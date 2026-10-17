#!/usr/bin/env bash
# In lỗi của xcodebuild thành annotation để đọc được ngay trên trang Actions.
log="$1"; label="$2"
encode() { awk 'BEGIN{ORS="%0A"} {gsub(/%/,"%25"); gsub(/\r/,""); print}'; }
errors=$(grep -E "^e: |error:|What went wrong|fatal error|Could not resolve|unable to|cannot find|No such module|failed|Failed" "$log" \
  | grep -v "^note:" | sed -E 's#(file://)?/(Users|home)/runner/work/[^/]+/[^/]+/##' | sort -u | head -n 60)
if [ -n "$errors" ]; then
  echo "::error title=${label} errors::$(printf '%s' "$errors" | encode)"
fi
# Luôn gửi kèm phần cuối nhật ký (cắt dòng dài)
echo "::warning title=${label} log tail::$(tail -n 45 "$log" | cut -c1-220 | sed -E 's#/Users/runner/work/[^/]+/[^/]+/##g' | encode)"
