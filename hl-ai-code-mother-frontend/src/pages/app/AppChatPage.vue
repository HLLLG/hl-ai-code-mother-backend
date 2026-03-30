<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedVersion"
          class="version-select"
          placeholder="版本选择"
          :options="versionOptions"
          :disabled="versionOptions.length === 0 || switchingVersion || !canEditChat"
          :loading="switchingVersion"
          @change="handleVersionChange"
        />
        <a-button
          v-if="canEditChat"
          danger
          @click="leaveChatSession"
          :disabled="chatSocketConnecting || isGenerating"
        >
          退出对话
        </a-button>
        <a-button
          v-else
          type="primary"
          @click="enterChatSession"
          :disabled="chatSocketConnecting || !!chatState?.occupied"
        >
          {{ chatState?.occupied ? '当前有人协作中' : '进入对话' }}
        </a-button>
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button v-if="canViewMembers" type="default" @click="openMembersPage">
          成员管理
        </a-button>
        <a-button type="default" @click="exportChatHistory" :loading="exportingChatHistory">
          <template #icon>
            <DownloadOutlined />
          </template>
          下载历史对话
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying" :disabled="!isOwner || !canEditChat">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <div class="chat-status-banner" :class="{ readonly: !canEditChat, editing: canEditChat }">
          <span>{{ chatStatusText }}</span>
        </div>
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <div v-if="hasMoreHistory && messages.length > 0" class="load-more-wrapper">
            <a-button type="link" :loading="loadingMoreHistory" @click="loadMoreHistory">
              加载更多
            </a-button>
          </div>
          <div v-else-if="loadingHistory && messages.length === 0" class="history-loading">
            <a-spin size="small" />
            <span>正在加载历史消息...</span>
          </div>
          <div v-for="(message, index) in messages" :key="message.id ?? index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="message.userAvatar || loginUserStore.loginUser.userAvatar" />
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
          <div v-if="!loadingHistory && messages.length === 0" class="empty-history">
            暂无对话消息，快来开始创作吧
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!canEditChat" :title="readonlyTooltipText" placement="top">
              <a-textarea
                v-model:value="userInput"
                placeholder="请描述你想生成的网站，越详细效果越好哦"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !canEditChat"
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
                v-if="isGenerating"
                danger
                @click="stopGenerating"
                :loading="isStopping"
                :disabled="!canEditChat"
              >
                <template #icon>
                  <PauseCircleOutlined />
                </template>
              </a-button>
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!canEditChat || isGenerating"
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
          <div v-else-if="isGenerating && !previewUrl" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe v-else-if="previewUrl" :src="previewUrl" class="preview-iframe"></iframe>
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
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { message } from 'ant-design-vue'
import {
  getAppVoById,
  getAppChatState,
  deployApp as deployAppApi,
  deleteApp as deleteAppApi,
  getAppVersionCount,
  stopChatToGenCode,
  updateAppVersion,
} from '@/api/appController'
import { downloadChatHistoryMd, listChatHistory } from '@/api/chatHistoryController'
import { CodeGenTypeEnum } from '@/utils/codeGenTypes'
import request from '@/request'

import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import logo from '@/assets/logo.png'
import { API_BASE_URL, WS_BASE_URL, getStaticPreviewUrl } from '@/config/env'

import {
  CloudUploadOutlined,
  DownloadOutlined,
  SendOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  PauseCircleOutlined,
} from '@ant-design/icons-vue'
import { userLoginStore } from '@/stores/loginUser.ts'
import { canAccessAppMembers } from '@/utils/appMembers.ts'

const route = useRoute()
const router = useRouter()
const loginUserStore = userLoginStore()
const CHAT_HISTORY_PAGE_SIZE = 10

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<string>()
const versionCount = ref(0)
const selectedVersion = ref<number>()
const switchingVersion = ref(false)
const pendingVersion = ref<number>()

// 对话相关
interface Message {
  id?: number
  type: 'user' | 'ai'
  content: string
  createTime?: string
  loading?: boolean
  userName?: string
  userAvatar?: string
  memberRole?: string
}

interface AppChatSocketEvent {
  eventType?: string
  appId?: number
  data?: any
}

interface AppChatConnectionPayload {
  accessMode?: 'editor' | 'viewer'
  state?: API.AppChatStateVO
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const isStopping = ref(false)
const messagesContainer = ref<HTMLElement>()
const hasInitialConversation = ref(false)
const activeEventSource = ref<EventSource | null>(null)
const activeAiMessageIndex = ref<number | null>(null)
const isManualStop = ref(false)
const chatSocket = ref<WebSocket | null>(null)
const chatSocketConnecting = ref(false)
const chatAccessMode = ref<'editor' | 'viewer'>('viewer')
const chatState = ref<API.AppChatStateVO>()
const loadingHistory = ref(false)
const loadingMoreHistory = ref(false)
const hasMoreHistory = ref(false)
const historyMessageCount = ref(0)
const oldestHistoryCursor = ref<string>()

// 预览相关
const previewUrl = ref('')

// 部署相关
const deploying = ref(false)
const exportingChatHistory = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const canViewMembers = computed(() => {
  return canAccessAppMembers(appInfo.value, loginUserStore.loginUser.id)
})

const canEditChat = computed(() => chatAccessMode.value === 'editor')

const readonlyTooltipText = computed(() => {
  if (chatState.value?.occupied) {
    return `${chatState.value.occupyUserName || '其他成员'} 正在协作中，你当前只能查看对话`
  }
  return '当前未进入对话，请先点击进入对话'
})

const chatStatusText = computed(() => {
  if (canEditChat.value) {
    return '你已进入对话，可发送消息、停止生成和切换版本'
  }
  if (chatState.value?.occupied) {
    return `${chatState.value.occupyUserName || '其他成员'} 正在协作中，你当前处于只读围观模式`
  }
  return '当前无人占用对话，你可以点击进入对话开始协作'
})

// 应用详情相关
const appDetailVisible = ref(false)

const versionOptions = computed(() => {
  return Array.from({ length: versionCount.value }, (_, index) => {
    const version = index + 1
    return {
      label: `版本${version}`,
      value: version,
    }
  })
})

const getApiAppId = () => {
  return appId.value as unknown as number
}

const normalizeCursor = (record?: API.ChatHistory) => {
  return record?.createTime || record?.updateTime
}

const mapChatHistoryToMessage = (record: API.ChatHistory): Message => {
  const normalizedType = record.messageType?.toLowerCase()
  return {
    id: record.id,
    type: normalizedType === 'user' ? 'user' : 'ai',
    content: record.message || '',
    createTime: record.createTime,
    userName: normalizedType === 'user' ? record.userName : 'AI 助手',
    userAvatar:
      normalizedType === 'user' ? record.userAvatar || loginUserStore.loginUser.userAvatar : logo,
    memberRole: record.memberRole,
  }
}

const shouldShowPreview = () => {
  const hasHistoryPreview = historyMessageCount.value >= 2
  const hasGeneratedPreview = !!appInfo.value?.codeGenType && !!appInfo.value?.currentVersion
  return !!appId.value && (hasHistoryPreview || hasGeneratedPreview)
}

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

const syncVersionState = () => {
  const currentVersion = Number(appInfo.value?.currentVersion) || 1
  versionCount.value = Math.max(versionCount.value, currentVersion)
  selectedVersion.value = pendingVersion.value ?? currentVersion
}

const fetchVersionCount = async () => {
  if (!appId.value) {
    return
  }

  try {
    const res = await getAppVersionCount({
      appId: getApiAppId(),
    })
    if (res.data.code === 0) {
      versionCount.value = Math.max(res.data.data ?? 0, 1)
      syncVersionState()
    }
  } catch (error) {
    console.error('获取版本数量失败：', error)
  }
}

const fetchChatHistory = async (cursor?: string) => {
  if (!appId.value) {
    return [] as API.ChatHistory[]
  }

  const res = await listChatHistory({
    appId: getApiAppId(),
    pageSize: CHAT_HISTORY_PAGE_SIZE,
    ...(cursor ? { lastCreatTime: cursor } : {}),
  })

  if (res.data.code !== 0) {
    throw new Error(res.data.message || '获取对话历史失败')
  }

  return res.data.data?.records ?? []
}

const updateHistoryPagination = (records: API.ChatHistory[]) => {
  hasMoreHistory.value = records.length >= CHAT_HISTORY_PAGE_SIZE
  oldestHistoryCursor.value = normalizeCursor(records[0])
}

const loadInitialHistory = async () => {
  loadingHistory.value = true
  try {
    const records = await fetchChatHistory()
    const sortedRecords = [...records].sort((a, b) => {
      return new Date(a.createTime || '').getTime() - new Date(b.createTime || '').getTime()
    })
    messages.value = sortedRecords.map(mapChatHistoryToMessage)
    historyMessageCount.value = records.length
    updateHistoryPagination(records)
    hasInitialConversation.value = records.length > 0
  } catch (error) {
    console.error('加载历史消息失败：', error)
    message.error('加载历史消息失败，请刷新重试')
  } finally {
    loadingHistory.value = false
    await nextTick()
    scrollToBottom()
  }
}

const loadMoreHistory = async () => {
  if (!hasMoreHistory.value || loadingMoreHistory.value || !oldestHistoryCursor.value) {
    return
  }

  loadingMoreHistory.value = true
  try {
    const records = await fetchChatHistory(oldestHistoryCursor.value)
    const sortedRecords = [...records].sort((a, b) => {
      return new Date(a.createTime || '').getTime() - new Date(b.createTime || '').getTime()
    })
    const historyMessages = sortedRecords.map(mapChatHistoryToMessage)
    const existingIds = new Set(messages.value.map((item) => item.id).filter(Boolean))
    const dedupedHistory = historyMessages.filter((item) => !item.id || !existingIds.has(item.id))
    messages.value = [...dedupedHistory, ...messages.value]
    historyMessageCount.value += dedupedHistory.length
    updateHistoryPagination(records)
  } catch (error) {
    console.error('加载更多历史消息失败：', error)
    message.error('加载更多失败，请稍后重试')
  } finally {
    loadingMoreHistory.value = false
  }
}

const maybeSendInitialMessage = async () => {
  if (!canEditChat.value || hasInitialConversation.value || !appInfo.value?.initPrompt) {
    return
  }

  hasInitialConversation.value = true
  await sendInitialMessage(appInfo.value.initPrompt)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return false
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      syncVersionState()
      return true
    }
    message.error('获取应用信息失败')
    router.push('/')
    return false
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
    return false
  }
}

const fetchChatState = async () => {
  if (!appId.value) {
    return
  }
  try {
    const res = await getAppChatState({
      appId: getApiAppId(),
    })
    if (res.data.code === 0) {
      chatState.value = res.data.data
    }
  } catch (error) {
    console.error('获取协作状态失败：', error)
  }
}

const syncChatConnection = (payload?: AppChatConnectionPayload) => {
  if (!payload) {
    return
  }
  chatAccessMode.value = payload.accessMode === 'editor' ? 'editor' : 'viewer'
  chatState.value = payload.state
}

const appendIncomingMessage = (record: API.ChatHistory) => {
  const nextMessage = mapChatHistoryToMessage(record)
  const existingIndex = messages.value.findIndex((item) => item.id && item.id === nextMessage.id)
  if (existingIndex >= 0) {
    messages.value[existingIndex] = {
      ...messages.value[existingIndex],
      ...nextMessage,
      loading: false,
    }
    return
  }
  if (canEditChat.value && record.userId === loginUserStore.loginUser.id) {
    if (nextMessage.type === 'user') {
      return
    }
    const loadingIndex =
      activeAiMessageIndex.value ??
      messages.value.findIndex((item) => item.type === 'ai' && item.loading)
    if (loadingIndex >= 0 && messages.value[loadingIndex]) {
      messages.value[loadingIndex] = {
        ...messages.value[loadingIndex],
        ...nextMessage,
        loading: false,
      }
      return
    }
  }
  messages.value.push(nextMessage)
  historyMessageCount.value += 1
  nextTick(scrollToBottom)
}

const handleSocketEvent = async (event: MessageEvent<string>) => {
  try {
    const payload = JSON.parse(event.data) as AppChatSocketEvent
    switch (payload.eventType) {
      case 'state_snapshot':
        syncChatConnection(payload.data as AppChatConnectionPayload)
        break
      case 'lock_acquired':
        chatState.value = payload.data as API.AppChatStateVO
        if (chatState.value?.occupyUserId !== loginUserStore.loginUser.id) {
          chatAccessMode.value = 'viewer'
        }
        break
      case 'lock_released':
        chatState.value = payload.data as API.AppChatStateVO
        if (chatAccessMode.value !== 'editor') {
          chatAccessMode.value = 'viewer'
        }
        break
      case 'message_added':
        appendIncomingMessage(payload.data as API.ChatHistory)
        break
      case 'generation_started':
        if (!canEditChat.value) {
          isGenerating.value = true
        }
        break
      case 'generation_finished':
      case 'generation_stopped':
        if (!canEditChat.value) {
          isGenerating.value = false
        }
        break
      case 'version_switched': {
        const nextVersion = Number(payload.data?.version)
        if (nextVersion > 0) {
          selectedVersion.value = nextVersion
          if (appInfo.value) {
            appInfo.value = {
              ...appInfo.value,
              currentVersion: nextVersion,
            }
          }
          updatePreview()
        }
        break
      }
      case 'error':
        if (payload.data?.message) {
          message.warning(payload.data.message)
        }
        break
      default:
        break
    }
  } catch (error) {
    console.error('处理协作消息失败：', error)
  }
}

const buildChatSocketUrl = () => {
  return `${WS_BASE_URL}/ws/app/${getApiAppId()}/chat`
}

const connectChatSocket = async () => {
  if (!appId.value) {
    return
  }
  if (chatSocket.value) {
    chatSocket.value.close()
    chatSocket.value = null
  }
  chatSocketConnecting.value = true
  await new Promise<void>((resolve) => {
    let settled = false
    const socket = new WebSocket(buildChatSocketUrl())
    chatSocket.value = socket
    socket.onopen = () => undefined
    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as AppChatSocketEvent
        if (payload.eventType === 'state_snapshot' && !settled) {
          settled = true
          resolve()
        }
      } catch (error) {
        console.error('解析协作初始化消息失败：', error)
      }
      void handleSocketEvent(event)
    }
    socket.onerror = (error) => {
      console.error('协作 WebSocket 连接失败：', error)
      if (!settled) {
        settled = true
        resolve()
      }
    }
    socket.onclose = () => {
      if (chatSocket.value === socket) {
        chatSocket.value = null
        chatSocketConnecting.value = false
        if (chatAccessMode.value === 'editor') {
          chatAccessMode.value = 'viewer'
        }
      }
      if (!settled) {
        settled = true
        resolve()
      }
    }
  })
  chatSocketConnecting.value = false
}

const sendChatSocketAction = (action: 'enter' | 'leave') => {
  if (!chatSocket.value || chatSocket.value.readyState !== WebSocket.OPEN) {
    return false
  }
  chatSocket.value.send(JSON.stringify({ action }))
  return true
}

const enterChatSession = () => {
  if (!sendChatSocketAction('enter')) {
    message.warning('协作连接尚未建立，请稍后重试')
  }
}

const leaveChatSession = (notify = true) => {
  const sent = sendChatSocketAction('leave')
  if (!sent && notify) {
    message.warning('当前协作连接不可用')
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
  activeAiMessageIndex.value = aiMessageIndex
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value || !canEditChat.value) {
    return
  }

  const currentMessage = userInput.value.trim()
  userInput.value = ''

  messages.value.push({
    type: 'user',
    content: currentMessage,
  })

  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  isGenerating.value = true
  activeAiMessageIndex.value = aiMessageIndex
  await generateCode(currentMessage, aiMessageIndex)
}

const cleanupActiveGeneration = () => {
  activeEventSource.value?.close()
  activeEventSource.value = null
  activeAiMessageIndex.value = null
  isGenerating.value = false
  isStopping.value = false
}

const markAiMessageStopped = () => {
  const index = activeAiMessageIndex.value
  if (index === null || !messages.value[index]) {
    return
  }
  const currentContent = messages.value[index].content?.trim()
  messages.value[index].loading = false
  if (!currentContent) {
    messages.value[index].content = '本次生成已停止。'
  }
}

const stopGenerating = async (shouldNotify = true) => {
  if (!isGenerating.value || isStopping.value || !appId.value || !canEditChat.value) {
    return false
  }

  isStopping.value = true
  isManualStop.value = true
  try {
    const res = await stopChatToGenCode({
      appId: getApiAppId(),
    })
    if (res.data.code !== 0) {
      console.error('停止生成失败：', res.data.message)
      isManualStop.value = false
      isStopping.value = false
      if (shouldNotify) {
        message.error('停止生成失败：' + res.data.message)
      }
      return false
    }
    markAiMessageStopped()
    cleanupActiveGeneration()
    if (shouldNotify) {
      message.success('已停止生成')
    }
    return true
  } catch (error) {
    console.error('停止生成失败：', error)
    isManualStop.value = false
    isStopping.value = false
    if (shouldNotify) {
      message.error('停止生成失败，请重试')
    }
    return false
  }
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let streamCompleted = false

  const finalizeStream = (options?: { refreshApp?: boolean }) => {
    activeEventSource.value?.close()
    activeEventSource.value = null
    activeAiMessageIndex.value = null
    isGenerating.value = false
    isStopping.value = false
    const shouldRefreshApp = options?.refreshApp ?? false
    const wasManualStop = isManualStop.value
    isManualStop.value = false

    if (shouldRefreshApp && !wasManualStop) {
      setTimeout(async () => {
        const fetched = await fetchAppInfo()
        if (fetched) {
          await fetchVersionCount()
          await loadInitialHistory()
          updatePreview()
        }
      }, 1000)
    }
  }

  try {
    activeEventSource.value?.close()

    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: String(appId.value || ''),
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    const eventSource = new EventSource(url, {
      withCredentials: true,
    })
    activeEventSource.value = eventSource

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted || activeEventSource.value !== eventSource) return

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
      if (streamCompleted || activeEventSource.value !== eventSource) return

      streamCompleted = true
      messages.value[aiMessageIndex].loading = false
      finalizeStream({ refreshApp: true })
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || activeEventSource.value !== eventSource) return
      if (isManualStop.value) {
        streamCompleted = true
        markAiMessageStopped()
        finalizeStream()
        return
      }
      // 检查是否是正常的连接关闭
      if (eventSource.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        messages.value[aiMessageIndex].loading = false
        finalizeStream({ refreshApp: true })
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
  cleanupActiveGeneration()
  isManualStop.value = false
  message.error('生成失败，请重试')
}

// 更新预览
const updatePreview = () => {
  if (appId.value && shouldShowPreview()) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    previewUrl.value = getStaticPreviewUrl(
      codeGenType,
      String(appId.value),
      String(appInfo.value?.currentVersion || 1),
    )
    return
  }
  previewUrl.value = ''
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const getDefaultChatHistoryFileName = () => {
  const safeAppName = appInfo.value?.appName?.trim()?.replace(/[\\/:*?"<>|]/g, '_')
  return `${safeAppName || `chat-history-${appId.value || 'app'}`}.md`
}

const getFileNameFromContentDisposition = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return ''
  }

  const utf8FileNameMatch = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (utf8FileNameMatch?.[1]) {
    return decodeURIComponent(utf8FileNameMatch[1].trim().replace(/^"|"$/g, ''))
  }

  const normalFileNameMatch = contentDisposition.match(/filename\s*=\s*(?:"([^"]+)"|([^;]+))/i)
  const rawFileName = normalFileNameMatch?.[1] || normalFileNameMatch?.[2]
  return rawFileName?.trim() || ''
}

const getDownloadErrorMessage = async (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const errorData: unknown = error.response?.data
    if (errorData instanceof Blob) {
      const errorText = (await errorData.text()).trim()
      if (!errorText) {
        return '下载历史对话失败，请重试'
      }
      try {
        const parsed = JSON.parse(errorText)
        return parsed.message || parsed.description || '下载历史对话失败，请重试'
      } catch {
        return errorText
      }
    }
    return error.message || '下载历史对话失败，请重试'
  }

  if (error instanceof Error) {
    return error.message
  }

  return '下载历史对话失败，请重试'
}

const exportChatHistory = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  if (exportingChatHistory.value) {
    return
  }

  exportingChatHistory.value = true
  let objectUrl = ''
  let downloadLink: HTMLAnchorElement | null = null

  try {
    const res = await downloadChatHistoryMd({
      appId: getApiAppId(),
    }, {
      responseType: 'blob',
    })

    const rawData: unknown = res.data
    const blob = rawData instanceof Blob ? rawData : new Blob([String(rawData)], { type: 'text/markdown' })
    const contentDisposition = res.headers['content-disposition'] as string | undefined
    const fileName = getFileNameFromContentDisposition(contentDisposition) || getDefaultChatHistoryFileName()

    objectUrl = URL.createObjectURL(blob)
    downloadLink = document.createElement('a')
    downloadLink.href = objectUrl
    downloadLink.download = fileName
    downloadLink.style.display = 'none'
    document.body.appendChild(downloadLink)
    downloadLink.click()
    message.success('历史对话下载成功')
  } catch (error) {
    console.error('下载历史对话失败：', error)
    const errorMessage = await getDownloadErrorMessage(error)
    message.error(`下载历史对话失败：${errorMessage}`)
  } finally {
    downloadLink?.remove()
    if (objectUrl) {
      URL.revokeObjectURL(objectUrl)
    }
    exportingChatHistory.value = false
  }
}

const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  if (!canEditChat.value || !isOwner.value) {
    message.warning('当前只有进入对话的应用 owner 可以部署')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: getApiAppId(),
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

const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

const openMembersPage = () => {
  if (appInfo.value?.id) {
    router.push(`/app/members/${appInfo.value.id}`)
  }
}

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

const handleVersionChange = async (version: number) => {
  if (!appId.value || version === Number(appInfo.value?.currentVersion)) {
    return
  }
  if (!canEditChat.value) {
    message.warning('当前只有进入对话的成员可以切换版本')
    return
  }

  const previousVersion = Number(appInfo.value?.currentVersion) || selectedVersion.value || 1
  pendingVersion.value = version
  switchingVersion.value = true
  try {
    const res = await updateAppVersion({
      appId: getApiAppId(),
      version,
    })

    if (res.data.code === 0) {
      selectedVersion.value = version
      if (appInfo.value) {
        appInfo.value = {
          ...appInfo.value,
          currentVersion: version,
        }
      }
      updatePreview()
      const fetched = await fetchAppInfo()
      if (fetched) {
        pendingVersion.value = undefined
        selectedVersion.value = version
        if (appInfo.value) {
          appInfo.value = {
            ...appInfo.value,
            currentVersion: version,
          }
        }
        updatePreview()
        message.success(`已切换到版本${version}`)
      }
    } else {
      pendingVersion.value = undefined
      selectedVersion.value = previousVersion
      message.error('切换版本失败：' + res.data.message)
    }
  } catch (error) {
    console.error('切换版本失败：', error)
    pendingVersion.value = undefined
    selectedVersion.value = previousVersion
    message.error('切换版本失败，请重试')
  } finally {
    switchingVersion.value = false
  }
}

onMounted(async () => {
  const fetched = await fetchAppInfo()
  if (!fetched) {
    return
  }
  await fetchVersionCount()
  await loadInitialHistory()
  await fetchChatState()
  await connectChatSocket()
  updatePreview()
  await maybeSendInitialMessage()
})

onBeforeRouteLeave(async () => {
  if (isGenerating.value && canEditChat.value) {
    await stopGenerating(false)
  }
  leaveChatSession(false)
  chatSocket.value?.close()
})

onBeforeUnmount(() => {
  activeEventSource.value?.close()
  leaveChatSession(false)
  chatSocket.value?.close()
})
</script>

<style scoped>
#appChatPage {
  display: flex;
  flex: 1;
  min-height: 0;
  height: 100%;
  flex-direction: column;
  gap: 16px;
  padding: 8px;
  color: var(--app-text-body);
  overflow: hidden;
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
  align-items: center;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  min-height: 0;
  height: clamp(620px, calc(100vh - 170px), 820px);
  max-height: clamp(620px, calc(100vh - 170px), 820px);
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
  height: 100%;
  max-height: 100%;
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

.chat-status-banner {
  position: relative;
  z-index: 1;
  padding: 12px 22px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  font-size: 14px;
}

.chat-status-banner.editing {
  color: var(--app-primary);
  background: rgba(24, 144, 255, 0.08);
}

.chat-status-banner.readonly {
  color: var(--app-text-secondary);
  background: rgba(250, 173, 20, 0.1);
}

.messages-container {
  flex: 1;
  min-height: 0;
  max-height: 100%;
  padding: 22px;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
  scroll-behavior: smooth;
}

.load-more-wrapper,
.history-loading,
.empty-history {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--app-text-muted);
}

.empty-history {
  min-height: 100%;
  margin-bottom: 0;
}

.message-item {
  min-width: 0;
  margin-bottom: 16px;
}

.user-message,
.ai-message {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

.message-content {
  max-width: min(78%, 760px);
  max-height: 640px;
  padding: 14px 16px;
  border-radius: 20px;
  line-height: 1.7;
  word-break: break-word;
  overflow-x: auto;
  overflow-y: auto;
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
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-actions :deep(.ant-btn) {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(37, 99, 235, 0.28);
}

.input-actions :deep(.ant-btn-dangerous) {
  background: rgba(255, 255, 255, 0.92);
  color: #ef4444;
  box-shadow: 0 10px 26px rgba(239, 68, 68, 0.16);
}

/* 右侧预览区域 */
.preview-section {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  max-height: 100%;
  border: 1px solid var(--app-card-border);
  border-radius: var(--app-radius-xl);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.58));
  box-shadow: var(--app-card-shadow);
  overflow: hidden;
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
  height: 0;
  position: relative;
  overflow: auto;
  overscroll-behavior: contain;
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
  display: block;
  width: 100%;
  height: 100%;
  min-height: 100%;
  border: none;
  background: #ffffff;
}

.header-right :deep(.ant-btn),
.preview-actions :deep(.ant-btn) {
  border-radius: 999px;
  font-weight: 600;
}

.version-select {
  min-width: 132px;
}

.header-right :deep(.ant-select-selector) {
  border-radius: 999px !important;
  border-color: rgba(203, 213, 225, 0.88) !important;
  background: rgba(255, 255, 255, 0.8) !important;
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
    overflow: visible;
  }

  .main-content {
    height: auto;
    max-height: none;
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .chat-section,
  .preview-section {
    height: clamp(420px, 68vh, 640px);
    max-height: clamp(420px, 68vh, 640px);
    min-height: 420px;
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

  .version-select {
    width: 100%;
  }

  .main-content {
    gap: 12px;
  }

  .chat-section,
  .preview-section {
    height: 62vh;
    max-height: 62vh;
    min-height: 380px;
  }

  .message-content {
    max-width: 88%;
    max-height: 640px;
  }
}
</style>
