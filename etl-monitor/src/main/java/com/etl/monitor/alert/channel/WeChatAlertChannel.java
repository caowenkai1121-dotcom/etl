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

/**
 * 企业微信告警渠道
 * 使用企业微信 Webhook 发送告警通知
 */
@Slf4j
@Component("WECHAT")
@RequiredArgsConstructor
public class WeChatAlertChannel implements AlertChannel {

    private final RestTemplate restTemplate;
    private WeChatConfig config;

    @Override
    public String getChannelType() {
        return "WECHAT";
    }

    @Override
    public boolean send(AlertMessage alert) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            log.warn("企业微信告警渠道未配置");
            return false;
        }

        try {
            JSONObject requestBody = buildRequestBody(alert);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                config.getWebhookUrl(), request, String.class);

            JSONObject result = JSONObject.parseObject(response.getBody());
            if (result.getIntValue("errcode") == 0) {
                log.info("企业微信告警发送成功: messageId={}", alert.getMessageId());
                return true;
            } else {
                log.error("企业微信告警发送失败: messageId={}, error={}",
                    alert.getMessageId(), result.getString("errmsg"));
                return false;
            }

        } catch (Exception e) {
            log.error("企业微信告警发送异常: messageId={}", alert.getMessageId(), e);
            return false;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            AlertMessage testMessage = new AlertMessage();
            testMessage.setMessageId("test");
            testMessage.setLevel(AlertMessage.AlertLevel.INFO);
            testMessage.setTitle("企业微信告警测试");
            testMessage.setContent("这是一条测试消息，用于验证企业微信告警渠道是否正常工作。");
            testMessage.setAlertTime(java.time.LocalDateTime.now());
            testMessage.setAlertType(com.etl.monitor.alert.AlertRule.AlertType.SYSTEM_ERROR);
            testMessage.setSource("告警服务");

            return send(testMessage);
        } catch (Exception e) {
            log.error("企业微信告警测试连接失败", e);
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

        // 根据配置选择消息类型
        if (config.getMessageType() == null || "markdown".equals(config.getMessageType())) {
            body.put("msgtype", "markdown");
            JSONObject markdown = new JSONObject();
            markdown.put("content", alert.toMarkdown());
            body.put("markdown", markdown);
        } else if ("text".equals(config.getMessageType())) {
            body.put("msgtype", "text");
            JSONObject text = new JSONObject();
            text.put("content", alert.toPlainText());
            body.put("text", text);
        }

        // 配置@人员
        if (config.getMentionedList() != null || config.isMentionedAll()) {
            JSONObject mentioned = new JSONObject();
            if (config.getMentionedList() != null) {
                mentioned.put("mentioned_list", config.getMentionedList());
            }
            if (config.isMentionedAll()) {
                mentioned.put("mentioned_mobile_list", new String[]{"@all"});
            }
            body.put("mentioned_list", mentioned);
        }

        return body;
    }

    /**
     * 设置配置
     */
    public void setConfig(WeChatConfig config) {
        this.config = config;
        log.info("企业微信告警渠道配置已更新: webhook={}", config.getWebhookUrl());
    }

    /**
     * 企业微信配置类
     */
    @Data
    public static class WeChatConfig {
        private String webhookUrl;
        private String messageType = "markdown";
        private String[] mentionedList;
        private boolean mentionedAll = false;
    }
}
