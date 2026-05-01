import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

// 监控API
export const getOverview = () => request.get('/monitor/overview')
export const getTrend = (days) => request.get('/monitor/trend', { params: { days } })
export const getExecutionPage = (params) => request.get('/monitor/execution/page', { params })
export const getExecutionDetail = (id) => request.get(`/monitor/execution/${id}`)
export const getRecentAlerts = (limit) => request.get('/alert/recent', { params: { limit } })
export const getPoolStatus = () => request.get('/monitor/pool-status')
export const getThreadPoolStatus = () => request.get('/monitor/thread-pool-status')
export const getCacheStatus = () => request.get('/monitor/cache-status')
export const getSystemInfo = () => request.get('/monitor/system-info')

// 数据源API
export const getDatasourcePage = (params) => request.get('/datasource/page', { params })
export const getDatasource = (id) => request.get(`/datasource/${id}`)
export const createDatasource = (data) => request.post('/datasource', data)
export const updateDatasource = (id, data) => request.put(`/datasource/${id}`, data)
export const deleteDatasource = (id) => request.delete(`/datasource/${id}`)
export const testConnection = (id) => request.post(`/datasource/${id}/test`)
export const getTables = (id) => request.get(`/datasource/${id}/tables`)
export const getTableInfo = (dsId, tableName) => request.get(`/datasource/${dsId}/tables/${tableName}`)
export const getDatasourceTypes = () => request.get('/datasource/types')
export const getDatasourceList = () => request.get('/datasource/list')

// 任务API
export const getTaskPage = (params) => request.get('/task/page', { params })
export const getTask = (id) => request.get(`/task/${id}`)
export const createTask = (data) => request.post('/task', data)
export const updateTask = (id, data) => request.put(`/task/${id}`, data)
export const deleteTask = (id) => request.delete(`/task/${id}`)
export const executeTask = (id) => request.post(`/task/${id}/execute`)
export const stopTask = (id) => request.post(`/task/${id}/stop`)
export const getTaskProgress = (id) => request.get(`/task/${id}/progress`)
export const getTaskExecutions = (id, params) => request.get(`/task/${id}/executions`, { params })
export const getSyncModes = () => request.get('/task/sync-modes')
export const getTaskDependencies = (id) => request.get(`/task/${id}/dependencies`)
export const addTaskDependency = (id, data) => request.post(`/task/${id}/dependencies`, data)

// CDC API
export const getCdcConfigList = () => request.get('/cdc-config/list')
export const getCdcConfigPage = (params) => request.get('/cdc-config/page', { params })
export const createCdcConfig = (data) => request.post('/cdc-config', data)
export const updateCdcConfig = (id, data) => request.put(`/cdc-config/${id}`, data)
export const deleteCdcConfig = (id) => request.delete(`/cdc-config/${id}`)
export const startCdcConfig = (id) => request.post(`/cdc-config/${id}/start`)
export const stopCdcConfig = (id) => request.post(`/cdc-config/${id}/stop`)
export const getCdcConfigStatus = (id) => request.get(`/cdc-config/${id}/status`)
export const startCdcTask = (id) => request.post(`/cdc/task/${id}/start`)
export const stopCdcTask = (id) => request.post(`/cdc/task/${id}/stop`)

// 日志API
export const getLogPage = (params) => request.get('/log/page', { params })
export const exportLog = (params) => request.get('/log/export', { params, responseType: 'blob' })

// 告警API
export const getAlertRulePage = (params) => request.get('/alert/rule/page', { params })
export const getAlertRule = (id) => request.get(`/alert/rule/${id}`)
export const createAlertRule = (data) => request.post('/alert/rule', data)
export const updateAlertRule = (id, data) => request.put(`/alert/rule/${id}`, data)
export const deleteAlertRule = (id) => request.delete(`/alert/rule/${id}`)
export const toggleAlertRule = (id, enabled) => request.put(`/alert/rule/${id}/toggle`, null, { params: { enabled } })
export const getAlertRecordPage = (params) => request.get('/alert/record/page', { params })
export const ignoreAlert = (id) => request.put(`/alert/record/${id}/ignore`)
export const resolveAlert = (id) => request.put(`/alert/record/${id}/resolve`)
export const getAlertChannelList = () => request.get('/alert/channel/list')
export const testAlertChannel = (id) => request.post(`/alert/channel/${id}/test`)

// 数据质量API
export const getQualityRulePage = (params) => request.get('/quality/rule/page', { params })
export const getQualityRule = (id) => request.get(`/quality/rule/${id}`)
export const createQualityRule = (data) => request.post('/quality/rule', data)
export const updateQualityRule = (id, data) => request.put(`/quality/rule/${id}`, data)
export const deleteQualityRule = (id) => request.delete(`/quality/rule/${id}`)
export const getQualityReport = (params) => request.get('/quality/report', { params })
export const getQualityLogPageV2 = (params) => request.get('/quality/log/page', { params })

// 健康检查API
export const getHealthDetail = () => request.get('/health/detail')

// 转换API
export const getTransformPipelinePage = (params) => request.get('/transform/pipeline', { params })
export const getTransformPipeline = (id) => request.get(`/transform/pipeline/${id}`)
export const createTransformPipeline = (data) => request.post('/transform/pipeline', data)
export const updateTransformPipeline = (id, data) => request.put(`/transform/pipeline/${id}`, data)
export const deleteTransformPipeline = (id) => request.delete(`/transform/pipeline/${id}`)
export const getTransformStageList = (pipelineId) => request.get(`/transform/pipeline/${pipelineId}/steps`)
export const createTransformStage = (pipelineId, data) => request.post(`/transform/pipeline/${pipelineId}/steps`, data)
export const updateTransformStage = (id, data) => request.put(`/transform/pipeline/steps/${id}`, data)
export const deleteTransformStage = (id) => request.delete(`/transform/pipeline/steps/${id}`)
export const getTransformRules = (stageId) => request.get(`/transform/pipeline/${stageId}/steps`)
export const createTransformRule = (data) => request.post('/transform/pipeline/rule', data)
export const updateTransformRule = (id, data) => request.put('/transform/pipeline/rule', data)
export const deleteTransformRule = (id) => request.delete(`/transform/pipeline/rule/${id}`)
export const previewTransform = (id, data) => request.post(`/transform/pipeline/${id}/preview`, data)
export const reorderTransformStage = (pipelineId, data) => request.put(`/transform/pipeline/${pipelineId}/steps/reorder`, data)
export const getTransformRuleTypes = () => request.get('/transform/pipeline/rules')

// 调度API
export const scheduleTask = (taskId, cronExpression) => request.post(`/scheduler/task/${taskId}`, null, { params: { cronExpression } })
export const unscheduleTask = (taskId) => request.delete(`/scheduler/task/${taskId}`)
export const pauseTask = (taskId) => request.post(`/scheduler/task/${taskId}/pause`)
export const resumeTask = (taskId) => request.post(`/scheduler/task/${taskId}/resume`)
export const triggerTask = (taskId) => request.post(`/scheduler/task/${taskId}/trigger`)
export const updateCronExpression = (taskId, cronExpression) => request.put(`/scheduler/task/${taskId}/cron`, null, { params: { cronExpression } })
export const getScheduleInfo = (taskId) => request.get(`/scheduler/task/${taskId}/info`)

// DAG API
export const getDagPage = (params) => request.get('/scheduler/dag', { params })
export const getDag = (id) => request.get(`/scheduler/dag/${id}`)
export const createDag = (data) => request.post('/scheduler/dag', data)
export const updateDag = (id, data) => request.put(`/scheduler/dag/${id}`, data)
export const deleteDag = (id) => request.delete(`/scheduler/dag/${id}`)
export const executeDag = (id) => request.post(`/scheduler/dag/${id}/execute`)
export const getDagNodes = (id) => request.get(`/scheduler/dag/${id}/nodes`)
export const saveDagNodes = (id, data) => request.put(`/scheduler/dag/${id}/nodes`, data)

// 告警渠道创建/更新
export const createAlertChannel = (data) => request.post('/alert/channel', data)
export const updateAlertChannel = (id, data) => request.put(`/alert/channel/${id}`, data)

// CDC配置部署
export const deployCdcConfig = (id) => request.post(`/cdc-config/${id}/deploy`)
export const toggleCdcConfig = (id, enabled) => request.put(`/cdc-config/${id}/enable`, null, { params: { enabled } })

// 日志API扩展
export const getLogStats = (params) => request.get('/log/stats/overview', { params })
export const getLogStatsByStage = (params) => request.get('/log/stats/by-stage', { params })
export const getLogStatsByRule = (params) => request.get('/log/stats/by-rule', { params })
export const getErrorTrend = (params) => request.get('/log/stats/error-trend', { params })

// 链路追踪
export const getTraceDetail = (traceId) => request.get(`/log/trace/${traceId}`)
export const getTransformDetail = (id) => request.get(`/log/transform/${id}`)
export const getArchiveLogs = (params) => request.get('/log/archive', { params })

// 配置中心
export const getConfigList = (params) => request.get('/config/list', { params })
export const getConfig = (group, key) => request.get(`/config/${group}/${key}`)
export const updateConfig = (group, key, data) => request.put(`/config/${group}/${key}`, data)

// 维护API
export const cleanLogs = () => request.post('/maintenance/clean-logs')
export const clearCache = () => request.post('/maintenance/clear-cache')

// 实时任务和资源监控API
export const getRealtimeTasks = () => request.get('/monitor/realtime-tasks')
export const getResource = () => request.get('/monitor/resource')

// 新版API分组形式（保留兼容）
export const monitorAPI = {
  getOverview: () => getOverview(),
  getTrend: (days) => getTrend(days),
  getExecutionPage: (params) => getExecutionPage(params),
  getExecutionDetail: (id) => getExecutionDetail(id),
  getRecentAlerts: (limit) => getRecentAlerts(limit),
  getPoolStatus: () => getPoolStatus(),
  getThreadPoolStatus: () => getThreadPoolStatus(),
  getCacheStatus: () => getCacheStatus(),
  getSystemInfo: () => getSystemInfo(),
  getRealtimeTasks: () => getRealtimeTasks(),
  getResource: () => getResource()
}

export const datasourceAPI = {
  getPage: (params) => getDatasourcePage(params),
  getList: () => getDatasourceList(),
  get: (id) => getDatasource(id),
  create: (data) => createDatasource(data),
  update: (id, data) => updateDatasource(id, data),
  delete: (id) => deleteDatasource(id),
  testConnection: (id) => testConnection(id),
  getTables: (id) => getTables(id),
  getTableInfo: (dsId, tableName) => getTableInfo(dsId, tableName),
  getTypes: () => getDatasourceTypes()
}

export const taskAPI = {
  getPage: (params) => getTaskPage(params),
  get: (id) => getTask(id),
  create: (data) => createTask(data),
  update: (id, data) => updateTask(id, data),
  delete: (id) => deleteTask(id),
  execute: (id) => executeTask(id),
  stop: (id) => stopTask(id),
  getProgress: (id) => getTaskProgress(id),
  getExecutions: (id, params) => getTaskExecutions(id, params),
  getSyncModes: () => getSyncModes(),
  getDependencies: (id) => getTaskDependencies(id),
  addDependency: (id, data) => addTaskDependency(id, data)
}

export const cdcAPI = {
  getConfigs: () => getCdcConfigList(),
  getPage: (params) => getCdcConfigPage(params),
  create: (data) => createCdcConfig(data),
  update: (id, data) => updateCdcConfig(id, data),
  delete: (id) => deleteCdcConfig(id),
  start: (id) => startCdcConfig(id),
  stop: (id) => stopCdcConfig(id),
  getStatus: (id) => getCdcConfigStatus(id),
  startTask: (id) => startCdcTask(id),
  stopTask: (id) => stopCdcTask(id)
}

export const logAPI = {
  getLogPage: (params) => getLogPage(params),
  exportLog: (params) => exportLog(params)
}

export const alertAPI = {
  getAlertRulePage: (params) => getAlertRulePage(params),
  getAlertRule: (id) => getAlertRule(id),
  createAlertRule: (data) => createAlertRule(data),
  updateAlertRule: (id, data) => updateAlertRule(id, data),
  deleteAlertRule: (id) => deleteAlertRule(id),
  toggleAlertRule: (id, enabled) => toggleAlertRule(id, enabled),
  getAlertRecordPage: (params) => getAlertRecordPage(params),
  ignoreAlert: (id) => ignoreAlert(id),
  resolveAlert: (id) => resolveAlert(id),
  getAlertChannelList: () => getAlertChannelList(),
  testAlertChannel: (id) => testAlertChannel(id)
}

export const qualityAPI = {
  getQualityRulePage: (params) => getQualityRulePage(params),
  getQualityRule: (id) => getQualityRule(id),
  createQualityRule: (data) => createQualityRule(data),
  updateQualityRule: (id, data) => updateQualityRule(id, data),
  deleteQualityRule: (id) => deleteQualityRule(id),
  getQualityReport: (params) => getQualityReport(params),
  getQualityLogPageV2: (params) => getQualityLogPageV2(params)
}

export const schedulerAPI = {
  getPage: (params) => request.get('/scheduler/page', { params }),
  createTask: (data) => request.post('/scheduler/task', data),
  updateTask: (id, data) => request.put(`/scheduler/task/${id}`, data),
  deleteTask: (id) => request.delete(`/scheduler/task/${id}`),
  triggerTask: (id) => request.post(`/scheduler/task/${id}/trigger`),
  pauseTask: (id) => request.post(`/scheduler/task/${id}/pause`),
  resumeTask: (id) => request.post(`/scheduler/task/${id}/resume`),
  getScheduleInfo: (id) => request.get(`/scheduler/task/${id}/info`)
}

export const dagAPI = {
  getPage: (params) => request.get('/scheduler/dag', { params }),
  get: (id) => request.get(`/scheduler/dag/${id}`),
  create: (data) => request.post('/scheduler/dag', data),
  update: (id, data) => request.put(`/scheduler/dag/${id}`, data),
  delete: (id) => request.delete(`/scheduler/dag/${id}`),
  execute: (id) => request.post(`/scheduler/dag/${id}/execute`),
  getNodes: (id) => request.get(`/scheduler/dag/${id}/nodes`),
  saveNodes: (id, data) => request.put(`/scheduler/dag/${id}/nodes`, data),
  validate: (data) => request.post('/scheduler/dag/validate', data)
}

export const transformAPI = {
  getTransformPipelinePage: (params) => getTransformPipelinePage(params),
  getTransformPipeline: (id) => getTransformPipeline(id),
  createTransformPipeline: (data) => createTransformPipeline(data),
  updateTransformPipeline: (id, data) => updateTransformPipeline(id, data),
  deleteTransformPipeline: (id) => deleteTransformPipeline(id),
  getTransformStageList: (pipelineId) => getTransformStageList(pipelineId),
  createTransformStage: (pipelineId, data) => createTransformStage(pipelineId, data),
  updateTransformStage: (id, data) => updateTransformStage(id, data),
  deleteTransformStage: (id) => deleteTransformStage(id),
  getTransformRules: (stageId) => getTransformRules(stageId),
  createTransformRule: (data) => createTransformRule(data),
  updateTransformRule: (id, data) => updateTransformRule(id, data),
  deleteTransformRule: (id) => deleteTransformRule(id),
  previewTransform: (id, data) => previewTransform(id, data),
  reorderTransformStage: (pipelineId, data) => reorderTransformStage(pipelineId, data),
  getTransformRuleTypes: () => getTransformRuleTypes()
}

export const transformRulesAPI = {
  getPage: (params) => request.get('/transform/pipeline', { params }),
  getList: () => request.get('/transform/rules'),
  get: (id) => request.get(`/transform/pipeline/${id}`),
  create: (data) => request.post('/transform/pipeline', data),
  update: (id, data) => request.put(`/transform/pipeline/${id}`, data),
  delete: (id) => request.delete(`/transform/pipeline/${id}`),
  getRuleTypes: () => request.get('/transform/pipeline/rules')
}

export const healthAPI = {
  getHealthDetail: () => getHealthDetail()
}

export const getTransformLogPage = (params) => request.get('/log/transform/page', { params })
export const getTransformLogDetail = (id) => request.get(`/log/transform/${id}`)

export const getLogByTraceId = (traceId) => request.get(`/log/trace/${traceId}`)

// ==================== 工作流API ====================
export const getWorkflowPage = (params) => request.get('/workflow/page', { params })
export const getWorkflow = (id) => request.get(`/workflow/${id}`)
export const createWorkflow = (data) => request.post('/workflow', data)
export const updateWorkflow = (id, data) => request.put(`/workflow/${id}`, data)
export const deleteWorkflow = (id) => request.delete(`/workflow/${id}`)
export const publishWorkflow = (id) => request.post(`/workflow/${id}/publish`)
export const executeWorkflow = (id) => request.post(`/workflow/${id}/execute`)
export const getWorkflowNodeTypes = () => request.get('/workflow/node-types')

// 文件夹API
export const getFolderTree = () => request.get('/workflow/folder/tree')
export const createFolder = (data) => request.post('/workflow/folder', data)
export const updateFolder = (id, data) => request.put(`/workflow/folder/${id}`, data)
export const deleteFolder = (id) => request.delete(`/workflow/folder/${id}`)
export const getWorkflowsByFolder = (folderId) => request.get(`/workflow/folder/${folderId}/workflows`)

// ==================== API服务API ====================
export const getApiServicePage = (params) => request.get('/api-service/page', { params })
export const getApiService = (id) => request.get(`/api-service/${id}`)
export const createApiService = (data) => request.post('/api-service', data)
export const updateApiService = (id, data) => request.put(`/api-service/${id}`, data)
export const deleteApiService = (id) => request.delete(`/api-service/${id}`)
export const onlineApiService = (id) => request.post(`/api-service/${id}/online`)
export const offlineApiService = (id) => request.post(`/api-service/${id}/offline`)
export const testApiService = (id, data) => request.post(`/api-service/${id}/test`, data)
export const getApiServiceStats = (id) => request.get(`/api-service/${id}/stats`)

// ==================== 发布管理API ====================
export const getPublishPage = (params) => request.get('/publish/page', { params })
export const publishTask = (taskId, data) => request.post(`/publish/task/${taskId}`, data)
export const getPendingPublish = () => request.get('/publish/pending')
export const approvePublish = (id, data) => request.post(`/publish/${id}/approve`, data)
export const rejectPublish = (id, data) => request.post(`/publish/${id}/reject`, data)
export const getPublishHistory = (taskId) => request.get(`/publish/history/${taskId}`)
export const getPublishOverview = () => request.get('/publish/overview')

export const workflowAPI = {
  getPage: (params) => getWorkflowPage(params),
  get: (id) => getWorkflow(id),
  create: (data) => createWorkflow(data),
  update: (id, data) => updateWorkflow(id, data),
  delete: (id) => deleteWorkflow(id),
  publish: (id) => publishWorkflow(id),
  execute: (id) => executeWorkflow(id),
  getNodeTypes: () => getWorkflowNodeTypes(),
  getFolderTree: () => getFolderTree(),
  createFolder: (data) => createFolder(data),
  updateFolder: (id, data) => updateFolder(id, data),
  deleteFolder: (id) => deleteFolder(id),
  getWorkflowsByFolder: (folderId) => getWorkflowsByFolder(folderId)
}

export const apiServiceAPI = {
  getPage: (params) => getApiServicePage(params),
  get: (id) => getApiService(id),
  create: (data) => createApiService(data),
  update: (id, data) => updateApiService(id, data),
  delete: (id) => deleteApiService(id),
  online: (id) => onlineApiService(id),
  offline: (id) => offlineApiService(id),
  test: (id, data) => testApiService(id, data),
  getStats: (id) => getApiServiceStats(id)
}

export const publishAPI = {
  getPage: (params) => getPublishPage(params),
  publishTask: (taskId, data) => publishTask(taskId, data),
  getPending: () => getPendingPublish(),
  approve: (id, data) => approvePublish(id, data),
  reject: (id, data) => rejectPublish(id, data),
  getHistory: (taskId) => getPublishHistory(taskId),
  getOverview: () => getPublishOverview()
}
