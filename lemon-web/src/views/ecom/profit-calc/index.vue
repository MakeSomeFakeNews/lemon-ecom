<template>
  <div class="calc-page">
    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="calc" :tab="$t('ecomDev.calc.tabCalc')">
          <a-alert type="info" show-icon class="tip" :message="$t('ecomDev.calc.tip')" />
          <ProfitCalculator ref="calcRef" @saved="schemeTable?.reload()" />
          <div class="section-title">{{ $t('ecomDev.calc.schemes') }}</div>
          <SchemeTable ref="schemeTable" @load="loadScheme" />
        </a-tab-pane>
        <a-tab-pane key="compare" :tab="$t('ecomDev.calc.tabCompare')">
          <ProfitCompare ref="compareRef" />
        </a-tab-pane>
        <a-tab-pane key="rate" :tab="$t('ecomDev.calc.tabRate')">
          <RateTab />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import ProfitCalculator from '@/components/ecom/dev/ProfitCalculator.vue'
import ProfitCompare from '@/components/ecom/dev/ProfitCompare.vue'
import SchemeTable from '@/components/ecom/dev/SchemeTable.vue'
import RateTab from './RateTab.vue'

/**
 * 利润试算：单平台试算（正算利润 / 反算售价、保存方案）、多平台对比、平台佣金费率维护
 */
const tab = ref('calc')
const calcRef = ref()
const compareRef = ref()
const schemeTable = ref()

async function loadScheme(params) {
  tab.value = 'calc'
  await nextTick()
  calcRef.value?.load(params)
  window.scrollTo?.({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.section-title {
  margin: 20px 0 8px;
  font-weight: 600;
}
</style>
