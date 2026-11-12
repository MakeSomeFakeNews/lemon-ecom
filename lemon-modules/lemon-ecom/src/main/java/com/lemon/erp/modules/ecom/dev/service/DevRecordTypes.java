package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 开发项目的附件（调研报告、报价单、样品照片等），按开发项目的数据权限访问
 */
@Component
@RequiredArgsConstructor
public class DevRecordTypes implements RecordTypeProvider {

    private final EcomDevProjectMapper projectMapper;

    @Override
    public List<RecordType> recordTypes() {
        return List.of(new RecordType("ecom_dev_project", "ecom.label.dev.project", "ecom:devProject:list", "ecom:devProject:edit",
                EcomDevProjectService.RULE, projectMapper::selectById));
    }
}
