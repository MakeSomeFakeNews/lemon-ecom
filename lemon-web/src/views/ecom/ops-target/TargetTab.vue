<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <a-date-picker v-model:value="month" picker="month" value-format="YYYY-MM" :allow-clear="false" @change="load" />
        <a-radio-group v-model:value="targetType" option-type="button" button-style="solid" :options="toOptions(dict.ecom_ops_target_type)" @change="load" />
        <span v-if="dirty" class="warn">{{ $t('ecomOper.target.unsaved') }}</span>
      </a-space>
      <a-space wrap>
        <a-button v-perm="'ecom:opsTarget:edit'" @click="copy"><template #icon><CopyOutlined /></template>{{ $t('ecomOper.target.copy') }}</a-button>
        <a-button v-perm="'ecom:opsTarget:edit'" type="primary" :loading="saving" @click="save"><template #icon><SaveOutlined /></template>{{ $t('ecomOper.target.save') }}</a-button>
      </a-space>
    </div>
    <a-alert type="info" show-icon :message="$t('ecomOper.target.settingTip')" class="tip" />
    <a-table row-key="refId" size="middle" :columns="columns" :data-source="rows" :loading="loading" :pagination="false" :scroll="{ x: 1200, y: '100%' }" class="table">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div>{{ record.refName || record.refId }}</div>
          <div v-if="isStore" class="sub-text">
            {{ record.code }} · <DictTag type="ecom_platform" :value="record.platform" /> {{ record.marketplace }}
            <span v-if="record.ownerName"> · {{ $t('ecomOper.common.owner') }} {{ record.ownerName }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'last'">
          <div>{{ $t('ecomOper.common.sales') }} {{ money(record.lastSales) }}</div>
          <div class="sub-text">{{ $t('ecomOper.common.profit') }} {{ money(record.lastProfit) }} · {{ $t('ecomOper.common.orders') }} {{ record.lastOrders }}</div>
        </template>
        <template v-else-if="column.key === 'salesTarget'">
          <a-input-number v-model:value="record.salesTarget" :min="0" :precision="2" :disabled="!editable" class="full" @change="touch" />
        </template>
        <template v-else-if="column.key === 'profitTarget'">
          <a-input-number v-model:value="record.profitTarget" :precision="2" :disabled="!editable" class="full" @change="touch" />
        </template>
        <template v-else-if="column.key === 'orderTarget'">
          <a-input-number v-model:value="record.orderTarget" :min="0" :precision="0" :disabled="!editable" class="full" @change="touch" />
        </template>
        <template v-else-if="column.key === 'remark'">
          <a-input v-model:value="record.remark" :maxlength="255" :disabled="!editable" @change="touch" />
        </template>
      </template>
      <template #summary>
        <a-table-summary fixed>
          <a-table-summary-row>
            <a-table-summary-cell :index="0" :col-span="2"><b>{{ $t('ecomOper.target.total') }}</b></a-table-summary-cell>
            <a-table-summary-cell :index="2" align="right"><b>{{ money(sum('salesTarget')) }}</b></a-table-summary-cell>
            <a-table-summary-cell :index="3" align="right"><b>{{ money(sum('profitTarget')) }}</b></a-table-summary-cell>
            <a-table-summary-cell :index="4" align="right"><b>{{ sum('orderTarget') }}</b></a-table-summary-cell>
            <a-table-summary-cell :index="5" />
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { CopyOutlined, SaveOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { copyTargets, saveTargets, targetEditRows } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money, nz } from '@/utils/number'
import { EcomOpsTargetType } from '@/constants/enums'

/** 目标设置：选月份与对象类型，表格中直接录入销售额、利润、订单数目标（参考上月实际），保存只提交本页全部行 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_ops_target_type')
const editable = useUserStore().hasPerm('ecom:opsTarget:edit')

const month = ref(dayjs().format('YYYY-MM'))
const targetType = ref(EcomOpsTargetType.STORE)
const isStore = computed(() => targetType.value === EcomOpsTargetType.STORE)
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const dirty = ref(false)

const columns = computed(() => [
  { title: t('ecomOper.target.name'), key: 'name', width: 300, fixed: 'left' },
  { title: t('ecomOper.target.lastMonth'), key: 'last', width: 230 },
  { title: t('ecomOper.target.salesTarget'), key: 'salesTarget', width: 160 },
  { title: t('ecomOper.target.profitTarget'), key: 'profitTarget', width: 160 },
  { title: t('ecomOper.target.orderTarget'), key: 'orderTarget', width: 130 },
  { title: t('common.field.remark'), key: 'remark', width: 220 }
])

const sum = (f) => rows.value.reduce((s, r) => s + nz(r[f]), 0)
const touch = () => (dirty.value = true)

async function load() {
  loading.value = true
  try {
    const list = await targetEditRows(month.value, targetType.value)
    rows.value = (list || []).map((r) => ({ ...r, salesTarget: nz(r.salesTarget), profitTarget: nz(r.profitTarget), orderTarget: nz(r.orderTarget) }))
    dirty.value = false
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveTargets({
      month: month.value,
      targetType: targetType.value,
      rows: rows.value.map((r) => ({ refId: r.refId, salesTarget: r.salesTarget ?? 0, profitTarget: r.profitTarget ?? 0, orderTarget: r.orderTarget ?? 0, remark: r.remark || null }))
    })
    message.success(t('ecomOper.target.saved'))
    await load()
  } finally {
    saving.value = false
  }
}

function copy() {
  Modal.confirm({
    title: t('ecomOper.target.copyConfirm', { month: month.value }),
    onOk: async () => {
      const n = await copyTargets({ month: month.value, targetType: targetType.value })
      message.success(t('ecomOper.target.copied', { n }))
      await load()
    }
  })
}

onMounted(load)
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
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.tip {
  margin-bottom: 8px;
}
.table {
  flex: 1;
  min-height: 0;
}
.full {
  width: 100%;
}
.warn {
  color: #fa8c16;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
