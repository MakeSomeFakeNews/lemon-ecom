<template>
  <div class="progress-card">
    <a-steps :current="flow.current" :status="flow.status" :items="flow.items" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { InboundStatus } from '@/constants/enums'

/** 头程货件流程进度：创建（草稿 / 审批中）→ 已发运 → 在途 → 已签收；取消时在当前步骤显示错误 */
const props = defineProps({ detail: { type: Object, required: true } })
const { t } = useI18n()

const days = (from, to) => dayjs(to).startOf('day').diff(dayjs(from).startOf('day'), 'day')

const flow = computed(() => {
  const d = props.detail
  const s = d.status
  const created = d.createTime ? String(d.createTime).slice(0, 10) : ''
  let transitDesc = d.etaDate ? t('ecomOrder.inbound.flow.eta', { date: d.etaDate }) : ''
  if (s === InboundStatus.SHIPPED && d.etaDate) {
    const left = days(dayjs(), d.etaDate)
    transitDesc = left >= 0 ? t('ecomOrder.inbound.flow.daysLeft', { date: d.etaDate, n: left }) : t('ecomOrder.inbound.flow.overdue', { date: d.etaDate, n: -left })
  } else if (s === InboundStatus.RECEIVED && d.shipDate && d.receiveDate) {
    transitDesc = t('ecomOrder.inbound.flow.transitDays', { n: days(d.shipDate, d.receiveDate) })
  } else if (s === InboundStatus.SHIPPED && d.shipDate) {
    transitDesc = t('ecomOrder.inbound.flow.shippedDays', { n: days(d.shipDate, dayjs()) })
  }
  const items = [
    {
      title: s === InboundStatus.APPROVING ? t('ecomOrder.inbound.flow.approving') : t('ecomOrder.inbound.flow.draft'),
      description: s === InboundStatus.CANCELLED ? t('ecomOrder.inbound.cancelled') : created
    },
    {
      title: t('ecomOrder.inbound.flow.shipped'),
      description: d.shipDate && (s === InboundStatus.DRAFT || s === InboundStatus.APPROVING) ? t('ecomOrder.inbound.flow.planned', { date: d.shipDate }) : d.shipDate || ''
    },
    { title: t('ecomOrder.inbound.flow.transit'), description: transitDesc },
    { title: t('ecomOrder.inbound.flow.received'), description: d.receiveDate || '' }
  ]
  const map = {
    [InboundStatus.DRAFT]: { current: 0, status: 'process' },
    [InboundStatus.APPROVING]: { current: 0, status: 'process' },
    [InboundStatus.SHIPPED]: { current: 2, status: 'process' },
    [InboundStatus.RECEIVED]: { current: 3, status: 'finish' },
    [InboundStatus.CANCELLED]: { current: 0, status: 'error' }
  }
  return { items, ...(map[s] || { current: 0, status: 'process' }) }
})
</script>

<style scoped>
.progress-card {
  padding: 16px 24px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
}
</style>
