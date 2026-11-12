<template>
  <a-drawer :open="open" :title="$t('ecomAnalytics.forecast.events')" width="min(860px, 100vw)" @close="open = false">
    <div class="toolbar">
      <StoreSelect v-model:value="storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" @change="load" />
      <a-checkbox v-model:checked="upcoming" @change="load">{{ $t('ecomAnalytics.forecast.upcomingOnly') }}</a-checkbox>
      <a-button v-perm="'ecom:forecast:event'" type="primary" @click="drawer.openCreate({ storeId: storeId })">
        <template #icon><PlusOutlined /></template>{{ $t('ecomAnalytics.forecast.addEvent') }}
      </a-button>
    </div>
    <div class="sub-text tip">{{ $t('ecomAnalytics.forecast.eventTip') }}</div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 760 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'range'">{{ record.startDate }} ~ {{ record.endDate }}</template>
        <template v-else-if="column.key === 'store'">{{ record.storeName || $t('ecomAnalytics.common.allStores') }}</template>
        <template v-else-if="column.key === 'factor'">× {{ qty(record.factor, 2) }}</template>
        <template v-else-if="column.key === 'action'">
          <TableActions
            :actions="[
              { type: 'edit', perm: 'ecom:forecast:event', onClick: () => drawer.openEdit(record) },
              { type: 'delete', perm: 'ecom:forecast:event', confirm: $t('ecomAnalytics.forecast.confirmDeleteEvent', { name: record.name }), onClick: () => remove(record.id) }
            ]"
          />
        </template>
      </template>
    </a-table>

    <FormDrawer :drawer="drawer" :create-title="$t('ecomAnalytics.forecast.addEvent')" :edit-title="$t('ecomAnalytics.forecast.editEvent')" width="min(480px, 100vw)">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item :label="$t('ecomAnalytics.forecast.eventName')" name="name">
          <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecomAnalytics.forecast.eventNamePh')" />
        </a-form-item>
        <a-form-item :label="$t('ecomAnalytics.forecast.eventRange')" name="range">
          <a-range-picker v-model:value="form.range" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('ecomAnalytics.common.store')" name="storeId" :extra="$t('ecomAnalytics.forecast.eventStoreExtra')">
          <StoreSelect v-model:value="form.storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('ecomAnalytics.forecast.factor')" name="factor" :extra="$t('ecomAnalytics.forecast.factorExtra')">
          <a-input-number v-model:value="form.factor" :min="0.01" :max="20" :step="0.1" :precision="2" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')" name="remark">
          <a-textarea v-model:value="form.remark" :maxlength="255" :rows="2" />
        </a-form-item>
      </a-form>
    </FormDrawer>
  </a-drawer>
</template>

<script setup>
import { ref } from 'vue'
import dayjs from 'dayjs'
import { PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import TableActions from '@/components/common/TableActions.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { addForecastEvent, deleteForecastEvent, listForecastEvents, updateForecastEvent } from '@/api/ecom/analytics'
import { useAction } from '@/composables/useAction'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { qty } from '@/utils/number'
import { t } from '@/locales'

/**
 * 促销 / 节假日系数日历：日期区间（含首尾）内的预测量 × 系数，店铺为空表示全部店铺；历史中这些日期的销量先 ÷ 系数作为基线。
 * 修改后在下次生成预测时生效。open(storeId) 打开
 */
const open = ref(false)
const loading = ref(false)
const storeId = ref()
const upcoming = ref(true)
const rows = ref([])

const columns = [
  { title: t('ecomAnalytics.forecast.eventName'), dataIndex: 'name', width: 160, ellipsis: true },
  { title: t('ecomAnalytics.forecast.eventRange'), key: 'range', width: 200 },
  { title: t('ecomAnalytics.common.store'), key: 'store', width: 150, ellipsis: true },
  { title: t('ecomAnalytics.forecast.factor'), key: 'factor', width: 90, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 100, align: 'right' }
]

async function load() {
  loading.value = true
  try {
    rows.value = await listForecastEvents({ storeId: storeId.value || undefined, from: upcoming.value ? dayjs().format('YYYY-MM-DD') : undefined })
  } finally {
    loading.value = false
  }
}

function show(store) {
  storeId.value = store
  open.value = true
  load()
}

const emptyForm = () => ({ name: '', range: [], storeId: undefined, factor: 1.5, remark: '' })
const toPayload = (f) => ({ name: f.name, startDate: f.range?.[0], endDate: f.range?.[1], storeId: f.storeId || null, factor: f.factor, remark: f.remark })
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (r) => ({ ...r, range: [r.startDate, r.endDate], factor: Number(r.factor) }),
  create: (p) => addForecastEvent(p),
  update: (id, p) => updateForecastEvent(id, p),
  toPayload,
  onSuccess: () => load()
})
const { formRef, form } = drawer
const required = (key) => [{ required: true, message: t(key) }]
const rules = {
  name: required('ecomAnalytics.forecast.eventNameRequired'),
  range: [{ required: true, type: 'array', min: 2, message: t('ecomAnalytics.forecast.eventRangeRequired') }],
  factor: required('ecomAnalytics.forecast.factorRequired')
}

const remove = useAction(deleteForecastEvent, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

defineExpose({ open: show })
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 200px;
}
.full {
  width: 100%;
}
.tip {
  margin: 8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
