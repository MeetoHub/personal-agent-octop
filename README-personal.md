# personal-agent-octop

这是 [TencentCloud/Octop](https://github.com/TencentCloud/Octop) 的 fork（个人智能体版）。

主体就是 Octop 完整源码，所有个人智能体改造都在这个 fork 上提交，通过 `git merge upstream/main` 追踪腾讯上游最新代码。

## 我们的改动在哪

- **NAS/本体改动**：直接改在 `src/octop/` 里（工单 tickets、手机反向通道 phone），已提交在本仓库历史中，`git log` 可见。
- **手机 App**：`android/` 目录（原生 Kotlin/Compose）。
- **方案文档**：`docs/personal-agent/`（已脱敏）。

## 追踪上游

```bash
git fetch upstream
git merge upstream/main
```

冲突只在双方都改过的少数文件（如 `app.py`），逐个解决即可。详见 `docs/personal-agent/追踪上游说明.md`。

> 本 fork 基于上游 commit `30f4aec`（1.0.3b7）。上游代码版权归腾讯，许可 MIT；我们的增量改动同遵 MIT。
