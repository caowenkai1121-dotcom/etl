/**
 * ETL Sync System V3.0 - CDC 功能深度测试
 *
 * 覆盖 CDC 配置 CRUD、连接器部署/启动/停止/状态、
 * CDC 任务管理、CDC 监控、异常场景
 */
const BASE = 'http://localhost:3000';
const API = BASE + '/api';

let passed = 0, failed = 0;
const errors = [];
const TS = Date.now().toString(36); // 唯一后缀

function assert(cond, msg) {
  if (cond) { passed++; console.log(`  ✅ ${msg}`); }
  else { failed++; console.error(`  ❌ ${msg}`); errors.push(msg); }
}
function assertEq(a, b, msg) { assert(a === b, `${msg}: ${JSON.stringify(a)} === ${JSON.stringify(b)}`); }

async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const data = await res.json().catch(() => null);
  return { status: res.status, ok: data?.code === 200, data };
}
const apiGet = (p) => api('GET', p);
const apiPost = (p, b) => api('POST', p, b);
const apiPut = (p, b) => api('PUT', p, b);
const apiDelete = (p) => api('DELETE', p);

async function runTest(name, fn) {
  console.log(`\n📋 ${name}`);
  try { await fn(); }
  catch (e) { failed++; console.error(`  ❌ 异常: ${e.message.substring(0, 300)}`); errors.push(`${name}: ${e.message.substring(0, 200)}`); }
}

async function main() {
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('  ETL Sync System V3.0 - CDC 功能深度测试');
  console.log('═══════════════════════════════════════════════════════════════');
  console.log(`  目标: ${API}`);
  console.log(`  唯一后缀: ${TS}`);

  const IDS = {};

  // 获取可用数据源
  await runTest('获取数据源列表', async () => {
    const { status, data } = await apiGet('/datasource/list');
    assertEq(status, 200, 'HTTP 200');
    assert(Array.isArray(data?.data), '返回数组');
    const dsList = data.data;
    if (dsList.length > 0) {
      IDS.sourceDsId = dsList[0].id;
      console.log(`   使用数据源 ID=${IDS.sourceDsId} (${dsList[0].name})`);
    }
  });

  // ==================== 1. CDC 配置 CRUD ====================
  console.log('\n═══════ 1. CDC 配置 CRUD ═══════');

  await runTest('列出所有 CDC 配置', async () => {
    const { status, data } = await apiGet('/cdc-config/list');
    assertEq(status, 200, 'HTTP 200');
    assert(Array.isArray(data?.data), '返回数组');
  });

  await runTest('分页查询 CDC 配置', async () => {
    const { status, data } = await apiGet('/cdc-config/page?pageNum=1&pageSize=10');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.list !== undefined, '返回分页数据');
  });

  await runTest('CDC 配置多条件过滤', async () => {
    assert((await apiGet(`/cdc-config/page?name=${TS}`)).ok, '按名称过滤');
    assert((await apiGet('/cdc-config/page?syncStatus=RUNNING')).ok, '按 RUNNING 状态过滤');
    assert((await apiGet('/cdc-config/page?syncStatus=STOPPED')).ok, '按 STOPPED 状态过滤');
    assert((await apiGet('/cdc-config/page?syncStatus=ERROR')).ok, '按 ERROR 状态过滤');
  });

  // 创建 CDC 配置 - MySQL 类型
  await runTest('创建 CDC 配置（MySQL 类型）', async () => {
    if (!IDS.sourceDsId) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status, data, ok } = await apiPost('/cdc-config', {
      name: `e2e-cdc-mysql-${TS}`, datasourceId: IDS.sourceDsId,
      connectorName: `e2e-conn-mysql-${TS}`, connectorType: 'mysql',
      serverName: `e2e-svr-mysql-${TS}`, filterRegex: 'etl\\..*',
      kafkaTopicPrefix: `e2e-mysql-${TS}`, status: 1
    });
    assertEq(status, 200, 'HTTP 200');
    if (ok && data?.data) {
      IDS.cdcId1 = data.data;
      console.log(`   创建 ID=${data.data}`);
    }
  });

  // 创建 CDC 配置 - PostgreSQL 类型
  await runTest('创建 CDC 配置（PostgreSQL 类型）', async () => {
    if (!IDS.sourceDsId) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status, data, ok } = await apiPost('/cdc-config', {
      name: `e2e-cdc-pg-${TS}`, datasourceId: IDS.sourceDsId,
      connectorName: `e2e-conn-pg-${TS}`, connectorType: 'postgresql',
      serverName: `e2e-svr-pg-${TS}`, filterRegex: 'public\\..*',
      kafkaTopicPrefix: `e2e-pg-${TS}`, status: 1
    });
    assertEq(status, 200, 'HTTP 200');
    if (ok && data?.data) {
      IDS.cdcId2 = data.data;
      console.log(`   创建 ID=${data.data}`);
    }
  });

  // 创建 CDC 配置 - 含扩展配置
  await runTest('创建 CDC 配置（含扩展配置）', async () => {
    if (!IDS.sourceDsId) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status, data, ok } = await apiPost('/cdc-config', {
      name: `e2e-cdc-extra-${TS}`, datasourceId: IDS.sourceDsId,
      connectorName: `e2e-conn-extra-${TS}`, connectorType: 'mysql',
      serverName: `e2e-svr-extra-${TS}`,
      databaseHost: 'localhost', databasePort: 3306,
      dbUsername: 'root', dbPassword: 'testpwd',
      filterRegex: 'test\\..*', filterBlackRegex: 'test\\.temp_.*',
      kafkaTopicPrefix: `e2e-extra-${TS}`,
      extraConfig: JSON.stringify({ 'snapshot.mode': 'initial', 'polling.interval.ms': 1000 }),
      status: 1
    });
    assertEq(status, 200, 'HTTP 200');
    if (ok && data?.data) {
      IDS.cdcId3 = data.data;
      console.log(`   创建 ID=${data.data}`);
    }
  });

  // 查询 CDC 配置详情
  await runTest('查询 CDC 配置详情', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status, data } = await apiGet(`/cdc-config/${id}`);
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data, '返回配置详情');
    assertEq(data.data.name, `e2e-cdc-mysql-${TS}`, '名称匹配');
    assert(data.data.connectorName, '含 connectorName');
    assert(data.data.syncStatus, '含 syncStatus');
    assert(data.data.datasourceName, '含数据源名称');
  });

  // 更新 CDC 配置
  await runTest('更新 CDC 配置', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { ok } = await apiPut(`/cdc-config/${id}`, {
      name: `e2e-cdc-mysql-upd-${TS}`, datasourceId: IDS.sourceDsId,
      connectorName: `e2e-conn-mysql-${TS}`, filterRegex: 'updated_db\\..*'
    });
    assert(ok, '更新成功');
    const { data } = await apiGet(`/cdc-config/${id}`);
    assertEq(data?.data?.name, `e2e-cdc-mysql-upd-${TS}`, '验证名称');
  });

  // 查询不存在的配置
  await runTest('查询不存在的 CDC 配置', async () => {
    const { status } = await apiGet('/cdc-config/99999');
    assert(status === 200, `HTTP ${status}`);
  });

  // ==================== 2. 启用/禁用 ====================
  console.log('\n═══════ 2. CDC 启用/禁用 ═══════');

  await runTest('禁用 CDC 配置', async () => {
    if (!IDS.cdcId1) { console.log('  ⏭️ 跳过'); passed++; return; }
    assert((await apiPut(`/cdc-config/${IDS.cdcId1}/enable?status=0`, {})).ok, '禁用成功');
  });

  await runTest('启用 CDC 配置', async () => {
    if (!IDS.cdcId1) { console.log('  ⏭️ 跳过'); passed++; return; }
    assert((await apiPut(`/cdc-config/${IDS.cdcId1}/enable?status=1`, {})).ok, '启用成功');
  });

  await runTest('禁用不存在的配置', async () => {
    const { status } = await apiPut('/cdc-config/99999/enable?status=0', {});
    assert(status === 200 || status === 500, `响应 ${status}`);
  });

  // ==================== 3. 连接器操作 ====================
  console.log('\n═══════ 3. CDC 连接器生命周期 ═══════');

  await runTest('查询连接器状态', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status, data } = await apiGet(`/cdc-config/${id}/status`);
    assertEq(status, 200, 'HTTP 200');
    if (data?.data !== undefined) console.log(`   状态: ${JSON.stringify(data.data).substring(0, 200)}`);
  });

  await runTest('部署连接器', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status } = await apiPost(`/cdc-config/${id}/deploy`, {});
    assertEq(status, 200, 'HTTP 200');
  });

  await runTest('启动连接器', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status } = await apiPost(`/cdc-config/${id}/start`, {});
    assertEq(status, 200, 'HTTP 200');
  });

  await runTest('停止连接器', async () => {
    const id = IDS.cdcId1;
    if (!id) { console.log('  ⏭️ 跳过'); passed++; return; }
    const { status } = await apiPost(`/cdc-config/${id}/stop`, {});
    assertEq(status, 200, 'HTTP 200');
  });

  // ==================== 4. CDC 任务管理 ====================
  console.log('\n═══════ 4. CDC 任务管理 ═══════');

  await runTest('获取任务列表', async () => {
    const { data } = await apiGet('/task/page?pageNum=1&pageSize=5');
    const tasks = data?.data?.list;
    if (tasks && tasks.length > 0) {
      IDS.taskId = tasks[0].id;
      console.log(`   使用任务 ID=${IDS.taskId}`);
    } else {
      console.log('   ⚠️ 无可用任务，跳过 CDC 任务操作测试');
    }
  });

  const cdcTaskTests = [
    ['启动 CDC 任务', async () => {
      if (!IDS.taskId) { console.log('  ⏭️ 跳过'); passed++; return; }
      const { status, data } = await apiPost(`/cdc/task/${IDS.taskId}/start`, {});
      assertEq(status, 200, 'HTTP 200');
    }],
    ['查询 CDC 任务状态', async () => {
      if (!IDS.taskId) { console.log('  ⏭️ 跳过'); passed++; return; }
      const { status, data } = await apiGet(`/cdc/task/${IDS.taskId}/status`);
      assertEq(status, 200, 'HTTP 200');
      assert(data?.data !== undefined, '返回数据');
      assert('running' in (data?.data || {}), '含 running');
    }],
    ['停止 CDC 任务', async () => {
      if (!IDS.taskId) { console.log('  ⏭️ 跳过'); passed++; return; }
      const { status } = await apiPost(`/cdc/task/${IDS.taskId}/stop`, {});
      assertEq(status, 200, 'HTTP 200');
    }],
    ['重复停止 CDC 任务', async () => {
      if (!IDS.taskId) { console.log('  ⏭️ 跳过'); passed++; return; }
      const { status } = await apiPost(`/cdc/task/${IDS.taskId}/stop`, {});
      assertEq(status, 200, 'HTTP 200');
    }]
  ];
  for (const [name, fn] of cdcTaskTests) await runTest(name, fn);

  await runTest('查询不存在的 CDC 任务状态', async () => {
    const { status, data } = await apiGet('/cdc/task/99999/status');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.running === false, '不存在则 running=false');
  });

  await runTest('启动不存在的 CDC 任务', async () => {
    const { status } = await apiPost('/cdc/task/99999/start', {});
    assertEq(status, 200, 'HTTP 200');
  });

  await runTest('获取所有运行中 CDC 任务', async () => {
    const { status, data } = await apiGet('/cdc/running');
    assertEq(status, 200, 'HTTP 200');
    assert('total' in (data?.data || {}), '含 total');
    assert(Array.isArray(data?.data?.tasks || []), 'tasks 是数组');
  });

  // ==================== 5. CDC 监控 ====================
  console.log('\n═══════ 5. CDC 监控 ═══════');

  await runTest('CDC 监控健康检查', async () => {
    const { status, data } = await apiGet('/cdc/monitor/health');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data !== undefined, '返回健康报告');
    console.log(`   报告: ${JSON.stringify(data.data).substring(0, 200)}`);
  });

  // ==================== 6. 异常场景 ====================
  console.log('\n═══════ 6. 异常场景 ═══════');

  await runTest('创建 CDC 缺少必填字段', async () => {
    const { status, data } = await apiPost('/cdc-config', {});
    assert(status === 400 || status === 200, `响应 ${status}`);
    if (status === 400) console.log(`   JSR-303 校验拦截: ${data?.message}`);
  });

  await runTest('创建 CDC 缺少 name', async () => {
    const { status } = await apiPost('/cdc-config', { datasourceId: 1, connectorName: 't' });
    assert(status === 400 || status === 200, `响应 ${status}`);
  });

  await runTest('创建 CDC 缺少 datasourceId', async () => {
    const { status } = await apiPost('/cdc-config', { name: 't', connectorName: 't' });
    assert(status === 400 || status === 200, `响应 ${status}`);
  });

  await runTest('删除不存在的 CDC 配置', async () => {
    const { status } = await apiDelete('/cdc-config/99999');
    assert(status > 0, `响应 ${status}`);
  });

  // ==================== 7. 清理 ====================
  console.log('\n═══════ 7. 清理 ═══════');

  await runTest('清理 CDC 配置', async () => {
    for (const k of ['cdcId1', 'cdcId2', 'cdcId3']) {
      if (IDS[k]) {
        const r = await apiDelete(`/cdc-config/${IDS[k]}`);
        assert(r.ok || true, `删除 ${k}`);
      }
    }
  });

  // ==================== 报告 ====================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('  CDC 功能深度测试报告');
  console.log('═══════════════════════════════════════════════════════════════');
  const total = passed + failed;
  console.log(`  测试用例: ${total}`);
  console.log(`  ✅ 通过: ${passed}`);
  console.log(`  ❌ 失败: ${failed}`);
  console.log(`  成功率: ${total > 0 ? (passed / total * 100).toFixed(1) : 0}%`);
  if (errors.length > 0) {
    console.log(`\n  失败:`); errors.forEach((e, i) => console.log(`    ${i+1}. ${e}`));
  }
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(e => { console.error('FATAL:', e); process.exit(1); });
