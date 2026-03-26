<template>
  <div id="user-register-page">
    <h1 class="title">HL AI应用生成 - 用户注册</h1>
    <h2 class="desc">不写一行代码，生成完整应用</h2>
    <a-form
      :model="formState"
      name="register"
      autocomplete="off"
      @finish="handleSubmit"
      style="min-width: 480px"
    >
      <a-form-item
        name="userAccount"
        :rules="[
          { required: true, message: '请输入账号！' },
          { min: 4, message: '账号长度至少4位！' },
        ]"
      >
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

      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码！' },
          { min: 8, message: '确认密码长度至少8位！' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请再次输入密码" />
      </a-form-item>

      <div class="tips">
        已有账号?
        <RouterLink to="/user/login" class="register-link">去登录</RouterLink>
      </div>

      <a-form-item>
        <a-button class="register-button" type="primary" html-type="submit" style="width: 100%"
          >注册</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userRegister } from '@/api/userController.ts'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const handleSubmit = async (values: API.UserRegisterRequest) => {
  if (values.userPassword !== values.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }

  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功，请登录')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败,' + res.data.message)
  }
}
</script>

<style scoped>
#user-register-page {
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

.register-link {
  color: rgba(22, 119, 255, 0.85);
}

.register-link:hover {
  color: #4096ff;
}

.register-button {
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
