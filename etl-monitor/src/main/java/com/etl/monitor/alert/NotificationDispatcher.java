package com.etl.monitor.alert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class NotificationDispatcher {
    private final Map<String, NotificationChannel> channels = new HashMap<>();

    public NotificationDispatcher(List<NotificationChannel> channelList) {
        for (NotificationChannel c : channelList) {
            channels.put(c.getChannelType(), c);
        }
    }

    public void dispatch(String channelsConfig, String title, String message) {
        if (channelsConfig == null || channelsConfig.isEmpty()) return;
        try {
            JSONArray arr = JSON.parseArray(channelsConfig);
            for (int i = 0; i < arr.size(); i++) {
                String type = arr.getString(i);
                NotificationChannel channel = channels.get(type);
                if (channel != null) {
                    try { channel.send(title, message); }
                    catch (Exception e) { log.error("[Notify] 渠道{}发送失败", type, e); }
                }
            }
        } catch (Exception e) {
            log.error("[Notify] 解析渠道配置失败: {}", channelsConfig, e);
        }
    }

    public interface NotificationChannel {
        String getChannelType();
        void send(String title, String message);
    }
}
