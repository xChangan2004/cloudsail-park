package com.changan.model.vo;

import com.changan.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrderDistributionVO {

    @Schema(description = "订单状态")
    private OrderStatus status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "订单数")
    private Long count;
}