# 文档中心

本目录汇总项目部署、答辩、需求追溯与性能验收材料。

## 目录总览

```text
docs/
├── deploy/
│   ├── local.md
│   ├── docker.md
│   └── production.md
├── defense/
├── requirements/
└── reports/
```

## 推荐阅读顺序

1. [`deploy/local.md`](deploy/local.md)：本地开发与联调
2. [`deploy/docker.md`](deploy/docker.md)：单机 Compose 启动
3. [`deploy/production.md`](deploy/production.md)：生产环境配置与上线

## 配置文件配套关系

部署文档会同时引用以下配置模板：

| 文件 | 作用 |
|------|------|
| [`../.env.example`](../.env.example) | 主站 Docker 一键启动主配置，含前后端公共地址 |
| [`../backend/.env.example`](../backend/.env.example) | backend 完整配置参考 |
| [`../frontend/.env.example`](../frontend/.env.example) | frontend 构建变量说明 |
| `frontend/.env.production` | frontend 可选覆盖文件，单独构建时使用 |
| [`../edge/.env.example`](../edge/.env.example) | edge 节点运行时变量 |

说明：
- 根 `.env` 现在同时驱动 backend runtime 与 frontend build-time 的公共地址变量。
- `frontend` 的 `VITE_*` 是 build-time config，修改后必须重新构建。

## deploy

| 文件 | 说明 | 场景 |
|------|------|------|
| [local.md](deploy/local.md) | 本地开发环境搭建与校验 | 日常开发 |
| [docker.md](deploy/docker.md) | 根 Compose 一键部署 | 演示、联调、单机测试 |
| [production.md](deploy/production.md) | 生产上线与运维配置 | 正式环境 |

## defense

| 文件 | 说明 |
|------|------|
| [index.md](defense/index.md) | 答辩材料总索引 |
| [demo-script.md](defense/demo-script.md) | 演示脚本 |
| [screenshot-checklist.md](defense/screenshot-checklist.md) | 截图清单 |
| [risk-closure.md](defense/risk-closure.md) | 风险闭环记录 |
| [metrics-table.md](defense/metrics-table.md) | 指标汇总 |

## requirements

| 文件 | 说明 |
|------|------|
| [traceability.md](requirements/traceability.md) | 需求追溯矩阵 |

## reports

| 文件 | 说明 |
|------|------|
| [gap-baseline.md](reports/gap-baseline.md) | 差距基线分析 |
| [perf-evidence-template.md](reports/perf-evidence-template.md) | 性能证据模板 |

## 使用建议

1. 第一次接手项目时，先看 `deploy/local.md` 和仓库根 `README.md`。
2. 需要 Docker 部署时，再看 `deploy/docker.md`。
3. 上生产前，务必完整走一遍 `deploy/production.md` 里的变量准备与验收清单。
