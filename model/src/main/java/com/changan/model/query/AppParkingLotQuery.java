package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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

    @Schema(description = "当前定位经度", example = "104.065735")
    @NotNull(message = "未获取到定位，请开启定位后重试")
    private BigDecimal lng;

    @Schema(description = "当前定位纬度", example = "30.659462")
    @NotNull(message = "未获取到定位，请开启定位后重试")
    private BigDecimal lat;

    @Schema(description = "附近范围（公里）", example = "10")
    @NotNull(message = "查询范围不能为空")
    @DecimalMin(value = "0.5", message = "范围最小0.5公里")
    private BigDecimal radiusKm;

    @Schema(description = "只看有空位")
    private Boolean onlyFree;

    @Schema(description = "排序：smart(有无空位优先+距离) / distance(距离升序) / price(价格升序)",
            allowableValues = {"smart", "distance", "price"})
    private String sortType;
}
