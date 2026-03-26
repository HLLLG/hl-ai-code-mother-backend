<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署按钮
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="logo" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                placeholder="请描述你想生成的网站，越详细效果越好哦"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              placeholder="请描述你想生成的网站，越详细效果越好哦"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe v-else :src="previewUrl" class="preview-iframe"></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getAppVoById,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
} from '@/api/appController'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import logo from '@/assets/logo.png'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'

import {
  CloudUploadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import { userLoginStore } from '@/stores/loginUser.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = userLoginStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()
const hasInitialConversation = ref(false) // 标记是否已经进行过初始对话

// 预览相关
const previewUrl = ref('')

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const isViewMode = computed(() => route.query.view === '1')

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 自动发送初始提示词（除非是查看模式或已经进行过初始对话）
      if (appInfo.value.initPrompt && !isViewMode.value && !hasInitialConversation.value) {
        hasInitialConversation.value = true
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  const message = userInput.value.trim()
  userInput.value = ''

  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    previewUrl.value = getStaticPreviewUrl(codeGenType, appId.value, appInfo.value?.currentVersion || '1')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
})
</script>

<style scoped>
#appChatPage {
  display: flex;
  flex: 1;
  min-height: 100%;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
  color: var(--app-text-body);
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  border: 1px solid var(--app-card-border);
  border-radius: var(--app-radius-xl);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.76), rgba(255, 255, 255, 0.56));
  box-shadow: var(--app-card-shadow);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.app-name {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--app-text-title);
  letter-spacing: 0.01em;
}

.header-right {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(380px, 1.05fr) minmax(480px, 1.35fr);
  gap: 18px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--app-card-border);
  border-radius: var(--app-radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.64));
  box-shadow: var(--app-card-shadow);
  overflow: hidden;
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
}

.chat-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), transparent 28%);
  pointer-events: none;
}

.messages-container {
  flex: 1;
  min-height: 0;
  padding: 22px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 16px;
}

.user-message,
.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-content {
  max-width: min(78%, 760px);
  padding: 14px 16px;
  border-radius: 20px;
  line-height: 1.7;
  word-wrap: break-word;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.user-message .message-content {
  background: linear-gradient(135deg, var(--app-primary), #4f8cff);
  color: #ffffff;
  border-bottom-right-radius: 8px;
}

.ai-message .message-content {
  background: rgba(255, 255, 255, 0.88);
  color: var(--app-text-body);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-bottom-left-radius: 8px;
}

.message-avatar {
  flex-shrink: 0;
  padding-top: 4px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--app-text-muted);
}

/* 输入区域 */
.input-container {
  position: relative;
  z-index: 1;
  padding: 18px 22px 22px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(248, 250, 252, 0.92));
}

.input-wrapper {
  position: relative;
}

.input-wrapper :deep(.ant-input),
.input-wrapper :deep(.ant-input-affix-wrapper),
.input-wrapper :deep(textarea.ant-input) {
  border-radius: 20px;
  border: 1px solid rgba(203, 213, 225, 0.88);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.68);
}

.input-wrapper :deep(textarea.ant-input) {
  padding: 18px 72px 18px 18px;
  color: var(--app-text-body);
}

.input-wrapper :deep(.ant-input:focus),
.input-wrapper :deep(.ant-input-focused) {
  border-color: rgba(37, 99, 235, 0.5);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.input-actions {
  position: absolute;
  right: 12px;
  bottom: 12px;
}

.input-actions :deep(.ant-btn) {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.28);
}

/* 右侧预览区域 */
.preview-section {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--app-card-border);
  border-radius: var(--app-radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.58));
  box-shadow: var(--app-card-shadow);
}

.preview-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), transparent 28%);
  pointer-events: none;
}

.preview-header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.preview-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text-title);
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(59, 130, 246, 0.06), transparent 24%),
    rgba(248, 250, 252, 0.72);
}

.preview-placeholder,
.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--app-text-muted);
}

.preview-placeholder p,
.preview-loading p {
  font-size: 15px;
}

.placeholder-icon {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  margin-bottom: 18px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(14, 165, 233, 0.14));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
  font-size: 38px;
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}

.header-right :deep(.ant-btn),
.preview-actions :deep(.ant-btn) {
  border-radius: 999px;
  font-weight: 600;
}

.header-right :deep(.ant-btn-default) {
  border-color: rgba(203, 213, 225, 0.88);
  background: rgba(255, 255, 255, 0.74);
}

.header-right :deep(.ant-btn-primary) {
  border: none;
  background: linear-gradient(135deg, var(--app-primary), var(--app-primary-strong));
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.26);
}

@media (max-width: 1280px) {
  .main-content {
    grid-template-columns: minmax(340px, 0.95fr) minmax(420px, 1.15fr);
  }
}

@media (max-width: 1024px) {
  #appChatPage {
    padding: 4px;
  }

  .main-content {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .chat-section,
  .preview-section {
    min-height: 480px;
  }
}

@media (max-width: 768px) {
  #appChatPage {
    gap: 12px;
    padding: 0;
  }

  .header-bar {
    padding: 16px;
    border-radius: 22px;
  }

  .app-name {
    font-size: 22px;
  }

  .header-right {
    width: 100%;
  }

  .main-content {
    gap: 12px;
  }

  .messages-container,
  .preview-header,
  .input-container {
    padding-left: 16px;
    padding-right: 16px;
  }

  .messages-container {
    padding-top: 16px;
    padding-bottom: 16px;
  }

  .message-content {
    max-width: 88%;
  }
}
</style>
