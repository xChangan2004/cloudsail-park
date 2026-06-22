package com.changan.model.query;

import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Schema(description = "菜单查询参数")
public class SysMenuQuery {

    private String name;

    private MenuType type;

    private CommonStatus status;
}
