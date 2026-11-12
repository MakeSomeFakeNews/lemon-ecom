package com.lemon.erp.modules.ecom.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/** 运营日报订阅：订阅人（每人一条）、店铺范围（为空为订阅人可见的全部店铺）、推送渠道 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ops_report_sub")
public class EcomOpsReportSub extends BaseEntity {

    private Long userId;
    /** 店铺 ID，逗号分隔 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String storeIds;
    /** 推送渠道（OpsPushChannel 名称），逗号分隔 */
    private String channels;
    private EnableStatus status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
    private Long lastReportId;
    private LocalDateTime lastPushTime;
    private Long createBy;

    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private List<String> storeNames;
}
