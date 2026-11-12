package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.ecom.dto.SyncResult;

import java.util.List;

/**
 * 渠道同步定时任务的执行结果说明（写入任务日志，存库内容用中文）
 */
final class EcomJobSummaries {

    private EcomJobSummaries() {
    }

    /**
     * @param verb 动作，如「回传」「更新」，后接 updated 条数
     */
    static String summary(List<SyncResult> results, String verb) {
        int created = results.stream().mapToInt(SyncResult::created).sum();
        int updated = results.stream().mapToInt(SyncResult::updated).sum();
        List<SyncResult> failed = results.stream().filter(r -> r.message() != null).toList();
        StringBuilder sb = new StringBuilder("处理 " + results.size() + " 个店铺"); // i18n-ignore 任务日志存库
        if (created > 0) {
            sb.append("，新增 ").append(created).append(" 条"); // i18n-ignore 任务日志存库
        }
        sb.append("，").append(verb).append(" ").append(updated).append(" 条"); // i18n-ignore 任务日志存库
        if (!failed.isEmpty()) {
            sb.append("，失败 ").append(failed.size()).append(" 个："); // i18n-ignore 任务日志存库
            failed.forEach(r -> sb.append(r.storeName()).append(": ").append(r.message()).append("; "));
        }
        return sb.toString();
    }
}
