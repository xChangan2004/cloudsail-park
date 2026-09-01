package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.ISysMenuService;
import com.changan.common.domain.R;
import com.changan.model.po.SysMenu;
import com.changan.model.query.SysMenuQuery;
import com.changan.model.vo.MenuSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/menu")
@Tag(name = "系统菜单相关接口")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService menuService;

    @GetMapping("/list")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:menu:list")
    @Operation(summary = "获取所有菜单列表")
    public R<List<MenuSimpleVO>> queryMenuList(SysMenuQuery query) {
        return R.ok(menuService.queryMenuList(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:menu:edit")
    @Operation(summary = "查询菜单详情")
    public R<SysMenu> queryMenuById(@PathVariable Long id) {
        return R.ok(menuService.queryMenuById(id));
    }

    @PostMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:menu:add")
    @Operation(summary = "新增菜单")
    @OperLog(type = "系统菜单", subType = "新增菜单")
    public R<Void> saveMenu(@Valid @RequestBody SysMenu sysMenu) {
        menuService.saveMenu(sysMenu);
        return R.ok();
    }

    @PutMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:menu:edit")
    @Operation(summary = "修改菜单")
    @OperLog(type = "系统菜单", subType = "修改菜单")
    public R<Void> updateMenu(@Valid @RequestBody SysMenu sysMenu) {
        menuService.updateMenu(sysMenu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:menu:delete")
    @Operation(summary = "删除菜单")
    @OperLog(type = "系统菜单", subType = "删除菜单")
    public R<Void> deleteMenuById(@PathVariable Long id) {
        menuService.deleteMenuById(id);
        return R.ok();
    }
}
