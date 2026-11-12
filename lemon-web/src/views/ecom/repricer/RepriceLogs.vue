<template>
  <div class="toolbar">
    <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomPublish.common.allStores')" class="sel-store" />
    <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.change.keywordPh')" class="kw" @press-enter="search" />
    <a-select v-model:value="query.status" allow-clear :placeholder="$t('ecomPublish.change.result')" :options="toOptions(dict.ecom_reprice_log_status)" class="sel" />
    <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" />
    <a-button @click="search"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    <a-button v-if="query.ruleId" @click="clearRule">{{ $t('ecomPublish.repricer.allRules') }}</a-button>
    <span class="spacer" />
    <template v-if="selectedKeys.length">
      <a-button v-perm="'ecom:repricer:execute'" type="primary" @click="apply(selectedKeys)">{{ $t('ecomPublish.repricer.applyN', { n: selectedKeys.length }) }}</a-button>
      <a-button v-perm="'ecom:repricer:execute'" @click="ignore(selectedKeys)">{{ $t('ecomPublish.repricer.ignoreN', { n: selectedKeys.length }) }}</a-button>
    </template>
  </div>
  <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: 1450 }"
           :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== RepriceLogStatus.SUGGESTED }) }" @change="onTableChange">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'runTime'">
        {{ record.runTime }}
        <div class="sub-text">{{ record.ruleName }}</div>
      </template>
      <template v-else-if="column.key === 'sku'">
        <div>{{ record.sellerSku }}</div>
        <div class="sub-text">{{ record.storeName }}</div>
      </template>
      <template v-else-if="column.key === 'change'">
        {{ record.oldPrice != null ? price(record.oldPrice) : '-' }}
        <template v-if="record.newPrice != null"> → <b :class="Number(record.newPrice) > Number(record.oldPrice) ? 'up' : 'down'">{{ price(record.newPrice) }}</b></template>
        <span class="sub-text"> {{ record.currency }}</span>
      </template>
      <template v-else-if="column.key === 'market'">
        <div>{{ $t('ecomPublish.repricer.lowest') }} {{ record.lowestPrice != null ? price(record.lowestPrice) : '-' }}</div>
        <div class="sub-text">{{ $t('ecomPublish.repricer.buybox') }} {{ record.buyboxPrice != null ? price(record.buyboxPrice) : '-' }} · {{ $t('ecomPublish.repricer.floor') }} {{ record.floorPrice != null ? price(record.floorPrice) : '-' }}</div>
      </template>
      <template v-else-if="column.key === 'status'">
        <DictTag type="ecom_reprice_log_status" :value="record.status" />
        <a-tooltip v-if="record.message" :title="record.message"><div class="sub-text ellipsis">{{ record.message }}</div></a-tooltip>
      </template>
      <template v-else-if="column.key === 'ops'"><TableActions :actions="rowActions(record)" /></template>
    </template>
  </a-table>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { applyRepriceLogs, ignoreRepriceLogs, pageRepriceLog } from '@/api/ecom/publish'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { RepriceLogStatus } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { price } from '@/utils/number'

/** 调价日志：原价 → 新价、竞品最低价 / 购物车价 / 底价、原因、结果；「仅建议」的可应用（推送）或忽略 */
const props = defineProps({ ruleId: { type: Number, default: undefined } })
const { t } = useI18n()
const dict = useDict('ecom_reprice_log_status')
const range = ref([])
const columns = [
  { title: t('ecomPublish.repricer.runTime'), key: 'runTime', width: 190, fixed: 'left' },
  { title: t('ecomPublish.common.sellerSku'), key: 'sku', width: 190 },
  { title: t('ecomPublish.repricer.priceChange'), key: 'change', width: 180 },
  { title: t('ecomPublish.repricer.market'), key: 'market', width: 230 },
  { title: t('ecomPublish.repricer.reason'), dataIndex: 'reason', width: 300 },
  { title: t('ecomPublish.change.result'), key: 'status', width: 200 },
  { ...actionColumn(2), key: 'ops' }
]
const query = reactive({ ruleId: props.ruleId, storeId: undefined, keyword: undefined, status: undefined, beginDate: undefined, endDate: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange } = useTable(pageRepriceLog, query)
function search() {
  query.beginDate = range.value?.[0]
  query.endDate = range.value?.[1]
  doSearch()
}
function clearRule() {
  query.ruleId = undefined
  search()
}
onMounted(load)
watch(
  () => props.ruleId,
  (v) => {
    query.ruleId = v
    search()
  }
)
const reload = () => {
  selectedKeys.value = []
  return load()
}
const apply = useAction((ids) => applyRepriceLogs(ids), { successKey: (r) => r.message, reload })
const ignore = useAction((ids) => ignoreRepriceLogs(ids), { successKey: (n) => t('ecomPublish.repricer.ignored', { n }), reload })
const rowActions = (r) => {
  const pending = r.status === RepriceLogStatus.SUGGESTED
  return [
    { type: 'run', label: t('ecomPublish.repricer.apply'), perm: 'ecom:repricer:execute', show: pending, onClick: () => apply([r.id]) },
    { type: 'cancel', label: t('ecomPublish.repricer.ignore'), perm: 'ecom:repricer:execute', show: pending, onClick: () => ignore([r.id]) }
  ]
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.sel-store {
  width: 200px;
}
.kw {
  width: 180px;
}
.sel {
  width: 130px;
}
.spacer {
  flex: 1;
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
  max-width: 180px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
