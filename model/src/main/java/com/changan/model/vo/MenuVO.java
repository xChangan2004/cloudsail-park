package com.changan.model.vo;

import com.changan.common.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "菜单节点")
public class MenuVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID（0=顶级）")
    private Long parentId;

    @Schema(description = "菜单名称", example = "停车场管理")
    private String name;

    @Schema(description = "菜单类型：1=目录 2=菜单 3=按钮")
    private MenuType type;

    @Schema(description = "路由路径", example = "/parking")
    private String path;

    @Schema(description = "组件路径", example = "parking/lot/index")
    private String component;

    @Schema(description = "菜单图标", example = "ParkingIcon")
    private String icon;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "子菜单列表")
    private List<MenuVO> children;
}