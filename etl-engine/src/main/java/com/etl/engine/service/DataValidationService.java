package com.etl.engine.service;

import com.etl.engine.entity.EtlDataValidation;
import com.etl.engine.validate.DataValidator;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据校验服务
 * 集成到同步流程中的数据校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataValidationService {

    /**
     * 执行数据校验
     *
     * @param sourceConnector  源数据源连接器
     * @param targetConnector  目标数据源连接器
     * @param sourceTable      源表名
     * @param targetTable      目标表名
     * @param primaryKeys      主键列名
     * @param sampleRate       抽样率
     * @return 校验结果
     */
    public EtlDataValidation validate(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                                       String sourceTable, String targetTable,
                                       List<String> primaryKeys, double sampleRate) {
        DataValidator validator = new DataValidator(sourceConnector, targetConnector, sampleRate);
        DataValidator.ValidationResult result = validator.validate(sourceTable, targetTable, primaryKeys);

        EtlDataValidation dv = new EtlDataValidation();
        dv.setSourceTable(sourceTable);
        dv.setTargetTable(targetTable);
        dv.setValidationType(result.isCountMatch() && result.getMismatchCount() == 0 ? "SAMPLE" : "FULL");
        dv.setStatus(result.isPassed() ? "PASSED" : "FAILED");
        dv.setSourceCount(result.getSourceCount());
        dv.setTargetCount(result.getTargetCount());
        dv.setCountMatch(result.isCountMatch() ? 1 : 0);
        dv.setSampleRate(sampleRate);
        dv.setSampleSize(result.getSampleSize());
        dv.setMatchCount(result.getMatchCount());
        dv.setMismatchCount(result.getMismatchCount());
        dv.setMissingKeyCount(result.getMissingKeys() != null ? result.getMissingKeys().size() : 0);
        dv.setExtraKeyCount(result.getExtraKeys() != null ? result.getExtraKeys().size() : 0);
        dv.setPassed(result.isPassed() ? 1 : 0);
        dv.setSummary(result.getSummary());
        dv.setStartedAt(LocalDateTime.now());
        dv.setCompletedAt(LocalDateTime.now());

        log.info("数据校验完成: {} -> {}, passed={}, source={}, target={}, mismatch={}",
            sourceTable, targetTable, result.isPassed(), result.getSourceCount(),
            result.getTargetCount(), result.getMismatchCount());

        return dv;
    }
}
