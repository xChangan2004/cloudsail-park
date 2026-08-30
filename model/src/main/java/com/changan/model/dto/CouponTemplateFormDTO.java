package com.changan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponTemplateFormDTO {

    @Schema(description = "模板ID（修改时必传）")
    private Long id;

    @NotBlank(message = "券名称不能为空")
    @Schema(description = "券名称")
    private String name;

    @NotNull(message = "使用门槛不能为空")
    @DecimalMin(value = "0", message = "使用门槛不能低于0")
    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @NotNull(message = "抵扣金额不能为空")
    @DecimalMin(value = "1", message = "抵扣金额必须大于0")
    @Schema(description = "抵扣金额")
    private BigDecimal amount;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量必须大于0")
    @Schema(description = "发放总量")
    private Integer totalCount;

    @Min(value = 1, message = "每人限领数必须大于0")
    @Schema(description = "每人限领数")
    private Integer perLimit;

    @Min(value = 1, message = "有效天数必须大于0")
    @Schema(description = "有效天数")
    private Integer validDays;
}
