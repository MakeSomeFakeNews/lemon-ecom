import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { t } from '@/locales'
import {
  addOrderNote,
  confirmPackageShipment,
  getOrderCheck,
  getOrderResends,
  getOrderTracks,
  listOrderLogs,
  pullPackageTracks,
  recheckOrder
} from '@/api/ecom/orderEnhance'

/**
 * 订单详情的增强信息：地址校验 / 黑名单结果、操作日志（含内部备注）、补发单与售后成本、各包裹的物流轨迹。
 * detail（ref）的 ID 或更新时间变化时重新读取；changed 在备注、重新校验后调用（通知页面刷新列表）
 */
export function useOrderEnhance(detail, changed) {
  const check = ref()
  const logs = ref([])
  const resends = ref({ resends: [], afterSaleCost: 0 })
  const tracks = ref([])
  const adding = ref(false)
  const rechecking = ref(false)

  async function load() {
    const id = detail.value?.id
    if (!id) return
    const [c, n, r, tr] = await Promise.all([getOrderCheck(id), listOrderLogs(id), getOrderResends(id), getOrderTracks(id)])
    if (id !== detail.value?.id) return
    check.value = c
    logs.value = n || []
    resends.value = r || { resends: [], afterSaleCost: 0 }
    tracks.value = tr || []
  }
  watch(() => [detail.value?.id, detail.value?.updateTime], load, { immediate: true })

  async function addNote(content) {
    adding.value = true
    try {
      await addOrderNote(detail.value.id, content)
      logs.value = (await listOrderLogs(detail.value.id)) || []
      changed()
      return true
    } finally {
      adding.value = false
    }
  }

  async function recheck() {
    rechecking.value = true
    try {
      check.value = await recheckOrder(detail.value.id)
      message.success(t('ecomOrd.check.rechecked'))
      changed()
    } finally {
      rechecking.value = false
    }
  }

  async function pull(packageId) {
    await pullPackageTracks(packageId)
    message.success(t('ecomOrd.track.pulled'))
    tracks.value = (await getOrderTracks(detail.value.id)) || []
  }

  async function reconfirm(packageId) {
    await confirmPackageShipment(packageId)
    message.success(t('ecomOrd.track.reconfirmed'))
    tracks.value = (await getOrderTracks(detail.value.id)) || []
  }

  return { check, logs, resends, tracks, adding, rechecking, load, addNote, recheck, pull, reconfirm }
}
