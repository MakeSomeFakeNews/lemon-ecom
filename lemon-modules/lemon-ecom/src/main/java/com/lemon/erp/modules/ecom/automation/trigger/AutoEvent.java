package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个自动化事件：触发器 + 对象 + 字段值。由 {@link AutoTriggerSource} 产生，交给 {@code AutoEngine} 逐条规则处理
 *
 * @param objectKey   对象标识（防抖按「规则 + 对象标识」判断），如 ORDER:123、STORE:5:ORDERS
 * @param objectLabel 对象名称（订单号、SKU、活动名称等，日志与消息中显示）
 * @param storeId     所属店铺（范围过滤、店铺负责人），没有为空
 * @param platform    平台（范围过滤）
 * @param site        站点（店铺的 marketplace）
 * @param skus        涉及的平台 SKU（范围过滤，任一命中）
 * @param tags        标签名称（范围过滤，任一命中）
 * @param fields      字段值（条件、消息变量、Webhook 报文），键与触发器声明的字段一致
 * @param refs        关联的业务对象 ID（动作使用，如挂起订单用 ORDER）
 * @param link        前端页面路由（消息、待办点击跳转）
 */
public record AutoEvent(AutoTrigger trigger, String objectKey, String objectLabel, Long storeId, EcomPlatform platform, String site,
                        List<String> skus, List<String> tags, Map<String, Object> fields, Map<AutoRef, Long> refs, String link,
                        LocalDateTime time) {

    public Long ref(AutoRef r) {
        return refs == null ? null : refs.get(r);
    }

    public Object field(String key) {
        return fields == null ? null : fields.get(key);
    }

    public static Builder builder(AutoTrigger trigger, String objectKey) {
        return new Builder(trigger, objectKey);
    }

    /** 构造事件；字段按声明顺序保存（日志、试运行按这个顺序显示） */
    public static final class Builder {
        private final AutoTrigger trigger;
        private final String objectKey;
        private String objectLabel;
        private Long storeId;
        private EcomPlatform platform;
        private String site;
        private List<String> skus = List.of();
        private List<String> tags = List.of();
        private final Map<String, Object> fields = new LinkedHashMap<>();
        private final Map<AutoRef, Long> refs = new EnumMap<>(AutoRef.class);
        private String link;
        private LocalDateTime time = LocalDateTime.now();

        private Builder(AutoTrigger trigger, String objectKey) {
            this.trigger = trigger;
            this.objectKey = objectKey;
        }

        public Builder label(String v) {
            this.objectLabel = v;
            return this;
        }

        /** 店铺：同时写入字段 storeName、platform、site */
        public Builder store(Long id, String name, EcomPlatform platform, String site) {
            this.storeId = id;
            this.platform = platform;
            this.site = site;
            if (id != null) {
                refs.put(AutoRef.STORE, id);
            }
            fields.put("storeName", name);
            fields.put("platform", platform == null ? null : platform.name());
            fields.put("site", site);
            return this;
        }

        public Builder skus(List<String> v) {
            this.skus = v == null ? List.of() : v;
            return this;
        }

        public Builder tags(List<String> v) {
            this.tags = v == null ? List.of() : v;
            return this;
        }

        public Builder field(String key, Object value) {
            fields.put(key, value);
            return this;
        }

        public Builder ref(AutoRef r, Long id) {
            if (id != null) {
                refs.put(r, id);
            }
            return this;
        }

        public Builder link(String v) {
            this.link = v;
            return this;
        }

        public Builder time(LocalDateTime v) {
            this.time = v;
            return this;
        }

        public AutoEvent build() {
            return new AutoEvent(trigger, objectKey, objectLabel, storeId, platform, site, skus, tags, fields, refs, link, time);
        }
    }
}
