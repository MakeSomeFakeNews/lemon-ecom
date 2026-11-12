package com.lemon.erp.modules.ecom.automation.dto;

import com.lemon.erp.framework.message.ImChannel;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoAdTarget;
import com.lemon.erp.modules.ecom.automation.model.AutoLogic;
import com.lemon.erp.modules.ecom.automation.model.AutoOperator;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自动化规则的组成部分（存库为 JSON）：对象范围、条件组、动作
 */
public final class AutoRuleParts {

    private AutoRuleParts() {
    }

    /**
     * 对象范围：各项为空表示不限，多项之间全部满足；SKU、标签任一命中即可
     *
     * @param sites 站点（店铺的 marketplace，如 US、DE）
     * @param tags  标签名称（订单标签、广告活动标签、买家消息标签）
     */
    public record Scope(@Size(max = 200, message = "{ecom.valid.auto.scopeTooLong}") List<Long> storeIds,
                        @Size(max = 20, message = "{ecom.valid.auto.scopeTooLong}") List<EcomPlatform> platforms,
                        @Size(max = 50, message = "{ecom.valid.auto.scopeTooLong}") List<@Size(max = 16) String> sites,
                        @Size(max = 200, message = "{ecom.valid.auto.scopeTooLong}") List<@Size(max = 64) String> skus,
                        @Size(max = 50, message = "{ecom.valid.auto.scopeTooLong}") List<@Size(max = 32) String> tags) {

        public static Scope empty() {
            return new Scope(List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    /** 一个条件：字段 + 运算符 + 值（多个值用逗号分隔） */
    public record Condition(@NotBlank(message = "{ecom.valid.auto.fieldRequired}") @Size(max = 64) String field,
                            @NotNull(message = "{ecom.valid.auto.operatorRequired}") AutoOperator operator,
                            @Size(max = 500, message = "{ecom.valid.auto.valueTooLong}") String value) {
    }

    /**
     * 条件组：items 与 groups 按 logic 组合（全部满足 / 任一满足），groups 只允许一层嵌套；都为空表示不限
     */
    public record Group(AutoLogic logic,
                        @Size(max = 20, message = "{ecom.valid.auto.tooManyConditions}") List<@Valid @NotNull Condition> items,
                        @Size(max = 10, message = "{ecom.valid.auto.tooManyConditions}") List<@Valid @NotNull Group> groups) {

        public static Group empty() {
            return new Group(AutoLogic.AND, List.of(), List.of());
        }
    }

    /**
     * 动作：按类型使用其中的部分字段（见 {@code automation.action} 中各处理器）。title / content / reason 支持 ${字段} 变量
     *
     * @param storeOwner  通知 / 待办发给店铺负责人
     * @param site        通知写入站内消息（为空视为是）
     * @param channels    通知同时推送的第三方平台应用消息（钉钉 / 企业微信 / 飞书，发给已绑定的成员）
     * @param imPlatform  群机器人平台；webhook 为空时用「第三方集成」中该平台配置的群机器人
     * @param secret      群机器人加签密钥 / Webhook 签名密钥（加密存储，查询时返回掩码）
     * @param adjustMode  调价 / 调整广告：PERCENT 百分比、AMOUNT 加减金额、FIXED 改为固定值
     * @param suggestOnly 调价只记录建议价，不修改
     * @param qty         采购计划数量，为空取补货建议量
     * @param dueDays     待办的期限（天）
     */
    public record Action(@NotNull(message = "{ecom.valid.auto.actionTypeRequired}") AutoActionType type,
                         @Size(max = 50) List<Long> userIds,
                         @Size(max = 20) List<Long> roleIds,
                         Boolean storeOwner,
                         Boolean site,
                         @Size(max = 3) List<ImChannel> channels,
                         @Size(max = 128, message = "{ecom.valid.auto.titleTooLong}") String title,
                         @Size(max = 1000, message = "{ecom.valid.auto.contentTooLong}") String content,
                         ImChannel imPlatform,
                         @Size(max = 500, message = "{ecom.valid.auto.urlTooLong}") String webhook,
                         @Size(max = 500, message = "{ecom.valid.auto.secretTooLong}") String secret,
                         @Size(max = 255, message = "{ecom.valid.auto.reasonTooLong}") String reason,
                         @Size(max = 20) List<Long> tagIds,
                         Long warehouseId,
                         Long channelId,
                         PriceAdjustMode adjustMode,
                         @Digits(integer = 14, fraction = 4, message = "{ecom.valid.auto.valueInvalid}") BigDecimal value,
                         @Digits(integer = 14, fraction = 4, message = "{ecom.valid.auto.valueInvalid}") BigDecimal minValue,
                         @Digits(integer = 14, fraction = 4, message = "{ecom.valid.auto.valueInvalid}") BigDecimal maxValue,
                         Boolean suggestOnly,
                         @Digits(integer = 14, fraction = 4, message = "{ecom.valid.auto.valueInvalid}") BigDecimal qty,
                         CsTicketType ticketType,
                         AutoAdTarget adTarget,
                         @Min(value = 0, message = "{ecom.valid.auto.dueDaysRange}") @Max(value = 365, message = "{ecom.valid.auto.dueDaysRange}") Integer dueDays) {

        /** 复制并替换密钥（保存时加密 / 查询时掩码） */
        public Action withSecret(String s) {
            return new Action(type, userIds, roleIds, storeOwner, site, channels, title, content, imPlatform, webhook, s, reason, tagIds, warehouseId, channelId,
                    adjustMode, value, minValue, maxValue, suggestOnly, qty, ticketType, adTarget, dueDays);
        }

        public static Action of(AutoActionType type) {
            return new Action(type, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }
}
