package com.lemon.erp.modules.ecom.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportStore;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 运营日报的店铺范围 */
public interface EcomOpsReportStoreMapper extends BaseMapper<EcomOpsReportStore> {

    int insertBatch(@Param("items") List<EcomOpsReportStore> items);
}
