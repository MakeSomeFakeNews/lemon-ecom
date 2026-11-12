package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.framework.excel.ImportResult;

import java.util.List;

/**
 * 到账流水导入结果：imported 为写入的流水数；matched 匹配到结算单（其中 diff 条金额有差异），unmatched 没有匹配到（记为有差异），
 * skipped 流水号重复跳过；有错误时整批不导入
 */
public record PayoutImportResult(int total, int imported, int matched, int diff, int unmatched, int skipped, List<ImportResult.RowError> errors) {
}
