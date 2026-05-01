<template>
  <div class="cdc-config-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.name" placeholder="请输入连接器名称" clearable style="width: 240px; margin-right: 16px;">
        <template #append>
          <el-button @click="fetchData">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        <span>新增CDC配置</span>
      </el-button>
    </div>

    <!-- 卡片式布局 -->
    <div class="cdc-cards">
      <el-card
        v-for="item in tableData"
        :key="item.id"
        class="cdc-card"
        :class="{ 'running': item.syncStatus === 'RUNNING' }"
      >
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <el-tag :type="item.connectorType === 'mysql' ? 'primary' : 'success'" size="small">
                {{ item.connectorType?.toUpperCase() }}
              </el-tag>
              <span>{{ item.name }}</span>
            </div>
            <div class="card-status">
              <span class="status-dot" :class="item.syncStatus === 'RUNNING' ? 'running' : item.syncStatus === 'ERROR' ? 'error' : 'stopped'"></span>
              <span>{{ getStatusLabel(item.syncStatus) }}</span>
            </div>
          </div>
        </template>
        <div class="card-content">
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">延迟时间:</span>
              <span class="info-value" :class="item.delay > 300000 ? 'warning' : 'normal'">
                {{ formatDelay(item.delay) }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">吞吐量:</span>
              <span class="info-value">{{ formatThroughput(item.throughput) }}</span>
            </div>
          </div>
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">服务器:</span>
              <span class="info-value">{{ item.serverName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">连接器:</span>
              <span class="info-value">{{ item.connectorName }}</span>
            </div>
          </div>
          <!-- 实时延迟图表 -->
          <div class="delay-chart">
            <div ref="getChartRef(item.id)" class="chart-container"></div>
          </div>
          <!-- 断点续传信息 -->
          <div class="offset-info">
            <span class="info-label">消费位点:</span>
            <span class="info-value">{{ item.currentOffset || '-' }}</span>
          </div>
        </div>
        <template #footer>
          <div class="card-actions">
            <el-button
              v-if="item.syncStatus !== 'DEPLOYED' && item.syncStatus !== 'RUNNING'"
              size="small"
              type="success"
              @click="handleDeploy(item)"
              :loading="deployLoading[item.id]"
            >
              <el-icon><Upload /></el-icon>
              部署
            </el-button>
            <el-button
              v-if="item.syncStatus === 'DEPLOYED'"
              size="small"
              type="primary"
              @click="handleStart(item)"
              :loading="startLoading[item.id]"
            >
              <el-icon><VideoPlay /></el-icon>
              启动
            </el-button>
            <el-button
              v-if="item.syncStatus === 'RUNNING'"
              size="small"
              type="warning"
              @click="handleStop(item)"
              :loading="stopLoading[item.id]"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(item)"
              :loading="deleteLoading[item.id]"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </template>
      </el-card>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form :model="form" label-width="130px" :rules="rules" ref="formRef">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="配置名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入配置名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联数据源" prop="datasourceId">
              <el-select v-model="form.datasourceId" placeholder="请选择数据源" style="width: 100%" @change="handleDatasourceChange">
                <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="连接器类型" prop="connectorType">
              <el-select v-model="form.connectorType" placeholder="请选择" style="width: 100%">
                <el-option label="MySQL" value="mysql" />
                <el-option label="PostgreSQL" value="postgresql" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连接器名称" prop="connectorName">
              <el-input v-model="form.connectorName" placeholder="全局唯一，如: etl-connector-mysql-1" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="服务器名称">
              <el-input v-model="form.serverName" placeholder="Kafka Topic前缀，如: etl-mysql" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Kafka Topic前缀">
              <el-input v-model="form.kafkaTopicPrefix" placeholder="为空则使用服务器名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">同步过滤配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="表过滤配置">
              <el-input v-model="form.filterRegex" placeholder="如: db1.table1,db2.table2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="黑名单过滤">
              <el-input v-model="form.filterBlackRegex" placeholder="排除的表" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cdcAPI, datasourceAPI } from '@/api'

const loading = ref(false)
const deployLoading = ref({})
const startLoading = ref({})
const stopLoading = ref({})
const deleteLoading = ref({})
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增CDC配置')
const formRef = ref(null)
const datasourceList = ref([])
const chartRefs = ref({})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: ''
})

const form = reactive({
  id: null,
  name: '',
  datasourceId: null,
  connectorName: '',
  connectorType: 'mysql',
  serverName: '',
  databaseHost: '',
  databasePort: null,
  dbUsername: '',
  dbPassword: '',
  filterRegex: '',
  filterBlackRegex: '',
  kafkaTopicPrefix: '',
  extraConfig: '',
  status: 1,
  statusBool: true
})

const rules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  connectorType: [{ required: true, message: '请选择连接器类型', trigger: 'change' }],
  connectorName: [{ required: true, message: '请输入连接器名称', trigger: 'blur' }]
}

onMounted(async () => {
  await fetchData()
  await fetchDatasources()
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await cdcAPI.getPage(queryParams)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
    // 模拟实时数据
    tableData.value = tableData.value.map(item => ({
      ...item,
      delay: item.syncStatus === 'RUNNING' ? Math.floor(Math.random() * 600000) : 0,
      throughput: item.syncStatus === 'RUNNING' ? Math.floor(Math.random() * 10000) : 0,
      currentOffset: '0:123456'
    }))
    await nextTick()
    initCharts()
  } catch (error) {
    console.error('获取CDC配置失败:', error)
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

const initCharts = () => {
  tableData.value.forEach(item => {
    const chartId = item.id
    const chartDom = chartRefs.value[chartId]
    if (chartDom) {
      const chart = echarts.init(chartDom)
      const data = generateRandomData()
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e4e7ed',
          textStyle: { color: '#303133' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['1m', '2m', '3m', '4m', '5m'],
          axisLine: { show: false },
          axisLabel: { show: false },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          axisLine: { show: false },
          axisLabel: { show: false },
          splitLine: { show: false }
        },
        series: [{
          name: '延迟',
          type: 'line',
          data: data,
          smooth: true,
          itemStyle: { color: '#4f6ef7' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(79, 110, 247, 0.3)' },
              { offset: 1, color: 'rgba(79, 110, 247, 0.05)' }
            ])
          },
          lineStyle: { width: 2 }
        }]
      })
    }
  })
}

const generateRandomData = () => {
  return Array.from({ length: 5 }, () => Math.floor(Math.random() * 600000))
}

const getChartRef = (id) => {
  return el => {
    chartRefs.value[id] = el
  }
}

const getStatusLabel = (status) => {
  const labels = { STOPPED: '停止', RUNNING: '运行中', ERROR: '错误', PAUSED: '已暂停', DEPLOYED: '已部署', CREATED: '已创建' }
  return labels[status] || status
}

const formatDelay = (delay) => {
  if (!delay || delay < 1000) {
    return '0s'
  } else if (delay < 60000) {
    return `${Math.floor(delay / 1000)}s`
  } else if (delay < 3600000) {
    const minutes = Math.floor(delay / 60000)
    const seconds = Math.floor((delay % 60000) / 1000)
    return `${minutes}m${seconds > 0 ? `${seconds}s` : ''}`
  } else {
    const hours = Math.floor(delay / 3600000)
    const minutes = Math.floor((delay % 3600000) / 60000)
    return `${hours}h${minutes > 0 ? `${minutes}m` : ''}`
  }
}

const formatThroughput = (throughput) => {
  if (!throughput) {
    return '0 events/s'
  } else if (throughput < 1000) {
    return `${throughput} events/s`
  } else {
    return `${(throughput / 1000).toFixed(1)}K events/s`
  }
}

const handleDatasourceChange = (dsId) => {
  const ds = datasourceList.value.find(d => d.id === dsId)
  if (ds) {
    form.connectorName = `etl-connector-${ds.type?.toLowerCase() || 'mysql'}-${dsId}`
    form.connectorType = ds.type?.toLowerCase() === 'postgresql' ? 'postgresql' : 'mysql'
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增CDC配置'
  Object.assign(form, {
    id: null,
    name: '',
    datasourceId: null,
    connectorName: '',
    connectorType: 'mysql',
    serverName: '',
    databaseHost: '',
    databasePort: null,
    dbUsername: '',
    dbPassword: '',
    filterRegex: '',
    filterBlackRegex: '',
    kafkaTopicPrefix: '',
    extraConfig: '',
    status: 1,
    statusBool: true
  })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  dialogTitle.value = '编辑CDC配置'
  try {
    const res = await cdcAPI.get(row.id)
    Object.assign(form, res.data, { statusBool: res.data.status === 1 })
    dialogVisible.value = true
  } catch (error) {
    console.error('获取配置失败:', error)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该CDC配置？', '提示', { type: 'warning' })
    deleteLoading.value[row.id] = true
    await cdcAPI.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  } finally {
    deleteLoading.value[row.id] = false
  }
}

const handleDeploy = async (row) => {
  try {
    deployLoading.value[row.id] = true
    const res = await cdcAPI.deploy(row.id)
    if (res.data) {
      ElMessage.success('部署成功')
      fetchData()
    } else {
      ElMessage.error('部署失败')
    }
  } catch (error) {
    console.error('部署失败:', error)
    ElMessage.error('部署失败')
  } finally {
    deployLoading.value[row.id] = false
  }
}

const handleStart = async (row) => {
  try {
    startLoading.value[row.id] = true
    const res = await cdcAPI.start(row.id)
    if (res.data) {
      ElMessage.success('启动成功')
      fetchData()
    } else {
      ElMessage.error('启动失败')
    }
  } catch (error) {
    console.error('启动失败:', error)
    ElMessage.error('启动失败')
  } finally {
    startLoading.value[row.id] = false
  }
}

const handleStop = async (row) => {
  try {
    stopLoading.value[row.id] = true
    const res = await cdcAPI.stop(row.id)
    if (res.data) {
      ElMessage.success('停止成功')
      fetchData()
    } else {
      ElMessage.error('停止失败')
    }
  } catch (error) {
    console.error('停止失败:', error)
    ElMessage.error('停止失败')
  } finally {
    stopLoading.value[row.id] = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const submitData = { ...form, status: form.statusBool ? 1 : 0 }
    delete submitData.statusBool

    if (form.id) {
      await cdcAPI.update(form.id, submitData)
    } else {
      await cdcAPI.create(submitData)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}
</script>

<style lang="scss" scoped>
.cdc-config-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100vh;

  .search-bar {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
  }

  .cdc-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
    gap: 20px;
    margin-bottom: 20px;
  }

  .cdc-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    &.running {
      border: 1px solid rgba(79, 110, 247, 0.3);
    }

    :deep(.el-card__header) {
      padding: 16px 20px;
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    :deep(.el-card__footer) {
      padding: 16px 20px;
      border-top: 1px solid #f0f2f5;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .card-status {
      display: flex;
      align-items: center;
      gap: 8px;

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;

        &.running {
          background: #52c41a;
          box-shadow: 0 0 6px rgba(82, 196, 26, 0.4);
          animation: breathe 2s infinite;
        }

        &.error {
          background: #ff4d4f;
          animation: blink 1s infinite;
        }

        &.stopped {
          background: #dcdfe6;
        }
      }
    }
  }

  .card-content {
    .info-row {
      display: flex;
      margin-bottom: 12px;

      .info-item {
        flex: 1;
        display: flex;
        justify-content: space-between;

        .info-label {
          font-size: 13px;
          color: #909399;
        }

        .info-value {
          font-size: 13px;
          color: #303133;
          font-weight: 500;

          &.warning {
            color: #faad14;
          }

          &.normal {
            color: #52c41a;
          }
        }
      }
    }

    .delay-chart {
      margin: 16px 0;

      .chart-container {
        height: 60px;
      }
    }

    .offset-info {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-top: 1px solid #f0f2f5;

      .info-label {
        font-size: 12px;
        color: #909399;
      }

      .info-value {
        font-size: 12px;
        color: #606266;
        font-family: monospace;
      }
    }
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .el-button {
      font-size: 12px;
    }
  }

  .pagination-wrap {
    display: flex;
    justify-content: flex-end;
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
  .cdc-config-page {
    padding: 16px;

    .search-bar {
      flex-direction: column;
      align-items: stretch;

      .el-input {
        margin-right: 0;
        margin-bottom: 12px;
      }
    }

    .cdc-cards {
      grid-template-columns: 1fr;
    }
  }
}
</style>
