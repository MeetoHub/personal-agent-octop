#!/usr/bin/env python3
"""
Octop 手机能力 MCP 桥。

Octop 的 MCP 客户端只支持 stdio（在本机 spawn 子进程），连不到手机。
所以这里做一个跑在 NAS 上的 stdio MCP server，把工具调用转发给手机 App 的
局域网 HTTP 接口（PhoneBridgeServer，默认 8765 端口）。

注册方式（Octop Web 端 → 专家 → 工具 → 自定义 MCP）：
    command = /usr/local/bin/python3.12   (以 which python3.12 为准)
    args    = ["<本文件绝对路径>"]
    env     = {
        "OCTOP_PHONE_URL":   "http://192.168.1.100:8765",
        "OCTOP_PHONE_TOKEN": "<手机 设置 → 手机桥 里显示的令牌>"
    }

手动自测：
    OCTOP_PHONE_TOKEN=xxx python3.12 octop_phone_mcp.py   # 进去后是 stdio，随便敲会报错，正常
    或直接:  curl "http://192.168.1.100:8765/health?token=xxx"
"""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request

from mcp.server.fastmcp import FastMCP

PHONE_BASE = os.environ.get("OCTOP_PHONE_URL", "http://192.168.1.100:8765").rstrip("/")
PHONE_TOKEN = os.environ.get("OCTOP_PHONE_TOKEN", "").strip()

mcp = FastMCP("octop-phone")


def _get(path: str, **params) -> dict:
    """调手机端接口。失败一律抛异常，让 agent 看到真实原因而不是空数据。"""
    if not PHONE_TOKEN:
        raise RuntimeError(
            "OCTOP_PHONE_TOKEN 没配置。去手机 App「设置 → 手机桥」复制令牌，填到 MCP 配置的 env 里。"
        )
    params["token"] = PHONE_TOKEN
    url = f"{PHONE_BASE}{path}?{urllib.parse.urlencode(params)}"
    try:
        with urllib.request.urlopen(url, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        if exc.code == 401:
            raise RuntimeError("手机拒绝了请求：令牌不对（401）。检查 OCTOP_PHONE_TOKEN。") from exc
        raise RuntimeError(f"手机返回 HTTP {exc.code}: {exc.reason}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(
            f"连不上手机 {PHONE_BASE}。检查：手机和 NAS 是否同一 WiFi、App 是否在前台/后台存活、"
            f"手机桥是否开启。原始错误：{exc.reason}"
        ) from exc


@mcp.tool()
def phone_status() -> str:
    """检查手机是否在线，以及三项能力有没有授权。调其他工具之前可以先看一眼。"""
    d = _get("/health")
    perms = []
    perms.append("联系人 ✓" if d.get("can_read_contacts") else "联系人 ✗")
    perms.append("短信 ✓" if d.get("can_read_sms") else "短信 ✗")
    perms.append("日历 ✓" if d.get("can_read_calendar") else "日历 ✗")
    return f"手机在线：{d.get('device')} (Android {d.get('android')})\n授权情况：{' · '.join(perms)}"


@mcp.tool()
def search_contacts(query: str = "", limit: int = 20) -> str:
    """按姓名模糊查手机联系人。query 留空则返回前 limit 个。

    Args:
        query: 姓名关键词，比如「张」「李四」。留空=全部。
        limit: 最多返回多少条，默认 20。
    """
    d = _get("/contacts", q=query, limit=max(1, min(limit, 200)))
    rows = d.get("contacts") or []
    if not rows:
        return f"没有匹配「{query or '(全部)'}」的联系人，或者用户没授权读取通讯录。"
    return "\n".join(f"- {r.get('name') or '(无名)'}  {r.get('phone')}" for r in rows)


@mcp.tool()
def recent_sms(limit: int = 20) -> str:
    """读手机最近收发的短信。

    Args:
        limit: 条数，默认 20，最多 200。
    """
    d = _get("/sms", limit=max(1, min(limit, 200)))
    rows = d.get("messages") or []
    if not rows:
        return "没有短信数据，或者用户没授权读取短信。"
    out = []
    for m in rows:
        arrow = "收" if m.get("incoming") else "发"
        body = (m.get("body") or "").replace("\n", " ")[:100]
        out.append(f"[{arrow}] {m.get('address')}: {body}")
    return "\n".join(out)


@mcp.tool()
def upcoming_calendar(days: int = 7) -> str:
    """看手机未来若干天的日程。

    Args:
        days: 往后看多少天，默认 7。
    """
    d = _get("/calendar", days=max(1, min(days, 365)))
    rows = d.get("events") or []
    if not rows:
        return f"未来 {days} 天没有日程，或者用户没授权读取日历。"
    import datetime

    out = []
    for e in rows:
        ts = (e.get("start") or 0) / 1000
        when = datetime.datetime.fromtimestamp(ts).strftime("%m-%d %H:%M")
        loc = e.get("location") or ""
        out.append(f"- {when}  {e.get('title')}" + (f"  @{loc}" if loc else ""))
    return "\n".join(out)


@mcp.tool()
def set_alarm(hour: int = -1, minute: int = -1, in_minutes: int = -1, label: str = "") -> str:
    """在手机上设一个闹钟。可以给绝对时间（hour+minute），也可以给相对时间（in_minutes）。

    Args:
        hour: 24 小时制的时，0-23。用相对时间时留空/传 -1。
        minute: 分，0-59。用相对时间时留空/传 -1。
        in_minutes: 多少分钟后响。比如用户说「20 分钟后提醒我」就传 20。
        label: 闹钟标签，会显示在闹钟界面上。
    """
    params: dict[str, object] = {}
    if in_minutes >= 0:
        params["in_minutes"] = in_minutes
    elif hour >= 0 and minute >= 0:
        params["h"] = hour
        params["m"] = minute
    else:
        return "参数不对：要么给 hour+minute，要么给 in_minutes。"
    if label:
        params["label"] = label

    d = _get("/alarm", **params)
    if d.get("ok"):
        return f"闹钟已设：{int(d.get('hour')):02d}:{int(d.get('minute')):02d}" + (f"（{label}）" if label else "")
    return "设闹钟失败：手机上没有可用的时钟应用，或系统拦截了跳转。"


@mcp.tool()
def phone_digest() -> str:
    """一次性拿到联系人 + 最近短信 + 未来 7 天日程的摘要。适合在对话开头了解用户近况。"""
    d = _get("/digest")
    return d.get("text") or "(手机没有返回内容)"


if __name__ == "__main__":
    mcp.run()
