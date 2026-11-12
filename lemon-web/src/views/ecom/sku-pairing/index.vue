<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="tab" class="pairing-tabs">
        <template #rightExtra>
          <a-space wrap>
            <a-button v-perm="'ecom:pairing:import'" @click="importOpen = true">
              <template #icon><ImportOutlined /></template>{{ $t('ecomCatalog.pairing.import') }}
            </a-button>
            <a-button v-perm="'ecom:order:outbound'" :loading="rematching" @click="rematch">
              <template #icon><SyncOutlined /></template>{{ $t('ecomCatalog.pairing.rematch') }}
            </a-button>
          </a-space>
        </template>
        <a-tab-pane key="unpaired" :tab="$t('ecomCatalog.pairing.unpairedTab')">
          <UnpairedTab ref="unpairedRef" @changed="onChanged" />
        </a-tab-pane>
        <a-tab-pane key="material" :tab="$t('ecomCatalog.pairing.materialTab')">
          <MaterialTab ref="materialRef" @changed="onChanged" />
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <ImportModal
      v-model:open="importOpen"
      :title="$t('ecomCatalog.pairing.importTitle')"
      :columns="importColumns"
      :importer="importPairs"
      :sample="importSample"
      @success="onChanged"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal } from 'ant-design-vue'
import { ImportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import UnpairedTab from './UnpairedTab.vue'
import MaterialTab from './MaterialTab.vue'
import { importPairs, rematchExceptions } from '@/api/ecom/pairing'

/**
 * SKU 配对中心：未配对的平台 SKU（推荐物料、一键确认）、以物料为中心看它在各店铺 / 平台的在线商品（解除配对）、
 * 批量导入配对、配对变化后重新匹配异常订单
 */
const { t } = useI18n()
const tab = ref('unpaired')
const unpairedRef = ref()
const materialRef = ref()

/** 配对变化后刷新两个标签页，并提示可以重新匹配异常订单 */
function onChanged() {
  unpairedRef.value?.reload()
  materialRef.value?.reload()
}

const rematching = ref(false)
async function rematch() {
  rematching.value = true
  try {
    const r = await rematchExceptions()
    Modal.info({ title: t('ecomCatalog.pairing.rematchDone'), content: t('ecomCatalog.pairing.rematchResult', { matched: r.matched, remaining: r.remaining }) })
  } finally {
    rematching.value = false
  }
}

const importOpen = ref(false)
const importColumns = [
  { title: t('ecom.common.storeCode'), key: 'storeCode', required: true },
  { title: t('ecom.common.sellerSku'), key: 'sellerSku', required: true },
  { title: t('ecom.listing.materialCode'), key: 'materialCode', required: true },
  { title: t('ecomCatalog.pairing.qty'), key: 'qty', type: 'number' }
]
const importSample = { storeCode: 'US01', sellerSku: 'SKU-0001', materialCode: 'M0001', qty: 1 }
</script>

<style scoped>
.pairing-tabs {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.pairing-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
}
.pairing-tabs :deep(.ant-tabs-content),
.pairing-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
}
</style>
