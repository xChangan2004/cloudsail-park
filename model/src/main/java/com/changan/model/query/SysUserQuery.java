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
@Schema(description = "菜单查询参数")
public class SysUserQuery extends PageQuery {

    @Schema(description = "用户名称（模糊查询）")
    private String username;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "状态")
    private CommonStatus status;
}
