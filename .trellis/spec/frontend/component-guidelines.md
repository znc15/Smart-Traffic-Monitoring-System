# Component Guidelines

> How components are built in this project.

---

## Overview

前端使用 Vue 3 `<script setup>` + TypeScript，UI 组件基于 shadcn-vue（Radix Vue 封装），样式用 Tailwind CSS。组件按功能域组织在 `src/components/` 下。

---

## Component Structure

```
src/components/
├── ui/          # shadcn-vue 基础组件（button, select, badge 等）
├── ai/          # AI 聊天相关组件
│   ├── index.ts          # 统一导出
│   ├── MarkdownBody.vue  # Markdown 渲染
│   ├── ChatMessage.vue   # 单条消息气泡
│   ├── RoadSelector.vue  # 道路下拉选择
│   └── PresetQuestions.vue # 预设推荐问题
└── AiFloatButton.vue     # 全局悬浮 AI 助手
```

文件模板：
```vue
<template>
  <!-- 模板 -->
</template>

<script setup lang="ts">
// imports → props → emits → 响应式状态 → 方法
</script>

<style scoped>
/* 仅在必要时使用 scoped style */
</style>
```

---

## Props Conventions

- 用 `defineProps<T>()` 泛型语法，类型从 composable 导出类型引用
- 可选 props 标注 `?`，不使用 `withDefaults`（直接用默认值更清晰）
- 事件用 `defineEmits<T>()` 泛型语法

示例（`ChatMessage.vue`）：
```ts
const props = defineProps<{
  message: Message
  showRegenerate?: boolean
}>()

defineEmits<{
  regenerate: []
}>()
```

---

## 聊天组件模式

### MarkdownBody

封装 `v-html` 渲染 Markdown 内容。scoped style 定义 `.markdown-body` 样式（代码块、表格、引用等），避免全局污染。**所有展示 AI 回复的地方必须统一使用此组件**，不要重复定义 Markdown 样式。

### ChatMessage

单条消息渲染组件，负责：
- 用户/AI 消息气泡布局（左右对齐 + 不同颜色）
- 角色头像（User/Bot 图标）
- 时间戳格式化
- 复制按钮（clipboard API）
- 重新生成按钮（仅最后一条 AI 消息显示，由父组件控制 `showRegenerate`）

### RoadSelector

`v-model` 双绑定组件，挂载时从 API 加载道路列表。支持"全部道路"空值选项。可禁用（流式生成期间）。

### PresetQuestions

纯展示组件，点击预设问题通过 `emit('select', question)` 通知父组件。不管理任何状态。

---

## Styling Patterns

- 优先使用 Tailwind CSS 工具类
- `scoped` style 仅用于无法用 Tailwind 表达的场景（如 `.markdown-body` 内部元素样式、CSS 变量引用）
- 颜色使用 HSL CSS 变量（`hsl(var(--primary))`），保持主题一致性
- 不在组件中使用全局样式或 `!important`

---

## Accessibility

- 表单控件绑定 `label`
- 交互按钮提供 `title` 属性
- 键盘操作：Enter 发送、Shift+Enter 换行、Escape 取消编辑
- 加载状态使用 Skeleton 骨架屏而非文字提示

---

## Common Mistakes

1. **在多处重复定义 `.markdown-body` 样式**：必须统一使用 `MarkdownBody` 组件。
2. **直接在视图中写 SSE 解析逻辑**：流式通信逻辑统一在 `useAiChat` composable 中。
3. **忘记 `group` class 导致 hover 操作按钮不显示**：消息操作按钮依赖 Tailwind `group-hover` 机制。