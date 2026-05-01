# ETL数据同步调度系统 V2.0 升级指南

## 升级前检查清单

1. 确认当前版本为 1.0.0
2. 备份 etl_system 数据库：`mysqldump -u root -p etl_system > etl_system_backup.sql`
3. 确认 Docker 服务正常运行
4. 确认前端服务已停止（升级后端期间前端不可用）
5. 记录当前运行中的任务状态

## 数据库升级步骤

```bash
# 1. 执行升级脚本
docker exec -i etl-mysql-system mysql -uroot -proot123456 etl_system < docker/mysql/init/02-upgrade-v2.sql

# 2. 验证升级结果
docker exec etl-mysql-system mysql -uroot -proot123456 etl_system -e "
  SHOW TABLES LIKE 'etl_%';
  SELECT COUNT(*) FROM etl_system_config;
  DESC etl_sync_task;
  DESC etl_task_execution;
"
```

预期结果：
- 新增4张表：etl_data_lineage、etl_task_dependency、etl_task_summary、etl_failed_task_archive
- etl_sync_task 新增 priority、incremental_type、feature_flags 字段
- etl_task_execution 新增 error_code、error_category、error_detail、failure_phase、affected_table、retry_attempt 字段
- etl_system_config 配置数从5条增加到33条

## 应用升级步骤

```bash
# 1. 编译项目
cd D:/data/zg/etl/etl-sync-system
mvn clean package -DskipTests -q

# 2. 停止当前运行的服务
# (根据实际部署方式停止)

# 3. 启动新版本服务
java -jar etl-api/target/etl-api-1.0.0.jar --spring.profiles.active=docker

# 4. 验证服务启动
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/detail
```

## 配置迁移说明

V2.0 新增了28项系统配置，默认值已通过升级脚本初始化。关键配置说明：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| feature.binlog_incremental.enabled | false | Binlog增量策略，需Debezium环境 |
| feature.desensitize.enabled | false | 数据脱敏功能 |
| feature.data_lineage.enabled | false | 数据血缘功能 |
| feature.stream_load_doris.enabled | false | Doris Stream Load优化 |
| sync.core.pool.size | 4 | 同步线程池核心大小 |
| sync.max.pool.size | 16 | 同步线程池最大大小 |
| global.max.concurrency | 10 | 全局最大并发任务数 |

### 功能开关启用方式

通过配置中心API启用新功能：
```bash
# 启用数据脱敏
curl -X PUT http://localhost:8080/api/config/ENGINE/feature.desensitize.enabled \
  -H "Content-Type: application/json" \
  -d '{"value": "true"}'

# 启用数据血缘
curl -X PUT http://localhost:8080/api/config/ENGINE/feature.data_lineage.enabled \
  -H "Content-Type: application/json" \
  -d '{"value": "true"}'
```

## 回滚方案

如果升级后出现问题，可按以下步骤回滚：

1. 停止新版本服务
2. 恢复数据库备份：`mysql -u root -p etl_system < etl_system_backup.sql`
3. 启动旧版本服务
4. 新增的表和字段不影响旧版本运行（MyBatis-Plus会忽略未映射的字段）

## 升级后验证步骤

1. 基础验证：
```bash
curl http://localhost:8080/api/health                          # 健康检查
curl http://localhost:8080/api/datasource/list                  # 数据源列表
curl http://localhost:8080/api/task/list                        # 任务列表
curl http://localhost:8080/api/monitor/overview                 # 系统概览
```

2. 新增API验证：
```bash
curl http://localhost:8080/api/config/list                      # 配置列表
curl http://localhost:8080/api/monitor/pool-status              # 连接池状态
curl http://localhost:8080/api/monitor/thread-pool-status       # 线程池状态
curl http://localhost:8080/api/monitor/cache-status             # 缓存状态
curl http://localhost:8080/api/monitor/system-info              # 系统信息
curl http://localhost:8080/api/health/detail                    # 详细健康检查
```

3. 功能验证：
- 手动触发一次全量同步任务，验证正常执行
- 手动触发一次增量同步任务，验证增量策略
- 查看执行记录和日志是否正常记录
- 检查连接池状态和线程池状态
