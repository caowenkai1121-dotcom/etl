<template>
  <div class="task-templates-page">
    <div class="page-header">
      <h2>任务模板</h2>
      <p class="subtitle">选择合适的模板快速创建ETL任务</p>
    </div>

    <el-tabs v-model="activeTab" class="templates-tabs">
      <el-tab-pane label="全部模板" name="all" />
      <el-tab-pane label="数据同步" name="sync" />
      <el-tab-pane label="数据转换" name="transform" />
      <el-tab-pane label="数据质量" name="quality" />
    </el-tabs>

    <div class="template-grid">
      <div
        v-for="tpl in filteredTemplates"
        :key="tpl.id"
        class="template-card"
        @click="useTemplate(tpl)"
      >
        <div class="template-icon" :style="{ background: tpl.color }">
          <el-icon :size="28"><component :is="tpl.icon" /></el-icon>
        </div>
        <div class="template-info">
          <h4 class="template-name">{{ tpl.name }}</h4>
          <p class="template-desc">{{ tpl.description }}</p>
          <div class="template-meta">
            <el-tag size="small" :type="tpl.categoryTag">{{ tpl.category }}</el-tag>
            <span class="template-usage">{{ tpl.usageCount }} 次使用</span>
          </div>
        </div>
        <div class="template-action">
          <el-button type="primary" size="small" round>使用模板</el-button>
        </div>
      </div>
      <div v-if="filteredTemplates.length === 0" class="empty-templates">
        <el-empty description="暂无匹配的模板" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('all')

const templates = ref([
  {
    id: 1, name: 'MySQL → MySQL 全量同步', description: '将源MySQL数据库全量数据同步到目标MySQL数据库，支持自动建表',
    category: '数据同步', categoryTag: 'primary', icon: 'Connection', color: '#e6f7ff',
    usageCount: 156
  },
  {
    id: 2, name: 'MySQL → MySQL 增量同步', description: '基于binlog的CDC增量同步，实时捕获数据变更并同步到目标库',
    category: '数据同步', categoryTag: 'success', icon: 'Odometer', color: '#f6ffed',
    usageCount: 203
  },
  {
    id: 3, name: '数据清洗与标准化', description: '对原始数据进行清洗、格式标准化、去重等ETL转换操作',
    category: '数据转换', categoryTag: 'warning', icon: 'Brush', color: '#fff7e6',
    usageCount: 89
  },
  {
    id: 4, name: 'JSON解析与扁平化', description: '解析嵌套JSON数据，将复杂结构扁平化为关系型表结构',
    category: '数据转换', categoryTag: 'warning', icon: 'DataAnalysis', color: '#fff7e6',
    usageCount: 67
  },
  {
    id: 5, name: '数据质量监控', description: '对数据源执行数据质量检查：完整性、唯一性、一致性、准确性',
    category: '数据质量', categoryTag: 'danger', icon: 'Checked', color: '#fff0f0',
    usageCount: 45
  },
  {
    id: 6, name: 'MySQL → Elasticsearch同步', description: '将MySQL业务数据实时同步到Elasticsearch，支持全文检索场景',
    category: '数据同步', categoryTag: 'primary', icon: 'Search', color: '#e6f7ff',
    usageCount: 128
  },
  {
    id: 7, name: '数据聚合与统计', description: '对原始数据进行分组聚合、窗口计算和统计指标生成',
    category: '数据转换', categoryTag: 'warning', icon: 'TrendCharts', color: '#fff7e6',
    usageCount: 73
  },
  {
    id: 8, name: '表结构同步', description: '自动同步源表结构变更到目标表，包括新增列、修改类型等DDL操作',
    category: '数据同步', categoryTag: 'primary', icon: 'CopyDocument', color: '#e6f7ff',
    usageCount: 41
  }
])

const filteredTemplates = computed(() => {
  if (activeTab.value === 'all') return templates.value
  const catMap = { sync: '数据同步', transform: '数据转换', quality: '数据质量' }
  return templates.value.filter(t => t.category === catMap[activeTab.value])
})

function useTemplate(tpl) {
  ElMessage.success(`已选择模板: ${tpl.name}`)
  router.push('/dev/task/new')
}
</script>

<style scoped>
.task-templates-page {
  padding: 0;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.subtitle {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.templates-tabs {
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 0 20px;
  border: 1px solid #ebeef5;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}
.template-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
}
.template-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: #409eff;
  transform: translateY(-2px);
}
.template-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.template-info {
  flex: 1;
  min-width: 0;
}
.template-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.template-desc {
  margin: 0 0 10px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
.template-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}
.template-usage {
  font-size: 12px;
  color: #c0c4cc;
}
.template-action {
  flex-shrink: 0;
  padding-top: 4px;
}
.empty-templates {
  grid-column: 1 / -1;
  padding: 60px 0;
}
</style>
