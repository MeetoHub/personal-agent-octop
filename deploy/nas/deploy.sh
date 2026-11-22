#!/usr/bin/env bash
# 把 personal-agent 补丁部署到 NAS（fnOS）。
#
#   ./deploy/nas/deploy.sh --check            只读核对：本地 vs 生产（不写任何东西）
#   ./deploy/nas/deploy.sh --apply            备份 → 转 LF → 上传 → 双端 SHA 校验
#   ./deploy/nas/deploy.sh --apply --restart  再重启服务（需要 NAS_SUDO_PASS）
#
# 必填：NAS_HOST、NAS_USER
# 可选：NAS_PORT(4222)、SSH_KEY(~/.ssh/id_ed25519)、REMOTE_ROOT、BACKUP_ROOT、
#       REMOTE_OWNER(octop-native:AppUsers)、NAS_SUDO_PASS
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC_ROOT="$REPO_ROOT/src/octop"
FILES_LIST="$REPO_ROOT/deploy/nas/files.txt"

NAS_PORT="${NAS_PORT:-4222}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
REMOTE_ROOT="${REMOTE_ROOT:-/vol1/@appcenter/octop-native/site-packages/octop}"
BACKUP_ROOT="${BACKUP_ROOT:-/vol1/@appshare/octop-native/data/.octop/patches}"
REMOTE_OWNER="${REMOTE_OWNER:-octop-native:AppUsers}"

MODE=""
RESTART=0
for arg in "$@"; do
  case "$arg" in
    --check) MODE="check" ;;
    --apply) MODE="apply" ;;
    --restart) RESTART=1 ;;
    *) echo "未知参数：$arg" >&2; exit 2 ;;
  esac
done
[ -n "$MODE" ] || { echo "用法：$0 --check | --apply [--restart]" >&2; exit 2; }
[ -n "${NAS_HOST:-}" ] || { echo "缺少 NAS_HOST" >&2; exit 2; }
[ -n "${NAS_USER:-}" ] || { echo "缺少 NAS_USER" >&2; exit 2; }

# accept-new：首次连接时把主机密钥写进 known_hosts，之后变化即拒绝，避免每次都关校验。
SSH=(ssh -i "$SSH_KEY" -p "$NAS_PORT" -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 "$NAS_USER@$NAS_HOST")
rsh() { "${SSH[@]}" "$@"; }

echo "== 目标：$NAS_USER@$NAS_HOST:$REMOTE_ROOT"
rsh "test -d '$REMOTE_ROOT'" || { echo "远端目录不存在：$REMOTE_ROOT" >&2; exit 1; }

# 收集文件清单
PATHS=(); MODES=(); SRCS=()
while IFS='|' read -r p m s; do
  # 去掉 CRLF 残留，否则 install 会拿到 "644"
  p="${p%$''}"; m="${m%$''}"; s="${s%$''}"
  case "$p" in ''|'#'*) continue ;; esac
  PATHS+=("$p"); MODES+=("${m:-644}"); SRCS+=("${s:-src/octop/$p}")
done < "$FILES_LIST"
# 第 i 个文件在本地仓库的位置（可能是 baseline 覆盖）
local_file() { echo "$REPO_ROOT/${SRCS[$1]}"; }

# 1) 本地卫生检查：必须存在、必须 LF
bad=0
for i in "${!PATHS[@]}"; do
  f="$(local_file "$i")"
  if [ ! -f "$f" ]; then echo "缺失：$f" >&2; bad=1; continue; fi
  # -U 关掉 MSYS 的文本模式，否则 grep 会把 CR 吃掉、漏报 CRLF
  if grep -qU $'\r' "$f"; then echo "CRLF：${PATHS[$i]}（$f，先转 LF）" >&2; bad=1; fi
done
[ "$bad" -eq 0 ] || { echo "本地检查未通过，终止。" >&2; exit 1; }

sha_of() { sed 's/\r$//' "$1" | sha256sum | cut -d' ' -f1; }

if [ "$MODE" = "check" ]; then
  echo
  printf '%-42s %-16s %-16s %s\n' "文件" "本地" "生产" "状态"
  drift=0
  for i in "${!PATHS[@]}"; do
    p="${PATHS[$i]}"
    l=$(sha_of "$(local_file "$i")")
    r=$(rsh "sha256sum '$REMOTE_ROOT/$p' 2>/dev/null | cut -d' ' -f1" || true)
    if [ -z "$r" ]; then st="缺失"; drift=1
    elif [ "$l" = "$r" ]; then st="一致"
    else st="不同"; drift=1; fi
    printf '%-42s %-16s %-16s %s\n' "$p" "${l:0:12}" "${r:0:12}" "$st"
  done
  echo
  [ "$drift" -eq 0 ] && echo "全部一致：生产就是当前提交。" || echo "存在差异（部署前属正常）。"
  exit 0
fi

# 2) 备份
TS=$(date +%Y%m%d-%H%M%S)
BK="$BACKUP_ROOT/backup-$TS"
echo "== 备份到 $BK"
rsh "mkdir -p '$BK' && cd '$REMOTE_ROOT' && for p in ${PATHS[*]}; do [ -f \"\$p\" ] && mkdir -p \"$BK/\$(dirname \$p)\" && cp -p \"\$p\" \"$BK/\$p\"; done; echo ok" >/dev/null
rsh "cd '$BK' && find . -type f -exec sha256sum {} \; | sort -k2 > SHA256SUMS" >/dev/null
echo "   备份文件数：$(rsh "find '$BK' -type f | wc -l")"

# 3) 上传（逐文件，stdin 传输，避免路径引号问题）
echo "== 上传"
for i in "${!PATHS[@]}"; do
  p="${PATHS[$i]}"; m="${MODES[$i]}"
  rsh "mkdir -p '$REMOTE_ROOT/$(dirname "$p")'" >/dev/null
  "${SSH[@]}" "cat > /tmp/padeploy.tmp" < "$(local_file "$i")"
  rsh "install -m $m /tmp/padeploy.tmp '$REMOTE_ROOT/$p' && rm -f /tmp/padeploy.tmp" >/dev/null
  rsh "chown '$REMOTE_OWNER' '$REMOTE_ROOT/$p'" 2>/dev/null || echo "   警告：无法 chown $p（保留原属主）"
  echo "   $p -> $m"
done

# 4) 双端校验
echo "== 校验"
fail=0
for i in "${!PATHS[@]}"; do
  p="${PATHS[$i]}"
  l=$(sha_of "$(local_file "$i")")
  r=$(rsh "sha256sum '$REMOTE_ROOT/$p' | cut -d' ' -f1")
  if [ "$l" = "$r" ]; then printf '   OK   %s\n' "$p"; else printf '   FAIL %s\n' "$p"; fail=1; fi
done
[ "$fail" -eq 0 ] || { echo "SHA 不一致，回滚：tar xzf $BK.tgz -C $REMOTE_ROOT" >&2; exit 1; }

# 5) 可选重启
if [ "$RESTART" -eq 1 ]; then
  [ -n "${NAS_SUDO_PASS:-}" ] || { echo "--restart 需要 NAS_SUDO_PASS" >&2; exit 2; }
  echo "== 重启服务"
  printf '%s' "$NAS_SUDO_PASS" | "${SSH[@]}" "cat > /tmp/.octop_pw && chmod 600 /tmp/.octop_pw"
  rsh "sudo -S -p '' bash -s < /tmp/.octop_pw" <<'REMOTE'
OLD=$(pgrep -f "octop.cli.main run" | head -1)
[ -n "$OLD" ] && kill "$OLD"
for _ in $(seq 1 30); do kill -0 "$OLD" 2>/dev/null || break; sleep 1; done
[ -n "$OLD" ] && kill -9 "$OLD" 2>/dev/null || true
runuser -u octop-native -- env \
  PYTHONPATH=/vol1/@appcenter/octop-native/site-packages \
  HOME=/vol1/@appshare/octop-native/data \
  OCTOP_HOME=/vol1/@appshare/octop-native/data/.octop \
  OCTOP_DATA_DIR=/vol1/@appshare/octop-native/data \
  OCTOP_INSTALL_MODE=fpk-native \
  nohup /var/apps/python312/target/bin/python3.12 -m octop.cli.main run \
  --host 0.0.0.0 --port 8089 --log-level info > /tmp/octop_restart.log 2>&1 &
REMOTE
  rsh "rm -f /tmp/.octop_pw"
  echo "   等待 health…"
  for _ in $(seq 1 30); do
    code=$(rsh "curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8089/api/health" || true)
    [ "$code" = "200" ] && break; sleep 1
  done
  echo "   health=$code"
  [ "$code" = "200" ] || { echo "重启后 health 不是 200，检查 /tmp/octop_restart.log" >&2; exit 1; }
fi

echo
echo "完成。回滚："
echo "  ssh -i $SSH_KEY -p $NAS_PORT $NAS_USER@$NAS_HOST \"cp -a '$BK/.' '$REMOTE_ROOT/'\""
[ "$RESTART" -eq 1 ] && echo "  然后按同样方式重启服务。"
