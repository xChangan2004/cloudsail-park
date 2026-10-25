package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "微信登录后绑定手机号表单（短信验证码方式）")
public class BindPhoneDTO {

    @NotBlank(message = "手机号码不能为空")
    @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "手机号码格式不正确")
    @Schema(description = "手机号码")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = RegexConstants.CODE_PATTERN, message = "验证码格式不正确")
    @Schema(description = "短信验证码")
    private String code;
}
