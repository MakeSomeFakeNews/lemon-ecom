<template>
  <a-modal
    :open="visible"
    :title="$t('ecomProfit.tax.apply')"
    :confirm-loading="saving"
    :ok-text="$t('common.action.confirm')"
    width="min(480px, 100vw)"
    @ok="submit"
    @cancel="visible = false"
  >
    <a-alert type="info" show-icon :message="$t('ecomProfit.tax.applyTip')" class="tip" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomProfit.common.orderDate')" required>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
      </a-form-item>
      <a-form-item :label="$t('ecomReport.common.store')">
        <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { thisMonth } from '@/components/ecom/ecomReport'
import { applyTax } from '@/api/ecom/tax'

/** 按当前税率重算一段下单时间内订单的税额与利润：open() 打开 */
const { t } = useI18n()
const visible = ref(false)
const saving = ref(false)
const range = ref(thisMonth())
const storeId = ref()

function open() {
  range.value = thisMonth()
  storeId.value = undefined
  visible.value = true
}

async function submit() {
  saving.value = true
  try {
    const n = await applyTax({ beginDate: range.value[0], endDate: range.value[1], storeId: storeId.value || undefined })
    message.success(t('ecomProfit.tax.applied', { n }))
    visible.value = false
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
