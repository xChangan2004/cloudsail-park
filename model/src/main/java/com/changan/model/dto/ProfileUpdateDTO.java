package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import com.changan.common.enums.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateDTO {

    @Schema(description = "昵称", example = "长安")
    @Size(max = 20, message = "昵称最长 20 个字符")
    private String nickname;

    @Schema(description = "手机号码", example = "13400000001")
    @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "手机号码格式错误")
    private String phone;

    @Schema(description = "性别")
    private Sex sex;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "新密码，不传或传空则不修改密码")
    @Pattern(regexp = "^$|" + RegexConstants.PASSWORD_PATTERN, message = "密码为 6-32 位字母、数字或下划线")
    private String password;

    @Schema(description = "确认密码，修改密码时必填")
    private String confirmPassword;
}
