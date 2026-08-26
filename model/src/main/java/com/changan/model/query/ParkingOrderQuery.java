package com.changan.model.query;

import com.changan.common.constants.RegexConstants;
import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "停车订单分页查询条件")
public class ParkingOrderQuery extends PageQuery {

    @Schema(description = "订单号（精确）")
    private String orderNo;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "车牌号（模糊）")
    private String plateNumber;      // 运营常用"这车牌欠费没"

    @Schema(description = "订单状态")
    private OrderStatus status;      // 筛"待支付"追欠款是高频场景

    @Schema(description = "下单开始时间")
    @JsonFormat(pattern = RegexConstants.DATE_TIME_FORMAT_PATTERN, timezone = "GMT+8")
    private LocalDateTime beginTime;

    @Schema(description = "下单结束时间")
    @JsonFormat(pattern = RegexConstants.DATE_TIME_FORMAT_PATTERN, timezone = "GMT+8")
    private LocalDateTime endTime;
}