package com.lemon.erp.modules.ecom.automation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自动化规则创建的待办提醒：指派给处理人，出现在首页「我的待办」、电商看板和「待办提醒」页面，处理后标为完成
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_auto_todo")
public class EcomAutoTodo extends BaseEntity {

    private Long ruleId;
    private Long assigneeId;
    private Long storeId;
    private String title;
    private String content;
    /** 点击跳转的页面 */
    private String link;
    private String objectKey;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
    private AutoTodoStatus status;
    private Long doneBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime doneTime;

    @TableField(exist = false)
    private String ruleName;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String storeName;
}
