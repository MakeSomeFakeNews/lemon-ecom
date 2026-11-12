<template>
  <span v-if="order.estProfit == null">-</span>
  <a-tooltip v-else :title="tip">
    <div :class="{ negative: Number(order.estProfit) < 0 }" class="nowrap">{{ baseMoney(order.estProfit) }}</div>
    <div v-if="order.estProfitRate != null" class="sub-text" :class="{ negative: Number(order.estProfitRate) < 0 }">{{ Number(order.estProfitRate) }}%</div>
  </a-tooltip>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { baseMoney } from './order'

/** 订单列表的预估利润与利润率，悬停显示运费、平台费用、商品成本的预估 */
const props = defineProps({ order: { type: Object, required: true } })
const { t } = useI18n()
const tip = computed(() => {
  const o = props.order
  return t('ecomOrd.estimate.tip', {
    shipping: o.estShipping != null ? baseMoney(o.estShipping) : t('ecomOrd.estimate.noRate'),
    fee: baseMoney(o.estFee),
    cost: baseMoney(o.estCost),
    channel: o.estChannelName || '-'
  })
})
</script>

<style scoped>
.nowrap {
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.negative {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
