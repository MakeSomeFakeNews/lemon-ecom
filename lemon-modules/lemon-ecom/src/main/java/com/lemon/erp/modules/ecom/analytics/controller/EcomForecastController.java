package com.lemon.erp.modules.ecom.analytics.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.analytics.dto.ForecastDetail;
import com.lemon.erp.modules.ecom.analytics.dto.GenerateResult;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastEvent;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSetting;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSku;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;
import com.lemon.erp.modules.ecom.analytics.service.EcomForecastService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 销量预测 /ecom/analytics/forecast：预测列表、单品预测详情（历史 + 各方法预测 + 区间 + 回测）、手工指定方法、立即生成、
 * 预测参数（α / β / 窗口 / 星期周数）、促销 / 节假日系数日历
 */
@RestController
@RequestMapping("/ecom/analytics/forecast")
@RequiredArgsConstructor
public class EcomForecastController {

    private final EcomForecastService forecastService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:forecast:list')")
    public R<PageResult<EcomForecastSku>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) ForecastMethod method) {
        return R.ok(forecastService.page(current, size, storeId, keyword, method));
    }

    @GetMapping("/{listingId}")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:list')")
    public R<ForecastDetail> detail(@PathVariable Long listingId) {
        return R.ok(forecastService.detail(listingId));
    }

    /** 手工指定预测方法：{ method }，为空恢复自动选择 */
    @PutMapping("/{listingId}/method")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:edit')")
    @OperLog(title = "指定预测方法", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> fixMethod(@PathVariable Long listingId, @RequestBody(required = false) MethodRequest body) {
        forecastService.fixMethod(listingId, body == null ? null : body.method());
        return R.ok();
    }

    /** 指定的预测方法，为空恢复自动选择 */
    public record MethodRequest(ForecastMethod method) {
    }

    /** 立即生成（storeId 为空时全部在售 SKU） */
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:generate')")
    @OperLog(title = "生成销量预测", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<GenerateResult> generate(@RequestParam(required = false) Long storeId) {
        return R.ok(forecastService.generate(storeId));
    }

    @GetMapping("/setting")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:list')")
    public R<EcomForecastSetting> setting() {
        return R.ok(forecastService.setting());
    }

    @PutMapping("/setting")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:edit')")
    @OperLog(title = "预测参数", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> saveSetting(@Validated @RequestBody EcomForecastSetting setting) {
        forecastService.saveSetting(setting);
        return R.ok();
    }

    /** 促销 / 节假日（与 [from, to] 有重叠的，都为空时全部） */
    @GetMapping("/event")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:list')")
    public R<List<EcomForecastEvent>> events(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false) Long storeId) {
        return R.ok(forecastService.events(from, to, storeId));
    }

    @PostMapping("/event")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:event')")
    @OperLog(title = "预测事件", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> addEvent(@Validated @RequestBody EcomForecastEvent event) {
        return R.ok(forecastService.saveEvent(null, event));
    }

    @PutMapping("/event/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:event')")
    @OperLog(title = "预测事件", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> updateEvent(@PathVariable Long id, @Validated @RequestBody EcomForecastEvent event) {
        return R.ok(forecastService.saveEvent(id, event));
    }

    @DeleteMapping("/event/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:forecast:event')")
    @OperLog(title = "预测事件", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteEvent(@PathVariable Long id) {
        forecastService.deleteEvent(id);
        return R.ok();
    }
}
