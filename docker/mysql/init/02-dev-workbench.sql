-- ============================================
-- ETL数据开发工作台 - 数据库表结构扩展
-- 版本: 3.1.0
-- 说明: 扩展现有表结构，新增数据开发相关表
-- ============================================

USE etl_system;

-- ============================================
-- 1. 任务文件夹表
-- ============================================
DROP TABLE IF EXISTS etl_task_folder;
CREATE TABLE etl_task_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '文件夹名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父文件夹ID，0表示根目录',
    path VARCHAR(500) COMMENT '完整路径，如/01产品功能示例/02基础功能',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务文件夹表';

-- ============================================
-- 2. 扩展同步任务表字段（跳过已存在字段）
-- ============================================
-- 注：如果字段已存在，请忽略错误

-- ============================================
-- 3. DAG流程配置表
-- ============================================
DROP TABLE IF EXISTS etl_dag_config;
CREATE TABLE etl_dag_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    nodes JSON NOT NULL COMMENT '节点配置JSON数组',
    edges JSON COMMENT '连线配置JSON数组',
    viewport JSON COMMENT '视口配置（缩放、位置）',
    version INT DEFAULT 1 COMMENT '版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    UNIQUE KEY uk_task_version (task_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DAG流程配置表';

-- ============================================
-- 4. DAG节点配置表（用于存储节点详细信息）
-- ============================================
DROP TABLE IF EXISTS etl_dag_node;
CREATE TABLE etl_dag_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    dag_id BIGINT NOT NULL COMMENT 'DAG配置ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点唯一标识',
    node_type VARCHAR(50) NOT NULL COMMENT '节点类型: SYNC/TRANSFORM/SCRIPT/CONTROL/FILE',
    node_name VARCHAR(200) NOT NULL COMMENT '节点名称',
    position_x INT DEFAULT 0 COMMENT 'X坐标',
    position_y INT DEFAULT 0 COMMENT 'Y坐标',
    config JSON COMMENT '节点配置JSON',
    input_schema JSON COMMENT '输入数据结构',
    output_schema JSON COMMENT '输出数据结构',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dag_id (dag_id),
    UNIQUE KEY uk_dag_node (dag_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DAG节点配置表';

-- ============================================
-- 5. 任务版本表
-- ============================================
DROP TABLE IF EXISTS etl_task_version;
CREATE TABLE etl_task_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    version INT NOT NULL COMMENT '版本号',
    config JSON NOT NULL COMMENT '完整配置快照JSON',
    change_log TEXT COMMENT '变更说明',
    publish_status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '发布状态',
    published_at DATETIME COMMENT '发布时间',
    published_by VARCHAR(50) COMMENT '发布人',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_version (version),
    UNIQUE KEY uk_task_version (task_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务版本表';

-- ============================================
-- 6. 任务发布记录表
-- ============================================
DROP TABLE IF EXISTS etl_task_publish;
CREATE TABLE etl_task_publish (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    version INT NOT NULL COMMENT '发布版本号',
    publish_type VARCHAR(20) DEFAULT 'FULL' COMMENT '发布类型: FULL-全量发布, UPDATE-更新发布',
    from_version INT COMMENT '源版本号（更新发布时）',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '发布状态: PENDING-待发布, SUCCESS-成功, FAILED-失败',
    remark VARCHAR(500) COMMENT '发布备注',
    published_by VARCHAR(50) COMMENT '发布人',
    published_at DATETIME COMMENT '发布时间',
    rollback_version INT COMMENT '回滚到的版本号',
    rollback_at DATETIME COMMENT '回滚时间',
    rollback_by VARCHAR(50) COMMENT '回滚人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务发布记录表';

-- ============================================
-- 7. 数据转换算子模板表
-- ============================================
DROP TABLE IF EXISTS etl_transform_operator;
CREATE TABLE etl_transform_operator (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code VARCHAR(50) NOT NULL COMMENT '算子编码',
    name VARCHAR(100) NOT NULL COMMENT '算子名称',
    category VARCHAR(50) NOT NULL COMMENT '算子分类: FIELD/DATA/AGGREGATE/JOIN/OTHER',
    description VARCHAR(500) COMMENT '算子描述',
    config_schema JSON COMMENT '配置项结构定义',
    default_config JSON COMMENT '默认配置',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据转换算子模板表';

-- ============================================
-- 8. 初始化数据
-- ============================================

-- 初始化根目录文件夹
INSERT INTO etl_task_folder (id, name, parent_id, path, sort_order, created_by) VALUES
(1, '01产品功能示例', 0, '/01产品功能示例', 1, 'system'),
(2, '02业务场景示例', 0, '/02业务场景示例', 2, 'system'),
(3, '03数据开发（ERP）示例', 0, '/03数据开发（ERP）示例', 3, 'system');

-- 初始化子文件夹
INSERT INTO etl_task_folder (name, parent_id, path, sort_order, created_by) VALUES
('01.快速入门', 1, '/01产品功能示例/01.快速入门', 1, 'system'),
('02.基础功能', 1, '/01产品功能示例/02.基础功能', 2, 'system'),
('03.管道&服务demo辅助', 1, '/01产品功能示例/03.管道&服务demo辅助', 3, 'system');

-- 初始化02.基础功能子文件夹
SET @basic_id = (SELECT id FROM etl_task_folder WHERE path = '/01产品功能示例/02.基础功能');
INSERT INTO etl_task_folder (name, parent_id, path, sort_order, created_by) VALUES
('01.数据同步', @basic_id, '/01产品功能示例/02.基础功能/01.数据同步', 1, 'system'),
('02.数据转换', @basic_id, '/01产品功能示例/02.基础功能/02.数据转换', 2, 'system'),
('03.脚本', @basic_id, '/01产品功能示例/02.基础功能/03.脚本', 3, 'system'),
('04.流程', @basic_id, '/01产品功能示例/02.基础功能/04.流程', 4, 'system'),
('文件传输', @basic_id, '/01产品功能示例/02.基础功能/文件传输', 5, 'system');

-- 初始化数据转换算子
INSERT INTO etl_transform_operator (code, name, category, description, config_schema, icon, sort_order) VALUES
('FIELD_SELECT', '字段选择', 'FIELD', '选择需要保留的字段', '{"fields":{"type":"array","label":"字段列表"}}', 'Document', 1),
('FIELD_RENAME', '字段重命名', 'FIELD', '重命名字段', '{"mappings":{"type":"array","label":"映射关系"}}', 'Edit', 2),
('FIELD_CALCULATE', '字段计算', 'FIELD', '通过表达式计算新字段', '{"expression":{"type":"string","label":"计算表达式"},"outputField":{"type":"string","label":"输出字段名"}}', 'Calculator', 3),
('DATA_FILTER', '数据过滤', 'DATA', '根据条件过滤数据', '{"condition":{"type":"string","label":"过滤条件"}}', 'Filter', 4),
('DATA_SORT', '数据排序', 'DATA', '对数据进行排序', '{"fields":{"type":"array","label":"排序字段"},"order":{"type":"string","label":"排序方式"}}', 'Sort', 5),
('DATA_DEDUP', '数据去重', 'DATA', '去除重复数据', '{"fields":{"type":"array","label":"去重字段"}}', 'DocumentRemove', 6),
('DATA_AGGREGATE', '数据聚合', 'AGGREGATE', '对数据进行聚合统计', '{"groupFields":{"type":"array","label":"分组字段"},"aggregateFields":{"type":"array","label":"聚合字段"}}', 'DataAnalysis', 7),
('DATA_JOIN', '数据关联', 'JOIN', '关联多个数据源', '{"joinType":{"type":"string","label":"关联类型"},"joinCondition":{"type":"string","label":"关联条件"}}', 'Connection', 8),
('FIELD_SPLIT', '字段拆分', 'FIELD', '将字段拆分为多个', '{"splitPattern":{"type":"string","label":"拆分规则"},"outputFields":{"type":"array","label":"输出字段"}}', 'Split', 9),
('NULL_HANDLE', '空值处理', 'DATA', '处理空值数据', '{"fields":{"type":"array","label":"处理字段"},"strategy":{"type":"string","label":"处理策略"},"defaultValue":{"type":"string","label":"默认值"}}', 'NullHandle', 10);

-- ============================================
-- 9. 任务收藏表
-- ============================================
DROP TABLE IF EXISTS etl_task_favorite;
CREATE TABLE etl_task_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_task_user (task_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务收藏表';

-- ============================================
-- 10. 任务执行历史统计表（物化视图）
-- ============================================
DROP TABLE IF EXISTS etl_task_stat;
CREATE TABLE etl_task_stat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    total_executions INT DEFAULT 0 COMMENT '总执行次数',
    success_executions INT DEFAULT 0 COMMENT '成功次数',
    failed_executions INT DEFAULT 0 COMMENT '失败次数',
    avg_duration BIGINT DEFAULT 0 COMMENT '平均执行时长(毫秒)',
    max_duration BIGINT DEFAULT 0 COMMENT '最大执行时长(毫秒)',
    min_duration BIGINT DEFAULT 0 COMMENT '最小执行时长(毫秒)',
    last_success_time DATETIME COMMENT '最后成功时间',
    last_failure_time DATETIME COMMENT '最后失败时间',
    total_rows BIGINT DEFAULT 0 COMMENT '总处理行数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行统计表';

-- ============================================
-- 完成提示
-- ============================================
SELECT '数据开发工作台表结构创建完成' AS message;
