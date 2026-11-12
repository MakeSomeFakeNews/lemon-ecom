package com.lemon.erp.modules.ecom.assistant.dto;

import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;
import com.lemon.erp.modules.ecom.assistant.service.BannedWordMatcher;

import java.util.List;

/** AI 运营助手的返回结构 */
public final class AssistantViews {

    private AssistantViews() {
    }

    /** 刊登编辑页实时检查用的词（启用、适用于该平台 / 站点） */
    public record ApplicableWord(Long id, String word, String wordType, BannedWordLevel level, BannedWordMatch matchMode, String suggestion) {
    }

    /** 合规检查结果 */
    public record CheckResult(List<BannedWordMatcher.Hit> hits, int blockCount, int warnCount) {
    }

    /** 日报详情：日报（含 AI 建议）+ 结构化内容 */
    public record ReportDetail(EcomOpsReport report, OpsReportContent content) {
    }

    /** 订阅设置页的选项：已接入并开启消息推送的第三方平台、日报定时任务的标识与默认 Cron（在「定时任务」中修改） */
    public record SubOptions(List<ImChannel> imChannels, String jobKey, String defaultCron) {
    }

    /** AI 生成的 Listing 文案与合规检查 */
    public record ListingCopy(String title, List<String> bullets, String description, String language, CheckResult check) {
    }

    /** 关键词建议的上下文：刊登记录的物料与站点 */
    public record KeywordResult(Long materialId, String marketplace, boolean sameCategory, int days, List<AssistantRows.KeywordRow> rows) {
    }
}
