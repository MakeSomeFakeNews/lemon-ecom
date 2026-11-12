package com.lemon.erp.modules.ecom.fba.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.fba.entity.EcomAmzReport;
import com.lemon.erp.modules.ecom.fba.model.AmzReportStatus;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import org.apache.ibatis.annotations.Param;

public interface EcomAmzReportMapper extends BaseMapper<EcomAmzReport> {

    IPage<EcomAmzReport> selectPageList(IPage<EcomAmzReport> page, @Param("storeId") Long storeId, @Param("reportType") AmzReportType reportType,
                                        @Param("status") AmzReportStatus status);
}
