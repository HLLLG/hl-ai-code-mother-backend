<template>
  <Layout class="basic-layout">
    <ALayoutHeader class="layout-header">
      <div class="header-container">
        <GlobalHeader title="HL应用生成" :menu-items="menuItems" />
      </div>
    </ALayoutHeader>

    <ALayoutContent class="layout-content">
      <div class="layout-container content-inner">
        <RouterView />
      </div>
    </ALayoutContent>

    <ALayoutFooter class="layout-footer">
      <GlobalFooter />
    </ALayoutFooter>
  </Layout>
</template>

<script setup lang="ts">
import { Layout } from 'ant-design-vue'
import { RouterView } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
import { menuItems } from '@/config/menu'

const { Header: ALayoutHeader, Content: ALayoutContent, Footer: ALayoutFooter } = Layout
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: auto;
  line-height: normal;
  padding: 0;
  background: linear-gradient(90deg, #1677ff 0%, #4096ff 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-container {
  width: 100%;
  padding: 8px 20px;
}

.layout-container {
  width: min(1200px, calc(100% - 48px));
  margin: 0 auto;
}

.layout-content {
  flex: 1;
  padding: 24px 0;
}

.content-inner {
  width: min(1200px, calc(100% - 48px));
}

.layout-footer {
  margin-top: auto;
  padding: 16px 24px 24px;
  background: #f5f7fa;
}

@media (max-width: 768px) {
  .layout-content {
    padding: 16px 0;
  }

  .header-container {
    padding: 8px 12px;
  }

  .layout-container,
  .content-inner {
    width: min(1200px, calc(100% - 32px));
  }

  .layout-footer {
    padding: 12px 16px 20px;
  }
}
</style>
