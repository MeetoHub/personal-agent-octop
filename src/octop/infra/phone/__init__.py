"""Reverse phone channel: the mobile app dials in, the server pushes tool calls.

Unlike ``infra/control/mobile`` (NAS drives the phone over adb), this channel is
inbound: the phone app keeps a WebSocket open to the NAS, so NAT, sleep and
changing IPs on the phone side do not matter. Only read-only capabilities are
exposed in the first batch.
"""

from __future__ import annotations

from octop.infra.phone.hub import (
    PhoneCallFailed,
    PhoneHub,
    PhoneLink,
    PhoneTimeout,
    PhoneUnavailable,
    get_phone_hub,
    reset_phone_hub,
)

__all__ = [
    "PhoneCallFailed",
    "PhoneHub",
    "PhoneLink",
    "PhoneTimeout",
    "PhoneUnavailable",
    "get_phone_hub",
    "reset_phone_hub",
]
