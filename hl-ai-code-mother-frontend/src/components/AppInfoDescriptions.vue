<template>
  <a-descriptions :column="column" :bordered="bordered">
    <a-descriptions-item v-if="showId" label="应用ID">
      {{ app?.id ?? '-' }}
    </a-descriptions-item>
    <a-descriptions-item label="创建者">
      <UserInfo :user="app?.user" size="small" />
    </a-descriptions-item>
    <a-descriptions-item label="创建时间">
      {{ formatTime(app?.createTime) }}
    </a-descriptions-item>
    <a-descriptions-item label="生成类型">
      <a-tag v-if="app?.codeGenType" color="blue">
        {{ formatCodeGenType(app.codeGenType) }}
      </a-tag>
      <span v-else>未知类型</span>
    </a-descriptions-item>
    <a-descriptions-item v-if="showUpdateTime" label="更新时间">
      {{ formatTime(app?.updateTime) }}
    </a-descriptions-item>
    <a-descriptions-item v-if="showDeployedTime" label="部署时间">
      {{ app?.deployedTime ? formatTime(app.deployedTime) : '未部署' }}
    </a-descriptions-item>
    <a-descriptions-item v-if="showPreviewAction" label="访问链接">
      <a-button v-if="canPreview" type="link" size="small" @click="emit('preview')">查看预览</a-button>
      <span v-else>未生成</span>
    </a-descriptions-item>
  </a-descriptions>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import UserInfo from '@/components/UserInfo.vue'
import { formatCodeGenType } from '@/utils/codeGenTypes'
import { formatTime } from '@/utils/time'

interface Props {
  app?: API.AppVO
  column?: number
  bordered?: boolean
  showId?: boolean
  showUpdateTime?: boolean
  showDeployedTime?: boolean
  showPreviewAction?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  column: 2,
  bordered: false,
  showId: false,
  showUpdateTime: false,
  showDeployedTime: false,
  showPreviewAction: false,
})

const emit = defineEmits<{
  (e: 'preview'): void
}>()

const canPreview = computed(() => {
  return !!props.app?.id && !!props.app?.codeGenType
})
</script>


