<template>
  <div class="task-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable style="width: 240px; margin-right: 16px;">
        <template #append>
          <el-button @click="fetchData">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-select v-model="queryParams.syncMode" placeholder="同步模式" clearable style="width: 120px; margin-right: 16px;" @change="fetchData">
        <el-option label="全部" value="" />
        <el-option label="全量" value="FULL" />
        <el-option label="增量" value="INCREMENTAL" />
        <el-option label="CDC" value="CDC" />
      </el-select>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        <span>新增任务</span>
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card class="data-card">
      <el-table :data="tableData" v-loading="loading" class="data-table">
        <el-table-column prop="name" label="任务名" min-width="140" show-overflow-tooltip />
        <el-table-column label="源→目标" min-width="200">
          <template #default="{ row }">
            <div class="source-target">
              <el-tag size="small" style="margin-right: 8px;">{{ getDatasourceName(row.sourceDsId) }}</el-tag>
              <el-icon style="color: #c0c4cc;"><ArrowRight /></el-icon>
              <el-tag size="small" style="margin-left: 8px;">{{ getDatasourceName(row.targetDsId) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="syncMode" label="模式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.syncMode === 'FULL' ? 'primary' : row.syncMode === 'INCREMENTAL' ? 'success' : 'warning'" size="small">
              {{ row.syncMode === 'FULL' ? '全量' : row.syncMode === 'INCREMENTAL' ? '增量' : 'CDC' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="status-dot" :class="row.status === 'RUNNING' ? 'running' : row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'error' : 'pending'"></span>
              <span>{{ getStatusText(row.status) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpression" label="Cron" width="140" show-overflow-tooltip />
        <el-table-column prop="lastSyncTime" label="最后执行" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'RUNNING'" size="small" type="success" @click="handleExecute(row)">
              <el-icon><VideoPlay /></el-icon>
              执行
            </el-button>
            <el-button v-if="row.status === 'RUNNING'" size="small" type="warning" @click="handleStop(row)">
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 实时进度条 -->
      <div v-if="progressVisible" class="progress-panel">
        <el-progress :percentage="progress.percentage" :status="progress.status" :stroke-width="16">
          <template #default="{ percentage }">
            <div class="progress-info">
              <span>{{ percentage }}%</span>
              <span class="progress-text">{{ progress.statusText }}</span>
            </div>
          </template>
        </el-progress>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        class="pagination-wrap"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1000px" @opened="onDialogOpened">
      <el-form :model="form" label-width="110px" :rules="rules" ref="formRef">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入任务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="任务描述">
              <el-input v-model="form.description" placeholder="请输入任务描述" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Cron表达式">
              <el-input v-model="form.cronExpression" placeholder="如 0 0 2 * * ?" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">源目标选择</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源数据源" prop="sourceDsId">
              <el-select v-model="form.sourceDsId" placeholder="请选择源数据源" style="width: 100%" @change="onSourceChange">
                <el-option v-for="ds in sourceDatasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标数据源" prop="targetDsId">
              <el-select v-model="form.targetDsId" placeholder="请选择目标数据源" style="width: 100%">
                <el-option v-for="ds in targetDatasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">同步配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="同步模式" prop="syncMode">
              <el-select v-model="form.syncMode" placeholder="请选择同步模式" style="width: 100%">
                <el-option label="全量同步" value="FULL" />
                <el-option label="增量同步" value="INCREMENTAL" />
                <el-option label="CDC同步" value="CDC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="同步范围" prop="syncScope">
              <el-select v-model="form.syncScope" placeholder="请选择同步范围" style="width: 100%">
                <el-option label="单表" value="TABLE" />
                <el-option label="多表" value="MULTI_TABLE" />
                <el-option label="全库" value="DATABASE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="同步策略">
              <el-select v-model="form.syncStrategy" placeholder="请选择同步策略" style="width: 100%">
                <el-option label="覆盖" value="OVERWRITE" />
                <el-option label="追加" value="APPEND" />
                <el-option label="更新" value="UPSERT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 增量字段配置（增量模式时显示） -->
        <template v-if="form.syncMode === 'INCREMENTAL'">
          <el-divider content-position="left">增量配置</el-divider>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="增量字段" prop="incrementField">
                <el-input v-model="form.incrementField" placeholder="如 updated_at" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="起始值">
                <el-input v-model="form.incrementValue" placeholder="如 2020-01-01 00:00:00" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-divider content-position="left">
          <span>表选择</span>
          <el-tag v-if="selectedTables.length > 0" size="small" type="success" style="margin-left: 10px;">已选 {{ selectedTables.length }} 张表</el-tag>
        </el-divider>
        <el-form-item label="源表列表" prop="tableConfig">
          <div class="table-select-area">
            <div class="table-select-left">
              <div class="select-tip" v-if="!form.sourceDsId">请先选择源数据源</div>
              <div class="select-tip" v-else-if="tableLoading">正在加载表列表...</div>
              <el-checkbox-group v-else v-model="selectedTables" @change="onTableSelectionChange">
                <div v-for="tbl in sourceTables" :key="tbl.tableName" class="table-item">
                  <el-checkbox :value="tbl.tableName">
                    <span class="table-name">{{ tbl.tableName }}</span>
                    <el-tag size="small" type="info" style="margin-left: 6px;">{{ tbl.columns?.length || 0 }} 列</el-tag>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
            <div class="table-select-right" v-if="selectedTables.length > 0">
              <div class="mapping-title">目标表名映射</div>
              <div v-for="tbl in selectedTables" :key="tbl" class="mapping-row">
                <span class="source-tbl">{{ tbl }}</span>
                <el-icon><ArrowRight /></el-icon>
                <el-input v-model="tableNameMap[tbl]" size="small" :placeholder="tbl" style="width: 180px;" />
              </div>
            </div>
          </div>
        </el-form-item>

        <!-- 字段映射（展开选中的表查看详情） -->
        <el-divider v-if="selectedTables.length > 0" content-position="left">字段映射（可选）</el-divider>
        <el-form-item v-if="selectedTables.length > 0" label="字段预览">
          <el-collapse accordion>
            <el-collapse-item v-for="tbl in selectedTables" :key="tbl" :title="tbl + ' → ' + (tableNameMap[tbl] || tbl)">
              <el-table :data="getTableColumns(tbl)" size="small" max-height="250">
                <el-table-column prop="columnName" label="源字段" width="180" />
                <el-table-column prop="columnType" label="类型" width="120" />
                <el-table-column prop="columnName" label="目标字段" width="180">
                  <template #default="{ row }">
                    <el-input v-model="columnNameMap[tbl + '.' + row.columnName]" size="small" :placeholder="row.columnName" />
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag v-if="row.primaryKey" size="small" type="warning">主键</el-tag>
                    <el-tag v-else size="small" type="info">普通</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskAPI, datasourceAPI, getTables, getTableInfo, scheduleTask } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增任务')
const formRef = ref(null)
const datasourceList = ref([])
const progressVisible = ref(false)
const submitLoading = ref(false)

// 表选择相关
const sourceTables = ref([])
const selectedTables = ref([])
const tableNameMap = reactive({})
const columnNameMap = reactive({})
const tableLoading = ref(false)
const tableColumnsCache = reactive({})  // 缓存表列信息
const columnsLoading = ref(false)

// 源数据源列表（排除DMS_DEV和DMS_UAT，因为这些只能作为源）
const sourceDatasourceList = computed(() => {
  return datasourceList.value.filter(ds => true) // 全部都可作为源
})

// 目标数据源列表（排除DMS_UAT和DMS_DEV，这些只能作为源）
const targetDatasourceList = computed(() => {
  return datasourceList.value.filter(ds => {
    return !['DMS_UAT', 'DMS_DEV'].includes(ds.name)
  })
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  syncMode: ''
})

const form = reactive({
  id: null,
  name: '',
  description: '',
  sourceDsId: null,
  targetDsId: null,
  syncMode: 'FULL',
  syncScope: 'TABLE',
  syncStrategy: 'OVERWRITE',
  tableConfig: '',
  fieldMapping: '',
  incrementField: '',
  incrementValue: '',
  cronExpression: '',
  priority: 5,
  batchSize: 1000
})

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceDsId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  targetDsId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  syncMode: [{ required: true, message: '请选择同步模式', trigger: 'change' }],
  syncScope: [{ required: true, message: '请选择同步范围', trigger: 'change' }]
}

const progress = ref({
  percentage: 0,
  status: 'active',
  statusText: '执行中'
})

onMounted(() => {
  fetchData()
  fetchDatasources()
})

const onDialogOpened = () => {
  // 对话框打开后的初始化
}

const onSourceChange = async (dsId) => {
  if (!dsId) {
    sourceTables.value = []
    return
  }
  tableLoading.value = true
  try {
    const res = await getTables(dsId)
    sourceTables.value = res.data || []
  } catch (e) {
    console.error('加载表列表失败:', e)
    sourceTables.value = []
    ElMessage.error('加载表列表失败')
  } finally {
    tableLoading.value = false
  }
}

const loadTableColumns = async (tableName) => {
  if (!form.sourceDsId || tableColumnsCache[tableName]) return
  try {
    const res = await getTableInfo(form.sourceDsId, tableName)
    const info = res.data
    if (info && info.columns) {
      tableColumnsCache[tableName] = info.columns
    }
  } catch (e) {
    console.error('加载列信息失败:', tableName, e)
  }
}

const onTableSelectionChange = async (vals) => {
  // 初始化新选中表的默认目标名
  for (const tbl of vals) {
    if (!tableNameMap[tbl]) {
      tableNameMap[tbl] = tbl + '_copy'
    }
  }
  // 清理取消选中的表
  for (const key of Object.keys(tableNameMap)) {
    if (!vals.includes(key)) {
      delete tableNameMap[key]
    }
  }
  // 加载新选中表的列信息
  columnsLoading.value = true
  try {
    for (const tbl of vals) {
      await loadTableColumns(tbl)
    }
  } finally {
    columnsLoading.value = false
  }
  // 自动生成tableConfig
  buildTableConfig()
}

const buildTableConfig = () => {
  const config = selectedTables.value.map(tbl => ({
    sourceTable: tbl,
    targetTable: tableNameMap[tbl] || (tbl + '_copy')
  }))
  form.tableConfig = JSON.stringify(config)
}

const getTableColumns = (tbl) => {
  return tableColumnsCache[tbl] || []
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await taskAPI.getPage(queryParams)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取任务列表失败:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const fetchDatasources = async () => {
  try {
    const res = await datasourceAPI.getPage({ pageNum: 1, pageSize: 100 })
    datasourceList.value = res.data?.list || []
  } catch (error) {
    console.error('获取数据源列表失败:', error)
    datasourceList.value = []
  }
}

const getDatasourceName = (dsId) => {
  const ds = datasourceList.value.find(d => d.id === dsId)
  return ds ? ds.name : ''
}

const getStatusText = (status) => {
  const texts = { 'CREATED': '已创建', 'RUNNING': '运行中', 'PAUSED': '已暂停', 'STOPPED': '已停止', 'SUCCESS': '成功', 'FAILED': '失败', 'SKIPPED': '已跳过' }
  return texts[status] || status
}

const handleAdd = () => {
  dialogTitle.value = '新增任务'
  Object.assign(form, {
    id: null,
    name: '',
    description: '',
    sourceDsId: null,
    targetDsId: null,
    syncMode: 'FULL',
    syncScope: 'TABLE',
    syncStrategy: 'OVERWRITE',
    tableConfig: '',
    fieldMapping: '',
    incrementField: '',
    incrementValue: '',
    cronExpression: '',
    priority: 5,
    batchSize: 1000
  })
  // 清空表选择状态
  selectedTables.value = []
  sourceTables.value = []
  for (const key of Object.keys(tableNameMap)) { delete tableNameMap[key] }
  for (const key of Object.keys(columnNameMap)) { delete columnNameMap[key] }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑任务'
  // 清除之前状态
  selectedTables.value = []
  sourceTables.value = []
  for (const key of Object.keys(tableNameMap)) { delete tableNameMap[key] }
  for (const key of Object.keys(columnNameMap)) { delete columnNameMap[key] }

  // 拷贝行数据到form，处理字段名映射
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    description: row.description || '',
    sourceDsId: row.sourceDsId,
    targetDsId: row.targetDsId,
    syncMode: row.syncMode || 'FULL',
    syncScope: row.syncScope || 'TABLE',
    syncStrategy: row.syncStrategy || 'OVERWRITE',
    tableConfig: row.tableConfig || '',
    fieldMapping: row.fieldMapping || '',
    incrementField: row.incrementalField || '',
    incrementValue: row.incrementalValue || '',
    cronExpression: row.cronExpression || '',
    priority: row.priority || 5,
    batchSize: row.batchSize || 1000
  })

  // 解析tableConfig回显已选表和目标表名映射
  if (row.tableConfig) {
    try {
      const config = typeof row.tableConfig === 'string' ? JSON.parse(row.tableConfig) : row.tableConfig
      if (Array.isArray(config)) {
        selectedTables.value = config.map(t => t.sourceTable)
        config.forEach(t => {
          if (t.targetTable && t.targetTable !== t.sourceTable) {
            tableNameMap[t.sourceTable] = t.targetTable
          }
        })
      }
    } catch (e) { /* ignore parse error */ }
  }

  // 解析fieldMapping回显字段映射
  if (row.fieldMapping) {
    try {
      const mappings = typeof row.fieldMapping === 'string' ? JSON.parse(row.fieldMapping) : row.fieldMapping
      if (Array.isArray(mappings)) {
        mappings.forEach(m => {
          if (m.sourceField && m.targetField && m.sourceField !== m.targetField) {
            const fullKey = (m.sourceTable || selectedTables.value[0] || '') + '.' + m.sourceField
            columnNameMap[fullKey] = m.targetField
          }
        })
      }
    } catch (e) { /* ignore parse error */ }
  }

  dialogVisible.value = true

  // 加载源表列表（等对话框渲染完成后会自动触发onSourceChange）
  if (row.sourceDsId) {
    await onSourceChange(row.sourceDsId)
    // 加载已选表的列信息
    if (selectedTables.value.length > 0) {
      columnsLoading.value = true
      try {
        for (const tbl of selectedTables.value) {
          await loadTableColumns(tbl)
        }
      } finally {
        columnsLoading.value = false
      }
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该任务？', '提示', { type: 'warning' })
    await taskAPI.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleExecute = async (row) => {
  try {
    await taskAPI.execute(row.id)
    ElMessage.success('任务已开始执行')
    fetchData()
    progressVisible.value = true
    simulateProgress()
  } catch (error) {
    console.error('执行任务失败:', error)
    ElMessage.error('执行任务失败')
  }
}

const handleStop = async (row) => {
  try {
    await taskAPI.stop(row.id)
    ElMessage.success('任务已停止')
    fetchData()
    progressVisible.value = false
  } catch (error) {
    console.error('停止任务失败:', error)
    ElMessage.error('停止任务失败')
  }
}

const simulateProgress = () => {
  let percent = 0
  const interval = setInterval(() => {
    percent += Math.floor(Math.random() * 10) + 1
    if (percent >= 100) {
      percent = 100
      clearInterval(interval)
      progress.value.status = 'success'
      progress.value.statusText = '完成'
      setTimeout(() => {
        progressVisible.value = false
      }, 1500)
    }
    progress.value.percentage = percent
  }, 500)
}

const handleSubmit = async () => {
  try {
    // 验证表选择
    if (selectedTables.value.length === 0) {
      ElMessage.warning('请至少选择一张表')
      return
    }
    buildTableConfig()
    // 生成字段映射
    const fieldMappings = []
    for (const tbl of selectedTables.value) {
      const cols = getTableColumns(tbl)
      for (const col of cols) {
        const key = tbl + '.' + col.columnName
        const targetCol = columnNameMap[key] || col.columnName
        if (targetCol !== col.columnName) {
          fieldMappings.push({
            sourceTable: tbl,
            sourceField: col.columnName,
            targetField: targetCol
          })
        }
      }
    }
    if (fieldMappings.length > 0) {
      form.fieldMapping = JSON.stringify(fieldMappings)
    }

    await formRef.value.validate()
    submitLoading.value = true
    const submitData = {
      ...form,
      incrementalField: form.incrementField || undefined,
      incrementalValue: form.incrementValue || undefined
    }
    let taskId = form.id
    if (form.id) {
      await taskAPI.update(form.id, submitData)
    } else {
      const result = await taskAPI.create(submitData)
      taskId = result.data  // 新建任务返回的ID
    }
    // 如果配置了Cron表达式，自动建立调度
    if (form.cronExpression && form.cronExpression.trim() && taskId) {
      try {
        await scheduleTask(taskId, form.cronExpression.trim())
        ElMessage.success('操作成功，调度已创建')
      } catch (e) {
        ElMessage.warning('任务已创建，但调度创建失败：' + (e.message || '未知错误'))
      }
    } else {
      ElMessage.success('操作成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.task-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100vh;

  .search-bar {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
  }

  .data-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) {
      display: none;
    }
  }

  .source-target {
    display: flex;
    align-items: center;
    justify-content: center;

    .el-tag {
      font-size: 12px;
    }

    .el-icon {
      font-size: 14px;
    }
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.running {
        background: #4f6ef7;
        box-shadow: 0 0 6px rgba(79, 110, 247, 0.4);
        animation: breathe 2s infinite;
      }

      &.success {
        background: #52c41a;
        box-shadow: 0 0 6px rgba(82, 196, 26, 0.4);
      }

      &.error {
        background: #ff4d4f;
        animation: blink 1s infinite;
      }

      &.pending {
        background: #dcdfe6;
      }
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .progress-panel {
    margin-top: 20px;

    .progress-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      font-size: 12px;
      color: #606266;

      .progress-text {
        margin-top: 4px;
        font-size: 11px;
      }
    }
  }
}

.table-select-area {
  display: flex;
  gap: 24px;
  width: 100%;

  .table-select-left {
    flex: 1;
    max-height: 300px;
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 12px;
    background: #fafafa;

    .select-tip {
      color: #909399;
      font-size: 13px;
      text-align: center;
      padding: 20px 0;
    }

    .table-item {
      padding: 4px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .table-name {
        font-weight: 500;
        font-size: 13px;
      }
    }
  }

  .table-select-right {
    flex: 1;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 12px;
    background: #fafafa;
    max-height: 300px;
    overflow-y: auto;

    .mapping-title {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebeef5;
    }

    .mapping-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .source-tbl {
        font-size: 12px;
        color: #606266;
        min-width: 120px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@media (max-width: 768px) {
  .task-page {
    padding: 16px;

    .search-bar {
      flex-wrap: wrap;

      .el-input,
      .el-select {
        width: 100%;
        margin-right: 0;
        margin-bottom: 12px;
      }
    }
  }
}
</style>
