<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomProfit.tax.country')" name="country">
          <a-input v-model:value="query.country" allow-clear :maxlength="8" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomProfit.tax.taxType')" name="taxType">
          <a-select v-model:value="query.taxType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_tax_type)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:show-search="showSearch" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:tax:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomProfit.tax.add') }}
        </a-button>
        <a-button v-perm="'ecom:tax:apply'" @click="applyRef.open()">
          <template #icon><ReloadOutlined /></template>{{ $t('ecomProfit.tax.apply') }}
        </a-button>
        <a-tooltip :title="$t('ecomProfit.tax.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
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
          <template v-if="column.key === 'country'">
            <span class="strong">{{ record.country }}</span>
            <a-tag v-if="record.builtin === 1" class="builtin">{{ $t('ecomProfit.common.sample') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'marketplace'">{{ record.marketplace || $t('ecomProfit.tax.allMarketplaces') }}</template>
          <template v-else-if="column.key === 'taxType'"><DictTag type="ecom_tax_type" :value="record.taxType" /></template>
          <template v-else-if="column.key === 'rate'">{{ percent(record.rate, 2) }}</template>
          <template v-else-if="column.key === 'facilitator'">
            <a-tag :color="record.facilitator === 1 ? 'purple' : 'blue'">
              {{ record.facilitator === 1 ? $t('ecomProfit.tax.facilitatorYes') : $t('ecomProfit.tax.facilitatorNo') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.country"
              :api="(s) => changeTaxRateStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:tax:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:tax:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:tax:remove', confirm: $t('ecomProfit.tax.confirmDelete', { name: record.country }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <TaxRateForm ref="formRef" @success="load" />
    <TaxApplyModal ref="applyRef" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { InfoCircleOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { percent } from '@/components/charts/charts'
import TaxRateForm from './TaxRateForm.vue'
import TaxApplyModal from './TaxApplyModal.vue'
import { changeTaxRateStatus, deleteTaxRate, pageTaxRate } from '@/api/ecom/tax'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/**
 * 税率设置：订单写入时按 收件国家 + 店铺站点 匹配（站点相同的优先），记到订单上；修改税率后用「重算订单税额」更新已有订单
 */
const userStore = useUserStore()
const dict = useDict('ecom_tax_type', 'sys_common_status')

const columns = [
  { title: t('ecomProfit.tax.country'), key: 'country', dataIndex: 'country', width: 130 },
  { title: t('ecomReport.common.marketplace'), key: 'marketplace', dataIndex: 'marketplace', width: 110 },
  { title: t('ecomProfit.tax.taxType'), key: 'taxType', dataIndex: 'taxType', width: 160, dict: 'ecom_tax_type' },
  { title: t('ecomProfit.tax.rate'), key: 'rate', dataIndex: 'rate', width: 90, align: 'right' },
  {
    title: t('ecomProfit.tax.facilitator'),
    key: 'facilitator',
    dataIndex: 'facilitator',
    width: 150,
    exportValue: (r) => (r.facilitator === 1 ? t('ecomProfit.tax.facilitatorYes') : t('ecomProfit.tax.facilitatorNo'))
  },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 260, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-tax-rate')
const showSearch = ref(true)

const emptyQuery = () => ({ country: '', taxType: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageTaxRate, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const applyRef = ref()
const remove = useAction(deleteTaxRate, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.strong {
  font-weight: 600;
}
.builtin {
  margin-left: 6px;
}
</style>
