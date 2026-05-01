package com.etl.engine.cdc;

import com.etl.engine.entity.EtlCdcPosition;
import com.etl.engine.mapper.CdcPositionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdcPositionManager {
    private final CdcPositionMapper cdcPositionMapper;

    /**
     * 获取CDC消费位点
     */
    public EtlCdcPosition getPosition(Long taskId, String connectorName) {
        return cdcPositionMapper.findByTaskAndConnector(taskId, connectorName);
    }

    /**
     * 保存CDC消费位点
     */
    public void savePosition(Long taskId, String connectorName, String topic, int partition, long offset) {
        EtlCdcPosition pos = cdcPositionMapper.findByTaskAndConnector(taskId, connectorName);
        if (pos == null) {
            pos = new EtlCdcPosition();
            pos.setTaskId(taskId);
            // 使用 connectorName 作为额外信息存储
            pos.setPositionType(connectorName);
            pos.setPositionValue(topic + ":" + partition + ":" + offset);
            pos.setExtra("{\"topic\":\"" + topic + "\",\"partition\":" + partition + ",\"offset\":" + offset + "}");
            pos.setUpdatedAt(LocalDateTime.now());
            cdcPositionMapper.insert(pos);
        } else {
            pos.setPositionType(connectorName);
            pos.setPositionValue(topic + ":" + partition + ":" + offset);
            pos.setExtra("{\"topic\":\"" + topic + "\",\"partition\":" + partition + ",\"offset\":" + offset + "}");
            pos.setUpdatedAt(LocalDateTime.now());
            cdcPositionMapper.updateById(pos);
        }
        log.info("[CDC] 位点已保存: task={}, connector={}, topic={}, partition={}, offset={}",
            taskId, connectorName, topic, partition, offset);
    }
}
