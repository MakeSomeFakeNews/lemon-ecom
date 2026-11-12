package com.lemon.erp.modules.ecom.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.ecom.entity.EcomPackageTrack;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EcomPackageTrackMapper extends BaseMapper<EcomPackageTrack> {

    int insertBatch(@Param("items") List<EcomPackageTrack> items);
}
