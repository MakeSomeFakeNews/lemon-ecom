package com.lemon.erp.modules.ecom.owh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransferItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomOwhTransferItemMapper extends BaseMapper<EcomOwhTransferItem> {

    List<EcomOwhTransferItem> selectByTransfer(@Param("transferId") Long transferId);

    void insertBatch(@Param("list") List<EcomOwhTransferItem> list);
}
