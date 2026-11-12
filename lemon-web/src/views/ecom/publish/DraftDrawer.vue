<template>
  <a-drawer :open="open" :title="$t('ecomPublish.draft.title')" width="min(860px, 100vw)" destroy-on-close @close="open = false">
    <a-spin :spinning="loading">
      <template v-if="draft">
        <a-descriptions size="small" :column="2" class="block">
          <a-descriptions-item :label="$t('ecomPublish.create.material')">{{ draft.materialCode }} · {{ draft.materialName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomPublish.draft.type')">{{ draft.spu ? $t('ecomPublish.draft.spu') : $t('ecomPublish.draft.single') }}</a-descriptions-item>
        </a-descriptions>

        <a-divider orientation="left">{{ $t('ecomPublish.draft.stores') }}</a-divider>
        <a-table row-key="id" size="small" :columns="itemColumns" :data-source="draft.items" :pagination="false">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'store'">
              {{ record.storeName }} <DictTag type="ecom_platform" :value="record.platform" />
            </template>
            <template v-else-if="column.key === 'status'"><DictTag type="ecom_publish_status" :value="record.status" /></template>
            <template v-else-if="column.key === 'ops'">
              <a @click="emit('edit-item', record.id)">{{ EDITABLE.includes(record.status) ? $t('common.action.edit') : $t('common.action.view') }}</a>
            </template>
          </template>
        </a-table>
        <div v-if="canAdd" class="add-stores">
          <a-select v-model:value="newStores" mode="multiple" :options="storeOptions" :placeholder="$t('ecomPublish.draft.addStoresPh')" class="store-select" />
          <a-button :disabled="!newStores.length" :loading="adding" @click="addStores">{{ $t('ecomPublish.draft.addStores') }}</a-button>
        </div>

        <a-divider orientation="left">{{ $t('ecomPublish.draft.master') }}</a-divider>
        <a-alert type="info" show-icon class="block" :message="$t('ecomPublish.draft.masterTip')" />
        <a-form layout="vertical" :disabled="!canEdit">
          <a-form-item :label="$t('ecomPublish.item.title')" required>
            <a-input v-model:value="draft.title" :maxlength="255" show-count />
          </a-form-item>
          <a-form-item :label="$t('ecomPublish.item.bullets')">
            <div v-for="(b, i) in draft.bullets" :key="i" class="bullet-row">
              <a-input v-model:value="draft.bullets[i]" :maxlength="500" />
              <a-button type="text" danger @click="draft.bullets.splice(i, 1)"><template #icon><DeleteOutlined /></template></a-button>
            </div>
            <a-button v-if="draft.bullets.length < 10" type="dashed" size="small" @click="draft.bullets.push('')">
              <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.item.addBullet') }}
            </a-button>
          </a-form-item>
          <a-form-item :label="$t('ecomPublish.item.description')">
            <a-textarea v-model:value="draft.description" :maxlength="20000" :auto-size="{ minRows: 3, maxRows: 10 }" />
          </a-form-item>
          <a-form-item :label="$t('ecomPublish.item.images')">
            <ImagesEditor v-model:value="draft.images" :material-id="draft.materialId" :disabled="!canEdit" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ecomPublish.item.brand')"><a-input v-model:value="draft.brand" :maxlength="64" /></a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ecomPublish.item.barcodeType')">
                <a-select v-model:value="draft.barcodeType" allow-clear :options="barcodeOptions" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="8">
              <a-form-item :label="$t('ecomPublish.item.upcExempt')">
                <a-switch v-model:checked="draft.upcExempt" :checked-value="1" :un-checked-value="0" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item v-if="draft.upcExempt === 1" :label="$t('ecomPublish.item.exemptionNote')">
            <a-input v-model:value="draft.exemptionNote" :maxlength="255" />
          </a-form-item>
        </a-form>
        <div v-if="canEdit" class="apply-row">
          <a-checkbox-group v-model:value="fields" :options="fieldOptions" />
        </div>
      </template>
    </a-spin>
    <template #footer>
      <div class="footer">
        <a-button @click="open = false">{{ $t('common.action.close') }}</a-button>
        <a-button v-if="canEdit" :loading="saving" @click="save(false)">{{ $t('ecomPublish.draft.saveMaster') }}</a-button>
        <a-button v-if="canEdit" type="primary" :loading="saving" :disabled="!fields.length" @click="save(true)">{{ $t('ecomPublish.draft.saveAndApply') }}</a-button>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { addPublishStores, applyPublishMaster, getPublishDraft, updatePublishDraft } from '@/api/ecom/publish'
import { PublishBarcodeType } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import ImagesEditor from './ImagesEditor.vue'
import { EDITABLE } from './publish'

/**
 * 刊登草稿：来源物料、各店铺的刊登记录（可继续增加店铺），以及主内容（标题、五点、描述、图片、品牌、条码）——
 * 修改主内容后可勾选字段「保存并应用到店铺」，覆盖草稿 / 失败 / 已下架的店铺记录。open(draftId)；changed 时刷新列表，edit-item(id) 编辑店铺内容
 */
const emit = defineEmits(['changed', 'edit-item'])
const { t } = useI18n()
const userStore = useUserStore()
const stores = useStores()
const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const adding = ref(false)
const draft = ref(null)
const newStores = ref([])
const fields = ref(['title', 'bullets', 'description', 'images', 'brand', 'barcode'])
const fieldOptions = ['title', 'bullets', 'description', 'images', 'brand', 'barcode'].map((f) => ({ value: f, label: t(`ecomPublish.draft.field.${f}`) }))
const barcodeOptions = Object.values(PublishBarcodeType).map((v) => ({ value: v, label: v }))
const canEdit = computed(() => userStore.hasPerm('ecom:publish:edit'))
const canAdd = computed(() => userStore.hasPerm('ecom:publish:add'))

const itemColumns = [
  { title: t('ecomPublish.common.store'), key: 'store', width: 220 },
  { title: t('common.field.status'), key: 'status', width: 100 },
  { title: t('ecomPublish.list.variants'), dataIndex: 'variantCount', width: 80, align: 'right' },
  { title: t('ecomPublish.list.platformItemId'), dataIndex: 'platformItemId', width: 140 },
  { title: '', key: 'ops', width: 70 }
]
const storeOptions = computed(() => {
  const used = new Set((draft.value?.items || []).map((i) => i.storeId))
  return stores.value.filter((s) => !used.has(s.id)).map((s) => ({ value: s.id, label: s.name }))
})

async function load(id) {
  loading.value = true
  try {
    const d = await getPublishDraft(id)
    draft.value = { ...d, bullets: d.bullets || [], images: d.images || [], upcExempt: d.upcExempt || 0 }
  } finally {
    loading.value = false
  }
}
function openDrawer(id) {
  open.value = true
  newStores.value = []
  load(id)
}

async function save(apply) {
  saving.value = true
  try {
    const d = draft.value
    await updatePublishDraft(d.id, {
      materialId: d.materialId,
      title: d.title,
      bullets: d.bullets.filter((b) => b && b.trim()),
      description: d.description || null,
      images: d.images,
      brand: d.brand || null,
      barcodeType: d.barcodeType || null,
      upcExempt: d.upcExempt ? 1 : 0,
      exemptionNote: d.exemptionNote || null,
      remark: d.remark || null
    })
    if (apply) {
      const n = await applyPublishMaster(d.id, fields.value)
      message.success(t('ecomPublish.draft.applied', { n }))
    } else {
      message.success(t('common.msg.saveSuccess'))
    }
    emit('changed')
    await load(d.id)
  } finally {
    saving.value = false
  }
}

async function addStores() {
  adding.value = true
  try {
    const n = await addPublishStores(draft.value.id, { storeIds: newStores.value })
    message.success(t('ecomPublish.draft.storesAdded', { n }))
    newStores.value = []
    emit('changed')
    await load(draft.value.id)
  } finally {
    adding.value = false
  }
}

defineExpose({ open: openDrawer })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.add-stores {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.store-select {
  flex: 1;
}
.bullet-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
.apply-row {
  margin-top: 4px;
}
.footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
