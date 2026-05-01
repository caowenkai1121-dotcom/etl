# FineDataLink 数据开发模块 1:1 还原升级计划

## 一、调研结论

### 1.1 现有系统基础（已具备）

**技术栈：**
- 前端：Vue 3 + Vite + Element Plus + Pinia + @antv/x6
- 后端：Spring Boot 3.2.5 + Java 17 + MyBatis Plus + Quartz
- 数据库：MySQL 8.0
- 部署：Docker Compose

**已有功能：**
- 数据开发工作台框架（dev-workbench）
- DAG画布（基于X6，支持拖拽、连线、缩放）
- 节点面板（5大类节点）
- 节点属性配置面板（支持各类节点配置）
- 13个数据转换算子组件
- 任务目录树管理
- 开发/生产模式切换UI
- 保存/运行/发布/调度配置弹窗
- 底部日志/统计面板
- 后端DAG配置API（保存/加载）
- 数据库表：etl_dag_config, etl_dag_node, etl_task_folder 等

### 1.2 与FineDataLink的差距分析

#### 前端UI差距
| 模块 | 现有状态 | FineDataLink | 工作量 |
|------|---------|-------------|--------|
| 节点面板布局 | 2列网格 | 单列列表式（带图标+文字） | 中 |
| 节点样式 | 简单矩形SVG | 精美卡片（圆角+颜色条+状态图标） | 中 |
| 左侧边栏 | 目录树+节点面板分开 | 统一侧边栏可切换 | 中 |
| 顶部工具栏 | 基本功能 | 运行下拉+还原+发布+更多操作 | 小 |
| 运行参数 | 无 | 运行前参数配置弹窗 | 中 |
| 日志面板 | 简单列表 | 结构化日志（可展开/折叠） | 中 |
| 开发/生产模式 | UI有，功能不完整 | 完整模式切换（编辑/只读） | 中 |

#### 节点类型差距
| 分类 | 现有节点 | FineDataLink节点 | 缺失 |
|------|---------|-----------------|------|
| 通用 | DB_SYNC, API_SYNC, SERVER_DS, FILE_READ, JIANADAOYUN | 离线同步、可视化转换、文件传输 | 可视化转换 |
| 脚本 | SQL_SCRIPT, PYTHON_SCRIPT, SHELL_SCRIPT | SQL脚本、Shell脚本、Bat脚本、Python脚本 | Bat脚本 |
| 流程 | 参数赋值、条件分支、调用任务、循环容器、消息通知、虚拟节点、备注 | 同上 | 无 |
| 其他 | 备注说明 | 任务注释 | 无 |

#### 后端功能差距
| 功能 | 现有状态 | FineDataLink | 工作量 |
|------|---------|-------------|--------|
| DAG执行引擎 | 基础框架 | 完整执行引擎（支持所有节点类型） | 大 |
| 任务调度 | Quartz基础 | Cron表达式+可视化配置 | 中 |
| 版本管理 | 表已创建 | 完整版本对比/回滚 | 中 |
| 发布流程 | 基础UI | 开发->发布->生产完整流程 | 中 |
| 运行日志 | 基础采集 | 结构化日志+实时推送 | 中 |
| 数据源集成 | 部分支持 | 完整数据源配置 | 大 |

## 二、升级实施计划

### 阶段一：前端UI还原（1-2周）

**目标：** 让界面看起来像FineDataLink

#### 1.1 节点面板改造
- 改为单列列表式布局
- 添加节点分类图标
- 调整节点项样式（与FineDataLink一致）

#### 1.2 DAG画布节点样式升级
- 节点改为圆角卡片样式
- 添加顶部颜色条
- 添加节点类型图标
- 优化选中/悬停效果
- 优化连线样式（带箭头）

#### 1.3 左侧边栏整合
- 统一任务目录树和节点面板
- 添加可折叠按钮
- 调整宽度适配

#### 1.4 顶部工具栏完善
- 添加运行下拉菜单（带参数选择）
- 完善还原、发布按钮
- 添加更多操作按钮

#### 1.5 底部面板优化
- 日志结构化展示
- 添加搜索/过滤功能
- 统计面板数据可视化

### 阶段二：节点类型补全（1周）

#### 2.1 新增"可视化转换"节点
- 前端：添加节点类型和图标
- 后端：支持ETL可视化转换执行

#### 2.2 新增"Bat脚本"节点
- 前端：添加节点类型和配置面板
- 后端：支持Bat脚本执行（Windows环境）

#### 2.3 节点配置面板完善
- 离线同步节点：完善数据源选择、表选择、字段映射
- 可视化转换节点：集成现有13个算子
- 脚本节点：支持SQL/Shell/Bat/Python
- 流程节点：完善条件表达式、参数传递

### 阶段三：后端功能完善（2-3周）

#### 3.1 DAG执行引擎升级
- 实现DAG拓扑排序执行
- 支持节点并行执行
- 支持条件分支（成功/失败路径）
- 支持循环容器
- 支持调用子任务

#### 3.2 任务调度配置
- Cron表达式解析和生成
- 可视化调度配置界面
- 调度任务与Quartz集成

#### 3.3 版本管理与发布
- 任务版本自动递增
- 版本对比功能
- 发布审批流程
- 回滚功能

#### 3.4 运行日志与监控
- 结构化日志采集
- WebSocket实时推送日志
- 执行统计汇总
- 错误告警

### 阶段四：数据源集成（1-2周）

#### 4.1 数据源管理
- 支持MySQL、PostgreSQL、Oracle、SQLServer
- 支持MongoDB、Elasticsearch、ClickHouse
- 支持Redis、Kafka、Hive
- 数据源测试连接

#### 4.2 数据同步节点
- 全量同步实现
- 增量同步实现（基于CDC或时间戳）
- 字段映射配置
- 数据类型转换

### 阶段五：测试与优化（1周）

#### 5.1 功能测试
- 所有节点类型测试
- DAG执行流程测试
- 调度任务测试
- 版本发布测试

#### 5.2 性能优化
- DAG大数据量执行优化
- 前端渲染性能优化
- 数据库查询优化

## 三、数据库变更

### 新增表
```sql
-- 任务执行实例表
CREATE TABLE etl_task_execution_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    version INT NOT NULL,
    status VARCHAR(20),
    trigger_type VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    duration BIGINT,
    row_count BIGINT,
    error_msg TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status)
);

-- 节点执行记录表
CREATE TABLE etl_node_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id BIGINT NOT NULL,
    node_id VARCHAR(50) NOT NULL,
    node_name VARCHAR(200),
    status VARCHAR(20),
    start_time DATETIME,
    end_time DATETIME,
    input_rows BIGINT,
    output_rows BIGINT,
    error_rows BIGINT,
    log_content TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_execution_id (execution_id)
);
```

## 四、实施优先级建议

考虑到工作量和价值，建议按以下优先级实施：

**P0（核心功能，必须实现）：**
1. 前端UI还原（节点面板、DAG画布、工具栏）
2. DAG执行引擎（基础执行能力）
3. 节点配置面板完善

**P1（重要功能，建议实现）：**
4. 任务调度配置
5. 运行日志与监控
6. 版本管理与发布

**P2（增强功能，可选实现）：**
7. 数据源集成扩展
8. 性能优化
9. 高级功能（数据血缘、影响分析）

## 五、风险与建议

### 风险
1. **工作量巨大**：完整1:1还原需要3-4人月的工作量
2. **技术复杂度**：DAG执行引擎、CDC同步等技术门槛较高
3. **测试覆盖**：大量节点类型需要充分测试

### 建议
1. **分阶段交付**：不要一次性完成所有功能，按阶段迭代
2. **先UI后功能**：先让界面看起来一致，再完善功能
3. **复用现有代码**：现有系统已有良好基础，尽量复用
4. **参考开源方案**：DAG执行可以参考Apache DolphinScheduler、Airflow等
