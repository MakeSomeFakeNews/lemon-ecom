<template>
  <div v-if="list.length">
    <div class="section-title">{{ $t('ecomOwh.diff.title') }}</div>
    <a-table row-key="id" size="small" :columns="columns" :data-source="list" :pagination="false" :scroll="{ x: 800 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }}</div>
          <div class="sub-text">{{ record.materialName }}</div>
        </template>
        <template v-else-if="['expectedQty', 'receivedQty'].includes(column.key)">{{ qty(record[column.key]) }}</template>
        <template v-else-if="column.key === 'diffQty'">
          <span :class="Number(record.diffQty) > 0 ? 'loss' : 'gain'">{{ Number(record.diffQty) > 0 ? '-' : '+' }}{{ qty(Math.abs(Number(record.diffQty))) }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag type="ecom_owh_diff_status" :value="record.status" />
        </template>
      </template>
    </a-table>
    <div class="sub-text tip">
      {{ $t('ecomOwh.diff.listTip') }} <a @click="router.push('/ecom/owh-diff')">{{ $t('ecomOwh.diff.goHandle') }}</a>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import { owhDiffsBySource } from '@/api/ecom/owh'
import { qty } from '@/utils/number'

/** 单据详情中的收货差异（实收与单据数量不一致的物料），没有差异时不显示 */
const props = defineProps({ sourceType: String, sourceId: Number, refreshKey: { type: [Number, String], default: 0 } })
const { t } = useI18n()
const router = useRouter()
const list = ref([])
const columns = [
  { title: t('ecomOwh.stock.material'), key: 'material', width: 200 },
  { title: t('ecomOwh.diff.expected'), key: 'expectedQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.received'), key: 'receivedQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.diff'), key: 'diffQty', width: 90, align: 'right' },
  { title: t('ecomOwh.diff.status'), key: 'status', width: 100 },
  { title: t('ecomOwh.diff.reason'), dataIndex: 'reason', width: 160, ellipsis: true },
  { title: t('ecomOwh.diff.diffDoc'), dataIndex: 'diffDocNo', width: 150 }
]
async function load() {
  list.value = props.sourceId ? (await owhDiffsBySource(props.sourceType, props.sourceId)) || [] : []
}
watch(() => [props.sourceId, props.refreshKey], load, { immediate: true })
</script>

<style scoped>
.section-title {
  margin: 16px 0 12px;
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tip {
  margin-top: 6px;
}
.loss {
  color: #ff4d4f;
}
.gain {
  color: #52c41a;
}
</style>
