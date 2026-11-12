<template>
  <a-modal v-model:open="visible" :title="$t('ecomAuto.test.title')" :footer="null" width="min(1000px, 96vw)" class="auto-test-modal">
    <a-alert type="info" show-icon :message="result?.summary" :description="$t('ecomAuto.test.tip')" class="summary" />
    <div class="samples">
      <div v-for="(s, i) in result?.samples || []" :key="i" class="sample" :class="{ run: willRun(s) }">
        <div class="sample-head">
          <a-tag :bordered="false">{{ $t(`ecomAuto.source.${s.source}`) }}</a-tag>
          <span class="object">{{ s.objectLabel || s.objectKey }}</span>
          <span v-if="s.storeName" class="store">{{ s.storeName }}</span>
          <span class="spacer" />
          <a-tag v-if="!s.inScope" color="default">{{ $t('ecomAuto.test.outScope') }}</a-tag>
          <template v-else>
            <a-tag :color="s.matched ? 'green' : 'default'">{{ s.matched ? $t('ecomAuto.test.matched') : $t('ecomAuto.test.notMatched') }}</a-tag>
            <a-tag v-if="s.matched && s.debounced" color="cyan">{{ $t('ecomAuto.test.debounced') }}</a-tag>
            <a-tag v-if="willRun(s)" color="blue">{{ $t('ecomAuto.test.willRun') }}</a-tag>
          </template>
        </div>
        <div class="detail">{{ s.matchDetail }}</div>
        <a-row :gutter="16">
          <a-col :xs="24" :md="11">
            <div class="label">{{ $t('ecomAuto.test.fields') }}</div>
            <div class="fields">
              <div v-for="(v, k) in shownFields(s)" :key="k" class="field">
                <span class="k">{{ fieldLabel(k) }}</span>
                <DictTag v-if="fieldDict(k)" :type="fieldDict(k)" :value="v" class="v-tag" />
                <span v-else class="v" :title="fmt(v)">{{ fmt(v) }}</span>
              </div>
            </div>
          </a-col>
          <a-col :xs="24" :md="13">
            <div class="label">{{ $t('ecomAuto.test.plan') }}</div>
            <div v-if="!(s.inScope && s.matched)" class="muted">{{ $t('ecomAuto.test.noRun') }}</div>
            <a-timeline v-else class="plan" :class="{ dim: s.debounced }">
              <a-timeline-item v-for="a in s.actions" :key="a.index" :color="a.available ? 'blue' : 'red'">
                <div class="act-name">{{ actionName(a.type) }}<a-tag v-if="!a.available" color="red" :bordered="false" class="na">{{ $t('ecomAuto.test.unavailable') }}</a-tag></div>
                <div class="act-desc">{{ a.description }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-col>
        </a-row>
      </div>
    </div>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { t } from '@/locales'
import { actionOf, fieldOf, useCatalog } from './autoText'

/** 试运行结果：每个样本的来源、是否在范围内 / 满足条件 / 会被防抖、条件判断说明、字段值、将执行的动作（不实际执行） */
const catalog = useCatalog()
const visible = ref(false)
const result = ref(null)
const rule = ref(null)
const MAX_FIELDS = 14

const willRun = (s) => s.inScope && s.matched && !s.debounced
const actionName = (type) => actionOf(catalog.value, type)?.name || type
const fieldLabel = (k) => fieldOf(catalog.value, rule.value?.trigger, k)?.label || k
const fieldDict = (k) => fieldOf(catalog.value, rule.value?.trigger, k)?.dict
const shownFields = (s) => Object.fromEntries(Object.entries(s.fields || {}).filter(([, v]) => v !== null && v !== '' && !(Array.isArray(v) && !v.length)).slice(0, MAX_FIELDS))

function fmt(v) {
  if (Array.isArray(v)) return v.join(', ')
  if (v === true) return t('common.status.yes')
  if (v === false) return t('common.status.no')
  return String(v)
}

defineExpose({
  open(res, form) {
    result.value = res
    rule.value = form
    visible.value = true
  }
})
</script>

<style scoped>
.summary {
  margin-bottom: 12px;
}
.samples {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 65vh;
  overflow-y: auto;
}
.sample {
  padding: 12px 14px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.sample.run {
  border-left: 3px solid var(--app-primary);
}
.sample-head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.object {
  font-weight: 600;
}
.store {
  color: var(--app-text-3);
  font-size: 12px;
}
.spacer {
  flex: 1;
}
.detail {
  margin: 8px 0;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--app-hover);
  color: var(--app-text-2);
  font-size: 12px;
  line-height: 1.7;
  word-break: break-word;
}
.label {
  margin-bottom: 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2px 12px;
}
.field {
  display: flex;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
}
.k {
  color: var(--app-text-3);
  flex: none;
}
.v {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.v-tag {
  margin: 0;
  font-size: 11px;
  line-height: 16px;
}
.muted {
  color: var(--app-text-3);
}
.plan {
  margin-top: 4px;
}
.plan.dim {
  opacity: 0.6;
}
.plan :deep(.ant-timeline-item) {
  padding-bottom: 10px;
}
.act-name {
  font-weight: 500;
}
.na {
  margin-left: 6px;
}
.act-desc {
  color: var(--app-text-2);
  font-size: 12px;
  word-break: break-word;
}
</style>
