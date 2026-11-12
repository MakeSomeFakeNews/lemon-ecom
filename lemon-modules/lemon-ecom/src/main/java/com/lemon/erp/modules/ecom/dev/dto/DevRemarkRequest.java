package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.constraints.Size;

/**
 * 审批意见 / 驳回原因
 */
public record DevRemarkRequest(@Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}") String remark) {
}
