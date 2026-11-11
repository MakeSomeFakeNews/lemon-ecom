<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomReport.common.allStores')" class="filter" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
        <a-input
          v-if="tab === 'daily'"
          v-model:value="query.keyword"
          allow-clear
          :placeholder="$t('ecomReport.ad.keywordPlaceholder')"
          class="filter"
          @press-enter="search"
        />
        <a-button type="primary" @click="search">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
        </a-button>
        <span class="sub-text">{{ $t('ecomReport.ad.acosTip') }}</span>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="summary" :tab="$t('ecomReport.ad.summary')">
          <AdSummary :params="params" :active="tab === 'summary'" />
        </a-tab-pane>
        <a-tab-pane key="daily" :tab="$t('ecomReport.ad.daily')">
          <AdDaily :params="params" :active="tab === 'daily'" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { lastDays, rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import AdSummary from './AdSummary.vue'
import AdDaily from './AdDaily.vue'

/**
 * 广告数据：「汇总」按店铺 / 广告活动 / SKU 汇总曝光、点击、花费、ACOS 等，「明细」为导入的广告日报。
 * 筛选条件在顶部共用，点「查询」后生效（params 替换为新对象，当前页签重新加载，另一个页签切换过去时再加载）
 */
const presets = rangePresets()
const tab = ref('summary')
/** 默认近 30 天 */
const query = reactive({ storeId: undefined, range: lastDays(30), keyword: '' })
const toParams = () => ({ storeId: query.storeId || undefined, ...rangeParams(query.range), keyword: query.keyword?.trim() || undefined })
const params = ref(toParams())

function search() {
  params.value = toParams()
}
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 220px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
