<template>
  <div class="data-management-page">
    <!-- 数据资产统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="asset-stat-card">
          <div class="asset-icon" style="background: #ecf5ff; color: #4f6ef7;">
            <el-icon :size="24"><Coin /></el-icon>
          </div>
          <div class="asset-content">
            <div class="asset-value">256</div>
            <div class="asset-label">数据表</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="asset-stat-card">
          <div class="asset-icon" style="background: #f0f9eb; color: #52c41a;">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="asset-content">
            <div class="asset-value">12</div>
            <div class="asset-label">数据源</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="asset-stat-card">
          <div class="asset-icon" style="background: #fdf6ec; color: #faad14;">
            <el-icon :size="24"><Share /></el-icon>
          </div>
          <div class="asset-content">
            <div class="asset-value">1,024</div>
            <div class="asset-label">数据字段</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="asset-stat-card">
          <div class="asset-icon" style="background: #fef0f0; color: #ff4d4f;">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="asset-content">
            <div class="asset-value">98.5%</div>
            <div class="asset-label">质量达标率</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据目录浏览 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="8">
        <el-card class="catalog-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><FolderOpened /></el-icon> 数据目录</span>
              <el-input v-model="catalogSearch" placeholder="搜索表/字段" size="small" clearable style="width: 180px">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </template>
          <div class="catalog-tree">
            <el-tree
              :data="catalogData"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              :default-expanded-keys="['1', '1-1']"
              :highlight-current="true"
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="catalog-node">
                  <el-icon v-if="data.type === 'datasource'" color="#4f6ef7"><Coin /></el-icon>
                  <el-icon v-else-if="data.type === 'database'" color="#52c41a"><FolderOpened /></el-icon>
                  <el-icon v-else-if="data.type === 'table'" color="#faad14"><Grid /></el-icon>
                  <el-icon v-else color="#909399"><Memo /></el-icon>
                  <span class="node-name">{{ node.label }}</span>
                  <el-tag v-if="data.rowCount" size="small" type="info" class="node-tag">{{ data.rowCount }}</el-tag>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>

      <!-- 表详情 + 数据血缘 -->
      <el-col :xs="24" :md="16">
        <el-card v-if="selectedTable" class="detail-card">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Grid /></el-icon> {{ selectedTable.name }}</span>
              <el-button size="small" type="primary" @click="activeTab = 'lineage'" v-if="activeTab !== 'lineage'">查看血缘</el-button>
              <el-button size="small" @click="activeTab = 'detail'" v-else>表详情</el-button>
            </div>
          </template>

          <!-- 表详情 -->
          <div v-if="activeTab === 'detail'" class="table-detail">
            <div class="detail-meta">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="所属数据源">{{ selectedTable.datasource }}</el-descriptions-item>
                <el-descriptions-item label="所属数据库">{{ selectedTable.database }}</el-descriptions-item>
                <el-descriptions-item label="行数">{{ selectedTable.rowCount || '未知' }}</el-descriptions-item>
                <el-descriptions-item label="存储大小">{{ selectedTable.size || '未知' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ selectedTable.createTime || '未知' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ selectedTable.updateTime || '未知' }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="column-list">
              <div class="section-subtitle">字段列表</div>
              <el-table :data="selectedTable.columns || []" size="small" stripe>
                <el-table-column prop="name" label="字段名" width="160" />
                <el-table-column prop="type" label="类型" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" type="info">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="comment" label="注释" min-width="200" show-overflow-tooltip />
                <el-table-column prop="nullable" label="可空" width="70">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.nullable ? 'success' : 'warning'">{{ row.nullable ? '是' : '否' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 数据血缘图 -->
          <div v-else class="lineage-container">
            <div ref="lineageChartRef" class="lineage-chart"></div>
          </div>
        </el-card>

        <!-- 默认提示 -->
        <el-card v-else class="detail-card">
          <div class="empty-detail">
            <el-icon :size="64" color="#dcdfe6"><FolderOpened /></el-icon>
            <p>请从左侧目录选择数据表</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据质量报告 -->
    <el-card class="quality-card">
      <template #header>
        <div class="card-header">
          <span class="card-title"><el-icon><Finished /></el-icon> 数据质量报告</span>
          <el-radio-group v-model="qualityPeriod" size="small">
            <el-radio-button value="day">今日</el-radio-button>
            <el-radio-button value="week">本周</el-radio-button>
            <el-radio-button value="month">本月</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <div ref="qualityScoreChartRef" class="quality-chart"></div>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="quality-detail">
            <div class="quality-item" v-for="item in qualityMetrics" :key="item.label">
              <div class="q-label">{{ item.label }}</div>
              <el-progress
                :percentage="item.value"
                :stroke-width="14"
                :color="item.color"
                :format="() => item.value + '%'"
              />
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const activeTab = ref('detail')
const catalogSearch = ref('')
const selectedTable = ref(null)
const qualityPeriod = ref('week')
const lineageChartRef = ref(null)
const qualityScoreChartRef = ref(null)
let lineageChart = null
let qualityScoreChart = null

const catalogData = ref([
  {
    id: '1', name: 'MySQL生产库', type: 'datasource',
    children: [
      {
        id: '1-1', name: 'order_db', type: 'database',
        children: [
          { id: '1-1-1', name: 'orders', type: 'table', rowCount: '2.3M' },
          { id: '1-1-2', name: 'order_items', type: 'table', rowCount: '8.5M' },
          { id: '1-1-3', name: 'order_status_log', type: 'table', rowCount: '15M' }
        ]
      },
      {
        id: '1-2', name: 'user_db', type: 'database',
        children: [
          { id: '1-2-1', name: 'users', type: 'table', rowCount: '5.2M' },
          { id: '1-2-2', name: 'user_profile', type: 'table', rowCount: '4.8M' },
          { id: '1-2-3', name: 'user_behavior', type: 'table', rowCount: '120M' }
        ]
      }
    ]
  },
  {
    id: '2', name: 'Doris分析库', type: 'datasource',
    children: [
      {
        id: '2-1', name: 'dwd', type: 'database',
        children: [
          { id: '2-1-1', name: 'dwd_order_detail', type: 'table', rowCount: '45M' },
          { id: '2-1-2', name: 'dwd_user_action', type: 'table', rowCount: '230M' }
        ]
      },
      {
        id: '2-2', name: 'ads', type: 'database',
        children: [
          { id: '2-2-1', name: 'ads_order_daily', type: 'table', rowCount: '365' },
          { id: '2-2-2', name: 'ads_user_retention', type: 'table', rowCount: '90' }
        ]
      }
    ]
  }
])

const qualityMetrics = ref([
  { label: '完整性', value: 99.2, color: '#52c41a' },
  { label: '准确性', value: 98.7, color: '#4f6ef7' },
  { label: '一致性', value: 97.5, color: '#13c2c2' },
  { label: '及时性', value: 99.8, color: '#52c41a' },
  { label: '唯一性', value: 99.9, color: '#4f6ef7' }
])

const tableDetails = {
  '1-1-1': {
    id: '1-1-1', name: 'orders', datasource: 'MySQL生产库', database: 'order_db',
    rowCount: '2,300,000', size: '1.2 GB', createTime: '2024-01-15', updateTime: '2024-12-20 10:30',
    columns: [
      { name: 'id', type: 'BIGINT', comment: '订单ID', nullable: false },
      { name: 'order_no', type: 'VARCHAR(32)', comment: '订单号', nullable: false },
      { name: 'user_id', type: 'BIGINT', comment: '用户ID', nullable: false },
      { name: 'total_amount', type: 'DECIMAL(12,2)', comment: '订单金额', nullable: false },
      { name: 'status', type: 'TINYINT', comment: '订单状态', nullable: false },
      { name: 'pay_time', type: 'DATETIME', comment: '支付时间', nullable: true },
      { name: 'create_time', type: 'DATETIME', comment: '创建时间', nullable: false },
      { name: 'update_time', type: 'DATETIME', comment: '更新时间', nullable: false }
    ]
  },
  '1-1-2': {
    id: '1-1-2', name: 'order_items', datasource: 'MySQL生产库', database: 'order_db',
    rowCount: '8,500,000', size: '3.5 GB', createTime: '2024-01-15', updateTime: '2024-12-20 10:30',
    columns: [
      { name: 'id', type: 'BIGINT', comment: '明细ID', nullable: false },
      { name: 'order_id', type: 'BIGINT', comment: '订单ID', nullable: false },
      { name: 'product_id', type: 'BIGINT', comment: '商品ID', nullable: false },
      { name: 'quantity', type: 'INT', comment: '数量', nullable: false },
      { name: 'unit_price', type: 'DECIMAL(10,2)', comment: '单价', nullable: false }
    ]
  },
  '2-1-1': {
    id: '2-1-1', name: 'dwd_order_detail', datasource: 'Doris分析库', database: 'dwd',
    rowCount: '45,000,000', size: '15 GB', createTime: '2024-03-01', updateTime: '2024-12-20 11:00',
    columns: [
      { name: 'id', type: 'BIGINT', comment: '主键', nullable: false },
      { name: 'order_no', type: 'VARCHAR(32)', comment: '订单号', nullable: false },
      { name: 'user_id', type: 'BIGINT', comment: '用户ID', nullable: false },
      { name: 'product_id', type: 'BIGINT', comment: '商品ID', nullable: false },
      { name: 'category_id', type: 'INT', comment: '类目ID', nullable: true },
      { name: 'amount', type: 'DECIMAL(12,2)', comment: '金额', nullable: false },
      { name: 'order_date', type: 'DATE', comment: '订单日期', nullable: false },
      { name: 'etl_time', type: 'DATETIME', comment: 'ETL时间', nullable: false }
    ]
  }
}

onMounted(() => {
  // 默认选中第一个表
  selectedTable.value = tableDetails['1-1-1']
  nextTick(() => {
    initLineageChart()
    initQualityChart()
  })
})

onUnmounted(() => {
  lineageChart?.dispose()
  qualityScoreChart?.dispose()
})

const handleNodeClick = (data) => {
  if (data.type === 'table' && tableDetails[data.id]) {
    selectedTable.value = tableDetails[data.id]
    activeTab.value = 'detail'
  }
}

const initLineageChart = () => {
  if (!lineageChartRef.value) return
  lineageChart = echarts.init(lineageChartRef.value)

  const nodes = [
    { id: '0', name: 'MySQL\norders', x: 80, y: 200, itemStyle: { color: '#4f6ef7' }, symbolSize: 55 },
    { id: '1', name: 'MySQL\norder_items', x: 80, y: 100, itemStyle: { color: '#4f6ef7' }, symbolSize: 50 },
    { id: '2', name: 'MySQL\nusers', x: 80, y: 300, itemStyle: { color: '#4f6ef7' }, symbolSize: 48 },
    { id: '3', name: 'ETL\n清洗转换', x: 350, y: 200, itemStyle: { color: '#faad14' }, symbolSize: 60 },
    { id: '4', name: 'Doris\ndwd_order_detail', x: 620, y: 200, itemStyle: { color: '#52c41a' }, symbolSize: 60 },
    { id: '5', name: 'Doris\nads_order_daily', x: 620, y: 340, itemStyle: { color: '#13c2c2' }, symbolSize: 48 },
    { id: '6', name: 'BI报表\n订单看板', x: 750, y: 340, itemStyle: { color: '#ff4d4f' }, symbolSize: 45 }
  ]

  const links = [
    { source: '0', target: '3' },
    { source: '1', target: '3' },
    { source: '2', target: '3' },
    { source: '3', target: '4' },
    { source: '4', target: '5' },
    { source: '5', target: '6' }
  ]

  lineageChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    series: [{
      type: 'graph',
      layout: 'none',
      roam: true,
      draggable: true,
      data: nodes,
      links: links,
      lineStyle: { color: '#c0c4cc', curveness: 0.2 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 8,
      label: { show: true, fontSize: 11, color: '#303133' },
      emphasis: { focus: 'adjacency', lineStyle: { width: 3 } }
    }]
  })
}

const initQualityChart = () => {
  if (!qualityScoreChartRef.value) return
  qualityScoreChart = echarts.init(qualityScoreChartRef.value)

  const dates = ['12/14', '12/15', '12/16', '12/17', '12/18', '12/19', '12/20']

  qualityScoreChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      textStyle: { color: '#303133' }
    },
    legend: { data: ['完整性', '准确性', '一致性', '及时性'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      min: 90,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
      axisLabel: { color: '#909399', formatter: '{value}%' }
    },
    series: [
      { name: '完整性', type: 'line', data: [98.5, 98.7, 99.0, 98.8, 99.1, 99.2, 99.2], smooth: true, lineStyle: { color: '#52c41a' }, itemStyle: { color: '#52c41a' } },
      { name: '准确性', type: 'line', data: [97.8, 98.0, 98.2, 98.5, 98.3, 98.6, 98.7], smooth: true, lineStyle: { color: '#4f6ef7' }, itemStyle: { color: '#4f6ef7' } },
      { name: '一致性', type: 'line', data: [96.5, 96.8, 97.0, 97.2, 97.5, 97.5, 97.5], smooth: true, lineStyle: { color: '#13c2c2' }, itemStyle: { color: '#13c2c2' } },
      { name: '及时性', type: 'line', data: [99.0, 99.2, 99.5, 99.3, 99.6, 99.7, 99.8], smooth: true, lineStyle: { color: '#faad14' }, itemStyle: { color: '#faad14' } }
    ]
  })
}
</script>

<style lang="scss" scoped>
.data-management-page {
  background: #f5f7fa;
  padding: 20px;
  min-height: 100%;

  .stats-row { margin-bottom: 20px; }

  .asset-stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s;
    margin-bottom: 16px;

    &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }

    .asset-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .asset-content {
      .asset-value { font-size: 24px; font-weight: 700; color: #303133; }
      .asset-label { font-size: 13px; color: #909399; margin-top: 2px; }
    }
  }

  .catalog-card, .detail-card, .quality-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }

  .catalog-card {
    height: calc(100% - 20px);

    .catalog-tree {
      max-height: 520px;
      overflow-y: auto;

      .catalog-node {
        display: flex;
        align-items: center;
        gap: 6px;
        flex: 1;

        .node-name { font-size: 13px; color: #303133; }
        .node-tag { margin-left: auto; }
      }
    }
  }

  .detail-card {
    .table-detail {
      .detail-meta { margin-bottom: 20px; }

      .column-list {
        .section-subtitle {
          font-size: 15px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 12px;
        }
      }
    }

    .lineage-container {
      .lineage-chart { height: 400px; }
    }

    .empty-detail {
      text-align: center;
      padding: 60px 0;
      color: #909399;

      p { margin-top: 16px; font-size: 15px; }
    }
  }

  .quality-card {
    .quality-chart { height: 280px; }

    .quality-detail {
      padding: 16px 0;

      .quality-item {
        margin-bottom: 24px;

        &:last-child { margin-bottom: 0; }

        .q-label {
          font-size: 14px;
          font-weight: 500;
          color: #606266;
          margin-bottom: 8px;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .data-management-page { padding: 16px; }
}
</style>
