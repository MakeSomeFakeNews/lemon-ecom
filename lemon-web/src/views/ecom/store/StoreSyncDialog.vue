<template>
  <a-modal
    :open="open"
    :title="title"
    :confirm-loading="running"
    :ok-text="okText"
    destroy-on-close
    @ok="run"
    @cancel="open = false"
  >
    <a-form layout="vertical">
      <template v-if="mode === 'orders'">
        <a-form-item :label="$t('ecom.hub.since')" :extra="$t('ecom.hub.sinceExtra')">
          <a-date-picker v-model:value="since" show-time class="full" :placeholder="$t('ecom.hub.sincePh')" :disabled-date="future" />
        </a-form-item>
        <div v-if="store?.lastSyncTime" class="sub-text">{{ $t('ecom.hub.lastSync') }}：{{ store.lastSyncTime }}</div>
      </template>
      <template v-else>
        <a-alert type="info" show-icon :message="$t('ecom.hub.pushTip')" class="tip" />
        <a-checkbox v-model:checked="all">{{ $t('ecom.hub.pushAll') }}</a-checkbox>
      </template>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { Modal, message } from 'ant-design-vue'
import { pushStoreInventory, syncStore } from '@/api/ecom/store'

/**
 * 店铺手工同步：open(store, 'orders') 同步订单（可选起始时间），open(store, 'inventory') 回传库存（可选全部回传）。
 * 完成后提示结果（结果带 message 表示失败），触发 done
 */
const emit = defineEmits(['done'])
const { t } = useI18n()

const open = ref(false)
const running = ref(false)
const mode = ref('orders')
const store = ref()
const since = ref()
const all = ref(false)

const title = computed(() =>
  mode.value === 'orders' ? t('ecom.hub.syncOrdersTitle', { name: store.value?.name }) : t('ecom.hub.pushTitle', { name: store.value?.name })
)
const okText = computed(() => (mode.value === 'orders' ? t('ecom.hub.syncOrders') : t('ecom.hub.pushInventory')))
const future = (d) => d && d.isAfter(dayjs())

function show(record, m) {
  store.value = record
  mode.value = m
  since.value = undefined
  all.value = false
  open.value = true
}

async function run() {
  const s = store.value
  running.value = true
  try {
    if (mode.value === 'orders') {
      const r = await syncStore(s.id, since.value ? since.value.format('YYYY-MM-DDTHH:mm:ss') : undefined)
      if (r.message) {
        Modal.warning({ title: t('ecom.store.syncFailed', { name: s.name }), content: r.message })
      } else {
        message.success(t('ecom.store.syncDone', { name: s.name, fetched: r.fetched, created: r.created, updated: r.updated }))
      }
    } else {
      const r = await pushStoreInventory(s.id, all.value)
      if (r.message) {
        Modal.warning({ title: t('ecom.hub.actionFailed', { name: s.name, action: t('ecom.hub.pushInventory') }), content: r.message })
      } else {
        message.success(t('ecom.hub.pushDone', { name: s.name, fetched: r.fetched, updated: r.updated }))
      }
    }
    open.value = false
    emit('done')
  } finally {
    running.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.full {
  width: 100%;
}
.tip {
  margin-bottom: 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
