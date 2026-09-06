package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.config.operlog.OperLog;
import com.changan.common.constants.Constants;
import com.changan.park.service.ISysUserService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.UserLoginDTO;
import com.changan.model.dto.UserSaveDTO;
import com.changan.model.dto.UserUpdateDTO;
import com.changan.model.query.SysUserQuery;
import com.changan.model.vo.LoginVO;
import com.changan.model.vo.UserDetailVO;
import com.changan.model.vo.UserPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/system/user")
@Tag(name = "系统用户相关接口")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService userService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        return R.ok(userService.login(loginDTO));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout() {
        userService.logout();
        return R.ok();
    }

    @PostMapping
    @Operation(summary = "新增用户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:add")
    @OperLog(type = "系统用户", subType = "新增用户")
    public R<Void> saveUser(@Valid @RequestBody UserSaveDTO dto) {
        userService.saveUser(dto);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户列表")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:list")
    public R<PageDTO<UserPageVO>> queryUserPage(SysUserQuery query) {
        return R.ok(userService.queryUserPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户详情")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:list")
    public R<UserDetailVO> queryUserById(@PathVariable @NotNull(message = "用户id不能为空") Long id) {
        return R.ok(userService.queryUserById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换用户状态")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    @OperLog(type = "系统用户", subType = "切换用户状态")
    public R<Void> updateUserStatus(
            @PathVariable @NotNull(message = "用户id不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") CommonStatus status) {
        userService.updateUserStatus(id, status);
        return R.ok();
    }

    @PutMapping("/resetPwd/{id}")
    @Operation(summary = "重置密码")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    @OperLog(type = "系统用户", subType = "重置密码")
    public R<Void> resetPwd(@PathVariable @NotNull(message = "用户id不能为空") Long id) {
        userService.resetPwd(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:delete")
    @OperLog(type = "系统用户", subType = "删除用户")
    public R<Void> deleteUserById(@PathVariable @NotNull(message = "用户id不能为空") Long id) {
        userService.deleteUserById(id);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改用户")
    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    @OperLog(type = "系统用户", subType = "修改用户")
    public R<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return R.ok();
    }
}
