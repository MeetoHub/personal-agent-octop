"""Phone channel API: inbound WS from the Octop mobile app."""

from __future__ import annotations

import json
import logging
import time
from typing import Any

from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect

from octop.api.deps import require_permission, resolve_user_from_token
from octop.infra.errors import OctopError
from octop.infra.phone.hub import get_phone_hub
from octop.infra.phone.protocol import (
    CLOSE_FORBIDDEN,
    CLOSE_MISSING_TOKEN,
    PERMISSION_KEY,
    PROTOCOL_VERSION,
    READ_ONLY_TOOLS,
    TYPE_ERROR,
    TYPE_HELLO,
    TYPE_HELLO_OK,
    TYPE_PING,
    TYPE_PONG,
    TYPE_TOOL_RESULT,
)
from octop.infra.users.permissions import user_has_permission

logger = logging.getLogger(__name__)

router = APIRouter()


@router.websocket("/phone/ws")
async def phone_inbound_ws(websocket: WebSocket) -> None:
    """The phone app dials in and stays connected; the server pushes tool calls."""
    server = websocket.app.state.octop_server
    # Accept first: an early close makes uvicorn answer HTTP 403, which the
    # dialer reads as "route missing" instead of "token rejected".
    await websocket.accept()

    raw = websocket.query_params.get("token")
    if not raw:
        header = websocket.headers.get("authorization") or ""
        if header.lower().startswith("bearer "):
            raw = header[7:].strip()
    if not raw:
        await websocket.close(code=CLOSE_MISSING_TOKEN, reason="missing token")
        return
    try:
        user = resolve_user_from_token(server, raw)
    except OctopError as exc:
        await websocket.close(code=CLOSE_MISSING_TOKEN, reason=f"auth: {exc.code.value}")
        return
    if not user_has_permission(user, PERMISSION_KEY):
        await websocket.close(code=CLOSE_FORBIDDEN, reason="permission required")
        return

    hub = get_phone_hub()
    link = await hub.register(user.id, websocket)
    try:
        await link.send(
            {
                "type": TYPE_HELLO_OK,
                "protocol": PROTOCOL_VERSION,
                "userId": int(user.id),
                "serverTime": int(time.time() * 1000),
                "tools": sorted(READ_ONLY_TOOLS),
            }
        )
        while True:
            raw_message = await websocket.receive_text()
            try:
                payload = json.loads(raw_message)
            except json.JSONDecodeError:
                continue
            if not isinstance(payload, dict):
                continue
            link.last_seen = time.time()
            kind = payload.get("type")
            if kind == TYPE_HELLO:
                link.apply_hello(payload)
            elif kind == TYPE_TOOL_RESULT:
                req_id = str(payload.get("reqId") or "")
                if req_id and not link.resolve(req_id, payload):
                    logger.debug("phone sent tool_result for unknown reqId %s", req_id)
            elif kind == TYPE_PING:
                await link.send({"type": TYPE_PONG, "time": int(time.time() * 1000)})
            else:
                await link.send({"type": TYPE_ERROR, "message": f"unknown type: {kind}"})
    except WebSocketDisconnect:
        pass
    except Exception:
        logger.exception("phone inbound ws error")
    finally:
        hub.unregister(user.id, link)


@router.get("/phone/status", summary="Phone channel status")
async def phone_status(
    user: Any = Depends(require_permission(PERMISSION_KEY)),
) -> dict[str, Any]:
    """Whether this user's phone is currently connected, and what it can do."""
    return get_phone_hub().status(user.id)
