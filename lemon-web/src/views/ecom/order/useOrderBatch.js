import { computed } from 'vue'
import { Modal, message } from 'ant-design-vue'
import {
  AuditOutlined,
  ExportOutlined,
  FilterOutlined,
  MergeCellsOutlined,
  NodeIndexOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  SyncOutlined,
  TagsOutlined
} from '@ant-design/icons-vue'
import { t } from '@/locales'
import { applyEcomOrderRules, auditEcomOrders, mergeEcomOrders, recheckEcomOrders, unholdEcomOrders } from '@/api/ecom/order'
import { ORDER_RESULT_KEYS, showBatchResult } from './order'

/** 各标签页可做的批量操作（前面的直接显示在批量操作条上，其余收进「更多」） */
const TAB_ACTIONS = {
  pending: ['audit', 'assign', 'rules', 'hold', 'outbound', 'tag', 'untag'],
  outOfStock: ['recheck', 'assign', 'hold', 'outbound', 'tag', 'untag'],
  toLabel: ['merge', 'assign', 'recheck', 'hold', 'tag', 'untag'],
  toShip: ['hold', 'tag', 'untag'],
  shipped: ['tag', 'untag'],
  abnormal: ['unhold', 'rules', 'audit', 'assign', 'tag', 'untag'],
  all: ['outbound', 'tag', 'untag']
}

/**
 * 平台订单的批量操作：确认 → 调用 → 列出失败 → 刷新（afterChange）。
 * modals 为页面中的弹窗 ref：{ hold, outbound, tag, assign }，打开时传入勾选的订单 ID
 */
export function useOrderBatch({ selectedKeys, tab, afterChange, modals }) {
  function runBatch(api, confirmTitle, content) {
    const ids = [...selectedKeys.value]
    const exec = async () => {
      showBatchResult(await api(ids), ids.length, ORDER_RESULT_KEYS)
      afterChange()
    }
    if (!confirmTitle) return exec()
    Modal.confirm({ title: confirmTitle, content, onOk: exec })
  }
  const n = () => selectedKeys.value.length
  const BATCH = {
    audit: () => ({
      key: 'audit', label: t('ecomOrder.ops.batchAudit'), icon: AuditOutlined, perm: 'ecom:order:audit', primary: true,
      run: () => runBatch(auditEcomOrders, t('ecomOrder.ops.auditConfirm', { n: n() }), t('ecomOrder.ops.auditHint'))
    }),
    rules: () => ({ key: 'rules', label: t('ecomOrder.ops.applyRules'), icon: FilterOutlined, perm: 'ecom:order:audit', run: () => runBatch(applyEcomOrderRules) }),
    recheck: () => ({ key: 'recheck', label: t('ecomOrder.ops.recheck'), icon: SyncOutlined, perm: 'ecom:order:audit', primary: true, run: () => runBatch(recheckEcomOrders) }),
    hold: () => ({ key: 'hold', label: t('ecomOrder.ops.hold'), icon: PauseCircleOutlined, perm: 'ecom:order:audit', run: () => modals.hold.value.open(selectedKeys.value) }),
    unhold: () => ({
      key: 'unhold', label: t('ecomOrder.ops.unhold'), icon: PlayCircleOutlined, perm: 'ecom:order:audit', primary: true,
      run: () => runBatch(unholdEcomOrders, t('ecomOrder.ops.unholdConfirm', { n: n() }))
    }),
    merge: () => ({
      key: 'merge', label: t('ecomOrder.ops.merge'), icon: MergeCellsOutlined, perm: 'ecom:order:merge',
      run: () => Modal.confirm({
        title: t('ecomOrder.ops.mergeConfirm', { n: n() }),
        content: t('ecomOrder.ops.mergeHint'),
        onOk: async () => {
          await mergeEcomOrders(selectedKeys.value)
          message.success(t('ecomOrder.ops.merged'))
          afterChange()
        }
      })
    }),
    outbound: () => ({ key: 'outbound', label: t('ecomOrder.order.batchOutbound'), icon: ExportOutlined, perm: 'ecom:order:outbound', run: () => modals.outbound.value.open(selectedKeys.value) }),
    tag: () => ({ key: 'tag', label: t('ecomOrd.tag.batchTag'), icon: TagsOutlined, perm: 'ecom:order:tag', run: () => modals.tag.value.open(selectedKeys.value, 'tag') }),
    untag: () => ({ key: 'untag', label: t('ecomOrd.tag.batchUntag'), icon: TagsOutlined, perm: 'ecom:order:tag', run: () => modals.tag.value.open(selectedKeys.value, 'untag') }),
    assign: () => ({ key: 'assign', label: t('ecomOrd.assign.batch'), icon: NodeIndexOutlined, perm: 'ecom:order:audit', run: () => modals.assign.value.open(selectedKeys.value) })
  }
  const batchActions = computed(() => (TAB_ACTIONS[tab.value] || []).map((k) => BATCH[k]()))
  return { batchActions }
}
