<template>
  <a-modal v-model:open="open" :title="editId ? $t('ecomDev.offer.edit') : $t('ecomDev.offer.link')" :confirm-loading="saving" width="760px" @ok="save">
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomDev.offer.material')" required>
            <MaterialSelect v-model:value="form.materialId" :label="form.materialLabel" :disabled="!!editId" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('ecomDev.offer.supplier')" :extra="$t('ecomDev.offer.supplierExtra')">
            <PartnerSelect v-model:value="form.supplierId" type="supplier" allow-clear :label="form.supplierLabel" />
          </a-form-item>
        </a-col>
        <template v-if="!editId">
          <a-col :span="24">
            <a-form-item :label="$t('ecomDev.offer.url')" required :extra="parsedText">
              <a-input-search v-model:value="form.offer" :placeholder="$t('ecomDev.offer.urlPh')" :enter-button="hasAccount ? $t('ecomDev.offer.query') : $t('ecomDev.offer.parse')"
                              :loading="querying" @search="query" />
            </a-form-item>
          </a-col>
          <a-col v-if="product" :span="24">
            <div class="product">
              <img v-if="product.imageUrl" :src="product.imageUrl" class="thumb" alt="" />
              <div class="info">
                <div class="title">{{ product.title }}</div>
                <div class="sub-text">
                  {{ product.sellerName || product.sellerLoginId }} · {{ $t('ecomDev.offer.moqText', { n: product.minOrderQty ?? '-', unit: product.unit || '' }) }}
                  · {{ $t('ecomDev.offer.stockText', { n: product.stock ?? '-' }) }}
                </div>
                <div v-if="product.priceRanges?.length" class="sub-text">
                  {{ product.priceRanges.map((r) => `≥${r.startQuantity}: ¥${r.price}`).join('  ') }}
                </div>
              </div>
            </div>
            <a-form-item v-if="product.skus?.length" :label="$t('ecomDev.offer.chooseSpec')">
              <a-radio-group v-model:value="form.specId" class="specs">
                <a-radio v-for="s in product.skus" :key="s.specId" :value="s.specId">
                  {{ s.specText || s.specId }} <span class="sub-text">¥{{ s.price ?? '-' }} · {{ $t('ecomDev.offer.stockText', { n: s.stock ?? '-' }) }}</span>
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <template v-if="!hasAccount">
            <a-col :span="24"><a-alert type="warning" show-icon class="tip" :message="$t('ecomDev.offer.manualTip')" /></a-col>
            <a-col :span="16"><a-form-item :label="$t('ecomDev.offer.title')"><a-input v-model:value="form.title" :maxlength="255" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="specId"><a-input v-model:value="form.specId" :maxlength="64" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.specText')"><a-input v-model:value="form.specText" :maxlength="255" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.seller')"><a-input v-model:value="form.sellerLoginId" :maxlength="64" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.sellerName')"><a-input v-model:value="form.sellerName" :maxlength="128" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.price')"><a-input-number v-model:value="form.price" :min="0" :precision="2" class="full" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.moq')"><a-input-number v-model:value="form.minOrderQty" :min="0" :precision="0" class="full" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item :label="$t('ecomDev.offer.unit')"><a-input v-model:value="form.unit" :maxlength="16" /></a-form-item></a-col>
          </template>
        </template>
        <a-col :span="8">
          <a-form-item :label="$t('ecomDev.offer.qtyRatio')" :extra="$t('ecomDev.offer.qtyRatioExtra')">
            <a-input-number v-model:value="form.qtyRatio" :min="0.0001" :precision="4" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('ecomDev.offer.alertThreshold')" :extra="$t('ecomDev.offer.alertExtra')">
            <a-input-number v-model:value="form.alertThreshold" :min="0" :precision="2" class="full" addon-after="%" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item :label="$t('common.field.remark')"><a-input v-model:value="form.remark" :maxlength="255" /></a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import PartnerSelect from '@/components/scm/PartnerSelect.vue'
import { linkAli1688Offer, listAli1688Account, parseAli1688Offer, previewAli1688Offer, updateAli1688Offer } from '@/api/ecom/dev'

/**
 * 关联 / 修改 1688 货源：粘贴 1688 商品链接解析 offerId（链接中带 specId 时一并识别）；有已授权账号时查询商品并选择规格，
 * 没有时手工填写标题、价格等。修改只改换算、供应商、提醒阈值、备注。ref 调用 open(record?)，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const open = ref(false)
const saving = ref(false)
const querying = ref(false)
const form = ref({})
const product = ref()
const parsed = ref()
const hasAccount = ref(false)
const editId = ref()

const parsedText = computed(() => (parsed.value ? t('ecomDev.offer.parsed', { offerId: parsed.value.offerId, specId: parsed.value.specId || '-' }) : ''))
const num = (v) => (v == null ? undefined : Number(v))

async function show(record) {
  editId.value = record?.id
  product.value = undefined
  parsed.value = undefined
  form.value = record
    ? {
        materialId: record.materialId,
        materialLabel: `${record.materialCode || ''} ${record.materialName || ''}`.trim(),
        supplierId: record.supplierId ?? undefined,
        supplierLabel: record.supplierName,
        offer: record.offerId,
        qtyRatio: num(record.qtyRatio),
        alertThreshold: num(record.alertThreshold),
        remark: record.remark
      }
    : { qtyRatio: 1 }
  open.value = true
  try {
    const accounts = await listAli1688Account()
    hasAccount.value = accounts.some((a) => a.authorized && a.status === 1)
  } catch {
    hasAccount.value = false
  }
}

async function query() {
  if (!form.value.offer?.trim()) return
  querying.value = true
  try {
    parsed.value = await parseAli1688Offer(form.value.offer.trim())
    if (parsed.value.specId) form.value.specId = parsed.value.specId
    if (hasAccount.value) {
      product.value = await previewAli1688Offer(parsed.value.offerId)
      if (!form.value.specId && product.value.skus?.length) form.value.specId = product.value.skus[0].specId
    }
  } finally {
    querying.value = false
  }
}

async function save() {
  const f = form.value
  if (!f.materialId || (!editId.value && !f.offer?.trim())) {
    message.warning(t('ecomDev.offer.required'))
    return
  }
  const payload = {
    materialId: f.materialId,
    offer: (f.offer || '').trim(),
    specId: f.specId || null,
    supplierId: f.supplierId ?? null,
    qtyRatio: f.qtyRatio ?? 1,
    alertThreshold: f.alertThreshold ?? null,
    title: f.title || null,
    specText: f.specText || null,
    sellerLoginId: f.sellerLoginId || null,
    sellerName: f.sellerName || null,
    price: f.price ?? null,
    minOrderQty: f.minOrderQty ?? null,
    unit: f.unit || null,
    remark: f.remark || null
  }
  saving.value = true
  try {
    if (editId.value) await updateAli1688Offer(editId.value, payload)
    else await linkAli1688Offer(payload)
    message.success(t('common.msg.saveSuccess'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
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
.product {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 4px;
}
.title {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.specs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
