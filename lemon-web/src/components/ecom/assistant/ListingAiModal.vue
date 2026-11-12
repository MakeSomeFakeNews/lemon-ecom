<template>
  <a-modal v-model:open="open" :title="$t('ecomAsst.listing.title')" width="min(1100px, 100vw)" :footer="null" :mask-closable="false" class="listing-ai-modal">
    <a-row :gutter="20">
      <a-col :xs="24" :md="10" class="settings">
        <div class="sec-title">{{ $t('ecomAsst.listing.settings') }}</div>
        <a-form layout="vertical">
          <a-form-item :label="$t('ecomAsst.listing.parts')">
            <a-checkbox-group v-model:value="opts.parts" :options="partOptions" />
          </a-form-item>
          <a-form-item :label="$t('ecomAsst.listing.language')" :extra="$t('ecomAsst.listing.languageExtra')">
            <a-select v-model:value="opts.language" allow-clear :placeholder="$t('ecomAsst.listing.languageAuto')" :options="toOptions(dict.ecom_cs_language)" />
          </a-form-item>
          <a-form-item>
            <template #label>
              {{ $t('ecomAsst.listing.keywords') }}
              <a-tooltip :title="$t('ecomAsst.listing.keywordsTip')"><QuestionCircleOutlined class="tip" /></a-tooltip>
            </template>
            <div class="kw-bar">
              <a-switch v-model:checked="sameCategory" size="small" @change="loadKeywords" />
              <span>{{ $t('ecomAsst.listing.sameCategory') }}</span>
              <span class="spacer" />
              <span class="sub-text">{{ $t('ecomAsst.listing.selectedN', { n: opts.keywords.length }) }}</span>
            </div>
            <a-table
              row-key="searchTerm"
              size="small"
              :columns="kwColumns"
              :data-source="keywords"
              :loading="kwLoading"
              :pagination="false"
              :scroll="{ y: 220 }"
              :row-selection="{ selectedRowKeys: picked, onChange: onPick }"
              :locale="{ emptyText: $t('ecomAsst.listing.noKeywords') }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'cvr'">{{ record.cvrPercent == null ? '-' : `${Number(record.cvrPercent).toFixed(1)}%` }}</template>
              </template>
            </a-table>
            <a-select v-model:value="custom" mode="tags" class="custom-kw" :placeholder="$t('ecomAsst.listing.customKeywords')" :token-separators="[',']" />
          </a-form-item>
          <a-form-item :label="$t('ecomAsst.listing.instruction')">
            <a-textarea v-model:value="opts.instruction" :maxlength="500" show-count :auto-size="{ minRows: 2, maxRows: 4 }" :placeholder="$t('ecomAsst.listing.instructionPh')" />
          </a-form-item>
          <a-button type="primary" block :loading="running" :disabled="!opts.parts.length" @click="run">
            <template #icon><RobotOutlined /></template>{{ result ? $t('ecomAsst.listing.regenerate') : $t('ecomAsst.listing.generate') }}
          </a-button>
        </a-form>
      </a-col>

      <a-col :xs="24" :md="14">
        <div class="sec-title">{{ $t('ecomAsst.listing.result') }}<span v-if="result?.language" class="sub-text"> · {{ result.language }}</span></div>
        <a-spin :spinning="running">
          <div v-if="!result" class="placeholder">
            <RobotOutlined class="ph-icon" />
            <div>{{ $t('ecomAsst.listing.placeholder') }}</div>
          </div>
          <template v-else>
            <CompliancePanel :result="check" :texts="result" :words="words" :default-expanded="false" />
            <div v-if="result.title" class="part">
              <a-checkbox v-model:checked="apply.title" class="part-check">{{ $t('ecomAsst.listing.partTitle') }} <span class="sub-text">({{ (result.title || '').length }})</span></a-checkbox>
              <div class="part-body"><HighlightText :text="result.title" :hits="hitsOf('title')" /></div>
            </div>
            <div v-if="result.bullets?.length" class="part">
              <a-checkbox v-model:checked="apply.bullets" class="part-check">{{ $t('ecomAsst.listing.partBullets') }}</a-checkbox>
              <ol class="part-body bullets">
                <li v-for="(b, i) in result.bullets" :key="i"><HighlightText :text="b" :hits="hitsOf('bullets', i)" /></li>
              </ol>
            </div>
            <div v-if="result.description" class="part">
              <a-checkbox v-model:checked="apply.description" class="part-check">{{ $t('ecomAsst.listing.partDescription') }}</a-checkbox>
              <div class="part-body desc"><HighlightText :text="result.description" :hits="hitsOf('description')" /></div>
            </div>
            <div class="footer">
              <span class="sub-text">{{ $t('ecomAsst.listing.applyTip') }}</span>
              <span class="spacer" />
              <a-button @click="open = false">{{ $t('common.action.cancel') }}</a-button>
              <a-button type="primary" :disabled="!apply.title && !apply.bullets && !apply.description" @click="confirmApply">{{ $t('ecomAsst.listing.apply') }}</a-button>
            </div>
          </template>
        </a-spin>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { QuestionCircleOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { generateListingCopy, listingKeywords } from '@/api/ecom/assistant'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import CompliancePanel from './CompliancePanel.vue'
import HighlightText from './HighlightText.vue'
import { checkListing } from './bannedWords'

/**
 * AI 写 Listing：选择要生成的部分、输出语言（默认按站点）、关键词（同物料 / 同品类在广告搜索词报告中出单的词，可补充自定义词）与补充要求，
 * 生成后预览（高亮命中的违禁词），勾选要应用的部分后触发 apply。open()
 */
const props = defineProps({
  itemId: Number,
  form: { type: Object, required: true },
  words: { type: Array, default: () => [] }
})
const emit = defineEmits(['apply'])
const { t } = useI18n()
const dict = useDict('ecom_cs_language')
const open = ref(false)
const running = ref(false)
const kwLoading = ref(false)
const sameCategory = ref(false)
const keywords = ref([])
const picked = ref([])
const custom = ref([])
const result = ref(null)
const opts = reactive({ parts: ['title', 'bullets', 'description'], language: undefined, instruction: '', keywords: [] })
const apply = reactive({ title: true, bullets: true, description: true })
const partOptions = computed(() => [
  { value: 'title', label: t('ecomAsst.listing.partTitle') },
  { value: 'bullets', label: t('ecomAsst.listing.partBullets') },
  { value: 'description', label: t('ecomAsst.listing.partDescription') }
])
const kwColumns = [
  { title: t('ecomAsst.listing.searchTerm'), dataIndex: 'searchTerm', ellipsis: true },
  { title: t('ecomAsst.listing.orders'), dataIndex: 'orders', width: 64, align: 'right' },
  { title: t('ecomAsst.listing.clicks'), dataIndex: 'clicks', width: 64, align: 'right' },
  { title: t('ecomAsst.listing.cvr'), key: 'cvr', width: 70, align: 'right' }
]
const check = computed(() => checkListing(props.words, result.value || {}))
const hitsOf = (field, index) => check.value.byField[index == null ? field : `${field}.${index}`] || []

function onPick(keys) {
  picked.value = keys
  syncKeywords()
}
const syncKeywords = () => (opts.keywords = [...new Set([...picked.value, ...custom.value.map((s) => s.trim()).filter(Boolean)])].slice(0, 30))

async function loadKeywords() {
  if (!props.itemId) return
  kwLoading.value = true
  try {
    const r = await listingKeywords({ itemId: props.itemId, sameCategory: sameCategory.value })
    keywords.value = r?.rows || []
    sameCategory.value = !!r?.sameCategory
    // 默认勾选出单最多的前 5 个
    if (!picked.value.length) picked.value = keywords.value.slice(0, 5).map((k) => k.searchTerm)
    syncKeywords()
  } finally {
    kwLoading.value = false
  }
}

function show() {
  result.value = null
  picked.value = []
  custom.value = []
  Object.assign(apply, { title: true, bullets: true, description: true })
  open.value = true
  loadKeywords()
}

async function run() {
  syncKeywords()
  running.value = true
  try {
    const f = props.form
    result.value = await generateListingCopy({
      itemId: props.itemId,
      title: f.title,
      bullets: (f.bullets || []).filter((b) => b && b.trim()),
      description: f.description,
      parts: opts.parts,
      keywords: opts.keywords,
      language: opts.language,
      instruction: opts.instruction
    })
    Object.assign(apply, { title: opts.parts.includes('title'), bullets: opts.parts.includes('bullets'), description: opts.parts.includes('description') })
  } finally {
    running.value = false
  }
}

function confirmApply() {
  const r = result.value
  const out = {}
  if (apply.title && r.title) out.title = r.title
  if (apply.bullets && r.bullets?.length) out.bullets = [...r.bullets]
  if (apply.description && r.description) out.description = r.description
  emit('apply', out)
  message.success(t('ecomAsst.listing.applied'))
  open.value = false
}

defineExpose({ open: show })
</script>

<style scoped>
.sec-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
}
.settings {
  border-right: 1px solid var(--app-border);
}
.kw-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.custom-kw {
  width: 100%;
  margin-top: 8px;
}
.tip {
  margin-left: 4px;
  color: var(--app-text-3);
}
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  color: var(--app-text-3);
  text-align: center;
}
.ph-icon {
  margin-bottom: 12px;
  font-size: 40px;
  color: #b37feb;
}
.part {
  margin-bottom: 14px;
}
.part-check {
  font-weight: 600;
}
.part-body {
  margin-top: 6px;
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  background: var(--app-bg, rgba(0, 0, 0, 0.02));
  line-height: 1.7;
}
.bullets {
  padding-left: 30px;
}
.bullets li {
  margin-bottom: 4px;
}
.desc {
  max-height: 260px;
  overflow: auto;
}
.footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
@media (max-width: 767px) {
  .settings {
    border-right: none;
    margin-bottom: 16px;
  }
}
</style>
