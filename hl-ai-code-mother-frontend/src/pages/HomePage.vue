<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import AppCard from '@/components/AppCard.vue'
import AppPreviewModal from '@/components/AppPreviewModal.vue'
import { userLoginStore } from '@/stores/loginUser.ts'
import {
  getAppStaticPreviewUrl,
  HOME_APP_PAGE_SIZE,
  isFeaturedApp,
  MAX_USER_APP_PAGE_SIZE,
} from '@/utils/app.ts'

const router = useRouter()
const loginUserStore = userLoginStore()

const quickPrompts = [
  '波普风电商页面',
  '企业网站',
  '电商运营后台',
  '暗黑话题社区',
]

const createPrompt = ref('')
const createLoading = ref(false)
const previewVisible = ref(false)
const currentPreviewApp = ref<API.AppVO>()
const currentPreviewUrl = ref('')

const isLogin = computed(() => !!loginUserStore.loginUser.id)

const myApps = ref<API.AppVO[]>([])
const myTotal = ref(0)
const myLoading = ref(false)
const myQuery = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: HOME_APP_PAGE_SIZE,
  appName: '',
})

const featuredApps = ref<API.AppVO[]>([])
const featuredTotal = ref(0)
const featuredLoading = ref(false)
const featuredQuery = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: HOME_APP_PAGE_SIZE,
  appName: '',
})

const fetchMyApps = async () => {
  if (!isLogin.value) {
    myApps.value = []
    myTotal.value = 0
    return
  }

  myLoading.value = true
  try {
    const res = await listMyAppVoByPage({
      ...myQuery,
      pageSize: Math.min(myQuery.pageSize ?? HOME_APP_PAGE_SIZE, MAX_USER_APP_PAGE_SIZE),
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records ?? []
      myTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取我的应用失败，' + res.data.message)
    }
  } finally {
    myLoading.value = false
  }
}

const fetchFeaturedApps = async () => {
  featuredLoading.value = true
  try {
    const res = await listGoodAppVoByPage({
      ...featuredQuery,
      pageSize: Math.min(featuredQuery.pageSize ?? HOME_APP_PAGE_SIZE, MAX_USER_APP_PAGE_SIZE),
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records ?? []
      featuredTotal.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取精选应用失败，' + res.data.message)
    }
  } finally {
    featuredLoading.value = false
  }
}

const handleCreateApp = async () => {
  const prompt = createPrompt.value.trim()
  if (!prompt) {
    message.warning('请输入想要生成的网站需求')
    return
  }

  createLoading.value = true
  try {
    const res = await addApp({ initPrompt: prompt })
    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功，正在进入对话页')
      await router.push({
        path: `/app/chat/${res.data.data}`,
      })
      createPrompt.value = ''
    } else {
      message.error('创建应用失败，' + res.data.message)
    }
  } finally {
    createLoading.value = false
  }
}

const useQuickPrompt = (prompt: string) => {
  createPrompt.value = prompt
}

const goToLogin = () => {
  router.push({
    path: '/user/login',
    query: {
      redirect: '/',
    },
  })
}

const openAppConversation = (app?: API.AppVO) => {
  if (!app?.id) {
    return
  }

  // 已生成过内容的应用进入查看模式，避免再次触发首轮自动生成。
  const shouldUseViewMode = Boolean(app.codeGenType || app.deployKey)

  router.push({
    path: `/app/chat/${app.id}`,
    query: shouldUseViewMode ? { view: '1' } : undefined,
  })
}

const canViewConversation = (app?: API.AppVO) => {
  const loginUserId = loginUserStore.loginUser.id
  const ownerId = app?.userId ?? app?.user?.id

  // 只有本人作品才展示“查看对话”按钮，避免查看他人作品时误入可对话入口。
  return !!loginUserId && !!ownerId && loginUserId === ownerId
}

const canPreviewApp = (app?: API.AppVO) => {
  return !!getAppStaticPreviewUrl(app)
}

const openAppPreview = (app?: API.AppVO) => {
  const previewUrl = getAppStaticPreviewUrl(app)
  if (!app?.id || !previewUrl) {
    message.warning('当前应用暂未生成可预览的作品')
    return
  }

  // 在首页弹出作品预览层，直接展示静态资源页面。
  currentPreviewApp.value = app
  currentPreviewUrl.value = previewUrl
  previewVisible.value = true
}

const handleMySearch = () => {
  myQuery.pageNum = 1
  fetchMyApps()
}

const handleFeaturedSearch = () => {
  featuredQuery.pageNum = 1
  fetchFeaturedApps()
}

const handleMyPageChange = (page: number, pageSize: number) => {
  myQuery.pageNum = page
  myQuery.pageSize = Math.min(pageSize, MAX_USER_APP_PAGE_SIZE)
  fetchMyApps()
}

const handleFeaturedPageChange = (page: number, pageSize: number) => {
  featuredQuery.pageNum = page
  featuredQuery.pageSize = Math.min(pageSize, MAX_USER_APP_PAGE_SIZE)
  fetchFeaturedApps()
}

watch(
  () => loginUserStore.loginUser.id,
  () => {
    fetchMyApps()
  },
)

onMounted(() => {
  fetchFeaturedApps()
  fetchMyApps()
})

</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <h1 style="color: rgba(0,255,255,0.46)">AI 应用生成平台</h1>
        <p>
          一句话轻松创建网站应用
        </p>

        <div class="prompt-card">
          <a-textarea
            v-model:value="createPrompt"
            class="prompt-textarea"
            :auto-size="{ minRows: 4, maxRows: 6 }"
            placeholder="例如：帮我创建一个信息管理系统"
          />
          <div class="prompt-footer">
            <a-space wrap>
              <a-tag
                v-for="prompt in quickPrompts"
                :key="prompt"
                class="quick-tag"
                @click="useQuickPrompt(prompt)"
              >
                {{ prompt }}
              </a-tag>
            </a-space>
            <a-button type="primary" size="large" :loading="createLoading" @click="handleCreateApp">
              开始创建
            </a-button>
          </div>
        </div>
      </div>
    </section>

    <section class="app-section glass-panel">
      <div class="section-shell">
        <div class="section-header">
          <div>
            <h2>我的作品</h2>
          </div>
          <a-button v-if="!isLogin" type="primary" ghost @click="goToLogin">登录后查看</a-button>
        </div>

        <div v-if="isLogin">
          <a-form layout="inline" :model="myQuery" class="search-form" @finish="handleMySearch">
            <a-form-item>
              <a-input v-model:value="myQuery.appName" allow-clear placeholder="搜索我的应用名称" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit">搜索</a-button>
            </a-form-item>
            <a-form-item>
              <a-button @click="handleMySearch">刷新</a-button>
            </a-form-item>
          </a-form>

          <a-spin :spinning="myLoading">
            <a-empty v-if="!myApps.length" description="你还没有创建应用，先从上方输入需求开始吧" />
            <a-row v-else :gutter="[20, 20]">
              <a-col v-for="app in myApps" :key="app.id" :xs="24" :md="12" :xl="8">
                <AppCard
                  :app="app"
                  :featured="isFeaturedApp(app.priority)"
                  :can-view-conversation="canViewConversation(app)"
                  :can-preview="canPreviewApp(app)"
                  :author-name="loginUserStore.loginUser.userName || app.user?.userName || '我'"
                  :author-initial="app.user?.userName || loginUserStore.loginUser.userName || '我'"
                  @conversation="openAppConversation"
                  @preview="openAppPreview"
                />
              </a-col>
            </a-row>
          </a-spin>

          <div v-if="myTotal > 0" class="pagination-wrap">
            <a-pagination
              v-model:current="myQuery.pageNum"
              v-model:pageSize="myQuery.pageSize"
              :total="myTotal"
              :page-size-options="['6', '12', '20']"
              :show-total="(total:number) => `共 ${total} 个应用`"
              @change="handleMyPageChange"
              @showSizeChange="handleMyPageChange"
            />
          </div>
        </div>

        <a-empty v-else description="登录后即可查看你的应用作品集" />
      </div>
    </section>

    <section class="app-section glass-panel">
      <div class="section-shell">
        <div class="section-header">
          <div>
            <h2>精选案例</h2>
          </div>
        </div>

        <a-form layout="inline" :model="featuredQuery" class="search-form" @finish="handleFeaturedSearch">
          <a-form-item>
            <a-input v-model:value="featuredQuery.appName" allow-clear placeholder="搜索精选应用名称" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit">搜索</a-button>
          </a-form-item>
          <a-form-item>
            <a-button @click="handleFeaturedSearch">刷新</a-button>
          </a-form-item>
        </a-form>

        <a-spin :spinning="featuredLoading">
          <a-empty v-if="!featuredApps.length" description="暂无精选案例" />
          <a-row v-else :gutter="[20, 20]">
            <a-col v-for="app in featuredApps" :key="app.id" :xs="24" :md="12" :xl="8">
              <AppCard
                :app="app"
                featured
                :can-view-conversation="canViewConversation(app)"
                :can-preview="canPreviewApp(app)"
                :author-name="app.user?.userName || 'NoCode 官方'"
                :author-initial="app.user?.userName || 'N'"
                :cover-title="app.appName || '精选应用'"
                @conversation="openAppConversation"
                @preview="openAppPreview"
              />
            </a-col>
          </a-row>
        </a-spin>

        <div v-if="featuredTotal > 0" class="pagination-wrap">
          <a-pagination
            v-model:current="featuredQuery.pageNum"
            v-model:pageSize="featuredQuery.pageSize"
            :total="featuredTotal"
            :page-size-options="['6', '12', '20']"
            :show-total="(total:number) => `共 ${total} 个精选应用`"
            @change="handleFeaturedPageChange"
            @showSizeChange="handleFeaturedPageChange"
          />
        </div>
      </div>
    </section>

    <AppPreviewModal
      v-model:open="previewVisible"
      :app="currentPreviewApp"
      :preview-url="currentPreviewUrl"
    />
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
  width: 100%;
  padding: 0 0 32px;
}

.hero-section {
  position: relative;
  overflow: hidden;
  width: 100%;
  min-height: min(720px, calc(100vh - 108px));
  padding: clamp(48px, 8vw, 88px) clamp(24px, 5vw, 64px);
  border-radius: 0;
  background:
    radial-gradient(circle at 12% 22%, rgba(255, 255, 255, 0.9), transparent 28%),
    radial-gradient(circle at 88% 90%, rgba(114, 239, 221, 0.5), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(232, 247, 255, 0.88) 60%, rgba(198, 243, 255, 0.74));
  box-shadow: inset 0 -20px 40px rgba(255, 255, 255, 0.18);
}

.hero-content {
  position: relative;
  z-index: 1;
  width: min(100%, 980px);
  margin: 0 auto;
  padding-top: clamp(32px, 9vh, 96px);
  text-align: center;
}

.hero-content h1 {
  margin-bottom: 16px;
  font-size: clamp(40px, 6vw, 72px);
  font-weight: 700;
  color: #0f172a;
}

.hero-content p {
  max-width: 760px;
  margin: 0 auto 28px;
  color: rgba(15, 23, 42, 0.72);
  font-size: clamp(16px, 2vw, 19px);
  line-height: 1.8;
}

.prompt-card,
.glass-panel {
  border: 1px solid rgba(255, 255, 255, 0.66);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.prompt-card {
  padding: 22px;
  text-align: left;
}

.prompt-textarea {
  margin-bottom: 16px;
}

.prompt-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.quick-tag {
  padding: 7px 14px;
  border-radius: 999px;
  cursor: pointer;
  user-select: none;
}

.app-section {
  width: min(100% - 48px, 1480px);
  margin: 0 auto;
  padding: 28px;
}

.section-shell {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.section-header h2 {
  margin-bottom: 8px;
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 700;
  color: #111827;
}

.section-header p {
  margin: 0;
  color: rgba(17, 24, 39, 0.6);
}

.search-form {
  margin-bottom: 20px;
}

.pagination-wrap {
  margin-top: 24px;
  text-align: right;
}

@media (max-width: 768px) {
  .home-page {
    gap: 20px;
    padding-bottom: 24px;
  }

  .hero-section {
    min-height: auto;
    padding: 28px 16px 32px;
    border-radius: 0;
  }

  .hero-content {
    width: 100%;
    padding-top: 12px;
  }

  .app-section {
    width: calc(100% - 24px);
    padding: 22px;
    border-radius: 24px;
  }

  .prompt-footer,
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }

}
</style>
