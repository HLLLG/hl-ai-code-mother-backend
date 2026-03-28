<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController.ts'
import { formatDateTime } from '@/utils/app.ts'

type TablePage = {
  current?: number
  pageSize?: number
}

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 90,
  },
  {
    title: '应用 id',
    dataIndex: 'appId',
    width: 120,
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 120,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 120,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    ellipsis: true,
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
]

const data = ref<API.ChatHistory[]>([])
const totalRow = ref(0)
const loading = ref(false)

const searchParams = reactive<API.ChatHistoryQueryRequest>({
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
    const res = await listAllChatHistoryByPageForAdmin({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
      totalRow.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取对话列表失败，' + res.data.message)
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
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.messageType = undefined
  searchParams.message = undefined
  searchParams.lastCreateTime = undefined
  fetchData()
}

const doTableChange = (page: TablePage) => {
  searchParams.pageNum = page.current ?? 1
  searchParams.pageSize = page.pageSize ?? 10
  fetchData()
}

const formatMessageType = (messageType?: string) => {
  const normalizedType = messageType?.toLowerCase()
  if (normalizedType === 'user') {
    return '用户'
  }
  if (normalizedType === 'ai' || normalizedType === 'assistant') {
    return 'AI'
  }
  return messageType || '未知'
}

const getMessageTypeColor = (messageType?: string) => {
  const normalizedType = messageType?.toLowerCase()
  if (normalizedType === 'user') {
    return 'blue'
  }
  if (normalizedType === 'ai' || normalizedType === 'assistant') {
    return 'purple'
  }
  return 'default'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="chat-history-manage-page">
    <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
      <a-form-item label="记录 id">
        <a-input-number v-model:value="searchParams.id" placeholder="输入记录 id" style="width: 140px" />
      </a-form-item>
      <a-form-item label="应用 id">
        <a-input-number v-model:value="searchParams.appId" placeholder="输入应用 id" style="width: 140px" />
      </a-form-item>
      <a-form-item label="用户 id">
        <a-input-number v-model:value="searchParams.userId" placeholder="输入用户 id" style="width: 140px" />
      </a-form-item>
      <a-form-item label="消息类型">
        <a-select
          v-model:value="searchParams.messageType"
          allow-clear
          placeholder="选择消息类型"
          :options="[
            { label: '用户', value: 'user' },
            { label: 'AI', value: 'ai' },
          ]"
          style="width: 140px"
        />
      </a-form-item>
      <a-form-item label="消息内容">
        <a-input v-model:value="searchParams.message" allow-clear placeholder="输入消息内容" />
      </a-form-item>
      <a-form-item label="游标时间">
        <a-input
          v-model:value="searchParams.lastCreateTime"
          allow-clear
          placeholder="输入 lastCreateTime"
          style="width: 220px"
        />
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
      :scroll="{ x: 1200 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'messageType'">
          <a-tag :color="getMessageTypeColor(record.messageType)">
            {{ formatMessageType(record.messageType) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'message'">
          <span class="ellipsis-text" :title="record.message || '暂无消息内容'">
            {{ record.message || '暂无消息内容' }}
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ formatDateTime(record.updateTime) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#chat-history-manage-page {
  width: 100%;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 0;
}

.ellipsis-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
