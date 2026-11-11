<template>
  <div class="table-page ecom-platform-stock">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecom.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.platformStock.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item name="diffOnly">
          <a-checkbox v-model:checked="query.diffOnly" @change="search">{{ $t('ecom.platformStock.diffOnly') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:platformStock:import'" type="primary" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('ecom.platformStock.import') }}
        </a-button>
        <span class="sub-text toolbar-tip">{{ $t('ecom.platformStock.tip') }}</span>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :row-class-name="(r) => (Number(r.diffQty) ? 'row-diff' : '')"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'material'">
            <template v-if="record.materialId">
              <div>{{ record.materialCode }}</div>
              <div class="sub-text">{{ record.materialName }}</div>
            </template>
            <a-tag v-else color="warning" :bordered="false"><WarningOutlined /> {{ $t('ecom.common.unmapped') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'packQty'">{{ qty(record.packQty) }}</template>
          <template v-else-if="QTY_KEYS.includes(column.key)">{{ qty(record[column.key]) }}</template>
          <template v-else-if="column.key === 'systemQty'">
            <span v-if="record.materialId">{{ qty(record.systemQty) }}</span>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'diffQty'">
            <span v-if="!record.materialId" class="sub-text">-</span>
            <b v-else-if="Number(record.diffQty)" class="diff">{{ Number(record.diffQty) > 0 ? '+' : '' }}{{ qty(record.diffQty) }}</b>
            <span v-else class="sub-text">0</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'delete', perm: 'ecom:platformStock:import', confirm: $t('ecom.platformStock.confirmDelete', { sku: record.sellerSku }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <ImportModal
      v-model:open="importOpen"
      :title="$t('ecom.platformStock.platformStock')"
      :columns="importColumns"
      :importer="importPlatformStock"
      :sample="importSample"
      @success="search"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ImportOutlined, WarningOutlined } from '@ant-design/icons-vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { deletePlatformStock, importPlatformStock, pagePlatformStock } from '@/api/ecom/platformStock'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { today } from '@/components/wms/wms'
import { qty } from '@/utils/number'

const { t } = useI18n()

/** 平台报告中的四个数量列 */
const QTY_KEYS = ['fulfillableQty', 'inboundQty', 'reservedQty', 'unfulfillableQty']

const columns = [
  { title: t('ecom.common.store'), dataIndex: 'storeName', width: 170 },
  { title: t('ecom.common.sellerSku'), dataIndex: 'sellerSku', width: 160 },
  { title: t('ecom.common.platformItemId'), dataIndex: 'platformItemId', width: 140 },
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
  { title: t('ecom.platformStock.fulfillable'), key: 'fulfillableQty', dataIndex: 'fulfillableQty', width: 100, align: 'right' },
  { title: t('ecom.platformStock.inbound'), key: 'inboundQty', dataIndex: 'inboundQty', width: 100, align: 'right' },
  { title: t('ecom.platformStock.reserved'), key: 'reservedQty', dataIndex: 'reservedQty', width: 90, align: 'right' },
  { title: t('ecom.platformStock.unfulfillable'), key: 'unfulfillableQty', dataIndex: 'unfulfillableQty', width: 100, align: 'right' },
  { title: t('ecom.platformStock.systemQty'), key: 'systemQty', dataIndex: 'systemQty', width: 110, align: 'right' },
  { title: t('ecom.platformStock.diffQty'), key: 'diffQty', dataIndex: 'diffQty', width: 100, align: 'right' },
  { title: t('ecom.platformStock.snapshotDate'), dataIndex: 'snapshotDate', width: 120 },
  actionColumn(1, [t('common.action.delete')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-platform-stock')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, keyword: undefined, diffOnly: false })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePlatformStock, query, {
  params: (q) => ({ ...q, diffOnly: q.diffOnly || undefined })
})
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const remove = useAction(deletePlatformStock, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// 导入平台库存报告：店铺 + 平台 SKU 只保留最近一次快照
const importOpen = ref(false)
const importColumns = [
  { title: t('ecom.common.storeCode'), key: 'storeCode', required: true },
  { title: t('ecom.common.sellerSku'), key: 'sellerSku', required: true },
  { title: t('ecom.common.platformItemId'), key: 'platformItemId' },
  { title: t('ecom.platformStock.fulfillable'), key: 'fulfillableQty', required: true, type: 'number' },
  { title: t('ecom.platformStock.inbound'), key: 'inboundQty', type: 'number' },
  { title: t('ecom.platformStock.reserved'), key: 'reservedQty', type: 'number' },
  { title: t('ecom.platformStock.unfulfillable'), key: 'unfulfillableQty', type: 'number' },
  { title: t('ecom.platformStock.snapshotDate'), key: 'snapshotDate', type: 'date' }
]
const importSample = { storeCode: 'US01', sellerSku: 'SKU-0001', platformItemId: 'B000000001', fulfillableQty: 120, inboundQty: 50, reservedQty: 3, unfulfillableQty: 0, snapshotDate: today() }
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.toolbar-tip {
  align-self: center;
}
.diff {
  color: #ff4d4f;
}
</style>

<style>
/* 有差异的行高亮 */
.ecom-platform-stock .ant-table-tbody > tr.row-diff > td {
  background: #fff2f0;
}
.ecom-platform-stock .ant-table-tbody > tr.row-diff:hover > td {
  background: #ffe7e3;
}
html[data-theme='dark'] .ecom-platform-stock .ant-table-tbody > tr.row-diff > td {
  background: #2c1618;
}
html[data-theme='dark'] .ecom-platform-stock .ant-table-tbody > tr.row-diff:hover > td {
  background: #3a1d20;
}
</style>
