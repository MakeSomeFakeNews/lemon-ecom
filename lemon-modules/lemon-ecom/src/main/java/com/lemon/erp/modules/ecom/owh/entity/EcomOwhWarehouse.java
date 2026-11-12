package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 海外仓仓库映射：一个系统仓库（wms_warehouse）对应一个服务商账号下的仓库代码；映射了的仓库就是海外仓，
 * 发货仓为它的包裹不走本地打单，推送海外仓出库单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_warehouse")
public class EcomOwhWarehouse extends BaseEntity {

    private Long accountId;
    private Long warehouseId;
    private String owhCode;
    private String owhName;
    private String country;
    /** 默认物流产品代码 */
    private String defaultProductCode;
    /** 审核后自动推送 1是 */
    private Integer autoPush;
    private EnableStatus status;
    private String remark;
    /** 不良品仓（退件质检不良的转入；为空时报废出库） */
    private Long defectWarehouseId;
    /** 收货地址（海外仓之间调拨时作为收件信息） */
    private String contact;
    private String phone;
    private String state;
    private String city;
    private String postcode;
    private String address;

    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private OwhProvider provider;
    @TableField(exist = false)
    private String warehouseCode;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String defectWarehouseName;
    @TableField(exist = false)
    private Integer autoSync;
}
