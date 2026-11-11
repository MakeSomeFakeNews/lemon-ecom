<template>
  <a-table
    :row-key="rowKey"
    size="middle"
    :columns="columns"
    :data-source="rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => applyPageChange(pagination, p)"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.kind === 'money'">
        <span :class="{ neg: nz(record[column.key]) < 0, strong: column.key === 'profit' }">{{ money(record[column.key]) }}</span>
      </template>
      <template v-else-if="column.kind === 'int'">{{ num(record[column.key]) }}</template>
      <template v-else-if="column.kind === 'qty'">{{ qty(record[column.key]) }}</template>
      <template v-else-if="column.kind === 'percent'">
        <span :class="{ neg: nz(record[column.key]) < 0 }">{{ percent(record[column.key], 2) }}</span>
      </template>
      <template v-else-if="column.kind === 'dict'"><DictTag :type="column.dict" :value="record[column.key]" /></template>
      <template v-else-if="column.key === 'material'">
        <template v-if="record.materialCode || record.materialName">{{ record.materialCode }} {{ record.materialName }}</template>
        <span v-else class="sub-text">-</span>
      </template>
      <template v-else-if="column.key === 'country'">{{ record.country || '-' }}</template>
    </template>
    <template v-if="rows.length" #summary>
      <a-table-summary fixed>
        <a-table-summary-row class="total-row">
          <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
            <template v-if="i === 0">{{ $t('ecomReport.common.total') }}</template>
            <template v-else-if="c.kind === 'money'">
              <span :class="{ neg: nz(total[c.key]) < 0 }">{{ money(total[c.key]) }}</span>
            </template>
            <template v-else-if="c.kind === 'int'">{{ num(total[c.key]) }}</template>
            <template v-else-if="c.kind === 'qty'">{{ qty(total[c.key]) }}</template>
            <template v-else-if="c.kind === 'percent'">
              <span :class="{ neg: nz(total[c.key]) < 0 }">{{ percent(total[c.key], 2) }}</span>
            </template>
          </a-table-summary-cell>
        </a-table-summary-row>
      </a-table-summary>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { percent } from '@/components/charts/charts'
import { applyPageChange, localPagination } from '@/components/ecom/ecomReport'
import { sumWidth } from '@/composables/useColumns'
import { money, num, nz, qty } from '@/utils/number'

/**
 * 利润分析的汇总表：列定义带 kind（money / int / qty / percent / dict）决定格式，利润与比率为负时标红；
 * 数据为接口返回的数组，前端分页，底部为 total 的合计行
 */
const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, required: true },
  total: { type: Object, default: () => ({}) },
  loading: Boolean,
  rowKey: { type: Function, required: true }
})

const pagination = reactive(localPagination(20))
const scrollX = computed(() => sumWidth(props.columns))
// 重新查询或切换页签后回到第一页
watch(() => props.rows, () => (pagination.current = 1))
</script>

<style scoped>
.neg {
  color: var(--status-critical, #cf1322);
}
.strong {
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
