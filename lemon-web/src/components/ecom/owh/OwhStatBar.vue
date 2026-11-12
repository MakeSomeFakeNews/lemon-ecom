<template>
  <a-card :bordered="false" class="page-card owh-stat-card">
    <a-spin :spinning="loading" size="small">
      <div class="owh-stats">
        <div v-for="s in items" :key="s.key" class="owh-stat" :class="[s.tone, { clickable: !!s.onClick, active: s.active }]" @click="s.onClick?.()">
          <div class="owh-stat-label">{{ s.label }}</div>
          <div class="owh-stat-value" :class="{ small: s.small }" :title="String(s.value)">{{ s.value }}</div>
        </div>
      </div>
    </a-spin>
    <div v-if="$slots.default" class="owh-stat-tip"><slot /></div>
  </a-card>
</template>

<script setup>
/**
 * 海外仓页面顶部的统计条（库存、费用等）：items 为 [{ key, label, value（已格式化的文字）, tone: warn | danger | ok, small, onClick, active }]，
 * 有 onClick 的可点击（如点击「有差异的 SKU」只看差异）；默认插槽为下方的说明文字
 */
defineProps({ items: { type: Array, default: () => [] }, loading: Boolean })
</script>

<style scoped>
.owh-stat-card :deep(.ant-card-body) {
  padding: 12px 16px;
}
.owh-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}
.owh-stat {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--app-hover);
  border: 1px solid transparent;
  min-width: 0;
}
.owh-stat.clickable {
  cursor: pointer;
}
.owh-stat.clickable:hover,
.owh-stat.active {
  border-color: var(--app-primary-border, #91caff);
}
.owh-stat-label {
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.owh-stat-value {
  margin-top: 2px;
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-variant-numeric: tabular-nums;
}
.owh-stat-value.small {
  font-size: 14px;
  font-weight: 400;
}
.owh-stat.warn .owh-stat-value {
  color: var(--status-warning);
}
.owh-stat.danger .owh-stat-value {
  color: var(--status-critical);
}
.owh-stat.ok .owh-stat-value {
  color: #389e0d;
}
.owh-stat-tip {
  margin-top: 8px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
