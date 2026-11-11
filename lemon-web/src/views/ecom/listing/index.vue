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
        <a-form-item name="unmappedOnly">
          <a-checkbox v-model:checked="query.unmappedOnly" @change="search">{{ $t('ecom.listing.unmappedOnly') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:listing:add'" type="primary" @click="formRef.openCreate({ storeId: query.storeId ?? undefined })">
          <template #icon><PlusOutlined /></template>{{ $t('ecom.listing.add') }}
        </a-button>
        <a-button v-perm="'ecom:listing:import'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="(r) => (r.materialId ? '' : 'row-unmapped')"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <div>{{ record.storeName || '-' }}</div>
            <div v-if="record.platform" class="sub-text"><DictTag type="ecom_platform" :value="record.platform" /></div>
          </template>
          <template v-else-if="column.key === 'title'">
            <div class="cell-ellipsis" :title="record.title">{{ record.title || '-' }}</div>
            <div v-if="record.variant" class="sub-text">{{ record.variant }}</div>
          </template>
          <template v-else-if="column.key === 'price'">
            <span v-if="record.price != null">{{ record.currency || '' }} {{ price(record.price) }}</span>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'fulfillment'">
            <DictTag type="ecom_fulfillment" :value="record.fulfillment" />
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_listing_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'material'">
            <template v-if="record.materialId">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }}{{ record.materialSpec ? `（${record.materialSpec}）` : '' }}</div>
            </template>
            <template v-else>
              <a-tag color="warning" :bordered="false"><WarningOutlined /> {{ $t('ecom.common.unmapped') }}</a-tag>
              <a v-if="userStore.hasPerm('ecom:listing:edit')" @click="mapRef.open(record)">{{ $t('ecom.listing.mapMaterial') }}</a>
            </template>
          </template>
          <template v-else-if="column.key === 'packQty'">{{ qty(record.packQty) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <ListingForm ref="formRef" @success="load" />
    <ListingMapModal ref="mapRef" @success="load" />
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
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ImportOutlined, LinkOutlined, PlusOutlined, WarningOutlined } from '@ant-design/icons-vue'
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
import { deleteListing, importListing, pageListing } from '@/api/ecom/listing'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { price, qty } from '@/utils/number'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_fulfillment', 'ecom_listing_status')

const columns = [
  { title: t('ecom.common.store'), key: 'store', dataIndex: 'storeName', width: 170 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId', width: 140 },
  { title: t('ecom.listing.title'), key: 'title', dataIndex: 'title', width: 280 },
  { title: t('ecom.listing.price'), key: 'price', dataIndex: 'price', width: 120, align: 'right' },
  { title: t('ecom.listing.fulfillment'), key: 'fulfillment', dataIndex: 'fulfillment', width: 110, dict: 'ecom_fulfillment' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_listing_status' },
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
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-listing')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, keyword: undefined, status: undefined, fulfillment: undefined, materialId: undefined, unmappedOnly: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageListing, query, {
  // 「只看未映射」→ mapped=false
  params: ({ unmappedOnly, ...rest }) => ({ ...rest, mapped: unmappedOnly ? false : undefined })
})
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const mapRef = ref()
const remove = useAction(deleteListing, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

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
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
