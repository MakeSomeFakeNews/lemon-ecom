<template>
  <a-modal v-model:open="visible" :title="$t('ecomOwh.tr.receiveTitle', { no: record?.transferNo })" :confirm-loading="saving" width="min(720px, 100vw)" @ok="submit">
    <p class="tip">{{ $t('ecomOwh.tr.receiveTip') }}</p>
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOwh.tr.receiveDate')">
        <a-date-picker v-model:value="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </a-form-item>
    </a-form>
    <a-table row-key="materialId" size="small" :columns="columns" :data-source="lines" :pagination="false">
      <template #bodyCell="{ column, record: line }">
        <template v-if="column.key === 'material'">
          <div>{{ line.materialCode }}</div>
          <div class="sub-text">{{ line.materialName }}</div>
        </template>
        <template v-else-if="column.key === 'qty'">{{ qty(line.qty) }}</template>
        <template v-else-if="column.key === 'actual'">
          <a-input-number v-model:value="line.actual" :min="0" :precision="0" class="full" />
        </template>
      </template>
    </a-table>
    <a-form-item v-if="hasDiff" :label="$t('ecomOwh.diff.reason')" class="reason">
      <a-input v-model:value="reason" :maxlength="255" :placeholder="$t('ecomOwh.tr.reasonPh')" />
    </a-form-item>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { getOwhTransfer, receiveOwhTransfer } from '@/api/ecom/owh'
import { qty } from '@/utils/number'

/** 调入本地仓的在途调拨确认收货：逐物料填写实收（默认等于调拨数量），不一致的记为收货差异（短少差异出库待处理）。open(record) */
const emit = defineEmits(['done'])
const { t } = useI18n()

const visible = ref(false)
const saving = ref(false)
const record = ref(null)
const lines = ref([])
const date = ref()
const reason = ref('')
const columns = [
  { title: t('ecomOwh.stock.material'), key: 'material', width: 260 },
  { title: t('ecomOwh.tr.qty'), key: 'qty', width: 100, align: 'right' },
  { title: t('ecomOwh.diff.received'), key: 'actual', width: 140 }
]
const hasDiff = computed(() => lines.value.some((l) => Number(l.actual) !== Number(l.qty)))

async function open(r) {
  record.value = r
  date.value = dayjs().format('YYYY-MM-DD')
  reason.value = ''
  const d = await getOwhTransfer(r.id)
  lines.value = (d.items || []).map((i) => ({ materialId: i.materialId, materialCode: i.materialCode, materialName: i.materialName, qty: Number(i.qty), actual: Number(i.qty) }))
  visible.value = true
}

async function submit() {
  saving.value = true
  try {
    await receiveOwhTransfer(record.value.id, {
      receiveDate: date.value,
      items: lines.value.map((l) => ({ materialId: l.materialId, qty: l.actual ?? 0 })),
      reason: reason.value || null
    })
    message.success(hasDiff.value ? t('ecomOwh.tr.receivedWithDiff') : t('ecomOwh.tr.received'))
    visible.value = false
    emit('done')
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
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.full {
  width: 100%;
}
.reason {
  margin-top: 12px;
}
</style>
