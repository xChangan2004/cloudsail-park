package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.ParkingSpaceStatus;
import com.changan.common.enums.ParkingSpaceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "停车场车位分页查询参数")
public class ParkingSpaceQuery extends PageQuery {

    @NotNull(message = "停车场ID不能为空")
    @Schema(description = "停车场ID")
    private Long lotId;

    @Schema(description = "车位编号")
    private String spaceCode;

    @Schema(description = "车位类型")
    private ParkingSpaceType spaceType;

    @Schema(description = "状态")
    private ParkingSpaceStatus status;
}
