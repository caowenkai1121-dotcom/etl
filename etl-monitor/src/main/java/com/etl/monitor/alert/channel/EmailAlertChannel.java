package com.etl.monitor.alert.channel;

import com.etl.monitor.alert.AlertChannel;
import com.etl.monitor.alert.AlertMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * 邮件告警渠道
 * 仅在配置了邮件服务器时启用
 */
@Slf4j
@Component("EMAIL")
@ConditionalOnBean(JavaMailSender.class)
public class EmailAlertChannel implements AlertChannel {

    private final JavaMailSender mailSender;
    private EmailConfig config;

    public EmailAlertChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    public boolean send(AlertMessage alert) {
        if (config == null || config.getRecipients() == null || config.getRecipients().length == 0) {
            log.warn("邮件告警渠道未配置或收件人为空");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置发件人
            helper.setFrom(config.getFrom());

            // 设置收件人
            helper.setTo(config.getRecipients());

            // 设置主题
            String subject = String.format("【%s】%s", alert.getLevel().getLabel(), alert.getTitle());
            helper.setSubject(subject);

            // 设置内容（HTML格式）
            String content = buildHtmlContent(alert);
            helper.setText(content, true);

            // 发送邮件
            mailSender.send(message);

            log.info("邮件告警发送成功: messageId={}, recipients={}",
                alert.getMessageId(), String.join(",", config.getRecipients()));
            return true;

        } catch (MessagingException e) {
            log.error("邮件告警发送失败: messageId={}", alert.getMessageId(), e);
            return false;
        }
    }

    @Override
    public boolean testConnection() {
        if (config == null || config.getRecipients() == null || config.getRecipients().length == 0) {
            log.warn("邮件告警渠道配置无效，无法测试连接");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getFrom());
            message.setTo(config.getRecipients()[0]);
            message.setSubject("ETL系统告警渠道测试");
            message.setText("这是一封测试邮件，用于验证邮件告警渠道是否正常工作。");
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("邮件发送测试失败", e);
            return false;
        }
    }

    @Override
    public boolean validateConfig() {
        if (config == null) {
            return false;
        }
        if (config.getFrom() == null || config.getFrom().isEmpty()) {
            return false;
        }
        if (config.getRecipients() == null || config.getRecipients().length == 0) {
            return false;
        }
        return true;
    }

    /**
     * 设置配置
     */
    public void setConfig(EmailConfig config) {
        this.config = config;
    }

    /**
     * 构建HTML邮件内容
     */
    private String buildHtmlContent(AlertMessage alert) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; padding: 20px; }");
        html.append(".header { background-color: #").append(alert.getLevel().getColor())
            .append("; color: white; padding: 15px; border-radius: 5px; }");
        html.append(".content { padding: 20px; background-color: #f5f7fa; margin-top: 10px; border-radius: 5px; }");
        html.append(".info-row { margin: 10px 0; }");
        html.append(".label { font-weight: bold; color: #606266; }");
        html.append(".value { color: #303133; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // 头部
        html.append("<div class='header'>");
        html.append("<h2>").append(alert.getTitle()).append("</h2>");
        html.append("<p>告警级别: ").append(alert.getLevel().getLabel()).append("</p>");
        html.append("</div>");

        // 内容
        html.append("<div class='content'>");
        html.append("<div class='info-row'><span class='label'>告警类型:</span> ")
            .append("<span class='value'>").append(alert.getAlertType().getDescription()).append("</span></div>");
        html.append("<div class='info-row'><span class='label'>告警时间:</span> ")
            .append("<span class='value'>").append(alert.getAlertTime()).append("</span></div>");
        html.append("<div class='info-row'><span class='label'>告警来源:</span> ")
            .append("<span class='value'>").append(alert.getSource()).append("</span></div>");

        if (alert.getTargetName() != null) {
            html.append("<div class='info-row'><span class='label'>关联对象:</span> ")
                .append("<span class='value'>").append(alert.getTargetName()).append("</span></div>");
        }

        html.append("<div class='info-row' style='margin-top: 15px;'><span class='label'>详细信息:</span></div>");
        html.append("<pre style='background: #fff; padding: 10px; border-radius: 3px;'>")
            .append(alert.getContent()).append("</pre>");

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 邮件配置
     */
    @Data
    public static class EmailConfig {
        private String from;
        private String[] recipients;
        private String template;
    }
}
