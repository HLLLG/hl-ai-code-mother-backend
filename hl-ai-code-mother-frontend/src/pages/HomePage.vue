<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { addApp, deleteApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController.ts'
import { userLoginStore } from '@/stores/loginUser.ts'
import {
  formatDateTime,
  getAppStaticPreviewUrl,
  getAppStatusText,
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

const goToChat = (appId?: number) => {
  if (!appId) {
    return
  }
  router.push(`/app/chat/${appId}`)
}

const goToMyAppChat = (app: API.AppVO) => {
  if (!app.id) {
    return
  }

  // 已生成过代码的作品进入查看模式，避免再次自动触发首轮生成
  if (app.codeGenType || app.deployKey) {
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

const goToEdit = (appId?: number) => {
  if (!appId) {
    return
  }
  router.push(`/app/edit/${appId}`)
}

const openPreview = (app: API.AppVO) => {
  const previewUrl = getAppStaticPreviewUrl(app)
  if (!previewUrl) {
    message.warning('当前应用暂未生成可预览内容')
    return
  }
  window.open(previewUrl, '_blank')
}

const handleDeleteApp = async (appId?: number) => {
  if (!appId) {
    return
  }

  const res = await deleteApp({ id: appId })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchMyApps()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

const handleMySearch = () => {
  myQuery.pageNum = 1
  fetchMyApps()
}

const handleFeaturedSearch = () => {
  featuredQuery.pageNum = 1
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
        <a-tag class="hero-badge" color="cyan">NoCode · AI 应用生成</a-tag>
        <h1>一句话，生成完整网站应用</h1>
        <p>
          输入你的需求，系统会自动创建应用、进入 AI 对话生成流程，并在右侧实时展示网页效果。
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
            <a-button @click="fetchMyApps">刷新</a-button>
          </a-form-item>
        </a-form>

        <a-spin :spinning="myLoading">
          <a-empty v-if="!myApps.length" description="你还没有创建应用，先从上方输入需求开始吧" />
          <a-row v-else :gutter="[20, 20]">
            <a-col v-for="app in myApps" :key="app.id" :xs="24" :md="12" :xl="8">
              <a-card class="app-card" :bordered="false" hoverable>
                <div class="app-cover" @click="goToMyAppChat(app)">
                  <img v-if="app.cover" :src="app.cover" alt="应用封面" />
                  <div v-else class="app-cover-placeholder">
                    <span>{{ app.appName || '未命名应用' }}</span>
                  </div>
                </div>
                <div class="app-card-body">
                  <div class="app-card-header">
                    <h3>{{ app.appName || '未命名应用' }}</h3>
                    <a-space wrap>
                      <a-tag :color="app.deployKey ? 'green' : app.codeGenType ? 'blue' : 'orange'">
                        {{ getAppStatusText(app) }}
                      </a-tag>
                      <a-tag v-if="isFeaturedApp(app.priority)" color="magenta">精选</a-tag>
                    </a-space>
                  </div>
                  <p class="app-desc">{{ app.initPrompt || '暂无应用描述' }}</p>
                  <div class="app-meta">
                    <span>生成类型：{{ app.codeGenType || '待生成' }}</span>
                    <span>创建时间：{{ formatDateTime(app.createTime) }}</span>
                  </div>
                  <a-space wrap class="app-actions">
                    <a-button type="link" @click="goToMyAppChat(app)">查看详情</a-button>
                    <a-button type="link" @click="openPreview(app)">预览</a-button>
                    <a-button type="link" @click="goToEdit(app.id)">编辑</a-button>
                    <a-popconfirm title="确认删除该应用吗？" ok-text="删除" cancel-text="取消" @confirm="handleDeleteApp(app.id)">
                      <a-button type="link" danger>删除</a-button>
                    </a-popconfirm>
                  </a-space>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </a-spin>

        <div v-if="myTotal > (myQuery.pageSize ?? HOME_APP_PAGE_SIZE)" class="pagination-wrap">
          <a-pagination
            v-model:current="myQuery.pageNum"
            v-model:pageSize="myQuery.pageSize"
            :total="myTotal"
            :page-size-options="['6', '12', '20']"
            show-size-changer
            :show-total="(total:number) => `共 ${total} 个应用`"
            @change="fetchMyApps"
          />
        </div>
      </div>

      <a-empty v-else description="登录后即可查看你的应用作品集" />
    </section>

    <section class="app-section glass-panel">
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
          <a-button @click="fetchFeaturedApps">刷新</a-button>
        </a-form-item>
      </a-form>

      <a-spin :spinning="featuredLoading">
        <a-empty v-if="!featuredApps.length" description="暂无精选案例" />
        <a-row v-else :gutter="[20, 20]">
          <a-col v-for="app in featuredApps" :key="app.id" :xs="24" :md="12" :xl="8">
            <a-card class="app-card" :bordered="false" hoverable>
              <div class="app-cover" @click="goToChat(app.id)">
                <img v-if="app.cover" :src="app.cover" alt="应用封面" />
                <div v-else class="app-cover-placeholder featured">
                  <span>{{ app.appName || '精选应用' }}</span>
                </div>
              </div>
              <div class="app-card-body">
                <div class="app-card-header">
                  <h3>{{ app.appName || '未命名应用' }}</h3>
                  <a-space wrap>
                    <a-tag color="magenta">精选</a-tag>
                    <a-tag :color="app.deployKey ? 'green' : app.codeGenType ? 'blue' : 'orange'">
                      {{ getAppStatusText(app) }}
                    </a-tag>
                  </a-space>
                </div>
                <p class="app-desc">{{ app.initPrompt || '暂无应用描述' }}</p>
                <div class="app-meta">
                  <span>作者：{{ app.user?.userName || 'NoCode 官方' }}</span>
                  <span>创建时间：{{ formatDateTime(app.createTime) }}</span>
                </div>
                <a-space wrap class="app-actions">
                  <a-button type="link" @click="goToChat(app.id)">查看详情</a-button>
                  <a-button type="link" @click="openPreview(app)">在线预览</a-button>
                </a-space>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </a-spin>

      <div v-if="featuredTotal > (featuredQuery.pageSize ?? HOME_APP_PAGE_SIZE)" class="pagination-wrap">
        <a-pagination
          v-model:current="featuredQuery.pageNum"
          v-model:pageSize="featuredQuery.pageSize"
          :total="featuredTotal"
          :page-size-options="['6', '12', '20']"
          show-size-changer
          :show-total="(total:number) => `共 ${total} 个精选应用`"
          @change="fetchFeaturedApps"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.hero-section {
  position: relative;
  overflow: hidden;
  padding: 56px 32px;
  border-radius: 32px;
  background:
    radial-gradient(circle at 12% 22%, rgba(255, 255, 255, 0.9), transparent 28%),
    radial-gradient(circle at 88% 90%, rgba(114, 239, 221, 0.5), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(232, 247, 255, 0.88) 60%, rgba(198, 243, 255, 0.74));
  box-shadow: 0 20px 60px rgba(19, 82, 127, 0.12);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 920px;
  margin: 0 auto;
  text-align: center;
}

.hero-badge {
  margin-bottom: 20px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
}

.hero-content h1 {
  margin-bottom: 16px;
  font-size: clamp(36px, 6vw, 64px);
  font-weight: 700;
  color: #0f172a;
}

.hero-content p {
  max-width: 720px;
  margin: 0 auto 28px;
  color: rgba(15, 23, 42, 0.72);
  font-size: 18px;
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
  padding: 28px;
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

.app-cover {
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

.app-card-body {
  padding-top: 20px;
}

.app-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.app-card-header h3 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 700;
}

.app-desc {
  min-height: 48px;
  margin-bottom: 14px;
  color: rgba(17, 24, 39, 0.68);
  line-height: 1.7;
}

.app-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: rgba(17, 24, 39, 0.52);
  font-size: 13px;
}

.app-actions {
  margin-top: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .hero-section,
  .app-section {
    padding: 22px;
    border-radius: 24px;
  }

  .prompt-footer,
  .section-header,
  .app-card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-cover {
    height: 180px;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
