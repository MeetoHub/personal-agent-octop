"""Connection registry + request/response correlation for the phone channel."""

from __future__ import annotations

import asyncio
import contextlib
import json
import logging
import time
import uuid
from dataclasses import dataclass, field
from typing import Any

from octop.infra.phone.protocol import (
    CLOSE_REPLACED,
    DEFAULT_TIMEOUT_S,
    TYPE_TOOL_CALL,
)

logger = logging.getLogger(__name__)

# A single phone answers serially; cap in-flight calls so a runaway agent
# cannot queue hundreds of requests behind a slow device.
MAX_PENDING_PER_LINK = 32


class PhoneUnavailable(RuntimeError):
    """No phone is connected for this user (or the link is saturated)."""


class PhoneTimeout(RuntimeError):
    """The phone did not answer before the deadline."""


class PhoneCallFailed(RuntimeError):
    """The phone answered with ``ok: false``."""


@dataclass
class PhoneLink:
    user_id: int
    websocket: Any
    device: dict[str, Any] = field(default_factory=dict)
    capabilities: tuple[str, ...] = ()
    connected_at: float = field(default_factory=time.time)
    last_seen: float = field(default_factory=time.time)
    closed: bool = False
    _send_lock: asyncio.Lock = field(default_factory=asyncio.Lock, repr=False)
    _pending: dict[str, asyncio.Future[Any]] = field(default_factory=dict, repr=False)

    async def send(self, payload: dict[str, Any]) -> None:
        async with self._send_lock:
            await self.websocket.send_text(json.dumps(payload, ensure_ascii=False))

    def apply_hello(self, payload: dict[str, Any]) -> None:
        device = payload.get("device")
        if isinstance(device, dict):
            self.device = {str(k): v for k, v in device.items()}
        caps = payload.get("capabilities")
        if isinstance(caps, (list, tuple)):
            self.capabilities = tuple(str(c) for c in caps)

    async def request(
        self,
        tool: str,
        args: dict[str, Any] | None = None,
        *,
        timeout: float = DEFAULT_TIMEOUT_S,
    ) -> Any:
        if self.closed:
            raise PhoneUnavailable("phone link is closed")
        if len(self._pending) >= MAX_PENDING_PER_LINK:
            raise PhoneUnavailable("phone is busy with too many pending calls")

        req_id = uuid.uuid4().hex
        fut: asyncio.Future[Any] = asyncio.get_running_loop().create_future()
        self._pending[req_id] = fut
        try:
            await self.send(
                {
                    "type": TYPE_TOOL_CALL,
                    "reqId": req_id,
                    "tool": tool,
                    "args": args or {},
                }
            )
            return await asyncio.wait_for(fut, timeout)
        except TimeoutError as exc:
            raise PhoneTimeout(f"phone did not answer {tool} within {timeout:.0f}s") from exc
        finally:
            self._pending.pop(req_id, None)

    def resolve(self, req_id: str, payload: dict[str, Any]) -> bool:
        """Deliver a ``tool_result``; unknown or already-settled ids are ignored."""
        fut = self._pending.get(req_id)
        if fut is None or fut.done():
            return False
        if payload.get("ok"):
            fut.set_result(payload.get("data"))
        else:
            fut.set_exception(PhoneCallFailed(str(payload.get("error") or "phone_error")))
        return True

    def fail_pending(self, reason: str) -> None:
        self.closed = True
        for req_id, fut in list(self._pending.items()):
            if not fut.done():
                fut.set_exception(PhoneUnavailable(reason))
            self._pending.pop(req_id, None)

    def describe(self) -> dict[str, Any]:
        return {
            "connected": not self.closed,
            "device": self.device,
            "capabilities": list(self.capabilities),
            "connected_at": self.connected_at,
            "last_seen": self.last_seen,
            "pending": len(self._pending),
        }


class PhoneHub:
    """One live link per user; a newer connection evicts the older one."""

    def __init__(self) -> None:
        self._links: dict[int, PhoneLink] = {}

    async def register(
        self,
        user_id: int,
        websocket: Any,
        *,
        device: dict[str, Any] | None = None,
        capabilities: tuple[str, ...] = (),
    ) -> PhoneLink:
        link = PhoneLink(
            user_id=int(user_id),
            websocket=websocket,
            device=dict(device or {}),
            capabilities=tuple(capabilities),
        )
        previous = self._links.get(link.user_id)
        self._links[link.user_id] = link
        if previous is not None and previous is not link:
            previous.fail_pending("replaced by a newer phone connection")
            with contextlib.suppress(Exception):
                await previous.websocket.close(
                    code=CLOSE_REPLACED, reason="replaced by newer connection"
                )
        return link

    def unregister(self, user_id: int, link: PhoneLink) -> None:
        if self._links.get(int(user_id)) is link:
            del self._links[int(user_id)]
        link.fail_pending("phone disconnected")

    def get(self, user_id: int) -> PhoneLink | None:
        link = self._links.get(int(user_id))
        if link is not None and link.closed:
            return None
        return link

    async def call(
        self,
        user_id: int,
        tool: str,
        args: dict[str, Any] | None = None,
        *,
        timeout: float = DEFAULT_TIMEOUT_S,
    ) -> Any:
        link = self.get(user_id)
        if link is None:
            raise PhoneUnavailable(
                "no phone connected — open the Octop mobile app and keep it in the background"
            )
        return await link.request(tool, args, timeout=timeout)

    def status(self, user_id: int) -> dict[str, Any]:
        link = self.get(user_id)
        if link is None:
            return {"connected": False, "device": {}, "capabilities": [], "pending": 0}
        return link.describe()

    def snapshot(self) -> list[dict[str, Any]]:
        return [{"user_id": uid, **link.describe()} for uid, link in self._links.items()]


_hub: PhoneHub | None = None


def get_phone_hub() -> PhoneHub:
    global _hub
    if _hub is None:
        _hub = PhoneHub()
    return _hub


def reset_phone_hub() -> None:
    """Test hook: drop the process-wide hub."""
    global _hub
    _hub = None
