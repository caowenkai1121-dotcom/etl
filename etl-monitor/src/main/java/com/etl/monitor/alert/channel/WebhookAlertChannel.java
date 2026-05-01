package com.etl.monitor.alert.channel;

import com.alibaba.fastjson2.JSONObject;
import com.etl.monitor.alert.AlertChannel;
import com.etl.monitor.alert.AlertMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;

/**
 * 自定义 Webhook 告警渠道
 * 支持发送 HTTP POST 请求到自定义的 Webhook 地址
 */
@Slf4j
@Component("WEBHOOK")
@RequiredArgsConstructor
public class WebhookAlertChannel implements AlertChannel {

    private final RestTemplate restTemplate;
    private WebhookConfig config;

    @Override
    public String getChannelType() {
        return "WEBHOOK";
    }

    @Override
    public boolean send(AlertMessage alert) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            log.warn("Webhook 告警渠道未配置");
            return false;
        }

        try {
            JSONObject requestBody = buildRequestBody(alert);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getWebhookUrl(), request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook 告警发送成功: messageId={}", alert.getMessageId());
                return true;
            } else {
                log.error("Webhook 告警发送失败: messageId={}, status={}, response={}",
                    alert.getMessageId(), response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            log.error("Webhook 告警发送异常: messageId={}", alert.getMessageId(), e);
            return false;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            AlertMessage testMessage = new AlertMessage();
            testMessage.setMessageId("test");
            testMessage.setLevel(AlertMessage.AlertLevel.INFO);
            testMessage.setTitle("Webhook 告警测试");
            testMessage.setContent("这是一条测试消息，用于验证 Webhook 告警渠道是否正常工作。");
            testMessage.setAlertTime(java.time.LocalDateTime.now());
            testMessage.setAlertType(com.etl.monitor.alert.AlertRule.AlertType.SYSTEM_ERROR);
            testMessage.setSource("告警服务");

            return send(testMessage);
        } catch (Exception e) {
            log.error("Webhook 告警测试连接失败", e);
            return false;
        }
    }

    @Override
    public boolean validateConfig() {
        if (config == null) {
            return false;
        }
        if (config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(AlertMessage alert) {
        JSONObject body = new JSONObject();

        // 基础字段
        body.put("alertType", alert.getAlertType().getDescription());
        body.put("severity", alert.getLevel().getLabel());
        body.put("title", alert.getTitle());
        body.put("content", alert.getContent());
        body.put("timestamp", alert.getAlertTime().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 扩展字段
        body.put("messageId", alert.getMessageId());
        body.put("source", alert.getSource());
        body.put("ruleId", alert.getRuleId());
        body.put("ruleName", alert.getRuleName());

        if (alert.getTargetType() != null) {
            body.put("targetType", alert.getTargetType());
        }
        if (alert.getTargetId() != null) {
            body.put("targetId", alert.getTargetId());
        }
        if (alert.getTargetName() != null) {
            body.put("targetName", alert.getTargetName());
        }

        // 扩展属性
        if (alert.getExtra() != null && !alert.getExtra().isEmpty()) {
            body.put("extra", alert.getExtra());
        }

        return body;
    }

    /**
     * 设置配置
     */
    public void setConfig(WebhookConfig config) {
        this.config = config;
        log.info("Webhook 告警渠道配置已更新: webhook={}", config.getWebhookUrl());
    }

    /**
     * Webhook 配置类
     */
    @Data
    public static class WebhookConfig {
        private String webhookUrl;
        private String method = "POST";
        private String contentType = "application/json";
        private String[] headers;
    }
}
