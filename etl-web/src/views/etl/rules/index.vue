<template>
  <div class="rules-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Document /></el-icon>
            转换规则库
          </span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索规则..."
            clearable
            style="width: 300px"
            @input="filterRules"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <!-- 规则分类导航 -->
      <div class="category-tabs">
        <el-radio-group v-model="currentCategory" size="large">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="basic">基础转换</el-radio-button>
          <el-radio-button label="string">字符串处理</el-radio-button>
          <el-radio-button label="security">加密安全</el-radio-button>
          <el-radio-button label="json">JSON处理</el-radio-button>
          <el-radio-button label="aggregate">聚合计算</el-radio-button>
          <el-radio-button label="advanced">高级规则</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 规则卡片网格 -->
      <div class="rules-grid" v-loading="loading">
        <div
          v-for="rule in filteredRules"
          :key="rule.id"
          class="rule-card"
          @click="showRuleDetail(rule)"
        >
          <div class="card-icon">{{ rule.icon }}</div>
          <div class="card-content">
            <div class="rule-name">{{ rule.name }}</div>
            <div class="rule-desc">{{ rule.description }}</div>
            <div class="rule-tags">
              <el-tag size="small" :type="getCategoryType(rule.category)">{{ getCategoryName(rule.category) }}</el-tag>
              <el-tag v-if="rule.hot" size="small" type="danger">热</el-tag>
              <el-tag v-if="rule.new" size="small" type="success">新</el-tag>
            </div>
          </div>
          <div class="card-footer">
            <el-button size="small" type="primary">使用</el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredRules.length === 0 && !loading" class="empty-rules">
        <el-empty description="暂无符合条件的规则" />
      </div>
    </el-card>

    <!-- 规则详情对话框 -->
    <el-dialog v-model="detailVisible" :title="selectedRule.name" width="700px" class="dark-dialog">
      <div class="rule-detail">
        <div class="detail-header">
          <div class="rule-icon-large">{{ selectedRule.icon }}</div>
          <div class="rule-info">
            <div class="rule-name">{{ selectedRule.name }}</div>
            <div class="rule-desc">{{ selectedRule.description }}</div>
            <div class="rule-tags">
              <el-tag size="small" :type="getCategoryType(selectedRule.category)">{{ getCategoryName(selectedRule.category) }}</el-tag>
              <el-tag size="small" type="info">版本 {{ selectedRule.version }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider>功能说明</el-divider>
        <div class="detail-section">
          {{ selectedRule.features }}
        </div>

        <el-divider>适用场景</el-divider>
        <div class="detail-section">
          {{ selectedRule.scenarios }}
        </div>

        <el-divider>配置示例</el-divider>
        <div class="detail-section">
          <el-input
            :model-value="JSON.stringify(selectedRule.example, null, 2)"
            type="textarea"
            :rows="8"
            disabled
            monospace
          />
        </div>

        <el-divider>使用说明</el-divider>
        <div class="detail-section">
          {{ selectedRule.usage }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const searchKeyword = ref('')
const currentCategory = ref('all')
const detailVisible = ref(false)
const selectedRule = ref({})

// 规则数据
const rulesList = ref([])

// 模拟数据
const mockRules = [
  // 基础转换
  { id: 'field_mapping', name: '字段映射', description: '将源字段值直接映射到目标字段', category: 'basic', icon: '🔄', hot: true, version: '1.2.0', features: '字段值直接映射', scenarios: '字段名称变更但内容一致的场景', usage: '直接配置源字段和目标字段即可', example: { sourceField: 'user_name', targetField: 'full_name' } },
  { id: 'field_rename', name: '字段重命名', description: '重命名字段但保持值不变', category: 'basic', icon: '✏️', hot: false, version: '1.1.0', features: '字段重命名', scenarios: '字段命名规范调整', usage: '配置旧字段名和新字段名', example: { oldName: 'user_name', newName: 'full_name' } },
  { id: 'field_remove', name: '字段移除', description: '从数据中移除指定字段', category: 'basic', icon: '❌', hot: false, version: '1.0.0', features: '字段移除', scenarios: '需要去除敏感或无用字段', usage: '配置需要移除的字段名', example: { fields: ['password', 'credit_card'] } },
  { id: 'field_add', name: '字段添加', description: '向数据中添加新字段', category: 'basic', icon: '➕', hot: false, version: '1.0.0', features: '字段添加', scenarios: '需要补充新字段', usage: '配置字段名和默认值', example: { field: 'status', defaultValue: 'active' } },
  { id: 'constant', name: '常量值', description: '为字段设置固定值', category: 'basic', icon: '📌', hot: false, version: '1.0.0', features: '字段设置固定值', scenarios: '需要统一设置的字段', usage: '配置字段名和固定值', example: { field: 'created_by', value: 'system' } },

  // 字符串处理
  { id: 'upper', name: '转大写', description: '将字符串转换为大写', category: 'string', icon: 'ABC', hot: false, version: '1.0.0', features: '字符串转大写', scenarios: '需要统一大小写的字段', usage: '配置源字段', example: { source: 'name' } },
  { id: 'lower', name: '转小写', description: '将字符串转换为小写', category: 'string', icon: 'abc', hot: false, version: '1.0.0', features: '字符串转小写', scenarios: '需要统一大小写的字段', usage: '配置源字段', example: { source: 'email' } },
  { id: 'trim', name: '去空格', description: '去除字符串两端的空格', category: 'string', icon: '✂️', hot: true, version: '1.1.0', features: '去除字符串两端空格', scenarios: '用户输入规范化', usage: '配置源字段', example: { source: 'username' } },
  { id: 'regex', name: '正则替换', description: '使用正则表达式进行复杂替换', category: 'string', icon: '🔍', hot: false, version: '1.3.0', features: '正则表达式替换', scenarios: '复杂字符串处理', usage: '配置源字段、正则表达式和替换内容', example: { source: 'phone', pattern: '(\\d{3})\\d{4}(\\d{4})', replacement: '$1****$2' } },

  // 加密安全
  { id: 'encrypt', name: '数据加密', description: '对敏感数据进行加密处理', category: 'security', icon: '🔐', hot: true, version: '2.0.0', features: '数据加密', scenarios: '存储或传输敏感数据', usage: '配置加密算法和密钥', example: { field: 'id_card', algorithm: 'AES', key: '********' } },
  { id: 'decrypt', name: '数据解密', description: '对加密数据进行解密', category: 'security', icon: '🔓', hot: false, version: '2.0.0', features: '数据解密', scenarios: '需要使用加密数据时', usage: '配置解密算法和密钥', example: { field: 'id_card', algorithm: 'AES', key: '********' } },
  { id: 'mask', name: '数据脱敏', description: '对敏感数据进行脱敏处理', category: 'security', icon: '🎭', hot: true, version: '1.5.0', features: '数据脱敏', scenarios: '展示敏感数据时', usage: '配置脱敏规则', example: { field: 'phone', mask: '138****1234' } },

  // JSON处理
  { id: 'json_parse', name: 'JSON解析', description: '解析JSON字符串为对象', category: 'json', icon: '📄', hot: true, version: '1.2.0', features: 'JSON字符串解析', scenarios: '处理JSON格式字段', usage: '配置源字段和目标字段', example: { source: 'json_data', target: 'parsed_data' } },
  { id: 'json_extract', name: 'JSON提取', description: '从JSON中提取特定字段', category: 'json', icon: '📤', hot: false, version: '1.1.0', features: 'JSON字段提取', scenarios: '需要提取JSON内部字段', usage: '配置源字段和JSON路径', example: { source: 'data', path: '$.user.name' } },
  { id: 'json_expand', name: 'JSON展开', description: '将JSON对象展开为字段', category: 'json', icon: '📣', hot: false, version: '1.1.0', features: 'JSON对象展开', scenarios: '将JSON扁平化', usage: '配置源字段', example: { source: 'user_info' } },

  // 聚合计算
  { id: 'sum', name: '求和', description: '对数值字段求和', category: 'aggregate', icon: '➕', hot: true, version: '1.0.0', features: '数值求和', scenarios: '统计场景', usage: '配置求和字段', example: { field: 'amount' } },
  { id: 'avg', name: '平均值', description: '计算平均值', category: 'aggregate', icon: '📐', hot: true, version: '1.0.0', features: '平均值计算', scenarios: '统计场景', usage: '配置字段', example: { field: 'score' } },
  { id: 'count', name: '计数', description: '统计记录数', category: 'aggregate', icon: '🔢', hot: true, version: '1.0.0', features: '记录计数', scenarios: '统计场景', usage: '无配置', example: {} },
  { id: 'groupby', name: '分组聚合', description: '按字段分组并聚合', category: 'aggregate', icon: '📦', hot: false, version: '1.1.0', features: '分组聚合', scenarios: '分组统计', usage: '配置分组字段', example: { group: 'category', aggregates: [{ field: 'amount', type: 'sum' }] } },

  // 高级规则
  { id: 'lookup', name: '查找表', description: '通过字典进行值映射', category: 'advanced', icon: '🔎', hot: true, version: '1.3.0', features: '字典值映射', scenarios: '需要将编码转换为含义', usage: '配置源字段和字典', example: { source: 'status', mapping: { '0': '禁用', '1': '启用' } } },
  { id: 'condition', name: '条件分支', description: '根据条件进行分支处理', category: 'advanced', icon: '🔀', hot: true, version: '1.4.0', features: '条件分支', scenarios: '复杂逻辑判断', usage: '配置条件和处理规则', example: { condition: 'age > 18', then: 'adult', else: 'minor' } },
  { id: 'date', name: '日期处理', description: '日期格式化和计算', category: 'advanced', icon: '📅', hot: true, version: '1.2.0', features: '日期处理', scenarios: '日期格式化', usage: '配置源字段和格式', example: { source: 'create_time', format: 'YYYY-MM-DD' } },
  { id: 'type_infer', name: '类型推断', description: '自动推断字段类型', category: 'advanced', icon: '🎯', hot: true, version: '1.1.0', features: '类型自动推断', scenarios: '数据导入时', usage: '配置字段', example: { fields: ['id', 'name', 'age'] } }
]

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    rulesList.value = mockRules
    loading.value = false
  }, 500)
})

// 过滤后的规则
const filteredRules = computed(() => {
  let rules = rulesList.value

  // 分类过滤
  if (currentCategory.value !== 'all') {
    rules = rules.filter(rule => rule.category === currentCategory.value)
  }

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    rules = rules.filter(rule =>
      rule.name.toLowerCase().includes(keyword) ||
      rule.description.toLowerCase().includes(keyword)
    )
  }

  return rules
})

const getCategoryType = (category) => {
  const types = {
    all: 'info',
    basic: 'primary',
    string: 'success',
    security: 'danger',
    json: 'warning',
    aggregate: 'info',
    advanced: 'info'
  }
  return types[category] || 'info'
}

const getCategoryName = (category) => {
  const names = {
    all: '全部',
    basic: '基础转换',
    string: '字符串处理',
    security: '加密安全',
    json: 'JSON处理',
    aggregate: '聚合计算',
    advanced: '高级规则'
  }
  return names[category] || category
}

const filterRules = () => {
  // 计算属性已经处理
}

const showRuleDetail = (rule) => {
  selectedRule.value = rule
  detailVisible.value = true
}
</script>

<style lang="scss" scoped>
.rules-page {
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

  .category-tabs {
    margin: 24px 0;
  }

  .rules-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
    margin-top: 24px;
  }

  .rule-card {
    background: #fff;
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 20px;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    flex-direction: column;

    &:hover {
      border-color: var(--primary-color);
      box-shadow: 0 8px 16px rgba(79, 110, 247, 0.15);
      transform: translateY(-2px);
    }

    .card-icon {
      font-size: 40px;
      text-align: center;
      margin-bottom: 12px;
    }

    .card-content {
      flex: 1;
    }

    .rule-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 8px;
    }

    .rule-desc {
      font-size: 14px;
      color: var(--text-secondary);
      line-height: 1.5;
      margin-bottom: 12px;
    }

    .rule-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .card-footer {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid var(--border-color);
      display: flex;
      justify-content: flex-end;
    }
  }

  .empty-rules {
    padding: 40px 0;
    text-align: center;
  }

  .rule-detail {
    .detail-header {
      display: flex;
      gap: 20px;
      margin-bottom: 24px;

      .rule-icon-large {
        font-size: 60px;
      }

      .rule-info {
        flex: 1;

        .rule-name {
          font-size: 20px;
          font-weight: 600;
          color: var(--text-primary);
          margin-bottom: 8px;
        }

        .rule-desc {
          font-size: 14px;
          color: var(--text-secondary);
          margin-bottom: 8px;
        }
      }
    }

    .detail-section {
      font-size: 14px;
      color: var(--text-primary);
      line-height: 1.6;
    }
  }
}
</style>
