<template>
  <span :class="['growth', cls]">{{ tag.text }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { growthTag } from '@/components/charts/charts'

/** 增长率（%）：增长绿色上箭头，下降红色下箭头，没有基数时为 - */
const props = defineProps({ value: { type: [Number, String], default: null } })
const tag = computed(() => growthTag(props.value))
const cls = computed(() => (props.value == null ? '' : tag.value.color === 'green' ? 'up' : 'down'))
</script>

<style scoped>
.growth {
  white-space: nowrap;
}
.up {
  color: var(--status-success, #389e0d);
}
.down {
  color: var(--status-critical, #cf1322);
}
</style>
