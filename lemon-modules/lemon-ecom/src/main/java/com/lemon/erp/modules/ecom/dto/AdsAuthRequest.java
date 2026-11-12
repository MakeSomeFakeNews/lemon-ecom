package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 保存店铺的广告接口授权：提交的授权字段与已保存的合并（留空不修改，空串删除，连接器缓存的令牌始终保留）
 */
@Data
public class AdsAuthRequest {

    private Map<String, String> credentials;

    /** 1 沙箱 0 正式 */
    private Integer sandbox;

    @Size(max = 255, message = "{ecom.valid.urlCannotExceedCharacters}")
    private String baseUrl;

    @Size(max = 32, message = "{ecom.valid.ads.profileTooLong}")
    private String profileId;

    /** 每天自动同步 1 是 0 否 */
    private Integer sync;
}
