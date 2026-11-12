package com.lemon.erp.modules.ecom.mapper;

import com.lemon.erp.modules.ecom.dto.EcomProfitViews;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 多维利润报表与 VAT 汇总的统计（只读，金额为本位币）。利润报表先在数据库中按 店铺 × 平台 SKU × 收件国家 × 月份 汇总为「单元」，
 * 再由 EcomProfitService 汇总到所选维度
 */
public interface EcomProfitMapper {

    /** 订单口径：按下单时间，订单级金额按本行金额占订单各行合计的比例分摊到行 */
    List<EcomProfitViews.Cell> selectOrderCells(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** 结算口径：挂订单的结算明细按入账日期、类型汇总（分摊到订单行） */
    List<EcomProfitViews.TypedCell> selectSettlementOrderItems(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 结算口径：不挂订单的订单收入、退款、代收代缴税（按店铺计入，SKU 为明细上的 SKU） */
    List<EcomProfitViews.TypedCell> selectSettlementUnlinked(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 结算口径：首次结算（订单收入入账）在期间内的订单的件数、成本、头程、尾程、税额 */
    List<EcomProfitViews.Cell> selectSettledOrderCells(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 费用分摊结果按 店铺 × SKU × 月份 × 类型 汇总（本位币） */
    List<EcomProfitViews.TypedCell> selectAllocations(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 广告花费按 店铺 × SKU × 月份 汇总（店铺币种，amount 为花费） */
    List<EcomProfitViews.TypedCell> selectAdSpend(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);

    /** 店铺属性：名称、平台、站点、币种、负责人 */
    List<EcomProfitViews.StoreAttr> selectStores();

    /** 在线商品属性：父商品、标题、映射物料 */
    List<EcomProfitViews.SkuAttr> selectSkuAttrs(@Param("storeId") Long storeId);

    /** 物料编码、名称 */
    List<EcomProfitViews.SkuAttr> selectMaterials(@Param("ids") java.util.Collection<Long> ids);

    /** VAT 汇总：按收件国家 × 月份 × 税种 × 是否代扣 × 币种（只含有税种的订单） */
    List<EcomProfitViews.VatRow> selectVatSales(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("storeId") Long storeId);

    /** VAT 汇总的进项：已发运头程货件的关税、进口增值税，按店铺站点 × 发运月份 */
    List<EcomProfitViews.VatRow> selectVatInputs(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("storeId") Long storeId);
}
