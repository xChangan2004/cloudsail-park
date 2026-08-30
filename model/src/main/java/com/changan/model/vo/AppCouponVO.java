package com.changan.model.vo;

import com.changan.common.enums.CustomerCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户端券包VO")
public class AppCouponVO {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "使用门槛")
    private BigDecimal threshold;

    @Schema(description = "抵扣金额")
    private BigDecimal amount;

    @Schema(description = "券状态")
    private CustomerCouponStatus status;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;
}