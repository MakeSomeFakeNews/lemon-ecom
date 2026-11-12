package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.modules.ecom.owh.model.OwhProvider;

import java.time.LocalDate;
import java.util.List;

/**
 * 第三方海外仓接口（SPI）：每个对接的服务商实现一个并注册为 Bean，由 {@link OwhRegistry} 按 {@link OwhProvider} 查找。
 * 与平台连接器（ChannelConnector）、物流商面单接口（CarrierConnector）相互独立。
 * <p>
 * SKU 统一使用本地物料编码（在海外仓后台登记商品时使用相同的 SKU）。调用方保证不在数据库事务中调用；
 * 接口错误统一抛出 {@code ecom.error.owh.*} 业务异常
 */
public interface OwhConnector {

    OwhProvider provider();

    /** 授权字段、正式 / 测试环境地址、能力 */
    OwhInfo info();

    /** 测试授权：默认拉取仓库列表 */
    default void test(OwhContext ctx) {
        warehouses(ctx);
    }

    /** 服务商的仓库列表 */
    List<OwhData.Warehouse> warehouses(OwhContext ctx);

    /** 物流产品（尾程渠道）列表；warehouseCode 为空时返回全部 */
    List<OwhData.Product> products(OwhContext ctx, String warehouseCode);

    /** 仓库的库存（全部 SKU，内部分页取完） */
    List<OwhData.Stock> inventory(OwhContext ctx, String warehouseCode);

    /** 创建入库单（ASN），返回服务商入库单号 */
    String createInbound(OwhContext ctx, OwhData.InboundDraft draft);

    /** 查询入库单状态与各 SKU 的收货数量 */
    OwhData.InboundState queryInbound(OwhContext ctx, String owhOrderNo);

    /** 创建出库单（一件代发），返回服务商出库单号（部分服务商同时返回运单号） */
    OwhData.OutboundResult createOutbound(OwhContext ctx, OwhData.OutboundDraft draft);

    /** 查询出库单状态、运单号 */
    OwhData.OutboundState queryOutbound(OwhContext ctx, String owhOrderNo);

    /**
     * 取消出库单；服务商已开始作业时提交截单申请
     *
     * @return true 已取消；false 已提交截单，需要之后同步结果
     */
    boolean cancelOutbound(OwhContext ctx, String owhOrderNo, String reason);

    /** 费用明细（按费用日期，含首尾两天） */
    List<OwhData.Fee> fees(OwhContext ctx, LocalDate begin, LocalDate end);

    /** 创建退件单，返回服务商退件单号 */
    String createReturn(OwhContext ctx, OwhData.ReturnDraft draft);

    /** 查询退件单状态与各 SKU 的良品 / 不良品数量 */
    OwhData.ReturnState queryReturn(OwhContext ctx, String owhOrderNo);

    /**
     * 调拨出库（发往另一个海外仓或本地仓，收件人为调入仓的地址）。默认按出库单创建，服务商有专门的 B2B / 调拨出库接口时覆盖
     */
    default OwhData.OutboundResult createTransferOut(OwhContext ctx, OwhData.OutboundDraft draft) {
        return createOutbound(ctx, draft);
    }

    /** 查询调拨出库单（状态、运单号），默认同出库单 */
    default OwhData.OutboundState queryTransferOut(OwhContext ctx, String owhOrderNo) {
        return queryOutbound(ctx, owhOrderNo);
    }

    /**
     * 取消已推送的入库单（服务商还没有收货时）。服务商没有取消接口时抛出 {@code ecom.error.owh.cancelUnsupported}，
     * 页面提示到服务商后台作废后「标记已作废」；能力 {@link com.lemon.erp.modules.ecom.owh.model.OwhCapability#CANCEL_INBOUND}
     */
    default void cancelInbound(OwhContext ctx, String owhOrderNo) {
        throw new com.lemon.erp.common.exception.BizException("ecom.error.owh.cancelUnsupported", info().provider().name());
    }

    /** 取消已推送的退件单（服务商还没有收货时）；没有接口时同 {@link #cancelInbound} */
    default void cancelReturn(OwhContext ctx, String owhOrderNo) {
        throw new com.lemon.erp.common.exception.BizException("ecom.error.owh.cancelUnsupported", info().provider().name());
    }
}
