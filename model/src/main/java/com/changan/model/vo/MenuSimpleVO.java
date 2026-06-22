package com.changan.model.vo;

import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "菜单简单信息")
public class MenuSimpleVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "菜单类型")
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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子菜单列表")
    private List<MenuSimpleVO> children;
}
