package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.dto.ReplenishSettingRequest;
import com.lemon.erp.modules.ecom.dto.ReplenishTransit;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.entity.EcomReplenishSetting;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.model.ReplenishScope;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import com.lemon.erp.modules.ecom.service.EcomReplenishSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 补货建议 /ecom/replenish：平台仓补货（按在线商品）、本地仓采购（按物料）、在途构成、补货设置。
 * 日均加权参数 w7 / w14 / w30 / w60（最近 7 / 14 / 30 / 60 天日均的权重），全部为空取默认 7 天 × 0.5 + 30 天 × 0.5
 */
@RestController
@RequestMapping("/ecom/replenish")
@RequiredArgsConstructor
public class EcomReplenishController {

    private final EcomReplenishService replenishService;
    private final EcomReplenishSettingService settingService;

    /** 平台仓补货建议 */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:replenish:list')")
    public R<List<ReplenishRow>> rows(@RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) ReplenishLevel level,
                                      @RequestParam(required = false) BigDecimal w7, @RequestParam(required = false) BigDecimal w14,
                                      @RequestParam(required = false) BigDecimal w30, @RequestParam(required = false) BigDecimal w60,
                                      @RequestParam(required = false) Boolean forecast) {
        return R.ok(replenishService.rows(storeId, keyword, level, ReplenishWeights.of(w7, w14, w30, w60).withForecast(forecast)));
    }

    /** 本地仓采购建议（物料，汇总全部店铺） */
    @GetMapping("/purchase")
    @PreAuthorize("@ss.hasPerm('ecom:replenish:list')")
    public R<List<PurchaseSuggestRow>> purchase(@RequestParam(required = false) String keyword, @RequestParam(required = false) ReplenishLevel level,
                                                @RequestParam(required = false) BigDecimal w7, @RequestParam(required = false) BigDecimal w14,
                                                @RequestParam(required = false) BigDecimal w30, @RequestParam(required = false) BigDecimal w60,
                                                @RequestParam(required = false) Boolean forecast) {
        return R.ok(replenishService.purchaseRows(keyword, level, ReplenishWeights.of(w7, w14, w30, w60).withForecast(forecast)));
    }

    /** 物料的在途构成：采购订单、头程货件（storeId 只看某店铺的货件） */
    @GetMapping("/transit")
    @PreAuthorize("@ss.hasPerm('ecom:replenish:list')")
    public R<ReplenishTransit> transit(@RequestParam Long materialId, @RequestParam(required = false) Long storeId) {
        return R.ok(replenishService.transit(materialId, storeId));
    }

    /** 补货设置（在线商品级传 listingId，物料级传 materialId），没有设置时返回空 */
    @GetMapping("/setting")
    @PreAuthorize("@ss.hasPerm('ecom:replenish:list')")
    public R<EcomReplenishSetting> setting(@RequestParam ReplenishScope scope, @RequestParam(required = false) Long listingId,
                                           @RequestParam(required = false) Long materialId) {
        return R.ok(settingService.find(scope, listingId, materialId));
    }

    /** 保存补货设置，全部项为空时删除设置（返回空） */
    @PutMapping("/setting")
    @PreAuthorize("@ss.hasPerm('ecom:replenish:setting')")
    @OperLog(title = "补货设置", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> saveSetting(@Validated @RequestBody ReplenishSettingRequest req) {
        return R.ok(settingService.save(req));
    }

    @DeleteMapping("/setting/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:replenish:setting')")
    @OperLog(title = "补货设置", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteSetting(@PathVariable Long id) {
        settingService.delete(id);
        return R.ok();
    }
}
