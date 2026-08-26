package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "出场账单VO")
public class ExitBillVO {

    @Schema(description = "出入记录ID")
    private Long recordId;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "出场时间")
    private LocalDateTime exitTime;

    @Schema(description = "停车时间（单位：分钟）")
    private Long duration;

    @Schema(description = "本次应用的计费规则名称")
    private String ruleName;

    @Schema(description = "计费规则说明")
    private String ruleDesc;

    @Schema(description = "应收金额")
    private BigDecimal amount;

    @Schema(description = "订单号")
    private String orderNo;
}
