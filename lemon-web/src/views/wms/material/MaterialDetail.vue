<template>
  <SubPage :open="detailOpen" :title="detail ? `${detail.code} ${detail.name}` : $t('wms.common.material')" width="min(960px, 100vw)" destroy-on-close @close="detailOpen = false">
    <template #extra>
      <a-button v-if="detail && canEdit" type="primary" @click="emit('edit', detail.id)">{{ $t('common.action.edit') }}</a-button>
    </template>
    <a-spin :spinning="detailLoading">
      <template v-if="detail">
        <MaterialImages :material-id="detail.id" :editable="canEdit" :size="88" class="block" @changed="emit('images-changed')" />
        <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="block">
          <a-descriptions-item :label="$t('wms.material.specUnit')">{{ detail.spec || '-' }} / {{ detail.unit || '-' }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.inout.category')"><DictTag type="wms_material_category" :value="detail.category" /></a-descriptions-item>
          <a-descriptions-item v-if="detail.barcode" :label="$t('wms.units.barcode')">{{ detail.barcode }}</a-descriptions-item>
          <a-descriptions-item :label="$t('wms.stock.safetyStock')">{{ detail.safetyStock != null ? formatQty(detail.safetyStock) + ' ' + (detail.unit || '') : '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import MaterialImages from '@/components/wms/MaterialImages.vue'
import { getMaterial } from '@/api/wms/material'
import { formatQty } from '@/components/wms/wms'
import { useUserStore } from '@/store/user'

/** 物料详情（子页）：图片、规格单位、分类、条码、安全库存。open(id) / reload()；edit(id) 编辑，images-changed 图片有变化 */
const emit = defineEmits(['edit', 'images-changed'])
const userStore = useUserStore()
const canEdit = computed(() => userStore.hasPerm('wms:material:edit'))
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref()

async function open(id) {
  detailOpen.value = true
  detailLoading.value = true
  try {
    detail.value = await getMaterial(id)
  } finally {
    detailLoading.value = false
  }
}
/** 打开着时重新加载（编辑保存后） */
const reload = () => detailOpen.value && detail.value && open(detail.value.id)

defineExpose({ open, reload })
</script>

<style scoped>
.block {
  margin-bottom: 16px;
}
</style>
