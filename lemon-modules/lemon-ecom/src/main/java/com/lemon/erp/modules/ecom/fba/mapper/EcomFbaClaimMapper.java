package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EcomFbaClaimMapper extends BaseMapper<EcomFbaClaim> {

    IPage<EcomFbaClaim> selectPageList(IPage<EcomFbaClaim> page, @Param("storeId") Long storeId, @Param("claimType") FbaClaimType claimType,
                                       @Param("status") FbaClaimStatus status, @Param("keyword") String keyword,
                                       @Param("deadlineBefore") LocalDate deadlineBefore);

    /** 按 状态 × 币种 汇总条数、估算金额、已赔付金额 */
    List<Map<String, Object>> selectSummary(@Param("storeId") Long storeId);

    /** 按 类型 × 币种 汇总待提交 / 已提交的条数和估算金额 */
    List<Map<String, Object>> selectTypeSummary(@Param("storeId") Long storeId);

    /** 店铺已有的来源键 */
    List<String> selectExistingKeys(@Param("storeId") Long storeId, @Param("keys") Collection<String> keys);

    /** 退款超过期限仍未退回 FBA 库存（库存流水中没有该订单的退货入库）、也没有赔偿的平台配送订单退货 */
    List<FbaViews.UnreturnedRefund> selectUnreturnedRefunds(@Param("storeId") Long storeId, @Param("refundBefore") LocalDateTime refundBefore,
                                                            @Param("refundAfter") LocalDateTime refundAfter);

    /** 在线商品的售价、佣金率、FNSKU（估算索赔单价） */
    List<FbaViews.ListingValue> selectListingValues(@Param("storeId") Long storeId);

    /** 货件对账（头程货件明细的发货件数与亚马逊实收件数） */
    IPage<FbaViews.ReceiptRow> selectReceiptPage(IPage<FbaViews.ReceiptRow> page, @Param("storeId") Long storeId, @Param("keyword") String keyword,
                                                 @Param("shortOnly") Boolean shortOnly);

    /** 已签收、有实收记录且短少的货件明细 */
    List<FbaViews.ReceiptRow> selectShortages(@Param("storeId") Long storeId);
}
