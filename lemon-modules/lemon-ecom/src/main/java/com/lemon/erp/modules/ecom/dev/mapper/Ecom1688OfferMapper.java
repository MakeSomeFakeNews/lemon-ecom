package com.lemon.erp.modules.ecom.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Offer;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface Ecom1688OfferMapper extends BaseMapper<Ecom1688Offer> {

    /**
     * 分页：带物料、供应商名称
     *
     * @param keyword    物料编码 / 名称、1688 标题、offerId、卖家
     * @param priceAlert 1 只看价格变动未确认的
     */
    IPage<Ecom1688Offer> selectPageList(IPage<Ecom1688Offer> page, @Param("keyword") String keyword, @Param("priceAlert") Integer priceAlert,
                                        @Param("supplierId") Long supplierId);

    /** 指定物料的关联（带物料名称） */
    List<Ecom1688Offer> selectByMaterials(@Param("materialIds") Collection<Long> materialIds);
}
