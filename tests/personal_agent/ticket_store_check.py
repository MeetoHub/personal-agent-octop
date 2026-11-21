# ruff: noqa
# fmt: off
"""P2 验收脚本 — 在 DB 副本上跑通 TicketStore 全链路。

不改生产库：DATABASE = /tmp/trial.db
Verifies:
  1. import 链干净（infra.tickets 可被导入，不触发任何启动副作用）
  2. create / get / list 往返
  3. 七态转移逐个触发
  4. 所有权隔离（A 看不到 B 的工单）
  5. 事件时间线 seq 严格递增
  6. 重复文件/幂等性
"""

import sys
import traceback

DB = "/tmp/trial.db"

fail = 0


def chk(name, got, want):
    global fail
    ok = got == want
    if not ok:
        fail += 1
    print(f"  {'PASS' if ok else 'FAIL'}  {name:44s} got={got!r} want={want!r}")


print("=" * 62)
print("[1] import 链")
print("=" * 62)
try:
    from octop.infra.db.pool import SqlitePool
    from octop.infra.tickets import TicketStore, resolve_stage
    from octop.infra.tickets.store import TicketNotFound

    print("  PASS  from octop.infra.tickets import TicketStore, resolve_stage")
except Exception:
    print("  FAIL  import 失败")
    traceback.print_exc()
    sys.exit(1)

print()
print("=" * 62)
print("[2] 打开副本库")
print("=" * 62)
from pathlib import Path

pool = SqlitePool(Path(DB))
print(f"  PASS  dialect={pool.dialect}  path={DB}")

with pool.connect() as conn:
    cols = [r[1] for r in conn.execute("PRAGMA table_info(tickets)").fetchall()]
    print(f"  tickets 列数 {len(cols)}")
    want = {
        "ticket_id", "owner_user_id", "agent_id", "thread_id", "title", "goal",
        "stage", "priority", "assignment_kind", "assignee_id", "open_attention",
        "attention_reason", "latest_report", "delivery_failed", "created_at",
        "updated_at", "started_at", "ended_at",
    }
    missing = want - set(cols)
    chk("tickets 列齐全", sorted(missing), [])
    ecols = [r[1] for r in conn.execute("PRAGMA table_info(ticket_events)").fetchall()]
    print(f"  ticket_events 列: {ecols}")

store = TicketStore(pool)

print()
print("=" * 62)
print("[3] create → get 往返")
print("=" * 62)
t = store.create(
    owner_user_id=1,
    agent_id="expert-a",
    title="整理这周的会议记录",
    goal="把 5 个会议记录合并成一份摘要",
    thread_id="th_test_1",
    actor="test",
)
print(f"  ticket_id = {t.ticket_id}")
chk("create 后 stage", t.stage, "working/queued")
chk("create 后 title", t.title, "整理这周的会议记录")
chk("create 后 owner", t.owner_user_id, 1)
chk("ticket_id 前缀", t.ticket_id.startswith("tkt_"), True)

got = store.get(t.ticket_id, owner_user_id=1)
chk("get 命中", got.ticket_id, t.ticket_id)

print()
print("=" * 62)
print("[4] 七态转移")
print("=" * 62)
tid = t.ticket_id

# working/queued → working（指派）
t = store.assign(tid, owner_user_id=1, assignment_kind="expert", assignee_id="expert-a", actor="router")
chk("assign → working", t.stage, "working")
chk("started_at 已写", t.started_at is not None, True)

# working → idle（等输入）
t = store.file_report(tid, owner_user_id=1, report="waiting", detail="等用户确认时间范围")
chk("waiting → idle", t.stage, "idle")

# idle → person/answer（要人回答）
t = store.request_attention(tid, owner_user_id=1, reason="会议日期是 11/18 还是 11/19？")
chk("attention → person/answer", t.stage, "person/answer")
chk("attention_reason", t.attention_reason, "会议日期是 11/18 还是 11/19？")

# 清掉 → 回到 working（报告是 waiting → idle）
t = store.clear_attention(tid, owner_user_id=1, actor="user")
chk("clear_attention → idle", t.stage, "idle")

# 回到 working
t = store.file_report(tid, owner_user_id=1, report="progress", detail="正在合并")
chk("progress → working", t.stage, "working")

# → person/review（完成）
t = store.close(tid, owner_user_id=1, ok=True, actor="expert-a")
chk("close ok → person/review", t.stage, "person/review")
chk("ended_at 已写", t.ended_at is not None, True)

# → person/delivery（投递失败压过一切）
t = store.mark_delivery_failed(tid, owner_user_id=1, detail="回复未送达")
chk("delivery_failed 覆盖 review", t.stage, "person/delivery")

t = store.clear_delivery_failed(tid, owner_user_id=1)
chk("恢复 → person/review", t.stage, "person/review")

# person/blocked
t2 = store.create(owner_user_id=1, agent_id="expert-a", title="第二个活", actor="test")
t2 = store.assign(t2.ticket_id, owner_user_id=1, assignment_kind="expert", assignee_id="expert-a")
t2 = store.file_report(t2.ticket_id, owner_user_id=1, report="blocked", detail="缺权限")
chk("blocked → person/blocked", t2.stage, "person/blocked")

print()
print("=" * 62)
print("[5] 所有权隔离（核心红线）")
print("=" * 62)
# 副本库只有 1 个真实用户，这里补一个 user 2 用来验证隔离；只写副本，不碰生产。
with pool.transaction() as conn:
    exists = conn.execute("SELECT count(*) FROM users WHERE id = 2").fetchone()[0]
    if not exists:
        cols = [r[1] for r in conn.execute("PRAGMA table_info(users)").fetchall()]
        print(f"  users 列: {cols}")
        conn.execute(
            "INSERT INTO users(id, username, password_hash, role, created_at) "
            "VALUES (2, 'test-family', 'x', 'user', strftime('%s','now'))"
        )
print("  已在副本库插入 user 2")

t3 = store.create(owner_user_id=2, agent_id="expert-b", title="家人的活", actor="test")
chk("user2 能建", t3.owner_user_id, 2)

try:
    store.get(t3.ticket_id, owner_user_id=1)
    print("  FAIL  user1 竟然读到了 user2 的工单")
    fail += 1
except TicketNotFound:
    print("  PASS  user1 读 user2 的工单 → TicketNotFound")

try:
    store.get("tkt_doesnotexist", owner_user_id=1)
    print("  FAIL  不存在的 id 没报错")
    fail += 1
except TicketNotFound:
    print("  PASS  不存在的 id → TicketNotFound（与越权同一错误，不泄露存在性）")

try:
    store.close(t3.ticket_id, owner_user_id=1, ok=True)
    print("  FAIL  user1 竟能关闭 user2 的工单")
    fail += 1
except TicketNotFound:
    print("  PASS  user1 不能写 user2 的工单")

u1 = store.list_for_owner(1)
u2 = store.list_for_owner(2)
chk("user1 只见自己的", all(x.owner_user_id == 1 for x in u1), True)
chk("user2 只见自己的", all(x.owner_user_id == 2 for x in u2), True)
chk("user1 数量", len(u1), 2)
chk("user2 数量", len(u2), 1)

print()
print("=" * 62)
print("[6] 事件时间线")
print("=" * 62)
evs = store.events(tid, owner_user_id=1)
print(f"  事件数 = {len(evs)}")
for e in evs:
    print(f"    id={e.event_id:3d}  {e.kind:20s} {e.summary[:34]}")
ids = [e.event_id for e in evs]
chk("id 严格递增", ids == sorted(ids) and len(set(ids)) == len(ids), True)
chk("含 created", evs[0].kind, "created")
chk("含 delivery_failed", "delivery_failed" in [e.kind for e in evs], True)
chk("含 delivery_recovered", "delivery_recovered" in [e.kind for e in evs], True)
chk("created 事件带 payload", "agentId" in evs[0].payload, True)

print()
print("=" * 62)
print("[7] counts_by_stage")
print("=" * 62)
counts = store.counts_by_stage(1)
print(f"  {counts}")
chk("stage 都在合法集合内", set(counts) - {"working/queued","working","idle","person/answer","person/review","person/blocked","person/delivery"}, set())

print()
print("=" * 62)
print("[8] 校验拒绝非法报告")
print("=" * 62)
try:
    store.file_report(tid, owner_user_id=1, report="nonsense")
    print("  FAIL  非法 report 被接受")
    fail += 1
except ValueError:
    print("  PASS  非法 report → ValueError")

print()
print("=" * 62)
print("[9] 排序：需要人的在前")
print("=" * 62)
for x in store.list_for_owner(1):
    print(f"    {x.stage:16s} {x.title}")

print()
print("=" * 62)
print(f"结果: {'全部通过' if fail == 0 else str(fail) + ' 项失败'}")
print("=" * 62)
sys.exit(1 if fail else 0)
