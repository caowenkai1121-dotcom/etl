<template>
  <div class="scheduler-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Clock /></el-icon>
            调度管理
          </span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建调度
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="调度名称">
            <el-input v-model="queryParams.name" placeholder="请输入名称" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择" clearable>
              <el-option label="运行中" value="RUNNING" />
              <el-option label="已暂停" value="PAUSED" />
              <el-option label="已停止" value="STOPPED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="调度名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="cronExpression" label="Cron表达式" width="160">
          <template #default="{ row }">
            <span class="cron-text">{{ row.cronExpression }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="taskCount" label="关联任务" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.taskCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="status-dot" :class="getStatusDotClass(row.status)"></span>
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nextExecutionTime" label="下次执行时间" width="180" show-overflow-tooltip />
        <el-table-column prop="lastExecutionTime" label="上次执行时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleTrigger(row)">手动触发</el-button>
            <el-button v-if="row.status === 'RUNNING'" size="small" type="warning" @click="handlePause(row)">暂停</el-button>
            <el-button v-if="row.status === 'PAUSED'" size="small" type="primary" @click="handleResume(row)">恢复</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false" class="dark-dialog">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="调度名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入调度名称" />
        </el-form-item>
        <el-form-item label="调度描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入调度描述" />
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="0 0 2 * * ?">
            <template #append>
              <el-button @click="showCronSelector = true">选择</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="关联任务">
          <el-select v-model="selectedTaskIds" multiple placeholder="请选择要调度的任务" style="width: 100%">
            <el-option v-for="task in taskList" :key="task.id" :label="task.name" :value="task.id" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">重试策略</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="重试次数">
              <el-input-number v-model="form.retryTimes" :min="0" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重试间隔(秒)">
              <el-input-number v-model="form.retryInterval" :min="10" :max="3600" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时时间(秒)">
              <el-input-number v-model="form.timeout" :min="60" :max="86400" :step="60" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="失败策略">
          <el-radio-group v-model="form.failureStrategy">
            <el-radio value="CONTINUE">继续执行后续任务</el-radio>
            <el-radio value="STOP">停止整个调度</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Cron表达式选择器 -->
    <el-dialog v-model="showCronSelector" title="选择Cron表达式" width="500px" class="dark-dialog">
      <div class="cron-selector">
        <div class="cron-presets">
          <div class="preset-item" v-for="preset in cronPresets" :key="preset.expression" @click="selectCron(preset.expression)">
            <div class="preset-name">{{ preset.name }}</div>
            <div class="preset-expression">{{ preset.expression }}</div>
            <div class="preset-desc">{{ preset.desc }}</div>
          </div>
        </div>
        <el-divider>或自定义</el-divider>
        <el-input v-model="customCron" placeholder="请输入Cron表达式" />
        <el-button type="primary" style="margin-top: 16px;" @click="selectCron(customCron)">使用自定义</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus } from '@element-plus/icons-vue'
import { getTaskPage, schedulerAPI } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新建调度')
const formRef = ref(null)
const showCronSelector = ref(false)
const customCron = ref('')
const taskList = ref([])
const selectedTaskIds = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  status: ''
})

const form = reactive({
  id: null,
  name: '',
  description: '',
  cronExpression: '',
  retryTimes: 3,
  retryInterval: 60,
  timeout: 3600,
  failureStrategy: 'STOP'
})

const rules = {
  name: [{ required: true, message: '请输入调度名称', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }]
}

const cronPresets = [
  { name: '每分钟执行', expression: '0 * * * * ?', desc: '每分钟的第0秒执行' },
  { name: '每小时执行', expression: '0 0 * * * ?', desc: '每小时的第0分0秒执行' },
  { name: '每天凌晨2点', expression: '0 0 2 * * ?', desc: '每天凌晨2点0分0秒执行' },
  { name: '每天凌晨3点', expression: '0 0 3 * * ?', desc: '每天凌晨3点0分0秒执行' },
  { name: '每周一凌晨2点', expression: '0 0 2 ? * MON', desc: '每周一凌晨2点0分0秒执行' },
  { name: '每月1日凌晨2点', expression: '0 0 2 1 * ?', desc: '每月1日凌晨2点0分0秒执行' },
  { name: '每30分钟执行', expression: '0 */30 * * * ?', desc: '每30分钟执行一次' },
]

onMounted(() => {
  fetchData()
  fetchTasks()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await schedulerAPI.getPage(queryParams)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchTasks = async () => {
  try {
    const res = await getTaskPage({ pageNum: 1, pageSize: 100 })
    taskList.value = res.data?.list || []
  } catch (e) {
    console.error(e)
  }
}

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = ''
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新建调度'
  Object.assign(form, {
    id: null,
    name: '',
    description: '',
    cronExpression: '',
    retryTimes: 3,
    retryInterval: 60,
    timeout: 3600,
    failureStrategy: 'STOP'
  })
  selectedTaskIds.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑调度'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    cronExpression: row.cronExpression,
    retryTimes: row.retryTimes || 3,
    retryInterval: row.retryInterval || 60,
    timeout: row.timeout || 3600,
    failureStrategy: row.failureStrategy || 'STOP'
  })
  selectedTaskIds.value = row.taskIds || []
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该调度？', '提示', { type: 'warning' })
    await schedulerAPI.deleteTask(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

const handleTrigger = async (row) => {
  try {
    await ElMessageBox.confirm('确定手动触发该调度？', '提示', { type: 'info' })
    await schedulerAPI.triggerTask(row.id)
    ElMessage.success('已触发')
  } catch (e) {}
}

const handlePause = async (row) => {
  try {
    await schedulerAPI.pauseTask(row.id)
    row.status = 'PAUSED'
    ElMessage.success('已暂停')
  } catch (e) {}
}

const handleResume = async (row) => {
  try {
    await schedulerAPI.resumeTask(row.id)
    row.status = 'RUNNING'
    ElMessage.success('已恢复')
  } catch (e) {}
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (form.id) {
      await schedulerAPI.updateTask(form.id, { ...form, taskIds: selectedTaskIds.value })
    } else {
      await schedulerAPI.createTask({ ...form, taskIds: selectedTaskIds.value })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}

const selectCron = (expression) => {
  form.cronExpression = expression
  showCronSelector.value = false
}

const getStatusType = (status) => {
  const types = { 'RUNNING': 'success', 'PAUSED': 'warning', 'STOPPED': 'danger' }
  return types[status] || 'info'
}

const getStatusDotClass = (status) => {
  const classes = { 'RUNNING': 'running', 'PAUSED': 'warning', 'STOPPED': 'stopped' }
  return classes[status] || 'pending'
}

const getStatusText = (status) => {
  const texts = { 'RUNNING': '运行中', 'PAUSED': '已暂停', 'STOPPED': '已停止' }
  return texts[status] || status
}
</script>

<style lang="scss" scoped>
.scheduler-page {
  .main-card {
    border-radius: 16px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 600;
      color: var(--text-primary);

      .title-icon {
        color: var(--primary-light);
        font-size: 20px;
      }
    }
  }

  .search-bar {
    padding: 20px;
    margin-bottom: 20px;
    background: rgba(79, 110, 247, 0.05);
    border-radius: 12px;
    border: 1px solid var(--border-color);
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }
  }

  .cron-text {
    font-family: 'Monaco', 'Consolas', monospace;
    background: rgba(79, 110, 247, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
    color: var(--primary-color);
    font-size: 13px;
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.running { background: #52c41a; box-shadow: 0 0 6px rgba(82, 196, 26, 0.5); }
      &.warning { background: #faad14; animation: breathe 2s infinite; }
      &.stopped { background: #ff4d4f; }
      &.pending { background: var(--text-muted); }
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .cron-selector {
    .cron-presets {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      margin-bottom: 16px;

      .preset-item {
        padding: 12px;
        background: rgba(79, 110, 247, 0.05);
        border: 1px solid var(--border-color);
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: var(--primary-color);
          background: rgba(79, 110, 247, 0.1);
        }

        .preset-name {
          font-weight: 600;
          color: var(--text-primary);
          margin-bottom: 4px;
        }

        .preset-expression {
          font-family: 'Monaco', 'Consolas', monospace;
          font-size: 12px;
          color: var(--primary-color);
          margin-bottom: 4px;
        }

        .preset-desc {
          font-size: 12px;
          color: var(--text-muted);
        }
      }
    }
  }
}

@keyframes breathe {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
