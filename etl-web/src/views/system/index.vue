<template>
  <div class="system-management">
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: #e6f7ff">
          <el-icon size="24" color="#1890ff"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ users.length }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f6ffed">
          <el-icon size="24" color="#52c41a"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ users.filter(u => u.status === 'active').length }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #fff7e6">
          <el-icon size="24" color="#fa8c16"><Key /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ roles.length }}</div>
          <div class="stat-label">角色数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: #f9f0ff">
          <el-icon size="24" color="#722ed1"><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatDate(lastLoginTime) }}</div>
          <div class="stat-label">最近登录</div>
        </div>
      </div>
    </div>

    <!-- 选项卡 -->
    <el-tabs v-model="activeTab" class="content-tabs">
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="userSearch"
              placeholder="搜索用户名或邮箱"
              prefix-icon="Search"
              clearable
              style="width: 260px"
            />
            <el-select v-model="userStatusFilter" placeholder="状态筛选" clearable style="width: 140px">
              <el-option label="活跃" value="active" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="handleAddUser">
              <el-icon><Plus /></el-icon>新增用户
            </el-button>
          </div>
        </div>
        <el-table :data="filteredUsers" stripe border style="width: 100%">
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <el-tag size="small" :type="row.role === 'admin' ? 'danger' : row.role === 'developer' ? 'warning' : 'info'">
                {{ roleLabel(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '活跃' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="lastLogin" label="最后登录" width="170">
            <template #default="{ row }">{{ formatDate(row.lastLogin) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEditUser(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <span class="total-info">共 {{ filteredUsers.length }} 条</span>
          <el-pagination
            v-model:current-page="userPage"
            :page-size="userPageSize"
            :total="filteredUsers.length"
            layout="prev, pager, next"
            small
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="角色管理" name="roles">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="roleSearch"
              placeholder="搜索角色名称"
              prefix-icon="Search"
              clearable
              style="width: 260px"
            />
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="handleAddRole">
              <el-icon><Plus /></el-icon>新增角色
            </el-button>
          </div>
        </div>
        <el-table :data="filteredRoles" stripe border style="width: 100%">
          <el-table-column prop="name" label="角色名称" min-width="140" />
          <el-table-column prop="code" label="角色编码" width="140" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column prop="userCount" label="关联用户数" width="110" />
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEditRole(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteRole(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrap">
          <span class="total-info">共 {{ filteredRoles.length }} 条</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="系统设置" name="settings">
        <el-form :model="systemSettings" label-width="140px" class="settings-form">
          <el-divider content-position="left">基本设置</el-divider>
          <el-form-item label="系统名称">
            <el-input v-model="systemSettings.systemName" />
          </el-form-item>
          <el-form-item label="系统版本">
            <el-tag>v2.3.0</el-tag>
          </el-form-item>
          <el-form-item label="管理员邮箱">
            <el-input v-model="systemSettings.adminEmail" />
          </el-form-item>
          <el-divider content-position="left">安全设置</el-divider>
          <el-form-item label="密码最小长度">
            <el-input-number v-model="systemSettings.minPasswordLength" :min="6" :max="32" />
          </el-form-item>
          <el-form-item label="登录失败锁定">
            <el-input-number v-model="systemSettings.loginLockThreshold" :min="3" :max="20" />
            <span class="form-hint">次失败后锁定账号</span>
          </el-form-item>
          <el-form-item label="会话超时">
            <el-input-number v-model="systemSettings.sessionTimeout" :min="5" :max="1440" />
            <span class="form-hint">分钟</span>
          </el-form-item>
          <el-divider content-position="left">通知设置</el-divider>
          <el-form-item label="告警通知">
            <el-switch v-model="systemSettings.alertEnabled" />
          </el-form-item>
          <el-form-item label="邮件通知">
            <el-switch v-model="systemSettings.emailEnabled" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSaveSettings">保存设置</el-button>
            <el-button @click="handleResetSettings">恢复默认</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEditUser ? '编辑用户' : '新增用户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="userForm" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" required>
          <el-input v-model="userForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="开发者" value="developer" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="userForm.status"
            active-value="active"
            inactive-value="disabled"
            active-text="活跃"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 角色编辑对话框 -->
    <el-dialog
      v-model="roleDialogVisible"
      :title="isEditRole ? '编辑角色' : '新增角色'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('users')

// ===== 用户管理 =====
const userSearch = ref('')
const userStatusFilter = ref('')
const userPage = ref(1)
const userPageSize = ref(10)

const users = ref([
  { id: 1, username: 'admin', email: 'admin@etl-sync.com', role: 'admin', status: 'active', createdAt: '2025-12-01T09:00:00', lastLogin: '2026-05-01T08:30:00' },
  { id: 2, username: 'zhangsan', email: 'zhangsan@etl-sync.com', role: 'developer', status: 'active', createdAt: '2026-01-15T10:30:00', lastLogin: '2026-04-30T17:45:00' },
  { id: 3, username: 'lisi', email: 'lisi@etl-sync.com', role: 'developer', status: 'active', createdAt: '2026-02-20T14:00:00', lastLogin: '2026-04-29T11:20:00' },
  { id: 4, username: 'wangwu', email: 'wangwu@etl-sync.com', role: 'viewer', status: 'disabled', createdAt: '2026-03-10T08:15:00', lastLogin: '2026-04-01T09:00:00' },
  { id: 5, username: 'zhaoliu', email: 'zhaoliu@etl-sync.com', role: 'viewer', status: 'active', createdAt: '2026-04-05T16:30:00', lastLogin: '2026-04-28T14:10:00' }
])

const filteredUsers = computed(() => {
  let result = users.value
  if (userSearch.value) {
    const q = userSearch.value.toLowerCase()
    result = result.filter(u => u.username.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
  }
  if (userStatusFilter.value) {
    result = result.filter(u => u.status === userStatusFilter.value)
  }
  return result
})

const lastLoginTime = computed(() => {
  const times = users.value.map(u => new Date(u.lastLogin).getTime())
  return new Date(Math.max(...times)).toISOString()
})

const userDialogVisible = ref(false)
const isEditUser = ref(false)
const editingUserId = ref(null)
const userForm = reactive({ username: '', email: '', role: 'developer', status: 'active' })

function roleLabel(role) {
  const map = { admin: '管理员', developer: '开发者', viewer: '查看者' }
  return map[role] || role
}

function handleAddUser() {
  isEditUser.value = false
  editingUserId.value = null
  Object.assign(userForm, { username: '', email: '', role: 'developer', status: 'active' })
  userDialogVisible.value = true
}

function handleEditUser(row) {
  isEditUser.value = true
  editingUserId.value = row.id
  Object.assign(userForm, { username: row.username, email: row.email, role: row.role, status: row.status })
  userDialogVisible.value = true
}

function handleSaveUser() {
  if (!userForm.username || !userForm.email) {
    ElMessage.warning('请填写用户名和邮箱')
    return
  }
  if (isEditUser.value) {
    const idx = users.value.findIndex(u => u.id === editingUserId.value)
    if (idx > -1) {
      users.value[idx] = { ...users.value[idx], ...userForm }
    }
    ElMessage.success('用户信息已更新')
  } else {
    const newId = Math.max(...users.value.map(u => u.id), 0) + 1
    users.value.push({
      id: newId,
      ...userForm,
      createdAt: new Date().toISOString(),
      lastLogin: '-'
    })
    ElMessage.success('用户创建成功')
  }
  userDialogVisible.value = false
}

function handleResetPassword(row) {
  ElMessageBox.confirm(`确定要重置用户 "${row.username}" 的密码吗？`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success(`用户 "${row.username}" 的密码已重置为默认密码`)
  }).catch(() => {})
}

function handleDeleteUser(row) {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？此操作不可撤销。`, '删除用户', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    users.value = users.value.filter(u => u.id !== row.id)
    ElMessage.success('用户已删除')
  }).catch(() => {})
}

// ===== 角色管理 =====
const roleSearch = ref('')

const roles = ref([
  { id: 1, name: '管理员', code: 'admin', description: '系统管理员，拥有所有权限', userCount: 1, createdAt: '2025-12-01T09:00:00' },
  { id: 2, name: '开发者', code: 'developer', description: '数据开发人员，可创建和编辑任务', userCount: 2, createdAt: '2025-12-01T09:00:00' },
  { id: 3, name: '查看者', code: 'viewer', description: '只读权限，仅可查看数据和任务状态', userCount: 2, createdAt: '2025-12-01T09:00:00' }
])

const filteredRoles = computed(() => {
  if (!roleSearch.value) return roles.value
  const q = roleSearch.value.toLowerCase()
  return roles.value.filter(r => r.name.toLowerCase().includes(q) || r.code.toLowerCase().includes(q))
})

const roleDialogVisible = ref(false)
const isEditRole = ref(false)
const editingRoleId = ref(null)
const roleForm = reactive({ name: '', code: '', description: '' })

function handleAddRole() {
  isEditRole.value = false
  editingRoleId.value = null
  Object.assign(roleForm, { name: '', code: '', description: '' })
  roleDialogVisible.value = true
}

function handleEditRole(row) {
  isEditRole.value = true
  editingRoleId.value = row.id
  Object.assign(roleForm, { name: row.name, code: row.code, description: row.description })
  roleDialogVisible.value = true
}

function handleSaveRole() {
  if (!roleForm.name || !roleForm.code) {
    ElMessage.warning('请填写角色名称和编码')
    return
  }
  if (isEditRole.value) {
    const idx = roles.value.findIndex(r => r.id === editingRoleId.value)
    if (idx > -1) {
      roles.value[idx] = { ...roles.value[idx], ...roleForm }
    }
    ElMessage.success('角色信息已更新')
  } else {
    const newId = Math.max(...roles.value.map(r => r.id), 0) + 1
    roles.value.push({ id: newId, ...roleForm, userCount: 0, createdAt: new Date().toISOString() })
    ElMessage.success('角色创建成功')
  }
  roleDialogVisible.value = false
}

function handleDeleteRole(row) {
  if (row.userCount > 0) {
    ElMessage.warning(`角色 "${row.name}" 还有 ${row.userCount} 个关联用户，无法删除`)
    return
  }
  ElMessageBox.confirm(`确定要删除角色 "${row.name}" 吗？`, '删除角色', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    roles.value = roles.value.filter(r => r.id !== row.id)
    ElMessage.success('角色已删除')
  }).catch(() => {})
}

// ===== 系统设置 =====
const systemSettings = reactive({
  systemName: 'ETL数据同步调度系统',
  adminEmail: 'admin@etl-sync.com',
  minPasswordLength: 8,
  loginLockThreshold: 5,
  sessionTimeout: 30,
  alertEnabled: true,
  emailEnabled: true
})

const defaultSettings = {
  systemName: 'ETL数据同步调度系统',
  adminEmail: 'admin@etl-sync.com',
  minPasswordLength: 8,
  loginLockThreshold: 5,
  sessionTimeout: 30,
  alertEnabled: true,
  emailEnabled: true
}

function handleSaveSettings() {
  ElMessage.success('系统设置已保存')
}

function handleResetSettings() {
  Object.assign(systemSettings, defaultSettings)
  ElMessage.success('已恢复默认设置')
}

// ===== 通用 =====
function formatDate(val) {
  if (!val || val === '-') return '-'
  try {
    const d = new Date(val)
    if (isNaN(d.getTime())) return String(val)
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch { return String(val) }
}
</script>

<style scoped>
.system-management {
  padding: 0;
}
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #ebeef5;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}
.content-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 0 20px 20px;
  border: 1px solid #ebeef5;
}
.content-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pagination-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding: 0 4px;
}
.total-info {
  font-size: 13px;
  color: #909399;
}
.settings-form {
  max-width: 600px;
  padding-top: 16px;
}
.form-hint {
  margin-left: 10px;
  color: #909399;
  font-size: 13px;
}
</style>
