<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

import { getAppVoById } from '@/api/appController.ts'
import {
  inviteMember,
  listAppMemberByPage,
  removeMember,
  updateMemberRole,
} from '@/api/appMemberController.ts'
import UserInfo from '@/components/UserInfo.vue'
import { userLoginStore } from '@/stores/loginUser.ts'
import { formatTime } from '@/utils/time.ts'
import {
  APP_MEMBER_ROLE,
  APP_MEMBER_ROLE_OPTIONS,
  APP_MEMBER_STATUS,
  APP_MEMBER_STATUS_OPTIONS,
  canAccessAppMembers,
  formatAppMemberRole,
  formatAppMemberStatus,
  getAppMemberRoleColor,
  getAppMemberStatusColor,
  isAppOwner,
} from '@/utils/appMembers.ts'

type TablePage = {
  current?: number
  pageSize?: number
}

const route = useRoute()
const router = useRouter()
const loginUserStore = userLoginStore()

const columns = [
  {
    title: '成员记录 ID',
    dataIndex: 'id',
    width: 110,
  },
  {
    title: '成员信息',
    dataIndex: 'user',
    width: 220,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    width: 110,
  },
  {
    title: '角色',
    dataIndex: 'memberRole',
    width: 150,
  },
  {
    title: '状态',
    dataIndex: 'memberStatus',
    width: 120,
  },
  {
    title: '邀请人 ID',
    dataIndex: 'invitedBy',
    width: 110,
  },
  {
    title: '最近活跃时间',
    dataIndex: 'lastActiveTime',
    width: 180,
  },
  {
    title: '加入时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    fixed: 'right',
    width: 320,
  },
]

const appId = computed(() => {
  return typeof route.params.id === 'string' ? route.params.id : ''
})
const appInfo = ref<API.AppVO>()
const data = ref<API.AppMemberVO[]>([])
const totalRow = ref(0)
const loading = ref(false)
const appLoading = ref(false)
const inviteVisible = ref(false)
const inviting = ref(false)
const roleSubmittingMap = reactive<Record<number, boolean>>({})
const deletingMap = reactive<Record<number, boolean>>({})
const memberRoleDraftMap = reactive<Record<number, string>>({})

const searchParams = reactive<API.AppMemberQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  appId: undefined,
  userId: undefined,
  memberRole: undefined,
  memberStatus: undefined,
})

const inviteForm = reactive<API.AppMemberInviteRequest>({
  appId: undefined,
  userId: undefined,
  memberRole: APP_MEMBER_ROLE.EDITOR,
})

const pagination = computed(() => ({
  current: searchParams.pageNum ?? 1,
  pageSize: searchParams.pageSize ?? 10,
  total: totalRow.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 条`,
}))

const isOwner = computed(() => {
  return isAppOwner(appInfo.value, loginUserStore.loginUser.id)
})

const myRoleText = computed(() => {
  if (isOwner.value) {
    return '拥有者'
  }
  return formatAppMemberRole(appInfo.value?.myMemberRole)
})

const myStatusText = computed(() => {
  if (isOwner.value) {
    return '已加入'
  }
  return formatAppMemberStatus(appInfo.value?.myMemberStatus)
})

const getApiAppId = () => {
  return appId.value as unknown as number
}

const roleOptions = APP_MEMBER_ROLE_OPTIONS
const statusOptions = APP_MEMBER_STATUS_OPTIONS

const syncRoleDrafts = (records: API.AppMemberVO[]) => {
  Object.keys(memberRoleDraftMap).forEach((key) => {
    delete memberRoleDraftMap[Number(key)]
  })
  records.forEach((record) => {
    if (record.id) {
      memberRoleDraftMap[record.id] = record.memberRole || APP_MEMBER_ROLE.VIEWER
    }
  })
}

const ensureCanView = (app?: API.AppVO) => {
  if (canAccessAppMembers(app, loginUserStore.loginUser.id)) {
    return true
  }
  message.error(
    app?.myMemberStatus === APP_MEMBER_STATUS.PENDING
      ? '请先接受邀请再查看成员'
      : '只有应用成员才能进入成员管理',
  )
  router.replace('/')
  return false
}

const fetchAppInfo = async () => {
  if (!appId.value) {
    message.error('应用 ID 不合法')
    router.replace('/')
    return false
  }

  appLoading.value = true
  try {
    const res = await getAppVoById({ id: getApiAppId() })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      return ensureCanView(res.data.data)
    }
    message.error(res.data.message || '获取应用信息失败')
    router.replace('/')
    return false
  } catch (error) {
    console.error(error)
    message.error('获取应用信息失败')
    router.replace('/')
    return false
  } finally {
    appLoading.value = false
  }
}

const fetchMembers = async () => {
  if (!appId.value) {
    return
  }

  loading.value = true
  try {
    const res = await listAppMemberByPage({
      ...searchParams,
      appId: getApiAppId(),
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records ?? []
      data.value = records
      totalRow.value = res.data.data.totalRow ?? 0
      syncRoleDrafts(records)
    } else {
      message.error('获取成员列表失败，' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('获取成员列表失败')
  } finally {
    loading.value = false
  }
}

const initPage = async () => {
  const passed = await fetchAppInfo()
  if (!passed) {
    return
  }
  await fetchMembers()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchMembers()
}

const resetSearch = () => {
  searchParams.pageNum = 1
  searchParams.pageSize = 10
  searchParams.userId = undefined
  searchParams.memberRole = undefined
  searchParams.memberStatus = undefined
  fetchMembers()
}

const doTableChange = (page: TablePage) => {
  searchParams.pageNum = page.current ?? 1
  searchParams.pageSize = page.pageSize ?? 10
  fetchMembers()
}

const goToAppChat = () => {
  if (!appId.value) {
    return
  }
  router.push(`/app/chat/${appId.value}`)
}

const openInviteModal = () => {
  inviteForm.userId = undefined
  inviteForm.memberRole = APP_MEMBER_ROLE.EDITOR
  inviteVisible.value = true
}

const handleInvite = async () => {
  if (!appId.value) {
    return
  }
  if (!inviteForm.userId) {
    message.warning('请输入被邀请用户 ID')
    return
  }

  inviting.value = true
  try {
    const res = await inviteMember({
      appId: getApiAppId(),
      userId: inviteForm.userId,
      memberRole: inviteForm.memberRole,
    })
    if (res.data.code === 0) {
      message.success('邀请成功，等待对方接受')
      inviteVisible.value = false
      await fetchMembers()
    } else {
      message.error('邀请失败，' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('邀请失败')
  } finally {
    inviting.value = false
  }
}

const canEditRole = (record: API.AppMemberVO) => {
  return (
    isOwner.value &&
    record.id != null &&
    record.userId !== appInfo.value?.userId &&
    record.memberStatus === APP_MEMBER_STATUS.ACTIVE
  )
}

const canDeleteMember = (record: API.AppMemberVO) => {
  return isOwner.value && record.id != null && record.userId !== appInfo.value?.userId
}

const updateRole = async (record: API.AppMemberVO) => {
  if (!record.userId || !appId.value || !record.id) {
    return
  }

  const nextRole = memberRoleDraftMap[record.id]
  if (!nextRole || nextRole === record.memberRole) {
    message.info('成员角色未发生变化')
    return
  }

  roleSubmittingMap[record.id] = true
  try {
    const res = await updateMemberRole({
      appId: getApiAppId(),
      userId: record.userId,
      memberRole: nextRole,
    })
    if (res.data.code === 0) {
      message.success('成员角色更新成功')
      await initPage()
    } else {
      message.error('成员角色更新失败，' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('成员角色更新失败')
  } finally {
    roleSubmittingMap[record.id] = false
  }
}

const doDelete = async (record: API.AppMemberVO) => {
  if (!record.id) {
    return
  }

  deletingMap[record.id] = true
  try {
    const res = await removeMember({ id: record.id })
    if (res.data.code === 0) {
      message.success('成员已移除')
      await initPage()
    } else {
      message.error('移除成员失败，' + res.data.message)
    }
  } catch (error) {
    console.error(error)
    message.error('移除成员失败')
  } finally {
    deletingMap[record.id] = false
  }
}

onMounted(() => {
  initPage()
})
</script>

<template>
  <div id="app-member-manage-page">
    <div class="page-header">
      <div>
        <h1>{{ appInfo?.appName || '成员管理' }}</h1>
        <p>只有应用成员可查看成员列表，只有拥有者可邀请成员和调整权限。</p>
      </div>
      <a-space wrap>
        <a-tag
          :color="
            isOwner
              ? getAppMemberRoleColor(APP_MEMBER_ROLE.OWNER)
              : getAppMemberRoleColor(appInfo?.myMemberRole)
          "
        >
          我的角色：{{ myRoleText }}
        </a-tag>
        <a-tag
          :color="
            getAppMemberStatusColor(isOwner ? APP_MEMBER_STATUS.ACTIVE : appInfo?.myMemberStatus)
          "
        >
          当前状态：{{ myStatusText }}
        </a-tag>
        <a-button type="default" @click="goToAppChat">返回作品详情</a-button>
        <a-button v-if="isOwner" type="primary" @click="openInviteModal">邀请成员</a-button>
      </a-space>
    </div>

    <a-card :loading="appLoading" class="content-card">
      <a-form layout="inline" :model="searchParams" class="search-form" @finish="doSearch">
        <a-form-item label="用户 ID">
          <a-input
            v-model:value="searchParams.userId"
            placeholder="输入用户 ID"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="成员角色">
          <a-select
            v-model:value="searchParams.memberRole"
            allow-clear
            placeholder="选择成员角色"
            :options="[{ label: '拥有者', value: APP_MEMBER_ROLE.OWNER }, ...roleOptions]"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item label="成员状态">
          <a-select
            v-model:value="searchParams.memberStatus"
            allow-clear
            placeholder="选择成员状态"
            :options="statusOptions"
            style="width: 160px"
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
        :scroll="{ x: 1500 }"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'user'">
            <UserInfo :user="record.user" :size="32" />
          </template>
          <template v-else-if="column.dataIndex === 'memberRole'">
            <a-tag :color="getAppMemberRoleColor(record.memberRole)">
              {{ formatAppMemberRole(record.memberRole) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'memberStatus'">
            <a-tag :color="getAppMemberStatusColor(record.memberStatus)">
              {{ formatAppMemberStatus(record.memberStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'lastActiveTime'">
            <span>{{ record.lastActiveTime ? formatTime(record.lastActiveTime) : '暂无' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span>{{ record.createTime ? formatTime(record.createTime) : '暂无' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap>
              <template v-if="canEditRole(record)">
                <a-select
                  v-model:value="memberRoleDraftMap[record.id]"
                  :options="roleOptions"
                  style="width: 110px"
                />
                <a-button
                  type="primary"
                  ghost
                  size="small"
                  class="action-btn"
                  :loading="roleSubmittingMap[record.id]"
                  @click="updateRole(record)"
                >
                  更新角色
                </a-button>
              </template>
              <a-tag v-else color="default">
                {{ record.userId === appInfo?.userId ? '拥有者不可编辑' : '仅 owner 可编辑' }}
              </a-tag>
              <a-popconfirm
                v-if="canDeleteMember(record)"
                title="确认移除该成员吗？"
                ok-text="移除"
                cancel-text="取消"
                @confirm="doDelete(record)"
              >
                <a-button danger size="small" class="action-btn" :loading="deletingMap[record.id]">
                  移除成员
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="inviteVisible"
      title="邀请成员"
      ok-text="发送邀请"
      cancel-text="取消"
      :confirm-loading="inviting"
      @ok="handleInvite"
    >
      <a-form layout="vertical" :model="inviteForm">
        <a-form-item label="用户 ID" required>
          <a-input
            v-model:value="inviteForm.userId"
            placeholder="请输入被邀请用户 ID"
            style="width: 100%"
          />
          <div class="form-tip">可让对方在个人信息页查看自己的用户 ID。</div>
        </a-form-item>
        <a-form-item label="成员角色">
          <a-select
            v-model:value="inviteForm.memberRole"
            :options="roleOptions"
            placeholder="选择成员角色"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
#app-member-manage-page {
  width: 100%;
  padding: 8px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.page-header p {
  margin: 0;
  color: rgba(0, 0, 0, 0.45);
}

.content-card {
  border-radius: 18px;
}

.search-form {
  row-gap: 12px;
}

.action-btn {
  min-width: 88px;
  border-radius: 999px;
}

.form-tip {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
}
</style>
