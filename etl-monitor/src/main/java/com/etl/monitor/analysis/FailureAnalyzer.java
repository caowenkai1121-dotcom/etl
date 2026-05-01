package com.etl.monitor.analysis;

import com.etl.common.exception.ErrorCode;
import com.etl.common.enums.FailurePhase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FailureAnalyzer {

    public FailureAnalysis analyze(Exception e, String tableName) {
        FailureAnalysis analysis = new FailureAnalysis();
        analysis.setTableName(tableName);
        analysis.setTimestamp(java.time.LocalDateTime.now());

        if (e instanceof com.etl.common.exception.EtlConnectionException) {
            analysis.setErrorCode(ErrorCode.CONNECTION_001.getCode());
            analysis.setErrorCategory("CONNECTION");
            analysis.setFailurePhase(FailurePhase.EXTRACT.getCode());
            analysis.setSuggestion("请检查数据库是否正常运行，或增加连接超时时间(配置项: connection.timeout.ms)");
        } else if (e instanceof com.etl.common.exception.EtlExtractException) {
            analysis.setErrorCode(ErrorCode.EXTRACT_001.getCode());
            analysis.setErrorCategory("EXTRACT");
            analysis.setFailurePhase(FailurePhase.EXTRACT.getCode());
            analysis.setSuggestion("查询超时，建议为增量字段添加索引，或增加查询超时时间(配置项: query.timeout.seconds)");
        } else if (e instanceof com.etl.common.exception.EtlTransformException) {
            analysis.setErrorCode(ErrorCode.TRANSFORM_001.getCode());
            analysis.setErrorCategory("TRANSFORM");
            analysis.setFailurePhase(FailurePhase.TRANSFORM.getCode());
            analysis.setSuggestion("数据转换失败，请检查转换规则配置或源数据格式");
        } else if (e instanceof com.etl.common.exception.EtlLoadException) {
            analysis.setErrorCode(ErrorCode.LOAD_003.getCode());
            analysis.setErrorCategory("LOAD");
            analysis.setFailurePhase(FailurePhase.LOAD.getCode());
            analysis.setSuggestion("写入超时，建议增加批次大小或检查目标库性能");
        } else if (e instanceof com.etl.common.exception.EtlConfigException) {
            analysis.setErrorCode(ErrorCode.CONFIG_001.getCode());
            analysis.setErrorCategory("CONFIG");
            analysis.setFailurePhase(null);
            analysis.setSuggestion("配置错误，请检查任务配置是否完整");
        } else if (e instanceof com.etl.common.exception.EtlException etl) {
            analysis.setErrorCode(etl.getMessage());
            analysis.setErrorCategory("UNKNOWN");
            analysis.setFailurePhase(null);
            analysis.setSuggestion("请查看详细错误日志或联系运维人员");
        } else {
            analysis.setErrorCode(ErrorCode.UNKNOWN.getCode());
            analysis.setErrorCategory("SYSTEM");
            analysis.setFailurePhase(null);
            analysis.setSuggestion("系统异常，请查看详细错误日志或联系运维人员");
        }

        analysis.setErrorMessage(e.getMessage());
        return analysis;
    }

    @Data
    public static class FailureAnalysis {
        private String errorCode;
        private String errorCategory;
        private String failurePhase;
        private String tableName;
        private String errorMessage;
        private String suggestion;
        private java.time.LocalDateTime timestamp;
    }
}
