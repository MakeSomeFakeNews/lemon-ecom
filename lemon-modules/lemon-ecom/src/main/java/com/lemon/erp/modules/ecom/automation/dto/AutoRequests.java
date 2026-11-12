package com.lemon.erp.modules.ecom.automation.dto;

import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 自动化规则中心的请求
 */
public final class AutoRequests {

    private AutoRequests() {
    }

    /** 从模板创建：可同时限定店铺 */
    public record FromTemplate(@Size(max = 200) List<Long> storeIds) {
    }

    /** 待办标为完成 / 重新打开 */
    public record TodoStatus(@NotEmpty(message = "{ecom.valid.ops.selectRecords}") @Size(max = 500, message = "{ecom.valid.ops.tooManyRecords}") List<Long> ids,
                             @NotNull AutoTodoStatus status) {
    }
}
