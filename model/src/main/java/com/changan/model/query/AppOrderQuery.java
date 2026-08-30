package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "用户端订单分页查询条件")
public class AppOrderQuery extends PageQuery {

    @Schema(description = "订单状态（不传查全部）")
    private OrderStatus status;
}
