package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.CouponTemplateFormDTO;
import com.changan.model.vo.CouponTemplatePageVO;
import com.changan.model.vo.CouponTemplateQuery;
import com.changan.park.service.ICouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/coupon/template")
@RequiredArgsConstructor
@Tag(name = "优惠券相关接口")
public class CouponTemplateController {

    private final ICouponTemplateService couponTemplateService;

    @PostMapping
    @Operation(summary = "保存优惠券模板")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "coupon:add")
    @OperLog(type = "优惠券", subType = "保存优惠券模板")
    public R<Void> saveCouponTemplate(@RequestBody @Valid CouponTemplateFormDTO dto) {
        couponTemplateService.saveCouponTemplate(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改优惠券模板")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "coupon:edit")
    @OperLog(type = "优惠券", subType = "修改优惠券模板")
    public R<Void> updateCouponTemplate(@RequestBody @Valid CouponTemplateFormDTO dto) {
        couponTemplateService.updateCouponTemplate(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除优惠券模板")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "coupon:delete")
    @OperLog(type = "优惠券", subType = "删除优惠券模板")
    public R<Void> deleteCouponTemplateById(@PathVariable Long id) {
        couponTemplateService.deleteCouponTemplateById(id);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询模板信息")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "coupon:list")
    @OperLog(type = "优惠券", subType = "分页查询模板信息")
    public R<PageDTO<CouponTemplatePageVO>> queryCouponTemplatePage(CouponTemplateQuery query) {
        return R.ok(couponTemplateService.queryCouponTemplatePage(query));
    }

    @PutMapping("/status/{id}")
    @Operation(summary = "上下架优惠券模板")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "coupon:status")
    @OperLog(type = "优惠券管理", subType = "上下架优惠券模板")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam CommonStatus status) {
        couponTemplateService.updateCouponTemplateStatus(id, status);
        return R.ok();
    }
}
