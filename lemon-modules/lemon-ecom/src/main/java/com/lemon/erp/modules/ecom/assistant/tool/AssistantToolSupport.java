package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 电商问数工具的公共部分：店铺范围参数（店铺名称 / 编码、平台、站点，在当前用户可见的店铺中匹配）、日期范围的默认值与校验、行数
 */
@Component
@RequiredArgsConstructor
public class AssistantToolSupport {

    /** 日期范围最长天数 */
    static final int MAX_DAYS = 366;

    private final EcomStoreService storeService;

    /**
     * 店铺范围
     *
     * @param ids      范围内的店铺 ID
     * @param filtered 是否按参数筛选过（否则为全部可见店铺）
     */
    public record Scope(Set<Long> ids, boolean filtered, List<EcomStore> stores) {

        public boolean contains(Long storeId) {
            return ids.contains(storeId);
        }

        /** 只有一个店铺时的 ID（可直接传给按单个店铺查询的方法），否则为 null */
        public Long single() {
            return filtered && ids.size() == 1 ? ids.iterator().next() : null;
        }
    }

    /** 店铺范围参数：store（名称或编码）、platform、marketplace */
    public static AiSchema scopeProps(AiSchema schema) {
        return schema.prop("store", AiSchema.string("ecom.ai.asst.param.store").maxLength(64), false)
                .prop("platform", AiSchema.enumOf("ecom.ai.asst.param.platform", Arrays.stream(EcomPlatform.values()).map(Enum::name).toArray(String[]::new)), false)
                .prop("marketplace", AiSchema.string("ecom.ai.asst.param.marketplace").maxLength(16), false);
    }

    public static AiSchema dateProps(AiSchema schema) {
        return schema.prop("startDate", AiSchema.date("ecom.ai.asst.param.startDate"), false)
                .prop("endDate", AiSchema.date("ecom.ai.asst.param.endDate"), false);
    }

    public static AiSchema topProp(AiSchema schema) {
        return schema.prop("top", AiSchema.integer("ecom.ai.param.top").min(1).max(AiTool.MAX_ROWS), false);
    }

    /** 按参数在当前用户可见的启用店铺中筛选；有条件但没有匹配的店铺时报错（列出可选店铺，模型据此请用户确认） */
    public Scope scope(AiToolArgs args) {
        List<EcomStore> all = storeService.options();
        String store = args.getString("store");
        EcomPlatform platform = args.has("platform") ? EcomPlatform.valueOf(args.getString("platform")) : null;
        String marketplace = args.getString("marketplace");
        List<EcomStore> list = all.stream()
                .filter(s -> platform == null || s.getPlatform() == platform)
                .filter(s -> marketplace == null || marketplace.equalsIgnoreCase(s.getMarketplace()))
                .toList();
        if (store != null) {
            String k = store.toLowerCase(Locale.ROOT);
            List<EcomStore> exact = list.stream().filter(s -> k.equals(lower(s.getCode())) || k.equals(lower(s.getName()))).toList();
            list = !exact.isEmpty() ? exact : list.stream().filter(s -> lower(s.getCode()).contains(k) || lower(s.getName()).contains(k)).toList();
        }
        boolean filtered = store != null || platform != null || marketplace != null;
        if (filtered && list.isEmpty()) {
            throw new BizException("ecom.error.asst.toolNoStore",
                    all.stream().map(s -> s.getName() + "(" + s.getPlatform() + "/" + s.getMarketplace() + ")").collect(Collectors.joining(", ")));
        }
        Set<Long> ids = list.stream().map(EcomStore::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        return new Scope(ids, filtered, list);
    }

    /** 开始日期：为空时为结束日期前 days − 1 天 */
    public static LocalDate begin(AiToolArgs args, LocalDate end, int days) {
        LocalDate b = args.getDate("startDate");
        LocalDate begin = b == null ? end.minusDays(days - 1L) : b;
        if (begin.isAfter(end)) {
            throw new BizException("ecom.error.asst.rangeInvalid");
        }
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS) {
            throw new BizException("ecom.error.asst.rangeTooLong", MAX_DAYS);
        }
        return begin;
    }

    /** 结束日期：为空时为昨天 */
    public static LocalDate end(AiToolArgs args) {
        LocalDate e = args.getDate("endDate");
        return e == null ? LocalDate.now().minusDays(1) : e;
    }

    public static int top(AiToolArgs args, int def) {
        return Math.min(AiTool.MAX_ROWS, Math.max(1, args.getInt("top", def)));
    }

    public static <T> List<T> head(List<T> list, int n) {
        return list.size() > n ? list.subList(0, n) : list;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
