// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /chatHistory/admin/list/page/vo */
export async function listAllChatHistoryByPageForAdmin(
  body: API.ChatHistoryQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatHistory>('/chatHistory/admin/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chatHistory/app/${param0} */
export async function listChatHistory(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listChatHistoryParams,
  options?: { [key: string]: any }
) {
  const { appId: param0, ...queryParams } = params
  return request<API.BaseResponsePageChatHistory>(`/chatHistory/app/${param0}`, {
    method: 'GET',
    params: {
      // pageSize has a default value: 10
      pageSize: '10',
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chatHistory/app/${param0}/export/md */
export async function downloadChatHistoryMd(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadChatHistoryMdParams,
  options?: { [key: string]: any }
) {
  const { appId: param0, ...queryParams } = params
  return request<string>(`/chatHistory/app/${param0}/export/md`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
