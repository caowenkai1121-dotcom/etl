package com.etl.engine.strategy;

import com.etl.common.domain.SyncPipelineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 更新同步策略
 * 根据主键或唯一键判断，存在则更新，不存在则插入（Upsert）
 */
@Slf4j
@Component
public class UpdateStrategy implements SyncStrategy {

    @Override
    public String getName() {
        return "UPDATE";
    }

    @Override
    public long execute(SyncPipelineContext context, List<Map<String, Object>> sourceData) throws Exception {
        log.info("执行更新同步策略, 数据量: {}", sourceData.size());
        return sourceData.size();
    }

    @Override
    public long syncTable(SyncPipelineContext context, String sourceTable, String targetTable) throws Exception {
        log.info("更新同步表: {} -> {}", sourceTable, targetTable);
        // 1. 从源表读取数据
        // 2. 对每条数据判断是否存在
        // 3. 存在则更新，不存在则插入
        return 0;
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }
}
