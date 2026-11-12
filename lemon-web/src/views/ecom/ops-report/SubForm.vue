<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomAsst.sub.add')" :edit-title="$t('ecomAsst.sub.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAsst.sub.user')" name="userId">
        <IdentityUserSelect v-model:value="form.userId" :label="form.userName" />
      </a-form-item>
      <a-form-item :label="$t('ecomAsst.sub.stores')" name="storeIds" :extra="$t('ecomAsst.sub.storesExtra')">
        <StoreMultiSelect v-model:value="form.storeIds" />
      </a-form-item>
      <a-form-item :label="$t('ecomAsst.sub.channels')" name="channels">
        <a-checkbox-group v-model:value="form.channels" class="channels">
          <a-checkbox :value="OpsPushChannel.SITE">{{ label(OpsPushChannel.SITE) }}</a-checkbox>
          <a-tooltip v-for="c in IM" :key="c" :title="imChannels.includes(c) ? '' : $t('ecomAsst.sub.imUnavailable')">
            <a-checkbox :value="c" :disabled="!imChannels.includes(c)">{{ label(c) }}</a-checkbox>
          </a-tooltip>
        </a-checkbox-group>
        <div class="sub-text">{{ $t('ecomAsst.sub.channelsExtra') }}</div>
      </a-form-item>
      <a-form-item :label="$t('common.field.status')" name="status">
        <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
      </a-form-item>
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import IdentityUserSelect from '@/components/common/IdentityUserSelect.vue'
import { saveOpsReportSub } from '@/api/ecom/assistant'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { OpsPushChannel } from '@/constants/enums'
import { STATUS_ENABLED } from '@/utils/constants'
import { findDict, toOptions } from '@/utils/dict'
import StoreMultiSelect from './StoreMultiSelect.vue'

/** 新增 / 修改订阅（按订阅人保存，同一人只有一条）；没有接入的第三方平台不可选。open(record?) */
defineProps({ imChannels: { type: Array, default: () => [] } })
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_ops_push_channel', 'sys_common_status')
const IM = [OpsPushChannel.DINGTALK, OpsPushChannel.WECOM, OpsPushChannel.FEISHU]
const label = (c) => findDict(dict.ecom_ops_push_channel, c)?.label || c
const rules = {
  userId: [{ required: true, message: t('common.rule.required', { name: t('ecomAsst.sub.user') }) }],
  channels: [{ required: true, type: 'array', min: 1, message: t('ecomAsst.sub.channelRequired') }]
}

const emptyForm = () => ({ id: undefined, userId: undefined, userName: '', storeIds: [], channels: [OpsPushChannel.SITE], status: STATUS_ENABLED, remark: '' })
const save = (_id, payload) => saveOpsReportSub(payload)
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (r) => ({
    id: r.id,
    userId: r.userId,
    userName: r.userName,
    storeIds: String(r.storeIds || '').split(',').filter(Boolean).map(Number),
    channels: String(r.channels || '').split(',').filter(Boolean),
    status: r.status,
    remark: r.remark || ''
  }),
  create: (payload) => saveOpsReportSub(payload),
  update: save,
  toPayload: ({ userId, storeIds, channels, status, remark }) => ({ userId, storeIds, channels, status, remark }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ open: (r) => (r ? drawer.openEdit(r) : drawer.openCreate()) })
</script>

<style scoped>
.channels {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 0;
}
.sub-text {
  margin-top: 4px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
