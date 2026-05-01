<template>
  <div class="api-editor-page">
    <div class="editor-header">
      <el-button @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回列表</el-button>
      <div class="header-center">
        <span class="title">{{ isNew ? '新建API服务' : '编辑API服务' }}</span>
        <el-tag v-if="!isNew" :type="form.status === 'ONLINE' ? 'success' : 'info'" size="small">
          {{ form.status === 'ONLINE' ? '已上线' : '编辑中' }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button @click="handlePreview">预览SQL</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>
    </div>

    <div class="editor-content">
      <el-row :gutter="20">
        <el-col :span="16">
          <el-card class="main-card">
            <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="API名称" prop="name">
                    <el-input v-model="form.name" placeholder="请输入API名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="请求方法">
                    <el-radio-group v-model="form.method">
                      <el-radio value="GET">GET</el-radio>
                      <el-radio value="POST">POST</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="请求路径" prop="path">
                <el-input v-model="form.path" placeholder="如 /user/list">
                  <template #prepend>/data-api</template>
                </el-input>
              </el-form-item>

              <el-form-item label="关联数据源" prop="datasourceId">
                <el-select v-model="form.datasourceId" placeholder="选择数据源" style="width: 100%">
                  <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                </el-select>
              </el-form-item>

              <el-form-item label="SQL模板" prop="sqlTemplate">
                <div class="sql-editor">
                  <div class="sql-toolbar">
                    <div class="sql-inserts">
                      <el-button size="small" @click="insertSQL('SELECT ')">SELECT</el-button>
                      <el-button size="small" @click="insertSQL('FROM ')">FROM</el-button>
                      <el-button size="small" @click="insertSQL('WHERE ')">WHERE</el-button>
                      <el-button size="small" @click="insertSQL('ORDER BY ')">ORDER BY</el-button>
                      <el-button size="small" @click="insertSQL('JOIN ')">JOIN</el-button>
                      <el-button size="small" @click="insertSQL('GROUP BY ')">GROUP BY</el-button>
                      <el-button size="small" @click="insertParam">${}</el-button>
                    </div>
                    <el-button size="small" @click="formatSQL">格式化</el-button>
                  </div>
                  <el-input
                    v-model="form.sqlTemplate"
                    type="textarea"
                    :rows="10"
                    placeholder="SELECT * FROM users WHERE status = ${status}"
                    class="sql-textarea"
                  />
                  <div class="sql-help">
                    <span>参数占位符: ${参数名}，自动从请求参数中匹配</span>
                    <span class="sql-params-hint" v-if="detectedParams.length > 0">
                      已检测参数: <el-tag v-for="p in detectedParams" :key="p" size="small" type="warning" style="margin-left: 4px;">{{ p }}</el-tag>
                    </span>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="参数定义">
                <div class="param-def-section">
                  <el-table :data="form.paramList" size="small" border>
                    <el-table-column prop="name" label="参数名" width="150">
                      <template #default="{ row }">
                        <el-input v-model="row.name" size="small" placeholder="参数名" />
                      </template>
                    </el-table-column>
                    <el-table-column prop="type" label="类型" width="120">
                      <template #default="{ row }">
                        <el-select v-model="row.type" size="small">
                          <el-option label="字符串" value="string" />
                          <el-option label="数字" value="number" />
                          <el-option label="日期" value="date" />
                          <el-option label="布尔" value="boolean" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column prop="required" label="必填" width="70">
                      <template #default="{ row }">
                        <el-checkbox v-model="row.required" />
                      </template>
                    </el-table-column>
                    <el-table-column prop="defaultValue" label="默认值" width="140">
                      <template #default="{ row }">
                        <el-input v-model="row.defaultValue" size="small" placeholder="默认值" />
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="说明" min-width="150">
                      <template #default="{ row }">
                        <el-input v-model="row.description" size="small" placeholder="参数说明" />
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="70">
                      <template #default="{ $index }">
                        <el-button size="small" type="danger" @click="removeParam($index)" circle>
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="param-actions">
                    <el-button size="small" type="primary" @click="addParam">
                      <el-icon><Plus /></el-icon> 添加参数
                    </el-button>
                    <el-button size="small" @click="syncParamsFromSQL">
                      <el-icon><Refresh /></el-icon> 从SQL同步
                    </el-button>
                  </div>
                </div>
              </el-form-item>

              <el-divider />

              <el-row :gutter="24">
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
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述API的功能和用途" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 右侧面板 -->
        <el-col :span="8">
          <el-card class="side-card">
            <template #header><span class="side-title">SQL预览</span></template>
            <pre class="sql-preview">{{ form.sqlTemplate || '(尚未输入SQL)' }}</pre>
          </el-card>

          <el-card class="side-card">
            <template #header><span class="side-title">API示例</span></template>
            <div class="api-example">
              <div class="example-item">
                <span class="example-label">请求URL:</span>
                <code>{{ exampleUrl }}</code>
              </div>
              <div class="example-item">
                <span class="example-label">返回格式:</span>
                <pre class="example-json">{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "..."
    }
  ],
  "message": "success"
}</pre>
              </div>
            </div>
          </el-card>

          <el-card class="side-card">
            <template #header><span class="side-title">快捷操作</span></template>
            <div class="quick-actions">
              <el-button style="width: 100%; margin-bottom: 8px;" @click="handleTest">
                <el-icon><VideoPlay /></el-icon>
                在线测试
              </el-button>
              <el-button style="width: 100%;" @click="handleCopySQL">
                <el-icon><CopyDocument /></el-icon>
                复制SQL
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 测试对话框 -->
    <el-dialog v-model="testDialogVisible" title="在线测试" width="600px">
      <div class="test-content">
        <div class="test-url">{{ exampleUrl }}</div>
        <el-input v-model="testInputParams" type="textarea" :rows="6" placeholder='{"id": 1, "name": "test"}' />
        <el-button type="primary" @click="runTest" :loading="runningTest" style="margin-top: 12px;">
          <el-icon><VideoPlay /></el-icon>
          执行
        </el-button>
        <div v-if="testOutput" class="test-output">
          <div class="output-header">返回结果 ({{ testDuration }}ms)</div>
          <pre>{{ JSON.stringify(testOutput, null, 2) }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiServiceAPI, getDatasourceList } from '@/api'

const router = useRouter()
const route = useRoute()
const isNew = computed(() => !route.params.id)

const formRef = ref(null)
const saving = ref(false)
const datasourceList = ref([])

const form = reactive({
  id: null,
  name: '',
  path: '',
  method: 'GET',
  datasourceId: null,
  sqlTemplate: '',
  paramsConfig: '',
  authType: 'TOKEN',
  rateLimit: 100,
  timeout: 30,
  description: '',
  paramList: [],
  status: 'OFFLINE'
})

const rules = {
  name: [{ required: true, message: '请输入API名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入请求路径', trigger: 'blur' }],
  datasourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  sqlTemplate: [{ required: true, message: '请输入SQL模板', trigger: 'blur' }]
}

const testDialogVisible = ref(false)
const testInputParams = ref('')
const testOutput = ref(null)
const testDuration = ref(0)
const runningTest = ref(false)

const detectedParams = computed(() => {
  const sql = form.sqlTemplate || ''
  const matches = sql.match(/\$\{(\w+)\}/g) || []
  return [...new Set(matches.map(m => m.slice(2, -1)))]
})

const exampleUrl = computed(() => {
  const base = window.location.origin
  const path = form.path ? `/data-api${form.path}` : '/data-api/{path}'
  return `${base}${path}`
})

onMounted(async () => {
  await fetchDatasources()
  if (!isNew.value) {
    await loadApiService()
  }
})

const fetchDatasources = async () => {
  try {
    const res = await getDatasourceList()
    datasourceList.value = res.data || []
  } catch (e) {
    console.error('获取数据源失败:', e)
  }
}

const loadApiService = async () => {
  try {
    const res = await apiServiceAPI.get(route.params.id)
    Object.assign(form, res.data)
    if (form.paramsConfig) {
      try {
        form.paramList = JSON.parse(form.paramsConfig)
      } catch (e) {
        form.paramList = []
      }
    }
  } catch (e) {
    ElMessage.error('加载API服务失败')
    goBack()
  }
}

const goBack = () => router.push('/api-service')

const handleSave = async () => {
  try {
    await formRef.value.validate()
    form.paramsConfig = JSON.stringify(form.paramList)
    saving.value = true
    if (isNew.value) {
      await apiServiceAPI.create(form)
    } else {
      await apiServiceAPI.update(form.id, form)
    }
    ElMessage.success('保存成功')
    goBack()
  } catch (e) {
    if (e !== false) ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const addParam = () => {
  form.paramList.push({ name: '', type: 'string', required: false, defaultValue: '', description: '' })
}

const removeParam = (index) => {
  form.paramList.splice(index, 1)
}

const insertParam = () => {
  form.sqlTemplate += ' ${}'
}

const insertSQL = (keyword) => {
  form.sqlTemplate += '\n' + keyword
}

const formatSQL = () => {
  form.sqlTemplate = form.sqlTemplate
    .replace(/\s+/g, ' ')
    .replace(/FROM /gi, '\nFROM ')
    .replace(/WHERE /gi, '\nWHERE ')
    .replace(/ORDER BY /gi, '\nORDER BY ')
    .replace(/GROUP BY /gi, '\nGROUP BY ')
    .replace(/JOIN /gi, '\nJOIN ')
    .replace(/LEFT JOIN /gi, '\nLEFT JOIN ')
    .trim()
}

const syncParamsFromSQL = () => {
  const existingNames = new Set(form.paramList.map(p => p.name))
  detectedParams.value.forEach(name => {
    if (!existingNames.has(name)) {
      form.paramList.push({ name, type: 'string', required: false, defaultValue: '', description: '自动检测' })
    }
  })
  ElMessage.success(`已同步 ${detectedParams.value.length} 个参数`)
}

const handlePreview = () => {
  ElMessage.info(form.sqlTemplate || 'SQL模板为空')
}

const handleCopySQL = () => {
  if (form.sqlTemplate) {
    navigator.clipboard.writeText(form.sqlTemplate).then(() => {
      ElMessage.success('SQL已复制到剪贴板')
    })
  } else {
    ElMessage.warning('SQL模板为空')
  }
}

const handleTest = () => {
  testInputParams.value = '{\n  \n}'
  testOutput.value = null
  testDialogVisible.value = true
}

const runTest = async () => {
  runningTest.value = true
  const startTime = Date.now()
  try {
    const params = testInputParams.value ? JSON.parse(testInputParams.value) : {}
    await new Promise(resolve => setTimeout(resolve, 800))
    testOutput.value = {
      code: 200,
      data: [{ id: 1, name: '示例数据1' }, { id: 2, name: '示例数据2' }],
      total: 2,
      message: 'success',
      params: params
    }
    testDuration.value = Date.now() - startTime
  } catch (e) {
    testOutput.value = { code: 400, message: 'JSON参数格式错误' }
    testDuration.value = Date.now() - startTime
  } finally {
    runningTest.value = false
  }
}
</script>

<style lang="scss" scoped>
.api-editor-page {
  background: #f5f7fa;
  min-height: 100%;

  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #f0f0f0;
    position: sticky;
    top: 0;
    z-index: 10;

    .header-center {
      display: flex;
      align-items: center;
      gap: 12px;

      .title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  .editor-content {
    padding: 20px 24px;

    .main-card, .side-card {
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
      margin-bottom: 20px;
    }

    .side-card {
      .side-title { font-size: 15px; font-weight: 600; color: #303133; }
    }
  }

  .sql-editor {
    width: 100%;

    .sql-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      padding: 8px 12px;
      background: #f8f9fa;
      border: 1px solid #e8e8e8;
      border-radius: 6px 6px 0 0;

      .sql-inserts {
        display: flex;
        gap: 4px;
        flex-wrap: wrap;
      }
    }

    .sql-textarea {
      :deep(.el-textarea__inner) {
        font-family: 'Fira Code', 'Monaco', monospace;
        font-size: 13px;
        line-height: 1.6;
        border-radius: 0 0 6px 6px;
        background: #fff;
      }
    }

    .sql-help {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 8px;
      font-size: 12px;
      color: #909399;

      .sql-params-hint {
        display: flex;
        align-items: center;
      }
    }
  }

  .param-def-section {
    .param-actions {
      display: flex;
      gap: 8px;
      margin-top: 8px;
    }
  }

  .sql-preview {
    background: #1e1e2e;
    color: #cdd6f4;
    padding: 16px;
    border-radius: 8px;
    font-family: 'Fira Code', 'Monaco', monospace;
    font-size: 13px;
    line-height: 1.5;
    min-height: 80px;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .api-example {
    .example-item {
      margin-bottom: 16px;

      .example-label {
        display: block;
        font-size: 13px;
        color: #606266;
        margin-bottom: 6px;
        font-weight: 500;
      }

      code {
        background: #f5f7fa;
        padding: 6px 10px;
        border-radius: 4px;
        font-family: monospace;
        font-size: 12px;
        word-break: break-all;
        display: inline-block;
      }

      .example-json {
        background: #f5f7fa;
        padding: 10px 14px;
        border-radius: 6px;
        font-size: 12px;
        margin: 0;
        line-height: 1.5;
      }
    }
  }

  .quick-actions {
    padding: 4px 0;
  }

  .test-content {
    .test-url {
      background: #f5f7fa;
      padding: 10px 14px;
      border-radius: 6px;
      font-size: 13px;
      font-family: monospace;
      margin-bottom: 16px;
      word-break: break-all;
    }

    .test-output {
      margin-top: 16px;

      .output-header {
        font-weight: 500;
        color: #303133;
        margin-bottom: 8px;
      }

      pre {
        background: #1e1e2e;
        color: #cdd6f4;
        padding: 16px;
        border-radius: 8px;
        overflow-x: auto;
        font-size: 13px;
        line-height: 1.5;
      }
    }
  }
}

@media (max-width: 768px) {
  .api-editor-page {
    .editor-header { padding: 12px 16px; flex-wrap: wrap; gap: 8px; }
    .editor-content { padding: 16px; }
  }
}
</style>
