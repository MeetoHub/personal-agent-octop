"""Wire protocol for the reverse phone channel.

The phone app opens ``/api/phone/ws?token=<user token>`` and keeps the socket
open. The server pushes one request at a time and correlates the answer by
``reqId``::

    server -> phone  {"type": "tool_call", "reqId": "…", "tool": "calendar.today", "args": {}}
    phone  -> server {"type": "tool_result", "reqId": "…", "ok": true, "data": {…}}
    phone  -> server {"type": "tool_result", "reqId": "…", "ok": false, "error": "permission_denied"}

The first batch is read-only on purpose: nothing here mutates the phone.
"""

from __future__ import annotations

PROTOCOL_VERSION = 1

TYPE_HELLO = "hello"
TYPE_HELLO_OK = "hello_ok"
TYPE_TOOL_CALL = "tool_call"
TYPE_TOOL_RESULT = "tool_result"
TYPE_PING = "ping"
TYPE_PONG = "pong"
TYPE_ERROR = "error"

# Bridge-compatible close codes (4001 auth, 4003 permission, 1011 server error).
CLOSE_MISSING_TOKEN = 4001
CLOSE_REPLACED = 4002
CLOSE_FORBIDDEN = 4003
CLOSE_SERVER_ERROR = 1011

DEFAULT_TIMEOUT_S = 10.0
SLOW_TIMEOUT_S = 20.0

# 个人手机反向通道自己的权限键。不要复用 "mobile"——那是 Remote Android
# （NAS 用 ADB 连同一网络内的云手机）的权限，两个场景要分开授权。
PERMISSION_KEY = "phone"

# Wire tool ids the phone must implement. Read-only batch.
TOOL_CONTACTS_SEARCH = "contacts.search"
TOOL_SMS_RECENT = "sms.recent"
TOOL_CALENDAR_TODAY = "calendar.today"
TOOL_NOTIFICATIONS_RECENT = "notifications.recent"
TOOL_HEALTH_SUMMARY = "health.summary"
TOOL_LOCATION_CURRENT = "location.current"
TOOL_PHOTOS_RECENT = "photos.recent"

READ_ONLY_TOOLS: frozenset[str] = frozenset(
    {
        TOOL_CONTACTS_SEARCH,
        TOOL_SMS_RECENT,
        TOOL_CALENDAR_TODAY,
        TOOL_NOTIFICATIONS_RECENT,
        TOOL_HEALTH_SUMMARY,
        TOOL_LOCATION_CURRENT,
        TOOL_PHOTOS_RECENT,
    }
)
