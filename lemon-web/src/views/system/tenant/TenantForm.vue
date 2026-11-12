<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('system.tenant.add')" :edit-title="$t('system.tenant.edit')">
    <a-alert v-if="!form.id" type="info" show-icon class="form-tip" :message="$t('system.tenant.createTip')" />
    <a-alert v-else-if="form.platform" type="info" show-icon class="form-tip" :message="$t('system.tenant.platformTip')" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('system.tenant.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.name')" name="name" :extra="$t('system.tenant.nameTip')">
            <a-input v-model:value="form.name" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.contact')" name="contactName"><a-input v-model:value="form.contactName" :maxlength="64" /></a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.dept.contactPhone')" name="contactPhone"><a-input v-model:value="form.contactPhone" :maxlength="32" /></a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('system.tenant.sectionFeatures') }}</div>
      <a-row :gutter="16">
        <a-col v-if="!form.id" :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.mode')" name="mode" :extra="$t('system.tenant.modeTip')">
            <a-radio-group v-model:value="form.mode" button-style="solid">
              <a-radio-button value="template">{{ $t('system.tenant.byTemplate') }}</a-radio-button>
              <a-radio-button value="package">{{ $t('system.tenant.byPackage') }}</a-radio-button>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col v-if="form.id || form.mode === 'package'" :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.package')" name="packageId" :extra="$t('system.tenant.packageTip')">
            <a-select
              v-model:value="form.packageId"
              :options="packageOptions"
              :disabled="form.platform"
              :placeholder="form.platform ? $t('system.tenant.unlimited') : $t('common.placeholder.select')"
            />
          </a-form-item>
        </a-col>
        <a-col v-if="!form.id && form.mode === 'template'" :span="24">
          <a-form-item name="menuIds" :extra="$t('system.tenant.templateTip')">
            <a-form-item-rest>
              <FeaturePicker v-if="catalog" v-model="form.menuIds" :catalog="catalog" @industry="(i) => (form.templateName = i?.name || '')" />
            </a-form-item-rest>
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('system.tenant.sectionLimits') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.expireTime')" name="expireTime" :extra="$t('system.tenant.expireTip')">
            <a-date-picker
              v-model:value="form.expireTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              :disabled="form.platform"
              :placeholder="$t('system.tenant.permanent')"
              class="full"
            />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.accountLimit')" name="accountLimit" :extra="$t('system.tenant.accountLimitTip')">
            <a-input-number v-model:value="form.accountLimit" :min="1" :disabled="form.platform" :placeholder="$t('system.tenant.unlimited')" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('system.tenant.sectionAdmin') }}</div>
      <a-row :gutter="16">
        <template v-if="!form.id">
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item :label="$t('system.tenant.adminUsername')" name="adminUsername">
              <a-input v-model:value="form.adminUsername" :maxlength="64" :placeholder="$t('system.tenant.adminPlaceholder')" autocomplete="off" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :lg="8">
            <a-form-item :label="$t('system.tenant.adminPassword')" name="adminPassword" :extra="$t('system.tenant.adminPasswordTip')">
              <a-input-password v-model:value="form.adminPassword" autocomplete="new-password" />
            </a-form-item>
          </a-col>
        </template>
        <a-col v-else :xs="24" :sm="12" :lg="8">
          <a-form-item :label="$t('system.tenant.adminUsername')">
            <a-input :value="form.adminUsername" disabled />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('system.tenant.sectionOther') }}</div>
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark"><a-textarea v-model:value="form.remark" :rows="3" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import FeaturePicker from '@/components/system/FeaturePicker.vue'
import { addTenant, getTenant, tenantFeatures, tenantPackageOptions, updateTenant } from '@/api/system/tenant'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_ENABLED } from '@/utils/constants'

/**
 * 租户新增 / 编辑（子页）：基本信息、开通功能（新建时按行业模板或已有套餐）、使用限制、管理员账号、备注。
 * openCreate() / openEdit(record)；保存后触发 saved
 */
const emit = defineEmits(['saved'])
const { t } = useI18n()

const packageOptions = ref([])
const catalog = ref()
async function loadPackages() {
  const [packages, features] = await Promise.all([tenantPackageOptions(), catalog.value || tenantFeatures()])
  packageOptions.value = packages.map((p) => ({ label: p.name, value: p.id }))
  catalog.value = features
}

const rules = {
  name: [{ required: true, whitespace: true, message: t('system.tenant.inputName') }],
  packageId: [{ validator: (_, v) => (form.value.platform || form.value.mode === 'template' || v ? Promise.resolve() : Promise.reject(t('system.tenant.selectPackage'))) }],
  menuIds: [{ validator: (_, v) => (form.value.mode !== 'template' || v?.length ? Promise.resolve() : Promise.reject(t('system.tenant.selectTemplate'))) }],
  adminUsername: [{ required: true, whitespace: true, message: t('system.tenant.inputAdmin') }],
  adminPassword: [{ required: true, message: t('system.tenant.inputAdminPassword') }]
}
const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    packageId: undefined,
    contactName: '',
    contactPhone: '',
    expireTime: null,
    accountLimit: null,
    adminUsername: '',
    adminPassword: '',
    status: STATUS_ENABLED,
    remark: '',
    platform: false,
    mode: 'template',
    menuIds: [],
    templateName: ''
  }),
  beforeOpen: loadPackages,
  fetch: (record) => getTenant(record.id),
  create: addTenant,
  update: updateTenant,
  toPayload: ({ id, platform, packageName, accountCount, adminUserId, adminUsername, adminPassword, mode, menuIds, templateName, ...rest }) => {
    if (id) return rest
    const payload = { ...rest, adminUsername, adminPassword }
    // 按行业模板：不传套餐，后端按菜单匹配或新建套餐
    return mode === 'template' ? { ...payload, packageId: null, menuIds, templateName: templateName || t('system.tenant.custom') } : payload
  },
  onSuccess: () => emit('saved')
})
const { formRef, form } = drawer

defineExpose({ openCreate: () => drawer.openCreate(), openEdit: drawer.openEdit })
</script>

<style scoped>
.form-tip {
  margin-bottom: 16px;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
.full {
  width: 100%;
}
</style>
