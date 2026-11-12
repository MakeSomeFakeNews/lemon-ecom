<template>
  <div class="cond-group" :class="{ nested }">
    <div class="group-head">
      <span class="muted">{{ $t('ecomAuto.editor.match') }}</span>
      <a-segmented v-model:value="group.logic" size="small" :options="logicOptions" />
      <span class="spacer" />
      <a-button v-if="nested" type="text" size="small" danger @click="emit('remove')">
        <template #icon><DeleteOutlined /></template>
      </a-button>
    </div>

    <div v-for="(c, i) in group.items" :key="i" class="cond-row">
      <span class="cond-no">{{ i + 1 }}</span>
      <a-select
        v-model:value="c.field"
        show-search
        option-filter-prop="label"
        :options="fieldOptions"
        :placeholder="$t('ecomAuto.editor.fieldPh')"
        class="field"
        @change="onField(c)"
      />
      <a-select v-model:value="c.operator" :options="operatorOptions(c)" class="op" @change="onOperator(c)" />
      <template v-if="!NO_VALUE.includes(c.operator)">
        <a-select
          v-if="fieldOf(c)?.type === AutoFieldType.BOOLEAN"
          v-model:value="c.value"
          :options="boolOptions"
          class="val"
        />
        <a-select
          v-else-if="fieldOf(c)?.dict"
          :value="enumValue(c)"
          :mode="MULTI.includes(c.operator) ? 'multiple' : undefined"
          :options="dictOptions(fieldOf(c).dict)"
          :max-tag-count="2"
          class="val"
          @change="(v) => (c.value = Array.isArray(v) ? v.join(',') : v)"
        />
        <a-input-number
          v-else-if="fieldOf(c)?.type === AutoFieldType.NUMBER"
          :value="c.value === '' || c.value == null ? null : Number(c.value)"
          :placeholder="$t('ecomAuto.editor.valuePh')"
          class="val"
          @change="(v) => (c.value = v === null || v === undefined ? '' : String(v))"
        />
        <a-input
          v-else
          v-model:value="c.value"
          :maxlength="500"
          :placeholder="MULTI.includes(c.operator) ? $t('ecomAuto.editor.multiValuePh') : $t('ecomAuto.editor.valuePh')"
          class="val"
        />
      </template>
      <span v-else class="val" />
      <a-button type="text" danger @click="group.items.splice(i, 1)">
        <template #icon><MinusCircleOutlined /></template>
      </a-button>
    </div>

    <div v-if="!nested">
      <ConditionGroup
        v-for="(g, i) in group.groups"
        :key="'g' + i"
        :group="g"
        :fields="fields"
        :operators="operators"
        nested
        @remove="group.groups.splice(i, 1)"
      />
    </div>

    <div v-if="!group.items.length && !group.groups?.length" class="empty-tip">{{ $t('ecomAuto.editor.noCondition') }}</div>

    <div class="group-foot">
      <a-button type="dashed" size="small" :disabled="group.items.length >= 20" @click="group.items.push(emptyCondition())">
        <template #icon><PlusOutlined /></template>{{ $t('ecomAuto.editor.addCondition') }}
      </a-button>
      <a-button v-if="!nested" type="dashed" size="small" :disabled="(group.groups || []).length >= 10" @click="addGroup">
        <template #icon><PartitionOutlined /></template>{{ $t('ecomAuto.editor.addGroup') }}
      </a-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { DeleteOutlined, MinusCircleOutlined, PartitionOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { AutoFieldType, AutoLogic, AutoOperator } from '@/constants/enums'
import { useDictStore } from '@/store/dict'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { MULTI, NO_VALUE, emptyCondition, emptyGroup } from './autoText'

/**
 * 条件组编辑：「全部满足 / 任一满足」切换，条件行（字段 → 按字段类型可用的运算符 → 按类型输入值：数字、文本、字典枚举、是否），
 * 顶层可再加条件组（只有一层嵌套）。fields 为触发器的字段，operators 为各字段类型可用的运算符
 */
defineOptions({ name: 'ConditionGroup' })
const props = defineProps({
  group: { type: Object, required: true },
  fields: { type: Array, default: () => [] },
  operators: { type: Object, default: () => ({}) },
  nested: Boolean
})
const emit = defineEmits(['remove'])
const dictStore = useDictStore()

const logicOptions = [
  { value: AutoLogic.AND, label: t('ecomAuto.editor.logicAnd') },
  { value: AutoLogic.OR, label: t('ecomAuto.editor.logicOr') }
]
const boolOptions = [
  { value: 'true', label: t('common.status.yes') },
  { value: 'false', label: t('common.status.no') }
]
const fieldOptions = computed(() => props.fields.map((f) => ({ value: f.key, label: f.label })))
const fieldOf = (c) => props.fields.find((f) => f.key === c.field)

function operatorOptions(c) {
  const f = fieldOf(c)
  const list = f ? props.operators[f.type] || [] : Object.values(AutoOperator)
  return list.map((o) => ({ value: o, label: t(`ecomAuto.op.${o}`) }))
}

function dictOptions(type) {
  dictStore.load(type).catch(() => {})
  return toOptions(dictStore.cache[type] || [])
}

const enumValue = (c) => {
  if (!c.value) return MULTI.includes(c.operator) ? [] : undefined
  return MULTI.includes(c.operator) ? String(c.value).split(',').filter(Boolean) : c.value
}

/** 换字段：运算符不适用时改为第一个可用的，清空值（是否字段默认「是」） */
function onField(c) {
  const f = fieldOf(c)
  const ops = f ? props.operators[f.type] || [] : []
  if (!ops.includes(c.operator)) c.operator = ops[0] || AutoOperator.EQ
  c.value = f?.type === AutoFieldType.BOOLEAN ? 'true' : ''
}

function onOperator(c) {
  if (NO_VALUE.includes(c.operator)) c.value = ''
  else if (!MULTI.includes(c.operator) && String(c.value).includes(',') && fieldOf(c)?.dict) c.value = String(c.value).split(',')[0]
}

function addGroup() {
  const g = emptyGroup()
  g.logic = props.group.logic === AutoLogic.AND ? AutoLogic.OR : AutoLogic.AND
  g.items.push(emptyCondition())
  if (!props.group.groups) props.group.groups = []
  props.group.groups.push(g)
}
</script>

<style scoped>
.cond-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cond-group.nested {
  margin: 4px 0 4px 28px;
  padding: 10px 12px;
  border: 1px dashed var(--app-border);
  border-radius: 8px;
  background: var(--app-hover);
}
.group-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.muted {
  color: var(--app-text-3);
  font-size: 13px;
}
.spacer {
  flex: 1;
}
.cond-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cond-no {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--app-hover);
  color: var(--app-text-2);
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  flex: none;
}
.field {
  width: 200px;
}
.op {
  width: 110px;
}
.val {
  flex: 1;
  min-width: 160px;
}
.empty-tip {
  padding: 8px 12px;
  color: var(--app-text-3);
  font-size: 13px;
  background: var(--app-hover);
  border-radius: 6px;
}
.group-foot {
  display: flex;
  gap: 8px;
}
</style>
