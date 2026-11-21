#!/usr/bin/env bash
# 校验补丁锚点还在。上游合并后如果这些行被吃掉，这个脚本会红。
set -euo pipefail
fail=0
check() { # <文件> <正则> <说明>
  if grep -qE "$2" "$1"; then
    echo "OK    $3"
  else
    echo "MISS  $3   ($1)"
    fail=1
  fi
}

A=src/octop/api/app.py
M=src/octop/infra/agents/manager.py
E=src/octop/infra/errors.py

check "$A" '^        tickets,$'                                  "app.py 导入 tickets"
check "$A" '_RouterMount\(tickets\.router, "/api", \["tickets"\]\)' "app.py 挂载 tickets"
check "$A" '^        phone,$'                                    "app.py 导入 phone"
check "$A" '_RouterMount\(phone\.router, "/api", \["phone"\]\)'     "app.py 挂载 phone"

check "$M" 'build_phone_tools'                                   "manager.py 构建 phone_tools"
check "$M" 'merged_tools\.extend\(phone_tools\)'                 "manager.py 合并 phone_tools"
check "$M" '\*phone_tools'                                       "manager.py reserved 含 phone_tools"

check "$E" 'TICKET_NOT_FOUND'                                    "errors.py 含 TICKET_NOT_FOUND"

[ "$fail" -eq 0 ] && echo "锚点齐全。" || { echo "锚点缺失，合并上游时被覆盖了。" >&2; exit 1; }
