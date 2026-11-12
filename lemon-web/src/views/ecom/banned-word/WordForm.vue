<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomAsst.word.add')" :edit-title="$t('ecomAsst.word.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAsst.word.word')" name="word" :extra="$t('ecomAsst.word.wordExtra')">
        <a-input v-model:value="form.word" :maxlength="128" show-count />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.word.wordType')" name="wordType">
            <a-select v-model:value="form.wordType" :options="toOptions(dict.ecom_banned_word_type)" @change="onTypeChange" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.word.level')" name="level">
            <a-select v-model:value="form.level" :options="toOptions(dict.ecom_banned_word_level)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.word.matchMode')" name="matchMode" :extra="$t('ecomAsst.word.matchExtra')">
            <a-radio-group v-model:value="form.matchMode" :options="toOptions(dict.ecom_banned_word_match)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-radio-group v-model:value="form.status" :options="toOptions(dict.sys_common_status, true)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.common.platform')" name="platform">
            <a-select v-model:value="form.platform" allow-clear :placeholder="$t('ecomAsst.word.allPlatforms')" :options="toOptions(dict.ecom_platform)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.common.marketplace')" name="marketplace">
            <a-select v-model:value="form.marketplace" allow-clear show-search :placeholder="$t('ecomAsst.word.allMarketplaces')" :options="toOptions(dict.ecom_marketplace)" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('ecomAsst.word.suggestion')" name="suggestion">
        <a-textarea v-model:value="form.suggestion" :maxlength="255" show-count :auto-size="{ minRows: 2, maxRows: 4 }" :placeholder="$t('ecomAsst.word.suggestionPh')" />
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
import { addBannedWord, getBannedWord, updateBannedWord } from '@/api/ecom/assistant'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { BannedWordLevel, BannedWordMatch } from '@/constants/enums'
import { STATUS_ENABLED } from '@/utils/constants'
import { toOptions } from '@/utils/dict'

/** 新增 / 修改违禁词：夸大宣传类默认只提醒，其他类型默认阻止发布。open(record?) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_banned_word_type', 'ecom_banned_word_level', 'ecom_banned_word_match', 'ecom_platform', 'ecom_marketplace', 'sys_common_status')
const WARN_TYPES = ['EXAGGERATION']
const rules = {
  word: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAsst.word.word') }) }],
  wordType: [{ required: true, message: t('common.rule.required', { name: t('ecomAsst.word.wordType') }) }],
  level: [{ required: true, message: t('common.rule.required', { name: t('ecomAsst.word.level') }) }]
}
const emptyForm = () => ({
  id: undefined, word: '', wordType: undefined, level: BannedWordLevel.BLOCK, matchMode: BannedWordMatch.WORD,
  platform: undefined, marketplace: undefined, suggestion: '', status: STATUS_ENABLED, remark: ''
})
const drawer = useFormDrawer({
  emptyForm,
  fetch: (r) => getBannedWord(r.id),
  create: addBannedWord,
  update: updateBannedWord,
  toPayload: ({ word, wordType, level, matchMode, platform, marketplace, suggestion, status, remark }) => ({
    word, wordType, level, matchMode, platform: platform || null, marketplace: marketplace || null, suggestion, status, remark
  }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

/** 新增时按类型带出默认的处理方式 */
function onTypeChange(v) {
  if (!form.value.id) form.value.level = WARN_TYPES.includes(v) ? BannedWordLevel.WARN : BannedWordLevel.BLOCK
}

defineExpose({ open: (r) => (r ? drawer.openEdit(r) : drawer.openCreate()) })
</script>
