package com.etl.datasource.metadata;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MetadataCacheManager {

    private final Cache<String, TableInfo> tableInfoCache;
    private final Cache<String, List<ColumnInfo>> columnsCache;
    private final Cache<String, List<String>> primaryKeysCache;

    public MetadataCacheManager() {
        this.tableInfoCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
        this.columnsCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
        this.primaryKeysCache = Caffeine.newBuilder()
            .maximumSize(1000).expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    }

    public TableInfo getTableInfo(Long dsId, String db, String table) {
        return tableInfoCache.getIfPresent(buildKey(dsId, db, table, "tableInfo"));
    }

    public void putTableInfo(Long dsId, String db, String table, TableInfo info) {
        tableInfoCache.put(buildKey(dsId, db, table, "tableInfo"), info);
    }

    public List<ColumnInfo> getColumns(Long dsId, String db, String table) {
        return columnsCache.getIfPresent(buildKey(dsId, db, table, "columns"));
    }

    public void putColumns(Long dsId, String db, String table, List<ColumnInfo> columns) {
        columnsCache.put(buildKey(dsId, db, table, "columns"), columns);
    }

    public List<String> getPrimaryKeys(Long dsId, String db, String table) {
        return primaryKeysCache.getIfPresent(buildKey(dsId, db, table, "primaryKeys"));
    }

    public void putPrimaryKeys(Long dsId, String db, String table, List<String> keys) {
        primaryKeysCache.put(buildKey(dsId, db, table, "primaryKeys"), keys);
    }

    public void invalidateTable(Long dsId, String db, String table) {
        tableInfoCache.invalidate(buildKey(dsId, db, table, "tableInfo"));
        columnsCache.invalidate(buildKey(dsId, db, table, "columns"));
        primaryKeysCache.invalidate(buildKey(dsId, db, table, "primaryKeys"));
        log.info("清除表缓存: dsId={}, db={}, table={}", dsId, db, table);
    }

    public void invalidateByDatasource(Long dsId) {
        String prefix = dsId + ":";
        tableInfoCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        columnsCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        primaryKeysCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        log.info("清除数据源全部缓存: dsId={}", dsId);
    }

    public void invalidateAll() {
        tableInfoCache.invalidateAll();
        columnsCache.invalidateAll();
        primaryKeysCache.invalidateAll();
        log.info("清除所有元数据缓存");
    }

    public java.util.Map<String, Object> getCacheStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("tableInfoCache", java.util.Map.of(
            "size", tableInfoCache.estimatedSize(),
            "hitRate", tableInfoCache.stats().hitRate()
        ));
        stats.put("columnsCache", java.util.Map.of(
            "size", columnsCache.estimatedSize(),
            "hitRate", columnsCache.stats().hitRate()
        ));
        stats.put("primaryKeysCache", java.util.Map.of(
            "size", primaryKeysCache.estimatedSize(),
            "hitRate", primaryKeysCache.stats().hitRate()
        ));
        return stats;
    }

    private String buildKey(Long dsId, String db, String table, String type) {
        return dsId + ":" + db + ":" + table + ":" + type;
    }
}
