import { Modal, message } from 'ant-design-vue'
import { t } from '@/locales'
import {
  approveDevProject,
  deleteDevProject,
  moveDevProject,
  rejectDevProject,
  submitDevProject,
  withdrawDevProject
} from '@/api/ecom/dev'
import { MOVES } from './dev'

/**
 * 开发项目的状态操作（列表、看板、详情共用）：流转、提交 / 撤回审批、通过、驳回、删除。
 * remarkModal 为 RemarkModal 组件的 ref，changed 为操作成功后的回调
 */
export function useDevActions(remarkModal, changed) {
  const done = async (key) => {
    if (key) message.success(t(key))
    await changed?.()
  }

  /** 流转：to 指定目标状态时直接确认（放弃需要原因），否则弹窗选择 */
  function move(p, to) {
    const statuses = to ? [to] : MOVES[p.status] || []
    if (!statuses.length) return
    remarkModal.value.open({
      title: t('ecomDev.move.title', { no: p.docNo }),
      statuses,
      status: to,
      onOk: async (remark, status) => {
        await moveDevProject(p.id, status, remark)
        await done('ecomDev.move.moved')
      }
    })
  }

  function submit(p) {
    Modal.confirm({
      title: t('ecomDev.action.submitConfirm', { no: p.docNo }),
      content: t('ecomDev.action.submitContent'),
      async onOk() {
        const flow = await submitDevProject(p.id)
        await done(flow ? 'ecomDev.action.submittedFlow' : 'ecomDev.action.submitted')
      }
    })
  }

  function withdraw(p) {
    Modal.confirm({
      title: t('ecomDev.action.withdrawConfirm', { no: p.docNo }),
      async onOk() {
        await withdrawDevProject(p.id)
        await done('ecomDev.action.withdrawn')
      }
    })
  }

  function approve(p) {
    remarkModal.value.open({
      title: t('ecomDev.action.approveTitle', { no: p.docNo }),
      remarkLabel: t('ecomDev.action.approveRemark'),
      onOk: async (remark) => {
        await approveDevProject(p.id, remark)
        await done('ecomDev.action.approved')
      }
    })
  }

  function reject(p) {
    remarkModal.value.open({
      title: t('ecomDev.action.rejectTitle', { no: p.docNo }),
      remarkLabel: t('ecomDev.action.rejectReason'),
      remarkRequired: true,
      onOk: async (remark) => {
        await rejectDevProject(p.id, remark)
        await done('ecomDev.action.rejected')
      }
    })
  }

  function remove(p, after) {
    Modal.confirm({
      title: t('ecomDev.action.deleteConfirm', { no: p.docNo }),
      okType: 'danger',
      async onOk() {
        await deleteDevProject(p.id)
        after?.()
        await done('common.msg.deleteSuccess')
      }
    })
  }

  return { move, submit, withdraw, approve, reject, remove }
}
