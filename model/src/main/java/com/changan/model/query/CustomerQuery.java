package com.changan.model.query;

import com.changan.common.domain.query.PageQuery;
import com.changan.common.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "客户分页查询条件")
public class CustomerQuery extends PageQuery {

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态")
    private CommonStatus status;
}
