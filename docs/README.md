# 文档中心

> 智能交通监控系统（Smart Traffic Monitoring System）项目文档目录，涵盖部署指南、答辩材料、需求追溯与性能报告。

---

## 目录总览

```
docs/
├── deploy/                        # 部署文档
│   ├── local.md                   # 本地开发部署
│   ├── docker.md                  # Docker 容器部署
│   └── production.md              # 生产环境部署
├── defense/                       # 答辩与演示材料
│   ├── index.md                   # 答辩文档索引
│   ├── demo-script.md             # 演示脚本
│   ├── screenshot-checklist.md    # 截图清单
│   ├── risk-closure.md            # 风险关闭记录
│   └── metrics-table.md           # 指标表
├── requirements/                  # 需求管理
│   └── traceability.md            # 需求追溯矩阵
└── reports/                       # 报告模板
    ├── gap-baseline.md            # 差距基线分析
    └── perf-evidence-template.md  # 性能证据模板
```

---

## deploy — 部署文档

提供从本地开发到生产上线的完整部署指引。

| 文件 | 说明 | 适用场景 |
|------|------|----------|
| [local.md](deploy/local.md) | 本地开发环境搭建与启动流程 | 日常开发、调试 |
| [docker.md](deploy/docker.md) | Docker Compose 一键部署 | 集成测试、演示环境 |
| [production.md](deploy/production.md) | 生产环境部署与运维配置 | 正式上线、性能调优 |

**推荐阅读顺序**：`local.md` → `docker.md` → `production.md`

---

## defense — 答辩与演示材料

答辩准备所需的全部材料，从演示脚本到风险闭环记录。

| 文件 | 说明 |
|------|------|
| [index.md](defense/index.md) | 答辩文档总索引，串联所有材料 |
| [demo-script.md](defense/demo-script.md) | 演示流程脚本，含操作步骤与话术 |
| [screenshot-checklist.md](defense/screenshot-checklist.md) | 截图清单，确保关键界面全部留档 |
| [risk-closure.md](defense/risk-closure.md) | 风险关闭记录，展示问题解决过程 |
| [metrics-table.md](defense/metrics-table.md) | 核心指标汇总表（性能、准确率等） |

---

## requirements — 需求管理

| 文件 | 说明 |
|------|------|
| [traceability.md](requirements/traceability.md) | 需求追溯矩阵，将需求项映射到代码实现与测试用例 |

需求追溯矩阵确保每个功能需求都有对应的实现与验证，是答辩时证明项目完整性的重要依据。

---

## reports — 报告模板

| 文件 | 说明 |
|------|------|
| [gap-baseline.md](reports/gap-baseline.md) | 差距基线分析，对比目标指标与当前状态 |
| [perf-evidence-template.md](reports/perf-evidence-template.md) | 性能证据模板，用于记录与呈现测试结果 |

---

## 使用建议

1. **首次接触项目**：先阅读 `deploy/local.md` 搭建本地环境，再浏览其余文档了解全貌。
2. **准备答辩**：按 `defense/index.md` 索引逐项检查，配合 `screenshot-checklist.md` 确认截图完备。
3. **性能验收**：使用 `reports/perf-evidence-template.md` 模板记录测试数据，填入 `defense/metrics-table.md`。
4. **需求变更**：及时更新 `requirements/traceability.md`，保持需求与实现的同步。
