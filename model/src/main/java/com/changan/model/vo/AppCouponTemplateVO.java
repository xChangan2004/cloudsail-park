package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "领券中心-可领券列表VO")
public class AppCouponTemplateVO {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "券名称")
    private String name;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "抵扣金额")
    private BigDecimal amount;

    @Schema(description = "有效天数（领取后N天有效）")
    private Integer validDays;

    @Schema(description = "剩余可领数")
    private Integer remainCount;

    @Schema(description = "我已领取张数")
    private Integer received;

    @Schema(description = "是否可再领（前端据此置灰按钮）")
    private Boolean canReceive;
}