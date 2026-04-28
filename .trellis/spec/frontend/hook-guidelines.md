# Hook Guidelines

> How hooks are used in this project.

---

## Overview

自定义 composable 统一放在 `frontend/src/composables/` 目录下，使用 Vue 3 Composition API 的 `ref`/`computed`/`watch` 管理状态。命名以 `use` 前缀开头，导出类型和函数。

---

## Custom Hook Patterns

### 单例 vs 多实例

- **页面级共享逻辑**用单例模式（模块顶层 `ref`），如路由守卫、全局主题。
- **组件级可复用逻辑**用工厂函数模式（每次调用创建新状态），如 `useAiChat(options)`。

`useAiChat` 是工厂函数模式：每个调用方（`AiAssistantView`、`AiFloatButton`）持有独立的对话和消息状态，互不干扰。

---

## SSE 流式 Composable 模式

`useAiChat` 展示了 SSE 流式通信的标准 composable 模式：

```ts
// 核心：工厂函数接受配置，返回响应式状态和方法
export function useAiChat(options: UseAiChatOptions = {}): UseAiChatReturn {
  const streaming = ref(false)
  const streamingContent = ref('')
  const abortController = ref<AbortController | null>(null)

  // 发送消息时创建 AbortController，支持取消
  // 通过 ReadableStream reader 逐块解析 SSE event
  // 生命周期：streaming=true → 解析chunk → streaming=false
}
```

关键约定：
- `AbortController` 用于取消进行中的 SSE 流，`stopGeneration()` 调用 `.abort()`
- `streamingContent` 在流式期间逐步累加，完成后清空并通过 `loadMessages()` 刷新完整消息列表
- 超时通过 `authFetch` 的 `timeoutMs` 参数控制（SSE 聊天用 120s）
- composable 不直接引用组件实例，通过 `messagesContainer` ref 实现自动滚动

---

## Data Fetching

所有 API 请求通过 `authFetch`（`lib/api.ts`）统一处理：
- 自动注入 `Authorization` header
- 401 自动跳转登录
- 支持 `AbortController` 集成
- 返回原始 `Response`，由调用方处理错误

分页 API 返回格式：`{ items: T[], total: number, page: number, size: number }`，composable 中做兼容处理：

```ts
const data = await res.json()
conversations.value = Array.isArray(data) ? data : (data.items || [])
```

---

## Naming Conventions

- composable 文件名：`useXxx.ts`（PascalCase）
- 导出的类型：`XxxOptions`、`XxxReturn`
- 响应式状态用 `ref`，只读派生用 `computed`
- 异步方法返回 `Promise<void>` 或 `Promise<T | null>`

---

## Common Mistakes

1. **忘记传递 `messagesContainer`**：自动滚动依赖外部传入的 DOM ref，不传则 `scrollToBottom` 为空操作。
2. **SSE 解析不处理 buffer 残片**：`buffer.split('\n\n')` 后必须保留最后一个未完整块，下次拼接。
3. **在 composable 内直接操作路由**：composable 应保持纯逻辑，路由跳转交给组件层。