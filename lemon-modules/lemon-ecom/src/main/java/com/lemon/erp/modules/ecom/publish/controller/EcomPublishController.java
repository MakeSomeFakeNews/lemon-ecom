package com.lemon.erp.modules.ecom.publish.controller;

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
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.connector.PlatformAttribute;
import com.lemon.erp.modules.ecom.publish.connector.PlatformCategory;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishDraft;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishDraftService;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishService;

import java.util.List;
import java.util.Map;

/**
 * 刊登中心 /ecom/publish：刊登记录列表与状态数量、草稿（从物料 / SPU 创建、增加店铺、主内容应用到店铺）、
 * 单店内容修改与建议价重算、提交发布 / 重试 / 下架、日志，以及按店铺查询平台类目与属性
 */
@RestController
@RequestMapping("/ecom/publish")
@RequiredArgsConstructor
public class EcomPublishController {

    private static final String TITLE = "刊登中心"; // i18n-ignore 操作日志标题存库

    private final EcomPublishDraftService draftService;
    private final EcomPublishService publishService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<PageResult<EcomPublishItem>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long storeId, @RequestParam(required = false) EcomPlatform platform,
                                               @RequestParam(required = false) PublishStatus status, @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long draftId) {
        return R.ok(draftService.page(current, size, storeId, platform, status, keyword, draftId));
    }

    @GetMapping("/counts")
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<Map<String, Long>> counts(@RequestParam(required = false) Long storeId, @RequestParam(required = false) EcomPlatform platform,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(draftService.counts(storeId, platform, keyword));
    }

    @GetMapping("/draft/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<EcomPublishDraft> draft(@PathVariable Long id) {
        return R.ok(draftService.getDraft(id));
    }

    @PostMapping("/draft")
    @PreAuthorize("@ss.hasPerm('ecom:publish:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> createDraft(@Valid @RequestBody PublishRequests.CreateDraft req) {
        return R.ok(draftService.createDraft(req));
    }

    @PutMapping("/draft/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateDraft(@PathVariable Long id, @Valid @RequestBody EcomPublishDraft draft) {
        draftService.updateDraft(id, draft);
        return R.ok();
    }

    @PostMapping("/draft/{id}/stores")
    @PreAuthorize("@ss.hasPerm('ecom:publish:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Integer> addStores(@PathVariable Long id, @Valid @RequestBody PublishRequests.AddStores req) {
        return R.ok(draftService.addStores(id, req));
    }

    /** 主内容应用到可修改的刊登记录 */
    @PostMapping("/draft/{id}/apply")
    @PreAuthorize("@ss.hasPerm('ecom:publish:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Integer> applyMaster(@PathVariable Long id, @RequestBody(required = false) PublishRequests.ApplyMaster req) {
        return R.ok(draftService.applyMaster(id, req == null ? null : req.fields()));
    }

    /** 物料（含 SPU 下各 SKU）的图片 */
    @GetMapping("/material-images/{materialId}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<List<String>> materialImages(@PathVariable Long materialId) {
        return R.ok(draftService.materialImages(materialId));
    }

    @GetMapping("/item/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<EcomPublishItem> item(@PathVariable Long id) {
        return R.ok(draftService.getItem(id));
    }

    @PutMapping("/item/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateItem(@PathVariable Long id, @Valid @RequestBody EcomPublishItem item) {
        draftService.updateItem(id, item);
        return R.ok();
    }

    /** 按价格模板重新计算建议价并改为建议价 */
    @PostMapping("/item/{id}/recalc")
    @PreAuthorize("@ss.hasPerm('ecom:publish:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<EcomPublishItem> recalc(@PathVariable Long id) {
        return R.ok(draftService.recalc(id));
    }

    @DeleteMapping("/item/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:publish:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        draftService.deleteItems(ids);
        return R.ok();
    }

    @GetMapping("/item/{id}/logs")
    @PreAuthorize("@ss.hasPerm('ecom:publish:list')")
    public R<List<EcomPublishLog>> logs(@PathVariable Long id) {
        return R.ok(publishService.logs(id));
    }

    /** 提交发布 / 重试（失败的重新提交），后台执行 */
    @PostMapping("/submit")
    @PreAuthorize("@ss.hasPerm('ecom:publish:push')")
    @OperLog(title = TITLE, type = OperType.OTHER)
    public R<PublishViews.ActionResult> submit(@Valid @RequestBody IdsRequest req) {
        return R.ok(publishService.submit(req.ids()));
    }

    @PostMapping("/offline")
    @PreAuthorize("@ss.hasPerm('ecom:publish:push')")
    @OperLog(title = TITLE, type = OperType.OTHER)
    public R<PublishViews.ActionResult> offline(@Valid @RequestBody IdsRequest req) {
        return R.ok(publishService.offline(req.ids()));
    }

    /** 刊登设置：是否配置了后端外网地址（图片公网地址） */
    @GetMapping("/settings")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<PublishViews.Settings> settings() {
        return R.ok(publishService.settings());
    }

    /** 店铺的刊登能力（是否能查类目 / 属性、发布、下架、改价） */
    @GetMapping("/capabilities")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<PublishViews.StoreCapabilities> capabilities(@RequestParam Long storeId) {
        return R.ok(publishService.capabilities(storeId));
    }

    @GetMapping("/categories")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<List<PlatformCategory>> categories(@RequestParam Long storeId, @RequestParam(required = false) String parentId,
                                                @RequestParam(required = false) String keyword) {
        return R.ok(publishService.categories(storeId, parentId, keyword));
    }

    @GetMapping("/attributes")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<List<PlatformAttribute>> attributes(@RequestParam Long storeId, @RequestParam String categoryId) {
        return R.ok(publishService.attributes(storeId, categoryId));
    }
}
