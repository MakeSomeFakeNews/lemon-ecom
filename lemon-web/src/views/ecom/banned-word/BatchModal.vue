<template>
  <a-modal v-model:open="open" :title="$t('ecomAsst.word.batchTitle')" :confirm-loading="saving" width="min(620px, 100vw)" @ok="submit">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAsst.word.batchWords')" name="words" :extra="$t('ecomAsst.word.batchExtra', { n: count })">
        <a-textarea v-model:value="form.words" :auto-size="{ minRows: 6, maxRows: 12 }" :maxlength="10000" :placeholder="$t('ecomAsst.word.batchPh')" />
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.word.wordType')" name="wordType">
            <a-select v-model:value="form.wordType" :options="toOptions(dict.ecom_banned_word_type)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.word.level')" name="level">
            <a-select v-model:value="form.level" :options="toOptions(dict.ecom_banned_word_level)" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('ecomAsst.word.matchMode')" name="matchMode">
            <a-select v-model:value="form.matchMode" :options="toOptions(dict.ecom_banned_word_match)" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('ecomAsst.common.platform')" name="platform">
            <a-select v-model:value="form.platform" allow-clear :placeholder="$t('ecomAsst.word.allPlatforms')" :options="toOptions(dict.ecom_platform)" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('ecomAsst.common.marketplace')" name="marketplace">
            <a-select v-model:value="form.marketplace" allow-clear show-search :placeholder="$t('ecomAsst.word.allMarketplaces')" :options="toOptions(dict.ecom_marketplace)" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('ecomAsst.word.suggestion')" name="suggestion">
        <a-input v-model:value="form.suggestion" :maxlength="255" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { batchAddBannedWord } from '@/api/ecom/assistant'
import { useDict } from '@/composables/useDict'
import { BannedWordLevel, BannedWordMatch } from '@/constants/enums'
import { toOptions } from '@/utils/dict'

/** 批量添加违禁词：一行一个，其余属性相同，已存在的跳过。open() */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_banned_word_type', 'ecom_banned_word_level', 'ecom_banned_word_match', 'ecom_platform', 'ecom_marketplace')
const open = ref(false)
const saving = ref(false)
const formRef = ref()
const empty = () => ({ words: '', wordType: undefined, level: BannedWordLevel.BLOCK, matchMode: BannedWordMatch.WORD, platform: undefined, marketplace: undefined, suggestion: '' })
const form = reactive(empty())
const count = computed(() => new Set(form.words.split(/\r?\n/).map((s) => s.trim().toLowerCase()).filter(Boolean)).size)
const rules = {
  words: [{ required: true, whitespace: true, message: t('ecomAsst.word.batchRequired') }],
  wordType: [{ required: true, message: t('common.rule.required', { name: t('ecomAsst.word.wordType') }) }],
  level: [{ required: true, message: t('common.rule.required', { name: t('ecomAsst.word.level') }) }]
}

function show() {
  Object.assign(form, empty())
  formRef.value?.clearValidate()
  open.value = true
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const n = await batchAddBannedWord({ ...form, platform: form.platform || null, marketplace: form.marketplace || null })
    message.success(t('ecomAsst.word.batchDone', { n, skipped: Math.max(0, count.value - n) }))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
