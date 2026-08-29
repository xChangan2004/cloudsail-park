package com.changan.park.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.changan.common.constants.Constants;
import com.changan.park.service.ISysUserService;
import com.changan.common.domain.R;
import com.changan.common.domain.dto.PageDTO;
import com.changan.common.enums.CommonStatus;
import com.changan.model.dto.UserFormDTO;
import com.changan.model.dto.UserLoginDTO;
import com.changan.model.dto.UserResetPwdDTO;
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

    @PostMapping
    @Operation(summary = "新增用户")
    public R<Void> saveUser(@Valid @RequestBody UserFormDTO dto) {
        userService.saveUser(dto);
        return R.ok();
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询用户列表")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:page")
    public R<PageDTO<UserPageVO>> queryUserPage(SysUserQuery query) {
        return R.ok(userService.queryUserPage(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户详情")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    public R<UserDetailVO> queryUserById(@PathVariable @NotNull(message = "用户id不能为空") Long id) {
        return R.ok(userService.queryUserById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换用户状态")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    public R<Void> updateUserStatus(
            @PathVariable @NotNull(message = "用户id不能为空") Long id,
            @RequestParam @NotNull(message = "状态不能为空") CommonStatus status) {
        userService.updateUserStatus(id, status);
        return R.ok();
    }

    @PutMapping("/resetPwd")
    @Operation(summary = "重置密码")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    public R<Void> resetPwd(@Valid @RequestBody UserResetPwdDTO dto) {
        userService.resetPwd(dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:delete")
    public R<Void> deleteUserById(@PathVariable Long id) {
        userService.deleteUserById(id);
        return R.ok();
    }

    @PutMapping
    @Operation(summary = "修改用户")
//    @SaCheckPermission(type = Constants.Admin.TYPE, value = "system:user:edit")
    public R<Void> updateUser(@Valid @RequestBody UserFormDTO dto) {
        userService.updateUser(dto);
        return R.ok();
    }
}
