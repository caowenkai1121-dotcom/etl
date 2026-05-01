import request from './index'

// ==================== 数据开发工作台API ====================

// 文件夹管理
export const getDevFolderTree = () => request.get('/folders/tree')
export const createDevFolder = (data) => request.post('/folders', data)
export const updateDevFolder = (id, data) => request.put(`/folders/${id}`, data)
export const deleteDevFolder = (id) => request.delete(`/folders/${id}`)
export const moveDevFolder = (id, data) => request.put(`/folders/${id}/move`, data)

// 数据开发任务
export const getDevTaskList = (params) => request.get('/dev/tasks', { params })
export const getDevTask = (id) => request.get(`/dev/tasks/${id}`)
export const createDevTask = (data) => request.post('/dev/tasks', data)
export const updateDevTask = (id, data) => request.put(`/dev/tasks/${id}`, data)
export const deleteDevTask = (id) => request.delete(`/dev/tasks/${id}`)
export const runDevTask = (id) => request.post(`/dev/tasks/${id}/run`)
export const stopDevTask = (id) => request.post(`/dev/tasks/${id}/stop`)
export const publishDevTask = (id, data) => request.post(`/dev/tasks/${id}/publish`, data)

// DAG配置
export const getDagConfig = (taskId, version) => {
  const params = version ? { version } : {}
  return request.get(`/dag/${taskId}`, { params })
}
export const saveDagConfig = (taskId, data) => request.put(`/dag/${taskId}`, data)
export const copyDagConfig = (sourceTaskId, targetTaskId) => request.post('/dag/copy', null, { params: { sourceTaskId, targetTaskId } })

// 任务版本
export const getTaskVersionHistory = (taskId) => request.get(`/dev/tasks/${taskId}/versions`)
export const getTaskVersion = (taskId, version) => request.get(`/dev/tasks/${taskId}/versions/${version}`)
export const rollbackTaskVersion = (taskId, version) => request.post(`/dev/tasks/${taskId}/rollback`, { version })

// 任务发布
export const getTaskPublishHistory = (taskId) => request.get(`/publish/history/${taskId}`)
export const batchPublishTasks = (taskIds, data) => request.post('/publish/batch', { taskIds, ...data })

// 任务收藏
export const getFavoriteTasks = () => request.get('/dev/tasks', { params: { favorite: true } })
export const addFavoriteTask = (taskId) => request.post(`/dev/tasks/${taskId}/favorite`)
export const removeFavoriteTask = (taskId) => request.delete(`/dev/tasks/${taskId}/favorite`)

// 任务统计
export const getTaskStats = (taskId) => request.get(`/dev/tasks/${taskId}/stats`)

// 概览统计
export const getDevStats = () => request.get('/dev/stats')

// 运行日志
export const getRunLogs = (taskId, executionId) => {
  const params = executionId ? { executionId } : {}
  return request.get(`/dev/tasks/${taskId}/logs`, { params })
}

// 算子模板
export const getTransformOperators = () => request.get('/dev/operators')

// 导出API对象
export const devAPI = {
  // 文件夹
  getFolderTree: () => getDevFolderTree(),
  createFolder: (data) => createDevFolder(data),
  updateFolder: (id, data) => updateDevFolder(id, data),
  deleteFolder: (id) => deleteDevFolder(id),
  moveFolder: (id, data) => moveDevFolder(id, data),

  // 任务
  getTaskList: (params) => getDevTaskList(params),
  getTask: (id) => getDevTask(id),
  createTask: (data) => createDevTask(data),
  updateTask: (id, data) => updateDevTask(id, data),
  deleteTask: (id) => deleteDevTask(id),
  runTask: (id) => runDevTask(id),
  stopTask: (id) => stopDevTask(id),
  publishTask: (id, data) => publishDevTask(id, data),

  // DAG
  getDag: (taskId, version) => getDagConfig(taskId, version),
  saveDag: (taskId, data) => saveDagConfig(taskId, data),
  copyDag: (sourceTaskId, targetTaskId) => copyDagConfig(sourceTaskId, targetTaskId),

  // 版本
  getVersionHistory: (taskId) => getTaskVersionHistory(taskId),
  getVersion: (taskId, version) => getTaskVersion(taskId, version),
  rollback: (taskId, version) => rollbackTaskVersion(taskId, version),

  // 发布
  getPublishHistory: (taskId) => getTaskPublishHistory(taskId),
  batchPublish: (taskIds, data) => batchPublishTasks(taskIds, data),

  // 收藏
  getFavorites: () => getFavoriteTasks(),
  addFavorite: (taskId) => addFavoriteTask(taskId),
  removeFavorite: (taskId) => removeFavoriteTask(taskId),

  // 统计
  getStats: (taskId) => getTaskStats(taskId),
  getDevStats: () => getDevStats(),

  // 日志
  getTaskLogs: (taskId, execId) => getRunLogs(taskId, execId),

  // 算子
  getOperators: () => getTransformOperators(),

  // 别名/便捷方法
  saveSchedule: (taskId, data) => request.put(`/dev/schedule/${taskId}`, data),
  getSchedule: (taskId) => request.get(`/dev/schedule/${taskId}`),
  copyTask: (taskId) => request.post(`/dev/tasks/${taskId}/copy`, { name: '' }),
  favoriteTask: (taskId, isFavorite) => isFavorite ? addFavoriteTask(taskId) : removeFavoriteTask(taskId),
  getTaskVersions: (taskId) => getTaskVersionHistory(taskId),
  rollbackTask: (taskId, version) => rollbackTaskVersion(taskId, version)
}
