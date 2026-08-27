package com.changan.model.vo;

import com.changan.common.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "退款记录分页VO")
public class RefundRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "第三方退款流水号")
    private String transactionId;

    @Schema(description = "退款金额")
    private BigDecimal amount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "状态")
    private RefundStatus status;

    @Schema(description = "退款时间")
    private LocalDateTime createTime;
}