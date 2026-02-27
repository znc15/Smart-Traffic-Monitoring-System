# 风险闭环

## 已识别风险
1. ByteTrack 依赖失败导致追踪不可用。
2. Vue 默认入口切换后出现回归。
3. 双写阶段出现主从数据漂移。
4. Redis 缓存导致短时不一致。

## 处置策略
1. `TRACKER_BACKEND=simple` 立即降级。
2. 保留 `/react` 回滚入口。
3. 使用 `scripts/check_mirror_consistency.sh` 持续校验。
4. 写后主动失效 + 短 TTL。
