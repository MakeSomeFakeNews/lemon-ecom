<template>
  <a-modal v-model:open="open" :title="$t('ecomOrder.pkg.labelTitle')" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="tip" :message="$t('ecomOrder.pkg.labelCount', { n: ids.length })" />
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOrder.common.channel')" :extra="$t('ecomOrder.pkg.labelChannelHint')">
        <ChannelSelect v-model:value="form.channelId" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrder.pkg.weight')" :extra="$t('ecomOrder.pkg.labelWeightHint')">
        <a-input-number v-model:value="form.weight" :min="0.001" :precision="3" class="full" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { labelEcomPackages } from '@/api/ecom/package'
import { showBatchResult } from '../order/order'
import { LABEL_RESULT_KEYS } from './package'

/**
 * 批量申请运单号：渠道需对接物流商（物流渠道中设置）；包裹没有渠道 / 重量时用这里选择的。
 * 后端逐个包裹在物流商下单、取面单，返回失败的包裹及原因；ref 调用 open(ids)，完成后触发 done
 */
const emit = defineEmits(['done'])

const open = ref(false)
const saving = ref(false)
const ids = ref([])
const form = reactive({ channelId: undefined, weight: undefined })

function show(packageIds) {
  ids.value = [...packageIds]
  form.channelId = undefined
  form.weight = undefined
  open.value = true
}

async function submit() {
  saving.value = true
  try {
    const res = await labelEcomPackages({ ids: ids.value, channelId: form.channelId, weight: form.weight })
    showBatchResult(res, ids.value.length, LABEL_RESULT_KEYS)
    open.value = false
    emit('done')
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
