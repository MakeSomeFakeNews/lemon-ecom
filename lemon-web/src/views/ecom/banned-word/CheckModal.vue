<template>
  <a-modal v-model:open="open" :title="$t('ecomAsst.word.checkTitle')" :footer="null" width="min(760px, 100vw)">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.common.platform')">
            <a-select v-model:value="platform" allow-clear :placeholder="$t('ecomAsst.word.allPlatforms')" :options="toOptions(dict.ecom_platform)" @change="loadWords" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomAsst.common.marketplace')">
            <a-select v-model:value="marketplace" allow-clear show-search :placeholder="$t('ecomAsst.word.allMarketplaces')" :options="toOptions(dict.ecom_marketplace)" @change="loadWords" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('ecomAsst.word.checkText')">
        <a-textarea v-model:value="text" :auto-size="{ minRows: 5, maxRows: 10 }" :maxlength="20000" :placeholder="$t('ecomAsst.word.checkPh')" />
      </a-form-item>
    </a-form>
    <CompliancePanel v-if="text" :result="result" :texts="{ description: text }" :words="words" :loaded="loaded" :show-field="false" />
    <div v-if="text && result.hits.length" class="preview"><HighlightText :text="text" :hits="result.hits" /></div>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { applicableBannedWords } from '@/api/ecom/assistant'
import CompliancePanel from '@/components/ecom/assistant/CompliancePanel.vue'
import HighlightText from '@/components/ecom/assistant/HighlightText.vue'
import { checkListing } from '@/components/ecom/assistant/bannedWords'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/** 文本检测：选择平台 / 站点，粘贴标题或描述，实时高亮命中的违禁词。open() */
const dict = useDict('ecom_platform', 'ecom_marketplace')
const open = ref(false)
const platform = ref()
const marketplace = ref()
const text = ref('')
const words = ref([])
const loaded = ref(false)
const result = computed(() => checkListing(words.value, { description: text.value }))

async function loadWords() {
  loaded.value = false
  words.value = (await applicableBannedWords(platform.value, marketplace.value)) || []
  loaded.value = true
}

function show() {
  open.value = true
  loadWords()
}

defineExpose({ open: show })
</script>

<style scoped>
.preview {
  max-height: 240px;
  overflow: auto;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  line-height: 1.7;
}
</style>
