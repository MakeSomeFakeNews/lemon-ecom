<template>
  <div>
    <!-- 地址校验与黑名单 -->
    <a-alert v-if="check && (check.issues.length || check.blacklistId)" :type="check.invalid || check.blacklistId ? 'error' : 'warning'" show-icon class="block">
      <template #message>
        <span>{{ check.invalid ? $t('ecomOrd.check.invalid') : check.blacklistId ? $t('ecomOrd.check.blacklisted') : $t('ecomOrd.check.warnings') }}</span>
      </template>
      <template #description>
        <div v-if="check.blacklistDesc" class="issue"><a-tag color="magenta" :bordered="false">{{ $t('ecomOrd.check.blacklist') }}</a-tag>{{ check.blacklistDesc }}</div>
        <div v-for="(i, k) in check.issues" :key="k" class="issue">
          <a-tag :color="i.error ? 'error' : 'warning'" :bordered="false">{{ i.error ? $t('ecomOrd.addr.error') : $t('ecomOrd.addr.warn') }}</a-tag>{{ i.message }}
        </div>
        <a-space class="issue-actions">
          <a-button v-if="canEditAddress && can('ecom:order:edit')" size="small" @click="emit('address', check.issues)">{{ $t('ecomOrd.addr.edit') }}</a-button>
          <a-button v-if="can('ecom:order:audit')" size="small" :loading="rechecking" @click="recheck">{{ $t('ecomOrd.check.recheck') }}</a-button>
        </a-space>
      </template>
    </a-alert>

    <!-- 利润预估 -->
    <div class="section-title">
      {{ $t('ecomOrd.estimate.title') }}
      <a-tooltip :title="$t('ecomOrd.estimate.help')"><QuestionCircleOutlined class="help" /></a-tooltip>
    </div>
    <a-descriptions :column="{ xs: 1, sm: 2, lg: 4 }" size="small" class="block">
      <a-descriptions-item :label="$t('ecomOrd.estimate.channel')">{{ detail.estChannelName || '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.weight')">{{ detail.estWeight != null ? `${Number(detail.estWeight)} kg` : '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.shipping')">{{ detail.estShipping != null ? baseMoney(detail.estShipping) : $t('ecomOrd.estimate.noRate') }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.fee')">{{ detail.estFee != null ? baseMoney(detail.estFee) : '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.cost')">{{ detail.estCost != null ? baseMoney(detail.estCost) : '-' }}</a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.profit')">
        <b v-if="detail.estProfit != null" :class="{ negative: Number(detail.estProfit) < 0 }">{{ baseMoney(detail.estProfit) }}</b><span v-else>-</span>
      </a-descriptions-item>
      <a-descriptions-item :label="$t('ecomOrd.estimate.rate')">
        <span v-if="detail.estProfitRate != null" :class="{ negative: Number(detail.estProfitRate) < 0 }">{{ Number(detail.estProfitRate) }}%</span><span v-else>-</span>
      </a-descriptions-item>
    </a-descriptions>

    <!-- 补发与售后成本 -->
    <template v-if="detail.orderType !== EcomOrderType.RESEND && resends.resends.length">
      <div class="section-title">{{ $t('ecomOrd.resend.list') }} · {{ $t('ecomOrd.resend.afterSaleCost', { amount: baseMoney(resends.afterSaleCost) }) }}</div>
      <a-table row-key="id" size="small" :columns="resendColumns" :data-source="resends.resends" :pagination="false" :scroll="{ x: 700 }" class="block">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'no'"><a @click="emit('open-order', record.id)">{{ record.platformOrderNo }}</a></template>
          <template v-else-if="column.key === 'localStatus'"><DictTag type="ecom_local_status" :value="record.localStatus" /></template>
          <template v-else-if="column.key === 'cost'">{{ baseMoney(Number(record.costAmount || 0) + Number(record.shippingCost || 0)) }}</template>
        </template>
      </a-table>
    </template>

    <!-- 物流轨迹 -->
    <template v-if="tracks.length">
      <div class="section-title">{{ $t('ecomOrd.track.title') }}</div>
      <a-collapse class="block" :default-active-key="tracks.map((x) => x.pkg.id)">
        <a-collapse-panel v-for="tr in tracks" :key="tr.pkg.id" :header="`${tr.pkg.packageNo} · ${tr.pkg.trackingNo || '-'}`">
          <TrackTimeline :pkg="tr.pkg" :events="tr.events">
            <template #extra>
              <a v-if="can('ecom:track:sync')" @click="pull(tr.pkg.id)">{{ $t('ecomOrd.track.pull') }}</a>
              <a v-if="tr.pkg.shipConfirm === 0 && can('ecom:package:ship')" @click="reconfirm(tr.pkg.id)">{{ $t('ecomOrd.track.reconfirm') }}</a>
              <span v-if="tr.pkg.shipConfirmMsg" class="sub" :title="tr.pkg.shipConfirmMsg">{{ tr.pkg.shipConfirmMsg }}</span>
            </template>
          </TrackTimeline>
        </a-collapse-panel>
      </a-collapse>
    </template>

    <!-- 内部备注 -->
    <div class="section-title">{{ $t('ecomOrd.note.title') }}</div>
    <div v-if="can('ecom:order:tag')" class="note-input">
      <a-input v-model:value="note" :maxlength="500" :placeholder="$t('ecomOrd.note.placeholder')" @press-enter="addNote" />
      <a-button type="primary" :loading="adding" :disabled="!note.trim()" @click="addNote">{{ $t('ecomOrd.note.add') }}</a-button>
    </div>
    <a-list v-if="notes.length" size="small" :data-source="notes" class="block">
      <template #renderItem="{ item }">
        <a-list-item>
          <div class="note">
            <div>{{ item.content }}</div>
            <div class="sub">{{ item.createByName || '-' }} · {{ item.createTime }}</div>
          </div>
        </a-list-item>
      </template>
    </a-list>
    <div v-else class="sub block">{{ $t('ecomOrd.note.empty') }}</div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TrackTimeline from '../order-track/TrackTimeline.vue'
import {
  addOrderNote,
  confirmPackageShipment,
  getOrderCheck,
  getOrderResends,
  getOrderTracks,
  listOrderNotes,
  pullPackageTracks,
  recheckOrder
} from '@/api/ecom/orderEnhance'
import { useUserStore } from '@/store/user'
import { EcomLocalStatus, EcomOrderType } from '@/constants/enums'
import { baseMoney } from './order'

/**
 * 订单详情中的订单增强信息：地址校验 / 黑名单结果（修改地址、重新校验）、利润预估、补发单与售后成本、各包裹的物流轨迹（拉取、按包裹重新回传）、
 * 内部备注历史。detail 变化时重新读取；操作后触发 changed，修改地址触发 address(issues)，点补发单号触发 open-order(id)
 */
const props = defineProps({ detail: { type: Object, required: true } })
const emit = defineEmits(['changed', 'address', 'open-order'])
const { t } = useI18n()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)

const check = ref()
const notes = ref([])
const resends = ref({ resends: [], afterSaleCost: 0 })
const tracks = ref([])
const note = ref('')
const adding = ref(false)
const rechecking = ref(false)

const canEditAddress = computed(() => ![EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED, EcomLocalStatus.PARTIAL, EcomLocalStatus.LABELED].includes(props.detail.localStatus))
const resendColumns = [
  { title: t('ecomOrder.common.platformOrderNo'), key: 'no', width: 180 },
  { title: t('ecomOrder.order.orderTime'), dataIndex: 'orderTime', width: 160 },
  { title: t('ecomOrder.order.localStatus'), key: 'localStatus', width: 100 },
  { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo', width: 160 },
  { title: t('ecomOrd.resend.cost'), key: 'cost', width: 120, align: 'right' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true }
]

async function load() {
  const id = props.detail.id
  const [c, n, r, tr] = await Promise.all([getOrderCheck(id), listOrderNotes(id), getOrderResends(id), getOrderTracks(id)])
  if (id !== props.detail.id) return
  check.value = c
  notes.value = n || []
  resends.value = r || { resends: [], afterSaleCost: 0 }
  tracks.value = tr || []
}
watch(() => [props.detail.id, props.detail.updateTime], load, { immediate: true })

async function addNote() {
  if (!note.value.trim()) return
  adding.value = true
  try {
    await addOrderNote(props.detail.id, note.value.trim())
    note.value = ''
    notes.value = (await listOrderNotes(props.detail.id)) || []
    emit('changed')
  } finally {
    adding.value = false
  }
}

async function recheck() {
  rechecking.value = true
  try {
    check.value = await recheckOrder(props.detail.id)
    message.success(t('ecomOrd.check.rechecked'))
    emit('changed')
  } finally {
    rechecking.value = false
  }
}

async function pull(packageId) {
  await pullPackageTracks(packageId)
  message.success(t('ecomOrd.track.pulled'))
  tracks.value = (await getOrderTracks(props.detail.id)) || []
}

async function reconfirm(packageId) {
  await confirmPackageShipment(packageId)
  message.success(t('ecomOrd.track.reconfirmed'))
  tracks.value = (await getOrderTracks(props.detail.id)) || []
}

defineExpose({ reload: load })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
.section-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
.help {
  margin-left: 4px;
  color: var(--app-text-3);
  cursor: help;
}
.issue {
  margin-bottom: 4px;
}
.issue-actions {
  margin-top: 6px;
}
.negative {
  color: var(--ant-color-error, #ff4d4f);
}
.sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.note-input {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.note {
  width: 100%;
}
</style>
