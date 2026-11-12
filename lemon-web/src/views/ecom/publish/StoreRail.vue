<template>
  <aside class="store-rail">
    <div class="rail-head">
      <div class="rail-product" :title="draft?.title || ''">
        <img v-if="draft?.images?.length" :src="draft.images[0]" class="rail-thumb" alt="" />
        <div v-else class="rail-thumb empty"><PictureOutlined /></div>
        <div class="rail-product-text">
          <div class="rail-name">{{ draft?.materialName || '-' }}</div>
          <div class="sub-text">{{ draft?.materialCode || '' }}</div>
        </div>
      </div>
      <a-button block size="small" class="rail-master" @click="emit('master')">
        <template #icon><ProfileOutlined /></template>{{ $t('ecomPublish.item.masterContent') }}
      </a-button>
    </div>
    <div class="rail-title">{{ $t('ecomPublish.item.storesN', { n: items.length }) }}</div>
    <ul class="rail-list">
      <li v-for="it in items" :key="it.id" class="rail-item" :class="{ active: it.id === currentId }" @click="emit('select', it.id)">
        <div class="rail-item-name" :title="it.storeName">{{ it.storeName }}</div>
        <div class="rail-item-tags">
          <DictTag type="ecom_platform" :value="it.platform" />
          <DictTag type="ecom_publish_status" :value="it.status" />
        </div>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { PictureOutlined, ProfileOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'

/** 刊登工作台左侧：同一刊登草稿下的店铺记录，点击切换（select(id)），顶部进入主内容（master） */
defineProps({
  draft: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  currentId: { type: Number, default: undefined }
})
const emit = defineEmits(['select', 'master'])
</script>

<style scoped>
.store-rail {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.rail-head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--app-border);
}
.rail-product {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  min-width: 0;
}
.rail-thumb {
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--app-border);
}
.rail-thumb.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-3);
}
.rail-product-text {
  min-width: 0;
}
.rail-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rail-title {
  margin: 12px 0 6px;
  color: var(--app-text-3);
  font-size: 12px;
}
.rail-list {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  list-style: none;
}
.rail-item {
  padding: 8px 10px;
  margin-bottom: 4px;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.rail-item:hover {
  background: var(--app-hover);
}
.rail-item.active {
  border-color: var(--ant-color-primary, #1677ff);
  background: var(--ant-color-primary-bg, #e6f4ff);
}
.rail-item-name {
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rail-item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.rail-item-tags :deep(.ant-tag) {
  margin-inline-end: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>

<style>
html[data-theme='dark'] .store-rail .rail-item.active {
  background: rgba(22, 119, 255, 0.16);
}
</style>
