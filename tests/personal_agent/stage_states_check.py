# ruff: noqa
# fmt: off
import sys, types, importlib.util

# stage.py 无外部依赖，直接加载
spec = importlib.util.spec_from_file_location("stage", "src/octop/infra/tickets/stage.py")
stage = importlib.util.module_from_spec(spec)
sys.modules['stage'] = stage
spec.loader.exec_module(stage)

F = stage.TicketFacts
R = stage.resolve_stage
fail = 0
def chk(name, got, want):
    global fail
    ok = got == want
    if not ok: fail += 1
    print(f"  {'PASS' if ok else 'FAIL'}  {name:38s} {got:16s} (want {want})")

print("=== 七态覆盖 ===")
chk("delivery_failed 压过一切",       R(F(delivery_failed=True, latest_report="ready", open_attention=True)), "person/delivery")
chk("open_attention 压过 ready",      R(F(open_attention=True, latest_report="ready", assignment_kind="expert")), "person/answer")
chk("ready -> review",                R(F(latest_report="ready", assignment_kind="expert")), "person/review")
chk("blocked -> person/blocked",      R(F(latest_report="blocked", assignment_kind="expert")), "person/blocked")
chk("failed  -> person/blocked",      R(F(latest_report="failed", assignment_kind="expert")), "person/blocked")
chk("未指派 -> working/queued",        R(F(assignment_kind=None)), "working/queued")
chk("assignment_kind 空串 -> queued", R(F(assignment_kind="")), "working/queued")
chk("已指派+无报告 -> working",         R(F(assignment_kind="expert")), "working")
chk("已指派+progress -> working",      R(F(assignment_kind="expert", latest_report="progress")), "working")
chk("已指派+waiting -> idle",          R(F(assignment_kind="expert", latest_report="waiting")), "idle")
chk("dropped -> idle",                R(F(assignment_kind="expert", latest_report="dropped")), "idle")

print()
print("=== 全 7 态可达 ===")
reachable = set()
for df in (0,1):
    for oa in (0,1):
        for rep in (None,"progress","waiting","ready","blocked","failed","dropped"):
            for ak in (None,"expert","team"):
                reachable.add(R(F(delivery_failed=bool(df), open_attention=bool(oa), latest_report=rep, assignment_kind=ak)))
missing = set(stage.STAGES) - reachable
extra = reachable - set(stage.STAGES)
print(f"  可达态: {len(reachable)}/7  缺失: {missing or '无'}  越界: {extra or '无'}")
if missing or extra: fail += 1

print()
print("=== is_person_stage ===")
for s in stage.STAGES:
    want = s.startswith("person/")
    chk(f"is_person_stage({s})", str(stage.is_person_stage(s)), str(want))

print()
print("=== validate_report ===")
for good in [None,"progress","waiting","ready","blocked","failed","dropped"]:
    try:
        stage.validate_report(good); print(f"  PASS  接受 {good!r}")
    except ValueError:
        print(f"  FAIL  误拒 {good!r}"); fail += 1
for bad in ["done","","READY","nonsense"]:
    try:
        stage.validate_report(bad); print(f"  FAIL  误接受 {bad!r}"); fail += 1
    except (ValueError, TypeError):
        print(f"  PASS  拒绝 {bad!r}")

print()
print("=== stage_sort_key 排序：需要人的排最前 ===")
order = sorted(stage.STAGES, key=stage.stage_sort_key)
print("  " + " < ".join(order))
expect_first = ["person/delivery","person/answer","person/blocked","person/review"]
if order[:4] != expect_first: print("  FAIL 前四位不符"); fail += 1
else: print("  PASS 前四位是人等项")

print()
print("=== from_row 容错（模拟 sqlite3.Row / dict）===")
row = {"latest_report":"ready","open_attention":1,"delivery_failed":0,"assignment_kind":"expert","assignee_id":"a1"}
f = F.from_row(row)
chk("from_row dict", R(f), "person/answer")
class FakeRow:
    def __init__(s,d): s.d=d
    def __getitem__(s,k):
        if k not in s.d: raise IndexError(k)
        return s.d[k]
f2 = F.from_row(FakeRow({"latest_report":None,"open_attention":0,"delivery_failed":0,"assignment_kind":None,"assignee_id":None}))
chk("from_row 缺列容错", R(f2), "working/queued")

print()
print(f"{'='*50}")
print(f"结果: {'全部通过' if fail==0 else f'{fail} 项失败'}")
sys.exit(1 if fail else 0)
