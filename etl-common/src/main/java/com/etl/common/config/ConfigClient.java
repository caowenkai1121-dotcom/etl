package com.etl.common.config;

/**
 * 配置中心客户端接口
 * 定义配置中心客户端的契约方法
 */
public interface ConfigClient {

    /**
     * 获取字符串类型配置
     * @param group 配置分组
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getString(String group, String key, String defaultValue);

    /**
     * 获取整数类型配置
     * @param group 配置分组
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getInt(String group, String key, int defaultValue);

    /**
     * 获取长整数类型配置
     * @param group 配置分组
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    long getLong(String group, String key, long defaultValue);

    /**
     * 获取双精度浮点数类型配置
     * @param group 配置分组
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    double getDouble(String group, String key, double defaultValue);

    /**
     * 获取布尔类型配置
     * @param group 配置分组
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    boolean getBoolean(String group, String key, boolean defaultValue);

    /**
     * 刷新配置
     */
    void refresh();
}
