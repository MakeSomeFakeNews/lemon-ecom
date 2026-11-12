import { ref, watch } from 'vue'
import { owhByPackages } from '@/api/ecom/owh'
import { useUserStore } from '@/store/user'

/**
 * 包裹列表的海外仓出库状态：列表变化后按包裹 ID 批量查询，返回 packageId → { outboundId, outboundNo, status, owhOrderNo, trackingNo, errorMsg, accountName }
 */
export function useOwhPackages(list) {
  const userStore = useUserStore()
  const owhMap = ref({})
  watch(
    list,
    async (rows) => {
      const ids = (rows || []).map((r) => r.id)
      if (!ids.length || !(userStore.hasPerm('ecom:package:list') || userStore.hasPerm('ecom:owhOutbound:list'))) {
        owhMap.value = {}
        return
      }
      const res = (await owhByPackages(ids)) || []
      owhMap.value = Object.fromEntries(res.map((r) => [r.packageId, r]))
    },
    { immediate: true }
  )
  return owhMap
}
