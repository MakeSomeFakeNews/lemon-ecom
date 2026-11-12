<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="saving" @ok="submit">
    <a-alert v-if="mode === 'tracking'" type="info" show-icon class="tip" :message="$t('ecomOrder.pkg.trackingHint')" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <template v-if="mode === 'tracking'">
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('ecomOrder.common.carrier')" name="carrier" :rules="[{ required: true, whitespace: true, message: $t('ecomOrder.order.inputCarrier') }]">
              <a-input v-model:value="form.carrier" :maxlength="64" :placeholder="$t('ecomOrder.order.carrierPh')" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12">
            <a-form-item :label="$t('ecomOrder.common.trackingNo')" name="trackingNo" :rules="[{ required: true, whitespace: true, message: $t('ecomOrder.order.inputTrackingNo') }]">
              <a-input v-model:value="form.trackingNo" :maxlength="64" />
            </a-form-item>
          </a-col>
        </template>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.common.channel')" name="channelId" :extra="channelLocked ? $t('ecomOrder.pkg.channelLocked') : undefined">
            <ChannelSelect v-model:value="form.channelId" :label="pkg?.channelName" :disabled="channelLocked" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.pkg.weight')" name="weight">
            <a-input-number v-model:value="form.weight" :min="0" :precision="3" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.order.shippingCostInput')" name="shippingCost" :extra="$t('ecomOrder.order.shippingCostHint')">
            <a-input-number v-model:value="form.shippingCost" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
        <a-col v-if="mode === 'edit'" :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { setEcomPackageTracking, updateEcomPackage } from '@/api/ecom/package'
import { EcomPackageStatus } from '@/constants/enums'

/**
 * 包裹的手工运单（mode = tracking：填写承运商、运单号后为待发货）与修改（mode = edit：渠道、重量、运费、备注，已打单的不能改渠道）。
 * ref 调用 openTracking(pkg) / openEdit(pkg)，保存后触发 done
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const saving = ref(false)
const mode = ref('tracking')
const pkg = ref()
const formRef = ref()
const form = reactive({ carrier: '', trackingNo: '', channelId: undefined, weight: undefined, shippingCost: undefined, remark: '' })

const title = computed(() =>
  mode.value === 'tracking' ? t('ecomOrder.pkg.trackingTitle', { no: pkg.value?.packageNo || '' }) : t('ecomOrder.pkg.editTitle', { no: pkg.value?.packageNo || '' })
)
const channelLocked = computed(() => mode.value === 'edit' && pkg.value?.status !== EcomPackageStatus.WAIT_LABEL)
const num = (v) => (v == null ? undefined : Number(v))

function fill(p, m) {
  mode.value = m
  pkg.value = p
  Object.assign(form, {
    carrier: p.carrierName || '',
    trackingNo: p.carrierOrderNo ? '' : p.trackingNo || '',
    channelId: p.channelId ?? undefined,
    weight: num(p.weight),
    shippingCost: num(p.shippingCost),
    remark: p.remark || ''
  })
  open.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (mode.value === 'tracking') {
      await setEcomPackageTracking(pkg.value.id, {
        carrier: form.carrier.trim(),
        trackingNo: form.trackingNo.trim(),
        channelId: form.channelId ?? null,
        weight: form.weight ?? null,
        shippingCost: form.shippingCost ?? null
      })
    } else {
      await updateEcomPackage(pkg.value.id, { channelId: form.channelId ?? null, weight: form.weight ?? null, shippingCost: form.shippingCost ?? null, remark: form.remark || null })
    }
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ openTracking: (p) => fill(p, 'tracking'), openEdit: (p) => fill(p, 'edit') })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
