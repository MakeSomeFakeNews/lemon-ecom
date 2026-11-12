<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('scm.supplier.supplier')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('scm.supplier.codeOrName')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('scm.supplier.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.scm_supplier_level)" />
        </a-form-item>
        <a-form-item :label="$t('scm.supplier.type')" name="logistics">
          <a-select v-model:value="query.logistics" :placeholder="$t('common.placeholder.all')" allow-clear :options="typeOptions" />
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
        <a-button v-perm="'scm:supplier:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('scm.supplier.add') }}
        </a-button>
        <AiExtractButton target="scm.supplier" perm="scm:supplier:add" :title="$t('scm.supplier.supplier')" @result="(r) => formRef.applyAi(r)" />
        <a-button v-perm="'scm:supplier:add'" @click="importOpen = true">
          <template #icon><ImportOutlined /></template>{{ $t('common.action.import') }}
        </a-button>
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
              <a-tag v-if="record.logistics === 1" color="cyan" :bordered="false" class="logistics-tag">{{ $t('scm.supplier.logistics') }}</a-tag>
              <a-tooltip v-if="record.mergedTo" :title="$t('scm.partnerMerge.mergedTo', { name: record.mergedToName })">
                <a-tag :bordered="false" class="logistics-tag">{{ $t('scm.partnerMerge.merged') }}</a-tag>
              </a-tooltip>
            </div>
            <div v-if="record.customerName" class="sub-text">{{ $t('scm.supplier.linkedCustomerOf', { name: record.customerName }) }}</div>
            <div class="sub-text code-line">
              <span class="code">{{ record.code }}</span>
              <!-- 营运路线较长时省略显示，悬浮看全文 -->
              <a-tooltip v-if="record.logistics === 1 && record.routes" :overlay-style="{ maxWidth: '420px' }">
                <template #title>
                  <div class="routes-tip-label">{{ $t('scm.supplier.routes') }}</div>
                  <div class="routes-tip">{{ record.routes }}</div>
                </template>
                <span class="routes">{{ record.routes }}</span>
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'level'">
            <DictTag type="scm_supplier_level" :value="record.level" />
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contact || '-' }}</div>
            <div v-if="record.phone" class="sub-text">{{ record.phone }}</div>
          </template>
          <template v-else-if="column.key === 'paymentDays'">{{ record.paymentDays ? $t('scm.supplier.days', { n: record.paymentDays }) : $t('scm.supplier.cash') }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changePartnerStatus('supplier', record.id, s)"
              :disabled="!userStore.hasPerm('scm:supplier:edit')"
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

    <SupplierForm ref="formRef" @saved="load" />

    <SupplierPriceDrawer v-model:open="priceOpen" :supplier="priceSupplier" />
    <PartnerMergeModal ref="mergeRef" type="supplier" @merged="load" />
    <ImportModal
      v-model:open="importOpen"
      :title="$t('scm.supplier.supplier')"
      :columns="importColumns"
      :importer="(data) => importPartner('supplier', data)"
      :sample="{ code: 'S0001', name: $t('scm.supplier.sampleName'), contact: $t('scm.supplier.sampleContact'), phone: '13900000000', paymentDays: 30 }"
      @success="search"
    />
    <RecordExtrasModal
      v-model:open="extrasOpen"
      :title="extrasTitle"
      biz-type="scm_supplier"
      :biz-id="extrasId"
      :editable="userStore.hasPerm('scm:supplier:edit')"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DollarOutlined, ImportOutlined, MergeCellsOutlined, PaperClipOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import ImportModal from '@/components/ImportModal.vue'
import RecordExtrasModal from '@/components/RecordExtrasModal.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import SupplierPriceDrawer from '@/components/scm/SupplierPriceDrawer.vue'
import PartnerMergeModal from '@/components/scm/PartnerMergeModal.vue'
import { changePartnerStatus, deletePartner, pagePartner, importPartner } from '@/api/scm/partner'
import { sumWidth, useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { useCustomColumns } from '@/composables/useCustomFields'
import { useAction } from '@/composables/useAction'
import AiExtractButton from '@/components/ai/AiExtractButton.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import SupplierForm from './SupplierForm.vue'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'scm_supplier_level')

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
// 供货价格
const priceOpen = ref(false)
const priceSupplier = ref()
function openPrices(record) {
  priceSupplier.value = record
  priceOpen.value = true
}
const importColumns = [
  { title: t('scm.supplier.code'), key: 'code', required: true },
  { title: t('scm.supplier.name'), key: 'name', required: true },
  { title: t('scm.supplier.level'), key: 'level', dict: 'scm_supplier_level' },
  { title: t('scm.supplier.contact'), key: 'contact' },
  { title: t('scm.supplier.phone'), key: 'phone' },
  { title: t('common.field.email'), key: 'email' },
  { title: t('scm.supplier.address'), key: 'address' },
  { title: t('scm.supplier.taxNo'), key: 'taxNo' },
  { title: t('scm.supplier.bankName'), key: 'bankName' },
  { title: t('scm.supplier.bankAccount'), key: 'bankAccount' },
  { title: t('scm.supplier.paymentDaysCol'), key: 'paymentDays', type: 'int' },
  { title: t('common.field.remark'), key: 'remark' }
]

const columns = [
  { title: t('scm.supplier.supplier'), key: 'name', width: 240 },
  { title: t('scm.supplier.level'), key: 'level', width: 100, dict: 'scm_supplier_level' },
  { title: t('scm.supplier.contact'), key: 'contact', width: 150 },
  { title: t('scm.supplier.paymentDays'), key: 'paymentDays', width: 110 },
  { title: t('scm.supplier.address'), dataIndex: 'address', width: 220, ellipsis: true },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 150, ellipsis: true },
  actionColumn(3)
]
const { checked, visibleColumns } = useColumns(columns, 'scm-supplier')
const showSearch = ref(true)

const query = reactive({ keyword: undefined, level: undefined, logistics: undefined, status: undefined })
/** 类型：物流公司（承运商）/ 其他供应商 */
const typeOptions = [
  { value: 1, label: t('scm.supplier.typeLogistics') },
  { value: 0, label: t('scm.supplier.typeGoods') }
]
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable((p) => pagePartner('supplier', p), query)
// 自定义字段：表单中填写，勾选「列表显示」的作为列
const { columns: cfColumns, cell: cfCell } = useCustomColumns('scm_supplier', list)
const tableColumns = computed(() => {
  const cols = visibleColumns.value
  const action = cols.findIndex((c) => c.key === 'action')
  return action < 0 ? [...cols, ...cfColumns.value] : [...cols.slice(0, action), ...cfColumns.value, ...cols.slice(action)]
})

function resetQuery() {
  Object.assign(query, { keyword: undefined, level: undefined, logistics: undefined, status: undefined })
  search()
}

onMounted(load)

// 新增 / 编辑（子页）
const formRef = ref()

const remove = useAction((id) => deletePartner('supplier', id), { successKey: 'common.msg.deleteSuccess', reload: () => load() })

// 资料合并（issue #95）
const mergeRef = ref()

/** 行操作：编辑、供应商价格、删除直接显示，资料、合并在「更多」中 */
const rowActions = (record) => [
  { type: 'edit', perm: 'scm:supplier:edit', onClick: () => formRef.value.openEdit(record) },
  { key: 'prices', label: t('scm.supplier.prices'), icon: DollarOutlined, onClick: () => openPrices(record) },
  { type: 'delete', perm: 'scm:supplier:remove', confirm: t('scm.supplier.confirmDelete', { name: record.name }), onClick: () => remove(record.id) },
  { key: 'extras', label: t('scm.supplier.extras'), icon: PaperClipOutlined, more: true, onClick: () => openExtras(record) },
  { key: 'merge', label: t('scm.partnerMerge.action'), icon: MergeCellsOutlined, perm: 'scm:supplier:merge', show: !record.mergedTo, more: true, onClick: () => mergeRef.value.open(record) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.logistics-tag {
  margin-left: 4px;
}
.code-line {
  display: flex;
  min-width: 0;
}
.code-line .code {
  flex: none;
}
.code-line .routes {
  flex: 1;
  min-width: 0;
  margin-left: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.routes-tip-label {
  opacity: 0.75;
}
.routes-tip {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
