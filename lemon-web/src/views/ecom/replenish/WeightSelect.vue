<template>
  <a-space :size="4" wrap>
    <a-select :value="preset" class="preset" :options="presetOptions" @change="onPreset" />
    <template v-if="preset === CUSTOM">
      <a-input-number
        v-for="k in PERIODS"
        :key="k"
        :value="value?.[k]"
        :min="0"
        :max="100"
        :precision="2"
        :placeholder="$t('ecomPurchase.replenish.weightPh')"
        class="weight"
        :addon-before="$t('ecomPurchase.replenish.days', { n: k.slice(1) })"
        @change="(v) => onWeight(k, v)"
      />
    </template>
  </a-space>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CUSTOM, PERIODS, WEIGHT_PRESETS } from './replenish'

/**
 * 日均加权选择：预设（默认 7 天 × 0.5 + 30 天 × 0.5）或自定义 7 / 14 / 30 / 60 天的权重。
 * v-model:value 为 { w7, w14, w30, w60 }
 */
const props = defineProps({ value: Object })
const emit = defineEmits(['update:value'])
const { t } = useI18n()

const same = (a, b) => !!a?.forecast === !!b?.forecast && PERIODS.every((k) => Number(a?.[k] || 0) === Number(b?.[k] || 0))
const custom = ref(false)
const preset = computed(() => (custom.value ? CUSTOM : WEIGHT_PRESETS.find((p) => same(p.weights, props.value))?.key || CUSTOM))
const presetOptions = computed(() => [
  ...WEIGHT_PRESETS.map((p) => ({ value: p.key, label: t(`ecomPurchase.replenish.preset.${p.key}`) })),
  { value: CUSTOM, label: t('ecomPurchase.replenish.custom') }
])

function onPreset(key) {
  custom.value = key === CUSTOM
  if (key !== CUSTOM) emit('update:value', { ...WEIGHT_PRESETS.find((p) => p.key === key).weights })
}
function onWeight(k, v) {
  custom.value = true
  emit('update:value', { ...props.value, forecast: undefined, [k]: v ?? undefined })
}
</script>

<style scoped>
.preset {
  width: 280px;
}
.weight {
  width: 130px;
}
</style>
