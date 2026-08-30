package com.changan.park.controller.notify;

import com.changan.park.service.IPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
@Tag(name = "支付回调接口（支付宝服务器调用）")
public class PayNotifyController {

    private final IPayService payService;

    @PostMapping("/alipay")
    @Operation(summary = "支付宝异步通知（验签+落账，返回success/failure纯文本）")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, String.join(",", v)));
        return payService.handleNotify(params) ? "success" : "failure";
    }
}
