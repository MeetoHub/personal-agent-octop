#!/usr/bin/env bash
# 只读体检：服务、健康、补丁哈希、数据库状态。不改任何东西。
# 用法：NAS_HOST=... NAS_USER=... ./deploy/nas/verify.sh
set -euo pipefail

NAS_PORT="${NAS_PORT:-4222}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
REMOTE_ROOT="${REMOTE_ROOT:-/vol1/@appcenter/octop-native/site-packages/octop}"
DB="${DB:-/vol1/@appshare/octop-native/data/.octop/octop.db}"
PY="${PY:-/var/apps/python312/target/bin/python3.12}"

[ -n "${NAS_HOST:-}" ] && [ -n "${NAS_USER:-}" ] || { echo "需要 NAS_HOST / NAS_USER" >&2; exit 2; }

ssh -i "$SSH_KEY" -p "$NAS_PORT" -o BatchMode=yes -o ConnectTimeout=10 "$NAS_USER@$NAS_HOST" "
P='$REMOTE_ROOT'
echo '== 进程 =='
pgrep -af 'octop.cli.main run' | tail -2 || echo 'NOT RUNNING'
echo '== 端点 =='
curl -s -o /dev/null -w 'health=%{http_code}\n' http://127.0.0.1:8089/api/health
curl -s -o /dev/null -w 'phone_status=%{http_code} (401=正常，需 token)\n' http://127.0.0.1:8089/api/phone/status
echo '== 补丁文件 =='
sha256sum \$P/api/app.py \$P/infra/errors.py \$P/infra/agents/manager.py \
  \$P/api/routers/tickets.py \$P/api/routers/phone.py \
  \$P/infra/tickets/*.py \$P/infra/phone/*.py 2>&1
echo '== 数据库 =='
'$PY' - <<'PY' 2>&1 | tail -6
import sqlite3
c = sqlite3.connect('file:$DB?mode=ro', uri=True)
print('schema_version', c.execute('select version from _schema_version').fetchone()[0])
print('tables', c.execute(\"select count(*) from sqlite_master where type='table'\").fetchone()[0])
print('tickets/events', c.execute('select count(*) from tickets').fetchone()[0],
      c.execute('select count(*) from ticket_events').fetchone()[0])
print('integrity', c.execute('pragma integrity_check').fetchone()[0])
PY
echo '== 最近备份 =='
ls -1dt /vol1/@appshare/octop-native/data/.octop/patches/*/ 2>/dev/null | head -3
"
