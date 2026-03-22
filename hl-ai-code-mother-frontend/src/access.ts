import router from '@/router'
import { userLoginStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach((to, from, next) => {
  const loginUserStore = userLoginStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error("您没有访问权限")
      // 若用户已登录，则跳转回原来的页面
      if (loginUser) {
        next(from.fullPath)
        return
      }
      // 否则跳转到登录页面
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
