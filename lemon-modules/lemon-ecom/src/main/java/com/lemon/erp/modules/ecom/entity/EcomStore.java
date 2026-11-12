package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台店铺：一个平台站点上的一个卖家账号。每个店铺对应一个客户（scm_customer，应收挂在它下面）、
 * 一个默认发货仓（自发货订单出库）和可选的平台仓（FBA / 海外仓，平台配送订单从它出库、头程货件调入）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_store")
public class EcomStore extends BaseEntity {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String code;

    @NotBlank(message = "{ecom.valid.storeNameRequired}")
    @Size(max = 64, message = "{ecom.valid.storeNameCannotExceedCharacters}")
    private String name;

    @NotNull(message = "{ecom.valid.selectPlatform}")
    private EcomPlatform platform;

    /** 站点 / 市场（业务字典 ecom_marketplace），如 US、DE、JP */
    @NotBlank(message = "{ecom.valid.selectMarketplace}")
    @Size(max = 16)
    private String marketplace;

    /** 店铺结算币种（fin_currency），订单、结算、广告金额默认按此币种 */
    @NotBlank(message = "{ecom.valid.selectCurrency}")
    @Size(max = 8)
    private String currency;

    @Size(max = 64, message = "{ecom.valid.timezoneCannotExceedCharacters}")
    private String timezone;

    /** 对应的客户（销售出库、应收的往来单位），新建店铺时自动建立 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long customerId;

    /** 自发货订单的默认发货仓、头程货件的默认调出仓 */
    @NotNull(message = "{ecom.valid.selectWarehouse}")
    private Long warehouseId;

    /** 平台仓（FBA / 平台海外仓）：平台配送订单从它出库，头程货件调入它；为空时平台配送订单不能出库 */
    private Long platformWarehouseId;

    /** 预估平台佣金率（%）：结算回写实际费用前按它估算订单利润 */
    @DecimalMin(value = "0", message = "{ecom.valid.commissionRateRange}")
    @DecimalMax(value = "100", message = "{ecom.valid.commissionRateRange}")
    private BigDecimal commissionRate;

    /** 补货周期（天）：从下单采购 / 发运到平台仓可售的天数 */
    @Min(value = 0, message = "{ecom.valid.leadDaysRange}")
    @Max(value = 365, message = "{ecom.valid.leadDaysRange}")
    private Integer leadDays;

    /** 安全库存天数 */
    @Min(value = 0, message = "{ecom.valid.leadDaysRange}")
    @Max(value = 365, message = "{ecom.valid.leadDaysRange}")
    private Integer safetyDays;

    /** 负责人（用户） */
    private Long ownerId;

    /** 平台卖家 ID（亚马逊 Seller ID 等） */
    @Size(max = 64, message = "{ecom.valid.sellerIdCannotExceedCharacters}")
    private String sellerId;

    /**
     * 渠道接口授权（JSON，按平台不同，见 StoreCredentials），只写不读：保存后列表、详情只返回是否已配置。
     * 为空时该店铺只能手工导入订单
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 4000, message = "{ecom.valid.credentialsCannotExceedCharacters}")
    private String credentials;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastSyncTime;

    /** 渠道接口环境：1 沙箱 / 测试，0 正式 */
    private Integer sandbox;

    /** 自定义接口地址（私有网关、测试用模拟服务器），为空按环境取连接器的默认地址 */
    @Size(max = 255, message = "{ecom.valid.urlCannotExceedCharacters}")
    private String apiBaseUrl;

    /** 自动同步订单 1 是 0 否 */
    private Integer orderSync;

    /** 自动回传可售库存（自发货）1 是 0 否 */
    private Integer inventorySync;

    /** 回传库存时保留的安全数量（件） */
    @jakarta.validation.constraints.DecimalMin(value = "0", message = "{ecom.valid.countCannotNegative}")
    private BigDecimal inventoryBuffer;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime authorizedTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastSyncMessage;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String platformWarehouseName;
    @TableField(exist = false)
    private String ownerName;
    /** 是否已配置渠道接口授权 */
    @TableField(exist = false)
    private Boolean hasCredentials;
    /** 在线商品数 */
    @TableField(exist = false)
    private Integer listingCount;
}
