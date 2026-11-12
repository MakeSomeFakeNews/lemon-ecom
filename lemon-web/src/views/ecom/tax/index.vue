<template>
  <div class="tax-page">
    <a-card :bordered="false" class="page-card tabs-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="rates" :tab="$t('ecomProfit.tax.rates')" />
        <a-tab-pane key="vat" :tab="$t('ecomProfit.tax.vatSummary')" />
      </a-tabs>
    </a-card>
    <TaxRates v-if="tab === 'rates'" />
    <VatSummary v-else />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TaxRates from './TaxRates.vue'
import VatSummary from './VatSummary.vue'

/** 税务：税率设置（VAT / GST / 销售税，是否平台代扣代缴）与 VAT 汇总（按收件国家 × 月份） */
const tab = ref('rates')
</script>

<style scoped>
.tabs-card :deep(.ant-card-body) {
  padding-bottom: 0;
}
.tabs-card :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}
</style>
