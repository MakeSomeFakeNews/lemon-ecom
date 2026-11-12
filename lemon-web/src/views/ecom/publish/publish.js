import { ref } from 'vue'
import { publishSettings } from '@/api/ecom/publish'
import { PublishStatus } from '@/constants/enums'

/** 可以修改内容的状态（与后端 EcomPublishDraftService.EDITABLE 一致） */
export const EDITABLE = [PublishStatus.DRAFT, PublishStatus.FAILED, PublishStatus.OFFLINE]

/** 键值对象 ↔ 行数组（属性、其他参数、变体属性的编辑） */
export const toRows = (obj) => Object.entries(obj || {}).map(([key, value]) => ({ key, value }))
export const toObject = (rows) =>
  Object.fromEntries((rows || []).filter((r) => r.key && String(r.key).trim() && r.value != null && String(r.value).trim()).map((r) => [String(r.key).trim(), String(r.value).trim()]))

/** 变体属性 { Color: Red, Size: XL } ↔ 文字 "Color: Red; Size: XL" */
export const attrsText = (obj) =>
  Object.entries(obj || {})
    .map(([k, v]) => `${k}: ${v}`)
    .join('; ')
export function parseAttrs(text) {
  const out = {}
  for (const part of String(text || '').split(/[;；]/)) {
    const i = part.search(/[:：]/)
    if (i > 0) {
      const k = part.slice(0, i).trim()
      const v = part.slice(i + 1).trim()
      if (k && v) out[k] = v
    }
  }
  return out
}

/** 店铺刊登内容表单 → 保存的数据（刊登工作台） */
export function itemPayload(f) {
  return {
    title: f.title,
    bullets: f.bullets.filter((b) => b && b.trim()),
    description: f.description || null,
    images: f.images,
    brand: f.brand || null,
    barcodeType: f.barcodeType || null,
    upcExempt: f.upcExempt ? 1 : 0,
    exemptionNote: f.exemptionNote || null,
    categoryId: f.categoryId || null,
    categoryName: f.categoryName || null,
    attributes: f.attributes,
    publishTemplateId: f.publishTemplateId || null,
    priceTemplateId: f.priceTemplateId || null,
    fulfillment: f.fulfillment || null,
    shippingTemplate: f.shippingTemplate || null,
    returnPolicy: f.returnPolicy || null,
    handlingDays: f.handlingDays ?? null,
    extra: f.extra,
    parentSku: f.parentSku || null,
    variants: f.variants.map((v) => ({
      materialId: v.materialId,
      sellerSku: v.sellerSku,
      variantText: v.variantText || Object.values(parseAttrs(v.attrsText)).join(' / ') || null,
      variantAttrs: parseAttrs(v.attrsText),
      barcode: v.barcode || null,
      price: v.price,
      qty: v.qty ?? 0
    }))
  }
}

// ---------------------------------------------------------------- 刊登设置（后端外网地址）


const settings = ref(null)
let pending = null
/** 刊登设置（全局共享，只请求一次）：{ publicUrlConfigured, publicUrl } */
export function usePublishSettings() {
  if (!settings.value && !pending) {
    pending = publishSettings()
      .then((s) => (settings.value = s))
      .catch(() => {})
      .finally(() => (pending = null))
  }
  return settings
}

/** 图片中有系统内的相对地址（/api/files/...），需要配置外网地址才能被平台访问 */
export const hasRelativeImages = (images) => (images || []).some((u) => u && !/^https?:\/\//.test(u.trim()))
