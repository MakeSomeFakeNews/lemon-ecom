<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('scm.orderForm.createTitle', { kind: conf.label })" :edit-title="$t('scm.orderForm.editTitle', { kind: conf.label })" width="min(1200px, 100vw)">
    <AiPrefillAlert v-if="ai" :ai="ai" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="conf.partnerLabel" name="partnerId" :class="ai?.cls('partnerId')">
            <PartnerSelect
              v-model:value="form.partnerId"
              :type="conf.partner"
              :label="form.partnerName"
              @select="onPartnerSelect"
            />
            <div v-if="ai?.source && !form.partnerId && form.partnerName" class="ai-warn-text">{{ $t('ai.extract.aiText', { text: form.partnerName }) }}</div>
          </a-form-item>
          <a-alert
            v-if="kind === 'purchase' && partnerLevel === SupplierLevel.D"
            type="warning"
            show-icon
            class="level-warning"
            :message="$t('scm.orderForm.levelD')"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.order.orderDate')" name="orderDate" :class="ai?.cls('orderDate')">
            <a-date-picker v-model:value="form.orderDate" value-format="YYYY-MM-DD" class="full" @change="refreshRate" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="4">
          <a-form-item :label="$t('scm.order.deliveryDate')" name="deliveryDate" :class="ai?.cls('deliveryDate')">
            <a-date-picker v-model:value="form.deliveryDate" value-format="YYYY-MM-DD" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="8">
          <a-form-item :label="conf.warehouseLabel" name="warehouseId" :class="ai?.cls('warehouseId')">
            <WarehouseSelect v-model:value="form.warehouseId" :placeholder="$t('scm.orderForm.warehousePlaceholder')" />
          </a-form-item>
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('scm.orderForm.currency')" name="currency" :class="ai?.cls('currency')">
            <a-select :value="form.currency" :options="currencyOptions(currencyList)" @change="onCurrencyChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('scm.orderForm.exchangeRate')" name="exchangeRate" :extra="$t('scm.orderForm.rateExtra', { currency: form.currency })">
            <a-input-number v-model:value="form.exchangeRate" :min="0" :precision="6" class="full" @change="onRateChange" />
          </a-form-item>
        </a-col>
        <a-col v-if="foreign" :xs="24" :lg="16">
          <a-alert
            type="info"
            show-icon
            class="fc-tip"
            :message="$t(kind === 'purchase' ? 'scm.orderForm.foreignTipPurchase' : 'scm.orderForm.foreignTipSales', { currency: form.currency })"
          />
        </a-col>
        <a-col :xs="12" :sm="6" :lg="4">
          <a-form-item :label="$t('scm.taxRate.invoiceKind')" name="invoiceKind" :tooltip="$t('scm.taxRate.invoiceKindExtra')">
            <InvoiceKindSelect v-model:value="form.invoiceKind" @change="onInvoiceKindChange" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('scm.orderForm.optional')" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="items-head">
        <span class="section-title">
          {{ $t('scm.orderForm.itemsTitle', { n: form.items.length, qty: formatQty(totalQty), amount: symbol + formatMoney(totalAmount) }) }}
          <span v-if="foreign && Number(form.exchangeRate)" class="sub-text">≈ {{ symMoney(totalAmount * Number(form.exchangeRate)) }}</span>
        </span>
        <a-space>
          <a-button @click="matrixOpen = true">
            <template #icon><TableOutlined /></template>{{ $t('scm.orderForm.byVariant') }}
          </a-button>
          <a-button type="primary" ghost @click="addRow">
            <template #icon><PlusOutlined /></template>{{ $t('scm.orderForm.addMaterial') }}
          </a-button>
        </a-space>
      </div>

      <a-table
        row-key="key"
        size="small"
        class="items-table"
        :columns="columns"
        :data-source="form.items"
        :pagination="false"
        :scroll="{ x: scrollX }"
        :locale="{ emptyText: $t('scm.orderForm.emptyItems') }"
      >
        <template #headerCell="{ column }">
          <TaxRateTitle v-if="column.key === 'taxRate'" :title="column.title" @apply="applyAllTaxRate" />
        </template>
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'material'">
            <a-form-item :name="['items', index, 'materialId']" :rules="requiredRule($t('scm.orderForm.selectMaterial'))" class="cell-item" :class="ai?.cls(`items[${index}].materialId`)">
              <MaterialSelect v-model:value="record.materialId" :label="record.materialLabel" @select="(m) => applyMaterial(record, m)" />
            </a-form-item>
            <div v-if="!record.materialId && record.aiText" class="ai-warn-text">{{ $t('ai.extract.aiText', { text: record.aiText }) }}</div>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-form-item :name="['items', index, 'unitQty']" :rules="requiredRule($t('scm.orderForm.inputQty'))" class="cell-item" :class="ai?.cls(`items[${index}].qty`)">
              <a-input-number
                v-model:value="record.unitQty"
                :min="0"
                :precision="record.serialEnabled === 1 && !record.unitName ? 0 : 4"
                :addon-after="record.units?.length ? undefined : record.unit || undefined"
                class="full"
                @change="kind === 'sales' && fetchSalesPrice(record)"
              >
                <template v-if="record.units?.length" #addonAfter>
                  <a-select
                    :value="record.unitName || ''"
                    :options="unitOptions(record)"
                    :dropdown-match-select-width="false"
                    class="unit-select"
                    @change="(v) => changeUnit(record, v)"
                  />
                </template>
              </a-input-number>
            </a-form-item>
            <div v-if="record.unitName && record.unitQty != null" class="sub-text">= {{ formatQty(baseQty(record)) }} {{ record.unit }}</div>
            <div v-if="record.minQty && record.unitQty && baseQty(record) < record.minQty" class="warn-text">
              {{ $t('scm.orderForm.belowMoq', { qty: formatQty(record.minQty), unit: record.unit || '' }) }}
            </div>
          </template>
          <template v-else-if="column.key === 'price'">
            <a-form-item :name="['items', index, 'unitPrice']" :rules="requiredRule($t('scm.orderForm.inputPrice'))" class="cell-item" :class="ai?.cls(`items[${index}].price`)">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="4"
                :prefix="symbol"
                class="full"
                @change="record.priceAuto = false"
              />
            </a-form-item>
            <div v-if="record.priceSource && record.priceAuto" class="sub-text">{{ $t(`pricing.source.${record.priceSource}`) }}</div>
            <div v-if="supplyPriceDiff(record) != null" class="sub-text">
              {{ $t('scm.orderForm.supplyPrice') }} <a @click="useSupplyPrice(record)">{{ symbol }}{{ formatQty(supplyPriceDiff(record)) }}</a>
            </div>
            <div v-if="record.unitName" class="sub-text">{{ $t('scm.orderForm.perUnit', { unit: record.unitName }) }}</div>
            <a v-if="record.materialId && canReference" class="ref-link" @click="openReference(record)">
              <HistoryOutlined /> {{ $t('pricing.reference.open') }}
            </a>
          </template>
          <template v-else-if="column.key === 'taxRate'">
            <div :class="ai?.cls(`items[${index}].taxRate`)">
              <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="2" addon-after="%" class="full" />
            </div>
          </template>
          <template v-else-if="column.key === 'amount'">
            <div class="amount">{{ symbol }}{{ formatMoney(lineAmount(record.unitQty, record.unitPrice)) }}</div>
            <div class="sub-text">{{ $t('scm.orderForm.taxValue', { amount: formatMoney(lineTax(lineAmount(record.unitQty, record.unitPrice), record.taxRate)) }) }}</div>
          </template>
          <template v-else-if="column.key === 'deliveryDate'">
            <a-date-picker v-model:value="record.deliveryDate" value-format="YYYY-MM-DD" :placeholder="$t('scm.orderForm.sameAsOrder')" class="full" />
          </template>
          <template v-else-if="column.key === 'remark'">
            <a-input v-model:value="record.remark" :maxlength="255" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </template>
        </template>
      </a-table>
      <WholeDiscountBar v-if="form.items.length" v-model:value="form.wholeDiscount" :total="totalAmount" :symbol="symbol" />
      <CustomFieldsForm :fields="cfFields" :values="form.cf || {}" :title="$t('scm.orderForm.extInfo')" :span="8" />
    </a-form>
    <VariantMatrixModal v-model:open="matrixOpen" @confirm="addVariantRows" />
    <PriceReference ref="referenceRef" pickable @pick="onReferencePick" />
  </FormDrawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, HistoryOutlined, PlusOutlined, TableOutlined } from '@ant-design/icons-vue'
import VariantMatrixModal from '@/components/wms/VariantMatrixModal.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import PartnerSelect from './PartnerSelect.vue'
import PriceReference from './PriceReference.vue'
import WholeDiscountBar from './WholeDiscountBar.vue'
import InvoiceKindSelect from './InvoiceKindSelect.vue'
import TaxRateTitle from './TaxRateTitle.vue'
import { applyTaxRate, useTaxRates } from './taxRate'
import { defaultPrices } from '@/api/scm/pricing'
import { useUserStore } from '@/store/user'
import { addOrder, getOrder, updateOrder } from '@/api/scm/order'
import { materialUnits } from '@/api/wms/material'
import { partnerOptions, supplierPrices } from '@/api/scm/partner'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { formatQty, rowKey, today } from '@/components/wms/wms'
import { ORDER_KINDS, formatMoney, lineAmount, lineTax, qty4 } from './scm'
import { BASE_CURRENCY, currencyOptions, currencySymbol, fetchRate, isForeign, useCurrencies } from '@/components/fin/currency'
import CustomFieldsForm from '@/components/common/CustomFieldsForm.vue'
import AiPrefillAlert from '@/components/ai/AiPrefillAlert.vue'
import { useCustomFields } from '@/composables/useCustomFields'
import { OrderKind, SupplierLevel } from '@/constants/enums'
import { symMoney } from '@/utils/number'
/**
 * 新增 / 编辑采购、销售订单（草稿）。采购订单选择供应商后，按供货价格自动带出单价、税率；
 * 销售订单选择客户和物料后按开单默认价（客户执行价 → 价格等级价 → 零售价 → 最近售价，/scm/pricing/default）
 * 带出单价、税率，手工改过的单价不再自动覆盖。单价下方可打开「价格参考」查看历史价格并点选使用。
 * 物料有辅助单位时可按辅助单位录入：表单中 unitQty / unitPrice 为所选单位的数量、单价，
 * 提交时折算为基本单位的 qty / price（qty = unitQty × 换算率，price = unitPrice ÷ 换算率），并带上 unitName
 * 外币订单：单价、金额为订单币种的原币，按人民币维护的供货价格自动带出时除以汇率
 * 整单优惠（含抹零）：保存时按行金额分摊到各行；编辑时单价按原价（origPrice）录入，保存时重新分摊
 * 通过 ref 调用 openCreate(defaults) / openEdit(record)，保存成功后触发 success(id)
 */
const props = defineProps({
  kind: { type: String, required: true },
  /** 智能录入的预填（useAiPrefill 的返回值）：表单顶部列出需要核对的提示，有提示的字段高亮 */
  ai: Object
})
const emit = defineEmits(['success'])

const { t } = useI18n()
const conf = computed(() => ORDER_KINDS[props.kind])

const columns = [
  { title: t('scm.order.material'), key: 'material', width: 280 },
  { title: t('common.field.quantity'), key: 'qty', width: 150 },
  { title: t('scm.order.taxPrice'), key: 'price', width: 150 },
  { title: t('scm.order.taxRate'), key: 'taxRate', width: 130 },
  { title: t('scm.order.totalAmount'), key: 'amount', width: 130, align: 'right' },
  { title: t('scm.order.deliveryDate'), key: 'deliveryDate', width: 140 },
  { title: t('common.field.remark'), key: 'remark', width: 150 },
  { title: '', key: 'action', width: 50, fixed: 'right' }
]
const scrollX = columns.reduce((sum, c) => sum + c.width, 0)

const requiredRule = (msg) => [{ required: true, message: msg }]
const rules = computed(() => ({
  partnerId: requiredRule(t('common.rule.requiredSelect', { name: conf.value.partnerLabel })),
  orderDate: requiredRule(t('scm.orderForm.selectOrderDate')),
  exchangeRate: foreign.value ? [{ required: true, type: 'number', min: 0.000001, message: t('scm.orderForm.inputRate') }] : []
}))

const emptyForm = () => ({
  id: undefined,
  partnerId: undefined,
  partnerName: undefined,
  orderDate: today(),
  deliveryDate: undefined,
  warehouseId: undefined,
  currency: BASE_CURRENCY,
  exchangeRate: 1,
  wholeDiscount: undefined,
  invoiceKind: undefined,
  remark: '',
  items: []
})

const toRow = (item) => {
  const unitRatio = item.unitName && Number(item.unitRatio) ? Number(item.unitRatio) : 1
  return {
    ...item,
    key: item.id ? `item-${item.id}` : rowKey(),
    materialLabel: item.materialCode ? `${item.materialCode} ${item.materialName}` : undefined,
    unitName: item.unitName || null,
    unitRatio,
    unitQty: item.qty != null ? qty4(Number(item.qty) / unitRatio) : undefined,
    unitPrice: item.price != null ? qty4(Number(item.price) * unitRatio) : undefined,
    taxRate: item.taxRate != null ? Number(item.taxRate) : undefined,
    units: []
  }
}

// ---------------------------------------------------------------- 辅助单位
/** 物料的辅助单位：materialId → Promise<[{ unit, ratio }]>，表单打开期间缓存 */
const unitCache = new Map()
function loadUnits(materialId) {
  if (!unitCache.has(materialId)) {
    unitCache.set(
      materialId,
      materialUnits(materialId).catch((e) => {
        unitCache.delete(materialId)
        throw e
      })
    )
  }
  return unitCache.get(materialId)
}

/** 加载行的辅助单位；当前单位不在列表中（已删除）时保留它，避免编辑旧订单时丢失 */
async function attachUnits(row) {
  const materialId = row.materialId
  if (!materialId) return
  const list = await loadUnits(materialId).catch(() => [])
  if (row.materialId !== materialId) return
  const units = list.map((u) => ({ unit: u.unit, ratio: Number(u.ratio) }))
  if (row.unitName && !units.some((u) => u.unit === row.unitName)) units.push({ unit: row.unitName, ratio: row.unitRatio })
  row.units = units
}

const ratioOf = (row) => (row.unitName ? Number(row.unitRatio) || 1 : 1)
/** 基本单位数量 */
const baseQty = (row) => (row.unitQty == null ? 0 : qty4(Number(row.unitQty) * ratioOf(row)))
/** 基本单位单价 */
const basePrice = (row) => (row.unitPrice == null ? null : qty4(Number(row.unitPrice) / ratioOf(row)))

const unitOptions = (row) => [
  { value: '', label: row.unit || t('scm.orderForm.baseUnit') },
  ...row.units.map((u) => ({ value: u.unit, label: t('scm.orderForm.unitOption', { unit: u.unit, ratio: `${formatQty(u.ratio)}${row.unit || ''}` }) }))
]

/** 换单位：数量保持输入的数字，单价按基本单位价折算到新单位 */
function changeUnit(row, unitName) {
  const price = basePrice(row)
  const unit = row.units.find((u) => u.unit === unitName)
  row.unitName = unit ? unit.unit : null
  row.unitRatio = unit ? unit.ratio : 1
  if (price != null) row.unitPrice = qty4(price * row.unitRatio)
  if (props.kind === 'sales') fetchSalesPrice(row)
}

// 供应商的供货价格：materialId → price
const prices = ref(new Map())
async function loadPrices(supplierId) {
  prices.value = new Map()
  if (props.kind !== 'purchase' || !supplierId) return
  const list = await supplierPrices(supplierId)
  prices.value = new Map(list.map((p) => [p.materialId, p]))
}

// 自定义字段（采购订单 scm_purchase / 销售订单 scm_sales）
const cf = useCustomFields(props.kind === 'sales' ? 'scm_sales' : 'scm_purchase')
const cfFields = cf.fields
const drawer = useFormDrawer(
  cf.wrap({
    emptyForm,
    fetch: async (record) => {
      const order = await getOrder(props.kind, record.id)
      await loadPrices(order.partnerId)
      // 单价按优惠前的原价编辑（保存时重新分摊整单优惠）
      const items = order.items.map((i) => toRow(i.origPrice != null ? { ...i, price: i.origPrice } : i))
      await Promise.all(items.map(attachUnits))
      return {
        ...order,
        currency: order.currency || BASE_CURRENCY,
        exchangeRate: order.exchangeRate != null ? Number(order.exchangeRate) : 1,
        wholeDiscount: Number(order.wholeDiscount) || undefined,
        invoiceKind: order.invoiceKind || undefined,
        items
      }
    },
    beforeOpen: (record) => {
      unitCache.clear()
      if (!record) prices.value = new Map()
      partnerLevel.value = undefined
    },
    create: async (data) => {
      const id = await addOrder(props.kind, data)
      emit('success', id)
      return id
    },
    update: async (id, data) => {
      await updateOrder(props.kind, id, data)
      emit('success', id)
    },
    toPayload: (f) => ({
      partnerId: f.partnerId,
      orderDate: f.orderDate,
      deliveryDate: f.deliveryDate || null,
      warehouseId: f.warehouseId || null,
      currency: f.currency || BASE_CURRENCY,
      exchangeRate: isForeign(f.currency) ? f.exchangeRate : 1,
      wholeDiscount: f.wholeDiscount || null,
      invoiceKind: f.invoiceKind || null,
      remark: f.remark,
      items: f.items.map((i) => ({
        materialId: i.materialId,
        qty: baseQty(i),
        price: basePrice(i),
        unitName: i.unitName || null,
        taxRate: i.taxRate ?? 0,
        deliveryDate: i.deliveryDate || null,
        remark: i.remark || null
      }))
    })
  })
)
const { form, formRef } = drawer
// 关闭表单时清除智能录入的提示
watch(drawer.open, (v) => !v && props.ai?.clear())

// ---------------------------------------------------------------- 币种、汇率
const currencyList = useCurrencies()
const foreign = computed(() => isForeign(form.value.currency))
const symbol = computed(() => currencySymbol(form.value.currency))

/** 人民币价格折算为订单币种（外币时除以汇率，保留 4 位小数；未填汇率时不折算） */
const fromBase = (price) => {
  const rate = Number(form.value.exchangeRate)
  return foreign.value && rate > 0 ? qty4(Number(price) / rate) : Number(price)
}

/** 自动带出的单价按新的币种 / 汇率重新折算 */
function refillAutoPrices() {
  if (props.kind === 'sales') {
    fetchSalesPrices(form.value.items).catch(() => {})
    return
  }
  form.value.items.forEach((row) => row.priceAuto && fillPrice(row, true))
}

let rateSeq = 0
/** 按订单日期带出记账汇率（可修改） */
async function refreshRate() {
  if (!foreign.value) return
  const seq = ++rateSeq
  const rate = await fetchRate(form.value.currency, form.value.orderDate)
  if (seq !== rateSeq || rate == null) return
  form.value.exchangeRate = rate
  refillAutoPrices()
}

async function onCurrencyChange(code) {
  form.value.currency = code
  if (!isForeign(code)) {
    form.value.exchangeRate = 1
    refillAutoPrices()
    return
  }
  form.value.exchangeRate = undefined
  await refreshRate()
  if (form.value.exchangeRate == null) message.warning(t('scm.orderForm.noRate', { code }))
}

function onRateChange() {
  refillAutoPrices()
}

const totalQty = computed(() => form.value.items.reduce((sum, i) => sum + baseQty(i), 0))
const totalAmount = computed(() => form.value.items.reduce((sum, i) => sum + lineAmount(i.unitQty, i.unitPrice), 0))

// 按规格录入：款式的规格矩阵中填写的各 SKU 数量逐行加入明细（单价按物料自动带出）
const matrixOpen = ref(false)
function addVariantRows(lines) {
  lines.forEach(({ material, qty }) => {
    addRow()
    const row = form.value.items[form.value.items.length - 1]
    row.materialId = material.id
    applyMaterial(row, material)
    row.unitQty = qty
  })
}

function addRow() {
  form.value.items.push({
    key: rowKey(),
    materialId: undefined,
    unitName: null,
    unitRatio: 1,
    unitQty: undefined,
    unitPrice: undefined,
    units: [],
    taxRate: tax.fallback(form.value.invoiceKind),
    priceAuto: true
  })
}

// ---------------------------------------------------------------- 税率：发票类型、统一设置（issue #82）
/** 默认税率（业务参数）与发票类型：新明细的税率为发票类型的税率，没有时为默认税率 */
const tax = useTaxRates()
function onInvoiceKindChange(kind, rate) {
  if (rate == null) return
  message.success(t('scm.taxRate.invoiceKindApplied', { n: applyTaxRate(form.value.items, rate), rate }))
}
function applyAllTaxRate(rate) {
  message.success(t('scm.taxRate.applied', { n: applyTaxRate(form.value.items, rate), rate }))
}

const partnerLevel = ref()
function onPartnerSelect(partner) {
  form.value.partnerName = `${partner.code} ${partner.name}`
  return loadPartnerData(partner)
}

/**
 * 选定往来单位后加载相关数据：销售订单按客户重新取价；采购订单加载供应商的供货价格。
 * keepPrices（打开时已带入明细，如智能录入的预填）：保留已有的单价，只补全空的单价、税率；
 * 否则（手工换了往来单位）自动带出的单价按新单位更新
 */
async function loadPartnerData(partner, keepPrices = false) {
  partnerLevel.value = partner.level
  if (props.kind === 'sales') {
    // 手工改过（含预填）的单价不覆盖；所有行一次取价
    await fetchSalesPrices(form.value.items).catch(() => {})
    return
  }
  await loadPrices(partner.id)
  form.value.items.forEach((row) => fillPrice(row, !keepPrices))
}

/** 按 ID 查往来单位（取供应商等级）；label 为「编码 名称」，按编码搜索，查不到时只用 ID */
async function findPartner(id, label) {
  const code = label?.split(' ')[0]
  const list = code ? await partnerOptions(conf.value.partner, code).catch(() => []) : []
  return list.find((p) => p.id === id) || { id }
}

/** 销售订单取单行的开单默认价 */
const fetchSalesPrice = (row) => fetchSalesPrices([row])

/**
 * 销售订单按开单默认价取价（多行一次请求）：单价为空或是自动带出的，更新单价（有税率时更新税率）
 */
async function fetchSalesPrices(rows) {
  const list = rows.filter((row) => row.materialId)
  if (props.kind !== 'sales' || !list.length || !form.value.partnerId) return
  const seqs = list.map((row) => (row.priceSeq = (row.priceSeq || 0) + 1))
  const res = await defaultPrices({
    kind: OrderKind.SALES,
    partnerId: form.value.partnerId,
    materialIds: list.map((row) => row.materialId),
    qtys: list.map((row) => baseQty(row) || 1),
    date: form.value.orderDate || undefined
  })
  list.forEach((row, i) => {
    if (seqs[i] !== row.priceSeq) return
    // 默认价按行返回（与请求的物料一一对应），按基本单位、本位币
    const p = res[i]?.materialId === row.materialId ? res[i] : undefined
    if (p && p.price != null && (row.priceAuto || row.unitPrice == null)) {
      row.unitPrice = qty4(fromBase(Number(p.price) * ratioOf(row)))
      // 选了发票类型时税率按发票类型，否则按价格来源的税率（没有时保持原税率）
      const rate = tax.sourceRate(form.value.invoiceKind, p.taxRate)
      if (rate != null) row.taxRate = rate
      row.priceAuto = true
      row.priceSource = p.source
    }
  })
}

function fillPrice(row, overwrite) {
  const p = prices.value.get(row.materialId)
  if (!p) return
  // 供货价格按基本单位
  if (overwrite || row.unitPrice == null) {
    row.unitPrice = qty4(fromBase(Number(p.price) * ratioOf(row)))
    row.priceAuto = true
  }
  if (overwrite || row.taxRate == null) row.taxRate = tax.sourceRate(form.value.invoiceKind, p.taxRate) ?? row.taxRate
  row.minQty = p.minQty ? Number(p.minQty) : undefined
}

// ---------------------------------------------------------------- 供货价提示
/** 采购订单：单价与供应商供货价不一致时返回供货价（订单单位、订单币种），用于提示 */
function supplyPriceDiff(row) {
  if (props.kind !== 'purchase' || row.unitPrice == null) return null
  const p = prices.value.get(row.materialId)
  if (!p) return null
  const price = qty4(fromBase(Number(p.price) * ratioOf(row)))
  return Math.abs(price - Number(row.unitPrice)) > 0.00005 ? price : null
}
function useSupplyPrice(row) {
  row.unitPrice = supplyPriceDiff(row)
  row.priceAuto = true
}

function applyMaterial(row, material) {
  Object.assign(row, {
    materialLabel: material.label,
    materialCode: material.code,
    materialName: material.name,
    unit: material.unit,
    serialEnabled: material.serialEnabled,
    // 扫辅助单位条码时直接选该单位
    unitName: material.scanUnit || null,
    unitRatio: material.scanUnit ? Number(material.scanRatio) || 1 : 1,
    units: material.scanUnit ? [{ unit: material.scanUnit, ratio: Number(material.scanRatio) || 1 }] : []
  })
  attachUnits(row)
  if (props.kind === 'sales') {
    row.priceSource = undefined
    row.priceAuto = true
    fetchSalesPrice(row).catch(() => {})
    return
  }
  fillPrice(row, true)
}

// 价格参考：点选的历史价格（基本单位、本位币）按当前单位、币种填入单价
const userStore = useUserStore()
const canReference = computed(() =>
  userStore.hasPerm(['scm:sales:list', 'scm:salesOut:list', 'scm:purchase:list', 'scm:purchaseIn:list'])
)
const referenceRef = ref()
let referenceRow
function openReference(row) {
  referenceRow = row
  referenceRef.value.open({
    materialId: row.materialId,
    materialName: row.materialLabel,
    partnerId: form.value.partnerId,
    kind: props.kind === 'purchase' ? OrderKind.PURCHASE : OrderKind.SALES
  })
}
function onReferencePick(price) {
  if (!referenceRow) return
  referenceRow.unitPrice = qty4(fromBase(price * ratioOf(referenceRow)))
  referenceRow.priceAuto = false
}

/**
 * 传入的明细（基本单位）转为表单行。带了往来单位（如智能录入的预填）时与手工选择一样加载供货价格等，
 * 但保留传入的单价：只补全空的单价，税率为空时按供货价 / 默认价带出（都没有时按发票类型 / 默认税率）
 */
async function openCreate(defaults = {}) {
  // 同步调用 drawer.openCreate，之前不能有 await（asPrefilled 依赖这一点）
  await drawer.openCreate(defaults.items?.length ? { ...defaults, items: defaults.items.map(toRow) } : defaults)
  const noTax = new Set()
  form.value.items.forEach((row, i) => {
    if (defaults.items?.[i]?.taxRate == null) {
      noTax.add(row)
      row.taxRate = null
    }
    attachUnits(row)
  })
  if (defaults.partnerId) {
    const partner = await findPartner(defaults.partnerId, defaults.partnerName)
    if (form.value.partnerId === defaults.partnerId) await loadPartnerData(partner, true).catch(() => {})
  }
  await tax.ready
  noTax.forEach((row) => row.taxRate == null && (row.taxRate = tax.fallback(form.value.invoiceKind)))
}

defineExpose({ openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.items-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}
.section-title {
  font-weight: 500;
}
.items-table :deep(.ant-table-cell) {
  vertical-align: top;
}
.cell-item {
  margin-bottom: 0;
}
.amount {
  line-height: 32px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.unit-select {
  min-width: 64px;
}
.ref-link {
  font-size: 12px;
}
.fc-tip {
  margin-bottom: 16px;
}
.level-warning {
  margin-top: -8px;
  margin-bottom: 12px;
}
</style>
