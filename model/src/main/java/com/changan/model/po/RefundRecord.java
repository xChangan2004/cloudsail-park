package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("refund_record")
@Schema(description = "退款记录实体")
public class RefundRecord extends BaseEntity {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "支付记录ID")
    private Long paymentId;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "第三方退款流水号")
    private String channelRefundNo;

    @Schema(description = "退款金额")
    private BigDecimal amount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "退款状态")
    private RefundStatus status;

    @Schema(description = "实际退款时间")
    private LocalDateTime refundTime;
}
