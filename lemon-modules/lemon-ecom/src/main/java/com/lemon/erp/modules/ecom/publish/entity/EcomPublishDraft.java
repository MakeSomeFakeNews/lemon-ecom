package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.publish.model.PublishBarcodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 刊登草稿：来源物料（多规格款式即 SPU，变体为其下的 SKU）与主内容（标题、五点描述、描述、图片、品牌、条码 / UPC 豁免）。
 * 选择店铺后按店铺生成刊登记录（{@link EcomPublishItem}），每个店铺一份可单独修改的内容
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_publish_draft")
public class EcomPublishDraft extends BaseEntity {

    @NotNull(message = "{ecom.valid.pub.materialRequired}")
    private Long materialId;

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

    /** UPC 豁免 1 是 */
    private Integer upcExempt;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String exemptionNote;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long deptId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    /** 来源物料是多规格款式 */
    @TableField(exist = false)
    private Boolean spu;
    /** 各店铺的刊登记录（详情返回） */
    @TableField(exist = false)
    private List<EcomPublishItem> items;
}
