# 流量趋势持久化 + AI 智能助手 + 监控节点详情

## Goal

三个功能迭代，提升系统的数据连续性、智能化和管理可观测性：
1. 路段流量趋势不再随页面刷新丢失，持久化到后端数据库
2. AI 智能助手，可分析路段交通状况并提出建议，支持 OpenAI/Claude 格式 API
3. 系统管理监控页面，展示节点完整信息并支持点击查看详情

## What I already know

### 功能1：流量趋势持久化
- 当前 `store/traffic.ts` 的 `historicalData` 是纯内存 reactive 数组，最多 120 条
- `pushHistory()` 在每次 WebSocket 消息时追加一个数据点（时间 + 各路段 total）
- 后端已有 `TrafficSampleEntity`、`TrafficSampleRepository`、`MirrorWriteService`
- 后端 `CameraPollerService` 每 3 秒轮询一次边缘节点并更新 `TrafficService`

### 功能2：AI 智能助手
- 后端已有 `token_llm` 表（用户 LLM token 余额，默认 5000）
- 需要新增：LLM 配置表（API Key、Base URL、模型名、提供商类型）、对话 API、前端 UI
- 前端入口：侧边栏新增"AI 助手"独立页面 + 全局浮动快捷按钮
- 支持接入 OpenAI 格式和 Claude 格式 API
- API 配置在系统管理中设置（AdminView 站点设置 Tab 或新增 Tab）

### 功能3：监控节点详情
- 后端 `CameraPollerService.getNodeHealthMap()` 已返回丰富数据：
  - camera_id, name, road_name, edge_node_id, node_url
  - online, health_status, status_reason_code, status_reason_message
  - last_success_time, last_poll_time, latency_ms
  - error_count, consecutive_failures, last_error, last_error_stage
  - edge_metrics (CPU、内存等)
- 前端当前只显示 node_id、心跳、CPU、内存、状态，不能点开查看
- 需要丰富表格列 + 支持点击行展开详情

## Requirements

### 功能1：流量趋势持久化
- 后端在每次轮询到有效交通数据时，将流量样本写入数据库（复用/扩展现有 entity）
- 前端 `AnalyticsView` 的"路段流量趋势"图表从后端 API 加载历史数据，而非依赖内存
- 内存中的 `historicalData` 仍保留用于实时推送，但页面刷新后从后端恢复
- API 支持时间范围查询和粒度（hourly/daily/weekly/monthly）

### 功能2：AI 智能助手
- **LLM 配置管理**：在系统管理中新增"AI 配置" Tab，可设置：
  - 提供商类型（OpenAI / Claude / 自定义 OpenAI 兼容）
  - API Base URL
  - API Key
  - 模型名
  - 配置存储在 site_settings 或新建配置表
- **对话 API**：
  - POST 端点，接收用户消息 + 可选的路段上下文
  - 后端将路段实时数据（流量、拥堵指数、车速等）注入 system prompt
  - 流式响应（SSE）返回 AI 回复
  - 使用用户的 token_llm 余额做额度控制
- **历史聊天记录**：
  - 对话历史持久化到数据库（ai_chat_conversations + ai_chat_messages 表）
  - 支持查询历史对话列表
  - 支持加载历史消息上下文（多轮对话）
- **前端 UI**：
  - 侧边栏新增"AI 助手"页面（/ai-assistant）
  - 全局浮动按钮（右下角），点击弹出对话面板
  - 对话界面支持选择路段上下文
  - 显示对话历史列表，可切换查看
  - 显示 AI 分析结果和建议

### 功能3：监控节点详情
- 丰富节点表格列：名称、道路、节点 URL、健康状态、延迟、错误次数、连续失败次数
- 支持点击行展开详情面板，显示完整信息：
  - 基本信息：节点 ID、名称、道路名、edge_node_id、node_url
  - 状态信息：health_status、status_reason_code、status_reason_message、last_error
  - 性能指标：latency_ms、edge_metrics（CPU、内存、磁盘等）
  - 时间信息：last_poll_time、last_success_time
  - 错误统计：error_count、consecutive_failures

## Acceptance Criteria

- [ ] 页面刷新后，流量趋势图表能从后端恢复历史数据
- [ ] AI 助手页面可通过侧边栏访问
- [ ] 全局浮动按钮在任何页面可见，点击可弹出对话面板
- [ ] AI 助手可成功调用 OpenAI/Claude 格式 API 并返回流式响应
- [ ] 对话历史持久化，刷新后可查看历史对话列表
- [ ] 支持多轮对话（加载历史消息作为上下文）
- [ ] 系统管理中可配置 LLM API 参数
- [ ] 监控页面节点表格显示完整信息列
- [ ] 点击节点行可展开/查看详细信息面板

## Definition of Done

- 后端新增 API 通过 curl / httpie 可验证
- 前端所有新增页面无明显 UI 错误
- 代码风格与项目现有模式一致（Vue 3 Composition API + shadcn-vue）

## Out of Scope

- Token 余额充值/管理界面（使用现有默认值）
- 监控节点的时间序列图表（仅当前状态展示）

## Technical Notes

### 关键文件
- `frontend/src/store/traffic.ts` — 实时流量状态管理
- `frontend/src/views/AnalyticsView.vue` — 数据分析页面
- `frontend/src/views/AdminView.vue` — 系统管理页面
- `frontend/src/components/AppLayout.vue` — 布局和侧边栏
- `frontend/src/router.ts` — 路由配置
- `backend/.../service/CameraPollerService.java` — 节点健康数据
- `backend/.../model/TrafficSampleEntity.java` — 流量样本实体
- `backend/.../model/TokenLlmEntity.java` — LLM Token 余额
- `backend/.../model/SiteSettingsEntity.java` — 站点设置（可扩展存 LLM 配置）

### 现有模式
- 前端使用 `authFetch` 做认证请求
- 后端使用 Spring Boot + JPA
- 数据库迁移使用 Flyway（V1__init.sql）
- WebSocket 用于实时数据推送
