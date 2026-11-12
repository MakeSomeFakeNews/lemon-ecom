<template>
  <div class="table-page ecom-fba-fee">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="fee-tabs">
        <a-tab-pane key="audit" :tab="$t('ecomFba.audit.tabs.audit')" />
        <a-tab-pane key="dims" :tab="$t('ecomFba.audit.tabs.dims')" />
        <a-tab-pane key="tiers" :tab="$t('ecomFba.audit.tabs.tiers')" />
      </a-tabs>
      <AuditTab v-if="tab === 'audit'" />
      <DimTab v-else-if="tab === 'dims'" />
      <TierTab v-else />
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import AuditTab from './AuditTab.vue'
import DimTab from './DimTab.vue'
import TierTab from './TierTab.vue'

/** FBA 费用核查：配送费核查与生成索赔、商品尺寸、尺寸分段与费率（?tab=dims 直接打开尺寸） */
const route = useRoute()
const tab = ref(['audit', 'dims', 'tiers'].includes(route.query.tab) ? route.query.tab : 'audit')
</script>

<style scoped>
.fee-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
</style>

<style>
.ecom-fba-fee .tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
