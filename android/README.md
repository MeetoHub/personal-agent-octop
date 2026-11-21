# 手机端（原生 Android App）

Kotlin + Jetpack Compose 原生 App，**不是 WebView / PWA / TWA**。UI 复刻 Octop 本体的设计语言（圆角、配色、间距、动效 token），但针对手机重新设计信息架构。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Gradle Kotlin DSL（`gradlew`）
- 设计 token：见 `app/src/main/java/com/octop/mobile/ui/theme/OctopTokens.kt`（radius 5/8/12/18/9999、98 色、8 套配色）
- 图标：45 个缓存 SVG，对应 `lucide-react`，stroke 比例 2/24

## 编译

```bash
./gradlew assembleDebug
```

或本机用 `dev.ps1`（`build` / `run` / `shot` / `log` / `screen` / `pair`）。

> `local.properties`（本地 Android SDK 路径）已被 `.gitignore` 排除，需自行生成。

## 连接 NAS

App 里填 NAS 地址（示例 `http://< your-nas >:8089`）+ Octop 网页端同款账号密码。同 WiFi 用内网 IP，外网走节点小宝/内网穿透地址。详见 `连接说明.md`。

## 与 NAS 的两种通道

1. **读取/对话**：走 Octop 现有 HTTP API（`core/sync/OctopClient.kt`，9 个端点）。
2. **手机控制（规划/开发中）**：反向 WS 通道——手机主动连 NAS，NAS 下发工具意图，手机用原生 API 执行。第一批只读能力（通讯录/短信/日历/通知/健康/位置/相册），逐工具授权 + 全局开关。
   - 注意：Octop 自带的 `infra/control/mobile/` 是 adb 路线，与本 App 通道是两套，不混用。

## 信息架构

4 个主 Tab：对话 / 活 / 团队 / 我的。团队页把“有记忆的专家”和“无记忆的 persona（16 部门）”分开呈现，不合并。

## 后端接口清单

见 `docs/octop-api-inventory.md`。
