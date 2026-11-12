<template>
  <a-modal
    v-model:open="visible"
    :title="$t('ecomPurchase.generate.title')"
    width="min(860px, 100vw)"
    :confirm-loading="saving"
    :ok-text="$t('ecomPurchase.generate.title')"
    @ok="submit"
  >
    <a-alert type="info" show-icon class="block" :message="$t('ecomPurchase.generate.tip')" />
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomPurchase.generate.warehouse')">
            <WarehouseSelect v-model:value="warehouseId" :placeholder="$t('ecomPurchase.generate.warehousePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('common.field.remark')">
            <a-input v-model:value="remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
    <a-table row-key="materialId" size="small" :columns="columns" :data-source="lines" :pagination="false" :scroll="{ x: 700, y: 360 }">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'material'">
          <div>{{ record.materialCode }} {{ record.materialName }}</div>
          <div v-if="record.spec" class="sub-text">{{ record.spec }}</div>
        </template>
        <template v-else-if="column.key === 'suggestQty'">{{ qty(record.suggestQty) }}</template>
        <template v-else-if="column.key === 'qty'">
          <a-input-number v-model:value="record.qty" :min="0" :precision="4" class="full" :addon-after="record.unit || undefined" />
        </template>
        <template v-else-if="column.key === 'supplier'">
          <span v-if="record.supplierName">{{ record.supplierName }}</span>
          <span v-else class="sub-text">{{ $t('ecomPurchase.replenish.noSupplier') }}</span>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { purchasePlanFromReplenish } from '@/api/ecom/purchasePlan'
import { qty } from '@/utils/number'
import { weightParams } from './replenish'

/**
 * 由本地仓采购建议勾选的物料生成采购计划：可调整计划采购量、收货仓库（默认取需求店铺的发货仓）。
 * ref 调用 open(rows, weights)；成功后触发 created(planId)
 */
const emit = defineEmits(['created'])
const { t } = useI18n()

const columns = [
  { title: t('ecom.common.material'), key: 'material', width: 260 },
  { title: t('ecomPurchase.replenish.suggestPurchase'), key: 'suggestQty', width: 110, align: 'right' },
  { title: t('ecomPurchase.generate.qty'), key: 'qty', width: 170 },
  { title: t('ecomPurchase.replenish.supplier'), key: 'supplier', width: 160 }
]

const visible = ref(false)
const saving = ref(false)
const lines = ref([])
const warehouseId = ref()
const remark = ref('')
let weights = {}

function open(rows, w) {
  weights = w
  lines.value = rows.map((r) => ({ ...r, qty: Number(r.suggestQty) > 0 ? Number(r.suggestQty) : undefined }))
  warehouseId.value = undefined
  remark.value = ''
  visible.value = true
}

async function submit() {
  if (lines.value.some((l) => !(Number(l.qty) > 0))) {
    message.warning(t('ecomPurchase.generate.noQty'))
    return
  }
  saving.value = true
  try {
    const id = await purchasePlanFromReplenish({
      warehouseId: warehouseId.value ?? null,
      remark: remark.value || null,
      ...weightParams(weights),
      items: lines.value.map((l) => ({ materialId: l.materialId, qty: l.qty }))
    })
    visible.value = false
    emit('created', id)
  } finally {
    saving.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
