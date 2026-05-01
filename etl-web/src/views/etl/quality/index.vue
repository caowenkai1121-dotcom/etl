<template>
  <div class="quality-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Finished /></el-icon>
            数据质量
          </span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="quality-tabs">
        <!-- ========== Tab1: 规则配置 ========== -->
        <el-tab-pane label="规则配置" name="rule">
          <div class="tab-toolbar">
            <el-button type="primary" @click="handleAddRule">
              <el-icon><Plus /></el-icon>
              新建规则
            </el-button>
          </div>

          <el-table :data="ruleTableData" v-loading="ruleLoading" stripe class="data-table">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="ruleName" label="规则名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="ruleType" label="校验类型" width="130">
              <template #default="{ row }">
                <el-tag :type="getRuleTypeTag(row.ruleType)" size="small">
                  {{ getRuleTypeLabel(row.ruleType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetField" label="目标字段" width="130" show-overflow-tooltip />
            <el-table-column prop="threshold" label="阈值" width="100" align="center">
              <template #default="{ row }">
                <span>{{ row.threshold ?? '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="severity" label="严重级别" width="100">
              <template #default="{ row }">
                <el-tag :type="getSeverityTag(row.severity)" size="small">
                  {{ getSeverityLabel(row.severity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="enabled" label="启用" width="70" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled === 1"
                  @change="(val) => handleToggleRule(row, val)"
                  size="small"
                />
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditRule(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="ruleQuery.pageNum"
            v-model:page-size="ruleQuery.pageSize"
            :total="ruleTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchRules"
            @current-change="fetchRules"
            class="pagination-wrap"
          />
        </el-tab-pane>

        <!-- ========== Tab2: 质量报告 ========== -->
        <el-tab-pane label="质量报告" name="report">
          <!-- 统计概览 -->
          <div class="report-stats">
            <div class="stat-card-inline">
              <div class="stat-icon gradient-purple">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ reportStats.total }}</div>
                <div class="stat-label">总校验执行</div>
              </div>
            </div>
            <div class="stat-card-inline">
              <div class="stat-icon gradient-green">
                <el-icon><SuccessFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ reportStats.passed }}</div>
                <div class="stat-label">通过</div>
              </div>
            </div>
            <div class="stat-card-inline">
              <div class="stat-icon gradient-red">
                <el-icon><WarningFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ reportStats.failed }}</div>
                <div class="stat-label">失败</div>
              </div>
            </div>
            <div class="stat-card-inline">
              <div class="stat-icon gradient-cyan">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ reportStats.passRate }}%</div>
                <div class="stat-label">通过率</div>
              </div>
            </div>
          </div>

          <!-- 搜索过滤 -->
          <div class="search-bar">
            <el-form :inline="true" :model="reportQuery">
              <el-form-item label="规则名称">
                <el-input v-model="reportQuery.ruleName" placeholder="请输入" clearable />
              </el-form-item>
              <el-form-item label="执行结果">
                <el-select v-model="reportQuery.result" placeholder="请选择" clearable>
                  <el-option label="通过" value="PASSED" />
                  <el-option label="失败" value="FAILED" />
                </el-select>
              </el-form-item>
              <el-form-item label="执行时间">
                <el-date-picker
                  v-model="reportQuery.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="fetchReport">查询</el-button>
                <el-button @click="resetReportQuery">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 报告日志表格 -->
          <el-table :data="reportTableData" v-loading="reportLoading" stripe class="data-table">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="taskName" label="任务名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="ruleName" label="规则名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="ruleType" label="校验类型" width="110">
              <template #default="{ row }">
                <el-tag :type="getRuleTypeTag(row.ruleType)" size="small">
                  {{ getRuleTypeLabel(row.ruleType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === 'PASSED' ? 'success' : 'danger'" size="small">
                  {{ row.result === 'PASSED' ? '通过' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalRows" label="总行数" width="80" align="center" />
            <el-table-column prop="errorRows" label="异常行数" width="90" align="center" />
            <el-table-column prop="errorRate" label="异常率" width="90" align="center">
              <template #default="{ row }">
                <span :class="row.errorRate > 5 ? 'text-danger' : row.errorRate > 1 ? 'text-warning' : 'text-success'">
                  {{ row.errorRate }}%
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="executedAt" label="执行时间" width="170" />
          </el-table>

          <el-pagination
            v-model:current-page="reportQuery.pageNum"
            v-model:page-size="reportQuery.pageSize"
            :total="reportTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchReport"
            @current-change="fetchReport"
            class="pagination-wrap"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 规则对话框 -->
    <el-dialog v-model="ruleDialogVisible" :title="ruleDialogTitle" width="540px" class="dark-dialog">
      <el-form :model="ruleForm" label-width="110px" :rules="ruleFormRules" ref="ruleFormRef">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="校验类型" prop="ruleType">
          <el-select v-model="ruleForm.ruleType" placeholder="请选择" style="width: 100%">
            <el-option label="非空校验" value="NOT_NULL" />
            <el-option label="唯一性校验" value="UNIQUE" />
            <el-option label="值范围校验" value="RANGE" />
            <el-option label="格式校验" value="FORMAT" />
            <el-option label="值枚举校验" value="ENUM" />
            <el-option label="自定义SQL" value="CUSTOM_SQL" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标字段" prop="targetField">
          <el-input v-model="ruleForm.targetField" placeholder="请输入目标字段名" />
        </el-form-item>
        <el-form-item label="校验表达式">
          <el-input v-model="ruleForm.expression" type="textarea" :rows="2" placeholder="根据校验类型填写表达式，如：age > 0 && age < 150" />
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="ruleForm.threshold" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="严重级别" prop="severity">
          <el-select v-model="ruleForm.severity" placeholder="请选择" style="width: 100%">
            <el-option label="提示" value="INFO" />
            <el-option label="警告" value="WARNING" />
            <el-option label="错误" value="ERROR" />
            <el-option label="严重" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabledBool" active-text="是" inactive-text="否" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getQualityRulePage, getQualityRule, createQualityRule, updateQualityRule, deleteQualityRule,
  getQualityReport, getQualityLogPageV2
} from '@/api'

const activeTab = ref('rule')

// ========== 规则配置 ==========
const ruleLoading = ref(false)
const ruleTableData = ref([])
const ruleTotal = ref(0)
const ruleQuery = reactive({ pageNum: 1, pageSize: 10 })

const ruleDialogVisible = ref(false)
const ruleDialogTitle = ref('新建规则')
const ruleFormRef = ref(null)
const ruleForm = reactive({
  id: null, ruleName: '', ruleType: 'NOT_NULL', targetField: '',
  expression: '', threshold: 0, severity: 'WARNING', enabledBool: true
})
const ruleFormRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择校验类型', trigger: 'change' }],
  targetField: [{ required: true, message: '请输入目标字段', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择严重级别', trigger: 'change' }]
}

// ========== 质量报告 ==========
const reportLoading = ref(false)
const reportTableData = ref([])
const reportTotal = ref(0)
const reportStats = reactive({ total: 0, passed: 0, failed: 0, passRate: 0 })
const reportQuery = reactive({ pageNum: 1, pageSize: 10, ruleName: '', result: '', dateRange: null })

onMounted(() => {
  fetchRules()
})

watch(activeTab, (val) => {
  if (val === 'report') {
    fetchReport()
  }
})

// ---------- 规则 CRUD ----------
const fetchRules = async () => {
  ruleLoading.value = true
  try {
    const params = { pageNum: ruleQuery.pageNum, pageSize: ruleQuery.pageSize }
    const res = await getQualityRulePage(params)
    ruleTableData.value = res.data?.list || []
    ruleTotal.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    ruleLoading.value = false
  }
}

const handleAddRule = () => {
  ruleDialogTitle.value = '新建规则'
  Object.assign(ruleForm, {
    id: null, ruleName: '', ruleType: 'NOT_NULL', targetField: '',
    expression: '', threshold: 0, severity: 'WARNING', enabledBool: true
  })
  ruleDialogVisible.value = true
}

const handleEditRule = async (row) => {
  ruleDialogTitle.value = '编辑规则'
  try {
    const res = await getQualityRule(row.id)
    const data = res.data || row
    Object.assign(ruleForm, {
      id: data.id,
      ruleName: data.ruleName,
      ruleType: data.ruleType,
      targetField: data.targetField,
      expression: data.expression || '',
      threshold: data.threshold ?? 0,
      severity: data.severity || 'WARNING',
      enabledBool: data.enabled === 1
    })
    ruleDialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const handleDeleteRule = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该质量规则？', '提示', { type: 'warning' })
    await deleteQualityRule(row.id)
    ElMessage.success('删除成功')
    fetchRules()
  } catch (e) {}
}

const handleToggleRule = async (row, val) => {
  try {
    const enabled = val ? 1 : 0
    await updateQualityRule(row.id, { ...row, enabled })
    ElMessage.success(val ? '已启用' : '已禁用')
    fetchRules()
  } catch (e) {}
}

const handleSubmitRule = async () => {
  try {
    await ruleFormRef.value.validate()
    const submitData = {
      ruleName: ruleForm.ruleName,
      ruleType: ruleForm.ruleType,
      targetField: ruleForm.targetField,
      expression: ruleForm.expression,
      threshold: ruleForm.threshold,
      severity: ruleForm.severity,
      enabled: ruleForm.enabledBool ? 1 : 0
    }
    if (ruleForm.id) {
      await updateQualityRule(ruleForm.id, submitData)
    } else {
      await createQualityRule(submitData)
    }
    ElMessage.success('操作成功')
    ruleDialogVisible.value = false
    fetchRules()
  } catch (e) {}
}

// ---------- 质量报告 ----------
const fetchReport = async () => {
  reportLoading.value = true
  try {
    const params = {
      pageNum: reportQuery.pageNum,
      pageSize: reportQuery.pageSize
    }
    if (reportQuery.ruleName) params.ruleName = reportQuery.ruleName
    if (reportQuery.result) params.result = reportQuery.result
    if (reportQuery.dateRange) {
      params.startDate = reportQuery.dateRange[0]
      params.endDate = reportQuery.dateRange[1]
    }

    // 获取报告统计数据
    try {
      const statsRes = await getQualityReport(params)
      const stats = statsRes.data || {}
      reportStats.total = stats.total || 0
      reportStats.passed = stats.passed || 0
      reportStats.failed = stats.failed || 0
      reportStats.passRate = stats.passRate ?? (
        (stats.total && stats.total > 0)
          ? ((stats.passed / stats.total) * 100).toFixed(1)
          : 100
      )
    } catch (e) { /* 接口可能不存在，仅静默失败 */ }

    // 获取报告日志列表
    try {
      const logRes = await getQualityLogPageV2(params)
      reportTableData.value = logRes.data?.list || []
      reportTotal.value = logRes.data?.total || 0
    } catch (e) {
      // 降级：用报告数据填充
      reportTableData.value = []
      reportTotal.value = 0
    }
  } catch (e) {
    console.error(e)
  } finally {
    reportLoading.value = false
  }
}

const resetReportQuery = () => {
  reportQuery.ruleName = ''
  reportQuery.result = ''
  reportQuery.dateRange = null
  fetchReport()
}

// ---------- 工具方法 ----------
const getRuleTypeTag = (type) => {
  const tags = { NOT_NULL: 'danger', UNIQUE: 'warning', RANGE: 'primary', FORMAT: 'info', ENUM: 'success', CUSTOM_SQL: 'warning' }
  return tags[type] || 'info'
}

const getRuleTypeLabel = (type) => {
  const labels = { NOT_NULL: '非空校验', UNIQUE: '唯一性', RANGE: '值范围', FORMAT: '格式校验', ENUM: '值枚举', CUSTOM_SQL: '自定义SQL' }
  return labels[type] || type
}

const getSeverityTag = (severity) => {
  const tags = { INFO: 'info', WARNING: 'warning', ERROR: 'danger', CRITICAL: 'danger' }
  return tags[severity] || 'info'
}

const getSeverityLabel = (severity) => {
  const labels = { INFO: '提示', WARNING: '警告', ERROR: '错误', CRITICAL: '严重' }
  return labels[severity] || severity
}
</script>

<style lang="scss" scoped>
.quality-page {
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

  .quality-tabs {
    :deep(.el-tabs__content) {
      padding-top: 16px;
    }
  }

  .tab-toolbar {
    margin-bottom: 16px;
    display: flex;
    justify-content: flex-end;
  }

  .search-bar {
    padding: 20px;
    margin-bottom: 20px;
    background: rgba(99, 102, 241, 0.05);
    border-radius: 12px;
    border: 1px solid var(--border-color);
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .report-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 20px;
  }
}
</style>
