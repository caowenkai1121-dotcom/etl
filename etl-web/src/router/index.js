import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      // ========== Demo中心 ==========
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { section: 'demo', title: '监控大盘' }
      },
      {
        path: 'demo-center',
        name: 'DemoCenter',
        component: () => import('@/views/demo-center/index.vue'),
        meta: { section: 'demo', title: 'Demo中心' }
      },
      {
        path: 'demo/quickstart',
        redirect: '/demo-center',
        meta: { section: 'demo', title: '快速入门' }
      },
      {
        path: 'demo/templates',
        name: 'DemoTemplates',
        component: () => import('@/views/task-templates/index.vue'),
        meta: { section: 'demo', title: '任务模板' }
      },

      // ========== O&M Center ==========
      {
        path: 'ops-center',
        name: 'OpsCenter',
        component: () => import('@/views/ops-center/index.vue'),
        meta: { section: 'ops', title: '运维中心' }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { section: 'ops', title: '实时监控' }
      },
      {
        path: 'log',
        name: 'Log',
        component: () => import('@/views/log/index.vue'),
        meta: { section: 'ops', title: '日志查询' }
      },
      {
        path: 'log/transform',
        name: 'TransformLog',
        component: () => import('@/views/log/transform.vue'),
        meta: { section: 'ops', title: '转换日志' }
      },
      {
        path: 'log/trace/:traceId',
        name: 'TraceDetail',
        component: () => import('@/views/log/trace.vue'),
        meta: { section: 'ops', title: '链路追踪' }
      },
      {
        path: 'alert',
        name: 'Alert',
        component: () => import('@/views/alert/index.vue'),
        meta: { section: 'ops', title: '告警管理' }
      },
      {
        path: 'health',
        name: 'Health',
        component: () => import('@/views/health/index.vue'),
        meta: { section: 'ops', title: '健康检查' }
      },

      // ========== Data Pipeline ==========
      {
        path: 'data-pipeline',
        name: 'DataPipeline',
        component: () => import('@/views/data-pipeline/index.vue'),
        meta: { section: 'pipeline', title: '数据管道' }
      },
      {
        path: 'scheduler',
        name: 'Scheduler',
        component: () => import('@/views/execution/index.vue'),
        meta: { section: 'pipeline', title: '调度管理' }
      },
      {
        path: 'execution',
        name: 'Execution',
        component: () => import('@/views/execution/index.vue'),
        meta: { section: 'pipeline', title: '执行记录' }
      },
      {
        path: 'etl/pipeline',
        name: 'EtlPipeline',
        component: () => import('@/views/etl/pipeline/index.vue'),
        meta: { section: 'pipeline', title: '任务编排' }
      },
      {
        path: 'quality',
        name: 'Quality',
        component: () => import('@/views/etl/quality/index.vue'),
        meta: { section: 'pipeline', title: '数据质量' }
      },

      // ========== Data Development ==========
      {
        path: 'dev/workbench',
        name: 'DevWorkbenchHome',
        component: () => import('@/views/dev-workbench/Workbench.vue'),
        meta: { section: 'dev', title: '工作台首页' }
      },
      {
        path: 'dev/task/new',
        name: 'DevTaskNew',
        component: () => import('@/views/dev-workbench/index.vue'),
        meta: { section: 'dev', title: '新建任务' }
      },
      {
        path: 'dev/task/:id',
        name: 'DevTaskEditor',
        component: () => import('@/views/dev-workbench/index.vue'),
        meta: { section: 'dev', title: '任务编辑' }
      },

      // ========== Data Development - Real-Time Task ==========
      {
        path: 'realtime/workbench',
        name: 'RealtimeWorkbench',
        component: () => import('@/views/dev-workbench/RealtimeWorkbench.vue'),
        meta: { section: 'dev', title: '实时任务首页' }
      },
      {
        path: 'realtime/task/new',
        name: 'RealtimeTaskNew',
        component: () => import('@/views/dev-workbench/RealtimeTaskEditor.vue'),
        meta: { section: 'dev', title: '新建实时管道' }
      },
      {
        path: 'realtime/task/:id',
        name: 'RealtimeTaskEditor',
        component: () => import('@/views/dev-workbench/RealtimeTaskEditor.vue'),
        meta: { section: 'dev', title: '实时管道编辑' }
      },
      {
        path: 'datasource',
        name: 'Datasource',
        component: () => import('@/views/datasource/index.vue'),
        meta: { section: 'dev', title: '数据源管理' }
      },
      {
        path: 'etl/rules',
        name: 'EtlRules',
        component: () => import('@/views/etl/transform/index.vue'),
        meta: { section: 'dev', title: '转换规则' }
      },
      {
        path: 'etl/debug',
        name: 'EtlDebug',
        component: () => import('@/views/etl/transform/index.vue'),
        meta: { section: 'dev', title: '调试预览' }
      },
      {
        path: 'cdc-config',
        name: 'CdcConfig',
        component: () => import('@/views/cdc-config/index.vue'),
        meta: { section: 'dev', title: 'CDC配置' }
      },
      {
        path: 'task',
        name: 'Task',
        component: () => import('@/views/task/index.vue'),
        meta: { section: 'dev', title: '任务列表' }
      },
      {
        path: 'workflow',
        name: 'Workflow',
        component: () => import('@/views/workflow/index.vue'),
        meta: { section: 'dev', title: '工作流' }
      },

      // ========== Data Service ==========
      {
        path: 'api-service',
        name: 'ApiService',
        component: () => import('@/views/api-service/index.vue'),
        meta: { section: 'service', title: 'API服务管理' }
      },
      {
        path: 'api-service/editor/:id?',
        name: 'ApiEditor',
        component: () => import('@/views/api-service/editor.vue'),
        meta: { section: 'service', title: 'API编辑' }
      },
      {
        path: 'publish',
        name: 'Publish',
        component: () => import('@/views/publish/index.vue'),
        meta: { section: 'service', title: '发布管理' }
      },

      // ========== Data Management ==========
      {
        path: 'data-management',
        name: 'DataManagement',
        component: () => import('@/views/data-management/index.vue'),
        meta: { section: 'data', title: '数据管理' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('@/views/config/index.vue'),
        meta: { section: 'data', title: '配置中心' }
      },

      // ========== System Management ==========
      {
        path: 'system',
        name: 'SystemManagement',
        component: () => import('@/views/system/index.vue'),
        meta: { section: 'system', title: '系统管理' }
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/index.vue'),
        meta: { section: 'system', title: '用户管理' }
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/index.vue'),
        meta: { section: 'system', title: '角色管理' }
      },
      {
        path: 'system/settings',
        name: 'SystemSettings',
        component: () => import('@/views/system/index.vue'),
        meta: { section: 'system', title: '系统设置' }
      },

      // ========== Recycle Bin ==========
      {
        path: 'recycle',
        name: 'RecycleBin',
        component: () => import('@/views/recycle-bin/index.vue'),
        meta: { section: 'recycle', title: '回收站' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
