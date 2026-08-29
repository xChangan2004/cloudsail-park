package com.changan.model.dto;

import com.changan.common.enums.FeeRuleType;
import com.changan.model.po.FeeRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FeeRuleFormDTO {

    @Schema(description = "规则ID")
    private Long id;

    @NotNull(message = "停车场ID不能为空")
    @Schema(description = "停车场ID")
    private Long lotId;

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称")
    private String ruleName;

    @NotNull(message = "规则类型不能为空")
    @Schema(description = "规则类型")
    private FeeRuleType ruleType;

    @NotNull(message = "免费时长不能为空")
    @Min(value = 0, message = "免费时长不能小于0")
    @Schema(description = "免费时长（分钟）")
    private Integer freeMinutes;

    @Schema(description = "首小时费率")
    @DecimalMin(value = "0.0", message = "费率不能为负数")
    private BigDecimal firstHourRate;

    @Schema(description = "后续小时费率")
    @DecimalMin(value = "0.0", message = "费率不能为负数")
    private BigDecimal additionalRate;

    @Schema(description = "单日封顶金额")
    @DecimalMin(value = "0.0", message = "封顶金额不能为负数")
    private BigDecimal dailyCap;

    @Valid
    @Schema(description = "分时段费率配置")
    private List<FeeRule.TimeSegment> timeSegments;

    @Valid
    @Schema(description = "阶梯费率配置")
    private List<FeeRule.Tier> tiers;
}
