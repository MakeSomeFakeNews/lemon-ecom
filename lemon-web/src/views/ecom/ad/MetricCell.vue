<template>
  <span>
    {{ fmt(kind, record[field]) }}
    <span v-if="tag" :class="['delta', tag.color]">{{ tag.text }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { LOWER_BETTER, changeTag, fmt } from './adMetrics'

/** 指标单元格：值 + 环比（花费、销售额、ACOS 显示） */
const props = defineProps({
  record: { type: Object, required: true },
  field: { type: String, required: true },
  kind: { type: String, required: true }
})
const COMPARE = new Set(['spend', 'sales', 'acos'])
const tag = computed(() => (COMPARE.has(props.field) ? changeTag(props.record[props.field], props.record.prev?.[props.field], LOWER_BETTER.has(props.field)) : null))
</script>

<style scoped>
.delta {
  display: block;
  font-size: 11px;
  line-height: 1.2;
}
.delta.green {
  color: #389e0d;
}
.delta.red {
  color: #cf1322;
}
</style>
