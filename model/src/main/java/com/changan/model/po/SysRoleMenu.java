package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_role_menu")
@Schema(name = "角色菜单关联实体")
public class SysRoleMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "菜单ID")
    private Long menuId;
}
