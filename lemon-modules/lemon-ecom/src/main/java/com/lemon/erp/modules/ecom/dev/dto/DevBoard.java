package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;

import java.util.List;

/**
 * 开发看板：每个状态一列（项目数 + 最近更新的项目，每列最多 {@code limit} 张卡片）
 */
public record DevBoard(List<Column> columns, int limit) {

    public record Column(DevProjectStatus status, long count, List<EcomDevProject> items) {
    }
}
