/**
 * ETL Sync System V3.0 - ETL 数据处理功能深度测试
 *
 * 覆盖 Pipeline 流水线、转换阶段、转换规则、数据质量、数据校验
 * 所有规则类型、所有阶段类型、完整工作流、边界场景
 */
const BASE = 'http://localhost:3000';
const API = BASE + '/api';

let passed = 0, failed = 0;
const errors = [];

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

// ============================================================
// ETL 阶段类型和规则类型常量
// ============================================================
const STAGE_TYPES = ['CLEAN', 'TRANSFORM', 'ENRICH', 'VALIDATE', 'ROUTE'];
const RULE_TYPES = [
  'VALUE_MAP', 'FORMAT_CONVERT', 'EXPRESSION', 'FIELD_CONCAT',
  'DEFAULT_VALUE', 'TRIM', 'UPPER_CASE', 'LOWER_CASE',
  'REGEX_REPLACE', 'FIELD_RENAME', 'FIELD_ADD', 'FIELD_REMOVE',
  'FIELD_SPLIT', 'FILTER', 'SCRIPT', 'DICT_LOOKUP', 'API_CALL'
];

async function main() {
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('  ETL Sync System V3.0 - ETL 数据处理功能深度测试');
  console.log('═══════════════════════════════════════════════════════════════');
  console.log(`  目标: ${API}`);

  const IDS = {};

  // ==================== 1. Pipeline CRUD ====================
  console.log('\n═══════ 1. Pipeline 完整 CRUD ═══════');

  await runTest('创建 Pipeline（仅必填字段）', async () => {
    const { status, data } = await apiPost('/etl/pipeline', {
      name: 'etl-test-minimal', description: 'minimal test'
    });
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data > 0, `返回 ID: ${data?.data}`);
    IDS.pipeline1 = data.data;
  });

  await runTest('创建 Pipeline（全字段）', async () => {
    const { ok, data } = await apiPost('/etl/pipeline', {
      name: 'etl-test-full', description: 'full fields test', status: 1,
      createdBy: 'e2e-tester'
    });
    assert(ok && data?.data > 0, `创建成功 ID=${data?.data}`);
    IDS.pipeline2 = data.data;
  });

  await runTest('分页查询 Pipeline', async () => {
    const { status, data } = await apiGet('/etl/pipeline/page?pageNum=1&pageSize=10');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.list !== undefined, '返回 list');
    assert(data?.data?.total >= 2, `total >= 2 (实际: ${data.data.total})`);
  });

  await runTest('分页查询 Pipeline（带名称过滤）', async () => {
    const { data } = await apiGet('/etl/pipeline/page?name=etl-test-full');
    assert(data?.code === 200, 'HTTP 200');
    const list = data?.data?.list || [];
    assert(list.some(p => p.name === 'etl-test-full'), '过滤结果包含匹配项');
  });

  await runTest('查询 Pipeline 详情', async () => {
    const { status, data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}`);
    assertEq(status, 200, 'HTTP 200');
    assertEq(data?.data?.name, 'etl-test-full', '名称匹配');
    assert(data?.data?.status !== undefined, '含 status');
  });

  await runTest('更新 Pipeline', async () => {
    const { ok } = await apiPut(`/etl/pipeline/${IDS.pipeline2}`, {
      name: 'etl-test-full-updated', description: 'updated desc'
    });
    assert(ok, '更新成功');
    const { data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}`);
    assertEq(data?.data?.name, 'etl-test-full-updated', '验证更新后名称');
  });

  await runTest('查询不存在的 Pipeline', async () => {
    const { data } = await apiGet('/etl/pipeline/99999');
    assert(data?.code === 200, '返回 200');
    assert(data?.data === null, '返回 null 数据');
  });

  // ==================== 2. Stage CRUD ====================
  console.log('\n═══════ 2. 转换阶段完整 CRUD ═══════');

  const stageIds = [];
  await runTest('创建 Stage（所有类型）', async () => {
    for (const type of STAGE_TYPES) {
      const { ok, data } = await apiPost('/etl/pipeline/stage', {
        pipelineId: IDS.pipeline2, stageName: `阶段-${type}`, stageOrder: stageIds.length,
        stageType: type, enabled: 1, stopOnError: type === 'VALIDATE' ? 1 : 0,
        description: `${type} 类型阶段测试`
      });
      assert(ok && data?.data > 0, `创建 ${type} 阶段 ID=${data?.data}`);
      stageIds.push(data.data);
    }
  });
  IDS.stageIds = stageIds;

  await runTest('查询 Pipeline 下的 Stage 列表', async () => {
    const { status, data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}/stages`);
    assertEq(status, 200, 'HTTP 200');
    assert(Array.isArray(data?.data), '返回数组');
    assertEq(data.data.length, STAGE_TYPES.length, `含 ${STAGE_TYPES.length} 个阶段`);
  });

  await runTest('更新 Stage', async () => {
    const { ok } = await apiPut(`/etl/pipeline/stage/${stageIds[0]}`, {
      stageName: '清洗阶段-更新', stageType: 'CLEAN', description: 'updated'
    });
    assert(ok, '更新成功');
    const { data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}/stages`);
    const updated = data.data.find(s => s.id === stageIds[0]);
    assertEq(updated?.stageName, '清洗阶段-更新', '验证名称更新');
  });

  await runTest('Stage 重排序', async () => {
    const reversed = [...stageIds].reverse();
    const { ok } = await apiPut('/etl/pipeline/stage/reorder', reversed);
    assert(ok, '重排序成功');
    const { data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}/stages`);
    assertEq(data.data[0].id, reversed[0], '验证顺序已变更');
  });

  // ==================== 3. Rule CRUD ====================
  console.log('\n═══════ 3. 转换规则完整 CRUD ═══════');

  const ruleIds = [];
  await runTest('创建 Rule（所有类型）', async () => {
    for (let i = 0; i < RULE_TYPES.length; i++) {
      const type = RULE_TYPES[i];
      const ruleConfig = getRuleConfigExample(type);
      const { ok, data } = await apiPost('/etl/pipeline/rule', {
        stageId: stageIds[0], ruleName: `规则-${type}`, ruleType: type,
        sortOrder: i, sourceField: 'source_col', targetField: 'target_col',
        enabled: 1, ruleConfig: ruleConfig ? JSON.stringify(ruleConfig) : null,
        filterExpression: null
      });
      assert(ok && data?.data > 0, `创建 ${type} 规则 ID=${data?.data}`);
      ruleIds.push(data.data);
    }
  });
  IDS.ruleIds = ruleIds;

  await runTest('查询 Stage 下的 Rule 列表', async () => {
    const { status, data } = await apiGet(`/etl/pipeline/stage/${stageIds[0]}/rules`);
    assertEq(status, 200, 'HTTP 200');
    assert(Array.isArray(data?.data), '返回数组');
    assertEq(data.data.length, RULE_TYPES.length, `含 ${RULE_TYPES.length} 条规则`);
  });

  await runTest('更新 Rule', async () => {
    const { ok } = await apiPut(`/etl/pipeline/rule/${ruleIds[0]}`, {
      ruleName: '值映射-更新', ruleType: 'VALUE_MAP', sortOrder: 99,
      sourceField: 'updated_col', targetField: 'updated_col'
    });
    assert(ok, '更新成功');
    const { data } = await apiGet(`/etl/pipeline/stage/${stageIds[0]}/rules`);
    const updated = data.data.find(r => r.id === ruleIds[0]);
    assertEq(updated?.sourceField, 'updated_col', '验证源字段更新');
  });

  await runTest('创建 Rule（含 JSON 配置）', async () => {
    const { ok, data } = await apiPost('/etl/pipeline/rule', {
      stageId: stageIds[0], ruleName: '正则替换规则', ruleType: 'REGEX_REPLACE',
      sortOrder: 99, sourceField: 'phone', targetField: 'phone',
      ruleConfig: JSON.stringify({ pattern: '(\\d{3})\\d{4}(\\d{4})', replacement: '$1****$2' })
    });
    assert(ok && data?.data > 0, `创建含 JSON 配置规则 ID=${data?.data}`);
    IDS.regexRuleId = data.data;
  });

  await runTest('创建 Rule（含过滤表达式）', async () => {
    const { ok, data } = await apiPost('/etl/pipeline/rule', {
      stageId: stageIds[1], ruleName: '过滤规则', ruleType: 'FILTER',
      sortOrder: 0, sourceField: 'age', targetField: 'age',
      filterExpression: 'age > 0 AND age < 150'
    });
    assert(ok && data?.data > 0, `创建含过滤表达式规则 ID=${data?.data}`);
    IDS.filterRuleId = data.data;
  });

  await runTest('创建 Rule（脚本规则）', async () => {
    const { ok, data } = await apiPost('/etl/pipeline/rule', {
      stageId: stageIds[1], ruleName: '脚本转换', ruleType: 'SCRIPT',
      sortOrder: 1, sourceField: 'input', targetField: 'output',
      ruleConfig: JSON.stringify({ script: 'function transform(val){return val.toUpperCase();}', language: 'GROOVY' })
    });
    assert(ok && data?.data > 0, `创建脚本规则 ID=${data?.data}`);
    IDS.scriptRuleId = data.data;
  });

  // ==================== 4. 预览功能 ====================
  console.log('\n═══════ 4. 转换预览 ═══════');

  await runTest('POST 预览转换', async () => {
    const { status, data } = await apiPost('/etl/pipeline/preview', {
      pipelineId: IDS.pipeline2, sampleData: [{ name: 'test', age: 25 }]
    });
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data !== undefined, '返回预览结果');
  });

  // ==================== 5. 转换日志 ====================
  console.log('\n═══════ 5. 转换日志查询 ═══════');

  await runTest('分页查询转换日志', async () => {
    const { status, data } = await apiGet('/log/transform/page?pageNum=1&pageSize=10');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.list !== undefined, '返回分页数据');
  });

  await runTest('转换日志多条件过滤', async () => {
    const { ok } = await apiGet('/log/transform/page?pageNum=1&pageSize=10&ruleType=VALUE_MAP');
    assert(ok, '按规则类型过滤');
    const { ok: ok2 } = await apiGet('/log/transform/page?pageNum=1&pageSize=10&status=SUCCESS');
    assert(ok2, '按状态过滤');
    const { ok: ok3 } = await apiGet('/log/transform/page?pageNum=1&pageSize=10&traceId=T001');
    assert(ok3, '按 TraceID 过滤');
  });

  await runTest('查询转换日志详情', async () => {
    const { status } = await apiGet('/log/transform/0');
    assert(status === 200, `HTTP ${status}`);
  });

  // ==================== 6. 数据质量 ====================
  console.log('\n═══════ 6. 数据质量测试 ═══════');

  await runTest('分页查询质量日志', async () => {
    const { status, data } = await apiGet('/etl/quality/log/page?pageNum=1&pageSize=10');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.list !== undefined, '返回分页数据');
  });

  await runTest('质量日志严重级别过滤', async () => {
    const { ok } = await apiGet('/etl/quality/log/page?severity=ERROR');
    assert(ok, '按 ERROR 级别过滤');
    const { ok: ok2 } = await apiGet('/etl/quality/log/page?severity=WARNING');
    assert(ok2, '按 WARNING 级别过滤');
    const { ok: ok3 } = await apiGet('/etl/quality/log/page?severity=INFO');
    assert(ok3, '按 INFO 级别过滤');
  });

  await runTest('质量日志状态过滤', async () => {
    const { ok } = await apiGet('/etl/quality/log/page?status=OPEN');
    assert(ok, '按 OPEN 状态过滤');
    const { ok: ok2 } = await apiGet('/etl/quality/log/page?status=IGNORED');
    assert(ok2, '按 IGNORED 状态过滤');
  });

  await runTest('质量日志任务 ID 过滤', async () => {
    const { ok } = await apiGet('/etl/quality/log/page?taskId=1');
    assert(ok, '按任务 ID 过滤');
  });

  await runTest('获取质量报告', async () => {
    const { status, data } = await apiGet('/etl/quality/report');
    assertEq(status, 200, 'HTTP 200');
    if (data?.data) {
      const report = data.data;
      assert(typeof report.total === 'number', '含 total');
      assert(typeof report.passed === 'number', '含 passed');
      assert(typeof report.failed === 'number', '含 failed');
      assert(typeof report.passRate === 'number', '含 passRate');
    }
  });

  // ==================== 7. 数据校验 ====================
  console.log('\n═══════ 7. 数据校验测试 ═══════');

  await runTest('分页查询校验记录', async () => {
    const { status, data } = await apiGet('/etl/validation/page?pageNum=1&pageSize=10');
    assertEq(status, 200, 'HTTP 200');
    assert(data?.data?.list !== undefined, '返回分页数据');
  });

  await runTest('校验记录状态过滤', async () => {
    const { ok } = await apiGet('/etl/validation/page?status=PASSED');
    assert(ok, '按 PASSED 状态过滤');
    const { ok: ok2 } = await apiGet('/etl/validation/page?status=FAILED');
    assert(ok2, '按 FAILED 状态过滤');
  });

  await runTest('校验记录任务 ID 过滤', async () => {
    const { ok } = await apiGet('/etl/validation/page?taskId=1');
    assert(ok, '按任务 ID 过滤');
  });

  await runTest('查询校验详情', async () => {
    const { status } = await apiGet('/etl/validation/0');
    assert(status === 200, `HTTP ${status}`);
  });

  // ==================== 8. Pipeline Workflow ====================
  console.log('\n═══════ 8. 完整 Pipeline 工作流测试 ═══════');

  await runTest('多层 Pipeline 查询', async () => {
    const { data } = await apiGet(`/etl/pipeline/${IDS.pipeline2}`);
    assert(data?.data, 'Pipeline 存在');
    const stages = await apiGet(`/etl/pipeline/${IDS.pipeline2}/stages`);
    assert(stages.data?.data?.length > 0, '有阶段');
    const rules = await apiGet(`/etl/pipeline/stage/${IDS.stageIds[0]}/rules`);
    assert(rules.data?.data?.length > 0, '阶段有规则');
  });

  await runTest('重命名 Pipeline 后验证结构完整', async () => {
    await apiPut(`/etl/pipeline/${IDS.pipeline1}`, { name: 'etl-minimal-renamed' });
    const { data } = await apiGet(`/etl/pipeline/${IDS.pipeline1}`);
    assertEq(data?.data?.name, 'etl-minimal-renamed', '名称已更新');
  });

  // ==================== 9. 异常场景 ====================
  console.log('\n═══════ 9. 异常场景测试 ═══════');

  await runTest('删除不存在的 Pipeline', async () => {
    const { ok } = await apiDelete('/etl/pipeline/99999');
    assert(ok, '删除不存在的 Pipeline 不报错');
  });

  await runTest('删除不存在的 Stage', async () => {
    const { ok } = await apiDelete('/etl/pipeline/stage/99999');
    assert(ok, '删除不存在的 Stage 不报错');
  });

  await runTest('删除不存在的 Rule', async () => {
    const { ok } = await apiDelete('/etl/pipeline/rule/99999');
    assert(ok, '删除不存在的 Rule 不报错');
  });

  await runTest('无参数 Pipeline 查询', async () => {
    const { ok } = await apiGet('/etl/pipeline/page');
    assert(ok, '无参数分页查询');
  });

  await runTest('Pipeline 大量分页参数', async () => {
    const { ok } = await apiGet('/etl/pipeline/page?pageNum=1&pageSize=100');
    assert(ok, '大 pageSize 查询');
    const { ok: ok2 } = await apiGet('/etl/pipeline/page?pageNum=100&pageSize=10');
    assert(ok2, '超出范围 pageNum 查询');
  });

  await runTest('Stage 重排空列表', async () => {
    const { ok } = await apiPut('/etl/pipeline/stage/reorder', []);
    assert(ok, '空列表重排');
  });

  // ==================== 10. 清理 ====================
  console.log('\n═══════ 10. 清理测试数据 ═══════');

  await runTest('删除额外创建的 Rule', async () => {
    if (IDS.regexRuleId) assert((await apiDelete(`/etl/pipeline/rule/${IDS.regexRuleId}`)).ok, '删除正则规则');
    if (IDS.filterRuleId) assert((await apiDelete(`/etl/pipeline/rule/${IDS.filterRuleId}`)).ok, '删除过滤规则');
    if (IDS.scriptRuleId) assert((await apiDelete(`/etl/pipeline/rule/${IDS.scriptRuleId}`)).ok, '删除脚本规则');
  });

  await runTest('删除所有 Stage', async () => {
    for (const id of IDS.stageIds || []) {
      const { ok } = await apiDelete(`/etl/pipeline/stage/${id}`);
      assert(ok, `删除 Stage ${id}`);
    }
  });

  await runTest('删除所有 Rule', async () => {
    for (const id of IDS.ruleIds || []) {
      const { ok } = await apiDelete(`/etl/pipeline/rule/${id}`);
      assert(ok, `删除 Rule ${id}`);
    }
  });

  await runTest('删除 Pipeline', async () => {
    if (IDS.pipeline1) assert((await apiDelete(`/etl/pipeline/${IDS.pipeline1}`)).ok, '删除 pipeline1');
    if (IDS.pipeline2) assert((await apiDelete(`/etl/pipeline/${IDS.pipeline2}`)).ok, '删除 pipeline2');
  });

  // ==================== 测试报告 ====================
  console.log('\n═══════════════════════════════════════════════════════════════');
  console.log('  ETL 数据处理深度测试报告');
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

// ============================================================
// 辅助：根据规则类型返回示例 JSON 配置
// ============================================================
function getRuleConfigExample(type) {
  switch (type) {
    case 'VALUE_MAP': return { mapping: { 'Y': '是', 'N': '否', '1': '激活', '0': '停用' } };
    case 'FORMAT_CONVERT': return { sourceFormat: 'yyyyMMdd', targetFormat: 'yyyy-MM-dd' };
    case 'EXPRESSION': return { expression: '(salary * 1.1).toFixed(2)', resultType: 'double' };
    case 'FIELD_CONCAT': return { fields: ['firstName', 'lastName'], separator: ' ', targetField: 'fullName' };
    case 'DEFAULT_VALUE': return { defaultValue: 0, onNull: true, onEmpty: true };
    case 'TRIM': return { char: null, side: 'both' };
    case 'UPPER_CASE': return {};
    case 'LOWER_CASE': return {};
    case 'REGEX_REPLACE': return { pattern: '\\s+', replacement: '_' };
    case 'FIELD_RENAME': return { sourceName: 'old_name', targetName: 'new_name' };
    case 'FIELD_ADD': return { fieldName: 'createTime', valueExpression: 'new Date()', fieldType: 'datetime' };
    case 'FIELD_REMOVE': return {};
    case 'FIELD_SPLIT': return { field: 'full_address', separator: ',', targetFields: ['province', 'city', 'district'] };
    case 'FILTER': return {};
    case 'SCRIPT': return { script: 'function transform(row) { return row; }', language: 'GROOVY', timeoutMs: 5000 };
    case 'DICT_LOOKUP': return { dictCode: 'GENDER', sourceField: 'gender_code', targetField: 'gender_name' };
    case 'API_CALL': return { url: 'http://example.com/api/lookup', method: 'POST', timeoutMs: 3000 };
    default: return null;
  }
}

main().catch(e => { console.error('FATAL:', e); process.exit(1); });
