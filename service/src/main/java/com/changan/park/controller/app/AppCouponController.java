package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.common.enums.CustomerCouponStatus;
import com.changan.common.utils.StpKit;
import com.changan.model.vo.AppCouponTemplateVO;
import com.changan.model.vo.AppCouponVO;
import com.changan.park.service.ICustomerCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/app/coupon")
@RequiredArgsConstructor
@Tag(name = "优惠券相关接口")
public class AppCouponController {

    private final ICustomerCouponService customerCouponService;

    @PostMapping("/receive/{templateId}")
    @Operation(summary = "领取优惠券")
    public R<Void> receiveCoupon(@PathVariable Long templateId) {
        long customerId = StpKit.APP.getLoginIdAsLong();
        customerCouponService.receiveCoupon(templateId, customerId);
        return R.ok();
    }

    @GetMapping("/mine")
    @Operation(summary = "我的券包")
    public R<List<AppCouponVO>> mine(CustomerCouponStatus status) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(customerCouponService.queryMyCouponPage(status, customerId));
    }

    @GetMapping("/usable")
    @Operation(summary = "查询指定金额下可用的优惠券（支付页选券弹层）")
    public R<List<AppCouponVO>> usable(BigDecimal amount) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(customerCouponService.queryUsableCoupons(customerId, amount));
    }

    @GetMapping("/templates")
    @Operation(summary = "可领券列表")
    public R<List<AppCouponTemplateVO>> templates() {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(customerCouponService.queryReceiveableList(customerId));
    }
}
