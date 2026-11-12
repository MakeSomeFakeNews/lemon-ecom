<template>
  <a-tooltip :title="label">
    <span class="platform-avatar" :style="{ background: brand.color, width: `${size}px`, height: `${size}px`, fontSize: `${Math.round(size * 0.4)}px` }">
      {{ brand.text }}
    </span>
  </a-tooltip>
</template>

<script setup>
import { computed } from 'vue'
import { useDict } from '@/composables/useDict'
import { platformBrand } from './health'

/** 平台图标：品牌色方块 + 缩写，悬浮显示平台名称 */
const props = defineProps({ platform: { type: String, default: undefined }, size: { type: Number, default: 32 } })
const dict = useDict('ecom_platform')
const brand = computed(() => platformBrand(props.platform))
const label = computed(() => (dict.ecom_platform || []).find((d) => d.value === props.platform)?.label || props.platform)
</script>

<style scoped>
.platform-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  letter-spacing: -0.5px;
  line-height: 1;
  user-select: none;
}
</style>
