package com.etl.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.monitor.entity.EtlAlertRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录Mapper
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<EtlAlertRecord> {

    /**
     * 查询指定时间范围内的告警记录
     */
    @Select("SELECT * FROM etl_alert_record WHERE created_at >= #{startTime} ORDER BY created_at DESC LIMIT #{limit}")
    List<EtlAlertRecord> selectRecentAlerts(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);

    /**
     * 统计今日告警数量
     */
    @Select("SELECT COUNT(*) FROM etl_alert_record WHERE DATE(created_at) = CURDATE()")
    int countTodayAlerts();
}
