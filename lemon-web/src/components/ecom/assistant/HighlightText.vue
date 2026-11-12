<template>
  <span class="hl-text"><template v-for="(s, i) in parts" :key="i"><mark v-if="s.level" :class="s.level === BannedWordLevel.BLOCK ? 'block' : 'warn'">{{ s.text }}</mark><template v-else>{{ s.text }}</template></template></span>
</template>

<script setup>
import { computed } from 'vue'
import { BannedWordLevel } from '@/constants/enums'
import { segments } from './bannedWords'

/** 高亮显示命中的违禁词：阻止类为红色、提醒类为橙色。text 为原文，hits 为 checkText 的结果；maxLength 时只显示第一个命中附近的片段 */
const props = defineProps({
  text: { type: String, default: '' },
  hits: { type: Array, default: () => [] },
  maxLength: { type: Number, default: 0 }
})

const parts = computed(() => {
  let text = props.text || ''
  let hits = props.hits
  if (props.maxLength && text.length > props.maxLength) {
    const first = Math.min(...hits.flatMap((h) => h.ranges.map((r) => r.start)), text.length)
    const from = Math.max(0, first - Math.floor(props.maxLength / 3))
    const to = Math.min(text.length, from + props.maxLength)
    const prefix = from > 0 ? '…' : ''
    const shift = from - prefix.length
    text = prefix + text.slice(from, to) + (to < props.text.length ? '…' : '')
    hits = hits.map((h) => ({ ...h, ranges: h.ranges.map((r) => ({ start: r.start - shift, end: r.end - shift })).filter((r) => r.start >= prefix.length && r.end <= text.length) }))
  }
  return segments(text, hits)
})
</script>

<style scoped>
.hl-text {
  white-space: pre-wrap;
  word-break: break-word;
}
mark {
  padding: 0 2px;
  border-radius: 3px;
}
mark.block {
  background: rgba(245, 34, 45, 0.18);
  color: #cf1322;
  font-weight: 600;
}
mark.warn {
  background: rgba(250, 140, 22, 0.2);
  color: #d46b08;
}
</style>

<style>
html[data-theme='dark'] .hl-text mark.block {
  background: rgba(245, 34, 45, 0.3);
  color: #ff7875;
}
html[data-theme='dark'] .hl-text mark.warn {
  background: rgba(250, 140, 22, 0.3);
  color: #ffc069;
}
</style>
