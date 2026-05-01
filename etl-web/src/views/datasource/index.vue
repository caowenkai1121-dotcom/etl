<template>
  <div class="datasource-page">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="ds-stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #4f6ef7;"><el-icon><Coin /></el-icon></div>
          <div class="stat-body">
            <div class="stat-val">{{ stats.total }}</div>
            <div class="stat-lbl">数据源总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="ds-stat-card">
          <div class="stat-icon" style="background: #f0f9eb; color: #52c41a;"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-body">
            <div class="stat-val">{{ stats.connected }}</div>
            <div class="stat-lbl">连接正常</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="ds-stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #ff4d4f;"><el-icon><Warning /></el-icon></div>
          <div class="stat-body">
            <div class="stat-val">{{ stats.failed }}</div>
            <div class="stat-lbl">连接异常</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="ds-stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #faad14;"><el-icon><Timer /></el-icon></div>
          <div class="stat-body">
            <div class="stat-val">{{ stats.untested }}</div>
            <div class="stat-lbl">未测试</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="left">
        <el-input v-model="queryParams.name" placeholder="搜索数据源名称" clearable style="width: 240px; margin-right: 12px;">
          <template #append>
            <el-button @click="fetchData"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
        <el-select v-model="queryParams.type" placeholder="类型筛选" clearable style="width: 140px; margin-right: 12px;" @change="fetchData">
          <el-option label="全部类型" value="" />
          <el-option-group label="关系型数据库">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="SQL Server" value="SQLSERVER" />
            <el-option label="Doris" value="DORIS" />
            <el-option label="ClickHouse" value="CLICKHOUSE" />
          </el-option-group>
          <el-option-group label="NoSQL">
            <el-option label="MongoDB" value="MONGODB" />
            <el-option label="Elasticsearch" value="ELASTICSEARCH" />
            <el-option label="Redis" value="REDIS" />
          </el-option-group>
        </el-select>
        <el-select v-model="connectionFilter" placeholder="连接状态" clearable style="width: 130px;" @change="fetchData">
          <el-option label="全部状态" value="" />
          <el-option label="连接成功" :value="1" />
          <el-option label="连接失败" :value="2" />
          <el-option label="未测试" :value="0" />
        </el-select>
      </div>
      <div class="right">
        <el-button @click="handleBatchTest" :disabled="tableData.length === 0">
          <el-icon><Connection /></el-icon>
          批量测试
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增数据源
        </el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-card class="data-card">
      <el-table :data="tableData" v-loading="loading" class="data-table" row-key="id" :expand-row-keys="expandedRows" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-content">
              <div class="expand-header">
                <span class="expand-title">表列表 ({{ row.tables?.length || 0 }} 张表)</span>
                <div class="expand-actions">
                  <el-input v-model="tableSearch[row.id]" placeholder="搜索表名" size="small" clearable style="width: 180px; margin-right: 8px;" />
                  <el-button size="small" @click="loadTables(row)" :loading="tableLoading[row.id]">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                </div>
              </div>
              <el-tree
                v-if="row.tables && row.tables.length > 0"
                :data="filteredTables(row)"
                :props="{ label: 'tableName', children: 'columns' }"
                node-key="tableName"
                default-expand-all
                class="table-tree"
              >
                <template #default="{ node }">
                  <span class="tree-node">
                    <el-icon v-if="node.data.type === 'table'" color="#4f6ef7" style="margin-right: 4px;"><Grid /></el-icon>
                    {{ node.label }}
                    <el-tag v-if="node.data.type === 'column'" size="small" type="info" style="margin-left: 8px;">
                      {{ node.data.columnType }}
                    </el-tag>
                    <span v-if="node.data.comment" style="margin-left: 8px; color: #909399; font-size: 12px;">
                      {{ node.data.comment }}
                    </span>
                  </span>
                </template>
              </el-tree>
              <div v-else-if="!row.tables" class="empty-tables">点击刷新加载表列表</div>
              <div v-else class="empty-tables">未找到匹配的表</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="ds-name-cell">
              <el-icon color="#4f6ef7"><component :is="getDsIcon(row.type)" /></el-icon>
              <span class="ds-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small" effect="light">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <code class="host-addr">{{ row.host }}:{{ row.port }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="databaseName" label="数据库" width="130" show-overflow-tooltip />
        <el-table-column prop="connectionTest" label="连接状态" width="110">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="status-dot" :class="row.connectionTest === 1 ? 'success' : row.connectionTest === 2 ? 'error' : 'pending'"></span>
              <span>{{ row.connectionTest === 1 ? '正常' : row.connectionTest === 2 ? '异常' : '未测试' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="连接池" width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.connectionTest === 1"
              :percentage="row.poolUsage || 35"
              :stroke-width="6"
              :show-text="false"
              :color="getPoolColor(row.poolUsage)"
            />
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" :loading="testLoading[row.id]" @click="handleTest(row)">
              <el-icon><Connection /></el-icon>
              测试
            </el-button>
            <el-button size="small" @click="handleClone(row)">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入数据源名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" @change="handleTypeChange" style="width: 100%">
                <el-option-group label="关系型数据库">
                  <el-option label="MySQL" value="MYSQL" />
                  <el-option label="PostgreSQL" value="POSTGRESQL" />
                  <el-option label="Oracle" value="ORACLE" />
                  <el-option label="SQL Server" value="SQLSERVER" />
                  <el-option label="Doris" value="DORIS" />
                  <el-option label="ClickHouse" value="CLICKHOUSE" />
                </el-option-group>
                <el-option-group label="NoSQL">
                  <el-option label="MongoDB" value="MONGODB" />
                  <el-option label="Elasticsearch" value="ELASTICSEARCH" />
                  <el-option label="Redis" value="REDIS" />
                </el-option-group>
                <el-option-group label="大数据">
                  <el-option label="Hive" value="HIVE" />
                </el-option-group>
                <el-option-group label="消息队列">
                  <el-option label="Kafka" value="KAFKA" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="主机" prop="host">
              <el-input v-model="form.host" placeholder="请输入主机地址" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据库" prop="databaseName">
              <el-input v-model="form.databaseName" placeholder="请输入数据库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字符集">
              <el-input v-model="form.charset" placeholder="默认utf8mb4" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="高级配置">
          <el-collapse>
            <el-collapse-item title="连接池配置" name="pool">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="最小连接数">
                    <el-input-number v-model="form.minPoolSize" :min="1" :max="50" size="small" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="最大连接数">
                    <el-input-number v-model="form.maxPoolSize" :min="1" :max="100" size="small" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="超时(ms)">
                    <el-input-number v-model="form.connectionTimeout" :min="1000" :max="60000" :step="1000" size="small" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-collapse-item>
            <el-collapse-item title="备注信息" name="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleTestConnection" :loading="testConnLoading">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <div>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { datasourceAPI } from '@/api'

const loading = ref(false)
const tableLoading = ref({})
const testLoading = ref({})
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据源')
const formRef = ref(null)
const expandedRows = ref([])
const connectionFilter = ref('')
const tableSearch = ref({})
const testConnLoading = ref(false)

const stats = reactive({
  total: 0,
  connected: 0,
  failed: 0,
  untested: 0
})

const updateStats = () => {
  stats.total = tableData.value.length
  stats.connected = tableData.value.filter(d => d.connectionTest === 1).length
  stats.failed = tableData.value.filter(d => d.connectionTest === 2).length
  stats.untested = tableData.value.filter(d => !d.connectionTest).length
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: '',
  type: ''
})

const form = reactive({
  id: null,
  name: '',
  type: 'MYSQL',
  host: '',
  port: 3306,
  databaseName: '',
  username: '',
  password: '',
  charset: 'utf8mb4',
  minPoolSize: 5,
  maxPoolSize: 20,
  connectionTimeout: 10000,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  databaseName: [{ required: true, message: '请输入数据库', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const defaultPorts = {
  MYSQL: 3306, POSTGRESQL: 5432, ORACLE: 1521, SQLSERVER: 1433, DORIS: 9030, CLICKHOUSE: 8123,
  MONGODB: 27017, ELASTICSEARCH: 9200, REDIS: 6379,
  HIVE: 10000, KAFKA: 9092
}

const handleTypeChange = (type) => {
  if (defaultPorts[type]) form.port = defaultPorts[type]
}

const getDsIcon = (type) => {
  const icons = { MYSQL: 'Coin', POSTGRESQL: 'Coin', REDIS: 'Coin', KAFKA: 'Connection' }
  return icons[type] || 'Coin'
}

const getTypeTagType = (type) => {
  const colors = {
    MYSQL: '', POSTGRESQL: 'success', ORACLE: 'warning', SQLSERVER: '', DORIS: 'warning', CLICKHOUSE: 'success',
    MONGODB: 'success', ELASTICSEARCH: 'warning', REDIS: 'danger', HIVE: 'warning', KAFKA: 'info'
  }
  return colors[type] || 'info'
}

const getTypeLabel = (type) => {
  const labels = {
    MYSQL: 'MySQL', POSTGRESQL: 'PostgreSQL', ORACLE: 'Oracle', SQLSERVER: 'SQL Server', DORIS: 'Doris', CLICKHOUSE: 'ClickHouse',
    MONGODB: 'MongoDB', ELASTICSEARCH: 'ES', REDIS: 'Redis', HIVE: 'Hive', KAFKA: 'Kafka'
  }
  return labels[type] || type
}

const getPoolColor = (usage) => {
  if (!usage) return '#4f6ef7'
  if (usage > 80) return '#ff4d4f'
  if (usage > 60) return '#faad14'
  return '#4f6ef7'
}

const filteredTables = (row) => {
  const search = tableSearch.value[row.id]?.toLowerCase() || ''
  if (!search || !row.tables) return row.tables
  return row.tables.filter(t => t.tableName?.toLowerCase().includes(search))
}

onMounted(() => fetchData())

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (connectionFilter.value !== '') params.connectionTest = connectionFilter.value
    const res = await datasourceAPI.getPage(params)
    tableData.value = (res.data?.list || []).map(d => ({
      ...d,
      poolUsage: d.poolUsage ?? 0
    }))
    total.value = res.data?.total || 0
    updateStats()
  } catch (error) {
    console.error('获取数据失败:', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadTables = async (row) => {
  tableLoading.value[row.id] = true
  try {
    const res = await datasourceAPI.getTables(row.id)
    row.tables = (res.data || []).map(t => ({
      ...t,
      type: 'table',
      columns: (t.columns || []).map(c => ({ ...c, type: 'column' }))
    }))
    if (!expandedRows.value.includes(row.id)) {
      expandedRows.value.push(row.id)
    }
  } catch (error) {
    console.error('加载表数据失败:', error)
    row.tables = []
    ElMessage.error('加载表数据失败')
  } finally {
    tableLoading.value[row.id] = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增数据源'
  Object.assign(form, {
    id: null, name: '', type: 'MYSQL', host: '', port: 3306,
    databaseName: '', username: '', password: '', charset: 'utf8mb4',
    minPoolSize: 5, maxPoolSize: 20, connectionTimeout: 10000, remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑数据源'
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

const handleClone = (row) => {
  dialogTitle.value = '复制数据源'
  Object.assign(form, {
    ...row,
    id: null,
    name: row.name + ' (副本)',
    password: ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该数据源？关联的API和管道将无法使用。', '提示', { type: 'warning' })
    await datasourceAPI.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

const handleTest = async (row) => {
  testLoading.value[row.id] = true
  try {
    const res = await datasourceAPI.testConnection(row.id)
    if (res.data) ElMessage.success('连接成功')
    else ElMessage.error('连接失败')
    fetchData()
  } catch (error) {
    ElMessage.error('连接失败')
  } finally {
    testLoading.value[row.id] = false
  }
}

const handleBatchTest = async () => {
  for (const row of tableData.value) {
    await handleTest(row)
  }
}

const handleTestConnection = async () => {
  testConnLoading.value = true
  try {
    if (form.id) {
      await datasourceAPI.testConnection(form.id)
    } else {
      await datasourceAPI.create(form)
    }
    ElMessage.success('连接测试成功')
  } catch (e) {
    ElMessage.error('连接测试失败')
  } finally {
    testConnLoading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (form.id) {
      await datasourceAPI.update(form.id, form)
    } else {
      await datasourceAPI.create(form)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}
</script>

<style lang="scss" scoped>
.datasource-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100%;

  .stats-row { margin-bottom: 20px; }

  .ds-stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border-radius: 12px;
    padding: 18px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s;
    margin-bottom: 16px;

    &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }

    .stat-icon {
      width: 46px;
      height: 46px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .stat-body {
      .stat-val { font-size: 24px; font-weight: 700; color: #303133; }
      .stat-lbl { font-size: 13px; color: #909399; margin-top: 2px; }
    }
  }

  .search-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .data-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    :deep(.el-card__body) { padding: 20px; }
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }
  }

  .ds-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .ds-name { font-weight: 500; }
  }

  .host-addr {
    background: #f5f7fa;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    color: #606266;
  }

  .expand-content {
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;

    .expand-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .expand-title {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }

      .expand-actions { display: flex; align-items: center; }
    }

    .table-tree { max-height: 400px; overflow-y: auto; }

    .empty-tables {
      text-align: center;
      padding: 32px 0;
      color: #909399;
      font-size: 14px;
    }
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.success { background: #52c41a; box-shadow: 0 0 6px rgba(82, 196, 26, 0.4); }
      &.error { background: #ff4d4f; animation: blink 1s infinite; }
      &.pending { background: #dcdfe6; }
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .dialog-footer {
    display: flex;
    justify-content: space-between;
  }

  .tree-node {
    font-size: 13px;
    color: #606266;
    display: flex;
    align-items: center;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

@media (max-width: 768px) {
  .datasource-page {
    padding: 16px;

    .search-bar {
      flex-direction: column;
      align-items: stretch;
      gap: 12px;

      .left {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .el-input, .el-select { margin-right: 0 !important; width: 100% !important; }
      }
    }
  }
}
</style>
