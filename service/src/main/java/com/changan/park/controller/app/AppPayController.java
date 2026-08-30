package com.changan.park.controller.app;

import com.changan.common.utils.StpKit;
import com.changan.park.service.IPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/pay")
@RequiredArgsConstructor
@Tag(name = "支付相关接口")
public class AppPayController {

    private final IPayService payService;

    @GetMapping(value = "/{orderId}")
    @Operation(summary = "发起H5支付")
    public String createPay(@PathVariable Long orderId,
                            @RequestParam(required = false) Long couponId) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return payService.createMyPay(orderId, couponId, customerId);
    }
}