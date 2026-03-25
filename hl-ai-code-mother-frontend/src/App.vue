<template>
  <a-config-provider :locale="locale">
    <BasicLayout />
  </a-config-provider>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { userLoginStore } from '@/stores/loginUser.ts'

const loginUserStore = userLoginStore()
loginUserStore.fetchLoginUser()

const locale = zhCN
</script>
