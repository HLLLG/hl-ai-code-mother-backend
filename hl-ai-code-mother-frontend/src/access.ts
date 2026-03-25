import router from '@/router'
import { userLoginStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

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

  next()
})
