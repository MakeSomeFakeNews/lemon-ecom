<template>
  <div class="tab-body">
    <a-form layout="inline" class="block">
      <a-form-item :label="$t('common.field.keyword')">
        <a-input v-model:value="query.keyword" :placeholder="$t('ecomCatalog.pairing.materialKeywordPh')" allow-clear class="w240" @press-enter="search" />
      </a-form-item>
      <a-form-item>
        <a-checkbox v-model:checked="query.pairedOnly" @change="search">{{ $t('ecomCatalog.pairing.pairedOnly') }}</a-checkbox>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="search">{{ $t('common.action.search') }}</a-button>
      </a-form-item>
    </a-form>
    <a-table
      row-key="materialId"
      size="middle"
      class="grow"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 900, y: '100%' }"
      :expandable="{ expandedRowKeys: expanded, onExpand }"
      @change="onTableChange"
    >
      <template #expandedRowRender="{ record }">
        <a-table row-key="key" size="small" :columns="listingColumns" :data-source="listings[record.materialId] || []" :loading="!listings[record.materialId]"
                 :pagination="false" :scroll="{ x: 860 }">
          <template #bodyCell="{ column, record: l }">
            <template v-if="column.key === 'store'">
              {{ l.storeName }} <DictTag v-if="l.platform" type="ecom_platform" :value="l.platform" />
            </template>
            <template v-else-if="column.key === 'kind'">
              <DictTag type="ecom_compose_type" :value="l.composeType || ListingComposeType.SINGLE" />
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(l.packQty) }}</template>
            <template v-else-if="column.key === 'status'"><DictTag type="ecom_listing_status" :value="l.status" /></template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm v-if="userStore.hasPerm('ecom:pairing:edit')" :title="$t('ecomCatalog.pairing.unpairConfirm', { sku: l.sellerSku })"
                            @confirm="unpair(record.materialId, l.id)">
                <a class="danger">{{ $t('ecomCatalog.pairing.unpair') }}</a>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text">{{ record.materialSpec }}</div>
        </template>
        <template v-else-if="column.key === 'listingCount'">{{ record.listingCount }}</template>
        <template v-else-if="column.key === 'bundleCount'">{{ record.bundleCount }}</template>
        <template v-else-if="column.key === 'storeCount'">{{ record.storeCount }}</template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { listingsOfMaterial, pagePairingMaterials, unpairListings } from '@/api/ecom/pairing'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { ListingComposeType } from '@/constants/enums'
import { qty } from '@/utils/number'

/** 以物料为中心：物料对应的在线商品数（单品）、组合数、店铺数，展开看各店铺 / 平台的平台 SKU，可解除配对。ref 调用 reload() */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const userStore = useUserStore()
useDict('ecom_platform', 'ecom_compose_type', 'ecom_listing_status')

const columns = [
  { title: t('ecomCatalog.pairing.material'), key: 'material', width: 300 },
  { title: t('ecomCatalog.pairing.listingCount'), key: 'listingCount', width: 130, align: 'right' },
  { title: t('ecomCatalog.pairing.bundleCount'), key: 'bundleCount', width: 130, align: 'right' },
  { title: t('ecomCatalog.pairing.storeCount'), key: 'storeCount', width: 130, align: 'right' }
]
const listingColumns = [
  { title: t('ecom.common.store'), key: 'store', width: 200 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 170 },
  { title: t('ecomCatalog.common.title'), dataIndex: 'title', ellipsis: true },
  { title: t('ecomCatalog.listing.composeType'), key: 'kind', width: 90 },
  { title: t('ecomCatalog.pairing.qty'), key: 'qty', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', width: 90 },
  { title: t('common.action.operation'), key: 'action', width: 90 }
]

const query = reactive({ keyword: undefined, pairedOnly: true })
const { list, loading, pagination, load, search, onTableChange } = useTable(pagePairingMaterials, query)
onMounted(load)
const expanded = ref([])
const listings = reactive({})

async function loadListings(materialId) {
  const rows = (await listingsOfMaterial(materialId)) || []
  listings[materialId] = rows.map((l, i) => ({ ...l, key: `${l.id}-${i}` }))
}
async function onExpand(open, record) {
  expanded.value = open ? [...expanded.value, record.materialId] : expanded.value.filter((k) => k !== record.materialId)
  if (open && !listings[record.materialId]) await loadListings(record.materialId)
}
async function unpair(materialId, listingId) {
  await unpairListings(listingId)
  message.success(t('ecomCatalog.pairing.unpaired'))
  await loadListings(materialId)
  emit('changed')
}
function reload() {
  Object.keys(listings).forEach((k) => delete listings[k])
  expanded.value = []
  return load()
}

defineExpose({ reload })
</script>

<style scoped>
.tab-body {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.grow {
  flex: 1;
  min-height: 0;
}
.block {
  margin-bottom: 12px;
  row-gap: 8px;
}
.w240 {
  width: 240px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
