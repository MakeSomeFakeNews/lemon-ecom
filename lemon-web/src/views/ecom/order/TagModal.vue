<template>
  <a-modal v-model:open="open" :title="mode === 'untag' ? $t('ecomOrd.tag.untagTitle', { n: ids.length }) : $t('ecomOrd.tag.tagTitle', { n: ids.length })"
           :confirm-loading="saving" @ok="submit">
    <a-form layout="vertical">
      <a-form-item :label="$t('ecomOrd.tag.tags')">
        <a-select v-model:value="tagIds" mode="multiple" :options="options" option-filter-prop="label" :placeholder="$t('ecomOrd.tag.selectTags')">
          <template #option="{ label, color }">
            <a-tag :color="color" :bordered="false">{{ label }}</a-tag>
          </template>
        </a-select>
      </a-form-item>
    </a-form>
    <a v-if="mode !== 'untag'" v-perm="'ecom:orderTag:list'" @click="router.push('/ecom/order-tag')">{{ $t('ecomOrd.tag.manage') }}</a>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { orderTagOptions, tagOrders, untagOrders } from '@/api/ecom/orderEnhance'

/** 批量打标签 / 去标签：open(ids, 'tag' | 'untag')，完成后触发 done */
const emit = defineEmits(['done'])
const { t } = useI18n()
const router = useRouter()
const open = ref(false)
const saving = ref(false)
const ids = ref([])
const mode = ref('tag')
const tagIds = ref([])
const tags = ref([])
const options = computed(() => tags.value.map((x) => ({ value: x.id, label: x.name, color: x.color })))

async function show(orderIds, m = 'tag') {
  ids.value = [...orderIds]
  mode.value = m
  tagIds.value = []
  open.value = true
  tags.value = (await orderTagOptions()) || []
}

async function submit() {
  if (!tagIds.value.length) {
    message.warning(t('ecomOrd.tag.selectTags'))
    return
  }
  saving.value = true
  try {
    const n = mode.value === 'untag' ? await untagOrders(ids.value, tagIds.value) : await tagOrders(ids.value, tagIds.value)
    message.success(t(mode.value === 'untag' ? 'ecomOrd.tag.untagged' : 'ecomOrd.tag.tagged', { n }))
    open.value = false
    emit('done')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>
