<template>
  <div class="home-page">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="nav-left">
        <el-icon :size="28" color="#409EFF"><Ship /></el-icon>
        <span class="nav-title">海洋环境预报系统</span>
      </div>
      <div class="nav-right">
        <el-button type="primary" round @click="$router.push('/login')">登 录</el-button>
        <el-button round @click="$router.push('/register')">注 册</el-button>
      </div>
    </header>

    <!-- 英雄区 -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">海洋环境预报系统</h1>
        <p class="hero-subtitle">Ocean Environment Forecast System</p>
        <p class="hero-desc">实时监测海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="$router.push('/register')">
            立即注册
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" round @click="$router.push('/login')">
            已有账号？去登录
          </el-button>
        </div>
      </div>
    </section>

    <!-- 特性介绍 -->
    <section class="features">
      <div class="section-title">
        <h2>系统功能</h2>
        <p>全方位海洋环境预报数据服务</p>
      </div>
      <el-row :gutter="30" class="feature-row">
        <el-col :span="8">
          <div class="feature-card">
            <div class="feature-icon" style="background: #e6f7ff;">
              <el-icon :size="40" color="#1890ff"><TrendCharts /></el-icon>
            </div>
            <h3>数据可视化</h3>
            <p>海表温度、叶绿素浓度实时图表展示，多观测点趋势对比分析</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <div class="feature-icon" style="background: #f6ffed;">
              <el-icon :size="40" color="#52c41a"><Setting /></el-icon>
            </div>
            <h3>模型管理</h3>
            <p>集成多种海洋预报模型，支持参数配置、启停控制与状态监控</p>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="feature-card">
            <div class="feature-icon" style="background: #fff7e6;">
              <el-icon :size="40" color="#fa8c16"><DataBoard /></el-icon>
            </div>
            <h3>预报记录</h3>
            <p>历史预报数据查询与导出，支持按日期、观测点多维度检索</p>
          </div>
        </el-col>
      </el-row>
    </section>

    <!-- 页脚 -->
    <footer class="footer">
      <p>Ocean Environment Forecast System &copy; 2026</p>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f0f5fa;
}

/* 导航栏 */
.navbar {
  height: 64px;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 60px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a3a5c;
}

.nav-right {
  display: flex;
  gap: 12px;
}

/* 英雄区 */
.hero {
  background: linear-gradient(135deg, #0c1d3e 0%, #1a3a5c 40%, #0d4f7c 100%);
  padding: 100px 60px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.hero::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to top, rgba(255,255,255,0.06) 0%, transparent 100%);
  animation: wave 6s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 12px;
  letter-spacing: 2px;
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255,255,255,0.7);
  margin-bottom: 24px;
  letter-spacing: 4px;
  text-transform: uppercase;
}

.hero-desc {
  font-size: 16px;
  color: rgba(255,255,255,0.6);
  line-height: 1.8;
  margin-bottom: 40px;
}

.hero-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
}

/* 特性区 */
.features {
  padding: 80px 60px;
  background: #fff;
}

.section-title {
  text-align: center;
  margin-bottom: 50px;
}

.section-title h2 {
  font-size: 30px;
  color: #1a3a5c;
  margin-bottom: 10px;
}

.section-title p {
  color: #8899aa;
  font-size: 16px;
}

.feature-row {
  max-width: 1100px;
  margin: 0 auto;
}

.feature-card {
  text-align: center;
  padding: 40px 24px;
  border-radius: 12px;
  transition: transform 0.3s, box-shadow 0.3s;
  cursor: pointer;
}

.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.1);
}

.feature-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.feature-card h3 {
  font-size: 18px;
  color: #1a3a5c;
  margin-bottom: 12px;
}

.feature-card p {
  color: #8899aa;
  line-height: 1.6;
  font-size: 14px;
}

/* 页脚 */
.footer {
  text-align: center;
  padding: 30px;
  color: #8899aa;
  font-size: 14px;
  background: #f0f5fa;
  border-top: 1px solid #e8e8e8;
}

@media (max-width: 768px) {
  .navbar { padding: 0 20px; }
  .hero { padding: 60px 20px; }
  .hero-title { font-size: 28px; }
  .features { padding: 50px 20px; }
  .hero-actions { flex-direction: column; align-items: center; }
}
</style>
