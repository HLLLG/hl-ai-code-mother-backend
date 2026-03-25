import { API_BASE_URL } from '@/request'

export const FEATURED_APP_PRIORITY = 99
export const HOME_APP_PAGE_SIZE = 6
export const MAX_USER_APP_PAGE_SIZE = 20

export const getAppStaticPreviewUrl = (app?: Pick<API.AppVO, 'id' | 'codeGenType'>) => {
  if (!app?.id || !app.codeGenType) {
    return ''
  }

  return `${API_BASE_URL}/static/${app.codeGenType}_${app.id}/`
}

export const formatDateTime = (dateTime?: string) => {
  if (!dateTime) {
    return '-'
  }

  return dateTime.replace('T', ' ').slice(0, 19)
}

export const getAppStatusText = (app?: API.AppVO) => {
  if (app?.deployKey) {
    return '已部署'
  }
  if (app?.codeGenType) {
    return '已生成'
  }
  return '生成中'
}

export const isFeaturedApp = (priority?: number) => {
  return (priority ?? 0) >= FEATURED_APP_PRIORITY
}

