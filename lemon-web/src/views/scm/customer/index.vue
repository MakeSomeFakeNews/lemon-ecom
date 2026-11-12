<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.customer.customer')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.customer.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.customer.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_customer_level)" />
        </a-form-item>
        <a-form-item :label="$t('scm.customer.region')" name="region">
          <a-select v-model:value="query.region" :placeholder="$t('common.placeholder.all')" allow-clear show-search option-filter-prop="label" :options="toOptions(dict.scm_region)" />
        </a-form-item>
        <a-form-item :label="$t('scm.customer.channel')" name="channel">
          <a-select v-model:value="query.channel" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_channel)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar
        :exporter="exportAll"
        v-model:size="size"
        v-model:checked="checked"
        v-model:show-search="showSearch"
        :columns="columns"
        @refresh="load"
      >
        <a-button v-perm="'scm:customer:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.customer.add') }}
        </a-button>
        <AiExtractButton target="scm.customer" perm="scm:customer:add" :title="$t('scm.customer.customer')" @result="(r) => formRef.applyAi(r)" />
        <a-button v-perm="'scm:customer:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
        <a-dropdown>
          <a-button><DollarOutlined />{{ $t('pricing.customerPrice.batch') }} <DownOutlined /></a-button>
          <template #overlay>
            <a-menu @click="({ key }) => (key === 'import' ? (priceImportOpen = true) : exportPrices())">
              <a-menu-item v-if="userStore.hasPerm('scm:customer:edit')" key="import"><ImportOutlined /> {{ $t('pricing.customerPrice.importAll') }}</a-menu-item>
              <a-menu-item key="export"><ExportOutlined /> {{ $t('pricing.customerPrice.exportAll') }}</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="tableColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: sumWidth(tableColumns), y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div>
              {{ record.name }}
              <a-tooltip v-if="record.mergedTo" :title="$t('scm.partnerMerge.mergedTo', { name: record.mergedToName })">
                <a-tag :bordered="false" class="merged-tag">{{ $t('scm.partnerMerge.merged') }}</a-tag>
              </a-tooltip>
            </div>
            <div class="sub-text">{{ record.code }}</div>
            <div v-if="record.linkedSupplierName" class="sub-text">{{ $t('scm.customer.linkedSupplierOf', { name: record.linkedSupplierName }) }}</div>
          </template>
          <template v-else-if="column.key === 'level'">
            <DictTag type="scm_customer_level" :value="record.level" />
          </template>
          <template v-else-if="column.key === 'priceLevel'">{{ levelName(record.priceLevel ?? 0) || '-' }}</template>
          <template v-else-if="column.key === 'region'">
            <DictTag v-if="record.region" type="scm_region" :value="record.region" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'channel'">
            <DictTag type="scm_channel" :value="record.channel || 'DIRECT'" />
            <div v-if="record.ownerName" class="sub-text">{{ $t('scm.customer.ownerOf', { name: record.ownerName }) }}</div>
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'creditLimit'">{{ record.creditLimit != null ? symMoney(record.creditLimit) : $t('scm.customer.unlimited') }}</template>
          <template v-else-if="column.key === 'paymentDays'">{{ record.paymentDays ? $t('scm.customer.days', { n: record.paymentDays }) : $t('scm.customer.cash') }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePartnerStatus('customer', record.id, s)"
              :disabled="!userStore.hasPerm('scm:customer:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.custom">{{ cfCell(record, column) }}</template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <CustomerForm ref="formRef" @saved="load" />

    <ImportModal
      v-model:open="importOpen"
      :title="$t('scm.customer.customer')"
      :columns="importColumns"
      :importer="(data) => importPartner('customer', data)"
      :sample="{ code: 'C0001', name: $t('scm.customer.sampleName'), contact: $t('scm.customer.sampleContact'), phone: '13800000000', paymentDays: 30 }"
      @success="search"
    />
    <ImportModal
      v-model:open="priceImportOpen"
      :title="$t('pricing.customerPrice.importTitle')"
      :columns="priceImportColumns"
      :importer="importCustomerPrices"
      :sample="priceImportSample"
    />
    <RecordExtrasModal
      v-model:open="extrasOpen"
      :title="extrasTitle"
      biz-type="scm_customer"
      :biz-id="extrasId"
      :editable="userStore.hasPerm('scm:customer:edit')"
    />
    <PartnerMergeModal ref="mergeRef" type="customer" @merged="load" />
    <CustomerPriceDrawer ref="customerPriceRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DollarOutlined, DownOutlined, ExportOutlined, ImportOutlined, MergeCellsOutlined, PaperClipOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import PartnerMergeModal from '@/components/scm/PartnerMergeModal.vue'
import CustomerPriceDrawer from '@/components/scm/CustomerPriceDrawer.vue'
import { usePriceLevels } from '@/components/wms/priceLevels'
import { CUSTOMER_PRICE_COLUMNS, exportCustomerPriceRows, priceImportSample } from '@/components/scm/customerPrices'
import { importCustomerPrices } from '@/api/scm/pricing'
import { changePartnerStatus, deletePartner, pagePartner, importPartner } from '@/api/scm/partner'
import { sumWidth, useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { formatMoney } from '@/components/scm/scm'
import { useCustomColumns } from '@/composables/useCustomFields'
import { useAction } from '@/composables/useAction'
import AiExtractButton from '@/components/ai/AiExtractButton.vue'

import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import CustomerForm from './CustomerForm.vue'
import { symMoney } from '@/utils/number'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'scm_customer_level', 'scm_channel', 'scm_region')

// 价格等级名称（列表列）与客户执行价
const { levelName } = usePriceLevels()
const customerPriceRef = ref()
// 执行价批量导入导出（全部客户，含客户编码列）
const priceImportOpen = ref(false)
const priceImportColumns = CUSTOMER_PRICE_COLUMNS(true)
const exportPrices = () => exportCustomerPriceRows({ keyword: query.keyword || undefined })

const importOpen = ref(false)
// 附件、变更记录
const extrasOpen = ref(false)
const extrasId = ref()
const extrasTitle = ref('')
function openExtras(record) {
  extrasId.value = record.id
  extrasTitle.value = `${record.code} ${record.name}`
  extrasOpen.value = true
}
const importColumns = [
  { title: t('scm.customer.code'), key: 'code', required: true },
  { title: t('scm.customer.name'), key: 'name', required: true },
  { title: t('scm.customer.customerLevel'), key: 'level', dict: 'scm_customer_level' },
  { title: t('scm.customer.region'), key: 'region', dict: 'scm_region' },
  { title: t('scm.customer.contact'), key: 'contact' },
  { title: t('scm.customer.phone'), key: 'phone' },
  { title: t('scm.customer.shipAddress'), key: 'address' },
  { title: t('scm.customer.taxNo'), key: 'taxNo' },
  { title: t('scm.customer.creditLimit'), key: 'creditLimit', type: 'number' },
  { title: t('scm.customer.paymentDaysCol'), key: 'paymentDays', type: 'int' },
  { title: t('pricing.level.priceLevelImport'), key: 'priceLevel', type: 'int' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('scm.customer.customer'), key: 'name', width: 240 },
  { title: t('scm.customer.level'), key: 'level', width: 100, dict: 'scm_customer_level' },
  { title: t('pricing.level.priceLevel'), key: 'priceLevel', width: 100, exportValue: (r) => levelName(r.priceLevel ?? 0) },
  { title: t('scm.customer.region'), key: 'region', width: 110, dict: 'scm_region' },
  { title: t('scm.customer.channel'), key: 'channel', width: 150, dict: 'scm_channel' },
  { title: t('scm.customer.contact'), key: 'contact', width: 150 },
  { title: t('scm.customer.creditLimit'), key: 'creditLimit', width: 130, align: 'right' },
  { title: t('scm.customer.paymentDays'), key: 'paymentDays', width: 110 },
  { title: t('scm.customer.address'), dataIndex: 'address', width: 220, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  actionColumn(3)
]
const { checked, visibleColumns } = useColumns(columns, 'scm-customer')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, level: undefined, status: undefined, channel: undefined, region: undefined })
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((p) => pagePartner('customer', p), query)
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const { columns: cfColumns, cell: cfCell } = useCustomColumns('scm_customer', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, level: undefined, status: undefined, channel: undefined, region: undefined })
  search()
}

onMounted(load)

// 新增 / 编辑（子页）
const formRef = ref()

const remove = useAction((id) => deletePartner('customer', id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })
// 资料合并（issue #95）
const mergeRef = ref()

/** 行操作：编辑、客户价格、删除直接显示，资料、合并在「更多」中 */
const rowActions = (record) => [
  { type: 'edit', perm: 'scm:customer:edit', onClick: () => formRef.value.openEdit(record) },
  { key: 'price', label: t('pricing.customerPrice.open'), icon: DollarOutlined, onClick: () => customerPriceRef.value.open(record) },
  { type: 'delete', perm: 'scm:customer:remove', confirm: t('scm.customer.confirmDelete', { name: record.name }), onClick: () => remove(record.id) },
  { key: 'extras', label: t('scm.customer.extras'), icon: PaperClipOutlined, more: true, onClick: () => openExtras(record) },
  { key: 'merge', label: t('scm.partnerMerge.action'), icon: MergeCellsOutlined, perm: 'scm:customer:merge', show: !record.mergedTo, more: true, onClick: () => mergeRef.value.open(record) }
]
</script>

<style scoped>
.merged-tag {
  margin-left: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
