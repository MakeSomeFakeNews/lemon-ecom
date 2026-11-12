<template>
  <div class="table-page">
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="load">
        <a-select v-model:value="query.status" :placeholder="$t('fin.statement.allStatus')" allow-clear :options="toOptions(dict.fin_statement_status)" class="filter" @change="search" />
        <PartnerSelect v-model:value="query.supplierId" type="supplier" allow-clear :placeholder="$t('fin.statement.supplier')" class="filter-wide" @update:value="search" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('fin.statement.keywordPh')" allow-clear class="filter-wide" @search="search" />
        <template #extra>
          <a-button v-perm="'fin:statement:add'" type="primary" @click="openGenerate">
            <template #icon><PlusOutlined /></template>{{ $t('fin.statement.generate') }}
          </a-button>
        </template>
      </TableToolbar>

      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statementNo'">
            <a @click="openDetail(record.id)">{{ record.statementNo }}</a>
          </template>
          <template v-else-if="column.key === 'period'">{{ record.beginDate }} ~ {{ record.endDate }}</template>
          <template v-else-if="column.key === 'amount'">
            {{ symMoney(record.amount) }}
            <div class="sub-text">{{ $t('fin.statement.billCount', { n: record.billCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'invoice'">
            <template v-if="record.invoiceNo">
              {{ record.invoiceNo }}
              <div class="sub-text">{{ symMoney(record.invoiceAmount) }} · {{ record.invoiceDate }}</div>
            </template>
            <span v-else class="sub-text">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="fin_statement_status" :value="record.status" />
            <a-tooltip v-if="record.supplierRemark" :title="record.supplierRemark"><div class="sub-text ellipsis">{{ record.supplierRemark }}</div></a-tooltip>
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="[{ type: 'view', onClick: () => openDetail(record.id) }]" />
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="generateOpen" :title="$t('fin.statement.generate')" :confirm-loading="saving" @ok="submitGenerate">
      <a-alert type="info" show-icon class="block" :message="$t('fin.statement.generateTip')" />
      <a-form layout="vertical">
        <a-form-item :label="$t('fin.statement.supplier')" required>
          <PartnerSelect v-model:value="gen.supplierId" type="supplier" />
        </a-form-item>
        <a-form-item :label="$t('fin.statement.period')" required>
          <a-range-picker v-model:value="gen.range" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('common.field.remark')">
          <a-input v-model:value="gen.remark" :maxlength="255" />
        </a-form-item>
      </a-form>
    </a-modal>

    <SubPage :open="detailOpen" :title="detail ? $t('fin.statement.detailTitle', { no: detail.statementNo }) : ''" width="min(980px, 100vw)" @close="detailOpen = false">
      <template v-if="detail" #extra>
        <a-space>
          <a-button @click="exportDetail"><template #icon><DownloadOutlined /></template>{{ $t('ecomFixa.statement.export') }}</a-button>
          <template v-if="[StatementStatus.DRAFT, StatementStatus.DISPUTED].includes(detail.status)">
            <span v-perm="'fin:statement:remove'">
              <a-popconfirm :title="$t('fin.statement.confirmDelete')" @confirm="remove"><a-button danger>{{ $t('common.action.delete') }}</a-button></a-popconfirm>
            </span>
            <span v-perm="'fin:statement:edit'">
              <a-popconfirm :title="$t('fin.statement.confirmSend')" @confirm="send"><a-button type="primary">{{ $t('fin.statement.send') }}</a-button></a-popconfirm>
            </span>
          </template>
          <a-button v-if="detail.status === StatementStatus.SENT" v-perm="'fin:statement:edit'" type="primary" @click="openResult">
            <template #icon><AuditOutlined /></template>{{ $t('ecomFixa.statement.register') }}
          </a-button>
        </a-space>
      </template>
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 3 }" size="small" bordered class="block">
          <a-descriptions-item :label="$t('fin.statement.supplier')">{{ detail.supplierCode }} {{ detail.supplierName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.period')">{{ detail.beginDate }} ~ {{ detail.endDate }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="fin_statement_status" :value="detail.status" /></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.amount')"><b>{{ symMoney(detail.amount) }}</b></a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.confirmTime')">
            {{ detail.confirmTime || '-' }}
            <span v-if="detail.confirmByName" class="sub-text">{{ $t('ecomFixa.statement.registeredBy', { name: detail.confirmByName }) }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('fin.statement.invoice')">
            <template v-if="detail.invoiceNo">{{ detail.invoiceNo }} {{ symMoney(detail.invoiceAmount) }} {{ detail.invoiceDate }}</template>
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.supplierRemark" :label="$t('fin.statement.supplierRemark')" :span="3">{{ detail.supplierRemark }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="detail.status === StatementStatus.SENT" type="info" show-icon class="block" :message="$t('ecomFixa.statement.sentTip')" />
        <StatementLines :lines="detail.lines" />
        <div class="section-title">{{ $t('fin.statement.attachments') }}</div>
        <AttachmentPanel biz-type="fin_statement" :biz-id="detail.id" :editable="userStore.hasPerm('fin:statement:edit')" />
      </template>
    </SubPage>

    <a-modal v-model:open="resultOpen" :title="$t('ecomFixa.statement.register')" :confirm-loading="saving" @ok="submitResult">
      <a-form layout="vertical">
        <a-form-item :label="$t('ecomFixa.statement.result')" required>
          <a-radio-group v-model:value="result.status">
            <a-radio :value="StatementStatus.CONFIRMED">{{ $t('ecomFixa.statement.confirmed') }}</a-radio>
            <a-radio :value="StatementStatus.DISPUTED">{{ $t('ecomFixa.statement.disputed') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('ecomFixa.statement.replyDate')">
          <a-date-picker v-model:value="result.confirmDate" value-format="YYYY-MM-DD" class="full" />
        </a-form-item>
        <a-form-item :label="$t('fin.statement.supplierRemark')" :required="result.status === StatementStatus.DISPUTED">
          <a-textarea v-model:value="result.supplierRemark" :maxlength="500" :rows="3" :placeholder="$t('ecomFixa.statement.remarkPh')" />
        </a-form-item>
        <template v-if="result.status === StatementStatus.CONFIRMED">
          <a-form-item :label="$t('ecomFixa.statement.invoiceNo')">
            <a-input v-model:value="result.invoiceNo" :maxlength="64" :placeholder="$t('ecomFixa.statement.optional')" />
          </a-form-item>
          <a-space v-if="result.invoiceNo" class="full" :size="16">
            <a-form-item :label="$t('ecomFixa.statement.invoiceAmount')">
              <a-input-number v-model:value="result.invoiceAmount" :precision="2" class="full" />
            </a-form-item>
            <a-form-item :label="$t('ecomFixa.statement.invoiceDate')">
              <a-date-picker v-model:value="result.invoiceDate" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-space>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { AuditOutlined, DownloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import DictTag from '@/components/DictTag.vue'
import SubPage from '@/components/SubPage.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TableToolbar from '@/components/TableToolbar.vue'
import StatementLines from '@/components/fin/StatementLines.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { deleteStatement, generateStatement, getStatement, pageStatement, registerStatementResult, sendStatement } from '@/api/fin'
import { writeExcel } from '@/utils/excel'
import { formatMoney } from '@/components/fin/fin'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { useUserStore } from '@/store/user'

import { StatementStatus } from '@/constants/enums'
import { symMoney } from '@/utils/number'
/** 供应商对账：汇总期间内的应付单生成对账单，导出发送给供应商，收到回复后由内部人员登记确认结果（确认可登记发票 / 有异议及说明） */
const { t } = useI18n()
const dict = useDict('fin_statement_status')
const userStore = useUserStore()

const columns = [
  { title: t('fin.statement.statementNo'), key: 'statementNo', width: 180 },
  { title: t('fin.statement.supplier'), dataIndex: 'supplierName', width: 200, ellipsis: true },
  { title: t('fin.statement.period'), key: 'period', width: 210 },
  { title: t('fin.statement.amount'), key: 'amount', width: 160, align: 'right' },
  { title: t('fin.statement.invoice'), key: 'invoice', width: 200 },
  { title: t('common.field.status'), key: 'status', width: 130 },
  actionColumn(1, [t('common.action.view')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'fin-statement')
const query = reactive({ status: undefined, supplierId: undefined, keyword: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageStatement, query)
onMounted(load)

const saving = ref(false)
const generateOpen = ref(false)
const gen = ref({})
function openGenerate() {
  const last = dayjs().subtract(1, 'month')
  gen.value = { supplierId: undefined, range: [last.startOf('month').format('YYYY-MM-DD'), last.endOf('month').format('YYYY-MM-DD')], remark: '' }
  generateOpen.value = true
}
async function submitGenerate() {
  if (!gen.value.supplierId || !gen.value.range) return message.warning(t('fin.statement.selectRequired'))
  saving.value = true
  try {
    const id = await generateStatement({ supplierId: gen.value.supplierId, beginDate: gen.value.range[0], endDate: gen.value.range[1], remark: gen.value.remark || null })
    message.success(t('fin.statement.generated'))
    generateOpen.value = false
    load()
    openDetail(id)
  } finally {
    saving.value = false
  }
}

const detailOpen = ref(false)
const detail = ref()
async function openDetail(id) {
  detail.value = await getStatement(id)
  detailOpen.value = true
}
async function send() {
  await sendStatement(detail.value.id)
  message.success(t('fin.statement.sent'))
  detail.value = await getStatement(detail.value.id)
  load()
}
const resultOpen = ref(false)
const result = ref({})
function openResult() {
  result.value = { status: StatementStatus.CONFIRMED, confirmDate: dayjs().format('YYYY-MM-DD'), supplierRemark: '', invoiceNo: '', invoiceAmount: detail.value.amount, invoiceDate: undefined }
  resultOpen.value = true
}
async function submitResult() {
  const r = result.value
  if (r.status === StatementStatus.DISPUTED && !r.supplierRemark?.trim()) return message.warning(t('ecomFixa.statement.remarkRequired'))
  saving.value = true
  try {
    await registerStatementResult(detail.value.id, { ...r, invoiceNo: r.invoiceNo || null, supplierRemark: r.supplierRemark || null })
    message.success(t('ecomFixa.statement.registered'))
    resultOpen.value = false
    detail.value = await getStatement(detail.value.id)
    load()
  } finally {
    saving.value = false
  }
}

/** 导出对账单（发给供应商核对）：表头信息 + 应付单明细 */
function exportDetail() {
  const d = detail.value
  const headers = [
    { title: t('common.field.date'), width: 14 },
    { title: t('fin.statementLines.billNo'), width: 22 },
    { title: t('fin.statementLines.source'), width: 24 },
    { title: t('fin.statementLines.amount'), width: 16 },
    { title: t('fin.statementLines.tax'), width: 14 }
  ]
  const rows = [
    ...(d.lines || []).map((l) => [l.billDate, l.billNo, l.sourceNo || '', Number(l.amount), Number(l.taxAmount || 0)]),
    [],
    [t('fin.statement.statementNo'), d.statementNo],
    [t('fin.statement.supplier'), `${d.supplierCode || ''} ${d.supplierName || ''}`.trim()],
    [t('fin.statement.period'), `${d.beginDate} ~ ${d.endDate}`],
    [t('fin.statement.amount'), Number(d.amount)]
  ]
  writeExcel(t('fin.statement.detailTitle', { no: d.statementNo }), headers, rows)
}

async function remove() {
  await deleteStatement(detail.value.id)
  message.success(t('common.msg.deleted'))
  detailOpen.value = false
  load()
}
</script>

<style scoped>
.filter {
  width: 140px;
}
.filter-wide {
  width: 220px;
}
.full {
  width: 100%;
}
.block {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
