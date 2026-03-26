<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import AppCover from '@/components/AppCover.vue'
import UserInfo from '@/components/UserInfo.vue'
import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController.ts'
import { CODE_GEN_TYPE_OPTIONS, formatCodeGenType } from '@/utils/codeGenTypes.ts'
import { formatTime } from '@/utils/time.ts'
import { FEATURED_APP_PRIORITY, formatDateTime, isFeaturedApp } from '@/utils/app.ts'

const router = useRouter()

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 90,
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
    ellipsis: true,
  },
  {
    title: '封面',
    dataIndex: 'cover',
    width: 120,
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    ellipsis: true,
    width: 220,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 140,
  },
  {
    title: '部署键',
    dataIndex: 'deployKey',
    ellipsis: true,
    width: 100,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 180,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 180,
  },
  {
    title: '创建者',
    dataIndex: 'user',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 300,
  },
]

const data = ref<API.AppVO[]>([])
const totalRow = ref(0)
const loading = ref(false)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: totalRow.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`,
}))

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAppVoByPageByAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      totalRow.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取应用列表失败，' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const resetSearch = () => {
  searchParams.pageNum =1
  searchParams.pageSize =10
  searchParams.appName = undefined
  searchParams.userId = undefined
  searchParams.codeGenType = undefined
  searchParams.priority = undefined
  fetchData()
}


const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const goToEdit = (id?: number) => {
  if (!id) {
    return
  }
  router.push(`/app/edit/${id}`)
}

const goToDetail = (id?: number) => {
  if (!id) {
    return
  }
  router.push({
    path: `/app/chat/${id}`,
    query: {
      view: '1',
    },
  })
}

const doDelete = async (id?: number) => {
  if (!id) {
    return
  }

  const res = await deleteAppByAdmin({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

const doFeature = async (record: API.AppVO) => {
  if (!record.id) {
    return
  }

  const featured = isFeaturedApp(record.priority)
  const nextPriority = featured ? 0 : FEATURED_APP_PRIORITY

  const res = await updateAppByAdmin({
    id: record.id,
    appName: record.appName,
    cover: record.cover,
    priority: nextPriority,
  })
  if (res.data.code === 0) {
    message.success(featured ? '已取消精选' : '已设置为精选应用')
    fetchData()
  } else {
    message.error((featured ? '取消精选失败，' : '精选设置失败，') + res.data.message)
  }
}

const getFeatureButtonStyle = (featured: boolean) => {
  if (featured) {
    return {
      borderColor: '#faad14',
      color: '#d48806',
      background: '#fffbe6',
    }
  }

  return {
    borderColor: '#722ed1',
    background: '#722ed1',
    boxShadow: '0 4px 10px rgba(114, 46, 209, 0.22)',
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="app-manage-page">
    <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" allow-clear placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="创建者 id">
        <a-input v-model:value="searchParams.userId" placeholder="输入用户 id" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-select
          v-model:value="searchParams.codeGenType"
          allow-clear
          placeholder="选择生成类型"
          :options="CODE_GEN_TYPE_OPTIONS"
          style="width: 180px"
        />
      </a-form-item>
      <a-form-item label="优先级">
        <a-input-number v-model:value="searchParams.priority" placeholder="输入优先级" style="width: 120px"/>
      </a-form-item>
      <a-form-item>
        <a-space wrap>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button @click="resetSearch">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-divider />

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 1500 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <AppCover
            :src="record.cover"
            :title="record.appName || '无封面'"
            width="72px"
            height="48px"
            radius="8px"
            padding="6px"
            font-size="12px"
          />
        </template>
        <template v-else-if="column.dataIndex === 'initPrompt'">
          <span class="ellipsis-text" :title="record.initPrompt || '暂无初始提示词'">
            {{ record.initPrompt || '暂无初始提示词' }}
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <span>{{ formatCodeGenType(record.codeGenType) }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag :color="isFeaturedApp(record.priority) ? 'magenta' : 'blue'">
            {{ isFeaturedApp(record.priority) ? '精选' : (record.priority ?? 0) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'deployedTime'">
          <span v-if="record.deployedTime">{{ formatTime(record.deployedTime) }}</span>
          <span v-else class="subtle-text">未部署</span>
        </template>
        <template v-else-if="column.dataIndex === 'user'">
          <UserInfo :user="record.user" :size="32" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ formatDateTime(record.updateTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button
              type="default"
              size="small"
              class="action-btn action-btn-detail"
              @click="goToDetail(record.id)"
            >
              详情
            </a-button>
            <a-button
              type="primary"
              size="small"
              ghost
              class="action-btn"
              @click="goToEdit(record.id)"
            >
              编辑
            </a-button>
            <a-popconfirm
              title="确认删除该应用吗？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="primary" danger size="small" class="action-btn">删除</a-button>
            </a-popconfirm>
            <a-button
              :type="isFeaturedApp(record.priority) ? 'default' : 'primary'"
              size="small"
              class="action-btn"
              :style="getFeatureButtonStyle(isFeaturedApp(record.priority))"
              @click="doFeature(record)"
            >
              {{ isFeaturedApp(record.priority) ? '取消精选' : '精选' }}
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#app-manage-page {
  width: 100%;
  padding: 4px 0;
}

.search-form {
  row-gap: 12px;
}


.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtle-text {
  color: rgba(0, 0, 0, 0.45);
}

.action-btn {
  min-width: 68px;
  border-radius: 999px;
  font-weight: 500;
}

.action-btn-detail {
  border-color: #d9d9d9;
  color: rgba(0, 0, 0, 0.72);
}
</style>
