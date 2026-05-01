package com.etl.monitor.alert;

/**
 * 告警渠道接口
 */
public interface AlertChannel {

    /**
     * 获取渠道类型
     */
    String getChannelType();

    /**
     * 发送告警通知
     *
     * @param alert 告警信息
     * @return 是否发送成功
     */
    boolean send(AlertMessage alert);

    /**
     * 测试渠道连接
     *
     * @return 是否连接正常
     */
    boolean testConnection();

    /**
     * 渠道配置校验
     *
     * @return 配置是否有效
     */
    boolean validateConfig();
}
