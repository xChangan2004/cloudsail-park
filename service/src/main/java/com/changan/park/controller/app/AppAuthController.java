package com.changan.park.controller.app;

import com.changan.common.constants.RegexConstants;
import com.changan.common.domain.R;
import com.changan.model.dto.AppLoginDTO;
import com.changan.model.vo.AppLoginVO;
import com.changan.park.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
@Tag(name = "认证相关接口")
public class AppAuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<AppLoginVO> login(@RequestBody @Valid AppLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @PostMapping("/code")
    @Operation(summary = "发送验证码")
    public R<Void> code(
            @NotNull(message = "手机号码不能为空")
            @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "手机号码格式不正确")
            String phone) {
        authService.code(phone);
        return R.ok();
    }
}
