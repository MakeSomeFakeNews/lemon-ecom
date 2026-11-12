<template>
  <div class="category-picker">
    <template v-if="remote">
      <a-input-group compact class="row">
        <a-input :value="name || id" readonly :placeholder="$t('ecomPublish.category.selectPh')" class="value" />
        <a-button :disabled="disabled" @click="openModal">{{ $t('ecomPublish.category.select') }}</a-button>
        <a-button v-if="id && !disabled" @click="clear">{{ $t('common.action.clear') }}</a-button>
      </a-input-group>
      <div v-if="id" class="sub-text">ID：{{ id }}</div>
    </template>
    <a-row v-else :gutter="8">
      <a-col :span="10">
        <a-input :value="id" :placeholder="$t('ecomPublish.category.idPh')" :disabled="disabled" @update:value="(v) => emit('update:id', v)" />
      </a-col>
      <a-col :span="14">
        <a-input :value="name" :placeholder="$t('ecomPublish.category.namePh')" :disabled="disabled" @update:value="(v) => emit('update:name', v)" />
      </a-col>
    </a-row>

    <a-modal v-model:open="modalOpen" :title="$t('ecomPublish.category.title')" :footer="null" width="min(720px, 100vw)">
      <a-input-search v-model:value="keyword" :placeholder="$t('ecomPublish.category.keywordPh')" enter-button allow-clear @search="search" />
      <a-breadcrumb v-if="!keyword && path.length" class="crumbs">
        <a-breadcrumb-item><a @click="goTo(-1)">{{ $t('ecomPublish.category.root') }}</a></a-breadcrumb-item>
        <a-breadcrumb-item v-for="(p, i) in path" :key="p.id"><a @click="goTo(i)">{{ p.name }}</a></a-breadcrumb-item>
      </a-breadcrumb>
      <a-spin :spinning="loading">
        <a-list size="small" :data-source="list" class="list" :locale="{ emptyText: $t('ecomPublish.category.empty') }">
          <template #renderItem="{ item }">
            <a-list-item>
              <div class="item-text">
                <div>{{ item.name }}</div>
                <div v-if="item.path && item.path !== item.name" class="sub-text">{{ item.path }}</div>
              </div>
              <template #actions>
                <a v-if="!item.leaf && !keyword" @click="enter(item)">{{ $t('ecomPublish.category.children') }}</a>
                <a v-if="item.leaf" @click="choose(item)">{{ $t('ecomPublish.category.choose') }}</a>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-spin>
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { publishCategories } from '@/api/ecom/publish'

/**
 * 平台类目：店铺的连接器能查询类目时（亚马逊 Product Type、虾皮、TikTok、eBay、Lazada）弹窗按关键字搜索或逐级浏览，
 * 只能选叶子类目；否则手工填写类目 ID 与名称。v-model:id、v-model:name
 */
const props = defineProps({ storeId: Number, id: String, name: String, remote: Boolean, disabled: Boolean })
const emit = defineEmits(['update:id', 'update:name', 'change'])

const modalOpen = ref(false)
const keyword = ref('')
const list = ref([])
const loading = ref(false)
const path = ref([])

async function fetch(params) {
  loading.value = true
  try {
    list.value = (await publishCategories({ storeId: props.storeId, ...params })) || []
  } finally {
    loading.value = false
  }
}
function openModal() {
  modalOpen.value = true
  keyword.value = ''
  path.value = []
  fetch({})
}
function search() {
  if (keyword.value) fetch({ keyword: keyword.value })
  else fetch({ parentId: path.value.length ? path.value[path.value.length - 1].id : undefined })
}
function enter(item) {
  path.value.push({ id: item.id, name: item.name })
  fetch({ parentId: item.id })
}
function goTo(i) {
  path.value = path.value.slice(0, i + 1)
  fetch({ parentId: path.value.length ? path.value[path.value.length - 1].id : undefined })
}
function choose(item) {
  emit('update:id', item.id)
  emit('update:name', item.path || item.name)
  emit('change', item)
  modalOpen.value = false
}
function clear() {
  emit('update:id', undefined)
  emit('update:name', undefined)
  emit('change', null)
}
</script>

<style scoped>
.row {
  display: flex;
}
.value {
  flex: 1;
}
.crumbs {
  margin: 8px 0;
}
.list {
  max-height: 420px;
  overflow: auto;
  margin-top: 8px;
}
.item-text {
  min-width: 0;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
