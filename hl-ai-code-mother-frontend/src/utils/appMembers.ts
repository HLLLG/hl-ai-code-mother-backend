export const APP_MEMBER_ROLE = {
  OWNER: 'owner',
  EDITOR: 'editor',
  VIEWER: 'viewer',
} as const

export const APP_MEMBER_STATUS = {
  PENDING: 0,
  ACTIVE: 1,
  REMOVED: 2,
  REJECTED: 3,
} as const

export const APP_MEMBER_ROLE_OPTIONS = [
  {
    label: '编辑者',
    value: APP_MEMBER_ROLE.EDITOR,
  },
  {
    label: '查看者',
    value: APP_MEMBER_ROLE.VIEWER,
  },
]

export const APP_MEMBER_STATUS_OPTIONS = [
  {
    label: '待接受',
    value: APP_MEMBER_STATUS.PENDING,
  },
  {
    label: '已加入',
    value: APP_MEMBER_STATUS.ACTIVE,
  },
  {
    label: '已移除',
    value: APP_MEMBER_STATUS.REMOVED,
  },
  {
    label: '已拒绝',
    value: APP_MEMBER_STATUS.REJECTED,
  },
]

export const formatAppMemberRole = (role?: string) => {
  switch (role) {
    case APP_MEMBER_ROLE.OWNER:
      return '拥有者'
    case APP_MEMBER_ROLE.EDITOR:
      return '编辑者'
    case APP_MEMBER_ROLE.VIEWER:
      return '查看者'
    default:
      return '未知角色'
  }
}

export const formatAppMemberStatus = (status?: number) => {
  switch (status) {
    case APP_MEMBER_STATUS.PENDING:
      return '待接受'
    case APP_MEMBER_STATUS.ACTIVE:
      return '已加入'
    case APP_MEMBER_STATUS.REMOVED:
      return '已移除'
    case APP_MEMBER_STATUS.REJECTED:
      return '已拒绝'
    default:
      return '未知状态'
  }
}

export const getAppMemberStatusColor = (status?: number) => {
  switch (status) {
    case APP_MEMBER_STATUS.PENDING:
      return 'gold'
    case APP_MEMBER_STATUS.ACTIVE:
      return 'green'
    case APP_MEMBER_STATUS.REMOVED:
      return 'default'
    case APP_MEMBER_STATUS.REJECTED:
      return 'red'
    default:
      return 'default'
  }
}

export const getAppMemberRoleColor = (role?: string) => {
  switch (role) {
    case APP_MEMBER_ROLE.OWNER:
      return 'purple'
    case APP_MEMBER_ROLE.EDITOR:
      return 'blue'
    case APP_MEMBER_ROLE.VIEWER:
      return 'cyan'
    default:
      return 'default'
  }
}

export const isAppOwner = (app: API.AppVO | undefined, loginUserId?: number) => {
  return !!app?.userId && !!loginUserId && app.userId === loginUserId
}

export const isPendingAppInvite = (app: API.AppVO | undefined) => {
  return app?.myMemberStatus === APP_MEMBER_STATUS.PENDING
}

export const isActiveAppMember = (app: API.AppVO | undefined) => {
  return app?.myMemberStatus === APP_MEMBER_STATUS.ACTIVE
}

export const canAccessAppDetail = (app: API.AppVO | undefined, loginUser?: API.LoginUserVO) => {
  if (!app || !loginUser?.id) {
    return false
  }
  if (loginUser.userRole === 'admin') {
    return true
  }
  return isAppOwner(app, loginUser.id) || isActiveAppMember(app)
}

export const canAccessAppMembers = (app: API.AppVO | undefined, loginUserId?: number) => {
  if (!app) {
    return false
  }
  return isAppOwner(app, loginUserId) || isActiveAppMember(app)
}
