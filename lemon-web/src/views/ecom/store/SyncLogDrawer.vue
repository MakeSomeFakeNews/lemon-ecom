<template>
  <a-drawer :open="visible" :title="$t('ecom.hub.logsTitle', { name: store?.name })" width="min(1080px, 100vw)" destroy-on-close @close="visible = false">
    <SyncLogTable v-if="store" :api="pageStoreSyncLogs" :store-id="store.id" />
  </a-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { pageStoreSyncLogs } from '@/api/ecom/store'
import SyncLogTable from '../channel/SyncLogTable.vue'

/** 一个店铺的同步日志：open(store) 打开 */
const visible = ref(false)
const store = ref()

function open(record) {
  store.value = record
  visible.value = true
}

defineExpose({ open })
</script>
