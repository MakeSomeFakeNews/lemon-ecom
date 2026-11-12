<template>
  <FormDrawer page :drawer="drawer" :create-title="$t('ecomOrder.rule.add')" :edit-title="$t('ecomOrder.rule.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <div class="section-title">{{ $t('ecomOrder.rule.sectionBasic') }}</div>
      <a-row :gutter="16">
        <a-col :xs="24" :sm="16" :lg="8">
          <a-form-item :label="$t('ecomOrder.rule.name')" name="name">
            <a-input v-model:value="form.name" :maxlength="64" :placeholder="$t('ecomOrder.rule.namePh')" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8" :lg="4">
          <a-form-item :label="$t('ecomOrder.rule.priority')" name="priority" :extra="$t('ecomOrder.rule.priorityHint')">
            <a-input-number v-model:value="form.priority" :min="0" :max="9999" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :lg="12">
          <a-form-item :label="$t('common.field.remark')" name="remark">
            <a-input v-model:value="form.remark" :maxlength="255" />
          </a-form-item>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomOrder.rule.conditions') }}</div>
      <a-row :gutter="[16, 16]" class="groups">
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrder.rule.groupScope') }}</div>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item :label="$t('ecomOrder.rule.stores')" name="storeIds">
                  <a-select v-model:value="form.storeIds" mode="multiple" allow-clear :options="storeOptions" option-filter-prop="label" :placeholder="$t('ecomOrder.rule.any')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomOrder.rule.platforms')" name="platforms">
                  <a-select v-model:value="form.platforms" mode="multiple" allow-clear :options="toOptions(dict.ecom_platform)" :placeholder="$t('ecomOrder.rule.any')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomOrder.rule.fulfillment')" name="fulfillment">
                  <a-select v-model:value="form.fulfillment" allow-clear :options="toOptions(dict.ecom_fulfillment)" :placeholder="$t('ecomOrder.rule.any')" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomOrder.rule.countries')" name="countries">
                  <a-select v-model:value="form.countries" mode="tags" :token-separators="[',', ' ']" :open="false" :placeholder="$t('ecomOrder.rule.countriesPh')" @change="upperCountries" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>
        </a-col>
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrder.rule.groupProduct') }}</div>
            <a-form-item :label="$t('ecomOrder.rule.skus')" name="skus">
              <a-select v-model:value="form.skus" mode="tags" :token-separators="[',', ' ']" :open="false" :placeholder="$t('ecomOrder.rule.skusPh')" />
            </a-form-item>
            <a-form-item :label="$t('ecomOrder.rule.materials')" name="materials">
              <div class="material-tags">
                <a-tag v-for="m in form.materials" :key="m.id" closable @close="removeMaterial(m.id)">{{ m.label }}</a-tag>
                <MaterialSelect :value="undefined" :placeholder="$t('ecomOrder.rule.addMaterial')" class="material-select" @select="addMaterial" />
              </div>
            </a-form-item>
          </section>
        </a-col>
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrder.rule.groupValue') }}</div>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrder.rule.amount')">
                  <a-space-compact class="full">
                    <a-input-number v-model:value="form.amountMin" :min="0" :precision="2" :placeholder="$t('ecomOrder.rule.min')" class="half" />
                    <a-input-number v-model:value="form.amountMax" :min="0" :precision="2" :placeholder="$t('ecomOrder.rule.max')" class="half" />
                  </a-space-compact>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrder.rule.weightRange')">
                  <a-space-compact class="full">
                    <a-input-number v-model:value="form.weightMin" :min="0" :precision="3" :placeholder="$t('ecomOrder.rule.min')" class="half" />
                    <a-input-number v-model:value="form.weightMax" :min="0" :precision="3" :placeholder="$t('ecomOrder.rule.max')" class="half" />
                  </a-space-compact>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrd.rule.profitRateMax')" name="profitRateMax" :extra="$t('ecomOrd.rule.profitRateHint')">
                  <a-input-number v-model:value="form.profitRateMax" :precision="2" :min="-1000" :max="1000" addon-after="%" class="full" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>
        </a-col>
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrd.rule.checks') }}</div>
            <a-form-item name="checks">
              <a-form-item-rest>
                <div class="flags">
                  <a-checkbox v-model:checked="form.blacklistHit">{{ $t('ecomOrd.rule.blacklistHit') }}</a-checkbox>
                  <a-checkbox v-model:checked="form.addressIssue">{{ $t('ecomOrd.rule.addressIssue') }}</a-checkbox>
                  <a-checkbox v-model:checked="form.remoteArea">{{ $t('ecomOrd.rule.remoteArea') }}</a-checkbox>
                </div>
              </a-form-item-rest>
            </a-form-item>
          </section>
        </a-col>
      </a-row>

      <div class="section-title">{{ $t('ecomOrder.rule.actions') }}</div>
      <a-row :gutter="[16, 16]" class="groups">
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrder.rule.groupAssign') }}</div>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrder.rule.warehouse')" name="warehouseId">
                  <WarehouseSelect v-model:value="form.warehouseId" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrder.rule.channel')" name="channelId">
                  <ChannelSelect v-model:value="form.channelId" :label="form.channelName" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomOrder.rule.cutoffTime')" name="cutoffTime" :extra="$t('ecomOrder.rule.cutoffHint')">
                  <a-time-picker v-model:value="form.cutoffTime" format="HH:mm" value-format="HH:mm" class="full" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>
        </a-col>
        <a-col :xs="24" :xl="12">
          <section class="group">
            <div class="group-title">{{ $t('ecomOrder.rule.groupAudit') }}</div>
            <a-form-item name="flags">
              <a-form-item-rest>
                <div class="flags">
                  <a-checkbox v-model:checked="form.holdOrder" @change="() => form.holdOrder && (form.autoAudit = false)">{{ $t('ecomOrder.rule.holdOrder') }}</a-checkbox>
                  <a-checkbox v-model:checked="form.autoAudit" @change="() => form.autoAudit && (form.holdOrder = false)">{{ $t('ecomOrder.rule.autoAudit') }}</a-checkbox>
                  <a-checkbox v-model:checked="form.stopOnMatch">{{ $t('ecomOrder.rule.stopOnMatch') }}</a-checkbox>
                </div>
              </a-form-item-rest>
            </a-form-item>
            <a-form-item v-if="form.holdOrder" :label="$t('ecomOrder.rule.holdReason')" name="holdReason">
              <a-input v-model:value="form.holdReason" :maxlength="255" :placeholder="$t('ecomOrder.ops.holdReasonPh')" />
            </a-form-item>
            <a-form-item :label="$t('ecomOrd.rule.tags')" name="tagIds" :extra="$t('ecomOrd.rule.tagsHint')">
              <a-select v-model:value="form.tagIds" mode="multiple" allow-clear :options="tagOptions" option-filter-prop="label" />
            </a-form-item>
          </section>
        </a-col>
      </a-row>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import ChannelSelect from '@/components/ecom/ChannelSelect.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import MaterialSelect from '@/components/wms/MaterialSelect.vue'
import WarehouseSelect from '@/components/wms/WarehouseSelect.vue'
import { addOrderRule, getOrderRule, updateOrderRule } from '@/api/ecom/orderRule'
import { orderTagOptions } from '@/api/ecom/orderEnhance'
import { materialsByIds } from '@/api/wms/material'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { toOptions } from '@/utils/dict'

/**
 * 审单规则新增 / 编辑（子页）：条件（店铺、平台、配送方式、收件国家、平台 SKU、物料、金额与重量区间，留空不限）和动作
 * （指定发货仓、物流渠道、截单时间、人工审核或自动审核、命中后停止）。多值条件在后端存为逗号分隔的字符串。
 * openCreate() / openEdit(record) 打开，保存后触发 success
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_fulfillment')
const stores = useStores()
const storeOptions = computed(() => stores.value.map((s) => ({ value: s.id, label: s.name })))
const tagOptions = ref([])

const rules = {
  name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOrder.rule.name') }) }],
  priority: [{ required: true, message: t('common.rule.required', { name: t('ecomOrder.rule.priority') }) }]
}

const split = (s) => (s ? String(s).split(',').filter(Boolean) : [])
const join = (a) => (a && a.length ? a.join(',') : null)
const num = (v) => (v == null ? undefined : Number(v))

function upperCountries(v) {
  form.value.countries = [...new Set((v || []).map((c) => String(c).trim().toUpperCase()).filter(Boolean))]
}
function addMaterial(m) {
  if (m && !form.value.materials.some((x) => x.id === m.id)) form.value.materials.push({ id: m.id, label: `${m.code} ${m.name}` })
}
const removeMaterial = (id) => (form.value.materials = form.value.materials.filter((m) => m.id !== id))

const drawer = useFormDrawer({
  emptyForm: () => ({
    id: undefined,
    name: '',
    priority: 100,
    storeIds: [],
    platforms: [],
    fulfillment: undefined,
    countries: [],
    skus: [],
    materials: [],
    amountMin: undefined,
    amountMax: undefined,
    weightMin: undefined,
    weightMax: undefined,
    warehouseId: undefined,
    channelId: undefined,
    channelName: undefined,
    cutoffTime: undefined,
    holdOrder: false,
    holdReason: '',
    autoAudit: false,
    stopOnMatch: false,
    profitRateMax: undefined,
    blacklistHit: false,
    addressIssue: false,
    remoteArea: false,
    tagIds: [],
    remark: ''
  }),
  beforeOpen: async () => {
    tagOptions.value = ((await orderTagOptions()) || []).map((x) => ({ value: x.id, label: x.name }))
  },
  fetch: async (record) => {
    const d = await getOrderRule(record.id)
    const materialIds = split(d.materialIds).map(Number)
    const found = materialIds.length ? (await materialsByIds(materialIds)) || [] : []
    return {
      ...d,
      channelName: record.channelName,
      storeIds: split(d.storeIds).map(Number),
      platforms: split(d.platforms),
      countries: split(d.countries),
      skus: split(d.skus),
      materials: materialIds.map((id) => {
        const m = found.find((x) => x.id === id)
        return { id, label: m ? `${m.code} ${m.name}` : String(id) }
      }),
      amountMin: num(d.amountMin),
      amountMax: num(d.amountMax),
      weightMin: num(d.weightMin),
      weightMax: num(d.weightMax),
      holdOrder: d.holdOrder === 1,
      autoAudit: d.autoAudit === 1,
      stopOnMatch: d.stopOnMatch === 1,
      cutoffTime: d.cutoffTime || undefined,
      holdReason: d.holdReason || '',
      profitRateMax: num(d.profitRateMax),
      blacklistHit: d.blacklistHit === 1,
      addressIssue: d.addressIssue === 1,
      remoteArea: d.remoteArea === 1,
      tagIds: split(d.tagIds).map(Number),
      remark: d.remark || ''
    }
  },
  create: addOrderRule,
  update: updateOrderRule,
  // 没有设置动作、同时要求人工审核和自动审核时由后端提示
  toPayload: (f) => {
    return {
      name: f.name.trim(),
      priority: f.priority,
      storeIds: join(f.storeIds),
      platforms: join(f.platforms),
      fulfillment: f.fulfillment ?? null,
      countries: join(f.countries),
      skus: join(f.skus),
      materialIds: join(f.materials.map((m) => m.id)),
      amountMin: f.amountMin ?? null,
      amountMax: f.amountMax ?? null,
      weightMin: f.weightMin ?? null,
      weightMax: f.weightMax ?? null,
      warehouseId: f.warehouseId ?? null,
      channelId: f.channelId ?? null,
      cutoffTime: f.cutoffTime || null,
      holdOrder: f.holdOrder ? 1 : 0,
      holdReason: f.holdOrder ? f.holdReason || null : null,
      autoAudit: f.autoAudit ? 1 : 0,
      stopOnMatch: f.stopOnMatch ? 1 : 0,
      profitRateMax: f.profitRateMax ?? null,
      blacklistHit: f.blacklistHit ? 1 : 0,
      addressIssue: f.addressIssue ? 1 : 0,
      remoteArea: f.remoteArea ? 1 : 0,
      tagIds: join(f.tagIds),
      remark: f.remark || null
    }
  },
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer

defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.half {
  width: 50%;
}
.section-title {
  margin: 4px 0 12px;
  font-weight: 600;
}
.groups {
  margin-bottom: 20px;
}
.group {
  height: 100%;
  padding: 14px 16px 0;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.group-title {
  margin-bottom: 12px;
  color: var(--app-text-2);
  font-weight: 500;
}
.flags {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 4px;
}
.material-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.material-select {
  min-width: 260px;
  max-width: 100%;
}
</style>
