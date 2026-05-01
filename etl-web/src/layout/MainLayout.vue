<template>
  <div class="fdl-layout">
    <!-- 左侧导航区: 图标栏 + 展开面板 -->
    <div class="fdl-sidebar" :class="{ expanded: sidebarExpanded }">
      <!-- 顶部图标菜单栏 -->
      <div class="icon-nav-bar">
        <div class="icon-nav-logo" @click="navigateTo('/dashboard')">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28">
            <rect width="32" height="32" rx="6" fill="#1890ff"/>
            <path d="M7 16L13 22L25 10" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="icon-nav-list">
          <div
            v-for="item in navModules"
            :key="item.key"
            class="icon-nav-item"
            :class="{ active: activeModule === item.key }"
            @click="switchModule(item.key)"
          >
            <el-tooltip :content="item.label" placement="right" :show-after="500">
              <div class="icon-nav-icon">
                <el-icon :size="18"><component :is="item.icon" /></el-icon>
              </div>
            </el-tooltip>
            <span class="icon-nav-label">{{ item.label }}</span>
          </div>
        </div>
        <div class="icon-nav-bottom">
          <div class="icon-nav-item" @click="toggleSidebar">
            <div class="icon-nav-icon">
              <el-icon :size="18"><Fold v-if="sidebarExpanded" /><Expand v-else /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 展开的子菜单面板 -->
      <transition name="panel-slide">
        <div v-show="sidebarExpanded" class="submenu-panel">
          <div class="submenu-header">
            <span class="submenu-title">{{ currentModuleLabel }}</span>
          </div>
          <!-- Data Development 子导航: 离线任务 / 实时任务 -->
          <div class="sub-nav-tabs" v-if="activeModule === 'dev'">
            <span
              class="sub-nav-tab"
              :class="{ active: devSubTab === 'scheduled' }"
              @click="switchDevSubTab('scheduled')"
            >离线任务</span>
            <span
              class="sub-nav-tab"
              :class="{ active: devSubTab === 'realtime' }"
              @click="switchDevSubTab('realtime')"
            >实时任务</span>
          </div>
          <el-scrollbar class="submenu-scroll">
            <div class="submenu-list">
              <template v-for="group in currentSubMenus" :key="group.group">
                <div class="submenu-group-title">{{ group.group }}</div>
                <div
                  v-for="item in group.items"
                  :key="item.path"
                  class="submenu-item"
                  :class="{ active: isActivePath(item.path) }"
                  @click="navigateTo(item.path)"
                >
                  <el-icon :size="14" class="submenu-item-icon">
                    <component :is="item.icon" />
                  </el-icon>
                  <span>{{ item.label }}</span>
                </div>
              </template>
            </div>
          </el-scrollbar>
        </div>
      </transition>
    </div>

    <!-- 右侧主区域 -->
    <div class="fdl-main">
      <!-- 顶部栏 -->
      <header class="fdl-header">
        <div class="header-left">
          <div class="header-search">
            <el-icon class="search-icon"><Search /></el-icon>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索任务、数据源、API..."
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <el-icon v-if="searchKeyword" class="search-clear" @click="searchKeyword=''"><Close /></el-icon>
          </div>
        </div>
        <div class="header-right">
          <!-- 快捷操作 -->
          <el-tooltip content="新建任务" placement="bottom">
            <div class="header-action" @click="navigateTo('/dev/workbench')">
              <el-icon :size="18"><Plus /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="全屏" placement="bottom">
            <div class="header-action" @click="toggleFullscreen">
              <el-icon :size="18"><FullScreen /></el-icon>
            </div>
          </el-tooltip>

          <!-- 通知 -->
          <el-popover placement="bottom" :width="320" trigger="click">
            <template #reference>
              <div class="header-action">
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                  <el-icon :size="18"><Bell /></el-icon>
                </el-badge>
              </div>
            </template>
            <div class="notice-list">
              <div class="notice-header">
                <span>消息通知</span>
                <el-button link type="primary" size="small">全部已读</el-button>
              </div>
              <div class="notice-empty" v-if="notifications.length === 0">暂无未读消息</div>
              <div v-for="n in notifications" :key="n.id" class="notice-item">
                <div class="notice-dot" :class="n.level"></div>
                <div class="notice-content">
                  <div class="notice-title">{{ n.title }}</div>
                  <div class="notice-time">{{ n.time }}</div>
                </div>
              </div>
            </div>
          </el-popover>

          <!-- 用户菜单 -->
          <el-dropdown trigger="click" placement="bottom-end">
            <div class="header-user">
              <el-avatar :size="30" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="user-name">管理员</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon><Setting /></el-icon> 系统设置
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 面包屑 -->
      <div class="fdl-breadcrumb" v-if="breadcrumbs.length > 0">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(crumb, idx) in breadcrumbs" :key="idx" :to="idx < breadcrumbs.length - 1 ? crumb.path : undefined">
            {{ crumb.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 内容区域 -->
      <main class="fdl-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="$route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 状态
const sidebarExpanded = ref(true)
const activeModule = ref('dev')
const devSubTab = ref('scheduled')
const searchKeyword = ref('')
const unreadCount = ref(3)
const cachedViews = ref(['Dashboard', 'DevWorkbenchHome'])

// 导航模块配置
const navModules = [
  { key: 'demo', label: 'Demo中心', icon: 'HomeFilled' },
  { key: 'ops', label: 'O&M Center', icon: 'Monitor' },
  { key: 'pipeline', label: 'Data Pipeline', icon: 'Connection' },
  { key: 'dev', label: 'Data Development', icon: 'DataAnalysis' },
  { key: 'service', label: 'Data Service', icon: 'Promotion' },
  { key: 'data', label: 'Data Management', icon: 'Coin' },
  { key: 'system', label: 'System Management', icon: 'Setting' },
  { key: 'recycle', label: 'Recycle Bin', icon: 'Delete' }
]

// 各模块下的子菜单配置
const moduleSubMenus = {
  demo: [
    {
      group: 'Demo中心',
      items: [
        { path: '/dashboard', label: '监控大盘', icon: 'Odometer' },
        { path: '/demo/quickstart', label: '快速入门', icon: 'Guide' },
        { path: '/demo/templates', label: '任务模板', icon: 'Document' }
      ]
    }
  ],
  ops: [
    {
      group: '运维中心',
      items: [
        { path: '/monitor', label: '实时监控', icon: 'Odometer' },
        { path: '/log', label: '日志查询', icon: 'Tickets' },
        { path: '/alert', label: '告警管理', icon: 'Bell' },
        { path: '/health', label: '健康检查', icon: 'FirstAidKit' }
      ]
    }
  ],
  pipeline: [
    {
      group: '数据管道',
      items: [
        { path: '/scheduler', label: '调度管理', icon: 'Timer' },
        { path: '/execution', label: '执行记录', icon: 'Document' },
        { path: '/etl/pipeline', label: '任务编排', icon: 'Share' },
        { path: '/quality', label: '数据质量', icon: 'Finished' }
      ]
    }
  ],
  dev: {
    scheduled: [
      {
        group: '离线任务开发',
        items: [
          { path: '/dev/workbench', label: '工作台首页', icon: 'Calendar' },
          { path: '/datasource', label: '数据源管理', icon: 'Coin' },
          { path: '/etl/rules', label: '转换规则', icon: 'Edit' },
          { path: '/etl/debug', label: '调试预览', icon: 'View' }
        ]
      },
      {
        group: '离线任务管理',
        items: [
          { path: '/task', label: '任务列表', icon: 'List' },
          { path: '/workflow', label: '工作流', icon: 'Share' }
        ]
      }
    ],
    realtime: [
      {
        group: '实时任务开发',
        items: [
          { path: '/realtime/workbench', label: '实时任务首页', icon: 'Odometer' },
          { path: '/realtime/task/new', label: '新建实时管道', icon: 'Plus' },
          { path: '/cdc-config', label: 'CDC配置', icon: 'Connection' }
        ]
      },
      {
        group: '实时管道管理',
        items: [
          { path: '/data-pipeline', label: 'CDC管道管理', icon: 'Share' },
          { path: '/monitor', label: '实时监控', icon: 'Monitor' },
          { path: '/log', label: '日志查询', icon: 'Tickets' }
        ]
      }
    ]
  },
  service: [
    {
      group: '数据服务',
      items: [
        { path: '/api-service', label: 'API服务管理', icon: 'Link' },
        { path: '/publish', label: '发布管理', icon: 'Upload' }
      ]
    }
  ],
  data: [
    {
      group: '数据管理',
      items: [
        { path: '/config', label: '配置中心', icon: 'Tools' },
        { path: '/datasource', label: '数据源管理', icon: 'Coin' }
      ]
    }
  ],
  system: [
    {
      group: '系统管理',
      items: [
        { path: '/system', label: '用户管理', icon: 'User' },
        { path: '/system/roles', label: '角色管理', icon: 'Avatar' },
        { path: '/system/settings', label: '系统设置', icon: 'Setting' }
      ]
    }
  ],
  recycle: [
    {
      group: '回收站',
      items: [
        { path: '/recycle', label: '已删除项目', icon: 'Delete' }
      ]
    }
  ]
}

// 通知数据
const notifications = ref([
  { id: 1, title: '任务 "每日数据同步" 执行完成', time: '5分钟前', level: 'success' },
  { id: 2, title: '数据源 "MySQL生产库" 连接异常', time: '30分钟前', level: 'error' },
  { id: 3, title: '新版本 v2.3.0 可用于升级', time: '1小时前', level: 'info' }
])

// 计算属性
const currentModuleLabel = computed(() => {
  const mod = navModules.find(m => m.key === activeModule.value)
  return mod ? mod.label : ''
})

const currentSubMenus = computed(() => {
  const menus = moduleSubMenus[activeModule.value]
  if (!menus) return []
  if (activeModule.value === 'dev') {
    return menus[devSubTab.value] || []
  }
  return menus
})

const breadcrumbs = computed(() => {
  const crumbs = []
  const matched = route.matched.filter(r => r.meta?.title)
  for (const r of matched) {
    crumbs.push({ title: r.meta.title, path: r.path })
  }
  return crumbs
})

// 方法
const switchModule = (key) => {
  if (activeModule.value === key) {
    sidebarExpanded.value = !sidebarExpanded.value
    return
  }
  activeModule.value = key
  sidebarExpanded.value = true
  if (key === 'dev') devSubTab.value = 'scheduled'
  // 自动导航到该模块的第一个子菜单项
  const menus = moduleSubMenus[key]
  if (!menus) return
  const items = key === 'dev' ? (menus.scheduled || []) : menus
  if (items && items.length > 0 && items[0].items.length > 0) {
    navigateTo(items[0].items[0].path)
  }
}

const switchDevSubTab = (tab) => {
  devSubTab.value = tab
  const subMenus = moduleSubMenus.dev[tab]
  if (subMenus && subMenus.length > 0 && subMenus[0].items.length > 0) {
    navigateTo(subMenus[0].items[0].path)
  }
}

const toggleSidebar = () => {
  sidebarExpanded.value = !sidebarExpanded.value
}

const navigateTo = (path) => {
  if (route.path !== path) {
    router.push(path)
  }
}

const isActivePath = (path) => {
  if (route.path === path) return true
  return route.path.startsWith(path + '/')
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    console.log('Search:', searchKeyword.value)
  }
}

const toggleFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

// 根据当前路由自动切换模块
const syncModuleFromRoute = () => {
  const section = route.meta?.section
  if (section && navModules.find(m => m.key === section)) {
    activeModule.value = section
  }
}

watch(() => route.path, () => {
  syncModuleFromRoute()
})

onMounted(() => {
  syncModuleFromRoute()
})
</script>

<style lang="scss" scoped>
// ============================================
// FineDataLink 布局系统
// ============================================

$icon-bar-width: 60px;
$panel-width: 220px;
$header-height: 52px;
$primary-blue: #1890ff;
$primary-dark: #096dd9;
$bg-dark: #001529;
$bg-darker: #000c17;

.fdl-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f0f2f5;
}

// ============================================
// 侧边栏
// ============================================
.fdl-sidebar {
  display: flex;
  flex-shrink: 0;
  z-index: 100;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

// 图标导航栏
.icon-nav-bar {
  width: $icon-bar-width;
  background: $bg-dark;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.06);
}

.icon-nav-logo {
  width: 40px;
  height: 40px;
  margin: 12px 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;

  &:hover {
    background: rgba(255,255,255,0.1);
  }
}

.icon-nav-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 0; }
}

.icon-nav-bottom {
  padding: 8px;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.icon-nav-item {
  width: 44px;
  height: auto;
  min-height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  margin: 1px 0;
  padding: 4px 0;
  gap: 2px;

  .icon-nav-icon {
    color: rgba(255,255,255,0.55);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: color 0.2s;
    width: 28px;
    height: 28px;
  }

  .icon-nav-label {
    font-size: 9px;
    color: rgba(255,255,255,0.45);
    text-align: center;
    line-height: 1.1;
    max-width: 48px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    transition: color 0.2s;
  }

  &:hover {
    background: rgba(255,255,255,0.08);
    .icon-nav-icon { color: rgba(255,255,255,0.85); }
    .icon-nav-label { color: rgba(255,255,255,0.75); }
  }

  &.active {
    background: $primary-blue;
    .icon-nav-icon { color: #fff; }
    .icon-nav-label { color: rgba(255,255,255,0.9); }

    &::before {
      content: '';
      position: absolute;
      left: -8px;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      background: $primary-blue;
      border-radius: 0 3px 3px 0;
    }
  }
}

// 展开面板
.submenu-panel {
  width: $panel-width;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0,0,0,0.08);
  overflow: hidden;
}

.submenu-header {
  height: $header-height;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;

  .submenu-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
}

// Data Development 子导航标签
.sub-nav-tabs {
  display: flex;
  padding: 10px 12px;
  gap: 4px;
  background: #f5f7fa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;

  .sub-nav-tab {
    flex: 1;
    text-align: center;
    padding: 6px 0;
    font-size: 12px;
    font-weight: 500;
    color: #666;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { color: #333; background: rgba(24,144,255,0.04); }

    &.active {
      background: #fff;
      color: $primary-blue;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    }
  }
}

.submenu-scroll {
  flex: 1;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba(0,0,0,0.1);
    border-radius: 2px;
  }
}

.submenu-list {
  padding: 8px 0;
}

.submenu-group-title {
  padding: 12px 20px 6px;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.submenu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 20px;
  margin: 2px 8px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #555;
  transition: all 0.2s;

  .submenu-item-icon {
    color: #999;
    flex-shrink: 0;
  }

  &:hover {
    background: #f5f7fa;
    color: #1a1a1a;
  }

  &.active {
    background: #e6f4ff;
    color: $primary-blue;
    font-weight: 500;

    .submenu-item-icon {
      color: $primary-blue;
    }
  }
}

// 面板动画
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s;
  overflow: hidden;
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  width: 0 !important;
  opacity: 0;
}

// ============================================
// 主区域
// ============================================
.fdl-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

// 顶部栏
.fdl-header {
  height: $header-height;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  z-index: 10;
}

.header-left {
  flex: 1;
  max-width: 480px;
}

.header-search {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 0 12px;
  height: 34px;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:focus-within {
    background: #fff;
    border-color: $primary-blue;
    box-shadow: 0 0 0 2px rgba(24,144,255,0.1);
  }

  .search-icon {
    color: #999;
    font-size: 15px;
    flex-shrink: 0;
  }

  .search-input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    padding: 0 8px;
    font-size: 13px;
    color: #333;

    &::placeholder {
      color: #bbb;
    }
  }

  .search-clear {
    color: #999;
    cursor: pointer;
    font-size: 14px;

    &:hover { color: #666; }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-action {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;

  &:hover {
    background: #f5f7fa;
    color: $primary-blue;
  }

  :deep(.el-badge__content) {
    background: #ff4d4f;
    border: none;
  }
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  margin-left: 8px;

  &:hover { background: #f5f7fa; }

  .user-avatar {
    background: $primary-blue;
  }

  .user-name {
    font-size: 13px;
    color: #333;
    font-weight: 500;
  }

  .user-arrow {
    font-size: 11px;
    color: #999;
  }
}

// 面包屑
.fdl-breadcrumb {
  height: 36px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #f5f5f5;
  flex-shrink: 0;

  :deep(.el-breadcrumb) {
    font-size: 13px;

    .el-breadcrumb__inner {
      color: #999;
      &.is-link { color: $primary-blue; }
    }
    .el-breadcrumb__separator { color: #ccc; }
  }
}

// 内容区
.fdl-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f0f2f5;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba(0,0,0,0.12);
    border-radius: 3px;
    &:hover { background: rgba(0,0,0,0.2); }
  }
}

// 页面过渡
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.2s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

// ============================================
// 通知面板
// ============================================
.notice-list {
  .notice-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
    font-weight: 600;
    font-size: 14px;
  }
  .notice-empty {
    padding: 24px 0;
    text-align: center;
    color: #999;
    font-size: 13px;
  }
  .notice-item {
    display: flex;
    gap: 10px;
    padding: 12px 0;
    border-bottom: 1px solid #fafafa;
    cursor: pointer;
    &:hover { background: #fafafa; margin: 0 -12px; padding: 12px; border-radius: 6px; }
  }
  .notice-dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    margin-top: 6px;
    flex-shrink: 0;
    &.success { background: #52c41a; }
    &.error { background: #ff4d4f; }
    &.info { background: #1890ff; }
  }
  .notice-content {
    .notice-title { font-size: 13px; color: #333; line-height: 1.4; }
    .notice-time { font-size: 12px; color: #999; margin-top: 2px; }
  }
}

// 响应式
@media (max-width: 768px) {
  .submenu-panel { width: 180px; }
  .fdl-content { padding: 12px; }
  .fdl-header { padding: 0 12px; }
}
</style>
