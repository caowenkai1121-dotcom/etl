package com.etl.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.etl.engine.entity.EtlTaskSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 任务统计摘要Mapper
 */
@Mapper
public interface TaskSummaryMapper extends BaseMapper<EtlTaskSummary> {

    /**
     * 根据日期和任务ID查询统计摘要
     */
    @Select("SELECT * FROM etl_task_summary WHERE task_id = #{taskId} AND summary_date = #{summaryDate}")
    EtlTaskSummary selectByTaskIdAndDate(@Param("taskId") Long taskId,
                                        @Param("summaryDate") LocalDate summaryDate);

    /**
     * 根据日期查询统计摘要
     */
    @Select("SELECT * FROM etl_task_summary WHERE summary_date = #{summaryDate}")
    List<EtlTaskSummary> selectByDate(@Param("summaryDate") LocalDate summaryDate);
}
