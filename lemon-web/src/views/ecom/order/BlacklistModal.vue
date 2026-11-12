<template>
  <a-modal v-model:open="open" :title="$t('ecomOrd.blacklist.fromOrderTitle')" :confirm-loading="saving" @ok="submit">
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOrd.blacklist.matchType')">
        <a-radio-group v-model:value="form.matchType" @change="fillValue">
          <a-radio-button v-for="o in typeOptions" :key="o.value" :value="o.value" :disabled="!valueOf(o.value)">{{ o.label }}</a-radio-button>
        </a-radio-group>
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.blacklist.matchValue')">
        <a-input v-model:value="form.matchValue" :maxlength="255" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.blacklist.reason')">
        <a-input v-model:value="form.reason" :maxlength="255" :placeholder="$t('ecomOrd.blacklist.reasonPh')" />
      </a-form-item>
      <a-checkbox v-model:checked="form.storeOnly">{{ $t('ecomOrd.blacklist.storeOnly', { store: order?.storeName || '' }) }}</a-checkbox>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { addBlacklist } from '@/api/ecom/orderEnhance'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { BlacklistType } from '@/constants/enums'

/** 订单详情「拉黑买家」：按买家 ID / 邮箱 / 电话 / 地址加入黑名单，值取自订单。open(order)，成功后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const dict = useDict('ecom_blacklist_type')
const typeOptions = computed(() => toOptions(dict.ecom_blacklist_type))
const open = ref(false)
const saving = ref(false)
const order = ref()
const form = reactive({ matchType: BlacklistType.BUYER, matchValue: '', reason: '', storeOnly: false })

function valueOf(type) {
  const o = order.value
  if (!o) return ''
  switch (type) {
    case BlacklistType.BUYER:
      return o.buyerName || ''
    case BlacklistType.EMAIL:
      return o.buyerEmail || ''
    case BlacklistType.PHONE:
      return o.shipPhone || ''
    case BlacklistType.ADDRESS:
      return [o.shipAddress, o.shipPostcode].filter(Boolean).join(' ')
    default:
      return ''
  }
}
const fillValue = () => (form.matchValue = valueOf(form.matchType))

function show(o) {
  order.value = o
  const first = [BlacklistType.BUYER, BlacklistType.EMAIL, BlacklistType.PHONE, BlacklistType.ADDRESS].find((x) => valueOf(x)) || BlacklistType.BUYER
  Object.assign(form, { matchType: first, reason: '', storeOnly: false })
  fillValue()
  open.value = true
}

async function submit() {
  if (!form.matchValue?.trim()) {
    message.warning(t('ecomOrd.blacklist.valueRequired'))
    return
  }
  saving.value = true
  try {
    await addBlacklist({ matchType: form.matchType, matchValue: form.matchValue.trim(), reason: form.reason || null,
      storeId: form.storeOnly ? order.value.storeId : null })
    message.success(t('ecomOrd.blacklist.added'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
