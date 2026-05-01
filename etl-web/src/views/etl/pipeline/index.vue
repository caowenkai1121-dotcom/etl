<template>
  <div class="pipeline-page page-container">
    <!-- 列表视图 -->
    <div v-if="!showEditor" class="list-view">
      <el-card class="main-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon class="title-icon"><Share /></el-icon>
              转换流水线
            </span>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新建流水线
            </el-button>
          </div>
        </template>

        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-form :inline="true" :model="queryParams">
            <el-form-item label="名称">
              <el-input v-model="queryParams.name" placeholder="请输入" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.status" placeholder="请选择" clearable>
                <el-option label="已启用" value="ENABLED" />
                <el-option label="已停用" value="DISABLED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchData">查询</el-button>
              <el-button @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 流水线卡片 -->
        <div class="pipeline-grid">
          <div
            v-for="pipeline in pipelineList"
            :key="pipeline.id"
            class="pipeline-card"
            @click="handleEdit(pipeline)"
          >
            <div class="card-header-row">
              <span class="pipeline-name">{{ pipeline.name }}</span>
              <el-tag :type="pipeline.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ pipeline.status === 'ENABLED' ? '已启用' : '已停用' }}
              </el-tag>
            </div>
            <div class="pipeline-desc">{{ pipeline.description || '暂无描述' }}</div>
            <div class="pipeline-stats">
              <div class="stat-item">
                <span class="stat-value">{{ pipeline.stageCount || 0 }}</span>
                <span class="stat-label">步骤</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">源:</span>
                <span class="stat-value">{{ pipeline.source || '-' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">目标:</span>
                <span class="stat-value">{{ pipeline.target || '-' }}</span>
              </div>
            </div>
            <div class="pipeline-actions">
              <el-button size="small" type="primary" @click.stop="handlePreview(pipeline)">预览</el-button>
              <el-button size="small" type="success" @click.stop="handleExecute(pipeline)">执行</el-button>
              <el-button size="small" type="danger" @click.stop="handleDelete(pipeline)">删除</el-button>
            </div>
          </div>
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
    </div>

    <!-- 编辑器视图 -->
    <div v-else class="editor-view">
      <!-- 顶部工具栏 -->
      <div class="editor-toolbar">
        <div class="toolbar-left">
          <el-button icon="ArrowLeft" @click="handleBack">返回</el-button>
          <el-input v-model="currentPipeline.name" placeholder="流水线名称" class="name-input" />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="handleSave">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
          <el-button type="success" @click="handleExecuteCurrent">
            <el-icon><CaretRight /></el-icon>
            执行预览
          </el-button>
        </div>
      </div>

      <div class="editor-container">
        <!-- 左侧规则面板 -->
        <div class="rule-panel">
          <div class="panel-header">
            <span class="panel-title">转换规则</span>
          </div>
          <div class="rule-categories">
            <div
              v-for="cat in ruleCategories"
              :key="cat.id"
              class="category-item"
              :class="{ active: currentCategory === cat.id }"
              @click="currentCategory = cat.id"
            >
              <span class="category-icon">{{ cat.icon }}</span>
              <span class="category-name">{{ cat.name }}</span>
            </div>
          </div>
          <div class="rule-list">
            <div
              v-for="rule in getRulesByCategory(currentCategory)"
              :key="rule.id"
              class="rule-item"
              draggable="true"
              @dragstart="handleRuleDragStart($event, rule)"
            >
              <span class="rule-icon">{{ rule.icon }}</span>
              <span class="rule-name">{{ rule.name }}</span>
            </div>
          </div>
        </div>

        <!-- 中间画布 -->
        <div class="canvas-container" ref="canvasRef" @dragover.prevent @drop="handleCanvasDrop">
          <div class="pipeline-flow">
            <div
              v-for="(stage, index) in currentPipeline.stages"
              :key="stage.id"
              class="flow-stage"
              :class="{ active: selectedStage === stage }"
              @click="selectedStage = stage"
            >
              <div class="stage-header">
                <span class="stage-number">{{ index + 1 }}</span>
                <span class="stage-name">{{ stage.name }}</span>
                <el-button size="small" type="danger" circle @click.stop="handleRemoveStage(stage)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
              <div class="stage-body">
                <span class="stage-rule">{{ getRuleName(stage.ruleId) }}</span>
              </div>
              <div class="stage-preview" @click.stop="handleStagePreview(stage)">
                <el-icon><View /></el-icon>
                预览
              </div>
            </div>
          </div>
          <div v-if="currentPipeline.stages.length === 0" class="empty-canvas">
            <el-empty description="将左侧规则拖放到此处" />
          </div>
        </div>

        <!-- 右侧属性面板 -->
        <div class="property-panel">
          <div class="panel-header">
            <span class="panel-title">步骤配置</span>
          </div>
          <div class="property-content">
            <div v-if="selectedStage" class="stage-properties">
              <el-form label-width="100px" size="small">
                <el-form-item label="步骤名称">
                  <el-input v-model="selectedStage.name" placeholder="请输入" />
                </el-form-item>
                <el-form-item label="规则类型">
                  <el-select v-model="selectedStage.ruleId" placeholder="请选择" style="width: 100%">
                    <el-option
                      v-for="rule in allRules"
                      :key="rule.id"
                      :label="rule.name"
                      :value="rule.id"
                    />
                  </el-select>
                </el-form-item>

                <!-- 动态规则配置 -->
                <el-divider>规则配置</el-divider>
                <div v-for="config in getRuleConfig(selectedStage.ruleId)" :key="config.key">
                  <el-form-item :label="config.label">
                    <el-input
                      v-if="config.type === 'input'"
                      v-model="selectedStage.config[config.key]"
                      :placeholder="config.placeholder"
                    />
                    <el-select
                      v-if="config.type === 'select'"
                      v-model="selectedStage.config[config.key]"
                      placeholder="请选择"
                      style="width: 100%"
                    >
                      <el-option
                        v-for="opt in config.options"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </el-form-item>
                </div>
              </el-form>
            </div>
            <div v-else class="no-selection">
              <el-empty description="请选择一个步骤" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" title="数据预览" width="1000px" class="dark-dialog">
      <div class="preview-content">
        <div class="preview-tabs">
          <el-radio-group v-model="previewMode">
            <el-radio-button value="input">输入数据</el-radio-button>
            <el-radio-button value="output">输出数据</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="previewData" stripe class="preview-table">
          <el-table-column
            v-for="col in previewColumns"
            :key="col"
            :prop="col"
            :label="col"
            show-overflow-tooltip
          />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share, Plus, ArrowLeft, Check, CaretRight, Close, View } from '@element-plus/icons-vue'

// 状态
const showEditor = ref(false)
const loading = ref(false)
const pipelineList = ref([])
const total = ref(0)
const selectedStage = ref(null)
const previewVisible = ref(false)
const previewMode = ref('input')
const previewData = ref([])
const previewColumns = ref([])
const currentCategory = ref('basic')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 12,
  name: '',
  status: ''
})

const currentPipeline = reactive({
  id: null,
  name: '',
  description: '',
  status: 'ENABLED',
  stages: []
})

// 规则分类
const ruleCategories = [
  { id: 'basic', name: '基础转换', icon: '📋' },
  { id: 'string', name: '字符串处理', icon: '📝' },
  { id: 'security', name: '加密安全', icon: '🔒' },
  { id: 'json', name: 'JSON处理', icon: '🔧' },
  { id: 'aggregate', name: '聚合计算', icon: '📊' },
  { id: 'advanced', name: '高级规则', icon: '🚀' }
]

// 规则列表
const allRules = [
  { id: 'field_mapping', name: '字段映射', category: 'basic', icon: '🔄', config: [] },
  { id: 'field_rename', name: '字段重命名', category: 'basic', icon: '✏️', config: [] },
  { id: 'field_remove', name: '字段移除', category: 'basic', icon: '❌', config: [] },
  { id: 'field_add', name: '字段添加', category: 'basic', icon: '➕', config: [] },
  { id: 'constant', name: '常量值', category: 'basic', icon: '📌', config: [] },
  { id: 'upper', name: '转大写', category: 'string', icon: 'ABC', config: [] },
  { id: 'lower', name: '转小写', category: 'string', icon: 'abc', config: [] },
  { id: 'trim', name: '去空格', category: 'string', icon: '✂️', config: [] },
  { id: 'regex', name: '正则替换', category: 'string', icon: '🔍', config: [] },
  { id: 'encrypt', name: '数据加密', category: 'security', icon: '🔐', config: [] },
  { id: 'decrypt', name: '数据解密', category: 'security', icon: '🔓', config: [] },
  { id: 'mask', name: '数据脱敏', category: 'security', icon: '🎭', config: [] },
  { id: 'json_parse', name: 'JSON解析', category: 'json', icon: '📄', config: [] },
  { id: 'json_extract', name: 'JSON提取', category: 'json', icon: '📤', config: [] },
  { id: 'json_expand', name: 'JSON展开', category: 'json', icon: '📣', config: [] },
  { id: 'sum', name: '求和', category: 'aggregate', icon: '➕', config: [] },
  { id: 'avg', name: '平均值', category: 'aggregate', icon: '📐', config: [] },
  { id: 'count', name: '计数', category: 'aggregate', icon: '🔢', config: [] },
  { id: 'groupby', name: '分组聚合', category: 'aggregate', icon: '📦', config: [] },
  { id: 'lookup', name: '查找表', category: 'advanced', icon: '🔎', config: [] },
  { id: 'condition', name: '条件分支', category: 'advanced', icon: '🔀', config: [] },
  { id: 'date', name: '日期处理', category: 'advanced', icon: '📅', config: [] },
  { id: 'type_infer', name: '类型推断', category: 'advanced', icon: '🎯', config: [] }
]

// 模拟数据
const mockPipelines = [
  { id: 1, name: '用户数据清洗', description: '清洗用户数据，脱敏手机号和邮箱', status: 'ENABLED', stageCount: 4, source: 'MySQL', target: 'PostgreSQL' },
  { id: 2, name: '订单数据转换', description: '订单数据格式转换', status: 'ENABLED', stageCount: 3, source: 'MySQL', target: 'Elasticsearch' },
  { id: 3, name: '日志数据处理', description: '日志数据解析和处理', status: 'DISABLED', stageCount: 5, source: 'Kafka', target: 'ClickHouse' },
  { id: 4, name: '产品数据同步', description: '产品数据转换和同步', status: 'ENABLED', stageCount: 2, source: 'PostgreSQL', target: 'MongoDB' }
]

onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    pipelineList.value = mockPipelines
    total.value = mockPipelines.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.name = ''
  queryParams.status = ''
  fetchData()
}

const handleCreate = () => {
  currentPipeline.id = null
  currentPipeline.name = ''
  currentPipeline.description = ''
  currentPipeline.stages = []
  selectedStage.value = null
  showEditor.value = true
}

const handleEdit = (pipeline) => {
  currentPipeline.id = pipeline.id
  currentPipeline.name = pipeline.name
  currentPipeline.description = pipeline.description
  currentPipeline.stages = [
    { id: 's1', name: '字段映射', ruleId: 'field_mapping', config: {} },
    { id: 's2', name: '数据脱敏', ruleId: 'mask', config: {} },
    { id: 's3', name: '格式转换', ruleId: 'field_rename', config: {} }
  ]
  showEditor.value = true
}

const handleDelete = async (pipeline) => {
  try {
    await ElMessageBox.confirm('确定删除该流水线？', '提示', { type: 'warning' })
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

const handlePreview = (pipeline) => {
  previewVisible.value = true
  previewData.value = [
    { id: 1, name: '张三', phone: '138****1234', email: 'zh***@example.com' },
    { id: 2, name: '李四', phone: '139****5678', email: 'li***@example.com' }
  ]
  previewColumns.value = ['id', 'name', 'phone', 'email']
}

const handleExecute = (pipeline) => {
  ElMessage.success('开始执行: ' + pipeline.name)
}

const handleBack = () => {
  showEditor.value = false
  fetchData()
}

const handleSave = async () => {
  try {
    await ElMessageBox.confirm('确定保存该流水线？', '提示', { type: 'info' })
    ElMessage.success('保存成功')
  } catch (e) {}
}

const handleExecuteCurrent = () => {
  ElMessage.success('开始执行预览')
}

const getRulesByCategory = (category) => {
  return allRules.filter(rule => rule.category === category)
}

const getRuleName = (ruleId) => {
  const rule = allRules.find(r => r.id === ruleId)
  return rule ? rule.name : '-'
}

const getRuleConfig = (ruleId) => {
  return [
    { key: 'source', label: '源字段', type: 'input', placeholder: '请输入' },
    { key: 'target', label: '目标字段', type: 'input', placeholder: '请输入' }
  ]
}

const handleRuleDragStart = (e, rule) => {
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('rule', JSON.stringify(rule))
}

const handleCanvasDrop = (e) => {
  const data = e.dataTransfer.getData('rule')
  if (data) {
    const rule = JSON.parse(data)
    addStage(rule)
  }
}

const addStage = (rule) => {
  const newStage = {
    id: 's' + Date.now(),
    name: rule.name,
    ruleId: rule.id,
    config: {}
  }
  currentPipeline.stages.push(newStage)
  selectedStage.value = newStage
}

const handleRemoveStage = (stage) => {
  const index = currentPipeline.stages.findIndex(s => s.id === stage.id)
  if (index !== -1) {
    currentPipeline.stages.splice(index, 1)
  }
  if (selectedStage.value === stage) {
    selectedStage.value = null
  }
}

const handleStagePreview = (stage) => {
  previewVisible.value = true
  previewData.value = [
    { id: 1, name: '张三', phone: '138****1234' },
    { id: 2, name: '李四', phone: '139****5678' }
  ]
  previewColumns.value = ['id', 'name', 'phone']
}
</script>

<style lang="scss" scoped>
.pipeline-page {
  // 列表视图样式
  .list-view {
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

    .pipeline-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;

      .pipeline-card {
        padding: 20px;
        background: #fff;
        border: 1px solid var(--border-color);
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          border-color: var(--primary-color);
          box-shadow: 0 4px 12px rgba(79, 110, 247, 0.15);
          transform: translateY(-2px);
        }

        .card-header-row {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .pipeline-name {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-primary);
          }
        }

        .pipeline-desc {
          font-size: 14px;
          color: var(--text-secondary);
          margin-bottom: 16px;
        }

        .pipeline-stats {
          display: flex;
          gap: 24px;
          padding-top: 16px;
          border-top: 1px solid var(--border-color);

          .stat-item {
            .stat-value {
              font-size: 20px;
              font-weight: 700;
              color: var(--primary-color);
            }

            .stat-label {
              font-size: 12px;
              color: var(--text-muted);
            }
          }
        }

        .pipeline-actions {
          display: flex;
          gap: 8px;
          margin-top: 16px;
          padding-top: 16px;
          border-top: 1px solid var(--border-color);
        }
      }
    }

    .pagination-wrap {
      margin-top: 24px;
      display: flex;
      justify-content: flex-end;
    }
  }

  // 编辑器视图样式
  .editor-view {
    .editor-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      background: #fff;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      margin-bottom: 16px;

      .toolbar-left {
        display: flex;
        align-items: center;
        gap: 16px;

        .name-input {
          width: 320px;
        }
      }

      .toolbar-right {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .editor-container {
      display: flex;
      gap: 8px;
      height: calc(100vh - 200px);

      .rule-panel {
        width: 220px;
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

        .rule-categories {
          display: flex;
          flex-direction: column;
          padding: 8px;
          border-bottom: 1px solid var(--border-color);

          .category-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 12px;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              background: rgba(79, 110, 247, 0.05);
            }

            &.active {
              background: rgba(79, 110, 247, 0.1);
            }
          }
        }

        .rule-list {
          flex: 1;
          padding: 8px;
          overflow-y: auto;

          .rule-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 12px;
            background: rgba(79, 110, 247, 0.05);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            cursor: grab;
            margin-bottom: 8px;
            transition: all 0.2s;

            &:hover {
              border-color: var(--primary-color);
              background: rgba(79, 110, 247, 0.1);
            }
          }
        }
      }

      .canvas-container {
        flex: 1;
        background: #f8f9fa;
        border: 1px solid var(--border-color);
        border-radius: 12px;
        padding: 32px;
        overflow-y: auto;

        .pipeline-flow {
          display: flex;
          flex-direction: column;
          gap: 16px;

          .flow-stage {
            position: relative;
            background: #fff;
            border: 2px solid var(--border-color);
            border-radius: 12px;
            padding: 16px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
              border-color: var(--primary-color);
            }

            &.active {
              border-color: var(--primary-color);
              box-shadow: 0 0 0 3px rgba(79, 110, 247, 0.2);
            }

            .stage-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 8px;

              .stage-number {
                width: 28px;
                height: 28px;
                border-radius: 50%;
                background: linear-gradient(135deg, #4f6ef7, #6e88ff);
                color: #fff;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 600;
                font-size: 14px;
              }

              .stage-name {
                flex: 1;
                margin-left: 12px;
                font-weight: 600;
                color: var(--text-primary);
              }
            }

            .stage-body {
              padding-left: 40px;
              color: var(--text-secondary);
              font-size: 14px;
            }

            .stage-preview {
              position: absolute;
              right: 56px;
              top: 50%;
              transform: translateY(-50%);
              display: flex;
              align-items: center;
              gap: 4px;
              padding: 6px 12px;
              background: rgba(79, 110, 247, 0.1);
              color: var(--primary-color);
              border-radius: 6px;
              font-size: 12px;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                background: var(--primary-color);
                color: #fff;
              }
            }
          }
        }

        .empty-canvas {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 100%;
        }
      }

      .property-panel {
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

        .property-content {
          flex: 1;
          padding: 16px;
          overflow-y: auto;
        }
      }
    }
  }

  .preview-content {
    .preview-tabs {
      margin-bottom: 16px;
    }

    .preview-table {
      border-radius: 8px;
    }
  }
}
</style>
