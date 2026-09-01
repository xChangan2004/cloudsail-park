package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.IParkingOrderService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.query.ParkingOrderQuery;
import com.changan.model.vo.ParkingOrderDetailVO;
import com.changan.model.vo.ParkingOrderPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/parking/order")
@Tag(name = "停车场订单相关接口")
@RequiredArgsConstructor
public class ParkingOrderController {

    private final IParkingOrderService parkingOrderService;

    @GetMapping("/page")
    @Operation(summary = "分页查询订单信息")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:order:list")
    public R<PageDTO<ParkingOrderPageVO>> queryOrderPage(ParkingOrderQuery query) {
        return R.ok(parkingOrderService.queryOrderPage(query));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭订单")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:order:close")
    @OperLog(type = "停车订单", subType = "关闭订单")
    public R<Void> closeOrder(@PathVariable @NotNull(message = "订单ID不能为空") Long id) {
        parkingOrderService.closeOrder(id);
        return R.ok();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询订单详情")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "parking:order:list")
    public R<ParkingOrderDetailVO> queryOrderById(@PathVariable @NotNull(message = "订单ID不能为空") Long id) {
        return R.ok(parkingOrderService.queryOrderById(id));
    }
}
