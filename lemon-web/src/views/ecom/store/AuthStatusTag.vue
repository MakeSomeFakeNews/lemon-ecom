<template>
  <a-tag v-if="status === StoreAuthStatus.AUTHORIZED" color="success" :bordered="false"><CheckCircleOutlined /> {{ $t('ecom.hub.auth.AUTHORIZED') }}</a-tag>
  <a-tag v-else-if="status === StoreAuthStatus.REAUTH" color="error" :bordered="false"><WarningOutlined /> {{ $t('ecom.hub.auth.REAUTH') }}</a-tag>
  <a-tag v-else :bordered="false">{{ $t('ecom.hub.auth.NONE') }}</a-tag>
</template>

<script setup>
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { StoreAuthStatus } from '@/constants/enums'

/** 店铺授权状态标签：未授权 / 已授权 / 需重新授权（StoreAuthStatus） */
defineProps({ status: { type: String, default: undefined } })
</script>
