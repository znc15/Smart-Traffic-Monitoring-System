# frontend-vue

Vue3 + TypeScript + ECharts 前端，当前为系统唯一入口。

## 访问入口

- 默认入口：`http://localhost:5173/`

## 功能范围

- 登录
- 首页监控（路段状态 + 视频预览）
- 分析页（图表 + 报表查询 + JSON/XLSX 下载）
- 管理员页（用户、摄像头、站点设置、系统监控）

## 契约约束

- 读取优先 `snake_case`
- 兼容读取 `camelCase`（保留一个迭代）
- 提交按后端当前契约写入

## 本地运行

```bash
cd frontend-vue
pnpm install
pnpm dev --port 5174
```

## 构建

```bash
cd frontend-vue
pnpm build
```
