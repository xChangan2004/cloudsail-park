package com.changan.model.vo;

import com.changan.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "停车订单分页VO")
public class ParkingOrderPageVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "客户昵称")
    private String customerName;

    @Schema(description = "应付金额")
    private BigDecimal amount;

    @Schema(description = "优惠金额")
    private BigDecimal discount;

    @Schema(description = "实付金额")
    private BigDecimal paid;

    @Schema(description = "订单状态")
    private OrderStatus status;

    @Schema(description = "支付时间")
    private LocalDateTime paidTime;

    @Schema(description = "创建时间（下单时间）")
    private LocalDateTime createTime;
}