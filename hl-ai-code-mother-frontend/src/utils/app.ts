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

export const getAppDeployPreviewUrl = (deployKey?: string) => {
  if (!deployKey) {
    return ''
  }

  // 部署地址与静态资源预览地址不同，这里严格走部署域名。
  return getDeployUrl(deployKey)
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

