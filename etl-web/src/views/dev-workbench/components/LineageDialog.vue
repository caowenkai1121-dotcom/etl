<template>
  <el-dialog
    v-model="visible"
    title="数据血缘"
    width="900px"
    :close-on-click-modal="false"
  >
    <el-empty v-if="!loading && lineageList.length === 0" description="暂无血缘数据，请先保存任务" />

    <el-table v-else :data="lineageList" v-loading="loading" size="small" style="width: 100%">
      <el-table-column prop="nodeName" label="节点" width="140" show-overflow-tooltip />
      <el-table-column prop="nodeType" label="类型" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="getNodeTypeTag(row.nodeType)">{{ getNodeTypeLabel(row.nodeType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="数据来源" min-width="180">
        <template #default="{ row }">
          <div v-if="row.sourceTable" class="lineage-cell">
            <el-tag size="small" type="info">{{ row.sourceTable }}</el-tag>
            <span v-if="row.sourceField" class="field-tag">{{ row.sourceField }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="transformLogic" label="转换逻辑" min-width="180" show-overflow-tooltip />
      <el-table-column label="数据目标" min-width="180">
        <template #default="{ row }">
          <div v-if="row.targetTable" class="lineage-cell">
            <el-tag size="small" type="success">{{ row.targetTable }}</el-tag>
            <span v-if="row.targetField" class="field-tag">{{ row.targetField }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button size="small" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import request from '@/api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  taskId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:visible'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const loading = ref(false)
const lineageList = ref([])

const nodeTypeMap = {
  DB_SYNC: '离线同步', API_SYNC: 'API同步', SERVER_DS: '服务器数据集',
  FILE_READ: '文件读取', JIANADAOYUN: '简道云',
  FIELD_SELECT: '字段选择', FIELD_RENAME: '字段重命名', FIELD_CALC: '字段计算',
  DATA_FILTER: '数据过滤', DATA_AGG: '数据聚合', DATA_JOIN: '数据关联',
  DATA_SORT: '数据排序', DATA_DEDUP: '数据去重', FIELD_SPLIT: '字段拆分',
  NULL_HANDLE: '空值处理', JSON_PARSE: 'JSON解析', XML_PARSE: 'XML解析',
  DATA_COMPARE: '数据对比', VISUAL_TRANSFORM: '可视化转换',
  SQL_SCRIPT: 'SQL脚本', PYTHON_SCRIPT: 'Python脚本', SHELL_SCRIPT: 'Shell脚本',
  BAT_SCRIPT: 'Bat脚本',
  CONDITION: '条件分支', PARAM_ASSIGN: '参数赋值', CALL_TASK: '调用任务',
  LOOP_CONTAINER: '循环容器', MESSAGE_NOTIFY: '消息通知',
  VIRTUAL_NODE: '虚拟节点', NOTE: '备注',
  FTP_UPLOAD: 'FTP上传', SFTP: 'SFTP', LOCAL_FILE: '本地文件',
  DB_OUTPUT: '数据库输出', API_OUTPUT: 'API输出'
}

const getNodeTypeLabel = (type) => nodeTypeMap[type] || type

const getNodeTypeTag = (type) => {
  if (['DB_SYNC', 'API_SYNC', 'FILE_READ', 'SERVER_DS'].includes(type)) return 'primary'
  if (['FIELD_SELECT', 'FIELD_RENAME', 'FIELD_CALC', 'DATA_FILTER', 'DATA_AGG', 'DATA_JOIN', 'VISUAL_TRANSFORM'].includes(type)) return 'warning'
  if (['SQL_SCRIPT', 'PYTHON_SCRIPT', 'SHELL_SCRIPT', 'BAT_SCRIPT'].includes(type)) return 'info'
  if (['DB_OUTPUT', 'API_OUTPUT'].includes(type)) return 'success'
  return ''
}

const loadLineage = async () => {
  if (!props.taskId) return
  loading.value = true
  try {
    const res = await request.get(`/lineage/task/${props.taskId}`)
    lineageList.value = res.data || []
  } catch (e) {
    console.error('加载血缘数据失败', e)
    lineageList.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.visible, (val) => {
  if (val) loadLineage()
})
</script>

<style lang="scss" scoped>
.lineage-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.field-tag {
  font-size: 12px;
  color: #666;
}

.text-muted {
  color: #999;
}
</style>
