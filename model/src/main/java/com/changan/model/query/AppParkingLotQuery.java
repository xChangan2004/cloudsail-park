package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "用户端停车场分页查询参数")
public class AppParkingLotQuery extends PageQuery {

    @Schema(description = "名称关键词（模糊）")
    private String name;

    @Schema(description = "当前定位经度，传入时按距离升序", example = "104.065735")
    private BigDecimal lng;

    @Schema(description = "当前定位纬度，传入时按距离升序", example = "30.659462")
    private BigDecimal lat;

    @Schema(description = "附近范围（公里），与lng/lat同时传入时生效", example = "10")
    @DecimalMin(value = "3", message = "范围最小3公里")
    private BigDecimal radiusKm;
}
