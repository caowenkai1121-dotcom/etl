package com.etl.api.config;

import com.etl.common.exception.*;
import com.etl.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 连接异常处理
     */
    @ExceptionHandler(EtlConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleEtlConnectionException(EtlConnectionException e) {
        log.error("连接异常: {}", e.getMessage(), e);
        // 记录告警
        return Result.error(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage());
    }

    /**
     * 抽取异常处理
     */
    @ExceptionHandler(EtlExtractException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleEtlExtractException(EtlExtractException e) {
        log.error("抽取异常: {}", e.getMessage(), e);
        // 标记任务失败
        return Result.error(500, e.getMessage());
    }

    /**
     * 加载异常处理
     */
    @ExceptionHandler(EtlLoadException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleEtlLoadException(EtlLoadException e) {
        log.error("加载异常: {}", e.getMessage(), e);
        // 判断是否可重试
        return Result.error(500, e.getMessage());
    }

    /**
     * 配置异常处理
     */
    @ExceptionHandler(EtlConfigException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleEtlConfigException(EtlConfigException e) {
        log.error("配置异常: {}", e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    /**
     * 调度异常处理
     */
    @ExceptionHandler(EtlScheduleException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleEtlScheduleException(EtlScheduleException e) {
        log.error("调度异常: {}", e.getMessage(), e);
        return Result.error(500, e.getMessage());
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(EtlBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleEtlBusinessException(EtlBusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    /**
     * ETL异常处理
     */
    @ExceptionHandler(EtlException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleEtlException(EtlException e) {
        log.error("ETL异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        if (e.isRetryable()) {
            log.warn("该异常可重试");
        }
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.isEmpty() ? "参数校验失败" : fieldErrors.get(0).getDefaultMessage();
        log.error("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.isEmpty() ? "参数绑定失败" : fieldErrors.get(0).getDefaultMessage();
        log.error("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统异常: " + e.getMessage());
    }
}
