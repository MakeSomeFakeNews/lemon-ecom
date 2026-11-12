package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.publish.model.PublishBarcodeType;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 刊登记录：一个刊登草稿在一个店铺的内容（标题、五点描述、描述、图片、变体、价格、库存、平台类目与属性、品牌、条码）和发布状态。
 * 只有草稿 / 失败 / 已下架的可以修改；发布成功后回写平台商品 ID、链接，并生成 / 关联在线商品
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_publish_item")
public class EcomPublishItem extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long draftId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long storeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PublishStatus status;

    @NotBlank(message = "{ecom.valid.pub.titleRequired}")
    @Size(max = 255, message = "{ecom.valid.pub.titleTooLong}")
    private String title;

    @TableField(exist = false)
    @Size(max = 10, message = "{ecom.valid.pub.tooManyBullets}")
    private List<String> bullets = new ArrayList<>();

    @JsonIgnore
    private String bulletsJson;

    @Size(max = 20000, message = "{ecom.valid.pub.descriptionTooLong}")
    private String description;

    @TableField(exist = false)
    @Size(max = 12, message = "{ecom.valid.pub.tooManyImages}")
    private List<String> images = new ArrayList<>();

    @JsonIgnore
    private String imagesJson;

    @Size(max = 64, message = "{ecom.valid.pub.brandTooLong}")
    private String brand;

    private PublishBarcodeType barcodeType;

    private Integer upcExempt;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String exemptionNote;

    /** 平台类目 ID（亚马逊为 Product Type） */
    @Size(max = 64, message = "{ecom.valid.pub.categoryTooLong}")
    private String categoryId;

    @Size(max = 255, message = "{ecom.valid.pub.categoryTooLong}")
    private String categoryName;

    /** 平台属性：属性键 → 值（多个值用英文逗号分隔，或 JSON） */
    @TableField(exist = false)
    @Size(max = 100, message = "{ecom.valid.pub.tooManyAttributes}")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @JsonIgnore
    private String attributesJson;

    private Long publishTemplateId;

    private Long priceTemplateId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String currency;

    private FulfillmentType fulfillment;

    @Size(max = 128, message = "{ecom.valid.pub.policyTooLong}")
    private String shippingTemplate;

    @Size(max = 128, message = "{ecom.valid.pub.policyTooLong}")
    private String returnPolicy;

    @Min(value = 0, message = "{ecom.valid.pub.handlingDaysRange}")
    @Max(value = 60, message = "{ecom.valid.pub.handlingDaysRange}")
    private Integer handlingDays;

    @TableField(exist = false)
    @Size(max = 50, message = "{ecom.valid.pub.tooManyAttributes}")
    private Map<String, String> extra = new LinkedHashMap<>();

    @JsonIgnore
    private String extraJson;

    /** 父 SKU（多变体时，亚马逊父体 / eBay 商品组），为空时按第一个 SKU 加 -P 生成 */
    @Size(max = 64, message = "{ecom.valid.pub.skuTooLong}")
    private String parentSku;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String platformItemId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String platformUrl;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String publishRef;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String errorMessage;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer retryCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime submitTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime publishTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime offlineTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 变体（保存时整体替换） */
    @Valid
    @Size(max = 200, message = "{ecom.valid.pub.tooManyVariants}")
    @TableField(exist = false)
    private List<EcomPublishVariant> variants;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String storeCode;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private Long materialId;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private Integer variantCount;
    @TableField(exist = false)
    private String publishTemplateName;
    @TableField(exist = false)
    private String priceTemplateName;
}
