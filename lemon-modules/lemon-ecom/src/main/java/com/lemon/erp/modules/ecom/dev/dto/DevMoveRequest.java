package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 手工流转（看板拖拽 / 按钮）：目标状态 + 说明（放弃时为放弃原因）
 */
public record DevMoveRequest(@NotNull(message = "{ecom.valid.dev.statusRequired}") DevProjectStatus status,
                             @Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}") String remark) {
}
