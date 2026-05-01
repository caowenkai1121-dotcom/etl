<template>
  <div class="api-service-page">
    <!-- 统计概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="api-stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #4f6ef7;"><el-icon><Promotion /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.totalApis }}</div>
            <div class="stat-label">API总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="api-stat-card">
          <div class="stat-icon" style="background: #f0f9eb; color: #52c41a;"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.onlineCount }}</div>
            <div class="stat-label">已上线</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="api-stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #faad14;"><el-icon><TrendCharts /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ formatCallCount(stats.todayCalls) }}</div>
            <div class="stat-label">今日调用</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="api-stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #ff4d4f;"><el-icon><Timer /></el-icon></div>
          <div class="stat-body">
            <div class="stat-value">{{ stats.avgResponseTime }}ms</div>
            <div class="stat-label">平均响应</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 调用趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <div class="card-header">
          <span class="card-title"><el-icon><DataLine /></el-icon> API调用趋势 (近7天)</span>
          <el-radio-group v-model="chartRange" size="small">
            <el-radio-button value="7">7天</el-radio-button>
            <el-radio-button value="14">14天</el-radio-button>
            <el-radio-button value="30">30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="callTrendChartRef" class="trend-chart"></div>
    </el-card>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="left">
        <el-input v-model="searchName" placeholder="搜索API名称或路径" clearable style="width: 240px">
          <template #append>
            <el-button @click="fetchData"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 130px; margin-left: 12px" @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="已上线" value="ONLINE" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
        <el-select v-model="filterMethod" placeholder="请求方法" clearable style="width: 110px; margin-left: 12px" @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
        </el-select>
      </div>
      <div class="right">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建API服务
        </el-button>
      </div>
    </div>

    <!-- API列表表格 -->
    <el-card class="data-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="API名称" min-width="180">
          <template #default="{ row }">
            <div class="api-name-cell">
              <el-icon><Promotion /></el-icon>
              <el-button text type="primary" @click="handleEdit(row)">{{ row.name }}</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="路径" min-width="220">
          <template #default="{ row }">
            <div class="path-cell">
              <el-tag size="small" :type="row.method === 'GET' ? 'success' : 'warning'" effect="light">
                {{ row.method }}
              </el-tag>
              <code class="api-path">/data-api{{ row.path }}</code>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="datasourceName" label="数据源" width="130" show-overflow-tooltip />
        <el-table-column prop="authType" label="认证方式" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="light">{{ getAuthTypeText(row.authType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ONLINE' ? '已上线' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="今日调用" width="110">
          <template #default="{ row }">
            <span class="call-count">{{ formatCallCount(row.todayCalls || 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="平均响应" width="110">
          <template #default="{ row }">
            <span :class="{ 'slow-response': (row.avgResponseTime || 0) > 500 }">
              {{ row.avgResponseTime || 0 }} ms
            </span>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="90">
          <template #default="{ row }">
            <span :class="{ 'error-rate': (row.successRate || 100) < 95 }">
              {{ row.successRate || 100 }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button
              :type="row.status === 'ONLINE' ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              <el-icon><component :is="row.status === 'ONLINE' ? 'SwitchButton' : 'VideoPlay'" /></el-icon>
              {{ row.status === 'ONLINE' ? '下线' : '上线' }}
            </el-button>
            <el-button size="small" @click="handleTest(row)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
        class="pagination"
      />
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入API名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属文件夹">
              <el-select v-model="form.folderId" placeholder="选择文件夹" style="width: 100%">
                <el-option label="标准接口" value="1" />
                <el-option label="内部接口" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">API配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="请求路径" prop="path">
              <el-input v-model="form.path" placeholder="如 /user/list">
                <template #prepend>/data-api</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="请求方法">
              <el-select v-model="form.method" style="width: 100%">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联数据源" prop="datasourceId">
              <el-select v-model="form.datasourceId" placeholder="选择数据源" style="width: 100%">
                <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">SQL配置</el-divider>
        <el-form-item label="SQL模板" prop="sqlTemplate">
          <el-input v-model="form.sqlTemplate" type="textarea" :rows="6" placeholder="SELECT * FROM table WHERE id = ${id}" />
          <div class="sql-tip">
            支持参数占位符: ${参数名}，如 ${id}、${name}、${status}
          </div>
        </el-form-item>

        <el-divider content-position="left">安全配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="认证方式">
              <el-select v-model="form.authType" style="width: 100%">
                <el-option label="Token认证" value="TOKEN" />
                <el-option label="IP白名单" value="IP" />
                <el-option label="签名验证" value="SIGN" />
                <el-option label="无认证" value="NONE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="限流(次/分)">
              <el-input-number v-model="form.rateLimit" :min="1" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="超时(秒)">
              <el-input-number v-model="form.timeout" :min="1" :max="60" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="API描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="API功能描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="API测试" width="600px">
      <div class="test-section">
        <div class="test-info">
          <span>请求路径:</span>
          <el-tag type="success" effect="light">{{ testApi.method }}</el-tag>
          <code>/data-api{{ testApi.path }}</code>
        </div>
        <div class="test-params">
          <span>请求参数 (JSON):</span>
          <el-input v-model="testParams" type="textarea" :rows="4" placeholder='{"id": 1}' />
        </div>
        <el-button type="primary" @click="executeTest" :loading="testLoading" style="margin-bottom: 16px;">
          <el-icon><VideoPlay /></el-icon>
          执行测试
        </el-button>
        <div v-if="testResult" class="test-result">
          <div class="result-header">
            <span>返回结果:</span>
            <el-tag v-if="testSuccess" type="success" size="small">200 OK</el-tag>
            <el-tag v-else type="danger" size="small">Error</el-tag>
          </div>
          <pre>{{ JSON.stringify(testResult, null, 2) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { apiServiceAPI, getDatasourceList } from '@/api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const searchName = ref('')
const filterStatus = ref('')
const filterMethod = ref('')
const datasourceList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建API服务')
const submitLoading = ref(false)
const formRef = ref(null)
const chartRange = ref('7')

const callTrendChartRef = ref(null)
let callTrendChart = null

const stats = reactive({
  totalApis: 0,
  onlineCount: 0,
  todayCalls: 0,
  avgResponseTime: 0
})

const updateStats = () => {
  stats.totalApis = total.value
  stats.onlineCount = tableData.value.filter(a => a.status === 'ONLINE').length
  stats.todayCalls = tableData.value.reduce((sum, a) => sum + (a.todayCalls || 0), 0)
  if (tableData.value.length > 0) {
    stats.avgResponseTime = Math.round(tableData.value.reduce((sum, a) => sum + (a.avgResponseTime || 0), 0) / tableData.value.length)
  }
}

const queryParams = reactive({ pageNum: 1, pageSize: 10 })

const form = reactive({
  id: null, name: '', path: '', method: 'GET', datasourceId: null,
  sqlTemplate: '', paramsConfig: '', authType: 'TOKEN',
  rateLimit: 100, timeout: 30, description: '', folderId: null
})

const rules = {
  name: [{ required: true, message: '请输入API名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入请求路径', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  sqlTemplate: [{ required: true, message: '请输入SQL模板', trigger: 'blur' }]
}

const testDialogVisible = ref(false)
const testApi = ref({})
const testParams = ref('')
const testResult = ref(null)
const testSuccess = ref(false)
const testLoading = ref(false)

onMounted(() => {
  fetchData()
  fetchDatasources()
  nextTick(() => initCallTrendChart())
})

onUnmounted(() => {
  callTrendChart?.dispose()
})

const initCallTrendChart = () => {
  if (!callTrendChartRef.value) return
  callTrendChart = echarts.init(callTrendChartRef.value)

  const dates = ['12/14', '12/15', '12/16', '12/17', '12/18', '12/19', '12/20']
  callTrendChart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#e4e7ed', textStyle: { color: '#303133' } },
    legend: { data: ['调用次数', '成功率'], top: 0, textStyle: { color: '#606266' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e4e7ed' } }, axisLabel: { color: '#909399' } },
    yAxis: [
      { type: 'value', name: '次数', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f2f5' } }, axisLabel: { color: '#909399' } },
      { type: 'value', name: '成功率', min: 90, max: 100, axisLine: { show: false }, axisLabel: { color: '#909399', formatter: '{value}%' } }
    ],
    series: [
      { name: '调用次数', type: 'bar', data: [12500, 13800, 14200, 15600, 16800, 15200, 18300], itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#4f6ef7' }, { offset: 1, color: '#a8b8f7' }]), borderRadius: [4, 4, 0, 0] } },
      { name: '成功率', type: 'line', yAxisIndex: 1, data: [99.2, 99.5, 98.8, 99.1, 99.6, 99.3, 99.7], smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { color: '#52c41a', width: 2 }, itemStyle: { color: '#52c41a' } }
    ]
  })
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await apiServiceAPI.getPage({
      ...queryParams,
      name: searchName.value,
      status: filterStatus.value,
      method: filterMethod.value
    })
    tableData.value = (res.data?.list || []).map(d => ({
      ...d,
      todayCalls: d.todayCalls ?? 0,
      avgResponseTime: d.avgResponseTime ?? 0,
      successRate: d.successRate ?? 100
    }))
    total.value = res.data?.total || tableData.value.length
    updateStats()
  } catch (e) {
    console.error('获取API服务列表失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchDatasources = async () => {
  try {
    const res = await getDatasourceList()
    datasourceList.value = res.data || []
  } catch (e) {
    console.error('获取数据源列表失败:', e)
  }
}

const handleCreate = () => {
  dialogTitle.value = '新建API服务'
  Object.assign(form, {
    id: null, name: '', path: '', method: 'GET', datasourceId: null,
    sqlTemplate: '', paramsConfig: '', authType: 'TOKEN',
    rateLimit: 100, timeout: 30, description: '', folderId: null
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑API服务'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  const action = row.status === 'ONLINE' ? '下线' : '上线'
  try {
    await ElMessageBox.confirm(`确定${action}该API服务？`, '提示', { type: 'warning' })
    if (row.status === 'ONLINE') {
      await apiServiceAPI.offline(row.id)
    } else {
      await apiServiceAPI.online(row.id)
    }
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

const handleTest = (row) => {
  testApi.value = row
  testParams.value = ''
  testResult.value = null
  testDialogVisible.value = true
}

const executeTest = async () => {
  testLoading.value = true
  try {
    const params = testParams.value ? JSON.parse(testParams.value) : {}
    const res = await apiServiceAPI.test(testApi.value.id, params)
    testResult.value = res.data
    testSuccess.value = true
    ElMessage.success('测试成功')
  } catch (e) {
    ElMessage.error('测试失败')
    testResult.value = { error: '请求失败', message: e.message || '未知错误' }
    testSuccess.value = false
  } finally {
    testLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该API服务？', '提示', { type: 'warning' })
    await apiServiceAPI.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    if (form.id) {
      await apiServiceAPI.update(form.id, form)
    } else {
      await apiServiceAPI.create(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    if (e !== false) ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const formatCallCount = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const getAuthTypeText = (type) => {
  const map = { TOKEN: 'Token', IP: 'IP白名单', SIGN: '签名', NONE: '无' }
  return map[type] || type
}
</script>

<style lang="scss" scoped>
.api-service-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;

  .stats-row { margin-bottom: 20px; }

  .api-stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #fff;
    border-radius: 12px;
    padding: 18px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    margin-bottom: 16px;

    .stat-icon {
      width: 44px; height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .stat-body {
      .stat-value { font-size: 22px; font-weight: 700; color: #303133; }
      .stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
    }
  }

  .chart-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .trend-chart { height: 260px; }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    flex-wrap: wrap;
    gap: 12px;
  }

  .data-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .pagination { margin-top: 16px; justify-content: flex-end; }
  }

  .api-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .path-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .api-path {
      background: #f5f7fa;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      color: #606266;
      font-family: monospace;
    }
  }

  .call-count { font-weight: 600; color: #4f6ef7; }
  .slow-response { color: #ff4d4f; font-weight: 500; }
  .error-rate { color: #ff4d4f; font-weight: 500; }

  .sql-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }

  .test-section {
    .test-info {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;

      code {
        background: #f5f7fa;
        padding: 4px 10px;
        border-radius: 4px;
        font-family: monospace;
      }
    }

    .test-params {
      margin-bottom: 16px;
      span { display: block; margin-bottom: 8px; color: #606266; }
    }

    .test-result {
      .result-header {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 8px;
        span { color: #606266; }
      }
      pre {
        background: #f5f7fa;
        padding: 16px;
        border-radius: 8px;
        overflow-x: auto;
        max-height: 300px;
        font-size: 13px;
      }
    }
  }
}

@media (max-width: 768px) {
  .api-service-page { padding: 16px; }
}
</style>
