package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.common.domain.R;
import com.changan.model.dto.RefundFormDTO;
import com.changan.park.service.IPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/pay")
@RequiredArgsConstructor
@Tag(name = "支付相关接口")
public class PayController {

    private final IPayService payService;

    @PostMapping("/refund")
    @Operation(summary = "退款（全部）")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "pay:refund")
    @OperLog(type = "支付", subType = "订单退款")
    public R<Void> refund(@RequestBody @Valid RefundFormDTO dto) {
        payService.refund(dto);
        return R.ok();
    }
}
