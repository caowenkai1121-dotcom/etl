# MySQL 中文乱码问题分析与预防指南

## 问题概述

本文档记录了ETL同步系统中发现的中文乱码问题、修复方案以及后续开发中的预防措施。

---

## 问题原因分析

### 1. MySQL `utf8` vs `utf8mb4` 的本质区别

| 字符集 | 实际名称 | 字节长度 | 支持范围 |
|--------|----------|----------|----------|
| `utf8` | `utf8mb3` | 1-3字节 | 仅支持BMP字符（基本多语言平面） |
| `utf8mb4` | `utf8mb4` | 1-4字节 | 支持所有Unicode字符（包括emoji） |

**关键问题**: MySQL的 `utf8` 是历史遗留命名，实际上是 `utf8mb3`（3字节），无法存储部分中文字符和所有emoji。必须使用 `utf8mb4` 才能完整支持中文。

### 2. 发现的具体问题

#### 2.1 JDBC连接配置问题 (application-local.yml)

```yaml
# 错误配置
url: jdbc:mysql://localhost:3306/etl_system?characterEncoding=utf8

# 正确配置
url: jdbc:mysql://localhost:3306/etl_system?characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci
```

**问题**: 
- `characterEncoding=utf8` 导致连接使用3字节编码
- 缺少 `connectionCollation=utf8mb4_unicode_ci` 参数

#### 2.2 Docker MySQL容器配置问题 (docker-compose.yml)

```yaml
# 错误配置 - mysql-source服务
environment:
  MYSQL_CHARACTER_SET_SERVER: utf8mb4
  # 缺少 MYSQL_COLLATION_SERVER

command:
  --character-set-server=utf8mb4
  # 缺少 --collation-server=utf8mb4_unicode_ci
```

**问题**: 
- 源数据库容器缺少collation配置
- 可能导致数据库使用默认的 `latin1_swedish_ci` 或不正确的排序规则

#### 2.3 MySQL客户端连接字符集问题

**重要发现**: 即使数据库配置正确，MySQL客户端默认使用 `latin1` 连接字符集：

```
character_set_client     = latin1  (错误!)
character_set_connection = latin1  (错误!)
character_set_results    = latin1  (错误!)
```

这导致数据在传输过程中被错误编码，产生双重编码的乱码。

---

## 已修复的文件

| 文件 | 修改内容 |
|------|----------|
| `etl-api/src/main/resources/application-local.yml` | JDBC URL添加 `characterEncoding=utf8mb4` 和 `connectionCollation=utf8mb4_unicode_ci` |
| `etl-api/src/main/resources/application-e2e.yml` | JDBC URL添加 `characterEncoding=utf8mb4` 和 `connectionCollation=utf8mb4_unicode_ci` |
| `docker/docker-compose.yml` | mysql-source添加 `MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci` 和命令行参数 |
| `docker/mysql/init-source/01_init.sql` | 添加数据库创建语句，显式指定字符集 |
| `docker/mysql/init-target/01_init.sql` | 添加数据库创建语句，显式指定字符集 |
| `docker/mysql/init-system/01_init.sql` | 添加数据库创建语句，显式指定字符集 |
| `docker/mysql/init-cdc/init-cdc.sql` | 添加数据库创建语句，显式指定字符集 |
| `etl-datasource/.../MySQLConnector.java` | JDBC模板添加 `connectionCollation=utf8mb4_unicode_ci` |
| `docs/启动教程.md` | 更新所有JDBC示例 |
| `docker/mysql/fix-comments.sql` | 系统数据库表注释修复脚本 |
| `docker/mysql/fix-source-comments.sql` | 源数据库表注释修复脚本 |
| `etl-datasource/.../MySQLConnector.java` | JDBC模板添加 `connectionCollation=utf8mb4_unicode_ci` |
| `docs/启动教程.md` | 更新所有JDBC示例 |

---

## 现有数据修复

### 已修复的数据库表数据

| 数据库 | 表 | 问题 | 修复方式 |
|--------|-----|------|----------|
| source_db | users | 中文姓名乱码 | 重新插入正确数据 |
| source_db | products | 中文产品名乱码 | 重新插入正确数据 |
| source_db | orders | 中文产品名乱码 | 重新插入正确数据 |
| etl_system | etl_alert_rule | 中文告警名称乱码 | 重新插入正确数据 |
| etl_system | etl_system_config | 中文描述乱码 | 重新插入正确数据 |

### 已修复的表注释(COMMENT)

**系统数据库 (etl_system)**:
| 表名 | 修复后注释 |
|------|------------|
| etl_datasource | 数据源配置表 |
| etl_sync_task | 同步任务表 |
| etl_table_mapping | 表映射配置表 |
| etl_task_execution | 任务执行记录表 |
| etl_cdc_position | CDC同步位点表 |
| etl_sync_log | 同步日志表 |
| etl_sync_statistics | 同步统计表 |
| etl_field_type_mapping | 字段类型映射配置表 |
| etl_system_config | 系统配置表 |
| etl_alert_rule | 告警规则表 |
| etl_alert_record | 告警记录表 |

**源数据库 (source_db)**:
| 表名 | 修复后注释 |
|------|------------|
| users | 用户表 |
| products | 产品表 |
| orders | 订单表 |

### 修复脚本位置

- 系统数据库表注释修复: `docker/mysql/fix-comments.sql`
- 源数据库表注释修复: `docker/mysql/fix-source-comments.sql`

### 修复命令示例

```bash
# 错误方式 - 直接通过docker exec传递中文会产生乱码
docker exec mysql-container mysql -uroot -ppassword -e "INSERT INTO table VALUES ('中文')"

# 正确方式 - 创建SQL文件后复制到容器执行
# 1. 创建UTF-8编码的SQL文件
# 2. 复制到容器
docker cp fix-comments.sql mysql-container:/tmp/
# 3. 在容器内执行，指定字符集
docker exec mysql-container mysql -uroot -ppassword --default-character-set=utf8mb4 -e "source /tmp/fix-comments.sql"
```

---

## 预防措施 (后续开发必须遵守)

### 1. 数据库层面

```sql
-- 创建数据库时必须指定字符集
CREATE DATABASE database_name 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 创建表时必须指定字符集
CREATE TABLE table_name (
    ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2. JDBC连接层面

**必须包含以下参数**:

```
jdbc:mysql://host:port/database?useUnicode=true&characterEncoding=utf8mb4&connectionCollation=utf8mb4_unicode_ci
```

| 参数 | 值 | 说明 |
|------|-----|------|
| `useUnicode` | `true` | 启用Unicode支持 |
| `characterEncoding` | `utf8mb4` | 指定4字节UTF-8编码 |
| `connectionCollation` | `utf8mb4_unicode_ci` | 强制连接使用正确的排序规则 |

### 3. Docker容器层面

```yaml
environment:
  MYSQL_CHARACTER_SET_SERVER: utf8mb4
  MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci

command:
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_unicode_ci
```

### 4. MySQL命令行连接

**必须使用**:
```bash
mysql --default-character-set=utf8mb4 -uroot -p
```

### 5. 代码层面

- 所有INSERT/UPDATE操作确保使用参数化查询，不手动拼接SQL
- 读取数据时确保使用正确的字符编码
- 数据同步时，源端和目标端必须使用相同的字符集

---

## 验证方法

### 1. 检查数据库字符集

```sql
-- 查看数据库字符集
SHOW CREATE DATABASE database_name;

-- 查看表字符集
SHOW CREATE TABLE table_name;

-- 查看所有数据库字符集
SELECT schema_name, default_character_set_name, default_collation_name 
FROM information_schema.schemata;
```

### 2. 检查连接字符集

```sql
-- 查看当前连接字符集
SHOW VARIABLES LIKE 'character%';
SHOW VARIABLES LIKE 'collation%';
```

### 3. 测试中文存储

```sql
-- 插入包含中文的测试数据
INSERT INTO test_table (name) VALUES ('测试中文');

-- 验证存储结果
SELECT name, HEX(name) FROM test_table;
```

---

## 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| 使用 `utf8` 而非 `utf8mb4` | MySQL历史命名问题 | 始终使用 `utf8mb4` |
| 缺少 `connectionCollation` | 连接默认使用服务器设置 | 在JDBC URL中明确指定 |
| 数据库创建时未指定字符集 | 使用服务器默认值（可能是latin1） | 在CREATE DATABASE中明确指定 |
| 只设置了字符集未设置collation | 排序规则可能不匹配 | 同时设置CHARACTER SET和COLLATE |
| MySQL客户端连接未指定字符集 | 默认使用latin1 | 使用 `--default-character-set=utf8mb4` |

---

## 更新记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-04-12 | Claude | 初始文档，记录乱码问题修复 |
| 2026-04-12 | Claude | 修复现有数据库乱码数据，添加客户端字符集说明 |
| 2026-04-12 | Claude | 修复所有表和字段COMMENT中文乱码，添加修复脚本 |

---

## 参考资料

- [MySQL 8.0 Reference Manual - Character Sets](https://dev.mysql.com/doc/refman/8.0/en/charset.html)
- [MySQL UTF-8字符集详解](https://dev.mysql.com/doc/refman/8.0/en/charset-unicode-utf8mb4.html)
- [JDBC连接参数说明](https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-configuration-properties.html)
