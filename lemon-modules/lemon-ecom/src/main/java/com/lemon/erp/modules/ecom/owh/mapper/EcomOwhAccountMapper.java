package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhAccountMapper extends BaseMapper<EcomOwhAccount> {

    IPage<EcomOwhAccount> selectPageList(IPage<EcomOwhAccount> page, @Param("keyword") String keyword, @Param("provider") OwhProvider provider,
                                        @Param("status") EnableStatus status);
}
