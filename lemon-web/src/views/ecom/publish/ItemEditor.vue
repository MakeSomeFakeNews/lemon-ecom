<template>
  <FormDrawer page :drawer="drawer" :edit-title="title" width="100%" :readonly="readonly">
    <div ref="rootRef" class="pub-ws">
      <StoreRail class="pub-rail" :draft="draftInfo" :items="siblings" :current-id="form.id" @select="switchTo" @master="emit('open-draft', form.draftId)" />
      <div class="pub-main">
        <div class="pub-sticky">
          <div class="pub-head" :class="{ 'has-switch': siblings.length > 1 }">
            <a-select v-if="siblings.length > 1" :value="form.id" :options="storeOptions" class="pub-store-switch" @change="switchTo" />
            <span class="pub-store">{{ form.storeName }}</span>
            <DictTag v-if="form.platform" type="ecom_platform" :value="form.platform" />
            <DictTag v-if="form.status" type="ecom_publish_status" :value="form.status" />
            <a v-if="form.platformUrl" :href="form.platformUrl" target="_blank" rel="noopener" class="sub-text">{{ form.platformItemId || $t('ecomPublish.list.open') }}</a>
            <span v-else-if="form.platformItemId" class="sub-text">{{ form.platformItemId }}</span>
            <span class="spacer" />
            <a-button size="small" class="pub-master-btn" @click="emit('open-draft', form.draftId)">{{ $t('ecomPublish.item.masterContent') }}</a-button>
          </div>
          <a-anchor direction="horizontal" :items="anchorItems" :get-container="getContainer" :target-offset="104" class="pub-anchor" @click="(e) => e.preventDefault()" />
        </div>

        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" :disabled="readonly">
          <a-alert v-if="readonly" type="info" show-icon class="block" :message="$t('ecomPublish.item.readonlyTip')" />
          <a-alert v-if="caps?.note" type="info" show-icon class="block" :message="caps.note" />
          <a-alert v-if="form.errorMessage" type="error" show-icon class="block" :message="$t('ecomPublish.item.lastError')" :description="form.errorMessage" />

          <section id="pub-sec-basic" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.secBasic') }}</h3>
            <a-form-item :label="$t('ecomPublish.item.title')" name="title">
              <a-input v-model:value="form.title" :maxlength="255" show-count />
            </a-form-item>
            <a-form-item :label="$t('ecomPublish.item.bullets')">
              <div v-for="(b, i) in form.bullets" :key="i" class="bullet-row">
                <span class="bullet-no">{{ i + 1 }}</span>
                <a-input v-model:value="form.bullets[i]" :maxlength="500" />
                <a-button v-if="!readonly" type="text" danger @click="form.bullets.splice(i, 1)"><template #icon><DeleteOutlined /></template></a-button>
              </div>
              <a-button v-if="!readonly && form.bullets.length < 10" type="dashed" size="small" @click="form.bullets.push('')">
                <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.item.addBullet') }}
              </a-button>
              <span v-else-if="!form.bullets.length" class="sub-text">-</span>
            </a-form-item>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomPublish.item.brand')" name="brand">
                  <a-input v-model:value="form.brand" :maxlength="64" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomPublish.item.barcodeType')" name="barcodeType">
                  <a-select v-model:value="form.barcodeType" allow-clear :options="barcodeOptions" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="8">
                <a-form-item :label="$t('ecomPublish.item.upcExempt')" name="upcExempt">
                  <a-switch v-model:checked="form.upcExempt" :checked-value="1" :un-checked-value="0" />
                </a-form-item>
              </a-col>
              <a-col v-if="form.upcExempt === 1" :span="24">
                <a-form-item :label="$t('ecomPublish.item.exemptionNote')" name="exemptionNote">
                  <a-input v-model:value="form.exemptionNote" :maxlength="255" :placeholder="$t('ecomPublish.item.exemptionNotePh')" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>

          <section id="pub-sec-images" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.images') }}</h3>
            <ImagesEditor v-model:value="form.images" :material-id="form.materialId" :disabled="readonly" />
          </section>

          <section id="pub-sec-variants" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.secVariants') }}</h3>
            <VariantsSection :form="form" :readonly="readonly" :price-templates="priceTemplates" :next-key="nextKey" />
          </section>

          <section id="pub-sec-category" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.categoryAttrs') }}</h3>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomPublish.item.publishTemplate')" :extra="$t('ecomPublish.item.publishTemplateExtra')">
                  <a-select v-model:value="form.publishTemplateId" allow-clear :options="templateOptions" @change="applyTemplate" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item :label="$t('ecomPublish.item.category')" :required="caps?.categoryRequired">
                  <CategoryPicker v-model:id="form.categoryId" v-model:name="form.categoryName" :store-id="form.storeId" :remote="hasCap('CATEGORIES')" :disabled="readonly" />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item :label="$t('ecomPublish.item.attributes')">
                  <AttributesEditor v-model:value="form.attributes" :store-id="form.storeId" :category-id="form.categoryId" :remote="hasCap('ATTRIBUTES')" :disabled="readonly" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>

          <section id="pub-sec-desc" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.description') }}</h3>
            <a-form-item name="description">
              <a-textarea v-model:value="form.description" :maxlength="20000" show-count :auto-size="{ minRows: 6, maxRows: 18 }" />
            </a-form-item>
          </section>

          <section id="pub-sec-logistics" class="pub-sec">
            <h3 class="sec-title">{{ $t('ecomPublish.item.logistics') }}</h3>
            <a-row :gutter="16">
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomPublish.item.fulfillment')" name="fulfillment">
                  <a-select v-model:value="form.fulfillment" allow-clear :options="toOptions(dict.ecom_fulfillment)" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomPublish.item.shippingTemplate')" name="shippingTemplate">
                  <a-input v-model:value="form.shippingTemplate" :maxlength="128" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomPublish.item.returnPolicy')" name="returnPolicy">
                  <a-input v-model:value="form.returnPolicy" :maxlength="128" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12" :lg="6">
                <a-form-item :label="$t('ecomPublish.item.handlingDays')" name="handlingDays">
                  <a-input-number v-model:value="form.handlingDays" :min="0" :max="60" :precision="0" class="full" />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item :label="$t('ecomPublish.item.extra')" :extra="$t('ecomPublish.item.extraTip')">
                  <KeyValueEditor v-model:value="form.extra" :disabled="readonly" />
                </a-form-item>
              </a-col>
            </a-row>
          </section>
        </a-form>
      </div>
    </div>
    <template #actions="{ loading }">
      <template v-if="!readonly">
        <a-button :loading="saving" :disabled="loading" @click="save(false)">{{ $t('common.action.save') }}</a-button>
        <a-button v-if="canPush" type="primary" :loading="saving" :disabled="loading" @click="save(true)">
          <template #icon><CloudUploadOutlined /></template>{{ $t('ecomPublish.item.saveAndPublish') }}
        </a-button>
      </template>
      <!-- 只读时不显示保存（插槽为空会回退到默认的保存按钮） -->
      <span v-else />
    </template>
  </FormDrawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { CloudUploadOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import FormDrawer from '@/components/FormDrawer.vue'
import {
  getPublishDraft,
  getPublishItem,
  getPublishTemplate,
  priceTemplateOptions,
  publishCapabilities,
  publishTemplateOptions,
  submitPublish,
  updatePublishItem
} from '@/api/ecom/publish'
import { useDict } from '@/composables/useDict'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { PublishBarcodeType } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import AttributesEditor from './AttributesEditor.vue'
import CategoryPicker from './CategoryPicker.vue'
import ImagesEditor from './ImagesEditor.vue'
import KeyValueEditor from './KeyValueEditor.vue'
import StoreRail from './StoreRail.vue'
import VariantsSection from './VariantsSection.vue'
import { EDITABLE, attrsText, itemPayload } from './publish'

/**
 * 刊登工作台（子页）：左侧是同一刊登草稿下各店铺的刊登记录（点击切换，窄屏为顶部下拉），右侧按分区编辑当前店铺的内容——
 * 基本信息（标题、五点、品牌、条码 / UPC 豁免）、图片、变体价格库存（按价格模板填入建议价、试算利润率）、平台类目与属性、
 * 描述、配送与其他平台参数，顶部锚点跳转，底部「保存」「保存并发布」。
 * openEdit(record) 编辑（草稿 / 失败 / 已下架，其他状态只读），openView(record) 只读查看；保存后触发 success，open-draft(draftId) 打开主内容
 */
const emit = defineEmits(['success', 'open-draft'])
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('ecom_fulfillment', 'ecom_platform', 'ecom_publish_status')
const readonly = ref(false)
const forceView = ref(false)
const caps = ref(null)
const templates = ref([])
const priceTemplates = ref([])
const draftInfo = ref(null)
const saving = ref(false)
const rootRef = ref()
let seq = 0
const nextKey = () => ++seq

const canPush = computed(() => userStore.hasPerm('ecom:publish:push'))
const barcodeOptions = Object.values(PublishBarcodeType).map((v) => ({ value: v, label: v }))
const rules = { title: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomPublish.item.title') }) }] }
const hasCap = (c) => !!caps.value?.capabilities?.includes(c)
const templateOptions = computed(() => templates.value.map((x) => ({ value: x.id, label: x.name })))
const siblings = computed(() => draftInfo.value?.items || [])
const storeOptions = computed(() => siblings.value.map((s) => ({ value: s.id, label: s.storeName })))

const anchorItems = [
  { key: 'basic', href: '#pub-sec-basic', title: t('ecomPublish.item.secBasic') },
  { key: 'images', href: '#pub-sec-images', title: t('ecomPublish.item.images') },
  { key: 'variants', href: '#pub-sec-variants', title: t('ecomPublish.item.secVariants') },
  { key: 'category', href: '#pub-sec-category', title: t('ecomPublish.item.categoryAttrs') },
  { key: 'desc', href: '#pub-sec-desc', title: t('ecomPublish.item.description') },
  { key: 'logistics', href: '#pub-sec-logistics', title: t('ecomPublish.item.logistics') }
]
/** 锚点的滚动容器：子页的内容区 */
const getContainer = () => rootRef.value?.closest('.sub-page-body') || window

const emptyForm = () => ({ id: undefined, title: '', bullets: [], description: '', images: [], variants: [], attributes: {}, extra: {} })
const drawer = useFormDrawer({
  emptyForm,
  fetch: async (record) => {
    const item = await getPublishItem(record.id)
    const draftId = item.draftId ?? record.draftId
    const [c, ts, ps, d] = await Promise.all([
      publishCapabilities(item.storeId),
      publishTemplateOptions(item.platform),
      priceTemplateOptions(item.platform),
      draftId && draftInfo.value?.id !== draftId ? getPublishDraft(draftId) : Promise.resolve(null)
    ])
    caps.value = c
    templates.value = ts || []
    priceTemplates.value = ps || []
    if (d) draftInfo.value = d
    readonly.value = forceView.value || !EDITABLE.includes(item.status)
    return {
      ...item,
      draftId,
      bullets: item.bullets || [],
      images: item.images || [],
      attributes: item.attributes || {},
      extra: item.extra || {},
      variants: (item.variants || []).map((v) => ({ ...v, key: nextKey(), attrsText: attrsText(v.variantAttrs) }))
    }
  },
  update: updatePublishItem,
  toPayload: itemPayload,
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
const title = computed(() =>
  form.value.storeName ? `${draftInfo.value?.materialName || form.value.materialName || ''} · ${t(readonly.value ? 'ecomPublish.item.viewTitle' : 'ecomPublish.item.editTitle')}` : ''
)

function openEdit(record) {
  forceView.value = false
  draftInfo.value = null
  return drawer.openEdit(record)
}
function openView(record) {
  forceView.value = true
  draftInfo.value = null
  return drawer.openEdit(record)
}
/** 重新加载当前店铺记录和左侧列表（店铺状态可能已变化） */
async function reloadCurrent(id = form.value.id) {
  const draftId = form.value.draftId
  draftInfo.value = null
  await drawer.openEdit({ id, draftId })
}

/** 切换到同一草稿的另一个店铺，有未保存的修改时先确认 */
function switchTo(id) {
  if (id === form.value.id || drawer.loading.value) return
  const go = () => {
    forceView.value = false
    drawer.openEdit({ id, draftId: form.value.draftId })
  }
  if (!drawer.dirty.value || readonly.value) return go()
  Modal.confirm({
    title: t('system.role.discardTitle'),
    content: t('ecomPublish.item.switchConfirm'),
    okText: t('common.action.discard'),
    okType: 'danger',
    cancelText: t('system.role.keepEditing'),
    onOk: go
  })
}

/** 保存（留在工作台，便于继续编辑其他店铺）；publish 时保存后提交发布 */
async function save(publish) {
  if (saving.value) return
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const id = form.value.id
    await updatePublishItem(id, itemPayload(form.value))
    if (publish) {
      const res = await submitPublish([id])
      if (res?.errors?.length) message.error(res.errors.map((e) => e.message).join('; '))
      else message.success(t('ecomPublish.list.submitted', { n: res?.succeeded ?? 1 }))
    } else {
      message.success(t('common.msg.saveSuccess'))
    }
    emit('success')
    await reloadCurrent(id)
  } finally {
    saving.value = false
  }
}

/** 选择刊登模板：带入类目、属性（合并）、运费模板、退货政策、备货天数、配送方式、价格模板、其他参数 */
async function applyTemplate(id) {
  if (!id) return
  const tpl = await getPublishTemplate(id)
  const f = form.value
  if (tpl.categoryId) {
    f.categoryId = tpl.categoryId
    f.categoryName = tpl.categoryName
  }
  f.attributes = { ...(tpl.attributes || {}), ...(f.attributes || {}) }
  f.extra = { ...(tpl.extra || {}), ...(f.extra || {}) }
  f.shippingTemplate = tpl.shippingTemplate || f.shippingTemplate
  f.returnPolicy = tpl.returnPolicy || f.returnPolicy
  f.handlingDays = tpl.handlingDays ?? f.handlingDays
  f.fulfillment = tpl.fulfillment || f.fulfillment
  if (!f.brand && tpl.brand) f.brand = tpl.brand
  if (tpl.priceTemplateId) f.priceTemplateId = tpl.priceTemplateId
}

/** 外部修改了内容（如应用主内容）后重新加载 */
const refresh = () => (drawer.open.value && form.value.id ? reloadCurrent() : undefined)

/** 从主内容抽屉进入某个店铺：工作台已打开时按切换处理（有未保存修改先确认） */
const editItem = (id) => (drawer.open.value ? switchTo(id) : openEdit({ id }))

defineExpose({ openEdit, openView, refresh, editItem })
</script>

<style scoped>
.pub-ws {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.pub-rail {
  position: sticky;
  top: 0;
  flex: none;
  width: 240px;
  max-height: calc(100vh - 240px);
  padding-right: 16px;
  border-right: 1px solid var(--app-border);
}
.pub-main {
  flex: 1;
  min-width: 0;
}
.pub-sticky {
  position: sticky;
  top: -20px;
  z-index: 3;
  margin: -20px 0 16px;
  padding-top: 16px;
  background: var(--app-card);
  border-bottom: 1px solid var(--app-border);
}
.pub-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.pub-head :deep(.ant-tag) {
  margin-inline-end: 0;
}
.pub-store {
  font-size: 15px;
  font-weight: 600;
}
.pub-store-switch {
  display: none;
  width: 200px;
}
.pub-master-btn {
  display: none;
}
.pub-anchor :deep(.ant-anchor) {
  padding-bottom: 0;
}
.pub-anchor :deep(.ant-anchor::before) {
  display: none;
}
.pub-sec {
  padding-bottom: 8px;
  margin-bottom: 16px;
  scroll-margin-top: 104px;
}
/* 最后一个分区留出高度，锚点跳转时能滚到顶部、高亮正确 */
.pub-sec:last-child {
  min-height: calc(100vh - 360px);
}
.pub-sec + .pub-sec {
  padding-top: 16px;
  border-top: 1px dashed var(--app-border);
}
.sec-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 15px;
  font-weight: 600;
  color: var(--app-text);
}
.sec-title::before {
  content: '';
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #1677ff;
}
.block {
  margin-bottom: 12px;
}
.bullet-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.bullet-no {
  width: 20px;
  flex: none;
  color: var(--app-text-3);
  text-align: right;
}
.full {
  width: 100%;
}
.spacer {
  flex: 1;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
@media (max-width: 991px) {
  .pub-rail {
    display: none;
  }
  .pub-store-switch,
  .pub-master-btn {
    display: inline-block;
  }
  .has-switch .pub-store {
    display: none;
  }
}
@media (max-width: 575px) {
  .pub-sticky {
    top: -16px;
    margin-top: -16px;
  }
}
</style>
