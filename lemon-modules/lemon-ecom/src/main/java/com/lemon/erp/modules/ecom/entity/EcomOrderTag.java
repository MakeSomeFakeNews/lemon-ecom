package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单标签：名称、颜色。订单可打多个标签（ecom_order_tag_link），人工批量打标 / 去标，或由审单规则的「打标签」动作自动打上
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_tag")
public class EcomOrderTag extends BaseEntity {

    @NotBlank(message = "{ecom.valid.ord.tagNameRequired}")
    @Size(max = 32, message = "{ecom.valid.ord.tagNameTooLong}")
    private String name;
    /** Ant Design 预设色（blue、red …）或 #RRGGBB */
    @Pattern(regexp = "^(#[0-9a-fA-F]{6}|[a-z]{3,16})?$", message = "{ecom.valid.ord.tagColorInvalid}")
    private String color;
    private Integer sort;
    private EnableStatus status;
    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    /** 使用该标签的订单数 */
    @TableField(exist = false)
    private Integer orderCount;
    /** 订单上的标签：是否由审单规则自动打上 */
    @TableField(exist = false)
    private Integer autoTagged;
    /** 订单上的标签：所属订单（批量读取标签时使用） */
    @TableField(exist = false)
    private Long orderId;
}
