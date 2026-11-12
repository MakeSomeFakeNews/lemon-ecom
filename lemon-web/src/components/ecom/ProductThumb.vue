<template>
  <span class="product-thumb" :style="{ width: `${size}px`, height: `${size}px` }">
    <a-image v-if="src && !failed" :src="src" :width="size" :height="size" :preview="preview ? { src } : false" @error="failed = true" />
    <PictureOutlined v-else class="placeholder" :style="{ fontSize: `${Math.round(size * 0.45)}px` }" />
  </span>
</template>

<script setup>
import { ref, watch } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'

/** 商品缩略图：没有图片或加载失败时显示占位图标；preview 时点击放大 */
const props = defineProps({
  src: String,
  size: { type: Number, default: 40 },
  preview: { type: Boolean, default: true }
})
const failed = ref(false)
watch(() => props.src, () => (failed.value = false))
</script>

<style scoped>
.product-thumb {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  background: var(--app-hover);
}
.product-thumb :deep(.ant-image),
.product-thumb :deep(.ant-image-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.placeholder {
  color: var(--app-text-3);
}
</style>
