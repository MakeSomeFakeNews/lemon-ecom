<template>
  <a-modal v-model:open="open" :title="$t('ecomOrd.assign.title', { n: ids.length })" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon :message="$t('ecomOrd.assign.help')" class="block" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOrder.common.warehouse')">
        <WarehouseSelect v-model:value="warehouseId" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrder.common.channel')">
        <ChannelSelect v-model:value="channelId" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { assignOrders } from '@/api/ecom/orderEnhance'
import { ORDER_RESULT_KEYS, showBatchResult } from './order'

/** 批量指定发货仓 / 物流渠道（未打单的自发货订单）：open(ids)，完成后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const ids = ref([])
const warehouseId = ref()
const channelId = ref()

function show(orderIds) {
  ids.value = [...orderIds]
  warehouseId.value = undefined
  channelId.value = undefined
  open.value = true
}

async function submit() {
  if (warehouseId.value == null && channelId.value == null) {
    message.warning(t('ecomOrd.assign.pick'))
    return
  }
  saving.value = true
  try {
    showBatchResult(await assignOrders({ ids: ids.value, warehouseId: warehouseId.value ?? null, channelId: channelId.value ?? null }), ids.value.length,
      ORDER_RESULT_KEYS)
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
</style>
