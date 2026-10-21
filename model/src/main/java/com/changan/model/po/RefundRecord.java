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

    /**
     * 渠道侧关联号：支付宝为交易号 trade_no（退款不产生新交易，与支付流水的 transaction_id 相同），
     * 微信为退款单号 refund_id。渠道退款的唯一标识是「该号 + refundNo（out_request_no）」。
     */
    @Schema(description = "渠道侧关联号：支付宝为交易号 trade_no，微信为退款单号 refund_id")
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
