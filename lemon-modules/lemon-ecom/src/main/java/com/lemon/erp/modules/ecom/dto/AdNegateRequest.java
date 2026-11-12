package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 把搜索词添加为广告组的否定关键词（否定精准 / 否定词组）
 */
@Data
public class AdNegateRequest {

    /** NEGATIVE_EXACT（默认）或 NEGATIVE_PHRASE */
    private EcomAdMatchType matchType;

    @Valid
    @NotEmpty(message = "{ecom.valid.ads.negateItemsRequired}")
    @Size(max = 200, message = "{ecom.valid.ads.negateItemsRequired}")
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "{ecom.valid.ads.adGroupRequired}")
        private Long adGroupId;
        @NotBlank(message = "{ecom.valid.ads.targetTextRequired}")
        @Size(max = 255, message = "{ecom.valid.ads.targetTextTooLong}")
        private String text;
    }
}
