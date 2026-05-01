# ETL数据同步调度系统 V2.0 版本变更文档

## 版本信息
- 版本号: 2.0.0
- 升级日期: 2026-04-23
- 前置版本: 1.0.0

## 变更摘要

本次升级覆盖7大维度：架构升级、功能扩展、代码优化、兼容性强化、性能优化、运维监控完善，在完全兼容现有功能、接口、数据结构的前提下，对系统进行全方位深度升级。

## 新增功能

### 架构层面
- 分层异常体系：新增 ErrorCode 枚举（17个错误码）和6个分类异常类，支持 retryable 属性区分可重试/不可重试异常
- SPI 扩展机制：数据源连接器支持 SPI 注册，新增数据源无需修改核心代码
- 配置中心：基于 etl_system_config 表的动态配置中心，支持热加载

### 数据源管理
- 统一连接池：PostgreSQL、Doris 连接器统一使用 HikariCP，支持外部化配置
- 元数据缓存：基于 Caffeine 的 LRU+TTL 缓存层，减少高频元数据查询
- 连接池监控：支持连接泄漏检测、状态监控

### 引擎核心
- 增量策略框架：支持 TIMESTAMP/AUTO_INCREMENT/BINLOG 三种可插拔增量策略
- 流式处理：DataExtractor/DataHandler 回调机制，避免大表 OOM
- 数据清洗脱敏校验：DataCleanser/DataDesensitizer/DataValidator
- 批量加载优化：BatchLoader 多值 INSERT + 批量提交，Doris Stream Load
- 统一类型映射：TypeMappingService 支持 MySQL↔PostgreSQL↔Doris 完整字段映射
- 断点续传：全量同步支持断点续传（checkpoint JSON）
- 幂等性设计：追加模式 upsert 写入，覆盖模式临时表原子替换
- 失败回滚：RollbackManager 支持 MySQL/Doris 和 PostgreSQL 不同回滚策略
- 线程池管理：ThreadPoolManager 全局线程池 + Semaphore 并发控制
- 精确进度：ProgressTracker 基于 AtomicLong 线程安全进度追踪

### 任务调度
- 任务依赖：拓扑排序执行、循环依赖检测、FINISH/SUCCESS 两种依赖类型
- 任务重试：TaskRetryExecutor 指数退避重试（最大5分钟）
- SKIPPED 状态：依赖未满足时自动跳过

### 监控告警
- 异步日志：LogQueue + AsyncLogWriter 批量写入，队列满降级策略
- 告警增强：企业微信/自定义 Webhook 通道、告警静默机制、恢复通知
- 统计分析：StatisticsAggregator 每日聚合、FailureAnalyzer 失败原因分析
- 定时清理：HistoryCleanupJob 自动清理过期数据、失败任务归档

### 运维API
- 配置中心API：/api/config/list、/api/config/{group}/{key}（GET/PUT）
- 运维监控API：/api/monitor/pool-status、thread-pool-status、cache-status、system-info
- 详细健康检查：/api/health/detail
- 维护API：/api/maintenance/clean-logs、/api/maintenance/clear-cache
- 结构化进度推送：SyncProgressMessage WebSocket 消息

## 新增工具类
- DateUtil: 线程安全日期工具（DateTimeFormatter）
- SqlBuilder: 参数化SQL构建，防SQL注入
- StringUtil: 脱敏、截断、非法字符处理
- ValidationUtil: 参数校验、类型判断
- RetryUtil: 指数退避重试框架
- RateLimiter: 令牌桶限流器

## 新增枚举
- IncrementalType: TIMESTAMP/AUTO_INCREMENT/BINLOG
- FailurePhase: EXTRACT/TRANSFORM/LOAD
- AlertType: 5种告警类型
- OnFailStrategy: SKIP_ROW/DEFAULT_VALUE/ABORT

## 数据库变更
- etl_sync_task 新增3个字段：priority、incremental_type、feature_flags
- etl_task_execution 新增6个字段：error_code、error_category、error_detail、failure_phase、affected_table、retry_attempt
- 新增4张表：etl_data_lineage、etl_task_dependency、etl_task_summary、etl_failed_task_archive
- 新增28项系统配置（ENGINE/SYSTEM/DATASOURCE/MONITOR分组）

## 兼容性保证
- 所有现有表结构保留，仅新增字段和表
- 所有现有REST API路径/参数/返回值不变
- 所有现有任务配置JSON格式兼容
- 所有现有同步逻辑核心代码保留
- Quartz表结构和调度逻辑不变
- 新增功能默认关闭（feature开关）
