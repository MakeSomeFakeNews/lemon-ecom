<template>
  <SubPage :open="open" :title="detail ? $t('ecomOrder.return.detailTitle', { no: detail.docNo }) : $t('ecomOrder.return.title')" @close="open = false">
    <template v-if="detail" #extra>
      <a-space>
        <DictTag type="ecom_return_type" :value="detail.returnType" />
        <DictTag type="ecom_return_status" :value="detail.status" />
      </a-space>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('ecomOrder.common.docNo')">{{ detail.docNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.store')">{{ detail.storeName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.common.platformOrderNo')">{{ detail.platformOrderNo }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.buyer')">{{ detail.buyerName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.reason')">
            <DictTag v-if="detail.reason" type="ecom_return_reason" :value="detail.reason" />
            <span v-else>-</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.refundAmount')">
            <b>{{ fcMoney(detail.currency, detail.refundAmount) }}</b>
            <span v-if="detail.exchangeRate != null" class="sub-text"> @{{ Number(detail.exchangeRate) }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.warehouse')">{{ detail.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.tradeDoc')">{{ detail.tradeDocNos || detail.tradeDocNo || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomOrder.return.refundTime')">{{ detail.refundTime || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('common.field.createTime')">{{ detail.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="2">{{ detail.remark }}</a-descriptions-item>
        </a-descriptions>

        <div class="section-title">{{ $t('ecomOrder.return.items') }}</div>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="detail.items || []" :pagination="false" :scroll="{ x: 800 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'material'">
              <template v-if="record.materialId">{{ record.materialCode }} {{ record.materialName }}</template>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'qty'">{{ qty(record.qty) }}</template>
            <template v-else-if="column.key === 'refundAmount'">{{ fcMoney(detail.currency, record.refundAmount) }}</template>
          </template>
        </a-table>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { DeleteOutlined, ImportOutlined, PayCircleOutlined, StopOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import { closeEcomReturn, deleteEcomReturn, getEcomReturn, refundEcomReturn } from '@/api/ecom/return'
import { useUserStore } from '@/store/user'
import { qty } from '@/utils/number'
import { fcMoney } from '../order/order'
import { canClose, canDelete, canReceive, canRefund } from './return'

/**
 * 退货退款详情子页：登记退款、关闭、删除在本组件内完成（触发 changed）；退货入库需要选择日期，交给页面（事件 receive 带单据）。
 * ref 调用 open(id)、reload()
 */
const emit = defineEmits(['changed', 'receive'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const itemColumns = [
  { title: t('ecomOrder.common.sellerSku'), dataIndex: 'sellerSku', width: 150 },
  { title: t('ecomOrder.return.itemTitle'), dataIndex: 'title', width: 260, ellipsis: true },
  { title: t('ecomOrder.common.material'), key: 'material', width: 220 },
  { title: t('ecomOrder.return.returnQty'), key: 'qty', width: 110, align: 'right' },
  { title: t('ecomOrder.return.lineRefund'), key: 'refundAmount', width: 140, align: 'right' },
  { title: t('ecomOrd.returnPkg.package'), dataIndex: 'packageNo', width: 170 }
]

const open = ref(false)
const loading = ref(false)
const detail = ref()
let currentId

async function load(id) {
  loading.value = true
  try {
    detail.value = await getEcomReturn(id)
  } finally {
    loading.value = false
  }
}

function show(id) {
  currentId = id
  detail.value = undefined
  open.value = true
  return load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)

function confirmAction(title, content, api, success, { danger = false, closeAfter = false } = {}) {
  Modal.confirm({
    title,
    content,
    okType: danger ? 'danger' : 'primary',
    async onOk() {
      await api(currentId)
      message.success(success)
      if (closeAfter) open.value = false
      else await load(currentId)
      emit('changed')
    }
  })
}

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (canDelete(d) && can('ecom:return:remove')) {
    list.push({
      key: 'delete',
      label: t('common.action.delete'),
      icon: DeleteOutlined,
      danger: true,
      run: () => confirmAction(t('ecomOrder.return.deleteConfirm', { no: d.docNo }), '', deleteEcomReturn, t('common.msg.deleteSuccess'), { danger: true, closeAfter: true })
    })
  }
  if (canClose(d) && can('ecom:return:add')) {
    list.push({
      key: 'close',
      label: t('ecomOrder.return.close'),
      icon: StopOutlined,
      run: () => confirmAction(t('ecomOrder.return.closeConfirm', { no: d.docNo }), '', closeEcomReturn, t('ecomOrder.return.closed'))
    })
  }
  if (canReceive(d) && can('ecom:return:receive')) {
    list.push({ key: 'receive', label: t('ecomOrder.return.receive'), icon: ImportOutlined, primary: true, run: () => emit('receive', d) })
  }
  if (canRefund(d) && can('ecom:return:refund')) {
    list.push({
      key: 'refund',
      label: t('ecomOrder.return.refund'),
      icon: PayCircleOutlined,
      primary: true,
      run: () =>
        confirmAction(
          t('ecomOrder.return.refundConfirm', { no: d.docNo }),
          t('ecomOrder.return.refundContent', { amount: fcMoney(d.currency, d.refundAmount) }),
          refundEcomReturn,
          t('ecomOrder.return.refunded')
        )
    })
  }
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
