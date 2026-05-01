package com.etl.datasource.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import com.etl.common.utils.EncryptionUtil;
import com.etl.datasource.connector.AbstractConnector;
import com.etl.datasource.connector.ConnectorFactory;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.entity.EtlDatasource;
import com.etl.datasource.mapper.DatasourceMapper;
import com.etl.datasource.metadata.MetadataCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源服务
 */
@Slf4j
@Service
public class DatasourceService extends ServiceImpl<DatasourceMapper, EtlDatasource> {

    @Autowired
    private MetadataCacheManager metadataCacheManager;

    /**
     * 分页查询数据源
     */
    public Page<EtlDatasource> pageList(Integer pageNum, Integer pageSize, String name, String type) {
        LambdaQueryWrapper<EtlDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), EtlDatasource::getName, name)
               .eq(type != null && !type.isEmpty(), EtlDatasource::getType, type)
               .orderByDesc(EtlDatasource::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取所有数据源列表
     */
    public List<EtlDatasource> listAll() {
        LambdaQueryWrapper<EtlDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlDatasource::getStatus, 1)
               .orderByDesc(EtlDatasource::getCreatedAt);
        return list(wrapper);
    }

    /**
     * 创建数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDatasource(EtlDatasource datasource) {
        // 加密密码
        if (datasource.getPassword() != null && !datasource.getPassword().isEmpty()) {
            datasource.setPassword(EncryptionUtil.encrypt(datasource.getPassword()));
        }
        datasource.setConnectionTest(0);
        save(datasource);
        log.info("创建数据源成功: id={}, name={}", datasource.getId(), datasource.getName());
        return datasource.getId();
    }

    /**
     * 更新数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDatasource(EtlDatasource datasource) {
        EtlDatasource existing = getById(datasource.getId());
        if (existing == null) {
            throw EtlException.datasourceNotFound(datasource.getId());
        }

        // 如果密码有变化，加密新密码
        if (datasource.getPassword() != null && !datasource.getPassword().equals(existing.getPassword())) {
            datasource.setPassword(EncryptionUtil.encrypt(datasource.getPassword()));
        }

        // 移除旧的连接器缓存
        ConnectorFactory.removeConnector(datasource.getId());

        updateById(datasource);
        log.info("更新数据源成功: id={}", datasource.getId());
    }

    /**
     * 删除数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDatasource(Long id) {
        EtlDatasource datasource = getById(id);
        if (datasource == null) {
            throw EtlException.datasourceNotFound(id);
        }

        // 移除连接器缓存
        ConnectorFactory.removeConnector(id);

        removeById(id);
        log.info("删除数据源成功: id={}", id);
    }

    /**
     * 测试数据源连接
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean testConnection(Long id) {
        EtlDatasource datasource = getById(id);
        if (datasource == null) {
            throw EtlException.datasourceNotFound(id);
        }

        // 解密密码
        String decryptedPassword = EncryptionUtil.decrypt(datasource.getPassword());

        DatabaseConnector connector = ConnectorFactory.createConnector(
            id, datasource.getType(), datasource.getHost(), datasource.getPort(),
            datasource.getDatabaseName(), datasource.getUsername(), decryptedPassword,
            datasource.getCharset(), datasource.getExtraConfig()
        );

        boolean success = connector.testConnection();

        // 更新测试状态
        datasource.setConnectionTest(success ? 1 : 2);
        datasource.setLastTestTime(LocalDateTime.now());
        updateById(datasource);

        log.info("测试数据源连接: id={}, result={}", id, success ? "成功" : "失败");
        return success;
    }

    /**
     * 获取数据源连接器
     */
    public DatabaseConnector getConnector(Long id) {
        EtlDatasource datasource = getById(id);
        if (datasource == null) {
            throw EtlException.datasourceNotFound(id);
        }

        // 解密密码
        String decryptedPassword = EncryptionUtil.decrypt(datasource.getPassword());

        DatabaseConnector connector = ConnectorFactory.getOrCreateConnector(
            id, datasource.getType(), datasource.getHost(), datasource.getPort(),
            datasource.getDatabaseName(), datasource.getUsername(), decryptedPassword,
            datasource.getCharset(), datasource.getExtraConfig()
        );

        // 注入元数据缓存管理器
        if (connector instanceof AbstractConnector ac) {
            ac.setMetadataCacheManager(metadataCacheManager);
        }

        return connector;
    }

    /**
     * 获取数据源所有表
     */
    public List<TableInfo> getTables(Long datasourceId) {
        try {
            DatabaseConnector connector = getConnector(datasourceId);
            return connector.getTables();
        } catch (Exception e) {
            log.error("获取数据源表信息失败: datasourceId={}", datasourceId, e);
            throw EtlException.metadataFailed("", e);
        }
    }

    /**
     * 获取表信息
     */
    public TableInfo getTableInfo(Long datasourceId, String tableName) {
        try {
            DatabaseConnector connector = getConnector(datasourceId);
            return connector.getTableInfo(tableName);
        } catch (Exception e) {
            log.error("获取表信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw EtlException.metadataFailed(tableName, e);
        }
    }

    /**
     * 获取表字段信息
     */
    public List<ColumnInfo> getColumns(Long datasourceId, String tableName) {
        try {
            DatabaseConnector connector = getConnector(datasourceId);
            return connector.getColumns(tableName);
        } catch (Exception e) {
            log.error("获取表字段信息失败: datasourceId={}, tableName={}", datasourceId, tableName, e);
            throw EtlException.metadataFailed(tableName, e);
        }
    }

    /**
     * 获取数据源详情
     */
    public EtlDatasource getDetail(Long id) {
        EtlDatasource datasource = getById(id);
        if (datasource == null) {
            throw EtlException.datasourceNotFound(id);
        }
        // 解密密码返回
        datasource.setPassword(EncryptionUtil.decrypt(datasource.getPassword()));
        return datasource;
    }

    /**
     * 启用/禁用数据源
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        EtlDatasource datasource = getById(id);
        if (datasource == null) {
            throw EtlException.datasourceNotFound(id);
        }
        datasource.setStatus(status);
        updateById(datasource);

        if (status == 0) {
            ConnectorFactory.removeConnector(id);
        }
        log.info("更新数据源状态: id={}, status={}", id, status);
    }
}