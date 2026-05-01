package com.etl.scheduler.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etl.common.enums.ExecutionStatus;
import com.etl.common.event.TaskStatusChangeEvent;
import com.etl.scheduler.dag.*;
import com.etl.scheduler.entity.EtlScheduleDag;
import com.etl.scheduler.mapper.ScheduleDagMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class DagScheduleService implements DisposableBean {
    private final ScheduleDagMapper scheduleDagMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService dagExecutor;

    public DagScheduleService(ScheduleDagMapper scheduleDagMapper, ApplicationEventPublisher eventPublisher) {
        this.scheduleDagMapper = scheduleDagMapper;
        this.eventPublisher = eventPublisher;
        this.dagExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
            r -> {
                Thread t = new Thread(r, "dag-exec-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
    }

    public Long createDag(EtlScheduleDag dag) {
        validateDag(dag);
        scheduleDagMapper.insert(dag);
        return dag.getId();
    }

    public EtlScheduleDag getDag(Long id) {
        return scheduleDagMapper.selectById(id);
    }

    public void updateDag(EtlScheduleDag dag) {
        validateDag(dag);
        scheduleDagMapper.updateById(dag);
    }

    public void deleteDag(Long id) {
        scheduleDagMapper.deleteById(id);
    }

    public List<EtlScheduleDag> listAllDags() {
        return scheduleDagMapper.selectList(null);
    }

    public void executeDag(Long dagId) {
        EtlScheduleDag dag = scheduleDagMapper.selectById(dagId);
        if (dag == null) throw new IllegalArgumentException("DAG不存在: " + dagId);
        List<DagNode> nodes = parseNodes(dag.getDagConfig());
        List<DagEdge> edges = parseEdges(dag.getDagConfig());
        DagScheduler dagScheduler = new DagScheduler(dagExecutor);
        dagScheduler.execute(nodes, edges, code -> {
            log.info("[DAG] 执行节点: {}", code);
            eventPublisher.publishEvent(new TaskStatusChangeEvent(this, Long.valueOf(code),
                "PENDING", ExecutionStatus.RUNNING.getCode(), "DAG触发"));
        });
    }

    private void validateDag(EtlScheduleDag dag) {
        List<DagNode> nodes = parseNodes(dag.getDagConfig());
        List<DagEdge> edges = parseEdges(dag.getDagConfig());
        if (DagValidator.hasCycle(nodes, edges)) {
            throw new IllegalArgumentException("DAG存在循环依赖，无法保存");
        }
    }

    private List<DagNode> parseNodes(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        JSONObject config = JSON.parseObject(json);
        return config.getList("nodes", DagNode.class);
    }

    private List<DagEdge> parseEdges(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        JSONObject config = JSON.parseObject(json);
        return config.getList("edges", DagEdge.class);
    }

    @Override
    public void destroy() {
        dagExecutor.shutdown();
        try {
            if (!dagExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                dagExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dagExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("DAG线程池已关闭");
    }
}
