"""Ticket stage resolver — the 7-state machine from the DSH superagent prototype.

A ticket never stores its stage directly. It stores three raw facts written by the
expert that owns it:

* ``latest_report`` — progress / waiting / ready / blocked / failed / dropped / None
* ``open_attention`` — 1 when the expert needs the human to answer something
* ``delivery_failed`` — 1 when the reply could not be delivered
* ``assignment_kind``/``assignee_id`` — empty while the ticket has not been handed off

:func:`resolve_stage` folds those facts into one of seven stages. The order of the
checks *is* the state machine: first match wins, top to bottom. Keeping the stage
derived rather than stored means a ticket can never sit in an impossible state —
there is only ever one row of truth.

Stages (the ``working/…`` prefixes are canonical; the bare ones are not):

===============  =========================================================
stage            meaning
===============  =========================================================
person/delivery  the answer reached nobody — needs a human to resend
person/answer    the expert is blocked on a human answer
person/review    the expert says it is ready for a human to look at
person/blocked   the expert reported blocked or failed
working/queued   accepted but not yet handed to an executor
working          an executor is on it
idle             the executor asked for something and is waiting
===============  =========================================================

``dropped`` deserves a note: the prototype lists it among reports, but a dropped
ticket is not a stage a human needs to see — the work is simply gone. We map it to
``idle`` so it never claims live attention. See :data:`DROPPED_STAGE`.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

#: Reports an expert may file. ``None`` means "no report yet".
REPORTS: tuple[str, ...] = (
    "progress",
    "waiting",
    "ready",
    "blocked",
    "failed",
    "dropped",
)

#: The seven stages, in the order they are declared below (not the match order).
STAGES: tuple[str, ...] = (
    "working/queued",
    "working",
    "idle",
    "person/answer",
    "person/review",
    "person/blocked",
    "person/delivery",
)

#: Stages that wait on a human rather than on the machine.
PERSON_STAGES: frozenset[str] = frozenset(
    {"person/delivery", "person/answer", "person/review", "person/blocked"}
)

#: A dropped ticket is dead work, not live work. It parks in ``idle``.
DROPPED_STAGE = "idle"

#: Reports that mean the ticket is no longer moving on its own.
TERMINAL_REPORTS: frozenset[str] = frozenset({"ready", "blocked", "failed", "dropped"})


@dataclass(frozen=True, slots=True)
class TicketFacts:
    """The raw columns ``resolve_stage`` reads. Mirrors the ``tickets`` table.

    Built from a DB row by :meth:`from_row`; construct directly in tests.
    """

    latest_report: str | None = None
    open_attention: bool = False
    delivery_failed: bool = False
    assignment_kind: str | None = None
    assignee_id: str | None = None

    @classmethod
    def from_row(cls, row: object) -> TicketFacts:
        """Build from a ``sqlite3.Row`` / mapping without assuming its exact type."""

        def _get(key: str, default: object = None) -> object:
            try:
                return row[key]  # type: ignore[index]
            except (KeyError, IndexError):
                return default

        report = _get("latest_report")
        kind = _get("assignment_kind")
        assignee = _get("assignee_id")
        return cls(
            latest_report=str(report) if report else None,
            open_attention=bool(_get("open_attention", 0)),
            delivery_failed=bool(_get("delivery_failed", 0)),
            assignment_kind=str(kind) if kind else None,
            assignee_id=str(assignee) if assignee else None,
        )

    @property
    def assigned(self) -> bool:
        """True once an executor owns the ticket.

        An assignment needs a kind; ``assignee_id`` may legitimately be blank for
        an anonymous worker, so it is not part of this test.
        """

        return bool(self.assignment_kind and self.assignment_kind.strip())


def resolve_stage(facts: TicketFacts) -> str:
    """Fold the raw facts into one of :data:`STAGES`. First match wins.

    The order below is the whole state machine. Reordering these branches changes
    behaviour; the rationale for the current order:

    1. **delivery failure outranks everything.** If the human never received the
       words, no other status is true in any useful sense — they are looking at a
       ticket that silently said nothing.
    2. **an open question outranks a finished report.** The expert may have marked
       ``ready`` and *also* be waiting on an answer; surfacing the question is what
       unblocks progress.
    3. **ready → review.** Work is done; a human should look.
    4. **blocked/failed → person/blocked.** The expert gave up; only a human can
       restart it.
    5. **no assignment → working/queued.** Accepted, not yet picked up.
    6. **waiting → idle.** An executor is asking for input but nothing is blocked.
    7. **otherwise → working.** The default: something is moving.
    """

    if facts.delivery_failed:
        return "person/delivery"
    if facts.open_attention:
        return "person/answer"
    if facts.latest_report == "ready":
        return "person/review"
    if facts.latest_report in ("blocked", "failed"):
        return "person/blocked"
    if not facts.assigned:
        return "working/queued"
    if facts.latest_report == "waiting":
        return "idle"
    if facts.latest_report == "dropped":
        return DROPPED_STAGE
    return "working"


def is_person_stage(stage: str) -> bool:
    """True when the stage waits on the human rather than on the machine."""

    return stage in PERSON_STAGES


def validate_report(report: str | None) -> None:
    """Raise ``ValueError`` when *report* is not a known report value."""

    if report is None:
        return
    if report not in REPORTS:
        raise ValueError(f"unknown ticket report {report!r}; expected one of {list(REPORTS)}")


def stage_sort_key(stage: str) -> tuple[int, str]:
    """Sort person-blocked work first, then live work, then finished work.

    The tickets list renders in this order so that anything holding a human up is
    at the top where it will be seen.
    """

    priority = {
        "person/delivery": 0,
        "person/answer": 1,
        "person/blocked": 2,
        "person/review": 3,
        "working": 4,
        "working/queued": 5,
        "idle": 6,
    }
    return (priority.get(stage, 9), stage)


# Words that describe an *outcome the person will check later*, not an action. "整理一下
# 这周的会议记录" is a task; "整理是什么意思" is a question that happens to share a verb.
_TASK_VERBS = frozenset(
    {
        "整理", "汇总", "总结", "调研", "分析", "写", "起草", "生成", "制作", "做个",
        "翻译", "审核", "检查", "对比", "比较", "收集", "搜集", "筛选", "排期", "规划",
        "安排", "跟进", "监控", "盯着", "跑", "执行", "批量", "清理", "重构", "迁移",
        "部署", "测试", "改", "修", "升级", "导出", "导入", "同步", "备份", "抓取",
        "爬", "梳理", "拟定", "画", "设计",
    }
)

# Words that mark the utterance as a question about *how* or *what*, which the chat
# thread answers inline. These outrank task verbs when both appear.
_QUESTION_MARKERS = frozenset(
    {
        "是什么", "什么是", "为什么", "怎么", "怎样", "如何", "是什么意思", "吗", "呢",
        "区别", "哪个好", "能不能", "可不可以", "是不是", "有没有", "谁",
    }
)

# A task that finishes in one breath does not need a ticket. These bound the "long".
_LONG_HINTS = frozenset(
    {
        "这周", "本周", "上周", "这个月", "本月", "上个月", "今年", "去年", "季度",
        "全部", "所有", "每个", "逐个", "批量", "每天", "每周", "每月", "定期",
        "持续", "长期", "之后", "以后", "明天", "后天", "下周", "下个月",
        "然后", "接着", "再", "之后把", "同时",
    }
)


def is_long_task(text: str) -> bool:
    """Whether *text* should become a ticket instead of a chat reply.

    The product rule, per the person who asked for this: **a ticket is only created
    for work that is explicitly long**. A question, a greeting, or a single quick
    lookup stays in the thread where it belongs. Getting this wrong in the eager
    direction floods the list with noise, which is how task lists die.

    This is a *cheap first pass* the Router may use as a hint, never a decision on
    its own — the Router has the model and the conversation. It is deliberately
    conservative: when unsure it answers ``False``, because a missed ticket costs one
    extra round of clarification while a spurious one costs the person's trust in the
    list.

    Returns ``False`` for empty input.
    """

    s = text.strip()
    if not s:
        return False

    # A question is never a ticket, even when it contains a task verb.
    if any(m in s for m in _QUESTION_MARKERS):
        # ...unless it also plainly states a duration and an action, e.g.
        # "能帮我把这周所有会议整理一下吗" — a request phrased as a question.
        has_long = any(h in s for h in _LONG_HINTS)
        has_verb = any(v in s for v in _TASK_VERBS)
        return has_long and has_verb

    has_verb = any(v in s for v in _TASK_VERBS)
    has_long = any(h in s for h in _LONG_HINTS)

    # How many *distinct* action words appear. One action verb is how you ask for a
    # single quick thing ("写一个 hello world"). Two or more is a workflow: "调研……
    # 写个对比", "重构……清理掉". A person does not string actions together to ask for
    # something that takes five seconds.
    verbs = [v for v in _TASK_VERBS if v in s]

    if has_verb and has_long:
        return True
    if len(verbs) >= 2:
        return True
    if s.count("，") + s.count(",") + s.count("、") >= 2 and has_verb:
        return True
    if "然后" in s and "先" in s:
        return True
    # A count in the object bounds the work even without a time word: "200 张",
    # "500 条", "1200 行". Small numbers do not: "改一下第 3 页", "写一句 20 字",
    # "12 个" are single edits. The threshold is deliberately high — 50 is roughly
    # where a count stops meaning "one thing" and starts meaning "a pile".
    if has_verb:
        for num in re.findall(r"\d+", s):
            if int(num) >= 50:
                return True
        if any(m in s for m in ("三家", "两家", "多家", "几家", "几十", "上百", "整套", "全场", "所有", "全部")):
            return True
    return False
