package com.changan.model.vo;

import com.changan.common.enums.EntryExitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "App端停车记录详情")
public class AppRecordDetailVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "停车场经度")
    private BigDecimal lotLng;

    @Schema(description = "停车场纬度")
    private BigDecimal lotLat;

    @Schema(description = "停车场地址")
    private String lotAddress;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "出场时间，进行中为空")
    private LocalDateTime exitTime;

    @Schema(description = "入场图片")
    private String entryImage;

    @Schema(description = "出场图片")
    private String exitImage;

    @Schema(description = "状态")
    private EntryExitStatus status;

    @Schema(description = "已停时长（秒）")
    private Long seconds;

    @Schema(description = "费用预估（元）")
    private BigDecimal estimatedFee;
}
