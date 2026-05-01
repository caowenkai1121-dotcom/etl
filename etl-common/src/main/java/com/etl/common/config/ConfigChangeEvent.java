package com.etl.common.config;

import org.springframework.context.ApplicationEvent;

/**
 * 配置变更事件
 */
public class ConfigChangeEvent extends ApplicationEvent {

    private final String group;
    private final String key;
    private final String value;

    /**
     * 构造函数
     * @param source 事件源
     * @param group 配置分组
     * @param key 配置键
     * @param value 配置值
     */
    public ConfigChangeEvent(Object source, String group, String key, String value) {
        super(source);
        this.group = group;
        this.key = key;
        this.value = value;
    }

    public String getGroup() {
        return group;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
