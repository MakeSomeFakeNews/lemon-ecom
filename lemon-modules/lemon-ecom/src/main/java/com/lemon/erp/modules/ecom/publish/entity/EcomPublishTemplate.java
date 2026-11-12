package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 刊登模板：按平台保存常用的类目、属性默认值、品牌、运费模板、退货政策、备货天数、配送方式和默认价格模板，
 * 创建刊登时选择后带入每个店铺的刊登内容（之后可单独修改）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_publish_template")
public class EcomPublishTemplate extends BaseEntity {

    @NotBlank(message = "{ecom.valid.pub.nameRequired}")
    @Size(max = 64, message = "{ecom.valid.pub.nameTooLong}")
    private String name;

    @NotNull(message = "{ecom.valid.pub.platformRequired}")
    private EcomPlatform platform;

    @Size(max = 64, message = "{ecom.valid.pub.categoryTooLong}")
    private String categoryId;

    @Size(max = 255, message = "{ecom.valid.pub.categoryTooLong}")
    private String categoryName;

    /** 属性默认值：属性键 → 值 */
    @TableField(exist = false)
    @Size(max = 100, message = "{ecom.valid.pub.tooManyAttributes}")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @JsonIgnore
    private String attributesJson;

    @Size(max = 64, message = "{ecom.valid.pub.brandTooLong}")
    private String brand;

    @Size(max = 128, message = "{ecom.valid.pub.policyTooLong}")
    private String shippingTemplate;

    @Size(max = 128, message = "{ecom.valid.pub.policyTooLong}")
    private String returnPolicy;

    @Min(value = 0, message = "{ecom.valid.pub.handlingDaysRange}")
    @Max(value = 60, message = "{ecom.valid.pub.handlingDaysRange}")
    private Integer handlingDays;

    private FulfillmentType fulfillment;

    private Long priceTemplateId;

    /** 其他平台参数（如 eBay paymentPolicyId、merchantLocationKey） */
    @TableField(exist = false)
    @Size(max = 50, message = "{ecom.valid.pub.tooManyAttributes}")
    private Map<String, String> extra = new LinkedHashMap<>();

    @JsonIgnore
    private String extraJson;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String priceTemplateName;
}
