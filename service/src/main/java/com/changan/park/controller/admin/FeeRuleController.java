package com.changan.park.controller.admin;

import com.changan.park.service.IFeeRuleService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.FeeRuleFormDTO;
import com.changan.model.po.FeeRule;
import com.changan.model.query.FeeRuleQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "停车场计费规则相关接口")
@RestController
@RequestMapping("/admin/parking/fee-rule")
@RequiredArgsConstructor
public class FeeRuleController {

    private final IFeeRuleService feeRuleService;

    @PostMapping
    @Operation(summary = "新增计费规则")
    public R<Void> saveFeeRule(@RequestBody @Valid FeeRuleFormDTO dto) {
        feeRuleService.saveFeeRule(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除计费规则")
    public R<Void> deleteFeeRuleById(@PathVariable Long id) {
        feeRuleService.deleteFeeRuleById(id);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询计费规则")
    public R<PageDTO<FeeRule>> queryFeeRulePage(FeeRuleQuery query) {
        return R.ok(feeRuleService.queryFeeRulePage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询计费详情")
    public R<FeeRule> queryFeeRuleById(@PathVariable Long id) {
        return R.ok(feeRuleService.queryFeeRuleById(id));
    }

    @PutMapping
    @Operation(summary = "修改计费规则")
    public R<Void> updateFeeRule(@RequestBody @Valid FeeRuleFormDTO dto) {
        feeRuleService.updateFeeRule(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "切换计费规则状态")
    public R<Void> updateFeeRuleStatus(
            @PathVariable @NotNull(message = "规则ID不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") CommonStatus status
    ) {
        feeRuleService.updateFeeRuleStatus(id, status);
        return R.ok();
    }
}
