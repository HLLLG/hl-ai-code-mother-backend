import { h } from 'vue'
import type { MenuProps } from 'ant-design-vue'
import { HomeOutlined, InfoCircleOutlined, LinkOutlined } from '@ant-design/icons-vue'

type AntdMenuItem = NonNullable<MenuProps['items']>[number]

type BaseMenuItem = Exclude<AntdMenuItem, null | undefined>

export type MenuItemConfig = BaseMenuItem & {
  path?: string
  href?: string
  target?: '_self' | '_blank' | '_parent' | '_top'
}

export const menuItems: MenuItemConfig[] = [
  {
    key: 'home',
    icon: () => h(HomeOutlined),
    label: '首页',
    title: '首页',
    path: '/',
  },
  {
    key: 'about',
    icon: () => h(InfoCircleOutlined),
    label: '关于我们',
    title: '关于我们',
    path: '/about',
  },
  {
    key: 'others',
    icon: () => h(LinkOutlined),
    label: '程序员HL',
    title: '程序员HL',
    href: 'https://github.com/HLLLG',
    target: '_blank',
  },
]
