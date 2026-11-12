<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomPublish.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomPublish.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.change.keywordPh')" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.change.type')" name="changeType">
          <a-select v-model:value="query.changeType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_price_change_type)" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.change.source')" name="source">
          <a-select v-model:value="query.source" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_price_change_source)" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.change.result')" name="status">
          <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_price_change_status)" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.change.time')" name="range">
          <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item :label="$t('ecomPublish.change.batchNo')" name="batchNo">
          <a-input v-model:value="query.batchNo" allow-clear />
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <span class="sub-text">{{ $t('ecomPublish.change.tip') }}</span>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'store'">
            <div>{{ record.storeName }}</div>
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'sku'">
            <div>{{ record.sellerSku }}</div>
            <div class="sub-text ellipsis" :title="record.title">{{ record.title }}</div>
          </template>
          <template v-else-if="column.key === 'changeType'"><DictTag type="ecom_price_change_type" :value="record.changeType" /></template>
          <template v-else-if="column.key === 'change'">
            <span>{{ fmt(record, record.oldValue) }}</span>
            <ArrowRightOutlined class="arrow" />
            <b :class="trend(record)">{{ fmt(record, record.newValue) }}</b>
            <span v-if="record.currency" class="sub-text"> {{ record.currency }}</span>
          </template>
          <template v-else-if="column.key === 'source'">
            <DictTag type="ecom_price_change_source" :value="record.source" />
            <div v-if="record.ruleName" class="sub-text">{{ record.ruleName }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="ecom_price_change_status" :value="record.status" />
            <a-tooltip v-if="record.message" :title="record.message"><div class="sub-text ellipsis">{{ record.message }}</div></a-tooltip>
          </template>
          <template v-else-if="column.key === 'operator'">{{ record.operatorName || $t('ecomPublish.logs.system') }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { pagePriceChange } from '@/api/ecom/publish'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { PriceChangeType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { price, qty } from '@/utils/number'

/** 改价记录：批量改价 / 改库存、自动调价、刊登首次售价的历史（原值 → 新值、来源、是否推送成功），批量修改在「在线商品」中多选后进行 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_price_change_type', 'ecom_price_change_source', 'ecom_price_change_status')
const columns = [
  { title: t('ecomPublish.change.time'), dataIndex: 'createTime', width: 165 },
  { title: t('ecomPublish.common.store'), key: 'store', dataIndex: 'storeName', width: 160 },
  { title: t('ecomPublish.common.sellerSku'), key: 'sku', dataIndex: 'sellerSku', width: 220 },
  { title: t('ecomPublish.change.type'), key: 'changeType', dataIndex: 'changeType', width: 80, dict: 'ecom_price_change_type' },
  { title: t('ecomPublish.change.change'), key: 'change', width: 200, exportColumns: [
    { title: t('ecomPublish.change.oldValue'), dataIndex: 'oldValue' }, { title: t('ecomPublish.change.newValue'), dataIndex: 'newValue' }] },
  { title: t('ecomPublish.change.source'), key: 'source', dataIndex: 'source', width: 160, dict: 'ecom_price_change_source' },
  { title: t('ecomPublish.change.result'), key: 'status', dataIndex: 'status', width: 220, dict: 'ecom_price_change_status' },
  { title: t('ecomPublish.change.batchNo'), dataIndex: 'batchNo', width: 140 },
  { title: t('ecomPublish.change.operator'), key: 'operator', dataIndex: 'operatorName', width: 100 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-price-change')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, keyword: undefined, changeType: undefined, source: undefined, status: undefined, range: undefined, batchNo: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePriceChange, query, {
  params: ({ range, ...rest }) => ({ ...rest, beginDate: range?.[0], endDate: range?.[1] })
})
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const fmt = (r, v) => (v == null ? '-' : r.changeType === PriceChangeType.STOCK ? qty(v, 0) : price(v))
const trend = (r) => (r.oldValue == null || r.newValue == null ? '' : Number(r.newValue) > Number(r.oldValue) ? 'up' : Number(r.newValue) < Number(r.oldValue) ? 'down' : '')
</script>

<style scoped>
.arrow {
  margin: 0 6px;
  color: var(--app-text-3);
}
.up {
  color: #cf1322;
}
.down {
  color: #389e0d;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
