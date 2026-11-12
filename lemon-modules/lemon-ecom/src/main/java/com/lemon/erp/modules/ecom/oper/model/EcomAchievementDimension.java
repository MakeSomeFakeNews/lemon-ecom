package com.lemon.erp.modules.ecom.oper.model;

/**
 * 运营目标达成率的统计维度
 */
public enum EcomAchievementDimension {
    /** 按店铺（店铺目标） */
    STORE,
    /** 按负责人（运营人员目标对比其负责店铺的合计） */
    OWNER,
    /** 按平台（店铺目标按平台合计） */
    PLATFORM
}
