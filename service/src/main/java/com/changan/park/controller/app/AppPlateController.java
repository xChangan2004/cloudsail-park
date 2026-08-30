package com.changan.park.controller.app;

import com.changan.common.domain.R;
import com.changan.common.utils.StpKit;
import com.changan.model.po.CustomerPlate;
import com.changan.park.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/plate")
@RequiredArgsConstructor
@Tag(name = "车牌管理相关接口")
public class AppPlateController {

    private final ICustomerService customerService;

    @PostMapping
    @Operation(summary = "绑定车牌")
    public R<Void> bindPlate(@Valid @RequestBody CustomerPlate plate) {
        plate.setCustomerId(StpKit.APP.getLoginIdAsLong());
        customerService.bindPlate(plate);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "解绑车牌")
    public R<Void> unbindPlate(@PathVariable Long id) {
        customerService.unBindPlate(id, false);
        return R.ok();
    }

    @GetMapping("/list")
    @Operation(summary = "我的车牌列表")
    public R<List<CustomerPlate>> listMyPlate() {
        return R.ok(customerService.queryCustomerPlateListByCustomerId(StpKit.APP.getLoginIdAsLong()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询车牌详情")
    public R<CustomerPlate> queryPlateById(@PathVariable Long id) {
        return R.ok(customerService.queryCustomerPlateById(id));
    }
}
