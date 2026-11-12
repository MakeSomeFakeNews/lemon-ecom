<template>
  <FormDrawer :drawer="drawer" :create-title="title" :edit-title="title">
    <a-alert type="info" show-icon class="block" :message="$t('ecomPurchase.setting.tip')" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.setting.leadDays')" name="leadDays">
            <a-input-number v-model:value="form.leadDays" :min="0" :max="365" :precision="0" class="full" :placeholder="leadPh" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.setting.safetyDays')" name="safetyDays">
            <a-input-number v-model:value="form.safetyDays" :min="0" :max="365" :precision="0" class="full" :placeholder="safetyPh" />
          </a-form-item>
        </a-col>
        <a-col v-if="isMaterial" :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.setting.purchaseDays')" name="purchaseDays" :extra="$t('ecomPurchase.setting.purchaseDaysExtra')">
            <a-input-number v-model:value="form.purchaseDays" :min="0" :max="365" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item
            :label="$t('ecomPurchase.setting.dailySales')"
            name="dailySales"
            :extra="$t(isMaterial ? 'ecomPurchase.setting.dailySalesExtraMaterial' : 'ecomPurchase.setting.dailySalesExtraListing')"
          >
            <a-input-number v-model:value="form.dailySales" :min="0" :precision="4" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.setting.factor')" name="factor" :extra="$t('ecomPurchase.setting.factorExtra')">
            <a-input-number v-model:value="form.factor" :min="0" :max="100" :precision="2" :step="0.1" class="full" placeholder="1" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.setting.disabled')" name="disabled" :extra="$t('ecomPurchase.setting.disabledExtra')">
            <a-switch v-model:checked="form.disabled" />
          </a-form-item>
        </a-col>
        <a-col :xs="24">
          <a-form-item :label="$t('ecomPurchase.setting.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { getReplenishSetting, saveReplenishSetting } from '@/api/ecom/replenish'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { ReplenishScope } from '@/constants/enums'

/**
 * 补货设置抽屉：在线商品级（平台仓补货的行）或物料级（本地仓采购的行），为空的项不覆盖默认值；全部清空保存即删除。
 * ref 调用 open({ scope, listingId, materialId, name, storeLeadDays, storeSafetyDays })
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const target = ref({})
const isMaterial = computed(() => target.value.scope === ReplenishScope.MATERIAL)
const title = computed(() =>
  t(isMaterial.value ? 'ecomPurchase.setting.titleMaterial' : 'ecomPurchase.setting.titleListing', { name: target.value.name || '' })
)
const leadPh = computed(() => (target.value.storeLeadDays != null ? t('ecomPurchase.setting.leadDaysPh', { n: target.value.storeLeadDays }) : undefined))
const safetyPh = computed(() => (target.value.storeSafetyDays != null ? t('ecomPurchase.setting.leadDaysPh', { n: target.value.storeSafetyDays }) : undefined))

const emptyForm = () => ({ leadDays: null, safetyDays: null, purchaseDays: null, dailySales: null, factor: null, disabled: false, remark: '' })
let cleared = false
const drawer = useFormDrawer({
  emptyForm,
  create: async (payload) => {
    const id = await saveReplenishSetting(payload)
    cleared = id == null
    return id
  },
  update: (_, payload) => saveReplenishSetting(payload),
  toPayload: (f) => ({
    scope: target.value.scope,
    listingId: isMaterial.value ? null : target.value.listingId,
    materialId: isMaterial.value ? target.value.materialId : null,
    leadDays: f.leadDays ?? null,
    safetyDays: f.safetyDays ?? null,
    purchaseDays: isMaterial.value ? f.purchaseDays ?? null : null,
    dailySales: f.dailySales ?? null,
    factor: f.factor ?? null,
    disabled: !!f.disabled,
    remark: f.remark || null
  }),
  successMessage: () => t(cleared ? 'ecomPurchase.setting.cleared' : 'ecomPurchase.setting.saved'),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

async function open(t0) {
  target.value = t0
  const s = await getReplenishSetting({
    scope: t0.scope,
    listingId: t0.scope === ReplenishScope.LISTING ? t0.listingId : undefined,
    materialId: t0.scope === ReplenishScope.MATERIAL ? t0.materialId : undefined
  })
  return drawer.openCreate({
    ...emptyForm(),
    ...(s
      ? {
          leadDays: s.leadDays,
          safetyDays: s.safetyDays,
          purchaseDays: s.purchaseDays,
          dailySales: s.dailySales != null ? Number(s.dailySales) : null,
          factor: s.factor != null ? Number(s.factor) : null,
          disabled: s.disabled === 1,
          remark: s.remark || ''
        }
      : {})
  })
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
</style>
