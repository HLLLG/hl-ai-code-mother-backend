<template>
  <div id="user-login-page">
    <h2 class="title">HL AI应用生成 - 用户登录</h2>
    <h3 class="desc">不写一行代码，生成完整应用</h3>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号！' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码！' },
          { min: 8, message: '密码长度至少8位！' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <div class="tips">
        没有账号?
        <RouterLink to="/user/register" style="color: rgba(22, 119, 255, 0.85)">去注册</RouterLink>
      </div>

      <a-form-item>
        <a-button class="login-button" type="primary" html-type="submit" style="width: 100%"
          >登录</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLogin } from '@/api/userController.ts'
import { useRoute, useRouter } from 'vue-router'
import { userLoginStore } from '@/stores/loginUser.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = userLoginStore()

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const getRedirectPath = () => {
  const redirect = Array.isArray(route.query.redirect) ? route.query.redirect[0] : route.query.redirect
  if (!redirect) {
    return '/'
  }

  try {
    const redirectUrl = new URL(redirect, window.location.origin)
    if (redirectUrl.origin === window.location.origin) {
      return `${redirectUrl.pathname}${redirectUrl.search}${redirectUrl.hash}`
    }
  } catch {
    return redirect
  }

  return '/'
}

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: getRedirectPath(),
      replace: true,
    })
  } else {
    message.error('登录失败,' + res.data.message)
  }
}
</script>

<style scoped>
#user-login-page {
  max-width: 480px;
  margin: 0 auto;
}

.title {
  text-align: center;
}

.desc {
  text-align: center;
  color: #bbb;
}

.tips {
  text-align: right;
  font-size: 13px;
  color: #bbbbbb;
  margin-bottom: 12px;
}

.login-button {
  flex-shrink: 0;
  padding-inline: 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(25, 25, 0, 0.56), rgba(255, 255, 255, 0.14));
  color: black;
  font-weight: 600;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.56),
    0 10px 24px rgba(10, 56, 132, 0.18);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease,
    background-color 0.25s ease,
    border-color 0.25s ease;
}
</style>
