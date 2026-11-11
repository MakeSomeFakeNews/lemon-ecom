<template>
  <a-modal v-model:open="open" :title="$t('ecomOrder.order.outboundTitle')" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="tip" :message="$t('ecomOrder.order.outboundCount', { n: ids.length })" :description="$t('ecomOrder.order.outboundHint')" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOrder.order.outboundWarehouse')" :extra="$t('ecomOrder.order.outboundWarehouseHint')">
        <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('ecomOrder.order.outboundWarehouseHint')" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrder.order.bizDate')" :extra="$t('ecomOrder.order.bizDateHint')">
        <a-date-picker v-model:value="form.bizDate" value-format="YYYY-MM-DD" class="full" :placeholder="$t('ecomOrder.order.bizDateHint')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { h, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { outboundEcomOrders } from '@/api/ecom/order'

/**
 * 订单出库（单张或批量）：可指定出库仓库和业务日期，为空时后端按配送方式取店铺仓库、当天。
 * 后端逐单处理，返回失败的订单及原因（平台订单号 → 原因），有失败时弹窗列出；完成后触发 done
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const ids = ref([])
const form = reactive({ warehouseId: undefined, bizDate: undefined })

function show(orderIds) {
  ids.value = [...orderIds]
  form.warehouseId = undefined
  form.bizDate = undefined
  open.value = true
}

async function submit() {
  saving.value = true
  try {
    const failed = Object.entries((await outboundEcomOrders({ ids: ids.value, warehouseId: form.warehouseId, bizDate: form.bizDate })) || {})
    const ok = ids.value.length - failed.length
    if (!failed.length) {
      message.success(t('ecomOrder.order.outboundSuccess', { n: ok }))
    } else {
      Modal.warning({
        title: t('ecomOrder.order.outboundFailTitle'),
        width: 560,
        content: h('div', [
          h('div', { class: 'outbound-fail-summary' }, t('ecomOrder.order.outboundPartial', { ok, fail: failed.length })),
          h(
            'ul',
            { class: 'outbound-fail-list' },
            failed.map(([no, reason]) => h('li', [h('b', no), ' ', reason]))
          )
        ])
      })
    }
    open.value = false
    emit('done', { ok, failed })
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>

<style>
/* Modal.warning 的内容由函数渲染，不在组件作用域内 */
.outbound-fail-summary {
  margin-bottom: 8px;
}
.outbound-fail-list {
  max-height: 320px;
  margin: 0;
  padding-left: 20px;
  overflow: auto;
}
.outbound-fail-list li {
  margin-bottom: 4px;
}
</style>
