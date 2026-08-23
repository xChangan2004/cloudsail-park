package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "停车场分页查询参数")
public class ParkingLotQuery extends PageQuery {

    private String name;

    private String address;

    private CommonStatus status;
}
