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
@Schema(description = "角色分页查询参数")
public class SysRoleQuery extends PageQuery {

    @Schema(description = "角色名称（模糊查询）")
    private String name;

    @Schema(description = "角色权限标识符")
    private String code;

    @Schema(description = "状态")
    private CommonStatus status;
}
