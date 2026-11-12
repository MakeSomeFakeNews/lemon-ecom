package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 广告接口（SPI）：每个平台的广告 API 实现一个，注册为 Bean 后由 {@link AdsRegistry} 按 {@link EcomPlatform} 查找。
 * 与渠道接口 {@code ChannelConnector} 分开（授权、地址、限速都不同），但沿用同样的思路：
 * {@link #info()} 声明授权字段、正式 / 沙箱地址和能力，地址一律取 {@link AdsContext#baseUrl}，令牌刷新后用
 * {@link AdsContext#updateCredentials} 加密写回；HTTP 用 {@code HttpJson}（非 2xx 抛 {@code ConnectorHttpException}），
 * 二进制报告文件用 {@link AdsHttp}。
 * <p>
 * 调用方保证：不在数据库事务中调用；修改类接口（{@link #applyChanges}）一次传入同一店铺的多项修改，按 localId 返回各项结果，
 * 部分失败时其余照常提交。
 */
public interface AdsConnector {

    AdsConnectorInfo info();

    default EcomPlatform platform() {
        return info().platform();
    }

    default boolean supports(AdsCapability capability) {
        return info().supports(capability);
    }

    /**
     * OAuth 授权页地址（卖家在浏览器中登录平台并同意后，平台带 code 跳转到 redirectUri）。
     * 不支持 OAuth 的连接器（只能手工填写授权字段）抛出不支持
     *
     * @param ctx         已保存的授权字段（clientId 等应用信息）
     * @param redirectUri 回调地址（需要在平台应用中登记为允许的回调地址）
     * @param state       签名的 state（回调时原样带回，用于确定租户与店铺）
     */
    default String authorizeUrl(AdsContext ctx, String redirectUri, String state) {
        throw unsupported();
    }

    /**
     * OAuth 回调：用授权码换取令牌，返回要合并写入广告授权的字段（刷新令牌、访问令牌、过期时间等）
     */
    default Map<String, String> exchangeCode(AdsContext ctx, String code, String redirectUri) {
        throw unsupported();
    }

    /**
     * 某种广告类型、某个层级是否支持该能力（如亚马逊品牌推广的广告组没有默认竞价、展示型推广没有否定关键词）；
     * 默认按 {@link #supports(AdsCapability)}
     *
     * @param adType 广告类型，为空按商品推广
     * @param level  对象层级（活动 / 广告组 / 投放），否定关键词时为空
     */
    default boolean supports(com.lemon.erp.modules.ecom.model.EcomAdType adType, com.lemon.erp.modules.ecom.model.EcomAdLevel level,
                             AdsCapability capability) {
        return supports(capability);
    }

    /** 确保访问令牌有效（快过期时刷新并写回），默认无需令牌 */
    default void ensureToken(AdsContext ctx) {
    }

    /** 测试授权：默认有配置文件能力时列出配置文件，否则拉取一次结构 */
    default void test(AdsContext ctx) {
        if (supports(AdsCapability.PROFILES)) {
            profiles(ctx);
        } else {
            fetchStructure(ctx);
        }
    }

    /** 授权账号下的广告配置文件 */
    default List<AdsProfile> profiles(AdsContext ctx) {
        throw unsupported();
    }

    /** 活动 / 广告组 / 投放结构 */
    default AdsStructure fetchStructure(AdsContext ctx) {
        throw unsupported();
    }

    /**
     * 拉取报告：平台的报告接口一般是异步的（创建报告 → 轮询状态 → 下载文件），由连接器在方法内完成，
     * 超过等待上限时抛出 {@code ecom.error.ads.reportTimeout}
     *
     * @param types 要拉取的报告类型（只会传入 {@link AdsConnectorInfo#reportTypes()} 中的类型）
     * @param from  开始日期（含），不超过 {@link AdsConnectorInfo#maxReportDays()} 天
     */
    default Map<EcomAdReportType, List<AdsReportRow>> fetchReports(AdsContext ctx, Set<EcomAdReportType> types, LocalDate from, LocalDate to) {
        throw unsupported();
    }

    /** 修改竞价 / 预算 / 状态 */
    default List<AdsChangeResult> applyChanges(AdsContext ctx, List<AdsChange> changes) {
        throw unsupported();
    }

    /** 添加否定关键词，成功的结果带平台 ID */
    default List<AdsChangeResult> createNegativeKeywords(AdsContext ctx, List<AdsNegativeKeyword> keywords) {
        throw unsupported();
    }

    default BizException unsupported() {
        return new BizException("ecom.error.ads.unsupported", platform().name());
    }
}
