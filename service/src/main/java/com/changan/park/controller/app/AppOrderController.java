package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.utils.StpKit;
import com.changan.model.query.AppOrderQuery;
import com.changan.model.vo.AppOrderDetailVO;
import com.changan.model.vo.AppOrderVO;
import com.changan.model.vo.AppUnpaidCountVO;
import com.changan.model.vo.ParkingOrderDetailVO;
import com.changan.park.service.IParkingOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/orders")
@RequiredArgsConstructor
@Tag(name = "订单相关接口")
public class AppOrderController {

    private final IParkingOrderService parkingOrderService;

    @GetMapping
    @Operation(summary = "我的订单分页列表")
    public R<PageDTO<AppOrderVO>> pageMyOrders(AppOrderQuery query) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(parkingOrderService.queryMyOrderPage(query, customerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "我的订单详情")
    public R<AppOrderDetailVO> myOrderDetail(@PathVariable Long id) {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(parkingOrderService.queryMyOrderDetail(id, customerId));
    }

    @GetMapping("/unpaid-count")
    @Operation(summary = "我的待支付订单数（角标+欠费提醒）")
    public R<AppUnpaidCountVO> unpaidCount() {
        Long customerId = StpKit.APP.getLoginIdAsLong();
        return R.ok(parkingOrderService.queryMyUnpaidCount(customerId));
    }
}
