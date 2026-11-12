package com.lemon.erp.modules.ecom.owh.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 海外仓接口的返回结构
 */
public final class OwhViews {

    private OwhViews() {
    }

    /** 服务商说明（账号表单按它生成授权输入框） */
    public record ProviderView(String provider, String name, List<FieldView> fields, String productionUrl, String sandboxUrl, List<String> capabilities,
                               String docUrl) {
    }

    public record FieldView(String key, String label, boolean secret, boolean required) {
    }

    /**
     * 一次同步的结果
     *
     * @param name    账号 / 单据
     * @param count   处理的条数
     * @param message 失败原因，成功为空
     */
    public record SyncResult(String name, int count, String message) {
    }

    /** 库存调整结果：生成的盘盈入库单号 / 盘亏出库单号与调整的行数 */
    public record AdjustResult(String inDocNo, String outDocNo, int lines) {
    }

    /** 包裹的海外仓出库状态（包裹列表、订单详情用） */
    public record PackageOwh(Long packageId, Long outboundId, String outboundNo, String status, String owhOrderNo, String trackingNo, String errorMsg,
                             String accountName) {
    }

    /** 费用汇总：按月 × 类型 */
    public record FeeSummary(List<Map<String, Object>> rows, Map<String, BigDecimal> byType, BigDecimal total) {
    }
}
