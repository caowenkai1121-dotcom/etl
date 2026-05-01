package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志Mapper
 */
@Mapper
public interface SyncLogMapper extends BaseMapper<EtlSyncLog> {
}
