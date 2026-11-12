package com.lemon.erp.modules.ecom.automation.dto;

import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import lombok.Data;

import java.time.LocalDate;

/**
 * 执行日志查询：规则、触发器、结果、日期区间、对象（名称 / 标识）、店铺；executed 只看执行了动作的
 */
@Data
public class AutoLogQuery {
    private Long ruleId;
    private AutoTrigger trigger;
    private AutoResult result;
    private Boolean executed;
    private Long storeId;
    private LocalDate beginDate;
    private LocalDate endDate;
    private String keyword;

    /** 结束日期的次日（SQL 中按 &lt; 比较） */
    public LocalDate getEndExclusive() {
        return endDate == null ? null : endDate.plusDays(1);
    }
}
