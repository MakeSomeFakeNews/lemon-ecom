<template>
  <div class="profit-card">
    <div class="row head">
      <span>{{ $t('ecomOrder.order.totalBase') }}</span>
      <span class="val">{{ baseMoney(detail.totalBase) }}</span>
    </div>
    <div v-for="r in deductions" :key="r.key" class="row">
      <span class="label">
        {{ r.label }}
        <a-tag v-if="r.key === 'platformFee'" :bordered="false" :color="detail.feeActual === 1 ? 'green' : 'default'" class="fee-tag">
          {{ detail.feeActual === 1 ? $t('ecomOrder.order.feeActual') : $t('ecomOrder.order.feeEstimated') }}
        </a-tag>
      </span>
      <span class="val minus">
        −{{ baseMoney(r.base) }}
        <span v-if="r.fc != null" class="fc">{{ fcMoney(detail.currency, r.fc) }}</span>
      </span>
    </div>
    <a-divider class="divider" />
    <div class="row total">
      <span>{{ $t('ecomOrder.order.profit') }}</span>
      <span class="val" :class="{ negative: Number(detail.profit) < 0 }">
        {{ baseMoney(detail.profit) }}
        <span v-if="rate != null" class="rate">{{ rate }}%</span>
      </span>
    </div>

    <div class="est">
      <div class="est-title">
        {{ $t('ecomOrd.estimate.title') }}
        <a-tooltip :title="$t('ecomOrd.estimate.help')"><QuestionCircleOutlined class="help" /></a-tooltip>
      </div>
      <div class="row">
        <span class="label">{{ $t('ecomOrd.estimate.channel') }}</span>
        <span class="val">{{ detail.estChannelName || '-' }}<span v-if="detail.estWeight != null" class="fc">{{ Number(detail.estWeight) }} kg</span></span>
      </div>
      <div class="row">
        <span class="label">{{ $t('ecomOrd.estimate.shipping') }}</span>
        <span class="val">{{ detail.estShipping != null ? baseMoney(detail.estShipping) : $t('ecomOrd.estimate.noRate') }}</span>
      </div>
      <div class="row">
        <span class="label">{{ $t('ecomOrd.estimate.fee') }} / {{ $t('ecomOrd.estimate.cost') }}</span>
        <span class="val">{{ detail.estFee != null ? baseMoney(detail.estFee) : '-' }} / {{ detail.estCost != null ? baseMoney(detail.estCost) : '-' }}</span>
      </div>
      <div class="row">
        <span class="label">{{ $t('ecomOrd.estimate.profit') }}</span>
        <span class="val">
          <b v-if="detail.estProfit != null" :class="{ negative: Number(detail.estProfit) < 0 }">{{ baseMoney(detail.estProfit) }}</b><span v-else>-</span>
          <span v-if="detail.estProfitRate != null" class="rate" :class="{ negative: Number(detail.estProfitRate) < 0 }">{{ Number(detail.estProfitRate) }}%</span>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import { t } from '@/locales'
import { baseMoney, fcMoney } from './order'

/**
 * 订单详情的利润构成：本位币实付 − 原币费用（代扣税、VAT、佣金、配送费、退货处理费、其他费用、退款）× 汇率 − 商品成本 − 物流成本 = 利润
 * （与后端 EcomOrderService.profitOf 一致，金额为 0 的项不显示），下方为审单时的利润预估
 */
const props = defineProps({ detail: { type: Object, required: true } })

const deductions = computed(() => {
  const d = props.detail
  const rate = Number(d.exchangeRate) || 0
  const fc = (key, label) => ({ key, label, fc: Number(d[key]) || 0, base: (Number(d[key]) || 0) * rate })
  const base = (key, label) => ({ key, label, fc: null, base: Number(d[key]) || 0 })
  return [
    fc('withheldTax', t('ecomProfit.col.withheldTax')),
    fc('vatAmount', t('ecomOrdUi.detail.vat')),
    fc('platformFee', t('ecomOrder.order.platformFee')),
    fc('fulfillmentFee', t('ecomOrder.order.fulfillmentFee')),
    fc('returnFee', t('ecomProfit.col.returnFee')),
    fc('otherFee', t('ecomOrder.order.otherFee')),
    fc('refundAmount', t('ecomOrder.order.refundAmount')),
    base('costAmount', t('ecomOrder.order.costAmount')),
    base('shippingCost', t('ecomOrder.order.shippingCost'))
  ].filter((r) => r.base || r.key === 'platformFee' || r.key === 'costAmount')
})
const rate = computed(() => {
  const total = Number(props.detail.totalBase)
  return total ? Math.round((Number(props.detail.profit) / total) * 10000) / 100 : null
})
</script>

<style scoped>
.row {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  align-items: baseline;
  padding: 3px 0;
}
.label {
  color: var(--app-text-2);
}
.val {
  text-align: right;
  white-space: nowrap;
}
.minus {
  color: var(--app-text);
}
.fc {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.head .val {
  font-weight: 600;
}
.divider {
  margin: 8px 0;
}
.total {
  font-weight: 600;
  font-size: 15px;
}
.rate {
  margin-left: 6px;
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.negative {
  color: var(--ant-color-error, #ff4d4f);
}
.fee-tag {
  margin-left: 4px;
  font-size: 11px;
  line-height: 16px;
}
.est {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--app-hover);
}
.est-title {
  margin-bottom: 4px;
  font-weight: 500;
}
.help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
</style>
