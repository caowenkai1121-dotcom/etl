package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlSyncTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步任务Mapper
 */
@Mapper
public interface SyncTaskMapper extends BaseMapper<EtlSyncTask> {
}
