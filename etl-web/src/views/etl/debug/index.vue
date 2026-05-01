<template>
  <div class="debug-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Connection /></el-icon>
            转换调试
          </span>
        </div>
      </template>

      <!-- 顶部配置区域 -->
      <div class="config-section">
        <el-form :inline="true" :model="configForm" label-width="100px">
          <el-form-item label="流水线">
            <el-select v-model="configForm.pipelineId" placeholder="请选择" style="width: 240px">
              <el-option
                v-for="pipeline in pipelineList"
                :key="pipeline.id"
                :label="pipeline.name"
                :value="pipeline.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数据源">
            <el-select v-model="configForm.datasourceId" placeholder="请选择" style="width: 240px">
              <el-option
                v-for="ds in datasourceList"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="数据量">
            <el-input-number v-model="configForm.sampleCount" :min="10" :max="1000" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleStart" :loading="debugRunning">
              <el-icon><VideoPlay /></el-icon>
              开始调试
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 主要内容区域 -->
      <div class="debug-container" v-if="started">
        <!-- 左侧：步骤列表 -->
        <div class="steps-panel">
          <div class="panel-header">
            <span class="panel-title">调试步骤</span>
          </div>
          <div class="steps-list">
            <div
              v-for="(step, index) in steps"
              :key="step.id"
              class="step-item"
              :class="{ active: selectedStep === step, completed: step.status === 'completed', failed: step.status === 'failed' }"
              @click="selectedStep = step"
            >
              <div class="step-indicator">
                <div v-if="step.status === 'pending'" class="indicator-circle"></div>
                <el-icon v-else-if="step.status === 'running'" class="loading">
                  <Loading />
                </el-icon>
                <el-icon v-else-if="step.status === 'completed'" class="success">
                  <CircleCheck />
                </el-icon>
                <el-icon v-else class="error">
                  <CircleClose />
                </el-icon>
              </div>
              <div class="step-info">
                <div class="step-name">{{ step.name }}</div>
                <div class="step-desc">{{ step.desc }}</div>
              </div>
              <div class="step-action">
                <el-button
                  v-if="step.status !== 'running'"
                  size="small"
                  :type="step.breakpoint ? 'danger' : 'default'"
                  @click.stop="toggleBreakpoint(step)"
                >
                  <el-icon><CaretRight /></el-icon>
                  {{ step.breakpoint ? '已设断点' : '断点' }}
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：数据预览 -->
        <div class="data-panel">
          <div class="panel-header">
            <span class="panel-title">数据预览</span>
            <el-radio-group v-model="dataViewMode" size="small">
              <el-radio-button value="input">输入</el-radio-button>
              <el-radio-button value="output">输出</el-radio-button>
            </el-radio-group>
          </div>
          <div class="data-content">
            <el-table :data="tableData" stripe max-height="100%" class="data-table">
              <el-table-column
                v-for="col in tableColumns"
                :key="col"
                :prop="col"
                :label="col"
                show-overflow-tooltip
              />
            </el-table>
          </div>
          <div class="empty-data" v-if="tableData.length === 0">
            <el-empty description="暂无数据" />
          </div>
        </div>

        <!-- 右侧：性能面板 -->
        <div class="performance-panel">
          <div class="panel-header">
            <span class="panel-title">性能分析</span>
          </div>
          <div class="performance-content">
            <!-- 耗时统计 -->
            <div class="stat-card">
              <div class="stat-title">执行耗时</div>
              <div class="chart-container" ref="timeChartRef"></div>
            </div>

            <!-- 吞吐量 -->
            <div class="stat-card">
              <div class="stat-title">吞吐量</div>
              <div class="throughput-value">{{ throughput }} 条/秒</div>
              <div class="throughput-desc">平均处理速度</div>
            </div>

            <!-- 错误统计 -->
            <div class="stat-card">
              <div class="stat-title">数据统计</div>
              <div class="stats-grid">
                <div class="stat-item success">
                  <div class="stat-value">{{ successCount }}</div>
                  <div class="stat-label">成功</div>
                </div>
                <div class="stat-item error">
                  <div class="stat-value">{{ errorCount }}</div>
                  <div class="stat-label">失败</div>
                </div>
                <div class="stat-item info">
                  <div class="stat-value">{{ totalCount }}</div>
                  <div class="stat-label">总计</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：控制按钮 -->
      <div class="control-section" v-if="started">
        <el-button type="primary" @click="handleStep" :disabled="!canStep || debugRunning">
          <el-icon><Right /></el-icon>
          单步执行
        </el-button>
        <el-button type="success" @click="handleContinue" :disabled="!canContinue || debugRunning">
          <el-icon><VideoPlay /></el-icon>
          执行全部
        </el-button>
        <el-button @click="handlePause" :disabled="!isRunning">
          <el-icon><VideoPause /></el-icon>
          暂停
        </el-button>
      </div>

      <!-- 日志区域 -->
      <div class="log-section" v-if="started">
        <div class="section-header">
          <span class="section-title">调试日志</span>
          <el-button size="small" @click="clearLogs">清空</el-button>
        </div>
        <div class="log-area">
          <div class="log-line" v-for="(log, index) in logs" :key="index" :class="'log-' + log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-level" :class="'level-' + log.type">[{{ log.type }}]</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-section" v-else>
        <el-empty description="选择流水线和数据源开始调试">
          <template #image>
          <img src="" style="width: 150px; opacity: 0.5" />
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Connection, VideoPlay, RefreshLeft, Loading, CircleCheck,
  CircleClose, CaretRight, Right, VideoPause
} from '@element-plus/icons-vue'

const started = ref(false)
const debugRunning = ref(false)
const isRunning = ref(false)
const selectedStep = ref(null)
const dataViewMode = ref('input')

const configForm = reactive({
  pipelineId: null,
  datasourceId: null,
  sampleCount: 100
})

const pipelineList = ref([
  { id: 1, name: '用户数据清洗' },
  { id: 2, name: '订单数据转换' },
  { id: 3, name: '日志数据处理' }
])

const datasourceList = ref([
  { id: 1, name: 'MySQL - 用户库' },
  { id: 2, name: 'PostgreSQL - 订单库' },
  { id: 3, name: 'ClickHouse - 日志库' }
])

const steps = ref([
  { id: 's1', name: '数据抽取', desc: '从源表读取数据', status: 'pending', breakpoint: false, duration: 0 },
  { id: 's2', name: '字段映射', desc: '源字段到目标字段', status: 'pending', breakpoint: false, duration: 0 },
  { id: 's3', name: '数据脱敏', desc: '手机号和邮箱脱敏', status: 'pending', breakpoint: true, duration: 0 },
  { id: 's4', name: '格式转换', desc: '日期格式标准化', status: 'pending', breakpoint: false, duration: 0 },
  { id: 's5', name: '数据加载', desc: '写入目标表', status: 'pending', breakpoint: false, duration: 0 }
])

const logs = ref([
  { time: '14:32:05', type: 'info', message: '调试会话已启动' },
  { time: '14:32:06', type: 'info', message: '连接数据源成功' },
  { time: '14:32:07', type: 'info', message: '读取100条样本数据' }
])

const tableData = ref([
  { id: 1, name: '张三', phone: '138****1234', email: 'zhangsan@example.com' },
  { id: 2, name: '李四', phone: '139****5678', email: 'lisi@example.com' },
  { id: 3, name: '王五', phone: '137****9012', email: 'wangwu@example.com' },
  { id: 4, name: '赵六', phone: '136****3456', email: 'zhaoliu@example.com' },
  { id: 5, name: '钱七', phone: '135****7890', email: 'qianqi@example.com' }
])

const tableColumns = ref(['id', 'name', 'phone', 'email'])
const timeChartRef = ref(null)
const timeChart = ref(null)
const throughput = ref(0)
const successCount = ref(85)
const errorCount = ref(0)
const totalCount = ref(100)
const canStep = ref(false)
const canContinue = ref(false)

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  if (timeChart.value) {
    timeChart.value.dispose()
  }
})

const initChart = () => {
  nextTick(() => {
    if (timeChartRef.value) {
      timeChart.value = echarts.init(timeChartRef.value)
      updateChart()
    }
  })
}

const updateChart = () => {
  if (timeChart.value) {
    timeChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'value',
        axisLabel: { formatter: '{value} ms'
      },
      yAxis: {
        type: 'category',
        data: steps.value.map(s => s.name)
      },
      series: [
        {
          type: 'bar',
          data: steps.value.map(s => s.duration),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#4f6ef7' },
              { offset: 1, color: '#6e88ff' }
            ])
          }
        }
      ]
    })
  }
}

const toggleBreakpoint = (step) => {
  step.breakpoint = !step.breakpoint
}

const handleStart = async () => {
  if (!configForm.pipelineId) {
    ElMessage.warning('请选择流水线')
    return
  }
  if (!configForm.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }

  started.value = true
  debugRunning.value = true
  isRunning.value = true
  canStep.value = true
  canContinue.value = true

  // 模拟开始调试
  steps.value.forEach((step, index) => {
    step.status = 'pending'
  })

  logs.value = [
    { time: formatTime(), type: 'info', message: '调试会话已启动' },
    { time: formatTime(), type: 'info', message: '连接数据源成功' },
    { time: formatTime(), type: 'info', message: `读取${configForm.sampleCount}条样本数据' }
  ]

  await new Promise(resolve => setTimeout(resolve, 500))
  debugRunning.value = false

  // 选中第一步
  selectedStep.value = steps.value[0]
}

const handleStep = async () => {
  if (!selectedStep.value) return

  debugRunning.value = true
  const currentStepIndex = steps.value.findIndex(s => s.id === selectedStep.value.id)
  const currentStep = steps.value[currentStepIndex]
  currentStep.status = 'running'
  canStep.value = false
  canContinue.value = false

  logs.value.push({ time: formatTime(), type: 'info', message: `开始执行: ${currentStep.name}` })

  await new Promise(resolve => setTimeout(resolve, 800))

  currentStep.duration = Math.floor(Math.random() * 500) + 100
  currentStep.status = 'completed'
  throughput.value = Math.floor(Math.random() * 200) + 100

  logs.value.push({
    time: formatTime(),
    type: 'info',
    message: `执行完成: ${currentStep.name},
    type: 'success',
    message: `${currentStep.name}完成, ${configForm.sampleCount}条, 耗时${currentStep.duration}ms`
  })

  // 更新图表
  updateChart()

  // 移动到下一步
  if (currentStepIndex < steps.value.length - 1) {
    selectedStep.value = steps.value[currentStepIndex + 1]
    canStep.value = true
    canContinue.value = true
  } else {
    selectedStep.value = null
    canStep.value = false
    canContinue.value = false
    isRunning.value = false
    logs.value.push({
      time: formatTime(),
      type: 'success',
      message: '调试完成'
    })
  }

  debugRunning.value = false
}

const handleContinue = async () => {
  debugRunning.value = true
  isRunning.value = true
  canStep.value = false
  canContinue.value = false

  const startIndex = steps.value.findIndex(s => s.id === selectedStep.value?.id) || 0

  for (let i = startIndex; i < steps.value.length; i++) {
    const step = steps.value[i]
    step.status = 'running'
    selectedStep.value = step

    // 如果是断点，停止
    if (step.breakpoint && i > startIndex) {
      logs.value.push({
        time: formatTime(),
        type: 'warning',
        message: `遇到断点: ${step.name}`
      })
      canStep.value = true
      canContinue.value = true
      debugRunning.value = false
      return
    }

    logs.value.push({ time: formatTime(), type: 'info', message: `执行: ${step.name}` })

    await new Promise(resolve => setTimeout(resolve, 600))
    step.duration = Math.floor(Math.random() * 400) + 80
    step.status = 'completed'
    throughput.value = Math.floor(Math.random() * 300) + 150

    updateChart()
  }

  selectedStep.value = null
  isRunning.value = false
  canStep.value = false
  canContinue.value = false
  debugRunning.value = false

  logs.value.push({ time: formatTime(), type: 'success', message: '全部步骤执行完成' })
}

const handlePause = () => {
  isRunning.value = false
  ElMessage.info('已暂停')
}

const handleReset = () => {
  started.value = false
  selectedStep.value = null
  steps.value.forEach(step => {
    step.status = 'pending'
    step.duration = 0
  })
  logs.value = []
  throughput.value = 0
  debugRunning.value = false
  isRunning.value = false
  canStep.value = false
  canContinue.value = false
}

const clearLogs = () => {
  logs.value = []
}

const formatTime = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}
</script>

<style lang="scss" scoped>
.debug-page {
  .main-card {
    border-radius: 16px;
  }

  .card-header {
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

  .config-section {
    padding: 20px;
    margin-bottom: 16px;
    background: rgba(79, 110, 247, 0.05);
    border: 1px solid var(--border-color);
    border-radius: 12px;
  }

  .debug-container {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;

    .steps-panel {
      width: 280px;
      background: #fff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      display: flex;
      flex-direction: column;

      .panel-header {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color);

        .panel-title {
          font-weight: 600;
          color: var(--text-primary);
        }
      }

      .steps-list {
        flex: 1;
        padding: 8px;
        overflow-y: auto;

        .step-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px;
          border: 1px solid var(--border-color);
          border-radius: 8px;
          margin-bottom: 8px;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            background: rgba(79, 110, 247, 0.05);
          }

          &.active {
            border-color: var(--primary-color);
            background: rgba(79, 110, 247, 0.1);
          }

          &.completed {
            .step-name {
              color: #52c41a;
            }
          }

          &.failed {
            .step-name {
              color: #ff4d4f;
            }
          }

          .step-indicator {
            .indicator-circle {
              width: 20px;
              height: 20px;
              border-radius: 50%;
              border: 2px solid var(--border-color);
            }

            .loading {
              width: 20px;
              height: 20px;
              color: var(--primary-color);
              animation: rotate 1s linear infinite;
            }

            .success {
              color: #52c41a;
            }

            .error {
              color: #ff4d4f;
            }
          }

          .step-info {
            flex: 1;

            .step-name {
              font-weight: 600;
              font-size: 14px;
              color: var(--text-primary);
            }

            .step-desc {
              font-size: 12px;
              color: var(--text-muted);
            }
          }
        }
      }
    }

    .data-panel {
      flex: 1;
      background: #fff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      display: flex;
      flex-direction: column;

      .panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color);
      }

      .data-content {
        flex: 1;
        overflow: auto;
        padding: 16px;
      }
    }

    .performance-panel {
      width: 280px;
      background: #fff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      display: flex;
      flex-direction: column;

      .panel-header {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border-color);
      }

      .performance-content {
        flex: 1;
        padding: 16px;
        overflow-y: auto;

        .stat-card {
          margin-bottom: 16px;
          padding: 16px;
          background: rgba(79, 110, 247, 0.05);
          border: 1px solid var(--border-color);
          border-radius: 8px;

          .stat-title {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 12px;
          }

          .chart-container {
            height: 200px;
          }

          .throughput-value {
            font-size: 36px;
            font-weight: 700;
            color: var(--primary-color);
            margin-bottom: 8px;
          }

          .throughput-desc {
            font-size: 12px;
            color: var(--text-muted);
          }

          .stats-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 12px;

            .stat-item {
              text-align: center;

              &.success .stat-value {
                color: #52c41a;
              }

              &.error .stat-value {
                color: #ff4d4f;
              }

              &.info .stat-value {
                color: var(--text-primary);
              }

              .stat-value {
                font-size: 28px;
                font-weight: 700;
                margin-bottom: 4px;
              }

              .stat-label {
                font-size: 12px;
                color: var(--text-muted);
              }
            }
          }
        }
      }
    }
  }

  .control-section {
    display: flex;
    gap: 12px;
    justify-content: center;
    padding: 16px;
    background: #fff;
    border: 1px solid var(--border-color);
    border-radius: 12px;
    margin-bottom: 16px;
  }

  .log-section {
    background: #fff;
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 16px;

    .section-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
    }

    .log-area {
      height: 200px;
      overflow-y: auto;
      background: #1a1a2e;
      border-radius: 8px;
      padding: 12px;

      .log-line {
        display: flex;
        gap: 8px;
        padding: 4px 0;
        font-family: 'Monaco', 'Consolas', monospace;
        font-size: 13px;

        .log-time {
          color: #7f848e;
          min-width: 70px;
        }

        .log-level {
          min-width: 60px;
          font-weight: 600;

          &.level-info {
            color: #4f6ef7;
          }

          &.level-success {
            color: #52c41a;
          }

          &.level-warning {
            color: #faad14;
          }

          &.level-error {
            color: #ff4d4f;
          }
        }

        &.log-info {
          color: #e0e0e0;
        }

        &.log-success {
          color: #e0e0e0;
        }

        &.log-warning {
          color: #e0e0e0;
        }

        &.log-error {
          color: #e0e0e0;
        }
      }
    }
  }

  .empty-section {
    padding: 80px 0;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
