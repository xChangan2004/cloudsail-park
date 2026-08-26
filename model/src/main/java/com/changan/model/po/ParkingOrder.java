package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("parking_order")
@Schema(description = "停车订单实体")
public class ParkingOrder extends BaseEntity {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "出入记录ID")
    private Long recordId;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "应付金额")
    private BigDecimal amount;

    @Schema(description = "优惠")
    private BigDecimal discount;

    @Schema(description = "实付")
    private BigDecimal paid;

    @Schema(description = "状态")
    private OrderStatus status;

    @Schema(description = "实际支付时间")
    private LocalDateTime paidTime;
}
