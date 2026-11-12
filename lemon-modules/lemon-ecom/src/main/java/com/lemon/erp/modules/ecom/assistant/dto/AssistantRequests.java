package com.lemon.erp.modules.ecom.assistant.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordMatch;
import com.lemon.erp.modules.ecom.assistant.model.OpsPushChannel;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** AI 运营助手的请求 */
public final class AssistantRequests {

    private AssistantRequests() {
    }

    /** 新增 / 修改违禁词 */
    public record WordSave(
            @NotBlank(message = "{ecom.valid.asst.wordRequired}") @Size(max = 128, message = "{ecom.valid.asst.wordTooLong}") String word,
            @NotBlank(message = "{ecom.valid.asst.wordTypeRequired}") String wordType,
            @NotNull(message = "{ecom.valid.asst.levelRequired}") BannedWordLevel level,
            BannedWordMatch matchMode,
            EcomPlatform platform,
            @Size(max = 16, message = "{ecom.valid.asst.marketplaceTooLong}") String marketplace,
            @Size(max = 255, message = "{ecom.valid.asst.suggestionTooLong}") String suggestion,
            EnableStatus status,
            @Size(max = 255, message = "{ecom.valid.asst.remarkTooLong}") String remark) {
    }

    /** 批量添加：一行一个词，其余属性相同；已有的（同一词、平台、站点）跳过 */
    public record WordBatch(
            @NotBlank(message = "{ecom.valid.asst.wordRequired}") @Size(max = 10000, message = "{ecom.valid.asst.batchTooLong}") String words,
            @NotBlank(message = "{ecom.valid.asst.wordTypeRequired}") String wordType,
            @NotNull(message = "{ecom.valid.asst.levelRequired}") BannedWordLevel level,
            BannedWordMatch matchMode,
            EcomPlatform platform,
            @Size(max = 16, message = "{ecom.valid.asst.marketplaceTooLong}") String marketplace,
            @Size(max = 255, message = "{ecom.valid.asst.suggestionTooLong}") String suggestion) {
    }

    /** 合规检查：按平台 / 站点适用的词检查标题、五点、描述 */
    public record CheckRequest(EcomPlatform platform, String marketplace,
                               @Size(max = 1000, message = "{ecom.valid.asst.textTooLong}") String title,
                               @Size(max = 20, message = "{ecom.valid.asst.textTooLong}") List<String> bullets,
                               @Size(max = 50000, message = "{ecom.valid.asst.textTooLong}") String description) {
    }

    /** 立即生成日报：日期默认昨天，店铺为空时为当前用户可见的全部店铺；ai 为 false 时不生成 AI 建议 */
    public record ReportGenerate(LocalDate date, @Size(max = 500, message = "{ecom.valid.asst.tooManyStores}") List<Long> storeIds, Boolean ai) {
    }

    /** 保存订阅：订阅人、店铺范围（为空为订阅人可见的全部店铺）、推送渠道 */
    public record SubSave(@NotNull(message = "{ecom.valid.asst.userRequired}") Long userId,
                          @Size(max = 200, message = "{ecom.valid.asst.tooManyStores}") List<Long> storeIds,
                          @NotEmpty(message = "{ecom.valid.asst.channelRequired}") List<OpsPushChannel> channels,
                          EnableStatus status,
                          @Size(max = 255, message = "{ecom.valid.asst.remarkTooLong}") String remark) {
    }

    /**
     * AI 写 Listing：按刊登记录的平台、站点生成或优化标题 / 五点 / 描述
     *
     * @param parts    要生成的部分（title、bullets、description），为空全部
     * @param keywords 要融入的关键词（从搜索词报告勾选）
     * @param language 输出语言代码，为空按站点
     */
    public record ListingAiRequest(@NotNull(message = "{ecom.valid.asst.itemRequired}") Long itemId,
                                   @Size(max = 1000, message = "{ecom.valid.asst.textTooLong}") String title,
                                   @Size(max = 20, message = "{ecom.valid.asst.textTooLong}") List<String> bullets,
                                   @Size(max = 20000, message = "{ecom.valid.asst.textTooLong}") String description,
                                   @Size(max = 3, message = "{ecom.valid.asst.textTooLong}") List<String> parts,
                                   @Size(max = 30, message = "{ecom.valid.asst.tooManyKeywords}") List<String> keywords,
                                   @Size(max = 16, message = "{ecom.valid.asst.languageTooLong}") String language,
                                   @Size(max = 500, message = "{ecom.valid.asst.instructionTooLong}") String instruction) {
    }
}
