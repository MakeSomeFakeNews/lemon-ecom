<template>
  <a-modal v-model:open="visible" :title="$t('ecomOwh.inbound.create')" width="min(900px, 100vw)" :confirm-loading="saving" :ok-button-props="{ disabled: !selected }" @ok="submit">
    <p class="tip">{{ $t('ecomOwh.inbound.createTip') }}</p>
    <a-input-search v-model:value="keyword" :placeholder="$t('ecomOwh.inbound.sourcePh')" allow-clear class="search" @search="load" />
    <a-table
      :row-key="(r) => `${r.sourceType}-${r.sourceId}`"
      size="small"
      :columns="columns"
      :data-source="sources"
      :loading="loading"
      :pagination="{ pageSize: 8 }"
      :row-selection="{ type: 'radio', selectedRowKeys: selected ? [`${selected.sourceType}-${selected.sourceId}`] : [], onChange: (_k, rows) => (selected = rows[0]) }"
      :scroll="{ x: 760 }"
      :locale="{ emptyText: $t('ecomOwh.inbound.noSources') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'sourceType'">
          <DictTag type="ecom_owh_inbound_source" :value="record.sourceType" />
        </template>
        <template v-else-if="column.key === 'totalQty'">{{ qty(record.totalQty) }}</template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { addOwhInbound, owhInboundSources } from '@/api/ecom/owh'
import { qty } from '@/utils/number'

/** 从调入仓是海外仓的头程货件 / 在途调拨单生成海外仓入库单（装箱取货件的装箱信息）。open()，生成后触发 created(id) */
const emit = defineEmits(['created'])
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const sources = ref([])
const selected = ref(null)

const columns = [
  { title: t('ecomOwh.inbound.sourceType'), key: 'sourceType', width: 100 },
  { title: t('ecomOwh.inbound.sourceNo'), dataIndex: 'sourceNo', width: 160 },
  { title: t('ecomOwh.common.warehouse'), dataIndex: 'warehouseName', width: 160 },
  { title: t('ecomOwh.inbound.totalQty'), key: 'totalQty', width: 90, align: 'right' },
  { title: t('ecomOwh.inbound.boxCount'), dataIndex: 'boxCount', width: 70, align: 'right' },
  { title: t('ecomOwh.inbound.trackingNo'), dataIndex: 'trackingNo', width: 140 },
  { title: t('ecomOwh.inbound.bizDate'), dataIndex: 'bizDate', width: 110 }
]

async function load() {
  loading.value = true
  try {
    sources.value = (await owhInboundSources(keyword.value || undefined)) || []
  } finally {
    loading.value = false
  }
}

function open() {
  keyword.value = ''
  selected.value = null
  visible.value = true
  load()
}

async function submit() {
  saving.value = true
  try {
    const id = await addOwhInbound({ sourceType: selected.value.sourceType, sourceId: selected.value.sourceId })
    message.success(t('ecomOwh.inbound.created'))
    visible.value = false
    emit('created', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.tip {
  color: var(--app-text-3);
}
.search {
  margin-bottom: 8px;
}
</style>
