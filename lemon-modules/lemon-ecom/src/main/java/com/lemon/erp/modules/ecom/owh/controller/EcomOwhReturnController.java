package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.IdsRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ReturnCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.VoidRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhReturn;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海外仓退件 /ecom/owh/return：从 ERP 退货退款单生成、推送、同步收货质检结果（ERP 退货入库，不良品转仓 / 报废）、取消
 */
@RestController
@RequestMapping("/ecom/owh/return")
@RequiredArgsConstructor
public class EcomOwhReturnController {

    private final EcomOwhReturnService returnService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:list')")
    public R<PageResult<EcomOwhReturn>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             Long accountId, Long warehouseId, OwhReturnStatus status, String keyword) {
        return R.ok(returnService.page(current, size, accountId, warehouseId, status, keyword));
    }

    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:list')")
    public R<Map<String, Long>> statusCounts(Long accountId, Long warehouseId, String keyword) {
        return R.ok(returnService.statusCounts(accountId, warehouseId, keyword));
    }

    /** 可生成退件单的 ERP 退货退款单（退回仓是海外仓、待处理） */
    @GetMapping("/sources")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:add')")
    public R<List<Map<String, Object>>> sources(String keyword) {
        return R.ok(returnService.sources(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:list')")
    public R<EcomOwhReturn> get(@PathVariable Long id) {
        return R.ok(returnService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:add')")
    @OperLog(title = "海外仓退件单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody ReturnCreateRequest req) {
        return R.ok(returnService.create(req));
    }

    @PostMapping("/{id}/push")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:push')")
    @OperLog(title = "海外仓退件单推送", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> push(@PathVariable Long id) {
        returnService.push(id);
        return R.ok();
    }

    /** 同步（批量），返回 退件单 ID → 失败原因 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:sync')")
    @OperLog(title = "海外仓退件单同步", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> sync(@Validated @RequestBody IdsRequest req) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (Long id : req.ids()) {
            try {
                returnService.sync(id);
            } catch (BizException e) {
                failures.put(String.valueOf(id), e.getMessage());
            }
        }
        return R.ok(failures);
    }

    /** 标记已作废：服务商后台已作废（没有取消接口的服务商） */
    @PostMapping("/{id}/void")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:cancel')")
    @OperLog(title = "海外仓退件单作废", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> markVoided(@PathVariable Long id, @Validated @RequestBody(required = false) VoidRequest req) {
        returnService.markVoided(id, req == null ? null : req.reason());
        return R.ok();
    }

    /** 取消：草稿直接取消，已推送的调用服务商取消接口 */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:owhReturn:cancel')")
    @OperLog(title = "海外仓退件单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        returnService.cancel(id);
        return R.ok();
    }
}
