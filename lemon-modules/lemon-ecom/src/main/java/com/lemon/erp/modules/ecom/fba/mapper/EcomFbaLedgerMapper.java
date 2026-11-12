package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaLedger;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface EcomFbaLedgerMapper extends BaseMapper<EcomFbaLedger> {

    IPage<EcomFbaLedger> selectPageList(IPage<EcomFbaLedger> page, @Param("storeId") Long storeId, @Param("eventType") String eventType,
                                        @Param("keyword") String keyword, @Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate);

    /** 店铺 since 之后的调整类流水（原因在 reasons 中），识别丢失 / 损坏 / 找到 */
    List<EcomFbaLedger> selectAdjustments(@Param("storeId") Long storeId, @Param("since") LocalDate since,
                                          @Param("reasons") Collection<String> reasons);

    /** 已存在的行摘要（去重） */
    List<String> selectExistingKeys(@Param("storeId") Long storeId, @Param("keys") Collection<String> keys);

    /** 批量写入 */
    int insertBatch(@Param("list") List<EcomFbaLedger> list);
}
