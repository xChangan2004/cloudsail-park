package com.changan.park.controller.admin;

import com.changan.park.service.IPayService;
import com.changan.common.domain.R;
import com.changan.model.dto.RefundFormDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/pay")
@RequiredArgsConstructor
@Tag(name = "支付相关接口")
public class PayController {

    private final IPayService payService;

    @GetMapping(value = "/{orderNo}", produces = "text/html")
    @Operation(summary = "创建支付（返回支付页表单HTML）")
    public String createPay(@PathVariable @NotBlank(message = "订单号不能为空") String orderNo) {
        return payService.createPay(orderNo);
    }

    @PostMapping("/notify")
    @Operation(summary = "支付宝异步通知")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, String.join(",", v)));
        return payService.handleNotify(params) ? "success" : "failure";
    }

    @PostMapping("/refund")
    @Operation(summary = "退款（全部）")
    public R<Void> refund(@RequestBody @Valid RefundFormDTO dto) {
        payService.refund(dto);
        return R.ok();
    }
}
