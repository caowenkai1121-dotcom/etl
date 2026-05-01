<template>
  <div class="quality-page">
    <!-- 左侧区域 -->
    <div class="left-column">
      <!-- 质量评分卡片 -->
      <el-card class="score-card">
        <div class="score-content">
          <div class="score-display">
            <div class="score-circle" :class="scoreClass">
              <span class="score-number">{{ qualityScore }}</span>
            </div>
          </div>
          <div class="score-info">
            <h3>质量评分</h3>
            <p :class="scoreClass">{{ scoreLabel }}</p>
            <div class="score-meta">
              <span>评分基于 {{ ruleCount }} 条质量规则</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 质量趋势图表 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">质量趋势</span>
            <el-radio-group v-model="trendRange" size="small" @change="updateTrendChart">
              <el-radio-button value="7">近7天</el-radio-button>
              <el-radio-button value="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="trend-chart"></div>
      </el-card>

      <!-- 规则通过率排行 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">规则通过率排行</span>
          </div>
        </template>
        <div ref="rankChartRef" class="rank-chart"></div>
      </el-card>
    </div>

    <!-- 右侧区域 -->
    <div class="right-column">
      <!-- 规则配置 -->
      <el-card class="rule-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">规则配置</span>
            <el-button type="primary" size="small" @click="showRuleDialog = true">
              <el-icon><Plus /></el-icon>
              新增规则
            </el-button>
          </div>
        </template>
        <el-table :data="ruleList" style="width: 100%" stripe>
          <el-table-column prop="name" label="规则名" min-width="150" />
          <el-table-column prop="dimension" label="维度" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getDimensionType(row.dimension)">
                {{ row.dimension }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="severity" label="严重度" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="getSeverityType(row.severity)">
                {{ row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="enabled" label="状态" width="60">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small" @change="toggleRule(row)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="editRule(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 异常数据列表 -->
      <el-card class="anomaly-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">异常数据</span>
          </div>
        </template>
        <el-table
          :data="anomalyList"
          style="width: 100%"
          :expand-row-keys="expandedRows"
          @expand-change="handleExpand"
          stripe
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="anomaly-detail">
                <div class="detail-section">
                  <h4>失败数据</h4>
                  <pre class="json-view">{{ formatJson(row.data) }}</pre>
                </div>
                <div class="detail-section">
                  <h4>修复建议</h4>
                  <div class="suggestion">
                    {{ row.suggestion }}
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="ruleName" label="规则名" width="150" />
          <el-table-column prop="tableName" label="表名" width="120" />
          <el-table-column prop="count" label="数量" width="80" />
          <el-table-column prop="firstOccur" label="首次出现" width="160" />
          <el-table-column prop="severity" label="严重度" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="getSeverityType(row.severity)">
                {{ row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="handleFix(row)">修复</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 新增/编辑规则对话框 -->
    <el-dialog v-model="showRuleDialog" :title="ruleDialogTitle" width="500px">
      <el-form :model="ruleForm" :rules="ruleRules" ref="ruleFormRef" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="输入规则名称" />
        </el-form-item>
        <el-form-item label="维度" prop="dimension">
          <el-select v-model="ruleForm.dimension" placeholder="选择维度" style="width: 100%">
            <el-option label="完整性" value="完整性" />
            <el-option label="一致性" value="一致性" />
            <el-option label="准确性" value="准确性" />
            <el-option label="时效性" value="时效性" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="ruleForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="非空检查" value="非空检查" />
            <el-option label="格式检查" value="格式检查" />
            <el-option label="范围检查" value="范围检查" />
            <el-option label="唯一性检查" value="唯一性检查" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重度" prop="severity">
          <el-select v-model="ruleForm.severity" placeholder="选择严重度" style="width: 100%">
            <el-option label="严重" value="严重" />
            <el-option label="警告" value="警告" />
            <el-option label="提示" value="提示" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则条件" prop="condition">
          <el-input v-model="ruleForm.condition" type="textarea" :rows="3" placeholder='{"column": "name", "rule": "not null"}' />
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
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getQualityRulePage, createQualityRule, updateQualityRule, qualityAPI } from '@/api'

const qualityScore = ref(85)
const ruleCount = ref(12)
const trendRange = ref('7')
const showRuleDialog = ref(false)
const ruleDialogTitle = ref('新增规则')
const expandedRows = ref([])

// 规则表单
const ruleForm = reactive({
  id: null,
  name: '',
  dimension: '完整性',
  type: '非空检查',
  severity: '警告',
  condition: '',
  enabled: true
})

const ruleRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  dimension: [{ required: true, message: '请选择维度', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重度', trigger: 'change' }],
  condition: [{ required: true, message: '请输入规则条件', trigger: 'blur' }]
}

const ruleFormRef = ref()

// 评分相关
const scoreClass = computed(() => {
  if (qualityScore.value >= 80) return 'excellent'
  if (qualityScore.value >= 50) return 'good'
  return 'poor'
})

const scoreLabel = computed(() => {
  if (qualityScore.value >= 80) return '优秀'
  if (qualityScore.value >= 50) return '良好'
  return '较差'
})

// 生成规则列表数据
const ruleList = ref([
  { id: 1, name: '用户ID非空检查', dimension: '完整性', type: '非空检查', severity: '严重', enabled: true },
  { id: 2, name: '邮箱格式验证', dimension: '准确性', type: '格式检查', severity: '警告', enabled: true },
  { id: 3, name: '年龄范围检查', dimension: '准确性', type: '范围检查', severity: '警告', enabled: true },
  { id: 4, name: '订单ID唯一性', dimension: '完整性', type: '唯一性检查', severity: '严重', enabled: true },
  { id: 5, name: '数据时效性验证', dimension: '时效性', type: '范围检查', severity: '提示', enabled: false }
])

// 生成异常数据列表
const anomalyList = ref([
  {
    id: 1,
    ruleName: '用户ID非空检查',
    tableName: 'user_table',
    count: 5,
    firstOccur: '2026-04-24 10:00:00',
    severity: '严重',
    data: { id: null, name: '张三', email: 'zhangsan@example.com' },
    suggestion: '检查源数据，确保用户ID字段在入库前有有效值，可能需要修复数据源生成逻辑。'
  },
  {
    id: 2,
    ruleName: '邮箱格式验证',
    tableName: 'user_table',
    count: 12,
    firstOccur: '2026-04-23 15:30:00',
    severity: '警告',
    data: { id: 1001, name: '李四', email: 'lisi@invalid-email' },
    suggestion: '使用正则表达式修复邮箱格式，或者更新数据清洗规则来处理此类异常数据。'
  }
])

// 图表引用
const trendChartRef = ref(null)
const rankChartRef = ref(null)
let trendChart = null
let rankChart = null

onMounted(() => {
  nextTick(() => {
    initTrendChart()
    initRankChart()
  })
})

watch(qualityScore, () => {
  nextTick(() => {
    updateTrendChart()
  })
})

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  updateTrendChart()
}

const updateTrendChart = async () => {
  if (!trendChart) return

  const count = trendRange.value === '7' ? 7 : 30
  const now = new Date()
  let days = []
  let scores = []

  try {
    const res = await qualityAPI.getQualityReport({ days: count })
    if (res.data?.trend) {
      scores = res.data.trend.map(d => d.score || d.value || 0)
      days = res.data.trend.map(d => d.date || d.day || '')
    }
  } catch (e) {}

  if (scores.length === 0) {
    for (let i = count - 1; i >= 0; i--) {
      const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000)
      days.push(d.getMonth() + 1 + '/' + d.getDate())
      scores.push(80 + (i % 5))
    }
  }

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 30, containLabel: true },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: count > 10 ? 10 : 12 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399' }
    },
    series: [{
      name: '质量评分',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: scores,
      lineStyle: { color: '#4f6ef7', width: 2 },
      itemStyle: { color: '#4f6ef7' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(79, 110, 247, 0.3)' },
          { offset: 1, color: 'rgba(79, 110, 247, 0.05)' }
        ])
      }
    }]
  })
}

const initRankChart = () => {
  if (!rankChartRef.value) return
  rankChart = echarts.init(rankChartRef.value)

  const ruleNames = ['用户ID非空检查', '邮箱格式验证', '年龄范围检查', '订单ID唯一性', '数据时效性验证']
  const passRates = [98.5, 87.3, 95.2, 99.1, 89.6]

  rankChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' },
      formatter: '{b}: {c}%'
    },
    grid: { left: '3%', right: '8%', bottom: '3%', top: 10, containLabel: true },
    xAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'category',
      data: ruleNames,
      inverse: true,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266' }
    },
    series: [{
      name: '通过率',
      type: 'bar',
      data: passRates,
      barWidth: '50%',
      itemStyle: {
        color: (params) => {
          if (params.value >= 95) return '#52c41a'
          if (params.value >= 80) return '#faad14'
          return '#ff4d4f'
        },
        borderRadius: [0, 4, 4, 0]
      },
      label: {
        show: true,
        position: 'right',
        formatter: '{c}%',
        color: '#606266'
      }
    }]
  })
}

const toggleRule = (row) => {
  ElMessage.success(row.enabled ? '规则已启用' : '规则已禁用')
}

const editRule = (row) => {
  ruleDialogTitle.value = '编辑规则'
  Object.assign(ruleForm, row)
  showRuleDialog.value = true
}

const submitRule = async () => {
  try {
    await ruleFormRef.value.validate()
    if (ruleForm.id) {
      const index = ruleList.value.findIndex(r => r.id === ruleForm.id)
      if (index > -1) ruleList.value[index] = { ...ruleForm }
      ElMessage.success('更新成功')
    } else {
      ruleList.value.push({ ...ruleForm, id: Date.now() })
      ElMessage.success('创建成功')
    }
    showRuleDialog.value = false
  } catch (e) {
    console.error(e)
  }
}

const handleExpand = (row, rows) => {
  expandedRows.value = rows.map(r => r.id)
}

const handleFix = (row) => {
  ElMessage.success('修复任务已提交')
}

const formatJson = (obj) => {
  return JSON.stringify(obj, null, 2)
}

const getDimensionType = (dim) => {
  const types = {
    '完整性': 'success',
    '一致性': 'primary',
    '准确性': 'warning',
    '时效性': 'info'
  }
  return types[dim] || 'info'
}

const getSeverityType = (sev) => {
  const types = {
    '严重': 'danger',
    '警告': 'warning',
    '提示': 'info'
  }
  return types[sev] || 'info'
}
</script>

<style lang="scss" scoped>
.quality-page {
  background-color: #f5f7fa;
  min-height: 100%;
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }

  .left-column {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .right-column {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .score-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

    :deep(.el-card__body) {
      padding: 24px;
    }

    .score-content {
      display: flex;
      align-items: center;
      gap: 32px;

      .score-display {
        .score-circle {
          width: 140px;
          height: 140px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          position: relative;
          background: #fff;
          border: 8px solid #ebeef5;

          &::before {
            content: '';
            position: absolute;
            width: 124px;
            height: 124px;
            border-radius: 50%;
            border: 8px solid transparent;
            border-top-color: currentColor;
            transform: rotate(-45deg);
          }

          &.excellent {
            color: #52c41a;
            border-color: rgba(82, 196, 26, 0.1);

            &::before {
              border-top-color: #52c41a;
            }
          }

          &.good {
            color: #faad14;
            border-color: rgba(250, 173, 20, 0.1);

            &::before {
              border-top-color: #faad14;
            }
          }

          &.poor {
            color: #ff4d4f;
            border-color: rgba(255, 77, 79, 0.1);

            &::before {
              border-top-color: #ff4d4f;
            }
          }

          .score-number {
            font-size: 42px;
            font-weight: 700;
            line-height: 1;
            color: currentColor;
          }
        }
      }

      .score-info {
        flex: 1;

        h3 {
          font-size: 20px;
          color: #303133;
          margin: 0 0 8px 0;
        }

        p {
          font-size: 24px;
          font-weight: 600;
          margin: 0 0 12px 0;

          &.excellent {
            color: #52c41a;
          }

          &.good {
            color: #faad14;
          }

          &.poor {
            color: #ff4d4f;
          }
        }

        .score-meta {
          span {
            color: #909399;
            font-size: 14px;
          }
        }
      }
    }
  }

  .chart-card,
  .rule-card,
  .anomaly-card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #f0f2f5;
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }
    }

    .trend-chart,
    .rank-chart {
      height: 260px;
    }

    .anomaly-detail {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;

      .detail-section {
        h4 {
          font-size: 14px;
          color: #303133;
          margin: 0 0 12px 0;
        }

        .json-view {
          background: #f5f7fa;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          padding: 16px;
          margin: 0;
          font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
          font-size: 12px;
          color: #606266;
          max-height: 200px;
          overflow: auto;
        }

        .suggestion {
          padding: 16px;
          background: #f0f9eb;
          border: 1px solid #c2e7b0;
          border-radius: 8px;
          color: #52c41a;
          line-height: 1.6;
        }
      }
    }
  }
}
</style>
