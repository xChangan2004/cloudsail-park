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
@Schema(description = "支付记录分页查询条件")
public class PaymentRecordQuery extends PageQuery {

    @Schema(description = "订单号（精确）")
    private String orderNo;

    @Schema(description = "第三方交易流水号（精确）")
    private String transactionId;

    @Schema(description = "支付开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "支付结束时间")
    private LocalDateTime endTime;
}