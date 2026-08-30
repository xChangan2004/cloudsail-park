package com.changan.park.controller.app;

import com.changan.common.utils.StpKit;
import com.changan.park.service.IPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/pay")
@RequiredArgsConstructor
@Tag(name = "支付相关接口")
public class AppPayController {

    private final IPayService payService;

    @GetMapping(value = "/{orderId}", produces = "text/html")
    @Operation(summary = "发起H5支付（返回支付页表单HTML，跳转手机收银台）")
    public String createPay(@PathVariable Long orderId) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return payService.createMyPay(orderId, customerId);
    }
}