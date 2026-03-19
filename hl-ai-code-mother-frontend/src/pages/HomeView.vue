<script setup lang="ts">
</script>

<template>
  <section class="home-view">
    <h1>欢迎来到编程导航 AI 项目</h1>
    <p>这里是全局基础布局下的首页内容区域，后续页面都会在这里随路由切换展示。</p>
  </section>
</template>

<style scoped>
.home-view {
  padding: 32px;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}

.home-view h1 {
  margin-bottom: 12px;
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 600;
  color: #1f1f1f;
}

.home-view p {
  color: rgba(0, 0, 0, 0.65);
  font-size: 16px;
}

@media (max-width: 768px) {
  .home-view {
    padding: 24px;
    border-radius: 16px;
  }
}
</style>
