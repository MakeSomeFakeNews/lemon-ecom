<template>
  <EcomDashboard v-if="ecom" />
  <Workbench v-else />
</template>

<script setup>
import { defineAsyncComponent } from 'vue'
import { useUserStore } from '@/store/user'

/**
 * 系统首页：有电商看板权限（ecom:dashboard:list）的用户直接显示电商看板（与 /ecom/dashboard 同一组件），
 * 其他用户显示工作台（通知公告、功能导航、待办、审批、消息）
 */
const EcomDashboard = defineAsyncComponent(() => import('@/views/ecom/dashboard/index.vue'))
const Workbench = defineAsyncComponent(() => import('./Workbench.vue'))
const ecom = useUserStore().hasPerm('ecom:dashboard:list')
</script>
