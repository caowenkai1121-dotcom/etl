package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.entity.EtlDataLineage;
import com.etl.engine.mapper.DataLineageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据血缘控制器
 */
@RestController
@RequestMapping("/lineage")
@RequiredArgsConstructor
public class DataLineageController {

    private final DataLineageMapper dataLineageMapper;

    /**
     * 获取任务的血缘关系
     */
    @GetMapping("/task/{taskId}")
    public Result<List<EtlDataLineage>> getTaskLineage(@PathVariable Long taskId) {
        List<EtlDataLineage> list = dataLineageMapper.selectByTaskId(taskId);
        return Result.success(list);
    }

    /**
     * 获取数据源相关的血缘关系
     */
    @GetMapping("/datasource/{datasourceId}")
    public Result<List<EtlDataLineage>> getDatasourceLineage(@PathVariable Long datasourceId) {
        List<EtlDataLineage> list = dataLineageMapper.selectByDatasourceId(datasourceId);
        return Result.success(list);
    }

    /**
     * 获取表相关的血缘关系
     */
    @GetMapping("/table")
    public Result<List<EtlDataLineage>> getTableLineage(@RequestParam String tableName) {
        List<EtlDataLineage> list = dataLineageMapper.selectByTableName(tableName);
        return Result.success(list);
    }
}
