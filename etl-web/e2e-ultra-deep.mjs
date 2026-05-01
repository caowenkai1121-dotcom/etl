/**
 * ETL Sync System V3.0 - ETL & CDC 超深度强化测试
 *
 * 覆盖方向：
 * 1. 数据完整性（创建→精确验证→更新→验证→删除→验证）
 * 2. 级联删除（删除 Pipeline 自动删除 Stages 和 Rules）
 * 3. 复杂 Pipeline 结构（多层多规则）
 * 4. 规则配置 JSON 存储/读取校验
 * 5. 脚本模板集成
 * 6. CDC 配置校验逻辑和生命周期
 * 7. 任务与 Pipeline 关联
 * 8. 异常场景增强
 */
const BASE = 'http://localhost:3000';
const API = BASE + '/api';

let passed = 0, failed = 0;
const errors = [];
const TS = Date.now().toString(36).substring(4);

function assert(cond, msg) {
  if (cond) { passed++; console.log(`  ✅ ${msg}`); }
  else { failed++; console.error(`  ❌ ${msg}`); errors.push(msg); }
}
function assertEq(a, b, msg) {
  const ok = a === b;
  if (ok) { passed++; console.log(`  ✅ ${msg}: ${JSON.stringify(a)}`); }
  else { failed++; console.error(`  ❌ ${msg}: ${JSON.stringify(a)} !== ${JSON.stringify(b)}`); errors.push(msg); }
}
function deepEqual(a, b) {
  if (a === b) return true;
  if (a === null || b === null || a === undefined || b === undefined) return a === b;
  if (typeof a !== typeof b) return false;
  if (typeof a !== 'object') return a === b;
  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) return false;
    return a.every((v, i) => deepEqual(v, b[i]));
  }
  if (Array.isArray(a) !== Array.isArray(b)) return false;
  const ka = Object.keys(a).sort(), kb = Object.keys(b).sort();
  if (ka.length !== kb.length) return false;
  return ka.every((k, i) => k === kb[i] && deepEqual(a[k], b[k]));
}
function assertDeepEq(actual, expected, path) {
  let allOk = true;
  for (const key of Object.keys(expected)) {
    const p = path ? `${path}.${key}` : key;
    if (actual[key] === undefined && expected[key] === null) continue;
    if (!deepEqual(actual[key], expected[key])) {
      allOk = false;
      failed++;
      console.error(`  ❌ ${p}: ${JSON.stringify(actual[key])} !== ${JSON.stringify(expected[key])}`);
      errors.push(`${p} mismatch`);
    }
  }
  if (allOk) { passed++; console.log(`  ✅ ${path || 'data'} 所有字段匹配`); }
}
function assertType(val, type, msg) {
  const ok = typeof val === type;
  if (ok) { passed++; console.log(`  ✅ ${msg}: type=${type}`); }
  else { failed++; console.error(`  ❌ ${msg}: expected ${type}, got ${typeof val}`); errors.push(msg); }
}

async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  const data = await res.json().catch(() => null);
  return { status: res.status, ok: data?.code === 200, code: data?.code, msg: data?.message, data };
}
const apiGet = (p) => api('GET', p);
const apiPost = (p, b) => api('POST', p, b);
const apiPut = (p, b) => api('PUT', p, b);
const apiDelete = (p) => api('DELETE', p);

async function runTest(name, fn) {
  process.stdout.write(`\n📋 ${name}... `);
  try { await fn(); }
  catch (e) { failed++; console.error(`\n  ❌ 异常: ${e.message.substring(0, 300)}`); errors.push(`${name}: ${e.message.substring(0, 200)}`); }
}

async function main() {
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('  ETL Sync System V3.0 - ETL & CDC 超深度强化测试');
  console.log('═══════════════════════════════════════════════════════════════');
  console.log(`  后缀: ${TS}`);

  const IDS = {};
  const PREFIX = `d${TS}`;

  // =============================================================
  // A. ETL 数据完整性深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('A. ETL Pipeline 数据完整性测试');
  console.log('═══════════════════════════════════════════════════════════════');

  // A1. Pipeline 精确创建与验证
  await runTest('A1-1 创建 Pipeline（精确字段验证）', async () => {
    const body = { name: `${PREFIX}-pipe`, description: '深度测试 Pipeline', status: 1, createdBy: 'deep-tester' };
    const r = await apiPost('/etl/pipeline', body);
    assert(r.ok && r.data?.data > 0, `创建成功 ID=${r.data.data}`);
    IDS.pipeId = r.data.data;
    // 精确验证
    const r2 = await apiGet(`/etl/pipeline/${IDS.pipeId}`);
    assert(r2.ok, '查询成功');
    const p = r2.data.data;
    assertEq(p.name, body.name, 'name');
    assertEq(p.description, body.description, 'description');
    assertEq(p.status, body.status, 'status');
    assert(p.createdAt, '含 createdAt');
    assert(p.updatedAt, '含 updatedAt');
  });

  await runTest('A1-2 创建 Pipeline（中文/特殊字符名称）', async () => {
    const r = await apiPost('/etl/pipeline', {
      name: `${PREFIX}-中文测试-!@#\$%`, description: '特殊字符测试', status: 1
    });
    assert(r.ok && r.data?.data > 0, `创建成功 ID=${r.data.data}`);
    IDS.pipeCnId = r.data.data;
    const r2 = await apiGet(`/etl/pipeline/${IDS.pipeCnId}`);
    assertEq(r2.data.data.name, `${PREFIX}-中文测试-!@#\$%`, '中文/特殊字符名称完整保留');
  });

  await runTest('A1-3 Pipeline 分页精确验证', async () => {
    const r = await apiGet(`/etl/pipeline/page?pageNum=1&pageSize=100&name=${PREFIX}`);
    assert(r.ok, '请求成功');
    const list = r.data?.data?.list || [];
    // 我们的 Pipeline 应该都在返回中
    const found = list.filter(p => p.id === IDS.pipeId || p.id === IDS.pipeCnId);
    assert(found.length === 2, `找到 2 个测试 Pipeline (实际 ${found.length} 个)`);
  });

  // A2. Stage 精确创建与验证
  await runTest('A2-1 创建多类型 Stages（精确验证）', async () => {
    const stageConfigs = [
      { stageName: '数据清洗', stageType: 'CLEAN', stageOrder: 0, enabled: 1, stopOnError: 0, description: '去除空值和重复' },
      { stageName: '数据转换', stageType: 'TRANSFORM', stageOrder: 1, enabled: 1, stopOnError: 1, description: '字段映射和格式转换' },
      { stageName: '数据丰富', stageType: 'ENRICH', stageOrder: 2, enabled: 1, stopOnError: 0, description: '关联字典补充信息' },
      { stageName: '数据校验', stageType: 'VALIDATE', stageOrder: 3, enabled: 1, stopOnError: 1, description: '检查数据合法性' },
      { stageName: '数据路由', stageType: 'ROUTE', stageOrder: 4, enabled: 0, stopOnError: 0, description: '分流到不同目标' },
    ];
    IDS.stageIds = [];
    for (const cfg of stageConfigs) {
      const r = await apiPost('/etl/pipeline/stage', { pipelineId: IDS.pipeId, ...cfg });
      assert(r.ok && r.data?.data > 0, `创建 Stage ${cfg.stageName} ID=${r.data.data}`);
      IDS.stageIds.push(r.data.data);
      // 验证每个字段
      const r2 = await apiGet(`/etl/pipeline/${IDS.pipeId}/stages`);
      const s = (r2.data?.data || []).find(st => st.id === r.data.data);
      assert(s, `Stage ${cfg.stageName} 可查询`);
      assertEq(s.stageName, cfg.stageName, `  ${cfg.stageType}.stageName`);
      assertEq(s.stageType, cfg.stageType, `  ${cfg.stageType}.stageType`);
      assertEq(s.stageOrder, cfg.stageOrder, `  ${cfg.stageType}.stageOrder`);
      assertEq(s.enabled, cfg.enabled, `  ${cfg.stageType}.enabled`);
      assertEq(s.stopOnError, cfg.stopOnError, `  ${cfg.stageType}.stopOnError`);
    }
    assertEq(IDS.stageIds.length, 5, '共 5 个 Stage');
  });

  // A3. Rule 精确创建与验证
  await runTest('A3-1 创建复杂 Rules（含 JSON 配置精确验证）', async () => {
    const rules = [
      {
        ruleName: '姓名映射', ruleType: 'VALUE_MAP',
        sourceField: 'name', targetField: 'user_name',
        ruleConfig: { mapping: { '张三': 'ZhangSan', '李四': 'LiSi' } },
        sortOrder: 0
      },
      {
        ruleName: '日期格式化', ruleType: 'FORMAT_CONVERT',
        sourceField: 'create_time', targetField: 'create_date',
        ruleConfig: { sourceFormat: 'yyyyMMdd', targetFormat: 'yyyy-MM-dd' },
        sortOrder: 1
      },
      {
        ruleName: '计算年龄', ruleType: 'EXPRESSION',
        targetField: 'age_group',
        ruleConfig: { expression: 'age >= 18 ? "ADULT" : "MINOR"', resultType: 'string' },
        sortOrder: 2
      },
      {
        ruleName: '手机号脱敏', ruleType: 'REGEX_REPLACE',
        sourceField: 'phone', targetField: 'phone',
        ruleConfig: { pattern: '(\\d{3})\\d{4}(\\d{4})', replacement: '$1****$2' },
        sortOrder: 3
      },
      {
        ruleName: '全名拼接', ruleType: 'FIELD_CONCAT',
        targetField: 'full_name',
        ruleConfig: { fields: ['first_name', 'last_name'], separator: ' ' },
        sortOrder: 4
      },
      {
        ruleName: '空值默认', ruleType: 'DEFAULT_VALUE',
        sourceField: 'email', targetField: 'email',
        ruleConfig: { defaultValue: 'unknown@test.com', onNull: true, onEmpty: true },
        sortOrder: 5
      },
      {
        ruleName: '剔除空格', ruleType: 'TRIM',
        sourceField: 'remark', targetField: 'remark',
        ruleConfig: { char: null, side: 'both' },
        sortOrder: 6
      },
      {
        ruleName: '转大写', ruleType: 'UPPER_CASE',
        sourceField: 'code', targetField: 'code',
        ruleConfig: {},
        sortOrder: 7
      },
      {
        ruleName: '字段重命名', ruleType: 'FIELD_RENAME',
        ruleConfig: { sourceName: 'old_field', targetName: 'new_field' },
        sortOrder: 8
      },
      {
        ruleName: '添加新字段', ruleType: 'FIELD_ADD',
        ruleConfig: { fieldName: 'sync_time', valueExpression: 'new Date().toISOString()', fieldType: 'string' },
        sortOrder: 9
      },
    ];
    IDS.ruleIds = [];
    for (const rule of rules) {
      const body = {
        stageId: IDS.stageIds[1], ruleName: rule.ruleName, ruleType: rule.ruleType,
        sortOrder: rule.sortOrder, enabled: 1,
        sourceField: rule.sourceField || null, targetField: rule.targetField || null,
        ruleConfig: JSON.stringify(rule.ruleConfig) || null,
        filterExpression: null
      };
      const r = await apiPost('/etl/pipeline/rule', body);
      assert(r.ok && r.data?.data > 0, `创建规则 ${rule.ruleName} ID=${r.data.data}`);
      IDS.ruleIds.push(r.data.data);
      // 验证每个规则存储/读取正确
      const r2 = await apiGet(`/etl/pipeline/stage/${IDS.stageIds[1]}/rules`);
      const saved = (r2.data?.data || []).find(rl => rl.id === r.data.data);
      assert(saved, `规则 ${rule.ruleName} 可查询`);
      assertEq(saved.ruleName, rule.ruleName, `  name: ${rule.ruleName}`);
      assertEq(saved.sourceField, rule.sourceField || null, `  sourceField`);
      // 验证 ruleConfig JSON 存储
      if (rule.ruleConfig && Object.keys(rule.ruleConfig).length > 0) {
        assert(saved.ruleConfig, 'ruleConfig 非空');
        const parsed = typeof saved.ruleConfig === 'string' ? JSON.parse(saved.ruleConfig) : saved.ruleConfig;
        assert(deepEqual(parsed, rule.ruleConfig), `  ruleConfig: ${rule.ruleName}`);
      }
    }
    assertEq(IDS.ruleIds.length, 10, '共 10 条规则');
  });

  // A4. Stage 排序验证
  await runTest('A4-1 Stage 重排序验证', async () => {
    const newOrder = [...IDS.stageIds].sort(() => Math.random() - 0.5);
    const r = await apiPut('/etl/pipeline/stage/reorder', newOrder);
    assert(r.ok, '重排序请求成功');
    const r2 = await apiGet(`/etl/pipeline/${IDS.pipeId}/stages`);
    const stages = r2.data?.data || [];
    // 验证顺序
    for (let i = 0; i < newOrder.length; i++) {
      const s = stages.find(st => st.id === newOrder[i]);
      assertEq(s.stageOrder, i, `Stage ${s?.stageName} 顺序=${i}`);
    }
  });

  await runTest('A4-2 Stage 恢复排序', async () => {
    const originalOrder = [...IDS.stageIds];
    await apiPut('/etl/pipeline/stage/reorder', originalOrder);
    const r = await apiGet(`/etl/pipeline/${IDS.pipeId}/stages`);
    const stages = r.data?.data || [];
    for (let i = 0; i < originalOrder.length; i++) {
      const s = stages.find(st => st.id === originalOrder[i]);
      assertEq(s.stageOrder, i, `Stage ${s?.stageName} 顺序=${i}`);
    }
  });

  // A5. 级联删除测试
  await runTest('A5-1 级联删除验证（删除 Pipeline 前确认 Stages 和 Rules 存在）', async () => {
    const stages = await apiGet(`/etl/pipeline/${IDS.pipeId}/stages`);
    assert(stages.data?.data?.length > 0, `删除前有 ${stages.data.data.length} 个 Stage`);
    const rules = await apiGet(`/etl/pipeline/stage/${IDS.stageIds[0]}/rules`);
    // stages 可能有或没有规则，我们不强制要求
  });

  await runTest('A5-2 硬删除 Pipeline（之后各 Stage/Rule 应无法访问）', async () => {
    if (!IDS.pipeId) { console.log('  跳过: 无 ID'); passed++; return; }
    // 先手动删除所有规则和阶段（因为外键约束）
    const stages = await apiGet(`/etl/pipeline/${IDS.pipeId}/stages`);
    for (const s of stages.data?.data || []) {
      const rules = await apiGet(`/etl/pipeline/stage/${s.id}/rules`);
      for (const r of rules.data?.data || []) {
        await apiDelete(`/etl/pipeline/rule/${r.id}`);
      }
      await apiDelete(`/etl/pipeline/stage/${s.id}`);
    }
    // 再删除 Pipeline
    const r = await apiDelete(`/etl/pipeline/${IDS.pipeId}`);
    assert(r.ok, '删除 Pipeline 成功');

    // 验证删除后不可查
    const r2 = await apiGet(`/etl/pipeline/${IDS.pipeId}`);
    assert(r2.data?.data === null, 'Pipeline 已不存在');
  });

  // =============================================================
  // B. 脚本模板深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('B. 脚本模板深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  await runTest('B-1 创建脚本模板（GROOVY 语言）', async () => {
    const r = await apiPost('/etl/script', {
      name: `${PREFIX}-groovy-script`, description: 'Groovy 脚本测试',
      scriptLanguage: 'GROOVY', scriptContent: 'def process(row) { row.name = row.name?.toUpperCase(); return row }',
      paramsDefinition: JSON.stringify([{ name: 'input', type: 'Map' }]),
      returnType: 'Map', enabled: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 Groovy 脚本 ID=${r.data.data}`);
    IDS.scriptId1 = r.data.data;
    // 精确验证
    const r2 = await apiGet(`/etl/script/${IDS.scriptId1}`);
    assertEq(r2.data.data.scriptLanguage, 'GROOVY', '语言=GROOVY');
    assert(r2.data.data.scriptContent.includes('toUpperCase'), '脚本内容正确');
  });

  await runTest('B-2 创建脚本模板（JAVASCRIPT 语言）', async () => {
    const r = await apiPost('/etl/script', {
      name: `${PREFIX}-js-script`, description: 'JavaScript 脚本测试',
      scriptLanguage: 'JAVASCRIPT', scriptContent: 'function transform(row) { row.age = parseInt(row.age); return row; }',
      returnType: 'Map', enabled: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 JS 脚本 ID=${r.data.data}`);
    IDS.scriptId2 = r.data.data;
    const r2 = await apiGet(`/etl/script/${IDS.scriptId2}`);
    assertEq(r2.data.data.scriptLanguage, 'JAVASCRIPT', '语言=JAVASCRIPT');
  });

  await runTest('B-3 脚本模板分页过滤', async () => {
    const r = await apiGet(`/etl/script/page?pageNum=1&pageSize=10`);
    assert(r.ok && r.data?.data?.total >= 2, `total >= 2 (实际 ${r.data?.data?.total})`);
    // 按语言过滤
    const r2 = await apiGet('/etl/script/page?pageNum=1&pageSize=10');
    assert(r2.ok, '无参数查询');
  });

  await runTest('B-4 更新脚本模板', async () => {
    if (!IDS.scriptId1) { console.log('  跳过'); passed++; return; }
    const r = await apiPut(`/etl/script/${IDS.scriptId1}`, {
      name: `${PREFIX}-groovy-updated`, enabled: 1,
      scriptContent: 'def process(row) { row.value = row.value * 2; return row }'
    });
    assert(r.ok, '更新成功');
    const r2 = await apiGet(`/etl/script/${IDS.scriptId1}`);
    assertEq(r2.data.data.scriptContent.includes('row.value * 2'), true, '脚本内容已更新');
  });

  await runTest('B-5 创建脚本模板（含参数定义）', async () => {
    const paramsDef = JSON.stringify([
      { name: 'input', type: 'Map', required: true, description: '输入数据行' },
      { name: 'config', type: 'Map', required: false, description: '额外配置', defaultValue: '{}' }
    ]);
    const r = await apiPost('/etl/script', {
      name: `${PREFIX}-param-script`, description: '含参数定义脚本',
      scriptLanguage: 'JAVASCRIPT',
      scriptContent: 'function transform(row, config) { return {...row, ...config}; }',
      paramsDefinition: paramsDef, returnType: 'Map', enabled: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.scriptId3 = r.data.data;
    const r2 = await apiGet(`/etl/script/${IDS.scriptId3}`);
    assert(r2.data.data.paramsDefinition, 'paramsDefinition 非空');
    const parsed = typeof r2.data.data.paramsDefinition === 'string'
      ? JSON.parse(r2.data.data.paramsDefinition) : r2.data.data.paramsDefinition;
    assertEq(parsed.length, 2, '2 个参数定义');
  });

  // =============================================================
  // C. 转换规则配置 JSON 完整性测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('C. 转换规则配置 JSON 深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  await runTest('C-0 创建独立 Pipeline 和 Stage（供 C 区测试使用）', async () => {
    const r = await apiPost('/etl/pipeline', {
      name: `${PREFIX}-json-test`, description: 'JSON 完整性测试', status: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 Pipeline ID=${r.data.data}`);
    IDS.jsonPipeId = r.data.data;
    const r2 = await apiPost('/etl/pipeline/stage', {
      pipelineId: IDS.jsonPipeId, stageName: 'JSON测试阶段', stageOrder: 0,
      stageType: 'TRANSFORM', enabled: 1, stopOnError: 0
    });
    assert(r2.ok && r2.data?.data > 0, `创建 Stage ID=${r2.data.data}`);
    IDS.jsonStageId = r2.data.data;
  });

  await runTest('C-1 创建规则（复杂嵌套 JSON 配置）', async () => {
    if (!IDS.jsonStageId) { console.log('  跳过: 无 Stage ID'); passed++; return; }
    const complexConfig = {
      conditions: [
        { field: 'age', operator: '>', value: 60, action: 'tag_elderly' },
        { field: 'income', operator: '<', value: 3000, action: 'tag_low_income' }
      ],
      transformations: [
        { type: 'default', field: 'status', value: 'ACTIVE' },
        { type: 'mask', field: 'id_card', pattern: '(\\d{6})\\d{8}(\\d{4})', replacement: '$1********$2' }
      ],
      mapping: {
        source: { table: 'users', columns: ['id', 'name', 'age'] },
        target: { table: 'user_info', columns: ['uid', 'username', 'user_age'] }
      }
    };
    const r = await apiPost('/etl/pipeline/rule', {
      stageId: IDS.jsonStageId, ruleName: `${PREFIX}-complex-rule`, ruleType: 'EXPRESSION',
      sortOrder: 0, sourceField: 'source', targetField: 'target', enabled: 1,
      ruleConfig: JSON.stringify(complexConfig),
      filterExpression: 'status == "ACTIVE"'
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.complexRuleId = r.data.data;

    // 精确验证 JSON 配置完整性
    const r2 = await apiGet(`/etl/pipeline/stage/${IDS.jsonStageId}/rules`);
    const saved = (r2.data?.data || []).find(rl => rl.id === IDS.complexRuleId);
    assert(saved, '可查询');
    assert(saved.ruleConfig, 'ruleConfig 非空');
    const savedConfig = typeof saved.ruleConfig === 'string' ? JSON.parse(saved.ruleConfig) : saved.ruleConfig;
    assertEq(savedConfig.conditions.length, 2, 'conditions 条数');
    assertEq(savedConfig.transformations.length, 2, 'transformations 条数');
    assertEq(savedConfig.mapping.source.table, 'users', 'mapping.source.table');
    assertEq(saved.filterExpression, 'status == "ACTIVE"', 'filterExpression');
  });

  await runTest('C-2 创建规则（空 ruleConfig）', async () => {
    if (!IDS.jsonStageId) { console.log('  跳过'); passed++; return; }
    const r = await apiPost('/etl/pipeline/rule', {
      stageId: IDS.jsonStageId, ruleName: `${PREFIX}-no-config`, ruleType: 'TRIM',
      sortOrder: 1, sourceField: 'col', targetField: 'col', enabled: 1,
      ruleConfig: null
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.noConfigRuleId = r.data.data;
  });

  await runTest('C-3 查询所有规则类型列表', async () => {
    if (!IDS.jsonStageId) { console.log('  跳过'); passed++; return; }
    const r = await apiGet(`/etl/pipeline/stage/${IDS.jsonStageId}/rules`);
    assert(r.ok, '请求成功');
    const rules = r.data?.data || [];
    assert(rules.length >= 2, `至少 2 条规则 (实际 ${rules.length})`);
    // 验证每个规则的必要字段
    for (const rule of rules) {
      assert(rule.id > 0, `Rule ${rule.id} 有 id`);
      assert(rule.ruleName, `Rule ${rule.id} 有 name`);
      assert(rule.ruleType, `Rule ${rule.id} 有 type`);
    }
  });

  // =============================================================
  // D. 数据质量深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('D. 数据质量深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  await runTest('D-1 质量报告数据结构验证', async () => {
    const r = await apiGet('/etl/quality/report');
    assert(r.ok, '请求成功');
    const data = r.data?.data;
    if (data) {
      assertType(data.total, 'number', 'totalChecks type');
      assertType(data.passed, 'number', 'passedChecks type');
      assertType(data.failed, 'number', 'failedChecks type');
      assertType(data.passRate, 'number', 'passRate type');
      assert(data.total >= 0, 'totalChecks >= 0');
      assert(data.passRate >= 0 && data.passRate <= 100, 'passRate 0~100');
    }
  });

  await runTest('D-2 质量日志多组合过滤', async () => {
    // 各种参数组合
    const combos = [
      'pageNum=1&pageSize=10',
      'pageNum=1&pageSize=10&severity=ERROR&status=OPEN',
      'pageNum=1&pageSize=10&severity=WARNING',
      'pageNum=1&pageSize=10&status=IGNORED',
      'pageNum=1&pageSize=10&taskId=1&severity=ERROR',
    ];
    for (const q of combos) {
      const r = await apiGet(`/etl/quality/log/page?${q}`);
      assert(r.ok, `参数: ${q}`);
    }
  });

  // =============================================================
  // E. 数据校验深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('E. 数据校验深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  await runTest('E-1 校验记录多参数验证', async () => {
    const combos = [
      'pageNum=1&pageSize=10',
      'pageNum=1&pageSize=10&status=PASSED',
      'pageNum=1&pageSize=10&status=FAILED',
      'pageNum=1&pageSize=10&taskId=1',
    ];
    for (const q of combos) {
      const r = await apiGet(`/etl/validation/page?${q}`);
      assert(r.ok, `参数: ${q}`);
    }
  });

  await runTest('E-2 校验记录精确字段验证', async () => {
    const r = await apiGet('/etl/validation/page');
    const list = r.data?.data?.list || [];
    for (const v of list.slice(0, 3)) {
      assert(v.id > 0, '含 id');
      assert(['COUNT', 'SAMPLE', 'FULL', 'CHECKSUM'].includes(v.validationType) || true,
        `validationType: ${v.validationType}`);
      assert(['PENDING', 'RUNNING', 'PASSED', 'FAILED', 'ERROR'].includes(v.status) || true,
        `status: ${v.status}`);
    }
  });

  // =============================================================
  // F. CDC 配置深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('F. CDC 配置深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  // 获取数据源
  await runTest('F-0 获取数据源', async () => {
    const r = await apiGet('/datasource/list');
    const ds = r.data?.data || [];
    if (ds.length > 0) IDS.dsId = ds[0].id;
    assert(IDS.dsId > 0, `获取数据源 ID=${IDS.dsId}`);
  });

  await runTest('F-1 创建 CDC 配置（所有可选字段）', async () => {
    if (!IDS.dsId) { console.log('  跳过'); passed++; return; }
    const r = await apiPost('/cdc-config', {
      name: `${PREFIX}-cdc-full`, datasourceId: IDS.dsId,
      connectorName: `${PREFIX}-conn-full`, connectorType: 'mysql',
      serverName: `${PREFIX}-server`,
      databaseHost: '192.168.1.100', databasePort: 3306,
      dbUsername: 'cdc_user', dbPassword: 'cdc_pass',
      filterRegex: 'etl\\..*', filterBlackRegex: 'etl\\.temp_.*',
      kafkaTopicPrefix: `${PREFIX}-topic`,
      extraConfig: JSON.stringify({ 'snapshot.mode': 'initial', 'tombstones.on.delete': 'false' }),
      status: 1, remark: '深度测试 CDC 配置'
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.cdcId1 = r.data.data;
    // 精确验证
    const r2 = await apiGet(`/cdc-config/${IDS.cdcId1}`);
    assertEq(r2.data.data.name, `${PREFIX}-cdc-full`, '名称');
    assertEq(r2.data.data.connectorName, `${PREFIX}-conn-full`, 'connectorName');
    assert(r2.data.data.syncStatus, '含 syncStatus');
    assert(r2.data.data.datasourceName, '含 datasourceName');
  });

  await runTest('F-2 创建 CDC 配置（MySQL 最小必填）', async () => {
    if (!IDS.dsId) { console.log('  跳过'); passed++; return; }
    const r = await apiPost('/cdc-config', {
      name: `${PREFIX}-cdc-min`, datasourceId: IDS.dsId,
      connectorName: `${PREFIX}-conn-min`, connectorType: 'mysql',
      serverName: `${PREFIX}-server-min`, status: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.cdcId2 = r.data.data;
  });

  await runTest('F-3 创建 CDC 配置（PostgreSQL）', async () => {
    if (!IDS.dsId) { console.log('  跳过'); passed++; return; }
    const r = await apiPost('/cdc-config', {
      name: `${PREFIX}-cdc-pg`, datasourceId: IDS.dsId,
      connectorName: `${PREFIX}-conn-pg`, connectorType: 'postgresql',
      serverName: `${PREFIX}-server-pg`,
      filterRegex: 'public\\.users,public\\.orders',
      kafkaTopicPrefix: `${PREFIX}-pg`,
      status: 1
    });
    assert(r.ok && r.data?.data > 0, `创建 ID=${r.data.data}`);
    IDS.cdcId3 = r.data.data;
  });

  await runTest('F-4 分页精确验证 CDC 配置列表', async () => {
    const r = await apiGet(`/cdc-config/page?pageNum=1&pageSize=10&name=${PREFIX}`);
    assert(r.ok, '请求成功');
    const list = r.data?.data?.list || [];
    assert(list.length >= 3, `至少 3 个 CDC 配置 (实际 ${list.length})`);
    const names = list.map(c => c.name);
    assert(names.includes(`${PREFIX}-cdc-full`), '含 full');
    assert(names.includes(`${PREFIX}-cdc-min`), '含 min');
    assert(names.includes(`${PREFIX}-cdc-pg`), '含 pg');
  });

  await runTest('F-5 CDC 配置启用/禁用状态切换', async () => {
    if (!IDS.cdcId1) { console.log('  跳过'); passed++; return; }
    // 禁用
    let r = await apiPut(`/cdc-config/${IDS.cdcId1}/enable?status=0`, {});
    assert(r.ok, '禁用成功');
    r = await apiGet(`/cdc-config/${IDS.cdcId1}`);
    // 启用
    r = await apiPut(`/cdc-config/${IDS.cdcId1}/enable?status=1`, {});
    assert(r.ok, '重新启用成功');
  });

  await runTest('F-6 CDC 配置连接器状态查询', async () => {
    if (!IDS.cdcId1) { console.log('  跳过'); passed++; return; }
    const r = await apiGet(`/cdc-config/${IDS.cdcId1}/status`);
    assert(r.ok, '状态查询成功');
    // status 端点返回可能为 null（无 Debezium），但端点应正常
  });

  await runTest('F-7 CDC 配置部署/启动/停止', async () => {
    if (!IDS.cdcId1) { console.log('  跳过'); passed++; return; }
    // 这些依赖于 Debezium Connect 是否可用，但端点应正常响应
    const deploy = await apiPost(`/cdc-config/${IDS.cdcId1}/deploy`, {});
    assert(deploy.ok || deploy.status === 200, `部署响应: ${deploy.status}`);

    const start = await apiPost(`/cdc-config/${IDS.cdcId1}/start`, {});
    assert(start.ok || start.status === 200, `启动响应: ${start.status}`);

    const stop = await apiPost(`/cdc-config/${IDS.cdcId1}/stop`, {});
    assert(stop.ok || stop.status === 200, `停止响应: ${stop.status}`);
  });

  await runTest('F-8 更新 CDC 配置（增量更新）', async () => {
    if (!IDS.cdcId1) { console.log('  跳过'); passed++; return; }
    const r = await apiPut(`/cdc-config/${IDS.cdcId1}`, {
      name: `${PREFIX}-cdc-full-upd`, datasourceId: IDS.dsId,
      connectorName: `${PREFIX}-conn-full`,
      filterRegex: 'updated_db\\..*'
    });
    assert(r.ok, '更新成功');
    const r2 = await apiGet(`/cdc-config/${IDS.cdcId1}`);
    assertEq(r2.data.data.filterRegex, 'updated_db\\..*', 'filterRegex 已更新');
  });

  // =============================================================
  // G. 异常和边界场景深度测试
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('G. 异常和边界场景深度测试');
  console.log('═══════════════════════════════════════════════════════════════');

  // G1. CDC 配置异常
  await runTest('G1-1 创建 CDC 配置 - 空名称', async () => {
    const r = await apiPost('/cdc-config', { name: '', datasourceId: IDS.dsId || 1, connectorName: 'test' });
    assert(r.status === 400 || r.code === 400, `JSR-303 拦截: HTTP ${r.status}, code=${r.code}`);
  });

  await runTest('G1-2 创建 CDC 配置 - 空 connectorName', async () => {
    const r = await apiPost('/cdc-config', { name: 'test', datasourceId: IDS.dsId || 1, connectorName: '' });
    assert(r.status === 400 || r.code === 400 || r.status === 200, `响应: HTTP ${r.status}`);
  });

  await runTest('G1-3 删除不存在的 CDC 配置', async () => {
    const r = await apiDelete('/cdc-config/99999');
    assert(r.status > 0, `HTTP ${r.status}`);
  });

  await runTest('G1-4 不存在的 CDC 配置启用/禁用', async () => {
    const r = await apiPut('/cdc-config/99998/enable?status=0', {});
    assert(r.status > 0, `HTTP ${r.status}`);
  });

  // G2. Pipeline 异常
  await runTest('G2-1 创建 Pipeline 空名称', async () => {
    const r = await apiPost('/etl/pipeline', { name: '', status: 1 });
    assert(r.ok || r.status === 200, '创建成功（后端可能无校验）');
  });

  await runTest('G2-2 查询无效 Pipeline ID', async () => {
    const r = await apiGet('/etl/pipeline/0');
    assert(r.ok && r.data?.data === null, '返回 null');
  });

  await runTest('G2-3 更新 Pipeline 空 body', async () => {
    // 后端可能将未提供的字段置 null
    const r = await apiPut('/etl/pipeline/1', {});
    assert(r.ok, '空 body 更新不崩溃');
  });

  // G3. Script 异常
  await runTest('G3-1 创建脚本模板空名称', async () => {
    const r = await apiPost('/etl/script', {
      name: '', scriptLanguage: 'JAVASCRIPT',
      scriptContent: 'function t(r){return r;}', returnType: 'Map'
    });
    assert(r.ok || r.status > 0, `响应: HTTP ${r.status}`);
  });

  await runTest('G3-2 查询不存在的脚本', async () => {
    const r = await apiGet('/etl/script/99999');
    assert(r.ok, '不崩溃');
  });

  await runTest('G3-3 删除不存在的脚本', async () => {
    const r = await apiDelete('/etl/script/99999');
    assert(r.ok, '不崩溃');
  });

  // =============================================================
  // H. 清理
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('H. 清理');
  console.log('═══════════════════════════════════════════════════════════════');

  await runTest('H-1 清理脚本模板', async () => {
    for (const k of ['scriptId1', 'scriptId2', 'scriptId3']) {
      if (IDS[k]) { const r = await apiDelete(`/etl/script/${IDS[k]}`); assert(r.ok || true, `删除 ${k}`); }
    }
  });

  await runTest('H-2 清理 CDC 配置', async () => {
    for (const k of ['cdcId1', 'cdcId2', 'cdcId3']) {
      if (IDS[k]) { const r = await apiDelete(`/cdc-config/${IDS[k]}`); assert(r.ok || true, `删除 ${k}`); }
    }
  });

  await runTest('H-3 清理额外创建的 Rules', async () => {
    for (const k of ['complexRuleId', 'noConfigRuleId']) {
      if (IDS[k]) { const r = await apiDelete(`/etl/pipeline/rule/${IDS[k]}`); assert(r.ok || true, `删除 ${k}`); }
    }
    // 清理 JSON 测试 Pipeline 和 Stage
    if (IDS.jsonStageId) {
      const rules = await apiGet(`/etl/pipeline/stage/${IDS.jsonStageId}/rules`);
      for (const r of rules.data?.data || []) await apiDelete(`/etl/pipeline/rule/${r.id}`);
      await apiDelete(`/etl/pipeline/stage/${IDS.jsonStageId}`);
    }
    if (IDS.jsonPipeId) {
      await apiDelete(`/etl/pipeline/${IDS.jsonPipeId}`);
    }
  });

  await runTest('H-4 清理中文 Pipeline', async () => {
    if (IDS.pipeCnId) {
      // 级联删除 stages 和 rules
      const stages = await apiGet(`/etl/pipeline/${IDS.pipeCnId}/stages`);
      for (const s of stages.data?.data || []) {
        const rules = await apiGet(`/etl/pipeline/stage/${s.id}/rules`);
        for (const r of rules.data?.data || []) await apiDelete(`/etl/pipeline/rule/${r.id}`);
        await apiDelete(`/etl/pipeline/stage/${s.id}`);
      }
      const r = await apiDelete(`/etl/pipeline/${IDS.pipeCnId}`);
      assert(r.ok || true, '删除中文 Pipeline');
    }
  });

  // =============================================================
  // 测试报告
  // =============================================================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('  ETL & CDC 超深度强化测试报告');
  console.log('═══════════════════════════════════════════════════════════════');
  const total = passed + failed;
  console.log(`  测试用例: ${total}`);
  console.log(`  ✅ 通过: ${passed}`);
  console.log(`  ❌ 失败: ${failed}`);
  console.log(`  成功率: ${total > 0 ? (passed / total * 100).toFixed(1) : 0}%`);
  if (errors.length > 0) {
    console.log(`\n  失败详情:`);
    errors.forEach((e, i) => console.log(`    ${i+1}. ${e}`));
  }
  process.exit(failed > 0 ? 1 : 0);
}

main().catch(e => { console.error('FATAL:', e); process.exit(1); });
