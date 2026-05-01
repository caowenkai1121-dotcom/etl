package com.etl.monitor.log;

import com.etl.engine.entity.EtlSyncLog;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 日志队列，用于异步日志缓冲
 */
@Slf4j
public class LogQueue {

    private final BlockingQueue<EtlSyncLog> queue;
    private final int capacity;

    public LogQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * 添加日志到队列
     *
     * @param logEntry 日志条目
     * @return 是否添加成功
     */
    public boolean offer(EtlSyncLog logEntry) {
        boolean added = queue.offer(logEntry);
        if (!added) {
            // 队列满时降级：丢弃DEBUG/WARN，仅保留ERROR
            if ("ERROR".equals(logEntry.getLogLevel())) {
                // 尝试移除一个DEBUG日志腾出空间
                queue.poll();
                return queue.offer(logEntry);
            }
            log.debug("日志队列已满，丢弃日志: level={}", logEntry.getLogLevel());
        }
        return added;
    }

    /**
     * 批量从队列中取出日志
     *
     * @param maxCount 最大取出数量
     * @return 日志列表
     */
    public List<EtlSyncLog> drain(int maxCount) {
        List<EtlSyncLog> batch = new ArrayList<>();
        queue.drainTo(batch, maxCount);
        return batch;
    }

    /**
     * 获取当前队列大小
     */
    public int size() {
        return queue.size();
    }

    /**
     * 获取队列容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 判断队列是否为空
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
