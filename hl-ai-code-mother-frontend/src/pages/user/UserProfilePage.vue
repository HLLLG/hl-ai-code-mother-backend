<template>
  <div id="userProfilePage">
    <div class="page-header">
      <h1>个人信息</h1>
    </div>

    <div class="edit-container">
      <a-card title="基本信息" :loading="loading">
        <a-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="用户名" name="userName">
            <a-input
              v-model:value="formData.userName"
              placeholder="请输入用户名"
              :maxlength="50"
              show-count
            />
          </a-form-item>

          <a-form-item label="头像" name="userAvatar" extra="支持图片链接">
            <a-input v-model:value="formData.userAvatar" placeholder="请输入头像图片链接" />
            <div v-if="formData.userAvatar" class="avatar-preview">
              <a-avatar :src="formData.userAvatar" :size="96" />
            </div>
          </a-form-item>

          <a-form-item label="简介" name="userProfile">
            <a-textarea
              v-model:value="formData.userProfile"
              placeholder="请输入个人简介"
              :rows="4"
              :maxlength="500"
              show-count
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
              <a-button @click="resetForm">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card title="账户信息" style="margin-top: 24px">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="用户 ID">
            <span
              class="copyable"
              title="点击复制"
              @click="copyText(displayId, '用户 ID')"
            >
              {{ displayId }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="账号">
            <span
              class="copyable"
              title="点击复制"
              @click="copyText(displayAccount, '账号')"
            >
              {{ displayAccount }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item v-if="profile?.createTime" label="注册时间">
            {{ formatTime(profile.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item v-if="profile?.updateTime" label="更新时间">
            {{ formatTime(profile.updateTime) }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { getLoginUser, update as updateUser } from '@/api/userController'
import { userLoginStore } from '@/stores/loginUser.ts'
import { formatTime } from '@/utils/time'

const loginUserStore = userLoginStore()

const profile = ref<API.LoginUserVO>()
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const displayId = computed(() =>
  profile.value?.id != null ? String(profile.value.id) : '—'
)
const displayAccount = computed(() => profile.value?.userAccount || '—')

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 50, message: '用户名长度在 1-50 个字符', trigger: 'blur' },
  ],
  userAvatar: [
    {
      validator: async (_rule: unknown, value: string) => {
        const v = value?.trim()
        if (!v) {
          return Promise.resolve()
        }
        try {
          const u = new URL(v)
          if (u.protocol !== 'http:' && u.protocol !== 'https:') {
            return Promise.reject('请输入有效的图片 URL')
          }
          return Promise.resolve()
        } catch {
          return Promise.reject('请输入有效的图片 URL')
        }
      },
      trigger: 'blur',
    },
  ],
  userProfile: [{ max: 500, message: '简介最多 500 个字符', trigger: 'blur' }],
}

const applyProfileToForm = (data: API.LoginUserVO) => {
  formData.userName = data.userName || ''
  formData.userAvatar = data.userAvatar || ''
  formData.userProfile = data.userProfile || ''
}

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      profile.value = res.data.data
      applyProfileToForm(res.data.data)
    } else {
      message.error(res.data.message || '获取个人信息失败')
    }
  } catch (e) {
    console.error(e)
    message.error('获取个人信息失败')
  } finally {
    loading.value = false
  }
}

const copyText = async (text: string, label: string) => {
  if (!text || text === '—') {
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    message.success(`${label}已复制`)
  } catch {
    message.error('复制失败')
  }
}

const handleSubmit = async () => {
  const id = profile.value?.id
  if (id == null) {
    message.error('用户未登录或信息不完整')
    return
  }

  submitting.value = true
  try {
    const res = await updateUser({
      id,
      userName: formData.userName?.trim(),
      userAvatar: formData.userAvatar?.trim() ?? '',
      userProfile: formData.userProfile?.trim() ?? '',
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      await fetchProfile()
      await loginUserStore.fetchLoginUser(true)
    } else {
      message.error('修改失败：' + (res.data.message || ''))
    }
  } catch (e) {
    console.error(e)
    message.error('修改失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  if (profile.value) {
    applyProfileToForm(profile.value)
  }
  formRef.value?.clearValidate()
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
#userProfilePage {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.edit-container {
  border-radius: 8px;
}

.avatar-preview {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}

.copyable {
  cursor: pointer;
  color: #1677ff;
  word-break: break-all;
}

.copyable:hover {
  text-decoration: underline;
}
</style>
