# 报表表结构落地与切流说明

## 迁移目录与版本策略

- 迁移目录：`easyfamily-backend/src/main/resources/db/migration`
- 命名规则：`V{版本号}__{变更说明}.sql`
- 当前基线：
  - `V1__init_reporting_tables.sql`
  - 创建 `report_event`、`report_metric_daily`、`report_task_run`

## 指标映射规则（现有 3 个报表接口）

- `GET /api/v1/admin/reports/dau`
  - 指标来源：`report_metric_daily`
  - 映射：`metric_name='dau'`，`metric_value=DAU`

- `GET /api/v1/admin/reports/feature-hot`
  - 指标来源：`report_metric_daily`
  - 映射：`metric_name='feature_pv'`，`dim1=feature_name`，`metric_value=pv`

- `GET /api/v1/admin/reports/query-overview`
  - 指标来源：`report_metric_daily`
  - 映射：
    - `metric_name='query_total'` -> `totalQueryCount`
    - `metric_name='query_cache_hit'` -> `cacheHitCount`
    - `cacheHitRate = cacheHitCount / totalQueryCount`
  - 配置类字段（限额、provider）继续读取运行时配置服务，不走报表聚合表

## 事件入库与日聚合

- 查询链路写入 `report_event`（双写：保留内存计数 + 写事件明细）
- 读取报表前，会对当天执行一次轻量聚合刷新到 `report_metric_daily`
- 当 `report_metric_daily` 为空时，支持用当前内存快照做一次回填，避免切换初期空报表

## 切流/回滚开关

在 `application.yml` 中通过以下开关控制：

- `easyfamily.report.read-from-metric-table`
  - `true`：报表优先读 `report_metric_daily`
  - `false`：回退读内存统计（快速回滚）

- `easyfamily.report.backfill-from-memory-on-empty`
  - `true`：聚合表为空时，用内存快照回填一次
  - `false`：不回填，直接按当前数据源返回

## 上线建议

1. 先上线建表与双写，保持 `read-from-metric-table=false` 观察数据。
2. 对比内存报表与聚合表报表（DAU、feature PV、query total/cache hit）。
3. 误差在可接受范围后，将 `read-from-metric-table` 切为 `true`。
4. 若发现异常，立即改回 `false` 完成回滚，排查后再切流。
