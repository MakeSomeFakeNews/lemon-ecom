<template>
  <div class="tab-body">
    <div class="filters">
      <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomFba.receipt.keywordPh')" allow-clear class="w260" @search="search" />
      <a-checkbox v-model:checked="query.shortOnly" @change="search">{{ $t('ecomFba.receipt.shortOnly') }}</a-checkbox>
    </div>
    <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
      <span class="sub-text toolbar-tip">{{ $t('ecomFba.receipt.tip') }}</span>
    </TableToolbar>
    <a-table
      row-key="inboundItemId"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'docNo'">
          <div>{{ record.docNo }}</div>
          <div class="sub-text">{{ record.shipmentId || '-' }}</div>
        </template>
        <template v-else-if="column.key === 'status'"><DictTag type="ecom_inbound_status" :value="record.status" /></template>
        <template v-else-if="column.key === 'material'">
          <div>{{ record.sellerSku || '-' }}</div>
          <div class="sub-text">{{ record.materialCode }} {{ record.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'shippedQty'">{{ n0(record.shippedQty) }}</template>
        <template v-else-if="column.key === 'receivedQty'">
          <a-space v-if="can('ecom:fbaClaim:edit')" :size="4">
            <a-input-number v-model:value="edits[record.inboundItemId]" :min="0" :precision="0" size="small" class="w90" :placeholder="$t('ecomFba.receipt.notEntered')" />
            <a-button size="small" type="link" :disabled="edits[record.inboundItemId] == null" @click="save(record)">{{ $t('ecomFba.receipt.save') }}</a-button>
          </a-space>
          <span v-else>{{ record.receivedQty == null ? $t('ecomFba.receipt.notEntered') : n0(record.receivedQty) }}</span>
          <div v-if="record.source" class="sub-text">{{ $t(`ecomFba.dataSource.${record.source}`) }}</div>
        </template>
        <template v-else-if="column.key === 'shortQty'">
          <b v-if="Number(record.shortQty) > 0" class="neg">{{ n0(record.shortQty) }}</b>
          <span v-else class="sub-text">{{ record.shortQty == null ? '-' : 0 }}</span>
        </template>
        <template v-else-if="column.key === 'claimNo'">
          <a-tag v-if="record.claimNo" color="orange" :bordered="false">{{ record.claimNo }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              {
                key: 'sync',
                label: $t('ecomFba.receipt.syncApi'),
                icon: CloudSyncOutlined,
                perm: 'ecom:fbaClaim:sync',
                show: !!record.shipmentId,
                onClick: () => sync(record.inboundId)
              }
            ]"
          />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CloudSyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { pageReceipt, saveReceipt, syncReceipt } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'
import { t } from '@/locales'
import { n0, useCan } from '../fba-health/fba'

/** 货件对账：头程货件明细的发货件数与亚马逊实收件数，行内填写或按平台货件号从亚马逊同步 */
const props = defineProps({ storeId: Number })
const can = useCan()
const columns = [
  { title: t('ecomFba.receipt.docNo'), key: 'docNo', width: 170, exportColumns: [{ title: t('ecomFba.receipt.docNo'), dataIndex: 'docNo' }, { title: t('ecomFba.receipt.shipmentId'), dataIndex: 'shipmentId' }] },
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 130, ellipsis: true },
  { title: t('ecomFba.receipt.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'ecom_inbound_status' },
  { title: t('ecomFba.receipt.shipDate'), dataIndex: 'shipDate', width: 110 },
  { title: t('ecomFba.receipt.receiveDate'), dataIndex: 'receiveDate', width: 110 },
  { title: t('ecomFba.common.sku'), key: 'material', width: 200, exportColumns: [{ title: t('ecomFba.common.sku'), dataIndex: 'sellerSku' }, { title: t('ecomFba.common.material'), dataIndex: 'materialCode' }] },
  { title: t('ecomFba.receipt.shipped'), key: 'shippedQty', dataIndex: 'shippedQty', width: 90, align: 'right' },
  { title: t('ecomFba.receipt.received'), key: 'receivedQty', dataIndex: 'receivedQty', width: 190 },
  { title: t('ecomFba.receipt.short'), key: 'shortQty', dataIndex: 'shortQty', width: 80, align: 'right' },
  { title: t('ecomFba.receipt.claimNo'), key: 'claimNo', dataIndex: 'claimNo', width: 140 },
  actionColumn(1, [t('ecomFba.receipt.syncApi')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-receipt')
const query = reactive({ keyword: undefined, shortOnly: false })
const edits = reactive({})
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(
  async (params) => {
    const res = await pageReceipt(params)
    for (const r of res.records || []) edits[r.inboundItemId] = r.receivedQty != null ? Number(r.receivedQty) : undefined
    return res
  },
  query,
  { params: (p) => ({ storeId: props.storeId || undefined, keyword: p.keyword, shortOnly: p.shortOnly || undefined }) }
)
onMounted(load)
watch(() => props.storeId, () => search())

async function save(record) {
  await saveReceipt({ inboundItemId: record.inboundItemId, receivedQty: edits[record.inboundItemId] })
  message.success(t('ecomFba.receipt.saved'))
  load()
}
const sync = useAction(syncReceipt, { successKey: (n) => t('ecomFba.receipt.syncDone', { n: n || 0 }), reload: () => load() })
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.w260 {
  width: 260px;
  max-width: 100%;
}
.w90 {
  width: 90px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.toolbar-tip {
  align-self: center;
}
.neg {
  color: var(--status-critical, #cf1322);
}
</style>
