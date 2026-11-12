<template>
  <a-modal v-model:open="visible" :title="$t('ecomOwh.ret.create')" width="min(860px, 100vw)" :confirm-loading="saving" :ok-button-props="{ disabled: !selected }" @ok="submit">
    <p class="tip">{{ $t('ecomOwh.ret.createTip') }}</p>
    <a-input-search v-model:value="keyword" :placeholder="$t('ecomOwh.ret.sourcePh')" allow-clear class="search" @search="load" />
    <a-table
      row-key="id"
      size="small"
      :columns="columns"
      :data-source="sources"
      :loading="loading"
      :pagination="{ pageSize: 6 }"
      :row-selection="{ type: 'radio', selectedRowKeys: selected ? [selected.id] : [], onChange: (_k, rows) => (selected = rows[0]) }"
      :scroll="{ x: 700 }"
      :locale="{ emptyText: $t('ecomOwh.ret.noSources') }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'reason'">
          <DictTag v-if="record.reason" type="ecom_return_reason" :value="record.reason" />
        </template>
      </template>
    </a-table>
    <a-row :gutter="16" class="extra">
      <a-col :xs="24" :sm="8">
        <a-form-item :label="$t('ecomOwh.ret.carrier')"><a-input v-model:value="form.carrier" :maxlength="64" /></a-form-item>
      </a-col>
      <a-col :xs="24" :sm="8">
        <a-form-item :label="$t('ecomOwh.ret.trackingNo')"><a-input v-model:value="form.trackingNo" :maxlength="64" /></a-form-item>
      </a-col>
      <a-col :xs="24" :sm="8">
        <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
      </a-col>
    </a-row>
  </a-modal>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { addOwhReturn, owhReturnSources } from '@/api/ecom/owh'

/** 从退回仓是海外仓的待处理退货退款单生成海外仓退件单（可填写买家寄回的承运商与运单号）。open()，生成后触发 created(id) */
const emit = defineEmits(['created'])
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const sources = ref([])
const selected = ref(null)
const form = reactive({ carrier: '', trackingNo: '', remark: '' })

const columns = [
  { title: t('ecomOwh.ret.ecomReturnNo'), dataIndex: 'docNo', width: 150 },
  { title: t('ecomOwh.ret.orderNo'), dataIndex: 'platformOrderNo', width: 160 },
  { title: t('ecomOwh.common.store'), dataIndex: 'storeName', width: 140 },
  { title: t('ecomOwh.common.warehouse'), dataIndex: 'warehouseName', width: 150 },
  { title: t('ecomOwh.ret.reason'), key: 'reason', width: 100 },
  { title: t('ecomOwh.ret.createTime'), dataIndex: 'createTime', width: 160 }
]

async function load() {
  loading.value = true
  try {
    sources.value = (await owhReturnSources(keyword.value || undefined)) || []
  } finally {
    loading.value = false
  }
}

function open() {
  keyword.value = ''
  selected.value = null
  Object.assign(form, { carrier: '', trackingNo: '', remark: '' })
  visible.value = true
  load()
}

async function submit() {
  saving.value = true
  try {
    const id = await addOwhReturn({ ecomReturnId: selected.value.id, carrier: form.carrier || null, trackingNo: form.trackingNo || null, remark: form.remark || null })
    message.success(t('ecomOwh.ret.created'))
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
.extra {
  margin-top: 12px;
}
</style>
