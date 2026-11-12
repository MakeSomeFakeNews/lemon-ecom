<template>
  <a-steps class="owh-flow" size="small" :current="flow.current" :status="flow.status" :items="flow.items" />
</template>

<script setup>
/**
 * 海外仓单据详情的流程进度：flow 为 { items: [{ title, description }], current, status: process | finish | error }，
 * 由 owh-outbound/owh.js 中的 inboundFlow / outboundFlow / returnFlow / transferFlow 按单据计算
 */
defineProps({ flow: { type: Object, required: true } })
</script>

<style scoped>
.owh-flow {
  margin: 4px 0 20px;
  padding: 16px 24px;
  border-radius: 8px;
  background: var(--app-hover);
}
.owh-flow :deep(.ant-steps-item-description) {
  font-size: 12px;
  white-space: nowrap;
}
</style>
