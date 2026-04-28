# 流量趋势持久化 + AI 智能分析 + 节点监控汇聚详情

## Goal

三个功能迭代，提升系统的数据连续性、智能化和管理可观测性：
1. 路段流量趋势从后端 API 轮询加载（3秒间隔），刷新页面后数据不丢失
2. AI 智能助手（上下文感知 + 多轮对话），支持 OpenAI 格式和 Claude API
3. 节点配置管理监控页面，丰富节点信息列并支持点击查看详情

## Requirements

### 功能1：流量趋势持久化 + 3秒轮询

- 前端"路段流量趋势"图表不再依赖 WebSocket 实时推送的内存 `historicalData`
- 改为从后端 API 轮询加载历史数据，每 3 秒请求一次
- 后端已有 `traffic_samples` 表持久化遥测数据（通过 `TelemetryIngestionService`）
- 新增后端 API：返回最近 N 条流量样本（按时间升序），支持指定路段或全部路段
- 前端 `AnalyticsView` 的趋势图从该 API 加载，刷新页面后数据恢复
- 内存 `historicalData` 可保留作为 WebSocket 实时数据源（给交通态势概览等其他页面用），但趋势图不再依赖它
- 报表结果表格保持现有查询模式不变

### 功能2：AI 智能助手（上下文感知 + 多轮对话）

**LLM 配置管理**：
- 节点配置管理中新增"AI 配置" Tab
- 可设置：
  - 提供商类型（OpenAI / Claude / 自定义 OpenAI 兼容）
  - API Base URL
  - API Key
  - 模型名
- 配置存储在 `site_settings` 表（扩展字段）或新建 `llm_config` 表
- 提供"测试连接"按钮，保存前验证 API 可用性

**对话 API**：
- POST 端点，接收用户消息 + 可选的路段上下文
- 后端将路段实时数据（流量、拥堵指数、车速等）注入 system prompt
- SSE 流式响应返回 AI 回复
- 使用用户的 `token_llm` 余额做额度控制

**聊天记录持久化**：
- 新增数据库表：`ai_chat_conversations` + `ai_chat_messages`
- 支持查询历史对话列表
- 支持加载历史消息上下文（多轮对话）

**前端 UI**：
- 侧边栏新增"AI 智能分析"页面（/ai-assistant）
- 全局浮动按钮（右下角），点击弹出对话面板
- 对话界面支持选择路段上下文（下拉选择关联路段）
- 显示对话历史列表，可切换查看
- 支持 SSE 流式显示 AI 回复

**LLM 提供商**：
- 支持 OpenAI 格式 API（覆盖 OpenAI、DeepSeek、本地 Ollama 等）
- 支持 Claude API（Anthropic 原生）
- 后端统一抽象，根据配置的提供商类型选择调用方式

### 功能3：节点监控汇聚详情

- 丰富节点表格列：名称、道路、节点 URL、健康状态、延迟、错误次数、连续失败次数
- 支持点击行展开详情面板，显示完整信息：
  - 基本信息：camera_id、名称、道路名、edge_node_id、node_url
  - 状态信息：health_status、status_reason_code、status_reason_message、last_error
  - 性能指标：latency_ms、edge_metrics（CPU、内存使用率百分比、磁盘等）
  - 时间信息：last_poll_time、last_success_time
  - 错误统计：error_count、consecutive_failures
- 后端 `getNodeHealthMap()` 已返回所有数据，前端仅需展示层改动

## Acceptance Criteria

- [ ] 页面刷新后，流量趋势图表能从后端 API 恢复历史数据
- [ ] 趋势图每 3 秒自动刷新一次
- [ ] AI 智能分析页面可通过侧边栏访问
- [ ] 全局浮动按钮在任何页面可见，点击可弹出对话面板
- [ ] 可选择路段上下文，AI 回复包含该路段实时数据分析
- [ ] AI 智能分析支持 OpenAI 格式和 Claude 格式 API
- [ ] 节点配置管理中可配置 LLM API 参数，并提供测试连接功能
- [ ] SSE 流式响应实时显示 AI 回复
- [ ] 对话历史持久化，刷新后可查看历史对话列表
- [ ] 支持多轮对话（加载历史消息作为上下文）
- [ ] 监控页面节点表格显示完整信息列（名称、道路、URL、延迟、错误等）
- [ ] 点击节点行可展开/查看详细信息面板（含 CPU/内存使用率）

## Definition of Done

- 后端新增 API 通过 curl / httpie 可验证
- 前端所有新增页面无明显 UI 错误
- 代码风格与项目现有模式一致（Vue 3 Composition API + shadcn-vue）
- Flyway 迁移脚本可重复执行无误
- Lint / 编译无报错

## Out of Scope

- Token 余额充值/管理界面（使用现有默认值）
- 监控节点的时间序列图表（仅当前状态展示）
- AI 智能分析的语音输入/输出
- AI 对话导出功能

## Technical Approach

### 功能1 技术方案

- 后端新增 `GET /api/v1/traffic/samples/recent` 端点，参数 `road_name`（可选）、`limit`（默认 120）
- 查询 `TrafficSampleRepository` 最近 N 条记录
- 前端 `AnalyticsView` 用 `setInterval` 每 3 秒调用该 API
- 趋势图数据源从 `state.historicalData` 改为 API 返回结果

### 功能2 技术方案

- 数据库：新增 `V13__ai_assistant.sql`（llm_config 字段扩展到 site_settings + 聊天表）
- 后端 LLM 抽象：`LlmClient` 接口 + `OpenAiLlmClient` / `ClaudeLlmClient` 实现
- 后端新增 `AiAssistantController`（SSE 端点 + 对话 CRUD）
- 前端新增 `AiAssistantView.vue` + 全局浮动组件

### 功能3 技术方案

- 纯前端改动，`AdminView.vue` 的 health Tab
- 丰富表格列 + 点击展开行详情

## Technical Notes

### 关键文件
- `frontend/src/store/traffic.ts` — 实时流量状态管理
- `frontend/src/views/AnalyticsView.vue` — 历史数据统计页面
- `frontend/src/views/AdminView.vue` — 节点配置管理页面
- `frontend/src/components/AppLayout.vue` — 布局和侧边栏
- `frontend/src/router.ts` — 路由配置
- `frontend/src/lib/api.ts` — API 端点定义
- `backend/.../controller/TrafficController.java` — 交通数据端点
- `backend/.../service/TrafficService.java` — 交通服务（getAllSnapshots）
- `backend/.../repository/TrafficSampleRepository.java` — 流量样本查询
- `backend/.../model/SiteSettingsEntity.java` — 站点设置（可扩展 LLM 配置）
- `backend/.../model/TokenLlmEntity.java` — LLM Token 余额
- `backend/.../service/CameraPollerService.java` — 节点健康数据
- `backend/.../config/SecurityConfig.java` — 认证配置

### 现有模式
- 前端使用 `authFetch` 做认证请求
- 后端使用 Spring Boot + JPA
- 数据库迁移使用 Flyway（当前到 V12）
- WebSocket 用于实时数据推送
- 前端 UI：Vue 3 Composition API + shadcn-vue + Tailwind CSS + ECharts
