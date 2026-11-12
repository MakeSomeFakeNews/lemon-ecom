<template>
  <a-modal v-model:open="open" :title="$t('ecomOrd.addr.editTitle', { no: order?.platformOrderNo || '' })" width="min(860px, 100vw)" :confirm-loading="saving"
           @ok="submit">
    <a-alert v-if="issues.length" :type="issues.some((i) => i.error) ? 'error' : 'warning'" show-icon class="block">
      <template #message>{{ $t('ecomOrd.addr.issues') }}</template>
      <template #description>
        <ul class="issues">
          <li v-for="(i, k) in issues" :key="k">
            <a-tag :color="i.error ? 'error' : 'warning'" :bordered="false">{{ i.error ? $t('ecomOrd.addr.error') : $t('ecomOrd.addr.warn') }}</a-tag>{{ i.message }}
          </li>
        </ul>
      </template>
    </a-alert>
    <a-form layout="vertical">
      <AddressFields :form="form" />
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import AddressFields from './AddressFields.vue'
import { updateOrderAddress } from '@/api/ecom/orderEnhance'

/** 修改收件地址（地址校验不通过时在订单里修改），保存后后端自动重新校验：open(order, issues)，成功后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const order = ref()
const issues = ref([])
const form = reactive({})
const FIELDS = ['shipName', 'shipPhone', 'shipCountry', 'shipState', 'shipCity', 'shipPostcode', 'shipAddress']

function show(o, list = []) {
  order.value = o
  issues.value = list
  FIELDS.forEach((f) => (form[f] = o[f] || ''))
  open.value = true
}

async function submit() {
  saving.value = true
  try {
    const data = Object.fromEntries(FIELDS.map((f) => [f, form[f]?.trim() || null]))
    if (data.shipCountry) data.shipCountry = data.shipCountry.toUpperCase()
    await updateOrderAddress(order.value.id, data)
    message.success(t('ecomOrd.addr.saved'))
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
.issues {
  margin: 0;
  padding-left: 0;
  list-style: none;
}
.issues li {
  margin-bottom: 4px;
}
</style>
