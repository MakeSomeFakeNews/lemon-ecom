<template>
  <a-modal v-model:open="visible" :title="$t('ecomOwh.push.title', { n: ids.length })" :confirm-loading="pushing" @ok="submit">
    <p class="tip">{{ $t('ecomOwh.push.tip') }}</p>
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOwh.push.product')" :extra="$t('ecomOwh.push.productExtra')">
        <a-select
          v-model:value="productCode"
          allow-clear
          show-search
          option-filter-prop="label"
          :placeholder="$t('ecomOwh.push.productAuto')"
          :options="products.map((p) => ({ value: p.productCode, label: `${p.productName || ''} (${p.productCode})` }))"
        />
      </a-form-item>
      <a-form-item v-if="accounts.length > 1">
        <a-alert type="info" show-icon :message="$t('ecomOwh.push.multiAccount')" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { owhProducts, pushOwhOutbound } from '@/api/ecom/owh'
import { showBatchResult } from '../order/order'
import { OWH_RESULT_KEYS } from './owh'

/**
 * 推送海外仓：选中的待打单包裹（或推送失败的重推）生成海外仓出库单。物流产品可指定（同一账号时列出该账号启用的产品），
 * 留空按 包裹物流渠道对应的产品 → 仓库映射的默认产品。open(ids, accountIds)
 */
const emit = defineEmits(['done'])

const visible = ref(false)
const pushing = ref(false)
const ids = ref([])
const accounts = ref([])
const products = ref([])
const productCode = ref()

async function open(packageIds, accountIds = []) {
  ids.value = [...packageIds]
  accounts.value = [...new Set(accountIds.filter(Boolean))]
  productCode.value = undefined
  products.value = []
  visible.value = true
  if (accounts.value.length === 1) {
    products.value = ((await owhProducts(accounts.value[0])) || []).filter((p) => p.status === 1)
  }
}

async function submit() {
  pushing.value = true
  try {
    const res = await pushOwhOutbound({ ids: ids.value, productCode: accounts.value.length === 1 ? productCode.value || null : null })
    visible.value = false
    showBatchResult(res, ids.value.length, OWH_RESULT_KEYS)
    emit('done')
  } finally {
    pushing.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  color: var(--app-text-3);
}
</style>
