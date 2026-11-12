<template>
  <a-drawer v-model:open="visible" :title="$t('ecomAuto.template.title')" width="min(920px, 100vw)" destroy-on-close>
    <a-alert type="info" show-icon :message="$t('ecomAuto.template.tip')" class="tip" />
    <div class="bar">
      <span class="muted">{{ $t('ecomAuto.template.scope') }}</span>
      <a-select v-model:value="storeIds" mode="multiple" allow-clear :options="storeOptions" option-filter-prop="label" :max-tag-count="3"
        :placeholder="$t('ecomAuto.template.scopePh')" class="stores" />
    </div>
    <div class="bar">
      <a-segmented v-model:value="category" :options="categoryOptions" class="cats" />
    </div>
    <a-spin :spinning="loading">
      <div class="grid">
        <div v-for="tp in shown" :key="tp.code" class="tpl">
          <div class="tpl-head">
            <ThunderboltOutlined class="icon" />
            <span class="name">{{ tp.name }}</span>
          </div>
          <div class="desc">{{ tp.description }}</div>
          <div class="meta">
            <DictTag type="ecom_auto_trigger" :value="tp.trigger" />
            <span class="arrow">→</span>
            <a-tag v-for="a in tp.actionTypes" :key="a" :bordered="false" class="act">{{ findDict(dict.ecom_auto_action, a)?.label || a }}</a-tag>
          </div>
          <div class="ops">
            <a-button v-perm="'ecom:automation:add'" size="small" @click="edit(tp)">{{ $t('ecomAuto.template.edit') }}</a-button>
            <a-button v-perm="'ecom:automation:add'" type="primary" size="small" :loading="creating === tp.code" @click="create(tp)">
              {{ $t('ecomAuto.template.create') }}
            </a-button>
          </div>
        </div>
      </div>
    </a-spin>
  </a-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { autoRuleFromTemplate, automationTemplates } from '@/api/ecom/automation'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { t } from '@/locales'

/** 规则模板库：按分类浏览，选择适用店铺后一键创建，或带入编辑页修改后保存（@edit(rule)） */
const emit = defineEmits(['created', 'edit'])
const dict = useDict('ecom_auto_category', 'ecom_auto_action', 'ecom_auto_trigger')
const stores = useStores()
const storeOptions = computed(() => stores.value.map((s) => ({ value: s.id, label: s.name })))
const visible = ref(false)
const loading = ref(false)
const templates = ref([])
const category = ref('ALL')
const storeIds = ref([])
const creating = ref(null)

const categoryOptions = computed(() => {
  const cats = [...new Set(templates.value.map((x) => x.category))]
  return [{ value: 'ALL', label: t('ecomAuto.template.all') }, ...cats.map((c) => ({ value: c, label: findDict(dict.ecom_auto_category, c)?.label || c }))]
})
const shown = computed(() => templates.value.filter((x) => category.value === 'ALL' || x.category === category.value))

async function create(tp) {
  creating.value = tp.code
  try {
    await autoRuleFromTemplate(tp.code, { storeIds: storeIds.value })
    message.success(t('ecomAuto.template.created', { name: tp.name }))
    emit('created')
  } finally {
    creating.value = null
  }
}

function edit(tp) {
  const rule = { ...tp.rule, scope: { ...(tp.rule.scope || {}), storeIds: [...storeIds.value] } }
  visible.value = false
  emit('edit', rule)
}

defineExpose({
  async open() {
    visible.value = true
    if (templates.value.length) return
    loading.value = true
    try {
      templates.value = (await automationTemplates()) || []
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}
.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.spacer {
  flex: 1;
}
.muted {
  color: var(--app-text-3);
}
.stores {
  flex: 1;
  min-width: 240px;
}
.cats {
  max-width: 100%;
  overflow-x: auto;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.tpl {
  display: flex;
  flex-direction: column;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: 10px;
  transition: box-shadow 0.2s;
}
.tpl:hover {
  box-shadow: var(--app-shadow, 0 2px 8px rgba(0, 0, 0, 0.08));
}
.tpl-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon {
  color: var(--app-primary);
  font-size: 16px;
}
.name {
  font-weight: 600;
}
.desc {
  flex: 1;
  margin: 8px 0;
  color: var(--app-text-2);
  font-size: 13px;
  line-height: 1.6;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}
.arrow {
  color: var(--app-text-3);
}
.act {
  margin: 0;
}
.ops {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
