<template>
  <a-table
    :row-key="rowKey"
    size="middle"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
  >
    <template #bodyCell="{ column, record }">
      <slot v-if="column.custom" name="cell" :column="column" :record="record" />
      <template v-else>
        <template v-if="column.kind === 'money'">
          <span :class="{ neg: nz(record[column.key]) < 0 }">{{ money(record[column.key]) }}</span>
        </template>
        <template v-else-if="column.kind === 'qty'">{{ record[column.key] == null ? '-' : qty(record[column.key]) }}</template>
        <template v-else-if="column.kind === 'int'">{{ record[column.key] == null ? '-' : num(record[column.key]) }}</template>
        <template v-else-if="column.kind === 'days'">{{ record[column.key] == null ? '-' : qty(record[column.key], 1) }}</template>
        <template v-else-if="column.kind === 'percent'">{{ percent(record[column.key], 2) }}</template>
        <template v-else-if="column.kind === 'dict'"><DictTag :type="column.dict" :value="record[column.key]" /></template>
        <template v-else-if="column.kind === 'sku'">
          <div>{{ record.sellerSku }}</div>
          <div v-if="record.title" class="sub-text cell-ellipsis" :title="record.title">{{ record.title }}</div>
        </template>
        <template v-else-if="column.kind === 'material'">
          <template v-if="record.materialCode || record.materialName">
            <div>{{ record.materialCode }}</div>
            <div class="sub-text cell-ellipsis" :title="record.materialName">{{ record.materialName }}</div>
          </template>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else>{{ record[column.key] ?? '-' }}</template>
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { percent } from '@/components/charts/charts'
import { sumWidth } from '@/composables/useColumns'
import { money, num, nz, qty } from '@/utils/number'
import { t } from '@/locales'

/**
 * 数据分析的结果表：接口返回数组、前端分页；列定义带 kind（见 analytics.js 的 col）决定格式，
 * 列上 custom: true 时由插槽 cell（{ column, record }）显示
 */
const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, required: true },
  loading: Boolean,
  rowKey: { type: [String, Function], required: true }
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  showSizeChanger: true,
  showTotal: (n) => t('common.table.total', { total: n })
})
const scrollX = computed(() => sumWidth(props.columns))
watch(() => props.rows, () => (pagination.current = 1))
</script>

<style scoped>
.neg {
  color: var(--status-critical, #cf1322);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
