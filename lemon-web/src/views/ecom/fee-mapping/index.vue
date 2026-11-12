<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomReport.common.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('ecomProfit.fee.itemType')" name="itemType">
          <a-select v-model:value="query.itemType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_settlement_item_type)" />
        </a-form-item>
        <a-form-item :label="$t('ecomProfit.fee.rawType')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomProfit.fee.keywordPh')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:show-search="showSearch" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:feeMapping:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomProfit.fee.add') }}
        </a-button>
        <a-tooltip :title="$t('ecomProfit.fee.tip')"><InfoCircleOutlined class="tip-icon" /></a-tooltip>
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
          <template v-if="column.key === 'platform'">
            <DictTag v-if="record.platform !== EcomPlatform.OTHER" type="ecom_platform" :value="record.platform" />
            <a-tag v-else>{{ $t('ecomProfit.fee.generic') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'rawType'">
            <span class="mono">{{ record.rawType }}</span>
            <a-tag v-if="record.builtin === 1" class="builtin">{{ $t('ecomProfit.common.builtin') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'itemType'"><DictTag type="ecom_settlement_item_type" :value="record.itemType" /></template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.rawType"
              :api="(s) => changeFeeMappingStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:feeMapping:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:feeMapping:edit', onClick: () => formRef.openEdit(record) },
                {
                  type: 'delete',
                  perm: 'ecom:feeMapping:remove',
                  confirm: $t('ecomProfit.fee.confirmDelete', { name: record.rawType }),
                  onClick: () => remove(record.id)
                }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <FeeMappingForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { InfoCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import FeeMappingForm from './FeeMappingForm.vue'
import { changeFeeMappingStatus, deleteFeeMapping, pageFeeMapping } from '@/api/ecom/feeMapping'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { EcomPlatform } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 费用映射：平台结算报告中的原始类型名 → 系统结算明细类型。导入结算报告时没有填写类型的行按店铺平台识别，
 * 平台没有对应映射时用通用映射（平台「其他」）；匹配忽略首尾空格和大小写
 */
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_settlement_item_type')

const columns = [
  { title: t('ecomReport.common.platform'), key: 'platform', dataIndex: 'platform', width: 120, dict: 'ecom_platform' },
  { title: t('ecomProfit.fee.rawType'), key: 'rawType', dataIndex: 'rawType', width: 300 },
  { title: t('ecomProfit.fee.itemType'), key: 'itemType', dataIndex: 'itemType', width: 140, dict: 'ecom_settlement_item_type' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-fee-mapping')
const showSearch = ref(true)

const emptyQuery = () => ({ platform: undefined, itemType: undefined, keyword: '' })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageFeeMapping, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const remove = useAction(deleteFeeMapping, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
  align-self: center;
}
.mono {
  font-family: var(--app-font-mono, monospace);
}
.builtin {
  margin-left: 6px;
}
</style>
