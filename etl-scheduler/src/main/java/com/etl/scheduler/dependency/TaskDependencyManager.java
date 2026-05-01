package com.etl.scheduler.dependency;

import com.etl.common.exception.EtlScheduleException;
import com.etl.engine.entity.EtlTaskDependency;
import com.etl.engine.mapper.TaskDependencyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskDependencyManager {

    private final TaskDependencyMapper dependencyMapper;

    public void addDependency(Long taskId, Long dependsOnTaskId, String dependencyType) {
        // 检查循环依赖
        if (wouldCreateCycle(taskId, dependsOnTaskId)) {
            throw new EtlScheduleException(com.etl.common.exception.ErrorCode.SCHEDULE_001,
                "添加依赖会导致循环: taskId=" + taskId + ", dependsOn=" + dependsOnTaskId);
        }
        EtlTaskDependency dep = new EtlTaskDependency();
        dep.setTaskId(taskId);
        dep.setDependsOnTaskId(dependsOnTaskId);
        dep.setDependencyType(dependencyType);
        dependencyMapper.insert(dep);
        log.info("添加任务依赖: taskId={} -> dependsOn={}", taskId, dependsOnTaskId);
    }

    public void removeDependency(Long taskId, Long dependsOnTaskId) {
        dependencyMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EtlTaskDependency>()
            .eq(EtlTaskDependency::getTaskId, taskId)
            .eq(EtlTaskDependency::getDependsOnTaskId, dependsOnTaskId));
    }

    public List<EtlTaskDependency> getDependencies(Long taskId) {
        return dependencyMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EtlTaskDependency>()
                .eq(EtlTaskDependency::getTaskId, taskId));
    }

    public List<EtlTaskDependency> getDependents(Long taskId) {
        return dependencyMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EtlTaskDependency>()
                .eq(EtlTaskDependency::getDependsOnTaskId, taskId));
    }

    public List<Long> getExecutionOrder(List<Long> taskIds) {
        // 拓扑排序，返回按依赖关系排序的任务执行顺序
        Map<Long, List<Long>> graph = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (Long taskId : taskIds) {
            graph.putIfAbsent(taskId, new ArrayList<>());
            inDegree.putIfAbsent(taskId, 0);
        }

        for (Long taskId : taskIds) {
            List<EtlTaskDependency> deps = getDependencies(taskId);
            for (EtlTaskDependency dep : deps) {
                if (taskIds.contains(dep.getDependsOnTaskId())) {
                    graph.get(dep.getDependsOnTaskId()).add(taskId);
                    inDegree.merge(taskId, 1, Integer::sum);
                }
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<Long> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            result.add(current);
            for (Long next : graph.getOrDefault(current, List.of())) {
                int newDegree = inDegree.merge(next, -1, Integer::sum);
                if (newDegree == 0) queue.add(next);
            }
        }

        if (result.size() != taskIds.size()) {
            throw new EtlScheduleException(com.etl.common.exception.ErrorCode.SCHEDULE_001,
                "存在循环依赖");
        }

        return result;
    }

    private boolean wouldCreateCycle(Long taskId, Long dependsOnTaskId) {
        if (taskId.equals(dependsOnTaskId)) return true;
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(taskId);
        visited.add(taskId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            List<EtlTaskDependency> deps = getDependencies(current);
            for (EtlTaskDependency dep : deps) {
                if (dep.getDependsOnTaskId().equals(dependsOnTaskId)) return true;
                if (visited.add(dep.getDependsOnTaskId())) queue.add(dep.getDependsOnTaskId());
            }
        }
        return false;
    }
}
