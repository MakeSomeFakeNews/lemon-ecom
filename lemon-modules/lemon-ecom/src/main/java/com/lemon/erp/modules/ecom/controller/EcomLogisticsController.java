package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.LogisticsEstimate;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
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
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物流渠道与运费试算 /ecom/logistics
 */
@RestController
@RequestMapping("/ecom/logistics")
@RequiredArgsConstructor
public class EcomLogisticsController {

    private final EcomLogisticsService logisticsService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:logistics:list')")
    public R<PageResult<EcomLogisticsChannel>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword, @RequestParam(required = false) EnableStatus status,
                                                    @RequestParam(required = false) String transportMode) {
        return R.ok(logisticsService.page(current, size, keyword, status, transportMode));
    }

    /** 登录即可访问：发货、货件选择渠道 */
    @GetMapping("/options")
    public R<List<EcomLogisticsChannel>> options() {
        return R.ok(logisticsService.options());
    }

    /** 运费试算：按目的国和重量（kg）列出各渠道运费 */
    @GetMapping("/estimate")
    public R<List<LogisticsEstimate>> estimate(@RequestParam String country, @RequestParam BigDecimal weight) {
        return R.ok(logisticsService.estimate(country, weight));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:logistics:list')")
    public R<EcomLogisticsChannel> get(@PathVariable Long id) {
        return R.ok(logisticsService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:logistics:add')")
    @OperLog(title = "物流渠道", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated(Create.class) @RequestBody EcomLogisticsChannel channel) {
        return R.ok(logisticsService.create(channel));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:logistics:edit')")
    @OperLog(title = "物流渠道", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody EcomLogisticsChannel channel) {
        logisticsService.update(id, channel);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:logistics:edit')")
    @OperLog(title = "物流渠道", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        logisticsService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:logistics:remove')")
    @OperLog(title = "物流渠道", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        logisticsService.delete(ids);
        return R.ok();
    }
}
