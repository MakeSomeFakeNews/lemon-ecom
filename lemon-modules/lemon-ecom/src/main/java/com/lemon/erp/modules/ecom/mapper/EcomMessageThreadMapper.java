package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.cs.dto.ThreadQuery;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 买家消息会话（客服）
 */
public interface EcomMessageThreadMapper extends BaseMapper<EcomMessageThread> {

    IPage<EcomMessageThread> selectPageList(IPage<EcomMessageThread> page, @Param("q") ThreadQuery q);

    EcomMessageThread selectDetail(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    /** 各状态的会话数（status → cnt），另含超时数 overdue；条件与列表相同、不含状态 */
    List<Map<String, Object>> countByStatus(@Param("q") ThreadQuery q);
}
