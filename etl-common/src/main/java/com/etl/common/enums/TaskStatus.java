package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum TaskStatus {

    CREATED("CREATED", "已创建", false),
    RUNNING("RUNNING", "运行中", true),
    PAUSED("PAUSED", "已暂停", false),
    STOPPED("STOPPED", "已停止", false),
    RESUMING("RESUMING", "恢复中", true),
    RETRYING("RETRYING", "重试中", true);

    private final String code;
    private final String description;
    private final boolean active;

    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus: " + code);
    }

    /**
     * 检查是否可以从当前状态转换到目标状态
     */
    public boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case CREATED -> target == RUNNING || target == STOPPED;
            case RUNNING -> target == PAUSED || target == STOPPED;
            case PAUSED -> target == RESUMING || target == STOPPED;
            case STOPPED -> target == RUNNING || target == RETRYING;
            case RESUMING -> target == RUNNING || target == STOPPED;
            case RETRYING -> target == RUNNING || target == STOPPED;
        };
    }

    /**
     * 获取有效的目标状态列表
     */
    public List<TaskStatus> getValidTransitions() {
        return Arrays.stream(values())
            .filter(this::canTransitionTo)
            .toList();
    }

    /**
     * 判断任务是否处于活跃状态（正在执行某些操作）
     */
    public boolean isActive() {
        return active;
    }
}
