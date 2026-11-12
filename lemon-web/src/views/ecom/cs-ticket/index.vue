<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.ticket.keywordPh')" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomCs.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('common.placeholder.all')" @update:value="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomCs.ticket.type')" name="ticketType">
          <a-select v-model:value="query.ticketType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_cs_ticket_type)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomCs.ticket.source')" name="sourceType">
          <a-select v-model:value="query.sourceType" allow-clear :placeholder="$t('common.placeholder.all')" :options="sourceOptions" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <span class="sub-text tip">{{ $t('ecomCs.ticket.tip') }}</span>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        class="ticket-table"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'ticketType'">
            <DictTag type="ecom_cs_ticket_type" :value="record.ticketType" />
          </template>
          <template v-else-if="column.key === 'source'">
            <router-link :to="sourceLink(record)">{{ $t(`ecomCs.ticket.sourceType.${record.sourceType}`) }}</router-link>
          </template>
          <template v-else-if="column.key === 'platformOrderNo'">
            <router-link :to="{ path: '/ecom/order', query: { id: record.orderId } }">{{ record.platformOrderNo }}</router-link>
          </template>
          <template v-else-if="column.key === 'result'">
            <span v-if="!record.returnId && !record.resendOrderId">-</span>
            <template v-if="record.returnId">
              <router-link :to="{ path: '/ecom/return', query: { id: record.returnId } }">{{ record.returnNo }}</router-link>
              <DictTag type="ecom_return_status" :value="record.returnStatus" class="gap" />
            </template>
            <template v-else-if="record.resendOrderId">
              <router-link :to="{ path: '/ecom/order', query: { keyword: record.resendOrderNo } }">{{ record.resendOrderNo }}</router-link>
              <DictTag type="ecom_local_status" :value="record.resendStatus" class="gap" />
            </template>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { pageCsTicket } from '@/api/ecom/cs'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { CsSourceType } from '@/constants/enums'

/**
 * 售后工单：买家消息、评价、店铺反馈「转售后」的记录，显示生成的退货退款单 / 补发订单及其当前状态（在退货退款、平台订单中继续处理）
 */
const { t } = useI18n()
const dict = useDict('ecom_cs_ticket_type', 'ecom_return_status', 'ecom_local_status')
const sourceOptions = Object.values(CsSourceType).map((v) => ({ value: v, label: t(`ecomCs.ticket.sourceType.${v}`) }))

const columns = [
  { title: t('ecomCs.ticket.type'), key: 'ticketType', dataIndex: 'ticketType', width: 110, dict: 'ecom_cs_ticket_type' },
  { title: t('ecomCs.common.store'), dataIndex: 'storeName', width: 150 },
  { title: t('ecomCs.ticket.source'), key: 'source', width: 110, exportValue: (r) => t(`ecomCs.ticket.sourceType.${r.sourceType}`) },
  { title: t('ecomCs.ticket.order'), key: 'platformOrderNo', dataIndex: 'platformOrderNo', width: 190 },
  {
    title: t('ecomCs.ticket.result'),
    key: 'result',
    width: 300,
    exportValue: (r) => r.returnNo || r.resendOrderNo
  },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('ecomCs.ticket.createBy'), dataIndex: 'createByName', width: 110 },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-cs-ticket')
const showSearch = ref(true)

const emptyQuery = () => ({ storeId: undefined, ticketType: undefined, sourceType: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCsTicket, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

/** 来源的页面：会话在买家消息中打开，评价 / 反馈在评价管理中打开 */
function sourceLink(r) {
  if (r.sourceType === CsSourceType.THREAD) return { path: '/ecom/cs-message', query: { id: r.sourceId, tab: 'ALL' } }
  return { path: '/ecom/cs-review', query: { id: r.sourceId, tab: r.sourceType === CsSourceType.FEEDBACK ? 'feedback' : 'review' } }
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.gap {
  margin-left: 6px;
}
.tip {
  align-self: center;
}
.ticket-table :deep(td) {
  white-space: nowrap;
}
</style>
