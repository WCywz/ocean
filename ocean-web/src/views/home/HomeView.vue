<template>
  <div class="home-page">
    <!-- Nav -->
    <nav class="editorial-nav">
      <span class="editorial-nav__brand">海洋环境预报系统</span>
      <span class="editorial-nav__spacer"></span>
      <a class="editorial-link" style="margin-right: 20px;" @click="$router.push('/login')">登录</a>
      <a class="editorial-link" @click="$router.push('/register')">注册</a>
    </nav>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 style="font-family: var(--font-serif); font-size: 36px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0; letter-spacing: -0.02em;">
          海洋环境预报系统
        </h1>
        <p style="font-family: var(--font-serif); font-size: 15px; color: var(--color-text-muted); font-style: italic; margin: 0 0 32px 0;">
          Ocean Environment Forecast System
        </p>
        <p style="font-size: 15px; color: var(--color-text-secondary); line-height: 1.8; max-width: 560px; margin: 0 auto 36px auto;">
          实时监测海表温度与叶绿素浓度变化，为海洋科学研究与环境保护提供精准预报数据
        </p>
        <div style="display: flex; gap: 16px; justify-content: center;">
          <a class="editorial-btn" style="padding: 12px 32px; display: inline-block; text-decoration: none;" @click="$router.push('/register')">
            立即注册
          </a>
          <a class="editorial-btn-outline" style="padding: 11px 31px;" @click="$router.push('/login')">
            已有账号？去登录
          </a>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section style="padding: 72px 48px; border-top: 1px solid var(--color-divider);">
      <div style="text-align: center; margin-bottom: 48px;">
        <p class="editorial-section-label">Features</p>
        <h2 style="font-family: var(--font-serif); font-size: 24px; font-weight: 400; color: var(--color-text); margin: 0;">
          系统功能
        </h2>
      </div>
      <div style="display: flex; gap: 64px; max-width: 860px; margin: 0 auto;">
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">数据可视化</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">海表温度、叶绿素浓度实时图表展示，多观测点趋势对比分析</p>
        </div>
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">模型管理</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">集成多种海洋预报模型，支持参数配置、启停控制与状态监控</p>
        </div>
        <div style="flex: 1;">
          <h3 style="font-family: var(--font-serif); font-size: 16px; font-weight: 400; color: var(--color-text); margin: 0 0 8px 0;">预报记录</h3>
          <p style="font-size: 13px; color: var(--color-text-secondary); line-height: 1.7; margin: 0;">历史预报数据查询与导出，支持按日期、观测点多维度检索</p>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer style="text-align: center; padding: 28px; color: var(--color-text-muted); font-size: 12px; border-top: 1px solid var(--color-divider);">
      Ocean Environment Forecast System &copy; 2026
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  min-height: 100vh;
}
.hero {
  padding: 100px 48px 80px;
}
.hero-inner {
  max-width: 640px;
  margin: 0 auto;
  text-align: center;
}
</style>
