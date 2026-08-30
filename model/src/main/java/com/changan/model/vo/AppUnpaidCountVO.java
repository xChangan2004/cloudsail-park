package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "我的待支付订单统计VO")
public class AppUnpaidCountVO {

    @Schema(description = "待支付订单数（角标用）")
    private Long count;

    @Schema(description = "待支付总金额（元）")
    private BigDecimal totalAmount;
}