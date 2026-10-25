package com.changan.park.controller.app;

import com.changan.common.constants.RegexConstants;
import com.changan.common.domain.R;
import com.changan.model.dto.AppLoginDTO;
import com.changan.model.dto.BindPhoneDTO;
import com.changan.model.dto.BindPhoneWxDTO;
import com.changan.model.dto.WxLoginDTO;
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

    @PostMapping("/wx/login")
    @Operation(summary = "微信小程序登录")
    public R<AppLoginVO> wxLogin(@RequestBody @Valid WxLoginDTO dto) {
        return R.ok(authService.wxLogin(dto));
    }

    @PostMapping("/bind-phone")
    @Operation(summary = "绑定手机号（短信验证码）")
    public R<Void> bindPhone(@RequestBody @Valid BindPhoneDTO dto) {
        authService.bindPhone(dto);
        return R.ok();
    }

    @PostMapping("/bind-phone/wx")
    @Operation(summary = "绑定手机号（微信一键获取手机号）")
    public R<String> bindPhoneByWxCode(@RequestBody @Valid BindPhoneWxDTO dto) {
        return R.ok(authService.bindPhoneByWxCode(dto));
    }

    @PostMapping("/bind-wx")
    @Operation(summary = "绑定微信")
    public R<Void> bindWx(@RequestBody @Valid WxLoginDTO dto) {
        authService.bindWx(dto);
        return R.ok();
    }

    @PostMapping("/unbind-wx")
    @Operation(summary = "解绑微信")
    public R<Void> unbindWx() {
        authService.unbindWx();
        return R.ok();
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
