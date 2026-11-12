<template>
  <div class="sync-log-table">
    <div class="log-filters">
      <StoreSelect v-if="!storeId" :value="query.storeId" allow-clear class="filter-store" @update:value="onStore" />
      <a-select
        v-model:value="query.syncType"
        :placeholder="$t('ecom.hub.log.type')"
        allow-clear
        class="filter-select"
        :options="toOptions(dict.ecom_sync_type)"
        @change="search"
      />
      <a-select v-model:value="query.success" :placeholder="$t('ecom.hub.log.result')" allow-clear class="filter-select" :options="resultOptions" @change="search" />
      <a-button @click="load">
        <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
      </a-button>
    </div>
    <a-table
      row-key="id"
      size="middle"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="scroll"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'syncType'">
          <DictTag type="ecom_sync_type" :value="record.syncType" />
        </template>
        <template v-else-if="column.key === 'triggerBy'">{{ triggerLabel(record.triggerBy) }}</template>
        <template v-else-if="column.key === 'success'">
          <a-tag v-if="record.success === 1" color="success" :bordered="false">{{ $t('ecom.hub.log.success') }}</a-tag>
          <a-tag v-else color="error" :bordered="false">{{ $t('ecom.hub.log.failed') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'counts'">
          <span class="sub-text">{{ $t('ecom.hub.log.countsText', { fetched: record.fetched, created: record.created, updated: record.updated }) }}</span>
        </template>
        <template v-else-if="column.key === 'duration'">{{ duration(record) }}</template>
        <template v-else-if="column.key === 'message'">
          <a-tooltip v-if="record.message" :title="record.message">
            <span :class="{ 'error-text': record.success !== 1 }">{{ record.message }}</span>
          </a-tooltip>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { ReloadOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/**
 * 渠道同步日志表格：店铺页的日志抽屉（固定 storeId）和渠道接入页（可筛选店铺）共用。
 * api 接收 { current, size, storeId, syncType, success }，返回分页
 */
const props = defineProps({
  api: { type: Function, required: true },
  storeId: { type: [Number, String], default: undefined },
  /** 表格高度：抽屉中不限，页面中占满 */
  fill: { type: Boolean, default: false }
})

const { t, te } = useI18n()
const dict = useDict('ecom_sync_type')
const query = reactive({ storeId: props.storeId, syncType: undefined, success: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(props.api, query)
onMounted(load)
watch(
  () => props.storeId,
  (v) => {
    query.storeId = v
    search()
  }
)

function onStore(v) {
  query.storeId = v
  search()
}

/** 外部（总览中点击店铺）设置筛选条件 */
function filter(conditions) {
  Object.assign(query, { storeId: undefined, syncType: undefined, success: undefined }, conditions)
  search()
}

const resultOptions = computed(() => [
  { value: 1, label: t('ecom.hub.log.success') },
  { value: 0, label: t('ecom.hub.log.failed') }
])

const columns = computed(() => [
  { title: t('ecom.hub.log.time'), dataIndex: 'startTime', width: 170 },
  ...(props.storeId ? [] : [{ title: t('ecom.common.store'), dataIndex: 'storeName', width: 160, ellipsis: true }]),
  { title: t('ecom.hub.log.type'), key: 'syncType', width: 110 },
  { title: t('ecom.hub.log.trigger'), key: 'triggerBy', width: 100 },
  { title: t('ecom.hub.log.result'), key: 'success', width: 80 },
  { title: t('ecom.hub.log.counts'), key: 'counts', width: 230 },
  { title: t('ecom.hub.log.duration'), key: 'duration', width: 80, align: 'right' },
  { title: t('ecom.hub.log.message'), key: 'message', ellipsis: true, width: 260 }
])
const scroll = computed(() => (props.fill ? { x: 1100, y: '100%' } : { x: 1000 }))

const triggerLabel = (v) => (v && te(`ecom.hub.trigger.${v}`) ? t(`ecom.hub.trigger.${v}`) : v)

function duration(r) {
  if (!r.startTime || !r.endTime) return ''
  const s = dayjs(r.endTime).diff(dayjs(r.startTime), 'second')
  return t('ecom.hub.log.seconds', { n: s })
}

defineExpose({ load, filter })
</script>

<style scoped>
.log-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.filter-store {
  width: 220px;
}
.filter-select {
  width: 140px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-text {
  color: var(--ant-color-error, #ff4d4f);
}
</style>
