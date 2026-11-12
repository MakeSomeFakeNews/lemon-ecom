<template>
  <div class="images">
    <a-alert v-if="needPublicUrl" type="warning" show-icon class="url-alert" :message="$t('ecomPublish.image.publicUrlMissing')"
             :description="$t('ecomPublish.image.publicUrlHow')" />
    <div class="image-list">
      <div v-for="(url, i) in value" :key="url + i" class="image-item" :class="{ main: i === 0 }">
        <img :src="url" alt="" />
        <span v-if="i === 0" class="main-tag">{{ $t('ecomPublish.image.main') }}</span>
        <div v-if="!disabled" class="image-ops">
          <a-button size="small" type="text" :disabled="i === 0" @click="move(i, -1)"><template #icon><LeftOutlined /></template></a-button>
          <a-button size="small" type="text" danger @click="remove(i)"><template #icon><DeleteOutlined /></template></a-button>
          <a-button size="small" type="text" :disabled="i === value.length - 1" @click="move(i, 1)"><template #icon><RightOutlined /></template></a-button>
        </div>
      </div>
      <div v-if="!value.length" class="empty sub-text">{{ $t('ecomPublish.image.empty') }}</div>
    </div>
    <div v-if="!disabled" class="image-actions">
      <a-button size="small" :disabled="!materialId || value.length >= max" @click="openPicker"><template #icon><PictureOutlined /></template>{{ $t('ecomPublish.image.fromMaterial') }}</a-button>
      <a-input-search v-model:value="url" size="small" class="url-input" :placeholder="$t('ecomPublish.image.urlPh')" :enter-button="$t('ecomPublish.image.addUrl')"
                      :disabled="value.length >= max" @search="addUrl" />
      <span class="sub-text">{{ $t('ecomPublish.image.tip', { max }) }}</span>
    </div>
    <a-modal v-model:open="pickerOpen" :title="$t('ecomPublish.image.pickTitle')" width="min(720px, 100vw)" @ok="addPicked">
      <a-spin :spinning="loading">
        <div v-if="!candidates.length" class="sub-text">{{ $t('ecomPublish.image.noMaterialImages') }}</div>
        <a-checkbox-group v-model:value="picked" class="picker">
          <label v-for="c in candidates" :key="c" class="pick-item">
            <img :src="c" alt="" />
            <a-checkbox :value="c" :disabled="value.includes(c)" />
          </label>
        </a-checkbox-group>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, LeftOutlined, PictureOutlined, RightOutlined } from '@ant-design/icons-vue'
import { materialPublishImages } from '@/api/ecom/publish'
import { t } from '@/locales'
import { hasRelativeImages, usePublishSettings } from './publish'

/**
 * 刊登图片：第一张为主图，可左右移动排序、删除；从物料图片（含 SPU 下各 SKU 的主图）中挑选，或填写图片地址。
 * v-model:value 为 URL 数组
 */
const props = defineProps({ value: { type: Array, default: () => [] }, materialId: Number, max: { type: Number, default: 12 }, disabled: Boolean })
const emit = defineEmits(['update:value'])
const url = ref('')
const settings = usePublishSettings()
/** 有系统内图片但没有配置后端外网地址：平台访问不到 */
const needPublicUrl = computed(() => settings.value && !settings.value.publicUrlConfigured && hasRelativeImages(props.value))

function move(i, d) {
  const list = [...props.value]
  ;[list[i], list[i + d]] = [list[i + d], list[i]]
  emit('update:value', list)
}
function remove(i) {
  emit('update:value', props.value.filter((_, j) => j !== i))
}
function addUrl() {
  const u = url.value.trim()
  if (!u) return
  if (!/^https?:\/\//.test(u) && !u.startsWith('/')) {
    message.warning(t('ecomPublish.image.urlInvalid'))
    return
  }
  if (!props.value.includes(u)) emit('update:value', [...props.value, u].slice(0, props.max))
  url.value = ''
}

const pickerOpen = ref(false)
const loading = ref(false)
const candidates = ref([])
const picked = ref([])
async function openPicker() {
  pickerOpen.value = true
  picked.value = []
  loading.value = true
  try {
    candidates.value = (await materialPublishImages(props.materialId)) || []
  } finally {
    loading.value = false
  }
}
function addPicked() {
  emit('update:value', [...props.value, ...picked.value.filter((p) => !props.value.includes(p))].slice(0, props.max))
  pickerOpen.value = false
}
</script>

<style scoped>
.url-alert {
  margin-bottom: 8px;
}
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.image-item {
  position: relative;
  width: 104px;
  height: 104px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  overflow: hidden;
}
.image-item.main {
  border-color: #1677ff;
}
.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.main-tag {
  position: absolute;
  top: 0;
  left: 0;
  background: #1677ff;
  color: #fff;
  font-size: 12px;
  padding: 0 6px;
  border-bottom-right-radius: 6px;
}
.image-ops {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.85);
}
.image-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.url-input {
  width: 360px;
  max-width: 100%;
}
.picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pick-item {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
}
.pick-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pick-item :deep(.ant-checkbox-wrapper) {
  position: absolute;
  top: 4px;
  right: 4px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>

<style>
html[data-theme='dark'] .image-ops {
  background: rgba(0, 0, 0, 0.6);
}
</style>
