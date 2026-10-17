#!/usr/bin/env bash
# In các dòng lỗi của xcodebuild thành một thông báo (annotation) để đọc được ngay trên trang Actions.
log="$1"; label="$2"
tail -n 30 "$log"
errors=$(grep -E "error:|fatal error|Could not resolve|unable to|cannot find|No such module|BUILD FAILED" "$log" \
  | sed -E 's#^/Users/runner/work/[^/]+/[^/]+/##' | sort -u | head -n 60)
if [ -n "$errors" ]; then
  # %0A = xuống dòng trong annotation
  msg=$(printf '%s' "$errors" | sed ':a;N;$!ba;s/%/%25/g;s/\r//g;s/\n/%0A/g')
  echo "::error title=${label} build errors::${msg}"
fi
