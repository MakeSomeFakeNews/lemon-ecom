<template>
  <span v-if="!health || !health.supported" class="sub-text">{{ $t('ecom.hub.channel.capUnsupported') }}</span>
  <a-tooltip v-else :title="tooltip">
    <div class="cell" @click="emit('click')">
      <span v-if="health.success === true" class="ok"><CheckCircleFilled /> {{ $t('ecom.hub.log.success') }}</span>
      <span v-else-if="health.success === false" class="bad"><CloseCircleFilled /> {{ $t('ecom.hub.log.failed') }}</span>
      <span v-else class="sub-text"><MinusCircleOutlined /> {{ $t('ecom.hub.channel.never') }}</span>
      <a-badge v-if="health.failures24h" :count="health.failures24h" :number-style="{ backgroundColor: '#ff4d4f' }" class="badge" />
      <div v-if="health.time" class="sub-text" :title="health.time">{{ relativeTime(health.time) }}</div>
      <div v-else-if="off" class="sub-text">{{ off }}</div>
    </div>
  </a-tooltip>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCircleFilled, CloseCircleFilled, MinusCircleOutlined } from '@ant-design/icons-vue'
import { relativeTime } from '../store/health'

/** 渠道总览中一类同步的状态：最近一次成功 / 失败 / 未执行、时间、近 24 小时失败次数；悬停显示失败原因，点击查看日志 */
const props = defineProps({
  health: { type: Object, default: undefined },
  /** 自动同步开关关闭时的说明 */
  off: { type: String, default: undefined }
})
const emit = defineEmits(['click'])
const { t } = useI18n()

const tooltip = computed(() => {
  const h = props.health
  const parts = []
  if (h?.time) parts.push(h.time)
  if (h?.message) parts.push(h.message)
  if (h?.failures24h) parts.push(t('ecom.hub.channel.fail24h', { n: h.failures24h }))
  if (props.off) parts.push(props.off)
  return parts.join(' · ') || undefined
})
</script>

<style scoped>
.cell {
  cursor: pointer;
}
.ok {
  color: var(--ant-color-success, #52c41a);
}
.bad {
  color: var(--ant-color-error, #ff4d4f);
}
.badge {
  margin-left: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
