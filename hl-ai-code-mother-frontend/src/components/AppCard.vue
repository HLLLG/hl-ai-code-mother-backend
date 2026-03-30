<template>
  <a-card class="app-card social-card" :bordered="false" hoverable :body-style="{ padding: '16px 18px 18px' }">
    <AppCover :src="app.cover" :title="coverTitle" :featured="featured" zoom-on-hover>
      <div class="cover-overlay">
        <div class="cover-actions">
          <a-button
            v-if="showAcceptInvite"
            type="primary"
            size="large"
            class="preview-button preview-button-primary"
            @click.stop="emit('accept', app)"
          >
            {{ acceptButtonText }}
          </a-button>
          <a-button
            v-if="canViewConversation"
            type="primary"
            size="large"
            class="preview-button preview-button-primary"
            @click.stop="emit('conversation', app)"
          >
            {{ conversationButtonText }}
          </a-button>
          <a-button
            size="large"
            class="preview-button preview-button-secondary"
            :disabled="!canPreview"
            @click.stop="emit('preview', app)"
          >
            {{ previewButtonText }}
          </a-button>
          <a-button
            v-if="canViewMembers"
            size="large"
            class="preview-button preview-button-secondary"
            @click.stop="emit('members', app)"
          >
            {{ memberButtonText }}
          </a-button>
        </div>
      </div>
    </AppCover>
    <div class="app-card-body social-card-body">
      <a-avatar class="author-avatar" :src="app.user?.userAvatar || userAvatar">
        {{ authorInitialText }}
      </a-avatar>
      <div class="author-meta">
        <div class="author-title-row">
          <h3>{{ app.appName || '未命名应用' }}</h3>
          <a-tag v-if="featured" color="magenta">{{ featuredTagText }}</a-tag>
        </div>
        <div class="author-subtitle">
          <span>{{ authorNameText }}</span>
          <span>{{ formatDateTime(app.createTime) }}</span>
        </div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import AppCover from '@/components/AppCover.vue'
import { formatDateTime } from '@/utils/app'

interface Props {
  app: API.AppVO
  featured?: boolean
  featuredTagText?: string
  showAcceptInvite?: boolean
  canViewConversation?: boolean
  canPreview?: boolean
  canViewMembers?: boolean
  authorName?: string
  authorInitial?: string
  userAvatar?: string
  coverTitle?: string
  acceptButtonText?: string
  conversationButtonText?: string
  previewButtonText?: string
  memberButtonText?: string
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
  featuredTagText: '精选',
  showAcceptInvite: false,
  canViewConversation: false,
  canPreview: false,
  canViewMembers: false,
  authorName: '',
  authorInitial: '',
  userAvatar: '',
  coverTitle: '',
  acceptButtonText: '接受邀请',
  conversationButtonText: '查看对话',
  previewButtonText: '查看作品',
  memberButtonText: '查看成员',
})

const emit = defineEmits<{
  (e: 'accept', app: API.AppVO): void
  (e: 'conversation', app: API.AppVO): void
  (e: 'preview', app: API.AppVO): void
  (e: 'members', app: API.AppVO): void
}>()

const authorNameText = computed(() => props.authorName || props.app.user?.userName || '未命名用户')
const authorInitialText = computed(() => {
  return (props.authorInitial || authorNameText.value || '我').slice(0, 1)
})
</script>

<style scoped>
.app-card {
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.32);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.cover-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-card:hover .cover-overlay {
  opacity: 1;
}

.preview-button {
  min-width: 132px;
  height: 44px;
  border: none;
  border-radius: 999px;
  font-weight: 600;
}

.preview-button-primary {
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.15);
}

.preview-button-secondary {
  background: rgba(17, 24, 39, 0.78);
  color: #ffffff;
}

.preview-button-primary:hover,
.preview-button-primary:focus {
  background: #ffffff;
  color: #111827;
}

.preview-button-secondary:hover,
.preview-button-secondary:focus {
  background: rgba(17, 24, 39, 0.92);
  color: #ffffff;
}

.preview-button-secondary[disabled] {
  background: rgba(255, 255, 255, 0.56);
  color: rgba(17, 24, 39, 0.42);
  box-shadow: none;
}

.app-card-body.social-card-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 0;
}

.author-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #22c55e, #06b6d4);
}

.author-meta {
  min-width: 0;
  flex: 1;
}

.author-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.author-title-row h3 {
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-subtitle {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: rgba(17, 24, 39, 0.52);
  font-size: 13px;
}

@media (max-width: 768px) {
  .cover-overlay {
    opacity: 1;
    align-items: flex-end;
    padding-bottom: 18px;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0.38));
  }

  .app-card-body.social-card-body,
  .author-title-row {
    align-items: flex-start;
  }

  .author-title-row {
    flex-direction: column;
  }

  .author-title-row h3 {
    white-space: normal;
  }
}
</style>


