package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomOrderSetting;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.service.EcomOrderSettingService;
import com.lemon.erp.modules.ecom.service.EcomTrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 物流跟踪 /ecom/track：已发货包裹的轨迹列表（状态、预警标签页）、时间线、手工拉取；订单作业设置（地址校验开关、17TRACK、预警时限）
 */
@RestController
@RequestMapping("/ecom/track")
@RequiredArgsConstructor
public class EcomTrackController {

    private final EcomTrackService trackService;
    private final EcomOrderSettingService settingService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:track:list')")
    public R<PageResult<EcomPackage>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           OrderEnhanceViews.TrackQuery query) {
        return R.ok(trackService.page(current, size, query));
    }

    /** 各轨迹状态 / 预警的包裹数：{ "S:状态": n, "A:预警": n } */
    @GetMapping("/counts")
    @PreAuthorize("@ss.hasPerm('ecom:track:list')")
    public R<Map<String, Long>> counts(OrderEnhanceViews.TrackQuery query) {
        return R.ok(trackService.counts(query));
    }

    @GetMapping("/{packageId}")
    @PreAuthorize("@ss.hasAnyPerm('ecom:track:list', 'ecom:order:list', 'ecom:package:list')")
    public R<OrderEnhanceViews.PackageTracks> tracks(@PathVariable Long packageId) {
        return R.ok(trackService.tracks(packageId));
    }

    /** 拉取一个包裹的轨迹（失败时提示原因） */
    @PostMapping("/{packageId}/pull")
    @PreAuthorize("@ss.hasPerm('ecom:track:sync')")
    public R<OrderEnhanceViews.PackageTracks> pull(@PathVariable Long packageId) {
        return R.ok(trackService.pull(packageId));
    }

    /** 批量拉取，返回 { 包裹号: 失败原因 } */
    @PostMapping("/pull")
    @PreAuthorize("@ss.hasPerm('ecom:track:sync')")
    public R<Map<String, String>> pull(@Validated @RequestBody IdsRequest req) {
        return R.ok(trackService.pull(req.ids()));
    }

    @GetMapping("/setting")
    @PreAuthorize("@ss.hasAnyPerm('ecom:track:list', 'ecom:track:setting')")
    public R<EcomOrderSetting> setting() {
        return R.ok(settingService.get());
    }

    @PutMapping("/setting")
    @PreAuthorize("@ss.hasPerm('ecom:track:setting')")
    @OperLog(title = "订单作业设置", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> saveSetting(@Validated @RequestBody OrderEnhanceViews.SettingRequest req) {
        settingService.save(req);
        return R.ok();
    }
}
