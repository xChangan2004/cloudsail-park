package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "我的停车状态（当前在场）")
public class MyParkingStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "停车状态：null=当前不在场，有值=在场信息")
    private Boolean parking;

    @Schema(description = "出入场记录ID")
    private Long recordId;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "已停时长（分钟）")
    private Long parkedMinutes;

    @Schema(description = "预估费用（预留字段，二期用计费策略试算填充）")
    private BigDecimal estimatedFee;
}