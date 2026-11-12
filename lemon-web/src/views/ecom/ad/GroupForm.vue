<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomAds.group.add')" :edit-title="$t('ecomAds.group.edit')" width="min(480px, 100vw)">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomAds.group.name')" name="name">
        <a-input v-model:value="form.name" :maxlength="128" />
      </a-form-item>
      <a-form-item :label="$t('ecomAds.common.platformId')" name="platformAdGroupId">
        <a-input v-model:value="form.platformAdGroupId" :maxlength="64" />
      </a-form-item>
      <a-form-item v-if="!form.id" :label="$t('ecomAds.group.defaultBid')" name="defaultBid">
        <a-input-number v-model:value="form.defaultBid" :min="0" :precision="2" class="full" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import FormDrawer from '@/components/FormDrawer.vue'
import { addGroup, updateGroup } from '@/api/ecom/ads'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { t } from '@/locales'

/** 广告组新增 / 编辑：默认竞价只在新建时填写，之后用「修改竞价」同步到平台 */
const emit = defineEmits(['success'])
const rules = { name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomAds.group.name') }) }] }
const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, campaignId: undefined, name: '', platformAdGroupId: '', defaultBid: undefined }),
  create: addGroup,
  update: updateGroup,
  toPayload: (f) => ({ campaignId: f.campaignId, name: f.name.trim(), platformAdGroupId: f.platformAdGroupId || null, defaultBid: f.defaultBid ?? null }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
