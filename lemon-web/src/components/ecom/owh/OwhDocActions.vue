<template>
  <a-space wrap :size="8">
    <a-button
      v-for="a in ordered"
      :key="a.key"
      :type="a.primary ? 'primary' : 'default'"
      :danger="a.danger"
      :loading="a.loading"
      @click="a.run"
    >
      <template v-if="a.icon" #icon><component :is="a.icon" /></template>{{ a.label }}
    </a-button>
  </a-space>
</template>

<script setup>
/**
 * 海外仓单据详情顶部的操作按钮（SubPage extra 插槽中）：actions 为 [{ key, label, icon, primary, danger, loading, run }]，
 * 由详情按单据状态和权限计算（主要操作排在最右侧），危险操作的二次确认由 run 负责（页面的 useAction / 确认弹窗）
 */
import { computed } from 'vue'

const props = defineProps({ actions: { type: Array, default: () => [] } })
/** 主要操作排在最右侧 */
const ordered = computed(() => [...props.actions].sort((a, b) => Number(!!a.primary) - Number(!!b.primary)))
</script>
