<template>
  <a-table row-key="id" size="small" :columns="columns" :data-source="items" :pagination="false" :scroll="{ x: 1180 }">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'product'">
        <div class="product">
          <ProductThumb :src="record.imageUrl" :size="48" />
          <div class="product-main">
            <div class="title" :title="record.title">{{ record.title || '-' }}</div>
            <div class="sub-text">
              <span>{{ record.sellerSku || '-' }}</span>
              <span v-if="record.platformItemId"> · {{ record.platformItemId }}</span>
            </div>
          </div>
        </div>
      </template>
      <template v-else-if="column.key === 'qty'"><b>{{ qty(record.qty) }}</b></template>
      <template v-else-if="column.key === 'price'">{{ price(record.price) }}</template>
      <template v-else-if="column.key === 'amount'">
        <div>{{ money(record.amount) }}</div>
        <div v-if="Number(record.shipping) || Number(record.tax) || Number(record.discount)" class="sub-text nowrap">
          {{ $t('ecomOrdUi.detail.lineExtra', { shipping: money(record.shipping), tax: money(record.tax), discount: money(record.discount) }) }}
        </div>
      </template>
      <template v-else-if="column.key === 'material'">
        <template v-if="record.bundle === 1 && record.parts?.length">
          <a-tag color="purple" :bordered="false">{{ $t('ecomCatalog.listing.bundle') }}</a-tag>
          <div v-for="p in record.parts" :key="p.id" class="sub-text">{{ p.materialCode }} {{ p.materialName }} × {{ qty(p.unitQty) }}</div>
        </template>
        <template v-else-if="record.materialId">
          <div class="ellipsis" :title="`${record.materialCode} ${record.materialName}`">{{ record.materialCode }} {{ record.materialName }}</div>
          <div class="sub-text ellipsis">{{ record.materialSpec }}</div>
        </template>
        <a-tag v-else color="error" :bordered="false">{{ $t('ecomOrder.common.unmapped') }}</a-tag>
      </template>
      <template v-else-if="column.key === 'materialQty'">
        <template v-if="record.bundle === 1 && record.parts?.length">
          <div v-for="p in record.parts" :key="p.id">{{ qty(p.materialQty) }} {{ p.unit || '' }}</div>
        </template>
        <template v-else>{{ record.materialId ? `${qty(record.materialQty)} ${record.unit || ''}` : '-' }}</template>
      </template>
      <template v-else-if="column.key === 'allocatedQty'">
        <span :class="{ partial: Number(record.allocatedQty) < Number(record.qty) }">{{ qty(record.allocatedQty) }} / {{ qty(record.qty) }}</span>
      </template>
      <template v-else-if="column.key === 'cost'">{{ record.cost != null ? baseMoney(record.cost) : '-' }}</template>
    </template>
  </a-table>
</template>

<script setup>
import ProductThumb from '@/components/ecom/ProductThumb.vue'
import { t } from '@/locales'
import { money, price, qty } from '@/utils/number'
import { baseMoney } from './order'

/** 订单详情的商品明细：图片 + 标题 + SKU、数量、单价、金额（运费 / 税 / 折扣）、映射的物料与出库数量、已分配包裹、成本 */
defineProps({ items: { type: Array, default: () => [] } })

const columns = [
  { title: t('ecomOrdUi.detail.product'), key: 'product', width: 340 },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 70, align: 'right' },
  { title: t('ecomOrder.order.itemPrice'), key: 'price', width: 100, align: 'right' },
  { title: t('ecomOrder.order.itemAmountCol'), key: 'amount', width: 130, align: 'right' },
  { title: t('ecomOrder.order.itemMaterial'), key: 'material', width: 220 },
  { title: t('ecomOrder.order.itemMaterialQty'), key: 'materialQty', width: 100, align: 'right' },
  { title: t('ecomOrder.ops.allocated'), key: 'allocatedQty', width: 100, align: 'right' },
  { title: t('ecomOrder.order.itemCost'), key: 'cost', width: 120, align: 'right' }
]
</script>

<style scoped>
.product {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}
.product-main {
  flex: 1;
  min-width: 0;
}
.title {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 18px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.nowrap {
  white-space: nowrap;
}
.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.partial {
  color: var(--ant-color-warning, #faad14);
}
</style>
