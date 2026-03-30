// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /appMember/accept */
export async function acceptInvite(
  body: API.AppMemberAcceptRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/appMember/accept', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /appMember/delete */
export async function removeMember(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/appMember/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /appMember/invite */
export async function inviteMember(
  body: API.AppMemberInviteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/appMember/invite', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /appMember/list/page */
export async function listAppMemberByPage(
  body: API.AppMemberQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppMemberVO>('/appMember/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /appMember/update/role */
export async function updateMemberRole(
  body: API.AppMemberRoleUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/appMember/update/role', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
