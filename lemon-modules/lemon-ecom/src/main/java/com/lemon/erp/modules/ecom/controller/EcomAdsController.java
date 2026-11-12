package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.ads.AdsProfile;
import com.lemon.erp.modules.ecom.dto.AdNegateRequest;
import com.lemon.erp.modules.ecom.dto.AdReportImportRequest;
import com.lemon.erp.modules.ecom.dto.AdValueRequest;
import com.lemon.erp.modules.ecom.dto.AdsAuthRequest;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.service.EcomAdAnalysisService;
import com.lemon.erp.modules.ecom.service.EcomAdChangeService;
import com.lemon.erp.modules.ecom.service.EcomAdIngestService;
import com.lemon.erp.modules.ecom.service.EcomAdScheduleService;
import com.lemon.erp.modules.ecom.service.EcomAdStructureService;
import com.lemon.erp.modules.ecom.service.EcomAdsAuthService;
import com.lemon.erp.modules.ecom.service.EcomAdsSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 广告管理 /ecom/ads：广告结构（活动 / 广告组 / 投放）、竞价 / 预算 / 状态修改、报告导入、分析（概览、趋势、层级报表、搜索词、TACOS）、
 * 广告授权与同步、分时策略。旧版广告日报导入与汇总仍在 /ecom/ad
 */
@RestController
@RequestMapping("/ecom/ads")
@RequiredArgsConstructor
public class EcomAdsController {

    private static final String TITLE = "广告管理"; // i18n-ignore 操作日志标题存库

    private final EcomAdStructureService structureService;
    private final EcomAdChangeService changeService;
    private final EcomAdIngestService ingestService;
    private final EcomAdAnalysisService analysisService;
    private final EcomAdsAuthService authService;
    private final EcomAdsSyncService syncService;
    private final EcomAdScheduleService scheduleService;

    // ---------------------------------------------------------------- 活动 / 广告组 / 投放

    @GetMapping("/campaigns")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<PageResult<EcomAdCampaign>> campaigns(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) EcomAdStatus status, @RequestParam(required = false) EcomAdType adType) {
        return R.ok(structureService.page(current, size, storeId, keyword, status, adType));
    }

    @GetMapping("/campaigns/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<EcomAdCampaign> campaign(@PathVariable Long id) {
        return R.ok(structureService.detail(id));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> createCampaign(@Valid @RequestBody EcomAdCampaign c) {
        return R.ok(structureService.createCampaign(c));
    }

    @PutMapping("/campaigns/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateCampaign(@PathVariable Long id, @Valid @RequestBody EcomAdCampaign c) {
        structureService.updateCampaign(id, c);
        return R.ok();
    }

    @DeleteMapping("/campaigns/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> deleteCampaigns(@PathVariable List<Long> ids) {
        structureService.deleteCampaigns(ids);
        return R.ok();
    }

    @GetMapping("/campaigns/{id}/groups")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<EcomAdGroup>> groups(@PathVariable Long id) {
        return R.ok(structureService.groups(id));
    }

    @PostMapping("/groups")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> createGroup(@Valid @RequestBody EcomAdGroup g) {
        return R.ok(structureService.createGroup(g));
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateGroup(@PathVariable Long id, @Valid @RequestBody EcomAdGroup g) {
        structureService.updateGroup(id, g);
        return R.ok();
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> deleteGroup(@PathVariable Long id) {
        structureService.deleteGroup(id);
        return R.ok();
    }

    @GetMapping("/targets")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<PageResult<EcomAdTarget>> targets(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long campaignId, @RequestParam(required = false) Long adGroupId,
                                               @RequestParam(required = false) String keyword, @RequestParam(required = false) EcomAdStatus status,
                                               @RequestParam(required = false) EcomAdTargetType targetType) {
        return R.ok(structureService.targets(current, size, campaignId, adGroupId, keyword, status, targetType));
    }

    @PostMapping("/targets")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> createTarget(@Valid @RequestBody EcomAdTarget t) {
        return R.ok(structureService.createTarget(t));
    }

    @PutMapping("/targets/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateTarget(@PathVariable Long id, @Valid @RequestBody EcomAdTarget t) {
        structureService.updateTarget(id, t);
        return R.ok();
    }

    @DeleteMapping("/targets/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> deleteTargets(@PathVariable List<Long> ids) {
        structureService.deleteTargets(ids);
        return R.ok();
    }

    /** 修改竞价：level = TARGET（基准竞价）/ AD_GROUP（默认竞价），返回是否已同步到平台 */
    @PutMapping("/{level}/{id}/bid")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Boolean> changeBid(@PathVariable EcomAdLevel level, @PathVariable Long id, @Valid @RequestBody AdValueRequest req) {
        return R.ok(changeService.changeBid(level, id, req.value()));
    }

    @PutMapping("/campaigns/{id}/budget")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Boolean> changeBudget(@PathVariable Long id, @Valid @RequestBody AdValueRequest req) {
        return R.ok(changeService.changeBudget(id, req.value()));
    }

    @PutMapping("/{level}/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Boolean> changeStatus(@PathVariable EcomAdLevel level, @PathVariable Long id, @RequestParam EcomAdStatus status) {
        return R.ok(changeService.changeStatus(level, id, status));
    }

    // ---------------------------------------------------------------- 报告导入

    @PostMapping("/report/import")
    @PreAuthorize("@ss.hasPerm('ecom:ad:import')")
    @OperLog(title = TITLE, type = OperType.IMPORT)
    public R<AdsViews.ImportResult> importReport(@Valid @RequestBody AdReportImportRequest req) {
        return R.ok(ingestService.importFile(req));
    }

    // ---------------------------------------------------------------- 分析

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<AdsViews.Overview> overview(@RequestParam(required = false) Long storeId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analysisService.overview(storeId, beginDate, endDate));
    }

    @GetMapping("/trend")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<AdsViews.TrendRow>> trend(@RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(analysisService.trend(storeId, beginDate, endDate));
    }

    /** 层级报表：level = CAMPAIGN（可按店铺、关键字）/ AD_GROUP（campaignId）/ TARGET（adGroupId 或 campaignId） */
    @GetMapping("/hierarchy")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<AdsViews.Hierarchy> hierarchy(@RequestParam(defaultValue = "CAMPAIGN") EcomAdLevel level, @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) Long campaignId, @RequestParam(required = false) Long adGroupId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.hierarchy(level, storeId, campaignId, adGroupId, beginDate, endDate, keyword));
    }

    @GetMapping("/search-terms")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<AdsViews.SearchTermRow>> searchTerms(@RequestParam(required = false) Long storeId, @RequestParam(required = false) Long campaignId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                       @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "false") boolean wasteOnly,
                                                       @RequestParam(required = false) Integer minClicks, @RequestParam(required = false) BigDecimal minSpend) {
        return R.ok(analysisService.searchTerms(storeId, campaignId, beginDate, endDate, keyword, wasteOnly, minClicks, minSpend));
    }

    @PostMapping("/search-terms/negate")
    @PreAuthorize("@ss.hasPerm('ecom:ad:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<EcomAdChangeService.NegateResult> negate(@Valid @RequestBody AdNegateRequest req) {
        return R.ok(changeService.negate(req));
    }

    @GetMapping("/tacos")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<AdsViews.TacosRow>> tacos(@RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.tacos(storeId, beginDate, endDate, keyword));
    }

    // ---------------------------------------------------------------- 授权与同步

    @GetMapping("/auth/connectors")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<AdsViews.ConnectorView>> connectors() {
        return R.ok(authService.connectors());
    }

    @GetMapping("/auth/stores")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<AdsViews.StoreAuth>> authStores() {
        return R.ok(authService.stores());
    }

    @PutMapping("/auth/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> saveAuth(@PathVariable Long storeId, @Valid @RequestBody AdsAuthRequest req) {
        authService.save(storeId, req);
        return R.ok();
    }

    @DeleteMapping("/auth/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> clearAuth(@PathVariable Long storeId) {
        authService.clear(storeId);
        return R.ok();
    }

    @PostMapping("/auth/{storeId}/test")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    public R<Void> testAuth(@PathVariable Long storeId) {
        authService.test(storeId);
        return R.ok();
    }

    @GetMapping("/auth/{storeId}/profiles")
    @PreAuthorize("@ss.hasPerm('ecom:ad:auth')")
    public R<List<AdsProfile>> profiles(@PathVariable Long storeId) {
        return R.ok(authService.profiles(storeId));
    }

    /** 同步结构与最近 days 天的报告（后台执行，结果见广告授权中的同步时间 / 失败原因） */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:sync')")
    public R<Void> sync(@PathVariable Long storeId, @RequestParam(defaultValue = "7") int days) {
        authService.requireConnector(structureStore(storeId));
        syncService.syncAsync(storeId, days);
        return R.ok();
    }

    /** 只同步结构（立即返回结果） */
    @PostMapping("/sync/{storeId}/structure")
    @PreAuthorize("@ss.hasPerm('ecom:ad:sync')")
    public R<AdsViews.SyncResult> syncStructure(@PathVariable Long storeId) {
        return R.ok(syncService.syncStructure(storeId));
    }

    private com.lemon.erp.modules.ecom.entity.EcomStore structureStore(Long storeId) {
        return authService.storeEntity(storeId);
    }

    // ---------------------------------------------------------------- 分时策略

    @GetMapping("/schedules")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<EcomAdSchedule>> schedules(@RequestParam(required = false) Long storeId) {
        return R.ok(scheduleService.list(storeId));
    }

    @GetMapping("/schedules/campaign/{campaignId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<EcomAdSchedule> schedule(@PathVariable Long campaignId) {
        return R.ok(scheduleService.byCampaign(campaignId));
    }

    @PutMapping("/schedules/campaign/{campaignId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:schedule')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<EcomAdScheduleService.ApplyResult> saveSchedule(@PathVariable Long campaignId, @Valid @RequestBody EcomAdSchedule s) {
        return R.ok(scheduleService.save(campaignId, s));
    }

    @DeleteMapping("/schedules/campaign/{campaignId}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:schedule')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<EcomAdScheduleService.ApplyResult> deleteSchedule(@PathVariable Long campaignId) {
        return R.ok(scheduleService.delete(campaignId));
    }

    @PostMapping("/schedules/campaign/{campaignId}/apply")
    @PreAuthorize("@ss.hasPerm('ecom:ad:schedule')")
    public R<EcomAdScheduleService.ApplyResult> applySchedule(@PathVariable Long campaignId) {
        return R.ok(scheduleService.applyNow(campaignId));
    }
}
