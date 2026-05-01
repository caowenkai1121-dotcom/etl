-- ETL系统Docker环境初始化数据
-- ============================================
-- 重要: 确保数据库使用utf8mb4字符集以支持中文
-- 执行顺序: 在00-schema.sql之后执行
-- ============================================

USE etl_system;

-- 插入Docker环境默认数据源
INSERT INTO etl_datasource (name, type, host, port, database_name, username, password, charset, status)
SELECT '本地MySQL', 'MYSQL', 'mysql-system', 3306, 'etl_system', 'root', 'root123456', 'utf8mb4', 1
WHERE NOT EXISTS (SELECT 1 FROM etl_datasource WHERE name = '本地MySQL');

INSERT INTO etl_datasource (name, type, host, port, database_name, username, password, charset, status)
SELECT '源端MySQL', 'MYSQL', 'mysql-source', 3306, 'source_db', 'root', 'root123456', 'utf8mb4', 1
WHERE NOT EXISTS (SELECT 1 FROM etl_datasource WHERE name = '源端MySQL');

INSERT INTO etl_datasource (name, type, host, port, database_name, username, password, charset, status)
SELECT 'PostgreSQL', 'POSTGRESQL', 'postgres', 5432, 'etl_db', 'postgres', 'postgres123456', 'utf8', 1
WHERE NOT EXISTS (SELECT 1 FROM etl_datasource WHERE name = 'PostgreSQL');

INSERT INTO etl_datasource (name, type, host, port, database_name, username, password, charset, status)
SELECT 'Apache Doris', 'DORIS', 'doris-fe', 9030, 'etl', 'root', '', 'utf8mb4', 1
WHERE NOT EXISTS (SELECT 1 FROM etl_datasource WHERE name = 'Apache Doris');
