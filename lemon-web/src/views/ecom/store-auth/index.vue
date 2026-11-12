<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('ecomOper.auth.tip')" class="tip" />
      <a-tabs v-model:active-key="tab" class="page-tabs">
        <a-tab-pane key="store" :tab="$t('ecomOper.auth.tabStore')">
          <StoreTab v-if="loaded.store" ref="storeRef" />
        </a-tab-pane>
        <a-tab-pane key="user" :tab="$t('ecomOper.auth.tabUser')">
          <UserTab v-if="loaded.user" @changed="storeRef?.reload()" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import StoreTab from './StoreTab.vue'
import UserTab from './UserTab.vue'

/**
 * 店铺授权（店铺数据权限）：按店铺维护授权成员（运营、客服、助理），或按用户勾选授权的店铺。
 * 用户在电商各页面只能看到负责的店铺与被授权店铺的数据，超级管理员和全部数据权限的角色不受限制
 */
const tab = ref('store')
const loaded = reactive({ store: true })
const storeRef = ref()
watch(tab, (v) => (loaded[v] = true))
</script>

<style scoped>
.tip {
  margin-bottom: 4px;
}
.page-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.page-tabs :deep(.ant-tabs-content-holder),
.page-tabs :deep(.ant-tabs-content),
.page-tabs :deep(.ant-tabs-tabpane-active) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
</style>
