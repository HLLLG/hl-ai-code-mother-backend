import { getDeployUrl, getStaticPreviewUrl } from '@/config/env'

export const FEATURED_APP_PRIORITY = 99
export const HOME_APP_PAGE_SIZE = 6
export const MAX_USER_APP_PAGE_SIZE = 20

export const getAppStaticPreviewUrl = (app?: API.AppVO) => {
  if (!app?.id || !app.codeGenType) {
    return ''
  }

  // 作品预览地址与后端 serveStaticResource 路由保持一致：/static/{codeGenType}_{appId}/...
  return getStaticPreviewUrl(app.codeGenType, String(app.id), String(app.currentVersion || 1))
}

export const getAppDeployPreviewUrl = (deployKey?: string, version?: number) => {
  if (!deployKey) {
    return ''
  }

  // 部署地址与静态资源预览地址不同，这里严格走部署域名。
  return getDeployUrl(deployKey, version)
}

export const formatDateTime = (dateTime?: string) => {
  if (!dateTime) {
    return '-'
  }

  return dateTime.replace('T', ' ').slice(0, 19)
}


export const isFeaturedApp = (priority?: number) => {
  return (priority ?? 0) >= FEATURED_APP_PRIORITY
}

export const APP_CHAT_MESSAGE_TYPE_ENUM = {
  INFO: 'INFO',
  ERROR: 'ERROR',
  ENTER_CHAT: 'ENTER_CHAT',
  EXIT_CHAT: 'EXIT_CHAT',
  CHAT_ACTION: 'CHAT_ACTION',
  /** 围观：同步编辑者与 AI 的流式对话 */
  CHAT_STREAM: 'CHAT_STREAM',
  BUILD_DONE: 'BUILD_DONE',
}

export const APP_CHAT_MESSAGE_TYPE_MAP = {
  INFO: '发送通知',
  ERROR: '发送错误',
  ENTER_CHAT: '进入对话状态',
  EXIT_CHAT: '退出对话状态',
  CHAT_ACTION: '执行对话操作',
  CHAT_STREAM: '对话流式同步',
}

/** 与后端 AppChatStreamPhaseEnum 一致 */
export const APP_CHAT_STREAM_PHASE = {
  START: 'START',
  CHUNK: 'CHUNK',
  DONE: 'DONE',
  ERROR: 'ERROR',
  STOPPED: 'STOPPED',
} as const




