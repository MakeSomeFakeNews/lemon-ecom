<template>
  <a-modal v-model:open="open" :title="$t('ecomOrder.order.shipTitle', { no: order?.platformOrderNo || '' })" width="min(760px, 100vw)" :confirm-loading="saving" @ok="submit">
    <a-alert type="info" show-icon class="tip" :message="$t('ecomOrder.order.shipHint')" />
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.common.carrier')" name="carrier" :rules="[{ required: true, message: $t('ecomOrder.order.inputCarrier') }]">
            <a-input v-model:value="form.carrier" :maxlength="64" :placeholder="$t('ecomOrder.order.carrierPh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.common.trackingNo')" name="trackingNo" :rules="[{ required: true, message: $t('ecomOrder.order.inputTrackingNo') }]">
            <a-input v-model:value="form.trackingNo" :maxlength="64" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.order.shipTime')" name="shipTime">
            <a-date-picker v-model:value="form.shipTime" show-time value-format="YYYY-MM-DD HH:mm:ss" format="YYYY-MM-DD HH:mm" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.common.channel')" name="channelId">
            <ChannelSelect v-model:value="form.channelId" :label="order?.channelName" @select="onChannel" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.order.weight')" name="weight">
            <a-input-number v-model:value="form.weight" :min="0" :precision="3" class="full">
              <template #addonAfter>
                <span class="weight-unit">{{ $t('ecomOrder.common.kg') }}</span>
                <a-button type="link" size="small" :loading="estimating" class="estimate-btn" @click="estimate">{{ $t('ecomOrder.order.estimate') }}</a-button>
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="12">
          <a-form-item :label="$t('ecomOrder.order.shippingCostInput')" name="shippingCost" :extra="$t('ecomOrder.order.shippingCostHint')">
            <a-input-number v-model:value="form.shippingCost" :min="0" :precision="2" prefix="¥" class="full" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <!-- 运费试算结果：选中一行填入渠道与运费 -->
    <template v-if="estimates">
      <div class="sub-text est-hint">{{ $t('ecomOrder.order.estimateHint', { country: order?.shipCountry, weight: estimatedWeight }) }}</div>
      <a-table
        row-key="channelId"
        size="small"
        :columns="estColumns"
        :data-source="estimates"
        :pagination="false"
        :row-selection="{ type: 'radio', selectedRowKeys: form.channelId != null ? [form.channelId] : [], onChange: (keys) => pick(keys[0]) }"
        :custom-row="(r) => ({ onClick: () => pick(r.channelId), class: 'est-row' })"
        :locale="{ emptyText: $t('ecomOrder.order.estimateEmpty', { country: order?.shipCountry }) }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'channel'">
            <div>{{ record.channelName }}</div>
            <div class="sub-text">{{ record.channelCode }}</div>
          </template>
          <template v-else-if="column.key === 'fee'"><b>{{ symMoney(record.fee) }}</b></template>
          <template v-else-if="column.key === 'days'">
            {{ record.daysMin != null || record.daysMax != null ? $t('ecomOrder.order.estDaysValue', { min: record.daysMin ?? '', max: record.daysMax ?? '' }) : '-' }}
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { estimateFreight } from '@/api/ecom/logistics'
import { shipEcomOrder } from '@/api/ecom/order'
import { money, symMoney } from '@/utils/number'

/**
 * 订单发货：填写承运商、运单号、发货时间，可选物流渠道并按重量「试算运费」（GET /ecom/logistics/estimate）列出各渠道运费，
 * 选中一条填入渠道与运费；运费（本位币）也可手填，留空时后端按渠道和重量估算。提交 POST /ecom/order/{id}/ship，完成后触发 done
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const estColumns = [
  { title: t('ecomOrder.order.estChannel'), key: 'channel', width: 200 },
  { title: t('ecomOrder.order.estProvider'), dataIndex: 'provider', width: 120 },
  { title: t('ecomOrder.order.estMode'), dataIndex: 'transportMode', width: 110 },
  { title: t('ecomOrder.order.estFee'), key: 'fee', width: 120, align: 'right' },
  { title: t('ecomOrder.order.estDays'), key: 'days', width: 120 }
]

const open = ref(false)
const saving = ref(false)
const estimating = ref(false)
const formRef = ref()
const order = ref()
const form = reactive({ carrier: '', trackingNo: '', shipTime: undefined, channelId: undefined, weight: undefined, shippingCost: undefined })
/** 试算结果，没有试算过时为 null */
const estimates = ref(null)
const estimatedWeight = ref()

function show(o) {
  order.value = o
  Object.assign(form, {
    carrier: o.carrier || '',
    trackingNo: o.trackingNo || '',
    shipTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    channelId: o.channelId ?? undefined,
    weight: undefined,
    shippingCost: undefined
  })
  estimates.value = null
  open.value = true
  // 弹窗内容在打开后才渲染，上一次的校验提示在下一帧清除
  nextTick(() => formRef.value?.clearValidate())
}

/** 手工改渠道时清掉按别的渠道选中的运费（保留手填的运费由用户自行判断，这里只在来自试算时清除） */
function onChannel(channel) {
  const hit = estimates.value?.find((e) => e.channelId === channel.id)
  if (hit) form.shippingCost = Number(hit.fee)
}

async function estimate() {
  if (!order.value?.shipCountry) {
    message.warning(t('ecomOrder.order.estimateNeedCountry'))
    return
  }
  if (!(Number(form.weight) > 0)) {
    message.warning(t('ecomOrder.order.estimateNeedWeight'))
    return
  }
  estimating.value = true
  try {
    estimates.value = (await estimateFreight(order.value.shipCountry, form.weight)) || []
    estimatedWeight.value = form.weight
    // 当前渠道在结果中时直接带出运费
    const hit = estimates.value.find((e) => e.channelId === form.channelId)
    if (hit) form.shippingCost = Number(hit.fee)
  } finally {
    estimating.value = false
  }
}

function pick(channelId) {
  const hit = estimates.value?.find((e) => e.channelId === channelId)
  if (!hit) return
  form.channelId = hit.channelId
  form.shippingCost = Number(hit.fee)
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    await shipEcomOrder(order.value.id, {
      carrier: form.carrier.trim(),
      trackingNo: form.trackingNo.trim(),
      shipTime: form.shipTime || undefined,
      channelId: form.channelId ?? undefined,
      weight: form.weight ?? undefined,
      shippingCost: form.shippingCost ?? undefined
    })
    message.success(t('ecomOrder.order.shipped'))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.full {
  width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.est-hint {
  margin: 4px 0 8px;
}
.weight-unit {
  margin-right: 8px;
  color: var(--app-text-3);
}
.estimate-btn {
  height: 22px;
  padding: 0 4px;
}
:deep(.est-row) {
  cursor: pointer;
}
</style>
