<template>
  <div class="table-page ecom-listing">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecom.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.listing.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_listing_status)" />
        </a-form-item>
        <a-form-item :label="$t('ecom.listing.fulfillment')" name="fulfillment">
          <a-select v-model:value="query.fulfillment" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_fulfillment)" />
        </a-form-item>
        <a-form-item :label="$t('ecom.common.material')" name="materialId">
          <MaterialSelect v-model:value="query.materialId" :placeholder="$t('ecom.listing.materialPh')" />
        </a-form-item>
        <a-form-item v-if="view === 'sku'" :label="$t('ecomCatalog.listing.composeType')" name="composeType">
          <a-select v-model:value="query.composeType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_compose_type)" />
        </a-form-item>
        <a-form-item name="unmappedOnly">
          <a-checkbox v-model:checked="query.unmappedOnly" @change="search">{{ $t('ecom.listing.unmappedOnly') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="reload">
        <a-segmented v-model:value="view" :options="viewOptions" @change="reload" />
        <a-button v-perm="'ecom:listing:add'" type="primary" @click="formRef.openCreate({ storeId: query.storeId ?? undefined })">
          <template #icon><PlusOutlined /></template>{{ $t('ecom.listing.add') }}
        </a-button>
        <a-button v-perm="'ecom:listing:import'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
        <a-button v-perm="'ecom:listing:edit'" @click="styleRef.open({ storeId: query.storeId ?? undefined })">
          <template #icon><ApartmentOutlined /></template>{{ $t('ecomCatalog.style.title') }}
        </a-button>
        <template v-if="view === 'sku' && selectedKeys.length">
          <a-divider type="vertical" />
          <span class="selected-hint">{{ $t('common.table.selected', { n: selectedKeys.length }) }}</span>
          <a-button @click="openFnsku">
            <template #icon><BarcodeOutlined /></template>{{ $t('ecomCatalog.fnsku.print') }}
          </a-button>
          <a-button v-perm="'ecom:priceChange:add'" @click="openBatchPrice">
            <template #icon><DollarOutlined /></template>{{ $t('ecomPublish.batch.button') }}
          </a-button>
          <a-button type="link" @click="selectedKeys = []">{{ $t('common.action.cancel') }}</a-button>
        </template>
      </TableToolbar>

      <SpuView v-if="view === 'spu'" ref="spuRef" :size="size" @edit="(r) => formRef.openEdit(r)"
               @map-style="(g) => styleRef.open({ storeId: g.storeId, parentItemId: g.parentItemId })" />
      <a-table
        v-else
        :row-selection="rowSelection"
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="(r) => (paired(r) ? '' : 'row-unmapped')"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <div class="product">
              <a-image v-if="record.imageUrl" :src="record.imageUrl" :width="48" :height="48" class="thumb" />
              <div v-else class="thumb empty"><PictureOutlined /></div>
              <div class="product-text">
                <a-tooltip :title="record.title" placement="topLeft" :mouse-enter-delay="0.4">
                  <a class="title-clamp" @click="openRow(record)">{{ record.title || record.sellerSku }}</a>
                </a-tooltip>
                <div v-if="record.variant" class="sub-text ellipsis" :title="record.variant">{{ record.variant }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'sku'">
            <div class="mono ellipsis" :title="record.sellerSku">{{ record.sellerSku }}</div>
            <div class="sub-text ellipsis" :title="[record.platformItemId, record.fnsku].filter(Boolean).join(' · ')">
              <a v-if="record.url && record.platformItemId" :href="record.url" target="_blank" rel="noopener" :title="$t('ecom.listing.openOnPlatform')">
                {{ record.platformItemId }} <ExportOutlined class="ext-icon" />
              </a>
              <span v-else>{{ record.platformItemId || '-' }}</span>
              <template v-if="record.fnsku"> · {{ record.fnsku }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'store'">
            <div class="ellipsis" :title="record.storeName">{{ record.storeName || '-' }}</div>
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'price'">
            <template v-if="record.price != null">
              <span class="currency">{{ record.currency || '' }}</span> <span class="amount">{{ price(record.price) }}</span>
            </template>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'platformQty'">
            <span v-if="record.platformQty != null" :class="{ 'zero-stock': Number(record.platformQty) <= 0 }">{{ qty(record.platformQty, 0) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'sales30'">
            <span v-if="record.sales30 != null" :class="{ 'sub-text': !Number(record.sales30) }">{{ qty(record.sales30, 0) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'fulfillment'">
            <DictTag type="ecom_fulfillment" :value="record.fulfillment" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_listing_status" :value="record.status" />
            <div v-if="record.stockLocked === 1" class="lock-line">
              <a-tooltip :title="$t('ecomPublish.batch.lockedTip')">
                <a-tag color="orange" :bordered="false" class="lock-tag"><LockOutlined /> {{ $t('ecomPublish.batch.locked') }}</a-tag>
              </a-tooltip>
              <a v-if="userStore.hasPerm('ecom:priceChange:add')" @click="unlockStock(record)">{{ $t('ecomPublish.batch.unlock') }}</a>
            </div>
          </template>
          <template v-else-if="column.key === 'material'">
            <template v-if="record.composeType === ListingComposeType.BUNDLE && record.bundleItems?.length">
              <a-tag color="purple" :bordered="false">{{ $t('ecomCatalog.listing.bundle') }}</a-tag>
              <div class="sub-text ellipsis" :title="bundleText(record)">{{ bundleText(record) }}</div>
            </template>
            <template v-else-if="record.materialId">
              <div class="ellipsis" :title="record.materialCode">{{ record.materialCode }}</div>
              <div class="sub-text ellipsis" :title="materialLabel(record)">{{ record.materialName }}{{ record.materialSpec ? `（${record.materialSpec}）` : '' }}</div>
            </template>
            <template v-else>
              <a-tag color="warning" :bordered="false"><WarningOutlined /> {{ $t('ecom.common.unmapped') }}</a-tag>
              <a v-if="userStore.hasPerm('ecom:listing:edit')" @click="mapRef.open(record)">{{ $t('ecom.listing.mapMaterial') }}</a>
            </template>
          </template>
          <template v-else-if="column.key === 'packQty'">{{ record.composeType === ListingComposeType.BUNDLE ? '-' : qty(record.packQty) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ListingForm ref="formRef" @success="reload" />
    <ListingMapModal ref="mapRef" @success="reload" />
    <StyleMapModal ref="styleRef" @success="reload" />
    <FnskuLabelModal v-model:open="fnskuOpen" :items="fnskuItems" />
    <BatchPriceModal ref="batchPriceRef" @success="reload" />
    <ImportModal
      v-model:open="importOpen"
      :title="$t('ecom.listing.listing')"
      :columns="importColumns"
      :importer="importListing"
      :sample="importSample"
      @success="search"
    />
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApartmentOutlined, BarcodeOutlined, DollarOutlined, ExportOutlined, ImportOutlined, LinkOutlined, LockOutlined, PictureOutlined, PlusOutlined, WarningOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import ListingForm from './ListingForm.vue'
import ListingMapModal from './ListingMapModal.vue'
import SpuView from './SpuView.vue'
import StyleMapModal from './StyleMapModal.vue'
import FnskuLabelModal from '@/components/ecom/FnskuLabelModal.vue'
import BatchPriceModal from './BatchPriceModal.vue'
import { lockListingStock } from '@/api/ecom/publish'
import { bundleText, materialLabel } from './listing'
import { ListingComposeType } from '@/constants/enums'
import { deleteListing, importListing, pageListing } from '@/api/ecom/listing'
import { getMaterial } from '@/api/wms/material'
import { useRoute, useRouter } from 'vue-router'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { price, qty } from '@/utils/number'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_fulfillment', 'ecom_listing_status', 'ecom_compose_type')
/** 已配对：单品映射了物料，或组合有组成 */
const paired = (r) => (r.composeType === ListingComposeType.BUNDLE ? !!r.bundleItems?.length : !!r.materialId)

const columns = [
  {
    title: t('ecom.listing.product'),
    key: 'title',
    dataIndex: 'title',
    width: 320,
    fixed: 'left',
    exportColumns: [
      { title: t('ecom.listing.title'), dataIndex: 'title' },
      { title: t('ecom.listing.variant'), dataIndex: 'variant' }
    ]
  },
  {
    title: t('ecom.listing.skuAsin'),
    key: 'sku',
    dataIndex: 'sellerSku',
    width: 190,
    exportColumns: [
      { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku' },
      { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId' },
      { title: 'FNSKU', dataIndex: 'fnsku' }
    ]
  },
  { title: t('ecom.common.store'), key: 'store', dataIndex: 'storeName', width: 150 },
  { title: t('ecom.listing.price'), key: 'price', dataIndex: 'price', width: 120, align: 'right' },
  { title: t('ecom.listing.stock'), key: 'platformQty', dataIndex: 'platformQty', width: 100, align: 'right' },
  { title: t('ecom.listing.sales30'), key: 'sales30', dataIndex: 'sales30', width: 110, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_listing_status' },
  { title: t('ecom.listing.fulfillment'), key: 'fulfillment', dataIndex: 'fulfillment', width: 110, dict: 'ecom_fulfillment' },
  {
    title: t('ecom.common.material'),
    key: 'material',
    width: 220,
    exportColumns: [
      { title: t('ecom.listing.materialCode'), dataIndex: 'materialCode' },
      { title: t('ecom.listing.materialName'), dataIndex: 'materialName' }
    ]
  },
  { title: t('ecom.common.packQty'), key: 'packQty', dataIndex: 'packQty', width: 90, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: { showTitle: true } },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-listing')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, keyword: undefined, status: undefined, fulfillment: undefined, materialId: undefined, composeType: undefined,
  unmappedOnly: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search: searchSku, onTableChange, exportAll } = useTable(pageListing, query, {
  // 「只看未映射」→ mapped=false
  params: ({ unmappedOnly, ...rest }) => ({ ...rest, mapped: unmappedOnly ? false : undefined })
})
onMounted(load)

/** 视图：SKU（每个平台 SKU 一行）/ SPU（按父商品分组，展开看变体） */
const view = ref('sku')
const viewOptions = [
  { value: 'sku', label: t('ecomCatalog.spu.skuView') },
  { value: 'spu', label: t('ecomCatalog.spu.spuView') }
]
const spuRef = ref()
const spuQuery = () => ({ storeId: query.storeId, keyword: query.keyword, mapped: query.unmappedOnly ? false : undefined })
function search() {
  if (view.value === 'spu') nextTick(() => spuRef.value?.load(spuQuery()))
  else searchSku()
}
const reload = () => (view.value === 'spu' ? nextTick(() => spuRef.value?.load(spuQuery())) : load())
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const styleRef = ref()
// FNSKU 标签：勾选的在线商品各打一张（可在弹窗中改份数）
const fnskuOpen = ref(false)
const fnskuItems = ref([])
// 批量改价 / 改库存：勾选的在线商品
const batchPriceRef = ref()
const unlockStock = useAction((r) => lockListingStock(r.id, false), { confirm: () => t('ecomPublish.batch.unlockConfirm'), reload: () => reload() })
const openBatchPrice = () => batchPriceRef.value.open(list.value.filter((r) => selectedKeys.value.includes(r.id)))

function openFnsku() {
  fnskuItems.value = list.value.filter((r) => selectedKeys.value.includes(r.id)).map((r) => ({ fnsku: r.fnsku, sellerSku: r.sellerSku, title: r.title, qty: 1 }))
  fnskuOpen.value = true
}

const formRef = ref()
// 从开发项目「新建在线商品」跳转过来（?newMaterialId=物料ID）：打开新增表单并带出物料与标题
const route = useRoute()
const router = useRouter()
onMounted(async () => {
  const materialId = Number(route.query.newMaterialId)
  if (!materialId) return
  router.replace({ query: { ...route.query, newMaterialId: undefined } })
  const m = await getMaterial(materialId)
  await nextTick()
  formRef.value?.openCreate({ materialId, materialLabel: `${m.code} ${m.name}`, title: m.name, imageUrl: m.imageUrl || undefined })
})
const mapRef = ref()
const openRow = (record) => (userStore.hasPerm('ecom:listing:edit') ? formRef.value.openEdit(record) : record.url && window.open(record.url, '_blank', 'noopener'))
const remove = useAction(deleteListing, { successKey: 'common.msg.deleteSuccess', reload: () => reload() })

const rowActions = (record) => [
  { type: 'edit', perm: 'ecom:listing:edit', onClick: () => formRef.value.openEdit(record) },
  { key: 'map', label: t('ecom.listing.mapMaterial'), icon: LinkOutlined, perm: 'ecom:listing:edit', onClick: () => mapRef.value.open(record) },
  { type: 'delete', perm: 'ecom:listing:remove', confirm: t('ecom.listing.confirmDelete', { sku: record.sellerSku }), onClick: () => remove(record.id) }
]

// 导入：按店铺编码、物料编码定位店铺和物料
const importOpen = ref(false)
const importColumns = [
  { title: t('ecom.common.storeCode'), key: 'storeCode', required: true },
  { title: t('ecom.common.sellerSku'), key: 'sellerSku', required: true },
  { title: t('ecom.common.platformItemId'), key: 'platformItemId' },
  { title: t('ecom.listing.title'), key: 'title' },
  { title: t('ecom.listing.variant'), key: 'variant' },
  { title: t('ecom.listing.price'), key: 'price', type: 'number' },
  { title: t('ecom.store.currency'), key: 'currency' },
  { title: t('ecom.listing.fulfillment'), key: 'fulfillment', dict: 'ecom_fulfillment' },
  { title: t('common.field.status'), key: 'status', dict: 'ecom_listing_status' },
  { title: 'FNSKU', key: 'fnsku' },
  { title: t('ecom.listing.materialCode'), key: 'materialCodeInput' },
  { title: t('ecom.common.packQty'), key: 'packQty', type: 'number' }
]
const importSample = { storeCode: 'US01', sellerSku: 'SKU-0001', platformItemId: 'B000000001', title: 'Sample Product', variant: 'Red / XL', price: 19.99, currency: 'USD', materialCodeInput: 'M0001', packQty: 1 }
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.product-text {
  flex: 1;
  min-width: 0;
}
.product :deep(.thumb),
.thumb {
  flex: none;
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--app-border);
}
.product :deep(.ant-image) {
  flex: none;
}
.thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--app-text-3);
  background: var(--app-bg);
}
.title-clamp {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
  word-break: break-word;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
}
.ext-icon {
  font-size: 11px;
}
.currency {
  margin-right: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
.amount {
  font-weight: 500;
}
.zero-stock {
  color: #cf1322;
}
.lock-line {
  margin-top: 4px;
  white-space: nowrap;
}
.selected-hint {
  color: var(--app-text-2);
}
</style>

<style>
/* 未映射物料的行高亮 */
.ecom-listing .ant-table-tbody > tr.row-unmapped > td {
  background: #fffbe6;
}
.ecom-listing .ant-table-tbody > tr.row-unmapped:hover > td {
  background: #fff7d6;
}
html[data-theme='dark'] .ecom-listing .ant-table-tbody > tr.row-unmapped > td {
  background: #2b2611;
}
html[data-theme='dark'] .ecom-listing .ant-table-tbody > tr.row-unmapped:hover > td {
  background: #3a3216;
}
</style>
