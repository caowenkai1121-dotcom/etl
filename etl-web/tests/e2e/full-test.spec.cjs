const { test, expect } = require('@playwright/test')

const BASE_URL = 'http://localhost:3000'

// ==================== Part 1: 后端API全面测试 ====================
test.describe('Part 1: 后端API接口验证(30个)', () => {
  const apiTests = [
    // 监控模块 (6)
    { name: '[监控] 概览', url: '/api/monitor/overview', check: d => d.todaySuccess !== undefined },
    { name: '[监控] 趋势', url: '/api/monitor/trend?days=7', check: d => Array.isArray(d) },
    { name: '[监控] 执行记录', url: '/api/monitor/execution/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[监控] 线程池', url: '/api/monitor/thread-pool-status', check: d => d.corePoolSize !== undefined },
    { name: '[监控] 缓存', url: '/api/monitor/cache-status', check: d => d.tableInfoCache !== undefined },
    { name: '[监控] 系统信息', url: '/api/monitor/system-info', check: d => d.jvmMaxMemory !== undefined },
    // 数据源模块 (3)
    { name: '[数据源] 分页', url: '/api/datasource/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[数据源] 类型', url: '/api/datasource/types', check: d => Array.isArray(d) && d.includes('MYSQL') },
    { name: '[数据源] 列表', url: '/api/datasource/list', check: d => Array.isArray(d) },
    // 任务模块 (2)
    { name: '[任务] 分页', url: '/api/task/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[任务] 同步模式', url: '/api/task/sync-modes', check: d => Array.isArray(d) },
    // CDC模块 (2)
    { name: '[CDC] 列表', url: '/api/cdc-config/list', check: d => Array.isArray(d) },
    { name: '[CDC] 分页', url: '/api/cdc-config/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    // 转换模块 (3)
    { name: '[转换] 流水线分页', url: '/api/transform/pipeline?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[转换] 规则类型', url: '/api/transform/pipeline/rules', check: d => Array.isArray(d) },
    { name: '[转换] 规则列表', url: '/api/transform/rules', check: d => Array.isArray(d) },
    // 调度模块 (3)
    { name: '[调度] 任务列表', url: '/api/scheduler/tasks', check: d => Array.isArray(d) },
    { name: '[调度] DAG列表', url: '/api/scheduler/dag', check: d => Array.isArray(d) },
    { name: '[调度] DAG节点', url: '/api/scheduler/dag/1/nodes', check: d => Array.isArray(d) },
    // 告警模块 (4)
    { name: '[告警] 规则分页', url: '/api/alert/rule/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[告警] 渠道', url: '/api/alert/channels', check: d => Array.isArray(d) },
    { name: '[告警] 渠道列表', url: '/api/alert/channel/list', check: d => Array.isArray(d) },
    { name: '[告警] 记录分页', url: '/api/alert/record/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    // 质量模块 (1)
    { name: '[质量] 规则分页', url: '/api/quality/rule/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    // 日志模块 (4)
    { name: '[日志] 分页', url: '/api/log/page?pageNum=1&pageSize=10', check: d => d.list !== undefined },
    { name: '[日志] 统计概览', url: '/api/log/stats/overview', check: d => d.totalLogs !== undefined },
    { name: '[日志] 阶段统计', url: '/api/log/stats/by-stage', check: d => Array.isArray(d) },
    { name: '[日志] 错误趋势', url: '/api/log/stats/error-trend?days=7', check: d => Array.isArray(d) },
    // 系统模块 (2)
    { name: '[系统] 健康检查', url: '/api/health/detail', check: d => d.database !== undefined },
    { name: '[系统] 配置列表', url: '/api/config/list', check: d => d.list !== undefined },
  ]

  for (const api of apiTests) {
    test(api.name, async ({ request }) => {
      const response = await request.get(`${BASE_URL}${api.url}`)
      expect(response.status(), `${api.name} HTTP状态码`).toBe(200)
      const body = await response.json()
      expect(body.code, `${api.name} 业务码`).toBe(200)
      expect(body.success, `${api.name} success标志`).toBe(true)
      expect(api.check(body.data), `${api.name} 数据内容验证`).toBe(true)
    })
  }
})

// ==================== Part 2: 前端页面路由可达性 ====================
test.describe('Part 2: 前端页面路由可达性(17个)', () => {
  const routes = [
    { path: '/dashboard', title: '监控大盘' },
    { path: '/datasource', title: '数据源管理' },
    { path: '/task', title: '任务管理' },
    { path: '/cdc-config', title: 'CDC配置' },
    { path: '/scheduler', title: '调度管理' },
    { path: '/scheduler/dag', title: 'DAG编排' },
    { path: '/execution', title: '执行记录' },
    { path: '/etl/pipeline', title: '转换流水线' },
    { path: '/etl/rules', title: '转换规则' },
    { path: '/etl/debug', title: '调试预览' },
    { path: '/monitor', title: '实时监控' },
    { path: '/log', title: '日志查询' },
    { path: '/alert', title: '告警管理' },
    { path: '/quality', title: '数据质量' },
    { path: '/config', title: '配置中心' },
    { path: '/health', title: '健康检查' },
    { path: '/log/transform', title: '转换日志' },
  ]

  for (const route of routes) {
    test(`页面可达: ${route.path} (${route.title})`, async ({ request }) => {
      const response = await request.get(`${BASE_URL}${route.path}`)
      expect(response.status(), `${route.path} HTTP状态`).toBe(200)
      const body = await response.text()
      expect(body.length, `${route.path} 页面内容不为空`).toBeGreaterThan(100)
      expect(body, `${route.path} 应为HTML页面`).toContain('<div')
    })
  }
})

// ==================== Part 3: 前端页面API调用无500错误 ====================
test.describe('Part 3: 前端页面API调用无服务端错误(10个)', () => {
  const pages = [
    '/dashboard', '/datasource', '/task', '/cdc-config',
    '/monitor', '/log', '/alert', '/quality', '/config', '/health'
  ]

  for (const p of pages) {
    test(`${p} 无API 500错误`, async ({ page }) => {
      const serverErrors = []
      page.on('response', response => {
        if (response.url().includes('/api/') && response.status() >= 500) {
          serverErrors.push(`${response.status()} ${response.url()}`)
        }
      })

      await page.goto(`${BASE_URL}${p}`, { waitUntil: 'domcontentloaded', timeout: 20000 })
      await page.waitForTimeout(3000)

      expect(serverErrors, `${p} 不应有500错误`).toEqual([])
    })
  }
})

// ==================== Part 4: 前端页面无严重JS错误 ====================
test.describe('Part 4: 前端页面无严重JS错误(10个)', () => {
  const pages = [
    '/dashboard', '/datasource', '/task', '/cdc-config',
    '/monitor', '/log', '/alert', '/quality', '/config', '/health'
  ]

  for (const p of pages) {
    test(`${p} 无严重JS错误`, async ({ page }) => {
      const jsErrors = []
      page.on('pageerror', error => {
        jsErrors.push(error.message)
      })

      await page.goto(`${BASE_URL}${p}`, { waitUntil: 'domcontentloaded', timeout: 20000 })
      await page.waitForTimeout(3000)

      const criticalErrors = jsErrors.filter(e =>
        !e.includes('WebSocket') &&
        !e.includes('favicon') &&
        !e.includes('ResizeObserver')
      )
      expect(criticalErrors, `${p} 不应有严重JS错误`).toEqual([])
    })
  }
})

// ==================== Part 5: 关键页面UI元素验证 ====================
test.describe('Part 5: 关键页面UI元素验证', () => {
  test('仪表盘 - 统计卡片与快捷操作', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(3000)
    // 统计卡片
    const statCards = page.locator('.stat-card')
    const count = await statCards.count()
    expect(count, '仪表盘应有4个统计卡片').toBe(4)
    // 快捷操作
    await expect(page.getByText('新建数据源')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('创建任务')).toBeVisible()
  })

  test('数据源 - 搜索与新增按钮', async ({ page }) => {
    await page.goto(`${BASE_URL}/datasource`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByPlaceholder('请输入数据源名称')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('新增数据源')).toBeVisible()
  })

  test('任务管理 - 搜索与筛选', async ({ page }) => {
    await page.goto(`${BASE_URL}/task`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByPlaceholder('请输入任务名称')).toBeVisible({ timeout: 5000 })
    await expect(page.getByPlaceholder('同步模式')).toBeVisible()
    await expect(page.getByText('新增任务')).toBeVisible()
  })

  test('CDC配置 - 卡片布局', async ({ page }) => {
    await page.goto(`${BASE_URL}/cdc-config`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByPlaceholder('请输入连接器名称')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('新增CDC配置')).toBeVisible()
  })

  test('实时监控 - 图表区域', async ({ page }) => {
    await page.goto(`${BASE_URL}/monitor`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(3000)
    await expect(page.getByText('吞吐量曲线')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('同步延迟热力图')).toBeVisible()
    await expect(page.getByText('资源使用率')).toBeVisible()
    await expect(page.getByText('同步速率趋势')).toBeVisible()
  })

  test('告警管理 - Tab切换', async ({ page }) => {
    await page.goto(`${BASE_URL}/alert`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByText('告警列表')).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('规则管理')).toBeVisible()
    await expect(page.getByText('渠道配置')).toBeVisible()
    await expect(page.getByText('告警趋势')).toBeVisible()
  })

  test('健康检查 - 系统状态', async ({ page }) => {
    await page.goto(`${BASE_URL}/health`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByText(/系统正常|系统异常/)).toBeVisible({ timeout: 5000 })
    await expect(page.getByText('手动刷新')).toBeVisible()
  })

  test('配置中心 - 数据库配置', async ({ page }) => {
    await page.goto(`${BASE_URL}/config`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)
    await expect(page.getByText('数据库配置')).toBeVisible({ timeout: 5000 })
  })
})

// ==================== Part 6: 前端CRUD交互验证 ====================
test.describe('Part 6: 前端CRUD交互验证', () => {
  test('数据源 - 新增表单填写与提交', async ({ page }) => {
    await page.goto(`${BASE_URL}/datasource`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)

    await page.getByText('新增数据源').click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })

    // 填写所有必填字段
    await page.getByPlaceholder('请输入名称').fill('E2E自动测试数据源')
    await page.locator('.el-dialog').getByPlaceholder('请输入主机地址').fill('10.0.0.1')
    await page.locator('.el-dialog').getByPlaceholder('请输入数据库名称').fill('e2e_db')
    await page.locator('.el-dialog').getByPlaceholder('请输入用户名').fill('admin')

    // 提交
    await page.locator('.el-dialog').getByText('确定').click()
    await page.waitForTimeout(2000)

    // 验证成功
    await expect(page.getByText('操作成功')).toBeVisible({ timeout: 5000 })
  })

  test('任务 - 新增表单验证', async ({ page }) => {
    await page.goto(`${BASE_URL}/task`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)

    await page.getByText('新增任务').click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })

    // 验证表单分区
    await expect(page.getByText('基础信息')).toBeVisible()
    await expect(page.getByText('源目标选择')).toBeVisible()
    await expect(page.getByText('同步配置')).toBeVisible()

    await page.getByPlaceholder('请输入任务名称').fill('E2E自动测试任务')
    await page.locator('.el-dialog').getByText('取消').click()
  })

  test('告警 - 新增规则表单', async ({ page }) => {
    await page.goto(`${BASE_URL}/alert`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)

    await page.getByText('规则管理').click()
    await page.waitForTimeout(2000)

    await page.getByText('新增规则').click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })

    await page.getByPlaceholder('输入规则名称').fill('E2E自动测试规则')
    await page.getByPlaceholder('JSON格式条件').fill('{"test": true}')
    await page.locator('.el-dialog').getByText('取消').click()
  })

  test('CDC - 新增配置表单', async ({ page }) => {
    await page.goto(`${BASE_URL}/cdc-config`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)

    await page.getByText('新增CDC配置').click()
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })

    await page.getByPlaceholder('请输入配置名称').fill('E2E自动测试CDC')
    await page.getByPlaceholder('全局唯一，如: etl-connector-mysql-1').fill('e2e-test-connector')
    await page.locator('.el-dialog').getByText('取消').click()
  })
})

// ==================== Part 7: 侧边栏导航 ====================
test.describe('Part 7: 侧边栏导航', () => {
  test('所有一级菜单可点击导航', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(2000)

    const navItems = [
      { text: '监控大盘', path: '/dashboard' },
      { text: '数据源管理', path: '/datasource' },
      { text: '任务管理', path: '/task' },
      { text: 'CDC配置', path: '/cdc-config' },
    ]

    for (const item of navItems) {
      const menu = page.getByText(item.text).first()
      if (await menu.isVisible()) {
        await menu.click()
        await page.waitForTimeout(2000)
        expect(page.url(), `点击"${item.text}"应跳转到${item.path}`).toContain(item.path)
      }
    }
  })
})
