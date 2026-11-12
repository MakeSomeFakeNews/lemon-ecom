<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomPublish.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomPublish.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.competitor.keywordPh')" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.competitor.source')" name="source">
          <a-select v-model:value="query.source" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_competitor_source)" />
        </a-form-item>
        <a-form-item name="latestOnly">
          <a-checkbox v-model:checked="query.latestOnly" @change="search">{{ $t('ecomPublish.competitor.latestOnly') }}</a-checkbox>
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:competitor:add'" type="primary" @click="openForm"><template #icon><PlusOutlined /></template>{{ $t('ecomPublish.competitor.add') }}</a-button>
        <a-button v-perm="'ecom:competitor:add'" @click="importOpen = true"><template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}</a-button>
        <a-button v-perm="'ecom:competitor:add'" @click="fetchOpen = true"><template #icon><CloudDownloadOutlined /></template>{{ $t('ecomPublish.competitor.fetch') }}</a-button>
        <a-button v-if="selectedKeys.length" v-perm="'ecom:competitor:remove'" danger @click="remove(selectedKeys)">{{ $t('ecomPublish.list.deleteN', { n: selectedKeys.length }) }}</a-button>
        <span class="sub-text">{{ $t('ecomPublish.competitor.tip') }}</span>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination" :row-selection="rowSelection"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'sku'">
            <div>{{ record.sellerSku }} <span v-if="record.platformItemId" class="sub-text">{{ record.platformItemId }}</span></div>
            <div class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'store'">
            <div>{{ record.storeName }}</div>
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'myPrice'">{{ record.myPrice != null ? price(record.myPrice) : '-' }}</template>
          <template v-else-if="column.key === 'lowest'">
            <span v-if="record.lowestPrice != null" :class="gapClass(record)">{{ price(record.lowestPrice) }}</span>
            <span v-else>-</span>
            <div v-if="Number(record.lowestShipping) > 0" class="sub-text">+ {{ price(record.lowestShipping) }}</div>
          </template>
          <template v-else-if="column.key === 'buybox'">
            {{ record.buyboxPrice != null ? price(record.buyboxPrice) : '-' }}
            <a-tag v-if="record.buyboxWinner" color="green" :bordered="false">{{ $t('ecomPublish.competitor.winner') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'source'"><DictTag type="ecom_competitor_source" :value="record.source" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="formOpen" :title="$t('ecomPublish.competitor.add')" :confirm-loading="saving" width="min(640px, 100vw)" @ok="saveForm">
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('ecomPublish.common.store')" name="storeId" :rules="[{ required: true, message: $t('ecomPublish.common.selectStore') }]">
              <StoreSelect v-model:value="form.storeId" @update:value="form.listingId = undefined" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ecomPublish.common.listing')" name="listingId" :rules="[{ required: true, message: $t('ecomPublish.common.selectListing') }]">
              <ListingSelect v-model:value="form.listingId" :store-id="form.storeId" />
            </a-form-item>
          </a-col>
          <a-col :span="8"><a-form-item :label="$t('ecomPublish.competitor.lowestPrice')"><a-input-number v-model:value="form.lowestPrice" :min="0" :precision="2" class="full" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item :label="$t('ecomPublish.competitor.lowestShipping')"><a-input-number v-model:value="form.lowestShipping" :min="0" :precision="2" class="full" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item :label="$t('ecomPublish.competitor.buyboxPrice')"><a-input-number v-model:value="form.buyboxPrice" :min="0" :precision="2" class="full" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item :label="$t('ecomPublish.competitor.offerCount')"><a-input-number v-model:value="form.offerCount" :min="0" :precision="0" class="full" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item :label="$t('ecomPublish.competitor.winner')"><a-switch v-model:checked="form.buyboxWinner" :checked-value="1" :un-checked-value="0" /></a-form-item></a-col>
          <a-col :span="8">
            <a-form-item :label="$t('ecomPublish.competitor.snapshotTime')"><a-date-picker v-model:value="form.snapshotTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" /></a-form-item>
          </a-col>
          <a-col :span="24"><a-form-item :label="$t('ecomPublish.competitor.competitor')"><a-input v-model:value="form.competitor" :maxlength="128" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="fetchOpen" :title="$t('ecomPublish.competitor.fetch')" :confirm-loading="fetching" @ok="doFetch">
      <a-alert type="info" show-icon class="block" :message="$t('ecomPublish.competitor.fetchTip')" />
      <StoreSelect v-model:value="fetchStoreId" platform="AMAZON" class="full" />
    </a-modal>

    <ImportModal v-model:open="importOpen" :title="$t('ecomPublish.competitor.importTitle')" :columns="importColumns" :importer="importCompetitorPrice"
                 :sample="importSample" @success="search" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CloudDownloadOutlined, ImportOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import ListingSelect from '@/components/ecom/ListingSelect.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { addCompetitorPrice, deleteCompetitorPrice, fetchCompetitorPrice, importCompetitorPrice, pageCompetitorPrice } from '@/api/ecom/publish'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { price } from '@/utils/number'

/**
 * 竞品价格：在线商品的竞品最低价（及运费）、购物车价、本店是否持有购物车的快照。亚马逊店铺经 Product Pricing API 拉取（自动调价执行时也会拉取），
 * 其他平台手工录入或 Excel 导入；自动调价取每个商品 72 小时内最近的一条
 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_competitor_source')
const columns = [
  { title: t('ecomPublish.competitor.snapshotTime'), dataIndex: 'snapshotTime', width: 165 },
  { title: t('ecomPublish.common.store'), key: 'store', dataIndex: 'storeName', width: 160 },
  { title: t('ecomPublish.common.sellerSku'), key: 'sku', dataIndex: 'sellerSku', width: 240 },
  { title: t('ecomPublish.competitor.myPrice'), key: 'myPrice', dataIndex: 'myPrice', width: 100, align: 'right' },
  { title: t('ecomPublish.competitor.lowestPrice'), key: 'lowest', dataIndex: 'lowestPrice', width: 110, align: 'right' },
  { title: t('ecomPublish.competitor.buyboxPrice'), key: 'buybox', dataIndex: 'buyboxPrice', width: 150, align: 'right' },
  { title: t('ecomPublish.competitor.offerCount'), dataIndex: 'offerCount', width: 80, align: 'right' },
  { title: t('ecomPublish.competitor.competitor'), dataIndex: 'competitor', width: 160, ellipsis: true },
  { title: t('ecomPublish.competitor.source'), key: 'source', dataIndex: 'source', width: 100, dict: 'ecom_competitor_source' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(1)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-competitor-price')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, keyword: undefined, source: undefined, latestOnly: true })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, selectedKeys, rowSelection, load, search, onTableChange, exportAll } = useTable(pageCompetitorPrice, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

/** 竞品最低价（含运费）低于本店售价时标红 */
const gapClass = (r) => (r.myPrice != null && Number(r.lowestPrice) + Number(r.lowestShipping || 0) < Number(r.myPrice) ? 'below' : '')

const formOpen = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
function openForm() {
  form.value = { storeId: query.storeId, listingId: undefined, buyboxWinner: 0 }
  formOpen.value = true
}
async function saveForm() {
  await formRef.value.validate()
  saving.value = true
  try {
    await addCompetitorPrice(form.value)
    message.success(t('common.msg.saveSuccess'))
    formOpen.value = false
    load()
  } finally {
    saving.value = false
  }
}

const fetchOpen = ref(false)
const fetching = ref(false)
const fetchStoreId = ref()
async function doFetch() {
  if (!fetchStoreId.value) {
    message.warning(t('ecomPublish.common.selectStore'))
    return
  }
  fetching.value = true
  try {
    const r = await fetchCompetitorPrice({ storeId: fetchStoreId.value })
    message.success(t('ecomPublish.competitor.fetched', { requested: r.requested, saved: r.saved }))
    fetchOpen.value = false
    load()
  } finally {
    fetching.value = false
  }
}

function remove(ids) {
  Modal.confirm({
    title: t('ecomPublish.list.deleteConfirm', { n: [].concat(ids).length }),
    okType: 'danger',
    onOk: async () => {
      await deleteCompetitorPrice(ids)
      message.success(t('common.msg.deleteSuccess'))
      selectedKeys.value = []
      load()
    }
  })
}
const rowActions = (r) => [{ type: 'delete', perm: 'ecom:competitor:remove', onClick: () => remove([r.id]) }]

const importOpen = ref(false)
const importColumns = [
  { title: t('ecomPublish.common.storeCode'), key: 'storeCode', required: true },
  { title: t('ecomPublish.common.sellerSku'), key: 'sellerSku', required: true },
  { title: t('ecomPublish.competitor.lowestPrice'), key: 'lowestPrice', type: 'number' },
  { title: t('ecomPublish.competitor.lowestShipping'), key: 'lowestShipping', type: 'number' },
  { title: t('ecomPublish.competitor.buyboxPrice'), key: 'buyboxPrice', type: 'number' },
  { title: t('ecomPublish.competitor.winnerFlag'), key: 'buyboxWinner', type: 'number' },
  { title: t('ecomPublish.competitor.offerCount'), key: 'offerCount', type: 'number' },
  { title: t('ecomPublish.competitor.competitor'), key: 'competitor' },
  { title: t('ecomPublish.competitor.snapshotTime'), key: 'snapshotTime' },
  { title: t('common.field.remark'), key: 'remark' }
]
const importSample = { storeCode: 'US01', sellerSku: 'SKU-0001', lowestPrice: 19.49, lowestShipping: 0, buyboxPrice: 19.99, buyboxWinner: 0, offerCount: 6,
  competitor: 'Competitor Store', snapshotTime: '2026-11-01 10:00:00' }
</script>

<style scoped>
.below {
  color: #cf1322;
  font-weight: 500;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
