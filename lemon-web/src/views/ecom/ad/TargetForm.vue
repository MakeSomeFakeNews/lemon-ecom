<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomAds.target.add')" :edit-title="$t('ecomAds.target.edit')" width="min(480px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAds.target.type')" name="targetType">
        <a-select v-model:value="form.targetType" :options="typeOptions" :disabled="!!form.id" @change="onTypeChange" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.target.text')" name="targetText">
        <a-input v-model:value="form.targetText" :maxlength="255" :placeholder="$t('ecomAds.target.textPh')" />
      </a-form-item>
      <a-form-item v-if="needsMatch" :label="$t('ecomAds.target.matchType')" name="matchType">
        <a-select v-model:value="form.matchType" :options="matchOptions" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.common.platformId')" name="platformTargetId">
        <a-input v-model:value="form.platformTargetId" :maxlength="64" />
      </a-form-item>
      <a-form-item v-if="!form.id && form.targetType !== EcomAdTargetType.NEGATIVE" :label="$t('ecomAds.target.bid')" name="bid">
        <a-input-number v-model:value="form.bid" :min="0" :precision="2" class="full" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import FormDrawer from '@/components/FormDrawer.vue'
import { addTarget, updateTarget } from '@/api/ecom/ads'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { EcomAdMatchType, EcomAdTargetType } from '@/constants/enums'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'

/** 投放新增 / 编辑：关键词选精准 / 词组 / 广泛，否定关键词选否定精准 / 否定词组，商品与自动投放不选匹配方式 */
const emit = defineEmits(['success'])
const dict = useDict('ecom_ad_match_type')
const typeOptions = Object.values(EcomAdTargetType).map((v) => ({ value: v, label: t(`ecomAds.enum.targetType.${v}`) }))
const NEGATIVE = [EcomAdMatchType.NEGATIVE_EXACT, EcomAdMatchType.NEGATIVE_PHRASE]
const rules = {
  targetType: [{ required: true, message: t('common.rule.required', { name: t('ecomAds.target.type') }) }],
  targetText: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAds.target.text') }) }],
  matchType: [{ required: true, message: t('common.rule.required', { name: t('ecomAds.target.matchType') }) }]
}

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, adGroupId: undefined, targetType: EcomAdTargetType.KEYWORD, targetText: '', matchType: EcomAdMatchType.EXACT,
    platformTargetId: '', bid: undefined }),
  fetch: (r) => ({ id: r.id, adGroupId: r.adGroupId, targetType: r.targetType, targetText: r.name, matchType: r.matchType, platformTargetId: r.platformId || '' }),
  create: addTarget,
  update: updateTarget,
  toPayload: (f) => ({
    adGroupId: f.adGroupId,
    targetType: f.targetType,
    targetText: f.targetText.trim(),
    matchType: needsMatchOf(f.targetType) ? f.matchType : null,
    platformTargetId: f.platformTargetId || null,
    bid: f.bid ?? null
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

const needsMatchOf = (type) => type === EcomAdTargetType.KEYWORD || type === EcomAdTargetType.NEGATIVE
const needsMatch = computed(() => needsMatchOf(form.value.targetType))
const matchOptions = computed(() =>
  toOptions(dict.ecom_ad_match_type).filter((o) => (form.value.targetType === EcomAdTargetType.NEGATIVE) === NEGATIVE.includes(o.value))
)
function onTypeChange(type) {
  form.value.matchType = type === EcomAdTargetType.NEGATIVE ? EcomAdMatchType.NEGATIVE_EXACT : type === EcomAdTargetType.KEYWORD ? EcomAdMatchType.EXACT : undefined
}

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
