package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 偏远地区邮编段：同一国家的邮编（去空格、大写）按字符串在 [起, 止] 之间，或以起 / 止为前缀时视为偏远地区。
 * 地址校验时识别，订单记为偏远地区（审单规则可按此条件挂起 / 指定渠道 / 打标签）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_remote_area")
public class EcomRemoteArea extends BaseEntity {

    @NotBlank(message = "{ecom.valid.ord.countryRequired}")
    @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}")
    private String country;
    @NotBlank(message = "{ecom.valid.ord.postcodeFromRequired}")
    @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}")
    private String postcodeFrom;
    /** 为空时与起相同（前缀匹配） */
    @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}")
    private String postcodeTo;
    @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}")
    private String name;
    private EnableStatus status;
    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;
}
