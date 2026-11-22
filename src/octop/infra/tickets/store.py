"""Ticket persistence — one row of truth per ticket, plus an append-only event log.

Two tables back this module (``infra/db/migrations/026_tickets.sql``):

``tickets``
    One row per unit of delegated work. The **stage is never stored**; it is derived
    from four raw columns by :func:`octop.infra.tickets.stage.resolve_stage`. Writing a
    stage into the table would let it drift out of sync with the facts, so we don't.

``ticket_events``
    Append-only timeline. Every mutation writes one row, so the ticket's history can be
    replayed and the mobile UI can show "what happened" without diffing snapshots.

Tenancy
-------
Every read and every write is scoped to ``owner_user_id``. This is **stricter than the
rest of Octop**: :func:`octop.api.common.agent.assert_agent_owner` lets admins bypass
ownership checks, but tickets deliberately do not. A ticket holds a private
conversation and the family members on this box each own their own; an admin looking at
their own list should not see their spouse's. There is no ``include_others`` flag —
adding one would mean every call site could leak, so instead the only way to read
another user's ticket is to already hold its id **and** be its owner.

Callers that need an admin-wide view (none today) must query the repo with the target
``owner_user_id`` explicitly, which makes the intent visible at the call site.
"""

from __future__ import annotations

import json
import uuid
from dataclasses import dataclass, field
from typing import Any, cast

from octop.infra.db.pool import DatabasePool
from octop.infra.db.repos._base import DbRow, now_ts
from octop.infra.tickets.stage import (
    TicketFacts,
    resolve_stage,
    stage_sort_key,
    validate_report,
)

_TICKET_COLS = (
    "ticket_id, owner_user_id, agent_id, thread_id, title, goal, stage, priority, "
    "assignment_kind, assignee_id, open_attention, attention_reason, latest_report, "
    "delivery_failed, created_at, updated_at, started_at, ended_at"
)

#: ``kind`` values for :meth:`TicketStore.append_event`.
EVENT_KINDS: tuple[str, ...] = (
    "created",
    "assigned",
    "report",
    "attention",
    "attention_cleared",
    "delivery_failed",
    "delivery_recovered",
    "completed",
    "failed",
    "note",
)


class TicketNotFound(LookupError):
    """No ticket with that id belongs to that owner.

    Deliberately the *same* error whether the ticket is missing or owned by somebody
    else: distinguishing the two would confirm the existence of another user's ticket.
    """


@dataclass(frozen=True, slots=True)
class Ticket:
    """A ticket row. ``stage`` is computed, never read from the table."""

    ticket_id: str
    owner_user_id: int
    agent_id: str
    title: str
    stage: str
    goal: str = ""
    thread_id: str | None = None
    priority: int = 0
    assignment_kind: str = "expert"
    assignee_id: str | None = None
    open_attention: bool = False
    attention_reason: str | None = None
    latest_report: str | None = None
    delivery_failed: bool = False
    created_at: int = 0
    updated_at: int = 0
    started_at: int | None = None
    ended_at: int | None = None

    @classmethod
    def from_row(cls, row: DbRow) -> Ticket:
        facts = TicketFacts.from_row(row)
        # Trust the resolver over any stored ``stage`` column: the column exists for
        # indexing only and is refreshed by the store on every write.
        return cls(
            ticket_id=str(row["ticket_id"]),
            owner_user_id=int(row["owner_user_id"]),
            agent_id=str(row["agent_id"]),
            title=str(row["title"]),
            stage=resolve_stage(facts),
            goal=str(row["goal"] or ""),
            thread_id=str(row["thread_id"]) if row["thread_id"] else None,
            priority=int(row["priority"] or 0),
            assignment_kind=str(row["assignment_kind"] or "expert"),
            assignee_id=str(row["assignee_id"]) if row["assignee_id"] else None,
            open_attention=bool(row["open_attention"]),
            attention_reason=str(row["attention_reason"]) if row["attention_reason"] else None,
            latest_report=str(row["latest_report"]) if row["latest_report"] else None,
            delivery_failed=bool(row["delivery_failed"]),
            created_at=int(row["created_at"]),
            updated_at=int(row["updated_at"]),
            started_at=int(row["started_at"]) if row["started_at"] else None,
            ended_at=int(row["ended_at"]) if row["ended_at"] else None,
        )

    def to_dict(self) -> dict[str, Any]:
        """Wire shape. Keys are stable; the mobile client depends on them."""

        return {
            "ticketId": self.ticket_id,
            "ownerUserId": self.owner_user_id,
            "agentId": self.agent_id,
            "threadId": self.thread_id,
            "title": self.title,
            "goal": self.goal,
            "stage": self.stage,
            "priority": self.priority,
            "assignmentKind": self.assignment_kind,
            "assigneeId": self.assignee_id,
            "openAttention": self.open_attention,
            "attentionReason": self.attention_reason,
            "latestReport": self.latest_report,
            "deliveryFailed": self.delivery_failed,
            "createdAt": self.created_at,
            "updatedAt": self.updated_at,
            "startedAt": self.started_at,
            "endedAt": self.ended_at,
        }


@dataclass(frozen=True, slots=True)
class TicketEvent:
    """One timeline entry.

    The table stores ``summary`` plus a ``payload_json`` blob. ``payload`` is kept as a
    parsed dict on the object so callers never touch raw JSON, and ``summary`` is the
    already-rendered one-liner the UI can show without understanding the payload.
    """

    event_id: int
    ticket_id: str
    kind: str
    at: int
    summary: str = ""
    actor: str = ""
    payload: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_row(cls, row: DbRow) -> TicketEvent:
        raw = row["payload_json"]
        try:
            payload = json.loads(raw) if raw else {}
        except (TypeError, ValueError):
            payload = {"_unparsed": str(raw)}
        if not isinstance(payload, dict):
            payload = {"_value": payload}
        return cls(
            event_id=int(row["id"]),
            ticket_id=str(row["ticket_id"]),
            kind=str(row["kind"]),
            at=int(row["created_at"]),
            summary=str(row["summary"] or ""),
            actor=str(row["actor"] or ""),
            payload=payload,
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "id": self.event_id,
            "ticketId": self.ticket_id,
            "kind": self.kind,
            "at": self.at,
            "summary": self.summary,
            "actor": self.actor,
            "payload": self.payload,
        }


def new_ticket_id() -> str:
    """Opaque, unguessable, and sortable by creation time (ULID-shaped prefix).

    The id is exposed on the wire, so it must not leak the owner or the row id. A
    UUID4 hex would do that but loses ordering; this keeps a time prefix so a raw
    ``ORDER BY ticket_id`` roughly follows creation.
    """

    return f"tkt_{now_ts():08x}{uuid.uuid4().hex[:16]}"


class TicketStore:
    """All ticket reads and writes. Every method takes an ``owner_user_id``."""

    def __init__(self, db: DatabasePool) -> None:
        self._db = db

    # ---------------------------------------------------------------- create

    def create(
        self,
        *,
        owner_user_id: int,
        agent_id: str,
        title: str,
        goal: str = "",
        thread_id: str | None = None,
        priority: int = 0,
        assignment_kind: str | None = None,
        assignee_id: str | None = None,
        actor: str = "",
    ) -> Ticket:
        """Insert a new ticket and log a ``created`` event.

        ``assignment_kind`` defaults to ``None`` on purpose: filing a ticket is not the
        same act as handing it to an executor. A ticket created here lands in
        ``working/queued``; call :meth:`assign` to move it to ``working``. Passing a
        kind here is allowed for the case where the Router already knows the executor,
        and it goes straight to ``working``.
        """

        title = title.strip()
        if not title:
            raise ValueError("ticket title must not be empty")
        if not agent_id.strip():
            raise ValueError("ticket agent_id must not be empty")
        if assignment_kind is not None and not assignment_kind.strip():
            raise ValueError("assignment_kind must be None or a non-empty string")

        ticket_id = new_ticket_id()
        ts = now_ts()
        kind = assignment_kind.strip() if assignment_kind else None
        facts = TicketFacts(
            latest_report=None,
            open_attention=False,
            delivery_failed=False,
            # ``assigned`` keys off a non-empty kind, so passing the stored value
            # straight through is what decides working/queued versus working.
            assignment_kind=kind,
            assignee_id=assignee_id,
        )
        with self._db.transaction() as conn:
            conn.execute(
                "INSERT INTO tickets("  # noqa: S608 - fixed column list, no interpolation
                "ticket_id, owner_user_id, agent_id, thread_id, title, goal, stage, "
                "priority, assignment_kind, assignee_id, open_attention, attention_reason, "
                "latest_report, delivery_failed, created_at, updated_at, started_at, ended_at"
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, NULL, NULL, 0, ?, ?, NULL, NULL)",
                (
                    ticket_id,
                    owner_user_id,
                    agent_id.strip(),
                    thread_id,
                    title,
                    goal,
                    resolve_stage(facts),
                    priority,
                    # The column is NOT NULL DEFAULT 'expert'; an unassigned ticket
                    # stores the empty string, which ``assigned`` correctly reads as
                    # falsy. NULL would be rejected by the column constraint.
                    kind or "",
                    assignee_id,
                    ts,
                    ts,
                ),
            )
            self._append_event_tx(
                conn,
                ticket_id,
                "created",
                ts,
                summary=title,
                actor=actor,
                payload={"agentId": agent_id, "assignmentKind": assignment_kind},
            )
        row = self._fetch_row(ticket_id, owner_user_id)
        return Ticket.from_row(row)

    # ------------------------------------------------------------------ read

    def get(self, ticket_id: str, *, owner_user_id: int) -> Ticket:
        """Fetch one ticket or raise :class:`TicketNotFound`."""

        return Ticket.from_row(self._fetch_row(ticket_id, owner_user_id))

    def _fetch_row(self, ticket_id: str, owner_user_id: int) -> DbRow:
        with self._db.connect() as conn:
            row = conn.execute(
                f"SELECT {_TICKET_COLS} FROM tickets "  # noqa: S608 - constant
                "WHERE ticket_id = ? AND owner_user_id = ?",
                (ticket_id, owner_user_id),
            ).fetchone()
        if row is None:
            raise TicketNotFound(ticket_id)
        return cast(DbRow, row)

    def list_for_owner(
        self,
        owner_user_id: int,
        *,
        stages: list[str] | None = None,
        agent_id: str | None = None,
        limit: int = 100,
        offset: int = 0,
    ) -> list[Ticket]:
        """List a user's tickets, person-blocked work first.

        ``stages`` filters *after* resolution because the stage is derived, so the
        query counts every row for the owner and trims in Python. That is fine at this
        scale (a family box has hundreds of tickets, not millions) and keeps the SQL
        free of a resolver copy that could drift.
        """

        if limit <= 0:
            return []
        sql = (
            f"SELECT {_TICKET_COLS} FROM tickets WHERE owner_user_id = ?"  # noqa: S608
        )
        params: list[Any] = [owner_user_id]
        if agent_id:
            sql += " AND agent_id = ?"
            params.append(agent_id)
        sql += " ORDER BY priority DESC, updated_at DESC"

        with self._db.connect() as conn:
            rows = conn.execute(sql, tuple(params)).fetchall()

        tickets = [Ticket.from_row(cast(DbRow, r)) for r in rows]
        if stages:
            wanted = set(stages)
            tickets = [t for t in tickets if t.stage in wanted]
        tickets.sort(key=lambda t: (stage_sort_key(t.stage), -t.priority, -t.updated_at))
        return tickets[offset : offset + limit]

    def counts_by_stage(self, owner_user_id: int) -> dict[str, int]:
        """Stage histogram for the "活" tab badges."""

        counts: dict[str, int] = {}
        for ticket in self.list_for_owner(owner_user_id, limit=10_000):
            counts[ticket.stage] = counts.get(ticket.stage, 0) + 1
        return counts

    # ----------------------------------------------------------------- write

    def update(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        title: str | None = None,
        goal: str | None = None,
        priority: int | None = None,
        actor: str = "",
    ) -> Ticket:
        """Rename, re-describe, or re-prioritise a ticket.

        Only these three fields are editable here. Stage, reports, attention, and
        delivery state all change through their own methods, because each of them has
        a rule attached and a ``None`` meaning "leave alone" would blur it.

        ``sets`` is assembled from a fixed allowlist of column names, so no caller
        string ever reaches the SQL fragment.
        """

        pairs: list[tuple[str, str, Any]] = [
            ("title", "title = ?", title),
            ("goal", "goal = ?", goal),
            ("priority", "priority = ?", priority),
        ]
        sets_parts: list[str] = []
        params: list[Any] = []
        for _name, fragment, value in pairs:
            if value is None:
                continue
            sets_parts.append(fragment)
            params.append(value)
        if not sets_parts:
            return self.get(ticket_id, owner_user_id=owner_user_id)
        return self._update(
            ticket_id,
            owner_user_id,
            sets=", ".join(sets_parts),
            params=tuple(params),
            event_kind="note",
            event_summary="ticket edited",
            event_payload={"fields": [n for n, _, v in pairs if v is not None]},
            actor=actor,
        )

    def _update(
        self,
        ticket_id: str,
        owner_user_id: int,
        *,
        sets: str,
        params: tuple[Any, ...],
        event_kind: str | None = None,
        event_summary: str = "",
        event_payload: dict[str, Any] | None = None,
        actor: str = "",
    ) -> Ticket:
        """Apply a column update, refresh the derived stage, and log an event.

        ``sets`` is a caller-supplied fragment of ``col = ?, col = ?``. It is built
        from a fixed set of literals in each caller below — never from user input.
        """

        ts = now_ts()
        with self._db.transaction() as conn:
            row = conn.execute(
                f"SELECT {_TICKET_COLS} FROM tickets "  # noqa: S608
                "WHERE ticket_id = ? AND owner_user_id = ?",
                (ticket_id, owner_user_id),
            ).fetchone()
            if row is None:
                raise TicketNotFound(ticket_id)

            conn.execute(
                f"UPDATE tickets SET {sets}, updated_at = ? "  # noqa: S608 - literal sets
                "WHERE ticket_id = ? AND owner_user_id = ?",
                (*params, ts, ticket_id, owner_user_id),
            )
            fresh = conn.execute(
                f"SELECT {_TICKET_COLS} FROM tickets WHERE ticket_id = ?"  # noqa: S608
                " AND owner_user_id = ?",
                (ticket_id, owner_user_id),
            ).fetchone()
            assert fresh is not None
            stage = resolve_stage(TicketFacts.from_row(cast(DbRow, fresh)))
            conn.execute(
                "UPDATE tickets SET stage = ? WHERE ticket_id = ? AND owner_user_id = ?",
                (stage, ticket_id, owner_user_id),
            )
            if event_kind:
                self._append_event_tx(
                    conn,
                    ticket_id,
                    event_kind,
                    ts,
                    summary=event_summary,
                    actor=actor,
                    payload=event_payload,
                )
            final = conn.execute(
                f"SELECT {_TICKET_COLS} FROM tickets WHERE ticket_id = ?"  # noqa: S608
                " AND owner_user_id = ?",
                (ticket_id, owner_user_id),
            ).fetchone()
            assert final is not None
        return Ticket.from_row(cast(DbRow, final))

    def file_report(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        report: str | None,
        actor: str = "",
        detail: str = "",
    ) -> Ticket:
        """Record a progress report. ``None`` clears the report."""

        validate_report(report)
        return self._update(
            ticket_id,
            owner_user_id,
            sets="latest_report = ?",
            params=(report,),
            event_kind="report",
            event_summary=detail or (report or "cleared"),
            actor=actor,
        )

    def request_attention(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        reason: str,
        actor: str = "",
    ) -> Ticket:
        """Mark that the expert needs the human to answer something."""

        return self._update(
            ticket_id,
            owner_user_id,
            sets="open_attention = 1, attention_reason = ?",
            params=(reason,),
            event_kind="attention",
            event_summary=reason,
            actor=actor,
        )

    def clear_attention(self, ticket_id: str, *, owner_user_id: int, actor: str = "") -> Ticket:
        """Clear the open question — the human answered, or it went stale."""

        return self._update(
            ticket_id,
            owner_user_id,
            sets="open_attention = 0, attention_reason = NULL",
            params=(),
            event_kind="attention_cleared",
            actor=actor,
        )

    def mark_delivery_failed(
        self, ticket_id: str, *, owner_user_id: int, detail: str = "", actor: str = ""
    ) -> Ticket:
        """The reply reached nobody. Outranks every other stage."""

        return self._update(
            ticket_id,
            owner_user_id,
            sets="delivery_failed = 1",
            params=(),
            event_kind="delivery_failed",
            event_summary=detail,
            actor=actor,
        )

    def clear_delivery_failed(
        self, ticket_id: str, *, owner_user_id: int, actor: str = ""
    ) -> Ticket:
        return self._update(
            ticket_id,
            owner_user_id,
            sets="delivery_failed = 0",
            params=(),
            event_kind="delivery_recovered",
            actor=actor,
        )

    def assign(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        assignment_kind: str,
        assignee_id: str | None,
        actor: str = "",
    ) -> Ticket:
        """Hand the ticket to an executor. Sets ``started_at`` on the first hand-off.

        ``started_at`` uses ``COALESCE`` so a re-assignment keeps the original start
        time — that is what "when did this work begin" means to the human reading it.
        """

        return self._update(
            ticket_id,
            owner_user_id,
            sets="assignment_kind = ?, assignee_id = ?, started_at = COALESCE(started_at, ?)",
            params=(assignment_kind, assignee_id, now_ts()),
            event_kind="assigned",
            event_summary=f"{assignment_kind}:{assignee_id or ''}",
            actor=actor,
        )

    def close(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        ok: bool = True,
        actor: str = "",
    ) -> Ticket:
        """Finish the ticket. ``ok=False`` records a failure.

        The closing report decides the final stage: ``ready`` → ``person/review``
        (a human should look at the result), ``failed`` → ``person/blocked``. A ticket
        is never auto-archived, because the human has not seen the result yet.
        """

        return self._update(
            ticket_id,
            owner_user_id,
            sets="latest_report = ?, ended_at = ?",
            params=("ready" if ok else "failed", now_ts()),
            event_kind="completed" if ok else "failed",
            actor=actor,
        )

    # ---------------------------------------------------------------- events

    def _append_event_tx(
        self,
        conn: Any,
        ticket_id: str,
        kind: str,
        ts: int,
        *,
        summary: str = "",
        actor: str = "",
        payload: dict[str, Any] | None = None,
    ) -> None:
        """Append an event inside an open transaction.

        Ordering comes from the autoincrement ``id``, so there is no per-ticket ``seq``
        to compute — that keeps concurrent appends from racing on a ``MAX(seq)`` read.
        """

        conn.execute(
            "INSERT INTO ticket_events(ticket_id, kind, actor, summary, payload_json, created_at) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (
                ticket_id,
                kind,
                actor,
                summary,
                json.dumps(payload or {}, ensure_ascii=False, separators=(",", ":")),
                ts,
            ),
        )

    def append_event(
        self,
        ticket_id: str,
        *,
        owner_user_id: int,
        kind: str,
        summary: str = "",
        actor: str = "",
        payload: dict[str, Any] | None = None,
    ) -> None:
        """Append a free-form event (a note, a log line). Verifies ownership first."""

        if kind not in EVENT_KINDS:
            raise ValueError(f"unknown event kind {kind!r}; expected one of {list(EVENT_KINDS)}")
        self._fetch_row(ticket_id, owner_user_id)  # ownership check
        with self._db.transaction() as conn:
            self._append_event_tx(
                conn, ticket_id, kind, now_ts(), summary=summary, actor=actor, payload=payload
            )

    def events(self, ticket_id: str, *, owner_user_id: int, limit: int = 200) -> list[TicketEvent]:
        """Timeline, oldest first. Ownership is checked before reading."""

        self._fetch_row(ticket_id, owner_user_id)
        if limit <= 0:
            return []
        with self._db.connect() as conn:
            rows = conn.execute(
                "SELECT id, ticket_id, kind, actor, summary, payload_json, created_at "
                "FROM ticket_events WHERE ticket_id = ? ORDER BY id DESC LIMIT ?",
                (ticket_id, limit),
            ).fetchall()
        return [TicketEvent.from_row(cast(DbRow, r)) for r in reversed(rows)]
