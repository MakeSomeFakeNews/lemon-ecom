package com.lemon.erp.modules.ecom.oper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.entity.EcomStoreMember;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 店铺授权
 */
public interface EcomStoreMemberMapper extends BaseMapper<EcomStoreMember> {

    /** 用户可见的店铺：负责的（owner_id）与被授权的 */
    List<Long> selectVisibleStoreIds(@Param("userId") Long userId);

    /** 店铺授权页：店铺与负责人 */
    IPage<OperViews.StoreAuthRow> selectStorePage(IPage<OperViews.StoreAuthRow> page, @Param("keyword") String keyword,
                                                  @Param("platform") EcomPlatform platform, @Param("userId") Long userId);

    /** 店铺的授权成员（含用户名、昵称、店铺名） */
    List<EcomStoreMember> selectByStoreIds(@Param("storeIds") Collection<Long> storeIds);

    /** 用户被授权的店铺 */
    List<EcomStoreMember> selectByUser(@Param("userId") Long userId);

    /** 用户负责的店铺 */
    List<OperViews.StoreBrief> selectOwnedStores(@Param("userId") Long userId);

    /** 批量授权（单条多值 INSERT） */
    int insertBatch(@Param("items") List<EcomStoreMember> items);

    /** 按登录名查找账号 ID（演示数据用，只读） */
    Long selectUserIdByUsername(@Param("username") String username);

    /** 用户昵称（只读），不存在为 null */
    String selectNickname(@Param("userId") Long userId);
}
