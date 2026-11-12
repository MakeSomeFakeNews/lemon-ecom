<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomDev.project.add')" :edit-title="$t('ecomDev.project.editTitle', { no: form.docNo || '' })">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomDev.project.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomDev.project.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.category')" name="category">
            <MaterialCategorySelect v-model="form.category" usable-only :placeholder="$t('common.placeholder.select')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.brand')" name="brand">
            <a-select v-model:value="form.brand" allow-clear show-search :options="toOptions(dict.wms_material_brand)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.source')" name="source">
            <a-select v-model:value="form.source" allow-clear :options="toOptions(dict.ecom_dev_source)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.owner')" name="ownerId">
            <UserSelect v-model:value="form.ownerId" :label="form.ownerName" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.platform')" name="platform">
            <a-select v-model:value="form.platform" allow-clear :options="toOptions(dict.ecom_platform)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomDev.project.marketplace')" name="marketplace">
            <a-select v-model:value="form.marketplace" allow-clear show-search :options="toOptions(dict.ecom_marketplace)" @change="onMarketplace" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomDev.project.sectionMarket') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomDev.project.competitorUrl')" name="competitorUrl">
            <a-input v-model:value="form.competitorUrl" :maxlength="500" placeholder="https://" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.currency')" name="currency">
            <a-input v-model:value="form.currency" :maxlength="8" :placeholder="$t('ecomDev.project.currencyPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.competitorPrice')" name="competitorPrice">
            <a-input-number v-model:value="form.competitorPrice" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.competitorReviews')" name="competitorReviews">
            <a-input-number v-model:value="form.competitorReviews" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.competitorRating')" name="competitorRating">
            <a-input-number v-model:value="form.competitorRating" :min="0" :max="5" :step="0.1" :precision="1" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.estMonthlySales')" name="estMonthlySales">
            <a-input-number v-model:value="form.estMonthlySales" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.targetPrice')" name="targetPrice">
            <a-input-number v-model:value="form.targetPrice" :min="0" :precision="2" class="full" :addon-after="form.currency || undefined" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomDev.project.estPurchasePrice')" name="estPurchasePrice">
            <a-input-number v-model:value="form.estPurchasePrice" :min="0" :precision="2" class="full" addon-after="CNY" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomDev.project.sectionSize') }}</div>
      <a-row :gutter="16">
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.project.weightKg')" name="weightKg">
            <a-input-number v-model:value="form.weightKg" :min="0" :precision="3" class="full" addon-after="kg" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.project.lengthCm')" name="lengthCm">
            <a-input-number v-model:value="form.lengthCm" :min="0" :precision="1" class="full" addon-after="cm" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.project.widthCm')" name="widthCm">
            <a-input-number v-model:value="form.widthCm" :min="0" :precision="1" class="full" addon-after="cm" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6">
          <a-form-item :label="$t('ecomDev.project.heightCm')" name="heightCm">
            <a-input-number v-model:value="form.heightCm" :min="0" :precision="1" class="full" addon-after="cm" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomDev.project.sectionNotes') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomDev.project.sellingPoints')" name="sellingPoints">
            <a-textarea v-model:value="form.sellingPoints" :maxlength="1000" :rows="4" show-count />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('ecomDev.project.description')" name="description">
            <a-textarea v-model:value="form.description" :maxlength="2000" :rows="4" show-count />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-alert type="info" show-icon :message="$t('ecomDev.project.imageTip')" />
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import UserSelect from '@/components/user/UserSelect.vue'
import MaterialCategorySelect from '@/components/wms/MaterialCategorySelect.vue'
import { addDevProject, getDevProject, updateDevProject } from '@/api/ecom/dev'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'

/**
 * 开发项目表单（页式）：候选产品资料、竞品与预估、尺寸重量、卖点与调研说明。图片在详情中上传。
 * ref 调用 openCreate(defaults)、openEdit(record)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_marketplace', 'ecom_dev_source', 'wms_material_brand')

/** 站点 → 币种（与后端 DevMarketplaces 一致，新站点手工填写） */
const CURRENCY = { US: 'USD', CA: 'CAD', MX: 'MXN', BR: 'BRL', UK: 'GBP', DE: 'EUR', FR: 'EUR', IT: 'EUR', ES: 'EUR', NL: 'EUR', PL: 'PLN', SE: 'SEK', AE: 'AED', SA: 'SAR', JP: 'JPY', AU: 'AUD', SG: 'SGD', MY: 'MYR', TH: 'THB', PH: 'PHP', VN: 'VND', ID: 'IDR' }

const FIELDS = ['name', 'category', 'brand', 'source', 'platform', 'marketplace', 'currency', 'competitorUrl', 'competitorPrice', 'competitorReviews',
  'competitorRating', 'estMonthlySales', 'targetPrice', 'estPurchasePrice', 'weightKg', 'lengthCm', 'widthCm', 'heightCm', 'ownerId', 'sellingPoints',
  'description', 'remark']
const num = (v) => (v == null ? undefined : Number(v))

const emptyForm = () => ({ name: '', platform: 'AMAZON', marketplace: 'US', currency: 'USD', category: undefined, brand: undefined, source: undefined })

const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const d = await getDevProject(record.id)
    const f = { id: d.id, docNo: d.docNo, ownerName: d.ownerName }
    FIELDS.forEach((k) => (f[k] = d[k] ?? undefined))
    ;['competitorPrice', 'competitorRating', 'targetPrice', 'estPurchasePrice', 'weightKg', 'lengthCm', 'widthCm', 'heightCm'].forEach((k) => (f[k] = num(f[k])))
    return f
  },
  create: addDevProject,
  update: updateDevProject,
  toPayload: (f) => Object.fromEntries(FIELDS.map((k) => [k, f[k] === '' || f[k] === undefined ? null : f[k]])),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

const rules = { name: [{ required: true, message: t('ecomDev.project.nameRequired') }] }

function onMarketplace(v) {
  if (v && CURRENCY[v]) form.value.currency = CURRENCY[v]
}

defineExpose({ openCreate: (defaults) => drawer.openCreate(defaults), openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.section-title {
  font-weight: 600;
  margin: 4px 0 12px;
}
</style>
