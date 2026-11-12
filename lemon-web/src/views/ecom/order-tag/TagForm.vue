<template>
  <FormDrawer :drawer="drawer" :create-title="$t('ecomOrd.tag.add')" :edit-title="$t('ecomOrd.tag.edit')">
    <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
      <a-form-item :label="$t('ecomOrd.tag.name')" name="name">
        <a-input v-model:value="form.name" :maxlength="32" />
      </a-form-item>
      <a-form-item :label="$t('ecomOrd.tag.color')" name="color">
        <div class="colors">
          <a-tag v-for="c in COLORS" :key="c" :color="c" :bordered="false" class="color" :class="{ active: form.color === c }" @click="form.color = c">
            {{ form.name || $t('ecomOrd.tag.preview') }}
          </a-tag>
        </div>
      </a-form-item>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="$t('ecomOrd.tag.sort')" name="sort">
            <a-input-number v-model:value="form.sort" :min="0" :max="9999" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="$t('common.field.status')" name="status">
            <a-switch v-model:checked="form.status" :checked-value="STATUS_ENABLED" :un-checked-value="STATUS_DISABLED" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item :label="$t('common.field.remark')" name="remark">
        <a-input v-model:value="form.remark" :maxlength="255" />
      </a-form-item>
    </a-form>
  </FormDrawer>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import FormDrawer from '@/components/FormDrawer.vue'
import { addOrderTag, updateOrderTag } from '@/api/ecom/orderEnhance'
import { useFormDrawer } from '@/composables/useFormDrawer'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

/** 订单标签新增 / 编辑：名称、颜色（预设色）、排序、状态。openCreate() / openEdit(record)，保存后触发 success */
const emit = defineEmits(['success'])
const { t } = useI18n()
const COLORS = ['red', 'volcano', 'orange', 'gold', 'lime', 'green', 'cyan', 'blue', 'geekblue', 'purple', 'magenta']
const rules = { name: [{ required: true, whitespace: true, message: t('common.rule.required', { name: t('ecomOrd.tag.name') }) }] }

const drawer = useFormDrawer({
  emptyForm: () => ({ id: undefined, name: '', color: 'blue', sort: 0, status: STATUS_ENABLED, remark: '' }),
  create: addOrderTag,
  update: updateOrderTag,
  toPayload: (f) => ({ name: f.name.trim(), color: f.color, sort: f.sort ?? 0, status: f.status, remark: f.remark || null }),
  onSuccess: () => emit('success')
})
const { formRef, form } = drawer
defineExpose({ openCreate: drawer.openCreate, openEdit: drawer.openEdit })
</script>

<style scoped>
.full {
  width: 100%;
}
.colors {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.color {
  cursor: pointer;
  outline: 2px solid transparent;
}
.color.active {
  outline-color: var(--ant-color-primary, #1677ff);
}
</style>
