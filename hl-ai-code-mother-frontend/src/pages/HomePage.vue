<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import { userLoginStore } from '@/stores/loginUser.ts'
import {
  formatDateTime,
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
        query: {
          autoPrompt: prompt,
        },
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

const goToChat = (appId?: number, view = false) => {
  if (!appId) {
    return
  }
  router.push({
    path: `/app/chat/${appId}`,
    query: view ? { view: '1' } : undefined,
  })
}

const goToMyAppChat = (app: API.AppVO, forceView = false) => {
  if (!app.id) {
    return
  }

  // 已生成过代码的作品进入查看模式，避免再次自动触发首轮生成
  if (forceView || app.codeGenType || app.deployKey) {
    router.push({
      path: `/app/chat/${app.id}`,
      query: {
        view: '1',
      },
    })
    return
  }

  router.push(`/app/chat/${app.id}`)
}

const goToPreview = (app: API.AppVO, isMine = false) => {
  if (isMine) {
    goToMyAppChat(app, true)
    return
  }
  goToChat(app.id, true)
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

const myTotalPages = computed(() => {
  const pageSize = myQuery.pageSize ?? HOME_APP_PAGE_SIZE
  return Math.max(1, Math.ceil(myTotal.value / pageSize))
})

const featuredTotalPages = computed(() => {
  const pageSize = featuredQuery.pageSize ?? HOME_APP_PAGE_SIZE
  return Math.max(1, Math.ceil(featuredTotal.value / pageSize))
})
</script>

<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <h1 style="color: rgba(0,255,255,0.57)">AI 应用生成平台</h1>
        <p>
          一句话轻松创建网站应用
        </p>

        <div class="prompt-card">
          <a-textarea
            v-model:value="createPrompt"
            class="prompt-textarea"
            :auto-size="{ minRows: 4, maxRows: 6 }"
            placeholder="例如：使用 NoCode 创建一个信息管理系统"
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
            <p>支持按应用名称搜索、分页查看、继续编辑与删除自己的应用。</p>
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
                <a-card class="app-card social-card" :bordered="false" hoverable>
                  <div class="app-cover" @click="goToMyAppChat(app)">
                    <img v-if="app.cover" :src="app.cover" alt="应用封面" />
                    <div v-else class="app-cover-placeholder">
                      <span>{{ app.appName || '未命名应用' }}</span>
                    </div>
                    <div class="cover-overlay">
                      <a-button type="primary" size="large" class="preview-button" @click.stop="goToPreview(app, true)">
                        预览
                      </a-button>
                    </div>
                  </div>
                  <div class="app-card-body social-card-body">
                    <a-avatar class="author-avatar" :src="app.user?.userAvatar">
                      {{ (app.user?.userName || loginUserStore.loginUser.userName || '我').slice(0, 1) }}
                    </a-avatar>
                    <div class="author-meta">
                      <div class="author-title-row">
                        <h3>{{ app.appName || '未命名应用' }}</h3>
                        <a-tag v-if="isFeaturedApp(app.priority)" color="magenta">精选</a-tag>
                      </div>
                      <div class="author-subtitle">
                        <span>{{ loginUserStore.loginUser.userName || app.user?.userName || '我' }}</span>
                        <span>{{ formatDateTime(app.createTime) }}</span>
                      </div>
                    </div>
                  </div>
                </a-card>
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
            <p>精选应用支持按名称搜索和分页浏览，点击后可查看应用详情与网页效果。</p>
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
              <a-card class="app-card social-card" :bordered="false" hoverable>
                <div class="app-cover" @click="goToChat(app.id)">
                  <img v-if="app.cover" :src="app.cover" alt="应用封面" />
                  <div v-else class="app-cover-placeholder featured">
                    <span>{{ app.appName || '精选应用' }}</span>
                  </div>
                  <div class="cover-overlay">
                    <a-button type="primary" size="large" class="preview-button" @click.stop="goToPreview(app)">
                      预览
                    </a-button>
                  </div>
                </div>
                <div class="app-card-body social-card-body">
                  <a-avatar class="author-avatar" :src="app.user?.userAvatar">
                    {{ (app.user?.userName || 'N').slice(0, 1) }}
                  </a-avatar>
                  <div class="author-meta">
                    <div class="author-title-row">
                      <h3>{{ app.appName || '未命名应用' }}</h3>
                      <a-tag color="magenta">精选</a-tag>
                    </div>
                    <div class="author-subtitle">
                      <span>{{ app.user?.userName || 'NoCode 官方' }}</span>
                      <span>{{ formatDateTime(app.createTime) }}</span>
                    </div>
                  </div>
                </div>
              </a-card>
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

.app-card {
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
}

.app-card :deep(.ant-card-body) {
  padding: 16px 18px 18px;
}

.app-cover {
  position: relative;
  overflow: hidden;
  height: 220px;
  border-radius: 18px;
  background: #f3f6fb;
  cursor: pointer;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.app-card:hover .app-cover img {
  transform: scale(1.03);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.32);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.app-card:hover .cover-overlay {
  opacity: 1;
}

.preview-button {
  min-width: 132px;
  height: 44px;
  border: none;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  font-weight: 600;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.15);
}

.preview-button:hover,
.preview-button:focus {
  background: #ffffff;
  color: #111827;
}

.app-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 20px;
  background:
    linear-gradient(145deg, rgba(12, 24, 68, 0.88), rgba(73, 145, 255, 0.4)),
    linear-gradient(135deg, #eff6ff, #e0f2fe);
  color: #ffffff;
  font-size: 28px;
  font-weight: 600;
  text-align: center;
}

.app-cover-placeholder.featured {
  background:
    linear-gradient(145deg, rgba(65, 16, 125, 0.88), rgba(234, 88, 12, 0.45)),
    linear-gradient(135deg, #fff7ed, #fae8ff);
}

.app-card-body.social-card-body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-top: 0;
}

.author-avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #22c55e, #06b6d4);
}

.author-meta {
  min-width: 0;
  flex: 1;
}

.author-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.author-title-row h3 {
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.author-subtitle {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: rgba(17, 24, 39, 0.52);
  font-size: 13px;
}

.pagination-wrap {
  margin-top: 24px;
  text-align: right;
}

.pagination-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: rgba(17, 24, 39, 0.58);
  font-size: 13px;
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

  .app-cover {
    height: 180px;
  }

  .cover-overlay {
    opacity: 1;
    align-items: flex-end;
    padding-bottom: 18px;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0.38));
  }

  .app-card-body.social-card-body,
  .author-title-row {
    align-items: flex-start;
  }

  .author-title-row {
    flex-direction: column;
  }

  .author-title-row h3 {
    white-space: normal;
  }
}
</style>
