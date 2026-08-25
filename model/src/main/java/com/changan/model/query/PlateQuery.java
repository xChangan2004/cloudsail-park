package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.EnergyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "车牌信息分页查询条件")
public class PlateQuery extends PageQuery {

    @Schema(description = "车牌号码")
    private String plateName;

    @Schema(description = "能源类型")
    private EnergyType energyType;
}
