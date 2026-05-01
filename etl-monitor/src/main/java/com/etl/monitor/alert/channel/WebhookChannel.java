package com.etl.monitor.alert.channel;

import com.etl.monitor.alert.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WebhookChannel implements NotificationDispatcher.NotificationChannel {

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private String webhookUrl;

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @Override
    public String getChannelType() {
        return "WEBHOOK";
    }

    @Override
    public void send(String title, String message) {
        if (restTemplate == null) {
            log.warn("RestTemplate未配置，无法发送Webhook通知");
            return;
        }
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("example.com")) {
            log.warn("Webhook URL未配置，无法发送通知");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("message", message);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForObject(webhookUrl, entity, String.class);
            log.info("Webhook通知发送成功: {}", title);
        } catch (Exception e) {
            log.error("Webhook通知发送失败", e);
        }
    }
}
