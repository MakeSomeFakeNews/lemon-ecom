package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 海外仓接口的数据结构（与服务商无关）
 */
public final class OwhData {

    private OwhData() {
    }

    /** 服务商仓库 */
    public record Warehouse(String code, String name, String country) {
    }

    /**
     * 物流产品
     *
     * @param warehouseCode 适用的仓库，为空表示通用
     * @param carrier       承运商（回传平台用），可空
     */
    public record Product(String code, String name, String warehouseCode, String carrier) {
    }

    /** 一个 SKU 的库存：可用、在途、锁定（待出库） */
    public record Stock(String sku, BigDecimal available, BigDecimal transit, BigDecimal locked) {
    }

    /**
     * 入库单
     *
     * @param refNo         参考号（本系统入库单号），服务商按它防重复
     * @param transportMode 运输方式（业务字典 ecom_transport_mode 的取值，可空）
     */
    public record InboundDraft(String refNo, String warehouseCode, String transportMode, String carrier, String trackingNo, LocalDate etaDate,
                               List<Box> boxes, String remark) {
    }

    /**
     * 一箱
     *
     * @param boxMark 箱唛号
     * @param items   箱内 SKU 与数量
     */
    public record Box(int boxNo, String boxMark, BigDecimal length, BigDecimal width, BigDecimal height, BigDecimal weight, List<Line> items) {
    }

    /**
     * SKU 与数量
     *
     * @param name 品名（物料名称）
     */
    public record Line(String sku, String name, BigDecimal qty) {
    }

    /**
     * 入库单状态
     *
     * @param status   归一后的状态（PUSHED / RECEIVING / RECEIVED / CANCELLED）
     * @param rawStatus 服务商原始状态
     * @param received SKU → 已收货数量
     */
    public record InboundState(OwhInboundStatus status, String rawStatus, Map<String, BigDecimal> received, LocalDateTime receiveTime) {
    }

    /**
     * 出库单
     *
     * @param refNo       参考号（包裹号），服务商按它防重复
     * @param productCode 物流产品代码
     * @param currency    申报币种（订单币种）
     */
    public record OutboundDraft(String refNo, String warehouseCode, String productCode, Receiver receiver, List<Line> items, String currency,
                                BigDecimal declaredValue, String platformOrderNo, String remark) {
    }

    /** 收件人（国家为二字码） */
    public record Receiver(String name, String phone, String email, String country, String state, String city, String postcode, String address) {
    }

    /**
     * 出库单创建结果
     *
     * @param owhOrderNo  服务商出库单号
     * @param trackingNo  运单号（下单即返回的服务商），可空
     * @param estimateFee 预估费用，可空
     */
    public record OutboundResult(String owhOrderNo, String trackingNo, BigDecimal estimateFee, String feeCurrency) {
    }

    /**
     * 出库单状态
     *
     * @param status     归一后的状态（PUSHED / INTERCEPTING / OUTBOUND / CANCELLED）
     * @param carrier    承运商（可空）
     * @param shipTime   海外仓出库时间（可空）
     */
    public record OutboundState(OwhOutboundStatus status, String rawStatus, String trackingNo, String carrier, LocalDateTime shipTime) {
    }

    /**
     * 一条费用
     *
     * @param externalId 服务商的费用流水号（去重）
     * @param refNo      关联单号（服务商出入库单号或参考号），可空
     * @param amount     金额（正数）
     */
    public record Fee(String externalId, OwhFeeType type, LocalDate date, String refNo, String warehouseCode, String sku, String currency,
                      BigDecimal amount, String remark) {
    }

    /**
     * 退件单（买家退货寄回海外仓）
     *
     * @param refNo      参考号（本系统退件单号）
     * @param trackingNo 买家寄回的运单号（可空，服务商按 SKU 与参考号认领）
     */
    public record ReturnDraft(String refNo, String warehouseCode, String platformOrderNo, String carrier, String trackingNo, String buyerName,
                              String country, List<Line> lines, String remark) {
    }

    /**
     * 退件单状态
     *
     * @param status 归一后的状态（PUSHED / RECEIVED / CANCELLED）
     * @param good   SKU → 良品数量
     * @param bad    SKU → 不良品数量
     */
    public record ReturnState(OwhReturnStatus status, String rawStatus, Map<String, BigDecimal> good, Map<String, BigDecimal> bad, LocalDateTime receiveTime) {
    }
}
