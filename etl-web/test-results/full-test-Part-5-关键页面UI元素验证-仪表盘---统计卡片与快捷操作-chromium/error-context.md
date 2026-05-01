# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: full-test.spec.cjs >> Part 5: 关键页面UI元素验证 >> 仪表盘 - 统计卡片与快捷操作
- Location: tests\e2e\full-test.spec.cjs:148:3

# Error details

```
Error: locator.count: Target crashed 
```

# Test source

```ts
  53  |       expect(response.status(), `${api.name} HTTP状态码`).toBe(200)
  54  |       const body = await response.json()
  55  |       expect(body.code, `${api.name} 业务码`).toBe(200)
  56  |       expect(body.success, `${api.name} success标志`).toBe(true)
  57  |       expect(api.check(body.data), `${api.name} 数据内容验证`).toBe(true)
  58  |     })
  59  |   }
  60  | })
  61  | 
  62  | // ==================== Part 2: 前端页面路由可达性 ====================
  63  | test.describe('Part 2: 前端页面路由可达性(17个)', () => {
  64  |   const routes = [
  65  |     { path: '/dashboard', title: '监控大盘' },
  66  |     { path: '/datasource', title: '数据源管理' },
  67  |     { path: '/task', title: '任务管理' },
  68  |     { path: '/cdc-config', title: 'CDC配置' },
  69  |     { path: '/scheduler', title: '调度管理' },
  70  |     { path: '/scheduler/dag', title: 'DAG编排' },
  71  |     { path: '/execution', title: '执行记录' },
  72  |     { path: '/etl/pipeline', title: '转换流水线' },
  73  |     { path: '/etl/rules', title: '转换规则' },
  74  |     { path: '/etl/debug', title: '调试预览' },
  75  |     { path: '/monitor', title: '实时监控' },
  76  |     { path: '/log', title: '日志查询' },
  77  |     { path: '/alert', title: '告警管理' },
  78  |     { path: '/quality', title: '数据质量' },
  79  |     { path: '/config', title: '配置中心' },
  80  |     { path: '/health', title: '健康检查' },
  81  |     { path: '/log/transform', title: '转换日志' },
  82  |   ]
  83  | 
  84  |   for (const route of routes) {
  85  |     test(`页面可达: ${route.path} (${route.title})`, async ({ request }) => {
  86  |       const response = await request.get(`${BASE_URL}${route.path}`)
  87  |       expect(response.status(), `${route.path} HTTP状态`).toBe(200)
  88  |       const body = await response.text()
  89  |       expect(body.length, `${route.path} 页面内容不为空`).toBeGreaterThan(100)
  90  |       expect(body, `${route.path} 应为HTML页面`).toContain('<div')
  91  |     })
  92  |   }
  93  | })
  94  | 
  95  | // ==================== Part 3: 前端页面API调用无500错误 ====================
  96  | test.describe('Part 3: 前端页面API调用无服务端错误(10个)', () => {
  97  |   const pages = [
  98  |     '/dashboard', '/datasource', '/task', '/cdc-config',
  99  |     '/monitor', '/log', '/alert', '/quality', '/config', '/health'
  100 |   ]
  101 | 
  102 |   for (const p of pages) {
  103 |     test(`${p} 无API 500错误`, async ({ page }) => {
  104 |       const serverErrors = []
  105 |       page.on('response', response => {
  106 |         if (response.url().includes('/api/') && response.status() >= 500) {
  107 |           serverErrors.push(`${response.status()} ${response.url()}`)
  108 |         }
  109 |       })
  110 | 
  111 |       await page.goto(`${BASE_URL}${p}`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  112 |       await page.waitForTimeout(3000)
  113 | 
  114 |       expect(serverErrors, `${p} 不应有500错误`).toEqual([])
  115 |     })
  116 |   }
  117 | })
  118 | 
  119 | // ==================== Part 4: 前端页面无严重JS错误 ====================
  120 | test.describe('Part 4: 前端页面无严重JS错误(10个)', () => {
  121 |   const pages = [
  122 |     '/dashboard', '/datasource', '/task', '/cdc-config',
  123 |     '/monitor', '/log', '/alert', '/quality', '/config', '/health'
  124 |   ]
  125 | 
  126 |   for (const p of pages) {
  127 |     test(`${p} 无严重JS错误`, async ({ page }) => {
  128 |       const jsErrors = []
  129 |       page.on('pageerror', error => {
  130 |         jsErrors.push(error.message)
  131 |       })
  132 | 
  133 |       await page.goto(`${BASE_URL}${p}`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  134 |       await page.waitForTimeout(3000)
  135 | 
  136 |       const criticalErrors = jsErrors.filter(e =>
  137 |         !e.includes('WebSocket') &&
  138 |         !e.includes('favicon') &&
  139 |         !e.includes('ResizeObserver')
  140 |       )
  141 |       expect(criticalErrors, `${p} 不应有严重JS错误`).toEqual([])
  142 |     })
  143 |   }
  144 | })
  145 | 
  146 | // ==================== Part 5: 关键页面UI元素验证 ====================
  147 | test.describe('Part 5: 关键页面UI元素验证', () => {
  148 |   test('仪表盘 - 统计卡片与快捷操作', async ({ page }) => {
  149 |     await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  150 |     await page.waitForTimeout(3000)
  151 |     // 统计卡片
  152 |     const statCards = page.locator('.stat-card')
> 153 |     const count = await statCards.count()
      |                                   ^ Error: locator.count: Target crashed 
  154 |     expect(count, '仪表盘应有4个统计卡片').toBe(4)
  155 |     // 快捷操作
  156 |     await expect(page.getByText('新建数据源')).toBeVisible({ timeout: 5000 })
  157 |     await expect(page.getByText('创建任务')).toBeVisible()
  158 |   })
  159 | 
  160 |   test('数据源 - 搜索与新增按钮', async ({ page }) => {
  161 |     await page.goto(`${BASE_URL}/datasource`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  162 |     await page.waitForTimeout(2000)
  163 |     await expect(page.getByPlaceholder('请输入数据源名称')).toBeVisible({ timeout: 5000 })
  164 |     await expect(page.getByText('新增数据源')).toBeVisible()
  165 |   })
  166 | 
  167 |   test('任务管理 - 搜索与筛选', async ({ page }) => {
  168 |     await page.goto(`${BASE_URL}/task`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  169 |     await page.waitForTimeout(2000)
  170 |     await expect(page.getByPlaceholder('请输入任务名称')).toBeVisible({ timeout: 5000 })
  171 |     await expect(page.getByPlaceholder('同步模式')).toBeVisible()
  172 |     await expect(page.getByText('新增任务')).toBeVisible()
  173 |   })
  174 | 
  175 |   test('CDC配置 - 卡片布局', async ({ page }) => {
  176 |     await page.goto(`${BASE_URL}/cdc-config`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  177 |     await page.waitForTimeout(2000)
  178 |     await expect(page.getByPlaceholder('请输入连接器名称')).toBeVisible({ timeout: 5000 })
  179 |     await expect(page.getByText('新增CDC配置')).toBeVisible()
  180 |   })
  181 | 
  182 |   test('实时监控 - 图表区域', async ({ page }) => {
  183 |     await page.goto(`${BASE_URL}/monitor`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  184 |     await page.waitForTimeout(3000)
  185 |     await expect(page.getByText('吞吐量曲线')).toBeVisible({ timeout: 5000 })
  186 |     await expect(page.getByText('同步延迟热力图')).toBeVisible()
  187 |     await expect(page.getByText('资源使用率')).toBeVisible()
  188 |     await expect(page.getByText('同步速率趋势')).toBeVisible()
  189 |   })
  190 | 
  191 |   test('告警管理 - Tab切换', async ({ page }) => {
  192 |     await page.goto(`${BASE_URL}/alert`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  193 |     await page.waitForTimeout(2000)
  194 |     await expect(page.getByText('告警列表')).toBeVisible({ timeout: 5000 })
  195 |     await expect(page.getByText('规则管理')).toBeVisible()
  196 |     await expect(page.getByText('渠道配置')).toBeVisible()
  197 |     await expect(page.getByText('告警趋势')).toBeVisible()
  198 |   })
  199 | 
  200 |   test('健康检查 - 系统状态', async ({ page }) => {
  201 |     await page.goto(`${BASE_URL}/health`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  202 |     await page.waitForTimeout(2000)
  203 |     await expect(page.getByText(/系统正常|系统异常/)).toBeVisible({ timeout: 5000 })
  204 |     await expect(page.getByText('手动刷新')).toBeVisible()
  205 |   })
  206 | 
  207 |   test('配置中心 - 数据库配置', async ({ page }) => {
  208 |     await page.goto(`${BASE_URL}/config`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  209 |     await page.waitForTimeout(2000)
  210 |     await expect(page.getByText('数据库配置')).toBeVisible({ timeout: 5000 })
  211 |   })
  212 | })
  213 | 
  214 | // ==================== Part 6: 前端CRUD交互验证 ====================
  215 | test.describe('Part 6: 前端CRUD交互验证', () => {
  216 |   test('数据源 - 新增表单填写与提交', async ({ page }) => {
  217 |     await page.goto(`${BASE_URL}/datasource`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  218 |     await page.waitForTimeout(2000)
  219 | 
  220 |     await page.getByText('新增数据源').click()
  221 |     await page.waitForTimeout(1000)
  222 |     await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })
  223 | 
  224 |     // 填写所有必填字段
  225 |     await page.getByPlaceholder('请输入名称').fill('E2E自动测试数据源')
  226 |     await page.locator('.el-dialog').getByPlaceholder('请输入主机地址').fill('10.0.0.1')
  227 |     await page.locator('.el-dialog').getByPlaceholder('请输入数据库名称').fill('e2e_db')
  228 |     await page.locator('.el-dialog').getByPlaceholder('请输入用户名').fill('admin')
  229 | 
  230 |     // 提交
  231 |     await page.locator('.el-dialog').getByText('确定').click()
  232 |     await page.waitForTimeout(2000)
  233 | 
  234 |     // 验证成功
  235 |     await expect(page.getByText('操作成功')).toBeVisible({ timeout: 5000 })
  236 |   })
  237 | 
  238 |   test('任务 - 新增表单验证', async ({ page }) => {
  239 |     await page.goto(`${BASE_URL}/task`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  240 |     await page.waitForTimeout(2000)
  241 | 
  242 |     await page.getByText('新增任务').click()
  243 |     await page.waitForTimeout(1000)
  244 |     await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 5000 })
  245 | 
  246 |     // 验证表单分区
  247 |     await expect(page.getByText('基础信息')).toBeVisible()
  248 |     await expect(page.getByText('源目标选择')).toBeVisible()
  249 |     await expect(page.getByText('同步配置')).toBeVisible()
  250 | 
  251 |     await page.getByPlaceholder('请输入任务名称').fill('E2E自动测试任务')
  252 |     await page.locator('.el-dialog').getByText('取消').click()
  253 |   })
```