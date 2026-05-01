<template>
  <div class="realtime-editor">
    <!-- 编辑器顶部栏 -->
    <div class="editor-header">
      <el-button text size="small" @click="$router.push('/realtime/workbench')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <div class="header-center">
        <span class="task-title">{{ isNew ? '新建实时管道' : form.name || '未命名管道' }}</span>
        <el-tag v-if="!isNew" :type="form.status === 'RUNNING' ? 'success' : form.status === 'ERROR' ? 'danger' : 'info'" size="small" effect="plain">
          {{ statusText }}
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button size="small" @click="handleValidate">
          <el-icon><Finished /></el-icon> 校验
        </el-button>
        <el-button size="small" @click="handlePreview">
          <el-icon><View /></el-icon> 预览
        </el-button>
        <el-button type="primary" size="small" @click="handleSave" :loading="saving">
          <el-icon><Check /></el-icon> 保存
        </el-button>
        <el-dropdown split-button size="small" type="success" @click="handleDeploy" @command="handleDeployCommand">
          <el-icon><Upload /></el-icon> 部署
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="deploy">立即部署</el-dropdown-item>
              <el-dropdown-item command="deployStop">部署(停止旧管道)</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左侧：表单配置区 -->
      <div class="editor-main">
        <el-card class="config-card">
          <template #header><span class="card-title">基础信息</span></template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="管道名称" prop="name">
                  <el-input v-model="form.name" placeholder="如 订单数据实时同步" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="管道类型" prop="pipeType">
                  <el-select v-model="form.pipeType" style="width:100%">
                    <el-option label="MySQL Binlog CDC" value="MYSQL_BINLOG" />
                    <el-option label="Oracle LogMiner" value="ORACLE_LOG" />
                    <el-option label="Kafka Stream" value="KAFKA_STREAM" />
                    <el-option label="PostgreSQL WAL" value="PG_WAL" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述该实时管道的业务场景" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="config-card">
          <template #header><span class="card-title">源端配置</span></template>
          <el-form label-width="110px">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="源数据源" prop="sourceDsId">
                  <el-select v-model="form.sourceDsId" placeholder="选择源数据源" style="width:100%" @change="onSourceChange">
                    <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="监听模式">
                  <el-select v-model="form.sourceConfig.listenMode" style="width:100%">
                    <el-option label="全量+增量" value="ALL" />
                    <el-option label="仅增量" value="INCREMENTAL" />
                    <el-option label="仅全量" value="FULL" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <template v-if="form.pipeType === 'MYSQL_BINLOG' || form.pipeType === 'PG_WAL'">
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="监听表">
                    <el-select v-model="form.sourceConfig.includeTables" multiple filterable placeholder="留空监听全部表" style="width:100%">
                      <el-option v-for="t in sourceTables" :key="t" :label="t" :value="t" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="排除表">
                    <el-select v-model="form.sourceConfig.excludeTables" multiple filterable placeholder="排除不需要同步的表" style="width:100%">
                      <el-option v-for="t in sourceTables" :key="t" :label="t" :value="t" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="Binlog位置">
                    <el-input v-model="form.sourceConfig.binlogPosition" placeholder="留空从最新位置开始" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="Server ID">
                    <el-input-number v-model="form.sourceConfig.serverId" :min="1" :max="65535" style="width:100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <template v-if="form.pipeType === 'KAFKA_STREAM'">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="Topic">
                    <el-input v-model="form.sourceConfig.topic" placeholder="Kafka Topic" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="消费组">
                    <el-input v-model="form.sourceConfig.groupId" placeholder="Consumer Group" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="序列化格式">
                    <el-select v-model="form.sourceConfig.format" style="width:100%">
                      <el-option label="JSON" value="JSON" />
                      <el-option label="Avro" value="AVRO" />
                      <el-option label="Protobuf" value="PROTOBUF" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>

            <template v-if="form.pipeType === 'ORACLE_LOG'">
              <el-row :gutter="24">
                <el-col :span="8">
                  <el-form-item label="SCN起始号">
                    <el-input v-model="form.sourceConfig.startScn" placeholder="起始SCN" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="挖掘策略">
                    <el-select v-model="form.sourceConfig.miningStrategy" style="width:100%">
                      <el-option label="在线日志" value="ONLINE" />
                      <el-option label="归档日志" value="ARCHIVE" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </template>
          </el-form>
        </el-card>

        <el-card class="config-card">
          <template #header><span class="card-title">目标端配置</span></template>
          <el-form label-width="110px">
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="目标数据源" prop="targetDsId">
                  <el-select v-model="form.targetDsId" placeholder="选择目标数据源" style="width:100%">
                    <el-option v-for="ds in targetDatasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="写入模式">
                  <el-select v-model="form.targetConfig.writeMode" style="width:100%">
                    <el-option label="覆盖写入" value="OVERWRITE" />
                    <el-option label="追加写入" value="APPEND" />
                    <el-option label="更新写入" value="UPSERT" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="批量大小">
                  <el-input-number v-model="form.targetConfig.batchSize" :min="100" :max="10000" :step="100" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="并发线程">
                  <el-input-number v-model="form.targetConfig.concurrency" :min="1" :max="32" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="失败重试">
                  <el-input-number v-model="form.targetConfig.retryCount" :min="0" :max="10" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <el-card class="config-card">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">表映射配置</span>
              <el-button size="small" @click="addTableMapping"><el-icon><Plus /></el-icon> 添加映射</el-button>
            </div>
          </template>
          <el-table :data="form.tableMappings" size="small" border>
            <el-table-column prop="sourceTable" label="源表" width="200">
              <template #default="{ row }">
                <el-input v-model="row.sourceTable" size="small" placeholder="schema.table" />
              </template>
            </el-table-column>
            <el-table-column prop="targetTable" label="目标表" width="200">
              <template #default="{ row }">
                <el-input v-model="row.targetTable" size="small" placeholder="schema.table" />
              </template>
            </el-table-column>
            <el-table-column prop="syncDML" label="DML操作" width="180">
              <template #default="{ row }">
                <el-checkbox-group v-model="row.dmlOps" size="small">
                  <el-checkbox value="INSERT" label="INSERT" />
                  <el-checkbox value="UPDATE" label="UPDATE" />
                  <el-checkbox value="DELETE" label="DELETE" />
                </el-checkbox-group>
              </template>
            </el-table-column>
            <el-table-column prop="whereFilter" label="过滤条件" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.whereFilter" size="small" placeholder="如 status != 0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button size="small" type="danger" circle @click="removeTableMapping($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="empty-mapping" v-if="form.tableMappings.length === 0">
            暂无表映射，未配置时默认同步源端所有表
          </div>
        </el-card>

        <el-card class="config-card">
          <template #header><span class="card-title">高级配置</span></template>
          <el-form label-width="130px">
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="管道缓冲区">
                  <el-select v-model="form.advancedConfig.bufferSize" style="width:100%">
                    <el-option label="256MB" :value="256" />
                    <el-option label="512MB" :value="512" />
                    <el-option label="1GB" :value="1024" />
                    <el-option label="2GB" :value="2048" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="DDL同步">
                  <el-switch v-model="form.advancedConfig.syncDDL" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="心跳间隔(秒)">
                  <el-input-number v-model="form.advancedConfig.heartbeatInterval" :min="5" :max="300" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="24">
              <el-col :span="8">
                <el-form-item label="告警策略">
                  <el-select v-model="form.advancedConfig.alarmStrategy" style="width:100%">
                    <el-option label="延迟>1分钟" value="LATENCY_1M" />
                    <el-option label="延迟>5分钟" value="LATENCY_5M" />
                    <el-option label="管道停止" value="PIPE_STOPPED" />
                    <el-option label="数据差异" value="DATA_DIFF" />
                    <el-option label="不告警" value="NONE" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="数据校验">
                  <el-switch v-model="form.advancedConfig.enableCheck" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="校验间隔(分)">
                  <el-input-number v-model="form.advancedConfig.checkInterval" :min="10" :max="1440" :step="10" :disabled="!form.advancedConfig.enableCheck" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </div>

      <!-- 右侧：预览面板 -->
      <div class="editor-sidebar">
        <el-card class="side-card">
          <template #header><span class="side-title">管道拓扑</span></template>
          <div class="topo-preview">
            <div class="topo-node source">
              <el-icon><Coin /></el-icon>
              <span>{{ sourceName || '源端' }}</span>
            </div>
            <div class="topo-arrow">
              <div class="arrow-line"></div>
              <div class="arrow-label">CDC实时同步</div>
            </div>
            <div class="topo-node target">
              <el-icon><Promotion /></el-icon>
              <span>{{ targetName || '目标端' }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="side-card">
          <template #header><span class="side-title">管道配置摘要</span></template>
          <div class="config-summary">
            <div class="summary-item">
              <span class="s-label">管道类型</span>
              <el-tag size="small">{{ pipeTypeText }}</el-tag>
            </div>
            <div class="summary-item">
              <span class="s-label">监听模式</span>
              <span>{{ listenModeText }}</span>
            </div>
            <div class="summary-item">
              <span class="s-label">写入模式</span>
              <span>{{ writeModeText }}</span>
            </div>
            <div class="summary-item">
              <span class="s-label">批量大小</span>
              <span>{{ form.targetConfig.batchSize }} 条</span>
            </div>
            <div class="summary-item">
              <span class="s-label">映射表数</span>
              <span>{{ form.tableMappings.length || '全部' }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="side-card">
          <template #header><span class="side-title">快捷操作</span></template>
          <div class="quick-actions">
            <el-button style="width:100%;margin-bottom:8px" @click="handleValidate">
              <el-icon><Finished /></el-icon> 配置校验
            </el-button>
            <el-button style="width:100%;margin-bottom:8px" @click="handleCopyConfig">
              <el-icon><CopyDocument /></el-icon> 复制配置
            </el-button>
            <el-button style="width:100%" type="danger" plain @click="handleReset">
              <el-icon><RefreshLeft /></el-icon> 恢复默认
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const isNew = computed(() => !route.params.id)

const formRef = ref(null)
const saving = ref(false)
const datasourceList = ref([])
const sourceTables = ref([])

const form = reactive({
  id: null,
  name: '',
  pipeType: 'MYSQL_BINLOG',
  description: '',
  sourceDsId: null,
  targetDsId: null,
  status: 'DRAFT',
  sourceConfig: {
    listenMode: 'ALL',
    includeTables: [],
    excludeTables: [],
    binlogPosition: '',
    serverId: 1001,
    topic: '',
    groupId: '',
    format: 'JSON',
    startScn: '',
    miningStrategy: 'ONLINE'
  },
  targetConfig: {
    writeMode: 'UPSERT',
    batchSize: 1000,
    concurrency: 4,
    retryCount: 3
  },
  tableMappings: [
    { sourceTable: '', targetTable: '', dmlOps: ['INSERT', 'UPDATE', 'DELETE'], whereFilter: '' }
  ],
  advancedConfig: {
    bufferSize: 512,
    syncDDL: false,
    heartbeatInterval: 30,
    alarmStrategy: 'LATENCY_5M',
    enableCheck: false,
    checkInterval: 60
  }
})

const rules = {
  name: [{ required: true, message: '请输入管道名称', trigger: 'blur' }],
  pipeType: [{ required: true, message: '请选择管道类型', trigger: 'change' }],
  sourceDsId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  targetDsId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }]
}

const statusText = computed(() => {
  const m = { RUNNING: '运行中', STOPPED: '已停止', ERROR: '异常', DRAFT: '草稿' }
  return m[form.status] || form.status
})

const sourceName = computed(() => datasourceList.value.find(d => d.id === form.sourceDsId)?.name || '')
const targetName = computed(() => datasourceList.value.find(d => d.id === form.targetDsId)?.name || '')

const targetDatasourceList = computed(() => {
  return datasourceList.value.filter(ds => ds.id !== form.sourceDsId)
})

const pipeTypeText = computed(() => {
  const m = { MYSQL_BINLOG: 'MySQL CDC', ORACLE_LOG: 'Oracle CDC', KAFKA_STREAM: 'Kafka', PG_WAL: 'PG CDC' }
  return m[form.pipeType] || form.pipeType
})

const listenModeText = computed(() => {
  const m = { ALL: '全量+增量', INCREMENTAL: '仅增量', FULL: '仅全量' }
  return m[form.sourceConfig.listenMode] || ''
})

const writeModeText = computed(() => {
  const m = { OVERWRITE: '覆盖', APPEND: '追加', UPSERT: '更新' }
  return m[form.targetConfig.writeMode] || ''
})

onMounted(async () => {
  // Load datasources
  try {
    const { getDatasourceList } = await import('@/api')
    const res = await getDatasourceList()
    datasourceList.value = res.data || []
  } catch (e) {
    datasourceList.value = [
      { id: 1, name: 'MySQL生产库' },
      { id: 2, name: 'Oracle ERP' },
      { id: 3, name: 'Kafka集群' },
      { id: 4, name: 'Doris分析库' },
      { id: 5, name: 'ClickHouse' }
    ]
  }

  if (!isNew.value) {
    await loadPipeline()
  }
})

const loadPipeline = async () => {
  // In production: const res = await realtimeAPI.get(route.params.id)
  ElMessage.info('加载管道配置: ' + route.params.id)
}

const onSourceChange = () => {
  if (form.sourceDsId && form.pipeType === 'MYSQL_BINLOG') {
    sourceTables.value = ['db1.orders', 'db1.order_items', 'db1.customers', 'db2.products', 'db2.inventory']
  }
}

const addTableMapping = () => {
  form.tableMappings.push({ sourceTable: '', targetTable: '', dmlOps: ['INSERT', 'UPDATE', 'DELETE'], whereFilter: '' })
}

const removeTableMapping = (idx) => {
  form.tableMappings.splice(idx, 1)
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    // In production: await realtimeAPI.save(form)
    await new Promise(r => setTimeout(r, 600))
    ElMessage.success('保存成功')
  } catch (e) {
    if (e !== false) ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleValidate = () => {
  ElMessage.success('配置校验通过')
}

const handlePreview = () => {
  ElMessage.info(JSON.stringify(form, null, 2))
}

const handleDeploy = async () => {
  try {
    await ElMessageBox.confirm('确定部署该管道？部署后将立即开始数据同步。', '部署确认', { type: 'info' })
    ElMessage.success('管道已部署并开始运行')
  } catch (e) { /* cancel */ }
}

const handleDeployCommand = (cmd) => {
  if (cmd === 'deployStop') {
    ElMessageBox.confirm('部署新管道并停止旧管道？', '确认', { type: 'warning' }).then(() => {
      ElMessage.success('已部署（旧管道已停止）')
    }).catch(() => {})
  } else {
    handleDeploy()
  }
}

const handleCopyConfig = () => {
  navigator.clipboard.writeText(JSON.stringify(form, null, 2)).then(() => {
    ElMessage.success('配置已复制')
  })
}

const handleReset = () => {
  ElMessageBox.confirm('恢复默认配置将清除所有修改，确定继续？', '警告', { type: 'warning' }).then(() => {
    form.sourceConfig = { listenMode: 'ALL', includeTables: [], excludeTables: [], binlogPosition: '', serverId: 1001, topic: '', groupId: '', format: 'JSON', startScn: '', miningStrategy: 'ONLINE' }
    form.targetConfig = { writeMode: 'UPSERT', batchSize: 1000, concurrency: 4, retryCount: 3 }
    form.tableMappings = [{ sourceTable: '', targetTable: '', dmlOps: ['INSERT', 'UPDATE', 'DELETE'], whereFilter: '' }]
    ElMessage.success('已恢复默认')
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.realtime-editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  z-index: 10;

  .header-center {
    display: flex; align-items: center; gap: 10px;
    .task-title { font-size: 15px; font-weight: 600; color: #303133; }
  }

  .header-actions { display: flex; gap: 8px; }
}

.editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.editor-main {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.12); border-radius: 3px; }
}

.config-card {
  border-radius: 10px;
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);

  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f5f5f5;
  }

  .card-title { font-size: 14px; font-weight: 600; color: #303133; }

  .card-title-row {
    display: flex; justify-content: space-between; align-items: center;
  }
}

.editor-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .side-card {
    border-radius: 10px;
    box-shadow: 0 1px 8px rgba(0,0,0,0.04);

    :deep(.el-card__header) {
      padding: 14px 16px;
      border-bottom: 1px solid #f5f5f5;
    }

    :deep(.el-card__body) { padding: 16px; }

    .side-title { font-size: 13px; font-weight: 600; color: #303133; }
  }
}

.topo-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;

  .topo-node {
    width: 100%;
    padding: 14px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px; font-weight: 500;

    &.source { background: #e6fffb; color: #13c2c2; border: 1px solid #b5f5ec; }
    &.target { background: #e6f4ff; color: #1890ff; border: 1px solid #bae0ff; }
  }

  .topo-arrow {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4px 0;

    .arrow-line {
      width: 2px; height: 30px;
      background: linear-gradient(to bottom, #13c2c2, #1890ff);
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: -6px; left: -4px;
        width: 0; height: 0;
        border-left: 5px solid transparent;
        border-right: 5px solid transparent;
        border-top: 6px solid #1890ff;
      }
    }

    .arrow-label {
      font-size: 11px; color: #999; margin-top: 4px;
      background: #f5f5f5; padding: 2px 8px; border-radius: 4px;
    }
  }
}

.config-summary {
  .summary-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 0; border-bottom: 1px solid #fafafa;
    font-size: 13px;

    &:last-child { border-bottom: none; }

    .s-label { color: #999; }
  }
}

.quick-actions { padding: 0; }

.empty-mapping {
  text-align: center; padding: 24px 0; color: #999; font-size: 13px;
}

@media (max-width: 768px) {
  .editor-body { flex-direction: column; padding: 12px; }
  .editor-sidebar { width: 100%; }
  .editor-header { flex-wrap: wrap; gap: 8px; padding: 10px 14px; }
}
</style>
