<template>
  <div class="demo-center">
    <!-- 欢迎横幅 -->
    <div class="hero-banner">
      <div class="hero-content">
        <h1 class="hero-title">欢迎来到，FineDataLink</h1>
        <p class="hero-subtitle">在线 Demo 平台</p>
        <div class="hero-actions">
          <el-button type="warning" size="large" round @click="scrollTo('updates')" class="hero-btn hero-btn-updates">
            <el-icon><Bell /></el-icon> Demo更新日志
          </el-button>
          <el-button type="primary" size="large" round @click="scrollTo('consult')" class="hero-btn hero-btn-consult">
            <el-icon><Service /></el-icon> 方案咨询
          </el-button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="visual-orb v1"></div>
        <div class="visual-orb v2"></div>
        <div class="visual-orb v3"></div>
        <div class="visual-line vl1"></div>
        <div class="visual-line vl2"></div>
      </div>
    </div>

    <!-- 视频播放器 + 了解FineDataLink按钮 -->
    <div class="video-section">
      <div class="video-card">
        <div class="video-player">
          <div class="play-btn">
            <el-icon :size="52"><VideoPlay /></el-icon>
          </div>
          <div class="video-overlay-text">产品介绍视频</div>
        </div>
        <div class="video-cta">
          <el-button type="primary" size="large" round @click="$router.push('/dev/workbench')">
            了解 FineDataLink
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 资源区 -->
    <div class="resources-section">
      <h3 class="section-title">资源</h3>
      <el-row :gutter="16">
        <el-col :span="4" v-for="r in resources" :key="r.label">
          <div class="resource-card" @click="r.action ? r.action() : null">
            <div class="resource-icon" :style="{ background: r.color }">
              <el-icon :size="22"><component :is="r.icon" /></el-icon>
            </div>
            <span class="resource-label">{{ r.label }}</span>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 功能卡片网格 -->
    <div class="features-section">
      <h3 class="section-title">功能介绍</h3>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" v-for="card in featureCards" :key="card.title" class="feature-col">
          <div class="feature-card" @click="card.action ? card.action() : null">
            <div class="feature-img" :style="{ background: card.bg }">
              <el-icon :size="28"><component :is="card.icon" /></el-icon>
            </div>
            <div class="feature-info">
              <span class="feature-title">{{ card.title }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 底部资源卡片 -->
    <div class="bottom-resources">
      <h3 class="section-title">更多资源</h3>
      <el-row :gutter="16">
        <el-col :span="4" v-for="r in bottomResources" :key="r.label">
          <div class="bottom-card" :id="r.id" @click="r.action ? r.action() : null">
            <div class="bottom-card-icon" :style="{ background: r.color }">
              <el-icon :size="18"><component :is="r.icon" /></el-icon>
            </div>
            <div class="bottom-card-text">
              <h4>{{ r.label }}</h4>
              <p>{{ r.desc }}</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 分类Tab区域 -->
    <div class="showcase-section">
      <el-tabs v-model="showcaseTab" class="showcase-tabs">
        <el-tab-pane label="热门推荐" name="hot">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6" v-for="(item, idx) in hotRecommendations" :key="idx">
              <div class="showcase-card">
                <div class="showcase-thumb hot">
                  <span class="showcase-rank">{{ idx + 1 }}</span>
                </div>
                <span class="showcase-name">{{ item.title }}</span>
                <span class="showcase-desc">{{ item.desc }}</span>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="客户案例" name="cases">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6" v-for="(item, idx) in customerCases" :key="idx">
              <div class="showcase-card">
                <div class="showcase-thumb case">
                  <span class="showcase-rank">{{ idx + 1 }}</span>
                </div>
                <span class="showcase-name">{{ item.title }}</span>
                <span class="showcase-desc">{{ item.desc }}</span>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="功能应用" name="features">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6" v-for="(item, idx) in featureApps" :key="idx">
              <div class="showcase-card">
                <div class="showcase-thumb app">
                  <span class="showcase-rank">{{ idx + 1 }}</span>
                </div>
                <span class="showcase-name">{{ item.title }}</span>
                <span class="showcase-desc">{{ item.desc }}</span>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        <el-tab-pane label="价值场景" name="value">
          <el-row :gutter="16">
            <el-col :xs="12" :sm="6" v-for="(item, idx) in valueScenes" :key="idx">
              <div class="showcase-card">
                <div class="showcase-thumb value">
                  <span class="showcase-rank">{{ idx + 1 }}</span>
                </div>
                <span class="showcase-name">{{ item.title }}</span>
                <span class="showcase-desc">{{ item.desc }}</span>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const showcaseTab = ref('hot')

const scrollTo = (id) => {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}

const resources = [
  { label: '产品更新动态', icon: 'Bell', color: 'linear-gradient(135deg, #1890ff, #40a9ff)', action: () => scrollTo('updates') },
  { label: '联系技术支持', icon: 'Service', color: 'linear-gradient(135deg, #52c41a, #73d13d)', action: () => scrollTo('consult') },
  { label: '加入社区', icon: 'UserFilled', color: 'linear-gradient(135deg, #722ed1, #9254de)' },
  { label: '最佳实践', icon: 'Medal', color: 'linear-gradient(135deg, #fa8c16, #ffa940)' },
  { label: '帮助文档', icon: 'Document', color: 'linear-gradient(135deg, #13c2c2, #36cfc9)' },
  { label: '文档AI小助手', icon: 'MagicStick', color: 'linear-gradient(135deg, #eb2f96, #f759ab)' }
]

const bottomResources = [
  { id: 'updates', label: '产品更新动态', desc: '了解最新版本功能和改进', icon: 'Bell', color: '#e6f4ff', action: () => {} },
  { id: 'consult', label: '联系技术支持', desc: '获取专业技术支持和帮助', icon: 'Service', color: '#f0f9eb', action: () => {} },
  { label: '加入社区', desc: '与开发者交流分享经验', icon: 'UserFilled', color: '#f9f0ff' },
  { label: '最佳实践', desc: '行业解决方案和案例', icon: 'Medal', color: '#fff7e6' },
  { label: '帮助文档', desc: '完整的产品使用文档', icon: 'Document', color: '#e6fffb' },
  { label: '文档AI小助手', desc: '智能搜索和推荐', icon: 'MagicStick', color: '#fff0f6' }
]

const featureCards = [
  { title: '数据开发功能介绍', icon: 'DataAnalysis', bg: 'linear-gradient(135deg, #1890ff, #40a9ff)', action: () => router.push('/dev/workbench') },
  { title: '设备监控看板', icon: 'Odometer', bg: 'linear-gradient(135deg, #52c41a, #73d13d)' },
  { title: '电商经营场景方案', icon: 'ShoppingCart', bg: 'linear-gradient(135deg, #722ed1, #9254de)' },
  { title: '数据血缘功能介绍', icon: 'Share', bg: 'linear-gradient(135deg, #fa8c16, #ffa940)' },
  { title: '写入FineBI公共数据', icon: 'Upload', bg: 'linear-gradient(135deg, #13c2c2, #36cfc9)' },
  { title: '数据下云', icon: 'Download', bg: 'linear-gradient(135deg, #eb2f96, #f759ab)' },
  { title: '数据管道功能介绍', icon: 'Connection', bg: 'linear-gradient(135deg, #2f54eb, #597ef7)', action: () => router.push('/data-pipeline') },
  { title: '设备数据采集与应用', icon: 'Monitor', bg: 'linear-gradient(135deg, #a0d911, #bae637)' },
  { title: '零售业务场景', icon: 'Goods', bg: 'linear-gradient(135deg, #f5222d, #ff4d4f)' },
  { title: '数据服务功能介绍', icon: 'Promotion', bg: 'linear-gradient(135deg, #faad14, #ffc53d)', action: () => router.push('/api-service') },
  { title: '生产实时看板', icon: 'DataLine', bg: 'linear-gradient(135deg, #1890ff, #69c0ff)' },
  { title: '财务收入核算', icon: 'Coin', bg: 'linear-gradient(135deg, #52c41a, #95de64)' }
]

const hotRecommendations = [
  { rank: 1, title: '数据开发入门', desc: '快速上手ETL任务开发' },
  { rank: 2, title: 'ETL最佳实践', desc: '高效数据处理方案' },
  { rank: 3, title: '实时同步方案', desc: 'CDC实时数据管道' },
  { rank: 4, title: 'API服务构建', desc: '快速构建数据API' }
]

const customerCases = [
  { rank: 1, title: '零售行业方案', desc: '全渠道数据整合' },
  { rank: 2, title: '金融数据平台', desc: '实时风控数据管道' },
  { rank: 3, title: '制造业数字化', desc: '产线数据采集分析' },
  { rank: 4, title: '医疗数据集成', desc: '多院区数据互通' }
]

const featureApps = [
  { rank: 1, title: '数据同步', desc: '异构数据源实时同步' },
  { rank: 2, title: '实时管道', desc: 'CDC增量数据捕获' },
  { rank: 3, title: '任务编排', desc: 'DAG可视化编排' },
  { rank: 4, title: 'API管理', desc: '数据服务化发布' }
]

const valueScenes = [
  { rank: 1, title: '数仓搭建', desc: '企业级数据仓库' },
  { rank: 2, title: '数据中台', desc: '统一数据服务平台' },
  { rank: 3, title: '报表分析', desc: 'BI可视化分析' },
  { rank: 4, title: '业务监控', desc: '实时业务指标监控' }
]
</script>

<style lang="scss" scoped>
.demo-center {
  max-width: 1400px;
  margin: 0 auto;
}

// 欢迎横幅
.hero-banner {
  background: linear-gradient(135deg, #1890ff 0%, #0050b3 50%, #003a8c 100%);
  border-radius: 16px;
  padding: 48px 56px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;

  .hero-content { position: relative; z-index: 2; }
  .hero-title { color: #fff; font-size: 32px; font-weight: 700; margin: 0 0 8px; }
  .hero-subtitle { color: rgba(255,255,255,0.85); font-size: 20px; margin: 0 0 24px; font-weight: 300; }
  .hero-actions { display: flex; gap: 12px; }
  .hero-btn {
    font-size: 15px; padding: 10px 28px; border: none;
    &.hero-btn-updates { background: rgba(255,255,255,0.18); color: #fff; &:hover { background: rgba(255,255,255,0.28); } }
    &.hero-btn-consult { background: rgba(255,255,255,0.1); color: #fff; &:hover { background: rgba(255,255,255,0.22); } }
  }

  .hero-visual {
    position: absolute; right: 80px; top: 50%; transform: translateY(-50%);
    width: 240px; height: 160px;
    .visual-orb {
      position: absolute; border-radius: 50%;
      &.v1 { width: 80px; height: 80px; background: rgba(255,255,255,0.12); top: 0; left: 60px; }
      &.v2 { width: 50px; height: 50px; background: rgba(255,255,255,0.08); top: 40px; right: 20px; }
      &.v3 { width: 100px; height: 100px; background: rgba(255,255,255,0.06); bottom: 0; left: 10px; }
    }
    .visual-line {
      position: absolute; height: 2px; background: rgba(255,255,255,0.15);
      &.vl1 { width: 140px; top: 60px; left: 20px; transform: rotate(-12deg); }
      &.vl2 { width: 100px; top: 90px; left: 70px; transform: rotate(8deg); }
    }
  }
}

// 视频区域
.video-section { margin-bottom: 24px; }

.video-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.video-player {
  height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1628 0%, #132442 40%, #1a2d4a 100%);
  cursor: pointer;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background:
      radial-gradient(ellipse at 30% 40%, rgba(24,144,255,0.08) 0%, transparent 60%),
      radial-gradient(ellipse at 70% 60%, rgba(114,46,209,0.06) 0%, transparent 60%);
  }

  .play-btn {
    width: 96px; height: 96px;
    border-radius: 50%;
    background: rgba(255,255,255,0.1);
    border: 2px solid rgba(255,255,255,0.18);
    display: flex; align-items: center; justify-content: center;
    color: #fff;
    transition: all 0.35s;
    position: relative; z-index: 1;

    &:hover {
      background: rgba(24,144,255,0.3);
      border-color: rgba(24,144,255,0.5);
      transform: scale(1.1);
      box-shadow: 0 0 40px rgba(24,144,255,0.2);
    }
  }

  .video-overlay-text {
    color: rgba(255,255,255,0.5);
    font-size: 15px;
    margin-top: 20px;
    letter-spacing: 2px;
    position: relative; z-index: 1;
  }
}

.video-cta {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(to bottom, #fafafa, #fff);
  border-top: 1px solid #f0f0f0;

  .el-button {
    font-size: 16px;
    padding: 14px 48px;
    font-weight: 500;
  }
}

// 资源区
.resources-section { margin-bottom: 24px; }

.section-title {
  font-size: 18px; font-weight: 600; color: #1a1a1a;
  margin: 0 0 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0;
}

.resource-card {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 24px 12px; background: #fff; border-radius: 12px;
  cursor: pointer; transition: all 0.25s; border: 1px solid #f0f0f0;

  &:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }
  .resource-icon {
    width: 48px; height: 48px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center; color: #fff;
  }
  .resource-label { font-size: 13px; color: #555; text-align: center; font-weight: 500; }
}

// 功能卡片
.features-section { margin-bottom: 24px; }
.feature-col { margin-bottom: 16px; }

.feature-card {
  background: #fff; border-radius: 12px; overflow: hidden;
  cursor: pointer; transition: all 0.25s; border: 1px solid #f0f0f0;

  &:hover { box-shadow: 0 6px 20px rgba(0,0,0,0.08); transform: translateY(-2px); }
  .feature-img {
    height: 80px; display: flex; align-items: center; justify-content: center; color: #fff;
  }
  .feature-info {
    padding: 12px 16px;
    .feature-title { font-size: 13px; color: #333; font-weight: 500; line-height: 1.4; }
  }
}

// 底部资源卡片
.bottom-resources { margin-bottom: 24px; }

.bottom-card {
  display: flex; align-items: flex-start; gap: 12px;
  background: #fff; border-radius: 12px; padding: 20px;
  cursor: pointer; transition: all 0.25s; border: 1px solid #f0f0f0;
  margin-bottom: 16px;

  &:hover { box-shadow: 0 4px 15px rgba(0,0,0,0.06); transform: translateY(-1px); }

  .bottom-card-icon {
    width: 40px; height: 40px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    flex-shrink: 0;
    .el-icon { color: #fff; }
  }
  .bottom-card-text {
    h4 { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 4px; }
    p { font-size: 12px; color: #999; margin: 0; }
  }
}

// 展示区tabs
.showcase-section {
  .showcase-tabs {
    background: #fff; border-radius: 12px; padding: 24px; border: 1px solid #f0f0f0;
    :deep(.el-tabs__header) { margin-bottom: 20px; }
  }

  .showcase-card {
    cursor: pointer;
    .showcase-thumb {
      height: 100px; border-radius: 10px; margin-bottom: 10px;
      display: flex; align-items: flex-start; justify-content: flex-end;
      padding: 10px; position: relative;
      &.hot { background: linear-gradient(135deg, #e6f4ff, #91caff); }
      &.case { background: linear-gradient(135deg, #f9f0ff, #d3adf7); }
      &.app { background: linear-gradient(135deg, #e6fffb, #87e8de); }
      &.value { background: linear-gradient(135deg, #fff7e6, #ffd591); }
      .showcase-rank {
        font-size: 28px; font-weight: 700; color: rgba(0,0,0,0.08);
      }
    }
    .showcase-name { font-size: 14px; color: #333; font-weight: 500; display: block; }
    .showcase-desc { font-size: 12px; color: #999; margin-top: 4px; display: block; }
  }
}

@media (max-width: 768px) {
  .hero-banner { padding: 32px 24px; }
  .hero-visual { display: none; }
  .hero-title { font-size: 24px; }
  .video-player { height: 160px; }
}
</style>
