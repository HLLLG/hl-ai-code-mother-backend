import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController.ts'

export const userLoginStore = defineStore('loginUser', () => {
  // 默认值
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })
  const hasFetchedLoginUser = ref(false)
  let fetchLoginUserPromise: Promise<API.LoginUserVO> | null = null

  // 获取登陆用户信息
  async function fetchLoginUser(force = false) {
    if (!force && fetchLoginUserPromise) {
      return fetchLoginUserPromise
    }

    fetchLoginUserPromise = (async () => {
      try {
        const res = await getLoginUser()
        if (res.data.code === 0 && res.data.data) {
          loginUser.value = res.data.data
        } else {
          loginUser.value = {
            userName: '未登录',
          }
        }
      } catch {
        loginUser.value = {
          userName: '未登录',
        }
      } finally {
        hasFetchedLoginUser.value = true
        fetchLoginUserPromise = null
      }

      return loginUser.value
    })()

    return fetchLoginUserPromise
  }

  // 更新登录用户信息
  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = {
      userName: '未登录',
      ...newLoginUser,
    }
    hasFetchedLoginUser.value = true
  }

  return { loginUser, hasFetchedLoginUser, fetchLoginUser, setLoginUser }
})
