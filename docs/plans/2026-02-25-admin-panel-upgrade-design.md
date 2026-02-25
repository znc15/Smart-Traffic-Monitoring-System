# 管理面板全面升级设计

日期: 2026-02-25

## 概述

对智能交通监控系统的后台管理面板进行全面升级，涵盖三大方面：视频流展示、缺失功能补齐、UI/体验升级。

## 一、数据模型变更

### CameraEntity 新增字段
- `roadName` (String, nullable) — 该摄像头监控的道路名称
- 道路作为摄像头的附属字段，不单独建表

### SiteSettingsEntity 增强
- 新增 `logoUrl` (String) — 站点 Logo 地址
- 新增 `footerText` (String) — 页脚文字
- 保留现有 `siteName` + `announcement`

### TrafficProperties 去硬编码
- 移除 `app.traffic.roads` 硬编码配置
- `TrafficService` 改为从数据库 CameraEntity 读取道路列表

## 二、视频流预览

### 摄像头管理预览
- 摄像头列表每行增加「预览」按钮
- 点击弹出 Dialog，内嵌 `<video>` 连接边缘节点 `/api/video` (WebM)
- 自动降级：WebM 不可用时回退到 `<img src="/api/stream">` (MJPEG)
- 视频源 URL 基于 `CameraEntity.streamUrl` 拼接

### 系统监控节点视频
- 边缘节点卡片中增加缩略视频预览
- 点击可展开查看
- 节点离线时显示占位图 + 离线状态

### 关键约束
- 前端直连边缘节点获取视频流，不经后端代理
- `streamUrl` 即边缘节点 HTTP 地址（如 `http://192.168.1.100:8000`）

## 三、UI/体验升级

### AdminPage 布局重构
- Tab 导航改为侧边栏 + 内容区（专业管理后台风格）
- 侧边栏：图标 + 文字，支持折叠
- 顶部：面包屑 + 管理员信息

### 摄像头管理
- 表格改为卡片网格布局（缩略预览、名称、道路、状态）
- 卡片悬停显示操作按钮
- 新增道路名称输入字段

### 用户管理
- 表格保留，增加搜索/筛选
- 角色/状态切换加确认动画

### 网站设置
- 分区卡片布局（基本信息、公告管理、外观设置）
- 实时预览效果

### 通用体验
- framer-motion 页面切换动画
- 操作 toast 提示统一
- 响应式适配移动端

## 技术栈
- 后端: Spring Boot 3.4 + JPA + PostgreSQL
- 前端: React + TypeScript + Tailwind + shadcn/ui + framer-motion
