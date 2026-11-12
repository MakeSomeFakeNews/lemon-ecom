package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 产品开发项目（候选产品）：调研资料、竞品、预估采购价与尺寸重量、开发负责人、图片；
 * 状态 待调研 → 调研中 → 打样中 → 待审批 → 已通过（生成物料）/ 已驳回 / 已放弃
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_dev_project")
public class EcomDevProject extends BaseEntity {

    private String docNo;
    private String name;
    /** 物料分类编码 */
    private String category;
    private String brand;
    /** 产品来源（业务字典 ecom_dev_source） */
    private String source;
    private EcomPlatform platform;
    private String marketplace;
    /** 站点币种 */
    private String currency;
    private String competitorUrl;
    private BigDecimal competitorPrice;
    private Integer competitorReviews;
    private BigDecimal competitorRating;
    private Integer estMonthlySales;
    /** 目标售价（站点币种） */
    private BigDecimal targetPrice;
    /** 预估采购价（本位币） */
    private BigDecimal estPurchasePrice;
    private BigDecimal weightKg;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    /** 试算单件利润（本位币，保存试算方案时回写） */
    private BigDecimal estProfit;
    /** 试算利润率（%） */
    private BigDecimal estMargin;
    private Long ownerId;
    /** 图片地址 JSON 数组 */
    private String images;
    private String sellingPoints;
    private String description;
    private DevProjectStatus status;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private DevProjectStatus prevStatus;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String flowInstanceId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long materialId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long approveBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String rejectReason;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String approveByName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    /** 样品数 / 合格样品数（列表、看板） */
    @TableField(exist = false)
    private Integer sampleCount;
    @TableField(exist = false)
    private Integer passedSampleCount;
    /** 样品费合计 */
    @TableField(exist = false)
    private BigDecimal sampleFee;
    @TableField(exist = false)
    private List<EcomDevSample> samples;
    @TableField(exist = false)
    private List<EcomDevLog> logs;
}
