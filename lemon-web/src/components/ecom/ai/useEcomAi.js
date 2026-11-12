import { ref } from 'vue'
import { ecomAiStatus } from '@/api/ecom/aiText'

/**
 * AI 写作的可用状态（整个页面会话内共享，只请求一次；配置变化后调用 refresh）。
 * available 为 false 时按钮禁用并提示 reason，有「AI 设置」权限的用户可以直接跳转去开启
 */
const status = ref({ available: false, reason: '', loaded: false })
let pending = null

export function useEcomAi() {
  async function refresh() {
    pending = ecomAiStatus()
      .then((s) => {
        status.value = { available: !!s?.available, reason: s?.reason || '', loaded: true }
      })
      .catch(() => {
        status.value = { available: false, reason: '', loaded: true }
      })
    return pending
  }
  if (!status.value.loaded && !pending) refresh()
  return { status, refresh }
}
