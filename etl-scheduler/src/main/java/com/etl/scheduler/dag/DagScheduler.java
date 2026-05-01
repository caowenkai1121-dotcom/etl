package com.etl.scheduler.dag;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class DagScheduler {

    private final ExecutorService executorService;

    public DagScheduler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public DagScheduler() {
        this(Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2)));
    }

    public DagScheduler(int threadCount) {
        this(Executors.newFixedThreadPool(Math.max(1, threadCount)));
    }

    public void execute(List<DagNode> nodes, List<DagEdge> edges, DagTaskExecutor executor) {
        if (DagValidator.hasCycle(nodes, edges)) {
            throw new IllegalArgumentException("DAG存在循环依赖");
        }
        if (nodes == null) nodes = Collections.emptyList();
        if (edges == null) edges = Collections.emptyList();
        Map<String, List<String>> adj = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (DagNode n : nodes) {
            if (n == null || n.getCode() == null) continue;
            adj.put(n.getCode(), new ArrayList<>());
            inDegree.put(n.getCode(), 0);
        }
        for (DagEdge e : edges) {
            if (e == null || e.getFrom() == null || e.getTo() == null) continue;
            if (adj.containsKey(e.getFrom())) {
                adj.get(e.getFrom()).add(e.getTo());
                inDegree.merge(e.getTo(), 1, Integer::sum);
            }
        }
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }
        while (!queue.isEmpty()) {
            List<String> batch = new ArrayList<>();
            while (!queue.isEmpty()) batch.add(queue.poll());
            List<Future<?>> futures = new ArrayList<>();
            for (String code : batch) {
                futures.add(executorService.submit(() -> {
                    try { executor.execute(code); }
                    catch (Exception e) { log.error("[DAG] 节点 {} 执行失败", code, e); }
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("[DAG] 任务被中断", ie);
                } catch (ExecutionException ee) {
                    log.error("[DAG] 任务执行异常", ee.getCause());
                }
            }
            for (String code : batch) {
                List<String> nextNodes = adj.get(code);
                if (nextNodes == null) continue;
                for (String next : nextNodes) {
                    int newDegree = inDegree.merge(next, -1, Integer::sum);
                    if (newDegree == 0) queue.add(next);
                }
            }
        }
    }

    public interface DagTaskExecutor {
        void execute(String nodeCode) throws Exception;
    }
}
