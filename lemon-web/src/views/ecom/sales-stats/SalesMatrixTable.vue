<template>
  <a-table
    :row-key="(r) => r.key"
    size="small"
    bordered
    :columns="columns"
    :data-source="matrix.rows"
    :loading="loading"
    :pagination="pagination"
    :scroll="{ x: scrollX }"
    @change="(p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'label'">
        <template v-if="matrix.dimension === SalesDimension.SKU">
          <a v-if="record.listingId" :title="$t('ecomAnalytics.sales.viewTrend')" @click="emit('trend', record)">{{ record.sellerSku }}</a>
          <span v-else>{{ record.sellerSku }}</span>
          <div class="sub-text cell-ellipsis" :title="record.title">{{ record.title }}</div>
        </template>
        <template v-else-if="matrix.dimension === SalesDimension.SPU">
          <div>{{ record.spu }}</div>
          <div class="sub-text cell-ellipsis" :title="record.title">{{ $t('ecomAnalytics.sales.spuSkus', { n: record.skuCount }) }} {{ record.title }}</div>
        </template>
        <template v-else-if="matrix.dimension === SalesDimension.MATERIAL">
          <div>{{ record.materialCode }}</div>
          <div class="sub-text cell-ellipsis" :title="record.materialName">{{ record.materialName }} {{ record.materialSpec || '' }}</div>
        </template>
        <template v-else-if="matrix.dimension === SalesDimension.STORE">
          <div>{{ record.storeName }}</div>
          <DictTag type="ecom_platform" :value="record.platform" />
        </template>
        <DictTag v-else-if="matrix.dimension === SalesDimension.PLATFORM" type="ecom_platform" :value="record.platform" />
        <span v-else>{{ record.country || '-' }}</span>
      </template>
      <template v-else-if="column.key === 'store'">{{ record.storeName }}</template>
      <template v-else-if="column.periodIndex != null">
        <span :class="{ zero: !nz(record[measure][column.periodIndex]) }">{{ cell(record[measure][column.periodIndex]) }}</span>
      </template>
      <template v-else-if="column.key === 'total'"><b>{{ cell(record[measure === 'qty' ? 'totalQty' : 'totalAmount']) }}</b></template>
      <template v-else-if="column.key === 'prev'">{{ cell(record[measure === 'qty' ? 'prevQty' : 'prevAmount']) }}</template>
      <template v-else-if="column.key === 'mom'"><GrowthText :value="record[measure === 'qty' ? 'momQtyPercent' : 'momAmountPercent']" /></template>
      <template v-else-if="column.key === 'yoyValue'">{{ cell(record[measure === 'qty' ? 'yoyQty' : 'yoyAmount']) }}</template>
      <template v-else-if="column.key === 'yoy'"><GrowthText :value="record[measure === 'qty' ? 'yoyQtyPercent' : 'yoyAmountPercent']" /></template>
    </template>
    <template v-if="matrix.rows.length" #summary>
      <a-table-summary fixed>
        <a-table-summary-row class="total-row">
          <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
            <template v-if="i === 0">{{ $t('ecomAnalytics.common.total') }}</template>
            <template v-else-if="c.periodIndex != null">{{ cell(matrix.total[measure][c.periodIndex]) }}</template>
            <template v-else-if="c.key === 'total'">{{ cell(matrix.total[measure === 'qty' ? 'totalQty' : 'totalAmount']) }}</template>
            <template v-else-if="c.key === 'prev'">{{ cell(matrix.total[measure === 'qty' ? 'prevQty' : 'prevAmount']) }}</template>
            <template v-else-if="c.key === 'mom'"><GrowthText :value="matrix.total[measure === 'qty' ? 'momQtyPercent' : 'momAmountPercent']" /></template>
            <template v-else-if="c.key === 'yoyValue'">{{ cell(matrix.total[measure === 'qty' ? 'yoyQty' : 'yoyAmount']) }}</template>
            <template v-else-if="c.key === 'yoy'"><GrowthText :value="matrix.total[measure === 'qty' ? 'yoyQtyPercent' : 'yoyAmountPercent']" /></template>
          </a-table-summary-cell>
        </a-table-summary-row>
      </a-table-summary>
    </template>
  </a-table>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { money, nz, qty } from '@/utils/number'
import { SalesDimension } from '@/constants/enums'
import { t } from '@/locales'
import GrowthText from './GrowthText.vue'
import { periodTitle } from './salesStats'

/**
 * 销量矩阵表：左侧为维度对象（SKU 维度还显示店铺，SKU 可点击查看走势），中间每个时间段一列，右侧合计、环比、同比；
 * measure 为 qty（件数）或 amount（销售额），底部为合计行。数据为接口返回的全部行，前端分页
 */
const props = defineProps({
  matrix: { type: Object, required: true },
  measure: { type: String, default: 'qty' },
  loading: Boolean
})
const emit = defineEmits(['trend'])

const pagination = reactive({ current: 1, pageSize: 20, showSizeChanger: true, showTotal: (n) => t('common.table.total', { total: n }) })
watch(() => props.matrix, () => (pagination.current = 1))

const cell = (v) => (props.measure === 'qty' ? qty(v) : money(v))
const valueWidth = computed(() => (props.measure === 'qty' ? 80 : 100))

const columns = computed(() => {
  const m = props.matrix
  const withStore = [SalesDimension.SKU, SalesDimension.SPU].includes(m.dimension)
  const label = { key: 'label', title: t(`ecomAnalytics.sales.dim${m.dimension}`), width: 200, fixed: 'left' }
  const store = withStore ? [{ key: 'store', title: t('ecomAnalytics.common.store'), width: 130, ellipsis: true }] : []
  const periods = m.periods.map((p, i) => ({ key: `p${i}`, periodIndex: i, title: periodTitle(m.grain, p), width: valueWidth.value, align: 'right' }))
  const right = [
    { key: 'total', title: t('ecomAnalytics.common.total'), width: 110, align: 'right' },
    { key: 'prev', title: t('ecomAnalytics.sales.prev'), width: 100, align: 'right' },
    { key: 'mom', title: t('ecomAnalytics.sales.mom'), width: 90, align: 'right' },
    { key: 'yoyValue', title: t('ecomAnalytics.sales.yoyValue'), width: 100, align: 'right' },
    { key: 'yoy', title: t('ecomAnalytics.sales.yoy'), width: 90, align: 'right' }
  ]
  return [label, ...store, ...periods, ...right]
})
const scrollX = computed(() => columns.value.reduce((s, c) => s + (c.width || 100), 0))
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.cell-ellipsis {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.zero {
  color: var(--app-text-3);
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
