package com.changan.model.vo;

import com.changan.common.domain.query.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "优惠券模板分页查询条件")
public class CouponTemplateQuery extends PageQuery {

    @Schema(description = "券名称（模糊）")
    private String name;

    @Schema(description = "状态")
    private Integer status;
}