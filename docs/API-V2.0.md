# ETL数据同步调度系统 V2.0 新增接口文档

## 一、配置中心 API

### 1. 查询配置列表

**请求：**
```
GET /api/config/list?page=1&size=20&group=ENGINE
```

**参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页条数，默认20 |
| group | string | 否 | 配置分组筛选 |

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "configGroup": "ENGINE",
        "configKey": "batch.size",
        "configValue": "1000",
        "configType": "INT",
        "description": "默认批量处理大小",
        "isEditable": 1
      }
    ],
    "total": 33,
    "page": 1,
    "size": 20
  }
}
```

### 2. 获取配置值

**请求：**
```
GET /api/config/{group}/{key}
```

**示例：**
```
GET /api/config/ENGINE/batch.size
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "configGroup": "ENGINE",
    "configKey": "batch.size",
    "configValue": "1000",
    "configType": "INT"
  }
}
```

### 3. 修改配置值（热加载）

**请求：**
```
PUT /api/config/{group}/{key}
Content-Type: application/json

{
  "value": "2000",
  "description": "默认批量处理大小"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success"
}
```

**说明：** 修改后自动触发热加载，部分配置立即生效（线程池大小、缓存TTL等），部分下次任务执行时生效（批次大小、重试次数等）。

## 二、运维监控 API

### 1. 连接池状态

**请求：**
```
GET /api/monitor/pool-status
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "pools": {
      "mysql-pool-1": {
        "poolName": "mysql-pool-1",
        "activeConnections": 3,
        "idleConnections": 7,
        "totalConnections": 10,
        "threadsAwaitingConnection": 0,
        "isClosed": false
      }
    },
    "totalPools": 2,
    "activePools": 2
  }
}
```

### 2. 线程池状态

**请求：**
```
GET /api/monitor/thread-pool-status
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "corePoolSize": 4,
    "maximumPoolSize": 16,
    "activeCount": 2,
    "poolSize": 4,
    "queueSize": 0,
    "completedTaskCount": 150,
    "availablePermits": 8
  }
}
```

### 3. 缓存状态

**请求：**
```
GET /api/monitor/cache-status
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "tableInfoCache": {"size": 45, "hitRate": 0.87},
    "columnsCache": {"size": 45, "hitRate": 0.92},
    "primaryKeysCache": {"size": 45, "hitRate": 0.95}
  }
}
```

### 4. 系统信息

**请求：**
```
GET /api/monitor/system-info
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "jvmMaxMemory": 2147483648,
    "jvmUsedMemory": 536870912,
    "jvmFreeMemory": 1610612736,
    "availableProcessors": 8,
    "diskTotal": 107374182400,
    "diskFree": 53687091200,
    "uptimeMs": 86400000
  }
}
```

## 三、健康检查 API

### 详细健康检查

**请求：**
```
GET /api/health/detail
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "database": {"status": "UP"},
    "connectionPools": {
      "mysql-pool-1": {
        "activeConnections": 3,
        "idleConnections": 7,
        "isClosed": false
      }
    },
    "jvm": {
      "maxMemory": "2.0 GB",
      "usedMemory": "512.0 MB",
      "freeMemory": "1.5 GB",
      "usedPercent": "25.0%"
    },
    "disk": {
      "total": "100.0 GB",
      "free": "50.0 GB",
      "usable": "50.0 GB"
    }
  }
}
```

## 四、维护 API

### 1. 手动触发日志清理

**请求：**
```
POST /api/maintenance/clean-logs
```

**响应：**
```json
{
  "code": 200,
  "message": "success"
}
```

**说明：** 清理过期日志（默认保留30天）和执行记录（成功保留90天、失败保留180天），失败记录归档后再清理。

### 2. 手动清除缓存

**请求：**
```
POST /api/maintenance/clear-cache
```

**响应：**
```json
{
  "code": 200,
  "message": "success"
}
```

**说明：** 清除元数据缓存、监控数据缓存、配置缓存，下次访问时自动重建。

## 五、WebSocket 结构化进度推送

V2.0 增强了 WebSocket 推送，新增结构化进度消息（通过同一个 WebSocket 连接推送）：

**消息格式：**
```json
{
  "type": "PROGRESS",
  "taskId": 1001,
  "executionId": 2001,
  "status": "RUNNING",
  "progress": 43.33,
  "totalRows": 300000,
  "processedRows": 130000,
  "successRows": 129950,
  "failedRows": 50,
  "elapsedSeconds": 120,
  "estimatedRemainingSeconds": 180,
  "rowsPerSecond": 1083.33,
  "currentTable": "user_table",
  "timestamp": "2026-04-23T10:30:00"
}
```

**字段说明：**
| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 消息类型：PROGRESS/LOG |
| progress | decimal | 进度百分比（0~100） |
| estimatedRemainingSeconds | int | 预估剩余秒数，-1表示无法预估 |
| rowsPerSecond | double | 当前同步速率 |
| currentTable | string | 当前正在同步的表名 |

## 六、常见问题

### Q: 升级后旧任务能正常运行吗？
A: 可以。所有旧任务配置完全兼容，新增字段均有默认值。增量同步默认使用 TIMESTAMP 策略，与原有行为一致。

### Q: 新功能如何启用？
A: 通过配置中心 API 修改 `feature.*.enabled` 配置即可启用，无需重启服务。

### Q: 连接池参数如何调整？
A: 通过配置中心修改 `DATASOURCE.pool.*` 系列配置，新连接会使用新参数。已创建的连接池需要重启服务后生效。

### Q: Doris Stream Load 如何启用？
A: 1) 确保 Doris FE HTTP 端口可访问；2) 设置 `feature.stream_load_doris.enabled=true`；3) 目标数据源为 Doris 类型时自动使用 Stream Load。

### Q: 异步日志写入失败怎么办？
A: 系统自动降级为同步写入，不影响数据同步。检查日志写入线程池状态和数据库连接是否正常。

### Q: 任务依赖如何配置？
A: 通过任务依赖API添加依赖关系，支持两种类型：FINISH（上游完成后即可执行）和 SUCCESS（上游必须成功才执行）。系统自动检测循环依赖。
