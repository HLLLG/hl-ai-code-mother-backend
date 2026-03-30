import router from '@/router'
import { userLoginStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'
import { getAppVoById } from '@/api/appController.ts'
import { canAccessAppDetail, canAccessAppMembers } from '@/utils/appMembers.ts'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

const resolveRouteAppId = (idParam: unknown) => {
  if (typeof idParam !== 'string') {
    return ''
  }
  return idParam.trim()
}

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = userLoginStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }

  const isLogin = !!loginUser?.id
  const isAdmin = loginUser?.userRole === 'admin'
  const toUrl = to.fullPath
  const requiresAuth = Boolean(to.meta.requiresAuth)
  const requiresAdmin = Boolean(to.meta.adminOnly) || toUrl.startsWith('/admin')
  const requiresAppAccess = Boolean(to.meta.requiresAppAccess)
  const requiresAppMember = Boolean(to.meta.requiresAppMember)

  if (requiresAdmin && !isAdmin) {
    message.error('您没有访问权限')
    if (isLogin) {
      next(from.fullPath && from.fullPath !== to.fullPath ? from.fullPath : '/')
      return
    }
    next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  if (requiresAuth && !isLogin) {
    message.warning('请先登录')
    next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  if ((requiresAppAccess || requiresAppMember) && isLogin) {
    const appId = resolveRouteAppId(to.params.id)
    if (!appId) {
      message.error('应用 ID 不合法')
      next('/')
      return
    }

    try {
      const res = await getAppVoById({ id: appId as unknown as number })
      if (res.data.code !== 0 || !res.data.data) {
        message.error(res.data.message || '应用不存在')
        next('/')
        return
      }

      const app = res.data.data
      const canVisitDetail = canAccessAppDetail(app, loginUser)
      const canVisitMembers = canAccessAppMembers(app, loginUser.id)

      if (requiresAppMember && !canVisitMembers) {
        message.error(app.myMemberStatus === 0 ? '请先接受邀请再查看成员' : '只有应用成员才能进入成员管理')
        next('/')
        return
      }

      if (requiresAppAccess && !canVisitDetail) {
        message.error(app.myMemberStatus === 0 ? '请先接受邀请再查看作品和对话' : '您没有访问该应用的权限')
        next('/')
        return
      }
    } catch {
      message.error('获取应用信息失败')
      next('/')
      return
    }
  }

  next()
})
