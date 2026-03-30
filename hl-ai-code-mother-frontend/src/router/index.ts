import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import AppMemberManagePage from '@/pages/app/AppMemberManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
      meta: {
        fullWidth: true,
      },
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
      meta: {
        requiresAuth: true,
        adminOnly: true,
      },
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
      meta: {
        requiresAuth: true,
        adminOnly: true,
      },
    },
    {
      path: '/admin/chatHistoryManage',
      name: '对话管理',
      component: ChatHistoryManagePage,
      meta: {
        requiresAuth: true,
        adminOnly: true,
      },
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
      meta: {
        requiresAuth: true,
        fullWidth: true,
        requiresAppAccess: true,
      },
    },
    {
      path: '/app/members/:id',
      name: '成员管理',
      component: AppMemberManagePage,
      meta: {
        requiresAuth: true,
        requiresAppMember: true,
      },
    },
    {
      path: '/app/edit/:id',
      name: '应用编辑',
      component: AppEditPage,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/profile',
      name: '个人信息',
      component: UserProfilePage,
      meta: {
        requiresAuth: true,
      },
    },
  ],
})

export default router
