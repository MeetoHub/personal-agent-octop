"""Read-only phone tools backed by the reverse WS channel.

The phone app dials in (see ``api/routers/phone.py``); these tools push a
request down that socket and return whatever the phone sends back. Pure I/O:
each tool returns a JSON string and never raises into the harness.

Read-only by design — no tap, no send, no delete.
"""

from __future__ import annotations

import json
from typing import Annotated, Any

from langchain_core.tools import StructuredTool
from langgraph.config import get_config
from pydantic import Field

from octop.config import OctopConfig
from octop.infra.phone import hub as phone_hub
from octop.infra.phone.protocol import (
    SLOW_TIMEOUT_S,
    TOOL_CALENDAR_TODAY,
    TOOL_CONTACTS_SEARCH,
    TOOL_HEALTH_SUMMARY,
    TOOL_LOCATION_CURRENT,
    TOOL_NOTIFICATIONS_RECENT,
    TOOL_PHOTOS_RECENT,
    TOOL_SMS_RECENT,
)
from octop.infra.users.permissions import user_has_permission

PHONE_CONTACTS_SEARCH = "phone_contacts_search"
PHONE_SMS_RECENT = "phone_sms_recent"
PHONE_CALENDAR_TODAY = "phone_calendar_today"
PHONE_NOTIFICATIONS_RECENT = "phone_notifications_recent"
PHONE_HEALTH_SUMMARY = "phone_health_summary"
PHONE_LOCATION_CURRENT = "phone_location_current"
PHONE_PHOTOS_RECENT = "phone_photos_recent"


def _tool_ctx() -> dict[str, Any]:
    return get_config().get("configurable") or {}


def _require_phone_access(config: OctopConfig, ctx: dict[str, Any], user_repo: Any) -> None:
    if not config.capabilities.mobile.enabled:
        raise ValueError("Phone channel is not enabled on this host")
    user_raw = ctx.get("user")
    if user_raw is None:
        raise ValueError("missing configurable.user")
    if ctx.get("user_is_admin"):
        return
    row = user_repo.get(int(user_raw))
    if row is None or not user_has_permission(row, "mobile"):
        raise ValueError("mobile permission required")


async def _call(
    ctx: dict[str, Any],
    tool: str,
    args: dict[str, Any] | None = None,
    *,
    timeout: float = 10.0,
) -> str:
    try:
        data = await phone_hub.get_phone_hub().call(
            int(ctx["user"]), tool, args or {}, timeout=timeout
        )
    except phone_hub.PhoneUnavailable as exc:
        return json.dumps({"error": str(exc), "code": "phone_unavailable"}, ensure_ascii=False)
    except phone_hub.PhoneTimeout as exc:
        return json.dumps({"error": str(exc), "code": "phone_timeout"}, ensure_ascii=False)
    except phone_hub.PhoneCallFailed as exc:
        return json.dumps({"error": str(exc), "code": "phone_error"}, ensure_ascii=False)
    except Exception as exc:  # noqa: BLE001 - tools never raise into the harness
        return json.dumps({"error": str(exc)}, ensure_ascii=False)
    return json.dumps({"ok": True, "data": data}, ensure_ascii=False)


def build_phone_tools(
    config: OctopConfig,
    *,
    user_repo: Any,
) -> list[StructuredTool]:
    """Return the read-only phone tools (always registered; calls fail fast when no phone is online)."""
    if not config.capabilities.mobile.enabled:
        return []

    async def phone_contacts_search(
        query: Annotated[
            str, Field(description="Name, phone number or pinyin fragment to search for.")
        ] = "",
        limit: Annotated[int, Field(description="Max contacts to return.", ge=1, le=50)] = 20,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(ctx, TOOL_CONTACTS_SEARCH, {"query": query, "limit": limit})
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_sms_recent(
        limit: Annotated[int, Field(description="Max messages to return.", ge=1, le=50)] = 20,
        hours: Annotated[int, Field(description="Look-back window in hours.", ge=1, le=720)] = 72,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(ctx, TOOL_SMS_RECENT, {"limit": limit, "hours": hours})
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_calendar_today(
        timezone_offset_minutes: Annotated[
            int | None,
            Field(description="Device UTC offset in minutes; omit to use the phone's own zone."),
        ] = None,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            args: dict[str, Any] = {}
            if timezone_offset_minutes is not None:
                args["timezoneOffsetMinutes"] = timezone_offset_minutes
            return await _call(ctx, TOOL_CALENDAR_TODAY, args)
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_notifications_recent(
        limit: Annotated[int, Field(description="Max notifications to return.", ge=1, le=50)] = 20,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(ctx, TOOL_NOTIFICATIONS_RECENT, {"limit": limit})
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_health_summary(
        days: Annotated[int, Field(description="How many days to summarise.", ge=1, le=30)] = 1,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(ctx, TOOL_HEALTH_SUMMARY, {"days": days})
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_location_current() -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(ctx, TOOL_LOCATION_CURRENT, {}, timeout=SLOW_TIMEOUT_S)
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    async def phone_photos_recent(
        limit: Annotated[int, Field(description="Max photos to list.", ge=1, le=20)] = 10,
        hours: Annotated[int, Field(description="Look-back window in hours.", ge=1, le=720)] = 168,
    ) -> str:
        try:
            ctx = _tool_ctx()
            _require_phone_access(config, ctx, user_repo)
            return await _call(
                ctx,
                TOOL_PHOTOS_RECENT,
                {"limit": limit, "hours": hours},
                timeout=SLOW_TIMEOUT_S,
            )
        except Exception as exc:
            return json.dumps({"error": str(exc)}, ensure_ascii=False)

    return [
        StructuredTool.from_function(
            coroutine=phone_contacts_search,
            name=PHONE_CONTACTS_SEARCH,
            description="Search the user's phone contacts by name or number (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_sms_recent,
            name=PHONE_SMS_RECENT,
            description="Read recent SMS/MMS messages from the user's phone (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_calendar_today,
            name=PHONE_CALENDAR_TODAY,
            description="Read today's calendar events from the user's phone (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_notifications_recent,
            name=PHONE_NOTIFICATIONS_RECENT,
            description="Read recent notifications from the user's phone (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_health_summary,
            name=PHONE_HEALTH_SUMMARY,
            description="Summarise the user's phone health data: steps, sleep, heart rate (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_location_current,
            name=PHONE_LOCATION_CURRENT,
            description="Get the user's current location from the phone (read-only).",
        ),
        StructuredTool.from_function(
            coroutine=phone_photos_recent,
            name=PHONE_PHOTOS_RECENT,
            description="List recent photos on the user's phone with timestamps (read-only).",
        ),
    ]
