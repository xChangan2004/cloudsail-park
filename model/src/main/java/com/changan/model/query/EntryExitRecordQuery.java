package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.EntryExitStatus;
import com.changan.common.enums.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "出入记录页查询条件")
public class EntryExitRecordQuery extends PageQuery {

    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "车牌号码")
    private String plateNumber;

    @Schema(description = "入场类型")
    private EntryType entryType;

    @Schema(description = "状态")
    private EntryExitStatus status;

    @Schema(description = "入场开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "入场结束时间")
    private LocalDateTime endTime;
}
