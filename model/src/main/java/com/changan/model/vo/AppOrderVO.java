package com.changan.model.vo;

import com.changan.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户端订单列表VO")
public class AppOrderVO {

    @Schema(description = "订单ID（跳转详情用）")
    private Long id;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "优惠金额")
    private BigDecimal discount;

    @Schema(description = "实付")
    private BigDecimal paid;

    @Schema(description = "订单状态")
    private OrderStatus status;

    @Schema(description = "停车时长")
    private Long duration;

    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}