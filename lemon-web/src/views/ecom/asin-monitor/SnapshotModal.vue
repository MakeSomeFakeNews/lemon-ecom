<template>
  <a-modal v-model:open="open" :title="$t('ecomOper.monitor.recordTitle', { item: monitor?.itemId || '' })" :confirm-loading="saving" width="min(720px, 100vw)" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOper.monitor.snapshotDate')" name="snapshotDate" :rules="[{ required: true, message: $t('common.rule.requiredSelect', { name: $t('ecomOper.monitor.snapshotDate') }) }]">
            <a-date-picker v-model:value="form.snapshotDate" value-format="YYYY-MM-DD" :allow-clear="false" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOper.monitor.listingStatus')" name="listingStatus">
            <a-select v-model:value="form.listingStatus" allow-clear :options="statusOptions" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOper.monitor.price')" name="price">
            <a-input-number v-model:value="form.price" :min="0" :precision="2" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('ecomOper.monitor.category')" name="category">
            <a-input v-model:value="form.category" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOper.monitor.rankMain')" name="rankMain">
            <a-input-number v-model:value="form.rankMain" :min="1" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="16">
          <a-form-item :label="$t('ecomOper.monitor.subCategory')" name="subCategory">
            <a-input v-model:value="form.subCategory" :maxlength="128" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-form-item :label="$t('ecomOper.monitor.rankSub')" name="rankSub">
            <a-input-number v-model:value="form.rankSub" :min="1" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="8">
          <a-form-item :label="$t('ecomOper.monitor.rating')" name="rating">
            <a-input-number v-model:value="form.rating" :min="0" :max="5" :step="0.1" :precision="1" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="8">
          <a-form-item :label="$t('ecomOper.monitor.reviewCount')" name="reviewCount">
            <a-input-number v-model:value="form.reviewCount" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :xs="8">
          <a-form-item :label="$t('ecomOper.monitor.sellerCount')" name="sellerCount" :tooltip="$t('ecomOper.monitor.sellerCountTip')">
            <a-input-number v-model:value="form.sellerCount" :min="0" :precision="0" class="full" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomOper.monitor.titleText')" name="title">
            <a-input v-model:value="form.title" :maxlength="500" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomOper.monitor.mainImage')" name="mainImage">
            <a-input v-model:value="form.mainImage" :maxlength="500" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item :label="$t('ecomOper.monitor.bullets')" name="bullets">
            <a-textarea v-model:value="form.bullets" :maxlength="4000" :auto-size="{ minRows: 3, maxRows: 8 }" :placeholder="$t('ecomOper.monitor.bulletsPh')" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { recordSnapshot } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { ListingStatus } from '@/constants/enums'

/** 手工录入一天的快照（同一天覆盖）：默认带出监控的最新数据，只改变化的项；保存后返回新产生的预警数。ref 调用 open(monitor) */
const emit = defineEmits(['success'])
const { t } = useI18n()
const dict = useDict('ecom_listing_status')
const statusOptions = computed(() => (dict.ecom_listing_status || []).filter((d) => d.value === ListingStatus.ACTIVE || d.value === ListingStatus.INACTIVE)
  .map((d) => ({ value: d.value, label: d.label })))

const open = ref(false)
const saving = ref(false)
const formRef = ref()
const monitor = ref()
const form = ref({})

function show(m) {
  monitor.value = m
  form.value = {
    snapshotDate: dayjs().format('YYYY-MM-DD'),
    category: m.category, rankMain: m.rankMain, subCategory: m.subCategory, rankSub: m.rankSub,
    rating: m.rating == null ? undefined : Number(m.rating), reviewCount: m.reviewCount, sellerCount: m.sellerCount,
    price: m.price == null ? undefined : Number(m.price), title: m.title, mainImage: m.mainImage, bullets: m.bullets, listingStatus: m.listingStatus
  }
  open.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = Object.fromEntries(Object.entries(form.value).map(([k, v]) => [k, v === '' || v === undefined ? null : v]))
    const alerts = await recordSnapshot(monitor.value.id, payload)
    message.success(alerts?.length ? t('ecomOper.monitor.recordedAlerts', { n: alerts.length }) : t('ecomOper.monitor.recorded'))
    open.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: show })
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
