package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.file.FileStorageService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.framework.security.datascope.DataScopes;
import com.lemon.erp.framework.workflow.ApprovalFlow;
import com.lemon.erp.framework.workflow.ApprovalSupport;
import com.lemon.erp.framework.workflow.FlowFinishedEvent;
import com.lemon.erp.framework.workflow.FlowStatus;
import com.lemon.erp.modules.ecom.dev.dto.DevBoard;
import com.lemon.erp.modules.ecom.dev.dto.DevMaterialRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectQuery;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevSampleRequest;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevLog;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevSample;
import com.lemon.erp.modules.ecom.dev.entity.EcomProfitScheme;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevLogMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevProjectMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevSampleMapper;
import com.lemon.erp.modules.ecom.dev.mapper.EcomProfitSchemeMapper;
import com.lemon.erp.modules.ecom.dev.model.DevLogAction;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import com.lemon.erp.modules.ecom.dev.model.DevSampleResult;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialImageService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 产品开发项目：候选产品资料、样品记录、流转记录；状态 待调研 → 调研中 → 打样中 → 待审批 → 已通过 / 已驳回，进行中可放弃。
 * <p>
 * 提交审批：流程「开发项目审批」（ecom_dev_project）已发布时发起审批，通过后自动生成物料，驳回为已驳回，撤回 / 终止恢复提交前的状态；
 * 未发布时进入待审批，由有审批权限的人在项目中通过或驳回。审批通过后生成物料（名称、分类、品牌、参考成本 = 预估采购价、图片），
 * 物料被删除后可重新生成。数据权限按创建人部门 + 创建人
 */
@Service
@RequiredArgsConstructor
public class EcomDevProjectService {

    public static final ApprovalFlow FLOW = new ApprovalFlow("ecom_dev_project", "ecom_dev_project:");
    static final DataPermissionRule RULE = new DataPermissionRule("ecom_dev_project", "dept_id", "create_by");
    /** 图片目录、单张上限、张数上限 */
    static final String IMAGE_DIR = "ecom-dev";
    static final long IMAGE_MAX_SIZE = 5L * 1024 * 1024;
    static final int MAX_IMAGES = 10;
    /** 看板每列最多显示的卡片数 */
    static final int BOARD_LIMIT = 200;
    static final String SOURCE_DICT = "ecom_dev_source";
    static final String BRAND_DICT = "wms_material_brand";
    /** 状态名称的消息键前缀（ecom.label.dev.status.状态） */
    static final String STATUS_LABEL_PREFIX = "ecom.label.dev.status.";

    private final EcomDevProjectMapper projectMapper;
    private final EcomDevSampleMapper sampleMapper;
    private final EcomDevLogMapper logMapper;
    private final EcomProfitSchemeMapper schemeMapper;
    private final DocNoService docNoService;
    private final WmsMaterialService materialService;
    private final WmsMaterialImageService materialImageService;
    private final ScmSupplierService supplierService;
    private final ApprovalSupport approvalSupport;
    private final FileStorageService fileStorageService;
    private final DictLookup dictLookup;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- 查询

    @DataPermission(table = "ecom_dev_project", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<EcomDevProject> page(long current, long size, DevProjectQuery query) {
        Page<EcomDevProject> page = Page.of(current, size);
        projectMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    /** 看板：按状态分列，每列最近更新的项目（同样按数据权限） */
    public DevBoard board(DevProjectQuery query) {
        DevProjectQuery q = query == null ? new DevProjectQuery(null, null, null, null, null, null)
                : new DevProjectQuery(null, query.keyword(), query.ownerId(), query.platform(), query.source(), query.category());
        Map<DevProjectStatus, Long> counts = new EnumMap<>(DevProjectStatus.class);
        DataScopes.load(RULE, () -> projectMapper.selectStatusCounts(q)).forEach(m -> {
            Object s = m.get("status");
            Object c = m.get("cnt");
            if (s != null && c != null) {
                counts.put(DevProjectStatus.valueOf(s.toString()), ((Number) c).longValue());
            }
        });
        List<DevBoard.Column> columns = new ArrayList<>();
        for (DevProjectStatus status : DevProjectStatus.values()) {
            DevProjectQuery sq = new DevProjectQuery(status, q.keyword(), q.ownerId(), q.platform(), q.source(), q.category());
            long count = counts.getOrDefault(status, 0L);
            List<EcomDevProject> items = count == 0 ? List.of() : DataScopes.load(RULE, () -> projectMapper.selectBoard(sq, BOARD_LIMIT));
            columns.add(new DevBoard.Column(status, count, items));
        }
        return new DevBoard(columns, BOARD_LIMIT);
    }

    /** 详情：含样品、流转记录 */
    public EcomDevProject detail(Long id) {
        EcomDevProject p = DataScopes.require(RULE, () -> projectMapper.selectDetail(id), "ecom.error.dev.projectNotFound");
        p.setSamples(sampleMapper.selectByProject(id));
        p.setLogs(logMapper.selectByProject(id));
        return p;
    }

    /** 按 ID 读取（按数据权限），不存在或无权访问时报错 */
    public EcomDevProject get(Long id) {
        return DataScopes.require(RULE, () -> projectMapper.selectById(id), "ecom.error.dev.projectNotFound");
    }

    /** 附件等按数据权限读取，不存在返回 null */
    public EcomDevProject find(Long id) {
        return DataScopes.load(RULE, () -> projectMapper.selectById(id));
    }

    // ---------------------------------------------------------------- 新建 / 修改 / 删除

    @Transactional
    public Long create(DevProjectRequest req) {
        EcomDevProject p = new EcomDevProject();
        apply(p, req);
        p.setDocNo(docNoService.next(EcomDevDocNos.PROJECT));
        p.setStatus(DevProjectStatus.PENDING_RESEARCH);
        p.setDeptId(SecurityUtils.getLoginUser().deptId());
        if (p.getOwnerId() == null) {
            p.setOwnerId(SecurityUtils.getUserId());
        }
        projectMapper.insert(p);
        log(p.getId(), DevLogAction.CREATE, null, DevProjectStatus.PENDING_RESEARCH, null);
        return p.getId();
    }

    /** 修改资料：审批中、已通过、已放弃的项目不能修改 */
    public void update(Long id, DevProjectRequest req) {
        EcomDevProject old = get(id);
        if (!old.getStatus().editable()) {
            throw new BizException("ecom.error.dev.notEditable", old.getDocNo());
        }
        EcomDevProject p = new EcomDevProject();
        apply(p, req);
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate()
                .set(EcomDevProject::getName, p.getName())
                .set(EcomDevProject::getCategory, p.getCategory())
                .set(EcomDevProject::getBrand, p.getBrand())
                .set(EcomDevProject::getSource, p.getSource())
                .set(EcomDevProject::getPlatform, p.getPlatform())
                .set(EcomDevProject::getMarketplace, p.getMarketplace())
                .set(EcomDevProject::getCurrency, p.getCurrency())
                .set(EcomDevProject::getCompetitorUrl, p.getCompetitorUrl())
                .set(EcomDevProject::getCompetitorPrice, p.getCompetitorPrice())
                .set(EcomDevProject::getCompetitorReviews, p.getCompetitorReviews())
                .set(EcomDevProject::getCompetitorRating, p.getCompetitorRating())
                .set(EcomDevProject::getEstMonthlySales, p.getEstMonthlySales())
                .set(EcomDevProject::getTargetPrice, p.getTargetPrice())
                .set(EcomDevProject::getEstPurchasePrice, p.getEstPurchasePrice())
                .set(EcomDevProject::getWeightKg, p.getWeightKg())
                .set(EcomDevProject::getLengthCm, p.getLengthCm())
                .set(EcomDevProject::getWidthCm, p.getWidthCm())
                .set(EcomDevProject::getHeightCm, p.getHeightCm())
                .set(EcomDevProject::getOwnerId, p.getOwnerId() == null ? old.getOwnerId() : p.getOwnerId())
                .set(EcomDevProject::getSellingPoints, p.getSellingPoints())
                .set(EcomDevProject::getDescription, p.getDescription())
                .set(EcomDevProject::getRemark, p.getRemark())
                .set(EcomDevProject::getUpdateTime, LocalDateTime.now())
                .eq(EcomDevProject::getId, id)
                .in(EcomDevProject::getStatus, DevProjectStatus.EDITABLE));
    }

    private void apply(EcomDevProject p, DevProjectRequest r) {
        p.setName(r.name().trim());
        p.setCategory(StringUtils.hasText(r.category()) ? r.category().trim() : null);
        p.setBrand(StringUtils.hasText(r.brand()) ? r.brand().trim() : null);
        p.setSource(StringUtils.hasText(r.source()) ? r.source().trim() : null);
        if (p.getSource() != null) {
            dictLookup.check(SOURCE_DICT, p.getSource(), "ecom.label.dev.source");
        }
        if (p.getBrand() != null) {
            dictLookup.check(BRAND_DICT, p.getBrand(), "ecom.label.dev.brand");
        }
        p.setPlatform(r.platform());
        p.setMarketplace(StringUtils.hasText(r.marketplace()) ? r.marketplace().trim() : null);
        if (p.getMarketplace() != null) {
            dictLookup.check(EcomConstants.MARKETPLACE_DICT, p.getMarketplace(), "ecom.label.marketplace");
        }
        String currency = StringUtils.hasText(r.currency()) ? r.currency().trim().toUpperCase(Locale.ROOT) : DevMarketplaces.currency(p.getMarketplace());
        p.setCurrency(currency);
        p.setCompetitorUrl(StringUtils.hasText(r.competitorUrl()) ? r.competitorUrl().trim() : null);
        p.setCompetitorPrice(r.competitorPrice());
        p.setCompetitorReviews(r.competitorReviews());
        p.setCompetitorRating(r.competitorRating());
        p.setEstMonthlySales(r.estMonthlySales());
        p.setTargetPrice(r.targetPrice());
        p.setEstPurchasePrice(r.estPurchasePrice());
        p.setWeightKg(r.weightKg());
        p.setLengthCm(r.lengthCm());
        p.setWidthCm(r.widthCm());
        p.setHeightCm(r.heightCm());
        p.setOwnerId(r.ownerId());
        p.setSellingPoints(r.sellingPoints());
        p.setDescription(r.description());
        p.setRemark(r.remark());
    }

    /** 删除：只能删除还没有生成物料的待调研、已驳回、已放弃项目（样品、流转记录一并删除，试算方案取消关联） */
    @Transactional
    public void delete(List<Long> ids) {
        List<String> images = new ArrayList<>();
        for (Long id : ids) {
            EcomDevProject p = get(id);
            if (p.getMaterialId() != null || (p.getStatus() != DevProjectStatus.PENDING_RESEARCH && p.getStatus() != DevProjectStatus.REJECTED
                    && p.getStatus() != DevProjectStatus.ABANDONED)) {
                throw new BizException("ecom.error.dev.cannotDelete", p.getDocNo());
            }
            images.addAll(images(p));
        }
        sampleMapper.delete(Wrappers.<EcomDevSample>lambdaQuery().in(EcomDevSample::getProjectId, ids));
        logMapper.delete(Wrappers.<EcomDevLog>lambdaQuery().in(EcomDevLog::getProjectId, ids));
        schemeMapper.update(Wrappers.<EcomProfitScheme>lambdaUpdate().set(EcomProfitScheme::getProjectId, null).in(EcomProfitScheme::getProjectId, ids));
        projectMapper.deleteByIds(ids);
        afterCommit(() -> images.forEach(fileStorageService::deleteImage));
    }

    // ---------------------------------------------------------------- 状态流转

    /** 手工流转（看板拖拽 / 按钮）：只能在待调研、调研中、打样中、已驳回、已放弃之间按规则移动 */
    @Transactional
    public void move(Long id, DevProjectStatus target, String remark) {
        EcomDevProject p = get(id);
        if (p.getStatus() == target) {
            return;
        }
        if (!p.getStatus().canMoveTo(target)) {
            throw new BizException("ecom.error.dev.cannotMove", p.getDocNo(), statusLabel(p.getStatus()), statusLabel(target));
        }
        boolean abandon = target == DevProjectStatus.ABANDONED;
        StatusTransitions.change(projectMapper, EcomDevProject::getId, id, EcomDevProject::getStatus, p.getStatus(), target,
                w -> w.set(EcomDevProject::getRejectReason, abandon ? remark : p.getRejectReason()).set(EcomDevProject::getUpdateTime, LocalDateTime.now()),
                "ecom.error.dev.statusChanged", p.getDocNo());
        log(id, abandon ? DevLogAction.ABANDON : DevLogAction.MOVE, p.getStatus(), target, remark);
    }

    /**
     * 提交审批：需要填写预估采购价和目标售价。流程已发布时发起审批（审批中仍显示待审批），未发布时进入待审批由审批人手工处理
     *
     * @return true 已发起审批流程，false 等待手工审批
     */
    @Transactional
    public boolean submit(Long id) {
        EcomDevProject p = get(id);
        if (!DevProjectStatus.SUBMITTABLE.contains(p.getStatus())) {
            throw new BizException("ecom.error.dev.cannotSubmit", p.getDocNo());
        }
        if (p.getEstPurchasePrice() == null || p.getTargetPrice() == null) {
            throw new BizException("ecom.error.dev.submitNeedPrice");
        }
        DevProjectStatus from = p.getStatus();
        Runnable toPending = () -> StatusTransitions.change(projectMapper, EcomDevProject::getId, id, EcomDevProject::getStatus, from,
                DevProjectStatus.PENDING_APPROVAL,
                w -> w.set(EcomDevProject::getPrevStatus, from == DevProjectStatus.REJECTED ? DevProjectStatus.RESEARCHING : from)
                        .set(EcomDevProject::getRejectReason, null).set(EcomDevProject::getFlowInstanceId, null)
                        .set(EcomDevProject::getUpdateTime, LocalDateTime.now()),
                "ecom.error.dev.statusChanged", p.getDocNo());
        boolean flow = approvalSupport.isAvailable(FLOW);
        if (flow) {
            approvalSupport.start(FLOW, id, flowVariables(p), toPending,
                    instanceId -> projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate()
                            .set(EcomDevProject::getFlowInstanceId, instanceId).eq(EcomDevProject::getId, id)));
        } else {
            toPending.run();
        }
        log(id, DevLogAction.SUBMIT, from, DevProjectStatus.PENDING_APPROVAL, null);
        return flow;
    }

    /** 流程变量（与 EcomDevFlowBusinesses 声明一致） */
    private Map<String, Object> flowVariables(EcomDevProject p) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("estMargin", Decimals.nz(p.getEstMargin()));
        vars.put("estProfit", Decimals.nz(p.getEstProfit()));
        vars.put("targetPrice", Decimals.nz(p.getTargetPrice()));
        vars.put("estPurchasePrice", Decimals.nz(p.getEstPurchasePrice()));
        vars.put("estMonthlySales", p.getEstMonthlySales() == null ? 0 : p.getEstMonthlySales());
        vars.put("sampleFee", Decimals.sum(sampleMapper.selectList(Wrappers.<EcomDevSample>lambdaQuery()
                .eq(EcomDevSample::getProjectId, p.getId())), EcomDevSample::getSampleFee));
        vars.put("platform", p.getPlatform() == null ? "" : p.getPlatform().name());
        vars.put("projectName", p.getName());
        return vars;
    }

    /** 撤回审批：恢复提交前的状态，流程仍在运行时撤销 */
    @Transactional
    public void withdraw(Long id) {
        EcomDevProject p = get(id);
        if (p.getStatus() != DevProjectStatus.PENDING_APPROVAL) {
            throw new BizException("ecom.error.dev.notPending", p.getDocNo());
        }
        DevProjectStatus back = backStatus(p);
        StatusTransitions.change(projectMapper, EcomDevProject::getId, id, EcomDevProject::getStatus, DevProjectStatus.PENDING_APPROVAL, back,
                w -> w.set(EcomDevProject::getFlowInstanceId, null).set(EcomDevProject::getUpdateTime, LocalDateTime.now()),
                "ecom.error.dev.statusChanged", p.getDocNo());
        approvalSupport.cancelIfRunning(p.getFlowInstanceId(), Messages.get("ecom.text.dev.withdrawn"));
        log(id, DevLogAction.WITHDRAW, DevProjectStatus.PENDING_APPROVAL, back, null);
    }

    /** 手工审批通过（没有发起审批流程时），随后生成物料 */
    @Transactional
    public void approve(Long id, String remark) {
        EcomDevProject p = requireManual(id);
        doApprove(p, SecurityUtils.getUserId(), remark);
    }

    /** 手工驳回（没有发起审批流程时） */
    @Transactional
    public void reject(Long id, String reason) {
        EcomDevProject p = requireManual(id);
        doReject(p, SecurityUtils.getUserId(), reason);
    }

    private EcomDevProject requireManual(Long id) {
        EcomDevProject p = get(id);
        if (p.getStatus() != DevProjectStatus.PENDING_APPROVAL) {
            throw new BizException("ecom.error.dev.notPending", p.getDocNo());
        }
        if (p.getFlowInstanceId() != null) {
            throw new BizException("ecom.error.dev.inFlow", p.getDocNo());
        }
        return p;
    }

    private void doApprove(EcomDevProject p, Long userId, String remark) {
        StatusTransitions.change(projectMapper, EcomDevProject::getId, p.getId(), EcomDevProject::getStatus, DevProjectStatus.PENDING_APPROVAL,
                DevProjectStatus.APPROVED, w -> w.set(EcomDevProject::getApproveBy, userId).set(EcomDevProject::getApproveTime, LocalDateTime.now())
                        .set(EcomDevProject::getUpdateTime, LocalDateTime.now()),
                "ecom.error.dev.statusChanged", p.getDocNo());
        log(p.getId(), DevLogAction.APPROVE, DevProjectStatus.PENDING_APPROVAL, DevProjectStatus.APPROVED, remark);
        createMaterial(projectMapper.selectById(p.getId()), null);
    }

    private void doReject(EcomDevProject p, Long userId, String reason) {
        StatusTransitions.change(projectMapper, EcomDevProject::getId, p.getId(), EcomDevProject::getStatus, DevProjectStatus.PENDING_APPROVAL,
                DevProjectStatus.REJECTED, w -> w.set(EcomDevProject::getApproveBy, userId).set(EcomDevProject::getApproveTime, LocalDateTime.now())
                        .set(EcomDevProject::getRejectReason, reason).set(EcomDevProject::getUpdateTime, LocalDateTime.now()),
                "ecom.error.dev.statusChanged", p.getDocNo());
        log(p.getId(), DevLogAction.REJECT, DevProjectStatus.PENDING_APPROVAL, DevProjectStatus.REJECTED, reason);
    }

    /** 审批流程结束：通过 → 已通过并生成物料；驳回 → 已驳回；撤销 / 终止 → 恢复提交前的状态 */
    @EventListener
    @Transactional
    public void onFlowFinished(FlowFinishedEvent e) {
        FLOW.onFinishedWithStatus(e, (id, status) -> {
            EcomDevProject p = projectMapper.selectById(id);
            if (p == null || p.getStatus() != DevProjectStatus.PENDING_APPROVAL) {
                return;
            }
            if (status == FlowStatus.APPROVED) {
                doApprove(p, null, null);
            } else if (status == FlowStatus.REJECTED) {
                doReject(p, null, Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.dev.flowRejected"));
            } else {
                DevProjectStatus back = backStatus(p);
                StatusTransitions.tryChange(projectMapper, EcomDevProject::getId, id, EcomDevProject::getStatus, DevProjectStatus.PENDING_APPROVAL,
                        back, w -> w.set(EcomDevProject::getFlowInstanceId, null));
                log(id, DevLogAction.WITHDRAW, DevProjectStatus.PENDING_APPROVAL, back, null);
            }
        });
    }

    /** 状态名称（按当前语言） */
    private static String statusLabel(DevProjectStatus s) {
        return Messages.get(STATUS_LABEL_PREFIX + s.name());
    }

    private static DevProjectStatus backStatus(EcomDevProject p) {
        return p.getPrevStatus() == null || p.getPrevStatus() == DevProjectStatus.PENDING_APPROVAL ? DevProjectStatus.RESEARCHING : p.getPrevStatus();
    }

    // ---------------------------------------------------------------- 生成物料

    /**
     * 已通过的项目生成物料（审批通过时自动生成；物料被删除后可重新生成）
     *
     * @return 物料 ID
     */
    @Transactional
    public Long generateMaterial(Long id, DevMaterialRequest req) {
        EcomDevProject p = get(id);
        if (p.getStatus() != DevProjectStatus.APPROVED) {
            throw new BizException("ecom.error.dev.notApproved", p.getDocNo());
        }
        if (p.getMaterialId() != null && !materialService.mapByIds(List.of(p.getMaterialId())).isEmpty()) {
            throw new BizException("ecom.error.dev.materialExists", p.getDocNo());
        }
        return createMaterial(p, req);
    }

    private Long createMaterial(EcomDevProject p, DevMaterialRequest req) {
        WmsMaterial m = new WmsMaterial();
        m.setCode(req == null || !StringUtils.hasText(req.code()) ? null : req.code().trim());
        m.setName(p.getName().length() > 128 ? p.getName().substring(0, 128) : p.getName());
        m.setSpec(req == null || !StringUtils.hasText(req.spec()) ? null : req.spec().trim());
        m.setUnit(req != null && StringUtils.hasText(req.unit()) ? req.unit().trim() : Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.dev.defaultUnit"));
        m.setCategory(p.getCategory());
        m.setBrand(p.getBrand());
        m.setRefCost(p.getEstPurchasePrice());
        m.setBatchEnabled(0);
        m.setSerialEnabled(0);
        m.setRemark(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.dev.materialRemark", p.getDocNo()));
        materialService.create(m);
        List<String> images = images(p);
        if (!images.isEmpty()) {
            materialImageService.copyFromUrls(m.getId(), images);
        }
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate().set(EcomDevProject::getMaterialId, m.getId()).eq(EcomDevProject::getId, p.getId()));
        log(p.getId(), DevLogAction.MATERIAL, DevProjectStatus.APPROVED, DevProjectStatus.APPROVED, m.getCode());
        return m.getId();
    }

    // ---------------------------------------------------------------- 图片

    /** 上传一张图片（最多 10 张），返回图片地址 */
    @Transactional
    public String uploadImage(Long id, MultipartFile file) {
        EcomDevProject p = get(id);
        List<String> images = new ArrayList<>(images(p));
        if (images.size() >= MAX_IMAGES) {
            throw new BizException("ecom.error.dev.imageTooMany", MAX_IMAGES);
        }
        String url = fileStorageService.storeImage(file, IMAGE_DIR, IMAGE_MAX_SIZE);
        images.add(url);
        saveImages(id, images);
        return url;
    }

    /** 删除图片（提交后删除文件） */
    @Transactional
    public void deleteImage(Long id, String url) {
        EcomDevProject p = get(id);
        List<String> images = new ArrayList<>(images(p));
        if (!images.remove(url)) {
            return;
        }
        saveImages(id, images);
        afterCommit(() -> fileStorageService.deleteImage(url));
    }

    /** 设为主图（排到第一张） */
    public void setMainImage(Long id, String url) {
        EcomDevProject p = get(id);
        List<String> images = new ArrayList<>(images(p));
        if (images.remove(url)) {
            images.add(0, url);
            saveImages(id, images);
        }
    }

    private void saveImages(Long id, List<String> images) {
        try {
            projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate()
                    .set(EcomDevProject::getImages, images.isEmpty() ? null : objectMapper.writeValueAsString(images)).eq(EcomDevProject::getId, id));
        } catch (JacksonException e) {
            throw new BizException("ecom.error.dev.paramsInvalid");
        }
    }

    List<String> images(EcomDevProject p) {
        if (!StringUtils.hasText(p.getImages())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(p.getImages(), new TypeReference<List<String>>() {
            });
        } catch (JacksonException e) {
            return List.of();
        }
    }

    // ---------------------------------------------------------------- 样品

    /** 新增样品：项目在待调研 / 调研中时自动进入打样中 */
    @Transactional
    public Long addSample(Long projectId, DevSampleRequest req) {
        EcomDevProject p = get(projectId);
        checkSampleEditable(p);
        EcomDevSample s = new EcomDevSample();
        applySample(s, req);
        s.setProjectId(projectId);
        sampleMapper.insert(s);
        if (p.getStatus() == DevProjectStatus.PENDING_RESEARCH || p.getStatus() == DevProjectStatus.RESEARCHING) {
            if (StatusTransitions.tryChange(projectMapper, EcomDevProject::getId, projectId, EcomDevProject::getStatus, p.getStatus(),
                    DevProjectStatus.SAMPLING)) {
                log(projectId, DevLogAction.MOVE, p.getStatus(), DevProjectStatus.SAMPLING, null);
            }
        }
        return s.getId();
    }

    public void updateSample(Long sampleId, DevSampleRequest req) {
        EcomDevSample s = sample(sampleId);
        checkSampleEditable(get(s.getProjectId()));
        applySample(s, req);
        sampleMapper.update(Wrappers.<EcomDevSample>lambdaUpdate()
                .set(EcomDevSample::getSupplierId, s.getSupplierId())
                .set(EcomDevSample::getSupplierName, s.getSupplierName())
                .set(EcomDevSample::getSampleUrl, s.getSampleUrl())
                .set(EcomDevSample::getSampleQty, s.getSampleQty())
                .set(EcomDevSample::getSampleFee, s.getSampleFee())
                .set(EcomDevSample::getQuotePrice, s.getQuotePrice())
                .set(EcomDevSample::getRequestDate, s.getRequestDate())
                .set(EcomDevSample::getArrivalDate, s.getArrivalDate())
                .set(EcomDevSample::getResult, s.getResult())
                .set(EcomDevSample::getEvaluation, s.getEvaluation())
                .set(EcomDevSample::getUpdateTime, LocalDateTime.now())
                .eq(EcomDevSample::getId, sampleId));
    }

    public void deleteSample(Long sampleId) {
        EcomDevSample s = sample(sampleId);
        checkSampleEditable(get(s.getProjectId()));
        sampleMapper.deleteById(sampleId);
    }

    private EcomDevSample sample(Long id) {
        EcomDevSample s = id == null ? null : sampleMapper.selectById(id);
        if (s == null) {
            throw new BizException("ecom.error.dev.sampleNotFound");
        }
        return s;
    }

    private static void checkSampleEditable(EcomDevProject p) {
        if (!p.getStatus().editable()) {
            throw new BizException("ecom.error.dev.notEditable", p.getDocNo());
        }
    }

    private void applySample(EcomDevSample s, DevSampleRequest r) {
        if (r.supplierId() == null && !StringUtils.hasText(r.supplierName())) {
            throw new BizException("ecom.error.dev.sampleSupplierRequired");
        }
        if (r.supplierId() != null) {
            supplierService.get(r.supplierId());
        }
        s.setSupplierId(r.supplierId());
        s.setSupplierName(StringUtils.hasText(r.supplierName()) ? r.supplierName().trim() : null);
        s.setSampleUrl(StringUtils.hasText(r.sampleUrl()) ? r.sampleUrl().trim() : null);
        s.setSampleQty(r.sampleQty());
        s.setSampleFee(r.sampleFee() == null ? null : Decimals.money(r.sampleFee()));
        s.setQuotePrice(r.quotePrice());
        s.setRequestDate(r.requestDate());
        s.setArrivalDate(r.arrivalDate());
        s.setResult(r.result() == null ? DevSampleResult.PENDING : r.result());
        s.setEvaluation(r.evaluation());
    }

    // ---------------------------------------------------------------- 其他

    /** 保存试算方案时回写项目的试算利润与利润率 */
    void updateEstimate(Long projectId, BigDecimal profit, BigDecimal margin) {
        projectMapper.update(Wrappers.<EcomDevProject>lambdaUpdate().set(EcomDevProject::getEstProfit, profit)
                .set(EcomDevProject::getEstMargin, margin).eq(EcomDevProject::getId, projectId));
    }

    private void log(Long projectId, DevLogAction action, DevProjectStatus from, DevProjectStatus to, String remark) {
        EcomDevLog l = new EcomDevLog();
        l.setProjectId(projectId);
        l.setAction(action);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setRemark(remark == null ? null : remark.length() > 500 ? remark.substring(0, 500) : remark);
        l.setCreateBy(SecurityUtils.getUserIdOrNull());
        logMapper.insert(l);
    }

    private static void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            r.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                r.run();
            }
        });
    }
}
