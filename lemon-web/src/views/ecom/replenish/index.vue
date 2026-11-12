<template>
  <div class="table-page ecom-replenish">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item v-if="isPlatform" :label="$t('ecom.common.store')" name="storeId">
          <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecom.common.allStores')" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input
            v-model:value="query.keyword"
            :placeholder="isPlatform ? $t('ecomPurchase.replenish.keywordPh') : $t('ecomPurchase.replenish.materialKeywordPh')"
            allow-clear
          />
        </a-form-item>
        <a-form-item :label="$t('ecom.replenish.level')" name="level">
          <a-select v-model:value="query.level" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_replenish_level)" />
        </a-form-item>
        <a-form-item :label="$t('ecomPurchase.replenish.weights')" name="weights" :tooltip="$t('ecomPurchase.replenish.weightsExtra')">
          <WeightSelect v-model:value="query.weights" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <div class="stats">
        <a-radio-group v-model:value="tab" button-style="solid" @change="onTab">
          <a-radio-button value="platform">{{ $t('ecomPurchase.replenish.tabPlatform') }}</a-radio-button>
          <a-radio-button value="purchase">{{ $t('ecomPurchase.replenish.tabPurchase') }}</a-radio-button>
        </a-radio-group>
        <!-- 紧急 / 预警 / 充足数量：点击按紧急程度筛选 -->
        <div
          v-for="s in STATS"
          :key="s.level"
          class="stat"
          :class="[s.cls, { active: query.level === s.level }]"
          role="button"
          tabindex="0"
          @click="toggleLevel(s.level)"
          @keydown.enter="toggleLevel(s.level)"
        >
          <component :is="s.icon" class="stat-icon" />
          <div class="stat-body">
            <div class="stat-label">{{ levelLabel(s.level) }}</div>
            <div class="stat-value">{{ num(counts[s.level]) }}</div>
          </div>
        </div>
        <div class="stat stat-total">
          <ShoppingCartOutlined class="stat-icon" />
          <div class="stat-body">
            <div class="stat-label">{{ isPlatform ? $t('ecomUiOps.replenish.totalShip') : $t('ecomUiOps.replenish.totalPurchase') }}</div>
            <div class="stat-value">
              {{ num(suggestTotal.qty) }}
              <span class="stat-sub">{{ $t('ecomUiOps.replenish.totalSku', { n: suggestTotal.count }) }}</span>
            </div>
          </div>
        </div>
        <a-popover placement="bottomRight" overlay-class-name="replenish-tip">
          <template #content>
            <div class="tip-body">{{ isPlatform ? $t('ecomPurchase.replenish.tipPlatform') : $t('ecomPurchase.replenish.tipPurchase') }}</div>
          </template>
          <a class="stats-tip"><QuestionCircleOutlined /> {{ $t('ecomUiOps.replenish.howCalc') }}</a>
        </a-popover>
      </div>

      <PlatformTable
        v-if="isPlatform"
        ref="platformTable"
        :rows="rows"
        :loading="loading"
        :selectable="canShipment"
        @refresh="load"
        @setting="openListingSetting"
        @transit="(r) => transitModal.open({ materialId: r.materialId, storeId: r.storeId, name: `${r.storeName} · ${r.sellerSku}` })"
      >
        <template #toolbar="{ selected }">
          <a-button v-if="SHIPMENT_PLAN_ENABLED" v-perm="'ecom:shipmentPlan:add'" type="primary" @click="toShipment(selected)">
            <template #icon><SendOutlined /></template>{{ $t('ecomPurchase.replenish.toShipment') }}
          </a-button>
        </template>
      </PlatformTable>
      <PurchaseTable
        v-else
        ref="purchaseTable"
        :rows="rows"
        :loading="loading"
        :selectable="canPlan"
        @refresh="load"
        @setting="openMaterialSetting"
        @transit="(r) => transitModal.open({ materialId: r.materialId, name: `${r.materialCode} ${r.materialName}` })"
      >
        <template #toolbar="{ selected }">
          <a-button v-perm="'ecom:purchasePlan:add'" type="primary" @click="toPlan(selected)">
            <template #icon><ShoppingCartOutlined /></template>{{ $t('ecomPurchase.replenish.toPlan') }}
            <span v-if="selected.length">&nbsp;({{ selected.length }})</span>
          </a-button>
        </template>
      </PurchaseTable>
    </a-card>

    <SettingDrawer ref="settingDrawer" @success="load" />
    <TransitModal ref="transitModal" />
    <GeneratePlanModal ref="planModal" @created="onPlanCreated" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { AlertOutlined, CheckCircleOutlined, QuestionCircleOutlined, SendOutlined, ShoppingCartOutlined, WarningOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { purchaseSuggestions, replenishRows, shipmentPlanFromReplenish } from '@/api/ecom/replenish'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'
import { num } from '@/utils/number'
import { ReplenishLevel, ReplenishScope } from '@/constants/enums'
import PlatformTable from './PlatformTable.vue'
import PurchaseTable from './PurchaseTable.vue'
import WeightSelect from './WeightSelect.vue'
import SettingDrawer from './SettingDrawer.vue'
import TransitModal from './TransitModal.vue'
import GeneratePlanModal from './GeneratePlanModal.vue'
import { WEIGHT_PRESETS, validWeights, weightParams } from './replenish'

/**
 * 补货建议：平台仓补货（店铺 × 在线商品）与本地仓采购（物料，汇总全部店铺）两段建议，日均可按 7 / 14 / 30 / 60 天加权；
 * 行上可打开补货设置（在线商品级 / 物料级）、查看在途构成；本地仓采购勾选生成采购计划。
 * 平台仓补货勾选「生成发货计划」调用发货计划模块的 POST /ecom/shipment-plan/from-replenish（权限 ecom:shipmentPlan:add）
 */
const SHIPMENT_PLAN_ENABLED = true

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const dict = useDict('ecom_replenish_level')
const canPlan = computed(() => userStore.hasPerm('ecom:purchasePlan:add'))
const canShipment = computed(() => SHIPMENT_PLAN_ENABLED && userStore.hasPerm('ecom:shipmentPlan:add'))

/** 顶部统计：紧急、预警、充足 */
const STATS = [
  { level: ReplenishLevel.URGENT, icon: AlertOutlined, cls: 'stat-urgent' },
  { level: ReplenishLevel.WARN, icon: WarningOutlined, cls: 'stat-warn' },
  { level: ReplenishLevel.OK, icon: CheckCircleOutlined, cls: 'stat-ok' }
]
const levelLabel = (level) => findDict(dict.ecom_replenish_level, level)?.label || level

const showSearch = ref(true)
const tab = ref('platform')
const isPlatform = computed(() => tab.value === 'platform')
const emptyQuery = () => ({ storeId: undefined, keyword: undefined, level: undefined, weights: { ...WEIGHT_PRESETS[0].weights } })
const query = reactive(emptyQuery())
// 从看板「紧急补货」跳转过来时带上紧急程度
const route = useRoute()
if (route.query.level) query.level = String(route.query.level)

const all = ref([])
const loading = ref(false)

/** 取全部行，紧急程度在前端筛选，统计数量始终是全部行的 */
async function load() {
  if (!validWeights(query.weights)) {
    message.warning(t('ecomPurchase.replenish.weightInvalid'))
    return
  }
  loading.value = true
  try {
    const params = { keyword: query.keyword || undefined, ...weightParams(query.weights) }
    all.value = (isPlatform.value ? await replenishRows({ ...params, storeId: query.storeId || undefined }) : await purchaseSuggestions(params)) || []
  } finally {
    loading.value = false
  }
}
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  load()
}
function onTab() {
  all.value = []
  load()
}

const counts = computed(() => {
  const c = { [ReplenishLevel.URGENT]: 0, [ReplenishLevel.WARN]: 0, [ReplenishLevel.OK]: 0 }
  all.value.forEach((r) => (c[r.level] = (c[r.level] || 0) + 1))
  return c
})
/** 默认排序：紧急 → 预警 → 充足，同级按可售天数从少到多（没有销量的排在最后）；表头排序可改 */
const LEVEL_ORDER = { [ReplenishLevel.URGENT]: 0, [ReplenishLevel.WARN]: 1, [ReplenishLevel.OK]: 2 }
const coverOf = (r) => {
  const v = isPlatform.value ? r.coverDays : r.localCoverDays
  return v == null ? Infinity : Number(v)
}
const rows = computed(() =>
  (query.level ? all.value.filter((r) => r.level === query.level) : all.value)
    .slice()
    .sort((a, b) => (LEVEL_ORDER[a.level] ?? 9) - (LEVEL_ORDER[b.level] ?? 9) || coverOf(a) - coverOf(b))
)
/** 建议量合计（当前筛选，不含不补货的） */
const suggestTotal = computed(() => {
  const list = rows.value.filter((r) => !r.disabled && Number(r.suggestQty) > 0)
  return { count: list.length, qty: list.reduce((sum, r) => sum + Number(r.suggestQty), 0) }
})
const toggleLevel = (level) => (query.level = query.level === level ? undefined : level)

// ---------------------------------------------------------------- 补货设置、在途、生成计划

const settingDrawer = ref()
const transitModal = ref()
const planModal = ref()
const purchaseTable = ref()
const platformTable = ref()

function openListingSetting(r) {
  settingDrawer.value.open({
    scope: ReplenishScope.LISTING,
    listingId: r.listingId,
    name: `${r.storeName} · ${r.sellerSku}`,
    storeLeadDays: r.storeLeadDays,
    storeSafetyDays: r.storeSafetyDays
  })
}
function openMaterialSetting(r) {
  settingDrawer.value.open({ scope: ReplenishScope.MATERIAL, materialId: r.materialId, name: `${r.materialCode} ${r.materialName}` })
}

function toPlan(selected) {
  if (!selected.length) {
    message.warning(t('ecomPurchase.replenish.selectFirst'))
    return
  }
  planModal.value.open(selected, query.weights)
}
function onPlanCreated(id) {
  purchaseTable.value?.clearSelection()
  router.push({ path: '/ecom/purchase-plan', query: { id } })
}

/** 生成发货计划：只能是同一店铺的商品，数量取平台仓建议补货 */
async function toShipment(selected) {
  const list = selected.filter((r) => Number(r.suggestQty) > 0)
  if (!list.length) {
    message.warning(t('ecomPurchase.replenish.selectFirst'))
    return
  }
  if (new Set(list.map((r) => r.storeId)).size > 1) {
    message.warning(t('ecomPurchase.replenish.shipmentStoreOne'))
    return
  }
  await shipmentPlanFromReplenish({
    storeId: list[0].storeId,
    toWarehouseId: list[0].platformWarehouseId,
    items: list.map((r) => ({ listingId: r.listingId, qty: Number(r.suggestQty) }))
  })
  message.success(t('ecomPurchase.replenish.shipmentCreated'))
  platformTable.value?.clearSelection()
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 130px;
  padding: 6px 14px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-left-width: 4px;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: box-shadow 0.2s;
}
.stat:hover,
.stat.active {
  box-shadow: 0 0 0 2px var(--ant-color-primary-border, #91caff);
}
.stat-icon {
  font-size: 20px;
}
.stat-label {
  color: var(--app-text-3);
  font-size: 12px;
}
.stat-value {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}
.stat-urgent {
  border-left-color: #ff4d4f;
}
.stat-urgent .stat-icon {
  color: #ff4d4f;
}
.stat-warn {
  border-left-color: #faad14;
}
.stat-warn .stat-icon {
  color: #faad14;
}
.stat-ok {
  border-left-color: #52c41a;
}
.stat-ok .stat-icon {
  color: #52c41a;
}
.stat-total {
  border-left-color: var(--ant-color-primary, #1677ff);
  cursor: default;
}
.stat-total:hover {
  box-shadow: none;
}
.stat-total .stat-icon {
  color: var(--ant-color-primary, #1677ff);
}
.stat-sub {
  margin-left: 4px;
  color: var(--app-text-3);
  font-size: 12px;
  font-weight: normal;
}
.stats-tip {
  margin-left: auto;
  font-size: 13px;
}
</style>

<style>
.replenish-tip .tip-body {
  max-width: 460px;
  line-height: 1.7;
}
/* 紧急行淡红底色 */
.ecom-replenish .ant-table-tbody > tr.row-urgent > td {
  background: #fff8f7;
}
.ecom-replenish .ant-table-tbody > tr.row-urgent:hover > td {
  background: #fff1ef;
}
html[data-theme='dark'] .ecom-replenish .ant-table-tbody > tr.row-urgent > td {
  background: #221819;
}
html[data-theme='dark'] .ecom-replenish .ant-table-tbody > tr.row-urgent:hover > td {
  background: #2c1c1e;
}
</style>
