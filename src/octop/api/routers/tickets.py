"""Tickets — the REST surface for work a person has delegated to an expert.

Five endpoints, all scoped to the caller. Nothing here exposes another person's
work, and **admins get no bypass** — see the note on :func:`_store_for`.

Why the isolation is written the way it is
------------------------------------------
:func:`octop.api.common.agent.assert_agent_owner` short-circuits for admins. That is
right for agents, which are an administrator's business. It is wrong for tickets: a
ticket contains what a person told their assistant in confidence, and the household
model in this deployment means one machine is shared by people who must not read each
other's work. So every query carries ``owner_user_id == user.id`` and an id belonging
to someone else is reported exactly like an id that does not exist. A 403-versus-404
difference would let a caller probe for the existence of other people's tickets.
"""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, ConfigDict, Field

from octop.api.deps import current_user, get_server, require_database
from octop.infra.errors import ErrorCode, OctopError
from octop.infra.tickets import (
    STAGES,
    Ticket,
    TicketEvent,
    TicketNotFound,
    TicketStore,
    is_long_task,
    validate_report,
)

router = APIRouter()


# --------------------------------------------------------------------------- bodies


class _CamelBody(BaseModel):
    """Accept both ``snake_case`` and ``camelCase`` keys on the wire.

    The dashboard and the phone both speak camelCase — the response payloads are
    camelCase too — so a request built by either client must not be rejected for
    casing alone. ``populate_by_name`` keeps the Python-side field names usable.
    """

    model_config = ConfigDict(populate_by_name=True)


class TicketCreateBody(_CamelBody):
    title: str = Field(min_length=1, max_length=200)
    goal: str = ""
    agent_id: str = Field(min_length=1, max_length=64, alias="agentId")
    thread_id: str | None = Field(default=None, alias="threadId")
    priority: int = 0
    assignment_kind: str | None = Field(default=None, alias="assignmentKind")
    assignee_id: str | None = Field(default=None, alias="assigneeId")


class TicketPatchBody(_CamelBody):
    """Fields a person may change directly. Stage is never one of them."""

    title: str | None = None
    goal: str | None = None
    priority: int | None = None


class ReportBody(_CamelBody):
    report: str
    summary: str = ""


class AttentionBody(_CamelBody):
    reason: str = Field(min_length=1, max_length=500)


class AssignBody(_CamelBody):
    assignment_kind: str = Field(min_length=1, max_length=32, alias="assignmentKind")
    assignee_id: str = Field(min_length=1, max_length=64, alias="assigneeId")


class DeliveryBody(_CamelBody):
    failed: bool = True


# ------------------------------------------------------------------------ plumbing


def _store_for(server: Any) -> TicketStore:
    """The ticket store, bound to the control-plane pool.

    Note what is *absent*: there is no ``user`` parameter. The store itself enforces
    ownership on every method, taking ``owner_user_id`` as a required keyword, so
    there is no code path in this module that can read or write a row without naming
    whose it is.
    """

    require_database(server)
    assert server.services is not None
    return TicketStore(server.services.db)


def _ticket_payload(ticket: Ticket) -> dict[str, Any]:
    return ticket.to_dict()


def _event_payload(event: TicketEvent) -> dict[str, Any]:
    return event.to_dict()


def _translate(fn: Any, *args: Any, **kwargs: Any) -> Any:
    """Run a store call, turning ``TicketNotFound`` into the public error.

    Every "someone else's ticket" case and every "no such ticket" case arrives here,
    which is what keeps the two indistinguishable to the caller.
    """

    try:
        return fn(*args, **kwargs)
    except TicketNotFound as exc:
        raise OctopError(ErrorCode.TICKET_NOT_FOUND, "ticket not found", status=404) from exc


# ----------------------------------------------------------------------- endpoints


@router.get("/tickets", summary="List my tickets")
async def list_tickets(
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
    stage: str | None = Query(default=None, description="Filter to one stage"),
    agent_id: str | None = Query(default=None, description="Filter to one expert"),
    limit: int = Query(default=100, ge=1, le=500),
    offset: int = Query(default=0, ge=0),
) -> dict[str, Any]:
    """My tickets, most-urgent first, plus the per-stage counts the badge needs."""

    if stage is not None and stage not in STAGES:
        raise OctopError(ErrorCode.APP_CONFIG_INVALID, f"unknown stage {stage!r}")
    store = _store_for(server)
    stages = [stage] if stage else None
    rows = _translate(
        store.list_for_owner,
        user.id,
        stages=stages,
        agent_id=agent_id,
        limit=limit,
        offset=offset,
    )
    counts = _translate(store.counts_by_stage, user.id)
    return {
        "items": [_ticket_payload(t) for t in rows],
        "counts": counts,
    }


@router.post("/tickets", status_code=201, summary="Open a ticket for a long task")
async def create_ticket(
    body: TicketCreateBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """Open a ticket.

    The caller decides whether the work is long enough to deserve one; this endpoint
    does not second-guess it. :func:`octop.infra.tickets.is_long_task` is exposed so a
    client can ask, but enforcement lives in the Router that decides to call this.
    """

    title = body.title.strip()
    if not title:
        raise OctopError(ErrorCode.APP_CONFIG_INVALID, "title must not be empty")
    store = _store_for(server)
    ticket = _translate(
        store.create,
        owner_user_id=user.id,
        agent_id=body.agent_id.strip(),
        title=title,
        goal=body.goal,
        thread_id=body.thread_id,
        priority=body.priority,
        assignment_kind=body.assignment_kind,
        assignee_id=body.assignee_id,
    )
    return _ticket_payload(ticket)


@router.get("/tickets/{ticket_id}", summary="Get one ticket")
async def get_ticket(
    ticket_id: str,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    store = _store_for(server)
    ticket = _translate(store.get, ticket_id, owner_user_id=user.id)
    return _ticket_payload(ticket)


@router.get("/tickets/{ticket_id}/events", summary="Get a ticket's timeline")
async def list_ticket_events(
    ticket_id: str,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
    limit: int = Query(default=200, ge=1, le=1000),
) -> list[dict[str, Any]]:
    """The timeline, oldest event first — how the work got from opened to here."""

    store = _store_for(server)
    events = _translate(store.events, ticket_id, owner_user_id=user.id, limit=limit)
    return [_event_payload(e) for e in events]


@router.patch("/tickets/{ticket_id}", summary="Edit a ticket's title, goal or priority")
async def patch_ticket(
    ticket_id: str,
    body: TicketPatchBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """Rename or re-prioritise. Stage and reports are the expert's to change."""

    store = _store_for(server)
    set_fields = body.model_fields_set
    kwargs: dict[str, Any] = {}
    if "title" in set_fields:
        title = (body.title or "").strip()
        if not title:
            raise OctopError(ErrorCode.APP_CONFIG_INVALID, "title must not be empty")
        kwargs["title"] = title
    if "goal" in set_fields:
        kwargs["goal"] = body.goal or ""
    if "priority" in set_fields:
        kwargs["priority"] = body.priority if body.priority is not None else 0
    if not kwargs:
        ticket = _translate(store.get, ticket_id, owner_user_id=user.id)
        return _ticket_payload(ticket)
    ticket = _translate(store.update, ticket_id, owner_user_id=user.id, **kwargs)
    return _ticket_payload(ticket)


@router.post("/tickets/{ticket_id}/report", summary="File a progress report")
async def report_ticket(
    ticket_id: str,
    body: ReportBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """The expert says how it is going. Only the owner may do this on their ticket."""

    try:
        validate_report(body.report)
    except ValueError as exc:
        raise OctopError(ErrorCode.APP_CONFIG_INVALID, str(exc)) from exc
    store = _store_for(server)
    ticket = _translate(
        store.file_report,
        ticket_id,
        owner_user_id=user.id,
        report=body.report,
        detail=body.summary,
        actor=user.username,
    )
    return _ticket_payload(ticket)


@router.post("/tickets/{ticket_id}/attention", summary="Ask the owner a question")
async def request_attention(
    ticket_id: str,
    body: AttentionBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """Raise the hand. This is the only thing that puts a ticket in person/answer."""

    reason = body.reason.strip()
    if not reason:
        raise OctopError(ErrorCode.APP_CONFIG_INVALID, "reason must not be empty")
    store = _store_for(server)
    ticket = _translate(
        store.request_attention,
        ticket_id,
        owner_user_id=user.id,
        reason=reason,
    )
    return _ticket_payload(ticket)


@router.delete("/tickets/{ticket_id}/attention", status_code=200, summary="Lower the hand")
async def clear_attention(
    ticket_id: str,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    store = _store_for(server)
    ticket = _translate(store.clear_attention, ticket_id, owner_user_id=user.id)
    return _ticket_payload(ticket)


@router.post("/tickets/{ticket_id}/assign", summary="Hand work to a subagent or expert")
async def assign_ticket(
    ticket_id: str,
    body: AssignBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    store = _store_for(server)
    ticket = _translate(
        store.assign,
        ticket_id,
        owner_user_id=user.id,
        assignment_kind=body.assignment_kind.strip(),
        assignee_id=body.assignee_id.strip(),
    )
    return _ticket_payload(ticket)


@router.post("/tickets/{ticket_id}/delivery", summary="Mark delivery as failed or recovered")
async def set_delivery(
    ticket_id: str,
    body: DeliveryBody,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    store = _store_for(server)
    if body.failed:
        ticket = _translate(store.mark_delivery_failed, ticket_id, owner_user_id=user.id)
    else:
        ticket = _translate(store.clear_delivery_failed, ticket_id, owner_user_id=user.id)
    return _ticket_payload(ticket)


@router.post("/tickets/{ticket_id}/close", summary="Close a ticket")
async def close_ticket(
    ticket_id: str,
    user: Any = Depends(current_user),
    server: Any = Depends(get_server),
) -> dict[str, Any]:
    """The work is finished. The ticket moves to person/review, not to gone."""

    store = _store_for(server)
    ticket = _translate(store.close, ticket_id, owner_user_id=user.id)
    return _ticket_payload(ticket)


@router.post("/tickets/classify", summary="Ask whether a request should become a ticket")
async def classify_request(
    body: dict[str, Any],
    _user: Any = Depends(current_user),
) -> dict[str, Any]:
    """A cheap hint for clients that want to prompt before opening a ticket.

    Deliberately *not* authoritative: the Router has the conversation and the model.
    This answers a keyword question so the mobile app can ask "记到「活」里?" without
    a round trip to the model.
    """

    text = str(body.get("text") or "")
    return {"is_long_task": is_long_task(text)}


__all__ = ["router"]
