<template>
  <el-dialog
    v-model="visible"
    title="版本管理"
    width="700px"
    :close-on-click-modal="false"
  >
    <el-table :data="versions" size="small" style="width: 100%">
      <el-table-column prop="version" label="版本号" width="80" />
      <el-table-column prop="publishedBy" label="发布人" width="100" />
      <el-table-column prop="publishedAt" label="发布时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="changeLog" label="变更说明" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="handleCompare(row)">对比</el-button>
          <el-button type="warning" size="small" text @click="handleRollback(row)">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button size="small" @click="visible = false">关闭</el-button>
    </template>

    <!-- 版本对比对话框 -->
    <el-dialog v-model="compareVisible" title="版本对比" width="800px" append-to-body>
      <div class="compare-panel">
        <div class="compare-side">
          <div class="compare-header">当前版本</div>
          <pre class="compare-content">{{ currentConfigJson }}</pre>
        </div>
        <div class="compare-side">
          <div class="compare-header">目标版本 {{ compareVersion }}</div>
          <pre class="compare-content">{{ targetConfigJson }}</pre>
        </div>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { devAPI } from '@/api/dev'

const props = defineProps({
  visible: { type: Boolean, default: false },
  taskId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:visible'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const versions = ref([])
const compareVisible = ref(false)
const compareVersion = ref('')
const currentConfigJson = ref('')
const targetConfigJson = ref('')

const formatTime = (time) => {
  if (!time) return ''
  try {
    return new Date(time).toLocaleString('zh-CN')
  } catch (e) {
    return String(time)
  }
}

const loadVersions = async () => {
  if (!props.taskId) return
  try {
    const res = await devAPI.getTaskVersions(props.taskId)
    const list = res.data || []
    // 如果没有版本记录，显示发布历史
    if (list.length === 0) {
      const publishRes = await devAPI.getPublishHistory(props.taskId)
      versions.value = (publishRes.data || []).map(v => ({
        version: v.version || 1,
        publishedBy: v.publishedBy || 'admin',
        publishedAt: v.publishedAt,
        changeLog: v.changeLog || '初始版本'
      }))
    } else {
      versions.value = list
    }
  } catch (e) {
    console.error('加载版本失败', e)
    versions.value = []
  }
}

const handleCompare = async (row) => {
  compareVersion.value = row.version
  try {
    const currentRes = await devAPI.getDag(props.taskId)
    currentConfigJson.value = JSON.stringify(currentRes.data, null, 2)

    const targetRes = await devAPI.getDag(props.taskId, row.version)
    targetConfigJson.value = JSON.stringify(targetRes.data, null, 2)

    compareVisible.value = true
  } catch (e) {
    ElMessage.error('加载版本配置失败')
  }
}

const handleRollback = async (row) => {
  try {
    await ElMessageBox.confirm(`确定回滚到版本 ${row.version}？未保存的修改将丢失。`, '回滚确认', { type: 'warning' })
    await devAPI.rollbackTask(props.taskId, row.version)
    ElMessage.success('回滚成功')
    loadVersions()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('回滚失败')
      console.error(e)
    }
  }
}

watch(() => props.visible, (val) => {
  if (val) loadVersions()
})
</script>

<style lang="scss" scoped>
.compare-panel {
  display: flex;
  gap: 16px;
  height: 500px;

  .compare-side {
    flex: 1;
    display: flex;
    flex-direction: column;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
    overflow: hidden;

    .compare-header {
      padding: 8px 12px;
      background: #f5f7fa;
      font-weight: 600;
      font-size: 13px;
      border-bottom: 1px solid #e8e8e8;
    }

    .compare-content {
      flex: 1;
      overflow: auto;
      padding: 12px;
      margin: 0;
      font-size: 12px;
      line-height: 1.5;
      background: #fafafa;
    }
  }
}
</style>
