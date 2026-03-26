<template>
  <a-modal
    v-model:open="visible"
    :footer="null"
    :title="null"
    :width="1200"
    wrapClassName="app-preview-modal-wrap"
    :body-style="{ padding: '0' }"
    :mask-style="{
      background: 'rgba(244, 246, 255, 0.42)',
      backdropFilter: 'blur(14px) saturate(130%)',
      WebkitBackdropFilter: 'blur(14px) saturate(130%)',
    }"
  >
    <div class="preview-modal-shell">
      <div class="preview-modal-header">
        <div class="preview-meta">
          <h2>{{ app?.appName || '未命名应用' }}</h2>
          <div class="preview-meta-row">
            <span class="preview-time">{{ formatDateTime(app?.createTime) }}</span>
            <span class="preview-divider">·</span>
            <UserInfo :user="app?.user" size="small" />
          </div>
        </div>
        <a-button v-if="deployUrl" class="deploy-button" @click="openDeployPage">
          查看部署
        </a-button>
      </div>

      <div class="preview-frame-container">
        <iframe
          v-if="previewUrl"
          :src="previewUrl"
          class="preview-frame"
          title="应用作品预览"
          style="min-height: 960px"
        />
        <div v-else class="preview-empty">
          当前应用还没有可预览的作品内容
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import UserInfo from '@/components/UserInfo.vue'
import { formatDateTime, getAppDeployPreviewUrl } from '@/utils/app.ts'

interface Props {
  open: boolean
  app?: API.AppVO
  previewUrl?: string
}

interface Emits {
  (e: 'update:open', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const deployUrl = computed(() => {
  return getAppDeployPreviewUrl(props.app?.deployKey)
})

const openDeployPage = () => {
  if (!deployUrl.value) {
    return
  }

  // 预览弹层里只负责打开部署站点，避免与静态资源预览地址混淆。
  window.open(deployUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.preview-modal-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(147, 197, 253, 0.2), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(248, 250, 252, 0.6));
  padding: 20px;
  border-radius: 28px;
  min-height: calc(100vh - 40px);
  max-height: calc(100vh - 40px);
}

.preview-modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.preview-meta {
  min-width: 0;
  flex: 1;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.28);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px) saturate(132%);
  -webkit-backdrop-filter: blur(16px) saturate(132%);
}

.preview-meta h2 {
  margin: 0 0 6px;
  color: #111827;
  font-size: clamp(22px, 2vw, 30px);
  font-weight: 700;
  line-height: 1.1;
}

.preview-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: rgba(17, 24, 39, 0.68);
  font-size: 14px;
}

.preview-time,
.preview-divider {
  flex-shrink: 0;
}

.deploy-button {
  height: 42px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.38);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(16px) saturate(132%);
  -webkit-backdrop-filter: blur(16px) saturate(132%);
  color: #111827;
  font-weight: 600;
}

.deploy-button:hover,
.deploy-button:focus {
  color: #111827;
  background: rgba(255, 255, 255, 0.56);
}

.preview-frame-container {
  position: relative;
  flex: 1;
  min-height: 0;
  height: auto;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.88);
  border-radius: 24px;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.06), transparent 24%),
    rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 64px rgba(15, 23, 42, 0.12);
}

.preview-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}

.preview-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: rgba(255, 255, 255, 0.52);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  color: rgba(17, 24, 39, 0.58);
  font-size: 16px;
}

:deep(.app-preview-modal-wrap .ant-modal) {
  width: min(96vw, 1680px) !important;
  max-width: 1680px;
  margin: 20px auto;
  padding-bottom: 0;
}

:deep(.app-preview-modal-wrap .ant-modal-content) {
  overflow: hidden;
  border-radius: 32px;
  background: transparent;
  box-shadow: none;
  max-height: calc(100vh - 40px);
}

:deep(.app-preview-modal-wrap .ant-modal-body) {
  max-height: calc(100vh - 40px);
}

:deep(.app-preview-modal-wrap .ant-modal-close) {
  top: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  background: rgba(255, 255, 255, 0.38);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(16px) saturate(132%);
  -webkit-backdrop-filter: blur(16px) saturate(132%);
}

@media (max-width: 768px) {
  .preview-modal-shell {
    gap: 12px;
    padding: 12px;
    min-height: calc(100vh - 16px);
    max-height: calc(100vh - 16px);
  }

  .preview-modal-header {
    flex-direction: column;
    align-items: stretch;
  }

  .preview-meta {
    padding: 12px 14px;
  }

  .preview-meta h2 {
    font-size: 22px;
  }

  .preview-meta-row {
    font-size: 13px;
    gap: 6px;
  }

  .deploy-button {
    align-self: flex-start;
    height: 38px;
    padding: 0 16px;
  }

  .preview-frame-container {
    min-height: 0;
    height: auto;
  }

  .preview-frame,
  .preview-empty {
    height: 100%;
  }

  :deep(.app-preview-modal-wrap .ant-modal) {
    width: calc(100vw - 16px) !important;
    margin: 8px auto;
  }

  :deep(.app-preview-modal-wrap .ant-modal-content),
  :deep(.app-preview-modal-wrap .ant-modal-body) {
    max-height: calc(100vh - 16px);
  }
}
</style>

