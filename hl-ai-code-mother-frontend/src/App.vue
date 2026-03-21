<template>
  <a-config-provider :locale="locale">
    <BasicLayout />
  </a-config-provider>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { userLoginStore } from '@/stores/loginUser.ts'

const loginUserStore = userLoginStore()
loginUserStore.fetchLoginUser()

const locale = dayjs.locale('zh-cn')
</script>
