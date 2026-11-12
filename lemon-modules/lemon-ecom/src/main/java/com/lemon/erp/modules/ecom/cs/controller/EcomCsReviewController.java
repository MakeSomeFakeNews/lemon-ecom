package com.lemon.erp.modules.ecom.cs.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.modules.ecom.cs.dto.CsRequests;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.dto.FeedbackImportRow;
import com.lemon.erp.modules.ecom.cs.dto.ReviewImportRow;
import com.lemon.erp.modules.ecom.cs.dto.ReviewQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomFeedback;
import com.lemon.erp.modules.ecom.cs.entity.EcomReview;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import com.lemon.erp.modules.ecom.cs.service.EcomCsSyncService;
import com.lemon.erp.modules.ecom.cs.service.EcomReviewRequestService;
import com.lemon.erp.modules.ecom.cs.service.EcomReviewService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 评价管理 /ecom/cs/review：商品评价（列表、统计、导入、同步、回复、处理）、店铺反馈（/feedback）、请求评价（/request）
 */
@RestController
@RequestMapping("/ecom/cs/review")
@RequiredArgsConstructor
public class EcomCsReviewController {

    private final EcomReviewService reviewService;
    private final EcomCsSyncService syncService;
    private final EcomReviewRequestService requestService;
    private final ImportExecutor importExecutor;

    // ---------------------------------------------------------------- 商品评价

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<PageResult<EcomReview>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          @ModelAttribute ReviewQuery query) {
        return R.ok(reviewService.page(current, size, query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<EcomReview> detail(@PathVariable Long id) {
        return R.ok(reviewService.detail(id));
    }

    /** 概况：评价数、平均分、差评数 / 差评率（不超过 3 星）、待处理、已回复 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<CsViews.ReviewSummary> summary(@ModelAttribute ReviewQuery query) {
        return R.ok(reviewService.summary(query));
    }

    /** 按 SKU：评价数、平均分、差评数与差评率 */
    @GetMapping("/sku-stats")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<List<CsViews.SkuStat>> skuStats(@ModelAttribute ReviewQuery query) {
        return R.ok(reviewService.skuStats(query));
    }

    /** 按天的评分趋势（默认最近 30 天） */
    @GetMapping("/trend")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<List<CsViews.TrendPoint>> trend(@ModelAttribute ReviewQuery query) {
        return R.ok(reviewService.trend(query));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:import')")
    @OperLog(title = "商品评价", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<ReviewImportRow> req) {
        return R.ok(importExecutor.run(req, reviewService::importRow));
    }

    /** 手工同步店铺的商品评价（平台支持时） */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:sync')")
    @OperLog(title = "商品评价", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<CsViews.SyncOutcome> sync(@PathVariable Long storeId) {
        return R.ok(syncService.syncReviews(storeId));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:reply')")
    @OperLog(title = "商品评价", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> reply(@PathVariable Long id, @Valid @RequestBody CsRequests.ReviewReply req) {
        reviewService.reply(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:edit')")
    @OperLog(title = "商品评价", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody CsRequests.Handle req) {
        reviewService.handle(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:remove')")
    @OperLog(title = "商品评价", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        reviewService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 店铺反馈

    @GetMapping("/feedback")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<PageResult<EcomFeedback>> feedbackPage(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                    @ModelAttribute ReviewQuery query) {
        return R.ok(reviewService.feedbackPage(current, size, query));
    }

    @PostMapping("/feedback/import")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:import')")
    @OperLog(title = "店铺反馈", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importFeedback(@Valid @RequestBody ImportRequest<FeedbackImportRow> req) {
        return R.ok(importExecutor.run(req, reviewService::importFeedbackRow));
    }

    @PostMapping("/feedback/{id}/reply")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:reply')")
    @OperLog(title = "店铺反馈", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> replyFeedback(@PathVariable Long id, @Valid @RequestBody CsRequests.ReviewReply req) {
        reviewService.replyFeedback(id, req);
        return R.ok();
    }

    @PutMapping("/feedback/{id}/handle")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:edit')")
    @OperLog(title = "店铺反馈", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> handleFeedback(@PathVariable Long id, @Valid @RequestBody CsRequests.Handle req) {
        reviewService.handleFeedback(id, req);
        return R.ok();
    }

    @DeleteMapping("/feedback/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:remove')")
    @OperLog(title = "店铺反馈", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteFeedback(@PathVariable List<Long> ids) {
        reviewService.deleteFeedback(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 请求评价

    /** 可请求评价的订单（发货 5–30 天）：pendingOnly 只列还没有成功请求过的，status 按请求结果筛选 */
    @GetMapping("/request")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:list')")
    public R<PageResult<CsViews.RequestCandidate>> candidates(@RequestParam(defaultValue = "1") long current,
                                                              @RequestParam(defaultValue = "10") long size,
                                                              @RequestParam(required = false) EcomPlatform platform,
                                                              @RequestParam(required = false) Long storeId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "false") boolean pendingOnly,
                                                              @RequestParam(required = false) CsRequestStatus status) {
        return R.ok(requestService.candidates(current, size, platform, storeId, keyword, pendingOnly, status));
    }

    @PostMapping("/request")
    @PreAuthorize("@ss.hasPerm('ecom:csReview:request')")
    @OperLog(title = "请求评价", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<CsViews.RequestOutcome>> request(@Valid @RequestBody CsRequests.ReviewRequest req) {
        return R.ok(requestService.request(req.orderIds()));
    }
}
