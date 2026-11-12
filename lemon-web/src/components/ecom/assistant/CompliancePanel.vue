<template>
  <div class="compliance" :class="state">
    <div class="head" @click="expanded = !expanded">
      <component :is="icon" class="icon" />
      <span class="title">{{ summary }}</span>
      <span v-if="!words.length && loaded" class="sub">{{ $t('ecomAsst.check.noWords') }}</span>
      <span class="spacer" />
      <a v-if="result.hits.length" class="toggle">{{ expanded ? $t('ecomAsst.check.collapse') : $t('ecomAsst.check.expand') }}</a>
    </div>
    <div v-if="expanded && result.hits.length" class="list">
      <div v-for="(h, i) in result.hits" :key="i" class="hit">
        <div class="hit-head">
          <span v-if="showField" class="field">{{ fieldLabel(h) }}</span>
          <a-tag :color="h.level === BannedWordLevel.BLOCK ? 'red' : 'orange'" class="word">{{ h.word }}</a-tag>
          <DictTag type="ecom_banned_word_type" :value="h.wordType" />
          <span class="level">{{ h.level === BannedWordLevel.BLOCK ? $t('ecomAsst.check.blockTag') : $t('ecomAsst.check.warnTag') }}</span>
        </div>
        <div class="excerpt"><HighlightText :text="textOf(h)" :hits="[h]" :max-length="90" /></div>
        <div v-if="h.suggestion" class="suggest">{{ $t('ecomAsst.check.suggestion') }}：{{ h.suggestion }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCircleFilled, CloseCircleFilled, ExclamationCircleFilled } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { BannedWordLevel } from '@/constants/enums'
import HighlightText from './HighlightText.vue'

/**
 * 合规检查结果：汇总（通过 / 有阻止发布的词 / 只有提醒），展开后逐条列出命中的词、所在字段、类型、替换建议和高亮的上下文。
 * - result：checkListing 的结果；texts：{ title, bullets, description }；words：适用的词（为空时提示违禁词库为空）；showField 是否显示所在字段
 */
const props = defineProps({
  result: { type: Object, required: true },
  texts: { type: Object, required: true },
  words: { type: Array, default: () => [] },
  loaded: { type: Boolean, default: true },
  defaultExpanded: { type: Boolean, default: true },
  /** 是否显示命中所在的字段（标题 / 五点 / 描述），单段文字检测时不显示 */
  showField: { type: Boolean, default: true }
})
const { t } = useI18n()
useDict('ecom_banned_word_type')
const expanded = ref(props.defaultExpanded)

const state = computed(() => (props.result.block ? 'is-block' : props.result.warn ? 'is-warn' : 'is-ok'))
const icon = computed(() => (props.result.block ? CloseCircleFilled : props.result.warn ? ExclamationCircleFilled : CheckCircleFilled))
const summary = computed(() => {
  const r = props.result
  if (r.block) return t('ecomAsst.check.blockSummary', { block: r.block, warn: r.warn })
  if (r.warn) return t('ecomAsst.check.warnSummary', { warn: r.warn })
  return t('ecomAsst.check.okSummary', { n: props.words.length })
})
const fieldLabel = (h) =>
  h.field === 'bullets' ? t('ecomAsst.check.fieldBullet', { n: h.index + 1 }) : t(h.field === 'title' ? 'ecomAsst.check.fieldTitle' : 'ecomAsst.check.fieldDescription')
const textOf = (h) => (h.field === 'bullets' ? props.texts.bullets?.[h.index] : props.texts[h.field]) || ''
</script>

<style scoped>
.compliance {
  margin-bottom: 12px;
  border: 1px solid;
  border-radius: 8px;
  font-size: 13px;
}
.compliance.is-ok {
  border-color: #b7eb8f;
  background: #f6ffed;
}
.compliance.is-warn {
  border-color: #ffd591;
  background: #fff7e6;
}
.compliance.is-block {
  border-color: #ffa39e;
  background: #fff1f0;
}
.head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
}
.icon {
  font-size: 15px;
}
.is-ok .icon {
  color: #52c41a;
}
.is-warn .icon {
  color: #fa8c16;
}
.is-block .icon {
  color: #f5222d;
}
.title {
  font-weight: 600;
}
.sub {
  color: var(--app-text-3);
}
.spacer {
  flex: 1;
}
.list {
  max-height: 260px;
  overflow: auto;
  padding: 0 12px 8px;
}
.hit {
  padding: 8px 0;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
}
.hit-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.hit-head :deep(.ant-tag) {
  margin-inline-end: 0;
}
.field {
  min-width: 56px;
  color: var(--app-text-2);
}
.word {
  font-weight: 600;
}
.level {
  color: var(--app-text-3);
  font-size: 12px;
}
.excerpt {
  margin-top: 4px;
  color: var(--app-text-2);
}
.suggest {
  margin-top: 2px;
  color: var(--app-text-3);
  font-size: 12px;
}
</style>

<style>
html[data-theme='dark'] .compliance.is-ok {
  border-color: #274916;
  background: rgba(82, 196, 26, 0.08);
}
html[data-theme='dark'] .compliance.is-warn {
  border-color: #593815;
  background: rgba(250, 140, 22, 0.08);
}
html[data-theme='dark'] .compliance.is-block {
  border-color: #5c2223;
  background: rgba(245, 34, 45, 0.08);
}
html[data-theme='dark'] .compliance .hit {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
