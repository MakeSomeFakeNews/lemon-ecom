package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackEvent;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.dto.LabelRequest;
import com.lemon.erp.modules.ecom.dto.PackageQuery;
import com.lemon.erp.modules.ecom.dto.PackageUpdateRequest;
import com.lemon.erp.modules.ecom.dto.PickList;
import com.lemon.erp.modules.ecom.dto.ScanRequest;
import com.lemon.erp.modules.ecom.dto.TrackingRequest;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.service.EcomPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 包裹与打单 /ecom/package：申请运单号、面单、拣货单、发货（批量 / 扫描）、取消
 */
@RestController
@RequestMapping("/ecom/package")
@RequiredArgsConstructor
public class EcomPackageController {

    private final EcomPackageService packageService;

    /** 分页：storeId、warehouseId、channelId、status、keyword（包裹号 / 运单号 / 平台订单号 / 收件人）、beginDate、endDate */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:package:list')")
    public R<PageResult<EcomPackage>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, PackageQuery query) {
        return R.ok(packageService.page(current, size, query));
    }

    /** 各状态的包裹数：{ 状态: 数量 } */
    @GetMapping("/status-counts")
    @PreAuthorize("@ss.hasPerm('ecom:package:list')")
    public R<Map<String, Long>> statusCounts(PackageQuery query) {
        return R.ok(packageService.statusCounts(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:package:list')")
    public R<EcomPackage> get(@PathVariable Long id) {
        return R.ok(packageService.detail(id));
    }

    /** 批量申请运单号，返回失败（或面单获取失败的提示）的包裹：{ 包裹号: 原因 } */
    @PostMapping("/label")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    @OperLog(title = "包裹申请运单号", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> label(@Validated @RequestBody LabelRequest req) {
        return R.ok(packageService.label(req));
    }

    @PostMapping("/{id}/label-file")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    @OperLog(title = "包裹申请运单号", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> refetchLabel(@PathVariable Long id) {
        packageService.refetchLabel(id);
        return R.ok();
    }

    /** 手工填写运单号 */
    @PutMapping("/{id}/tracking")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    @OperLog(title = "包裹填写运单号", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> tracking(@PathVariable Long id, @Validated @RequestBody TrackingRequest req) {
        packageService.manualTracking(id, req);
        return R.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    @OperLog(title = "包裹", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody PackageUpdateRequest req) {
        packageService.update(id, req);
        return R.ok();
    }

    /** 单个包裹的面单 PDF */
    @GetMapping("/{id}/label")
    @PreAuthorize("@ss.hasPerm('ecom:package:list')")
    public ResponseEntity<byte[]> labelPdf(@PathVariable Long id) {
        return pdf(packageService.labelPdf(id), "label-" + id + ".pdf");
    }

    /** 批量打印面单：合并为一个 PDF */
    @PostMapping("/labels")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    public ResponseEntity<byte[]> labelsPdf(@Validated @RequestBody IdsRequest req) {
        return pdf(packageService.labelsPdf(req.ids()), "labels.pdf");
    }

    /** 拣货单（按仓库，每个仓库一张，按库位排序），前端按打印模板 ecom.pickList 打印 */
    @PostMapping("/pick-list")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    public R<List<PickList>> pickList(@Validated @RequestBody IdsRequest req) {
        return R.ok(packageService.pickList(req.ids()));
    }

    /** 批量发货，返回失败的包裹 { 包裹号: 原因 } */
    @PostMapping("/ship")
    @PreAuthorize("@ss.hasPerm('ecom:package:ship')")
    @OperLog(title = "包裹发货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Map<String, String>> ship(@Validated @RequestBody IdsRequest req) {
        return R.ok(packageService.ship(req.ids(), null));
    }

    /** 扫描发货：扫运单号（或包裹号），校验后发货，返回包裹详情 */
    @PostMapping("/scan")
    @PreAuthorize("@ss.hasPerm('ecom:package:ship')")
    @OperLog(title = "包裹扫描发货", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<EcomPackage> scan(@Validated @RequestBody ScanRequest req) {
        return R.ok(packageService.scan(req.code()));
    }

    /** 取消包裹（已在物流商下单的同时取消物流商订单） */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:package:label')")
    @OperLog(title = "包裹取消", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        packageService.cancel(id);
        return R.ok();
    }

    /** 物流轨迹（对接了物流商的包裹） */
    @GetMapping("/{id}/track")
    @PreAuthorize("@ss.hasPerm('ecom:package:list')")
    public R<List<CarrierTrackEvent>> track(@PathVariable Long id) {
        return R.ok(packageService.track(id));
    }

    private static ResponseEntity<byte[]> pdf(byte[] data, String name) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(data);
    }
}
