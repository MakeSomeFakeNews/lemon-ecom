<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomAds.campaign.add')" :edit-title="$t('ecomAds.campaign.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAds.common.store')" name="storeId">
        <StoreSelect v-model:value="form.storeId" :disabled="!!form.id" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.campaign.name')" name="name">
        <a-input v-model:value="form.name" :maxlength="128" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.campaign.adType')" name="adType">
            <a-select v-model:value="form.adType" :options="toOptions(dict.ecom_ad_type)" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.common.platformId')" name="platformCampaignId">
            <a-input v-model:value="form.platformCampaignId" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col v-if="!form.id" :span="12">
          <a-form-item :label="$t('ecomAds.campaign.dailyBudget')" name="dailyBudget">
            <a-input-number v-model:value="form.dailyBudget" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.campaign.biddingStrategy')" name="biddingStrategy">
            <a-input v-model:value="form.biddingStrategy" :maxlength="32" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.campaign.startDate')" name="startDate">
            <a-date-picker v-model:value="form.startDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAds.campaign.endDate')" name="endDate">
            <a-date-picker v-model:value="form.endDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('ecomAds.campaign.tags')" name="tags">
        <a-select v-model:value="form.tags" mode="tags" :open="false" :token-separators="[',']" :placeholder="$t('ecomAds.campaign.tagsPh')" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.campaign.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import FormDrawer from '@/components/FormDrawer.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { addCampaign, getCampaign, updateCampaign } from '@/api/ecom/ads'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/** 广告活动新增 / 编辑（抽屉）：日预算只在新建时填写，之后用「修改预算」同步到平台 */
const emit = defineEmits(['success'])
const dict = useDict('ecom_ad_type')
const rules = {
  storeId: [{ required: true, message: t('common.rule.required', { name: t('ecomAds.common.store') }) }],
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAds.campaign.name') }) }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, storeId: undefined, name: '', adType: undefined, platformCampaignId: '', dailyBudget: undefined,
    biddingStrategy: '', startDate: undefined, endDate: undefined, tags: [], remark: '' }),
  fetch: async (record) => {
    const d = await getCampaign(record.id)
    return { ...d, tags: d.tags ? d.tags.split(',') : [], dailyBudget: d.dailyBudget == null ? undefined : Number(d.dailyBudget) }
  },
  create: addCampaign,
  update: updateCampaign,
  toPayload: (f) => ({
    storeId: f.storeId,
    name: f.name.trim(),
    adType: f.adType || null,
    platformCampaignId: f.platformCampaignId || null,
    dailyBudget: f.dailyBudget ?? null,
    biddingStrategy: f.biddingStrategy || null,
    startDate: f.startDate || null,
    endDate: f.endDate || null,
    tags: f.tags?.length ? f.tags.join(',') : null,
    remark: f.remark || null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
