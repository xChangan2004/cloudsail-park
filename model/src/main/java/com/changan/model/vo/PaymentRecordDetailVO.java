package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付记录详情VO")
public class PaymentRecordDetailVO extends PaymentRecordVO {

    @Schema(description = "第三方交易流水号")
    private String transactionId;

    @Schema(description = "回调内容（JSON）")
    private String callbackContent;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "回调时间")
    private LocalDateTime callbackTime;

    @Schema(description = "关闭/失败时间")
    private LocalDateTime closeTime;
}
