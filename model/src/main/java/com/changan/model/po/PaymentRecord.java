package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.PayMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("payment_record")
@Schema(description = "支付记录实体")
public class PaymentRecord extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    private Long orderId;

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

    @Schema(description = "回调内容（JSON）")
    private String callbackContent;
}
