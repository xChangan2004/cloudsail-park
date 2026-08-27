package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "退款记录分页查询条件")
public class RefundRecordQuery extends PageQuery {

    @Schema(description = "订单号（精确）")
    private String orderNo;

    @Schema(description = "退款开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "退款结束时间")
    private LocalDateTime endTime;
}