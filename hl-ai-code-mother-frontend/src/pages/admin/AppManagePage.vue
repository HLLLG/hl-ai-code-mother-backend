<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController.ts'
import { FEATURED_APP_PRIORITY, formatDateTime } from '@/utils/app.ts'

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
    title: '生成类型',
    dataIndex: 'codeGenType',
    width: 140,
  },
  {
    title: '部署键',
    dataIndex: 'deployKey',
    ellipsis: true,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 100,
  },
  {
    title: '创建者 id',
    dataIndex: 'userId',
    width: 110,
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
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 260,
  },
]

const data = ref<API.AppVO[]>([])
const totalRow = ref(0)
const loading = ref(false)

const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  id: undefined,
  appName: '',
  userId: undefined,
  codeGenType: '',
  deployKey: '',
  priority: undefined,
  cover: '',
  initPrompt: '',
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
  searchParams.pageNum = 1
  searchParams.pageSize = 10
  searchParams.id = undefined
  searchParams.appName = ''
  searchParams.userId = undefined
  searchParams.codeGenType = ''
  searchParams.deployKey = ''
  searchParams.priority = undefined
  searchParams.cover = ''
  searchParams.initPrompt = ''
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
  router.push(`/app/chat/${id}`)
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

  const res = await updateAppByAdmin({
    id: record.id,
    appName: record.appName,
    cover: record.cover,
    priority: FEATURED_APP_PRIORITY,
  })
  if (res.data.code === 0) {
    message.success('已设置为精选应用')
    fetchData()
  } else {
    message.error('精选设置失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="app-manage-page">
    <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
      <a-form-item label="应用 id">
        <a-input-number v-model:value="searchParams.id" :min="1" placeholder="输入 id" />
      </a-form-item>
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" allow-clear placeholder="输入应用名称" />
      </a-form-item>
      <a-form-item label="创建者 id">
        <a-input-number v-model:value="searchParams.userId" :min="1" placeholder="输入用户 id" />
      </a-form-item>
      <a-form-item label="生成类型">
        <a-input v-model:value="searchParams.codeGenType" allow-clear placeholder="输入 codeGenType" />
      </a-form-item>
      <a-form-item label="部署键">
        <a-input v-model:value="searchParams.deployKey" allow-clear placeholder="输入 deployKey" />
      </a-form-item>
      <a-form-item label="优先级">
        <a-input-number v-model:value="searchParams.priority" placeholder="输入优先级" />
      </a-form-item>
      <a-form-item label="封面">
        <a-input v-model:value="searchParams.cover" allow-clear placeholder="输入封面地址" />
      </a-form-item>
      <a-form-item label="初始提示词">
        <a-input v-model:value="searchParams.initPrompt" allow-clear placeholder="输入初始提示词" />
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
          <a-image v-if="record.cover" :src="record.cover" :width="72" :height="48" style="object-fit: cover" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag :color="(record.priority ?? 0) >= FEATURED_APP_PRIORITY ? 'magenta' : 'blue'">
            {{ record.priority ?? 0 }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ formatDateTime(record.updateTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-button type="link" @click="goToDetail(record.id)">详情</a-button>
            <a-button type="link" @click="goToEdit(record.id)">编辑</a-button>
            <a-popconfirm title="确认删除该应用吗？" ok-text="删除" cancel-text="取消" @confirm="doDelete(record.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
            <a-button
              type="link"
              :disabled="(record.priority ?? 0) >= FEATURED_APP_PRIORITY"
              @click="doFeature(record)"
            >
              精选
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#app-manage-page {
  padding: 4px 0;
}

.search-form {
  row-gap: 12px;
}
</style>


