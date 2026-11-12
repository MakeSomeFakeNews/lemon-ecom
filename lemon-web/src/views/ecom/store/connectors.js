import { computed, ref } from 'vue'
import { storeConnectors } from '@/api/ecom/store'

/** 已有渠道接口的平台（GET /ecom/store/connectors，授权字段名称已按当前语言解析），店铺列表与表单共用，只请求一次 */
const list = ref([])
let pending = null

export function useConnectors() {
  if (!pending) {
    pending = storeConnectors()
      .then((res) => (list.value = res || []))
      .catch(() => {
        pending = null
      })
  }
  const byPlatform = computed(() => Object.fromEntries(list.value.map((c) => [c.platform, c])))
  return { connectors: list, byPlatform }
}

/** 平台接口是否支持某个能力（ConnectorCapability） */
export const supports = (connector, capability) => !!connector && (connector.capabilities || []).includes(capability)
