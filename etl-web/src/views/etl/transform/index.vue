<template>
  <div class="transform-page page-container">
    <el-card class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon class="title-icon"><Setting /></el-icon>
            ETL转换管理
          </span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新建流水线
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-form :inline="true" :model="queryParams">
          <el-form-item label="名称">
            <el-input v-model="queryParams.name" placeholder="请输入流水线名称" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择" clearable>
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="fetchData">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="流水线名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <div class="status-indicator">
              <span class="status-dot" :class="row.status === 'ENABLED' ? 'success' : 'stopped'"></span>
              <span>{{ row.status === 'ENABLED' ? '启用' : '停用' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="stageCount" label="阶段数" width="80" align="center" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 新建/编辑流水线对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="dark-dialog">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="流水线名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入流水线名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入流水线描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.statusBool"
            active-text="启用"
            inactive-text="停用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getTransformPipelinePage, createTransformPipeline, updateTransformPipeline, deleteTransformPipeline } from '@/api'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新建流水线')
const formRef = ref(null)

const queryParams = reactive({ pageNum: 1, pageSize: 10, name: '', status: '' })

const form = reactive({
  id: null, name: '', description: '', status: 'ENABLED', statusBool: true
})

const rules = {
  name: [{ required: true, message: '请输入流水线名称', trigger: 'blur' }]
}

onMounted(() => { fetchData() })

const fetchData = async () => {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (!params.name) delete params.name
    if (!params.status) delete params.status
    const res = await getTransformPipelinePage(params)
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
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

const handleAdd = () => {
  dialogTitle.value = '新建流水线'
  Object.assign(form, { id: null, name: '', description: '', status: 'ENABLED', statusBool: true })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑流水线'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description,
    status: row.status,
    statusBool: row.status === 'ENABLED'
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该转换流水线？', '提示', { type: 'warning' })
    await deleteTransformPipeline(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {}
}

const handleViewDetail = (row) => {
  router.push({ name: 'EtlPipeline', query: { id: row.id } })
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const submitData = {
      name: form.name,
      description: form.description,
      status: form.statusBool ? 'ENABLED' : 'DISABLED'
    }
    if (form.id) {
      await updateTransformPipeline(form.id, submitData)
    } else {
      await createTransformPipeline(submitData)
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.transform-page {
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
    background: rgba(99, 102, 241, 0.05);
    border-radius: 12px;
    border: 1px solid var(--border-color);
  }

  .data-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__inner-wrapper::before) { display: none; }
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;

      &.success {
        background: var(--success-color);
        box-shadow: 0 0 6px rgba(16, 185, 129, 0.5);
      }
      &.stopped {
        background: var(--text-muted);
      }
    }
  }

  .pagination-wrap {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
