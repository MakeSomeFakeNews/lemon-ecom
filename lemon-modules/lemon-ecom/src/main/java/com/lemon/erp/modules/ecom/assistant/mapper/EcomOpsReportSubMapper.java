package com.lemon.erp.modules.ecom.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportSub;

import java.util.List;

/** 运营日报订阅 */
public interface EcomOpsReportSubMapper extends BaseMapper<EcomOpsReportSub> {

    /** 全部订阅（含订阅人昵称），按订阅人 */
    List<EcomOpsReportSub> selectListWithUser();
}
