<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOrd.blacklist.add')" :edit-title="$t('ecomOrd.blacklist.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomOrd.blacklist.matchType')" name="matchType">
        <a-radio-group v-model:value="form.matchType" :options="toOptions(dict.ecom_blacklist_type)" option-type="button" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.blacklist.matchValue')" name="matchValue" :extra="$t('ecomOrd.blacklist.valueHint.' + form.matchType)">
        <a-input v-model:value="form.matchValue" :maxlength="255" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.blacklist.store')" name="storeId" :extra="$t('ecomOrd.blacklist.storeHint')">
        <StoreSelect v-model:value="form.storeId" allow-clear :placeholder="$t('ecomOrd.blacklist.allStores')" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.blacklist.reason')" name="reason">
        <a-input v-model:value="form.reason" :maxlength="255" :placeholder="$t('ecomOrd.blacklist.reasonPh')" />
      </a-form-item>
      <a-form-item :label="$t('common.field.status')" name="status">
        <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { addBlacklist, updateBlacklist } from '@/api/ecom/orderEnhance'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import { BlacklistType } from '@/constants/enums'

/** 买家黑名单新增 / 编辑：匹配方式与值、生效店铺（为空全部店铺）、原因、状态 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_blacklist_type')
const rules = { matchValue: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOrd.blacklist.matchValue') }) }] }

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, matchType: BlacklistType.BUYER, matchValue: '', storeId: undefined, reason: '', status: STATUS_ENABLED }),
  create: addBlacklist,
  update: updateBlacklist,
  toPayload: (f) => ({ matchType: f.matchType, matchValue: f.matchValue.trim(), storeId: f.storeId ?? null, reason: f.reason || null, status: f.status }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>
