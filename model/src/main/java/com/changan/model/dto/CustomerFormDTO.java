package com.changan.model.dto;

import com.changan.common.constants.RegexConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CustomerFormDTO {

    @Schema(description = "客户ID")
    private Long id;

    @NotNull(message = "手机号码不能为空")
    @Pattern(regexp = RegexConstants.PHONE_PATTERN, message = "手机号码格式不正确")
    @Schema(description = "手机号码")
    private String phone;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;
}
