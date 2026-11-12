<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template v-if="report" #extra>
      <a-space wrap>
        <DictTag type="ecom_ops_report_source" :value="report.source" />
        <a-button v-perm="'ecom:opsReport:generate'" :loading="pushing" @click="push">
          <template #icon><SendOutlined /></template>{{ $t('ecomAsst.report.push') }}
        </a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="report && content">
        <div class="meta sub-text">
          <span>{{ $t('ecomAsst.detail.scope') }}：{{ report.scopeName }}（{{ $t('ecomAsst.report.storeCount', { n: report.storeCount }) }}）</span>
          <span>{{ $t('ecomAsst.detail.generatedAt') }}：{{ report.updateTime || report.createTime }}</span>
          <span v-if="report.createByName">{{ $t('ecomAsst.detail.generatedBy') }}：{{ report.createByName }}</span>
          <span v-if="report.pushCount">{{ $t('ecomAsst.report.pushed', { n: report.pushCount }) }}</span>
          <span>{{ $t('ecomAsst.detail.currencyTip') }}</span>
        </div>

        <!-- 核心指标 -->
        <div class="kpis">
          <div v-for="k in kpis" :key="k.key" class="kpi">
            <div class="kpi-label">{{ k.label }}</div>
            <div class="kpi-value" :class="{ danger: k.negative }">{{ k.value }}</div>
            <div class="kpi-sub">{{ k.sub }}</div>
            <div class="kpi-cmp">
              <span>{{ $t('ecomAsst.detail.vsPrev') }} <b :class="trendClass(k.prev, k.reverse)">{{ signedPct(k.prev) }}</b></span>
              <span>{{ $t('ecomAsst.detail.vsAvg') }} <b :class="trendClass(k.avg, k.reverse)">{{ signedPct(k.avg) }}</b></span>
            </div>
          </div>
        </div>

        <a-row :gutter="[16, 16]" class="block">
          <a-col :xs="24" :lg="10">
            <div class="panel">
              <div class="panel-title"><BulbOutlined /> {{ $t('ecomAsst.detail.highlights') }}</div>
              <ul v-if="content.highlights?.length" class="highlights">
                <li v-for="(h, i) in content.highlights" :key="i" :class="`hl-${String(h.level).toLowerCase()}`">
                  <component :is="hlIcon(h.level)" class="hl-icon" /><span>{{ h.text }}</span>
                </li>
              </ul>
              <a-empty v-else :image="simpleImage" :description="$t('ecomAsst.detail.noHighlights')" />
              <template v-if="content.todos?.length">
                <div class="panel-sub">{{ $t('ecomAsst.detail.todos') }}</div>
                <div class="todos">
                  <a v-for="td in content.todos" :key="td.kind" class="todo" @click="go(td.link)">
                    {{ $t(`ecomAsst.todo.${td.kind}`) }} <b>{{ td.count }}</b><RightOutlined />
                  </a>
                </div>
              </template>
            </div>
          </a-col>
          <a-col :xs="24" :lg="14">
            <AdvicePanel :report="report" @regenerated="reload" />
          </a-col>
        </a-row>

        <a-card :bordered="false" size="small" class="tables-card">
          <a-tabs v-model:active-key="tab">
            <a-tab-pane key="stores" :tab="$t('ecomAsst.detail.tabStores', { n: content.storeRows.length })">
              <DetailTable kind="stores" :rows="content.storeRows" />
            </a-tab-pane>
            <a-tab-pane key="abnormal" :tab="$t('ecomAsst.detail.tabAbnormal', { n: content.abnormalTotal || content.abnormalSkus.length })">
              <DetailTable kind="abnormal" :rows="content.abnormalSkus" />
            </a-tab-pane>
            <a-tab-pane key="reviews" :tab="$t('ecomAsst.detail.tabReviews', { n: content.badReviews.length })">
              <DetailTable kind="reviews" :rows="content.badReviews" />
            </a-tab-pane>
            <a-tab-pane key="stock" :tab="$t('ecomAsst.detail.tabStock', { n: content.stockRiskTotal || content.stockRisks.length })">
              <DetailTable kind="stock" :rows="content.stockRisks" />
            </a-tab-pane>
            <a-tab-pane key="ads" :tab="$t('ecomAsst.detail.tabAds', { n: content.adAlertTotal || content.adAlerts.length })">
              <DetailTable kind="ads" :rows="content.adAlerts" />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  BulbOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  RightOutlined,
  SendOutlined
} from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { getOpsReport, pushOpsReport } from '@/api/ecom/assistant'
import { useDict } from '@/composables/useDict'
import { money, num } from '@/utils/number'
import { change, pct, signedPct, trendClass } from './report'
import AdvicePanel from './AdvicePanel.vue'
import DetailTable from './DetailTable.vue'

/** 日报详情子页：核心指标卡（对比前一天、前 7 天日均）、要点与待办、AI 建议、店铺 / 异常 SKU / 差评 / 断货 / 广告表格。ref 调用 open(id)、reload() */
const emit = defineEmits(['changed'])
const { t } = useI18n()
const router = useRouter()
useDict('ecom_ops_report_source', 'ecom_ops_report_ai_status', 'ecom_platform', 'ecom_ops_abnormal_type', 'ecom_ops_ad_alert_type', 'ecom_replenish_level')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const open = ref(false)
const loading = ref(false)
const pushing = ref(false)
const report = ref()
const content = ref()
const tab = ref('stores')
let currentId

const title = computed(() => (report.value ? t('ecomAsst.detail.title', { date: report.value.reportDate }) : ''))

const kpis = computed(() => {
  const c = content.value
  if (!c) return []
  const k = c.kpi
  const p = c.prev
  const a = c.avg7
  return [
    { key: 'sales', label: t('ecomAsst.report.sales'), value: money(k.sales), sub: t('ecomAsst.detail.qty', { n: num(k.qty) }), prev: change(k.sales, p.sales), avg: change(k.sales, a.sales) },
    { key: 'profit', label: t('ecomAsst.report.profit'), value: money(k.profit), negative: Number(k.profit) < 0, sub: t('ecomAsst.detail.margin', { v: pct(k.marginPercent) }), prev: change(k.profit, p.profit), avg: change(k.profit, a.profit) },
    { key: 'orders', label: t('ecomAsst.report.orders'), value: num(k.orders), sub: t('ecomAsst.detail.aov', { v: money(k.orders ? Number(k.sales) / k.orders : 0) }), prev: change(k.orders, p.orders), avg: change(k.orders, a.orders) },
    { key: 'ad', label: t('ecomAsst.detail.adSpend'), value: money(k.adSpend), sub: `ACOS ${pct(k.acosPercent)} · TACOS ${pct(k.tacosPercent)}`, prev: change(k.adSpend, p.adSpend), avg: change(k.adSpend, a.adSpend), reverse: true },
    { key: 'refund', label: t('ecomAsst.detail.refund'), value: money(k.refund), sub: t('ecomAsst.detail.returns', { n: k.returns }), prev: change(k.refund, p.refund), avg: change(k.refund, a.refund), reverse: true }
  ]
})

const hlIcon = (level) => ({ GOOD: CheckCircleOutlined, BAD: CloseCircleOutlined, WARN: ExclamationCircleOutlined })[level] || InfoCircleOutlined

async function load(id) {
  currentId = id
  loading.value = true
  try {
    const d = await getOpsReport(id)
    report.value = d.report
    content.value = d.content
  } finally {
    loading.value = false
  }
}

function show(id) {
  report.value = null
  content.value = null
  tab.value = 'stores'
  open.value = true
  load(id)
}

async function reload() {
  if (currentId) await load(currentId)
  emit('changed')
}

async function push() {
  pushing.value = true
  try {
    const n = await pushOpsReport(currentId)
    message.success(t('ecomAsst.report.pushDone', { n }))
    await reload()
  } finally {
    pushing.value = false
  }
}

function go(link) {
  open.value = false
  router.push(link)
}

defineExpose({ open: show, reload })
</script>

<style scoped>
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 20px;
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.kpis {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi {
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 13px;
}
.kpi-value {
  margin: 4px 0 2px;
  font-size: 22px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.kpi-sub {
  color: var(--app-text-2);
  font-size: 12px;
}
.kpi-cmp {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 12px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--app-border);
  color: var(--app-text-3);
  font-size: 12px;
}
.kpi-cmp b {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.trend-up {
  color: #389e0d;
}
.trend-down {
  color: #cf1322;
}
.danger {
  color: #cf1322;
}
.block {
  margin-bottom: 16px;
}
.panel {
  height: 100%;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  background: var(--app-card);
}
.panel-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}
.panel-sub {
  margin: 12px 0 8px;
  color: var(--app-text-2);
  font-weight: 600;
}
.highlights {
  margin: 0;
  padding: 0;
  list-style: none;
}
.highlights li {
  display: flex;
  gap: 8px;
  padding: 5px 0;
  line-height: 1.6;
}
.hl-icon {
  flex: none;
  margin-top: 5px;
}
.hl-good .hl-icon {
  color: #52c41a;
}
.hl-bad .hl-icon {
  color: #f5222d;
}
.hl-warn .hl-icon {
  color: #fa8c16;
}
.hl-info .hl-icon {
  color: #1677ff;
}
.todos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.todo {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  color: var(--app-text);
  font-size: 13px;
}
.todo b {
  color: #d4380d;
}
.todo:hover {
  border-color: #1677ff;
  color: #1677ff;
}
.tables-card {
  border: 1px solid var(--app-border);
}
@media (max-width: 1199px) {
  .kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 767px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
