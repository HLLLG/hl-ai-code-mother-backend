<template>
  <header class="global-header">
    <div class="header-brand">
      <div class="brand" @click="goHome">
        <img :src="logoUrl" alt="网站 Logo" class="brand-logo" />
        <span class="brand-title">{{ title }}</span>
      </div>
    </div>

    <div class="header-main">
      <Menu
        mode="horizontal"
        :selected-keys="selectedKeys"
        :items="menuItems"
        class="header-menu"
        :style="menuStyle"
        @click="onMenuClick"
      />
    </div>

    <div class="header-actions">
      <Button class="login-button">登录</Button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button, Menu } from 'ant-design-vue'
import type { CSSProperties } from 'vue'
import type { MenuProps } from 'ant-design-vue'

import logoUrl from '@/assets/logo.png'
import type { MenuItemConfig } from '@/config/menu.ts'

const props = defineProps<{
  title: string
  menuItems: MenuItemConfig[]
}>()

const route = useRoute()
const router = useRouter()

const menuStyle: CSSProperties = {
  '--ant-menu-item-color': 'rgba(255, 255, 255, 0.84)',
  '--ant-menu-item-hover-color': '#ffffff',
  '--ant-menu-item-selected-color': '#ffffff',
  '--ant-menu-horizontal-item-selected-color': '#ffffff',
  '--ant-menu-horizontal-item-hover-color': '#ffffff',
  '--ant-menu-item-active-bg': 'rgba(255, 255, 255, 0.12)',
  '--ant-menu-item-hover-bg': 'rgba(255, 255, 255, 0.1)',
  '--ant-menu-item-selected-bg': 'rgba(255, 255, 255, 0.18)',
  '--ant-menu-active-bar-height': '0px',
  '--ant-menu-active-bar-width': '0px',
  '--ant-menu-horizontal-line-height': '46px',
  background: 'transparent',
  borderBottom: 'none',
} as CSSProperties

const selectedKeys = computed(() => {
  const matchedItem = props.menuItems.find((item) => {
    if (!('path' in item) || !item.path) {
      return false
    }

    return item.path === '/' ? route.path === item.path : route.path.startsWith(item.path)
  })

  return matchedItem?.key ? [String(matchedItem.key)] : []
})

const goHome = () => {
  if (route.path !== '/') {
    router.push('/')
  }
}

const onMenuClick: MenuProps['onClick'] = ({ key }) => {
  const target = props.menuItems.find((item) => String(item.key) === String(key))

  if (!target) {
    return
  }

  if ('href' in target && target.href) {
    window.open(target.href, target.target ?? '_self')
    return
  }

  if ('path' in target && target.path && target.path !== route.path) {
    router.push(target.path)
  }
}
</script>

<style scoped>
.global-header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 76px;
  width: 100%;
  padding: 10px 18px;
  border: 1px solid transparent;
  border-radius: 24px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.04)) padding-box,
    linear-gradient(120deg, rgba(255, 255, 255, 0.38), rgba(255, 255, 255, 0.12), rgba(120, 200, 255, 0.24))
      border-box;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 14px 40px rgba(6, 30, 75, 0.18),
    0 6px 16px rgba(6, 30, 75, 0.12);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}

.global-header::before {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 23px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.02));
  pointer-events: none;
}

.global-header > * {
  position: relative;
  z-index: 1;
}

.header-brand,
.header-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.header-main {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1 1 auto;
  min-width: 0;
}

.header-actions {
  justify-content: flex-end;
  margin-left: auto;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0.04));
  cursor: pointer;
  transition:
    background-color 0.25s ease,
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.brand:hover {
  border-color: rgba(255, 255, 255, 0.28);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
  box-shadow: 0 10px 24px rgba(8, 35, 87, 0.2);
  transform: translateY(-1px);
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 8px 18px rgba(13, 61, 145, 0.26);
}

.brand-title {
  color: #ffffff;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.2px;
  white-space: nowrap;
  text-shadow: 0 3px 12px rgba(0, 0, 0, 0.16);
}

.header-menu {
  flex: 1 1 auto;
  width: 100%;
  min-width: 0;
}

.header-menu :deep(.ant-menu) {
  width: 100%;
  background: transparent !important;
  border-bottom: none !important;
}

.header-menu :deep(.ant-menu-overflow) {
  display: flex;
  justify-content: center;
  width: 100%;
  gap: 10px;
}

.header-menu :deep(.ant-menu-item),
.header-menu :deep(.ant-menu-submenu),
.header-menu :deep(.ant-menu-submenu-title) {
  border-bottom: none !important;
}

.header-menu :deep(.ant-menu-item::after),
.header-menu :deep(.ant-menu-item-selected::after),
.header-menu :deep(.ant-menu-item:hover::after),
.header-menu :deep(.ant-menu-submenu::after),
.header-menu :deep(.ant-menu-submenu-selected::after),
.header-menu :deep(.ant-menu-submenu-title::after) {
  display: none !important;
  border-bottom: none !important;
  transform: none !important;
}

.header-menu :deep(.ant-menu-item) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-inline: 0;
  padding-inline: 18px;
  border: 1px solid transparent;
  border-radius: 999px;
  border-bottom: none !important;
  font-size: 15px;
  font-weight: 500;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.12);
  transition:
    background-color 0.25s ease,
    color 0.25s ease,
    transform 0.25s ease,
    box-shadow 0.25s ease,
    border-color 0.25s ease;
}

.header-menu :deep(.ant-menu-item:hover) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.06)) !important;
  border-color: rgba(255, 255, 255, 0.18);
  color: #ffffff !important;
  transform: translateY(-1px);
}

.header-menu :deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.12)) !important;
  border-color: rgba(255, 255, 255, 0.26);
  color: #ffffff !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    inset 0 -1px 0 rgba(255, 255, 255, 0.08),
    0 10px 24px rgba(7, 35, 88, 0.18);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.header-menu :deep(.ant-menu-item .ant-menu-title-content) {
  display: inline-flex;
  align-items: center;
}

.login-button {
  flex-shrink: 0;
  height: 42px;
  padding-inline: 18px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.78));
  color: #1677ff;
  font-weight: 600;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.56),
    0 10px 24px rgba(10, 56, 132, 0.18);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    background-color 0.25s ease,
    border-color 0.25s ease;
}

.login-button:hover,
.login-button:focus {
  color: #0958d9 !important;
  border-color: rgba(255, 255, 255, 0.42) !important;
  background: linear-gradient(135deg, #ffffff, rgba(255, 255, 255, 0.88)) !important;
  transform: translateY(-1px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 14px 28px rgba(10, 56, 132, 0.24);
}

@media (max-width: 992px) {
  .global-header {
    flex-wrap: wrap;
    gap: 14px;
    padding: 14px 16px;
  }

  .header-main {
    order: 3;
    flex: 1 0 100%;
    width: 100%;
  }

  .header-menu {
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .header-menu :deep(.ant-menu-overflow) {
    justify-content: flex-start;
    flex-wrap: nowrap;
  }
}

@media (max-width: 576px) {
  .global-header {
    min-height: auto;
    gap: 12px;
    padding: 12px;
    border-radius: 20px;
  }

  .global-header::before {
    border-radius: 19px;
  }

  .header-brand,
  .header-actions {
    width: calc(50% - 6px);
  }

  .header-actions {
    justify-content: flex-end;
  }

  .brand {
    padding: 8px 10px;
  }

  .brand-title {
    font-size: 18px;
  }

  .brand-logo {
    width: 36px;
    height: 36px;
  }

  .login-button {
    height: 38px;
    padding-inline: 16px;
  }

  .header-menu :deep(.ant-menu-item) {
    padding-inline: 14px;
    font-size: 14px;
  }
}
</style>
