<template>
  <Layout class="basic-layout">
    <ALayoutHeader class="layout-header">
      <div class="header-container">
        <GlobalHeader title="HL应用生成" :originItems="menuItems" />
      </div>
    </ALayoutHeader>

    <ALayoutContent class="layout-content">
      <div class="content-inner">
        <RouterView />
      </div>
    </ALayoutContent>

    <ALayoutFooter v-if="!route.meta.fullWidth" class="layout-footer">
      <GlobalFooter />
    </ALayoutFooter>
  </Layout>
</template>

<script setup lang="ts">
import { Layout } from 'ant-design-vue'
import { RouterView, useRoute } from 'vue-router'

import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
import { menuItems } from '@/config/menu'

const { Header: ALayoutHeader, Content: ALayoutContent, Footer: ALayoutFooter } = Layout
const route = useRoute()
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: transparent;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  height: auto;
  line-height: normal;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.header-container {
  width: 100%;
  padding: 12px 20px 0;
}

.layout-content {
  flex: 1;
  display: flex;
  width: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 24px 24px 28px;
}

.content-inner {
  display: flex;
  flex: 1;
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.layout-footer {
  margin-top: auto;
  padding: 16px 24px 24px;
  background: transparent;
}

@media (max-width: 768px) {
  .layout-content {
    min-height: 0;
    padding: 16px 16px 20px;
  }

  .header-container {
    padding: 10px 12px 0;
  }

  .layout-footer {
    padding: 12px 16px 20px;
  }
}
</style>
