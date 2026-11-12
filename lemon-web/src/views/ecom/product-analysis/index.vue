<template>
  <div class="product-analysis-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab" class="tabs" destroy-inactive-tab-pane>
        <a-tab-pane key="abc" :tab="$t('ecomAnalytics.product.tabAbc')"><AbcTab /></a-tab-pane>
        <a-tab-pane key="slow" :tab="$t('ecomAnalytics.product.tabSlow')"><SlowTab /></a-tab-pane>
        <a-tab-pane key="sellThrough" :tab="$t('ecomAnalytics.product.tabSellThrough')"><SellThroughTab /></a-tab-pane>
        <a-tab-pane key="returns" :tab="$t('ecomAnalytics.product.tabReturns')"><ReturnTab /></a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AbcTab from './AbcTab.vue'
import SlowTab from './SlowTab.vue'
import SellThroughTab from './SellThroughTab.vue'
import ReturnTab from './ReturnTab.vue'

/**
 * 商品分析（GET /ecom/analytics/product/*，金额为本位币）：ABC、滞销与库龄（含动销率）、售罄与周转（含断货天数、新品表现）、退货率排行，
 * 每个标签页有自己的筛选条件、图表、表格与导出（按钮权限 ecom:productAnalysis:export）
 */
const tab = ref('abc')
</script>

<style scoped>
.tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
</style>
