package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.result.PageResult;
import com.etl.engine.dto.WorkflowCreateRequest;
import com.etl.engine.dto.WorkflowResponse;
import com.etl.engine.entity.EtlTaskWorkflow;
import com.etl.engine.mapper.WorkflowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工作流服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService extends ServiceImpl<WorkflowMapper, EtlTaskWorkflow> {

    /**
     * 分页查询工作流
     */
    public PageResult<WorkflowResponse> pageList(Integer pageNum, Integer pageSize, String name, String status, Long folderId) {
        Page<EtlTaskWorkflow> page = new Page<>(pageNum, pageSize);

        lambdaQuery()
            .like(name != null && !name.isEmpty(), EtlTaskWorkflow::getName, name)
            .eq(status != null && !status.isEmpty(), EtlTaskWorkflow::getStatus, status)
            .eq(folderId != null, EtlTaskWorkflow::getFolderId, folderId)
            .orderByDesc(EtlTaskWorkflow::getCreateTime)
            .page(page);

        return PageResult.of(
            page.getRecords().stream().map(this::toResponse).toList(),
            page.getTotal(),
            pageNum,
            pageSize
        );
    }

    /**
     * 获取工作流详情
     */
    public WorkflowResponse getDetail(Long id) {
        EtlTaskWorkflow workflow = getById(id);
        if (workflow == null) {
            return null;
        }
        return toResponse(workflow);
    }

    /**
     * 创建工作流
     */
    @Transactional
    public Long createWorkflow(WorkflowCreateRequest request) {
        EtlTaskWorkflow workflow = new EtlTaskWorkflow();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setFolderId(request.getFolderId());
        workflow.setWorkflowJson(request.getWorkflowJson());
        workflow.setCronExpression(request.getCronExpression());
        workflow.setStatus("DRAFT");
        workflow.setPublishStatus("PENDING");
        workflow.setVersion(1);
        workflow.setCreateBy("system");
        workflow.setCreateTime(LocalDateTime.now());

        save(workflow);
        return workflow.getId();
    }

    /**
     * 更新工作流
     */
    @Transactional
    public void updateWorkflow(Long id, WorkflowCreateRequest request) {
        EtlTaskWorkflow workflow = getById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在");
        }

        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setFolderId(request.getFolderId());
        workflow.setWorkflowJson(request.getWorkflowJson());
        workflow.setCronExpression(request.getCronExpression());

        // 如果已发布，更新状态为"待更新"
        if ("PUBLISHED".equals(workflow.getPublishStatus())) {
            workflow.setPublishStatus("UPDATED");
            workflow.setVersion(workflow.getVersion() + 1);
        }

        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);
    }

    /**
     * 删除工作流
     */
    @Transactional
    public void deleteWorkflow(Long id) {
        removeById(id);
    }

    /**
     * 发布工作流
     */
    @Transactional
    public void publishWorkflow(Long id) {
        EtlTaskWorkflow workflow = getById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在");
        }

        workflow.setStatus("PUBLISHED");
        workflow.setPublishStatus("PUBLISHED");
        workflow.setUpdateTime(LocalDateTime.now());
        updateById(workflow);
    }

    /**
     * 执行工作流
     */
    public String executeWorkflow(Long id) {
        EtlTaskWorkflow workflow = getById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在");
        }

        String executionNo = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("开始执行工作流: id={}, name={}, executionNo={}", id, workflow.getName(), executionNo);

        // TODO: 实际执行逻辑，解析workflowJson并依次执行节点

        return executionNo;
    }

    /**
     * 获取文件夹下的工作流列表
     */
    public java.util.List<WorkflowResponse> listByFolder(Long folderId) {
        return lambdaQuery()
            .eq(folderId != null, EtlTaskWorkflow::getFolderId, folderId)
            .orderByAsc(EtlTaskWorkflow::getName)
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 转换为响应DTO
     */
    private WorkflowResponse toResponse(EtlTaskWorkflow entity) {
        WorkflowResponse response = new WorkflowResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setFolderId(entity.getFolderId());
        response.setWorkflowJson(entity.getWorkflowJson());
        response.setStatus(entity.getStatus());
        response.setPublishStatus(entity.getPublishStatus());
        response.setVersion(entity.getVersion());
        response.setCronExpression(entity.getCronExpression());
        response.setCreateBy(entity.getCreateBy());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }
}