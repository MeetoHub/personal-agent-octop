# personal-agent-octop

把腾讯开源的多智能体平台 [Octop](https://github.com/TencentCloud/Octop) 变成**个人智能体**，双端落地：

- **NAS 端（`nas/`）**：在 NAS 上部署 Octop 本体，附我们自己的增量补丁（工单系统、反向手机通道等），**可追踪上游**。
- **手机端（`android/`）**：原生 Android App（Kotlin + Compose，非 WebView/PWA），复用 Octop 本体的 UI 设计语言。

> 本项目是**增量层**：不 fork、不重打包 Octop 上游源码。上游升级时只需重新应用 `nas/patches/`，我们的改动始终可移植。

## 目录结构

```
.
├── nas/                      # NAS 部署本体（增量）
│   ├── patches/              # 我们改的 Octop 源码文件 + MANIFEST.md（SHA/部署纪律）
│   ├── migrations/           # 数据库迁移（如 026_tickets）
│   ├── deploy/               # 部署/备份/恢复脚本
│   └── tests/                # P1–P6 阶段测试
├── android/                  # 原生 Android App
│   ├── app/src/main/         # Kotlin 源码 + Compose UI
│   ├── gradle/               # Gradle wrapper
│   └── docs/                 # 手机端接口清单
├── docs/                     # 方案文档（已脱敏）
└── upstream/                 # 如何追踪 Octop 上游的说明
```

## 为什么这样设计：追踪上游

上游是 `TencentCloud/Octop`，我们基于 `1.0.3b7`（commit `30f4aec`）。策略：

1. **不在仓库里存放整份上游源码**（47MB，且会过时）。
2. `nas/patches/` 里每个 `.py` 文件是**相对上游的完整替换文件**，带 SHA256 记录在 `MANIFEST.md`。
3. 上游升级流程见 [`upstream/README.md`](upstream/README.md)：拉取新版本 → 按 MANIFEST 比对 → 重新应用补丁 → 跑 `nas/tests/` 回归。

这样上游每次发版，我们只做“重新打补丁”，不会积累无法合并的本地大改。

## 快速开始

- NAS 端部署：见 [`nas/README.md`](nas/README.md)
- 手机端编译：见 [`android/README.md`](android/README.md)
- 架构与阶段验证：见 [`docs/`](docs/)

## 架构要点

- Octop 是**多租户宿主 + 前端**，推理循环在外部 `octop-harness`，通过 ACP 挂载。
- “专家（expert）”有独立记忆/线程/HITL；“子智能体（sub-agent）”只是 persona 文件，无记忆。二者 UI 必须分开呈现。
- 手机对 NAS 的控制走**反向 WS 通道**：手机主动连 NAS，敏感数据留在设备侧，逐工具授权 + 全局开关。详见 `docs/手机端个人智能体技术方案.md`。
- NAS 自带的 adb 手机控制（`infra/control/mobile/`）是另一条独立路线，与本 App 通道不混用。

## 许可

MIT。Octop 上游代码版权归腾讯所有；本仓库仅包含对其的增量修改。部署前请遵守上游许可。
