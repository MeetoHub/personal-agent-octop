"""Tickets — the unit of work a person delegates to an expert.

A ticket is what turns "I asked the assistant something" into "something is being
worked on, and here is where it stands". It exists because a chat thread answers a
question and then goes quiet; work that takes minutes or hours needs a place to live
where the human can see it is still moving, see why it stopped, and pick it back up.

Layering
--------
:mod:`octop.infra.tickets.stage`
    The seven-state resolver. Pure functions over four raw columns; no I/O. Reused by
    the REST layer, the mobile client's badges, and the tests.

:mod:`octop.infra.tickets.store`
    Persistence. One method per state transition, each writing an event so the ticket
    keeps a timeline.

Design notes worth keeping in mind when changing this package:

* **The stage is derived, never stored as truth.** ``tickets.stage`` is a denormalised
  copy refreshed on every write so that SQL can index and sort on it; readers always
  recompute from the raw columns. One source of truth beats a column that can rot.
* **Ownership is checked on every path, and admins do not bypass it.** See the note in
  :mod:`octop.infra.tickets.store`.
* **Tickets are only created for work that is genuinely long-running.** A one-shot
  question stays in the chat thread. This is enforced by the caller (the Router
  decides), not here — this package will happily store whatever it is handed.
"""

from octop.infra.tickets.stage import (
    DROPPED_STAGE,
    PERSON_STAGES,
    REPORTS,
    STAGES,
    TERMINAL_REPORTS,
    TicketFacts,
    is_long_task,
    is_person_stage,
    resolve_stage,
    stage_sort_key,
    validate_report,
)
from octop.infra.tickets.store import (
    EVENT_KINDS,
    Ticket,
    TicketEvent,
    TicketNotFound,
    TicketStore,
    new_ticket_id,
)

__all__ = [
    "DROPPED_STAGE",
    "EVENT_KINDS",
    "PERSON_STAGES",
    "REPORTS",
    "STAGES",
    "TERMINAL_REPORTS",
    "Ticket",
    "TicketEvent",
    "TicketFacts",
    "TicketNotFound",
    "TicketStore",
    "is_long_task",
    "is_person_stage",
    "new_ticket_id",
    "resolve_stage",
    "stage_sort_key",
    "validate_report",
]
