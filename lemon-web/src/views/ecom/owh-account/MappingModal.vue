<template>
  <a-modal v-model:open="visible" :title="form.id ? $t('ecomOwh.panel.editMapping') : $t('ecomOwh.panel.addMapping')" :confirm-loading="saving" width="640px" @ok="submit">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.panel.warehouse')" name="warehouseId" :extra="$t('ecomOwh.panel.warehouseExtra')">
            <WarehouseSelect v-model:value="form.warehouseId" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.panel.owhCode')" name="owhCode">
            <a-auto-complete v-model:value="form.owhCode" :options="remoteOptions" :maxlength="64" @select="onPick">
              <template #default>
                <a-input-search :loading="fetching" :enter-button="$t('ecomOwh.panel.fetchRemote')" @search="fetchRemote" />
              </template>
            </a-auto-complete>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.panel.owhName')" name="owhName">
            <a-input v-model:value="form.owhName" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.country')" name="country">
            <a-input v-model:value="form.country" :maxlength="8" placeholder="US" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.autoPush')" name="autoPush" :extra="$t('ecomOwh.panel.autoPushExtra')">
            <a-switch :checked="form.autoPush === 1" @change="(v) => (form.autoPush = v ? 1 : 0)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.panel.defaultProduct')" name="defaultProductCode">
            <a-select
              v-model:value="form.defaultProductCode"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="products.map((p) => ({ value: p.productCode, label: `${p.productName || ''} (${p.productCode})` }))"
              :not-found-content="$t('ecomOwh.panel.noProducts')"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.panel.defectWarehouse')" name="defectWarehouseId" :extra="$t('ecomOwh.panel.defectWarehouseExtra')">
            <WarehouseSelect v-model:value="form.defectWarehouseId" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <div class="addr-title">{{ $t('ecomOwh.panel.addressTitle') }}</div>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.contact')" name="contact"><a-input v-model:value="form.contact" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.phone')" name="phone"><a-input v-model:value="form.phone" :maxlength="32" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.state')" name="state"><a-input v-model:value="form.state" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.city')" name="city"><a-input v-model:value="form.city" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.panel.postcode')" name="postcode"><a-input v-model:value="form.postcode" :maxlength="16" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="18">
          <a-form-item :label="$t('ecomOwh.panel.address')" name="address"><a-input v-model:value="form.address" :maxlength="255" /></a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch :checked="form.status === STATUS_ENABLED" @change="(v) => (form.status = v ? STATUS_ENABLED : STATUS_DISABLED)" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addOwhMapping, owhRemoteWarehouses, updateOwhMapping } from '@/api/ecom/owh'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

/** 新增 / 修改仓库映射：系统仓库 → 服务商仓库代码（可从服务商拉取仓库列表选择）、默认物流产品、自动推送。open(account, mapping, products) */
const emit = defineEmits(['done'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const accountId = ref(null)
const products = ref([])
const remote = ref([])
const fetching = ref(false)

const rules = {
  warehouseId: [{ required: true, message: t('common.rule.required', { name: t('ecomOwh.panel.warehouse') }) }],
  owhCode: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOwh.panel.owhCode') }) }]
}
const remoteOptions = computed(() => remote.value.map((w) => ({ value: w.code, label: `${w.code} ${w.name || ''} ${w.country || ''}`.trim() })))

function open(account, mapping, productList) {
  accountId.value = account.id
  products.value = productList || []
  remote.value = []
  form.value = mapping
    ? { ...mapping }
    : { id: undefined, warehouseId: undefined, owhCode: '', owhName: '', country: '', defaultProductCode: undefined, autoPush: 0, status: STATUS_ENABLED, remark: '',
        defectWarehouseId: undefined, contact: '', phone: '', state: '', city: '', postcode: '', address: '' }
  visible.value = true
}

async function fetchRemote() {
  fetching.value = true
  try {
    remote.value = (await owhRemoteWarehouses(accountId.value)) || []
    message.success(t('ecomOwh.panel.remoteFetched', { n: remote.value.length }))
  } finally {
    fetching.value = false
  }
}
function onPick(code) {
  const w = remote.value.find((x) => x.code === code)
  if (w) {
    form.value.owhName = w.name || form.value.owhName
    form.value.country = w.country || form.value.country
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const f = form.value
    const payload = {
      accountId: accountId.value,
      warehouseId: f.warehouseId,
      owhCode: f.owhCode,
      owhName: f.owhName || null,
      country: f.country || null,
      defaultProductCode: f.defaultProductCode || null,
      autoPush: f.autoPush || 0,
      status: f.status,
      remark: f.remark || null,
      defectWarehouseId: f.defectWarehouseId || null,
      contact: f.contact || null,
      phone: f.phone || null,
      state: f.state || null,
      city: f.city || null,
      postcode: f.postcode || null,
      address: f.address || null
    }
    if (f.id) await updateOwhMapping(f.id, payload)
    else await addOwhMapping(payload)
    message.success(t('common.msg.saveSuccess'))
    visible.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.addr-title {
  margin: 4px 0 8px;
  font-weight: 500;
}
</style>
