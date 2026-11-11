<template>
  <div class="cost-page">
    <a-card :bordered="false" class="page-card">
      <div class="head">
        <a-space wrap>
          <span>{{ $t('fin.cost.period') }}</span>
          <PeriodPicker v-model:value="period" @update:value="selectPeriod" />
          <a-button v-if="!isOpening" v-perm="'fin:cost:run'" type="primary" :loading="running" @click="run">
            {{ current ? $t('fin.cost.rerun') : $t('fin.cost.run') }}
          </a-button>
          <span v-if="current && !isOpening" v-perm="'fin:cost:run'">
            <a-popconfirm :title="$t('fin.cost.confirmUndo')" @confirm="undo">
              <a-button>{{ $t('fin.cost.undo') }}</a-button>
            </a-popconfirm>
          </span>
        </a-space>
        <span class="sub-text">{{ $t('fin.cost.headTip') }}</span>
      </div>
      <a-alert v-if="isOpening" type="info" show-icon class="opening-tip" :message="$t('fin.cost.openingTip')" />
      <a-row :gutter="16" class="stats">
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.status')" :value="isOpening ? $t('fin.cost.inventoryOpening') : current ? $t('fin.cost.costed') : $t('fin.cost.notCosted')" /></a-col>
        <a-col :xs="12" :md="5"><a-statistic :title="$t('fin.cost.salesCost')" :precision="2" prefix="¥" :value="Number(current?.salesCost || 0)" /></a-col>
      </a-row>
      <div v-if="current && !isOpening" class="sub-text">
        {{ $t('fin.cost.runInfo', { name: current.createByName, time: current.createTime }) }}
        <a v-if="current.voucherId" @click="openVoucher(current.voucherId)">{{ current.voucherNo }}</a>
        <span v-else>{{ $t('common.status.none') }}</span>
        <DictTag v-if="current.voucherStatus" type="fin_voucher_status" :value="current.voucherStatus" class="ml" />
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab">
        <a-tab-pane key="materials" :tab="$t('fin.cost.materialsTab')">
          <a-input-search v-model:value="keyword" :placeholder="$t('fin.cost.materialKeyword')" allow-clear class="filter" @search="materials.search" />
          <a-table
            row-key="id"
            size="small"
            bordered
            :columns="materialColumns"
            :data-source="materials.list.value"
            :loading="materials.loading.value"
            :pagination="materials.pagination"
            :scroll="{ x: 1700 }"
            :locale="{ emptyText: current ? $t('common.msg.noData') : $t('fin.cost.periodNotCosted') }"
            @change="materials.onTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ record.spec }}</div>
              </template>
              <template v-else-if="column.key === 'unitCost'">
                <div>{{ fmt(record.unitCost, 4) }}</div>
                <div class="sub-text"><DictTag type="wms_cost_method" :value="record.costMethod" /></div>
              </template>
              <template v-else-if="column.qty">
                <div class="num">{{ formatQty(record[column.qty]) === '0' ? '' : formatQty(record[column.qty]) }}</div>
                <div class="num sub-text">{{ moneyOrBlank(record[column.amount]) }}</div>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>
    <VoucherDetail v-model:open="voucherOpen" :voucher-id="voucherId" @changed="loadRuns" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import PeriodPicker from '@/components/fin/PeriodPicker.vue'
import VoucherDetail from '@/components/fin/VoucherDetail.vue'
import { materialCosts, pageCostRun, runCost, undoCost } from '@/api/fin'
import { useTable } from '@/composables/useTable'
import { formatQty } from '@/components/wms/wms'
import { currentPeriod, moneyOrBlank } from '@/components/fin/fin'
import { useI18n } from 'vue-i18n'

import { CostRunKind } from '@/constants/enums'
const { t } = useI18n()

const period = ref(currentPeriod())
const runs = ref([])
const current = computed(() => runs.value.find((r) => r.period === period.value))
const isOpening = computed(() => current.value?.kind === CostRunKind.OPENING)
const tab = ref('materials')
const keyword = ref('')

const fmt = (n, d) => Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: d })

const materialColumns = [
  { title: t('fin.cost.material'), key: 'material', width: 220, fixed: 'left' },
  { title: t('fin.cost.opening'), key: 'open', qty: 'openQty', amount: 'openAmount', width: 120, align: 'right' },
  { title: t('fin.cost.purchaseIn'), key: 'purchase', qty: 'purchaseQty', amount: 'purchaseAmount', width: 120, align: 'right' },
  { title: t('fin.cost.otherIn'), key: 'otherIn', qty: 'otherInQty', amount: 'otherInAmount', width: 120, align: 'right' },
  { title: t('fin.cost.outUnitCost'), key: 'unitCost', width: 150, align: 'right' },
  { title: t('fin.cost.salesOut'), key: 'sales', qty: 'salesQty', amount: 'salesAmount', width: 120, align: 'right' },
  { title: t('fin.cost.otherOut'), key: 'otherOut', qty: 'otherOutQty', amount: 'otherOutAmount', width: 120, align: 'right' },
  { title: t('fin.cost.closing'), key: 'close', qty: 'closeQty', amount: 'closeAmount', width: 130, align: 'right' }
]
const materials = useTable((p) => materialCosts(period.value, { ...p, keyword: keyword.value || undefined }), reactive({}))

async function loadRuns() {
  runs.value = (await pageCostRun({ current: 1, size: 120 })).records
}

function selectPeriod() {
  materials.search()
}

onMounted(async () => {
  await loadRuns()
  selectPeriod()
})

const running = ref(false)
function run() {
  Modal.confirm({
    title: current.value ? t('fin.cost.confirmRerun', { period: period.value }) : t('fin.cost.confirmRun', { period: period.value }),
    content: t('fin.cost.confirmRunContent'),
    async onOk() {
      running.value = true
      try {
        const res = await runCost(period.value)
        if (res.zeroCostMaterials.length) {
          Modal.warning({
            title: t('fin.cost.runDone', { n: res.materialCount }),
            content: t('fin.cost.zeroCostWarn', { list: res.zeroCostMaterials.join(t('fin.cost.sep')) })
          })
        } else {
          message.success(t('fin.cost.runDone', { n: res.materialCount }))
        }
        await loadRuns()
        selectPeriod()
      } finally {
        running.value = false
      }
    }
  })
}

async function undo() {
  await undoCost(period.value)
  message.success(t('fin.cost.undone'))
  await loadRuns()
  selectPeriod()
}

const voucherOpen = ref(false)
const voucherId = ref()
function openVoucher(id) {
  voucherId.value = id
  voucherOpen.value = true
}
</script>

<style scoped>
.opening-tip {
  margin: 12px 0;
}
.cost-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.stats {
  margin-bottom: 12px;
}
.filter {
  width: 240px;
  margin-bottom: 12px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ml {
  margin-left: 6px;
}
.block {
  margin-bottom: 12px;
}
.variance-head {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
