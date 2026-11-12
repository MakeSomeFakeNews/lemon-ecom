<template>
  <a-modal v-model:open="open" :title="$t('ecomDev.order.createTitle')" width="980px" :confirm-loading="creating" :ok-text="$t('ecomDev.order.create')"
           :ok-button-props="{ disabled: !preview?.canOrder }" @ok="create">
    <a-form v-if="!fixedPo" layout="inline" class="po-select">
      <a-form-item :label="$t('ecomDev.order.po')">
        <RemoteSelect v-model:value="poId" :fetch="fetchPo" :to-label="poText" class="po" :placeholder="$t('ecomDev.order.poPh')"
                      @update:value="loadPreview" />
      </a-form-item>
    </a-form>
    <a-spin :spinning="loading">
      <template v-if="preview">
        <a-descriptions size="small" :column="3" class="block">
          <a-descriptions-item :label="$t('ecomDev.order.po')">{{ preview.scmOrderNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomDev.order.supplier')">{{ preview.supplierName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.status')"><DictTag type="scm_order_status" :value="preview.scmOrderStatus" /></a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="!preview.canOrder" type="warning" show-icon class="block" :message="$t('ecomDev.order.cannotOrder')" />
        <div v-for="(g, gi) in preview.groups" :key="gi" class="group">
          <div class="group-head">
            <span><ShopOutlined /> {{ g.sellerName || g.sellerLoginId || $t('ecomDev.order.unknownSeller') }}</span>
            <span>{{ $t('ecomDev.order.groupAmount', { amount: money(g.amount) }) }}</span>
          </div>
          <a-table row-key="scmOrderItemId" size="small" :columns="lineColumns" :data-source="g.lines" :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'material'">
                <div>{{ record.materialCode }} {{ record.materialName }}</div>
                <div class="sub-text">{{ qty(record.materialQty) }}</div>
              </template>
              <template v-else-if="column.key === 'offer'">
                <div class="ellipsis" :title="record.title">{{ record.title || record.offerId }}</div>
                <div class="sub-text">{{ record.offerId }}<span v-if="record.specText"> · {{ record.specText }}</span></div>
              </template>
              <template v-else-if="column.key === 'aliQty'">
                {{ record.aliQty }}
                <div v-if="Number(record.qtyRatio) !== 1" class="sub-text">÷ {{ Number(record.qtyRatio) }}</div>
                <a-tag v-if="record.belowMinQty" color="orange">{{ $t('ecomDev.order.belowMoq', { n: record.minOrderQty }) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'price'">{{ record.price != null ? price(record.price) : '-' }}</template>
              <template v-else-if="column.key === 'amount'">{{ record.amount != null ? money(record.amount) : '-' }}</template>
            </template>
          </a-table>
        </div>
        <template v-if="preview.skipped?.length">
          <div class="section-title">{{ $t('ecomDev.order.skipped') }}</div>
          <div v-for="s in preview.skipped" :key="s.scmOrderItemId" class="skipped">
            {{ s.materialCode }} {{ s.materialName }} × {{ qty(s.qty) }} <span class="sub-text">— {{ s.reason }}</span>
          </div>
        </template>
        <template v-if="preview.existing?.length">
          <div class="section-title">{{ $t('ecomDev.order.existing') }}</div>
          <div v-for="o in preview.existing" :key="o.id" class="skipped">
            {{ o.aliOrderId }} <DictTag type="ecom_1688_order_status" :value="o.status" /> <span class="sub-text">{{ symMoney(o.totalAmount) }} · {{ o.sellerName || o.sellerLoginId }}</span>
          </div>
        </template>
        <a-form layout="vertical" class="opts">
          <a-row :gutter="16">
            <a-col :span="10">
              <a-form-item :label="$t('ecomDev.order.account')">
                <a-select v-model:value="accountId" :options="accountOptions" :placeholder="$t('ecomDev.order.defaultAccount')" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="14">
              <a-form-item :label="$t('ecomDev.order.message')">
                <a-input v-model:value="messageText" :maxlength="500" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>
      <a-empty v-else-if="!loading" :description="$t('ecomDev.order.selectPo')" />
    </a-spin>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ShopOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import RemoteSelect from '@/components/common/RemoteSelect.vue'
import { createAli1688Order, listAli1688Account, previewAli1688Order } from '@/api/ecom/dev'
import { OrderStatus } from '@/constants/enums'
import request from '@/utils/request'
import { money, price, qty, symMoney } from '@/utils/number'

/**
 * 采购订单一键下单到 1688：预览按卖家拆分的订单（物料 → 1688 规格、下单数量 = 采购数量 ÷ 换算），选择下单账号、填写留言后下单；
 * 没有关联货源或已下过单的明细列在下方。ref 调用 open(scmOrderId?)，下单成功后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const open = ref(false)
const loading = ref(false)
const creating = ref(false)
const preview = ref()
const poId = ref()
const fixedPo = ref(false)
const accountId = ref()
const accounts = ref([])
const messageText = ref('')

const accountOptions = computed(() =>
  accounts.value.filter((a) => a.authorized && a.status === 1).map((a) => ({ value: a.id, label: a.isDefault === 1 ? `${a.name} ★` : a.name }))
)

const lineColumns = [
  { title: t('ecomDev.order.material'), key: 'material', width: 220 },
  { title: t('ecomDev.order.offer'), key: 'offer', width: 300 },
  { title: t('ecomDev.order.aliQty'), key: 'aliQty', width: 110, align: 'right' },
  { title: t('ecomDev.order.price'), key: 'price', width: 90, align: 'right' },
  { title: t('ecomDev.order.amount'), key: 'amount', width: 100, align: 'right' }
]

/** 采购订单下拉：只列已审核 / 部分入库的 */
const fetchPo = ({ keyword, current, size }) =>
  request.get('/scm/purchase/order', { params: { orderNo: keyword || undefined, status: OrderStatus.APPROVED, current, size } })
const poText = (o) => `${o.orderNo} ${o.partnerName || ''}`

async function loadPreview() {
  await Promise.resolve()
  if (!poId.value) {
    preview.value = undefined
    return
  }
  loading.value = true
  try {
    preview.value = await previewAli1688Order(poId.value)
  } finally {
    loading.value = false
  }
}

async function show(scmOrderId) {
  fixedPo.value = !!scmOrderId
  poId.value = scmOrderId
  preview.value = undefined
  messageText.value = ''
  accountId.value = undefined
  open.value = true
  listAli1688Account().then((list) => (accounts.value = list || [])).catch(() => {})
  if (scmOrderId) await loadPreview()
}

async function create() {
  creating.value = true
  try {
    const r = await createAli1688Order({ scmOrderId: poId.value, accountId: accountId.value ?? null, message: messageText.value || null })
    open.value = false
    if (r.failures?.length) {
      Modal.warning({
        title: t('ecomDev.order.partialFailed', { ok: r.created.length, failed: r.failures.length }),
        content: r.failures.join('\n')
      })
    } else {
      message.success(t('ecomDev.order.created', { nos: r.created.map((o) => o.aliOrderId).join(', ') }))
    }
    emit('success', r)
  } finally {
    creating.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.po-select {
  margin-bottom: 12px;
}
.po {
  width: 420px;
}
.block {
  margin-bottom: 12px;
}
.group {
  margin-bottom: 12px;
}
.group-head {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  margin-bottom: 6px;
}
.section-title {
  margin: 12px 0 6px;
  font-weight: 600;
}
.skipped {
  font-size: 13px;
  margin-bottom: 2px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}
.opts {
  margin-top: 12px;
}
</style>
