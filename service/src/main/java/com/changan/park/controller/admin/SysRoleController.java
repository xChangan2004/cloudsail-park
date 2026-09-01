package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.ISysRoleService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.model.dto.AssignRoleMenuDTO;
import com.changan.model.po.SysRole;
import com.changan.model.query.SysRoleQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system/role")
@Tag(name = "系统角色相关接口")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;

    @GetMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:list")
    @Operation(summary = "查询所有角色列表")
    public R<List<SysRole>> queryRoleList() {
        return R.ok(roleService.queryRoleList());
    }

    @GetMapping("/page")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:list")
    @Operation(summary = "分页查询角色列表")
    public R<PageDTO<SysRole>> queryRolePage(SysRoleQuery query) {
        return R.ok(roleService.queryRolePage(query));
    }

    @PostMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:add")
    @Operation(summary = "新增角色")
    @OperLog(type = "系统角色", subType = "新增角色")
    public R<Void> saveRole(@Valid @RequestBody SysRole sysRole) {
        roleService.saveRole(sysRole);
        return R.ok();
    }

    @PutMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:edit")
    @Operation(summary = "修改角色")
    @OperLog(type = "系统角色", subType = "修改角色")
    public R<Void> updateRole(@Valid @RequestBody SysRole sysRole) {
        roleService.updateRole(sysRole);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:delete")
    @Operation(summary = "删除角色")
    @OperLog(type = "系统角色", subType = "删除角色")
    public R<Void> deleteRoleById(
            @PathVariable Long id) {
        roleService.deleteRoleById(id);
        return R.ok();
    }

    @DeleteMapping
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:delete")
    @Operation(summary = "批量删除角色")
    @OperLog(type = "系统角色", subType = "批量删除角色")
    public R<Void> batchDeleteRole(
            @RequestBody List<Long> ids) {
        roleService.batchDeleteRole(ids);
        return R.ok();
    }

    @GetMapping("/{id}/menus")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:list")
    @Operation(summary = "根据角色ID查询绑定的菜单")
    public R<List<Long>> queryRoleMenusByRoleId(
            @PathVariable Long id) {
        return R.ok(roleService.queryRoleMenusByRoleId(id));
    }

    @PostMapping("/assign")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:role:assign")
    @Operation(summary = "分配角色菜单")
    @OperLog(type = "系统角色", subType = "分配角色菜单")
    public R<Void> assignRole(@Valid @RequestBody AssignRoleMenuDTO assignDTO) {
        roleService.assignRole(assignDTO);
        return R.ok();
    }
}
