<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomPublish.common.platform')" name="platform">
          <a-select v-model:value="query.platform" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.price.keywordPh')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:priceTemplate:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.price.add') }}
        </a-button>
        <a-tooltip :title="$t('ecomPublish.price.formula')" :overlay-style="{ maxWidth: '420px' }">
          <span class="tip-text"><QuestionCircleOutlined /> {{ $t('ecomPublish.price.formulaLabel') }}</span>
        </a-tooltip>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'"><a @click="formRef.openEdit(record)">{{ record.name }}</a></template>
          <template v-else-if="column.key === 'platform'">
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
            <span v-else class="sub-text">{{ $t('ecomPublish.price.anyPlatform') }}</span>
          </template>
          <template v-else-if="column.key === 'costs'">{{ money(record.firstLegCost) }} / {{ money(record.otherCost) }}</template>
          <template v-else-if="column.key === 'rates'">{{ pct(record.commissionRate) }} / {{ pct(record.otherRate) }}</template>
          <template v-else-if="column.key === 'fixedFee'">{{ money(record.fixedFee) }}</template>
          <template v-else-if="column.key === 'targetMargin'">{{ pct(record.targetMargin) }}</template>
          <template v-else-if="column.key === 'priceTail'">{{ record.priceTail ?? '-' }}</template>
          <template v-else-if="column.key === 'minPrice'">{{ record.minPrice != null ? price(record.minPrice) : '-' }}</template>
          <template v-else-if="column.key === 'remark'">
            <a-tooltip v-if="record.remark" :title="record.remark" placement="topLeft"><span>{{ record.remark }}</span></a-tooltip>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>
    <PriceTemplateForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deletePriceTemplate, pagePriceTemplate } from '@/api/ecom/publish'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { money, price } from '@/utils/number'
import PriceTemplateForm from './PriceTemplateForm.vue'

/** 价格模板（定价公式）：成本 × 汇率 + 头程 + 平台佣金率 + 固定费 + 目标利润率 → 售价（可设尾数），刊登时计算建议价、批量改价按公式、自动调价推算底价 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'sys_common_status')
const columns = [
  { title: t('ecomPublish.price.name'), key: 'name', dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('ecomPublish.common.platform'), key: 'platform', dataIndex: 'platform', width: 110, dict: 'ecom_platform' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'sys_common_status' },
  { title: t('ecomPublish.price.costs'), key: 'costs', width: 140, align: 'right' },
  { title: t('ecomPublish.price.rates'), key: 'rates', width: 130, align: 'right' },
  { title: t('ecomPublish.price.fixedFee'), key: 'fixedFee', dataIndex: 'fixedFee', width: 90, align: 'right' },
  { title: t('ecomPublish.price.targetMargin'), key: 'targetMargin', dataIndex: 'targetMargin', width: 100, align: 'right' },
  { title: t('ecomPublish.price.priceTail'), key: 'priceTail', dataIndex: 'priceTail', width: 70, align: 'right' },
  { title: t('ecomPublish.price.minPrice'), key: 'minPrice', dataIndex: 'minPrice', width: 100, align: 'right' },
  { title: t('common.field.remark'), key: 'remark', dataIndex: 'remark', width: 220, ellipsis: true },
  actionColumn(2)
]
/** 百分数：去掉多余的小数 0 */
const pct = (v) => (v == null ? '-' : `${Number(v)}%`)
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-price-template')
const showSearch = ref(true)
const emptyQuery = () => ({ platform: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePriceTemplate, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const formRef = ref()
const remove = useAction(deletePriceTemplate, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:priceTemplate:edit', onClick: () => formRef.value.openEdit(r) },
  { type: 'delete', perm: 'ecom:priceTemplate:remove', confirm: { title: t('ecomPublish.common.deleteConfirm', { name: r.name }), okType: 'danger' }, onClick: () => remove(r.id) }
]
</script>

<style scoped>
.tip-text {
  align-self: center;
  color: var(--app-text-3);
  font-size: 12px;
  cursor: help;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
