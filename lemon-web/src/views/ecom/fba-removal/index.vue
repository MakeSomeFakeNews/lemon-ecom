<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomFba.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :platform="EcomPlatform.AMAZON" :placeholder="$t('ecomFba.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('ecomFba.removal.type')" name="removalType">
          <a-select v-model:value="query.removalType" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_fba_removal_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomFba.removal.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-tabs v-model:active-key="query.status" class="status-tabs" @change="search">
        <a-tab-pane key="" :tab="$t('ecomFba.removal.all')" />
        <a-tab-pane v-for="s in dict.ecom_fba_removal_status" :key="s.value" :tab="s.label" />
      </a-tabs>
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:fbaRemoval:add'" type="primary" @click="formRef.openCreate({ storeId: query.storeId })">
          <template #icon><PlusOutlined /></template>{{ $t('ecomFba.removal.add') }}
        </a-button>
        <a-tooltip :title="query.storeId ? '' : $t('ecomFba.common.pickStoreFirst')">
          <a-button v-perm="'ecom:fbaRemoval:sync'" :disabled="!query.storeId" @click="sync(query.storeId)">
            <template #icon><CloudSyncOutlined /></template>{{ $t('ecomFba.removal.sync') }}
          </a-button>
        </a-tooltip>
        <a-button v-perm="'ecom:fbaRemoval:sync'" @click="uploadRef.open(query.storeId, AmzReportType.REMOVAL_ORDERS)">
          <template #icon><UploadOutlined /></template>{{ $t('ecomFba.common.uploadReport') }}
        </a-button>
        <a-tooltip :title="$t('ecomFba.removal.flowTip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'docNo'"><a @click="detailRef.open(record.id)">{{ record.docNo }}</a></template>
          <template v-else-if="column.key === 'removalType'"><DictTag type="ecom_fba_removal_type" :value="record.removalType" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="ecom_fba_removal_status" :value="record.status" /></template>
          <template v-else-if="column.qty">{{ n0(record[column.key]) }}</template>
          <template v-else-if="column.key === 'removalFee'">{{ money(record.removalFee) }} <span class="sub-text">{{ record.currency }}</span></template>
          <template v-else-if="column.key === 'feeBase'">{{ money(record.feeBase) }}</template>
          <template v-else-if="column.key === 'tracking'">{{ [record.carrier, record.trackingNo].filter(Boolean).join(' ') }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <RemovalForm ref="formRef" @success="afterChange" />
    <RemovalDetail ref="detailRef" @edit="(d) => formRef.openEdit(d)" @receive="(d) => receiveRef.open(d)" />
    <ReceiveModal ref="receiveRef" @success="afterChange" />
    <SubmitModal ref="submitRef" @success="afterChange" />
    <AmzReportUploadModal ref="uploadRef" :types="[AmzReportType.REMOVAL_ORDERS]" @success="search" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CloudSyncOutlined, DeleteOutlined, InboxOutlined, InfoCircleOutlined, PlusOutlined, SendOutlined, StopOutlined, UploadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import AmzReportUploadModal from '@/components/ecom/AmzReportUploadModal.vue'
import RemovalForm from './RemovalForm.vue'
import RemovalDetail from './RemovalDetail.vue'
import ReceiveModal from './ReceiveModal.vue'
import SubmitModal from './SubmitModal.vue'
import { cancelRemoval, deleteRemoval, disposeRemoval, getRemoval, pageRemoval, syncRemoval } from '@/api/ecom/fba'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { AmzReportType, EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'
import { n0 } from '../fba-health/fba'
import { canCancel, canDelete, canDispose, canReceive, isEditable } from './removal'

/**
 * FBA 移除订单：计划 → 提交（亚马逊移除订单号）→ 报告同步发货 / 弃置数量 → 本地仓收货（良品 / 不良品）或弃置出库
 */
const route = useRoute()
const router = useRouter()
const dict = useDict('ecom_fba_removal_type', 'ecom_fba_removal_status')

const q = (key, title) => ({ title, key, dataIndex: key, width: 80, align: 'right', qty: true })
const columns = [
  { title: t('ecomFba.removal.docNo'), key: 'docNo', dataIndex: 'docNo', width: 160, fixed: 'left' },
  { title: t('ecomFba.common.store'), dataIndex: 'storeName', width: 140, ellipsis: true },
  { title: t('ecomFba.removal.type'), key: 'removalType', dataIndex: 'removalType', width: 80, dict: 'ecom_fba_removal_type' },
  { title: t('ecomFba.removal.platformOrderId'), dataIndex: 'platformOrderId', width: 160 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 110, dict: 'ecom_fba_removal_status' },
  q('totalQty', t('ecomFba.removal.requested')),
  q('shippedQty', t('ecomFba.removal.shipped')),
  q('disposedQty', t('ecomFba.removal.disposed')),
  q('receivedQty', t('ecomFba.removal.received')),
  { title: t('ecomFba.removal.removalFee'), key: 'removalFee', dataIndex: 'removalFee', width: 110, align: 'right' },
  { title: t('ecomFba.removal.feeBase'), key: 'feeBase', dataIndex: 'feeBase', width: 120, align: 'right' },
  { title: t('ecomFba.removal.warehouse'), dataIndex: 'warehouseName', width: 130, ellipsis: true },
  { title: t('ecomFba.removal.defectiveWarehouse'), dataIndex: 'defectiveWarehouseName', width: 120, ellipsis: true },
  { title: t('ecomFba.removal.tracking'), key: 'tracking', width: 150, ellipsis: true, exportValue: (r) => [r.carrier, r.trackingNo].filter(Boolean).join(' ') },
  { title: t('ecomFba.removal.requestDate'), dataIndex: 'requestDate', width: 110 },
  { title: t('ecomFba.removal.stockDocNos'), dataIndex: 'stockDocNos', width: 180, ellipsis: true },
  { title: t('ecomFba.removal.lastSync'), dataIndex: 'lastSyncTime', width: 160 },
  actionColumn(4, [t('ecomFba.removal.receive')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fba-removal')
const showSearch = ref(true)
const emptyQuery = () => ({ storeId: undefined, removalType: undefined, status: '', keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageRemoval, query, {
  params: (p) => ({ ...p, status: p.status || undefined })
})
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const detailRef = ref()
const receiveRef = ref()
const submitRef = ref()
const uploadRef = ref()
function afterChange() {
  load()
  detailRef.value?.reload()
}

onMounted(() => {
  load()
  // 库存健康勾选 SKU 生成移除计划：?storeId=&plan=[{ sku, qty }]
  if (route.query.plan) {
    let items = []
    try {
      items = JSON.parse(String(route.query.plan))
    } catch {
      items = []
    }
    formRef.value.openCreate({ storeId: Number(route.query.storeId) || undefined, items })
    router.replace({ query: {} })
  }
})

const sync = useAction(syncRemoval, { successKey: (ids) => t('ecomFba.common.syncCreated', { n: (ids || []).length }) })
const cancel = useAction(cancelRemoval, { reload: afterChange })
const remove = useAction(deleteRemoval, { successKey: 'common.msg.deleteSuccess', reload: afterChange })
const dispose = useAction((id) => disposeRemoval(id), {
  confirm: t('ecomFba.removal.disposeConfirm'),
  successKey: (no) => t('ecomFba.removal.disposeDone', { no }),
  reload: afterChange
})
const openReceive = async (record) => receiveRef.value.open(await getRemoval(record.id))

const rowActions = (record) => [
  { type: 'view', onClick: () => detailRef.value.open(record.id) },
  { key: 'receive', label: t('ecomFba.removal.receive'), icon: InboxOutlined, perm: 'ecom:fbaRemoval:receive', show: canReceive(record), onClick: () => openReceive(record) },
  { key: 'dispose', label: t('ecomFba.removal.dispose'), icon: DeleteOutlined, perm: 'ecom:fbaRemoval:receive', show: canDispose(record), onClick: () => dispose(record.id) },
  { type: 'edit', perm: 'ecom:fbaRemoval:edit', show: isEditable(record), onClick: () => formRef.value.openEdit(record) },
  { key: 'submit', label: t('ecomFba.removal.submit'), icon: SendOutlined, perm: 'ecom:fbaRemoval:edit', show: isEditable(record), more: true, onClick: () => submitRef.value.open(record) },
  {
    key: 'cancel',
    label: t('ecomFba.removal.cancel'),
    icon: StopOutlined,
    perm: 'ecom:fbaRemoval:edit',
    show: canCancel(record),
    more: true,
    confirm: t('ecomFba.removal.cancelConfirm', { no: record.docNo }),
    onClick: () => cancel(record.id)
  },
  {
    type: 'delete',
    perm: 'ecom:fbaRemoval:remove',
    show: canDelete(record),
    more: true,
    confirm: t('ecomFba.removal.deleteConfirm', { no: record.docNo }),
    onClick: () => remove(record.id)
  }
]
</script>

<style scoped>
.status-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
