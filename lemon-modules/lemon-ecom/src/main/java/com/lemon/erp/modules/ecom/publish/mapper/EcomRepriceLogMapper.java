package com.lemon.erp.modules.ecom.publish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceLog;
import com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 自动调价日志 */
public interface EcomRepriceLogMapper extends BaseMapper<EcomRepriceLog> {

    int insertBatch(@Param("items") List<EcomRepriceLog> items);

    Page<EcomRepriceLog> selectPageList(Page<EcomRepriceLog> page, @Param("ruleId") Long ruleId, @Param("storeId") Long storeId,
                                        @Param("status") RepriceLogStatus status, @Param("keyword") String keyword,
                                        @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
