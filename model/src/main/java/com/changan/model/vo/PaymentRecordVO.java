package com.changan.model.vo;

import com.changan.common.enums.PayMethod;
import com.changan.common.enums.PaymentStatus;
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

    @Schema(description = "商户支付单号")
    private String paymentNo;

    @Schema(description = "支付方式")
    private PayMethod payMethod;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "状态")
    private PaymentStatus status;

    @Schema(description = "发起时间")
    private LocalDateTime createTime;

    @Schema(description = "该订单的第几次支付尝试")
    private Integer attemptNo;

    @Schema(description = "该订单共几次支付尝试")
    private Integer attemptTotal;
}
