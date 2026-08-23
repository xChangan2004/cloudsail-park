package com.changan.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_menu")
@Schema(name = "系统菜单实体")
public class SysMenu extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "菜单类型")
    @NotNull(message = "菜单类型不能为空")
    private MenuType type;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "状态")
    private CommonStatus status;

    @Schema(description = "是否可见，默认 true")
    private Boolean visible = true;

    @Schema(description = "是否缓存，默认 true")
    private Boolean keepAlive = true;
}
