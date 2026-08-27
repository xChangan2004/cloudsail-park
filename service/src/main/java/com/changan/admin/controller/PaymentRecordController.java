package com.changan.admin.controller;

import com.changan.admin.service.IPaymentRecordService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.PaymentRecordQuery;
import com.changan.model.vo.PaymentRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "支付记录相关接口")
public class PaymentRecordController {

    private final IPaymentRecordService paymentRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询支付记录")
    public R<PageDTO<PaymentRecordVO>> queryPaymentRecordPage(PaymentRecordQuery query) {
        return R.ok(paymentRecordService.queryPaymentRecordPage(query));
    }
}
