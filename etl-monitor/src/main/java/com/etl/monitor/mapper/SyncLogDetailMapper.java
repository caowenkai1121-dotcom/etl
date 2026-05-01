package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlSyncLogDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 同步日志详情 Mapper 接口
 */
@Mapper
public interface SyncLogDetailMapper extends BaseMapper<EtlSyncLogDetail> {
}
