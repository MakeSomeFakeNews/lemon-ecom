package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomCarrier;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.EcomTrackAlert;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import com.lemon.erp.modules.ecom.model.TrackSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 包裹：一个仓库、一个物流渠道、一个运单号发出的一票货，包含一个或多个平台订单（合并）的全部或部分行（拆分）。
 * 打单、出库、发货都以包裹为单位；订单的处理状态由其包裹汇总，利润、成本仍按订单汇总
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_package")
public class EcomPackage extends BaseEntity {

    /** 包裹号（编码规则 PK），也是向物流商下单的客户单号 */
    private String packageNo;
    private Long storeId;
    /** 发货仓 */
    private Long warehouseId;
    /** 物流渠道 */
    private Long channelId;
    /** 对接的物流商（申请运单号时取渠道的设置） */
    private EcomCarrier carrier;
    /** 承运商名称（回传平台用，手工填写或取渠道的物流商） */
    private String carrierName;
    /** 物流商系统中的订单号（4PX 委托单号、云途运单号），取面单、取消时使用 */
    private String carrierOrderNo;
    /** 运单号（跟踪号） */
    private String trackingNo;
    /** 面单文件（附件目录下的相对路径），不返回前端 */
    @JsonIgnore
    private String labelFile;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime labelTime;
    /** 面单打印次数 */
    private Integer printCount;
    /** 重量（kg） */
    private BigDecimal weight;
    /** 运费（本位币）：按渠道费率试算或手工填写，发货时按订单金额分摊到各订单的物流成本 */
    private BigDecimal shippingCost;
    private EcomPackageStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    /** 收件人快照（合并的订单收件人相同） */
    private String shipName;
    private String shipCountry;
    /** 最近一次申请运单号 / 取面单失败的原因 */
    private String errorMsg;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 轨迹状态（归一后） */
    private EcomTrackStatus trackStatus;
    /** 轨迹预警 */
    private EcomTrackAlert trackAlert;
    /** 轨迹来源 */
    private TrackSource trackSource;
    /** 首条轨迹（上网）时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trackFirstTime;
    /** 最新轨迹时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trackLastTime;
    /** 最新轨迹描述 */
    private String trackLastEvent;
    /** 签收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trackDeliveredTime;
    /** 最近一次拉取轨迹的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime trackPullTime;
    /** 最近一次拉取轨迹失败的原因 */
    private String trackError;
    /** 按包裹回传平台（部分发货） 1成功 0失败 为空未回传 */
    private Integer shipConfirm;
    /** 按包裹回传平台的结果说明 */
    private String shipConfirmMsg;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String channelName;
    /** 包含的平台订单号（逗号分隔） */
    @TableField(exist = false)
    private String orderNos;
    @TableField(exist = false)
    private Integer orderCount;
    /** 件数合计（平台 SKU 件数） */
    @TableField(exist = false)
    private BigDecimal totalQty;
    @TableField(exist = false)
    private Boolean hasLabel;
    @TableField(exist = false)
    private List<EcomPackageItem> items;
}
