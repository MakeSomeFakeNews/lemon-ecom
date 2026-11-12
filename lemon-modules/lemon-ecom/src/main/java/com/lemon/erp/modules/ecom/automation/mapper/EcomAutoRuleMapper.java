package com.lemon.erp.modules.ecom.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface EcomAutoRuleMapper extends BaseMapper<EcomAutoRule> {

    /** 规则列表（创建人昵称、近 7 天失败次数） */
    IPage<EcomAutoRule> selectPageList(IPage<EcomAutoRule> page, @Param("keyword") String keyword, @Param("trigger") AutoTrigger trigger,
                                      @Param("status") EnableStatus status, @Param("failedSince") LocalDateTime failedSince);

    /** 执行了动作：执行次数 + 1，记录最近执行时间与结果 */
    int markTriggered(@Param("id") Long id, @Param("time") LocalDateTime time, @Param("result") AutoResult result);
}
