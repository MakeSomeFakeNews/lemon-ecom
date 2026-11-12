package com.lemon.erp.modules.ecom.oper.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.model.EcomAchievementDimension;
import com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType;
import com.lemon.erp.modules.ecom.oper.service.EcomOpsTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营目标与绩效 /ecom/oper/target：月度目标设置、达成率、运营人员绩效排行
 */
@RestController
@RequestMapping("/ecom/oper/target")
@RequiredArgsConstructor
public class EcomOpsTargetController {

    private final EcomOpsTargetService targetService;

    /** 目标设置：对象列表与本月目标、上月实际 */
    @GetMapping("/edit")
    @PreAuthorize("@ss.hasPerm('ecom:opsTarget:list')")
    public R<List<OperViews.TargetEditRow>> editRows(@RequestParam String month, @RequestParam EcomOpsTargetType targetType) {
        return R.ok(targetService.editRows(month, targetType));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPerm('ecom:opsTarget:edit')")
    @OperLog(title = "运营目标", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> save(@Valid @RequestBody OperRequests.TargetSaveRequest req) {
        targetService.save(req);
        return R.ok();
    }

    /** 复制上月目标（已设置的不覆盖），返回复制的条数 */
    @PostMapping("/copy")
    @PreAuthorize("@ss.hasPerm('ecom:opsTarget:edit')")
    @OperLog(title = "运营目标", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Integer> copy(@Valid @RequestBody OperRequests.TargetCopyRequest req) {
        return R.ok(targetService.copyLastMonth(req));
    }

    /** 月度达成率：按店铺、负责人或平台 */
    @GetMapping("/achievement")
    @PreAuthorize("@ss.hasPerm('ecom:opsTarget:list') or @ss.hasPerm('ecom:opsPerf:list')")
    public R<OperViews.Achievement> achievement(@RequestParam String month,
                                                @RequestParam(defaultValue = "STORE") EcomAchievementDimension dimension) {
        return R.ok(targetService.achievement(month, dimension));
    }

    /** 运营人员绩效排行 */
    @GetMapping("/performance")
    @PreAuthorize("@ss.hasPerm('ecom:opsPerf:list')")
    public R<List<OperViews.PerformanceRow>> performance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(targetService.performance(beginDate, endDate));
    }
}
