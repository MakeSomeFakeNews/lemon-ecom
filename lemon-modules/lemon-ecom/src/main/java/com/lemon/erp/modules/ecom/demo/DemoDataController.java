package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示数据 /ecom/demo：生成（异步）与进度
 */
@RestController
@RequestMapping("/ecom/demo")
@RequiredArgsConstructor
public class DemoDataController {

    private final DemoDataService demoDataService;
    private final DemoDataJob demoDataJob;

    /** 进度（电商看板没有数据时显示生成入口） */
    @GetMapping("/status")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<DemoStatus> status() {
        return R.ok(demoDataService.status());
    }

    /** 开始生成（后台执行，已生成过或正在生成时报错），返回当前进度 */
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPerm('ecom:demo:generate')")
    @OperLog(title = "电商演示数据", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<DemoStatus> generate() {
        demoDataService.begin();
        try {
            demoDataJob.generateAsync(SecurityUtils.getLoginUser());
        } catch (RuntimeException e) {
            demoDataService.abort(e.getMessage());
            throw e;
        }
        return R.ok(demoDataService.status());
    }
}
