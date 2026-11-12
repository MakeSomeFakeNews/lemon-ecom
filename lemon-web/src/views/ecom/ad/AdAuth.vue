<template>
  <a-alert type="info" show-icon :message="$t('ecomAds.auth.tip')" class="tip" />
  <a-table row-key="storeId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1100 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'storeName'">
        {{ record.storeName }}
        <div class="sub-text">{{ record.storeCode }} · <DictTag type="ecom_platform" :value="record.platform" /></div>
      </template>
      <template v-else-if="column.key === 'auth'">
        <a-tag v-if="!record.connectorSupported">{{ $t('ecomAds.auth.unsupported') }}</a-tag>
        <a-tag v-else-if="record.hasCredentials" color="green">{{ $t('ecomAds.auth.configured') }}</a-tag>
        <a-tag v-else color="orange">{{ $t('ecomAds.auth.notConfigured') }}</a-tag>
        <a-tag v-if="record.adsSandbox === 1" color="purple">{{ $t('ecomAds.auth.sandbox') }}</a-tag>
      </template>
      <template v-else-if="column.key === 'adsSync'">{{ record.adsSync === 0 ? $t('ecomAds.common.no') : $t('ecomAds.common.yes') }}</template>
      <template v-else-if="column.key === 'lastSync'">
        {{ record.adsLastSyncTime || '-' }}
        <div v-if="record.adsLastSyncMessage" class="error-text">{{ record.adsLastSyncMessage }}</div>
      </template>
      <template v-else-if="column.key === 'action'">
        <TableActions :actions="actions(record)" />
      </template>
    </template>
  </a-table>
  <AdAuthForm ref="formRef" :connectors="connectors" @success="load" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { adsAuthStores, adsConnectors, clearAdsAuth, syncAds, syncAdsStructure, testAdsAuth } from '@/api/ecom/ads'
import { useAction } from '@/composables/useAction'
import { t } from '@/locales'
import AdAuthForm from './AdAuthForm.vue'

/**
 * 广告授权页签：各店铺的广告接口授权状态、环境、配置文件、最近同步与失败原因；授权、测试、同步（后台执行）、同步结构、清除授权。
 * 授权与店铺的渠道授权分开保存，不经过店铺页面
 */
const rows = ref([])
const connectors = ref([])
const loading = ref(false)
const formRef = ref()
const columns = [
  { title: t('ecomAds.common.store'), key: 'storeName', width: 220 },
  { title: t('ecomAds.auth.connector'), key: 'auth', width: 180 },
  { title: t('ecomAds.auth.profile'), dataIndex: 'adsProfileId', key: 'adsProfileId', width: 160 },
  { title: t('ecomAds.auth.sync'), key: 'adsSync', width: 110 },
  { title: t('ecomAds.auth.lastSync'), key: 'lastSync', width: 280 },
  actionColumn(4)
]

async function load() {
  loading.value = true
  try {
    const [s, c] = await Promise.all([adsAuthStores(), adsConnectors()])
    rows.value = s || []
    connectors.value = c || []
  } finally {
    loading.value = false
  }
}
onMounted(load)

const test = useAction((r) => testAdsAuth(r.storeId), { successKey: 'ecomAds.auth.testOk' })
const sync = useAction((r) => syncAds(r.storeId, 7), { successKey: 'ecomAds.campaign.syncStarted', reload: () => load() })
const syncStructure = useAction((r) => syncAdsStructure(r.storeId), {
  successKey: (res) => t('ecomAds.campaign.syncStructureDone', { c: res.campaigns, g: res.adGroups, t: res.targets }),
  reload: () => load()
})
const clear = useAction((r) => clearAdsAuth(r.storeId), { successKey: 'common.msg.operationSuccess', reload: () => load() })

const actions = (r) => [
  { type: 'setting', label: t('ecomAds.auth.configure'), perm: 'ecom:ad:auth', show: r.connectorSupported, onClick: () => formRef.value.open(r) },
  { type: 'confirm', label: t('ecomAds.auth.test'), perm: 'ecom:ad:auth', show: r.hasCredentials, onClick: () => test(r) },
  { type: 'run', label: t('ecomAds.campaign.sync'), perm: 'ecom:ad:sync', show: r.hasCredentials, onClick: () => sync(r) },
  { type: 'list', label: t('ecomAds.campaign.syncStructure'), perm: 'ecom:ad:sync', show: r.hasCredentials, onClick: () => syncStructure(r) },
  { type: 'delete', label: t('ecomAds.auth.clear'), perm: 'ecom:ad:auth', show: r.hasCredentials, confirm: t('ecomAds.auth.clearConfirm', { name: r.storeName }), onClick: () => clear(r) }
]
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: #cf1322;
  font-size: 12px;
  white-space: normal;
}
</style>
