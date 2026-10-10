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

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "已停时长（秒）")
    private Long seconds;

    @Schema(description = "预估费用")
    private BigDecimal estimatedFee;
}