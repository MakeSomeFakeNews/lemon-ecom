package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 海外仓 SKU 映射：账号下一个物料对应的服务商 SKU（没有映射的物料按物料编码），库存同步、入库、出库、退件、调拨、费用都按它转换
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_sku")
public class EcomOwhSku extends BaseEntity {

    private Long accountId;
    private Long materialId;
    private String owhSku;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
}
