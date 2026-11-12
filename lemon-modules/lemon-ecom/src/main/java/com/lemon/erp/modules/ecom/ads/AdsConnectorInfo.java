package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.connector.AuthType;
import com.lemon.erp.modules.ecom.connector.CredentialField;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 广告接口的说明（与渠道接口 {@code ConnectorInfo} 相同的思路）：平台、授权方式、授权字段、正式 / 沙箱地址、能力、文档地址，
 * 以及广告特有的最低竞价、支持的报告类型和一次报告请求最多覆盖的天数
 *
 * @param minBid        平台允许的最低竞价（店铺币种），规则与分时调价计算后不低于它
 * @param reportTypes   可以通过接口拉取的报告类型
 * @param maxReportDays 一次报告请求最多覆盖的天数，超过时按段拆分
 * @param needsProfile  调用业务接口前是否必须选择广告配置文件（亚马逊的 profileId）
 */
public record AdsConnectorInfo(EcomPlatform platform, AuthType authType, List<CredentialField> fields, String productionBaseUrl,
                               String sandboxBaseUrl, Set<AdsCapability> capabilities, String docUrl, BigDecimal minBid,
                               Set<EcomAdReportType> reportTypes, int maxReportDays, boolean needsProfile) {

    public boolean supports(AdsCapability capability) {
        return capabilities.contains(capability);
    }

    /** 连接器自己缓存的字段键（令牌、过期时间），保存授权时始终保留 */
    public Set<String> cachedKeys() {
        return fields == null ? Set.of() : fields.stream().filter(CredentialField::cached).map(CredentialField::key).collect(Collectors.toUnmodifiableSet());
    }
}
