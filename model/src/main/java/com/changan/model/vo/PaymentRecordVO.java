package com.changan.model.vo;

import com.changan.common.enums.PayMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "支付记录分页VO")
public class PaymentRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "第三方交易流水号")
    private String transactionId;

    @Schema(description = "支付方式")
    private PayMethod payMethod;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "回调时间")
    private LocalDateTime callbackTime;
}