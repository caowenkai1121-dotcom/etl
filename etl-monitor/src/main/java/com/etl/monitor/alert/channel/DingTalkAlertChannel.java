package com.etl.monitor.alert.channel;

import com.alibaba.fastjson2.JSONObject;
import com.etl.monitor.alert.AlertChannel;
import com.etl.monitor.alert.AlertMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 钉钉告警渠道
 */
@Slf4j
@Component("DINGTALK")
public class DingTalkAlertChannel implements AlertChannel {

    private final RestTemplate restTemplate;
    private DingTalkConfig config;

    public DingTalkAlertChannel(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getChannelType() {
        return "DINGTALK";
    }

    @Override
    public boolean send(AlertMessage alert) {
        if (config == null || config.getWebhookUrl() == null || config.getWebhookUrl().isEmpty()) {
            log.warn("钉钉告警渠道未配置");
            return false;
        }

        try {
            // 构建请求URL（包含签名）
            String url = buildSignedUrl();

            // 构建消息体
            JSONObject message = buildMessage(alert);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(message.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // 解析响应
            JSONObject result = JSONObject.parseObject(response.getBody());
            if (result.getIntValue("errcode") == 0) {
                log.info("钉钉告警发送成功: messageId={}", alert.getMessageId());
                return true;
            } else {
                log.error("钉钉告警发送失败: messageId={}, error={}",
                    alert.getMessageId(), result.getString("errmsg"));
                return false;
            }

        } catch (Exception e) {
            log.error("钉钉告警发送异常: messageId={}", alert.getMessageId(), e);
            return false;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            AlertMessage testMessage = new AlertMessage();
            testMessage.setMessageId("test");
            testMessage.setLevel(AlertMessage.AlertLevel.INFO);
            testMessage.setTitle("告警渠道测试");
            testMessage.setContent("这是一条测试消息，用于验证钉钉告警渠道是否正常工作。");
            testMessage.setAlertTime(java.time.LocalDateTime.now());
            testMessage.setAlertType(com.etl.monitor.alert.AlertRule.AlertType.SYSTEM_ERROR);

            return send(testMessage);
        } catch (Exception e) {
            log.error("钉钉发送测试失败", e);
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
     * 设置配置
     */
    public void setConfig(DingTalkConfig config) {
        this.config = config;
    }

    /**
     * 构建带签名的URL
     */
    private String buildSignedUrl() {
        if (config.getSecret() == null || config.getSecret().isEmpty()) {
            return config.getWebhookUrl();
        }

        try {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + config.getSecret();

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);

            return config.getWebhookUrl() + "&timestamp=" + timestamp + "&sign=" + sign;

        } catch (Exception e) {
            log.error("生成钉钉签名失败", e);
            return config.getWebhookUrl();
        }
    }

    /**
     * 构建消息体
     */
    private JSONObject buildMessage(AlertMessage alert) {
        JSONObject message = new JSONObject();
        message.put("msgtype", "markdown");

        JSONObject markdown = new JSONObject();

        // 构建Markdown内容
        StringBuilder content = new StringBuilder();
        content.append("## ").append(alert.getTitle()).append("\n\n");
        content.append("**告警级别**: ").append(alert.getLevel().getLabel()).append("\n\n");
        content.append("**告警类型**: ").append(alert.getAlertType().getDescription()).append("\n\n");
        content.append("**告警时间**: ").append(alert.getAlertTime()).append("\n\n");
        content.append("**告警来源**: ").append(alert.getSource()).append("\n\n");

        if (alert.getTargetName() != null) {
            content.append("**关联对象**: ").append(alert.getTargetName()).append("\n\n");
        }

        content.append("**详细信息**:\n\n").append(alert.getContent());

        // 添加@人员
        if (config.getAtMobiles() != null && config.getAtMobiles().length > 0) {
            content.append("\n\n");
            for (String mobile : config.getAtMobiles()) {
                content.append("@").append(mobile).append(" ");
            }
        }

        markdown.put("content", content.toString());
        message.put("markdown", markdown);

        // @人员配置
        if (config.getAtMobiles() != null && config.getAtMobiles().length > 0) {
            JSONObject at = new JSONObject();
            at.put("atMobiles", config.getAtMobiles());
            at.put("isAtAll", config.isAtAll());
            message.put("at", at);
        }

        return message;
    }

    /**
     * 钉钉配置
     */
    @Data
    public static class DingTalkConfig {
        private String webhookUrl;
        private String secret;
        private String[] atMobiles;
        private boolean atAll = false;
    }
}
