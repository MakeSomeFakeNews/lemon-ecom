package com.lemon.erp.modules.ecom.oper.dto;

import com.lemon.erp.modules.ecom.oper.model.EcomPromoPhase;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 促销活动列表的条件：beginDate / endDate 为与活动期间有交集；phase 只对已生效的活动有意义（按当前时间 now 计算）
 */
@Data
public class PromotionQuery {
    private Long storeId;
    private String promoType;
    private EcomPromoStatus status;
    private EcomPromoPhase phase;
    private String keyword;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate beginDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    /** 由服务端填充：当前时间（阶段条件用） */
    private LocalDateTime now;
    /** 由服务端填充：endDate 的次日零点 */
    private LocalDateTime endNext;
}
