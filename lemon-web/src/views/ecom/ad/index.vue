<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <StoreSelect v-model:value="query.storeId" allow-clear :placeholder="$t('ecomAds.common.allStores')" class="filter" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
        <a-button type="primary" @click="search">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
        </a-button>
        <a-tooltip :title="$t('ecomAds.metric.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" destroy-inactive-tab-pane>
        <a-tab-pane key="overview" :tab="$t('ecomAds.tabs.overview')">
          <AdOverview :params="params" @open-tab="(k) => (tab = k)" />
        </a-tab-pane>
        <a-tab-pane key="campaigns" :tab="$t('ecomAds.tabs.campaigns')">
          <AdCampaigns :params="params" />
        </a-tab-pane>
        <a-tab-pane key="searchTerms" :tab="$t('ecomAds.tabs.searchTerms')">
          <AdSearchTerms :params="params" />
        </a-tab-pane>
        <a-tab-pane key="tacos" :tab="$t('ecomAds.tabs.tacos')">
          <AdTacos :params="params" />
        </a-tab-pane>
        <a-tab-pane v-if="userStore.hasPerm('ecom:adRule:list')" key="rules" :tab="$t('ecomAds.tabs.rules')">
          <RuleList embedded @logs="tab = 'logs'" />
        </a-tab-pane>
        <a-tab-pane key="schedules" :tab="$t('ecomAds.tabs.schedules')">
          <AdSchedules :store-id="params.storeId" />
        </a-tab-pane>
        <a-tab-pane v-if="userStore.hasPerm('ecom:adRule:list')" key="logs" :tab="$t('ecomAds.tabs.logs')">
          <RuleLogs :store-id="params.storeId" />
        </a-tab-pane>
        <a-tab-pane key="daily" :tab="$t('ecomAds.tabs.daily')">
          <AdDaily :params="params" :active="tab === 'daily'" />
        </a-tab-pane>
        <a-tab-pane key="auth" :tab="$t('ecomAds.tabs.auth')">
          <AdAuth />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { InfoCircleOutlined, SearchOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { useRoute } from 'vue-router'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { rangePresets } from '@/components/ecom/ecomReport'
import { useUserStore } from '@/store/user'
import AdOverview from './AdOverview.vue'
import AdCampaigns from './AdCampaigns.vue'
import AdSearchTerms from './AdSearchTerms.vue'
import AdTacos from './AdTacos.vue'
import AdSchedules from './AdSchedules.vue'
import AdDaily from './AdDaily.vue'
import AdAuth from './AdAuth.vue'
import RuleList from '../ad-rule/RuleList.vue'
import RuleLogs from '../ad-rule/RuleLogs.vue'

/**
 * 广告管理：概览（指标卡含环比 / 同比、趋势）、活动（活动 → 广告组 → 投放的层级报表与维护、导入报告、同步）、搜索词（零转化建议否定）、
 * SKU 与 TACOS、规则、分时策略、执行日志、日报明细、广告授权。店铺与日期范围在顶部共用，点「查询」后生效（params 替换为新对象）
 */
const userStore = useUserStore()
const presets = rangePresets()
// 看板「广告同步失败」等跳转时按 ?tab= 打开页签
const tab = ref(useRoute().query.tab ? String(useRoute().query.tab) : 'overview')
/** 默认近 30 天，截至昨天（广告报告一般次日才有） */
const yesterday = dayjs().subtract(1, 'day')
const query = reactive({ storeId: undefined, range: [yesterday.subtract(29, 'day').format('YYYY-MM-DD'), yesterday.format('YYYY-MM-DD')] })
const toParams = () => ({ storeId: query.storeId || undefined, beginDate: query.range?.[0], endDate: query.range?.[1] })
const params = ref(toParams())

function search() {
  params.value = toParams()
}
// 店铺、日期变化即查询
watch(() => [query.storeId, query.range?.[0], query.range?.[1]], search)
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
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
}
</style>
