package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dev.dto.DevBoard;
import com.lemon.erp.modules.ecom.dev.dto.DevMaterialRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevMoveRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectQuery;
import com.lemon.erp.modules.ecom.dev.dto.DevProjectRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevRemarkRequest;
import com.lemon.erp.modules.ecom.dev.dto.DevSampleRequest;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import com.lemon.erp.modules.ecom.dev.service.EcomDevProjectService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 产品开发项目 /ecom/dev/project：列表、看板、详情、资料维护、状态流转、提交 / 撤回审批、手工审批、生成物料、图片、样品
 */
@RestController
@RequestMapping("/ecom/dev/project")
@RequiredArgsConstructor
public class EcomDevProjectController {

    private static final String TITLE = "开发项目"; // i18n-ignore 操作日志标题存库

    private final EcomDevProjectService projectService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:devProject:list')")
    public R<PageResult<EcomDevProject>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) DevProjectStatus status, @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long ownerId, @RequestParam(required = false) EcomPlatform platform,
                                              @RequestParam(required = false) String source, @RequestParam(required = false) String category) {
        return R.ok(projectService.page(current, size, new DevProjectQuery(status, keyword, ownerId, platform, source, category)));
    }

    /** 看板：每个状态一列 */
    @GetMapping("/board")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:list')")
    public R<DevBoard> board(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long ownerId,
                             @RequestParam(required = false) EcomPlatform platform, @RequestParam(required = false) String source,
                             @RequestParam(required = false) String category) {
        return R.ok(projectService.board(new DevProjectQuery(null, keyword, ownerId, platform, source, category)));
    }

    /** 详情：含 samples、logs */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:list')")
    public R<EcomDevProject> get(@PathVariable Long id) {
        return R.ok(projectService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:devProject:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Validated @RequestBody DevProjectRequest req) {
        return R.ok(projectService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody DevProjectRequest req) {
        projectService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        projectService.delete(ids);
        return R.ok();
    }

    /** 手工流转（看板拖拽 / 按钮） */
    @PostMapping("/{id}/move")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> move(@PathVariable Long id, @Validated @RequestBody DevMoveRequest req) {
        projectService.move(id, req.status(), req.remark());
        return R.ok();
    }

    /** 提交审批，返回是否发起了审批流程 */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Boolean> submit(@PathVariable Long id) {
        return R.ok(projectService.submit(id));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> withdraw(@PathVariable Long id) {
        projectService.withdraw(id);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:approve')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> approve(@PathVariable Long id, @Validated @RequestBody(required = false) DevRemarkRequest req) {
        projectService.approve(id, req == null ? null : req.remark());
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:approve')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> reject(@PathVariable Long id, @Validated @RequestBody(required = false) DevRemarkRequest req) {
        projectService.reject(id, req == null ? null : req.remark());
        return R.ok();
    }

    /** 生成物料（已通过且还没有物料，或物料已被删除），返回物料 ID */
    @PostMapping("/{id}/material")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:material')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> material(@PathVariable Long id, @Validated @RequestBody(required = false) DevMaterialRequest req) {
        return R.ok(projectService.generateMaterial(id, req));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    public R<String> uploadImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return R.ok(projectService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/images")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    public R<Void> deleteImage(@PathVariable Long id, @RequestParam String url) {
        projectService.deleteImage(id, url);
        return R.ok();
    }

    @PutMapping("/{id}/images/main")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    public R<Void> mainImage(@PathVariable Long id, @RequestParam String url) {
        projectService.setMainImage(id, url);
        return R.ok();
    }

    @PostMapping("/{id}/samples")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> addSample(@PathVariable Long id, @Validated @RequestBody DevSampleRequest req) {
        return R.ok(projectService.addSample(id, req));
    }

    @PutMapping("/samples/{sampleId}")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> updateSample(@PathVariable Long sampleId, @Validated @RequestBody DevSampleRequest req) {
        projectService.updateSample(sampleId, req);
        return R.ok();
    }

    @DeleteMapping("/samples/{sampleId}")
    @PreAuthorize("@ss.hasPerm('ecom:devProject:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> deleteSample(@PathVariable Long sampleId) {
        projectService.deleteSample(sampleId);
        return R.ok();
    }
}
