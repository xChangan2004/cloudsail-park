package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.OperateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "操作日志分页查询条件")
public class SysOperateLogQuery extends PageQuery {

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "操作模块")
    private String type;

    @Schema(description = "操作名（模糊）")
    private String subType;

    @Schema(description = "操作开始时间")
    private LocalDateTime beginTime;

    @Schema(description = "操作结束时间")
    private LocalDateTime endTime;

    @Schema(description = "操作结果")
    private OperateStatus success;
}
