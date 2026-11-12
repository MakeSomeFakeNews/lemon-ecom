<template>
  <div class="board-page">
    <a-card :bordered="false" class="page-card toolbar">
      <div class="toolbar-row">
        <a-button v-perm="'ecom:devProject:add'" type="primary" @click="formRef.openCreate()"><template #icon><PlusOutlined /></template>{{ $t('ecomDev.project.add') }}</a-button>
        <a-button @click="router.push('/ecom/dev-project')"><template #icon><UnorderedListOutlined /></template>{{ $t('ecomDev.board.list') }}</a-button>
        <a-tooltip :title="$t('ecomDev.board.hint')" placement="bottomLeft">
          <QuestionCircleOutlined class="hint-icon" />
        </a-tooltip>
        <span class="spacer" />
        <a-input-search v-model:value="query.keyword" :placeholder="$t('ecomDev.project.keywordPh')" allow-clear class="search" @search="load" />
        <a-select v-model:value="query.platform" :placeholder="$t('ecomDev.project.platform')" allow-clear class="sel" :options="toOptions(dict.ecom_platform)" @change="load" />
        <a-select v-model:value="query.source" :placeholder="$t('ecomDev.project.source')" allow-clear class="sel" :options="toOptions(dict.ecom_dev_source)" @change="load" />
        <div class="sel-user"><UserSelect v-model:value="query.ownerId" :placeholder="$t('ecomDev.project.owner')" @update:value="load" /></div>
        <a-checkbox v-model:checked="showClosed">{{ $t('ecomDev.board.showClosed') }}</a-checkbox>
        <a-tooltip :title="$t('common.action.refresh')">
          <a-button @click="load"><template #icon><ReloadOutlined /></template></a-button>
        </a-tooltip>
      </div>
    </a-card>

    <a-spin :spinning="loading" wrapper-class-name="board-spin">
      <div class="board">
        <div
          v-for="col in visibleColumns"
          :key="col.status"
          class="column"
          :class="{ over: overStatus === col.status, allowed: dragging && dropAllowed(col.status), denied: dragging && !dropAllowed(col.status) }"
          @dragover.prevent="onDragOver(col.status)"
          @dragleave="overStatus = null"
          @drop.prevent="onDrop(col.status)"
        >
          <div class="col-head">
            <DictTag type="ecom_dev_status" :value="col.status" />
            <span class="count">{{ col.count }}</span>
            <a-tooltip v-if="avgMargin(col) != null" :title="$t('ecomDev.board.avgMargin')">
              <span :class="['col-metric', 'margin', marginClass(avgMargin(col))]">{{ $t('ecomDev.board.margin', { v: avgMargin(col) }) }}</span>
            </a-tooltip>
          </div>
          <div class="cards">
            <div
              v-for="p in col.items"
              :key="p.id"
              class="card"
              :draggable="canDrag"
              @dragstart="onDragStart(p, $event)"
              @dragend="onDragEnd"
              @click="detailRef.open(p.id)"
            >
              <div class="card-top">
                <img v-if="imagesOf(p)[0]" :src="imagesOf(p)[0]" class="thumb" alt="" />
                <div v-else class="thumb empty"><PictureOutlined /></div>
                <div class="card-info">
                  <div class="name" :title="p.name">{{ p.name }}</div>
                  <div class="sub-text">{{ p.docNo }}</div>
                  <div v-if="p.categoryName" class="sub-text ellipsis">{{ p.categoryName }}</div>
                </div>
              </div>
              <div class="card-meta">
                <DictTag type="ecom_platform" :value="p.platform" />
                <span v-if="p.marketplace" class="mkt">{{ p.marketplace }}</span>
                <span v-if="p.targetPrice != null" class="price">{{ p.currency }} {{ money(p.targetPrice) }}</span>
              </div>
              <div class="card-kpis">
                <div class="kpi">
                  <div class="kpi-label">{{ $t('ecomDev.board.kpiMargin') }}</div>
                  <div v-if="p.estMargin != null" :class="['kpi-value', 'margin', marginClass(p.estMargin)]">{{ Number(p.estMargin) }}%</div>
                  <div v-else class="kpi-value sub-text">-</div>
                </div>
                <div class="kpi">
                  <div class="kpi-label">{{ $t('ecomDev.board.kpiSales') }}</div>
                  <div class="kpi-value">{{ p.estMonthlySales ?? '-' }}</div>
                </div>
                <div class="kpi">
                  <div class="kpi-label">{{ $t('ecomDev.board.kpiSamples') }}</div>
                  <div class="kpi-value">{{ p.sampleCount ? `${p.passedSampleCount}/${p.sampleCount}` : '-' }}</div>
                </div>
              </div>
              <div class="card-foot">
                <span class="owner sub-text"><UserOutlined /> {{ p.ownerName || '-' }}</span>
                <span class="sub-text">{{ (p.createTime || '').slice(0, 10) }}</span>
              </div>
              <div v-if="p.flowInstanceId && p.status === DevProjectStatus.PENDING_APPROVAL" class="flow-tag">{{ $t('ecomDev.project.inFlow') }}</div>
            </div>
            <div v-if="col.count > col.items.length" class="more sub-text">{{ $t('ecomDev.board.more', { n: col.count - col.items.length }) }}</div>
            <a-empty v-if="!col.items.length" :image="simpleImage" :description="$t('ecomDev.board.emptyColumn')" class="col-empty" />
          </div>
        </div>
      </div>
    </a-spin>

    <ProjectForm ref="formRef" @success="load" />
    <ProjectDetail ref="detailRef" @changed="load" @edit="(r) => formRef.openEdit(r)" />
    <RemarkModal ref="remarkModal" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PictureOutlined, PlusOutlined, QuestionCircleOutlined, ReloadOutlined, UnorderedListOutlined, UserOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import UserSelect from '@/components/user/UserSelect.vue'
import ProjectForm from '../dev-project/ProjectForm.vue'
import ProjectDetail from '../dev-project/ProjectDetail.vue'
import RemarkModal from '../dev-project/RemarkModal.vue'
import { devBoard } from '@/api/ecom/dev'
import { DevProjectStatus } from '@/constants/enums'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { canManualApprove, canMove, canSubmit, imagesOf, marginClass } from '../dev-project/dev'
import { useDevActions } from '../dev-project/useDevActions'

/**
 * 开发看板：每个状态一列，卡片显示图片、平台站点、目标售价、试算利润率、样品、负责人。
 * 拖拽卡片改状态：在可手工流转的状态之间移动（放弃需填写原因），拖到「待审批」为提交审批，
 * 从「待审批」拖到「已通过 / 已驳回」为手工审批（没有发起审批流程时）；点击卡片打开详情
 */
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const dict = useDict('ecom_platform', 'ecom_dev_source')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const query = reactive({ keyword: undefined, platform: undefined, source: undefined, ownerId: undefined })
const columns = ref([])
const loading = ref(false)
const showClosed = ref(true)
const formRef = ref()
const detailRef = ref()
const remarkModal = ref()

const CLOSED = [DevProjectStatus.ABANDONED]
const visibleColumns = computed(() => columns.value.filter((c) => showClosed.value || !CLOSED.includes(c.status)))
const canDrag = computed(() => userStore.hasPerm('ecom:devProject:edit'))
/** 列中已试算项目的平均利润率（当前加载的卡片） */
function avgMargin(col) {
  const ms = col.items.filter((p) => p.estMargin != null).map((p) => Number(p.estMargin))
  return ms.length ? Math.round((ms.reduce((a, b) => a + b, 0) / ms.length) * 10) / 10 : null
}

async function load() {
  loading.value = true
  try {
    const params = Object.fromEntries(Object.entries(query).filter(([, v]) => v !== undefined && v !== null && v !== ''))
    columns.value = (await devBoard(params)).columns
  } finally {
    loading.value = false
  }
}
onMounted(load)

const { move, submit, approve, reject } = useDevActions(remarkModal, () => load())

// ---------------------------------------------------------------- 拖拽

const dragging = ref(null)
const overStatus = ref(null)

function dropAllowed(to) {
  const p = dragging.value
  if (!p || p.status === to) return false
  if (to === DevProjectStatus.PENDING_APPROVAL) return canSubmit(p)
  if (to === DevProjectStatus.APPROVED || to === DevProjectStatus.REJECTED) {
    if (canManualApprove(p)) return userStore.hasPerm('ecom:devProject:approve')
    return false
  }
  return canMove(p.status, to)
}

function onDragStart(p, e) {
  dragging.value = p
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(p.id))
}
function onDragEnd() {
  dragging.value = null
  overStatus.value = null
}
function onDragOver(status) {
  overStatus.value = status
}

function onDrop(to) {
  const p = dragging.value
  onDragEnd()
  if (!p || p.status === to) return
  if (to === DevProjectStatus.PENDING_APPROVAL) {
    if (canSubmit(p)) submit(p)
    else message.warning(t('ecomDev.board.cannotSubmit'))
    return
  }
  if (to === DevProjectStatus.APPROVED || to === DevProjectStatus.REJECTED) {
    if (canManualApprove(p) && userStore.hasPerm('ecom:devProject:approve')) {
      to === DevProjectStatus.APPROVED ? approve(p) : reject(p)
    } else {
      message.warning(p.flowInstanceId ? t('ecomDev.board.inFlow') : t('ecomDev.board.cannotApprove'))
    }
    return
  }
  if (!canMove(p.status, to)) {
    message.warning(t('ecomDev.board.cannotMove'))
    return
  }
  move(p, to)
}
</script>

<style scoped>
.board-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.toolbar :deep(.ant-card-body) {
  padding: 12px 16px;
}
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.spacer {
  flex: 1;
}
.hint-icon {
  color: var(--app-text-3);
  cursor: help;
}
.search {
  width: 220px;
}
.sel {
  width: 130px;
}
.sel-user {
  width: 160px;
}
.board-spin {
  flex: 1;
  min-height: 0;
}
.board-spin :deep(.ant-spin-container) {
  height: 100%;
}
.board {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  height: 100%;
  padding-bottom: 8px;
}
.column {
  flex: 1 0 248px;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  background: rgba(0, 0, 0, 0.035);
  border-radius: 8px;
  border: 2px solid transparent;
  min-height: 300px;
  transition: border-color 0.15s;
}
.column.allowed {
  border-color: #91caff;
}
.column.over.allowed {
  border-color: #1677ff;
  background: #e6f4ff;
}
.column.denied {
  opacity: 0.6;
}
.col-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px 6px;
  font-weight: 600;
}
.count {
  color: var(--app-text-3);
}
.col-metric {
  margin-left: auto;
  font-size: 12px;
  font-weight: 400;
}
.cards {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.card {
  position: relative;
  background: var(--app-card);
  border: 1px solid var(--app-border);
  border-radius: 6px;
  padding: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.card:hover {
  border-color: #91caff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.card[draggable='true'] {
  cursor: grab;
}
.card-top {
  display: flex;
  gap: 10px;
}
.thumb {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  object-fit: cover;
  flex: none;
  border: 1px solid var(--app-border);
}
.thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-hover);
  color: var(--app-text-3);
  font-size: 18px;
}
.card-info {
  min-width: 0;
  flex: 1;
}
.name {
  font-weight: 500;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.mkt {
  font-size: 12px;
}
.price {
  margin-left: auto;
  font-weight: 600;
}
.card-kpis {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 6px 0;
  border-top: 1px dashed var(--app-border);
  border-bottom: 1px dashed var(--app-border);
}
.kpi {
  text-align: center;
  min-width: 0;
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 11px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kpi-value {
  font-weight: 500;
  font-size: 13px;
}
.card-foot {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.flow-tag {
  position: absolute;
  right: 6px;
  top: 6px;
  font-size: 11px;
  color: #1677ff;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.more {
  text-align: center;
}
.col-empty {
  margin: 24px 0;
}
.margin.good {
  color: #389e0d;
}
.margin.low {
  color: #d48806;
}
.margin.loss {
  color: #cf1322;
}
</style>
<style>
html[data-theme='dark'] .board .column {
  background: rgba(255, 255, 255, 0.04);
}
html[data-theme='dark'] .board .column.over.allowed {
  background: #111a2c;
}
</style>
