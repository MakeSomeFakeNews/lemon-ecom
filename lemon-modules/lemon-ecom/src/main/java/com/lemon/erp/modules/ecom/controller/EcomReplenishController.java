package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 补货建议 /ecom/replenish
 */
@RestController
@RequestMapping("/ecom/replenish")
@RequiredArgsConstructor
public class EcomReplenishController {

    private final EcomReplenishService replenishService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:replenish:list')")
    public R<List<ReplenishRow>> rows(@RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) ReplenishLevel level) {
        return R.ok(replenishService.rows(storeId, keyword, level));
    }
}
