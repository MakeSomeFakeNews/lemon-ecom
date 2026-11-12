<template>
  <a-modal v-model:open="visible" :title="$t('ecomPublish.logs.title', { name: name })" :footer="null" width="min(760px, 100vw)">
    <a-spin :spinning="loading">
      <a-timeline v-if="logs.length" class="timeline">
        <a-timeline-item v-for="l in logs" :key="l.id" :color="l.success ? 'green' : 'red'">
          <div class="head">
            <DictTag type="ecom_publish_action" :value="l.action" />
            <span class="time">{{ l.createTime }}</span>
            <span class="sub-text">{{ l.operatorName || $t('ecomPublish.logs.system') }}</span>
          </div>
          <div v-if="l.message" :class="l.success ? 'msg' : 'msg error'">{{ l.message }}</div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else />
    </a-spin>
  </a-modal>
</template>

<script setup>
import { ref } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { publishLogs } from '@/api/ecom/publish'

/** 刊登日志：提交、发布、查询异步结果、下架，失败时显示平台返回的错误。open(record) */
const visible = ref(false)
const loading = ref(false)
const logs = ref([])
const name = ref('')

async function open(record) {
  visible.value = true
  name.value = `${record.storeName} · ${record.title}`
  loading.value = true
  try {
    logs.value = (await publishLogs(record.id)) || []
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.timeline {
  margin-top: 12px;
}
.head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.time {
  font-size: 12px;
}
.msg {
  margin-top: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.error {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
