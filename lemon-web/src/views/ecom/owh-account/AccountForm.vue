<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOwh.account.add')" :edit-title="$t('ecomOwh.account.edit')" width="min(720px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.account.code')" name="code">
            <a-input v-model:value="form.code" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.account.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOwh.common.provider')" name="provider">
            <a-select v-model:value="form.provider" :options="providers.map((p) => ({ value: p.provider, label: p.name }))" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.account.sandbox')" name="sandbox">
            <a-switch :checked="form.sandbox === 1" @change="(v) => (form.sandbox = v ? 1 : 0)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.account.autoSync')" name="autoSync" :extra="$t('ecomOwh.account.autoSyncExtra')">
            <a-switch :checked="form.autoSync !== 0" @change="(v) => (form.autoSync = v ? 1 : 0)" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomOwh.account.currency')" name="currency" :extra="$t('ecomOwh.account.currencyExtra')">
            <a-input v-model:value="form.currency" :maxlength="8" placeholder="USD" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomOwh.account.apiUrl')" name="apiBaseUrl" :extra="info ? $t('ecomOwh.account.endpoints', { prod: info.productionUrl, test: info.sandboxUrl }) : ''">
            <a-input v-model:value="form.apiBaseUrl" :maxlength="255" :placeholder="$t('ecomOwh.account.apiUrlPh')" />
          </a-form-item>
        </a-col>
      </a-row>

      <template v-if="info">
        <div class="cred-head">
          <span class="section-title">{{ $t('ecomOwh.account.credentials') }}</span>
          <a-tag :color="form.hasCredentials ? 'green' : 'default'" :bordered="false">
            {{ form.hasCredentials ? $t('ecomOwh.account.credSaved') : $t('ecomOwh.account.credMissing') }}
          </a-tag>
          <a-popconfirm v-if="form.id && form.hasCredentials" :title="$t('ecomOwh.account.clearConfirm')" @confirm="clear">
            <a-button size="small" type="link" danger>{{ $t('ecomOwh.account.clear') }}</a-button>
          </a-popconfirm>
          <a v-if="info.docUrl" :href="info.docUrl" target="_blank" rel="noopener" class="doc-link">{{ $t('ecomOwh.account.doc') }}</a>
        </div>
        <a-row :gutter="16">
          <a-col v-for="f in info.fields" :key="f.key" :xs="24" :sm="12">
            <a-form-item :label="f.label" :required="f.required && !form.hasCredentials">
              <a-input-password v-if="f.secret" v-model:value="form.auth[f.key]" autocomplete="new-password" :placeholder="form.hasCredentials ? $t('ecomOwh.account.keep') : ''" />
              <a-input v-else v-model:value="form.auth[f.key]" :placeholder="form.hasCredentials ? $t('ecomOwh.account.keep') : ''" />
            </a-form-item>
          </a-col>
        </a-row>
      </template>

      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addOwhAccount, clearOwhCredentials, getOwhAccount, owhProviders, updateOwhAccount } from '@/api/ecom/owh'
import { useFormDrawer } from '@/composables/useFormDrawer'

/**
 * 海外仓账号新增 / 编辑：服务商、环境、自定义地址、费用币种；授权字段按服务商声明生成（只写不读，留空不修改）
 */
const emit = defineEmits(['success'])
const { t } = useI18n()

const rules = {
  code: [
    { required: true, message: t('common.rule.required', { name: t('ecomOwh.account.code') }) },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: t('ecomOwh.account.codeRule') }
  ],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOwh.account.name') }) }],
  provider: [{ required: true, message: t('common.rule.required', { name: t('ecomOwh.common.provider') }) }]
}

const providers = ref([])
const FIELDS = ['code', 'name', 'provider', 'apiBaseUrl', 'currency', 'remark']
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, ...Object.fromEntries(FIELDS.map((f) => [f, undefined])), sandbox: 0, autoSync: 1, auth: {}, hasCredentials: false }),
  beforeOpen: async () => {
    if (!providers.value.length) providers.value = (await owhProviders()) || []
  },
  fetch: async (record) => ({ ...(await getOwhAccount(record.id)), auth: {} }),
  create: addOwhAccount,
  update: updateOwhAccount,
  toPayload: (f) => ({
    ...Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
    sandbox: f.sandbox || 0,
    autoSync: f.autoSync === 0 ? 0 : 1,
    auth: Object.fromEntries(Object.entries(f.auth || {}).filter(([, v]) => v != null && String(v).trim() !== ''))
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const info = computed(() => providers.value.find((p) => p.provider === form.value.provider))

async function clear() {
  await clearOwhCredentials(form.value.id)
  form.value.hasCredentials = false
  message.success(t('ecomOwh.account.cleared'))
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.cred-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 12px;
}
.section-title {
  font-weight: 500;
}
.doc-link {
  margin-left: auto;
}
</style>
