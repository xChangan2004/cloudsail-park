package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserResetPwdDTO {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = RegexConstants.PASSWORD_PATTERN, message = "密码格式不正确")
    @Schema(description = "密码")
    private String password;
}
