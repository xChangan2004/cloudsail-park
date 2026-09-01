package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.ICustomerService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.CustomerFormDTO;
import com.changan.model.po.Customer;
import com.changan.model.po.CustomerPlate;
import com.changan.model.query.CustomerQuery;
import com.changan.model.query.PlateQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "客户相关接口")
@RestController
@RequestMapping("/admin/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final ICustomerService customerService;

    @PostMapping
    @Operation(summary = "新增客户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:add")
    @OperLog(type = "客户", subType = "新增客户")
    public R<Void> saveCustomer(@RequestBody @Valid CustomerFormDTO dto) {
        customerService.saveCustomer(dto);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改客户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:edit")
    @OperLog(type = "客户", subType = "修改客户")
    public R<Void> updateCustomer(@RequestBody @Valid CustomerFormDTO dto) {
        customerService.updateCustomer(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}/wechat/unbind")
    @Operation(summary = "取消微信关联", description = "解除微信openId绑定；客户不存在、未绑定微信会抛出业务异常")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:edit")
    @OperLog(type = "客户", subType = "取消微信关联")
    public R<Void> unbindWechat(@PathVariable @NotNull(message = "客户ID不能为空") Long id) {
        customerService.unbindWechat(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换客户状态")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:edit")
    @OperLog(type = "客户", subType = "切换客户状态")
    public R<Void> updateCustomerStatus(
            @PathVariable @NotNull(message = "客户ID不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") CommonStatus status) {
        customerService.updateCustomerStatus(id, status);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询客户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:list")
    public R<PageDTO<Customer>> queryCustomerPage(CustomerQuery query) {
        return R.ok(customerService.queryCustomerPage(query));
    }

    @PostMapping("/plate")
    @Operation(summary = "绑定车牌")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:plate:add")
    @OperLog(type = "客户车牌", subType = "绑定车牌")
    public R<Void> bindPlate(@RequestBody @Valid CustomerPlate plate) {
        customerService.bindPlate(plate);
        return R.ok();
    }

    @DeleteMapping("/plate/{id}")
    @Operation(summary = "取消车牌绑定")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:plate:delete")
    @OperLog(type = "客户车牌", subType = "取消车牌绑定")
    public R<Void> unBindPlate(@PathVariable @NotNull(message = "车牌ID不能为空") Long id) {
        customerService.unBindPlate(id, true);
        return R.ok();
    }

    @GetMapping("/plate/list/{customerId}")
    @Operation(summary = "获取客户的车牌列表信息")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:plate:list")
    public R<List<CustomerPlate>> queryCustomerPlateListByCustomerId(
            @PathVariable @NotNull(message = "客户ID不能为空") Long customerId) {
        return R.ok(customerService.queryCustomerPlateListByCustomerId(customerId));
    }

    @GetMapping("/plate/page")
    @Operation(summary = "分页查询客户车牌信息")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "customer:plate:list")
    public R<PageDTO<CustomerPlate>> queryCustomerPlatePage(PlateQuery query) {
        return R.ok(customerService.queryCustomerPlatePage(query));
    }


}
