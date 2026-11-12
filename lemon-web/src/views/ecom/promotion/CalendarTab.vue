<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <a-button @click="shift(-1)"><template #icon><LeftOutlined /></template>{{ $t('ecomOper.promo.prevMonth') }}</a-button>
        <a-date-picker v-model:value="month" picker="month" value-format="YYYY-MM" :allow-clear="false" @change="load" />
        <a-button @click="shift(1)">{{ $t('ecomOper.promo.nextMonth') }}<RightOutlined /></a-button>
        <a-button @click="thisMonth">{{ $t('ecomOper.promo.today') }}</a-button>
        <StoreSelect v-model:value="storeId" :placeholder="$t('ecomOper.common.allStores')" class="store" />
      </a-space>
      <span class="sub-text">{{ $t('ecomOper.promo.calendarTip') }}</span>
    </div>
    <a-spin :spinning="loading" class="spin">
      <div class="gantt-wrap">
        <div class="gantt" :style="{ '--days': days.length }">
          <div class="row head">
            <div class="name-cell">{{ $t('ecomOper.promo.name') }}</div>
            <div class="days">
              <div v-for="d in days" :key="d.date" class="day" :class="{ weekend: d.weekend, today: d.today }">
                <div>{{ d.day }}</div>
                <div class="week">{{ d.week }}</div>
              </div>
            </div>
          </div>
          <div v-for="p in rows" :key="p.id" class="row">
            <div class="name-cell">
              <a class="ellipsis" :title="p.name" @click="emit('detail', p.id)">{{ p.name }}</a>
              <div class="sub-text ellipsis">{{ p.storeName }}</div>
            </div>
            <div class="days">
              <div v-for="d in days" :key="d.date" class="day" :class="{ weekend: d.weekend, today: d.today }" />
              <a-tooltip :title="tooltip(p)">
                <div class="bar" :class="{ draft: isDraftItem(p) }" :style="barStyle(p)" @click="emit('detail', p.id)">
                  <span class="ellipsis">{{ typeLabel(p.promoType) }} · {{ p.name }}</span>
                </div>
              </a-tooltip>
            </div>
          </div>
          <a-empty v-if="!loading && !rows.length" :description="$t('ecomOper.promo.calendarEmpty')" class="empty" />
        </div>
      </div>
    </a-spin>
    <div class="legend">
      <span v-for="d in dict.ecom_promo_type || []" :key="d.value" class="legend-item">
        <i :style="{ background: colorOf(d.value) }" />{{ d.label }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { promotionCalendar } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { EcomPromoStatus } from '@/constants/enums'

/** 促销日历：按月的甘特图，每个未取消的活动一行，颜色区分类型（草稿为虚线框），点击查看详情。ref 调用 reload() */
const emit = defineEmits(['detail'])
const dict = useDict('ecom_promo_type', 'ecom_promo_status', 'ecom_promo_phase')

const month = ref(dayjs().format('YYYY-MM'))
const storeId = ref()
const loading = ref(false)
const rows = ref([])

/** 字典颜色名 → 色值（没有颜色时按顺序取） */
const PALETTE = { blue: '#1677ff', red: '#f5222d', cyan: '#13c2c2', orange: '#fa8c16', magenta: '#eb2f96', purple: '#722ed1', green: '#52c41a', gold: '#faad14', volcano: '#fa541c', geekblue: '#2f54eb', lime: '#a0d911' }
const FALLBACK = Object.values(PALETTE)
function colorOf(type) {
  const list = dict.ecom_promo_type || []
  const d = findDict(list, type)
  if (d?.tagColor && PALETTE[d.tagColor]) return PALETTE[d.tagColor]
  const i = Math.max(0, list.findIndex((x) => x.value === type))
  return FALLBACK[i % FALLBACK.length]
}
const typeLabel = (v) => findDict(dict.ecom_promo_type, v)?.label || v
const isDraftItem = (p) => p.status === EcomPromoStatus.DRAFT

const first = computed(() => dayjs(`${month.value}-01`))
const days = computed(() => {
  const n = first.value.daysInMonth()
  const today = dayjs().format('YYYY-MM-DD')
  return Array.from({ length: n }, (_, i) => {
    const d = first.value.add(i, 'day')
    return { date: d.format('YYYY-MM-DD'), day: i + 1, week: d.format('dd'), weekend: d.day() === 0 || d.day() === 6, today: d.format('YYYY-MM-DD') === today }
  })
})

/** 条形位置：按小时折算到当月的百分比 */
function barStyle(p) {
  const start = first.value
  const end = start.add(1, 'month')
  const total = end.diff(start, 'hour')
  const s = Math.max(0, dayjs(p.startTime).diff(start, 'hour'))
  const e = Math.min(total, dayjs(p.endTime).diff(start, 'hour'))
  const color = colorOf(p.promoType)
  return {
    left: `${(s / total) * 100}%`,
    width: `${Math.max(((e - s) / total) * 100, 1.5)}%`,
    background: isDraftItem(p) ? 'transparent' : color,
    borderColor: color,
    color: isDraftItem(p) ? color : '#fff'
  }
}
function tooltip(p) {
  const phase = p.phase ? findDict(dict.ecom_promo_phase, p.phase)?.label : findDict(dict.ecom_promo_status, p.status)?.label
  return `${p.name} [${typeLabel(p.promoType)}${phase ? ' · ' + phase : ''}] ${String(p.startTime).slice(0, 16)} ~ ${String(p.endTime).slice(0, 16)}`
}

async function load() {
  loading.value = true
  try {
    rows.value = (await promotionCalendar({
      beginDate: first.value.format('YYYY-MM-DD'),
      endDate: first.value.endOf('month').format('YYYY-MM-DD'),
      storeId: storeId.value || undefined
    })) || []
  } finally {
    loading.value = false
  }
}
function shift(n) {
  month.value = first.value.add(n, 'month').format('YYYY-MM')
  load()
}
function thisMonth() {
  month.value = dayjs().format('YYYY-MM')
  load()
}
watch(storeId, load)
onMounted(load)
defineExpose({ reload: load })
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.store {
  width: 220px;
}
.spin {
  flex: 1;
  min-height: 0;
}
.gantt-wrap {
  overflow: auto;
  max-height: 100%;
}
.gantt {
  min-width: calc(200px + var(--days) * 32px);
}
.row {
  display: flex;
  border-bottom: 1px solid var(--app-border, #f0f0f0);
  min-height: 44px;
}
.row.head {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--app-bg-container, #fff);
  font-size: 12px;
}
.name-cell {
  width: 200px;
  flex: none;
  padding: 4px 8px;
  border-right: 1px solid var(--app-border, #f0f0f0);
  min-width: 0;
}
.days {
  flex: 1;
  display: flex;
  position: relative;
}
.day {
  flex: 1;
  text-align: center;
  border-right: 1px dashed var(--app-border, #f0f0f0);
  padding-top: 2px;
}
.day.weekend {
  background: rgba(0, 0, 0, 0.02);
}
.day.today {
  background: rgba(22, 119, 255, 0.08);
}
.week {
  color: var(--app-text-3);
}
.bar {
  position: absolute;
  top: 9px;
  height: 26px;
  border-radius: 4px;
  border: 1px solid;
  padding: 0 6px;
  font-size: 12px;
  line-height: 24px;
  cursor: pointer;
  overflow: hidden;
}
.bar.draft {
  border-style: dashed;
}
.ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.empty {
  margin: 32px 0;
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
}
.legend-item i {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin-right: 4px;
}
</style>
