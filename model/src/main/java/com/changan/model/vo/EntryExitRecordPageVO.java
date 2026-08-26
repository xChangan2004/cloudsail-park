package com.changan.model.vo;

import com.changan.common.enums.EntryExitStatus;
import com.changan.common.enums.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "出入记录分页实体对象")
public class EntryExitRecordPageVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "停车场名称")
    private String lotName;

    @Schema(description = "车位编号")
    private String spaceCode;

    @Schema(description = "客户昵称")
    private String customerName;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场类型")
    private EntryType entryType;

    @Schema(description = "状态")
    private EntryExitStatus status;

    @Schema(description = "入场时间")
    private LocalDateTime entryTime;

    @Schema(description = "出场时间")
    private LocalDateTime exitTime;

    @Schema(description = "停车时长（分钟）")
    private Long duration;
}
