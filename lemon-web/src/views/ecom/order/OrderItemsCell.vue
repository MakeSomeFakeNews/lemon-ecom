<template>
  <div v-if="items.length" class="items-cell">
    <div v-for="it in shown" :key="it.id" class="item">
      <ProductThumb :src="it.imageUrl" :size="36" />
      <div class="item-main">
        <div class="sku-line">
          <span class="sku" :title="it.sellerSku">{{ it.sellerSku || it.materialCode || '-' }}</span>
          <span class="qty">× {{ qty(it.qty) }}</span>
        </div>
        <div class="title" :title="it.title || it.materialName">{{ it.title || it.materialName || '-' }}</div>
      </div>
    </div>
    <a-popover v-if="rest > 0" placement="right" :title="$t('ecomOrdUi.list.allItems', { n: items.length })">
      <template #content>
        <div class="popover-list">
          <div v-for="it in items" :key="it.id" class="item">
            <ProductThumb :src="it.imageUrl" :size="32" :preview="false" />
            <div class="item-main">
              <div class="sku-line"><span class="sku">{{ it.sellerSku || '-' }}</span><span class="qty">× {{ qty(it.qty) }}</span></div>
              <div class="title">{{ it.title || it.materialName || '-' }}</div>
            </div>
          </div>
        </div>
      </template>
      <a class="more">{{ $t('ecomOrdUi.list.moreItems', { n: rest }) }}</a>
    </a-popover>
  </div>
  <span v-else class="sub-text">{{ $t('ecomOrdUi.list.itemCount', { n: order.itemCount || 0 }) }}</span>
</template>

<script setup>
import { computed } from 'vue'
import ProductThumb from '@/components/ecom/ProductThumb.vue'
import { qty } from '@/utils/number'

/** 订单列表的商品列：缩略图 + SKU × 数量 + 标题，最多显示 2 行，其余悬停查看 */
const props = defineProps({ order: { type: Object, required: true } })
const MAX = 2
const items = computed(() => props.order.items || [])
const shown = computed(() => items.value.slice(0, MAX))
const rest = computed(() => items.value.length - MAX)
</script>

<style scoped>
.items-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.item {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}
.item-main {
  min-width: 0;
  flex: 1;
  line-height: 18px;
}
.sku-line {
  display: flex;
  gap: 6px;
  align-items: baseline;
  min-width: 0;
}
.sku {
  overflow: hidden;
  font-weight: 500;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.qty {
  flex: none;
  color: var(--app-primary, #1677ff);
  font-weight: 500;
}
.title {
  overflow: hidden;
  color: var(--app-text-3);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.more {
  padding-left: 44px;
  font-size: 12px;
}
.popover-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
  max-height: 320px;
  overflow: auto;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
