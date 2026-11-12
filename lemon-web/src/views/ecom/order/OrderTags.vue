<template>
  <div v-if="order.tags?.length || showType" class="order-tags">
    <DictTag v-if="showType" type="ecom_order_type" :value="order.orderType" />
    <a-tag v-for="tg in order.tags || []" :key="tg.id" :color="tg.color" :bordered="false" class="tag">{{ tg.name }}</a-tag>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { EcomOrderType } from '@/constants/enums'

/** 订单列表中订单号下方的订单类型（手工 / 补发）与标签 */
const props = defineProps({ order: { type: Object, required: true } })
const showType = computed(() => props.order.orderType && props.order.orderType !== EcomOrderType.PLATFORM)
</script>

<style scoped>
.order-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 2px;
}
.tag {
  margin-inline-end: 0;
}
</style>
