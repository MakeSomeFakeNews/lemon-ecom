<template>
  <SubPage :open="open" :title="detail ? `${detail.docNo} ${detail.name}` : $t('ecomDev.project.title')" @close="open = false">
    <template v-if="detail" #extra>
      <DictTag type="ecom_dev_status" :value="detail.status" />
      <a-tag v-if="detail.flowInstanceId && isPending(detail)" color="processing">{{ $t('ecomDev.project.inFlow') }}</a-tag>
    </template>

    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-alert v-if="detail.rejectReason && (detail.status === DevProjectStatus.REJECTED || detail.status === DevProjectStatus.ABANDONED)"
                 type="warning" show-icon class="block"
                 :message="`${detail.status === DevProjectStatus.REJECTED ? $t('ecomDev.project.rejectReason') : $t('ecomDev.move.abandonReason')}：${detail.rejectReason}`" />
        <a-alert v-if="detail.materialId" type="success" show-icon class="block">
          <template #message>
            {{ $t('ecomDev.project.materialCreated', { code: detail.materialCode || '', name: detail.materialName || '' }) }}
            <a v-if="detail.materialName && can('wms:material:list')" class="ml" @click="goMaterial">{{ $t('ecomDev.project.viewMaterial') }}</a>
            <a v-if="detail.materialName && can('ecom:publish:add')" class="ml" @click="publishDraft.open({ materialId: detail.materialId, title: detail.name })">
              {{ $t('ecomDev.project.createPublish') }}
            </a>
            <a v-if="detail.materialName && can('ecom:listing:add')" class="ml" @click="goListing">{{ $t('ecomDev.project.createListing') }}</a>
            <span v-if="!detail.materialName" class="ml sub-text">{{ $t('ecomDev.project.materialDeleted') }}</span>
          </template>
        </a-alert>

        <div class="head">
          <div class="gallery">
            <a-image-preview-group>
              <div v-for="(url, i) in images" :key="url" class="img-box" :class="{ main: i === 0 }">
                <a-image :src="url" :width="96" :height="96" class="img" />
                <div v-if="canEditImages" class="img-ops">
                  <a v-if="i > 0" :title="$t('ecomDev.project.setMain')" @click="setMain(url)"><StarOutlined /></a>
                  <a-popconfirm :title="$t('ecomDev.project.deleteImageConfirm')" @confirm="removeImage(url)">
                    <a class="danger"><DeleteOutlined /></a>
                  </a-popconfirm>
                </div>
              </div>
            </a-image-preview-group>
            <a-upload v-if="canEditImages && images.length < 10" :show-upload-list="false" accept="image/*" :custom-request="upload" multiple>
              <div class="img-add"><PlusOutlined /><div>{{ $t('ecomDev.project.uploadImage') }}</div></div>
            </a-upload>
            <a-empty v-if="!images.length && !canEditImages" :image="simpleImage" :description="$t('ecomDev.project.noImage')" />
          </div>
          <a-descriptions :column="{ xs: 1, sm: 2, lg: 3 }" size="small" class="desc">
            <a-descriptions-item :label="$t('ecomDev.project.platform')">
              <DictTag type="ecom_platform" :value="detail.platform" /> {{ detail.marketplace || '' }}
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.category')">{{ detail.categoryName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.source')"><DictTag type="ecom_dev_source" :value="detail.source" /></a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.owner')">{{ detail.ownerName || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.brand')"><DictTag type="wms_material_brand" :value="detail.brand" /></a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.estMonthlySales')">{{ detail.estMonthlySales ?? '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.competitor')">
              <template v-if="detail.competitorPrice != null">{{ detail.currency }} {{ money(detail.competitorPrice) }}</template>
              <span v-if="detail.competitorReviews != null" class="sub-text"> · {{ $t('ecomDev.project.reviewsText', { n: detail.competitorReviews }) }}</span>
              <span v-if="detail.competitorRating != null" class="sub-text"> · ★ {{ Number(detail.competitorRating) }}</span>
              <a v-if="detail.competitorUrl" :href="detail.competitorUrl" target="_blank" rel="noopener" class="ml"><LinkOutlined /></a>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.targetPrice')">{{ detail.targetPrice != null ? `${detail.currency || ''} ${money(detail.targetPrice)}` : '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.estPurchasePrice')">{{ detail.estPurchasePrice != null ? `${symMoney(detail.estPurchasePrice)}` : '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.estMargin')">
              <span v-if="detail.estMargin != null" :class="['margin', marginClass(detail.estMargin)]">{{ Number(detail.estMargin) }}%</span>
              <span v-else class="sub-text">{{ $t('ecomDev.project.noEstimate') }}</span>
              <span v-if="detail.estProfit != null" class="sub-text"> · {{ symMoney(detail.estProfit) }}</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.size')">
              {{ sizeText(detail) || '-' }}<span v-if="detail.weightKg != null"> · {{ Number(detail.weightKg) }} kg</span>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ecomDev.project.createBy')">{{ detail.createByName || '-' }} {{ detail.createTime }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.sellingPoints" :label="$t('ecomDev.project.sellingPoints')" :span="3">{{ detail.sellingPoints }}</a-descriptions-item>
            <a-descriptions-item v-if="detail.description" :label="$t('ecomDev.project.description')" :span="3">
              <span class="pre">{{ detail.description }}</span>
            </a-descriptions-item>
            <a-descriptions-item v-if="detail.remark" :label="$t('common.field.remark')" :span="3">{{ detail.remark }}</a-descriptions-item>
          </a-descriptions>
        </div>

        <a-tabs v-model:active-key="tab">
          <a-tab-pane key="samples" :tab="$t('ecomDev.project.tabSamples', { n: (detail.samples || []).length })">
            <div class="tab-tools">
              <span class="sub-text">{{ $t('ecomDev.project.sampleSummary', { fee: money(detail.sampleFee), passed: detail.passedSampleCount || 0 }) }}</span>
              <a-button v-if="canEdit" size="small" type="primary" @click="sampleModal.open(detail.id)">
                <template #icon><PlusOutlined /></template>{{ $t('ecomDev.sample.add') }}
              </a-button>
            </div>
            <a-table row-key="id" size="small" :columns="sampleColumns" :data-source="detail.samples || []" :pagination="false" :scroll="{ x: 1100 }">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'supplier'">
                  <div>{{ record.supplierRecordName || record.supplierName }}</div>
                  <a v-if="record.sampleUrl" :href="record.sampleUrl" target="_blank" rel="noopener" class="sub-text"><LinkOutlined /> {{ $t('ecomDev.sample.link') }}</a>
                </template>
                <template v-else-if="column.key === 'sampleFee' || column.key === 'quotePrice'">{{ record[column.key] != null ? money(record[column.key]) : '-' }}</template>
                <template v-else-if="column.key === 'result'"><DictTag type="ecom_dev_sample_result" :value="record.result" /></template>
                <template v-else-if="column.key === 'action'">
                  <TableActions :actions="sampleActions(record)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="profit" :tab="$t('ecomDev.project.tabProfit')">
            <ProfitCalculator ref="calcRef" :key="`calc-${detail.id}`" :initial="calcInitial" :project-id="detail.id" @saved="onSchemeSaved" />
            <div class="section-title">{{ $t('ecomDev.project.schemes') }}</div>
            <SchemeTable ref="schemeTable" :project-id="detail.id" @load="(p) => calcRef?.load(p)" />
          </a-tab-pane>
          <a-tab-pane key="logs" :tab="$t('ecomDev.project.tabLogs')">
            <a-timeline class="timeline">
              <a-timeline-item v-for="l in detail.logs || []" :key="l.id" :color="logColor(l.action)">
                <div>
                  <b>{{ $t(`ecomDev.log.${l.action}`) }}</b>
                  <span v-if="l.fromStatus && l.toStatus && l.fromStatus !== l.toStatus" class="ml">
                    <DictTag type="ecom_dev_status" :value="l.fromStatus" /> → <DictTag type="ecom_dev_status" :value="l.toStatus" />
                  </span>
                </div>
                <div v-if="l.remark" class="remark">{{ l.remark }}</div>
                <div class="sub-text">{{ l.createByName || $t('ecomDev.log.system') }} · {{ l.createTime }}</div>
              </a-timeline-item>
            </a-timeline>
          </a-tab-pane>
          <a-tab-pane key="attachment" :tab="$t('ecomDev.project.tabAttachment')">
            <AttachmentPanel biz-type="ecom_dev_project" :biz-id="detail.id" :editable="can('ecom:devProject:edit')" />
          </a-tab-pane>
        </a-tabs>
      </template>
    </a-spin>

    <template v-if="detail && actions.length" #footer>
      <div class="drawer-footer">
        <a-button v-for="a in actions" :key="a.key" :type="a.primary ? 'primary' : 'default'" :danger="a.danger" @click="a.run">
          <template #icon><component :is="a.icon" /></template>{{ a.label }}
        </a-button>
      </div>
    </template>

    <SampleModal ref="sampleModal" @success="changed" />
    <RemarkModal ref="remarkModal" />
    <MaterialModal ref="materialModal" @success="changed" />
    <CreateDraftModal ref="publishDraft" @success="router.push('/ecom/publish')" />
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Empty, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  AuditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  LinkOutlined,
  PlusOutlined,
  SendOutlined,
  StarOutlined,
  SwapOutlined,
  UndoOutlined
} from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import AttachmentPanel from '@/components/AttachmentPanel.vue'
import TableActions from '@/components/common/TableActions.vue'
import ProfitCalculator from '@/components/ecom/dev/ProfitCalculator.vue'
import SchemeTable from '@/components/ecom/dev/SchemeTable.vue'
import SampleModal from './SampleModal.vue'
import RemarkModal from './RemarkModal.vue'
import MaterialModal from './MaterialModal.vue'
import CreateDraftModal from '../publish/CreateDraftModal.vue'
import { deleteDevImage, deleteDevSample, getDevProject, mainDevImage, uploadDevImage } from '@/api/ecom/dev'
import { DevLogAction, DevProjectStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { money, symMoney } from '@/utils/number'
import { canDelete, canGenerateMaterial, canManualApprove, canSubmit, imagesOf, isEditable, isPending, marginClass, MOVES, sizeText } from './dev'
import { useDevActions } from './useDevActions'

/**
 * 开发项目详情子页：资料与图片、样品记录、利润试算（保存方案回写试算利润率）、流转记录、附件；
 * 底部按状态与权限显示编辑、流转、提交 / 撤回审批、通过 / 驳回、生成物料、删除。ref 调用 open(id)、reload()
 */
const emit = defineEmits(['changed', 'edit'])
const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()
const can = (perm) => userStore.hasPerm(perm)
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const open = ref(false)
const loading = ref(false)
const detail = ref()
const tab = ref('samples')
const calcRef = ref()
const schemeTable = ref()
const sampleModal = ref()
const remarkModal = ref()
const materialModal = ref()
const publishDraft = ref()
let currentId

async function load(id) {
  loading.value = true
  try {
    detail.value = await getDevProject(id)
  } finally {
    loading.value = false
  }
}

function show(id, initialTab = 'samples') {
  currentId = id
  detail.value = undefined
  tab.value = initialTab
  open.value = true
  return load(id)
}
const reload = () => (open.value && currentId ? load(currentId) : undefined)
async function changed() {
  await reload()
  emit('changed')
}

const images = computed(() => imagesOf(detail.value))
const canEdit = computed(() => can('ecom:devProject:edit') && isEditable(detail.value))
const canEditImages = computed(() => can('ecom:devProject:edit') && detail.value?.status !== DevProjectStatus.ABANDONED)

const calcInitial = computed(() => {
  const d = detail.value || {}
  return {
    platform: d.platform || undefined,
    marketplace: d.marketplace || undefined,
    currency: d.currency || undefined,
    salePrice: d.targetPrice ?? undefined,
    purchasePrice: d.estPurchasePrice ?? undefined,
    weightKg: d.weightKg ?? undefined,
    lengthCm: d.lengthCm ?? undefined,
    widthCm: d.widthCm ?? undefined,
    heightCm: d.heightCm ?? undefined
  }
})

async function upload({ file, onSuccess, onError }) {
  try {
    await uploadDevImage(detail.value.id, file)
    onSuccess?.()
    await changed()
  } catch (e) {
    onError?.(e)
  }
}
async function removeImage(url) {
  await deleteDevImage(detail.value.id, url)
  await changed()
}
async function setMain(url) {
  await mainDevImage(detail.value.id, url)
  await changed()
}

const sampleColumns = [
  { title: t('ecomDev.sample.supplier'), key: 'supplier', width: 220 },
  { title: t('ecomDev.sample.sampleQty'), dataIndex: 'sampleQty', width: 70, align: 'right' },
  { title: t('ecomDev.sample.sampleFee'), key: 'sampleFee', width: 90, align: 'right' },
  { title: t('ecomDev.sample.quotePrice'), key: 'quotePrice', width: 90, align: 'right' },
  { title: t('ecomDev.sample.requestDate'), dataIndex: 'requestDate', width: 110 },
  { title: t('ecomDev.sample.arrivalDate'), dataIndex: 'arrivalDate', width: 110 },
  { title: t('ecomDev.sample.result'), key: 'result', width: 90 },
  { title: t('ecomDev.sample.evaluation'), dataIndex: 'evaluation', ellipsis: true },
  { title: t('common.action.operation'), key: 'action', width: 90, align: 'right', fixed: 'right' }
]
const sampleActions = (record) => [
  { type: 'edit', show: canEdit.value, onClick: () => sampleModal.value.open(detail.value.id, record) },
  {
    type: 'delete',
    show: canEdit.value,
    confirm: { title: t('ecomDev.sample.deleteConfirm'), okType: 'danger' },
    onClick: async () => {
      await deleteDevSample(record.id)
      message.success(t('common.msg.deleteSuccess'))
      await changed()
    }
  }
]

function onSchemeSaved() {
  schemeTable.value?.reload()
  changed()
}

const LOG_COLORS = { [DevLogAction.APPROVE]: 'green', [DevLogAction.MATERIAL]: 'green', [DevLogAction.REJECT]: 'red', [DevLogAction.ABANDON]: 'gray' }
const logColor = (a) => LOG_COLORS[a] || 'blue'

function goMaterial() {
  router.push({ path: '/wms/material', query: { keyword: detail.value.materialCode } })
}
function goListing() {
  router.push({ path: '/ecom/listing', query: { newMaterialId: detail.value.materialId } })
}

const { move, submit, withdraw, approve, reject, remove } = useDevActions(remarkModal, changed)

const actions = computed(() => {
  const d = detail.value
  if (!d) return []
  const list = []
  if (canDelete(d) && can('ecom:devProject:remove')) {
    list.push({ key: 'delete', label: t('common.action.delete'), icon: DeleteOutlined, danger: true, run: () => remove(d, () => (open.value = false)) })
  }
  if ((MOVES[d.status] || []).length && can('ecom:devProject:edit')) {
    list.push({ key: 'move', label: t('ecomDev.action.move'), icon: SwapOutlined, run: () => move(d) })
  }
  if (isPending(d) && can('ecom:devProject:edit')) {
    list.push({ key: 'withdraw', label: t('ecomDev.action.withdraw'), icon: UndoOutlined, run: () => withdraw(d) })
  }
  if (isEditable(d) && can('ecom:devProject:edit')) {
    list.push({ key: 'edit', label: t('common.action.edit'), icon: EditOutlined, run: () => emit('edit', d) })
  }
  if (canSubmit(d) && can('ecom:devProject:edit')) {
    list.push({ key: 'submit', label: t('ecomDev.action.submit'), icon: SendOutlined, primary: true, run: () => submit(d) })
  }
  if (canManualApprove(d) && can('ecom:devProject:approve')) {
    list.push({ key: 'reject', label: t('ecomDev.action.reject'), icon: CloseCircleOutlined, danger: true, run: () => reject(d) })
    list.push({ key: 'approve', label: t('ecomDev.action.approve'), icon: CheckCircleOutlined, primary: true, run: () => approve(d) })
  }
  if (isPending(d) && d.flowInstanceId && can('wf:task:todo')) {
    list.push({ key: 'flow', label: t('ecomDev.action.viewFlow'), icon: AuditOutlined, run: () => router.push('/workflow/todo') })
  }
  if (canGenerateMaterial(d) && can('ecom:devProject:material')) {
    list.push({ key: 'material', label: t('ecomDev.action.material'), icon: PlusOutlined, primary: true, run: () => materialModal.value.open(d.id) })
  }
  return list
})

defineExpose({ open: show, reload })
</script>

<style scoped>
.block {
  margin-bottom: 12px;
}
.head {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.gallery {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  align-content: flex-start;
  gap: 8px;
  max-width: 420px;
}
.desc {
  flex: 1;
  min-width: 300px;
}
.img-box {
  position: relative;
  border: 1px solid var(--app-border, #f0f0f0);
  border-radius: 6px;
  overflow: hidden;
}
.img-box.main {
  border-color: #faad14;
}
.img-box :deep(.ant-image-img) {
  object-fit: cover;
}
.img-ops {
  position: absolute;
  right: 2px;
  top: 2px;
  display: flex;
  gap: 6px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 4px;
  padding: 0 4px;
}
.img-add {
  width: 96px;
  height: 96px;
  border: 1px dashed var(--app-border, #d9d9d9);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--app-text-3);
  cursor: pointer;
  font-size: 12px;
}
.tab-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.ml {
  margin-left: 8px;
}
.danger {
  color: #ff4d4f;
}
.pre {
  white-space: pre-wrap;
}
.remark {
  margin: 2px 0;
}
.margin.good {
  color: #389e0d;
}
.margin.low {
  color: #d48806;
}
.margin.loss {
  color: #cf1322;
}
.timeline {
  padding-top: 8px;
}
</style>
