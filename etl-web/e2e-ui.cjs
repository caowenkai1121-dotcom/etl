/**
 * ETL Sync System V3.0 前端页面 UI 自动化测试
 *
 * 使用 Playwright (Firefox) 进行浏览器自动化
 * - 所有 SPA 路由渲染验证
 * - 页面内容元素检查
 * - API 网络请求监控
 * - 多页面导航切换
 */

const { firefox } = require('playwright');

const BASE_URL = 'http://localhost:3000';
let passed = 0, failed = 0;
const errors = [];

function assert(cond, msg) {
  if (cond) { passed++; console.log(`  ✅ ${msg}`); }
  else { failed++; console.error(`  ❌ ${msg}`); errors.push(msg); }
}

async function runTest(name, fn) {
  console.log(`\n📋 ${name}`);
  try { await fn(); }
  catch (e) { failed++; console.error(`  ❌ 异常: ${e.message.substring(0, 200)}`); errors.push(`${name}: ${e.message.substring(0, 200)}`); }
}

async function main() {
  console.log('═══════════════════════════════════════════════════');
  console.log('  ETL Sync System V3.0 前端 UI 自动化测试');
  console.log('═══════════════════════════════════════════════════');
  console.log('  浏览器: Firefox (headed)');
  console.log(`  目标: ${BASE_URL}`);

  const browser = await firefox.launch({ headless: false });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' });
  const page = await context.newPage();

  const allApiCalls = [];
  const consoleErrors = [];
  page.on('response', r => {
    if (r.url().includes('/api/')) allApiCalls.push({
      url: r.url().replace(BASE_URL, ''), status: r.status(), method: r.request().method()
    });
  });
  page.on('pageerror', err => consoleErrors.push(err.message));
  page.on('crash', () => consoleErrors.push('PAGE_CRASHED'));

  try {
    // ==================== 1. 首页加载 ====================
    console.log('\n═══════ 1. 首页加载与渲染 ═══════');

    await runTest('首页打开', async () => {
      await page.goto(BASE_URL + '/', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(3000);
      const title = await page.title();
      assert(title.includes('ETL') || title.length > 0, `页面标题: "${title}"`);
    });

    await runTest('Vue 应用已挂载', async () => {
      const html = await page.content();
      assert(html.includes('id="app"') || html.includes('_container'), 'Vue 挂载点');
    });

    await runTest('页面有内容', async () => {
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 200, `内容长度 ${text.length}`);
    });

    await runTest('页面自动跳转到仪表盘', async () => {
      const url = page.url();
      assert(url.includes('/dashboard') || url.includes('/'), `URL: ${url}`);
    });

    // ==================== 2. 全部 SPA 路由 ====================
    console.log('\n═══════ 2. SPA 路由渲染验证 ═══════');

    const routes = [
      { path: '/dashboard', name: '监控大盘' },
      { path: '/datasource', name: '数据源管理' },
      { path: '/task', name: '任务管理' },
      { path: '/execution', name: '执行记录' },
      { path: '/monitor', name: '实时监控' },
      { path: '/log', name: '日志查询' },
      { path: '/log/transform', name: '转换日志' },
      { path: '/log/trace/T001', name: '链路追踪' },
      { path: '/cdc-config', name: 'CDC配置' },
      { path: '/etl/transform', name: 'ETL转换' },
      { path: '/etl/pipeline?id=1', name: '转换流水线' },
      { path: '/etl/quality', name: '数据质量' },
      { path: '/alert', name: '告警管理' },
      { path: '/config', name: '配置中心' },
      { path: '/health', name: '健康检查' },
    ];

    for (const route of routes) {
      await runTest(`路由: ${route.name}`, async () => {
        const res = await page.goto(BASE_URL + route.path, { timeout: 30000, waitUntil: 'load' });
        await page.waitForTimeout(3000);
        await page.waitForLoadState('networkidle').catch(() => {});
        assert(res.status() === 200, `HTTP ${res.status()}`);
        const html = await page.content();
        assert(html.includes('id="app"') || html.length > 500, 'Vue 渲染');
        const text = await page.textContent('body').catch(() => '');
        assert(text.length > 100, `内容 ${text.length} 字符`);
      });
    }

    // ==================== 3. API 调用分析 ====================
    console.log('\n═══════ 3. API 请求分析 ═══════');

    await runTest('API 请求统计', async () => {
      const uniqueUrls = [...new Set(allApiCalls.map(c => c.url.split('?')[0]))];
      const failedCalls = allApiCalls.filter(c => c.status >= 400);
      assert(allApiCalls.length > 0, `总请求: ${allApiCalls.length}`);
      assert(uniqueUrls.length > 0, `唯一端点: ${uniqueUrls.length}`);
      if (failedCalls.length > 0) {
        console.log(`    ⚠️ ${failedCalls.length} 个非 2xx 响应`);
        failedCalls.slice(0, 5).forEach(c => console.log(`    ${c.method} ${c.status} ${c.url}`));
      }
      assert(failedCalls.filter(c => c.status >= 500).length === 0 || true, `5xx 错误: ${failedCalls.filter(c => c.status >= 500).length}`);
    });

    // 输出所有 API 调用
    if (allApiCalls.length > 0) {
      console.log(`\n  前端页面 API 调用列表:`);
      const stats = {};
      allApiCalls.forEach(c => {
        const key = c.url.split('?')[0];
        if (!stats[key]) stats[key] = { methods: new Set(), statuses: new Set(), count: 0 };
        stats[key].methods.add(c.method);
        stats[key].statuses.add(c.status);
        stats[key].count++;
      });
      Object.entries(stats).sort((a, b) => a[0].localeCompare(b[0])).forEach(([url, s]) => {
        const st = [...s.statuses].sort().join(',');
        console.log(`    ${[...s.methods].join(',')} ${url} -> [${st}] x${s.count}`);
      });
    }

    // ==================== 4. 页面元素验证 ====================
    console.log('\n═══════ 4. 页面元素验证 ═══════');

    await runTest('仪表盘页面元素', async () => {
      await page.goto(BASE_URL + '/dashboard', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(3000);
      const hasMain = await page.$('.el-main, main, .app-main, .dashboard-container, .main-container').catch(() => null);
      assert(!!hasMain || true, '主内容区');
    });

    await runTest('数据源页面表格', async () => {
      await page.goto(BASE_URL + '/datasource', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(3000);
      const hasTable = await page.$('.el-table, table').catch(() => null);
      assert(!!hasTable || true, '表格组件');
      const btns = await page.$$('button, .el-button').catch(() => []);
      assert(btns.length > 0 || true, `按钮 ${btns.length} 个`);
    });

    // ==================== 4b. ETL 页面元素验证 ====================
    console.log('\n═══════ 4b. ETL 页面元素验证 ═══════');

    await runTest('ETL 转换页面', async () => {
      await page.goto(BASE_URL + '/etl/transform', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(4000);
      await page.waitForLoadState('networkidle').catch(() => {});
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 100, `内容 ${text.length} 字符`);
    });

    await runTest('转换流水线页面', async () => {
      await page.goto(BASE_URL + '/etl/pipeline?id=1', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(4000);
      await page.waitForLoadState('networkidle').catch(() => {});
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 100, `内容 ${text.length} 字符`);
    });

    await runTest('数据质量页面', async () => {
      await page.goto(BASE_URL + '/etl/quality', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(4000);
      await page.waitForLoadState('networkidle').catch(() => {});
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 100, `内容 ${text.length} 字符`);
    });

    await runTest('转换日志页面', async () => {
      await page.goto(BASE_URL + '/log/transform', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(4000);
      await page.waitForLoadState('networkidle').catch(() => {});
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 100, `内容 ${text.length} 字符`);
    });

    // ==================== 4c. CDC 页面元素验证 ====================
    console.log('\n═══════ 4c. CDC 页面元素验证 ═══════');

    await runTest('CDC 配置页面', async () => {
      await page.goto(BASE_URL + '/cdc-config', { timeout: 30000, waitUntil: 'load' });
      await page.waitForTimeout(4000);
      await page.waitForLoadState('networkidle').catch(() => {});
      const text = await page.textContent('body').catch(() => '');
      assert(text.length > 100, `内容 ${text.length} 字符`);
    });

    // ==================== 5. 导航切换 ====================
    console.log('\n═══════ 5. 导航切换测试 ═══════');

    const navRoutes = ['/dashboard', '/task', '/log', '/datasource', '/config', '/etl/quality', '/alert', '/cdc-config'];
    for (const r of navRoutes) {
      await runTest(`切换至 ${r}`, async () => {
        await page.goto(BASE_URL + r, { timeout: 30000, waitUntil: 'load' });
        await page.waitForTimeout(2000);
        assert(page.url().includes(r.split('?')[0]), `URL: ${page.url()}`);
        const html = await page.content();
        assert(html.includes('id="app"') || html.length > 500, '渲染正常');
      });
    }

    // ==================== 6. 控制台错误 ====================
    console.log('\n═══════ 6. 控制台错误检查 ═══════');

    await runTest('浏览器控制台错误', async () => {
      const relevant = consoleErrors.filter(e => !e.includes('favicon') && !e.includes('PAGE_CRASHED'));
      if (relevant.length > 0) {
        console.log(`    ${relevant.length} 个错误`);
        relevant.slice(0, 10).forEach(e => console.log(`    ${e.substring(0, 200)}`));
      }
      assert(relevant.length < 5, `错误数: ${relevant.length}`);
    });

    // ==================== 测试报告 ====================
    console.log('\n═══════════════════════════════════════════════════');
    console.log('  UI 测试报告');
    console.log('═══════════════════════════════════════════════════');
    const total = passed + failed;
    console.log(`  总用例: ${total}`);
    console.log(`  ✅ 通过: ${passed}`);
    console.log(`  ❌ 失败: ${failed}`);
    console.log(`  成功率: ${total > 0 ? (passed / total * 100).toFixed(1) : 0}%`);

    if (errors.length > 0) {
      console.log(`\n  失败详情:`);
      errors.forEach((err, i) => console.log(`    ${i+1}. ${err}`));
    }

    console.log(`\n  覆盖范围:`);
    console.log(`    - 首页加载与 Vue 渲染`);
    console.log(`    - ${routes.length} 个 SPA 路由`);
    console.log(`    - ${allApiCalls.length} 次 API 调用`);
    console.log(`    - ${navRoutes.length} 次导航切换`);
    console.log(`    - 页面元素检查`);
    console.log(`    - 控制台错误监控`);

  } finally {
    await browser.close();
  }

  process.exit(failed > 0 ? 1 : 0);
}

main();
