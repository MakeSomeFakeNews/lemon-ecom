<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOrd.manual.title')" page>
    <a-alert type="info" show-icon :message="$t('ecomOrd.manual.help')" class="block" />
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomOrd.manual.sectionOrder') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.store')" name="storeId">
            <StoreSelect v-model:value="form.storeId" @update:value="onStore" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrd.manual.orderNo')" name="platformOrderNo">
            <a-input v-model:value="form.platformOrderNo" :maxlength="64" :placeholder="$t('ecomOrd.manual.orderNoPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.order.orderTime')" name="orderTime">
            <a-date-picker v-model:value="form.orderTime" show-time value-format="YYYY-MM-DD HH:mm:ss" class="full" :placeholder="$t('ecomOrd.manual.nowPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.order.fulfillment')" name="fulfillment">
            <a-select v-model:value="form.fulfillment" :options="toOptions(dict.ecom_fulfillment)" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrd.manual.buyerName')" name="buyerName">
            <a-input v-model:value="form.buyerName" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrd.manual.buyerEmail')" name="buyerEmail">
            <a-input v-model:value="form.buyerEmail" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrder.common.currency')" name="currency">
            <a-input v-model:value="form.currency" :maxlength="8" :placeholder="storeCurrency || $t('ecomOrd.manual.currencyPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-form-item :label="$t('ecomOrd.manual.shippingAmount')" name="shippingAmount">
            <a-input-number v-model:value="form.shippingAmount" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomOrd.manual.sectionAddress') }}</div>
      <AddressFields :form="form" />

      <div class="section-title">{{ $t('ecomOrd.manual.sectionLines') }}</div>
      <a-table row-key="key" size="small" :columns="lineColumns" :data-source="form.lines" :pagination="false" :scroll="{ x: 860 }" class="block">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'listing'">
            <ListingSelect v-model:value="record.listingId" :label="record.sellerSku" :store-id="form.storeId" class="full" @select="(l) => pickListing(record, l)" />
          </template>
          <template v-else-if="column.key === 'sellerSku'">
            <a-input v-model:value="record.sellerSku" :maxlength="64" />
          </template>
          <template v-else-if="column.key === 'title'">
            <a-input v-model:value="record.title" :maxlength="255" />
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="0.0001" :precision="0" class="full" />
          </template>
          <template v-else-if="column.key === 'price'">
            <a-input-number v-model:value="record.price" :min="0" :precision="2" class="full" />
          </template>
          <template v-else-if="column.key === 'amount'">{{ money((record.qty || 0) * (record.price || 0)) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" :disabled="form.lines.length <= 1" @click="form.lines.splice(index, 1)">
              <DeleteOutlined />
            </a-button>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block @click="addLine"><PlusOutlined /> {{ $t('ecomOrd.manual.addLine') }}</a-button>
      <div class="total">{{ $t('ecomOrd.manual.total', { amount: money(total) }) }}</div>

      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOrder.ops.weight')" name="weight" :extra="$t('ecomOrd.manual.weightHint')">
            <a-input-number v-model:value="form.weight" :min="0" :precision="3" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" :placeholder="$t('ecomOrd.manual.remarkPh')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import FormDrawer from '@/components/FormDrawer.vue'
import ListingSelect from '@/components/ecom/ListingSelect.vue'
import StoreSelect, { useStores } from '@/components/ecom/StoreSelect.vue'
import AddressFields from './AddressFields.vue'
import { createManualOrder } from '@/api/ecom/orderEnhance'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'
import { money } from '@/utils/number'
import { FulfillmentType } from '@/constants/enums'

/**
 * 手工创建订单（线下 / 站外 / 独立站）：选店铺、买家、收件地址、商品（按店铺的在线商品选择或直接填写平台 SKU），
 * 订单号留空自动生成；保存后与平台订单一样执行审单规则、地址校验与利润预估。openCreate() 打开，保存后触发 success(id)
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_fulfillment')
const stores = useStores()
let lineKey = 0
let createdId

const newLine = () => ({ key: ++lineKey, listingId: undefined, sellerSku: '', title: '', qty: 1, price: undefined })
const storeCurrency = computed(() => stores.value.find((s) => s.id === form.value.storeId)?.currency)
const total = computed(() => form.value.lines.reduce((sum, l) => sum + (l.qty || 0) * (l.price || 0), 0) + (form.value.shippingAmount || 0))

const required = (key) => [{ required: true, whitespace: true, message: t('common.rule.required', { name: t(key) }) }]
const rules = {
  storeId: [{ required: true, message: t('common.rule.required', { name: t('ecomOrder.common.store') }) }],
  shipName: required('ecomOrder.order.receiver'),
  shipCountry: required('ecomOrder.order.country')
}

const lineColumns = [
  { title: t('ecomOrd.manual.listing'), key: 'listing', width: 260 },
  { title: t('ecomOrder.common.sellerSku'), key: 'sellerSku', width: 160 },
  { title: t('ecomOrder.order.itemTitle'), key: 'title', width: 200 },
  { title: t('ecomOrder.common.qty'), key: 'qty', width: 90 },
  { title: t('ecomOrder.order.itemPrice'), key: 'price', width: 110 },
  { title: t('ecomOrder.order.itemAmountCol'), key: 'amount', width: 100, align: 'right' },
  { title: '', key: 'action', width: 50 }
]

function onStore() {
  form.value.lines.forEach((l) => (l.listingId = undefined))
}
function pickListing(line, l) {
  line.sellerSku = l.sellerSku
  line.title = l.title || ''
  if (l.price != null && line.price == null) line.price = Number(l.price)
}
function addLine() {
  form.value.lines.push(newLine())
}

const drawer = useFormDrawer({
  emptyForm: () => ({
    storeId: undefined,
    platformOrderNo: '',
    orderTime: undefined,
    fulfillment: FulfillmentType.SELLER,
    buyerName: '',
    buyerEmail: '',
    shipName: '',
    shipPhone: '',
    shipCountry: '',
    shipState: '',
    shipCity: '',
    shipPostcode: '',
    shipAddress: '',
    currency: '',
    shippingAmount: undefined,
    weight: undefined,
    remark: '',
    lines: [newLine()]
  }),
  create: async (payload) => {
    if (!payload.lines.length) {
      message.warning(t('ecomOrd.manual.linesRequired'))
      throw Object.assign(new Error('lines'), { handled: true })
    }
    createdId = await createManualOrder(payload)
  },
  toPayload: (f) => ({
    storeId: f.storeId,
    platformOrderNo: f.platformOrderNo?.trim() || null,
    orderTime: f.orderTime || null,
    fulfillment: f.fulfillment,
    buyerName: f.buyerName || null,
    buyerEmail: f.buyerEmail?.trim() || null,
    shipName: f.shipName,
    shipPhone: f.shipPhone || null,
    shipCountry: f.shipCountry?.trim().toUpperCase(),
    shipState: f.shipState || null,
    shipCity: f.shipCity || null,
    shipPostcode: f.shipPostcode || null,
    shipAddress: f.shipAddress || null,
    currency: f.currency?.trim().toUpperCase() || null,
    shippingAmount: f.shippingAmount ?? null,
    weight: f.weight ?? null,
    remark: f.remark || null,
    lines: f.lines.filter((l) => l.sellerSku?.trim()).map((l) => ({ sellerSku: l.sellerSku.trim(), title: l.title || null, qty: l.qty, price: l.price ?? 0 }))
  }),
  successMessage: t('ecomOrd.manual.created'),
  onSuccess: () => emit('success', createdId)
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.full {
  width: 100%;
}
.section-title {
  margin: 4px 0 12px;
  font-weight: 600;
}
.total {
  margin: 8px 0 16px;
  text-align: right;
  font-weight: 600;
}
</style>
