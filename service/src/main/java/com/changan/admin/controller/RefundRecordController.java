package com.changan.admin.controller;

import com.changan.admin.service.IRefundRecordService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.RefundRecordQuery;
import com.changan.model.vo.RefundRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
@Tag(name = "退款记录相关接口")
public class RefundRecordController {

    private final IRefundRecordService refundRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退款记录")
    public R<PageDTO<RefundRecordVO>> queryRefundRecordPage(RefundRecordQuery query) {
        return R.ok(refundRecordService.queryRefundRecordPage(query));
    }
}
