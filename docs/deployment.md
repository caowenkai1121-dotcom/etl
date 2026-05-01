# ETL数据同步调度系统 - 部署文档

## 一、环境准备

### 1.1 系统要求
- 操作系统：Linux/Windows/MacOS
- Docker：20.x 或更高版本
- Docker Compose：2.x 或更高版本
- 内存：至少 8GB（推荐 16GB）
- 磁盘：至少 50GB 可用空间

### 1.2 端口规划

| 服务 | 端口 | 说明 |
|-----|------|------|
| ETL Server | 8080 | 主服务端口 |
| 前端界面 | 3000 | Web界面 |
| MySQL System | 3306 | 系统数据库 |
| MySQL Source | 3307 | 测试源数据库 |
| PostgreSQL | 5432 | PostgreSQL |
| Kafka | 9092 | Kafka服务 |
| Zookeeper | 2181 | Zookeeper |
| Debezium Connect | 8083 | Debezium Connect REST API |

## 二、快速部署

### 2.1 一键启动
```bash
cd docker
docker-compose up -d
```

### 2.2 查看服务状态
```bash
docker-compose ps
```

### 2.3 查看日志
```bash
docker-compose logs -f etl-server
```

## 三、服务验证

### 3.1 健康检查
```bash
curl http://localhost:8080/api/health
```

### 3.2 访问地址
- API文档：http://localhost:8080/api/doc.html
- 前端界面：http://localhost:3000

## 四、CDC实时同步配置

### 4.1 架构说明
```
MySQL源库 -> Debezium Connect -> Kafka -> ETL系统 -> 目标库
```

### 4.2 MySQL Binlog配置

确保MySQL开启Binlog（ROW模式）：
```sql
SHOW VARIABLES LIKE 'log_bin';        -- 应为 ON
SHOW VARIABLES LIKE 'binlog_format';  -- 应为 ROW
```

创建Debezium用户：
```sql
CREATE USER 'debezium'@'%' IDENTIFIED BY 'debezium';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'debezium'@'%';
FLUSH PRIVILEGES;
```

### 4.3 注册Debezium连接器

通过ETL系统前端界面配置，或使用REST API：

```bash
curl -X PUT -H "Content-Type: application/json" \
  -d '{
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "database.hostname": "mysql-source",
    "database.port": "3306",
    "database.user": "root",
    "database.password": "root123456",
    "database.include.list": "source_db",
    "topic.prefix": "etl-mysql",
    "snapshot.mode": "initial"
  }' \
  http://localhost:8083/connectors/etl-mysql-connector/config
```

### 4.4 检查连接器状态
```bash
# 查看所有连接器
curl http://localhost:8083/connectors

# 查看连接器状态
curl http://localhost:8083/connectors/etl-mysql-connector/status
```

### 4.5 Kafka验证

```bash
# 查看Topic列表
docker exec -it etl-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# 消费消息（测试）
docker exec -it etl-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic etl-mysql.source_db.users --from-beginning
```

## 五、PostgreSQL CDC配置

### 5.1 WAL级别配置
```sql
SHOW wal_level;  -- 应为 logical
```

### 5.2 创建Debezium用户
```sql
CREATE USER debezium WITH REPLICATION PASSWORD 'debezium123';
GRANT CONNECT ON DATABASE etl_db TO debezium;
GRANT USAGE ON SCHEMA public TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
```

## 六、API使用示例

### 6.1 创建数据源
```bash
curl -X POST http://localhost:8080/api/datasource \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MySQL源库",
    "type": "MYSQL",
    "host": "mysql-source",
    "port": 3306,
    "databaseName": "source_db",
    "username": "root",
    "password": "root123456"
  }'
```

### 6.2 创建同步任务
```bash
curl -X POST http://localhost:8080/api/task \
  -H "Content-Type: application/json" \
  -d '{
    "name": "用户表同步",
    "sourceDsId": 1,
    "targetDsId": 2,
    "syncMode": "CDC",
    "syncScope": "SINGLE_TABLE",
    "tableConfig": [{"sourceTable": "t_user", "targetTable": "t_user"}],
    "batchSize": 1000
  }'
```

### 6.3 启动CDC任务
```bash
curl -X POST http://localhost:8080/api/cdc/task/1/start
```

### 6.4 创建定时任务
```bash
curl -X POST "http://localhost:8080/api/scheduler/task/1?cronExpression=0%200%202%20*%20*%20%3F"
```

## 七、故障排查

### 7.1 查看服务日志
```bash
docker-compose logs -f etl-server
docker-compose logs -f debezium-connect
```

### 7.2 Kafka消费问题
```bash
# 查看消费者组
docker exec -it etl-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# 重置消费者位点
docker exec -it etl-kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group etl-cdc-group-1 --reset-offsets --to-earliest --execute
```

### 7.3 数据同步验证
```bash
# 查看源库数据
docker exec -it etl-mysql-source mysql -uroot -proot123456 source_db -e "SELECT * FROM users;"

# 查看目标库数据
docker exec -it etl-mysql-system mysql -uroot -proot123456 target_db -e "SELECT * FROM users;"
```

## 八、数据备份与恢复

### 8.1 数据库备份
```bash
docker exec etl-mysql-system mysqldump -u root -proot123456 etl_system > etl_system_backup.sql
```

### 8.2 数据恢复
```bash
docker exec -i etl-mysql-system mysql -u root -proot123456 etl_system < etl_system_backup.sql
```

## 九、停止服务

```bash
cd docker
docker-compose down

# 删除数据卷（清空所有数据）
docker-compose down -v
```

---
*文档更新时间：2026-04-22*
