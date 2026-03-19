<template>
  <a-config-provider :locale="locale">
    <BasicLayout />
  </a-config-provider>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { healthCheck } from '@/api/healthController.ts'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

const locale = dayjs.locale('zh-cn')

healthCheck().then((res) => {
  console.log(res)
})
</script>
