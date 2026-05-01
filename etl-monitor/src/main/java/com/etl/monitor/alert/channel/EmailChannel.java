package com.etl.monitor.alert.channel;

import com.etl.monitor.alert.NotificationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(JavaMailSender.class)
public class EmailChannel implements NotificationDispatcher.NotificationChannel {

    private final JavaMailSender javaMailSender;
    private String[] recipients;

    public EmailChannel(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void setRecipients(String[] recipients) {
        this.recipients = recipients;
    }

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    public void send(String title, String message) {
        if (recipients == null || recipients.length == 0) {
            log.warn("邮件收件人未配置，无法发送邮件通知");
            return;
        }
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(recipients);
        mailMessage.setSubject(title);
        mailMessage.setText(message);
        try {
            javaMailSender.send(mailMessage);
            log.info("邮件通知发送成功: {}", title);
        } catch (Exception e) {
            log.error("邮件通知发送失败", e);
        }
    }
}
