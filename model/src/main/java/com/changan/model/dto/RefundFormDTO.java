package com.changan.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundFormDTO {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号")
    private String orderNo;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 255, message = "退款原因不能超过255个字")
    @Schema(description = "退款原因")
    private String reason;
}
