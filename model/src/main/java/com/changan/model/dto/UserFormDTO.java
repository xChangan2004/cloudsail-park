package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import com.changan.common.enums.CommonStatus;
import com.changan.common.enums.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UserFormDTO {

    @Schema(description = "用户ID")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = RegexConstants.PASSWORD_PATTERN, message = "密码格式不符合规范")
    @Schema(description = "登录密码")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "手机号格式错误")
    private String phone;

    @Schema(description = "邮箱")
    @Pattern(regexp = RegexConstants.EMAIL_PATTERN, message = "邮箱格式错误")
    private String email;

    @Schema(description = "性别")
    private Sex sex;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "账号状态 1启用 0禁用")
    private CommonStatus status;

    @NotEmpty(message = "至少分配一个角色")
    @Schema(description = "角色ID集合")
    private List<Long> roleIds;
}
