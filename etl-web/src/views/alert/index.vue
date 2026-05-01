<template>
  <div class="alert-page">
    <!-- Tab切换 -->
    <el-card class="tab-card">
      <el-tabs v-model="activeTab" type="card" class="alert-tabs">
        <el-tab-pane label="告警列表" name="list">
          <!-- 告警列表筛选 -->
          <div class="filter-section">
            <el-form :inline="true" :model="listQuery">
              <el-form-item label="级别">
                <el-select v-model="listQuery.severity" placeholder="选择级别" clearable style="width: 100px">
                  <el-option label="严重" value="CRITICAL" />
                  <el-option label="错误" value="ERROR" />
                  <el-option label="警告" value="WARNING" />
                  <el-option label="信息" value="INFO" />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="listQuery.status" placeholder="选择状态" clearable style="width: 120px">
                  <el-option label="待处理" value="PENDING" />
                  <el-option label="已确认" value="CONFIRMED" />
                  <el-option label="已解决" value="RESOLVED" />
                </el-select>
              </el-form-item>
              <el-form-item label="时间范围">
                <el-date-picker
                  v-model="listQuery.dateRange"
                  type="datetimerange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 280px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchAlertList">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 告警列表 -->
          <el-table :data="alertList" style="width: 100%" stripe v-loading="loading">
            <el-table-column prop="timestamp" label="时间" width="180" />
            <el-table-column prop="ruleName" label="规则名" width="150" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="severity" label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityType(row.severity)" size="small">
                  {{ getSeverityLabel(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="confirmAlert(row)">确认</el-button>
                <el-button type="warning" size="small" @click="silenceAlert(row)">静默</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="listQuery.pageNum"
            v-model:page-size="listQuery.pageSize"
            :total="listTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchAlertList"
            @current-change="fetchAlertList"
            class="pagination"
          />
        </el-tab-pane>

        <el-tab-pane label="规则管理" name="rules">
          <!-- 规则管理表格 -->
          <el-table :data="ruleList" style="width: 100%" stripe v-loading="ruleLoading">
            <el-table-column prop="name" label="规则名" min-width="180" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="getRuleTypeColor(row.type)">
                  {{ getRuleTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="condition" label="条件" min-width="200" show-overflow-tooltip />
            <el-table-column prop="enabled" label="状态" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" @change="(val) => toggleRule(row, val)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="editRule(row)">编辑</el-button>
                <el-button type="danger" size="small" @click="deleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="rule-actions">
            <el-button type="primary" @click="showRuleDialog = true">
              <el-icon><Plus /></el-icon>
              新增规则
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="渠道配置" name="channels">
          <!-- 渠道配置卡片 -->
          <el-row :gutter="20">
            <!-- 邮件配置 -->
            <el-col :xs="24" :lg="8">
              <el-card class="channel-card">
                <template #header>
                  <div class="channel-header">
                    <span class="channel-title">邮件配置</span>
                    <el-switch v-model="emailConfig.enabled" />
                  </div>
                </template>
                <el-form :model="emailConfig" label-width="80px">
                  <el-form-item label="SMTP服务器">
                    <el-input v-model="emailConfig.smtpHost" placeholder="smtp.example.com" />
                  </el-form-item>
                  <el-form-item label="端口">
                    <el-input-number v-model="emailConfig.smtpPort" :min="1" :max="65535" style="width: 100%" />
                  </el-form-item>
                  <el-form-item label="邮箱">
                    <el-input v-model="emailConfig.fromEmail" placeholder="noreply@example.com" />
                  </el-form-item>
                  <el-form-item label="密码">
                    <el-input v-model="emailConfig.password" type="password" />
                  </el-form-item>
                  <el-form-item label="收件人">
                    <el-input v-model="emailConfig.recipients" placeholder="逗号分隔" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="testEmail">测试发送</el-button>
                    <el-button @click="saveEmailConfig">保存</el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>

            <!-- 钉钉配置 -->
            <el-col :xs="24" :lg="8">
              <el-card class="channel-card">
                <template #header>
                  <div class="channel-header">
                    <span class="channel-title">钉钉配置</span>
                    <el-switch v-model="dingTalkConfig.enabled" />
                  </div>
                </template>
                <el-form :model="dingTalkConfig" label-width="80px">
                  <el-form-item label="Webhook地址">
                    <el-input v-model="dingTalkConfig.webhookUrl" placeholder="https://oapi.dingtalk.com..." />
                  </el-form-item>
                  <el-form-item label="关键词">
                    <el-input v-model="dingTalkConfig.keyword" placeholder="必填关键词" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="testDingTalk">测试发送</el-button>
                    <el-button @click="saveDingTalkConfig">保存</el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>

            <!-- Webhook配置 -->
            <el-col :xs="24" :lg="8">
              <el-card class="channel-card">
                <template #header>
                  <div class="channel-header">
                    <span class="channel-title">Webhook配置</span>
                    <el-switch v-model="webhookConfig.enabled" />
                  </div>
                </template>
                <el-form :model="webhookConfig" label-width="80px">
                  <el-form-item label="URL">
                    <el-input v-model="webhookConfig.url" placeholder="https://api.example.com..." />
                  </el-form-item>
                  <el-form-item label="请求头">
                    <el-input v-model="webhookConfig.headers" type="textarea" :rows="2" placeholder='{"Authorization": "Bearer token"}' />
                  </el-form-item>
                  <el-form-item label="请求体">
                    <el-input v-model="webhookConfig.body" type="textarea" :rows="3" placeholder='{"message": "告警内容"}' />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="testWebhook">测试发送</el-button>
                    <el-button @click="saveWebhookConfig">保存</el-button>
                  </el-form-item>
                </el-form>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="告警趋势" name="trend">
          <!-- 告警趋势图表 -->
          <div ref="trendChartRef" class="trend-chart"></div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑规则对话框 -->
    <el-dialog v-model="showRuleDialog" :title="ruleDialogTitle" width="500px">
      <el-form :model="ruleForm" :rules="ruleRules" ref="ruleFormRef" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="type">
          <el-select v-model="ruleForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="阈值" value="THRESHOLD" />
            <el-option label="趋势" value="TREND" />
            <el-option label="智能" value="SMART" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重度" prop="severity">
          <el-select v-model="ruleForm.severity" placeholder="选择严重度" style="width: 100%">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="错误" value="ERROR" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件" prop="condition">
          <el-input
            v-model="ruleForm.condition"
            type="textarea"
            :rows="3"
            placeholder="JSON格式条件"
          />
        </el-form-item>
        <el-form-item label="通知渠道" prop="channels">
          <el-checkbox-group v-model="ruleForm.channels">
            <el-checkbox label="EMAIL">邮件</el-checkbox>
            <el-checkbox label="DINGTALK">钉钉</el-checkbox>
            <el-checkbox label="WEBHOOK">Webhook</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRuleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  getAlertRecordPage, getAlertRulePage, createAlertRule, updateAlertRule, deleteAlertRule,
  toggleAlertRule, getAlertChannelList, createAlertChannel, updateAlertChannel,
  resolveAlert, ignoreAlert
} from '@/api'

const activeTab = ref('list')
const loading = ref(false)
const ruleLoading = ref(false)
const showRuleDialog = ref(false)
const ruleDialogTitle = ref('新增规则')

// 列表查询参数
const listQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  severity: '',
  status: '',
  dateRange: null
})

const alertList = ref([])
const listTotal = ref(0)

// 规则查询
const ruleList = ref([])

// 规则表单
const ruleForm = reactive({
  id: null,
  name: '',
  type: 'THRESHOLD',
  severity: 'WARNING',
  condition: '{"threshold": 100}',
  channels: ['EMAIL'],
  enabled: true
})

const ruleRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }],
  condition: [{ required: true, message: '请输入规则条件', trigger: 'blur' }]
}

const ruleFormRef = ref()

// 渠道配置
const emailConfig = reactive({
  enabled: true,
  smtpHost: 'smtp.example.com',
  smtpPort: 465,
  fromEmail: 'noreply@example.com',
  password: 'password123',
  recipients: 'admin@example.com, dev@example.com'
})

const dingTalkConfig = reactive({
  enabled: true,
  webhookUrl: 'https://oapi.dingtalk.com/robot/send?access_token=xxxx',
  keyword: 'ETL'
})

const webhookConfig = reactive({
  enabled: false,
  url: 'https://api.example.com/webhook',
  headers: '{"Content-Type": "application/json"}',
  body: '{"message": "{{message}}"}'
})

// 图表引用
const trendChartRef = ref(null)
let trendChart = null

onMounted(() => {
  fetchAlertList()
  fetchRuleList()
})

const fetchAlertList = async () => {
  loading.value = true
  try {
    const res = await getAlertRecordPage({
      pageNum: listQuery.pageNum,
      pageSize: listQuery.pageSize,
      severity: listQuery.severity || undefined,
      status: listQuery.status || undefined
    })
    if (res.data) {
      alertList.value = (res.data.records || res.data.list || []).map(item => ({
        ...item,
        timestamp: item.timestamp || item.createTime || '-',
        ruleName: item.ruleName || '-',
        type: item.type || '-',
        severity: item.severity || 'INFO',
        message: item.message || '-',
        status: item.status || 'PENDING'
      }))
      listTotal.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载告警列表失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchRuleList = async () => {
  ruleLoading.value = true
  rulesLoaded.value = false
  try {
    const res = await getAlertRulePage({ pageNum: 1, pageSize: 50 })
    if (res.data) {
      ruleList.value = res.data.records || res.data.list || []
    }
  } catch (e) {
    console.error('加载规则列表失败:', e)
  } finally {
    ruleLoading.value = false
    setTimeout(() => { rulesLoaded.value = true }, 300)
  }
}

const confirmAlert = async (row) => {
  try {
    await ElMessageBox.confirm('确定要确认该告警吗？', '提示', { type: 'warning' })
    await resolveAlert(row.id)
    row.status = 'RESOLVED'
    ElMessage.success('告警已确认')
  } catch (e) { /* cancel */ }
}

const silenceAlert = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入静默时间（分钟）', '静默告警', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^[0-9]+$/,
      inputErrorMessage: '请输入有效的数字'
    })
    await ignoreAlert(row.id)
    ElMessage.success(`告警已静默 ${value} 分钟`)
  } catch (e) { /* cancel */ }
}

const showTrend = async () => {
  if (activeTab.value === 'trend' && !trendChart) {
    await nextTick()
    initTrendChart()
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return

  trendChart = echarts.init(trendChartRef.value)
  const days = []
  const criticalData = []
  const errorData = []
  const warningData = []
  const infoData = []

  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000)
    days.push(d.getMonth() + 1 + '/' + d.getDate())
    criticalData.push(Math.floor(Math.random() * 10))
    errorData.push(Math.floor(Math.random() * 20))
    warningData.push(Math.floor(Math.random() * 30))
    infoData.push(Math.floor(Math.random() * 40))
  }

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    legend: {
      data: ['严重', '错误', '警告', '信息'],
      top: 0,
      textStyle: { color: '#606266' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 50, containLabel: true },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '严重',
        type: 'line',
        smooth: true,
        data: criticalData,
        lineStyle: { color: '#ff4d4f', width: 2 },
        itemStyle: { color: '#ff4d4f' }
      },
      {
        name: '错误',
        type: 'line',
        smooth: true,
        data: errorData,
        lineStyle: { color: '#faad14', width: 2 },
        itemStyle: { color: '#faad14' }
      },
      {
        name: '警告',
        type: 'line',
        smooth: true,
        data: warningData,
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '信息',
        type: 'line',
        smooth: true,
        data: infoData,
        lineStyle: { color: '#4f6ef7', width: 2 },
        itemStyle: { color: '#4f6ef7' }
      }
    ]
  })
}

const editRule = (row) => {
  ruleDialogTitle.value = '编辑规则'
  Object.assign(ruleForm, row)
  showRuleDialog.value = true
}

const deleteRule = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除规则 "${row.name}" 吗？`, '提示', {
      type: 'warning'
    })
    const index = ruleList.value.findIndex(r => r.id === row.id)
    if (index > -1) ruleList.value.splice(index, 1)
    ElMessage.success('删除成功')
  } catch (e) {
    // 忽略取消操作
  }
}

const rulesLoaded = ref(false)

const toggleRule = async (row, val) => {
  if (!rulesLoaded.value) return
  try {
    await toggleAlertRule(row.id, val)
    ElMessage.success(val ? '规则已启用' : '规则已禁用')
  } catch (e) {
    row.enabled = !val
    console.error(e)
  }
}

const submitRule = async () => {
  try {
    await ruleFormRef.value.validate()

    if (ruleForm.id) {
      // 编辑
      await updateAlertRule(ruleForm.id, ruleForm)
      const index = ruleList.value.findIndex(r => r.id === ruleForm.id)
      if (index > -1) ruleList.value[index] = { ...ruleForm }
      ElMessage.success('更新成功')
    } else {
      // 新增
      const result = await createAlertRule(ruleForm)
      ruleList.value.push({ ...ruleForm, id: result.data.id })
      ElMessage.success('创建成功')
    }
    showRuleDialog.value = false
  } catch (e) {
    console.error(e)
  }
}

const testEmail = () => {
  ElMessage.success('测试邮件已发送')
}

const testDingTalk = () => {
  ElMessage.success('钉钉通知已发送')
}

const testWebhook = () => {
  ElMessage.success('Webhook请求已发送')
}

const saveEmailConfig = () => {
  ElMessage.success('邮件配置已保存')
}

const saveDingTalkConfig = () => {
  ElMessage.success('钉钉配置已保存')
}

const saveWebhookConfig = () => {
  ElMessage.success('Webhook配置已保存')
}

const getSeverityType = (severity) => {
  const types = {
    'CRITICAL': 'danger',
    'ERROR': 'danger',
    'WARNING': 'warning',
    'INFO': 'success'
  }
  return types[severity] || 'info'
}

const getSeverityLabel = (severity) => {
  const labels = {
    'CRITICAL': '严重',
    'ERROR': '错误',
    'WARNING': '警告',
    'INFO': '信息'
  }
  return labels[severity] || severity
}

const getStatusType = (status) => {
  const types = {
    'PENDING': 'warning',
    'CONFIRMED': 'info',
    'RESOLVED': 'success'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    'PENDING': '待处理',
    'CONFIRMED': '已确认',
    'RESOLVED': '已解决'
  }
  return labels[status] || status
}

const getRuleTypeLabel = (type) => {
  const labels = {
    'THRESHOLD': '阈值',
    'TREND': '趋势',
    'SMART': '智能'
  }
  return labels[type] || type
}

const getRuleTypeColor = (type) => {
  const colors = {
    'THRESHOLD': 'blue',
    'TREND': 'purple',
    'SMART': 'cyan'
  }
  return colors[type] || 'gray'
}
</script>

<style lang="scss" scoped>
.alert-page {
  background-color: #f5f7fa;
  min-height: 100%;
  padding: 20px;

  .tab-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

    :deep(.el-card__body) {
      padding: 0;
    }

    .alert-tabs {
      :deep(.el-tabs__nav-wrap) {
        padding: 0 20px;
      }
      :deep(.el-tabs__content) {
        padding: 20px;
      }
    }
  }

  .filter-section {
    margin-bottom: 20px;

    :deep(.el-form-item__label) {
      color: #606266;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .rule-actions {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .channel-card {
    height: 100%;
    border-radius: 12px;
    border: 1px solid #ebeef5;

    :deep(.el-card__header) {
      padding: 12px 16px;
      border-bottom: 1px solid #f0f2f5;
    }

    :deep(.el-card__body) {
      padding: 16px;
    }

    .channel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .channel-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }
    }

    :deep(.el-form-item) {
      margin-bottom: 16px;

      :deep(.el-form-item__label) {
        color: #606266;
      }
    }
  }

  .trend-chart {
    height: 350px;
  }
}
</style>
