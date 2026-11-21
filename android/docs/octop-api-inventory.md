# Octop 服务端 API 全量清单

来源：`/var/apps/octop-native/target/site-packages/octop/api/routers`（octop 1.0.3b6）

> 全局前缀 `/api`，下表未含。共 **596** 条路由 / 81 个模块。

## acp.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/acp` | get_global_acp_runners |
| PUT | `/acp` | put_global_acp_runners |
| POST | `/acp/ask` | ask_acp_runner |
| POST | `/acp/install` | install_acp_runner |
| POST | `/acp/probe` | probe_acp_runner |
| GET | `/acp/{runner_name}` | get_global_acp_runner |
| PUT | `/acp/{runner_name}` | put_global_acp_runner |
| DELETE | `/acp/{runner_name}` | delete_global_acp_runner |
| GET | `/agents/{agent_id}/acp` | get_acp_config |
| PUT | `/agents/{agent_id}/acp` | put_acp_config |
| PUT | `/agents/{agent_id}/acp/tool` | put_acp_tool_toggle |
| GET | `/agents/{agent_id}/acp/{runner_name}` | get_acp_runner |
| PUT | `/agents/{agent_id}/acp/{runner_name}` | put_acp_runner |
| DELETE | `/agents/{agent_id}/acp/{runner_name}` | delete_acp_runner |

## admin.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/audit-log` | audit_log |
| GET | `/metrics` | metrics |
| GET | `/overview` | overview |

## agent_files.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/heartbeat-config` | get_heartbeat_config |
| PUT | `/agents/{agent_id}/heartbeat-config` | put_heartbeat_config |
| GET | `/agents/{agent_id}/memory/daily` | list_daily_memory |
| GET | `/agents/{agent_id}/memory/daily/{filename}` | read_daily_memory |
| DELETE | `/agents/{agent_id}/memory/daily/{filename}` | delete_daily_memory |
| POST | `/agents/{agent_id}/memory/manual-review` |  |
| GET | `/agents/{agent_id}/memory/manual-review` | get_manual_memory_review |
| GET | `/agents/{agent_id}/memory/strong` | get_strong_memory |
| PUT | `/agents/{agent_id}/memory/strong` | put_strong_memory |
| GET | `/agents/{agent_id}/memory/user` | get_user_profile |
| PUT | `/agents/{agent_id}/memory/user` | put_user_profile |

## agent_mcp.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/mcp/agents` | agent_mcp_post |
| GET | `/mcp/agents` | agent_mcp_get |
| DELETE | `/mcp/agents` | agent_mcp_delete |

## agent_tools.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/tool-settings` | get_tool_settings |
| PUT | `/agents/{agent_id}/tool-settings` | put_tool_settings |
| PATCH | `/agents/{agent_id}/tool-settings/{tool_name}` | patch_tool_setting |

## agents.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents` | list_agents |
| POST | `/agents` | create_agent |
| GET | `/agents/{agent_id}` | get_agent |
| PATCH | `/agents/{agent_id}` | patch_agent |
| DELETE | `/agents/{agent_id}` | delete_agent |
| POST | `/agents/{agent_id}/avatar` | upload_agent_avatar |
| GET | `/agents/{agent_id}/avatar` | get_agent_avatar |
| DELETE | `/agents/{agent_id}/avatar` | delete_agent_avatar |
| POST | `/agents/{agent_id}/read` | mark_agent_read |
| POST | `/agents/{agent_id}/reload` | reload_agent |
| POST | `/agents/{agent_id}/start` | start_agent |
| GET | `/agents/{agent_id}/status` | agent_status |
| POST | `/agents/{agent_id}/stop` | stop_agent |

## app_config.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/admin/app-config` | get_app_config |
| PUT | `/admin/app-config` | put_app_config |
| PUT | `/admin/app-config/raw` | put_raw_app_config |

## auth.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/captcha` | get_captcha |
| POST | `/change-password` | change_password |
| POST | `/login` | login |
| POST | `/logout` | logout |
| GET | `/me` | me |
| PATCH | `/me` | update_me |
| POST | `/me/avatar` | upload_my_avatar |
| DELETE | `/me/avatar` | delete_my_avatar |

## auth_device.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/device/register` | register_device |
| POST | `/device/revoke` | revoke_device |
| POST | `/device/token` | device_token |

## auth_ldap.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/ldap/config` | get_ldap_config |
| PUT | `/ldap/config` | put_ldap_config |
| POST | `/ldap/config/test` | test_ldap_config |
| GET | `/ldap/status` | ldap_status |

## auth_mfa.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/mfa/passkey/options` | mfa_passkey_options |
| POST | `/mfa/passkey/register/options` | passkey_register_options |
| POST | `/mfa/passkey/register/verify` | passkey_register_verify |
| DELETE | `/mfa/passkey/{credential_id}` | passkey_delete |
| POST | `/mfa/reauth` | mfa_reauth |
| POST | `/mfa/recovery/regenerate` | recovery_regenerate |
| GET | `/mfa/status` | mfa_status |
| DELETE | `/mfa/totp` | totp_disable |
| POST | `/mfa/totp/begin` | totp_begin |
| POST | `/mfa/totp/confirm` | totp_confirm |
| POST | `/mfa/verify` | mfa_verify |
| POST | `/passkey/login/options` | passkey_login_options |
| POST | `/passkey/login/verify` | passkey_login_verify |
| GET | `/passkey/status` | passkey_login_status |

## auth_oauth.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/oauth/bind/start` | oauth_bind_start |
| GET | `/oauth/callback` | oauth_callback |
| POST | `/oauth/exchange` | oauth_exchange |
| GET | `/oauth/providers/{kind}` | get_oauth_provider |
| PUT | `/oauth/providers/{kind}` | put_oauth_provider |
| POST | `/oauth/providers/{kind}/test` | test_oauth_provider |
| POST | `/oauth/start` | oauth_start |
| GET | `/oauth/status` | oauth_status |
| POST | `/oauth/unbind` | oauth_unbind |

## auth_oidc.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/oidc/callback` | oidc_callback |
| GET | `/oidc/config` | oidc_config |
| PUT | `/oidc/config` | put_oidc_config |
| POST | `/oidc/config/test` | test_oidc_config |
| POST | `/oidc/exchange` | oidc_exchange |
| POST | `/oidc/start` | oidc_start |
| GET | `/oidc/status` | oidc_status |

## backup.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `octop-export-/backup/auto` | get_auto_backup_settings |
| PUT | `octop-export-/backup/auto` | put_auto_backup_settings |
| POST | `octop-export-/backup/auto/run` | run_auto_backup_now |
| POST | `octop-export-/backup/create` | create_backup |
| GET | `octop-export-/backup/export` | export_backup |
| GET | `octop-export-/backup/files/{filename}` | download_backup_file |
| DELETE | `octop-export-/backup/files/{filename}` | remove_backup_file |
| POST | `octop-export-/backup/files/{filename}/restore` | restore_backup_file |
| POST | `octop-export-/backup/import` | import_backup |
| GET | `octop-export-/backup/list` | list_backups |
| GET | `octop-export-/backup/status` | get_backup_status |

## bridge.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/bridge/connections` | list_connections |
| POST | `/bridge/connections` | create_connection |
| GET | `/bridge/connections/{connection_id}` | get_connection |
| PATCH | `/bridge/connections/{connection_id}` | patch_connection |
| DELETE | `/bridge/connections/{connection_id}` | delete_connection |
| GET | `/bridge/connections/{connection_id}/agents` | list_remote_agents |
| POST | `/bridge/connections/{connection_id}/avatar` | upload_bridge_avatar |
| GET | `/bridge/connections/{connection_id}/avatar` | get_bridge_avatar |
| DELETE | `/bridge/connections/{connection_id}/avatar` | delete_bridge_avatar |
| WEBSOCKET | `/bridge/connections/{connection_id}/browser-stream/ws` | bridge_browser_stream_ws |
| GET | `/bridge/connections/{connection_id}/browser/env-status` | get_remote_browser_env_status |
| GET | `/bridge/connections/{connection_id}/browser/harness-sessions` | get_remote_browser_sessions |
| POST | `/bridge/connections/{connection_id}/browser/sessions/{session_id}/handoff` | post_remote_browser_handoff |
| POST | `/bridge/connections/{connection_id}/connect` | connect_connection |
| POST | `/bridge/connections/{connection_id}/disconnect` | disconnect_connection |
| GET | `/bridge/connections/{connection_id}/knowledge-bases` | list_remote_knowledge_bases |
| GET | `/bridge/connections/{connection_id}/knowledge-bases/capability` | get_remote_knowledge_capability |
| POST | `/bridge/connections/{connection_id}/probe` | probe_connection |
| GET | `/bridge/connections/{connection_id}/providers/active-model` | get_remote_active_model |
| GET | `/bridge/connections/{connection_id}/providers/resolved` | list_remote_resolved_models |
| POST | `/bridge/probe` | probe_peer |
| WEBSOCKET | `/bridge/ws` | bridge_inbound_ws |

## browser/env.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/browser/env-status` | env_status |
| POST | `/browser/install` | install |

## browser/harness.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/browser/harness-sessions` | list_harness_sessions |
| POST | `/browser/sessions/{session_id}/handoff` | handoff |
| POST | `/browser/shutdown` | shutdown_browser |

## browser/record_replay.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/browser/record-replay/replay` | replay_recording |
| POST | `/browser/record-replay/skill-content` | get_skill_content |
| POST | `/browser/record-replay/start` | record_start |
| GET | `/browser/record-replay/status` | record_status |
| POST | `/browser/record-replay/stop` | record_stop |
| POST | `/browser/record-replay/stop-and-generate-skill` | record_stop_and_generate_skill |

## browser/stream.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/browser-stream/ws` | browser_stream_ws |

## browser/uninstall.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/browser/uninstall` | uninstall_browser |

## channels.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/channels` | list_channels |
| POST | `/agents/{agent_id}/channels` | create_channel |
| POST | `/agents/{agent_id}/channels/dingtalk/qrcode/generate` | dingtalk_qrcode_generate |
| POST | `/agents/{agent_id}/channels/dingtalk/qrcode/poll` | dingtalk_qrcode_poll |
| POST | `/agents/{agent_id}/channels/feishu/bot-creator/poll` | feishu_bot_creator_poll |
| POST | `/agents/{agent_id}/channels/feishu/bot-creator/start` | feishu_bot_creator_start |
| POST | `/agents/{agent_id}/channels/feishu/bot-creator/stop` | feishu_bot_creator_stop |
| POST | `/agents/{agent_id}/channels/probe` | probe_channel_config |
| POST | `/agents/{agent_id}/channels/qq/qrcode/generate` | qq_qrcode_generate |
| POST | `/agents/{agent_id}/channels/qq/qrcode/poll` | qq_qrcode_poll |
| POST | `/agents/{agent_id}/channels/wecom/qrcode/generate` | wecom_qrcode_generate |
| POST | `/agents/{agent_id}/channels/wecom/qrcode/poll` | wecom_qrcode_poll |
| POST | `/agents/{agent_id}/channels/weixin/qrcode/generate` | weixin_qrcode_generate |
| POST | `/agents/{agent_id}/channels/weixin/qrcode/poll` | weixin_qrcode_poll |
| POST | `/agents/{agent_id}/channels/yuanbao/bot-creator/poll` | yuanbao_bot_creator_poll |
| POST | `/agents/{agent_id}/channels/yuanbao/bot-creator/start` | yuanbao_bot_creator_start |
| POST | `/agents/{agent_id}/channels/yuanbao/bot-creator/stop` | yuanbao_bot_creator_stop |
| GET | `/agents/{agent_id}/channels/{channel_id}` | get_channel |
| PATCH | `/agents/{agent_id}/channels/{channel_id}` | patch_channel |
| DELETE | `/agents/{agent_id}/channels/{channel_id}` | delete_channel |
| POST | `/agents/{agent_id}/channels/{channel_id}/test` | test_channel |
| GET | `/channels/plugin-kinds` | list_plugin_channel_kinds |

## chat/history.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/agents/{agent_id}/history-migration/start` | start_history_migration |
| GET | `/agents/{agent_id}/history-migration/status` | get_history_migration_status |
| PATCH | `/agents/{agent_id}/session` | rebind_session |
| GET | `/agents/{agent_id}/threads` | list_threads |
| POST | `/agents/{agent_id}/threads` | create_thread |
| PATCH | `/agents/{agent_id}/threads/{thread_id}` | patch_thread |
| DELETE | `/agents/{agent_id}/threads/{thread_id}` | delete_thread |
| POST | `/agents/{agent_id}/threads/{thread_id}/cancel` | cancel_thread_turn |
| GET | `/agents/{agent_id}/threads/{thread_id}/context-usage` | get_thread_context_usage |
| POST | `/agents/{agent_id}/threads/{thread_id}/fork` | fork_thread |
| GET | `/agents/{agent_id}/threads/{thread_id}/history` | get_thread_history |
| POST | `/agents/{agent_id}/threads/{thread_id}/read` | mark_thread_read |

## chat/notify_ws.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/notifications/ws` | dashboard_notifications_ws |

## chat/routes.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/agents/{agent_id}/chat/hitl/resume` | resume_hitl |
| POST | `/agents/{agent_id}/chat/polish` | polish_prompt |
| GET | `/agents/{agent_id}/chat/welcome` | get_chat_welcome |

## chat/trajectory.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/threads/{thread_id}/trajectory` | get_thread_trajectory |
| GET | `/agents/{agent_id}/threads/{thread_id}/trajectory/events/{event_id}` | get_trajectory_event |
| GET | `/agents/{agent_id}/threads/{thread_id}/trajectory/export` | export_thread_trajectory |
| GET | `/agents/{agent_id}/threads/{thread_id}/trajectory/metrics` | get_trajectory_metrics |
| GET | `/agents/{agent_id}/threads/{thread_id}/trajectory/stream` | stream_thread_trajectory |

## chat/ws.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/agents/{agent_id}/chat/ws` | dashboard_chat_ws |

## cloud.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| DELETE | `/` | logout |
| POST | `/action` | action |
| POST | `/authorize` | authorize |
| POST | `/callback` | callback |
| POST | `/import-from-tccli` | import_from_tccli |
| GET | `/instances` | instances |
| GET | `/metrics` | metrics |
| POST | `/metrics/batch` | metrics_batch |
| POST | `/metrics/cluster-trend` | metrics_cluster_trend |
| POST | `/refresh` | refresh |
| GET | `/regions` | regions |
| GET | `/status` | status |

## connectors.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/connector-instances` | list_instances |
| POST | `/connector-instances` | create_instance |
| GET | `/connector-instances/{instance_id}` | get_instance |
| PATCH | `/connector-instances/{instance_id}` | patch_instance |
| DELETE | `/connector-instances/{instance_id}` | delete_instance |
| POST | `/connector-instances/{instance_id}/agently-auth/logout` | agently_auth_logout |
| POST | `/connector-instances/{instance_id}/agently-auth/refresh` | agently_auth_refresh |
| POST | `/connector-instances/{instance_id}/agently-auth/start` | agently_auth_start |
| GET | `/connector-instances/{instance_id}/agently-auth/status` | agently_auth_status |
| POST | `/connector-instances/{instance_id}/feishu-user-auth/complete` | feishu_cli_user_auth_complete_instance |
| POST | `/connector-instances/{instance_id}/feishu-user-auth/start` | feishu_cli_user_auth_start_instance |
| POST | `/connector-instances/{instance_id}/refresh` | refresh_instance |
| POST | `/connector-instances/{instance_id}/test` | test_instance |
| GET | `/connectors/auth/{kind}/authorize-url` | auth_authorize_url |
| POST | `/connectors/auth/{kind}/exchange-code` | auth_exchange_code |
| GET | `/connectors/auth/{kind}/info` | auth_info |
| GET | `/connectors/catalog` | get_catalog |
| GET | `/connectors/custom-mcp` | get_custom_mcp |
| PUT | `/connectors/custom-mcp` | put_custom_mcp |
| PATCH | `/connectors/custom-mcp/servers/{server_name}` | patch_custom_mcp_server |
| POST | `/connectors/custom-mcp/test` | test_custom_mcp |
| POST | `/connectors/feishu-cli/user-auth/complete` | feishu_cli_user_auth_complete |
| POST | `/connectors/feishu-cli/user-auth/start` | feishu_cli_user_auth_start |
| GET | `/connectors/oauth/callback` | oauth_callback |
| GET | `/connectors/oauth/pending/{state_id}` | oauth_pending |
| POST | `/connectors/oauth/start` | oauth_start_unified |
| POST | `/connectors/oauth/{kind}/start` | oauth_start_legacy |
| POST | `/connectors/test-credentials` | test_credentials |
| GET | `/connectors/weknora/detect-local` | detect_weknora_on_octop_host |
| GET | `/connectors/{kind}/cli-status` | connector_cli_status |
| POST | `/connectors/{kind}/install-cli` | connector_install_cli |

## cron.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/cron` | list_cron |
| POST | `/agents/{agent_id}/cron` | create_cron |
| GET | `/agents/{agent_id}/cron/examples` | cron_examples |
| GET | `/agents/{agent_id}/cron/{cron_id}` | get_cron |
| PATCH | `/agents/{agent_id}/cron/{cron_id}` | patch_cron |
| DELETE | `/agents/{agent_id}/cron/{cron_id}` | delete_cron |
| POST | `/agents/{agent_id}/cron/{cron_id}/run-now` | run_now |
| GET | `/cron/settings` | cron_settings |

## decision.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/settings/decision` | get_decision_settings |
| PUT | `/settings/decision` | put_decision_settings |
| GET | `/settings/decision/laya` | get_laya_status |
| POST | `/settings/decision/laya/install` | install_laya |
| POST | `/settings/decision/laya/start` | start_laya |
| POST | `/settings/decision/laya/stop` | stop_laya |
| POST | `/settings/decision/test` | test_decision_settings |

## desktop/install.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/desktop/install` | install_desktop |

## desktop/settings.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/desktop/geometry` | set_desktop_geometry |
| GET | `/desktop/geometry` | get_desktop_geometry |

## desktop/status.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/desktop/status` | get_desktop_status |

## desktop/stream.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/desktop-stream/ws` | desktop_stream_ws |

## desktop/uninstall.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/desktop/uninstall` | uninstall_desktop |

## envs.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/envs` | list_envs |
| PUT | `/envs` |  |
| DELETE | `/envs/{key}` | delete_env |

## experts.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `octop-published-/agents/from-expert/{expert_id}` | create_agent_from_expert |
| POST | `octop-published-/agents/{agent_id}/publish-expert` | publish_agent_expert |
| GET | `octop-published-/experts` | list_experts |
| GET | `octop-published-/experts/hub` | list_expert_hub |
| GET | `octop-published-/experts/hub/{slug}` | get_expert_hub_item |
| POST | `octop-published-/experts/hub/{slug}/install` | install_expert_hub_item |
| GET | `octop-published-/experts/published` | list_published_experts |
| GET | `octop-published-/experts/published/{expert_id}` | get_published_expert |
| DELETE | `octop-published-/experts/published/{expert_id}` | unpublish_expert |
| GET | `octop-published-/experts/published/{expert_id}/avatar` | get_published_expert_avatar |
| POST | `octop-published-/experts/published/{expert_id}/install` | install_published_expert |
| POST | `octop-published-/experts/published/{expert_id}/refresh` | refresh_published_expert |
| GET | `octop-published-/experts/{expert_id}` | get_expert |

## filesystem.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/defaults` | filesystem_defaults |
| GET | `/dirs` | list_host_dirs |
| GET | `/docker-status` | get_docker_status |
| POST | `/ensure-bwrap` | ensure_bwrap |
| POST | `/ensure-docker` | post_ensure_docker |
| POST | `/mkdir` | mkdir_host_dir |
| POST | `/probe` | probe_host_dir |
| POST | `/rename` | rename_host_directory |
| GET | `/roots` | filesystem_roots |

## health.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | health |

## i18n.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/i18n/skills` | get_skill_labels |
| GET | `/i18n/tools` | get_tool_labels |

## internal_mcp.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/internal/mcp/{kind}/{instance_id}` | internal_mcp_post |
| GET | `/internal/mcp/{kind}/{instance_id}` | internal_mcp_get |
| DELETE | `/internal/mcp/{kind}/{instance_id}` | internal_mcp_delete |

## invites.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_invites |
| POST | `/` | create_invite |
| POST | `/redeem` | redeem_invite |
| POST | `/validate` | validate_invite |
| POST | `/{invite_id}/revoke` | revoke_invite |

## knowledge_bases.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/knowledge-bases` | list_bases |
| POST | `/knowledge-bases` | create_base |
| GET | `/knowledge-bases/capability` | capability |
| GET | `/knowledge-bases/default-open` | default_open_bases |
| GET | `/knowledge-bases/embedding-options` | embedding_options |
| PUT | `/knowledge-bases/feature` | put_feature |
| GET | `/knowledge-bases/graph-options` | graph_options |
| GET | `/knowledge-bases/ocr-options` | ocr_options |
| POST | `/knowledge-bases/onnx-activate` | activate_onnx_service |
| POST | `/knowledge-bases/onnx-download` | start_onnx_download |
| GET | `/knowledge-bases/onnx-download-status` | onnx_download_status |
| POST | `/knowledge-bases/onnx-test` | test_onnx_model |
| GET | `/knowledge-bases/rerank-options` | rerank_options |
| GET | `/knowledge-bases/{kb_id}` | get_base |
| PATCH | `/knowledge-bases/{kb_id}` | update_base |
| DELETE | `/knowledge-bases/{kb_id}` | delete_base |
| POST | `/knowledge-bases/{kb_id}/avatar` | upload_kb_avatar |
| GET | `/knowledge-bases/{kb_id}/avatar` | get_kb_avatar |
| DELETE | `/knowledge-bases/{kb_id}/avatar` | delete_kb_avatar |
| GET | `/knowledge-bases/{kb_id}/documents` | list_documents |
| POST | `/knowledge-bases/{kb_id}/documents` | upload_document |
| POST | `/knowledge-bases/{kb_id}/documents/text` | create_text_document |
| DELETE | `/knowledge-bases/{kb_id}/documents/{doc_id}` | delete_document |
| GET | `/knowledge-bases/{kb_id}/documents/{doc_id}/chunks` | list_document_chunks |
| GET | `/knowledge-bases/{kb_id}/documents/{doc_id}/content` | get_text_document |
| PUT | `/knowledge-bases/{kb_id}/documents/{doc_id}/content` | update_text_document |
| GET | `/knowledge-bases/{kb_id}/documents/{doc_id}/file` | download_document_file |
| POST | `/knowledge-bases/{kb_id}/documents/{doc_id}/move` | move_document |
| GET | `/knowledge-bases/{kb_id}/documents/{doc_id}/preview` | preview_document |
| POST | `/knowledge-bases/{kb_id}/documents/{doc_id}/reindex` | reindex_document |
| POST | `/knowledge-bases/{kb_id}/documents/{doc_id}/rename` | rename_document |
| POST | `/knowledge-bases/{kb_id}/folders` | create_folder |
| GET | `/knowledge-bases/{kb_id}/graph` | get_graph |
| DELETE | `/knowledge-bases/{kb_id}/graph` | delete_graph |
| POST | `/knowledge-bases/{kb_id}/graph/build` |  |
| GET | `/knowledge-bases/{kb_id}/graph/entity` | get_graph_entity |
| POST | `/knowledge-bases/{kb_id}/reindex` | reindex_base |

## managed.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/managed-agents` | create_managed_agent |
| GET | `/managed-agents/template` | managed_agent_template |

## mbti.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/mbti/apply` | apply_type |
| GET | `/mbti/current` | get_current_mbti |
| GET | `/mbti/preview/{code}` | get_persona_preview |
| GET | `/mbti/test/questions` | get_test_questions |
| POST | `/mbti/test/submit` | submit_test |
| GET | `/mbti/types` | list_types |
| GET | `/mbti/types/{code}` | get_type |

## media_generation.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | get_media_generation_settings |
| PUT | `/` | put_media_generation_settings |
| GET | `/presets` | list_media_provider_presets |
| POST | `/test` | test_media_generation_provider |

## memory.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/agents/{agent_id}/memory/atoms` | create_atom |
| POST | `/agents/{agent_id}/memory/atoms/list` | list_atoms |
| GET | `/agents/{agent_id}/memory/atoms/{atom_id}` | get_atom |
| POST | `/agents/{agent_id}/memory/atoms/{atom_id}:deprecate` | deprecate_atom |
| POST | `/agents/{agent_id}/memory/atoms/{atom_id}:replace` | replace_atom |
| POST | `/agents/{agent_id}/memory/candidates/list` | list_candidates |
| GET | `/agents/{agent_id}/memory/candidates/{candidate_id}` | get_candidate |
| POST | `/agents/{agent_id}/memory/candidates/{candidate_id}:promote` | promote_candidate |
| POST | `/agents/{agent_id}/memory/candidates/{candidate_id}:reject` | reject_candidate |
| POST | `/agents/{agent_id}/memory/entities/list` | list_entities |
| GET | `/agents/{agent_id}/memory/entities/{entity_id}` | get_entity |
| POST | `/agents/{agent_id}/memory/episodes/list` | list_episodes |
| GET | `/agents/{agent_id}/memory/episodes/{episode_id}` | get_episode |
| GET | `/agents/{agent_id}/memory/extract-config` | get_extract_config |
| PUT | `/agents/{agent_id}/memory/extract-config` | put_extract_config |
| POST | `/agents/{agent_id}/memory/journal/list` | list_journal |
| GET | `/agents/{agent_id}/memory/journal/recent` | recent_journal |
| POST | `/agents/{agent_id}/memory/raw_events/list` | list_raw_events |
| GET | `/agents/{agent_id}/memory/raw_events/{event_id}` | get_raw_event |
| GET | `/agents/{agent_id}/memory/stats/atom_kinds` | stats_atom_kinds |
| GET | `/agents/{agent_id}/memory/stats/counts` | stats_counts |
| GET | `/agents/{agent_id}/memory/stats/growth` | stats_growth |

## memory_portable.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/agents/{agent_id}/memory/portable/adopt` | adopt_agent_memory |
| POST | `/agents/{agent_id}/memory/portable/doctor` | doctor_agent_memory |
| POST | `/agents/{agent_id}/memory/portable/pack` | pack_agent_memory |
| GET | `/memory/portable/sources` | list_portable_sources |

## memory_storage.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/memory/storage/backups` | backups |
| DELETE | `/agents/{agent_id}/memory/storage/backups` | delete_backup |
| GET | `/agents/{agent_id}/memory/storage/check` | check_storage |
| GET | `/agents/{agent_id}/memory/storage/maintenance` | maintenance_status |
| POST | `/agents/{agent_id}/memory/storage/maintenance` | start_maintenance |
| POST | `/agents/{agent_id}/memory/storage/restore` | restore |

## mobile/install.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/mobile/install` | install_mobile |

## mobile/shell_ws.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/mobile/adb/shell/ws` | adb_shell_ws |

## mobile/status.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/mobile/agent-control` | get_agent_control |
| PUT | `/mobile/agent-control` | put_agent_control |
| GET | `/mobile/devices/{device}/info` | get_mobile_device_info |
| GET | `/mobile/status` | get_mobile_status |

## mobile/stream.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| WEBSOCKET | `/mobile-stream/ws` | mobile_stream_ws |

## observability.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/langfuse` | get_langfuse_config |
| PUT | `/langfuse` | put_langfuse_config |
| POST | `/langfuse/test` | test_langfuse_connection |

## ollama_models.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/ollama-models` | list_ollama_models |
| POST | `/ollama-models/download` | download_ollama_model |
| GET | `/ollama-models/download-status` | get_ollama_download_status |
| DELETE | `/ollama-models/download/{task_id}` | cancel_ollama_download |
| GET | `/ollama-models/service` | get_ollama_service |
| PUT | `/ollama-models/service` | put_ollama_service |
| DELETE | `/ollama-models/{name:path}` | delete_ollama_model |

## onnx_models.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/onnx-models/catalog` | get_catalog |
| PUT | `/onnx-models/config` | put_config |
| POST | `/onnx-models/download` | post_download |
| GET | `/onnx-models/download-status` | get_download_status |
| DELETE | `/onnx-models/local/{model_name:path}` | delete_local_model |
| GET | `/onnx-models/models/{model_name:path}/meta` | get_model_meta |
| GET | `/onnx-models/status` | get_status |
| POST | `/onnx-models/test` | post_test |

## plugins.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/plugins` | list_plugins |
| GET | `/plugins/agents/{agent_id}` | list_agent_plugins |
| PATCH | `/plugins/agents/{agent_id}` | patch_agent_plugins |
| GET | `/plugins/agents/{agent_id}/tools` | list_agent_plugin_tools |
| PATCH | `/plugins/agents/{agent_id}/tools` | patch_agent_plugin_tools |
| POST | `/plugins/install` | install_plugin |
| GET | `/plugins/market` | list_market_plugins |
| POST | `/plugins/market/{plugin_id}/install` | install_market_plugin |
| GET | `/plugins/market/{plugin_id}/ui/{file_path:path}` | get_market_plugin_ui_asset |
| POST | `/plugins/reload` | reload_plugins |
| POST | `/plugins/upload` | upload_plugin |
| PATCH | `/plugins/{plugin_id}` | patch_plugin |
| DELETE | `/plugins/{plugin_id}` | uninstall_plugin |
| GET | `/plugins/{plugin_id}/ui/{file_path:path}` | get_plugin_ui_asset |

## preferences.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/preferences` | get_preferences |
| PATCH | `/preferences` | patch_preferences |

## proactive_care.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/proactive-care` | get_proactive_care_config |
| PUT | `/agents/{agent_id}/proactive-care` | put_proactive_care_config |

## providers.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_providers |
| GET | `/` | admin_list_providers |
| POST | `/` | admin_create_provider |
| GET | `/active-model` | get_active_model |
| PUT | `/active-model` | set_active_model |
| DELETE | `/codex-oauth` | codex_oauth_logout |
| GET | `/codex-oauth/pending/{state_id}` | codex_oauth_pending |
| POST | `/codex-oauth/start` | codex_oauth_start |
| POST | `/fetch-models` | admin_fetch_provider_models |
| GET | `/magpie/discovery` | admin_discover_magpie |
| GET | `/presets` | list_provider_presets |
| GET | `/resolved` | list_resolved_models |
| POST | `/test-draft` | admin_test_provider_draft |
| PATCH | `/{provider_id}` | admin_patch_provider |
| DELETE | `/{provider_id}` | admin_delete_provider |
| POST | `/{provider_id}/test` | admin_test_provider |

## search.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/search/{provider_id}/test` |  |

## security.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | get_security_policy |
| PUT | `/` | put_security_policy |
| GET | `/defaults` | get_security_defaults |
| GET | `/tool-guard/rules` | get_tool_guard_rules |
| GET | `/tool-guard/rules/raw` | get_tool_guard_rules_raw |
| PUT | `/tool-guard/rules/raw` | put_tool_guard_rules_raw |
| POST | `/tool-guard/rules/reset` | post_tool_guard_rules_reset |

## settings.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/downloads/link` | create_download_link |
| GET | `/downloads/link/{token}` | open_download_link |
| POST | `/downloads/local` | save_local_download |
| GET | `/settings/capabilities` | get_capabilities |
| GET | `/settings/captcha` | get_captcha_settings |
| PUT | `/settings/captcha` | put_captcha_settings |
| GET | `/settings/hitl` | get_hitl_settings |
| GET | `/settings/timezone` | get_timezone_settings |
| GET | `/settings/upload` | get_upload_settings |
| GET | `/settings/webauthn` | get_webauthn_settings |
| PUT | `/settings/webauthn` | put_webauthn_settings |

## setup.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/setup/begin` | begin_setup |
| POST | `/setup/database` | apply_database |
| POST | `/setup/finish` | finish |
| POST | `/setup/initial-admin` | initial_admin |
| GET | `/setup/presets` | setup_presets |
| GET | `/setup/status` | status |
| POST | `/setup/test-database` | test_database |
| POST | `/setup/test-provider` | test_provider_draft |
| GET | `/setup/validate-token` | validate_wizard_token |
| POST | `/setup/verify-password` | verify_password |

## skill_packages.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/skill-packages` | list_skill_packages |
| POST | `/skill-packages` | create_skill_package |
| POST | `/skill-packages/from-skillhub` | create_from_skillhub |
| GET | `/skill-packages/hub/rankings` | package_hub_rankings |
| GET | `/skill-packages/hub/search` | package_hub_search |
| GET | `/skill-packages/{package_id}` | get_skill_package |
| PATCH | `/skill-packages/{package_id}` | update_skill_package |
| DELETE | `/skill-packages/{package_id}` | delete_skill_package |
| POST | `/skill-packages/{package_id}/avatar` | upload_skill_package_avatar |
| GET | `/skill-packages/{package_id}/avatar` | get_skill_package_avatar |
| DELETE | `/skill-packages/{package_id}/avatar` | delete_skill_package_avatar |
| GET | `/skill-packages/{package_id}/export` | export_skill_package |
| GET | `/skill-packages/{package_id}/skills` | list_package_skills |
| POST | `/skill-packages/{package_id}/skills` | create_package_skill |
| POST | `/skill-packages/{package_id}/skills/hub/install` | hub_install_package_skill |
| POST | `/skill-packages/{package_id}/skills/import` | import_package_skill |
| GET | `/skill-packages/{package_id}/skills/{slug}` | get_package_skill |
| PUT | `/skill-packages/{package_id}/skills/{slug}` | update_package_skill |
| DELETE | `/skill-packages/{package_id}/skills/{slug}` | delete_package_skill |
| GET | `/skill-packages/{package_id}/skills/{slug}/download` | download_package_skill_file |
| GET | `/skill-packages/{package_id}/skills/{slug}/file` | read_package_skill_file |
| GET | `/skill-packages/{package_id}/skills/{slug}/tree` | list_package_skill_tree |

## skills.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/skill-packages` | list_skill_package_mounts |
| PUT | `/agents/{agent_id}/skill-packages` | replace_skill_package_mounts |
| POST | `/agents/{agent_id}/skill-packages/{package_id}/copy` | copy_skill_package_to_workspace |
| GET | `/agents/{agent_id}/skills` | list_skills |
| POST | `/agents/{agent_id}/skills` | create_skill |
| POST | `/agents/{agent_id}/skills/copy` | copy_skill_from_agent |
| POST | `/agents/{agent_id}/skills/export` | export_skills_zip |
| POST | `/agents/{agent_id}/skills/hub/install` | hub_install_skill |
| GET | `/agents/{agent_id}/skills/hub/rankings` | hub_rankings |
| GET | `/agents/{agent_id}/skills/hub/search` | hub_search_skills |
| POST | `/agents/{agent_id}/skills/import` | import_skill_from_url |
| GET | `/agents/{agent_id}/skills/{name}` | get_skill |
| PUT | `/agents/{agent_id}/skills/{name}` | update_skill |
| DELETE | `/agents/{agent_id}/skills/{name}` | delete_skill |
| POST | `/agents/{agent_id}/skills/{name}/disable` | disable_skill |
| POST | `/agents/{agent_id}/skills/{name}/enable` | enable_skill |
| GET | `/agents/{agent_id}/skills/{name}/export` |  |
| POST | `/agents/{agent_id}/skills/{name}/push-to-package` | push_workspace_skill_to_package |
| GET | `/skills/hub/rankings` | hub_rankings_global |
| GET | `/skills/hub/search` | hub_search_skills_global |

## slash.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/slash/commands` | list_slash_commands |

## storage_backends.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_storage_backends |
| POST | `/` | create_storage_backend |
| GET | `/` | list_storage_backends_for_user |
| POST | `/probe` | probe_storage_backend_config |
| PATCH | `/{backend_id}` | patch_storage_backend |
| DELETE | `/{backend_id}` | delete_storage_backend |
| DELETE | `/{backend_id}/browse` | close_storage_backend_browse |
| GET | `/{backend_id}/download` | download_storage_backend_file |
| GET | `/{backend_id}/file` | read_storage_backend_file |
| POST | `/{backend_id}/test` | test_storage_backend |
| GET | `/{backend_id}/tree` | list_storage_backend_tree |

## subagents.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/subagents` | list_subagents |
| POST | `/agents/{agent_id}/subagents/install` | install_subagent |
| GET | `/subagent-catalog` | list_subagent_catalog |
| GET | `/subagent-catalog/divisions` | list_subagent_divisions |
| GET | `/subagent-catalog/{slug}` | get_subagent_catalog_item |

## teams.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/teams` | list_teams |
| POST | `/teams` | create_team |
| GET | `/teams/template` | team_template_files |
| GET | `/teams/{team_id}` | get_team |
| PATCH | `/teams/{team_id}` | patch_team |
| DELETE | `/teams/{team_id}` | delete_team |

## terminal.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `octop-zdotdir-/agents/{agent_id}/terminal/context` | terminal_context |
| WEBSOCKET | `octop-zdotdir-/agents/{agent_id}/terminal/ws` | terminal_ws |

## text_assist.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/text/assist` | text_assist |

## tls.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/issue` | tls_issue |
| POST | `/preflight` | tls_preflight |
| GET | `/status` | tls_status |

## update.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/update/check` | check_for_updates |
| GET | `/update/progress` | upgrade_progress |
| POST | `/update/restart` | restart_service_endpoint |
| PATCH | `/update/settings` | update_settings |
| GET | `/update/status` | update_status |
| POST | `/update/upgrade` | trigger_upgrade |

## uploads.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| POST | `/agents/{agent_id}/upload` | upload_attachment |

## usage.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/usage/export.xlsx` | user_export |
| GET | `/usage/export.xlsx` | admin_export |
| GET | `/usage/summary` | user_summary |
| GET | `/usage/summary` | admin_summary |

## user_groups.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_user_groups |
| POST | `/` | create_user_group |
| GET | `/audience` | sharing_audience |
| PATCH | `/{user_group_id}` | update_user_group |
| DELETE | `/{user_group_id}` | delete_user_group |
| POST | `/{user_group_id}/avatar` | upload_group_avatar |
| GET | `/{user_group_id}/avatar` | get_group_avatar |
| DELETE | `/{user_group_id}/avatar` | delete_group_avatar |
| GET | `/{user_group_id}/members` | list_members |
| PUT | `/{user_group_id}/members` | replace_members |

## user_roles.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_user_roles |
| POST | `/` | create_user_role |
| PATCH | `/{user_role_id}` | patch_user_role |
| DELETE | `/{user_role_id}` | delete_user_role |
| POST | `/{user_role_id}/avatar` | upload_role_avatar |
| GET | `/{user_role_id}/avatar` | get_role_avatar |
| DELETE | `/{user_role_id}/avatar` | delete_role_avatar |

## users.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | list_users |
| POST | `/` | create_user |
| POST | `/batch` | batch_users |
| GET | `/permissions` | list_permission_catalog |
| GET | `/{user_id}` | get_user |
| PATCH | `/{user_id}` | patch_user |
| DELETE | `/{user_id}` | delete_user |
| POST | `/{user_id}/avatar` | upload_user_avatar |
| GET | `/{user_id}/avatar` | get_user_avatar |
| DELETE | `/{user_id}/avatar` | delete_user_avatar |
| DELETE | `/{user_id}/mfa` | admin_reset_mfa |
| POST | `/{user_id}/reset-password` | reset_password |
| POST | `/{user_id}/unlock-login` | unlock_user_login |

## voice.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/` | admin_list_voice_providers |
| POST | `/` | admin_create_voice_provider |
| GET | `/active` | get_active_voice |
| PUT | `/active` | set_active_voice |
| GET | `/presets` | list_voice_presets |
| GET | `/providers` | list_voice_providers |
| POST | `/stt` | transcribe_audio |
| WEBSOCKET | `/stt-stream` | stream_transcription |
| POST | `/test-configuration` | admin_test_voice_configuration |
| POST | `/tts` | synthesize_speech |
| PATCH | `/{provider_id}` | admin_patch_voice_provider |
| DELETE | `/{provider_id}` | admin_delete_voice_provider |
| POST | `/{provider_id}/test` | admin_test_voice_provider |

## workspace.py

| 方法 | 路径 | 处理函数 |
|---|---|---|
| GET | `/agents/{agent_id}/media/preview` | preview_media |
| GET | `/agents/{agent_id}/workspace/archive` | export_workspace_archive |
| POST | `/agents/{agent_id}/workspace/archive` | import_workspace_archive |
| GET | `/agents/{agent_id}/workspace/doc` | read_doc |
| PUT | `/agents/{agent_id}/workspace/doc` | write_doc |
| GET | `/agents/{agent_id}/workspace/download` | download_file |
| GET | `/agents/{agent_id}/workspace/file` | read_file |
| PUT | `/agents/{agent_id}/workspace/file` | write_file |
| DELETE | `/agents/{agent_id}/workspace/file` | delete_workspace_file |
| GET | `/agents/{agent_id}/workspace/glob` | glob_files |
| GET | `/agents/{agent_id}/workspace/grep` | grep_files |
| GET | `/agents/{agent_id}/workspace/mention-index` |  |
| POST | `/agents/{agent_id}/workspace/mkdir` | mkdir_workspace_dir |
| POST | `/agents/{agent_id}/workspace/move` | move_workspace_file |
| GET | `/agents/{agent_id}/workspace/tree` | list_tree |
| POST | `/agents/{agent_id}/workspace/upload` | upload_file |
