package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志中保存的事件（JSON）：重试、试运行（用最近的真实事件）时还原为 {@link AutoEvent}
 */
public record AutoStoredEvent(String objectLabel, Long storeId, EcomPlatform platform, String site, List<String> skus, List<String> tags,
                              Map<String, Object> fields, Map<AutoRef, Long> refs, String link, LocalDateTime time) {

    public static String json(AutoEvent e) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (e.fields() != null) {
            e.fields().forEach((k, v) -> fields.put(k, v instanceof LocalDateTime t ? AutoText.format(t, com.lemon.erp.common.i18n.Messages.DEFAULT_LOCALE) : v));
        }
        return HttpJson.MAPPER.writeValueAsString(new AutoStoredEvent(e.objectLabel(), e.storeId(), e.platform(), e.site(), e.skus(), e.tags(), fields,
                e.refs(), e.link(), e.time()));
    }

    public static AutoEvent parse(AutoTrigger trigger, String objectKey, String json) {
        AutoStoredEvent s = HttpJson.MAPPER.readValue(json, AutoStoredEvent.class);
        Map<AutoRef, Long> refs = new EnumMap<>(AutoRef.class);
        if (s.refs() != null) {
            refs.putAll(s.refs());
        }
        return new AutoEvent(trigger, objectKey, s.objectLabel(), s.storeId(), s.platform(), s.site(), s.skus() == null ? List.of() : s.skus(),
                s.tags() == null ? List.of() : s.tags(), s.fields() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(s.fields()), refs, s.link(),
                s.time());
    }
}
