"""端到端验收：手机反向通道（PATCH-007）。

用法（在 NAS 上跑，或任何能访问 8089 的机器）：

    OCTOP_URL=http://127.0.0.1:8089 OCTOP_USER=... OCTOP_PASS=... \\
      env PYTHONPATH=/vol1/@appcenter/octop-native/site-packages \\
      /var/apps/python312/target/bin/python3.12 t_phone_e2e.py

退出码 0 = 全通过。
"""

from __future__ import annotations

import asyncio
import json
import os
import sys

import httpx
import websockets

BASE = os.environ.get("OCTOP_URL", "http://127.0.0.1:8089").rstrip("/")
WS = BASE.replace("http://", "ws://").replace("https://", "wss://") + "/api/phone/ws"
USER = os.environ.get("OCTOP_USER", "")
PWD = os.environ.get("OCTOP_PASS", "")

failures: list[str] = []


def check(name: str, ok: bool, detail: str = "") -> None:
    print(("PASS " if ok else "FAIL ") + name + (f"  {detail}" if detail else ""))
    if not ok:
        failures.append(name)


async def main() -> int:
    if not USER or not PWD:
        print("OCTOP_USER / OCTOP_PASS required")
        return 2

    async with httpx.AsyncClient(timeout=15) as c:
        r = await c.post(f"{BASE}/api/auth/login", json={"username": USER, "password": PWD})
        check("login", r.status_code == 200, str(r.status_code))
        if r.status_code != 200:
            return 1
        token = r.json().get("access_token", "")
        h = {"Authorization": f"Bearer {token}"}

        st = await c.get(f"{BASE}/api/phone/status", headers=h)
        check("status endpoint", st.status_code == 200, str(st.status_code))
        check("starts disconnected", st.json().get("connected") is False)

        async with websockets.connect(f"{WS}?token={token}") as ws:
            hello = json.loads(await asyncio.wait_for(ws.recv(), 5))
            check("hello_ok", hello.get("type") == "hello_ok")
            check("protocol v1", hello.get("protocol") == 1)
            check("7 tools advertised", len(hello.get("tools") or []) == 7)

            await ws.send(
                json.dumps(
                    {"type": "hello", "device": {"model": "e2e"}, "capabilities": ["contacts"]}
                )
            )
            await ws.send(json.dumps({"type": "ping"}))
            reply = json.loads(await asyncio.wait_for(ws.recv(), 5))
            check("pong", reply.get("type") == "pong")

            st = await c.get(f"{BASE}/api/phone/status", headers=h)
            body = st.json()
            check("registered", body.get("connected") is True)
            check("device seen", (body.get("device") or {}).get("model") == "e2e")

        await asyncio.sleep(0.4)
        st = await c.get(f"{BASE}/api/phone/status", headers=h)
        check("unregistered after close", st.json().get("connected") is False)

    try:
        async with websockets.connect(WS) as ws:
            await asyncio.wait_for(ws.recv(), 3)
        check("no-token rejected", False, "connection stayed open")
    except Exception as exc:  # noqa: BLE001 - any close is a pass
        check("no-token rejected (4001)", "4001" in str(exc), str(exc)[:80])

    print(f"\n{'ALL PASS' if not failures else 'FAILURES: ' + ', '.join(failures)}")
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
