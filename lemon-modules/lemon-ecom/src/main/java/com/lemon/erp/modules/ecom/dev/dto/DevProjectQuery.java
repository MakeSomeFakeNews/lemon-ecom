package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

/**
 * 开发项目查询条件
 *
 * @param keyword 编号 / 名称 / 卖点
 */
public record DevProjectQuery(DevProjectStatus status, String keyword, Long ownerId, EcomPlatform platform, String source, String category) {
}
