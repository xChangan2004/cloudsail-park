package com.changan.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "Token 值")
    private String token;

    @Schema(description = "登录用户信息")
    private UserInfoVO userInfo;

    @Schema(description = "角色标识列表", example = "[\"super_admin\"]")
    private List<String> roleList;

    @Schema(description = "权限标识列表", example = "[\"park:lot:list\", \"park:lot:create\"]")
    private List<String> permList;

    @Schema(description = "可访问的菜单树（侧边栏渲染用）")
    private List<MenuVO> menus;
}
