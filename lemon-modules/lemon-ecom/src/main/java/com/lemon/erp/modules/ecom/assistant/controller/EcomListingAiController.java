package com.lemon.erp.modules.ecom.assistant.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.service.EcomListingAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 写 Listing /ecom/ai/listing-copy：按刊登记录生成 / 优化标题、五点、描述（结果只返回给页面，确认后填入表单），关键词建议
 */
@RestController
@RequestMapping("/ecom/ai/listing-copy")
@RequiredArgsConstructor
public class EcomListingAiController {

    private final EcomListingAiService listingAiService;

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:publish:ai')")
    public R<AssistantViews.ListingCopy> generate(@Valid @RequestBody AssistantRequests.ListingAiRequest req) {
        return R.ok(listingAiService.generate(req));
    }

    /** 高转化搜索词：同物料（sameCategory 时扩大到同品类） */
    @GetMapping("/keywords")
    @PreAuthorize("@ss.hasPerm('ecom:publish:ai')")
    public R<AssistantViews.KeywordResult> keywords(@RequestParam Long itemId, @RequestParam(defaultValue = "false") boolean sameCategory,
                                                    @RequestParam(required = false) Integer days) {
        return R.ok(listingAiService.keywords(itemId, sameCategory, days));
    }
}
