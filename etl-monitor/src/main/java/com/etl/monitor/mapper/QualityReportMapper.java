package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlQualityReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质量校验报告Mapper
 */
@Mapper
public interface QualityReportMapper extends BaseMapper<EtlQualityReport> {
}
