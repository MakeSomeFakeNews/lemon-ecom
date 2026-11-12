package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.TaxType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台订单：从各平台导入 / 同步的销售订单。金额为订单币种，totalBase 等本位币金额按下单日汇率折算；
 * 出库时生成销售出库（直接开单，客户为店铺对应的客户），成本取出库成本，利润 = 本位币金额 − 税额 − 平台费用 − 退款 − 成本 − 运费
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order")
public class EcomOrder extends BaseEntity {

    private Long storeId;
    private String platformOrderNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidTime;

    /** 平台状态 */
    private EcomOrderStatus status;
    private FulfillmentType fulfillment;
    /** 本系统处理状态 */
    private EcomLocalStatus localStatus;

    private String buyerName;
    private String buyerEmail;
    private String shipName;
    private String shipPhone;
    /** 收件国家 / 地区代码，如 US */
    private String shipCountry;
    private String shipState;
    private String shipCity;
    private String shipPostcode;
    private String shipAddress;

    private String currency;
    /** 商品金额 */
    private BigDecimal itemAmount;
    /** 买家支付的运费 */
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    /** 优惠（促销、优惠券） */
    private BigDecimal discountAmount;
    /** 买家实付 = 商品 + 运费 + 税 − 优惠 */
    private BigDecimal totalAmount;
    /** 汇率：1 订单币种 = exchangeRate 本位币 */
    private BigDecimal exchangeRate;
    /** 买家实付（本位币） */
    private BigDecimal totalBase;

    /** 平台佣金（订单币种）：结算回写前为按店铺佣金率的估算 */
    private BigDecimal platformFee;
    /** 平台配送费（订单币种） */
    private BigDecimal fulfillmentFee;
    /** 其他费用（订单币种） */
    private BigDecimal otherFee;
    /** 费用是否为结算单回写的实际值 1 是 0 估算 */
    private Integer feeActual;
    /** 已退款金额（订单币种） */
    private BigDecimal refundAmount;
    /** 商品成本（本位币，出库时取出库成本） */
    private BigDecimal costAmount;
    /** 本公司承担的物流成本（本位币）：自发货为尾程运费，平台配送为头程分摊 */
    private BigDecimal shippingCost;
    /** 退货处理费（订单币种，结算回写） */
    private BigDecimal returnFee;
    /** 利润（本位币）：收入不含平台代扣的税和需卖家申报的 VAT */
    private BigDecimal profit;

    /** 适用的税种，为空表示没有匹配到启用的税率（不拆税） */
    private TaxType taxType;
    /** 适用税率（%） */
    private BigDecimal vatRate;
    /** 平台代扣代缴 1 是 0 否 */
    private Integer taxFacilitator;
    /** 需卖家申报的税额（订单币种）：按含税价 ÷ (1 + 税率) 拆出（价外税取订单税费），按退款比例冲减 */
    private BigDecimal vatAmount;
    /** 平台代扣代缴的税额（订单币种），不计收入 */
    private BigDecimal withheldTax;

    /** 生成的销售出库单 */
    private Long tradeDocId;
    private String tradeDocNo;
    /** 出库仓库 */
    private Long warehouseId;

    private String carrier;
    private String trackingNo;
    /** 发货使用的物流渠道 */
    private Long channelId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    /** 承诺发货截止 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipBy;
    /** 承诺送达截止 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliverBy;

    /** 异常原因 */
    private String exception;

    /** 挂起原因（审单规则要求人工审核或人工拦截） */
    private String holdReason;
    /** 挂起前的处理状态，取消挂起后恢复 */
    private EcomLocalStatus holdPrevStatus;
    /** 由哪条审单规则挂起（人工挂起为空），重新应用规则时只释放规则挂起的订单 */
    private Long holdRuleId;
    /** 计划发货时间：按审单规则的截单时间推算（截单前付款当天发，之后次日发） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime planShipTime;
    /** 预估重量（kg），审单规则按重量区间匹配，包裹默认重量 */
    private BigDecimal weight;
    /** 命中的审单规则名称（逗号分隔） */
    private String matchedRules;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    /** 买家留言 */
    private String note;
    private String remark;

    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    // ---------------------------------------------------------------- 订单增强（类型与来源、地址校验、黑名单、利润预估）

    /** 订单类型：平台 / 手工 / 补发 */
    private EcomOrderType orderType;
    /** 补发单的原订单 */
    private Long sourceOrderId;
    /** 补发单关联的售后工单 */
    private Long sourceTicketId;
    /** 补发单关联的退货退款单 */
    private Long sourceReturnId;
    /** 地址校验发现的问题（为空表示通过），偏远地区等提示也记在这里 */
    private String addressIssue;
    /** 地址校验不通过 1是 0否（必填缺失、邮编 / 电话格式、PO Box），审核时进入异常 */
    private Integer addressInvalid;
    /** 偏远地区 1是 0否 */
    private Integer remoteArea;
    /** 命中的买家黑名单 */
    private Long blacklistId;
    /** 利润预估使用的物流渠道 */
    private Long estChannelId;
    /** 利润预估使用的重量（kg） */
    private BigDecimal estWeight;
    /** 预估运费（本位币） */
    private BigDecimal estShipping;
    /** 预估平台费用（本位币） */
    private BigDecimal estFee;
    /** 预估商品成本（本位币） */
    private BigDecimal estCost;
    /** 预估利润（本位币） */
    private BigDecimal estProfit;
    /** 预估利润率（%） */
    private BigDecimal estProfitRate;

    /** 原订单号（补发单） */
    @TableField(exist = false)
    private String sourceOrderNo;
    /** 预估渠道名称 */
    @TableField(exist = false)
    private String estChannelName;
    /** 标签 */
    @TableField(exist = false)
    private List<EcomOrderTag> tags;
    /** 按包裹回传平台（部分发货）时的包裹号，连接器作为平台的包裹参考号（如亚马逊 packageReferenceId），为空表示整单回传 */
    @TableField(exist = false)
    private String shipPackageRef;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String channelName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<EcomOrderItem> items;
    /** 未取消的包裹数 */
    @TableField(exist = false)
    private Integer packageCount;
    /** 详情：订单的包裹（含已取消） */
    @TableField(exist = false)
    private List<EcomPackage> packages;
}
