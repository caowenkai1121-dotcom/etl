# ETL数据同步调度系统

企业级实时ETL数据同步调度系统，支持MySQL、PostgreSQL、Apache Doris之间的全量、增量、实时CDC数据同步。

## 🚀 功能特性

### 数据源支持
- **源端**: MySQL 8.0、PostgreSQL 15、Apache Doris
- **目标端**: MySQL、PostgreSQL、Doris
- 支持数据源连接测试、配置加密、元数据获取

### 同步模式
- **全量同步**: 一次性同步整表数据
- **增量同步**: 基于时间戳或自增ID的增量同步
- **实时CDC同步**: 
  - MySQL: **基于Debezium** (生产推荐)
  - PostgreSQL: 基于Debezium
  - 备选模式: 轮询模式(适合开发测试环境)

### 同步范围
- 单表同步
- 多表批量同步
- 整库自动同步
- 自动创建目标表、字段类型映射

### 高级能力
- 断点续传
- 失败重试
- 数据清洗转换
- 表结构变更同步
- 实时+离线混合模式

### 调度与监控
- Cron定时调度
- 实时任务启停
- 任务状态监控
- 同步日志、错误日志
- 同步速率、延迟监控

## 📦 技术栈

| 组件 | 技术 | 版本 |
|-----|------|-----|
| 核心框架 | SpringBoot | 3.2.x |
| ORM框架 | MyBatis-Plus | 3.5.x |
| 任务调度 | Quartz | 2.3.x |
| CDC引擎 | Debezium | 2.5.x |
| 消息队列 | Apache Kafka | 3.6.x |
| 存储数据库 | MySQL | 8.0 |
| 前端框架 | Vue 3 + Element Plus | - |

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      ETL调度管理系统                              │
├─────────────────────────────────────────────────────────────────┤
│  数据源管理  │  任务管理  │  调度管理  │  监控中心                 │
├─────────────────────────────────────────────────────────────────┤
│                      核心同步引擎层                               │
│  全量同步  │  增量同步  │  CDC实时  │  表结构映射                │
├─────────────────────────────────────────────────────────────────┤
│                      数据访问层                                   │
│  MySQL连接器  │  PG连接器  │  Doris连接器  │  元数据服务          │
├─────────────────────────────────────────────────────────────────┤
│                      基础设施层                                   │
│  Debezium Connect  │  Kafka  │  Quartz                         │
└─────────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 环境要求
- Docker 20.x+
- Docker Compose 2.x+
- JDK 17+ (本地开发)
- Maven 3.8+ (本地开发)

### 2. 一键启动

```bash
# 克隆项目
cd etl-sync-system

# 启动所有服务
cd docker
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 3. 访问系统
- API文档: http://localhost:8080/api/doc.html
- 健康检查: http://localhost:8080/api/health

### 4. 本地开发

```bash
# 编译打包
mvn clean package -DskipTests

# 启动服务
java -jar etl-api/target/etl-api-1.0.0.jar
```

## 📡 API接口

### 数据源管理
| 方法 | 路径 | 描述 |
|-----|------|-----|
| GET | /api/datasource/page | 分页查询数据源 |
| POST | /api/datasource | 创建数据源 |
| PUT | /api/datasource/{id} | 更新数据源 |
| DELETE | /api/datasource/{id} | 删除数据源 |
| POST | /api/datasource/{id}/test | 测试连接 |
| GET | /api/datasource/{id}/tables | 获取所有表 |

### 同步任务
| 方法 | 路径 | 描述 |
|-----|------|-----|
| GET | /api/task/page | 分页查询任务 |
| POST | /api/task | 创建任务 |
| PUT | /api/task/{id} | 更新任务 |
| DELETE | /api/task/{id} | 删除任务 |
| POST | /api/task/{id}/execute | 手动执行任务 |

### 调度管理
| 方法 | 路径 | 描述 |
|-----|------|-----|
| POST | /api/scheduler/task/{taskId} | 创建定时任务 |
| DELETE | /api/scheduler/task/{taskId} | 删除定时任务 |
| POST | /api/scheduler/task/{taskId}/pause | 暂停任务 |
| POST | /api/scheduler/task/{taskId}/resume | 恢复任务 |

### 监控管理
| 方法 | 路径 | 描述 |
|-----|------|-----|
| GET | /api/monitor/overview | 系统概览 |
| GET | /api/monitor/trend | 执行趋势 |
| GET | /api/monitor/execution/page | 执行记录分页 |

## 📁 项目结构

```
etl-sync-system/
├── docker/                    # Docker配置
│   ├── docker-compose.yml    # 容器编排
│   ├── Dockerfile            # 构建镜像
│   ├── mysql/init/           # 数据库初始化SQL
│   ├── debezium/conf/        # Debezium连接器配置
│   └── nginx/                # 前端Nginx配置
├── etl-common/               # 公共模块
├── etl-datasource/           # 数据源模块
├── etl-engine/               # 同步引擎模块
├── etl-scheduler/            # 调度模块
├── etl-monitor/              # 监控模块
├── etl-api/                  # API接口模块
├── etl-web/                  # 前端Vue3项目
├── docs/                     # 文档
│   ├── deployment.md         # 部署文档
│   └── DATABASE_ENCODING_GUIDE.md
└── pom.xml                   # Maven配置
```

## 📊 CDC同步流程

```
MySQL Binlog → Debezium Connect → Kafka → ETL消费 → 目标库
```

## 🔧 常见问题

### 1. Debezium连接失败
检查MySQL是否开启Binlog：
```sql
SHOW VARIABLES LIKE 'log_bin';
SHOW VARIABLES LIKE 'binlog_format';
```

### 2. PostgreSQL CDC失败
检查WAL级别：
```sql
SHOW wal_level;
-- 需要设置为 logical
```

## 📝 License

Apache License 2.0