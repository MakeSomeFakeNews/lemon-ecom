package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.entity.EcomAdRuleLog;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EcomAdRuleLogMapper extends BaseMapper<EcomAdRuleLog> {

    IPage<EcomAdRuleLog> selectPageList(IPage<EcomAdRuleLog> page, @Param("ruleId") Long ruleId, @Param("storeId") Long storeId,
                                        @Param("status") EcomAdLogStatus status, @Param("source") EcomAdLogSource source,
                                        @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end, @Param("keyword") String keyword);

    int insertBatch(@Param("items") List<EcomAdRuleLog> items);
}
