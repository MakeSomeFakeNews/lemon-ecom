package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.scm.dto.ReorderSuggestion;
import com.lemon.erp.modules.scm.service.ScmReorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 补货建议（再订货点）
 */
@RestController
@RequestMapping("/scm/reorder")
@RequiredArgsConstructor
public class ScmReorderController {

    private final ScmReorderService reorderService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('scm:reorder:list')")
    public R<List<ReorderSuggestion>> suggestions(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "true") boolean onlyNeeded) {
        return R.ok(reorderService.suggestions(keyword, onlyNeeded));
    }
}
