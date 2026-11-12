package com.lemon.erp.modules.ecom.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoTodo;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import org.apache.ibatis.annotations.Param;


public interface EcomAutoTodoMapper extends BaseMapper<EcomAutoTodo> {

    IPage<EcomAutoTodo> selectPageList(IPage<EcomAutoTodo> page, @Param("assigneeId") Long assigneeId, @Param("status") AutoTodoStatus status,
                                      @Param("keyword") String keyword);
}
