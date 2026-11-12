package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺授权：把店铺分配给用户（运营、客服、助理），用户在电商各页面只能看到负责的店铺和被授权店铺的数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_store_member")
public class EcomStoreMember extends BaseEntity {

    private Long storeId;
    private Long userId;
    /** 岗位（业务字典 ecom_store_member_role） */
    private String memberRole;
    private String remark;
    private Long createBy;

    @TableField(exist = false)
    private String username;
    @TableField(exist = false)
    private String nickname;
    @TableField(exist = false)
    private String storeName;
}
